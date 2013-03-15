package torrent.util.tree;

import org.johnnei.utils.JMath;


/**
 * A BinarySearchTree which expect small classes to identify each object in the tree<br/>
 * The Tree is based on an AVL tree so the find operations should be guaranteed O(log n)
 * @author Johnnei
 *
 * @param <T> The type of data to store. Must implement Comparable
 */
public class BinarySearchTree<T extends Comparable<T>> {
	
	private TreeNode<T> root;
	
	public void add(T data) {
		if(root == null)
			root = new TreeNode<T>(data);
		else
			root.add(data);
		checkAVL();
	}
	
	public T remove(T data) {
		if(root == null) {
			return null;
		} else {
			return root.remove(data).getValue();
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
	
	public T find(T target) {
		if(root.getValue().compareTo(target) == 0) {
			return root.getValue();
		} else {
			return root.find(target);
		}
	}
	
	public void printInOrder() {
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

}
