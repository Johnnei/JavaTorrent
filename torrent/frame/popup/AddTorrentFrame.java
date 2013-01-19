package torrent.frame.popup;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import torrent.download.MagnetLink;

public class AddTorrentFrame extends JPopup {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea magnetLink;
	private JButton okButton;
	
	public AddTorrentFrame(JFrame owner) {
		super(owner);
		magnetLink = new JTextArea();
		magnetLink.setPreferredSize(new Dimension(600, 24));
		okButton = createButton("Add");
		setTitle("Add torrent");
		add(new JLabel("Magnet link: "));
		add(magnetLink);
		add(okButton);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(okButton)) {
			MagnetLink mlink = new MagnetLink(magnetLink.getText());
			if(mlink.isDownloadable()) {
				setOk(true);
				setVisible(false);
			} else {
				JOptionPane.showMessageDialog(this, "The magnet link does not contain enough information to start downloading\nSorry about that.");
			}
		}
	}
	
	public MagnetLink getMagnetLink() {
		return new MagnetLink(magnetLink.getText());
	}

}
