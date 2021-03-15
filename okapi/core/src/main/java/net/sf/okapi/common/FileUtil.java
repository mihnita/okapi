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

package net.sf.okapi.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.exceptions.OkapiIOException;

/**
 * Helper methods for manipulating files.
 */
public final class FileUtil {

	/**
	 * Gets an array of the files in a given directory.
	 * <p>
	 * This method searches all {@link File}s recursively that pass the
	 * {@link FilenameFilter}. Adapted from
	 * http://snippets.dzone.com/posts/show/1875
	 * 
	 * @param directory
	 *            root directory
	 * @param filter
	 *            {@link FilenameFilter} used to filter the File candidates
	 * @param recurse
	 *            true to recurse in the sub-directories, false to not.
	 * @return an array of {@link File}s (File[])
	 */
	public static File[] getFilteredFilesAsArray(File directory,
			FilenameFilter filter, boolean recurse) {
		Collection<File> files = FileUtil.getFilteredFiles(directory, filter,
				recurse);
		File[] arr = new File[files.size()];
		return files.toArray(arr);
	}

	/**
	 * Gets a collection of the files in a given directory.
	 * <p>
	 * This method search all {@link File}s recursively that pass the
	 * {@link FilenameFilter}. Adapted from
	 * http://snippets.dzone.com/posts/show/1875
	 * 
	 * @param directory
	 *            root directory
	 * @param filter
	 *            {@link FilenameFilter} used to filter the File candidates
	 * @param recurse
	 *            true to recurse in the sub-directories, false to not.
	 * @return {@link Collection} of {@link File}s
	 */
	public static Collection<File> getFilteredFiles(File directory,
			FilenameFilter filter, boolean recurse) {
		// List of files / directories
		List<File> files = new LinkedList<>();

		// Get files / directories in the directory
		File[] entries = directory.listFiles();

		if (entries == null) {
			return files;
		}

		// Go over entries
		for (File entry : entries) {
			// If there is no filter or the filter accepts the
			// file / directory, add it to the list
			if (filter == null || filter.accept(directory, entry.getName())) {
				files.add(entry);
			}

			// If the file is a directory and the recurse flag
			// is set, recurse into the directory
			if (recurse && entry.isDirectory()) {
				files.addAll(getFilteredFiles(entry, filter, recurse));
			}
		}

		// Return collection of files
		return files;
	}

	// Out of guessLanguages for better performance
	private static final Pattern pattern = Pattern
			.compile(
					"\\s(srclang|source-?language|xml:lang|lang|trglang|(target)?locale|(target-?)?language)\\s*?=\\s*?['\"](.*?)['\"]",
					Pattern.CASE_INSENSITIVE);
	/**
	 * Tries to guess the language(s) declared in the given input file. The
	 * method should work with XLIFF, TMX, TTX and TS files.
	 * <p>
	 * The methods looks in the file line by line, in the 10 first KB, or until
	 * a source and at least one target are detected, whichever comes first.
	 * <p>
	 * The encoding for the file is determined based on the BOM, if present.
	 * @param path
	 *            the full path of the file to process.
	 * @return a list of strings that can be empty (never null). The first
	 *         string is the possible source language, the next strings are the
	 *         potential target languages.
	 */
	public static List<String> guessLanguages(String path) {
		InputStreamReader reader = null;
		String encoding = Charset.defaultCharset().name();
		// Deal with the potential BOM
		try (FileInputStream fis = new FileInputStream(path); 
				BOMAwareInputStream bis = new BOMAwareInputStream(fis, encoding)) {								
			encoding = bis.detectEncoding();			
			reader = new InputStreamReader(fis, encoding);
			return guessLanguages(reader);
		}
		catch (Exception e) {
			throw new OkapiException("Error while trying to guess language information.\n"
							+ e.getLocalizedMessage());
		}
	}

	/**
	 * Tries to guess the language(s) declared in the given input. The
	 * method should work with XLIFF, TMX, TTX and TS files.
	 * <p>
	 * The methods looks in the file line by line, in the 10 first KB, or until
	 * a source and at least one target are detected, whichever comes first.
	 *
	 * @param reader
	 * 		   a reader providing the content to examine.  This reader will be closed
	 *         by this method.
	 * @return a list of strings that can be empty (never null). The first
	 *         string is the possible source language, the next strings are the
	 *         potential target languages.
	 */
	public static List<String> guessLanguages(Reader reader) {
		ArrayList<String> list = new ArrayList<>();

		try {
			final int BYTES_TO_SCAN = 10240*10;
			char[] buffer = new char[BYTES_TO_SCAN];

			// Read the top of the file
			String trgValue = null;

			int readCount = reader.read(buffer, 0, BYTES_TO_SCAN);
			if (readCount <= 0)
				return list;
			String line = new String(buffer, 0, readCount);

			// Else: Try the detect the language codes
			// For XLIFF: source-language, xml:lang, lang, target-language
			// For TMX: srcLang, xml:lang, lang
			// For TTX: SourceLanguage, TargetLanguage, Lang
			// For TS: sourcelanguage, language
			// For TXML: locale, targetlocale
			// Note: the order matter: target cases should be last
			Matcher m = pattern.matcher(line);

			while (m.find()) {
				String lang = m.group(4).toLowerCase();
				if (lang.isEmpty()) {
					continue;
				}
				String name = m.group(1).toLowerCase();

				// If we have a header-type target declaration
				if (name.equals("language") || name.startsWith("target") || name.equals("trglang")) {
					if (list.isEmpty()) {
						// Note that we don't do anything to handle a second
						// match, but that should be OK
						trgValue = lang;
						continue; // Move to the next
					}
					// Else: we can add to the normal list as the source is
					// defined already
				}

				// Else: add the language
				if (!list.contains(lang)) {
					list.add(lang);
				}
				// Then check if we have a target to add. This will be done only
				// once.
				if (trgValue != null) {
					// Add the target
					list.add(trgValue);
					trgValue = null;
				}

				if (list.size() > 1)
					break;
			}

		} catch (Throwable e) {
			throw new OkapiException("Error while trying to guess language information.\n"
							+ e.getLocalizedMessage());
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// Swallow this error
				}
			}			
		}
		return list;
	}

	private static final Pattern XLIFF_SEGMENTATION_PATTERN = Pattern
	        .compile(
	                "<\\s*seg-source\\s*>",
	                Pattern.CASE_INSENSITIVE);

	/**
     * Scans xliff file to see if it is segmented (has &lt;seg-source&gt;)
     * @param path
     *            the full path of the xliff file to process.
     * @return a true if xliff is segmented, false otherwise.
     */
    public static boolean isXliffSegmented(String path) {
        InputStreamReader reader = null;       
        String encoding = Charset.defaultCharset().name();       
        
        // Deal with the potential BOM
		try (FileInputStream fis = new FileInputStream(path); 
				BOMAwareInputStream bis = new BOMAwareInputStream(fis, encoding)) {            
            encoding = bis.detectEncoding();            

            reader = new InputStreamReader(fis, encoding);
            return isXliffSegmented(reader);
		}
		catch (Exception e) {
            throw new OkapiException("Error while trying to find xliff seg-source.\n"
                            + e.getLocalizedMessage());
		}
    }

	/**
     * Scans xliff file to see if it is segmented (has &lt;seg-source&gt;)
	 * @param reader
	 * 		   a reader providing the xliff content to examine.  This reader
	 *         will be closed by this method.
     * @return a true if xliff is segmented, false otherwise.
     */
    public static boolean isXliffSegmented(Reader reader) {
    	try {
            final int BYTES_TO_SCAN = 10240;
            char[] buffer = new char[BYTES_TO_SCAN];

            int readCount = reader.read(buffer, 0, BYTES_TO_SCAN);
            if (readCount <= 0)
                return false;
            String line = new String(buffer, 0, readCount);

            // Else: Try the detect seg-source
            Matcher m = XLIFF_SEGMENTATION_PATTERN.matcher(line);
            if (m.find()) {
                return true;
            }            
            return false;       
        } catch (Throwable e) {
            throw new OkapiException("Error while trying to find xliff seg-source.\n"
                            + e.getLocalizedMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Swallow this error
                }
            }            
        }        
    }

	/**
	 * Deletes all files in the specified directory.
	 * 
	 * @param directoryPath
	 *            - the path to the directory
	 * @throws OkapiIOException
	 *             if a file cannot be deleted.
	 */
	public static void deleteAllFilesInDirectory(String directoryPath) {
		File directory = new File(directoryPath);
		// Get all files in directory
		File[] files = directory.listFiles();

		if (files == null) {
			throw new OkapiIOException("Error finding directory: "
					+ directoryPath);
		}

		for (File file : files) {
			// Delete each file
			if (!file.delete()) {
				throw new OkapiIOException("Error deleting file: "
						+ file.getPath());
			}
		}
	}

	/**
	 * Return a path to a locale based resource using the standard java property resource resolution. Works
	 * with any kind of files e.g., segmenter_en_US.srx or content_fr_FR.html
	 * <p><b>WARNING: Assumes default classLoader only!!</b> 
	 * @param baseName base name of the resource
	 * @param extension resource file extension
	 * @param locale locale of the resource we are looking for
	 * @return the path to the resource or null if not found
	 */
	public static String getLocaleBasedFile(String baseName, final String extension, LocaleId locale) {
		ResourceBundle.Control control = new ResourceBundle.Control() {
			private String resourceFound = null;

			@Override
			public List<String> getFormats(String baseName) {
				return Arrays.asList(extension);
			}

			@Override
			public ResourceBundle newBundle(String baseName, Locale locale, 
							String format, ClassLoader loader, boolean reload) {
				String bundleName = toBundleName(baseName, locale);
				String resourceName = toResourceName(bundleName, format);

				URL r = loader.getResource(resourceName);
				if (r != null) {
					resourceFound = new File(Util.URLtoURI(r)).getPath();					
					return new ResourceBundle() {

						@Override
						public Enumeration<String> getKeys() {
							return null;
						}

						@Override
						protected Object handleGetObject(String key) {
							return null;
						}
					};
				}
				return null;
			}

			@Override
			public String toString() {
				return resourceFound;
			}
		};
		ResourceBundle.clearCache();
		ResourceBundle.getBundle(baseName, locale.toJavaLocale(), control);
		return control.toString();
	}

	/**
	 * Gets the URI part before the file name.
	 * @param uri The URI to process.
	 * @return the URI part before the file name.
	 */
	public static String getPartBeforeFile (URI uri) {
		String tmp = uri.toString();
		int n = tmp.lastIndexOf('/');
		if ( n == -1 ) return uri.toString();
		else return tmp.substring(0, n+1);
	}
	
	public static File createTempFile(String prefix, String extension) {
		try {
			return File.createTempFile(prefix, extension);
		} catch (IOException e) {
			throw new OkapiIOException("Cannot create temporary file.", e);
		}		
	}
	
	public static File createTempFile(String prefix) {
		return createTempFile(prefix, ".tmp");
	}
	
	public static URL fileToUrl(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new OkapiException(e);
		}
	}

	public static File urlToFile(URL url) {
		try {
			return new File(url.toURI());
		} catch (URISyntaxException e) {
			throw new OkapiException(e);
		}
	}
}
