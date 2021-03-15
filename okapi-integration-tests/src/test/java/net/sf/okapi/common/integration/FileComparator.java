package net.sf.okapi.common.integration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;

public class FileComparator {
	private final static Logger LOGGER = LoggerFactory.getLogger(FileComparator.class);

	private FileComparator() {
		// don't instantiate this utility class
	}

	public static boolean utf8FilePerLineComparator(final Path actual, final Path expected) {
		return new FileCompare().compareFilesPerLines(actual.toString(), expected.toString(), StandardCharsets.UTF_8.name());
	}

	public static boolean eventCompare(final List<Event> actual, final List<Event> expected) { 
		return RoundTripUtils.compareEvents(actual, expected, false, true, false, true);
	}

	public static boolean zipAccurateXMLFileComparator(final Path actual, final Path expected) {
		final ArchiveFileCompare ac = new ArchiveFileCompare(FileComparator::accurateXmlFileCompare);
		return ac.compareFiles(actual, expected);
	}

	public static <T> boolean accurateXmlFileCompare(final T actual, final T expected) {
		final Diff documentDiff;
		try {
			documentDiff = RoundTripUtils.compareXml(actual, expected);
			final boolean fail = documentDiff.hasDifferences();
			if (fail) {
				LOGGER.error("XML Differences:");
				for (final Difference d : documentDiff.getDifferences()) {
					LOGGER.error("+ {}", d.toString());
				}
			}
			return !fail;
		} catch (final ParserConfigurationException e) {
			throw new OkapiBadFilterInputException("XML Parse Error: ", e);
		}
	}
}
