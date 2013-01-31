package torrent.frame.config;

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

import org.johnnei.utils.config.Config;
import org.johnnei.utils.config.DefaultConfig;

public class TabGeneral extends JPanel implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField downloadFolder;
	private JTextField portTextField;
	private JButton chooseFolderButton;
	private JButton portButton;

	public TabGeneral() {
		downloadFolder = new JTextField(Config.getConfig().getString("download-output_folder", DefaultConfig.DOWNLOAD_OUTPUT_FOLDER));
		downloadFolder.setPreferredSize(new Dimension(350, 24));
		downloadFolder.setEditable(false);
		chooseFolderButton = new JButton("Change");
		chooseFolderButton.addActionListener(this);
		
		portTextField = createTextfield("" + Config.getConfig().getInt("download-port", DefaultConfig.DOWNLOAD_PORT));
		portButton = createButton();

		addConfig("Download Folder", downloadFolder, chooseFolderButton);
		addConfig("Connection Port", portTextField, portButton);
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
			JFileChooser fc = new JFileChooser(new File(Config.getConfig().getString("download-output_folder", DefaultConfig.DOWNLOAD_OUTPUT_FOLDER)));
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (fc.showDialog(this, "Choose directory") == JFileChooser.APPROVE_OPTION) {
				File newFolder = fc.getSelectedFile();
				Config.getConfig().set("download-output_folder", newFolder.getAbsolutePath() + "\\");
				downloadFolder.setText(Config.getConfig().getString("download-output_folder", DefaultConfig.DOWNLOAD_OUTPUT_FOLDER));
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
		}
	}

}
