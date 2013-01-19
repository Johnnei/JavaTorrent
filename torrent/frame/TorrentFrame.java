package torrent.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;

import torrent.JavaTorrent;
import torrent.download.Torrent;

public class TorrentFrame extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TorrentDetails details;
	private TorrentList torrentList;
	private MenubarPanel menubar;
	private ArrayList<Torrent> torrents;

	public TorrentFrame() {
		setPreferredSize(new Dimension(1280, 720));
		setLayout(new BorderLayout());
		setTitle(JavaTorrent.BUILD);
		pack();
		setLocationRelativeTo(null);
		setPreferredSize(new Dimension(getWidth() + getInsets().left + getInsets().right, getHeight() + getInsets().top + getInsets().bottom));

		details = new TorrentDetails();
		details.setPreferredSize(new Dimension(getWidth(), 400));

		torrentList = new TorrentList();
		menubar = new MenubarPanel(this);

		add(menubar, BorderLayout.NORTH);
		add(torrentList, BorderLayout.CENTER);
		add(details, BorderLayout.SOUTH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setVisible(true);
		torrents = new ArrayList<Torrent>();
	}

	public void updateData() {
		for (int i = 0; i < torrents.size(); i++) {
			torrents.get(i).pollRates();
		}
		details.updateData();
	}

	public void addTorrent(Torrent torrent) {
		torrents.add(torrent);
		torrentList.add(torrent);
		changeSelectedTorrent(torrents.size() - 1);
	}

	public void changeSelectedTorrent(int index) {
		torrentList.setSelectedIndex(index);
		details.setTorrent(torrents.get(index));
	}
	
	public void repaint() {
		super.repaint();
	}

}
