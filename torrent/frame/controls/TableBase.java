package torrent.frame.controls;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Observable;

import javax.swing.JPanel;

import org.johnnei.utils.JMath;

/**
 * Table base, Note that this is not an actual table.<Br/>
 * This is a base to help you draw a table without any restrictions
 * 
 * @author Johnnei
 * 
 */
public abstract class TableBase extends JPanel implements MouseListener, MouseWheelListener {

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
	/**
	 * The amount of items
	 */
	private int itemCount;
	/**
	 * The amount of visible items
	 */
	private int visibleItemCount;
	/**
	 * Index of the selected row, note that this will not "stick" to the selected row values
	 */
	private int selectedIndex;
	/**
	 * The observable for this object
	 */
	private ObservablePanel observable;

	public TableBase(int rowSize) {
		addMouseWheelListener(this);
		addMouseListener(this);
		observable = new ObservablePanel();
		rowHeight = rowSize;
		selectedIndex = -1;
	}

	public void advanceLine() {
		drawY += rowHeight;
	}

	protected abstract void paintHeader(Graphics g);

	protected abstract void paintData(Graphics g);

	@Override
	public void paintComponent(Graphics g) {
		// Background
		drawY = 0;
		g.setColor(getBackgroundColor());
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(getForegroundColor());

		// Content
		drawY = rowHeight;
		paintData(g);

		// Pre-header
		g.setColor(new Color(0xC8, 0xDD, 0xF2));
		g.fillRect(0, 0, getWidth(), rowHeight);
		g.setColor(getForegroundColor());
		// Header
		paintHeader(g);
		visibleItemCount = (getHeight() - rowHeight) / rowHeight;
		paintScrollbar(g);
	}

	/**
	 * Draw a triangle
	 * 
	 * @param g The Graphics canvas
	 * @param x The bottom left position
	 * @param y The bottom position
	 * @param width The width of the triangle
	 * @param height The height of the triangle
	 */
	private void drawTriangle(Graphics g, int x, int y, int width, int height, boolean facingUp) {
		int topX = x + width / 2;
		Polygon triangle = new Polygon();
		if (facingUp) {
			triangle.addPoint(x, y);
			triangle.addPoint(topX, y - height);
			triangle.addPoint(x + width, y);
		} else {
			triangle.addPoint(x, y - height);
			triangle.addPoint(topX, y);
			triangle.addPoint(x + width, y - height);
		}
		g.fillPolygon(triangle);
	}

	public void paintScrollbar(Graphics g) {
		Color buttonColor = new Color(0x88, 0x88, 0x88);
		Color buttonArrowColor = new Color(0xAA, 0xAA, 0xAA);
		int x = getWidth() - 20;
		if (itemCount > visibleItemCount) {
			// Top Button
			g.setColor(buttonColor);
			g.fillRect(x, 0, 20, 20);
			g.setColor(buttonArrowColor);
			drawTriangle(g, x + 3, 17, 14, 14, true);

			// Bar Background
			g.setColor(new Color(0xDD, 0xDD, 0xDD));
			g.fillRect(x, 20, 20, getHeight() - 40);

			// Slide Button
			int maxSliderSize = getHeight() - 40;
			int hiddenItems = itemCount - visibleItemCount;
			double sliderSize = (double) maxSliderSize / hiddenItems;
			double scrollItemIndex = scrollY + 1D;
			int drawSliderSize = JMath.max((int) sliderSize, 5);
			int scrollBarOffset = 19 + (int) (scrollItemIndex * (sliderSize - ((double) drawSliderSize / hiddenItems)));
			g.setColor(new Color(0xAA, 0xAA, 0xAA));
			g.fillRect(x, scrollBarOffset, 20, drawSliderSize);

			// Bottom Button
			g.setColor(buttonColor);
			g.fillRect(x, getHeight() - 20, 20, 20);
			g.setColor(buttonArrowColor);
			drawTriangle(g, x + 3, getHeight() - 3, 14, 14, false);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int oldScrollY = scrollY;
		scrollY += e.getWheelRotation();
		if (scrollY < 0)
			scrollY = 0;
		int hiddenItemCount = itemCount - visibleItemCount;
		if (hiddenItemCount > 0) {
			if (scrollY > hiddenItemCount)
				scrollY = hiddenItemCount;
		} else {
			scrollY = oldScrollY;
		}
		if (scrollY != oldScrollY)
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
	 * Draws the row with a selected background color
	 * 
	 * @param g The graphics canvas
	 */
	public void drawSelectedBackground(Graphics g) {
		g.setColor(getSelectedBackgroundColor());
		g.fillRect(0, getDrawY(), getWidth(), rowHeight);
		g.setColor(getForegroundColor());
	}

	/**
	 * Checks if the current row is visible after scrolling
	 * 
	 * @return
	 */
	public boolean rowIsVisible() {
		return drawY + (rowHeight / 2) > getScrollY();
	}

	/**
	 * Gets the position to draw at
	 * 
	 * @return
	 */
	public int getDrawY() {
		return drawY - getScrollY();
	}

	/**
	 * Gets the position to drawString at
	 * 
	 * @return
	 */
	public int getTextY() {
		return getDrawY() + 4 + (rowHeight / 2);
	}

	/**
	 * Gets the header text height for drawString
	 * 
	 * @return
	 */
	public int getHeaderTextY() {
		return (rowHeight / 2);
	}

	/**
	 * Gets the current Scroll Y offset
	 * 
	 * @return
	 */
	public int getScrollY() {
		return scrollY * rowHeight;
	}

	/**
	 * The selected row
	 * 
	 * @return
	 */
	public int getSelectedIndex() {
		return selectedIndex;
	}

	/**
	 * Sets the amount of items in the table
	 * 
	 * @param i Count
	 */
	public void setItemCount(int i) {
		itemCount = i;
		if (selectedIndex > itemCount)
			selectedIndex = itemCount;
	}

	/**
	 * Gets the observable object for this table
	 * 
	 * @return
	 */
	public Observable getObservable() {
		return observable;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		Rectangle scrollUpHitbox = new Rectangle(getWidth() - 20, 0, 20, 20);
		Rectangle scrollDownHitbox = new Rectangle(getWidth() - 20, getHeight() - 20, 20, 20);
		if (scrollUpHitbox.contains(e.getPoint())) {
			mouseWheelMoved(new MouseWheelEvent(this, 0, System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false, 0, 0, -1));
		} else if (scrollDownHitbox.contains(e.getPoint())) {
			mouseWheelMoved(new MouseWheelEvent(this, 0, System.currentTimeMillis(), 0, e.getX(), e.getY(), 0, false, 0, 0, 1));
		} else if (e.getY() > rowHeight) {
			int rowY = (scrollY * rowHeight) + e.getY() - rowHeight;
			int oldSelectedIndex = selectedIndex;
			selectedIndex = (rowY / rowHeight);
			if (selectedIndex > itemCount)
				selectedIndex = 0;
			if (selectedIndex != oldSelectedIndex) {
				repaint();
				observable.notifyObservers(selectedIndex);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}
}
