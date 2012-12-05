package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JPanel;

public abstract class TableBase extends JPanel implements MouseWheelListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int scrollY;
	
	public TableBase() {
		addMouseWheelListener(this);
		scrollY = 0;
	}
	
	protected abstract void paintHeader(Graphics g);
	protected abstract void paintData(Graphics g);
	
	@Override
	public void paintComponent(Graphics g) {
		//Background
		g.setColor(getBackgroundColor());
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(getForegroundColor());
		
		//Content
		paintData(g);
		
		//Pre-header
		g.setColor(new Color(0xC8, 0xDD, 0xF2));
		g.fillRect(0, 0, getWidth(), 20);
		g.setColor(getForegroundColor());
		//Header
		paintHeader(g);
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		scrollY += e.getWheelRotation() * 20;
		if (scrollY < 0)
			scrollY = 0;
		repaint();
	}
	
	public Color getForegroundColor() {
		return Color.BLACK;
	}

	public Color getBackgroundColor() {
		return Color.WHITE;
	}

	public Color getSelectedBackgroundColor() {
		return new Color(0x00, 0xC0, 0xFF);
	}

	public int getScrollY() {
		return scrollY;
	}
}
