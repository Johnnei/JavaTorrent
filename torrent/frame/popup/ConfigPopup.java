package torrent.frame.popup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import torrent.frame.config.TabGeneral;
import torrent.frame.config.TabPeer;

public class ConfigPopup extends JPopup {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JTabbedPane tabControl;

	public ConfigPopup(JFrame owner) {
		super(owner);
		setLayout(new BorderLayout());
		tabControl = new JTabbedPane();
		tabControl.addTab("General", new TabGeneral());
		tabControl.addTab("Peer", new TabPeer());
		add(tabControl, BorderLayout.CENTER);
		setPreferredSize(new Dimension(600, 250));
		setResizable(false);
		setTitle("Change Settings");
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub

	}

}
