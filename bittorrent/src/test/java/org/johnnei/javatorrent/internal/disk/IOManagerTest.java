package org.johnnei.javatorrent.internal.disk;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.disk.IDiskJob;

import static com.jayway.awaitility.Awaitility.await;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link IOManager}
 */
public class IOManagerTest {

	@Test
	public void testAwaitTask() throws Exception {
		/*
		 * There's nothing in the manager so this call must return without invoking anything so can't cause exceptions.
		 */
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = mock(IDiskJob.class);
		diskJobMock.process();

		ReentrantLock cutLock = Whitebox.getInternalState(cut, "lock");
		Condition cutCondition = Whitebox.getInternalState(cut, "newTaskEvent");

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
	}

	@Test
	public void testAwaitTaskInterrupt() throws Exception {
		IOManager cut = new IOManager();

		ReentrantLock cutLock = Whitebox.getInternalState(cut, "lock");
		Condition cutCondition = Whitebox.getInternalState(cut, "newTaskEvent");

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

		await("Worker to stop on interrupt.").until(() -> !thread.isAlive());
	}

	@Test
	public void testOnSucces() throws Exception {
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = mock(IDiskJob.class);

		when(diskJobMock.getPriority()).thenReturn(5);

		cut.addTask(diskJobMock);
		cut.run();

		verify(diskJobMock).process();
	}

	@Test
	public void testFailOnFirstProcess() throws Exception {
		IOManager cut = new IOManager();

		IDiskJob diskJobMock = mock(IDiskJob.class);

		when(diskJobMock.getPriority()).thenReturn(5);
		doThrow(new IOException("Stubbed IO Exception")).doNothing().when(diskJobMock).process();

		cut.addTask(diskJobMock);
		cut.run();

		verify(diskJobMock, times(2)).process();
	}

	@Test
	public void testHonorPriority() throws IOException {
		IOManager cut = new IOManager();

		IDiskJob diskJobOneMock = mock(IDiskJob.class, "JobOne");
		IDiskJob diskJobTwoMock = mock(IDiskJob.class, "JobTwo");
		when(diskJobOneMock.getPriority()).thenReturn(15);
		when(diskJobTwoMock.getPriority()).thenReturn(5);

		// Test twice with the order swapped to ensure that the ordering isn't accidental
		cut.addTask(diskJobOneMock);
		cut.addTask(diskJobTwoMock);
		cut.run();

		cut.addTask(diskJobTwoMock);
		cut.addTask(diskJobOneMock);
		cut.run();

		InOrder ordered = inOrder(diskJobOneMock, diskJobTwoMock);
		ordered.verify(diskJobTwoMock).process();
		ordered.verify(diskJobOneMock).process();
		ordered.verify(diskJobTwoMock).process();
		ordered.verify(diskJobOneMock).process();
		ordered.verifyNoMoreInteractions();
	}
}
