package torrent.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.Timer;

import torrent.JavaTorrent;
import torrent.TorrentManager;
import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;

public class TorrentFrame extends JFrame implements Observer, ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TorrentDetails details;
	private TorrentList torrentList;
	private MenubarPanel menubar;
	private ArrayList<Torrent> torrents;
	private Timer updateTimer;

	public TorrentFrame(TorrentManager torrentManager, TrackerManager trackerManager) {
		setPreferredSize(new Dimension(1280, 720));
		setLayout(new BorderLayout());
		setTitle(JavaTorrent.BUILD);
		pack();
		setLocationRelativeTo(null);
		setPreferredSize(new Dimension(getWidth() + getInsets().left + getInsets().right, getHeight() + getInsets().top + getInsets().bottom));

		details = new TorrentDetails(trackerManager);
		details.setPreferredSize(new Dimension(getWidth(), 350));

		torrentList = new TorrentList();
		torrentList.getObservable().addObserver(this);
		menubar = new MenubarPanel(this, torrentManager);

		add(menubar, BorderLayout.NORTH);
		add(torrentList, BorderLayout.CENTER);
		add(details, BorderLayout.SOUTH);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		torrents = new ArrayList<Torrent>();
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
		torrentList.add(torrent);
	}

	public void changeSelectedTorrent(int index) {
		details.setTorrent(torrents.get(index));
	}

	@Override
	public void update(Observable o, Object arg) {
		changeSelectedTorrent((int) arg);
		repaint();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(updateTimer)) {
			updateData();
			repaint();
		}
	}

}
