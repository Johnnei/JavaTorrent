package unittesting;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import torrent.util.tree.BinarySearchTree;

public class BST {

	@Test
	public void leftRotation() {
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(12);
		bst.add(16);
		bst.add(8);
		bst.add(4);
		bst.add(14);
		bst.add(10);
		bst.add(2);
		bst.add(6);
		bst.add(1);
		bst.printInOrder();
		assertTrue("AVL Property (Left Rotation)", bst.isAVL());
	}
	@Test
	public void rightRotation() {
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(-12);
		bst.add(-16);
		bst.add(-8);
		bst.add(-4);
		bst.add(-14);
		bst.add(-10);
		bst.add(-2);
		bst.add(-6);
		bst.add(-1);
		bst.printInOrder();
		assertTrue("AVL Property (Right Rotation)", bst.isAVL());
	}
	
	@Test
	public void leftDoubleRotation() {
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(12);
		bst.add(16);
		bst.add(8);
		bst.add(4);
		bst.add(14);
		bst.add(10);
		bst.add(2);
		bst.add(6);
		bst.add(5);
		bst.printInOrder();
		assertTrue("AVL Property (Left Double Rotation)", bst.isAVL());
	}
	@Test
	public void rightDoubleRotation() {
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(-12);
		bst.add(-16);
		bst.add(-8);
		bst.add(-4);
		bst.add(-14);
		bst.add(-10);
		bst.add(-2);
		bst.add(-6);
		bst.add(-5);
		bst.printInOrder();
		assertTrue("AVL Property (Right Double Rotation)", bst.isAVL());
	}

}
