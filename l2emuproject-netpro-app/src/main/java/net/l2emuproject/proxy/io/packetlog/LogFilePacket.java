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
package net.l2emuproject.proxy.io.packetlog;

import java.util.Set;

import net.l2emuproject.proxy.network.EndpointType;

/**
 * Represents a packet loaded from a NetPro historical packet log file.
 * 
 * @author _dev_
 */
public class LogFilePacket
{
	private final EndpointType _endpoint;
	private final byte[] _content;
	private final long _receivalTime;
	private final Set<LoggedPacketFlag> _flags;
	
	/**
	 * Constructs a historical packet wrapper.
	 * 
	 * @param endpoint packet type
	 * @param content packet body
	 * @param receivalTime time of reception
	 * @param flags additional info
	 */
	public LogFilePacket(EndpointType endpoint, byte[] content, long receivalTime, Set<LoggedPacketFlag> flags)
	{
		_endpoint = endpoint;
		_content = content;
		_receivalTime = receivalTime;
		_flags = flags;
	}
	
	/**
	 * Returns the type of this packet.
	 * 
	 * @return packet type
	 */
	public EndpointType getEndpoint()
	{
		return _endpoint;
	}
	
	/**
	 * Returns the content of this packet.
	 * 
	 * @return packet body
	 */
	public byte[] getContent()
	{
		return _content;
	}
	
	/**
	 * Returns the time of arrival of this packet.
	 * 
	 * @return receival time
	 */
	public long getReceivalTime()
	{
		return _receivalTime;
	}
	
	/**
	 * Returns additional info about this packet.
	 * 
	 * @return additional info flags
	 */
	public Set<LoggedPacketFlag> getFlags()
	{
		return _flags;
	}
}
