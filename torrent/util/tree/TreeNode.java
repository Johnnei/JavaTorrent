package torrent.util.tree;

import org.johnnei.utils.JMath;



public class TreeNode<T extends Comparable<T>> {
	
	private T data;
	private TreeNode<T> leftNode;
	private TreeNode<T> rightNode;
	
	public TreeNode(T data) {
		this.data = data;
	}
	
	private TreeNode(T data, TreeNode<T> leftChild, TreeNode<T> rightChild) {
		this.data = data;
		this.leftNode = leftChild;
		this.rightNode = rightChild;
	}
	
	public void add(T data) {
		if(data.compareTo(this.data) <= 0) {
			if(leftNode == null) {
				leftNode = new TreeNode<T>(data);
			} else {
				leftNode.add(data);
			}
		} else {
			if(rightNode == null) {
				rightNode = new TreeNode<T>(data);
			} else {
				rightNode.add(data);
			}
		}
	}
	
	public TreeNode<T> remove(T data) {
		int compareResult = data.compareTo(data);
		if(compareResult == 0) {
			return this;
		} else if(compareResult < 0) {
			if(leftNode != null) {
				return leftNode.remove(data);
			} else {
				return null;
			}
		} else {
			if(rightNode != null) {
				return rightNode.remove(data);
			} else {
				return null;
			}
		}
	}
	
	public T find(T target) {
		int compareResult = data.compareTo(target);
		if(compareResult == 0) {
			return data;
		} else if(compareResult < 0) {
			if(leftNode != null) {
				return leftNode.find(target);
			} else
				return null;
		} else {
			if(rightNode != null) {
				return rightNode.find(target);
			} else
				return null; 
		}
	}
	
	public T getValue() {
		return data;
	}
	
	public int getHeight() {
		int leftResult = 0;
		int rightResult = 0;
		if(leftNode != null) {
			leftResult = leftNode.getHeight();
		}
		if(rightNode != null) {
			rightResult = rightNode.getHeight();
		}
		return 1 + Math.max(leftResult, rightResult);
	}
	
	public boolean fixAVL() {
		int leftNodeHeight = (leftNode != null) ? leftNode.getHeight() : 0;
		int rightNodeHeight = (rightNode != null) ? rightNode.getHeight() : 0;
		if(JMath.diff(leftNodeHeight, rightNodeHeight) > 1) {
			System.out.println("AVL has been broken [" + leftNodeHeight + ", " + rightNodeHeight + "]");
			if(leftNodeHeight > rightNodeHeight) {
				if(!leftNode.fixAVL()) { //We detected it is in the left node, but the leftNode has not found the problem
					System.out.println("Detected at leftNode in data: " + data);
					rotateLeft();
				}
			} else {
				if(!rightNode.fixAVL()) { //Same as leftNode but now for right
					System.out.println("Detected at RightNode in data: " + data);
					rotateRight();
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 *   5  ->   4
	 *  4       3 5
	 * 3 
	 */
	private void rotateLeft() {
		TreeNode<T> leftSubNode = leftNode.getLeftNode();
		rightNode = new TreeNode<T>(getValue(), null, rightNode);
		if(leftNode.getRightNode() != null) {
			rightNode = new TreeNode<T>(leftNode.getRightNode().getValue(), null, rightNode);
		}
		data = leftNode.getValue();
		leftNode = leftSubNode;
	}
	
	/**
	 * 0 ->  6
	 *  6	0 8
	 *   8	
	 */
	private void rotateRight() {
		if(leftNode != null) {
			doubleRotateRight();
		} else {
			TreeNode<T> rightSubNode = rightNode.getRightNode();
			leftNode = new TreeNode<T>(getValue());
			data = rightNode.getValue();
			rightNode = rightSubNode;
		}
	}
	
	private void doubleRotateRight() {
		System.out.println("doubleRotateRight");
	}
	
	private void doubleRotateLeft() {
		System.out.println("doubleRotateLeft");
	}
	
	public TreeNode<T> getLeftNode() {
		return leftNode;
	}
	
	public TreeNode<T> getRightNode() {
		return rightNode;
	}
	
	public int getRightNodeHeight() {
		if(rightNode != null)
			return rightNode.getHeight();
		else
			return 0;
	}
	
	public int getLeftNodeHeight() {
		if(leftNode != null)
			return leftNode.getHeight();
		else
			return 0;
	}
	
	private void write(int padding, String text) {
		for(int i = 0; i < padding; i++) 
			System.out.print("     ");
		System.out.println(text);
	}
	
	public void printInOrder(int depth) {
		if(rightNode != null)
			rightNode.printInOrder(depth + 1);
		else if(leftNode != null)
			write(depth + 1, "NULL");
		write(depth, data.toString());
		if(leftNode != null)
			leftNode.printInOrder(depth + 1);
		else if(rightNode != null)
			write(depth + 1, "NULL");
	}
	
	@Override
	public String toString() {
		if(data != null)
			return "TreeNode<T>(" + data + ")";
		return "TreeNode<T>(null)";
	}

}
