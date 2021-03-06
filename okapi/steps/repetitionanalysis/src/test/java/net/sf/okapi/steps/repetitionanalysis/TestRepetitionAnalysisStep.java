/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.repetitionanalysis;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinebuilder.XBatch;
import net.sf.okapi.common.pipelinebuilder.XBatchItem;
import net.sf.okapi.common.pipelinebuilder.XParameter;
import net.sf.okapi.common.pipelinebuilder.XPipeline;
import net.sf.okapi.common.pipelinebuilder.XPipelineStep;
import net.sf.okapi.filters.plaintext.PlainTextFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.segmentation.Parameters;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestRepetitionAnalysisStep {
	FileLocation root = FileLocation.fromClass(this.getClass());

	@Test
	public void testExactRepetitions() {
		String fname = "test1.txt";
		new XPipeline(
				"Test pipeline for repetition analysis step",
				new XBatch(						
						new XBatchItem(
								root.in(fname).asUrl(),
								"UTF-8",
								LocaleId.ENGLISH,
								LocaleId.GERMAN)						
						),
						
				new RawDocumentToFilterEventsStep(new PlainTextFilter()),
				//new EventLogger(),
				new XPipelineStep(
						new SegmentationStep(),
						new XParameter("copySource", true),
						new XParameter("sourceSrxPath", root.in("default.srx").toString()),
						// new XParameter("sourceSrxPath", root.in("myRules.srx").toString()),
						new XParameter("trimSrcLeadingWS", Parameters.TRIM_YES),
						new XParameter("trimSrcTrailingWS", Parameters.TRIM_YES)
				),
				new RepetitionAnalysisStep()
		).execute();
	}

	@Test
	public void testFuzzyRepetitions() {
		String fname = "test1.txt";
		new XPipeline(
				"Test pipeline for repetition analysis step",
				new XBatch(						
						new XBatchItem(
								root.in(fname).asUrl(),
								"UTF-8",
								LocaleId.ENGLISH,
								LocaleId.GERMAN)						
						),							
				new RawDocumentToFilterEventsStep(new PlainTextFilter()),
				//new EventLogger(),
				new XPipelineStep(
						new SegmentationStep(),
						new XParameter("copySource", true),
						new XParameter("sourceSrxPath", root.in("default.srx").toString()),
						// new XParameter("sourceSrxPath", root.in("myRules.srx").toString()),
						new XParameter("trimSrcLeadingWS", Parameters.TRIM_YES),
						new XParameter("trimSrcTrailingWS", Parameters.TRIM_YES)
				),
				new XPipelineStep(
						new RepetitionAnalysisStep(),
						new XParameter("fuzzyThreshold", 40)
				)
		).execute();
	}

}
