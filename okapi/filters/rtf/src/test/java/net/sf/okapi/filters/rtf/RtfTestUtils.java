package net.sf.okapi.filters.rtf;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.ArrayList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.EndSubfilter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RtfTestUtils {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	static private FileLocation root = FileLocation.fromClass(RtfTestUtils.class);

	public static String[] getTestFiles() {
		// read all files in the test rtf data directory
		File dir = root.in("/").asFile();
		FilenameFilter filter = (dir1, name) -> name.endsWith(".rtf");
		return dir.list(filter);
	}

	public void printEvents(ArrayList<Event> events) {
		for (Event event : events) {
			if (event.getEventType() == EventType.TEXT_UNIT) {
				assertTrue(event.getResource() instanceof ITextUnit);
			} else if (event.getEventType() == EventType.DOCUMENT_PART) {
				assertTrue(event.getResource() instanceof DocumentPart);
			} else if (event.getEventType() == EventType.START_GROUP || event.getEventType() == EventType.END_GROUP) {
				assertTrue(event.getResource() instanceof StartGroup || event.getResource() instanceof Ending);
			} else if (event.getEventType() == EventType.START_SUBFILTER || event.getEventType() == EventType.END_SUBFILTER) {
				assertTrue(event.getResource() instanceof StartSubfilter || event.getResource() instanceof EndSubfilter);
			}
			LOGGER.trace(": {}", event.getEventType().toString());
			if (event.getResource() != null) {
				if (event.getResource() instanceof DocumentPart) {
					LOGGER.trace(((DocumentPart) event.getResource()).getSourcePropertyNames().toString());
				} else {
					LOGGER.trace(event.getResource().toString());
				}
				if (event.getResource().getSkeleton() != null) {
					LOGGER.trace("\tSkeleton: {}", event.getResource().getSkeleton().toString());
				}
			}
		}
	}

	public void printEvents(String file) {
		try (RTFFilter filter = new RTFFilter();
			 InputStream htmlStream = root.in("/" + file).asInputStream()) {
			filter.open(new RawDocument(htmlStream, "UTF-8", LocaleId.fromString("en")));
			while (filter.hasNext()) {
				Event event = filter.next();
				if (event.getEventType() == EventType.TEXT_UNIT) {
					assertTrue(event.getResource() instanceof ITextUnit);
				} else if (event.getEventType() == EventType.DOCUMENT_PART) {
					assertTrue(event.getResource() instanceof DocumentPart);
				} else if (event.getEventType() == EventType.START_GROUP || event.getEventType() == EventType.END_GROUP) {
					assertTrue(event.getResource() instanceof StartGroup || event.getResource() instanceof Ending);
				} else if (event.getEventType() == EventType.START_SUBFILTER || event.getEventType() == EventType.END_GROUP) {
					assertTrue(event.getResource() instanceof StartSubfilter || event.getResource() instanceof EndSubfilter);
				}
				LOGGER.trace(": {}", event.getEventType().toString());
				if (event.getResource() != null) {
					if (event.getResource() instanceof DocumentPart) {
						LOGGER.trace(((DocumentPart) event.getResource()).getSourcePropertyNames().toString());
					} else {
						LOGGER.trace(event.getResource().toString());
					}
					if (event.getResource().getSkeleton() != null) {
						LOGGER.trace("\tSkeketon: {}", event.getResource().getSkeleton().toString());
					}
				}
			}
		} catch (Exception e) {
			LOGGER.trace(e.toString());
		}
	}
}
