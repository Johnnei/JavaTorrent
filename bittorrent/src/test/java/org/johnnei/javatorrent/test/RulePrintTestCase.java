package org.johnnei.javatorrent.test;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulePrintTestCase extends TestWatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(RulePrintTestCase.class);

	private LocalDateTime startedTime;

	@Override
	protected void starting(Description description) {
		LOGGER.info("[START OF TEST] {}.", description.getMethodName());
		startedTime = LocalDateTime.now();
		super.starting(description);
	}

	@Override
	protected void finished(Description description) {
		LOGGER.info("[END OF TEST] {}. Duration: {}.", description.getMethodName(), Duration.between(startedTime, LocalDateTime.now()));
		super.finished(description);
	}

}
