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
package net.l2emuproject.proxy.ui.savormix.loader;

import static net.l2emuproject.util.ISODateTime.ISO_DATE_TIME_ZONE_MS;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import eu.revengineer.simplejse.logging.BytesizeInterpreter;
import eu.revengineer.simplejse.logging.BytesizeInterpreter.BytesizeUnit;

import net.l2emuproject.lang.L2System;
import net.l2emuproject.lang.L2TextBuilder;
import net.l2emuproject.network.mmocore.MMOBuffer;
import net.l2emuproject.network.protocol.IGameProtocolVersion;
import net.l2emuproject.network.protocol.IProtocolVersion;
import net.l2emuproject.proxy.NetProInfo;
import net.l2emuproject.proxy.io.IOConstants;
import net.l2emuproject.proxy.io.conversion.ToL2PacketHackLogVisitor;
import net.l2emuproject.proxy.io.conversion.ToL2PacketHackRawLogVisitor;
import net.l2emuproject.proxy.io.conversion.ToPacketSamuraiLogVisitor;
import net.l2emuproject.proxy.io.conversion.ToPlaintextVisitor;
import net.l2emuproject.proxy.io.conversion.ToXMLVisitor;
import net.l2emuproject.proxy.io.definitions.VersionnedPacketTable;
import net.l2emuproject.proxy.network.EndpointType;
import net.l2emuproject.proxy.network.ListenSocket;
import net.l2emuproject.proxy.network.ProxySocket;
import net.l2emuproject.proxy.network.ServiceType;
import net.l2emuproject.proxy.network.game.client.L2GameClientConnections;
import net.l2emuproject.proxy.network.game.server.L2GameServerConnections;
import net.l2emuproject.proxy.network.login.client.L2LoginClientConnections;
import net.l2emuproject.proxy.network.login.server.L2LoginServerConnections;
import net.l2emuproject.proxy.network.meta.IPacketTemplate;
import net.l2emuproject.proxy.network.meta.UserDefinedProtocolVersion;
import net.l2emuproject.proxy.script.NetProScriptCache;
import net.l2emuproject.proxy.setup.IPAliasManager;
import net.l2emuproject.proxy.setup.SocketManager;
import net.l2emuproject.proxy.ui.ReceivedPacket;
import net.l2emuproject.proxy.ui.savormix.EventSink;
import net.l2emuproject.proxy.ui.savormix.component.ConnectionPane;
import net.l2emuproject.proxy.ui.savormix.component.ContributorAnimator;
import net.l2emuproject.proxy.ui.savormix.component.DisabledComponentUI;
import net.l2emuproject.proxy.ui.savormix.component.GcInfoDialog;
import net.l2emuproject.proxy.ui.savormix.component.GcInfoDialog.MemorySizeUnit;
import net.l2emuproject.proxy.ui.savormix.component.WatermarkPane;
import net.l2emuproject.proxy.ui.savormix.component.conv.StreamOptionDialog;
import net.l2emuproject.proxy.ui.savormix.component.packet.PacketExplainDialog;
import net.l2emuproject.proxy.ui.savormix.component.packet.PacketInject;
import net.l2emuproject.proxy.ui.savormix.component.packet.PacketList;
import net.l2emuproject.proxy.ui.savormix.component.packet.PacketListCleaner;
import net.l2emuproject.proxy.ui.savormix.component.packet.PacketTableAccessor;
import net.l2emuproject.proxy.ui.savormix.component.packet.config.PacketDefinitionLoadTask;
import net.l2emuproject.proxy.ui.savormix.component.packet.config.PacketDisplayConfig;
import net.l2emuproject.proxy.ui.savormix.component.packet.task.PacketTextLogTask;
import net.l2emuproject.proxy.ui.savormix.component.packet.task.PacketXMLLogTask;
import net.l2emuproject.proxy.ui.savormix.component.script.task.AllScriptReloadTask;
import net.l2emuproject.proxy.ui.savormix.component.script.task.CompiledScriptSearchTask;
import net.l2emuproject.proxy.ui.savormix.component.script.task.DirectoryIndexSearchTask;
import net.l2emuproject.proxy.ui.savormix.io.PacketLogChooser;
import net.l2emuproject.proxy.ui.savormix.io.task.HistoricalLogPacketVisitor;
import net.l2emuproject.proxy.ui.savormix.io.task.LogVisitationTask;
import net.l2emuproject.proxy.ui.savormix.io.task.PacketHackLogLoadTask;
import net.l2emuproject.proxy.ui.savormix.io.task.PacketHackRawLogLoadTask;
import net.l2emuproject.proxy.ui.savormix.io.task.PacketSamuraiLogLoadTask;
import net.l2emuproject.ui.AsyncTask;
import net.l2emuproject.ui.file.BetterExtensionFilter;
import net.l2emuproject.util.concurrent.L2ThreadPool;

/**
 * This class is a part of a reference GUI provided for education purposes only. These classes
 * should help to understand how to interact with the underlying proxy core.<BR>
 * <BR>
 * Creating an own GUI is the preferred way to start using this application.
 * 
 * @author savormix
 */
public final class Frontend extends JFrame implements IOConstants, EventSink
{
	private static final long serialVersionUID = 508940951025465462L;
	// private static final L2Logger LOG = L2Logger.getLogger(Frontend.class);
	private static final int MAX_PACKETS_TO_CLIPBOARD = 1000;
	
	/** Whether ignore new packets as they arrive. */
	public static boolean SCROLL_LOCK = false;
	
	private final PacketInject _injectDialog;
	private final PacketExplainDialog _explainDialog;
	private final StreamOptionDialog _streamDialog;
	private final JMenu _configMenu;
	private volatile Map<IProtocolVersion, PacketDisplayConfig> _configDialogs;
	final PacketLogChooser _logChooser;
	final JFileChooser _importChooser;
	
	final JCheckBox _cbGlobalCapture, _cbSessionCapture;
	private final JLabel _labProtocol, _labCP, _labSP;
	private final JPanel _dc;
	final JProgressBar _pbHeapState;
	
	final JMenu _mExport;
	
	private final BufferedImage _watermark;
	private final EventSink _sink;
	private final DisabledComponentUI _blockFeedback;
	
	AsyncTask<?, ?, ?> _gcTask;
	
	// TODO: load from preferences
	File _lastLogDir = LOG_DIRECTORY.toFile(), _lastImportDir = LOG_DIRECTORY.toFile(), _lastExportDir = LOG_DIRECTORY.toFile();
	
	/**
	 * Constructs the main window and launches the backend.
	 * 
	 * @throws HeadlessException
	 *             if launched on a terminal
	 */
	public Frontend() throws HeadlessException
	{
		super("L2EMU Unique Network Protocol Analysis Application");
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		//setIconImages(getIconList());
		
		// Layout
		final Container root = getContentPane();
		setContentPane(new JLayer<>(root, _blockFeedback = new DisabledComponentUI()));
		root.setLayout(new BorderLayout());
		
		final JPanel bottom = new JPanel(new BorderLayout());
		if (LoadOption.HIDE_CONTRIBUTORS.isNotSet())
		{
			final JPanel s = new JPanel(new GridLayout(1, 0));
			s.setBorder(BorderFactory.createTitledBorder("Contributors"));
			{
				final JLabel name = new JLabel("L2EMU UNIQUE");
				name.setHorizontalAlignment(SwingConstants.CENTER);
				final JLabel desc = new JLabel("http://www.l2emu-unique.net/");
				desc.setHorizontalAlignment(SwingConstants.CENTER);
				
				L2ThreadPool.schedule(new ContributorAnimator(name, desc), 5000);
				
				s.add(name);
				s.add(desc);
			}
			bottom.add(s, BorderLayout.SOUTH);
		}
		{
			final JPanel n = new JPanel(new GridLayout(2, 0));
			{
				(_cbSessionCapture = new JCheckBox("Disable session capture", false)).setVisible(LoadOption.DISABLE_PROXY.isNotSet());
				(_cbGlobalCapture = new JCheckBox("Disable global capture", false)).setVisible(LoadOption.DISABLE_PROXY.isNotSet());
			}
			final JPanel labDC = _dc = new JPanel();
			{
				_labProtocol = new JLabel("PROTOCOL", SwingConstants.CENTER);
				labDC.add(new JLabel("[C]"));
				labDC.add(_labCP = new JLabel("0/0"));
				labDC.add(new JLabel("[S]"));
				labDC.add(_labSP = new JLabel("0/0"));
			}
			final JPanel labRevision = new JPanel();
			{
				final L2TextBuilder tb = new L2TextBuilder();
				if (!"exported".equals(NetProInfo.getRevisionNumber()))
					tb.append('r').append(NetProInfo.getRevisionNumber()).append(' ');
				tb.append("executed on ").append(L2System.isJREMode() ? "JRE" : "JDK");
				labRevision.add(new JLabel(tb.moveToString()));
			}
			{
				(_pbHeapState = new JProgressBar(0, 1_000)).setStringPainted(true);
				final Timer updater = new Timer(1_000, e -> {
					final long free = Runtime.getRuntime().freeMemory(), total = Runtime.getRuntime().totalMemory(), used = total - free;
					final L2TextBuilder tb = new L2TextBuilder(BytesizeInterpreter.consolidate(used, BytesizeUnit.BYTES, BytesizeUnit.BYTES, BytesizeUnit.MEBIBYTES, "0M"));
					int end = tb.indexOf("m");
					tb.setCharAt(end - 1, Character.toUpperCase(tb.charAt(end)));
					tb.setLength(end);
					tb.append(" of ").append(BytesizeInterpreter.consolidate(total, BytesizeUnit.BYTES, BytesizeUnit.BYTES, BytesizeUnit.MEBIBYTES, "0M"));
					end = tb.indexOf("m", end + 1);
					tb.setCharAt(end - 1, Character.toUpperCase(tb.charAt(end)));
					tb.setLength(end);
					tb.append(" (").append(used * 100 / total).append("%)");
					_pbHeapState.setValue((int)(used * _pbHeapState.getMaximum() / total));
					_pbHeapState.setString(tb.moveToString());
				});
				updater.setRepeats(true);
				updater.setCoalesce(true);
				updater.setInitialDelay(0);
				updater.start();
			}
			{
				n.add(_cbSessionCapture);
				n.add(_labProtocol);
				n.add(labRevision);
				n.add(_cbGlobalCapture);
				n.add(labDC);
				n.add(_pbHeapState);
			}
			bottom.add(n, BorderLayout.NORTH);
		}
		root.add(bottom, BorderLayout.SOUTH);
		
		_mExport = new JMenu("Export");
		
		final ConnectionPane cp = new ConnectionPane(new CaptureSettingAccessor(), e -> _mExport.setEnabled(((JTabbedPane)e.getSource()).getSelectedComponent() instanceof PacketList));
		new PacketListCleaner(cp).start();
		if (LoadOption.DISABLE_PROXY.isNotSet())
		{
			L2LoginClientConnections.getInstance().addConnectionListener(cp);
			L2LoginClientConnections.getInstance().addPacketListener(cp);
			L2LoginServerConnections.getInstance().addConnectionListener(cp);
			L2LoginServerConnections.getInstance().addPacketListener(cp);
			L2GameClientConnections.getInstance().addConnectionListener(cp);
			L2GameServerConnections.getInstance().addConnectionListener(cp);
			L2GameClientConnections.getInstance().addPacketListener(cp);
			L2GameServerConnections.getInstance().addPacketListener(cp);
		}
		Loader.ACTIVE_UI_PANE = cp;
		_cbSessionCapture.addActionListener(e -> cp.onSessionCaptureChanged(_cbSessionCapture.isSelected()));
		root.add(cp, BorderLayout.CENTER);
		
		//try
		{
			_watermark = /*LoadOption.HIDE_OVERLAY.isNotSet() ? ImageIO.read(IMage.class.getResource("l2emu_logo.png")) : */null;
			final WatermarkPane gp = new WatermarkPane(_watermark);
			_sink = gp;
			setGlassPane(gp);
			getGlassPane().setVisible(true);
		}
		//catch (IOException e)
		{
			//throw new RuntimeException(e);
		}
		//catch (NullPointerException e)
		{
			//LOG.warn("Watermark overlay image is missing.");
			//throw e;
		}
		
		setPreferredSize(new Dimension(780, 580));
		setLocationByPlatform(true);
		pack();
		
		_injectDialog = new PacketInject(this);
		if (LoadOption.DISABLE_PROXY.isNotSet())
		{
			L2LoginClientConnections.getInstance().addConnectionListener(getInjectDialog());
			L2GameClientConnections.getInstance().addConnectionListener(getInjectDialog());
		}
		
		_explainDialog = new PacketExplainDialog(this);
		
		final JMenuBar mb = new JMenuBar();
		{
			final JMenu file = new JMenu("File");
			file.setMnemonic(KeyEvent.VK_F);
			// file.setToolTipText("Save/open packet logs.");
			{
				_logChooser = new PacketLogChooser(_lastLogDir, _watermark);
				
				final JMenuItem load = new JMenuItem("Open…");
				load.setToolTipText("Opens a packet log file.");
				load.setMnemonic(KeyEvent.VK_O);
				load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
				load.addActionListener(e -> {
					final int result = _logChooser.showOpenDialog(Frontend.this);
					if (result != JFileChooser.APPROVE_OPTION)
						return;
					
					_lastLogDir = _logChooser.getCurrentDirectory();
					//AutoLogger.loadConnections(Frontend.this, _logChooser.getDefaultLegacyLoginProtocol(), _logChooser.getDefaultLegacyGameProtocol(), _logChooser.getSelectedFiles());
				});
				file.add(load);
			}
			{
				_importChooser = new JFileChooser();
				_importChooser.setAcceptAllFileFilterUsed(true);
				_importChooser.setMultiSelectionEnabled(true);
				
				final JMenu imp = new JMenu("Import");
				imp.setToolTipText("Imports packets from a 3rd party packet log file.");
				imp.setMnemonic(KeyEvent.VK_I);
				
				{
					final JMenuItem item = new JMenuItem("L2PacketHack log…");
					item.setToolTipText("Opens a L2PacketHack (l2phx) packet log file.");
					item.addActionListener(e -> {
						_importChooser.setFileFilter(BetterExtensionFilter.create("L2PacketHack packet log", "pLog"));
						
						final int result = _importChooser.showOpenDialog(Frontend.this);
						if (result != JFileChooser.APPROVE_OPTION)
							return;
						
						new PacketHackLogLoadTask(Frontend.this).execute(_importChooser.getSelectedFiles());
					});
					imp.add(item);
				}
				
				{
					final JMenuItem item = new JMenuItem("L2PacketHack raw log…");
					item.setToolTipText("Opens a L2PacketHack (l2phx) raw packet log file.");
					item.addActionListener(e -> {
						_importChooser.setFileFilter(BetterExtensionFilter.create("L2PacketHack raw packet log", "rawLog"));
						
						final int result = _importChooser.showOpenDialog(Frontend.this);
						if (result != JFileChooser.APPROVE_OPTION)
							return;
						
						new PacketHackRawLogLoadTask(Frontend.this).execute(_importChooser.getSelectedFiles());
					});
					imp.add(item);
				}
				
				{
					final JMenuItem item = new JMenuItem("Packet Samurai log…");
					item.setToolTipText("Opens a Packet Samurai packet log file.");
					item.addActionListener(e -> {
						_importChooser.setFileFilter(BetterExtensionFilter.create("Packet Samurai packet log", "psl"));
						
						final int result = _importChooser.showOpenDialog(Frontend.this);
						if (result != JFileChooser.APPROVE_OPTION)
							return;
						
						new PacketSamuraiLogLoadTask(Frontend.this).execute(_importChooser.getSelectedFiles());
					});
					imp.add(item);
				}
				
				file.add(imp);
			}
			{
				_mExport.setToolTipText("Exports individual packets to a specific format.");
				_mExport.setMnemonic(KeyEvent.VK_E);
				
				{
					final JMenu submenu = new JMenu("Selected packet");
					submenu.setMnemonic(KeyEvent.VK_S);
					{
						final JMenuItem item = new JMenuItem("Text (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null || pta.getSelectedPacket() == null)
							{
								JOptionPane.showMessageDialog(Frontend.this, "Please select a packet first.", "No packet", JOptionPane.WARNING_MESSAGE);
								return;
							}
							
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToPlaintextVisitor.writePacket(pta.getSelectedPacket(), pta.getProtocolVersion(), new MMOBuffer(), pta.getCacheContext(), new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS),
										true, sb);
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null || pta.getSelectedPacket() == null)
							{
								JOptionPane.showMessageDialog(Frontend.this, "Please select a packet first.", "No packet", JOptionPane.WARNING_MESSAGE);
								return;
							}
							
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToXMLVisitor.writePacket(pta.getSelectedPacket(), pta.getProtocolVersion(), new MMOBuffer(), pta.getCacheContext(), new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS), sb);
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					_mExport.add(submenu);
				}
				{
					final JMenu submenu = new JMenu("Visible table packets");
					submenu.setMnemonic(KeyEvent.VK_V);
					{
						final JMenuItem item = new JMenuItem("Text (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final Iterator<ReceivedPacket> it = pta.getVisiblePackets().iterator();
							if (!it.hasNext())
								return;
							
							final MMOBuffer buf = new MMOBuffer();
							final DateFormat df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToPlaintextVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, false, sb);
								for (int i = 0; i < MAX_PACKETS_TO_CLIPBOARD && it.hasNext(); ++i)
									ToPlaintextVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, false, sb.append("\r\n"));
								if (it.hasNext())
									sb.append("\r\n…");
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("Text (file)…");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final JFileChooser saveDlg = new JFileChooser();
							saveDlg.setFileFilter(BetterExtensionFilter.create("Plaintext packet log", "txt"));
							if (saveDlg.showSaveDialog(Frontend.this) != JFileChooser.APPROVE_OPTION)
								return;
							
							final Collection<ReceivedPacket> packets = pta.getVisiblePackets();
							new PacketTextLogTask(Frontend.this, "Visible", saveDlg.getSelectedFile().toPath(), pta.getProtocolVersion(), pta.getCacheContext())
									.execute(packets.toArray(new ReceivedPacket[packets.size()]));
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final Iterator<ReceivedPacket> it = pta.getVisiblePackets().iterator();
							if (!it.hasNext())
								return;
							
							final MMOBuffer buf = new MMOBuffer();
							final DateFormat df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToXMLVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, sb);
								for (int i = 0; i < MAX_PACKETS_TO_CLIPBOARD && it.hasNext(); ++i)
									ToXMLVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, sb.append("\r\n"));
								if (it.hasNext())
									sb.append("\r\n…");
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (file)…");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final JFileChooser saveDlg = new JFileChooser();
							saveDlg.setFileFilter(BetterExtensionFilter.create("XML packet log", "xml"));
							if (saveDlg.showSaveDialog(Frontend.this) != JFileChooser.APPROVE_OPTION)
								return;
							
							final Collection<ReceivedPacket> packets = pta.getVisiblePackets();
							new PacketXMLLogTask(Frontend.this, "Visible", saveDlg.getSelectedFile().toPath(), pta.getProtocolVersion(), pta.getCacheContext())
									.execute(packets.toArray(new ReceivedPacket[packets.size()]));
						});
						submenu.add(item);
					}
					// TODO: 3rd party logs
					_mExport.add(submenu);
				}
				{
					final JMenu submenu = new JMenu("Table packets");
					submenu.setMnemonic(KeyEvent.VK_T);
					{
						final JMenuItem item = new JMenuItem("Text (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final Iterator<ReceivedPacket> it = pta.getTablePackets().iterator();
							if (!it.hasNext())
								return;
							
							final MMOBuffer buf = new MMOBuffer();
							final DateFormat df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToPlaintextVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, true, sb);
								for (int i = 0; i < MAX_PACKETS_TO_CLIPBOARD && it.hasNext(); ++i)
									ToPlaintextVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, true, sb.append("\r\n"));
								if (it.hasNext())
									sb.append("\r\n…");
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("Text (file)…");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final JFileChooser saveDlg = new JFileChooser();
							saveDlg.setFileFilter(BetterExtensionFilter.create("Plaintext packet log", "txt"));
							if (saveDlg.showSaveDialog(Frontend.this) != JFileChooser.APPROVE_OPTION)
								return;
							
							final Collection<ReceivedPacket> packets = pta.getTablePackets();
							new PacketTextLogTask(Frontend.this, "Table", saveDlg.getSelectedFile().toPath(), pta.getProtocolVersion(), pta.getCacheContext())
									.execute(packets.toArray(new ReceivedPacket[packets.size()]));
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final Iterator<ReceivedPacket> it = pta.getTablePackets().iterator();
							if (!it.hasNext())
								return;
							
							final MMOBuffer buf = new MMOBuffer();
							final DateFormat df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToXMLVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, sb);
								for (int i = 0; i < MAX_PACKETS_TO_CLIPBOARD && it.hasNext(); ++i)
									ToXMLVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, sb.append("\r\n"));
								if (it.hasNext())
									sb.append("\r\n…");
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (file)…");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final JFileChooser saveDlg = new JFileChooser();
							saveDlg.setFileFilter(BetterExtensionFilter.create("XML packet log", "xml"));
							if (saveDlg.showSaveDialog(Frontend.this) != JFileChooser.APPROVE_OPTION)
								return;
							
							final Collection<ReceivedPacket> packets = pta.getTablePackets();
							new PacketXMLLogTask(Frontend.this, "Table", saveDlg.getSelectedFile().toPath(), pta.getProtocolVersion(), pta.getCacheContext())
									.execute(packets.toArray(new ReceivedPacket[packets.size()]));
						});
						submenu.add(item);
					}
					// TODO: 3rd party logs
					_mExport.add(submenu);
				}
				{
					final JMenu submenu = new JMenu("Memory packets");
					submenu.setMnemonic(KeyEvent.VK_M);
					{
						final JMenuItem item = new JMenuItem("Text (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final Iterator<ReceivedPacket> it = pta.getMemoryPackets().iterator();
							if (!it.hasNext())
								return;
							
							final MMOBuffer buf = new MMOBuffer();
							final DateFormat df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToPlaintextVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, true, sb);
								for (int i = 0; i < MAX_PACKETS_TO_CLIPBOARD && it.hasNext(); ++i)
									ToPlaintextVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, true, sb.append("\r\n"));
								if (it.hasNext())
									sb.append("\r\n…");
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("Text (file)…");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final JFileChooser saveDlg = new JFileChooser();
							saveDlg.setFileFilter(BetterExtensionFilter.create("Plaintext packet log", "txt"));
							if (saveDlg.showSaveDialog(Frontend.this) != JFileChooser.APPROVE_OPTION)
								return;
							
							final Collection<ReceivedPacket> packets = pta.getMemoryPackets();
							new PacketTextLogTask(Frontend.this, "Memory", saveDlg.getSelectedFile().toPath(), pta.getProtocolVersion(), pta.getCacheContext())
									.execute(packets.toArray(new ReceivedPacket[packets.size()]));
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (clipboard)");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final Iterator<ReceivedPacket> it = pta.getMemoryPackets().iterator();
							if (!it.hasNext())
								return;
							
							final MMOBuffer buf = new MMOBuffer();
							final DateFormat df = new SimpleDateFormat(ISO_DATE_TIME_ZONE_MS);
							final L2TextBuilder sb = new L2TextBuilder();
							try
							{
								ToXMLVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, sb);
								for (int i = 0; i < MAX_PACKETS_TO_CLIPBOARD && it.hasNext(); ++i)
									ToXMLVisitor.writePacket(it.next(), pta.getProtocolVersion(), buf, pta.getCacheContext(), df, sb.append("\r\n"));
								if (it.hasNext())
									sb.append("\r\n…");
								Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.moveToString()), null);
							}
							catch (final IOException ex)
							{
								// L2TB doesn't throw
							}
						});
						submenu.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("XML (file)…");
						item.addActionListener(e -> {
							final PacketTableAccessor pta = cp.getCurrentTable();
							if (pta == null)
								return;
							
							final JFileChooser saveDlg = new JFileChooser();
							saveDlg.setFileFilter(BetterExtensionFilter.create("XML packet log", "xml"));
							if (saveDlg.showSaveDialog(Frontend.this) != JFileChooser.APPROVE_OPTION)
								return;
							
							final Collection<ReceivedPacket> packets = pta.getMemoryPackets();
							new PacketXMLLogTask(Frontend.this, "Memory", saveDlg.getSelectedFile().toPath(), pta.getProtocolVersion(), pta.getCacheContext())
									.execute(packets.toArray(new ReceivedPacket[packets.size()]));
						});
						submenu.add(item);
					}
					// TODO: 3rd party logs
					_mExport.add(submenu);
				}
				file.add(_mExport);
			}
			{
				final JMenu export = new JMenu("Transform to");
				export.setToolTipText("Converts packet log file(s) to a different format.");
				export.setMnemonic(KeyEvent.VK_C);
				
				{
					final JMenu raw = new JMenu("3rd party formats");
					raw.setToolTipText("Conversions that reconstruct data in a format compatible with other tools");
					{
						final JMenuItem m2 = new JMenuItem("L2PacketHack packet log…");
						m2.addActionListener(new LogConversionInvoker(new ToL2PacketHackLogVisitor()));
						raw.add(m2);
					}
					{
						final JMenuItem phx = new JMenuItem("L2PacketHack raw log…");
						phx.addActionListener(new LogConversionInvoker(new ToL2PacketHackRawLogVisitor()));
						raw.add(phx);
					}
					{
						final JMenuItem m2 = new JMenuItem("Packet Samurai log…");
						m2.addActionListener(new LogConversionInvoker(new ToPacketSamuraiLogVisitor()));
						raw.add(m2);
					}
					
					export.add(raw);
				}
				
				{
					final JMenu raw = new JMenu("Text");
					raw.setToolTipText("Conversions that present data in textual format");
					{
						final JMenuItem txt = new JMenuItem("Plaintext…");
						txt.addActionListener(new LogConversionInvoker(new ToPlaintextVisitor()));
						raw.add(txt);
					}
					{
						final JMenuItem xml = new JMenuItem("XML…");
						xml.addActionListener(new LogConversionInvoker(new ToXMLVisitor()));
						raw.add(xml);
					}
					
					export.add(raw);
				}
				
				export.addSeparator();
				
				{
					final JMenu raw = new JMenu("Other");
					raw.setToolTipText("Conversions that reconstruct data as it was received from socket");
					{
						final JMenuItem stream = new JMenuItem("Stream…");
						_streamDialog = new StreamOptionDialog(Frontend.this);
						stream.addActionListener(e -> _streamDialog.setVisible(true));
						raw.add(stream);
					}
					
					export.add(raw);
				}
				
				file.add(export);
			}
			
			file.addSeparator();
			
			{
				final JMenuItem gc = new JMenuItem("Manual GC");
				gc.setMnemonic(KeyEvent.VK_G);
				gc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
				gc.setToolTipText("Runs finalization on pending objects and performs garbage collection.");
				gc.addActionListener(e -> {
					if (_gcTask != null)
						return;
					
					gc.setEnabled(false);
					_gcTask = new AsyncTask<Void, Void, Void>(){
						private GcInfoDialog _dialog;
						
						@Override
						protected void onPreExecute()
						{
							_dialog = new GcInfoDialog(Frontend.this).setDisplayMode(MemorySizeUnit.KIBIBYTES);
						}
						
						@Override
						protected Void doInBackground(Void... params)
						{
							_dialog.markBefore();
							{
								System.gc();
								System.runFinalization();
								System.gc();
							}
							_dialog.markAfter();
							return null;
						}
						
						@Override
						protected void onPostExecute(Void result)
						{
							_gcTask = null;
							_dialog.prepareDialog().setVisible(true);
							gc.setEnabled(true);
						}
					}.execute((Void[])null);
				});
				file.add(gc);
			}
			
			{
				final JMenuItem prefs = new JMenuItem("Settings…");
				prefs.setToolTipText("Opens the application configuration dialog.");
				prefs.setMnemonic(KeyEvent.VK_S);
				prefs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
				prefs.setEnabled(false);
				//file.add(prefs);
			}
			
			file.addSeparator();
			{
				final JMenuItem exit = new JMenuItem("Exit");
				exit.setMnemonic(KeyEvent.VK_E);
				exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
				exit.setToolTipText("Closes the graphical user interface and terminates the proxy server.");
				exit.addActionListener(e -> System.exit(0));
				file.add(exit);
			}
			mb.add(file);
			
			final JMenu packets = new JMenu("Packets");
			packets.setMnemonic(KeyEvent.VK_P);
			packets.setToolTipText("Displays a list of packet manipulation options.");
			
			_configMenu = new JMenu("Configure display");
			_configDialogs = Collections.emptyMap();
			if (LoadOption.DISABLE_DEFS.isNotSet())
			{
				_configMenu.setMnemonic(KeyEvent.VK_C);
				// config.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
				_configMenu.setToolTipText("Select packets to be displayed in the packet list view.");
				rebuildProtocolMenu();
				packets.add(_configMenu);
				
				final JMenuItem reload = new JMenuItem("Reload");
				reload.setMnemonic(KeyEvent.VK_R);
				reload.setToolTipText("Reload protocol & packet definitions.");
				reload.addActionListener(e -> {
					final int answer = JOptionPane.showConfirmDialog(Frontend.this, "This will undo changes in any display configurations that are not saved.", "Chain reaction warning",
							JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
					if (answer != JOptionPane.YES_OPTION)
						return;
					
					new PacketDefinitionLoadTask(Frontend.this).execute((Void[])null);
				});
				packets.add(reload);
			}
			
			{
				final JMenuItem inject = new JMenuItem("Inject…");
				inject.setEnabled(LoadOption.DISABLE_PROXY.isNotSet());
				inject.setMnemonic(KeyEvent.VK_I);
				inject.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0));
				inject.setToolTipText("Send own-made packets to the client or server.");
				inject.addActionListener(e -> getInjectDialog().setVisible(true));
				packets.add(inject);
			}
			{
				final JMenuItem inject = new JMenuItem("Explain…");
				inject.setEnabled(LoadOption.DISABLE_DEFS.isNotSet());
				inject.setMnemonic(KeyEvent.VK_E);
				inject.setToolTipText("Interpret raw bytes as a packet content in any protocol version.");
				inject.addActionListener(e -> _explainDialog.setVisible(true));
				packets.add(inject);
			}
			{
				final JMenuItem scroll = new JCheckBoxMenuItem("Scroll lock", SCROLL_LOCK);
				scroll.setEnabled(LoadOption.DISABLE_PROXY.isNotSet());
				scroll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SCROLL_LOCK, 0));
				scroll.setToolTipText("Whether not to automatically scroll down the list as new packets are received.");
				scroll.addActionListener(e -> SCROLL_LOCK = scroll.isSelected());
				packets.addSeparator();
				packets.add(scroll);
			}
			mb.add(packets);
			
			if (LoadOption.DISABLE_SCRIPTS.isNotSet())
			{
				final JMenu scripts = new JMenu("Scripts");
				scripts.setMnemonic(KeyEvent.VK_S);
				scripts.setEnabled(!NetProScriptCache.getInstance().isCompilerUnavailable());
				scripts.setToolTipText(scripts.isEnabled() ? "Allows script management." : "Runtime script management requires NetPro to be run via a JDK executable.");
				{
					{
						final JMenuItem item = new JMenuItem("Reload all");
						item.setMnemonic(KeyEvent.VK_R);
						item.setToolTipText("Loads all scripts in the script directory.");
						item.addActionListener(e -> {
							if (JOptionPane.showConfirmDialog(Frontend.this,
									"This will attempt to load all scripts from the script directory. Managed scripts will be unloaded/reloaded, unmanaged scripts will be loaded a second time. Continue?",
									"Confirm action", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE) != JOptionPane.YES_OPTION)
								return;
							
							new AllScriptReloadTask(Frontend.this).execute((Void[])null);
						});
						scripts.add(item);
					}
					scripts.addSeparator();
					{
						final JMenuItem item = new JMenuItem("Load/reload…");
						item.setMnemonic(KeyEvent.VK_L);
						item.setToolTipText("Loads a specific script.");
						item.addActionListener(e -> {
							final String part = JOptionPane.showInputDialog(Frontend.this, "Input [a part of] the script's FQCN (class name, including all packages):", "Script load",
									JOptionPane.QUESTION_MESSAGE);
							if (part != null)
								new DirectoryIndexSearchTask(Frontend.this).execute(part);
						});
						scripts.add(item);
					}
					{
						final JMenuItem item = new JMenuItem("Unload…");
						item.setMnemonic(KeyEvent.VK_U);
						item.setToolTipText("Unloads a specific script.");
						item.addActionListener(e -> {
							final String part = JOptionPane.showInputDialog(Frontend.this, "Input [a part of] the script's FQCN (class name, including all packages):", "Script unload",
									JOptionPane.QUESTION_MESSAGE);
							if (part != null)
								new CompiledScriptSearchTask(Frontend.this).execute(part);
						});
						scripts.add(item);
					}
				}
				mb.add(scripts);
			}
			
			final JMenu help = new JMenu("Help");
			help.setMnemonic(KeyEvent.VK_H);
			help.setToolTipText("Displays a list of help options.");
			{
				final JMenuItem explain = new JMenuItem("Explain configuration");
				explain.setMnemonic(KeyEvent.VK_E);
				explain.setToolTipText("Explains current proxy server configuration.");
				
				final L2TextBuilder tb = new L2TextBuilder("<html><body><table border=\"1\">");
				tb.append("<th><td>NetPro interception</td><td>NetPro destination</td></th>");
				for (final ProxySocket socket : SocketManager.getInstance().getAuthSockets())
				{
					final InetSocketAddress listenAddress = socket.getListenAddress();
					tb.append("<tr><td>Login server</td><td>").append(IPAliasManager.toUserFriendlyString(listenAddress.getHostString()));
					tb.append(':').append(listenAddress.getPort()).append("</td><td>");
					String serviceIP = socket.getServiceAddress(); // hostname
					try
					{
						serviceIP = IPAliasManager.toUserFriendlyString(InetAddress.getByName(socket.getServiceAddress()).getHostAddress());
					}
					catch (final UnknownHostException e)
					{
						// ignore
					}
					tb.append(serviceIP).append(':').append(socket.getServicePort()).append("</td></tr>");
				}
				for (final ListenSocket socket : SocketManager.getInstance().getGameWorldSockets().values())
				{
					final InetSocketAddress listenAddress = socket.getListenAddress();
					tb.append("<tr><td>Game server</td><td>").append(IPAliasManager.toUserFriendlyString(listenAddress.getHostString()));
					tb.append(':').append(listenAddress.getPort()).append("</td><td>").append("[selected game server address]").append("</td></tr>");
				}
				tb.append("</table></body></html>");
				final String info = tb.moveToString();
				
				explain.addActionListener(e -> JOptionPane.showMessageDialog(Frontend.this, info, "Current configuration", JOptionPane.INFORMATION_MESSAGE));
				help.add(explain);
			}
			help.addSeparator();
			{
				final JMenuItem about = new JMenuItem("About");
				about.setMnemonic(KeyEvent.VK_A);
				about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
				about.setToolTipText("Shows information about this application.");
				
				/*
				final String suffix;
				if (NetProInfo.isUnreleased())
					suffix = "norelease";
				else if (NetProInfo.isSnapshot())
					suffix = "snapshot";
				else
					suffix = "stable";
				
				try (final InputStream is = IMage.class.getResourceAsStream("about_" + suffix + ".htm"); final BufferedInputStream in = new BufferedInputStream(is))
				{
					final ByteArrayOutputStream out = new ByteArrayOutputStream(in.available());
					for (int b; (b = in.read()) != -1;)
						out.write(b);
					final String text = out.toString("UTF-8");
					about.addActionListener(e -> JOptionPane.showMessageDialog(Frontend.this, text.replace("\r\n", "").replace("<revision_number>", NetProInfo.getRevisionNumber()), "About",
							JOptionPane.INFORMATION_MESSAGE, new ImageIcon(IMage.class.getResource("icon-256.png"))));
					help.add(about);
				}
				catch (final IOException e)
				{
					// whatever
				}
				*/
			}
			mb.add(help);
		}
		root.add(mb, BorderLayout.NORTH);
	}
	
	@Override
	public void startIgnoringEvents()
	{
		_sink.startIgnoringEvents();
		
		_blockFeedback.setActive(true);
		repaint();
	}
	
	@Override
	public void stopIgnoringEvents()
	{
		_sink.stopIgnoringEvents();
		
		_blockFeedback.setActive(false);
		repaint();
	}
	
	PacketInject getInjectDialog()
	{
		return _injectDialog;
	}
	
	/**
	 * Returns packet display configuration for the currently open tab.
	 * 
	 * @param version protocol version
	 * @param type connection endpoint
	 * @return display configuration
	 */
	public Set<IPacketTemplate> getCurrentlyDisplayedPackets(IProtocolVersion version, EndpointType type)
	{
		final PacketDisplayConfig dialog = _configDialogs.get(version);
		if (dialog != null)
			return dialog.getPackets(type);
		
		if (LoadOption.DISABLE_DEFS.isNotSet())
			return Collections.emptySet();
		
		// otherwise, all packets are unknown & forced visible
		return Collections.singleton(IPacketTemplate.ANY_DYNAMIC_PACKET);
	}
	
	/**
	 * Reload protocol and packet definitions.
	 * 
	 * @param toBeShownAfterReload a display configuration dialog to be opened
	 */
	public void initReload(IProtocolVersion toBeShownAfterReload)
	{
		for (final PacketDisplayConfig dlg : _configDialogs.values())
		{
			dlg.setVisible(false);
			dlg.dispose();
		}
		
		rebuildProtocolMenu();
		
		if (toBeShownAfterReload == null)
			return;
		
		final PacketDisplayConfig dlg = _configDialogs.get(toBeShownAfterReload);
		if (dlg != null)
			dlg.setVisible(true);
	}
	
	private void rebuildProtocolMenu()
	{
		final Map<IProtocolVersion, PacketDisplayConfig> configDialogs = new HashMap<>();
		
		// does not apply to login protocols at this time
		final SortedMap<String, JMenu> categories = new TreeMap<>();
		
		final List<IProtocolVersion> allProtocols = new ArrayList<>();
		final int separator;
		{
			final VersionnedPacketTable pdm = VersionnedPacketTable.getInstance();
			allProtocols.addAll(pdm.getKnownProtocols(ServiceType.LOGIN));
			{
				separator = allProtocols.size();
			}
			allProtocols.addAll(pdm.getKnownProtocols(ServiceType.GAME));
		}
		
		for (int i = separator; i < allProtocols.size(); ++i)
		{
			final UserDefinedProtocolVersion udpv = (UserDefinedProtocolVersion)allProtocols.get(i);
			final String cat = udpv.getCategory();
			JMenu menu = categories.get(cat);
			if (menu != null)
				continue;
			
			menu = new JMenu(cat);
			categories.put(cat, menu);
		}
		
		final Map<IProtocolVersion, Path> initialConfigs = new HashMap<>();
		for (final Entry<IProtocolVersion, PacketDisplayConfig> e : _configDialogs.entrySet())
			initialConfigs.put(e.getKey(), e.getValue().getLastConfig());
		
		final JMenu loginMenu = new JMenu("Login");
		for (int i = 0; i < allProtocols.size(); ++i)
		{
			final IProtocolVersion protocol = allProtocols.get(i);
			if (!(protocol instanceof UserDefinedProtocolVersion))
				continue;
			
			final UserDefinedProtocolVersion user = (UserDefinedProtocolVersion)protocol;
			
			final PacketDisplayConfig dlg = new PacketDisplayConfig(this, _watermark, protocol, initialConfigs.get(protocol));
			dlg.addListener(Loader.ACTIVE_UI_PANE);
			configDialogs.put(protocol, dlg);
			
			final JMenuItem cfg = new JMenuItem(user.toString());
			cfg.addActionListener(e -> dlg.setVisible(true));
			
			final JMenu container = user instanceof IGameProtocolVersion ? categories.get(user.getCategory()) : loginMenu;
			container.add(cfg);
		}
		
		_configMenu.removeAll();
		
		_configMenu.add(loginMenu);
		_configMenu.addSeparator();
		for (final JMenu catMenu : categories.values())
			_configMenu.add(catMenu);
		
		_configDialogs = configDialogs;
	}
	
	void updateBottomPanel(PacketLogSummary summary)
	{
		if (summary._protocol == null)
		{
			_labProtocol.setVisible(false);
			_dc.setVisible(false);
			_cbSessionCapture.setVisible(false);
			return;
		}
		
		_labProtocol.setText(summary._protocol.toString());
		_labProtocol.setVisible(true);
		final L2TextBuilder tb = new L2TextBuilder(8);
		tb.append(summary._displayedClientPackets).append(summary._unknownClientPackets ? "+" : "").append('/').append(summary._totalClientPackets);
		_labCP.setText(tb.toString());
		tb.setLength(0);
		tb.append(summary._displayedServerPackets).append(summary._unknownServerPackets ? "+" : "").append('/').append(summary._totalServerPackets);
		_labSP.setText(tb.moveToString());
		_dc.setVisible(true);
		
		if (summary._sessionCaptureDisabled == null)
		{
			_cbSessionCapture.setVisible(false);
			return;
		}
		
		_cbSessionCapture.setSelected(summary._sessionCaptureDisabled);
		_cbSessionCapture.setVisible(true);
	}
	
	/** A wrapper class that provides a summary for the packet table in the currently open tab. */
	public static final class PacketLogSummary
	{
		static final PacketLogSummary NO_LOG_VISIBLE = new PacketLogSummary(null, 0, 0, false, 0, 0, false, null);
		
		IProtocolVersion _protocol;
		int _displayedClientPackets, _totalClientPackets, _displayedServerPackets, _totalServerPackets;
		boolean _unknownClientPackets, _unknownServerPackets;
		Boolean _sessionCaptureDisabled;
		
		/**
		 * Constructs this wrapper.
		 * 
		 * @param protocol network protocol version
		 * @param displayedClientPackets amount of client packet types to be included in the packet list
		 * @param totalClientPackets amount of predefined client packet types
		 * @param unknownClientPackets whether to include client packets without a predefined type
		 * @param displayedServerPackets amount of server packet types to be included in the packet list
		 * @param totalServerPackets amount of predefined server packet types
		 * @param unknownServerPackets whether to include server packets without a predefined type
		 * @param sessionCaptureDisabled whether to stop packet capture
		 */
		public PacketLogSummary(IProtocolVersion protocol, int displayedClientPackets, int totalClientPackets, boolean unknownClientPackets, int displayedServerPackets, int totalServerPackets,
				boolean unknownServerPackets, Boolean sessionCaptureDisabled)
		{
			_protocol = protocol;
			_displayedClientPackets = displayedClientPackets;
			_totalClientPackets = totalClientPackets;
			_unknownClientPackets = unknownClientPackets;
			_displayedServerPackets = displayedServerPackets;
			_totalServerPackets = totalServerPackets;
			_unknownServerPackets = unknownServerPackets;
			_sessionCaptureDisabled = sessionCaptureDisabled;
		}
		
		@Override
		public String toString()
		{
			return _protocol != null ? _protocol.toString() : "N/A";
		}
	}
	
	/** Provides access to certain functions related to the currently open tab. */
	public final class CaptureSettingAccessor
	{
		/**
		 * Returns whether packet capture is disabled for all tabs.
		 * 
		 * @return whether packet capure is turned off
		 */
		public boolean isGlobalCaptureDisabled()
		{
			return _cbGlobalCapture.isSelected();
		}
		
		/**
		 * Notifies about a change related to the currently open tab.
		 * 
		 * @param packetLog currently open tab component
		 */
		public void onOpen(PacketList packetLog)
		{
			updateBottomPanel(packetLog != null ? packetLog.getSummary() : PacketLogSummary.NO_LOG_VISIBLE);
		}
	}
	
	private final class LogConversionInvoker implements ActionListener
	{
		private final HistoricalLogPacketVisitor _visitor;
		
		LogConversionInvoker(HistoricalLogPacketVisitor visitor)
		{
			_visitor = visitor;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			final int result = _logChooser.showOpenDialog(Frontend.this);
			if (result != JFileChooser.APPROVE_OPTION)
				return;
			
			final File[] selected = _logChooser.getSelectedFiles();
			final Path[] targets = new Path[selected.length];
			for (int i = 0; i < targets.length; ++i)
				targets[i] = selected[i].toPath();
			
			new LogVisitationTask(Frontend.this, "Converting", _visitor).execute(targets);
		}
	}
}
