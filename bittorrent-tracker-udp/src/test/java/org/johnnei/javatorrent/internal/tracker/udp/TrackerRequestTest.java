package org.johnnei.javatorrent.internal.tracker.udp;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.tracker.UdpTracker;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.johnnei.javatorrent.test.TestUtils.assertEqualsMethod;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrackerRequestTest {

	private MessageStub messageStub;

	private UdpTracker tracker;

	private TrackerRequest request;

	@BeforeEach
	public void setUp() throws Exception {
		messageStub = new MessageStub(TrackerAction.ANNOUNCE);
		tracker = new UdpTracker.Builder()
				.setUrl("udp://localhost:80")
				.build();
		tracker.setConnection(new Connection(5, Clock.systemDefaultZone()));
		request = new TrackerRequest(tracker, 7, messageStub);
	}

	@Test
	public void testValuesAndStubMessage() {
		assertEquals(tracker, request.getTracker(), "Incorrect tracker");
		assertEquals(7, request.getTransactionId(), "Transaction id");
		assertEquals(TrackerAction.ANNOUNCE, request.getAction(), "Incorrect action");
	}

	@Test
	public void testEqualsHashcode() {
		TrackerRequest requestTwo = new TrackerRequest(tracker, 3, messageStub);
		TrackerRequest requestThree = new TrackerRequest(tracker, 7, messageStub);
		assertEqualsMethod(request);
		assertEquals(request, requestThree, "Requests with same transaction ids didn't match");
		assertEquals(request.hashCode(), requestThree.hashCode(), "Requests with same transaction ids didn't match");
		assertNotEquals(request, requestTwo, "Requests with different transaction ids mustn't match");
		assertNotEquals(request.hashCode(), requestTwo.hashCode(), "Requests with different transaction ids mustn't match");
	}

	@Test
	public void testWriteRequest() {
		OutStream outStream = new OutStream();
		request.writeRequest(outStream);

		final byte[] expectedOutput = {
				// Connection ID
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05,
				// Action
				0x00, 0x00, 0x00, 0x01,
				// Transaction ID
				0x00, 0x00, 0x00, 0x07,
				// Message Stub write
				0x01
		};

		assertArrayEquals(expectedOutput, outStream.toByteArray(), "Incorrect output");
	}

	@Test
	public void testReadAndProcessRequest() throws Exception {
		final byte[] input = {
				// Action
				0x00, 0x00, 0x00, 0x01,
				// Transaction ID
				0x00, 0x00, 0x00, 0x07,
				// Message Stub read
				0x01
		};

		InStream inStream = new InStream(input);
		request.readResponse(inStream);
		request.process();

		assertTrue(messageStub.processed, "Message wasn't processed");
	}

	@Test
	public void testReadIncorrectTransaction() throws Exception {
		final byte[] input = {
				// Action
				0x00, 0x00, 0x00, 0x01,
				// Transaction ID
				0x00, 0x00, 0x00, 0x05,
				// Message Stub read
				0x01
		};

		InStream inStream = new InStream(input);
		Exception e = assertThrows(TrackerException.class, () -> request.readResponse(inStream));
		assertThat(e.getMessage(), containsString("transaction id"));
	}

	@Test
	public void testReadTrackerError() throws Exception {
		final byte[] input = {
				// Action
				0x00, 0x00, 0x00, 0x03,
				// Error Message
				0x53, 0x74, 0x75, 0x62
		};

		InStream inStream = new InStream(input);
		Exception e = assertThrows(TrackerException.class, () -> request.readResponse(inStream));
		assertThat(e.getMessage(), containsString("Stub"));
	}

	private final static class MessageStub implements IUdpTrackerPayload {

		private final TrackerAction action;

		private boolean processed;

		public MessageStub(TrackerAction action) {
			this.action = action;
		}

		@Override
		public void writeRequest(OutStream outStream) {
			outStream.writeByte(1);
		}

		@Override
		public void readResponse(InStream inStream) throws TrackerException {
			assertEquals(1, inStream.readByte(), "Incorrect byte read");
		}

		@Override
		public void process(UdpTracker tracker) {
			processed = true;
		}

		@Override
		public TrackerAction getAction() {
			return action;
		}

		@Override
		public int getMinimalSize() {
			return 0;
		}

	}

}
