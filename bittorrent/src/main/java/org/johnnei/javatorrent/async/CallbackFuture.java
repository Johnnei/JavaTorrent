package org.johnnei.javatorrent.async;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

public class CallbackFuture<V> extends FutureTask<V> {

	private final Consumer<FutureTask<V>> callback;

	public CallbackFuture(Callable<V> callable, Consumer<FutureTask<V>> callback) {
		super(callable);
		this.callback = callback;
	}

	@Override
	protected void done() {
		callback.accept(this);
	}

}
