package org.johnnei.javatorrent.bittorrent.encoding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class BencodeTest {

	@Test
	public void testBencode() {
		String bencodedInfo = "d4:spaml1:ai2ee4:eggsi12eei12e";

		Bencode bencoding = new Bencode(bencodedInfo);
		Map<String, Object> dictionary = bencoding.decodeDictionary();

		assertEquals("Incorrect element count in dictionary", 2, dictionary.size());
		assertTrue("Dictionary doesn't contain the key 'eggs'", dictionary.containsKey("eggs"));
		assertEquals("Eggs value doesn't match 12", 12, dictionary.get("eggs"));
		assertTrue("Dictionary doesn't contain the key 'spam'", dictionary.containsKey("spam"));
		List<?> spam = (List<?>) dictionary.get("spam");
		assertEquals("List doesn't contain 2 items", 2, spam.size());
		assertEquals("List[0] doesn't equal a", "a", spam.get(0));
		assertEquals("List[1] doesn't equal 2", 2, spam.get(1));
		assertEquals("Data at the end got read", 4, bencoding.remainingChars());
	}

	@Test
	public void testNestedDictionary() {
		String bencodedInfo = "d4:spamd4:eggs5:freshee";
		Bencode bencoding = new Bencode(bencodedInfo);
		Map<String, Object> dictionary = bencoding.decodeDictionary();

		assertEquals("Incorrect element count in dictionary", 1, dictionary.size());
		Map<?, ?> nestedDictionary = (Map<?, ?>) dictionary.get("spam");

		assertEquals("Incorrect element count in nested-dictionary", 1, nestedDictionary.size());
		assertEquals("Incorrect string in nested-dictionary", "fresh", nestedDictionary.get("eggs"));
	}

	@Test
	public void testNestedDictionaryAndListInList() {
		String bencodedInfo = "ld4:spam4:eggseli42eee";
		Bencode bencoding = new Bencode(bencodedInfo);
		List<Object> list = bencoding.decodeList();

		assertEquals("Incorrect element count in list", 2, list.size());
		Map<?, ?> nestedDictionary = (Map<?, ?>) list.get(0);

		assertEquals("Incorrect element count in nested-dictionary", 1, nestedDictionary.size());
		assertEquals("Incorrect string in nested-dictionary", "eggs", nestedDictionary.get("spam"));

		List<?> nestedList = (List<?>) list.get(1);
		assertEquals("Incorrect nested list element count", 1, nestedList.size());
		assertEquals("Incorrect integer as second element", 42, nestedList.get(0));
	}

	@Test
	public void testDecodeLong() {
		String bencodedInfo = "i231312312312312e";
		assertEquals("Long didn't get read properly", 231312312312312L, new Bencode(bencodedInfo).decodeInteger());
	}

	@Test(expected=IllegalStateException.class)
	public void testStringAsInteger() {
		String bencodedInfo = "4:spam";
		new Bencode(bencodedInfo).decodeInteger();
	}

	@Test(expected=IllegalStateException.class)
	public void testIntegerAsString() {
		String bencodedInfo = "i42e";
		new Bencode(bencodedInfo).decodeString();
	}

	@Test(expected=IllegalStateException.class)
	public void testStringAsList() {
		String bencodedInfo = "4:spam";
		new Bencode(bencodedInfo).decodeList();
	}

	@Test(expected=IllegalStateException.class)
	public void testStringAsDictionary() {
		String bencodedInfo = "4:spam";
		new Bencode(bencodedInfo).decodeDictionary();
	}

}
