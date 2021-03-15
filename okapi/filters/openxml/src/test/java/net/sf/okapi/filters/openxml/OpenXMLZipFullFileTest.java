/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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
package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a test that filters all files in the data directory.
 */

@RunWith(JUnit4.class)
public class OpenXMLZipFullFileTest {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private OpenXMLFilter openXMLFilter;
	private String[] testFileList;
	private LocaleId locEN = LocaleId.fromString("en");
	private LocaleId locENUS = LocaleId.fromString("en-us");
	private FileLocation root;

	@Before
	public void setUp() throws Exception {
		openXMLFilter = new OpenXMLFilter();
		openXMLFilter.setOptions(locEN, "UTF-8", true);
		root = FileLocation.fromClass(getClass());

		// read all files in the test html directory
		File dir = root.in("/").asFile();

		FilenameFilter filter = (dir1, name) -> ((name.endsWith(".docx") || name.endsWith(".docm")
					|| name.endsWith(".dotx") || name.endsWith(".dotm")
					|| name.endsWith(".pptx") || name.endsWith(".pptm")
					|| name.endsWith(".ppsx") || name.endsWith(".ppsm")
					|| name.endsWith(".potx") || name.endsWith(".potm")
					|| name.endsWith(".xlsx") || name.endsWith(".xlsm")
					|| name.endsWith(".xltx") || name.endsWith(".xltm")
					|| name.endsWith(".vsdx") || name.endsWith(".vsdm"))
				&& !name.startsWith("Output"));
		testFileList = dir.list(filter);
	}

	@After
	public void tearDown() {
		openXMLFilter.close();
	}

	@Test
	public void testAll() {
		for (String f : testFileList) {
			try {
				openXMLFilter.open(new RawDocument(root.in("/"+f).asUri(), "UTF-8", locENUS),true); // DWH 4-22-09
				while (openXMLFilter.hasNext()) {
					openXMLFilter.next();
				}
			}
			catch (Exception e) {
				LOGGER.warn("Error for file: {}", f, e);
				throw new OkapiException("Error for file: " + f + ": " + e.toString());
			}
		}
	}

	@Test
	public void testNonwellformed() {
		String filename = "/nonwellformed.specialtest";
		try
		{
			URI uriFf = new URI(filename);
			openXMLFilter.open(uriFf); // DWH 4-22-09
			while (openXMLFilter.hasNext()) {
				Event event = openXMLFilter.next();
				assertNotNull(event);
			}
			throw new OkapiException("Should have recognized" + filename + " is not an MSOffice 2007 file");
		}
		catch(Exception e)
		{
			LOGGER.trace("Error for file: {}: {}", filename ,e.toString());
			filename = "All is swell";
		}
	}
}
