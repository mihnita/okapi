/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.filters.vignette;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VignetteFilterTest2 {
	
	private VignetteFilter filter;
	private FileLocation root;
	private LocaleId locENUS = LocaleId.fromString("en-us");
	private LocaleId locESES = LocaleId.fromString("es-es");

	@Before
	public void setUp() {
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		fcMapper.addConfigurations(VignetteFilter.class.getName());
		filter = (VignetteFilter) fcMapper.createFilter("okf_vignette-nocdata");
		filter.setFilterConfigurationMapper(fcMapper);
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void testDefaultInfo () {
		assertNotNull(filter.getParameters());
		assertNotNull(filter.getName());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size()>0);
	}
	
	@Test
	public void testStartDocument () {
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
			new InputDocument(root.in("/Test01.xml").toString(), null),
			"UTF-8", locENUS, locESES));
	}
	
	@Test
	public void testSimpleEntry () {
		String snippet = createSimpleDoc();
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, locENUS, locESES), 1);
		assertNotNull(tu);
		assertEquals("ENtext", tu.getSource().toString());
	}

	@Test
	public void testSimpleEntryOutput () {
		String snippet = createSimpleDoc();
		String expected = "<importProject>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ENtext&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<stuff/>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ENtext&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
		String result = generateOutput(getEvents(snippet, locENUS, locESES));
		assertEquals(expected, result);		
	}

	@Test
	public void testComplexEntry () {
		String snippet = createComplexDoc();
		// Order is driven by the targets
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet, locENUS, locESES), 1);
		assertNotNull(tu);
		assertEquals("EN-id1", tu.getSource().toString());
		tu = FilterTestDriver.getTextUnit(getEvents(snippet, locENUS, locESES), 2);
		assertNotNull(tu);
		assertEquals("EN-id2", tu.getSource().toString());
	}

	@Test
	public void testComplexEntryOutput () {
		String snippet = createComplexDoc();
		String expected = "<importProject>"
			// ES id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>"
			// ES id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
		String result = generateOutput(getEvents(snippet, locENUS, locESES));
		assertEquals(expected, result);		
	}

	@Test
	public void testDoubleExtraction () {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/Test01.xml").toString(), null));
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locENUS, locESES, "out"));
	}
	
	private String createSimpleDoc () {
		return "<importProject>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ES&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<stuff/>"
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>&lt;p&gt;ENtext&lt;/p&gt;</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
	}

	private String createComplexDoc () {
		return "<importProject>"
			// ES id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>ES-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>"
			// ES id2
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id2ES</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>ES-id2</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id2</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>es_ES</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			// EN id1
			+ "<importContentInstance><contentInstance>"
			+ "<attribute name=\"SMCCONTENT-CONTENT-ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"SMCCONTENT-BODY\"><valueCLOB>EN-id1</valueCLOB></attribute>"
			+ "<attribute name=\"SOURCE_ID\"><valueString>id1</valueString></attribute>"
			+ "<attribute name=\"LOCALE_ID\"><valueString>en_US</valueString></attribute>"
			+ "</contentInstance></importContentInstance>"
			+ "<importProject>";
	}

	private String generateOutput (List<Event> list) {
		IFilterWriter writer = filter.createFilterWriter();
		writer.setOptions(locESES, "UTF-8");
		ByteArrayOutputStream writerBuffer = new ByteArrayOutputStream();
		writer.setOutput(writerBuffer);
		for (Event event : list) {
			writer.handleEvent(event);
		}
		writer.close();
		return writerBuffer.toString();
	}
	
	private ArrayList<Event> getEvents (String snippet,
		LocaleId srcLang,
		LocaleId trgLang)
	{
		return FilterTestDriver.getEvents(filter, snippet, srcLang, trgLang);
	}

}
