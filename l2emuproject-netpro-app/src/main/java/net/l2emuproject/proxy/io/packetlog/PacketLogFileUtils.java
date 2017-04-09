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

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.l2emuproject.network.protocol.IProtocolVersion;
import net.l2emuproject.proxy.io.IOConstants;
import net.l2emuproject.proxy.io.NewIOHelper;
import net.l2emuproject.proxy.io.definitions.VersionnedPacketTable;
import net.l2emuproject.proxy.io.exception.DamagedFileException;
import net.l2emuproject.proxy.io.exception.EmptyPacketLogException;
import net.l2emuproject.proxy.io.exception.FilesizeMeasureException;
import net.l2emuproject.proxy.io.exception.IncompletePacketLogFileException;
import net.l2emuproject.proxy.io.exception.InsufficientlyLargeFileException;
import net.l2emuproject.proxy.io.exception.TruncatedPacketLogFileException;
import net.l2emuproject.proxy.io.exception.UnknownFileTypeException;
import net.l2emuproject.proxy.io.packetlog.LogLoadOptions.LogLoadFlag;
import net.l2emuproject.proxy.network.EndpointType;
import net.l2emuproject.proxy.network.ServiceType;
import net.l2emuproject.proxy.ui.javafx.packet.ProtocolPacketHidingManager;

/**
 * Various methods related to NetPro historical packet log file handling.
 * 
 * @author _dev_
 */
public final class PacketLogFileUtils implements IOConstants
{
	private PacketLogFileUtils()
	{
		// utility class
	}
	
	/**
	 * Returns an iterator that can iterate over packets contained in a NetPro packet log file.
	 * 
	 * @param logFileMetadata log file metadata
	 * @return logfile packet iterator
	 * @throws IOException if a generic I/O error occurs
	 */
	public static final NetProPacketLogFileIterator getPacketIterator(LogFileHeader logFileMetadata) throws IOException
	{
		return new NetProPacketLogFileIterator(logFileMetadata);
	}
	
	/**
	 * Tests whether the packet should be loaded to memory based on user's selection of packets to be loaded.
	 * 
	 * @param packet packet from a log file
	 * @param options user's selected loading options
	 * @return whether the given {@code packet} should be loaded
	 */
	public static final boolean isLoadable(LogFilePacket packet, LogLoadOptions options)
	{
		final Set<LoggedPacketFlag> packetFlags = packet.getFlags();
		final Set<LogLoadFlag> loadFlags = options.getFlags();
		
		if (packetFlags.contains(LoggedPacketFlag.SYNTHETIC) && !loadFlags.contains(LogLoadFlag.INCLUDE_SYNTHETIC))
			return false;
		if (packetFlags.contains(LoggedPacketFlag.HIDDEN) && !loadFlags.contains(LogLoadFlag.INCLUDE_NON_CAPTURED))
			return false;
		
		if (loadFlags.contains(LogLoadFlag.INCLUDE_NON_VISIBLE))
			return true;
		
		final EndpointType endpoint = packet.getEndpoint();
		final IProtocolVersion protocol = options.getProtocol();
		return !ProtocolPacketHidingManager.getInstance().getHidingConfiguration(protocol).get().isHidden(endpoint,
				VersionnedPacketTable.getInstance().getTemplate(protocol, endpoint, packet.getContent()));
	}
	
	/**
	 * Reads a limited amount of data from the specified log file to verify that it is a valid NetPro packet log and stores it in the returned object.
	 * 
	 * @param packetLogFile path to log file
	 * @return log file metadata
	 * @throws FilesizeMeasureException if it was not possible to determine the size of the file, but it is necessary to validate the log file (older versions only)
	 * @throws InsufficientlyLargeFileException if the given file is too small to be a NetPro packet log
	 * @throws IncompletePacketLogFileException if the given file is either a work in progress or it's generation was abruptly terminated
	 * @throws UnknownFileTypeException if the given file is not a NetPro packet log (different/unknown format)
	 * @throws TruncatedPacketLogFileException if the given file does not include all mandatory metadata fields
	 * @throws EmptyPacketLogException if the given packet log is empty
	 * @throws DamagedFileException if the given file contains incoherent metadata
	 * @throws InterruptedException if the operation was cancelled by the user
	 * @throws IOException in case of a general I/O error
	 */
	public static final LogFileHeader getMetadata(Path packetLogFile) throws FilesizeMeasureException, InsufficientlyLargeFileException, IncompletePacketLogFileException, UnknownFileTypeException,
			TruncatedPacketLogFileException, EmptyPacketLogException, DamagedFileException, InterruptedException, IOException
	{
		FilesizeMeasureException fail = null;
		long size = -1;
		try
		{
			size = Files.size(packetLogFile);
			if (size < 9)
				throw new InsufficientlyLargeFileException();
		}
		catch (IOException e)
		{
			fail = new FilesizeMeasureException(e);
		}
		
		try (final SeekableByteChannel channel = Files.newByteChannel(packetLogFile, StandardOpenOption.READ); final NewIOHelper in = new NewIOHelper(channel))
		{
			try
			{
				final long magicValue = in.readLong();
				if (magicValue == LOG_MAGIC_INCOMPLETE)
					throw new IncompletePacketLogFileException();
				
				if (magicValue != LOG_MAGIC)
					throw new UnknownFileTypeException(magicValue);
			}
			catch (BufferOverflowException | EOFException e)
			{
				throw new InsufficientlyLargeFileException();
			}
			
			final int logFileVersion;
			try
			{
				logFileVersion = in.readByte() & 0xFF;
			}
			catch (BufferOverflowException e)
			{
				throw new InsufficientlyLargeFileException();
			}
			
			if (logFileVersion < 1)
				throw new DamagedFileException("Packet log version");
			
			final int headerSize, footerSize;
			final long footerStartPosition;
			
			if (logFileVersion >= 6)
			{
				headerSize = in.readInt();
				footerSize = in.readInt();
				footerStartPosition = in.readLong(); // kind of useless?
				
				if (headerSize < 0)
					throw new DamagedFileException("Header size");
				if (footerSize < 0)
					throw new DamagedFileException("Footer size");
			}
			else
			{
				if (fail != null)
					throw fail;
				
				headerSize = LOG_HEADER_SIZE_PRE_6;
				if (logFileVersion == 5)
					footerSize = LOG_FOOTER_SIZE_5;
				else
					footerSize = LOG_FOOTER_SIZE_PRE_5;
				footerStartPosition = size - footerSize;
			}
			
			if (footerStartPosition < headerSize)
				throw new DamagedFileException("Footer position");
			if (size != -1 && footerSize != size - footerStartPosition)
				throw new DamagedFileException("Footer size");
			
			// default header fields
			ServiceType service = ServiceType.GAME;
			final long creationTime;
			int protocolVersionNumber = -1;
			
			if (logFileVersion < 6)
			{
				if (in.readBoolean())
					service = ServiceType.LOGIN;
			}
			
			creationTime = in.readLong();
			
			if (logFileVersion >= 6)
			{
				if (in.readBoolean())
					service = ServiceType.LOGIN;
				protocolVersionNumber = in.readInt();
			}
			
			final long unreadHeaderBytes = headerSize - in.getPositionInChannel(false);
			if (unreadHeaderBytes < 0)
				throw new DamagedFileException("Header size");
			
			if (unreadHeaderBytes > 0)
			{
				if (logFileVersion <= LOG_VERSION)
					throw new DamagedFileException("Header size");
			}
			
			if (Thread.interrupted())
				throw new InterruptedException();
			
			extractProtocolVersion: if (protocolVersionNumber == -1)
			{
				in.skip(unreadHeaderBytes, false);
				
				// first packet should be [C] SendProtocolVersion
				if (!in.readBoolean()) // not a client packet
					break extractProtocolVersion;
				
				final int packetSize = in.readChar();
				if (packetSize < 1 + 4)
					break extractProtocolVersion;
				
				final int opcode = in.readByte();
				if (opcode != 0x00 && opcode != 0x0E) // Chronicle opcode/Throne opcode
					break extractProtocolVersion;
				
				final ByteBuffer leBuffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
				in.read(leBuffer);
				leBuffer.clear();
				
				protocolVersionNumber = leBuffer.getInt();
			}
			
			int totalPackets = -1;
			Map<Integer, Integer> cp = Collections.emptyMap(), sp = Collections.emptyMap();
			
			footer:
			{
				if (logFileVersion < 5)
					break footer;
				
				in.setPositionInChannel(footerStartPosition);
				totalPackets = in.readInt();
				if (totalPackets < 0)
					throw new DamagedFileException("Packet amount");
				if (totalPackets == 0)
					throw new EmptyPacketLogException();
				
				if (logFileVersion < 6)
					break footer;
				
				cp = new HashMap<>();
				sp = new HashMap<>();
				
				final int blocks = in.readByte();
				for (int i = 0; i < blocks; ++i)
				{
					final EndpointType endpoint = EndpointType.valueOf(in.readBoolean());
					final Map<Integer, Integer> tracker = endpoint.isClient() ? cp : sp;
					final int cnt = in.readInt();
					for (int j = 0; j < cnt; ++j)
					{
						final int packet = in.readInt();
						final int count = in.readInt();
						tracker.put(packet, count);
					}
					
					if (Thread.interrupted())
						throw new InterruptedException();
				}
			}
			
			return new LogFileHeader(packetLogFile, size, logFileVersion, headerSize, footerSize, footerStartPosition, creationTime, service, protocolVersionNumber, totalPackets, cp, sp);
		}
		catch (BufferUnderflowException e)
		{
			throw new TruncatedPacketLogFileException();
		}
	}
}