package org.johnnei.javatorrent.internal.torrent.peer;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.files.Piece;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Client}
 */
public class ClientTest {

	@Test
	public void testChoke() {
		Client cut = new Client();

		cut.choke();
		assertTrue(cut.isChoked(), "Client is unchoked after choke call");

		cut.unchoke();
		assertFalse(cut.isChoked(), "Client is choked after unchoke call");
	}

	@Test
	public void testInterested() {
		Client cut = new Client();

		cut.interested();
		assertTrue(cut.isInterested(), "Client is uninterested after interested call");

		cut.uninterested();
		assertFalse(cut.isInterested(), "Client is interested after uninterested call");
	}

	@Test
	public void testAddJob() {
		Client cut = new Client();

		Piece pieceMock = mock(Piece.class);
		when(pieceMock.getIndex()).thenReturn(1);

		cut.addJob(new Job(pieceMock, 2, 3));

		assertEquals(1, cut.getQueueSize(), "Job did not get added");
	}

	@Test
	public void testGetNextJob() {
		Piece pieceMock = mock(Piece.class);
		when(pieceMock.getIndex()).thenReturn(1);

		Client cut = new Client();
		Job job = new Job(pieceMock, 2, 3);

		cut.addJob(job);
		Job returnedJob = cut.popNextJob();

		assertEquals(job, returnedJob, "Incorrect job got returned");
		assertEquals(0, cut.getQueueSize(), "Job did not get removed");
	}

	@Test
	public void testRemoveJob() {
		Piece pieceMockOne = mock(Piece.class);
		Piece pieceMockTwo = mock(Piece.class);
		when(pieceMockOne.getIndex()).thenReturn(1);
		when(pieceMockTwo.getIndex()).thenReturn(2);

		Client cut = new Client();
		Job jobOne = new Job(pieceMockOne, 2, 3);
		Job jobTwo = new Job(pieceMockTwo, 3, 4);

		cut.addJob(jobOne);
		cut.addJob(jobTwo);

		assertEquals(2, cut.getQueueSize(), "Incorrect amount of jobs before remove");

		cut.removeJob(jobTwo);
		assertEquals(1, cut.getQueueSize(), "Incorrect amount of jobs after remove");
		Job returnedJob = cut.popNextJob();

		assertEquals(jobOne, returnedJob, "Incorrect job got returned");
	}

	@Test
	public void testClearJobs() {
		Piece pieceMockOne = mock(Piece.class);
		Piece pieceMockTwo = mock(Piece.class);
		when(pieceMockOne.getIndex()).thenReturn(1);
		when(pieceMockTwo.getIndex()).thenReturn(2);

		Client cut = new Client();

		cut.addJob(new Job(pieceMockOne, 2, 3));
		cut.addJob(new Job(pieceMockTwo, 3, 4));

		assertEquals(2, cut.getQueueSize(), "Incorrect amount of jobs before clear");

		cut.clearJobs();

		assertEquals(0, cut.getQueueSize(), "Incorrect amount of jobs after clear");
	}

	@Test
	public void testGetJobs() {
		Piece pieceMockOne = mock(Piece.class);
		Piece pieceMockTwo = mock(Piece.class);
		when(pieceMockOne.getIndex()).thenReturn(1);
		when(pieceMockTwo.getIndex()).thenReturn(2);

		Client cut = new Client();
		Job jobOne = new Job(pieceMockOne, 2, 3);
		Job jobTwo = new Job(pieceMockTwo, 3, 4);

		Collection<Job> jobs = new ArrayList<>();
		jobs.add(jobOne);
		jobs.add(jobTwo);

		cut.addJob(jobOne);
		cut.addJob(jobTwo);

		assertEquals(2, cut.getQueueSize(), "Incorrect amount of jobs before getting all");

		for (Job job : cut.getJobs()) {
			assertTrue(jobs.contains(job), "Incorrect job returned");
			jobs.remove(job);
		}
	}

}
