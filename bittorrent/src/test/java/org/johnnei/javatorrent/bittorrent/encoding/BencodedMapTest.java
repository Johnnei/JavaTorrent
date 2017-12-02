package org.johnnei.javatorrent.bittorrent.encoding;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link BencodedMap}
 */
public class BencodedMapTest {

	@Test
	public void testPut() throws Exception {
		BencodedMap cut = new BencodedMap();

		String key = "cow";

		assertNotPresent("Item available before added", cut.get(key));

		IBencodedValue valueMock = mock(IBencodedValue.class);
		cut.put(key, valueMock);

		Optional<IBencodedValue> value = cut.get(key);
		assertPresent("Item must be available after adding", value);
		assertEquals(valueMock, value.get(), "Incorrect value was returned");
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
		assertEquals(valueMock, value.get(), "Incorrect value was returned");

		IBencodedValue valueMockTwo = mock(IBencodedValue.class);
		cut.put(key, valueMockTwo);
		value = cut.get(key);
		assertPresent("Item must be available after adding", value);
		assertEquals(valueMockTwo, value.get(), "Incorrect value was returned after overwrite");
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
		assertEquals(valueMock, value.get(), "Incorrect item was removed");
	}

	@ParameterizedTest
	@MethodSource("unsupportedOperations")
	public void testUnsupportedOperations(Consumer<BencodedMap> consumer) {
		assertThrows(UnsupportedOperationException.class, () -> consumer.accept(new BencodedMap()));
	}

	public static Stream<Consumer<BencodedMap>> unsupportedOperations() {
		return Stream.of(
			BencodedMap::asBytes,
			BencodedMap::asString,
			BencodedMap::asLong,
			BencodedMap::asBigInteger,
			BencodedMap::asList
		);
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
		assertEquals(valueMock, value.get(), "Incorrect value was returned");

		Map<String, IBencodedValue> map = cut.asMap();
		assertEquals(1, map.size(), "Incorrect amount of items in map copy");
		assertEquals(valueMock, map.get("cow"), "Incorrect value in map copy");
	}

	@Test
	public void testSerialize() throws Exception {
		BencodedMap cut = new BencodedMap();

		IBencodedValue valueMock = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMock.serialize()).thenReturn("3:moo".getBytes(BitTorrent.DEFAULT_ENCODING));
		when(valueMockTwo.serialize()).thenReturn("4:eggs".getBytes(BitTorrent.DEFAULT_ENCODING));

		cut.put("cow", valueMock);
		cut.put("spam", valueMockTwo);

		assertEquals("d3:cow3:moo4:spam4:eggse", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING), "Incorrect serialized form");
	}

	/**
	 * Tests that the keys are sorted according to byte order.
	 */
	@Test
	public void testSerializeSortRawBytes() throws Exception {
		BencodedMap cut = new BencodedMap();

		IBencodedValue valueMock = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMock.serialize()).thenReturn("4:eggs".getBytes(BitTorrent.DEFAULT_ENCODING));
		when(valueMockTwo.serialize()).thenReturn("3:moo".getBytes(BitTorrent.DEFAULT_ENCODING));

		cut.put("cow", valueMock);
		cut.put("Cow", valueMockTwo);

		assertEquals("d3:Cow3:moo3:cow4:eggse", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING), "Incorrect serialized form");
	}

	/**
	 * Tests that adding in a different order produces the same result. The keys are sorted according to byte order.
	 */
	@Test
	public void testSerializeAddIncorrectOrder() throws Exception {
		BencodedMap cut = new BencodedMap();

		IBencodedValue valueMock = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMock.serialize()).thenReturn("3:moo".getBytes(BitTorrent.DEFAULT_ENCODING));
		when(valueMockTwo.serialize()).thenReturn("4:eggs".getBytes(BitTorrent.DEFAULT_ENCODING));

		cut.put("spam", valueMockTwo);
		cut.put("cow", valueMock);

		assertEquals("d3:cow3:moo4:spam4:eggse", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING), "Incorrect serialized form");
	}

}
