/*===========================================================================
  Copyright (C) 2020 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xliff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;

@RunWith(JUnit4.class)
public class XLIFFFilterXtmPropTest {

	private FileLocation location;
	private IFilterConfigurationMapper fcMapper;
    private LocaleId locENUS = LocaleId.fromString("en-US");
    private LocaleId locPLPL = LocaleId.fromString("pl-PL");
    private LocaleId locFRFR = LocaleId.fromString("fr-FR");

    @Before
    public void setUp() {
		location = FileLocation.fromClass(XLIFFFilterTest.class);
    	fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations("net.sf.okapi.filters.xliff.XLIFFFilter");
    }

	@Test
	public void testXtmDetection () {
		String snippet = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" 
				+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" xmlns:metrics=\"urn:lisa-metrics-tags\" xmlns:term=\"urn:xliff-term-extensions\" "
				+ "xmlns:xref=\"urn:xmlintl-xref\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xtm=\"urn:xliff-xtm-extensions\" "
				+ "version=\"1.2\" xsi:schemaLocation=\"urn:oasis:names:tc:xliff:document:1.2 http://www.xtm-intl.com/docs/xliff-core-1.2-transitional.xsd\">"
				+ "<file source-language=\"en-US\" target-language=\"fr-FR\" datatype=\"xml\" original=\"86e0069752dd4742810676d15c613fe0\" "
				+ "product-name=\"XTM International Limited xml:tm\" product-version=\"2.0\" tool=\"xtm-intl_XLIFF_Extract\" xml:space=\"default\" "
				+ "xtm:populate-target-with-source=\"no\"><header><skl><internal-file crc=\"af513f64\">"
				+ "BASE64data</internal-file></skl></header><body>"
				+ "<group id=\"g1\">"
				+ "<trans-unit id=\"t1\" translate=\"yes\" xtm:x-previous-crc=\"0\" xtm:x-next-crc=\"db73bd39\">"
				+ "<source>Context match segment.</source>"
				+ "<target state=\"signed-off\" state-qualifier=\"exact-match\">Context match segment (translated).</target>"
				+ "<alt-trans extype=\"leveraged-match\" match-quality=\"100%\" origin=\"29758623 - Segment status sample - 2020-05-12 - approved\" "
				+ "xtm:id=\"29758623\" xtm:project=\"Segment status sample\" xtm:changedby=\"jacek.florczyk\" xtm:changedate=\"20200512T110849Z\">"
				+ "<source>Context match segment.</source>"
				+ "<target>Context match segment (translated).</target>"
				+ "</alt-trans></trans-unit></group></body></file></xliff>";

		try ( IFilter filter = fcMapper.createFilter("okf_xliff") ) {
			StartSubDocument ssd = FilterTestDriver.getStartSubDocument(FilterTestDriver.getEvents(filter, snippet, locENUS, locFRFR), 1);
			assertNotNull(ssd);
			Property prop = ssd.getProperty(XLIFFFilter.PROP_XLIFF_FLAVOR);
			assertNotNull(prop);
			assertEquals(XLIFFFilter.XLIFF_FLAVOR_XTM, prop.getValue());
		}
	}

    @Test
    public void testSegmentProperties () {
    	try ( IFilter filter = fcMapper.createFilter("okf_xliff") ) {
    		filter.open(new RawDocument(location.in("/xtmxliff/StatusSample.docx.xlf").asUri(),
    			"UTF-8", locENUS, locPLPL, "okf_xliff"));
    		int tuCount = 0;
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( event.isTextUnit() ) {
    				tuCount++;
        			ITextUnit tu = event.getTextUnit();
        			TextContainer trgTc = tu.getTarget(locPLPL);
        			ISegments srcSegs = tu.getSourceSegments();
        			ISegments trgSegs = tu.getTargetSegments(locPLPL);
        			for ( Segment srcSeg : srcSegs ) {
        				Segment trgSeg = trgSegs.get(srcSeg.getId());
        				if ( trgSeg == null ) continue;
        				// Checks
        				switch ( tuCount ) {
        				case 1: // Exact match
        					assertEquals("Context match segment (translated).", trgSeg.getContent().getCodedText());
        					assertEquals(XLIFFFilter.EXACT_MATCH, trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER).getValue());
        					assertEquals("100", trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT).getValue());
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 2:
        					assertEquals("Exact match segment (translated).", trgSeg.getContent().getCodedText());
        					assertEquals(XLIFFFilter.LEVERAGED_TM, trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER).getValue());
        					assertEquals("100", trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT).getValue());
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 3:
        					assertEquals("", trgSeg.getContent().getCodedText());
        					assertEquals(XLIFFFilter.FUZZY_MATCH, trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER).getValue());
        					assertEquals("80", trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT).getValue());
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 4:
        					assertEquals("", trgSeg.getContent().getCodedText());
        					assertNull(trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT));
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 5:
        					assertEquals("", trgSeg.getContent().getCodedText());
        					assertEquals(XLIFFFilter.LEVERAGED_INHERITED, trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER).getValue());
        					assertEquals("100", trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT).getValue());
        					assertEquals("4", trgSeg.getProperty(XLIFFFilter.PROP_REPETITION).getValue());
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 6:
        					assertEquals("", trgSeg.getContent().getCodedText());
        					assertEquals(XliffXtmFilterExtension.SQ_XTM_FUZZY_FORWARD, trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER).getValue());
        					assertEquals("77", trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT).getValue());
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 7:
        					assertEquals("", trgSeg.getContent().getCodedText());
        					assertNull(trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT));
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED));
        					break;
        				case 8:
        					assertEquals("Locked segment", trgSeg.getContent().getCodedText());
        					assertEquals(XliffXtmFilterExtension.SQ_XTM_MANUAL_NOTRANS, trgTc.getProperty(XLIFFFilter.STATE_QUALIFIER).getValue());
        					assertNull(trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_PERCENT));
        					assertNull(trgSeg.getProperty(XLIFFFilter.PROP_REPETITION));
        					assertEquals("true", trgSeg.getProperty(XliffXtmFilterExtension.PROP_XTM_LOCKED).getValue());
        					break;
        				}
        			}
    			}
    			else if ( event.isStartSubDocument() ) {
    				StartSubDocument ssd = event.getStartSubDocument();
    				assertNotNull(ssd);
    				Property prop = ssd.getProperty(XLIFFFilter.PROP_XLIFF_FLAVOR);
    				assertNotNull(prop);
    				assertEquals(XLIFFFilter.XLIFF_FLAVOR_XTM, prop.getValue());
    				
    			}
    		}
    		assertEquals(8, tuCount);
    	}
    }

}
