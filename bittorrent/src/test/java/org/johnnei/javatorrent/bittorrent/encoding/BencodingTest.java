package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.johnnei.javatorrent.network.InStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Bencoding}
 */
public class BencodingTest {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testDecode() {
		String bencodedInfo = "d4:spaml1:ai2ee4:eggsi12ee";
		String unreadInfo = "i12e";

		Bencoding cut = new Bencoding();
		InStream inStream = new InStream((bencodedInfo + unreadInfo).getBytes(UTF8));
		IBencodedValue value = cut.decode(inStream);
		Map<String, IBencodedValue> dictionary = value.asMap();

		assertEquals("Incorrect element count in dictionary", 2, dictionary.size());
		assertTrue("Dictionary doesn't contain the key 'eggs'", dictionary.containsKey("eggs"));
		assertEquals("Eggs value doesn't match 12", 12L, dictionary.get("eggs").asLong());
		assertTrue("Dictionary doesn't contain the key 'spam'", dictionary.containsKey("spam"));
		List<IBencodedValue> spam = dictionary.get("spam").asList();
		assertEquals("List doesn't contain 2 items", 2, spam.size());
		assertEquals("List[0] doesn't equal a", "a", spam.get(0).asString());
		assertEquals("List[1] doesn't equal 2", 2, spam.get(1).asLong());
		assertEquals("Incorrect amount of characters read, the 'unreadInfo' should not be consumed.", 4, inStream.available());
	}

	@Test
	public void testNestedDictionary() {
		String bencodedInfo = "d4:spamd4:eggs5:freshee";
		Bencoding cut = new Bencoding();
		InStream inStream = new InStream(bencodedInfo.getBytes(UTF8));
		Map<String, IBencodedValue> dictionary = cut.decode(inStream).asMap();

		assertEquals("Incorrect element count in dictionary", 1, dictionary.size());
		Map<String, IBencodedValue> nestedDictionary = dictionary.get("spam").asMap();

		assertEquals("Incorrect element count in nested-dictionary", 1, nestedDictionary.size());
		assertEquals("Incorrect string in nested-dictionary", "fresh", nestedDictionary.get("eggs").asString());
		assertEquals("Incorrect amount of characters read, all should have been read", 0, inStream.available());
	}

	@Test
	public void testNestedDictionaryAndListInList() {
		String bencodedInfo = "ld4:spam4:eggseli42eee";
		Bencoding cut = new Bencoding();
		InStream inStream = new InStream(bencodedInfo.getBytes(UTF8));
		List<IBencodedValue> list = cut.decode(inStream).asList();

		assertEquals("Incorrect element count in list", 2, list.size());
		Map<String, IBencodedValue> nestedDictionary = list.get(0).asMap();

		assertEquals("Incorrect element count in nested-dictionary", 1, nestedDictionary.size());
		assertEquals("Incorrect string in nested-dictionary", "eggs", nestedDictionary.get("spam").asString());

		List<IBencodedValue> nestedList = list.get(1).asList();
		assertEquals("Incorrect nested list element count", 1, nestedList.size());
		assertEquals("Incorrect integer as second element", 42, nestedList.get(0).asLong());
		assertEquals("Incorrect amount of characters read, all should have been read", 0, inStream.available());
	}

	@Test
	public void testDecodeLong() {
		String bencodedInfo = "i231312312312312e";
		assertEquals("Long didn't get read properly", 231312312312312L, new Bencoding().decode(new InStream(bencodedInfo.getBytes(UTF8))).asLong());
	}

	@Test
	public void testDecodeString() {
		String bencodedInfo = "4:spam";
		assertEquals("Incorrect string", "spam", new Bencoding().decode(new InStream(bencodedInfo.getBytes(UTF8))).asString());
	}

	@Test
	public void testDecodeIncompleteString() {
		thrown.expect(IllegalArgumentException.class);

		String bencodedInfo = "4:sp";
		new Bencoding().decode(new InStream(bencodedInfo.getBytes(UTF8))).asString();
	}

	@Test
	public void testDecodeStringWithNonAsciiCharacters() {
		String bencodedInfo = "d1:v15:μTorrent 3.4.72:ypi62954ee";

		Bencoding cut = new Bencoding();
		BencodedMap map = (BencodedMap) cut.decode(new InStream(bencodedInfo.getBytes(UTF8)));

		assertEquals("Incorrect version string has been read", "μTorrent 3.4.7", map.get("v").get().asString());
	}
}