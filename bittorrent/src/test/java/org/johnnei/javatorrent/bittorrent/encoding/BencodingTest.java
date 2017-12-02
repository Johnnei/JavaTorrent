package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.InStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link Bencoding}
 */
public class BencodingTest {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Test
	public void testDecode() {
		String bencodedInfo = "d4:spaml1:ai2ee4:eggsi12ee";
		String unreadInfo = "i12e";

		Bencoding cut = new Bencoding();
		InStream inStream = new InStream((bencodedInfo + unreadInfo).getBytes(UTF8));
		IBencodedValue value = cut.decode(inStream);
		Map<String, IBencodedValue> dictionary = value.asMap();

		assertEquals(2, dictionary.size(), "Incorrect element count in dictionary");
		assertAll(
			() -> assertTrue(dictionary.containsKey("eggs"), "Dictionary doesn't contain the key 'eggs'"),
			() -> assertTrue(dictionary.containsKey("spam"), "Dictionary doesn't contain the key 'spam'"),
			() -> assertAll(
				() -> assertEquals(12L, dictionary.get("eggs").asLong(), "Eggs value doesn't match 12"),
				() -> {
					List<IBencodedValue> spam = dictionary.get("spam").asList();
					assertEquals(2, spam.size(), "List doesn't contain 2 items");
					assertAll(
						() -> assertEquals("a", spam.get(0).asString(), "List[0] doesn't equal a"),
						() -> assertEquals(2, spam.get(1).asLong(), "List[1] doesn't equal 2")
					);
				}
			)
		);
		assertEquals(4, inStream.available(), "Incorrect amount of characters read, the 'unreadInfo' should not be consumed.");
	}

	@Test
	public void testNestedDictionary() {
		String bencodedInfo = "d4:spamd4:eggs5:freshee";
		Bencoding cut = new Bencoding();
		InStream inStream = new InStream(bencodedInfo.getBytes(UTF8));
		Map<String, IBencodedValue> dictionary = cut.decode(inStream).asMap();

		assertEquals(1, dictionary.size(), "Incorrect element count in dictionary");
		Map<String, IBencodedValue> nestedDictionary = dictionary.get("spam").asMap();

		assertEquals(1, nestedDictionary.size(), "Incorrect element count in nested-dictionary");
		assertEquals("fresh", nestedDictionary.get("eggs").asString(), "Incorrect string in nested-dictionary");
		assertEquals(0, inStream.available(), "Incorrect amount of characters read, all should have been read");
	}

	@Test
	public void testNestedDictionaryAndListInList() {
		String bencodedInfo = "ld4:spam4:eggseli42eee";
		Bencoding cut = new Bencoding();
		InStream inStream = new InStream(bencodedInfo.getBytes(UTF8));
		List<IBencodedValue> list = cut.decode(inStream).asList();

		assertEquals(2, list.size(), "Incorrect element count in list");
		Map<String, IBencodedValue> nestedDictionary = list.get(0).asMap();

		assertEquals(1, nestedDictionary.size(), "Incorrect element count in nested-dictionary");
		assertEquals("eggs", nestedDictionary.get("spam").asString(), "Incorrect string in nested-dictionary");

		List<IBencodedValue> nestedList = list.get(1).asList();
		assertEquals(1, nestedList.size(), "Incorrect nested list element count");
		assertEquals((long) 42, nestedList.get(0).asLong(), "Incorrect integer as second element");
		assertEquals(0, inStream.available(), "Incorrect amount of characters read, all should have been read");
	}

	@Test
	public void testDecodeLong() {
		String bencodedInfo = "i231312312312312e";
		assertEquals(231312312312312L, new Bencoding().decode(new InStream(bencodedInfo.getBytes(UTF8))).asLong(), "Long didn't get read properly");
	}

	@Test
	public void testDecodeString() {
		String bencodedInfo = "4:spam";
		assertEquals("spam", new Bencoding().decode(new InStream(bencodedInfo.getBytes(UTF8))).asString(), "Incorrect string");
	}

	@Test
	public void testDecodeIncompleteString() {
		assertThrows(IllegalArgumentException.class, () -> new Bencoding().decode(new InStream("4:sp".getBytes(UTF8))).asString());
	}

	@Test
	public void testDecodeStringWithNonAsciiCharacters() {
		String bencodedInfo = "d1:v15:μTorrent 3.4.72:ypi62954ee";

		Bencoding cut = new Bencoding();
		BencodedMap map = (BencodedMap) cut.decode(new InStream(bencodedInfo.getBytes(UTF8)));

		assertEquals("μTorrent 3.4.7", map.get("v").get().asString(), "Incorrect version string has been read");
	}
}
