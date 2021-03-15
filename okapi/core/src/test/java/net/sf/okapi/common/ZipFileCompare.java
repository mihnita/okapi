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

package net.sf.okapi.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class compares two zip files to see if they have the same contents. The
 * filesExactlyTheSame method takes two files specified by their file paths and
 * indicates by calling FileCompare whether all files in the zip are exactly the
 * same as each other. This can be used to compare zip file output with a gold
 * standard zip file.
 */
public class ZipFileCompare {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final FileCompare fc = new FileCompare();

	public boolean compareFiles(String type, String out, String gold, String encoding, boolean ignoreEmtpyLines) {
		boolean failure = false;

		try (ZipFile outZipFile = new ZipFile(new File(out)); ZipFile goldZipFile = new ZipFile(new File(gold))) {

			Enumeration<? extends ZipEntry> outEntries = outZipFile.entries();
			Enumeration<? extends ZipEntry> goldEntries = goldZipFile.entries();

			HashMap<String, ZipEntry> outZipMap = new HashMap<>();
			while (outEntries.hasMoreElements()) {
				ZipEntry ze = outEntries.nextElement();
				outZipMap.put(ze.getName(), ze);
			}

			HashMap<String, ZipEntry> goldZipMap = new HashMap<>();
			while (goldEntries.hasMoreElements()) {
				ZipEntry ze = goldEntries.nextElement();
				goldZipMap.put(ze.getName(), ze);
			}

			if (outZipMap.keySet().size() != goldZipMap.keySet().size()) {
				logger.error("Difference in number of files:\n"
						+ " out: {}\n"
						+ "gold: {}\n"
						+ "{}",
						outZipMap.keySet().size(), goldZipMap.keySet().size(), Util.getFilename(out, true));
				return false;
			}

			if (!outZipMap.keySet().equals(goldZipMap.keySet())) {
				logger.error("Filenames do not match between the zipfiles\n{}", Util.getFilename(out, true));
				return false;
			}

			for (String filename : outZipMap.keySet()) {

				boolean same = false;

				try (InputStream ois = outZipFile.getInputStream(outZipMap.get(filename));
						InputStream gis = goldZipFile.getInputStream(goldZipMap.get(filename))) {
					switch (type) {
					case "PerLine":
						same = fc.compareFilesPerLines(ois, gis, encoding);
						break;
					case "PerLineIgnoreEmpty":
						same = fc.compareFilesPerLines(ois, gis, encoding, ignoreEmtpyLines);
						break;
					case "PerLineIgnoreEmptyIC":
						same = fc.compareFilesPerLines(ois, gis, encoding, ignoreEmtpyLines, true);
						break;
					default:
						same = fc.filesExactlyTheSame(ois, gis);
					}
				}

				if (!same) {
					logger.error("Output and Gold Entry {} differ\n{}", filename, Util.getFilename(out, true));
					failure = true;
				}
			}
		} catch (IOException e) {
			logger.error("Error opening/reading file {}\n", Util.getFilename(out, true));
			e.printStackTrace();
			failure = true;
		}

		return !failure;
	}

	public boolean compareFilesPerLines(String out, String gold, String encoding) {
		return compareFiles("PerLine", out, gold, encoding, false);
	}

	public boolean compareFilesPerLines(String out, String gold, String encoding, boolean ignoreEmtpyLines) {
		return compareFiles("PerLineIgnoreEmpty", out, gold, encoding, ignoreEmtpyLines);
	}

	public boolean compareFilesPerLinesIgnoreCase(String out, String gold, String encoding, boolean ignoreEmtpyLines) {
		return compareFiles("PerLineIgnoreEmptyIC", out, gold, encoding, ignoreEmtpyLines);
	}

	public boolean filesExactlyTheSame(String out, String gold) {
		return compareFiles("ExactlyTheSame", out, gold, null, false);
	}
}
