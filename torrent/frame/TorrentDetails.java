package torrent.frame;

import java.awt.event.KeyEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import torrent.download.tracker.TrackerManager;
import torrent.frame.table.PeerTableModel;
import torrent.frame.table.TrackerTableModel;

public class TorrentDetails extends JTabbedPane {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private TabGeneral tabGeneral;
	private TabFiles tabFiles;
	private TabPieces tabPieces;

	public TorrentDetails(TorrentFrame frame, TrackerManager manager) {
		tabGeneral = new TabGeneral(frame, manager);
		tabFiles = new TabFiles(frame);
		tabPieces = new TabPieces(frame);

		addTab("General", tabGeneral);
		addTab("Files", tabFiles);
		
		JTable trackerTable = new JTable(new TrackerTableModel(frame, manager));
		trackerTable.setFillsViewportHeight(true);
		addTab("Trackers", new JScrollPane(trackerTable));
		
		JTable peerTable = new JTable(new PeerTableModel(frame));
		peerTable.setFillsViewportHeight(true);
		addTab("Peers", new JScrollPane(peerTable));
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
