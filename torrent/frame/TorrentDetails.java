package torrent.frame;

import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import torrent.download.tracker.TrackerManager;

public class TorrentDetails extends JTabbedPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TabGeneral tabGeneral;
	private TabFiles tabFiles;
	private TabTracker tabTracker;
	private TabPieces tabPieces;
	private TabPeers tabPeers;

	public TorrentDetails(TorrentFrame frame, TrackerManager manager) {
		tabGeneral = new TabGeneral(frame, manager);
		tabFiles = new TabFiles(frame);
		tabTracker = new TabTracker(frame, manager);
		tabPieces = new TabPieces(frame);
		tabPeers = new TabPeers(frame);

		addTab("General", tabGeneral);
		addTab("Files", tabFiles);
		addTab("Trackers", tabTracker);
		addTab("Peers", tabPeers);
		addTab("Pieces", tabPieces);
		addTab("Log", new JPanel());

		// Hotkeys
		setMnemonicAt(0, KeyEvent.VK_1);
		setMnemonicAt(1, KeyEvent.VK_2);
		setMnemonicAt(2, KeyEvent.VK_3);
		setMnemonicAt(3, KeyEvent.VK_4);
		setMnemonicAt(4, KeyEvent.VK_5);
		setMnemonicAt(5, KeyEvent.VK_6);
	}
}
