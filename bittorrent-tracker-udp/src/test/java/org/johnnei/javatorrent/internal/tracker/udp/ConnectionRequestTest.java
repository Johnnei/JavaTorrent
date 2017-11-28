package org.johnnei.javatorrent.internal.tracker.udp;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.tracker.UdpTracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class ConnectionRequestTest {

	@Test
	public void testWriteRequest() {
		ConnectionRequest request = new ConnectionRequest(Clock.systemDefaultZone());
		OutStream outStream = new OutStream();

		request.writeRequest(outStream);

		byte[] output = outStream.toByteArray();
		assertEquals(0, output.length, "Didn't expect any output");
	}

	@Test
	public void testReadResponse() throws Exception {
		Clock clock = Clock.systemDefaultZone();
		byte[] inputBytes = {
				0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01,
		};
		InStream inStream = new InStream(inputBytes);

		ConnectionRequest request = new ConnectionRequest(clock);

		UdpTracker trackerMock = mock(UdpTracker.class);
		ArgumentCaptor<Connection> connectionCapture = ArgumentCaptor.forClass(Connection.class);

		request.readResponse(inStream);
		request.process(trackerMock);

		verify(trackerMock).setConnection(connectionCapture.capture());

		assertEquals(281479271743489L, connectionCapture.getValue().getId(), "Incorrect id");
	}

	@Test
	public void testGetAction() {
		ConnectionRequest request = new ConnectionRequest(Clock.systemDefaultZone());

		assertEquals(TrackerAction.CONNECT, request.getAction(), "Incorrect action");
	}

	@Test
	public void testMinimalSize() {
		ConnectionRequest request = new ConnectionRequest(Clock.systemDefaultZone());

		assertEquals(8, request.getMinimalSize(), "Incorrect action");
	}

}
