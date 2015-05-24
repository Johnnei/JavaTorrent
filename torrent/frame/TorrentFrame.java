package torrent.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;

import torrent.JavaTorrent;
import torrent.TorrentManager;
import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;
import torrent.frame.table.TorrentTableModel;

public class TorrentFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private TorrentDetails details;
	private JTable torrentList;
	private MenubarPanel menubar;
	private List<Torrent> torrents;
	private Timer updateTimer;

	public TorrentFrame(TorrentManager torrentManager, TrackerManager trackerManager) {
		torrents = new ArrayList<Torrent>();

		setPreferredSize(new Dimension(1280, 720));
		setLayout(new BorderLayout());
		setTitle(JavaTorrent.BUILD);
		pack();
		setLocationRelativeTo(null);
		setPreferredSize(new Dimension(getWidth() + getInsets().left + getInsets().right, getHeight() + getInsets().top + getInsets().bottom));

		details = new TorrentDetails(this, trackerManager);
		details.setPreferredSize(new Dimension(getWidth(), 350));

		torrentList = new JTable(new TorrentTableModel(torrents));
		torrentList.setFillsViewportHeight(true);
		menubar = new MenubarPanel(this, torrentManager, trackerManager);

		add(menubar, BorderLayout.NORTH);
		add(new JScrollPane(torrentList), BorderLayout.CENTER);
		add(details, BorderLayout.SOUTH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		updateTimer = new Timer(1000, this);
		updateTimer.start();
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
