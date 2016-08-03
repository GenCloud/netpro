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
package net.l2emuproject.proxy.io.conversion;

import static net.l2emuproject.proxy.io.packetlog.LoggedPacketFlag.HIDDEN;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.l2emuproject.network.mmocore.MMOBuffer;
import net.l2emuproject.network.protocol.IProtocolVersion;
import net.l2emuproject.network.protocol.ProtocolVersionManager;
import net.l2emuproject.proxy.NetProInfo;
import net.l2emuproject.proxy.io.IOConstants;
import net.l2emuproject.proxy.io.definitions.VersionnedPacketTable;
import net.l2emuproject.proxy.io.packetlog.LogFileHeader;
import net.l2emuproject.proxy.io.packetlog.LoggedPacketFlag;
import net.l2emuproject.proxy.network.ServiceType;
import net.l2emuproject.proxy.network.meta.IPacketTemplate;
import net.l2emuproject.proxy.network.meta.L2PpeProvider;
import net.l2emuproject.proxy.network.meta.PacketStructureElementVisitor;
import net.l2emuproject.proxy.network.meta.RandomAccessMMOBuffer;
import net.l2emuproject.proxy.network.meta.exception.InvalidFieldValueInterpreterException;
import net.l2emuproject.proxy.network.meta.exception.InvalidFieldValueModifierException;
import net.l2emuproject.proxy.network.meta.exception.InvalidPacketOpcodeSchemeException;
import net.l2emuproject.proxy.network.meta.exception.PartialPayloadEnumerationException;
import net.l2emuproject.proxy.network.meta.structure.BranchElement;
import net.l2emuproject.proxy.network.meta.structure.FieldElement;
import net.l2emuproject.proxy.network.meta.structure.LoopElement;
import net.l2emuproject.proxy.network.meta.structure.field.FieldValue;
import net.l2emuproject.proxy.network.meta.structure.field.FieldValueReadOption;
import net.l2emuproject.proxy.network.meta.structure.field.InterpreterContext;
import net.l2emuproject.proxy.network.meta.structure.field.bytes.AbstractByteArrayFieldElement;
import net.l2emuproject.proxy.network.meta.structure.field.bytes.ByteArrayFieldValue;
import net.l2emuproject.proxy.network.meta.structure.field.decimal.AbstractDecimalFieldElement;
import net.l2emuproject.proxy.network.meta.structure.field.decimal.DecimalFieldValue;
import net.l2emuproject.proxy.network.meta.structure.field.integer.AbstractIntegerFieldElement;
import net.l2emuproject.proxy.network.meta.structure.field.integer.IntegerFieldValue;
import net.l2emuproject.proxy.network.meta.structure.field.string.AbstractStringFieldElement;
import net.l2emuproject.proxy.network.meta.structure.field.string.StringFieldValue;
import net.l2emuproject.proxy.script.LogLoadScriptManager;
import net.l2emuproject.proxy.state.entity.context.ICacheServerID;
import net.l2emuproject.proxy.ui.ReceivedPacket;
import net.l2emuproject.proxy.ui.savormix.component.packet.DataType;
import net.l2emuproject.proxy.ui.savormix.io.task.HistoricalLogPacketVisitor;
import net.l2emuproject.proxy.ui.savormix.io.task.HistoricalPacketLog;
import net.l2emuproject.util.HexUtil;
import net.l2emuproject.util.ISODateTime;
import net.l2emuproject.util.logging.L2Logger;

/**
 * Writes a raw data stream based on visited packets.
 * 
 * @author _dev_
 */
public class ToPlaintextVisitor implements HistoricalLogPacketVisitor, IOConstants, ISODateTime
{
	static final L2Logger LOG = L2Logger.getLogger(ToPlaintextVisitor.class);
	
	private static final int PACKET_BOUNDARY_MARKER_LEN = 80;
	private static final Pattern HTML_IMG = Pattern.compile("<img src=\".+\" />");
	
	private final MMOBuffer _buf;
	private final DateFormat _df;
	
	private IProtocolVersion _protocol;
	private HistoricalPacketLog _cacheContext;
	BufferedWriter _writer;
	
	/** Constructs this visitor. */
	public ToPlaintextVisitor()
	{
		_buf = new MMOBuffer();
		_df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
	}
	
	@Override
	public void onStart(LogFileHeader logHeader) throws Exception
	{
		final Path logFile = logHeader.getLogFile();
		
		_protocol = ProtocolVersionManager.getInstance().getProtocol(logHeader.getProtocol(), logHeader.getService().isLogin());
		_cacheContext = new HistoricalPacketLog(logFile);
		
		_writer = Files.newBufferedWriter(logFile.resolveSibling(logFile.getFileName() + ".txt"));
		_writer.append("[V").append(String.valueOf(logHeader.getVersion())).append("] ").append(logFile.getFileName().toString()).append("\r\n\r\n");
		_writer.append("Protocol type: ").append(logHeader.getService().isLogin() ? "Auth[Gate]D" : "Lineage II").append("\r\n");
		_writer.append("Protocol version: ").append(String.valueOf(logHeader.getProtocol())).append('/').append(String.valueOf(_protocol)).append("\r\n");
		_writer.append("Packet count: ").append(String.valueOf(logHeader.getPackets())).append("\r\n");
		_writer.append("Creation date: ").append(_df.format(new Date(logHeader.getCreated()))).append("\r\n\r\n");
		
		_writer.append("Text generation date: ").append(_df.format(new Date())).append("\r\n");
		_writer.append("Generator version: ").append(NetProInfo.getRevisionNumber()).append("\r\n");
	}
	
	@Override
	public void onPacket(ReceivedPacket packet, Set<LoggedPacketFlag> flags) throws Exception
	{
		_writer.append("\r\n");
		writePacket(packet, _protocol, _buf, _cacheContext, _df, _writer, flags.contains(HIDDEN));
		_writer.append("\r\n");
	}
	
	/**
	 * Writes a packet in an interpreted plaintext form to {@code writer}.
	 * 
	 * @param packet a packet
	 * @param protocol protocol version
	 * @param buf preallocated content wrapper
	 * @param cacheContext entity cache context
	 * @param df preferred date format
	 * @param writer a string builder wrapper
	 * @return {@code writer}
	 * @throws IOException if packet content cannot be written
	 */
	public static final Appendable writePacket(ReceivedPacket packet, IProtocolVersion protocol, MMOBuffer buf, ICacheServerID cacheContext, DateFormat df, Appendable writer) throws IOException
	{
		writePacket(packet, protocol, buf, cacheContext, df, writer, false);
		return writer;
	}
	
	private static final void writePacket(ReceivedPacket packet, IProtocolVersion protocol, MMOBuffer buf, ICacheServerID cacheContext, DateFormat df, Appendable writer, boolean hidden)
			throws IOException
	{
		final boolean client = packet.getEndpoint().isClient();
		
		packetContent:
		{
			final ByteBuffer body = ByteBuffer.wrap(packet.getBody()).order(ByteOrder.LITTLE_ENDIAN);
			
			final InterpreterContext ctx;
			{
				RandomAccessMMOBuffer wireframe;
				try
				{
					//ServerListTypePublisher.LIST_TYPE.set(_owner.getServerListType());
					final ByteBuffer independentWrapper = body.duplicate().order(body.order());
					wireframe = L2PpeProvider.getPacketPayloadEnumerator().enumeratePacketPayload(protocol, buf.setByteBuffer(independentWrapper), packet::getEndpoint);
				}
				catch (InvalidPacketOpcodeSchemeException e)
				{
					if (!hidden)
					{
						for (int i = 0; i < PACKET_BOUNDARY_MARKER_LEN; ++i)
							writer.append('=');
						writer.append("\r\n").append("INVALID PACKET").append(HexUtil.printData(body.array()));
					}
					break packetContent;
				}
				catch (PartialPayloadEnumerationException e)
				{
					wireframe = e.getBuffer();
					
					final IPacketTemplate template = e.getTemplate();
					if (!hidden && template.isDefined())
					{
						final String message = (packet.getEndpoint().isClient() ? "[C] " : "[S] ") + e.getTemplate();
						if (e.getUnusedBytes() != -1)
							LOG.info(message + " " + e.getMessage());
						else
							LOG.error(message, e);
					}
				}
				ctx = new InterpreterContext(cacheContext, wireframe);
			}
			
			// Enable object analytics and whatnot
			if (cacheContext instanceof HistoricalPacketLog)
				LogLoadScriptManager.getInstance().onLoadedPacket(ServiceType.valueOf(protocol).isLogin(), client, body.array(), protocol, (HistoricalPacketLog)cacheContext, packet.getReceived());
			
			if (hidden)
				return;
			
			for (int i = 0; i < PACKET_BOUNDARY_MARKER_LEN; ++i)
				writer.append('=');
			writer.append("\r\n");
			
			final IPacketTemplate template = VersionnedPacketTable.getInstance().getTemplate(protocol, packet.getEndpoint(), body.array());
			writer.append("\t[").append(client ? 'C' : 'S').append("] ").append(HexUtil.bytesToHexString(template.getPrefix(), ":")).append(' ').append(template.getName()).append("\r\n");
			writer.append('\t').append(df.format(new Date(packet.getReceived()))).append("\r\n");
			body.position(template.getPrefix().length);
			
			final Map<FieldValueReadOption, Object> options = new EnumMap<>(FieldValueReadOption.class);
			options.put(FieldValueReadOption.APPLY_MODIFICATIONS, null);
			options.put(FieldValueReadOption.COMPUTE_INTERPRETATION, ctx);
			template.visitStructureElements(new PacketStructureElementVisitor() {
				private final Map<LoopElement, LoopVisitation> _loops = new HashMap<>();
				
				@Override
				public void onStart(int bytesWithoutOpcodes) throws RuntimeException
				{
					try
					{
						if (bytesWithoutOpcodes > 0)
						{
							for (int i = 0; i < PACKET_BOUNDARY_MARKER_LEN; ++i)
								writer.append('-');
							writer.append("\r\n");
						}
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public void onBranch(BranchElement element, boolean conditionMet) throws RuntimeException
				{
					// do nothing
				}
				
				@Override
				public void onBranchEnd(BranchElement element) throws RuntimeException
				{
					// do nothing
				}
				
				@Override
				public void onLoopStart(LoopElement element, int expectedIterations) throws RuntimeException
				{
					_loops.put(element, new LoopVisitation(expectedIterations));
					
					try
					{
						writer.append(expectedIterations > 0 ? "~~~~ Loop start ~~~~\r\n" : "~~~~ Empty loop ~~~~\r\n");
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public void onLoopIterationStart(LoopElement element) throws RuntimeException
				{
					final LoopVisitation lv = _loops.get(element);
					if (!lv._useDelimiter)
					{
						lv._useDelimiter = true;
						return;
					}
					
					try
					{
						writer.append("~~ Loop element delimiter ~~\r\n");
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public void onLoopIterationEnd(LoopElement element) throws RuntimeException
				{
					// do nothing
				}
				
				@Override
				public void onLoopEnd(LoopElement element) throws RuntimeException
				{
					if (_loops.remove(element)._iterationCount < 1)
						return;
					
					try
					{
						writer.append("~~~~ Loop end ~~~~\r\n");
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public void onByteArrayField(AbstractByteArrayFieldElement element, ByteArrayFieldValue value) throws RuntimeException
				{
					writeAsText(writer, element, value, value != null ? value.value() : null, DataType.BYTES);
				}
				
				@Override
				public void onDecimalField(AbstractDecimalFieldElement element, DecimalFieldValue value) throws RuntimeException
				{
					writeAsText(writer, element, value, value != null ? value.value() : null, DataType.FLOAT);
				}
				
				@Override
				public void onIntegerField(AbstractIntegerFieldElement element, IntegerFieldValue value) throws RuntimeException
				{
					int fieldWidth = 4;
					if (value == null)
					{
						final ByteBuffer fallback = ByteBuffer.allocate(8);
						try
						{
							element.readValue(new MMOBuffer().setByteBuffer(fallback), Collections.emptyMap());
							fieldWidth = fallback.position();
						}
						catch (BufferUnderflowException e)
						{
							// what
						}
						catch (InvalidFieldValueInterpreterException | InvalidFieldValueModifierException e)
						{
							// interp/mod explicitly disabled, cannot happen
						}
					}
					else
						fieldWidth = value.raw().length;
					
					final DataType visualDataType;
					switch (fieldWidth)
					{
						case 1:
							visualDataType = DataType.BYTE;
							break;
						case 2:
							visualDataType = DataType.WORD;
							break;
						case 4:
							visualDataType = DataType.DWORD;
							break;
						case 8:
							visualDataType = DataType.QWORD;
							break;
						default:
							throw new IllegalArgumentException(element + " = " + value);
					}
					writeAsText(writer, element, value, value != null ? value.value() : null, visualDataType);
				}
				
				@Override
				public void onStringField(AbstractStringFieldElement element, StringFieldValue value) throws RuntimeException
				{
					writeAsText(writer, element, value, value != null ? value.value() : null, DataType.STRING);
				}
				
				@Override
				public void onAbruptTermination(BufferUnderflowException e, int remainingBytes) throws RuntimeException
				{
					try
					{
						if (remainingBytes > 0)
							writer.append("and ").append(String.valueOf(remainingBytes)).append(" more bytes…\r\n");
					}
					catch (IOException e2)
					{
						throw new RuntimeException(e2);
					}
				}
				
				@Override
				public void onException(Exception e, int remainingBytes)
				{
					LOG.error("", e);
					
					try
					{
						writer.append("and ").append(String.valueOf(remainingBytes)).append(" more bytes…\r\n");
						writer.append(e.toString()).append("\r\n");
					}
					catch (IOException e2)
					{
						throw new RuntimeException(e2);
					}
				}
				
				@Override
				public void onCompletion(int remainingBytes) throws RuntimeException
				{
					if (remainingBytes < 1)
						return;
					
					try
					{
						writer.append("… and ").append(String.valueOf(remainingBytes)).append(" more bytes\r\n");
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
			}, body, options);
		}
		for (int i = 0; i < PACKET_BOUNDARY_MARKER_LEN; ++i)
			writer.append('=');
	}
	
	static final void writeAsText(Appendable writer, FieldElement<?> field, FieldValue value, Object readValue, DataType visualDataType)
	{
		Object interpretation = null;
		
		if (value != null)
		{
			interpretation = value.interpreted();
			if (interpretation instanceof RenderedImage)
				interpretation = readValue;
		}
		
		if (interpretation == null)
			interpretation = "N/A";
		else if (interpretation instanceof byte[])
			interpretation = HexUtil.bytesToHexString((byte[])interpretation, " ");
		else if (interpretation instanceof CharSequence)
		{
			final String alt = HTML_IMG.matcher(String.valueOf(interpretation)).replaceAll("");
			interpretation = alt.isEmpty() ? readValue : alt;
		}
		
		try
		{
			final String interp = String.valueOf(interpretation);
			writer.append('[').append(visualDataType.getId()).append("] ").append(field.getAlias()).append(": ").append(interp);
			switch (visualDataType)
			{
				case BYTE:
				case DWORD:
				case FLOAT:
				case QWORD:
				case WORD:
					final String read = String.valueOf(readValue);
					if (!interp.equals(read))
						writer.append(" (").append(read).append(')');
					break;
				default:
					break;
			}
			writer.append("\r\n");
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void onEnd() throws Exception
	{
		_writer.flush();
		_writer.close();
	}
	
	private static final class LoopVisitation
	{
		final int _iterationCount;
		boolean _useDelimiter;
		
		LoopVisitation(int iterationCount)
		{
			_iterationCount = iterationCount;
			_useDelimiter = false;
		}
	}
}
