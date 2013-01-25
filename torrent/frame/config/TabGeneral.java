package torrent.frame.config;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
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
	private JButton chooseFolderButton;

	public TabGeneral() {
		downloadFolder = new JTextField(Config.getConfig().getString("download-output_folder", DefaultConfig.DOWNLOAD_OUTPUT_FOLDER));
		downloadFolder.setPreferredSize(new Dimension(350, 24));
		downloadFolder.setEditable(false);
		chooseFolderButton = new JButton("Change");
		chooseFolderButton.addActionListener(this);

		add(new JLabel("Download Folder"));
		add(downloadFolder);
		add(chooseFolderButton);
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
		}
	}

}
