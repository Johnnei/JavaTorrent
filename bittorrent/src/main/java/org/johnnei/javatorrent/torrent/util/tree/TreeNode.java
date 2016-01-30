package org.johnnei.javatorrent.torrent.util.tree;

import org.johnnei.javatorrent.utils.JMath;



public class TreeNode<T extends Comparable<T>> {
	
	public static boolean DEBUG = true;
	
	private T data;
	private TreeNode<T> leftNode;
	private TreeNode<T> rightNode;
	
	public TreeNode(T data) {
		this.data = data;
	}
	
	/**
	 * Used in various operation to modify the tree in a more flexible way
	 * @param data
	 * @param leftChild
	 * @param rightChild
	 */
	TreeNode(T data, TreeNode<T> leftChild, TreeNode<T> rightChild) {
		this.data = data;
		this.leftNode = leftChild;
		this.rightNode = rightChild;
	}
	
	public void add(T data) {
		if(data.compareTo(this.data) <= 0) { //Smaller? Go to left
			if(leftNode == null) {
				leftNode = new TreeNode<T>(data);
			} else {
				leftNode.add(data);
			}
		} else {
			if(rightNode == null) { //Larger? Go to Right
				rightNode = new TreeNode<T>(data);
			} else {
				rightNode.add(data);
			}
		}
	}
	
	public T remove(T target) {
		int compareResult = target.compareTo(data);
		if(compareResult == 0) {
			return data;
		} else if(compareResult < 0) {
			if(leftNode != null) {
				T removedItem = leftNode.remove(target);
				if(removedItem == null)
					return null;
				
				if(leftNode.getValue().equals(target)) {
					//Remove the node from the tree
					if(leftNode.isLeaf()) //We did find it, and only 1 now is down there so we can safely delete that
						leftNode = null;
					else if(leftNode.getChildCount() == 1) {
						//Single Child Disconnect
						leftNode = leftNode.getMaxChild();
					} else {
						//Two Child Disconnect
						TreeNode<T> prevNode = leftNode;
						TreeNode<T> lastNode = leftNode.getLeftNode();
						while(lastNode.getLeftNode() != null) {
							prevNode = lastNode;
							lastNode = lastNode.getLeftNode();
						}
						this.leftNode.data = lastNode.getValue();
						prevNode.leftNode = lastNode.getRightNode();
					}
				}
				
				return removedItem;
			} else
				return null;
		} else {
			if(rightNode != null) {
				T removedItem = rightNode.remove(target);
				if(removedItem == null)
					return null;
				
				if(rightNode.getValue().equals(target)) {
					if(rightNode.isLeaf()) //We did find it, and only 1 now is down there so we can safely delete that
						rightNode = null;
					else if(rightNode.getChildCount() == 1) {
						//Single Child Disconnect
						rightNode = rightNode.getMaxChild();
					} else {
						//Two Child Disconnect
						TreeNode<T> prevNode = rightNode;
						TreeNode<T> lastNode = rightNode.getLeftNode();
						while(lastNode.getLeftNode() != null) {
							prevNode = lastNode;
							lastNode = lastNode.getLeftNode();
						}
						this.rightNode.data = lastNode.getValue();
						prevNode.leftNode = lastNode.getRightNode();
					}
				}
				
				return removedItem;
			} else
				return null; 
		}
	}
	
	public T find(T target) {
		int compareResult = target.compareTo(data);
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
			if(leftNodeHeight > rightNodeHeight) {
				if(!leftNode.fixAVL()) { //We detected it is in the left node, but the leftNode has not found the problem
					rotateLeft();
				}
			} else {
				if(!rightNode.fixAVL()) { //Same as leftNode but now for right
					rotateRight();
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Executes a single or double rotation based on the current tree status
	 */
	private void rotateLeft() {
		TreeNode<T> leftSubRight = leftNode.getRightNode();
		if(leftSubRight == null) {
			rightNode = new TreeNode<>(this.data);
			this.data = leftNode.getValue();
			leftNode = leftNode.getLeftNode();
		}else if(leftNode.getRightNode().isLeaf()) {
			//Single Rotation
			leftNode.rightNode = null;
			rightNode = new TreeNode<>(data, leftSubRight, rightNode);
			this.data = leftNode.getValue();
			leftNode = leftNode.getLeftNode();
		} else {
			//Double Rotation
			rightNode = new TreeNode<T>(data, null, rightNode);
			TreeNode<T> prevNode = leftNode;
			while(prevNode.getRightNode() != null) {
				TreeNode<T> node = prevNode.getRightNode();
				if(node.getRightNode() != null) {
					prevNode = node;
				} else {
					break;
				}
			}
			this.data = prevNode.getRightNode().getValue();
			prevNode.rightNode = prevNode.getRightNode().getLeftNode();
		}
	}
	
	/**
	 * Executes a single or double rotation based on the current tree status
	 */
	private void rotateRight() {
		TreeNode<T> rightSubLeft = rightNode.getLeftNode();
		if(rightSubLeft == null) {
			leftNode = new TreeNode<>(this.data);
			this.data = rightNode.getValue();
			rightNode = rightNode.getRightNode();
		} else if(rightSubLeft.isLeaf()) {
			//Single Rotation
			rightNode.leftNode = null;
			leftNode = new TreeNode<>(data, rightSubLeft, leftNode);
			this.data = rightNode.getValue();
			rightNode = rightNode.getRightNode();
		} else {
			//Double Rotation
			leftNode = new TreeNode<T>(data, leftNode, null);
			TreeNode<T> prevNode = rightNode;
			while(prevNode.getLeftNode() != null) {
				TreeNode<T> node = prevNode.getLeftNode();
				if(node.getLeftNode() != null) {
					prevNode = node;
				} else {
					break;
				}
			}
			this.data = prevNode.getLeftNode().getValue();
			prevNode.leftNode = prevNode.getLeftNode().getLeftNode();
		}
	}
	
	public TreeNode<T> getMaxChild() {
		if(leftNode != null && rightNode != null) {
			if(leftNode.getValue().compareTo(rightNode.getValue()) <= 0) {
				return rightNode;
			} else {
				return leftNode;
			}
		} else if(leftNode != null) {
			return leftNode;
		} else {
			return rightNode;
		}
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
	
	public boolean isLeaf() {
		return leftNode == null && rightNode == null;
	}
	
	public int getChildCount() {
		int count = 0;
		if(leftNode != null) {
			count++;
		}
		if(rightNode != null)
			count++;
		return count;
	}
	
	public int getSize() {
		int size = 1;
		if(leftNode != null)
			size += leftNode.getSize();
		if(rightNode != null)
			size += rightNode.getSize();
		return size;
	}
	
	//Root Removal stuff
	void setValue(T data) {
		this.data = data;
	}
	
	void setRightTree(TreeNode<T> child) {
		this.rightNode = child; 
	}
	
	void setLeftTree(TreeNode<T> child) {
		this.leftNode = child;
	}
	
	//PRINT STUFF
	
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
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof TreeNode<?>) {
			TreeNode<?> node = (TreeNode<?>)o;
			return node.getValue().equals(data);
		} else
			return o.equals(data);
	}

}
