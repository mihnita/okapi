/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;

@RunWith(JUnit4.class)
public class XLIFFFilterSDLPropTest {

	private FileLocation location;
	private IFilterConfigurationMapper fcMapper;
    private LocaleId locENUS = LocaleId.fromString("en-US");
    private LocaleId locFRFR = LocaleId.fromString("fr-FR");

    @Before
    public void setUp() {
		location = FileLocation.fromClass(XLIFFFilterTest.class);
    	fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations("net.sf.okapi.filters.xliff.XLIFFFilter");
    }

    @Test
    public void testSegmentProperties () {
    	try ( IFilter filter = fcMapper.createFilter("okf_xliff-sdl") ) {
    		filter.open(new RawDocument(location.in("/test.txt_en-US_fr-FR.sdlxliff").asUri(),
    			"UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) continue;
    			ITextUnit tu = event.getTextUnit();
    			
    			// Check we still have the properties on the text container
    			// And it is the values for the last segment
    			TextContainer tc = tu.getTarget(locFRFR);
    			assertEquals("document-match", tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN).getValue());
    			assertEquals("ApprovedSignOff", tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());

    			// Check the properties are also on each segment
    			ISegments segs = tc.getSegments();
    			assertEquals(5, segs.count());
    			Segment seg = segs.get(0);
    			assertEquals("interactive", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN).getValue());
    			assertEquals("Translated", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());
    			seg = segs.get(1);
    			assertEquals("mt", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN).getValue());
    			assertEquals("Translated", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());
    			seg = segs.get(2);
    			assertEquals("tm", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN).getValue());
    			assertEquals("Draft", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());
    			seg = segs.get(3);
    			assertEquals("document-match", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN).getValue());
    			assertEquals("ApprovedSignOff", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());
    			assertEquals("Perfect Match", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN_SYSTEM).getValue());
    			assertEquals("100", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_PERCENT).getValue());
    			seg = segs.get(4);
    			assertEquals("SourceAndTarget", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_TEXT_MATCH).getValue());
    			assertEquals("ApprovedSignOff", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF).getValue());
    			assertEquals("100", seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_PERCENT).getValue());
    		}
    	}
    }
    
    @Test
    public void testSdlRepetitions () {
    	try ( IFilter filter = fcMapper.createFilter("okf_xliff-sdl") ) {
    		filter.open(new RawDocument(location.in("/sdl-rep/test1.docx.sdlxliff").asUri(),
    			"UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));

			boolean s3=false, s5=false, s6=false;
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( event.isStartSubDocument() ) {
    				assertEquals(XLIFFFilter.XLIFF_FLAVOR_SDLXLIFF,
    					event.getStartSubDocument().getProperty(XLIFFFilter.PROP_XLIFF_FLAVOR).getValue());
    				continue;
    			}
    			else if ( !event.isTextUnit() ) {
    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			
    			TextContainer tc = tu.getTarget(locFRFR);
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
    				Property prop = seg.getProperty(XLIFFFilter.PROP_REPETITION);
    				if ( prop != null ) {
    					assertEquals("IAGmqmRECUZSs2tfvlcgFeCuYYk=", prop.getValue());
    					switch ( seg.getId() ) {
    					case "3": s3 = true; break;
    					case "5": s5 = true; break;
    					case "6": s6 = true; break;
    					default:
    						throw new RuntimeException("Unexpected repetition property");
    					}
    				}
    			}
    		}
    		// When document is done:
			assertTrue(s3);
			assertTrue(s5);
			assertTrue(s6);
    		
    	}
    }
    
    @Test
    public void testSegmentPropertiesOutputUsingTCLevelData () {
    	FileLocation.In inLocation = location.in("/test.txt_en-US_fr-FR.sdlxliff");
    	FileLocation.Out outLocation = location.out("/test.txt_en-US_fr-FR.OUTtc.sdlxliff");

    	try ( IFilter filter = fcMapper.createFilter("okf_xliff-sdl") ) {
    		filter.open(new RawDocument(inLocation.asUri(), "UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));
    		
    		// Parameters params = (Parameters)filter.getParameters();
    		// No change in the parameters
    		// Default is: conf is Translated by default for okf_xliff-sdl
    		//             use the TC when writing things out

    		IFilterWriter fw = filter.createFilterWriter();
    		fw.setOutput(outLocation.toString());
    		fw.setOptions(locFRFR, "UTF-8");
    		
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) {
    				fw.handleEvent(event);
    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			
    			// Check we still have the properties on the text container
    			// And it is the values for the last segment
    			TextContainer tc = tu.getTarget(locFRFR);
    			Property prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
    			assertEquals("document-match", prop.getValue());
    			prop.setValue("originFromTC"); // Change value for output
    			prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_LOCKED);
    			assertEquals(true, prop.getBoolean());
    			prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
    			assertEquals(SdlXliffConfLevel.APPROVED_SIGN_OFF.getConfValue(), prop.getValue());
    			prop = tc.getProperty(Property.STATE);
    			assertEquals(SdlXliffConfLevel.APPROVED_SIGN_OFF.getStateValue(), prop.getValue());
    			prop.setValue(SdlXliffConfLevel.REJECTED_SIGN_OFF.getStateValue()); // Change is state for output
    			
    			// Check for each segment
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
        			Property propOrigin = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
        			Property propLocked = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_LOCKED);
        			Property propConf = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
        			if ( seg.getId().equals("1") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("interactive", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("2") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("mt", propOrigin.getValue());
        				// Change values (This should have no effect in the output)
        				propConf.setValue(SdlXliffConfLevel.DRAFT.getConfValue());
        				propOrigin.setValue("mt-UpdatedValueSeg2");
        			}
        			else if ( seg.getId().equals("3") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.DRAFT.getConfValue(), propConf.getValue());
        				assertEquals("tm", propOrigin.getValue());
        				// Change the values (This should have no effect in the output)
        				propConf.setValue(SdlXliffConfLevel.TRANSLATED.getConfValue());
        				propOrigin.setValue("tm-UpdatedValueSeg3");
        			}
        			else if ( seg.getId().equals("4") ) {
        				assertTrue(propLocked.getBoolean());
        				assertEquals(SdlXliffConfLevel.APPROVED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("document-match", propOrigin.getValue());
        			}
    			}
    			
    			fw.handleEvent(event);
    		}
    		fw.close();
    		filter.close();

    		// Verify the output
    		filter.open(new RawDocument(outLocation.asUri(), "UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));
    		
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) {
    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			TextContainer tc = tu.getTarget(locFRFR);
    			// Check for each segment
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
        			Property propOrigin = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
        			Property propLocked = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_LOCKED);
        			Property propConf = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
        			if ( seg.getId().equals("1") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.REJECTED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("originFromTC", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("2") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.REJECTED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("originFromTC", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("3") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.REJECTED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("originFromTC", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("4") ) {
        				assertTrue(propLocked.getBoolean());
        				assertEquals(SdlXliffConfLevel.REJECTED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("originFromTC", propOrigin.getValue());
        			}
    			}
    		}
    	}
    }
    
    @Test
    public void testSegmentPropertiesOutputUsingSegLevelData () {
    	FileLocation.In inLocation = location.in("/test.txt_en-US_fr-FR.sdlxliff");
    	FileLocation.Out outLocation = location.out("/test.txt_en-US_fr-FR.OUTsg.sdlxliff");

    	try ( IFilter filter = fcMapper.createFilter("okf_xliff-sdl") ) {

    		filter.open(new RawDocument(inLocation.asUri(), "UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));
    		// Change okf_xliff-sdl defaults
    		Parameters params = (Parameters)filter.getParameters();
    		params.setUseSegsForSdlProps(true); // Now we write to segment properties

    		IFilterWriter fw = filter.createFilterWriter();
    		fw.setOutput(outLocation.toString());
    		fw.setOptions(locFRFR, "UTF-8");
    		
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) {
    				fw.handleEvent(event);
    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			
    			// Check we still have the properties on the text container
    			// And it is the values for the last segment
    			TextContainer tc = tu.getTarget(locFRFR);
    			Property prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
    			assertEquals("document-match", prop.getValue());
    			prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_LOCKED);
    			assertEquals(true, prop.getBoolean());
    			prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
    			assertEquals(SdlXliffConfLevel.APPROVED_SIGN_OFF.getConfValue(), prop.getValue());
    			
    			// Check for each segment
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
        			Property propOrigin = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
        			Property propLocked = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_LOCKED);
        			Property propConf = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
        			if ( seg.getId().equals("1") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("interactive", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("2") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("mt", propOrigin.getValue());
        				// Change values
        				propConf.setValue(SdlXliffConfLevel.DRAFT.getConfValue());
        				propOrigin.setValue("mt-UpdatedValueSeg2");
        			}
        			else if ( seg.getId().equals("3") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.DRAFT.getConfValue(), propConf.getValue());
        				assertEquals("tm", propOrigin.getValue());
        				// Change the values
        				propConf.setValue(SdlXliffConfLevel.TRANSLATED.getConfValue());
        				propOrigin.setValue("tm-UpdatedValueSeg3");
        			}
        			else if ( seg.getId().equals("4") ) {
        				assertTrue(propLocked.getBoolean());
        				assertEquals(SdlXliffConfLevel.APPROVED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("document-match", propOrigin.getValue());
        			}
    			}
    			
    			fw.handleEvent(event);
    		}
    		fw.close();
    		filter.close();

    		// Verify the output

    		filter.open(new RawDocument(outLocation.asUri(), "UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));

    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) {

    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			TextContainer tc = tu.getTarget(locFRFR);
    			// Check for each segment
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
        			Property propOrigin = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
        			Property propLocked = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_LOCKED);
        			Property propConf = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
        			if ( seg.getId().equals("1") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("interactive", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("2") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.DRAFT.getConfValue(), propConf.getValue());
        				assertEquals("mt-UpdatedValueSeg2", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("3") ) {
        				assertNull(propLocked);
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("tm-UpdatedValueSeg3", propOrigin.getValue());
        			}
        			else if ( seg.getId().equals("4") ) {
        				assertTrue(propLocked.getBoolean());
        				assertEquals(SdlXliffConfLevel.APPROVED_SIGN_OFF.getConfValue(), propConf.getValue());
        				assertEquals("document-match", propOrigin.getValue());
        			}
    			}
    		}
    	}
    }

    @Test
    public void testAddingSdlSegmentProperties () {
    	try ( IFilter filter = fcMapper.createFilter("okf_xliff-sdl") ) {
    		filter.open(new RawDocument(location.in("/adding-segprop.sdlxliff").asUri(),
    			"UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));

    		// Change default of okf_xliff-sdl
    		Parameters params = (Parameters)filter.getParameters();
    		params.setUseSegsForSdlProps(true); // Use segments for updating the SDL properties
    		// And conf default is 'Translated' per okf_xliff-sdl

    		IFilterWriter fw = filter.createFilterWriter();
    		fw.setOutput(location.out("/adding-segprop_OUT.sdlxliff").toString());
    		fw.setOptions(locFRFR, "UTF-8");
    		
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) {
    				fw.handleEvent(event);
    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			
    			// Check we still have the properties on the text container
    			// And it is the values for the last segment
    			TextContainer tc = tu.getTarget(locFRFR);
    			Property prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
    			assertEquals("mt", prop.getValue());
    			prop = tc.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF); // Comes from default
    			assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), prop.getValue());
    			
    			// Check for each segment
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
        			Property propOrigin = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
        			Property propConf = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
        			if ( seg.getId().equals("1") ) {
        				assertEquals("not-translated", propOrigin.getValue());
        				assertNull(propConf.getValue());
        			}
        			else if ( seg.getId().equals("2") ) {
        				assertNull(propConf.getValue());
        				assertEquals("mt", propOrigin.getValue());
        				// Change values
        				propOrigin.setValue("mt-Updated");
        				propConf.setValue(SdlXliffConfLevel.DRAFT.getConfValue());
        			}
        			else if ( seg.getId().equals("3") ) {
        				assertEquals(SdlXliffConfLevel.TRANSLATED.getConfValue(), propConf.getValue());
        				assertEquals("mt", propOrigin.getValue());
        				// Change values
        				propOrigin.setValue("mt-Updated2");
        				propConf.setValue(null); // To remove
        			}
    			}
    			
    			fw.handleEvent(event);
    		}
    		fw.close();
    		filter.close();

    		// Verify the output
    		filter.open(new RawDocument(location.out("/adding-segprop_OUT.sdlxliff").asUri(),
       			"UTF-8", locENUS, locFRFR, "okf_xliff-sdl"));
    		
    		while ( filter.hasNext() ) {
    			Event event = filter.next();
    			if ( !event.isTextUnit() ) {
    				continue;
    			}
    			ITextUnit tu = event.getTextUnit();
    			TextContainer tc = tu.getTarget(locFRFR);
    			// Check for each segment
    			ISegments segs = tc.getSegments();
    			for ( Segment seg : segs ) {
        			Property propOrigin = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN);
        			Property propConf = seg.getProperty(SdlXliffSkeletonWriter.PROP_SDL_CONF);
        			if ( seg.getId().equals("1") ) {
        				assertEquals("not-translated", propOrigin.getValue());
        				assertNull(propConf.getValue());
        			}
        			else if ( seg.getId().equals("2") ) {
        				assertEquals("mt-Updated", propOrigin.getValue());
//TODO        				assertEquals(SdlXliffConfLevel.DRAFT.getConfValue(), propConf.getValue());
        				assertNull(propConf.getValue()); // For now
        			}
        			else if ( seg.getId().equals("3") ) {
        				assertEquals("mt-Updated2", propOrigin.getValue());
        				assertEquals(SdlXliffConfLevel.UNSPECIFIED.getConfValue(), propConf.getValue());
        			}
    			}
    		}
    	}
    }
}
