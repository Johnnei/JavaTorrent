package org.johnnei.javatorrent.internal.disk;

import java.io.IOException;

import org.johnnei.javatorrent.disk.IDiskJob;

import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Tests {@link IOManager}
 */
public class IOManagerTest extends EasyMockSupport {

	@Test
	public void testMethodCallCompletes() {
		/*
		 * There's nothing in the manager so this call must return without invoking anything so can't cause exceptions.
		 */
		IOManager cut = new IOManager();

		cut.run();
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