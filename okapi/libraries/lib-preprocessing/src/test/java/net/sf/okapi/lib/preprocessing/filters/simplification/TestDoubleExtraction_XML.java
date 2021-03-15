package net.sf.okapi.lib.preprocessing.filters.simplification;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterUtil;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestDoubleExtraction_XML {

	private static final LocaleId ENUS = new LocaleId("en", "us");
	private FileLocation root;

	@Before
	public void startUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testDoubleExtraction_XML() {
		SimplificationFilter filter = new SimplificationFilter();
		
		Parameters params =	filter.getParameters();
		params.setSimplifyResources(true);
		params.setSimplifyCodes(false);
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/about.xml").toString(), null));
		list.add(new InputDocument(root.in("/failure.xml").toString(), null));
		list.add(new InputDocument(root.in("/PI-Problem.xml").toString(), null));
		list.add(new InputDocument(root.in("/simple_cdata.xml").toString(), null));
		list.add(new InputDocument(root.in("/subfilter-simple.xml").toString(), null));
		list.add(new InputDocument(root.in("/success.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_drive.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_href_reference.xml").toString(), null));
		list.add(new InputDocument(root.in("/translate-attr-subfilter.xml").toString(), null));
		list.add(new InputDocument(root.in("/xml-freemarker.xml").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
	
	@Test
	public void testDoubleExtraction_DefaultConfig() {
		IFilter filter = FilterUtil.createFilter("okf_simplification");
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/about.xml").toString(), null));
		list.add(new InputDocument(root.in("/failure.xml").toString(), null));
		list.add(new InputDocument(root.in("/PI-Problem.xml").toString(), null));
		list.add(new InputDocument(root.in("/simple_cdata.xml").toString(), null));
		list.add(new InputDocument(root.in("/subfilter-simple.xml").toString(), null));
		list.add(new InputDocument(root.in("/success.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_drive.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_href_reference.xml").toString(), null));
		list.add(new InputDocument(root.in("/translate-attr-subfilter.xml").toString(), null));
		list.add(new InputDocument(root.in("/xml-freemarker.xml").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
	
	@Test
	public void testDoubleExtraction_ResourcesConfig() {
		IFilter filter = FilterUtil.createFilter("okf_simplification-xmlResources");
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/about.xml").toString(), null));
		list.add(new InputDocument(root.in("/failure.xml").toString(), null));
		list.add(new InputDocument(root.in("/PI-Problem.xml").toString(), null));
		list.add(new InputDocument(root.in("/simple_cdata.xml").toString(), null));
		list.add(new InputDocument(root.in("/subfilter-simple.xml").toString(), null));
		list.add(new InputDocument(root.in("/success.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_drive.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_href_reference.xml").toString(), null));
		list.add(new InputDocument(root.in("/translate-attr-subfilter.xml").toString(), null));
		list.add(new InputDocument(root.in("/xml-freemarker.xml").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
	
	@Test
	public void testDoubleExtraction_CodesConfig() {
		IFilter filter = FilterUtil.createFilter("okf_simplification-xmlCodes");
		
		ArrayList<InputDocument> list = new ArrayList<>();

		list.add(new InputDocument(root.in("/about.xml").toString(), null));
		list.add(new InputDocument(root.in("/failure.xml").toString(), null));
		list.add(new InputDocument(root.in("/PI-Problem.xml").toString(), null));
		list.add(new InputDocument(root.in("/simple_cdata.xml").toString(), null));
		list.add(new InputDocument(root.in("/subfilter-simple.xml").toString(), null));
		list.add(new InputDocument(root.in("/success.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_drive.xml").toString(), null));
		list.add(new InputDocument(root.in("/test_href_reference.xml").toString(), null));
		list.add(new InputDocument(root.in("/translate-attr-subfilter.xml").toString(), null));
		list.add(new InputDocument(root.in("/xml-freemarker.xml").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", ENUS, ENUS, "out"));
	}
}
