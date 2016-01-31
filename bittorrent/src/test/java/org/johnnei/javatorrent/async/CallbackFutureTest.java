package org.johnnei.javatorrent.async;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

public class CallbackFutureTest {

	private CallbackFuture<String> cut;

	private boolean consumerCalled;

	@Test
	public void testDone() throws Exception {
		final String result = "Called";
		consumerCalled = false;

		Consumer<FutureTask<String>> consumer = (f) -> {
			try {
				Assert.assertEquals("Expected result", f.get(), result);
			} catch (Exception e) {
				throw new AssertionError("Consumer failed to get result of task", e);
			}
			consumerCalled = true;
		};

		Callable<String> callable = () -> {
			return "Called";
		};

		cut = new CallbackFuture<>(callable, consumer);
		cut.run();

		Assert.assertTrue("Consumer didn't get called", consumerCalled);
	}

}
