package net.sf.okapi.common.pipeline.integration;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongRunningStep extends BasePipelineStep {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	@Override
	public String getName() {
		return "Long Running Step";
	}

	@Override
	public String getDescription() {
		return "Simple step that runs for a long time";
	}

	@Override
	protected Event handleTextUnit(Event event) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
        LOGGER.trace("Long running step handled a text unit");
        return event;
	}
}
