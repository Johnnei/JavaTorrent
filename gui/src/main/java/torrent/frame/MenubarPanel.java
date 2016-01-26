package torrent.frame;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.johnnei.javatorrent.TorrentClient;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.frame.controls.ImageButton;
import torrent.frame.popup.AddTorrentFrame;
import torrent.frame.popup.ConfigPopup;

public class MenubarPanel extends JPanel implements ActionListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private JButton addTorrentButton;
	private JButton configButton;

	private final TorrentFrame owner;

	private final TorrentClient torrentClient;

	public MenubarPanel(TorrentFrame owner, TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		this.owner = owner;
		setPreferredSize(new Dimension(1280, 40));
		setLayout(null);
		try {
			addTorrentButton = createButton(5, 1, 125, "Add Torrent", "/download.png");
			configButton = createButton(135, 1, 150, "Change Settings", "/settings.png");
		} catch (IOException e) {
			e.printStackTrace();
		}

		add(addTorrentButton);
		add(configButton);
	}

	private JButton createButton(int x, int y, int width, String text, String img) throws IOException {
		return createButton(x, y, width, 38, text, img);
	}

	private JButton createButton(int x, int y, int width, int height, String text, String img) throws IOException {
		ImageButton button = new ImageButton(text, img);
		button.addActionListener(this);
		button.setBounds(x, y, width, height);
		return button;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(addTorrentButton)) {
			AddTorrentFrame addTorrent = new AddTorrentFrame(owner, torrentClient);
			if (addTorrent.isOk()) {
				MagnetLink link = addTorrent.getMagnetLink();
				Torrent torrent = link.getTorrent();
				torrent.start();
				owner.addTorrent(torrent);
			}
		} else if (e.getSource().equals(configButton)) {
			new ConfigPopup(owner);
		}
	}
}
