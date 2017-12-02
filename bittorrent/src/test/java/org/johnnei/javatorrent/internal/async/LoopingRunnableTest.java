package org.johnnei.javatorrent.internal.async;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.async.LoopingRunnable;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests {@link LoopingRunnable}
 */
public class LoopingRunnableTest {

	private CountDownLatch countDownLatch;

	@BeforeEach
	public void setUp() {
		countDownLatch = new CountDownLatch(1);
	}

	@Test
	public void testStoppable() throws Exception {
		LoopingRunnable runnable = new LoopingRunnable(() -> countDownLatch.countDown());
		Thread thread = new Thread(runnable);
		thread.setDaemon(true);
		thread.start();

		if (!countDownLatch.await(5, TimeUnit.SECONDS)) {
			fail("Runnable didn't run at least once in 5 seconds");
		}
		runnable.stop();
		thread.join(5000);

		if (thread.isAlive()) {
			fail("Thread didn't stop");
		}
	}
}
