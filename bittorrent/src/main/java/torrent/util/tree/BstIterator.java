package torrent.util.tree;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class BstIterator<T extends Comparable<T>> implements Iterator<T> {

	private int arrayPointer;
	private Object[] array;
	
	public BstIterator(int size, TreeNode<T> root) {
		array = new Object[size];
		addToArray(root);
		arrayPointer = -1;
	}
	
	private void addToArray(TreeNode<T> node) {
		if(node != null) {
			addToArray(node.getLeftNode());
			array[arrayPointer++] = node.getValue();
			addToArray(node.getRightNode());
		}
	}
	
	@Override
	public boolean hasNext() {
		return (arrayPointer + 1) < array.length;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T next() {
		arrayPointer++;
		if(arrayPointer >= array.length)
			throw new NoSuchElementException("There are no more items");
		return (T)array[arrayPointer];
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("BSTIterator does not allow removal");
	}

	

}
