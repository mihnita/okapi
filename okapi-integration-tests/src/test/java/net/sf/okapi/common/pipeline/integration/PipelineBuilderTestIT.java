/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  This library is free software; you can redistribute it and/or modify it 
  under the terms of the GNU Lesser General Public License as published by 
  the Free Software Foundation; either version 2.1 of the License, or (at 
  your option) any later version.

  This library is distributed in the hope that it will be useful, but 
  WITHOUT ANY WARRANTY; without even the implied warranty of 
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
  General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License 
  along with this library; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

  See also the full LGPL text here: http://www.gnu.org/copyleft/lesser.html
===========================================================================*/

package net.sf.okapi.common.pipeline.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.common.pipelinebuilder.XPipelineType;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.sentencealigner.SentenceAlignerStep;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PipelineBuilderTestIT
{
	@Test
	public void testParallelPipeline() {
		XPipeline p5 =
			new XPipeline(
					"Alignment pipeline. Source and target documents are processed by separate " +
					"pipelines connected in parallel. Events from both pipelines are finalized in TestAlignerStep.",
					new XPipeline(
							"Parallel pipeline for parallel handling of source and target documents.",
							XPipelineType.PARALLEL,
							
							new XPipeline(
									"Source document translatable text extraction",
									new XBatch(
											new XBatchItem(
													(new File("source.html")).toURI(),
													 "UTF-8",
													 LocaleId.ENGLISH)),
									new RawDocumentToFilterEventsStep()),
									
							new XPipeline(
									"Target document translatable text extraction",
									new XBatch(
											new XBatchItem(
													(new File("target.doc")).toURI(),
													 "UTF-16",
													 LocaleId.CHINA_CHINESE)),
									new RawDocumentToFilterEventsStep())),
									
					new SentenceAlignerStep(),
					
					new FilterEventsWriterStep());

		PipelineDriver pd = new PipelineDriver();
		pd.setPipeline(p5);
		assertNotNull(pd.getPipeline());
		assertEquals(3, pd.getPipeline().getSteps().size());
		
		p5.execute();
	}		
}
