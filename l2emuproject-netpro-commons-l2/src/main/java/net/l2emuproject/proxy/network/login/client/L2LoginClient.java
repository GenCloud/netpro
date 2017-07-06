/*
 * Copyright 2011-2015 L2EMU UNIQUE
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.l2emuproject.proxy.network.login.client;

import static net.l2emuproject.network.protocol.LoginProtocolVersion.MODERN;
import static net.l2emuproject.network.security.LoginCipher.READ_ONLY_C3_C4_TRANSFER_KEY;
import static net.l2emuproject.network.security.LoginCipher.READ_ONLY_MODERN_KEY;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Map;

import net.l2emuproject.network.protocol.ILoginProtocolVersion;
import net.l2emuproject.network.protocol.LoginProtocolVersion;
import net.l2emuproject.network.protocol.ProtocolVersionManager;
import net.l2emuproject.network.security.ICipher;
import net.l2emuproject.network.security.LoginCipher;
import net.l2emuproject.proxy.network.AbstractL2ClientProxy;
import net.l2emuproject.proxy.network.game.NewGameServerConnection;
import net.l2emuproject.proxy.network.login.server.L2LoginServer;
import net.l2emuproject.proxy.network.packets.ProxyRepeatedPacket;

/**
 * Internally represents a L2 client connected to a login server.
 * 
 * @author savormix
 */
public final class L2LoginClient extends AbstractL2ClientProxy
{
	/** Indicates a specific protocol that was used after C3 and before C4 */
	public static final int FLAG_PROTOCOL_TRANSFER = 1 << 0;
	/** Indicates that each game server will specify its additional and dynamic information. */
	public static final int FLAG_SERVER_LIST_C0 = 1 << 1;
	/** Indicates that each listed server will specify its name */
	public static final int FLAG_SERVER_LIST_NAMED = 1 << 2;
	/** Indicates that each listed server will specify the type mask */
	public static final int FLAG_SERVER_LIST_C1 = 1 << 3;
	/** Indicates that each listed server will specify the bracket flag */
	public static final int FLAG_SERVER_LIST_C2 = 1 << 4;
	/** Indicates that each listed server will specify player's character count(s) */
	public static final int FLAG_SERVER_LIST_FREYA = 1 << 5;
	/** Indicates that this client should be treated as a C4 client, regardless of what the server is */
	public static final int FLAG_MODERN_SERVER_2_TRANSFER_CLIENT = 1 << 6;
	/** Prelude protocol where packets are enciphered with C3/C4 transfer key */
	public static final int FLAG_CUSTOM_PROTOCOL_PRELUDE_WITH_TRANSFER_KEY = 1 << 30;
	
	private Map<Integer, NewGameServerConnection> _serverList;
	
	private ICipher _cipher;
	private boolean _firstTime;
	
	private int _protocolFlags;
	
	/**
	 * Creates an internal object representing a login client connection.
	 * 
	 * @param mmoController connection manager
	 * @param socketChannel connection
	 * @throws ClosedChannelException if the given channel was closed during operations
	 */
	protected L2LoginClient(L2LoginClientConnections mmoController, SocketChannel socketChannel) throws ClosedChannelException
	{
		super(mmoController, socketChannel);
		
		_serverList = Collections.emptyMap();
		
		_cipher = null;
		_firstTime = true;
		
		// default (before we actually know it) and not important
		setVersion(ProtocolVersionManager.getInstance().getFallbackProtocolLogin());
	}
	
	@Override
	protected void onDisconnectionImpl()
	{
		_serverList = Collections.emptyMap();
	}
	
	@Override
	protected void onForcedDisconnection()
	{
	}
	
	@Override
	protected boolean decipher(ByteBuffer buf)
	{
		final int size = buf.remaining();
		
		// This forces proxy to work transparently with all possible login protocols
		final boolean legacyProtocol = getProtocol().isOlderThan(MODERN);
		legacy: if (legacyProtocol)
		{
			if (isProtocolFlagSet(FLAG_CUSTOM_PROTOCOL_PRELUDE_WITH_TRANSFER_KEY))
				_cipher = new LoginCipher(READ_ONLY_C3_C4_TRANSFER_KEY);
			else if (size >= 128 + 16 || size == 32)
			{
				final ICipher transfer = new LoginCipher(READ_ONLY_C3_C4_TRANSFER_KEY);
				final ByteBuffer bb = ByteBuffer.allocate(size).order(buf.order());
				System.arraycopy(buf.array(), buf.position(), bb.array(), 0, size);
				transfer.decipher(bb);
				
				if (!LoginCipher.testChecksum(bb, 8)) // legacy client packet checksum scheme
					break legacy;
				
				if (bb.get(0) != (size == 32 ? 0x07 : 0x00))
					break legacy;
				
				enableProtocolFlags(FLAG_PROTOCOL_TRANSFER);
				_cipher = transfer;
				((L2LoginServer)getServer()).initCipher(READ_ONLY_C3_C4_TRANSFER_KEY);
				System.arraycopy(bb.array(), 0, buf.array(), buf.position(), size);
				return true;
			}
		}
		
		getCipher().decipher(buf);
		if (isProtocolFlagSet(FLAG_MODERN_SERVER_2_TRANSFER_CLIENT))
		{
			final int pos = buf.position();
			// allocate 8 extra bytes
			final ByteBuffer fake = ByteBuffer.allocate(buf.capacity() + 8);
			buf.clear();
			fake.put(buf).position(pos); // same position, different limit
			buf.position(pos); // recover position
			buf = fake; // test checksum on the fake one
		}
		if (!LoginCipher.testChecksum(buf, legacyProtocol ? 8 : 16)) // [legacy] client packet checksum scheme
		{
			LOG.info("Malformed client packet received from " + getHostAddress());
			close(new ProxyRepeatedPacket(new byte[] { 0x01, 0x01 })); // system error, try again (in case this was not an incompatible connection attempt)
			return false;
		}
		
		return true;
	}
	
	@Override
	protected void encipher(ByteBuffer buf)
	{
		final boolean first = isFirstTime();
		if (first && (getProtocol().isOlderThanOrEqualTo(LoginProtocolVersion.TRANSFER_C4) || isProtocolFlagSet(FLAG_PROTOCOL_TRANSFER) || isProtocolFlagSet(FLAG_MODERN_SERVER_2_TRANSFER_CLIENT)))
			return;
		
		// this assumes a valid packet was received
		int size = buf.remaining();
		size += (8 - (size & 7)) & 7; // padding
		
		final int limit = buf.limit();
		buf.limit(buf.position() + size);
		try
		{
			if (first)
				LoginCipher.complementEncipherInitialPacket(buf);
			else if (!((L2LoginServer)getTarget()).isFail2j())
				LoginCipher.injectChecksum(buf, 8); // server packet checksum scheme
				
			final ICipher cipher = first ? new LoginCipher(READ_ONLY_MODERN_KEY) : getCipher();
			cipher.encipher(buf);
		}
		finally
		{
			buf.limit(limit);
		}
	}
	
	@Override
	protected String getUID()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected boolean isAuthed()
	{
		return getCipher() != null;
	}
	
	/**
	 * Stores the {@code ServerList} sent to this client.
	 * 
	 * @param serverList game server list
	 */
	public void setServerList(Map<Integer, NewGameServerConnection> serverList)
	{
		_serverList = serverList;
	}
	
	/**
	 * Selects a server from the server list.
	 * 
	 * @param serverID ID of the server to select
	 */
	public void setSelectedServer(int serverID)
	{
		_serverList = Collections.singletonMap(serverID, _serverList.get(serverID));
	}
	
	/**
	 * Returns the selected server.
	 * 
	 * @return selected server
	 */
	public NewGameServerConnection getSelectedServer()
	{
		return _serverList.values().iterator().next();
	}
	
	/**
	 * Initializes the cipher with the Blowfish key received from the login server.
	 * 
	 * @param blowfishKey Blowfish Key
	 */
	public void initCipher(byte[] blowfishKey)
	{
		_cipher = new LoginCipher(isProtocolFlagSet(FLAG_MODERN_SERVER_2_TRANSFER_CLIENT) ? READ_ONLY_C3_C4_TRANSFER_KEY : blowfishKey);
	}
	
	@Override
	public ILoginProtocolVersion getProtocol()
	{
		return (ILoginProtocolVersion)super.getProtocol();
	}
	
	/**
	 * Changes the network protocol version associated with this connection.
	 * 
	 * @param version protocol version
	 */
	public void setVersion(ILoginProtocolVersion version)
	{
		super.setVersion(version);
	}
	
	/**
	 * Sets additional flags related to network protocol detection/negotiation.
	 * 
	 * @param flags flags to set
	 */
	public void enableProtocolFlags(int flags)
	{
		_protocolFlags |= flags;
	}
	
	/**
	 * Disables additional flags related to network protocol detection/negotiation.
	 * 
	 * @param flags flags to unset
	 */
	public void disableProtocolFlags(int flags)
	{
		_protocolFlags &= ~flags;
	}
	
	/**
	 * Tests if a protocol related flag is set.
	 * 
	 * @param flags flags to test
	 * @return are all flags set
	 */
	public boolean isProtocolFlagSet(int flags)
	{
		return (_protocolFlags & flags) == flags;
	}
	
	private ICipher getCipher()
	{
		return _cipher;
	}
	
	private boolean isFirstTime()
	{
		final boolean ft = _firstTime;
		_firstTime = false;
		return ft;
	}
	
	@Override
	public boolean ___supportsAheadOfTimeIntervention()
	{
		return true;
	}
}
