package org.johnnei.javatorrent.internal.utils;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.johnnei.javatorrent.internal.utils.IterableOps.foldLeft;
import static org.junit.jupiter.api.Assertions.*;

class IterableOpsTest {

	@Test
	public void testFoldLeft() {
		Collection<Integer> col = List.of(1, 2, 3, 4);
		assertEquals(10, foldLeft(col, 0, Integer::sum));
	}

}
