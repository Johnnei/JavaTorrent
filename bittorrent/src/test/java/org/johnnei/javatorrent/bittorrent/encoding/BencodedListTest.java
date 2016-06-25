package org.johnnei.javatorrent.bittorrent.encoding;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link BencodedList}
 */
public class BencodedListTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testAddAndGet() {
		IBencodedValue valueMockOne = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		BencodedList cut = new BencodedList();
		cut.add(valueMockOne);

		assertEquals("Incorrect amount of items in list", 1, cut.size());
		assertEquals("Incorrect value got returned", valueMockOne, cut.get(0));

		cut.add(valueMockTwo);
		assertEquals("Incorrect amount of items in list", 2, cut.size());
		assertEquals("Incorrect value got returned for index 0", valueMockOne, cut.get(0));
		assertEquals("Incorrect value got returned for index 1", valueMockTwo, cut.get(1));
	}

	@Test
	public void testSerialize() {
		IBencodedValue valueMockOne = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMockOne.serialize()).thenReturn("4:spam");
		when(valueMockTwo.serialize()).thenReturn("4:eggs");

		BencodedList cut = new BencodedList();

		cut.add(valueMockOne);
		cut.add(valueMockTwo);

		assertEquals("Incorrect serialized form", "l4:spam4:eggse", cut.serialize());
	}

	@Test
	public void testAsList() {
		IBencodedValue valueMockOne = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMockOne.serialize()).thenReturn("4:spam");
		when(valueMockTwo.serialize()).thenReturn("4:eggs");

		BencodedList cut = new BencodedList();

		cut.add(valueMockOne);
		cut.add(valueMockTwo);

		List<IBencodedValue> list = cut.asList();
		assertEquals("Incorrect list copy size", 2, list.size());
		assertEquals("Incorrect list copy on index 0", valueMockOne, list.get(0));
		assertEquals("Incorrect list copy on index 1", valueMockTwo, list.get(1));
	}

	@Test
	public void testAsString() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedList().asString();
	}

	@Test
	public void testAsLong() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedList().asLong();
	}

	@Test
	public void testAsMap() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedList().asMap();
	}

	@Test
	public void testAsBigInteger() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedList().asBigInteger();
	}

}