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

package net.sf.okapi.filters.dtd;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class DTDFilterTest {

	private DTDFilter filter;
	private final LocaleId locEN = LocaleId.fromString("en");
	private FileLocation root;

	@Before
	public void setUp() {
		filter = new DTDFilter();
		root = FileLocation.fromClass(getClass());
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
			new InputDocument(root.in("/Test01.dtd").toString(), null),
			"UTF-8", locEN, locEN));
	}
	
	@Test
	public void testSimpleEntry () {
		String snippet = "<!--Comment-->\n<!ENTITY entry1 \"Text1\"><!ENTITY test2 \"text2\">";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("Text1", tu.getSource().toString());
		assertEquals("entry1", tu.getName());
		assertNotNull(tu.getAnnotation(NoteAnnotation.class));
		NoteAnnotation note = tu.getAnnotation(NoteAnnotation.class);
		assertEquals("Comment", note.getNote(0).getNoteText());
	}
	
	@Test
	public void testLineBreaks () {
		String snippet = "<!--Comment-->\r<!ENTITY entry1 \"Text1\">\r";
		StartDocument sd = FilterTestDriver.getStartDocument(getEvents(snippet));
		assertNotNull(sd);
		assertEquals("\r", sd.getLineBreak());
	}

	@Test
	public void testEntryWithEnitties () {
		String snippet = "<!ENTITY entry1 \"&ent1;=ent1, %pent1;=pent1\">";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		List<Code> codes = tu.getSource().getFirstContent().getCodes();
		assertEquals(2, codes.size());
		assertEquals("&ent1;", codes.get(0).getData());
		assertEquals("%pent1;", codes.get(1).getData());
	}
	
	@Test
	public void testEntryWithNCRs () {
		String snippet = "<!ENTITY entry1 \"&#xe3;, &#xE3;, &#227;\">";
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet), 1);
		assertNotNull(tu);
		assertEquals("\u00e3, \u00e3, \u00e3", tu.getSource().toString());
	}
	
//	@Test
//	public void testLineBreaks () {
//		String snippet = "<!--Comment-->\r<!ENTITY e1 \"t1\">\r<!ENTITY e2 \"t2\">\r";
//TODO		assertEquals(snippet, FilterTestDriver.generateOutput(getEvents(snippet), locEN));
//	}
	
	@Test
	public void testDoubleExtraction () {
		// Read all files in the data directory
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/Test01.dtd").toString(), null));
		list.add(new InputDocument(root.in("/Test02.dtd").toString(), null));
		
		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));
	}

	private ArrayList<Event> getEvents(String snippet) {
		return FilterTestDriver.getEvents(filter, snippet, locEN);
	}

}
