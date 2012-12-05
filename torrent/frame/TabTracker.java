package torrent.frame;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import torrent.download.Torrent;
import torrent.download.tracker.Tracker;

public class TabTracker extends JPanel {

	public static final long serialVersionUID = 1L;

	private final Object[] HEADER = { "Tracker", "Status", "Seeders", "Leechers" };
	private JTable table;
	private DefaultTableModel tableModel;
	private JScrollPane scrollPane;
	private Torrent torrent;

	public TabTracker() {
		tableModel = new DefaultTableModel(new Object[][] {}, HEADER);
		table = new JTable(tableModel);
		table.setFillsViewportHeight(true);
		scrollPane = new JScrollPane(table);
		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	public void updateData() {
		if (torrent == null)
			return;
		Tracker[] trackers = torrent.getTrackers();
		int valid = 0;
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] != null)
				valid++;
		}
		Object[][] data = new Object[valid][HEADER.length];
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] == null)
				continue;
			data[i][0] = trackers[i].getTrackerName();
			data[i][1] = trackers[i].getStatus();
			data[i][2] = trackers[i].getSeeders();
			data[i][3] = trackers[i].getLeechers();
		}
		final Object[][] invokeData = data;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				tableModel.setDataVector(invokeData, HEADER);
			}
		});
	}
}
