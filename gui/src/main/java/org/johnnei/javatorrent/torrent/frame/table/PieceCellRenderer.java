package org.johnnei.javatorrent.torrent.frame.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.johnnei.javatorrent.torrent.download.files.Piece;

public class PieceCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;
	
	private Piece piece;
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		piece = (Piece) value;
		
		return this;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		float pixelsPerBlock = (float) getWidth() / piece.getBlockCount();
		
		boolean isDone = piece.isDone(0);
		boolean isRequested = piece.isRequested(0);
		boolean render = false;
		int startIndex = 0;
		
		for (int i = 0; i < piece.getBlockCount(); i++) {
			render = piece.isDone(i) != isDone || piece.isRequested(i) != isRequested;
			
			if (render || i + 1 >= piece.getBlockCount()) {
				if (isDone) {
					g.setColor(Color.GREEN);
				} else if (isRequested) {
					g.setColor(Color.ORANGE);
				} else {
					g.setColor(Color.RED);
				}
				
				int blockCount = 1 + i - startIndex;
				g.fillRect((int) (startIndex * pixelsPerBlock), 0, (int) (blockCount * pixelsPerBlock), getHeight());
				
				startIndex = i;
				isDone = piece.isDone(i);
				isRequested = piece.isRequested(i);
				render = false;
			}
		}
	}

}
