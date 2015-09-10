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
package net.l2emuproject.proxy.network.game.client;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import net.l2emuproject.network.ClientProtocolVersion;
import net.l2emuproject.network.IGameProtocolVersion;
import net.l2emuproject.network.ProtocolVersionManager;
import net.l2emuproject.network.mmocore.DataSizeHolder;
import net.l2emuproject.network.security.EmptyCipher;
import net.l2emuproject.network.security.GameCipher;
import net.l2emuproject.network.security.ICipher;
import net.l2emuproject.network.security.LegacyGameCipher;
import net.l2emuproject.network.security.ShuffledOpcodeManager;
import net.l2emuproject.proxy.network.AbstractL2ClientProxy;

/**
 * Internally represents a L2 client connected to a game server.
 * 
 * @author savormix
 */
public final class L2GameClient extends AbstractL2ClientProxy
{
	private ShuffledOpcodeManager _deobfuscator;
	
	private ICipher _cipher;
	private boolean _firstTime;
	
	private boolean _handshakeDone;
	
	private String _account;
	
	/**
	 * Creates an internal object representing a game client connection.
	 * 
	 * @param mmoController connection manager
	 * @param socketChannel connection
	 * @throws ClosedChannelException if the given channel was closed during operations
	 */
	public L2GameClient(L2GameClientConnections mmoController, SocketChannel socketChannel) throws ClosedChannelException
	{
		super(mmoController, socketChannel);
		
		_cipher = EmptyCipher.getInstance();
		_firstTime = true;
		
		// default (before we actually know it) and not important
		setVersion(ProtocolVersionManager.getInstance().getFallbackProtocolGame());
		_handshakeDone = false;
	}
	
	@Override
	protected void onDisconnectionImpl()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onForcedDisconnection()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean decipher(ByteBuffer buf, DataSizeHolder dataSize)
	{
		if (isFirstTime())
			return true;
			
		final int limit = buf.limit();
		buf.limit(buf.position() + dataSize.getSize());
		try
		{
			getCipher().decipher(buf);
			getDeobfuscator().decodeOpcodes(buf);
		}
		finally
		{
			buf.limit(limit);
		}
		
		return true;
	}
	
	@Override
	protected boolean encipher(ByteBuffer buf, int size)
	{
		if (isFirstTime())
		{
			setFirstTime(false);
			buf.position(buf.position() + size);
			return true;
		}
		
		final int limit = buf.limit();
		buf.limit(buf.position() + size);
		try
		{
			getCipher().encipher(buf);
		}
		finally
		{
			buf.limit(limit);
		}
		
		return true;
	}
	
	@Override
	protected String getUID()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Initializes the associated cipher with the key part received from the game server.
	 * 
	 * @param dynamicKeyPart key part
	 */
	public void initCipher(long dynamicKeyPart)
	{
		_cipher = initializeCipher(getProtocol(), dynamicKeyPart);
	}
	
	@Override
	public IGameProtocolVersion getProtocol()
	{
		return (IGameProtocolVersion)super.getProtocol();
	}
	
	/**
	 * Initializes protocol version related fields for this connection.
	 * 
	 * @param version protocol version
	 */
	public void setVersion(IGameProtocolVersion version)
	{
		super.setVersion(version);
		
		_deobfuscator = new ShuffledOpcodeManager(version, false);
	}
	
	/**
	 * Returns the packet opcode shuffling implementation.
	 * 
	 * @return opcode deobfuscator
	 */
	public ShuffledOpcodeManager getDeobfuscator()
	{
		return _deobfuscator;
	}
	
	@Override
	protected boolean isAuthed()
	{
		return getCipher() != EmptyCipher.getInstance();
	}
	
	private ICipher getCipher()
	{
		return _cipher;
	}
	
	private boolean isFirstTime()
	{
		return _firstTime;
	}
	
	public void setFirstTime(boolean firstTime)
	{
		_firstTime = firstTime;
	}
	
	/**
	 * Returns whether network protocol negotiation has been completed for this connection.
	 * 
	 * @return is connection successfully completed
	 */
	public boolean isHandshakeDone()
	{
		return _handshakeDone;
	}
	
	/**
	 * Specifies whether network protocol negotiation has been completed for this connection.
	 * 
	 * @param handshakeDone is connection completed
	 */
	public void setHandshakeDone(boolean handshakeDone)
	{
		_handshakeDone = handshakeDone;
	}
	
	/**
	 * Returns the associated authorization service identifier.
	 * 
	 * @return account name
	 */
	public String getAccount()
	{
		return _account;
	}
	
	/**
	 * Specifies the authorization service identifier to be associated with this connection.
	 * 
	 * @param account account name
	 */
	public void setAccount(String account)
	{
		_account = account;
	}
	
	/**
	 * Initializes the cipher to be associated with a game client connection.
	 * 
	 * @param protocol protocol version
	 * @param dynamicKeyPart game server's secret
	 * @return cipher to be used
	 */
	public static final ICipher initializeCipher(IGameProtocolVersion protocol, long dynamicKeyPart)
	{
		return protocol.isNewerThanOrEqualTo(ClientProtocolVersion.INTERLUDE) ? new GameCipher(dynamicKeyPart) : new LegacyGameCipher((int)dynamicKeyPart);
	}
	
	@Override
	public boolean ___supportsAheadOfTimeIntervention()
	{
		return false;
	}
}
