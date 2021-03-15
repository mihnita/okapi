/**
 * 
 */
package net.sf.okapi.common.integration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileUtil;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.RawDocument;

/**
 * @author jimh
 *
 */
public class EventRoundTripIT extends StricterRoundTripIT {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * @param configId
	 * @param dirName
	 * @param extensions
	 */
	public EventRoundTripIT(final String configId, final String dirName, final List<String> extensions) {
		super(configId, dirName, extensions);
	}

	/**
	 * @param configId
	 * @param dirName
	 * @param extensions
	 * @param defaultTargetLocale
	 */
	public EventRoundTripIT(final String configId, final String dirName, final List<String> extensions, final LocaleId defaultTargetLocale) {
		super(configId, dirName, extensions, defaultTargetLocale);
	}

	public void realTestFiles(final IParameters parameters, final boolean detectLocales, final IEventComparator comparator)
			throws FileNotFoundException, URISyntaxException {

		// run top level files (without config)
		for (final File file : IntegrationtestUtils.getTestFiles(dirName, extensions)) {
			if (parameters != null) {
				filter.setParameters(parameters);
			}
			runTest(false, detectLocales, file, configId, null, comparator);
			runTest(true, detectLocales, file, configId, null, comparator);
		}

		// run each subdirectory where we assume there is a custom config)
		for (final File d : IntegrationtestUtils.getSubDirs(dirName)) {
			for (final File c : IntegrationtestUtils.getConfigFile(d.getPath())) {
				for (final File file : IntegrationtestUtils.getTestFiles(d.getPath(), extensions, true)) {
					final String configName = Util.getFilename(c.getAbsolutePath(), false);
					final String customConfigPath = c.getParent();
					runTest(false, detectLocales, file, configName, customConfigPath, comparator);
					runTest(true, detectLocales, file, configName, customConfigPath, comparator);
				}
			}
		}
	}

	public void runTest(final boolean segment, final boolean detectLocales, final File file, final String configName,
			final String customConfigPath, final IEventComparator comparator) throws FileNotFoundException, URISyntaxException {
		final String f = file.getName();
		logger.debug(f);
		final String root = file.getParent() + File.separator;
		final String xliff = root + f + xliffExtractedExtension;
		final String original = root + f;
		final String tkitMerged = root + f + ".tkitMerged";
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
			RoundTripUtils.extract(source, target, original, xliff, configName, customConfigPath, segment);
			RoundTripUtils.merge(source, target, false, original, xliff, tkitMerged, configName, customConfigPath);

			try (RawDocument ord = new RawDocument(Util.toURI(original), "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH);
					RawDocument trd = new RawDocument(Util.toURI(tkitMerged), "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH)) {
				final List<Event> o = IntegrationtestUtils.getEvents(filter, ord);
				final List<Event> t = IntegrationtestUtils.getEvents(filter, trd);
				assertTrue("Compare Lines: " + f, comparator.compare(o, t));
			} 
		} catch (final Throwable e) {
			if (!knownFailingFiles.contains(f)) {
				errCol.addError(new OkapiTestException(f, e));
				logger.error("Failing test: {}\n{}", f, e.getMessage());
			} else {
				logger.info("Ignored known failing file: {}", f);
			}
		} 

	}
}
