package org.johnnei.javatorrent.bittorrent.encoding;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

public class BencoderTest {

	@Test
	public void testIntegerInt() {
		Bencoder bencoder = new Bencoder();
		bencoder.integer(5);

		assertEquals("Incorrect handling of integers", "i5e", bencoder.getBencodedData());
	}

	@Test
	public void testIntegerLong() {
		Bencoder bencoder = new Bencoder();
		bencoder.integer(231312312312312L);

		assertEquals("Incorrect handling of longs", "i231312312312312e", bencoder.getBencodedData());
	}

	@Test
	public void testString() {
		Bencoder bencoder = new Bencoder();
		bencoder.string("spam");

		assertEquals("Incorrect handling of strings", "4:spam", bencoder.getBencodedData());
	}

	@Test
	public void testList() {
		Bencoder bencoder = new Bencoder();
		bencoder.listStart();
		bencoder.string("spam");
		bencoder.string("eggs");
		bencoder.listEnd();

		assertEquals("Incorrect handling of lists", "l4:spam4:eggse", bencoder.getBencodedData());
	}

	@Test
	public void testDictionary() {
		Bencoder bencoder = new Bencoder();
		bencoder.dictionaryStart();
		bencoder.string("cow");
		bencoder.string("moo");
		bencoder.string("spam");
		bencoder.string("eggs");
		bencoder.dictionaryEnd();

		assertEquals("Incorrect handling of dictionaries", "d3:cow3:moo4:spam4:eggse", bencoder.getBencodedData());
	}

	@Ignore("See issue #40")
	@Test
	public void testDictionaryOrdering() {
		Bencoder bencoder = new Bencoder();
		bencoder.dictionaryStart();
		bencoder.string("spam");
		bencoder.string("eggs");
		bencoder.string("cow");
		bencoder.string("moo");
		bencoder.dictionaryEnd();

		assertEquals("Incorrect key sorting of dictionaries", "d3:cow3:moo4:spam4:eggse", bencoder.getBencodedData());
	}

	@Test
	public void testListInDictionary() {
		Bencoder bencoder = new Bencoder();
		bencoder.dictionaryStart();
		bencoder.string("spam");
		bencoder.listStart();
		bencoder.string("a");
		bencoder.string("b");
		bencoder.listEnd();
		bencoder.dictionaryEnd();

		assertEquals("Incorrect handling of lists in dictionaries", "d4:spaml1:a1:bee", bencoder.getBencodedData());
	}

}
