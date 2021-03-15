package net.sf.okapi.common.integration;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Policy.Parameters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.FileUtil;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.ZipFileCompare;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

public final class IntegrationtestUtils {
	final static FileLocation root = FileLocation.fromClass(IntegrationtestUtils.class);
	public static String GOLD_DIR = "/GOLDEN_COMPARE/gold_dummy.txt";

	/**
	 * Standard Okapi xliff output
	 * 
	 * @param events
	 * @param path
	 */
	public void writeXliff(List<Event> events, String path) {

		try (XLIFFWriter w = new XLIFFWriter()) {
			w.setOptions(LocaleId.ENGLISH, "UTF-8");
			w.setOutput(path);
			for (Event event : events) {
				w.handleEvent(event);
			}
		}
	}

	public static Collection<File> getTestFiles(String resourcePath,
			final List<String> extensions, boolean isDirPath)
			throws URISyntaxException {
		File dir;
		if (isDirPath) {
			dir = new File(resourcePath);
		} else {
			dir = root.in(resourcePath).asFile();
		}

		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String e : extensions) {
					if (name.endsWith(e)) {
						return true;
					}
				}
				return false;
			}
		};
		return FileUtil.getFilteredFiles(dir, filter, true);
	}

	public static Collection<File> getTestFilesNoRecurse(String resourcePath, final List<String> extensions)
			throws URISyntaxException {
		File dir = root.in(resourcePath).asFile();

		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String e : extensions) {
					if (name.endsWith(e)) {
						return true;
					}
				}
				return false;
			}
		};
		return FileUtil.getFilteredFiles(dir, filter, false);
	}

	public static Collection<File> getTestFiles(String resourcePath,
			final List<String> extensions) throws URISyntaxException {
		return getTestFiles(resourcePath, extensions, false);
	}
	
	public static String[] getTestFilesNames(String resourcePath, final List<String> extensions) throws URISyntaxException {
		Collection<File> files = getTestFiles(resourcePath, extensions);
		String[] paths = new String[files.size()];
		int i = 0;
		for (File f : files) {
			paths[i++] = f.getName();
		}
		return paths;
	}


	public static Collection<File> getSubDirs(String parentDirPath) throws URISyntaxException {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return new File(dir, name).isDirectory();
			}
		};
		return FileUtil
				.getFilteredFiles(root.in(parentDirPath).asFile(), filter, false);
	}

	public static File getSecondaryConfigFile(String resourcePath,	String primaryConfigName) throws URISyntaxException {
		// secondary must have .secondary extension
		List<String> extensions = asList(".secondary.fprm", ".secondary.its");
		Collection<File> secondaries = getTestFiles(resourcePath, extensions, true);
		for (File file : secondaries) {
			String p = Util.getFilename(primaryConfigName, false);
			// return the first one - only one .secondary per subdir
			return file;
		}

		return null;
	}

	public static Collection<File> getConfigFile(String resourcePath)
			throws URISyntaxException {
		List<String> extensions = asList(".fprm", ".its");
		Collection<File> primaries = new LinkedList<File>();
		for (File f : getTestFiles(resourcePath, extensions, true)) {
			String p = Util.getFilename(f.getName(), true);
			if (p.endsWith(".secondary.fprm") || p.endsWith(".secondary.its")) {
				continue;
			}
			primaries.add(f);
		}

		return primaries;
	}
	
	public static File asFile(Class<?> testClass, String rootPath, final String fileName) throws URISyntaxException, IOException {
		
		try (JarFile jarFile = new JarFile(rootPath)) {
			final Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				final String name = entry.getName();
				if (fileName.equals(name)) {
					jarFile.close();
					return new File(Util.buildPath(rootPath, name));
				}
			}
		} catch (IOException | SecurityException e) {
			for (String fname : new File(rootPath).list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (fileName.equals(name)) {						
						return true;
					}
					return false;
				}
			})) {
				String fpath = Util.buildPath(rootPath, fname);
				return new File(fpath);
			}
		}

		return null;
	}

	static public ArrayList<Event> getEvents(IFilter filter, RawDocument rd) {
		ArrayList<Event> list = new ArrayList<Event>();
		try {
			filter.open(rd);
			while (filter.hasNext()) {
				list.add(filter.next());
			}
		} finally {
			filter.close();
			rd.close();
		}
		return list;
	}

	static public ArrayList<Event> getNonTextUnitEvents(IFilter filter,
			RawDocument rd) {
		ArrayList<Event> list = new ArrayList<Event>();
		try {
			filter.open(rd);
			while (filter.hasNext()) {
				Event e = filter.next();
				if (!e.isTextUnit()) {
					list.add(e);
				}
			}
		} finally {
			filter.close();
			rd.close();
		}
		return list;
	}

	static public ArrayList<Event> getEvents(String snippet, IFilter filter,
			Parameters paramters) {
		ArrayList<Event> list = new ArrayList<Event>();

		if (paramters != null) {
			filter.setParameters((IParameters) paramters);
		}
		try (RawDocument rawDoc = new RawDocument(snippet, LocaleId.ENGLISH)) {
			filter.open(rawDoc);
			while (filter.hasNext()) {
				Event event = filter.next();
				list.add(event);
			}
		} finally {
			filter.close();
		}
		return list;
	}

	static public boolean compareZipWithGoldFile(String root, String outputBase) {
		ZipFileCompare zfc = new ZipFileCompare();
		String outputPath = root + File.separator + outputBase;
		String goldPath = root + File.separator + "gold" + File.separator
				+ outputBase;
		return zfc.compareFilesPerLines(outputPath, goldPath, "UTF-8", true);
	}

	static public boolean compareWithGoldFile(String root, String outputBase)
			throws FileNotFoundException {
		FileCompare fc = new FileCompare();
		String outputPath = root + File.separator + outputBase;
		String goldPath = root + File.separator + "gold" + File.separator
				+ outputBase;
		return fc.compareFilesPerLines(outputPath, goldPath, "UTF-8");
	}

	static public boolean compareWithGoldFile(String root, String outputBase,
			String goldBase) throws FileNotFoundException {
		FileCompare fc = new FileCompare();
		String outputPath = root + File.separator + outputBase;
		String goldPath = root + File.separator + "gold" + File.separator
				+ goldBase;
		return fc.compareFilesPerLines(outputPath, goldPath, "UTF-8");
	}

	public static boolean deleteDirRecursive(String path) {
		File d;
		d = new File(path);
		if (d.isDirectory()) {
			String[] children = d.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDirRecursive(d.getAbsolutePath()
						+ File.separator + children[i]);
				if (!success) {
					return false;
				}
			}
		}
		if (d.exists())
			return d.delete();
		else
			return true;
	}

	static public ArrayList<Event> getTextUnitEvents(IFilter filter,
			RawDocument rd) {
		ArrayList<Event> list = new ArrayList<Event>();
		try {
			filter.open(rd);
			while (filter.hasNext()) {
				Event e = filter.next();
				if (e.isTextUnit()) {
					list.add(e);
				}
			}
		} finally {
			filter.close();
			rd.close();
		}
		return list;
	}

	static public ArrayList<Event> removeEmptyTextUnits(List<Event> events) {
		ArrayList<Event> list = new ArrayList<Event>();
		for (Event event : events) {
			if (event.isTextUnit()) {
				ITextUnit tu = event.getTextUnit();
				if (tu.isEmpty()) {
					continue;
				}
			}

			list.add(event);
		}

		return list;
	}
}
