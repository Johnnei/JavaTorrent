package org.johnnei.javatorrent.torrent.util.tree;

import java.util.Iterator;

import org.johnnei.javatorrent.utils.JMath;


/**
 * A BinarySearchTree which expect small classes to identify each object in the tree<br/>
 * The Tree is based on an AVL tree so the find operations should be guaranteed O(log n)
 * @author Johnnei
 *
 * @param <T> The type of data to store. Must implement {@link Comparable}
 */
public class BinarySearchTree<T extends Comparable<T>> implements Iterable<T> {
	
	/**
	 * The total size of the tree
	 */
	private TreeNode<T> root;
	
	/**
	 * Adds the item to the tree
	 * @param data The item to add
	 */
	public void add(T data) {
		if(root == null)
			root = new TreeNode<T>(data);
		else
			root.add(data);
		checkAVL();
	}
	
	/**
	 * Removes the item from the tree
	 * @param target The item-blueprint to remove
	 * @return The removed item or <tt>null</tt> if not removed
	 */
	public T remove(T target) {
		if(root == null) {
			return null;
		} else {
			T removedItem = root.remove(target);
			if(removedItem != null && root.getValue().equals(removedItem)) {
				if(root.isLeaf()) {
					root = null;
				} else if(root.getChildCount() == 1) {
					root = root.getMaxChild();
				} else {
					if(root.getRightNode().isLeaf()) {
						root.setValue(root.getRightNode().getValue());
						root.setRightTree(null);
					} else {
						TreeNode<T> prevNode = root;
						TreeNode<T> lastNode = root.getRightNode();
						while(lastNode.getLeftNode() != null) {
							prevNode = lastNode;
							lastNode = lastNode.getLeftNode();
						}
						root.setValue(lastNode.getValue());
						prevNode.setLeftTree(lastNode.getRightNode());
					}
				}
			}
			if(root != null && !root.isLeaf()) {
				checkAVL();
			}
			return removedItem;
		}
	}
	
	/**
	 * Checks if the tree still has the AVL Property
	 */
	private void checkAVL() {
		if(root != null) {
			root.fixAVL();
		}
	}
	
	public int getHeight() {
		if(root != null)
			return root.getHeight();
		return 0;
	}
	
	public int getSize() {
		if(root != null) {
			return root.getSize();
		} else
			return 0;
	}
	
	/**
	 * Finds the item in the tree<br/>
	 * Due to the AVL-Tree Implementation this operation will be O(log n) where n is the size of the tree
	 * @param target The blueprint-item to find
	 * @return The actual item
	 */
	public T find(T target) {
		if(root == null)
			return null;
		if(root.getValue().compareTo(target) == 0) {
			return root.getValue();
		} else {
			return root.find(target);
		}
	}
	
	public void printInOrder() {
		System.out.println("-START OF TREE-");
		if(root != null) {
			root.printInOrder(0);
		}
		System.out.println("--END OF TREE--");
	}
	
	public boolean isAVL() {
		if(root == null)
			return true;
		int leftHeight = root.getLeftNodeHeight();
		int rightHeight = root.getRightNodeHeight();
		return JMath.diff(leftHeight, rightHeight) < 2;
	}

	public boolean isEmpty() {
		return root == null;
	}
	
	TreeNode<T> getRoot() {
		return root;
	}

	@Override
	public Iterator<T> iterator() {
		return new BstIterator<T>(getSize(), root);
	}

}
