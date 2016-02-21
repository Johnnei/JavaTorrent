package org.johnnei.javatorrent.torrent.peer;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Client}
 */
public class ClientTest {

	@Test
	public void testChoke() {
		Client cut = new Client();

		cut.choke();
		assertTrue("Client is unchoked after choke call", cut.isChoked());

		cut.unchoke();
		assertFalse("Client is choked after unchoke call", cut.isChoked());
	}

	@Test
	public void testInterested() {
		Client cut = new Client();

		cut.interested();
		assertTrue("Client is uninterested after interested call", cut.isInterested());

		cut.uninterested();
		assertFalse("Client is interested after uninterested call", cut.isInterested());
	}

	@Test
	public void testAddJob() {
		Client cut = new Client();

		cut.addJob(new Job(1, 2, 3));

		assertEquals("Job did not get added", 1, cut.getQueueSize());
	}

	@Test
	public void testGetNextJob() {
		Client cut = new Client();
		Job job = new Job(1, 2, 3);

		cut.addJob(job);
		Job returnedJob = cut.popNextJob();

		assertEquals("Incorrect job got returned", job, returnedJob);
		assertEquals("Job did not get removed", 0, cut.getQueueSize());
	}

	@Test
	public void testRemoveJob() {
		Client cut = new Client();
		Job jobOne = new Job(1, 2, 3);
		Job jobTwo = new Job(2, 3, 4);

		cut.addJob(jobOne);
		cut.addJob(jobTwo);

		assertEquals("Incorrect amount of jobs before remove", 2, cut.getQueueSize());

		cut.removeJob(jobTwo);
		assertEquals("Incorrect amount of jobs after remove", 1, cut.getQueueSize());
		Job returnedJob = cut.popNextJob();

		assertEquals("Incorrect job got returned", jobOne, returnedJob);
	}

	@Test
	public void testClearJobs() {
		Client cut = new Client();

		cut.addJob(new Job(1, 2, 3));
		cut.addJob(new Job(2, 3, 4));

		assertEquals("Incorrect amount of jobs before clear", 2, cut.getQueueSize());

		cut.clearJobs();

		assertEquals("Incorrect amount of jobs after clear", 0, cut.getQueueSize());
	}

	@Test
	public void testGetJobs() {
		Client cut = new Client();
		Job jobOne = new Job(1, 2, 3);
		Job jobTwo = new Job(2, 3, 4);

		Collection<Job> jobs = new ArrayList<>();
		jobs.add(jobOne);
		jobs.add(jobTwo);

		cut.addJob(jobOne);
		cut.addJob(jobTwo);

		assertEquals("Incorrect amount of jobs before getting all", 2, cut.getQueueSize());

		for (Job job : cut.getJobs()) {
			assertTrue("Incorrect job returned", jobs.contains(job));
			jobs.remove(job);
		}
	}

}