package org.johnnei.javatorrent.torrent.frame.popup;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;

public abstract class JPopup extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * True if the user pressed the normal close button
	 */
	private boolean isOk;

	public JPopup(JFrame owner) {
		super(owner, true);
		setLayout(new FlowLayout());
		isOk = false;
	}

	public void setOk(boolean b) {
		isOk = b;
	}

	public boolean isOk() {
		return isOk;
	}

	protected JButton createButton(String text) {
		JButton button = new JButton(text);
		button.addActionListener(this);
		return button;
	}

}
