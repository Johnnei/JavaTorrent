package torrent.util;

public class Heap {
	
	private ISortable[] items;
	
	/**
	 * Creates a sorted heap with the given size
	 * @param size
	 * The initial size
	 */
	public Heap(int size) {
		items = new ISortable[size];
	}
	
	/**
	 * Creates a sorted heap with the default size of 10
	 */
	public Heap() {
		this(10);
	}
	
	/**
	 * Adds an element to the heap
	 * @param sortable
	 * The element to add
	 */
	public void add(ISortable sortable) {
		int index = getFreeSpot();
		items[index] = sortable;
		while(get(index) > getParentNode(index)) {
			int newIndex = getParentNodeIndex(index);
			swap(index, getParentNodeIndex(index));
			index = newIndex;
		}
	}
	
	/**
	 * Grabs the largest element from the heap
	 * @return
	 * The largest element from the heap
	 */
	public ISortable remove() {
		//Remove Element
		ISortable removedItem = items[0];
		items[0] = null;
		//Swap last element to top and sort it
		if(getLastItemIndex() == -1)
			return removedItem;
		swap(0, getLastItemIndex());
		int index = 0;
		while(get(index) < getLeftChildNode(index) || get(index) < getRightChildNode(index)) {
			if(getLeftChildNode(index) > getRightChildNode(index)) {
				index = getLeftChildNodeIndex(index);
			} else {
				index = getRightChildNodeIndex(index);
			}
			swap(index, getParentNodeIndex(index));
		}
		return removedItem;
	}
	
	private int getLastItemIndex() {
		for(int i = items.length - 1; i >= 0; i--) {
			if(items[i] != null)
				return i;
		}
		return -1;
	}
	
	public int getParentNode(int index) {
		return get(getParentNodeIndex(index));
	}
	
	public int getParentNodeIndex(int index) {
		return (index - 1) / 2;
	}
	
	public int getLeftChildNode(int index) {
		return get(getLeftChildNodeIndex(index));
	}
	
	public int getLeftChildNodeIndex(int index) {
		return 2 * index + 1;
	}
	
	public int getRightChildNode(int index) {
		return get(getRightChildNodeIndex(index));
	}
	
	public int getRightChildNodeIndex(int index) {
		return 2 * index + 2;
	}
	
	/**
	 * Gets the next free spot index
	 * Expands the array if needed
	 * @return
	 * The free spot index
	 */
	private int getFreeSpot() {
		for(int i = 0; i < items.length; i++) {
			if(items[i] == null)
				return i;
		}
		int result = items.length;
		expand();
		return result;
	}
	
	private void expand() {
		ISortable[] newItems = new ISortable[items.length + 10];
		for(int i = 0; i < items.length; i++) {
			newItems[i] = items[i];
		}
		items = newItems;
	}
	
	/**
	 * Swaps the two values at index a and b
	 * @param indexA
	 * @param indexB
	 */
	private void swap(int indexA, int indexB) {
		ISortable temp = items[indexA];
		items[indexA] = items[indexB];
		items[indexB] = temp;
	}
	
	/**
	 * Gets an element at position index.
	 * Checks for nullpointer errors
	 * @param index
	 * @return
	 * The value of the item at the index or Integer.MIN_VALUE if null
	 */
	private int get(int index) {
		ISortable item = getItem(index);
		if(item == null)
			return Integer.MIN_VALUE;
		else
			return item.getValue();
	}
	
	private ISortable getItem(int index) {
		if(index < 0 || index >= items.length)
			return null;
		if(items[index] == null)
			return null;
		return items[index];
	}

	public int size() {
		return getLastItemIndex() + 1;
	}

}
