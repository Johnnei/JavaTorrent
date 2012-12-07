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
	/**
	 * Current Draw Y to draw at
	 */
	private int drawY;
	/**
	 * Scroll Offset
	 */
	private int scrollY;
	/**
	 * The size of a single row
	 */
	private int rowHeight;
	
	public TableBase(int rowSize) {
		addMouseWheelListener(this);
		rowHeight = rowSize;
	}
	
	public void advanceLine() {
		drawY += rowHeight;
	}
	
	protected abstract void paintHeader(Graphics g);
	protected abstract void paintData(Graphics g);
	
	@Override
	public void paintComponent(Graphics g) {
		//Background
		drawY = 0;
		g.setColor(getBackgroundColor());
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(getForegroundColor());
		
		//Content
		drawY = rowHeight;
		paintData(g);
		
		//Pre-header
		g.setColor(new Color(0xC8, 0xDD, 0xF2));
		g.fillRect(0, 0, getWidth(), rowHeight);
		g.setColor(getForegroundColor());
		//Header
		paintHeader(g);
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		scrollY += e.getWheelRotation();
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
	
	/**
	 * Checks if the current row is visible after scrolling
	 * @return
	 */
	public boolean rowIsVisible() {
		return drawY + (rowHeight / 2) > getScrollY();
	}
	
	/**
	 * Gets the position to draw at
	 * @return
	 */
	public int getDrawY() {
		return drawY - getScrollY() + 3;
	}
	
	/**
	 * Gets the position to drawString at
	 * @return
	 */
	public int getTextY() {
		return getDrawY() + (rowHeight / 2);
	}

	/**
	 * Gets the current Scroll Y offset
	 * @return
	 */
	public int getScrollY() {
		return scrollY * rowHeight;
	}
}
