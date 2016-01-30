package org.johnnei.javatorrent.torrent.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.Timer;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerManager;
import org.johnnei.javatorrent.torrent.frame.table.FilesTableModel;
import org.johnnei.javatorrent.torrent.frame.table.PeerTableModel;
import org.johnnei.javatorrent.torrent.frame.table.PieceCellRenderer;
import org.johnnei.javatorrent.torrent.frame.table.PiecesTableModel;
import org.johnnei.javatorrent.torrent.frame.table.ProgressCellRenderer;
import org.johnnei.javatorrent.torrent.frame.table.TorrentTableModel;
import org.johnnei.javatorrent.torrent.frame.table.TrackerTableModel;

public class TorrentFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JTable torrentList;
	private MenubarPanel menubar;
	private List<Torrent> torrents;
	private Timer updateTimer;

	public TorrentFrame(TorrentClient torrentClient) {
		torrents = new ArrayList<Torrent>();

		setPreferredSize(new Dimension(1280, 720));
		setLayout(new BorderLayout());
		setTitle(Version.BUILD);
		pack();
		setLocationRelativeTo(null);
		setPreferredSize(new Dimension(getWidth() + getInsets().left + getInsets().right, getHeight() + getInsets().top + getInsets().bottom));

		JTabbedPane details = createDetailsView(torrentClient.getTrackerManager());
		details.setPreferredSize(new Dimension(getWidth(), 350));

		torrentList = new JTable(new TorrentTableModel(torrents));
		torrentList.setDefaultRenderer(Double.class, new ProgressCellRenderer());
		torrentList.setFillsViewportHeight(true);
		menubar = new MenubarPanel(this, torrentClient);

		add(menubar, BorderLayout.NORTH);
		add(new JScrollPane(torrentList), BorderLayout.CENTER);
		add(details, BorderLayout.SOUTH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		updateTimer = new Timer(1000, this);
		updateTimer.start();
	}

	private JTabbedPane createDetailsView(TrackerManager trackerManager) {
		JTabbedPane detailsPane = new JTabbedPane();
		detailsPane.addTab("General", new TabGeneral(this, trackerManager));

		JTable filesTable = new JTable(new FilesTableModel(this));
		filesTable.setFillsViewportHeight(true);
		filesTable.setDefaultRenderer(Double.class, new ProgressCellRenderer());
		detailsPane.addTab("Files", new JScrollPane(filesTable));

		JTable trackerTable = new JTable(new TrackerTableModel(this, trackerManager));
		trackerTable.setFillsViewportHeight(true);
		detailsPane.addTab("Trackers", new JScrollPane(trackerTable));

		JTable peerTable = new JTable(new PeerTableModel(this));
		peerTable.setFillsViewportHeight(true);
		detailsPane.addTab("Peers", new JScrollPane(peerTable));


		JTable piecesTable = new JTable(new PiecesTableModel(this));
		piecesTable.setFillsViewportHeight(true);
		piecesTable.setDefaultRenderer(Piece.class, new PieceCellRenderer());
		detailsPane.addTab("Pieces", new JScrollPane(piecesTable));

		detailsPane.addTab("Log", new JPanel());

		// Hotkeys
		detailsPane.setMnemonicAt(0, KeyEvent.VK_1);
		detailsPane.setMnemonicAt(1, KeyEvent.VK_2);
		detailsPane.setMnemonicAt(2, KeyEvent.VK_3);
		detailsPane.setMnemonicAt(3, KeyEvent.VK_4);
		detailsPane.setMnemonicAt(4, KeyEvent.VK_5);
		detailsPane.setMnemonicAt(5, KeyEvent.VK_6);

		return detailsPane;
	}

	public void updateData() {
		for (int i = 0; i < torrents.size(); i++) {
			torrents.get(i).pollRates();
		}
	}

	public void addTorrent(Torrent torrent) {
		torrents.add(torrent);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(updateTimer)) {
			updateData();
			repaint();
		}
	}

	public Torrent getSelectedTorrent() {
		int row = torrentList.getSelectedRow();

		if (row == -1) {
			return null;
		}

		return torrents.get(row);
	}

}
