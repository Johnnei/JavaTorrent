package org.johnnei.javatorrent.internal.tracker.udp;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;

import java.time.Clock;

import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class ConnectionRequestTest extends EasyMockSupport {

	@Test
	public void testWriteRequest() {
		ConnectionRequest request = new ConnectionRequest(Clock.systemDefaultZone());
		OutStream outStream = new OutStream();

		request.writeRequest(outStream);

		byte[] output = outStream.toByteArray();
		assertEquals("Didn't expect any output", 0, output.length);
	}

	@Test
	public void testReadResponse() throws Exception {
		Clock clock = Clock.systemDefaultZone();
		byte[] inputBytes = {
				0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01,
		};
		InStream inStream = new InStream(inputBytes);

		ConnectionRequest request = new ConnectionRequest(clock);

		UdpTracker trackerMock = createMock(UdpTracker.class);
		Capture<Connection> connectionCapture = newCapture();
		trackerMock.setConnection(and(notNull(), capture(connectionCapture)));

		replayAll();

		request.readResponse(inStream);
		request.process(trackerMock);

		verifyAll();

		assertEquals("Incorrect id", 281479271743489L, connectionCapture.getValue().getId());
	}

	@Test
	public void testGetAction() {
		ConnectionRequest request = new ConnectionRequest(Clock.systemDefaultZone());

		assertEquals("Incorrect action", TrackerAction.CONNECT, request.getAction());
	}

	@Test
	public void testMinimalSize() {
		ConnectionRequest request = new ConnectionRequest(Clock.systemDefaultZone());

		assertEquals("Incorrect action", 8, request.getMinimalSize());
	}

}
