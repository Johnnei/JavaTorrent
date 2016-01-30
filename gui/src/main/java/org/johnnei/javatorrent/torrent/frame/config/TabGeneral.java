package org.johnnei.javatorrent.torrent.frame.config;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.johnnei.javatorrent.utils.config.Config;

public class TabGeneral extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField downloadFolder;
	private JTextField portTextField;
	private JTextField showPendingPeers;
	private JButton chooseFolderButton;
	private JButton portButton;
	private JButton pendingPeerBtn;

	public TabGeneral() {
		downloadFolder = new JTextField(Config.getConfig().getString("download-output_folder"));
		downloadFolder.setPreferredSize(new Dimension(350, 24));
		downloadFolder.setEditable(false);
		chooseFolderButton = new JButton("Change");
		chooseFolderButton.addActionListener(this);
		
		portTextField = createTextfield("" + Config.getConfig().getInt("download-port"));
		portButton = createButton();
		
		showPendingPeers = createTextfield("" + Config.getConfig().getBoolean("general-show_all_peers"));
		pendingPeerBtn = createButton();

		addConfig("Download Folder", downloadFolder, chooseFolderButton);
		addConfig("Connection Port", portTextField, portButton);
		addConfig("Show Pending Handshake Peers", showPendingPeers, pendingPeerBtn);
	}
	
	private void addConfig(String labelText, JTextField textField, JButton changeButton) {
		add(new JLabel(labelText));
		textField.setPreferredSize(new Dimension(450 - (labelText.length() * 5), 24));
		add(textField);
		changeButton.setToolTipText(labelText);
		add(changeButton);
	}

	private JTextField createTextfield(String text) {
		JTextField textField = new JTextField(text);
		textField.setEditable(false);
		return textField;
	}

	private JButton createButton() {
		JButton button = new JButton("Change");
		button.addActionListener(this);
		return button;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(chooseFolderButton)) {
			JFileChooser fc = new JFileChooser(new File(Config.getConfig().getString("download-output_folder")));
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showDialog(this, "Choose directory") == JFileChooser.APPROVE_OPTION) {
				File newFolder = fc.getSelectedFile();
				Config.getConfig().set("download-output_folder", newFolder.getAbsolutePath() + "\\");
				downloadFolder.setText(Config.getConfig().getString("download-output_folder"));
				repaint();
			}
		} else if(e.getSource().equals(portButton)) {
			String newValue = JOptionPane.showInputDialog(this, "Which port do you want to use to download on?");
			if (newValue == null)
				return;
			if (Config.isInt(newValue)) {
				int port = Integer.parseInt(newValue);
				if(port < 1025 || port > 65535) {
					JOptionPane.showMessageDialog(this, "The given port is not valid. (Has to be between 1025 and 65535)", "Error", JOptionPane.ERROR_MESSAGE);
				} else {
					Config.getConfig().set("download-port", port);
					portTextField.setText("" + port);
				}
			} else {
				JOptionPane.showMessageDialog(this, "The given port is not valid. (Has to be between 1025 and 65535)", "Error", JOptionPane.ERROR_MESSAGE);
			}
		} else if(e.getSource().equals(pendingPeerBtn)) {
			int newValue = JOptionPane.showConfirmDialog(this, "Show peers which have not yet passed handshake?", "Show all peers", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
			boolean b = newValue == 0;
			Config.getConfig().set("general-show_all_peers", b);
			showPendingPeers.setText("" + Config.getConfig().getBoolean("general-show_all_peers"));
		}
	}

}
