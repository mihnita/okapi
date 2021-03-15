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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.properties.PropertiesFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.rainbowkit.creation.ExtractionStep;
import net.sf.okapi.steps.rainbowkit.creation.Parameters;
import net.sf.okapi.steps.xsltransform.XSLTransformStep;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;

import static org.junit.Assert.assertTrue;
import org.junit.runners.JUnit4;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@RunWith(JUnit4.class)
public class ExtractionStepTest {
	
	private FileLocation root;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locFR = LocaleId.fromString("fr");
	
	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
	}
	
	@Test
	public void testSimpleStep ()
		throws XpathException, FileNotFoundException, SAXException, IOException
	{
		// Ensure output is deleted
		assertTrue(Util.deleteDirectory(root.out("/packXliff1").asFile()));
		
		IPipelineDriver pdriver = new PipelineDriver();
		FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
		fcMapper.addConfigurations(PropertiesFilter.class.getName());
		pdriver.setFilterConfigurationMapper(fcMapper);
		String rootDir = root.in("/").toString();
		pdriver.setRootDirectories(rootDir, rootDir);
		pdriver.addStep(new RawDocumentToFilterEventsStep());

		ExtractionStep es = new ExtractionStep();
		Parameters params = es.getParameters();
		String outputDir = root.out("/").toString();
		params.setPackageDirectory(outputDir);
		params.setWriterClass(XLIFFPackageWriter.class.getName());
		params.setSendOutput(true);
		params.setPackageName("packXliff1");
		pdriver.addStep(es);

		XSLTransformStep xslTransformStep = new XSLTransformStep();
		net.sf.okapi.steps.xsltransform.Parameters xsltParams = xslTransformStep.getParameters();
		xsltParams.setXsltPath(root.in("/addCustomAttribute.xsl").toString());
		xsltParams.setPassOnOutput(true);
		pdriver.addStep(xslTransformStep);
		
		URI inputURI = root.in("/test01.properties").asUri();
		URI outputURI = new File("/test01.out.properties").toURI();
		pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_properties", outputURI, "UTF-8", locEN, locFR));

		pdriver.processBatch();

		//verify that the extraction step did its work
		File file = root.out("/packXliff1/work/test01.properties.xlf").asFile();
		assertTrue(file.exists());
		
		//verify that the xslt transform step did its work
		Map<String, String> nsPrefixes = new HashMap<>();
		nsPrefixes.put("custom", "custom-uri");
		nsPrefixes.put("xlf", "urn:oasis:names:tc:xliff:document:1.2");
		XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(nsPrefixes));
		XMLAssert.assertXpathEvaluatesTo("custom-val", "/xlf:xliff/xlf:file/@custom:custom-attribute",
				new InputSource(new FileInputStream(file)));
	}
}
