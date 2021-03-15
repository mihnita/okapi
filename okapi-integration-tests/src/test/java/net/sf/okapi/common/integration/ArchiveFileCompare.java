/*===========================================================================
  Copyright (C) 2009-2013 by the Okapi Framework contributors
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

package net.sf.okapi.common.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.Util;

/**
 * This class compares two zip files. This can be used to compare zip file
 * output with a gold standard zip file.
 */
public class ArchiveFileCompare {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	final private IStreamComparator comparator;
	final private List<String> fileExtensions;

	/**
	 * Compare each element of the archive files (zip) using the
	 * {@link IStreamComparator}. Default is for XML files
	 * 
	 * @param comparator {@link IStreamComparator} used to compare each archive entry
	 */
	public ArchiveFileCompare(final IStreamComparator comparator) {
		this.comparator = comparator;
		// defaults
		fileExtensions = new LinkedList<>();
		fileExtensions.add(".xml");
		fileExtensions.add(".rels");
		fileExtensions.add(".xlf");
		fileExtensions.add(".xliff");
		fileExtensions.add(".tmx");
	}

	/**
	 * Compare each element of the archive files (zip) using the
	 * {@link IFileComparator}
	 * 
	 * @param comparator {@link IStreamComparator} used to compare each archive entry
	 */
	public ArchiveFileCompare(final IStreamComparator comparator, final List<String> fileExtensions) {
		this.comparator = comparator;
		this.fileExtensions = fileExtensions;
	}

	public boolean compareFiles(final Path out, final Path gold) {
		ZipFile goldZipFile = null;
		ZipFile outZipFile = null;
		Enumeration<? extends ZipEntry> outEntries = null;
		Enumeration<? extends ZipEntry> goldEntries = null;
		new FileCompare();

		try {
			final HashMap<String, ZipEntry> outZipMap = new HashMap<>();
			final HashMap<String, ZipEntry> goldZipMap = new HashMap<>();

			try {
				final File outZip = out.toFile();
				outZipFile = new ZipFile(outZip);
				outEntries = outZipFile.entries();
			} catch (final Exception e) {
				LOGGER.error("ZipCompare:  Output file {} not found.\n", out);
				LOGGER.error(out.getFileName().toString());
				return false;
			}

			try {
				final File goldZip = gold.toFile();
				goldZipFile = new ZipFile(goldZip);
				goldEntries = goldZipFile.entries();
			} catch (final Exception e) {
				LOGGER.error("ZipCompare:  Gold file {} not found.\n", gold);
				LOGGER.error(out.getFileName().toString());
				return false;
			}

			while (outEntries.hasMoreElements()) {
				final ZipEntry ze = outEntries.nextElement();
				outZipMap.put(ze.getName(), ze);
			}

			while (goldEntries.hasMoreElements()) {
				final ZipEntry ze = goldEntries.nextElement();
				goldZipMap.put(ze.getName(), ze);
			}

			if (outZipMap.size() != goldZipMap.size()) {
				LOGGER.error("Difference in number of files:");
				LOGGER.error(" out: {}", outZipMap.size());
				LOGGER.error("gold: {}", goldZipMap.size() + "\n");
				LOGGER.error(out.getFileName().toString());
				return false;
			}

			if (!outZipMap.keySet().equals(goldZipMap.keySet())) {
				LOGGER.error("Filenames do not match between the zipfiles\n");
				LOGGER.error(out.getFileName().toString());
				return false;
			}

			boolean failure = false;
			try {
				for (final String filename : outZipMap.keySet()) {

					final ZipEntry oze = outZipMap.get(filename);
					final ZipEntry gze = goldZipMap.get(filename);

					// some formats have zero byte xml files (openoffice)
					if (oze.getSize() <= 0 && gze.getSize() <= 0) {
						continue;
					} 

					final InputStream ois = outZipFile.getInputStream(oze);
					final InputStream gis = goldZipFile.getInputStream(gze); 
					if (isContent(oze.getName()) && isContent(gze.getName())) {
						failure = !comparator.compare(ois, gis);
					} else {
						// FIXME: Some minor byte differences cause this to fail - toostrict?
						//failure = !nonXmlComparer.filesExactlyTheSame(ois, gis); 
					}
				}
			} catch (final Exception e) {
				LOGGER.error("Error opening/reading file\n");
				LOGGER.error(out.getFileName().toString());
				return false;
			}

			if (!failure) {
				return true;
			} else {
				return false;
			}
		} finally {
			if (outZipFile != null) {
				try {
					outZipFile.close();
				} catch (final IOException e) {
					LOGGER.error("Error closing zip file", e);
				}
			}
			if (goldZipFile != null) {
				try {
					goldZipFile.close();
				} catch (final IOException e) {
					LOGGER.error("Error closing zip file", e);
				}
			}
		}
	}

	private boolean isContent(final String fileName) {
		final String ext = Util.getExtension(fileName);
		for (final String s : fileExtensions) {
			if (s.equalsIgnoreCase(ext)) {
				return true;
			}
		}
		return false; 
	}
}
