package org.johnnei.javatorrent.bittorrent.encoding;

import java.util.Map;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link BencodedMap}
 */
public class BencodedMapTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testPut() throws Exception {
		BencodedMap cut = new BencodedMap();

		String key = "cow";

		assertNotPresent("Item available before added", cut.get(key));

		IBencodedValue valueMock = mock(IBencodedValue.class);
		cut.put(key, valueMock);

		Optional<IBencodedValue> value = cut.get(key);
		assertPresent("Item must be available after adding", value);
		assertEquals("Incorrect value was returned", valueMock, value.get());
	}

	@Test
	public void testPutOverwrite() throws Exception {
		BencodedMap cut = new BencodedMap();

		String key = "cow";

		assertNotPresent("Item available before added", cut.get(key));

		IBencodedValue valueMock = mock(IBencodedValue.class);
		cut.put(key, valueMock);

		Optional<IBencodedValue> value = cut.get(key);
		assertPresent("Item must be available after adding", value);
		assertEquals("Incorrect value was returned", valueMock, value.get());

		IBencodedValue valueMockTwo = mock(IBencodedValue.class);
		cut.put(key, valueMockTwo);
		value = cut.get(key);
		assertPresent("Item must be available after adding", value);
		assertEquals("Incorrect value was returned after overwrite", valueMockTwo, value.get());
	}

	@Test
	public void testRemove() throws Exception {
		BencodedMap cut = new BencodedMap();

		String key = "cow";

		assertNotPresent("Item available before added", cut.get(key));
		assertNotPresent("An item was removed before adding anything", cut.remove(key));

		IBencodedValue valueMock = mock(IBencodedValue.class);
		cut.put(key, valueMock);

		assertPresent("Item must be available after adding", cut.get(key));

		Optional<IBencodedValue> value = cut.remove(key);
		assertPresent("Value must have been removed.", value);
		assertEquals("Incorrect item was removed", valueMock, value.get());
	}

	@Test
	public void testAsString() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedMap().asString();
	}

	@Test
	public void testAsLong() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedMap().asLong();
	}

	@Test
	public void testAsBigInteger() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedMap().asBigInteger();
	}

	@Test
	public void testAsMap() throws Exception {
		BencodedMap cut = new BencodedMap();

		String key = "cow";

		assertNotPresent("Item available before added", cut.get(key));

		IBencodedValue valueMock = mock(IBencodedValue.class);
		cut.put(key, valueMock);

		Optional<IBencodedValue> value = cut.get(key);
		assertPresent("Item must be available after adding", value);
		assertEquals("Incorrect value was returned", valueMock, value.get());

		Map<String, IBencodedValue> map = cut.asMap();
		assertEquals("Incorrect amount of items in map copy", 1, map.size());
		assertEquals("Incorrect value in map copy", valueMock, map.get("cow"));
	}

	@Test
	public void testAsList() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedMap().asList();
	}

	@Test
	public void testSerialize() throws Exception {
		BencodedMap cut = new BencodedMap();

		IBencodedValue valueMock = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMock.serialize()).thenReturn("3:moo");
		when(valueMockTwo.serialize()).thenReturn("4:eggs");

		cut.put("cow", valueMock);
		cut.put("spam", valueMockTwo);

		assertEquals("Incorrect serialized form", "d3:cow3:moo4:spam4:eggse", cut.serialize());
	}

	/**
	 * Tests that the keys are sorted according to byte order.
	 */
	@Test
	public void testSerializeSortRawBytes() throws Exception {
		BencodedMap cut = new BencodedMap();

		IBencodedValue valueMock = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMock.serialize()).thenReturn("4:eggs");
		when(valueMockTwo.serialize()).thenReturn("3:moo");

		cut.put("cow", valueMock);
		cut.put("Cow", valueMockTwo);

		assertEquals("Incorrect serialized form", "d3:Cow3:moo3:cow4:eggse", cut.serialize());
	}

	/**
	 * Tests that adding in a different order produces the same result. The keys are sorted according to byte order.
	 */
	@Test
	public void testSerializeAddIncorrectOrder() throws Exception {
		BencodedMap cut = new BencodedMap();

		IBencodedValue valueMock = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMock.serialize()).thenReturn("3:moo");
		when(valueMockTwo.serialize()).thenReturn("4:eggs");

		cut.put("spam", valueMockTwo);
		cut.put("cow", valueMock);

		assertEquals("Incorrect serialized form", "d3:cow3:moo4:spam4:eggse", cut.serialize());
	}

}