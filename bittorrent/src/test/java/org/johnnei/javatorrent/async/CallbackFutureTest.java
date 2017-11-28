package org.johnnei.javatorrent.async;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CallbackFutureTest {

	private CallbackFuture<String> cut;

	private boolean consumerCalled;

	@Test
	public void testDone() throws Exception {
		consumerCalled = false;

		Consumer<FutureTask<String>> consumer = (f) -> {
			try {
				assertEquals("Called", f.get());
			} catch (Exception e) {
				throw new AssertionError("Consumer failed to get result of task", e);
			}
			consumerCalled = true;
		};

		Callable<String> callable = () -> "Called";

		cut = new CallbackFuture<>(callable, consumer);
		cut.run();

		assertTrue(consumerCalled, "Consumer didn't get called");
	}

}
