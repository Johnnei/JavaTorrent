package torrent.frame;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import torrent.download.FileInfo;
import torrent.download.Torrent;

public class TabFiles extends JPanel {

	public static final long serialVersionUID = 1L;

	private final Object[] HEADER = { "Filename", "Size", "Pieces", "Pieces have" };
	private JTable table;
	private DefaultTableModel tableModel;
	private JScrollPane scrollPane;
	private Torrent torrent;

	public TabFiles() {
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
		if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
			return;
		FileInfo[] f = torrent.getTorrentFiles().getFiles();
		Object[][] data = new Object[f.length][HEADER.length];
		for (int i = 0; i < f.length; i++) {
			data[i][0] = f[i].getFilename();
			data[i][1] = f[i].getSize();
			data[i][2] = "" + (int) Math.ceil(f[i].getSize() / (double) torrent.getTorrentFiles().getPieceSize());
			data[i][3] = "" + f[i].getPieceHaveCount();
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
