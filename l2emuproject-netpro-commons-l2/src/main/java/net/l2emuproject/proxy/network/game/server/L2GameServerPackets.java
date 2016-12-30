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
package net.l2emuproject.proxy.network.game.server;

import java.nio.ByteBuffer;

import net.l2emuproject.proxy.network.Proxy;
import net.l2emuproject.proxy.network.ProxyPacketHandler;
import net.l2emuproject.proxy.network.game.client.L2GameClient;
import net.l2emuproject.proxy.network.game.server.packets.CharacterSelected;
import net.l2emuproject.proxy.network.game.server.packets.ExRaidReserveResult;
import net.l2emuproject.proxy.network.game.server.packets.ExShuffleSeedAndPublicKey;
import net.l2emuproject.proxy.network.game.server.packets.VersionCheck;
import net.l2emuproject.proxy.network.packets.ProxyReceivedPacket;

/**
 * Handles incoming game server packets.
 * 
 * @author savormix
 */
public final class L2GameServerPackets extends ProxyPacketHandler
{
	/** Packet identifier for packets that use an extended identifier */
	public static final int OPCODE_FOR_OP2 = 0xFE;
	
	@Override
	protected ProxyReceivedPacket handlePacketImpl(ByteBuffer buf, Proxy server, int opcode)
	{
		final L2GameClient client = (L2GameClient)server.getClient();
		switch (opcode)
		{
			case VersionCheck.OPCODE:
			case VersionCheck.OPCODE_LEGACY:
				// proxy only does what it must do
				// all other invasive ops can be done via scripts
				if (client.isHandshakeDone())
					return null;
				client.setHandshakeDone(true);
				return new VersionCheck(opcode);
			case CharacterSelected.OPCODE:
				return new CharacterSelected();
			case OPCODE_FOR_OP2:
				switch (buf.getChar(1))
				{
					case ExRaidReserveResult.OPCODE2:
						return new ExRaidReserveResult();
					case ExShuffleSeedAndPublicKey.OPCODE2:
						if (client.isHandshakeDone())
							return null;
						client.setHandshakeDone(true);
						return new ExShuffleSeedAndPublicKey();
				}
				//$FALL-THROUGH$
			default:
				// FIXME: enumerate if handshake is not done, to handle ExShuffle bla bla
				return null;
		}
	}
	
	L2GameServerPackets()
	{
		// singleton
	}
	
	/**
	 * Returns a singleton instance of this type.
	 * 
	 * @return an instance of this class
	 */
	public static final L2GameServerPackets getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static final class SingletonHolder
	{
		static final L2GameServerPackets INSTANCE = new L2GameServerPackets();
	}
}
