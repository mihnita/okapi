/*===========================================================================
  Copyright (C) 2010-2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.rainbowkit.xliff;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.its.html5.HTML5Filter;
import net.sf.okapi.filters.openoffice.OpenOfficeFilter;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.filters.rainbowkit.RainbowKitFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.rainbowkit.creation.ExtractionStep;
import net.sf.okapi.steps.rainbowkit.postprocess.MergingStep;
import net.sf.okapi.steps.rainbowkit.postprocess.Parameters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ExtractThenMergeTest {
	
	private FileLocation root;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	
	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void testSimpleExtractThenMerge () {
		// Ensure output is deleted
		assertTrue(Util.deleteDirectory(root.out("/xlf2Pack").asFile()));
		
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(PropertiesFilter.class.getName());
		fcMapper.addConfigurations(OpenOfficeFilter.class.getName());
		fcMapper.addConfigurations(HTML5Filter.class.getName());
		fcMapper.addConfigurations(RainbowKitFilter.class.getName());
		String rootDir = root.in("/").toString();
		fcMapper.setCustomConfigurationsDirectory(rootDir);
		fcMapper.updateCustomConfigurations();

		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		ExtractionStep es = new ExtractionStep();
		String outputDir = root.out("/").toString();
		es.getParameters().setPackageDirectory(outputDir);
		pdriver.addStep(es);
		net.sf.okapi.steps.rainbowkit.creation.Parameters ep = es.getParameters();
		ep.setWriterClass("net.sf.okapi.steps.rainbowkit.xliff.XLIFF2PackageWriter");
		ep.setPackageName("xlf2Pack");
		
		URI inputURI = root.in("/test01.properties").asUri();
		// This is relative path, so we don't build it using the root
		URI outputURI = new File("test01.out.properties").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_properties", outputURI, "UTF-8", locEN, locFR));

		inputURI = root.in("/test02.html").asUri();
		outputURI = new File("test02.out.html").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_itshtml5", outputURI, "UTF-8", locEN, locFR));
		
		inputURI = root.in("/sub Dir/test01.odt").asUri();
		outputURI = new File("sub Dir/test01.out.odt").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_openoffice", outputURI, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		File file = root.out("/xlf2Pack/work/test01.properties.xlf").asFile();
		assertTrue(file.exists());
		file = root.out("/xlf2Pack/work/test02.html.xlf").asFile();
		assertTrue(file.exists());
		file = root.out("/xlf2Pack/work/sub Dir/test01.odt.xlf").asFile();
		assertTrue(file.exists());

		//=== Now merge
		
		deleteOutputDir("/xlf2Pack/done");
		
		pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.setOutputDirectory(rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		outputDir = root.out("/xlf2Pack/done").toString();
		mrgStep.getParameters().setOverrideOutputPath(outputDir);
		pdriver.addStep(mrgStep);
		
//		Parameters prm = (Parameters)mrgStep.getParameters();
//		prm.setReturnRawDocument(true);
		
		inputURI = root.out("/xlf2Pack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt",
			null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		file = root.out("/xlf2Pack/done/test01.out.properties").asFile();
		assertTrue(file.exists());
		file = root.out("/xlf2Pack/done/test02.out.html").asFile();
		assertTrue(file.exists());
		file = root.out("/xlf2Pack/done/sub Dir/test01.out.odt").asFile();
		assertTrue(file.exists());
		
		// Compare
		FileCompare fc = new FileCompare();
		fc.filesExactlyTheSame(root.out("/xlf2Pack/done/test01.out.properties").toString(),
			root.out("/xlf2Pack/original/test01.properties").toString());
	}
	
    public void deleteOutputDir(String dirname) {
		Util.deleteDirectory(root.out(dirname).toString(), false);
    }
}
