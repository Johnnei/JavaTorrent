package org.johnnei.javatorrent.internal.tracker.http;

import okhttp3.HttpUrl;

/**
 * Class to represent tracker urls.
 */
public class TrackerUrl {

	private final String schema;

	private final String host;

	private final String path;

	private final int port;

	public TrackerUrl(String trackerUrl) {
		HttpUrl url = HttpUrl.parse(trackerUrl);

		if (url == null) {
			throw new IllegalArgumentException("Failed to parse url: " + trackerUrl);
		}

		schema = url.scheme();
		host = url.host();
		// Strip the leading /
		path = url.encodedPath().substring(1);
		port = url.port();
	}

	public String getSchema() {
		return schema;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}
}
