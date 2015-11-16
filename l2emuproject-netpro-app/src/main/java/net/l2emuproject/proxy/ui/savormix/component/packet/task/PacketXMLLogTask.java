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
package net.l2emuproject.proxy.ui.savormix.component.packet.task;

import java.awt.Window;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.l2emuproject.network.mmocore.MMOBuffer;
import net.l2emuproject.network.protocol.IProtocolVersion;
import net.l2emuproject.proxy.state.entity.context.ICacheServerID;
import net.l2emuproject.proxy.ui.ReceivedPacket;
import net.l2emuproject.proxy.ui.savormix.io.conv.ToXMLVisitor;
import net.l2emuproject.proxy.ui.savormix.io.task.AbstractPacketWriteTask;
import net.l2emuproject.util.ISODateTime;

/**
 * @author _dev_
 */
public final class PacketXMLLogTask extends AbstractPacketWriteTask implements ISODateTime
{
	private final IProtocolVersion _protocol;
	private final ICacheServerID _cacheContext;
	private final MMOBuffer _buf;
	private final DateFormat _df;
	
	public PacketXMLLogTask(Window owner, String desc, Path file, IProtocolVersion protocol, ICacheServerID cacheContext)
	{
		super(owner, desc, file);
		
		_protocol = protocol;
		_cacheContext = cacheContext;
		_buf = new MMOBuffer();
		_df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
	}
	
	@Override
	protected void writeHeader(Writer writer) throws IOException
	{
		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<packetLog revision=\"").append(String.valueOf(_protocol.getVersion())).append("\">\r\n");
	}
	
	@Override
	protected void writePacket(Writer writer, ReceivedPacket packet) throws IOException
	{
		ToXMLVisitor.writePacket(packet, _protocol, _buf, _cacheContext, _df, writer);
	}
	
	@Override
	protected void writeFooter(Writer writer) throws IOException
	{
		writer.append("</packetLog>");
	}
}
