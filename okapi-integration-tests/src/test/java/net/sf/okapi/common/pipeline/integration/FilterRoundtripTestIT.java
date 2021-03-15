/*===========================================================================*/
/* Copyright (C) 2008-2014 Jim Hargrave                                      */
/*---------------------------------------------------------------------------*/
/* This library is free software; you can redistribute it and/or modify it   */
/* under the terms of the GNU Lesser General Public License as published by  */
/* the Free Software Foundation; either version 2.1 of the License, or (at   */
/* your option) any later version.                                           */
/*                                                                           */
/* This library is distributed in the hope that it will be useful, but       */
/* WITHOUT ANY WARRANTY; without even the implied warranty of                */
/* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser   */
/* General Public License for more details.                                  */
/*                                                                           */
/* You should have received a copy of the GNU Lesser General Public License  */
/* along with this library; if not, write to the Free Software Foundation,   */
/* Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA              */
/*                                                                           */
/* See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html */
/*===========================================================================*/

package net.sf.okapi.common.pipeline.integration;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FilterRoundtripTestIT
{

	private IPipelineDriver driver;
	private IFilterConfigurationMapper fcMapper;
	private final LocaleId locEN = LocaleId.fromString("EN");
	private final LocaleId locES = LocaleId.fromString("ES");
	private final LocaleId locFR = LocaleId.fromString("FR");
	private FileLocation root;
	
	@Before
	public void setUp() {
		fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(fcMapper);
		driver.addStep(new RawDocumentToFilterEventsStep());
		driver.addStep(new FilterEventsWriterStep());
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void runPipelineFromString() {
		driver.clearItems();
		try (RawDocument rd = new RawDocument(
				"<p>Before <input type=\"radio\" name=\"FavouriteFare\" value=\"spam\" checked=\"checked\"/> after.</p>",
				locEN, locES);
				RawDocument rawDoc = new RawDocument(root.out("genericOutput.txt").asUri(), "UTF-8", locES)) {
			rd.setFilterConfigId("okf_html");
			driver.addBatchItem(rd, root.out("genericOutput.txt").asUri(), "UTF-8");
			driver.processBatch();
			assertEquals("spam", PipelineTestUtil.getFirstTUSource(rawDoc));
		}
	}

	@Test
	public void runPipelineFromStream() {
		driver.clearItems();
		try (RawDocument rd = new RawDocument("\nX\n\nY\n", locEN, locFR);
				RawDocument rawDoc = new RawDocument(root.out("genericOutput.txt").asUri(), "UTF-8", locFR)) {
			rd.setFilterConfigId("okf_html");
			driver.addBatchItem(rd, root.out("genericOutput.txt").asUri(), "UTF-8");
			driver.processBatch();
			assertEquals("X Y", PipelineTestUtil.getFirstTUSource(rawDoc));
		}
	}
	
	@Test
	public void runPipelineTwice() throws UnsupportedEncodingException {
		String snippet = "<b>TEST ME</b>";
		// First pass
		driver.clearItems();
		try (RawDocument rd = new RawDocument(snippet, locEN, locES)) {
			rd.setFilterConfigId("okf_html");
			driver.addBatchItem(rd, root.out("output1.html").asUri(), "UTF-8");
			driver.processBatch();
		}

		// Second pass
		driver.clearItems();
		try (RawDocument rd = new RawDocument(root.out("output1.html").asUri(), "UTF-8", locES, locEN)) {
			rd.setFilterConfigId("okf_html");
			driver.addBatchItem(rd, root.out("output2.html").asUri(), "UTF-8");
			driver.processBatch();
		}

		// Check result
		try (RawDocument rd = new RawDocument(root.out("output2.html").asUri(), "UTF-8", locES)) {
			assertEquals(snippet, PipelineTestUtil.getFirstTUSource(rd));
		}
	}
	
	@After
	public void cleanUp() {
	}

}
