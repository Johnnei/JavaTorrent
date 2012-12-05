package torrent.util;

public class HeapSort {
	
	private Heap heap;
	private ISortable[] items;
	
	public HeapSort(Heap heap) {
		this.heap = heap;
	}
	
	public void sort() {
		items = new ISortable[heap.size()];
		int index = 0;
		while(heap.size() > 0) {
			items[index++] = heap.remove();
		}
	}
	
	public ISortable[] getItems() {
		return items;
	}
}
