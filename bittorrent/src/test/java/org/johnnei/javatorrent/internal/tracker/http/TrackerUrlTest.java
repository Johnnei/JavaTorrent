package org.johnnei.javatorrent.internal.tracker.http;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link TrackerUrl}
 */
public class TrackerUrlTest {

	@ParameterizedTest(name = "testConstructor with {0}")
	@MethodSource("data")
	public void testDomainParsing(String url, String schema, String host, int port, String path) {
		TrackerUrl cut = new TrackerUrl(url);

		assertAll(
			() -> assertEquals(schema, cut.getSchema(), "Incorrect schema"),
			() -> assertEquals(host, cut.getHost(), "Incorrect host"),
			() -> assertEquals(port, cut.getPort(), "Incorrect port"),
			() -> assertEquals(path, cut.getPath(), "Incorrect path")
		);
	}

	public static Stream<Arguments> data() {
		return Stream.of(
			Arguments.of("http://www.google.com:80/announce", "http", "www.google.com", 80, "announce"),
			Arguments.of("https://localhost:80", "https", "localhost", 80, ""),
			Arguments.of("https://localhost", "https", "localhost", 443, "")
		);
	}

	@Test
	public void testIllegalUrl() {
		assertThrows(IllegalArgumentException.class, () -> new TrackerUrl("localhost"));
	}

}
