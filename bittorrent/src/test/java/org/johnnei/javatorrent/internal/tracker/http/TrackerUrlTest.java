package org.johnnei.javatorrent.internal.tracker.http;

import java.util.Arrays;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link TrackerUrl}
 */
@RunWith(Parameterized.class)
public class TrackerUrlTest {

	private static ExpectedException illegalUrl = ExpectedException.none();

	@BeforeClass
	public static void setUpClass() {
		illegalUrl.expect(IllegalArgumentException.class);
	}

	@Parameterized.Parameters(name = "testConstructor with {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "http://www.google.com:80/announce", "http", "www.google.com", 80, "announce", ExpectedException.none() },
				{ "https://localhost:80", "https", "localhost", 80, "", ExpectedException.none() },
				{ "https://localhost", "https", "localhost", 443, "", ExpectedException.none() },
				{ "localhost", "https", "localhost", 443, "", illegalUrl}
		});
	}

	@Parameterized.Parameter
	public String url;

	@Parameterized.Parameter(1)
	public String schema;

	@Parameterized.Parameter(2)
	public String host;

	@Parameterized.Parameter(3)
	public int port;

	@Parameterized.Parameter(4)
	public String path;

	@Rule
	@Parameterized.Parameter(5)
	public ExpectedException thrown;

	@Test
	public void testDomainParsing() {
		TrackerUrl cut = new TrackerUrl(url);

		assertEquals("Incorrect schema", schema, cut.getSchema());
		assertEquals("Incorrect host", host, cut.getHost());
		assertEquals("Incorrect port", port, cut.getPort());
		assertEquals("Incorrect path", path, cut.getPath());
	}

}