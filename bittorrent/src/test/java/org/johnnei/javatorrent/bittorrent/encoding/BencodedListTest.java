package org.johnnei.javatorrent.bittorrent.encoding;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link BencodedList}
 */
public class BencodedListTest {

	@Test
	public void testAddAndGet() {
		IBencodedValue valueMockOne = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		BencodedList cut = new BencodedList();
		cut.add(valueMockOne);

		assertEquals(1, cut.size(), "Incorrect amount of items in list");
		assertEquals(valueMockOne, cut.get(0), "Incorrect value got returned");

		cut.add(valueMockTwo);
		assertEquals(2, cut.size(), "Incorrect amount of items in list");
		assertAll(
			() -> assertEquals(valueMockOne, cut.get(0), "Incorrect value got returned for index 0"),
			() -> assertEquals(valueMockTwo, cut.get(1), "Incorrect value got returned for index 1")
		);
	}

	@Test
	public void testSerialize() {
		IBencodedValue valueMockOne = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMockOne.serialize()).thenReturn("4:spam".getBytes(BitTorrent.DEFAULT_ENCODING));
		when(valueMockTwo.serialize()).thenReturn("4:eggs".getBytes(BitTorrent.DEFAULT_ENCODING));

		BencodedList cut = new BencodedList();

		cut.add(valueMockOne);
		cut.add(valueMockTwo);

		assertEquals("l4:spam4:eggse", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING), "Incorrect serialized form");
	}

	@Test
	public void testAsList() {
		IBencodedValue valueMockOne = mock(IBencodedValue.class);
		IBencodedValue valueMockTwo = mock(IBencodedValue.class);

		when(valueMockOne.serialize()).thenReturn("4:spam".getBytes(BitTorrent.DEFAULT_ENCODING));
		when(valueMockTwo.serialize()).thenReturn("4:eggs".getBytes(BitTorrent.DEFAULT_ENCODING));

		BencodedList cut = new BencodedList();

		cut.add(valueMockOne);
		cut.add(valueMockTwo);

		List<IBencodedValue> list = cut.asList();
		assertEquals(2, list.size(), "Incorrect list copy size");
		assertAll(
			() -> assertEquals(valueMockOne, list.get(0), "Incorrect list copy on index 0"),
			() -> assertEquals(valueMockTwo, list.get(1), "Incorrect list copy on index 1")
		);
	}

	@ParameterizedTest
	@MethodSource("unsupportedOperations")
	public void testUnsupportedOperations(Consumer<BencodedList> consumer) {
		assertThrows(UnsupportedOperationException.class, () -> consumer.accept(new BencodedList()));
	}

	public static Stream<Consumer<BencodedList>> unsupportedOperations() {
		return Stream.of(
			BencodedList::asBytes,
			BencodedList::asString,
			BencodedList::asLong,
			BencodedList::asMap,
			BencodedList::asBigInteger
		);
	}

}
