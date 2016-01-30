package org.johnnei.javatorrent.torrent.frame.config;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.johnnei.javatorrent.utils.config.Config;

public class TabPeer extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JTextField maxPeer;
	private JTextField maxPeerBurstRatio;
	private JTextField maxConcurrentConnecting;
	private JTextField maxConnecting;
	private JButton maxPeerButton;
	private JButton maxPeerBurstRatioButton;
	private JButton maxConcurrentConnectingButton;
	private JButton maxConnectingButton;

	public TabPeer() {
		maxPeer = createTextfield("" + Config.getConfig().getInt("peer-max"));
		maxPeerBurstRatio = createTextfield("" + Config.getConfig().getFloat("peer-max_burst_ratio"));
		maxConcurrentConnecting = createTextfield("" + Config.getConfig().getInt("peer-max_concurrent_connecting"));
		maxConnecting = createTextfield("" + Config.getConfig().getInt("peer-max_connecting"));
		maxPeerButton = createButton();
		maxPeerBurstRatioButton = createButton();
		maxConcurrentConnectingButton = createButton();
		maxConnectingButton = createButton();

		addConfig("Max peers per torrent", maxPeer, maxPeerButton);
		addConfig("Max peers burst ratio", maxPeerBurstRatio, maxPeerBurstRatioButton);
		addConfig("Max concurrent connecting peers", maxConcurrentConnecting, maxConcurrentConnectingButton);
		addConfig("Max peer connecting queue", maxConnecting, maxConnectingButton);
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
		JButton button = (JButton) e.getSource();
		String newValue = JOptionPane.showInputDialog(this, button.getToolTipText());
		if (newValue == null)
			return;
		String key = "";
		String error = null;
		if (button.equals(maxPeerButton)) {
			key = "peer-max";
			if(Config.isInt(newValue)) {
				int i = Integer.parseInt(newValue);
				if(i < 10) {
					error = "This has to be a number of atleast 10";
				} else {
					maxPeer.setText(newValue);
				}
			} else {
				error = "This has to be a number of atleast 10";
			}
		} else if (button.equals(maxPeerBurstRatioButton)) {
			key = "peer-max-burst-ratio";
			if(Config.isFloat(newValue)) {
				float f = Float.parseFloat(newValue);
				if(f < 1 || f > 2) {
					error = "This has to be a number between 1.0 and 2.0 (including)";
				} else {
					maxPeerBurstRatio.setText(newValue);
				}
			} else {
				error = "This has to be a number between 1.0 and 2.0 (including)";
			}
		} else if (button.equals(maxConcurrentConnectingButton)) {
			key = "peer-max-concurrent-connecting";
			if(Config.isInt(newValue)) {
				int i = Integer.parseInt(newValue);
				if(i < 1 || i > 10) {
					error = "This has to be a number between 1 and 10 (including).";
				} else {
					maxConcurrentConnecting.setText(newValue);
				}
			} else {
				error = "This has to be a number between 1 and 10 (including).";
			}
		} else if (button.equals(maxConnectingButton)) {
			key = "peer-max_connecting";
			if(Config.isInt(newValue)) {
				int i = Integer.parseInt(newValue);
				if(i < 1 || i > 200) {
					error = "This has to be a number between 1 and 200 (including).";
				}
				maxConnecting.setText(newValue);
			} else {
				error = "This has to be a number between 1 and 200 (including).";
			}
		} else {
			error = "Unkown button";
		}
		if (error != null) {
			JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
		} else {
			Config.getConfig().set(key, newValue);
		}
	}

}
