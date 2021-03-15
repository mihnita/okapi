/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package org.w3c.its;

import static org.junit.Assert.assertTrue;

import java.io.File;
import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.Util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ITSTest_TestSuite {

	public static final String XML = "xml";
	public static final String HTML = "html";

	private FileLocation root = FileLocation.fromClass(this.getClass());
	private String BASE_DIR = "/its2.0";
	private String BASE_INPUT_DIR = BASE_DIR + "/inputdata";
	private String BASE_GOLD_DIR = BASE_DIR + "/expected";
	private FileCompare fc = new FileCompare();
	
	@Test
	public void process () {
		processBatches("/translate", Main.DC_TRANSLATE);
		processBatches("/localizationnote", Main.DC_LOCALIZATIONNOTE);
		processBatches("/terminology", Main.DC_TERMINOLOGY);
		processBatches("/directionality", Main.DC_DIRECTIONALITY);
		processBatches("/languageinformation", Main.DC_LANGUAGEINFORMATION);
		processBatches("/elementswithintext", Main.DC_WITHINTEXT);
		processBatches("/domain", Main.DC_DOMAIN);
		processBatches("/textanalysis", Main.DC_TEXTANALYSIS);
		processBatches("/localefilter", Main.DC_LOCALEFILTER);
		processBatches("/externalresource", Main.DC_EXTERNALRESOURCE);
		processBatches("/targetpointer", Main.DC_TARGETPOINTER);
		processBatches("/idvalue", Main.DC_IDVALUE);
		processBatches("/preservespace", Main.DC_PRESERVESPACE);
		processBatches("/locqualityissue", Main.DC_LOCQUALITYISSUE);
		processBatches("/locqualityrating", Main.DC_LOCQUALITYRATING);
		processBatches("/storagesize", Main.DC_STORAGESIZE);
		processBatches("/mtconfidence", Main.DC_MTCONFIDENCE);
		processBatches("/allowedcharacters", Main.DC_ALLOWEDCHARACTERS);
		processBatches("/provenance", Main.DC_PROVENANCE);
	}
	
	/**
	 * Shortcut to process both xml and html formats
	 * @param base
	 * @param category
	 * @throws URISyntaxException
	 */
	public void processBatches (String base, String category) {
		processBatch(base, "/html", category);
		processBatch(base, "/xml", category);
	}
	
	/**
	 * Process all files in specified folder
	 * @param base
	 * @param category
	 * @throws URISyntaxException
	 */
	public void processBatch (String base, String subDir, String category) {
		Util.deleteDirectory(root.out(BASE_DIR + base + subDir).toString(), true);
		File f = root.in(BASE_INPUT_DIR + base + subDir).asFile();
		if ( ! f.exists() ) return;
		String[] files = Util.getFilteredFiles(f.toString(), "");
		for ( String file : files ) {
			if ( file.contains("rules") || file.contains("standoff") ) continue;
			process(base, subDir, "/" + file, category);
		}
	}

	private void process (String baseName, String subDir, String fileName,
		String dataCategory)
	{
		String input = root.in(BASE_INPUT_DIR + baseName + subDir + fileName).toString();
		
		int n = fileName.lastIndexOf('.');
		if ( n > -1 ) fileName = fileName.substring(0, n);
		fileName += "output";
		fileName += ".txt";
		String output = root.out(BASE_DIR + baseName + subDir + fileName).toString();

		Main.main(new String[]{input, output, "-dc", dataCategory});
		assertTrue(new File(output).exists());
		
		String gold = root.in(BASE_GOLD_DIR + baseName + subDir + fileName).toString();
		assertTrue(fc.compareFilesPerLines(output, gold, "UTF-8"));
// Just compare for now, until the test cases are stable
//		fc.compareFilesPerLines(output, gold, "UTF-8");
	}
	
}
