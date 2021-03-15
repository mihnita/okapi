/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.multiparsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;

@RunWith(JUnit4.class)
public class MultiparsersFilterTests {

	private FilterConfigurationMapper fcMapper;
	
	private final LocaleId locENUS = LocaleId.fromString("en-US");
	private final LocaleId locFRCA = LocaleId.fromString("fr-CA");
	
	private FileLocation root;
	
	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
		fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations("net.sf.okapi.filters.markdown.MarkdownFilter");
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
	}

	@Test
	public void testSimpleRead () {
		try ( MultiParsersFilter filter = new MultiParsersFilter() ) {
			filter.setFilterConfigurationMapper(fcMapper);
			// Set parameters
			Parameters prms = filter.getParameters();
			prms.setCsvNoExtractCols("0,3");
			prms.setCsvFormatCols("2:okf_html,5:okf_markdown");
			prms.setCsvStartingRow(2);
			
			// process
			URI uri = root.in("/test01.csv").asUri();
			RawDocument rd = new RawDocument(uri, "UTF-8", locENUS, locFRCA);
			filter.open(rd);
			int count = 0;
			while ( filter.hasNext() ) {
				Event event = filter.next();
				switch ( event.getEventType() ) {
				case START_SUBDOCUMENT:
					break;
				case TEXT_UNIT:
					ITextUnit tu = event.getTextUnit();
					switch ( count ) {
					// Extraction starts at record 2
					case 0: assertEquals("ent1-2", tu.getSource().getCodedText()); break;
					case 1: assertEquals("ent2-2", tu.getSource().getCodedText()); break;
					case 2: assertEquals("ent3-2", tu.getSource().getCodedText()); break;
					case 3: assertEquals("ent4-2", tu.getSource().getCodedText()); break;
					case 4: assertEquals("ent1-3", tu.getSource().getCodedText()); break;
					case 5: assertEquals("ent2-3", tu.getSource().getCodedText()); break;
					case 6: assertEquals("ent3-3", tu.getSource().getCodedText()); break;
					case 7: assertEquals("ent4-3", tu.getSource().getCodedText()); break;
					default:
						throw new RuntimeException("Unexpected entry");
					}
					count++;
				default:
					break;
				}
			}
			assertEquals("Unexpected number of TUs", 8, count);
		}
	}

	@Test
	public void testSubFilterContent () {
		try ( MultiParsersFilter filter = new MultiParsersFilter() ) {
			filter.setFilterConfigurationMapper(fcMapper);
			// Set parameters
			Parameters prms = filter.getParameters();
			prms.setCsvNoExtractCols("0,3");
			prms.setCsvFormatCols("2:okf_html,5:okf_markdown");

			ITextUnit tu = null;
			URI uri = root.in("/test02.csv").asUri();
			RawDocument rd = new RawDocument(uri, "UTF-8", locENUS, locFRCA);
			filter.open(rd);
			while ( filter.hasNext() ) {
				Event event = filter.next();
				switch ( event.getEventType() ) {
				case TEXT_UNIT:
					tu = event.getTextUnit();
					if ( tu.getId().equals("tu4_sf2_tu1") ) {
						TextFragment tf = tu.getSource().getFirstContent();
						String tmp = GenericContent.fromFragmentToLetterCoded(tf, true);
						assertEquals("Text <g1>text</g1><x3/> + <g2>text</g2><x4/> text.", tmp);
					}
					break;
				default:
					break;
				}
			}
			// Check last entry
			assertNotNull(tu);
			assertEquals("last-Body", tu.getSource().getFirstContent().getCodedText());
		}
	}

	@Test
	public void testReadWrite () {
		IFilterWriter writer = null;
		File out = root.in("/test02.out.csv").asFile();
		out.delete();
		assertFalse(out.exists());

		try ( MultiParsersFilter filter = new MultiParsersFilter() ) {
			filter.setFilterConfigurationMapper(fcMapper);
			// Set parameters
			Parameters prms = filter.getParameters();
			prms.setCsvNoExtractCols("0,3");
			prms.setCsvFormatCols("2:okf_html,5:okf_markdown");
			// Prepare writer
			writer = filter.createFilterWriter();
			writer.setOptions(locFRCA, "UTF-8");
			writer.setOutput(out.getAbsolutePath());

			URI uri = root.in("/test02.csv").asUri();
			RawDocument rd = new RawDocument(uri, "UTF-8", locENUS, locFRCA);
			filter.open(rd);
			
			ITextUnit tu = null;
			while ( filter.hasNext() ) {
				Event event = filter.next();
				if ( event.isTextUnit() ) {
					tu = event.getTextUnit();
					TextContainer tc = tu.createTarget(locFRCA, true, IResource.COPY_ALL);
					ISegments segs = tc.getSegments();
					for ( Segment seg : segs ) {
						TextFragment tf = seg.getContent();
						tf.setCodedText(tf.getCodedText().toUpperCase());
					}
				}
				writer.handleEvent(event);
			}
		}
		assertTrue(out.exists());
	}

	@Test
	public void testTwoSubFilterContent () {
		try ( MultiParsersFilter filter = new MultiParsersFilter() ) {
			filter.setFilterConfigurationMapper(fcMapper);
			// Set parameters
			Parameters prms = filter.getParameters();
			prms.setCsvFormatCols("0:okf_markdown,1:okf_html");

			ITextUnit tu = null;
			URI uri = root.in("/test03.csv").asUri();
			RawDocument rd = new RawDocument(uri, "UTF-8", locENUS, locFRCA);
			filter.open(rd);
			int count = 0;
			while ( filter.hasNext() ) {
				Event event = filter.next();
				switch ( event.getEventType() ) {
				case TEXT_UNIT:
					tu = event.getTextUnit();
					String text = GenericContent.fromFragmentToLetterCoded(tu.getSource().getFirstContent(), true);
					switch ( count ) {
					case 0: assertEquals("Text <g1>bold</g1> and more", text); break;
					case 1: assertEquals("HTML <g1>bold</g1> and more", text); break;
					case 2: assertEquals("Plain text R&D", text); break;
					}
					count++;
					break;
				default:
					break;
				}
			}
			assertEquals(3, count);
		}
	}

	@Test
	public void preProcessingForMarkdownTest () {
		try ( MultiParsersFilter filter = new MultiParsersFilter() ) {
			String data = "- __Text__   \r\n More text";
			String res = filter.preProcessDataForMarkdown(data);
			assertEquals("- __Text__   \r\n\r\n[mrk1] More text", res);

			data = "- __Text__   \r\n\r\n More text"; // Not a match
			res = filter.preProcessDataForMarkdown(data);
			assertEquals(data, res);

			data = "- **Text**   \r\n More text";
			res = filter.preProcessDataForMarkdown(data);
			assertEquals("- **Text**   \r\n\r\n[mrk1] More text", res);

			data = "![img](url)\r\n More text";
			res = filter.preProcessDataForMarkdown(data);
			assertEquals("![img](url)\r\n\r\n[mrk1] More text", res);
		}
	}
	
	@Test
	public void autoDetectColumnTypesTest () {
		try ( MultiParsersFilter filter = new MultiParsersFilter() ) {
			filter.setFilterConfigurationMapper(fcMapper);
			// Set parameters
			Parameters prms = filter.getParameters();
			prms.setCsvAutoDetectColumnTypes(true);
			prms.setCsvAutoDetectColumnTypesRow(2);

			ITextUnit tu = null;
			URI uri = root.in("/test04.csv").asUri();
			RawDocument rd = new RawDocument(uri, "UTF-8", locENUS, locFRCA);
			filter.open(rd);
			int count = 0;
			while ( filter.hasNext() ) {
				Event event = filter.next();
				switch ( event.getEventType() ) {
				case TEXT_UNIT:
					tu = event.getTextUnit();
					String text = GenericContent.fromFragmentToLetterCoded(tu.getSource().getFirstContent(), true);
					switch ( count ) {
					case 0: assertEquals("some text", text); break;
					case 1: assertEquals("html <g1>bold</g1>", text); break;
					case 2: assertEquals("markdown <g1>bold</g1>", text); break;
					case 3: assertEquals("some text2", text); break;
					case 4: assertEquals("html <g1>bold2</g1>", text); break;
					case 5: assertEquals("markdown <g1>bold2</g1>", text); break;
					}
					count++;
					break;
				default:
					break;
				}
			}
			assertEquals(6, count);
		}
	}

}