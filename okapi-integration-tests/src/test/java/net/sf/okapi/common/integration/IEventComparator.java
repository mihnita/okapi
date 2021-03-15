package net.sf.okapi.common.integration;

import java.util.List;

import net.sf.okapi.common.Event;

public interface IEventComparator {
	boolean compare(List<Event> actual, List<Event> expected);
}
