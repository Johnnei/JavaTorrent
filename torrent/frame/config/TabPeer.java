package torrent.frame.config;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.johnnei.utils.config.Config;
import org.johnnei.utils.config.DefaultConfig;

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
		maxPeer = createTextfield("" + Config.getConfig().getInt("peer-max", DefaultConfig.PEER_MAX));
		maxPeerBurstRatio = createTextfield("" + Config.getConfig().getFloat("peer-max_burst_ratio", DefaultConfig.PEER_BURST_RATIO));
		maxConcurrentConnecting = createTextfield("" +Config.getConfig().getInt("peer-max_concurrent_connecting", DefaultConfig.PEER_MAX_CONCURRENT_CONNECTING));
		maxConnecting = createTextfield("" +Config.getConfig().getInt("peer-max_connecting", DefaultConfig.PEER_MAX_CONNECTING));
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
		JButton button = (JButton)e.getSource();
		String newValue = JOptionPane.showInputDialog(this, button.getToolTipText());
		if(newValue == null)
			return;
		String key = "";
		boolean error = false;
		if(button.equals(maxPeerButton)) {
			key = "peer-max";
			error = !Config.isInt(newValue);
			if(!error) {
				maxPeer.setText(newValue);
			}
		} else if(button.equals(maxPeerBurstRatioButton)) {
			key = "peer-max-burst-ratio";
			error = !Config.isFloat(newValue);
			if(!error) {
				maxPeerBurstRatio.setText(newValue);
			}
		} else if(button.equals(maxConcurrentConnectingButton)) {
			key = "peer-max-concurrent-connecting";
			error = !Config.isInt(newValue);
			if(!error) {
				maxConcurrentConnecting.setText(newValue);
			}
		} else if(button.equals(maxConnectingButton)) {
			key = "peer-max_connecting";
			error = !Config.isInt(newValue);
			if(!error) {
				maxConnecting.setText(newValue);
			}
		} else {
			error = true;
		}
		if(error) {
			JOptionPane.showMessageDialog(this, "\"" + newValue + "\" is not a valid value for " +  button.getToolTipText(), "Error", JOptionPane.ERROR_MESSAGE);
		} else {
			Config.getConfig().set(key, newValue);
		}
	}

}
