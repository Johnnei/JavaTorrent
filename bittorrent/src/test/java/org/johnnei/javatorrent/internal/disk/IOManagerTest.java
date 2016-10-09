package org.johnnei.javatorrent.internal.disk;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.disk.IDiskJob;

import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static com.jayway.awaitility.Awaitility.await;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Tests {@link IOManager}
 */
public class IOManagerTest extends EasyMockSupport {

	@Test
	public void testAwaitTask() throws Exception {
		/*
		 * There's nothing in the manager so this call must return without invoking anything so can't cause exceptions.
		 */
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = createMock(IDiskJob.class);
		diskJobMock.process();

		ReentrantLock cutLock = Whitebox.getInternalState(cut, "lock");
		Condition cutCondition = Whitebox.getInternalState(cut, "newTaskEvent");

		replayAll();

		Thread thread = new Thread(cut);
		thread.start();

		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			cutLock.lock();
			try {
				cutLock.hasWaiters(cutCondition);
			} finally {
				cutLock.unlock();
			}
		});

		cut.addTask(diskJobMock);
		thread.join(5000);

		verifyAll();
	}

	@Test
	public void testAwaitTaskInterrupt() throws Exception {
		/*
		 * There's nothing in the manager so this call must return without invoking anything so can't cause exceptions.
		 */
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = createMock(IDiskJob.class);
		diskJobMock.process();

		ReentrantLock cutLock = Whitebox.getInternalState(cut, "lock");
		Condition cutCondition = Whitebox.getInternalState(cut, "newTaskEvent");

		replayAll();

		Thread thread = new Thread(cut);
		thread.start();

		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			cutLock.lock();
			try {
				cutLock.hasWaiters(cutCondition);
			} finally {
				cutLock.unlock();
			}
		});

		thread.interrupt();

		await().until(() -> !thread.isAlive());
	}

	@Test
	public void testOnSucces() throws Exception {
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = createMock(IDiskJob.class);

		expect(diskJobMock.getPriority()).andStubReturn(5);
		diskJobMock.process();

		replayAll();

		cut.addTask(diskJobMock);
		cut.run();

		verifyAll();
	}

	@Test
	public void testFailOnFirstProcess() throws Exception {
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = createMock(IDiskJob.class);

		expect(diskJobMock.getPriority()).andStubReturn(5);
		diskJobMock.process();
		expectLastCall().andThrow(new IOException("Stubbed IO Exception"));
		diskJobMock.process();

		replayAll();

		cut.addTask(diskJobMock);
		cut.run();

		verifyAll();
	}

	@Test
	public void testHonorPriority() throws IOException {
		IOManager cut = new IOManager();

		IMocksControl mocksControl = createStrictControl();
		IDiskJob diskJobOneMock = mocksControl.createMock("JobOne", IDiskJob.class);
		IDiskJob diskJobTwoMock = mocksControl.createMock("JobTwo", IDiskJob.class);
		expect(diskJobOneMock.getPriority()).andStubReturn(15);
		expect(diskJobTwoMock.getPriority()).andStubReturn(5);

		diskJobTwoMock.process();
		diskJobOneMock.process();
		diskJobTwoMock.process();
		diskJobOneMock.process();

		replayAll();

		// Test twice with the order swapped to ensure that the ordering isn't accidental
		cut.addTask(diskJobOneMock);
		cut.addTask(diskJobTwoMock);
		cut.run();

		cut.addTask(diskJobTwoMock);
		cut.addTask(diskJobOneMock);
		cut.run();

		verifyAll();
	}
}