/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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

package net.sf.okapi.lib.merge.merge;

import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.lib.merge.step.OriginalDocumentXliffMergerStep;
import net.sf.okapi.steps.common.RawDocumentWriterStep;

@RunWith(JUnit4.class)
public class OriginalDocumentXliffMergerStepTest {
	private HtmlFilter htmlFilter;
	private FileLocation root;
	private OriginalDocumentXliffMergerStep merger;
	private RawDocumentWriterStep writer;

	@Before
	public void setUp() {
		htmlFilter = new HtmlFilter();
		merger = new OriginalDocumentXliffMergerStep();
		writer = new RawDocumentWriterStep();
		root = FileLocation.fromClass(getClass());
	}

	@After
	public void tearDown() {
		htmlFilter.close();
		merger.destroy();
		writer.destroy();
	}

	@SuppressWarnings("resource")
	@Test
	public void simpleMerge() {
		String input = "/simple.html";
		// Serialize the source file
		MergerUtil.writeXliffAndSkeleton(FilterTestDriver.getEvents(
					htmlFilter, 
					new RawDocument(root.in(input).asInputStream(), "UTF-8", LocaleId.ENGLISH), null), 
				root.out("").toString(), root.out(input+".xlf").toString());

		IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
		DefaultFilters.setMappings(fcm, true, true);
		merger.setFilterConfigurationMapper(fcm);
		merger.setOutputEncoding("UTF-8");
		RawDocument rd = new RawDocument(root.in(input).asInputStream(),"UTF-8", LocaleId.ENGLISH);
		rd.setFilterConfigId("okf_html");
		merger.setSecondInput(rd);
		List<LocaleId> ts = new LinkedList<>();
		ts.add(LocaleId.FRENCH);
		merger.setTargetLocales(ts);
		URI tempXlf = root.out(input+".xlf").asUri();
		Event e = merger.handleEvent(new Event(EventType.RAW_DOCUMENT, 
						new RawDocument(tempXlf, "UTF-8", LocaleId.ENGLISH, LocaleId.ENGLISH)));
		
		writer.setOutputURI(root.out(input+".merged").asUri());
		writer.handleEvent(e);
		writer.destroy();

		URI tempMerged = root.out(input+".merged").asUri();
		RawDocument ord = new RawDocument(root.in(input).asInputStream(), "UTF-8", LocaleId.ENGLISH);
		RawDocument trd = new RawDocument(tempMerged, "UTF-8", LocaleId.ENGLISH);
		List<Event> o = MergerUtil.getTextUnitEvents(htmlFilter, ord);
		List<Event> t = MergerUtil.getTextUnitEvents(htmlFilter, trd);
		assertTrue(o.size() == t.size());
		assertTrue(FilterTestDriver.compareEvents(o, t, false));
	}
}
