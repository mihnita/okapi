package net.sf.okapi.common.filters;

import static org.junit.Assert.assertEquals;

import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.RawDocument;

@RunWith(JUnit4.class)
public class FilterIterableTest {
	private static final String EXPECTED = ""
		+ "Start Document"
		+ "Text Unit"
		+ "Text Unit"
		+ "End Document";
	private static final String EXPECTED_TEXTUNITS = ""
		+ "Text Unit"
		+ "Text Unit";
	private DummyFilter filter;
	private RawDocument input;

	@Before
	public void setUp() {
		filter = new DummyFilter();
		input = new RawDocument("##def##", LocaleId.ENGLISH, LocaleId.FRENCH);
		filter.open(input);
	}

	@After
	public void tearDown() {
		input.close();
		filter.close();
	}

	@Test
	public void testWhileIteration() {
		final StringBuilder result = new StringBuilder();
		while (filter.hasNext()) {
			final Event event = filter.next();
			result.append(event.toString());
		}
		assertEquals(EXPECTED, result.toString());
	}

	@Test
	public void testForIteration() {
		final StringBuilder result = new StringBuilder();
		for (Event event : new FilterIterable(filter)) {
			result.append(event.toString());
		}
		assertEquals(EXPECTED, result.toString());
	}

	@Test
	public void testLambdaExpression() {
		final StringBuffer result = new StringBuffer();
		new FilterIterable(filter).forEach(result::append);
		assertEquals(EXPECTED, result.toString());
	}

	@Test
	public void testMethodReference() {
		final StringBuilder result = new StringBuilder();
		new FilterIterable(filter).forEach(result::append);
		assertEquals(EXPECTED, result.toString());
	}

	@Test
	public void testStream() {
		final String result = filter.stream()
				.filter(Event::isTextUnit)
				.map(Event::toString)
				.collect(Collectors.joining());
		assertEquals(EXPECTED_TEXTUNITS, result.toString());
	}
}
