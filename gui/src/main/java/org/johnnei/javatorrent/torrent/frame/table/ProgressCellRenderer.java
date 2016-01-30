package org.johnnei.javatorrent.torrent.frame.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ProgressCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;
	
	private double progress = 0;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		progress = (double) value;
		
		setText(String.format("%.1f%%", progress));
		setHorizontalAlignment(CENTER);
		
		return this;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		int progressWidth = (int) ((progress / 100) * getWidth());
		
		g.setColor(Color.GREEN);
		g.fillRect(0, 0, progressWidth, getHeight());
		
		super.paintComponent(g);
	}

}
