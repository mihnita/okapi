package net.sf.okapi.common.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.FileUtil;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.filters.IFilter;

public class BaseXliffCompareIT {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String configId;
	private final String dirName;
	private final List<String> extensions;
	private final LocaleId defaultTargetLocale;
	private final Set<String> knownFailingFiles = new HashSet<>();
	private final String xliffExtractedExtension;
	private boolean recursive = false;

	protected IFilter filter;

	@Rule
	public ErrorCollector errCol = new ErrorCollector();

	public BaseXliffCompareIT(final String configId, final String dirName, final List<String> extensions) {
		this(configId, dirName, extensions, LocaleId.FRENCH);
	}

	public BaseXliffCompareIT(final String configId, final String dirName,
			final List<String> extensions, final LocaleId defaultTargetLocale) {

		this.configId = configId;
		this.dirName = dirName;
		this.extensions = extensions;
		this.defaultTargetLocale = defaultTargetLocale;
		xliffExtractedExtension = configId.startsWith("okf_xliff") ? ".xliff_extracted" : ".xliff";
	}

	public void addKnownFailingFile(final String fileName) {
		knownFailingFiles.add(fileName);
	}
	public void setRecursive(final boolean recursive) {
		this.recursive = recursive;
	}

	public void realTestFiles(final IParameters parameters, final boolean detectLocales)
			throws FileNotFoundException, URISyntaxException {

		// run top level files (without config)
		final Collection<File> fileList = recursive
				? IntegrationtestUtils.getTestFiles(dirName, extensions)
						: IntegrationtestUtils.getTestFilesNoRecurse(dirName, extensions);
		for (final File file : fileList) {
			if (parameters != null) {
				filter.setParameters(parameters);
			}
			runTest(true, detectLocales, file, configId, null, null);
		}

		// run each subdirectory where we assume there is a custom config)
		for (final File d : IntegrationtestUtils.getSubDirs(dirName)) {
			for (final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
				for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), extensions, true)) {
					final String configName = Util.getFilename(c.getAbsolutePath(), false);
					final String customConfigPath = c.getParent();
					runTest(true, detectLocales, file, configName, customConfigPath, d);
				}
			}
		}
	}

	protected void runTest(final boolean segment, final boolean detectLocales, final File file,
			final String configName, final String customConfigPath, final File subDir)
					throws FileNotFoundException, URISyntaxException {

		final String f = file.getName();
		logger.debug(f);
		final String root = file.getParent() + File.separator;
		final String xliff = root + f + xliffExtractedExtension;
		final String original = root + f;
		final String sd = ((subDir == null) ? "" : subDir.getName() + "/");
		final String currentXliffRoot = new File(getClass().getResource("/XLIFF_PREV/dummy.txt").getPath()).getParent();
		final String xliffPrevious = currentXliffRoot + dirName + sd + f + xliffExtractedExtension;
		LocaleId source = LocaleId.ENGLISH;
		LocaleId target = defaultTargetLocale;
		if (detectLocales) {
			final List<String> locales = FileUtil.guessLanguages(file.getAbsolutePath());
			if (locales.size() >= 1) {
				source = LocaleId.fromString(locales.get(0));
			}
			if (locales.size() >= 2) {
				target = LocaleId.fromString(locales.get(1));
			}
		}

		try {
			RoundTripUtils.extract(source, target, original, xliff, configName, customConfigPath, segment, false);
			assertTrue("Compare Events: " + f,
					FileComparator.accurateXmlFileCompare(Paths.get(xliff), Paths.get(xliffPrevious)));
			/*
			 * FIXME: Merge, extract merged and compare to the xliff (there are some test
			 * failures we need to look at) RoundTripUtils.merge(source, target, false,
			 * original, xliff, tkitMerged, configName, customConfigPath);
			 * RoundTripUtils.extract(source, target, tkitMerged, xliff, configName,
			 * customConfigPath, segment, false); assertTrue("Compare Events: " + f,
			 * FileComparator.accurateXmlFileCompare(Paths.get(xliff),
			 * Paths.get(xliffPrevious)));
			 */
		} catch (final Throwable e) {
			if (!knownFailingFiles.contains(f)) {
				errCol.addError(new OkapiException(f, e));
			} else {
				logger.info("Ignored known failing file: {}", f);
			}
		}
	}
}
