package unittesting;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import torrent.util.tree.BinarySearchTree;

public class BST {

	@Test
	public void test() {
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
		assertTrue("AVL Property", bst.isAVL());
	}

}
