package net.sf.okapi.lib.preprocessing.filters.simplification;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterUtil;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestDoubleExtraction_HTML {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private FileLocation root;

	@Before
	public void startUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testDoubleExtraction_HTML() {
		SimplificationFilter filter = new SimplificationFilter();
		
		Parameters params =	filter.getParameters();
		params.setFilterConfigId("okf_html");
		params.setSimplifyResources(true);
		params.setSimplifyCodes(false);
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/aa324.html").toString(), null));
		list.add(new InputDocument(root.in("/form.html").toString(), null));
		list.add(new InputDocument(root.in("/W3CHTMHLTest1.html").toString(), null));
		list.add(new InputDocument(root.in("/msg00058.html").toString(), null));
		
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
	
	@Test
	public void testDoubleExtraction_DefaultConfig() {
		SimplificationFilter filter = (SimplificationFilter) FilterUtil.createFilter("okf_simplification");
		
		Parameters params =	filter.getParameters();
		params.setFilterConfigId("okf_html");
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/aa324.html").toString(), null));
		list.add(new InputDocument(root.in("/form.html").toString(), null));
		list.add(new InputDocument(root.in("/W3CHTMHLTest1.html").toString(), null));
		list.add(new InputDocument(root.in("/msg00058.html").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
	
	@Test
	public void testDoubleExtraction_ResourcesConfig() {
		SimplificationFilter filter = (SimplificationFilter) FilterUtil.createFilter("okf_simplification-xmlResources");
		
		Parameters params =	filter.getParameters();
		params.setFilterConfigId("okf_html");
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/aa324.html").toString(), null));
		list.add(new InputDocument(root.in("/form.html").toString(), null));
		list.add(new InputDocument(root.in("/W3CHTMHLTest1.html").toString(), null));
		list.add(new InputDocument(root.in("/msg00058.html").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
	
	@Test
	public void testDoubleExtraction_CodesConfig() {
		SimplificationFilter filter = (SimplificationFilter) FilterUtil.createFilter("okf_simplification-xmlCodes");
		
		Parameters params =	filter.getParameters();
		params.setFilterConfigId("okf_html");
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/aa324.html").toString(), null));
		list.add(new InputDocument(root.in("/form.html").toString(), null));
		list.add(new InputDocument(root.in("/W3CHTMHLTest1.html").toString(), null));
		list.add(new InputDocument(root.in("/msg00058.html").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
}
