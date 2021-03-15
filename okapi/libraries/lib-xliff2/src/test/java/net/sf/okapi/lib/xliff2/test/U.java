package net.sf.okapi.lib.xliff2.test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.its.ITSWriter;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

/**
 * Provides a set of utility functions for testing.
 */
public class U {

	public static final String STARTDOCWITHITS = "<?xml version=\"1.0\"?>\n"
		+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\" "
		+ "xmlns:its=\"http://www.w3.org/2005/11/its\" xmlns:itsxlf=\"http://www.w3.org/ns/its-xliff/\" "
		+ "its:version=\"2.0\">\n";
			
	public static final String STARTDOC = "<?xml version=\"1.0\"?>\n"
		+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\" version=\"2.0\" srcLang=\"en\" trgLang=\"fr\">\n";

	/**
	 * Format the markers in a fixed representation.
	 * @param codedText the coded text to format.
	 * @return the representation.
	 */
	public static String fmtMarkers (String codedText) {
		StringBuilder tmp = new StringBuilder();
		for ( int i=0; i<codedText.length(); i++ ) {
			if ( Fragment.isChar1(codedText.charAt(i)) ) {
				switch ( codedText.charAt(i) ) {
				case Fragment.MARKER_OPENING:
					tmp.append("{oA");
					break;
				case Fragment.MARKER_CLOSING:
					tmp.append("{cA");
					break;
				case Fragment.CODE_OPENING:
					tmp.append("{oC");
					break;
				case Fragment.CODE_CLOSING:
					tmp.append("{cC");
					break;
				case Fragment.CODE_STANDALONE:
					tmp.append("{hC");
					break;
				case Fragment.PCONT_STANDALONE:
					tmp.append("{$");
					break;
				}
				tmp.append("}");
				i++; // Skip index
			}
			else {
				tmp.append(codedText.charAt(i));
			}
		}
		return tmp.toString();
	}

	/**
	 * Format the markers in a fixed representation.
	 * @param frag the fragment to format.
	 * @return the representation.
	 */
	public static String fmtWithIDs (Fragment frag) {
		StringBuilder tmp = new StringBuilder();
		String ct = frag.getCodedText();
		for ( int i=0; i<ct.length(); i++ ) {
			if ( Fragment.isChar1(ct.charAt(i)) ) {
				Tag tag = frag.getTags().get(ct, i);
				switch ( ct.charAt(i) ) {
				case Fragment.MARKER_OPENING:
					tmp.append("{om:"+tag.getId());
					break;
				case Fragment.MARKER_CLOSING:
					tmp.append("{cm:"+tag.getId());
					break;
				case Fragment.CODE_OPENING:
					tmp.append("{oc:"+tag.getId());
					break;
				case Fragment.CODE_CLOSING:
					tmp.append("{cc:"+tag.getId());
					break;
				case Fragment.CODE_STANDALONE:
					tmp.append("{hc:"+tag.getId());
					break;
				case Fragment.PCONT_STANDALONE:
					tmp.append("{$");
					break;
				}
				tmp.append("}");
				i++;
			}
			else {
				tmp.append(ct.charAt(i));
			}
		}
		return tmp.toString();
	}

	public static List<Event> getEvents (String text) {
		List<Event> events = new ArrayList<>();
		try ( XLIFFReader reader = new XLIFFReader() ) {
			reader.open(text);
			while ( reader.hasNext() ) {
				events.add(reader.next());
			}
		}
		return events;
	}

	public static Unit getUnit (List<Event> list) {
		for ( Event event : list ) {
			if ( event.isUnit() ) {
				return event.getUnit();
			}
		}
		return null;
	}
	
	public static Unit getUnit (String text) {
		return getUnit(getEvents(text));
	}

	public static String writeEvents (List<Event> events) {
		StringWriter sw = new StringWriter();
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			writer.setLineBreak("\n");
			writer.create(sw, null);
			for ( Event event :events ) {
				writer.writeEvent(event);
			}
		}
		return sw.toString();
	}

	public static String writeUnit (Unit unit,
		String sourceLang,
		String targetLang)
	{
		StringWriter sw = new StringWriter();
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			writer.setLineBreak("\n");
			writer.create(sw, sourceLang, targetLang);
			StartXliffData sxd = new StartXliffData("2.0");
			ITSWriter.addDeclaration(sxd);
			writer.writeStartDocument(sxd, null);
			writer.writeUnit(unit);
		}
		return sw.toString();
	}

	/**
	 * Converts a given index value for an opening-code marker to its key.
	 * @param value the index value.
	 * @return the key.
	 */
	static public int kOC (int value) {
		return Fragment.toKey(Fragment.CODE_OPENING, Fragment.TAGREF_BASE+value);
	}
	
	/**
	 * Converts a given index value for an closing-code marker to its key.
	 * @param value the index value.
	 * @return the key.
	 */
	static public int kCC (int value) {
		return Fragment.toKey(Fragment.CODE_CLOSING, Fragment.TAGREF_BASE+value);
	}
	
	/**
	 * Converts a given index value for a standalone-code marker to its key.
	 * @param value the index value.
	 * @return the key.
	 */
	static public int kSC (int value) {
		return Fragment.toKey(Fragment.CODE_STANDALONE, Fragment.TAGREF_BASE+value);
	}

	/**
	 * Converts a given index value for an opening-annotation marker to its key.
	 * @param value the index value.
	 * @return the key.
	 */
	static public int kOA (int value) {
		return Fragment.toKey(Fragment.MARKER_OPENING, Fragment.TAGREF_BASE+value);
	}
	
	/**
	 * Converts a given index value for an closing-annotation marker to its key.
	 * @param value the index value.
	 * @return the key.
	 */
	static public int kCA (int value) {
		return Fragment.toKey(Fragment.MARKER_CLOSING, Fragment.TAGREF_BASE+value);
	}
	
	/**
	 * Converts a given index value for a standalone-protected content marker to its key.
	 * @param value the index value.
	 * @return the key.
	 */
	static public int kSP (int value) {
		return Fragment.toKey(Fragment.PCONT_STANDALONE, Fragment.TAGREF_BASE+value);
	}

	/**
	 * Gets the target fragment for a given part, or its source if the target does not exists.
	 * <p><b>Warning:</b> If the source is returned, this does not create a new content,
	 * so the markers in the returned fragment are the one of the source not a possible target,
	 * and any modifications is done in the source not the target.
	 * @param part the part where to the get target or source from.
	 * @return the target or source fragment, never null.
	 */
	static public Fragment getTargetOrSource (Part part) {
		if ( !part.hasTarget() ) return part.getSource();
		else return part.getTarget();
	}

}
