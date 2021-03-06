/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openoffice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.filters.RoundTripComparison;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ODFFilterTest {

	private ODFFilter filter;
	private FileLocation root = FileLocation.fromClass(getClass());
	private LocaleId locEN = LocaleId.fromString("en");
	private GenericContent fmt;

	@Before
	public void setUp() {
		filter = new ODFFilter();
		fmt = new GenericContent();
	}

	@Test
	public void testFirstTextUnit () {
		ITextUnit tu = FilterTestDriver.getTextUnit(filter,
			new InputDocument(root.in("/TestDocument01.odt_content.xml").toString(), null),
			"UTF-8", locEN, locEN, 1);
		assertNotNull(tu);
		assertEquals("Heading 1", tu.getSource().toString());
	}

	@Test
	public void testITSMarkup () {
		List<Event> events = getEvents(root.in("/Content_WithITS.xml").asFile(), LocaleId.ENGLISH);

		// With translate='no'
		ITextUnit tu = FilterTestDriver.getTextUnit(events, 2);
		assertEquals("To translate <1>NOT to translate <2>but translate this</2></1>.",
			fmt.setContent(tu.getSource().getFirstContent()).toString());
		List<Code> codes = tu.getSource().getFirstContent().getCodes();
		// Don't translate (code 0 is start tag
		assertEquals(false, codes.get(0).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.TRANSLATE)
			.getBoolean(GenericAnnotationType.TRANSLATE_VALUE));
		// Translate (code 1 is start tag
		assertEquals(true, codes.get(1).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.TRANSLATE)
			.getBoolean(GenericAnnotationType.TRANSLATE_VALUE));

		// Localization notes
		tu = FilterTestDriver.getTextUnit(events, 3);
		assertEquals("Text with <1>a note</1>. More text <2>with a note <3>and another note</3></2>.",
			fmt.setContent(tu.getSource().getFirstContent()).toString());
		codes = tu.getSource().getFirstContent().getCodes();
		// First note (code 0 is start tag)
		assertEquals("Localization Note", codes.get(0).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCNOTE)
			.getString(GenericAnnotationType.LOCNOTE_VALUE));
		assertEquals("description", codes.get(0).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCNOTE)
			.getString(GenericAnnotationType.LOCNOTE_TYPE));
		// Second note (code 2 is start tag)
		assertEquals("Localization Node Outer", codes.get(2).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCNOTE)
			.getString(GenericAnnotationType.LOCNOTE_VALUE));
		assertEquals("alert", codes.get(2).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCNOTE)
			.getString(GenericAnnotationType.LOCNOTE_TYPE));
		// Third note (code 3 is start tag)
		assertEquals("Localization Node Inner", codes.get(3).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCNOTE)
			.getString(GenericAnnotationType.LOCNOTE_VALUE));
		assertEquals("description", codes.get(3).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCNOTE)
			.getString(GenericAnnotationType.LOCNOTE_TYPE));

		// Terminology + translate
		tu = FilterTestDriver.getTextUnit(events, 5);
		codes = tu.getSource().getFirstContent().getCodes();
		assertEquals("Text with <1>a very long term <2>made of several words</2></1> and more text.",
			fmt.setContent(tu.getSource().getFirstContent()).toString());
		// Term (code 0 is start tag)
		assertNotNull(codes.get(0).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.TERM));
		assertEquals(0.8, codes.get(0).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.TERM)
			.getDouble(GenericAnnotationType.TERM_CONFIDENCE), 0.0);
		assertEquals("abc", codes.get(0).getGenericAnnotationString(
			GenericAnnotationType.TERM, GenericAnnotationType.ANNOTATORREF));
		// Translate (code 1 is start tag)
		assertEquals(false, codes.get(1).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.TRANSLATE)
			.getBoolean(GenericAnnotationType.TRANSLATE_VALUE));

		// Locale filter
		tu = FilterTestDriver.getTextUnit(events, 6);
		codes = tu.getSource().getFirstContent().getCodes();
		assertEquals("Locale filter: <1>for FR</1> and <2>Not for FR</2>.",
			fmt.setContent(tu.getSource().getFirstContent()).toString());
		// Applies to fr (code 0 is start tag)
		assertEquals("fr", codes.get(0).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCFILTER)
			.getString(GenericAnnotationType.LOCFILTER_VALUE));
		// Applies to fr (code 2 is start tag)
		assertEquals("!fr", codes.get(2).getGenericAnnotations().getFirstAnnotation(GenericAnnotationType.LOCFILTER)
			.getString(GenericAnnotationType.LOCFILTER_VALUE));
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
	public void testDoubleExtraction () {
		ArrayList<InputDocument> list = new ArrayList<>();
		list.add(new InputDocument(root.in("/TestDocument01.odt_content.xml").toString(), null));
		list.add(new InputDocument(root.in("/TestDocument01.odt_meta.xml").toString(), null));
		list.add(new InputDocument(root.in("/TestDocument01.odt_styles.xml").toString(), null));
		list.add(new InputDocument(root.in("/TestDocument02.odt_content.xml").toString(), null));
		list.add(new InputDocument(root.in("/ODFTest_footnote.xml").toString(), null));
		list.add(new InputDocument(root.in("/TestSpreadsheet01.ods_content.xml").toString(), null));

		RoundTripComparison rtc = new RoundTripComparison();
		assertTrue(rtc.executeCompare(filter, list, "UTF-8", locEN, locEN));
	}

	private ArrayList<Event> getEvents(File file,
		LocaleId srcLang)
	{
		return FilterTestDriver.getEvents(filter, new RawDocument(file.toURI(), "UTF-8", srcLang), null);
	}

}
