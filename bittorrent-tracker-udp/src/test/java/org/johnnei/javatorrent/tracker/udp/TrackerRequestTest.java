package org.johnnei.javatorrent.tracker.udp;

import static org.hamcrest.CoreMatchers.containsString;
import static org.johnnei.javatorrent.test.TestUtils.assertEqualsMethod;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.time.Clock;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TrackerRequestTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private MessageStub messageStub;

	private UdpTracker tracker;

	private TrackerRequest request;

	@Before
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
		assertEquals("Incorrect tracker", tracker, request.getTracker());
		assertEquals("Transaction id", 7, request.getTransactionId());
		assertEquals("Incorrect action", TrackerAction.ANNOUNCE, request.getAction());
	}

	@Test
	public void testEqualsHashcode() {
		TrackerRequest requestTwo = new TrackerRequest(tracker, 3, messageStub);
		TrackerRequest requestThree = new TrackerRequest(tracker, 7, messageStub);
		assertEqualsMethod(request);
		assertEquals("Requests with same transaction ids didn't match", request, requestThree);
		assertEquals("Requests with same transaction ids didn't match", request.hashCode(), requestThree.hashCode());
		assertNotEquals("Requests with different transaction ids mustn't match", request, requestTwo);
		assertNotEquals("Requests with different transaction ids mustn't match", request.hashCode(), requestTwo.hashCode());
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

		assertArrayEquals("Incorrect output", expectedOutput, outStream.toByteArray());
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

		assertTrue("Message wasn't processed", messageStub.processed);
	}

	@Test
	public void testReadIncorrectTransaction() throws Exception {
		thrown.expect(TrackerException.class);
		thrown.expectMessage(containsString("transaction id"));
		final byte[] input = {
				// Action
				0x00, 0x00, 0x00, 0x01,
				// Transaction ID
				0x00, 0x00, 0x00, 0x05,
				// Message Stub read
				0x01
		};

		InStream inStream = new InStream(input);
		request.readResponse(inStream);
	}

	@Test
	public void testReadTrackerError() throws Exception {
		thrown.expect(TrackerException.class);
		thrown.expectMessage(containsString("Stub"));
		final byte[] input = {
				// Action
				0x00, 0x00, 0x00, 0x03,
				// Error Message
				0x53, 0x74, 0x75, 0x62
		};

		InStream inStream = new InStream(input);
		request.readResponse(inStream);
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
			assertEquals("Incorrect byte read", 1, inStream.readByte());
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
