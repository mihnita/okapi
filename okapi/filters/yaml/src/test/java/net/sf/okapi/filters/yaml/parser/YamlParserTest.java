/*===========================================================================
  Copyright (C) 2009-2014 by the Okapi Framework contributors
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

package net.sf.okapi.filters.yaml.parser;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.FileUtil;
import net.sf.okapi.common.StreamUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunWith(JUnit4.class)
public class YamlParserTest {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
	}

	@Test
	public void singleString() throws ParseException {
			String snippet = "one # test";
			YamlParser yp = new YamlParser(snippet);
			yp.setHandler(new DummyHandler());
			yp.parse();
	}
	
	@Test
	public void singleArrayNoSpace() throws ParseException {
			String snippet = "t: ['v1','v4','v5']";
			YamlParser yp = new YamlParser(snippet);
			yp.setHandler(new DummyHandler());
			yp.parse();
	}
	
	@Test
	public void singleArrayWithSpace() throws ParseException {
			String snippet = "t: ['v1', 'v4', 'v5']";
			YamlParser yp = new YamlParser(snippet);
			yp.setHandler(new DummyHandler());
			yp.parse();
	}

	@Test @Ignore
	public void complex() throws ParseException {
		String snippet = "--- \n" +
				"en:\n" +
				"  ? - Detroit Tigers\n" +
				"    - Chicago cubs\n" +
				"  ? [ New York Yankees, Atlanta Braves ] : [ 2001-07-02, 2001-08-12, 2001-08-14 ]";
		YamlParser yp = new YamlParser(snippet);
		yp.setHandler(new DummyHandler());
		yp.parse();
	}
	
	@Test
	public void singleFile() throws ParseException {
			String snippet = StreamUtil.streamUtf8AsString(root.in("/yaml/issues/ios_emoji_surrogate.yaml").asInputStream());
			// String snippet = StreamUtil.streamUtf8AsString(root.in("/yaml/spec_test/example2_27.yaml").asInputStream());
			YamlParser yp = new YamlParser(snippet);
			yp.setHandler(new DummyHandler());
			yp.parse();
	}

	@Test @Ignore
	// This fails the parse, but only logs and does not fail
	// It only produces "noise"
	public void sanityCheck() throws Exception {
		for (File file : getTestFiles("/yaml/en.yml", Arrays.asList(".yml", ".yaml"), false)) {
			String snippet = StreamUtil.streamUtf8AsString(new FileInputStream(file));
			YamlParser yp = new YamlParser(snippet);
			yp.setHandler(new DummyHandler());
			//System.out.println(file.getPath());
			try {
				yp.parse();
			} catch (ParseException|TokenMgrException e) {
				LOGGER.error("FAIL: {}", file.getPath());
				LOGGER.error("Message: {}", e.getMessage());
			}
		}
	}
	
	private Collection<File> getTestFiles(String resourcePath, final List<String> extensions, boolean isDirPath)
			throws URISyntaxException {
		File dir;
		if (isDirPath) {
			dir = new File(resourcePath);
		} else {
			URL url = root.in(resourcePath).asUrl();
			dir = new File(url.toURI()).getParentFile();
		}

		FilenameFilter filter = (dir1, name) -> {
			for (String e : extensions) {
				if (name.endsWith(e)) {
					return true;
				}
			}
			return false;
		};
		return FileUtil.getFilteredFiles(dir, filter, true);
	}
}
