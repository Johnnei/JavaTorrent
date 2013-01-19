package torrent.frame;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.frame.popup.AddTorrentFrame;

public class MenubarPanel extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private JButton addTorrentButton;
	private TorrentFrame owner;
	
	public MenubarPanel(TorrentFrame owner) {
		this.owner = owner;
		setPreferredSize(new Dimension(1280, 40));
		
		addTorrentButton = createButton("Add Torrent");
		
		add(addTorrentButton);
	}
	
	private JButton createButton(String text) {
		JButton button = new JButton(text);
		button.addActionListener(this);
		return button;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource().equals(addTorrentButton)) {
			AddTorrentFrame addTorrent = new AddTorrentFrame(owner);
			if(addTorrent.isOk()) {
				MagnetLink link = addTorrent.getMagnetLink();
				Torrent torrent = link.getTorrent();
				torrent.initialise();
				torrent.start();
				owner.addTorrent(torrent);
			}
		}
	}

}
