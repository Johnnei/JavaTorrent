package torrent.frame.table;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public abstract class GenericTableModel<T> extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	
	private List<T> items;
	
	private String[] headers;

	public GenericTableModel(List<T> items, String[] headers) {
		this.items = items;
		this.headers = headers;
	}
	
	protected abstract Object getValueForColumn(T item, int columnIndex);
	
	@Override
	public int getRowCount() {
		return items.size();
	}

	@Override
	public int getColumnCount() {
		return headers.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return headers[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		T item = items.get(rowIndex);
		return getValueForColumn(item, columnIndex);
	}

}
