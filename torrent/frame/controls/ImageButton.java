package torrent.frame.controls;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;

public class ImageButton extends JButton {

	/**
	 * The image to be displayed
	 */
	private BufferedImage image;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ImageButton(String text, String file) throws IOException {
		setText(text);
		setContentAreaFilled(false);
		File imageFile = new File(file);
		if (!imageFile.exists()) {
			imageFile = new File("src/" + file);
		}
		image = ImageIO.read(imageFile);
	}

	@Override
	public void paintComponent(Graphics g) {
		g.drawImage(image, 0, 2, 48, 48, 0, 0, image.getWidth(), image.getHeight(), null);
		g.drawString(getText(), 50, 30);
	}

}
