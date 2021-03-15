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

package net.sf.okapi.steps.rainbowkit.postprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.openoffice.OpenOfficeFilter;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.filters.rainbowkit.RainbowKitFilter;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.rainbowkit.creation.ExtractionStep;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MergingStepTest {
	
	private FileLocation root;
	private FilterConfigurationMapper fcMapper;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	
	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
		fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(PropertiesFilter.class.getName());
		fcMapper.addConfigurations(OpenOfficeFilter.class.getName());
		fcMapper.addConfigurations(RainbowKitFilter.class.getName());
		fcMapper.addConfigurations(XLIFFFilter.class.getName());
		fcMapper.setCustomConfigurationsDirectory(root.in("/").toString());
		fcMapper.updateCustomConfigurations();
	}

	@Test
	public void textXLIFFMerging () {
		// Call in the same test because they use the same files and concurent test would not work
		testXLIFFMerging(false);
		testXLIFFMerging(true);
	}
	
	private void testXLIFFMerging (boolean returnRawDoc) {
		deleteOutputDir("/xliffPack/done");
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		pdriver.addStep(mrgStep);
		
		Parameters prm = mrgStep.getParameters();
		String outputDir = root.out("/xliffPack/done").toString();
		prm.setOverrideOutputPath(outputDir);
		prm.setReturnRawDocument(returnRawDoc);
		
		URI inputURI = root.in("/xliffPack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();
		
		assertTrue(mrgStep.getErrorCount()==0);

		File file = root.out("/xliffPack/done/test01.out.properties").asFile();
		assertTrue(file.exists());
		file = root.out("/xliffPack/done/sub Dir/test01.out.odt").asFile();
		assertTrue(file.exists());
	}

	@Test
	public void missedCodesInTargetNotAdded() throws IOException {
		deleteOutputDir("/xliffPack/done");

		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		pdriver.addStep(mrgStep);

		Parameters prm = (Parameters) mrgStep.getParameters();
		String outputDir = root.out("/xliffPack/done").toString();
		prm.setOverrideOutputPath(outputDir);
		prm.setAddMissingCodes(false);

		URI inputURI = root.in("/xliffPack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, StandardCharsets.UTF_8.name(), "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		pdriver.processBatch();

		final FileLocation.Out fileLocation = root.out("/xliffPack/done/test02.out.properties");
		assertTrue(fileLocation.asFile().exists());
		assertEquals(
			"ID10=Text for ID10 with %s and .",
			new String(Files.readAllBytes(fileLocation.asPath()), StandardCharsets.UTF_8.name()).trim()
		);
	}

	@Test
	public void testXLIFFExtractThenMerge () {
		// Extract
		// Start by clearing the output
		deleteOutputDir("/pack1");
		// Create the pipeline
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		// Add the steps
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		ExtractionStep extStep = new ExtractionStep();
		String outputDir = root.out("/").toString();
		extStep.getParameters().setPackageDirectory(outputDir);
		pdriver.addStep(extStep);
		// Add the input file
		URI inputURI = root.in("/test01.properties").asUri();
		// This is relative path, so we don't build it using the root
		URI outputURI = new File("test01.out.properties").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_properties", outputURI, "UTF-8", locEN, locFR));
		// Process
		pdriver.processBatch();
		// Check we have an output
		File file = root.out("/pack1/work/test01.properties.xlf").asFile();
		assertTrue(file.exists());
		
		// Now try to merge
		pdriver.clearSteps();
		pdriver.clearItems();
		// Add the steps
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		outputDir = root.out("/pack1/done").toString();
		mrgStep.getParameters().setOverrideOutputPath(outputDir);
		pdriver.addStep(mrgStep);
		// Set the input file
		inputURI = root.out("/pack1/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		// Process
		pdriver.processBatch();

		assertTrue(mrgStep.getErrorCount()==0);
		// Check we have a file
		file = root.out("/pack1/done/test01.out.properties").asFile();
		assertTrue(file.exists());
		// Compare original and merged.
		FileCompare fc = new FileCompare();
		assertTrue(fc.compareFilesPerLines(file.toString(), root.in("/test01.properties").toString(), "us-ascii"));
	}

	@Test
	public void testXINIMerging () {
		deleteOutputDir("/xiniPack/translated");
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		String outputDir = root.out("/xiniPack/translated").toString();
		mrgStep.getParameters().setOverrideOutputPath(outputDir);
		pdriver.addStep(mrgStep);
		
		Parameters prm = mrgStep.getParameters();
		prm.setReturnRawDocument(true);
		
		URI inputURI = root.in("/xiniPack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		assertTrue(mrgStep.getErrorCount()==0);
		File file = root.out("/xiniPack/translated/test1.out.xlf").asFile();
		assertTrue(file.exists());
	}
	
	@Test
	public void testXINIMergingWithOutputPath () {
		deleteOutputDir("/xiniPack/translated");
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		String outputDir = root.out("/omegatPack/done").toString();
		mrgStep.getParameters().setOverrideOutputPath(outputDir);
		pdriver.addStep(mrgStep);
		
		Parameters prm = mrgStep.getParameters();
		prm.setReturnRawDocument(true);
		prm.setOverrideOutputPath(root.out("/output/").toString());
		
		URI inputURI = root.in("/xiniPack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		assertTrue(mrgStep.getErrorCount()==0);
		File file = root.out("/output/test1.out.xlf").asFile();
		assertTrue(file.exists());
	}
	
	@Test
	public void testPOMerging () {
		deleteOutputDir("/poPack/done");
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		String outputDir = root.out("/poPack/done").toString();
		mrgStep.getParameters().setOverrideOutputPath(outputDir);
		pdriver.addStep(mrgStep);
		
		URI inputURI = root.in("/poPack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));
		
		pdriver.processBatch();

		assertTrue(mrgStep.getErrorCount()==0);
		File file = root.out("/poPack/done/test01.out.properties").asFile();
		assertTrue(file.exists());
		file = root.out("/poPack/done/sub Dir/test01.out.odt").asFile();
		assertTrue(file.exists());
		
	}
	
	@Test
	public void testOmegaTMerging () {
		deleteOutputDir("/omegatPack/done");
		
		IPipelineDriver pdriver = new PipelineDriver();
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());
		MergingStep mrgStep = new MergingStep();
		String outputDir = root.out("/omegatPack/done").toString();
		mrgStep.getParameters().setOverrideOutputPath(outputDir);
		pdriver.addStep(mrgStep);
		
		URI inputURI = root.in("/omegatPack/manifest.rkm").asUri();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_rainbowkit@noPrompt", null, "UTF-8", locEN, locFR));

		pdriver.processBatch();

		assertTrue(mrgStep.getErrorCount()==0);
		File file = root.out("/omegatPack/done/test01.out.properties").asFile();
		assertTrue(file.exists());
		file = root.out("/omegatPack/done/sub Dir/test01.out.odt").asFile();
		assertTrue(file.exists());	
	}

    public void deleteOutputDir(String dirname) {
		Util.deleteDirectory(root.out(dirname).toString(), false);
    }
}
