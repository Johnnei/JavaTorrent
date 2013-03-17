package unittesting;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import torrent.util.tree.BinarySearchTree;

public class BST {
	
	@Test
	public void basicBST() {
		System.out.println("Basic Binary Search Tree Test");
		
		String[] removeTypes = new String[] { "BST-Remove 2 Childs",  "BST-Remove Leaf", "BST-Remove 1 Child", "BST-Remove Root 2 Child"};
		int[] removes = new int[] { 5, -1, 4, 15 };
		
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(15);
		bst.add(7);
		bst.add(3);
		bst.add(21);
		bst.add(-1);
		bst.add(5);
		bst.add(4);
		bst.add(6);
		//bst.add(17);
		for(int i = 0; i < removes.length; i++) {
			bst.printInOrder();
			System.out.println(removeTypes[i]);
			int size = bst.getSize();
			bst.remove(removes[i]);
			assertTrue("Failed to remove " + removes[i], bst.find(removes[i]) == null);
			assertTrue("Removed Multiple item on remove of " + removes[i], size - 1 == bst.getSize());
		}
		assertTrue("AVL", bst.isAVL());
	}

	@Test
	public void leftRotation() {
		System.out.println("leftRotation");
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(12);
		bst.add(16);
		bst.add(8);
		bst.add(4);
		bst.add(14);
		bst.add(10);
		bst.add(2);
		bst.add(6);
		bst.printInOrder();
		bst.add(1);
		bst.printInOrder();
		assertTrue("AVL Property (Left Rotation)", bst.isAVL());
	}

	@Test
	public void rightRotation() {
		System.out.println("rightRotation");
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(-12);
		bst.add(-16);
		bst.add(-8);
		bst.add(-4);
		bst.add(-14);
		bst.add(-10);
		bst.add(-2);
		bst.add(-6);
		bst.printInOrder();
		bst.add(-1);
		bst.printInOrder();
		assertTrue("AVL Property (Right Rotation)", bst.isAVL());
	}

	@Test
	public void leftDoubleRotation() {
		System.out.println("doubleLeftRotation");
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(12);
		bst.add(16);
		bst.add(8);
		bst.add(4);
		bst.add(14);
		bst.add(10);
		bst.add(2);
		bst.add(6);
		bst.printInOrder();
		bst.add(5);
		bst.printInOrder();
		assertTrue("Not everything was added", bst.getSize() == 9);
		assertTrue("AVL Property (Left Double Rotation)", bst.isAVL());
	}

	@Test
	public void rightDoubleRotation() {
		System.out.println("doubleRightRotation");
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		bst.add(-12);
		bst.add(-16);
		bst.add(-8);
		bst.add(-4);
		bst.add(-14);
		bst.add(-10);
		bst.add(-2);
		bst.add(-6);
		bst.printInOrder();
		bst.add(-5);
		bst.printInOrder();
		assertTrue("AVL Property (Right Double Rotation)", bst.isAVL());
	}

	@Test
	public void removeMidTree() {
		System.out.println("Remove MidTree (Two Childs)");
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
		int size = bst.getSize();
		bst.printInOrder();
		Integer removedItem = bst.remove(6);
		if (removedItem != null)
			System.out.println("Removed: " + removedItem);
		else
			System.out.println("Removed: NULL");
		bst.printInOrder();
		assertTrue("Failed to remove 6", bst.find(6) == null);
		assertTrue("AVL Property", bst.isAVL());
		assertTrue("Removed Multiple Items", bst.getSize() == (size - 1));
	}
	@Test
	public void removeMidTree2() {
		System.out.println("Remove MidTree (One Child)");
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
		int size = bst.getSize();
		bst.printInOrder();
		Integer removedItem = bst.remove(8);
		if (removedItem != null)
			System.out.println("Removed: " + removedItem);
		else
			System.out.println("Removed: NULL");
		bst.printInOrder();
		assertTrue("Failed to remove 8", bst.find(8) == null);
		assertTrue("AVL Property", bst.isAVL());
		assertTrue("Removed Multiple Items", bst.getSize() == (size - 1));
	}

	@Test
	public void removeLeaf() {
		System.out.println("Remove Leaf");
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
		int size = bst.getSize();
		bst.printInOrder();
		Integer removedItem = bst.remove(2);
		if (removedItem != null)
			System.out.println("Removed: " + removedItem);
		else
			System.out.println("Removed: NULL");
		bst.printInOrder();
		assertTrue("Failed to remove 2", bst.find(2) == null);
		assertTrue("AVL Property", bst.isAVL());
		assertTrue("Removed Multiple Items", bst.getSize() == (size - 1));
	}

	@Test
	public void removeRootAndImbalance() {
		BinarySearchTree<Integer> bst = new BinarySearchTree<>();
		System.out.println("Remove Root");
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
		int[] removeOrder = new int[] { 12, 14, 10, 16, 2, 4, 8, 6, 5 };
		for(int i = 0; i < removeOrder.length; i++) {
			int size = bst.getSize();
			Integer j = bst.remove(removeOrder[i]);
			if(bst.getSize() != size - 1) {
				fail("Removed Multiple Items on " + removeOrder[i]);
			} else if(bst.find(removeOrder[i]) != null || j == null){
				fail("Failed to remove " + removeOrder[i]);
			} else if(!j.equals(removeOrder[i])) {
				fail("Removed " + j + " instead of " + removeOrder[i]);
			}
		}
		bst.printInOrder();
		assertTrue("AVL Property", bst.isAVL() && bst.isEmpty());
	}

}
