package net.sf.okapi.common.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
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

abstract public class BaseRoundTripSimplifyTkitsIT {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final String configId;
	private final String dirName;
	private final List<String> extensions;
	private final LocaleId defaultTargetLocale;
	private final Set<String> knownFailingFiles = new HashSet<>();
	private final String xliffExtractedExtension;

	protected IFilter filter;

	@Rule
	public ErrorCollector errCol = new ErrorCollector();

	public BaseRoundTripSimplifyTkitsIT(final String configId, final String dirName,
			final List<String> extensions, final String xliffExtractedExtension) {

		this(configId, dirName, extensions, xliffExtractedExtension, LocaleId.FRENCH);
	}

	public BaseRoundTripSimplifyTkitsIT(final String configId, final String dirName,
			final List<String> extensions, final String xliffExtractedExtension, final LocaleId defaultTargetLocale) {

		this.configId = configId;
		this.dirName = dirName;
		this.extensions = extensions;
		this.defaultTargetLocale = defaultTargetLocale;
		this.xliffExtractedExtension = xliffExtractedExtension;
	}

	public void addKnownFailingFile(final String fileName) {
		knownFailingFiles.add(fileName);
	}

	public void realTestFiles(final IParameters parameters, final boolean detectLocales, final IFileComparator comparator)
			throws FileNotFoundException, URISyntaxException {

		// run top level files (without config)
		for (final File file : IntegrationtestUtils.getTestFiles(dirName, extensions)) {
			if (parameters != null) {
				filter.setParameters(parameters);
			}
			runTest(true, detectLocales, file, configId, null, comparator);
		}

		// run each subdirectory where we assume there is a custom config)
		for (final File d : IntegrationtestUtils.getSubDirs(dirName)) {
			for (final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
				for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), extensions, true)) {
					final String configName = Util.getFilename(c.getAbsolutePath(), false);
					final String customConfigPath = c.getParent();
					runTest(true, detectLocales, file, configName, customConfigPath, comparator);
				}
			}
		}
	}

	protected void runTest(final boolean segment, final boolean detectLocales, final File file,
			final String configName, final String customConfigPath, final IFileComparator comparator)
					throws FileNotFoundException, URISyntaxException {
		final String f = file.getName();
		logger.debug(f);
		final String root = file.getParent() + File.separator;
		final String xliff = root + f + xliffExtractedExtension;
		final String original = root + f;
		final String tkitMerged = root + f + ".tkitMerged";
		final String merged = root + f + ".merged";

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
			RoundTripUtils.merge(source, target, false, original, xliff, merged, configName, customConfigPath);

			RoundTripUtils.extract(source, target, original, xliff, configName, customConfigPath, segment, true);
			RoundTripUtils.merge(source, target, false, original, xliff, tkitMerged, configName, customConfigPath);

			assertTrue("Compare Lines: " + f, comparator.compare(Paths.get(merged), Paths.get(tkitMerged)));
		} catch(final Throwable e) {
			if (!knownFailingFiles.contains(f)) {
				errCol.addError(new OkapiException(f, e));
				logger.error("Failing test: {}\n{}", f, e.getMessage());
			} else {
				logger.info("Ignored known failing file: {}", f);
			}
		}
	}
}
