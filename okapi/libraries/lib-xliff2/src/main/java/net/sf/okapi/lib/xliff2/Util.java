/*===========================================================================
  Copyright (C) 2011-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.ExtAttribute;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtElements;
import net.sf.okapi.lib.xliff2.core.IWithNotes;
import net.sf.okapi.lib.xliff2.core.InvalidMarkerOrderException;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Notes;
import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Tags;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.lang.Language;

/**
 * Provides various helper methods for the library.
 */
public class Util {
	
	private static final String VBAR = "|";
	private static final String CDATA_OPENING_BRACKET = "<![CDATA";

	/**
	 * Checks if a string is null or empty.
	 * @param string the string to check.
	 * @return true if the given string is null or empty.
	 */
	public static boolean isNoE (String string) {
		return (( string == null ) || string.isEmpty() );
	}

	/**
	 * Converts a text to an XML-escaped text.
	 * This method assumes the output is in UTF-16 or UTF-8 and that all characters
	 * are supported. It also assumes attribute values are between double-quotes.
	 * <p>Use {@link #toSafeXML(String)} to convert content where &lt;cp&gt; is allowed.
	 * @param text the text to convert.
	 * @param attribute true if the text is to be an XML attribute value.
	 * @return the escaped text.
	 */
	public static String toXML (String text,
		boolean attribute)
	{
		if ( text == null ) return "null";
		StringBuilder tmp = new StringBuilder(text.length());
		for ( int i=0; i<text.length(); i++ ) {
			char ch = text.charAt(i);
			if ( ch == '&' ) tmp.append("&amp;");
			else if ( ch == '<' ) tmp.append("&lt;");
			else if ( attribute && ( ch == '"' )) tmp.append("&quot;");
			else tmp.append(ch);
		}
		return tmp.toString();
	}

	/**
	 * Converts a text to an XML-escaped text and using &lt;cp&gt; elements
	 * for the characters that are invalid in XML.
	 * This method assumes the output is in UTF-16 or UTF-8 and that all characters
	 * are supported. It also assumes the text is an element content, not an attribute value.
	 * The initial angled bracket for an opening CDATA section is not escaped (fixes Okapi issue #527).
	 *
	 * @param text the text to convert.
	 * @return the escaped text.
	 */
	public static String toSafeXML (String text) {
		if ( text == null ) return "null";
		// In XML 1.0 the valid characters are:
		// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
		StringBuilder tmp = new StringBuilder(text.length());
		boolean inCDATAsection = false;

		for ( int i=0; i<text.length(); i++ ) {
			int cp = text.codePointAt(i);
			switch ( cp ) {
			case '&':
				tmp.append("&amp;");
				break;
			case '>':
				if (inCDATAsection && i >= 2 && "]]".equals(text.substring(i-2, i))) {
					tmp.append("&gt;");
					inCDATAsection = false;
				}
				else {
					tmp.append('>');
				}
				break;
			case '<':
				// Is this '<' part of an opening CDATA bracket? If yes, keep track, so that we can escape the corresponding closing bracket
				if ( text.length() >= i + CDATA_OPENING_BRACKET.length() && CDATA_OPENING_BRACKET.equals(text.substring(i, i + CDATA_OPENING_BRACKET.length())) ) {
					inCDATAsection = true;
				}
				tmp.append("&lt;");
				break;
			case 0x0009:
			case 0x000A:
			case 0x000D:
				tmp.append((char)cp);
				continue;
			default:
				if (( cp > 0x001F ) && ( cp < 0xD800 )) {
					tmp.append((char)cp);
					continue;
				}
				if ( cp > 0xD7FF ) {
					if (( cp < 0xE000 ) || ( cp == 0xFFFE ) || ( cp == 0xFFFF )) {
						tmp.append(String.format("<cp hex=\"%04X\"/>", cp));
					}
					else {
						tmp.append(Character.toChars(cp));
						i++; // Skip second char of the pair
					}
					continue;
				}
				// Else: control characters
				tmp.append(String.format("<cp hex=\"%04X\"/>", cp));
				continue;
			}
		}
		return tmp.toString();
	}

	/**
	 * Compares two strings allowing them to be null.
	 * @param s1 the first string.
	 * @param s2 the second string.
	 * @return 0 if the two string are identical.
	 */
	public static int compareAllowingNull (String s1,
		String s2)
	{
		if ( s1 == null ) {
			if ( s2 == null ) return 0;
			else return -1;
		}
		if ( s2 == null ) {
			return 1;
		}
		return s1.compareTo(s2);
		
	}

	/**
	 * Checks if two objects are equal, even if either is null.
	 * @param o1 the first object (can be null).
	 * @param o2 the second object (can be null).
	 * @return true if the objects are equals, false otherwise.
	 */
	public static boolean equals (Object o1,
		Object o2)
	{
		if ( o1 == null ) {
			if ( o2 == null ) return true;
			else return false;
		}
		if ( o2 == null ) {
			return false;
		}
		return o1.equals(o2);
	}
	
	/**
	 * Checks if a given value is in a given list of allowed values.
	 * @param allowedValues list of allowed values (each value must be separated by a semicolon). 
	 * @param value the value to check.
	 * @param name the name of the object to check (e.g. name of the attribute).
	 * @return true if the value is allowed and not null, false if it's null.
	 * @throws InvalidParameterException if the value is invalid.
	 */
	public static boolean checkValueList (String allowedValues,
		String value,
		String name)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if (!(";" + allowedValues + ";").contains(";" + value + ";")) {
			throw new InvalidParameterException(String.format("The value '%s' is not allowed for '%s'.", value, name));
		}
		return true;
	}
	
	/**
	 * Creates a list of sequences of code markers that must not be re-ordered
	 * from the list of all code markers in a given source or target content.
	 * @param list the list of code markers to build from.
	 * @param errorOnMissingFirstNo true to throw an exception if a 'no' is detected instead of a 'firstNo'
	 * as a start of a sequence, false to assume it's a firstNo and process without error.
	 * @return a list of fixed sequences.
	 * <p>Entries in the list are separated by a '|' and are made of a prefix 'c' for closing marker or 'z' for opening or placeholder,
	 * followed by the id of the code. the list ends with a '|'.
	 * @throws InvalidParameterException if errorOnMissingFirstNo is true and a firstNo is missing.
	 */
	public static List<String> createFixedSequences (List<CTag> list,
		boolean errorOnMissingFirstNo)
	{
		ArrayList<String> sequences = new ArrayList<>();
		StringBuilder tmp = new StringBuilder();
		boolean inSeq = false;
		int count = 0;
		for (CTag c : list) {
			// We can use the same prefix for opening and placeholder because they have different ids.
			String candidate = (c.getTagType() == TagType.CLOSING ? "c" : "z") + c.getId() + VBAR;
			if (inSeq) {
				switch (c.getCanReorder()) {
					case YES:
						// Ends a fixed sequence
						sequences.add(tmp.toString());
						if (count == 1) {
							//TODO: warning?
						}
						inSeq = false;
						break;
					case FIRSTNO:
						// Ends previous sequence
						sequences.add(tmp.toString());
						if (count == 1) {
							//TODO: warning?
						}
						// Start the new one
						tmp.setLength(0);
						count = 0;
						// Fall through
					case NO:
						// In both case no and firstNo: add the marker to the sequence
						tmp.append(candidate);
						count++;
						break;
				}
			} else {
				switch (c.getCanReorder()) {
					case YES:
						// Nothing to do
						break;
					case NO:
						// A sequence shouldn't start with 'no', but 'firstNo'
						// We can still work with it but we have the option of throwing an exception
						if (errorOnMissingFirstNo) {
							throw new InvalidParameterException(String.format(
									"Marker id='%s' in has canReorder='no' but starts a sequence and should have canReorder='firstNo'.", c.getId()));
						}
						// Else: assume it was meant to be a firstNo
					case FIRSTNO:
						// Start a fixed sequence
						tmp.setLength(0);
						tmp.append(candidate);
						count = 1;
						inSeq = true;
						break;
				}
			}
		}
		// Handles the case of a single sequence
		if ( inSeq ) {
			sequences.add(tmp.toString());
			if ( count == 1 ) {
				//TODO: warning?
			}
		}
		return sequences;
	}
	
	/**
	 * Verifies if the sequences of codes that cannot be re-ordered have been preserved.
	 * <p>Use {@link #createFixedSequences(List, boolean)} to create the lists to compare.
	 * @param original the list of the inline codes of the original state.
	 * @param toVerify the list of the inline codes of the state to verify.
	 * @param errorOnMissingFirstNo true to throw an exception if a 'no' is detected instead of a 'firstNo'
	 * as a start of a sequence, false to assume it's a firstNo and process without error.
	 * @throws InvalidMarkerOrderException if one marker of the list to verify is out-of-order.
	 * @see #createFixedSequences(List, boolean)
	 */
	public static void verifyReordering (List<CTag> original,
		List<CTag> toVerify,
		boolean errorOnMissingFirstNo)
	{
		// Create the lists of fixed sequences
		List<String> oriSequences = createFixedSequences(original, errorOnMissingFirstNo);
		List<String> newSequences = createFixedSequences(toVerify, errorOnMissingFirstNo);
		// Compare the lists of sequences
		for (String ori : oriSequences) {
			if (newSequences.contains(ori)) {
				// Sequence found: remove it
				newSequences.remove(ori);
			} else {
				// Get the id of the first marker
				String startId = ori.substring(1, ori.indexOf(VBAR));
				throw new InvalidMarkerOrderException(
						String.format("The sequence of inline codes that cannot be re-ordered starting with the tag id='%s' has been modified.", startId));
			}
		}
	}
	
	/**
	 * Verifies if a given value is a valid NMTOKEN value.
	 * Empty and null values are not valid.
	 * @param value the value to check.
	 * @return true if it is a valid NMTOKEN, false otherwise.
	 */
	public static boolean isValidNmtoken (String value) {
		if ( isNoE(value) ) return false;
		// See http://www.w3.org/TR/REC-xml/#NT-Nmtoken
		// ":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] | [#x370-#x37D] | [#x37F-#x1FFF]
		// | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] | [#xF900-#xFDCF]
		// | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
		// | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
		final int length = value.length();
		for (int i=0; i<length; ) {
			final int cp = value.codePointAt(i);
			switch ( cp ) {
			case ':':
			case '_':
			case '-':
			case '.':
			case '\u00b7':
				break;
			default:
				if (( cp >= 'A' && cp <= 'Z' ) || ( cp >= 'a' && cp <= 'z' )
					|| ( cp >= '0' && cp <= '9' )
					|| ( cp >= '\u00c0' && cp <= '\u00d6' )
					|| ( cp >= '\u00d8' && cp <= '\u00f6' )
					|| ( cp >= '\u00f8' && cp <= '\u02ff' )
					|| ( cp >= '\u0370' && cp <= '\u037d' )
					|| ( cp >= '\u037f' && cp <= '\u1fff' )
					|| ( cp >= '\u200c' && cp <= '\u200d' )
					|| ( cp >= '\u2070' && cp <= '\u218f' )
					|| ( cp >= '\u2c00' && cp <= '\u2fef' )
					|| ( cp >= '\u3001' && cp <= '\ud7ff' )
					|| ( cp >= '\uf900' && cp <= '\ufdcf' )
					|| ( cp >= '\ufdf0' && cp <= '\ufffd' )
					|| ( cp >= '\u0300' && cp <= '\u036f' )
					|| ( cp >= '\u203f' && cp <= '\u2040' )
					|| ( cp >= 0x10000 && cp <= 0xeffff ))
				{
					break;
				}
				else {
					return false;
				}
			}
			i += Character.charCount(cp);
		}
		return true;
	}

	/**
	 * Removes extension attributes and elements from a given object.
	 * Attributes and elements that are modules are not removed.
	 * Note that the extensions on the objects inside the given object are also removed.
	 * For example if the given object is a unit and it has a note with extensions, those extensions
	 * will be removed as well.
	 * @param object the object from where the extension must be removed.
	 */
	public static void removeExtensions (Object object) {
		// Remove the extension attributes on this object
		if ( object instanceof IWithExtAttributes ) {
			Iterator<ExtAttribute> iter = ((IWithExtAttributes)object).getExtAttributes().iterator();
			while ( iter.hasNext() ) {
				ExtAttribute attr = iter.next();
				if ( !attr.isModule() ) iter.remove();
			}
		}
		// Remove the extension elements on this object
		if ( object instanceof IWithExtElements ) {
			Iterator<ExtElement> iter = ((IWithExtElements)object).getExtElements().iterator();
			while ( iter.hasNext() ) {
				ExtElement elem = iter.next();
				if ( !elem.isModule() ) iter.remove();
			}
		}

		// Case of object with notes
		if ( object instanceof IWithNotes ) {
			Notes notes = ((IWithNotes)object).getNotes();
			removeExtensions(notes);
			for ( Note note : notes ) {
				removeExtensions(note);
			}
		}
		// Case of inline markers in Unit
		if ( object instanceof Unit ) {
			Unit unit = (Unit)object;
			Tags markers = unit.getStore().getSourceTags();
			for ( Tag m : markers ) {
				if ( m.isMarker() ) removeExtensions(m);
			}
			markers = unit.getStore().getTargetTags();
			for ( Tag m : markers ) {
				if ( m.isMarker() ) removeExtensions(m);
			}
		}
	}

	/**
	 * Removes modules attributes and elements from a given object.
	 * Attributes and elements that are extensions are not removed.
	 * Note that the modules on the objects inside the given object are also removed.
	 * @param object the object from where the modules must be removed.
	 * @param moduleSuffix suffix part of the module's namespace to remove, or null for all modules.
	 */
	public static void removeModules (Object object,
		String moduleSuffix)
	{
		// Remove the module attributes on this object
		if ( object instanceof IWithExtAttributes ) {
			Iterator<ExtAttribute> iter = ((IWithExtAttributes)object).getExtAttributes().iterator();
			while ( iter.hasNext() ) {
				ExtAttribute attr = iter.next();
				if ( isModule(attr.getNamespaceURI(), moduleSuffix) ) {
					iter.remove();
				}
			}
		}
		// Remove the module elements on this object
		if ( object instanceof IWithExtElements ) {
			Iterator<ExtElement> iter = ((IWithExtElements)object).getExtElements().iterator();
			while ( iter.hasNext() ) {
				ExtElement elem = iter.next();
				if ( isModule(elem.getQName().getNamespaceURI(), moduleSuffix) ) {
					iter.remove();
				}
			}
		}
		// Case of object with notes
		if ( object instanceof IWithNotes ) {
			Notes notes = ((IWithNotes)object).getNotes();
			removeModules(notes, moduleSuffix);
			for ( Note note : notes ) {
				removeModules(note, moduleSuffix);
			}
		}
		// Case of inline markers in Unit
		if ( object instanceof Unit ) {
			Unit unit = (Unit)object;
			Tags tags = unit.getStore().getSourceTags();
			for ( Tag m : tags ) {
				if ( m.isMarker() ) removeModules(m, moduleSuffix);
			}
			tags = unit.getStore().getTargetTags();
			for ( Tag m : tags ) {
				if ( m.isMarker() ) removeModules(m, moduleSuffix);
			}
		}
	}

	/**
	 * Indicates if the given namespace URI is of a given module or not.
	 * @param nsUri the full URI for the namespace to test.
	 * @param moduleSuffix the suffix of the namespace of the given module, use null for all modules.
	 * @return true if the given URI is for the given module, false otherwise. 
	 */
	static public boolean isModule (String nsUri,
		String moduleSuffix)
	{
		if ( nsUri.startsWith(Const.NS_XLIFF_MODSTART) ) {
			if ( moduleSuffix != null ) {
				return nsUri.endsWith(moduleSuffix);
			}
			return true;
		}
		return false;
	}

	/**
	 * Indicates if a given code point is valid in XML or not.
	 * @param cp the code point to evaluate.
	 * @return true if it is valid, false otherwise.
	 */
	static public boolean isValidInXML (int cp) {
		if (( cp > 0x001F ) && ( cp < 0xD800 )) return true; // Most of the time
		if ( cp > 0xD7FF ) {
			if ( cp < 0xE000 ) return false;
			if (( cp == 0xFFFE ) || ( cp == 0xFFFF )) return false;
			if ( cp > 0x10FFFF ) return false;
			return true;
		}
		// Else: control characters
		switch ( cp ) {
		case 0x0009:
		case 0x000a:
		case 0x000d:
			return true;
		}
		return false;
	}
	
	/**
	 * Verifies the copyOf attributes in a given unit.
	 * @param unit the unit to verify.
	 */
	static public void validateCopyOf (Unit unit) {
		Tags tags;
		for ( int i=0; i<2; i++ ) {
			if ( i == 0 ) tags = unit.getStore().getSourceTags();
			else {
				if ( !unit.getStore().hasTargetTag() ) break;
				tags = unit.getStore().getTargetTags();
			}
			// Process for the set of markers
			for ( Tag tag : tags ) {
				if ( tag.isMarker() ) continue;
				CTag ctag = (CTag)tag;
				String copyRef = ctag.getCopyOf();
				if ( copyRef == null ) continue;
				// CopyOf must not be used if the code has original data
				if ( ctag.getDataRef() != null ) {
					throw new XLIFFException(String.format(
						"Code id='%s' cannot, at the same time, be a copy of code id='%s' and have original data (data id='%s').",
						ctag.getId(), copyRef, ctag.getDataRef()));
				}
				// Check that copyOf refers to an existing code
				// copyOf exists only if it's a CMarker, so we can cast safely here
				CTag ori = (CTag)unit.getStore().getTag(copyRef, ctag.getTagType());
				if ( ori == null ) {
					throw new XLIFFException(String.format(
						"The code id='%s' is a copy of a code that does not exists (copyOf='%s').",
						ctag.getId(), copyRef));
				}
				// The reference code must not have canCopy set to no
				if ( !ori.getCanCopy() ) {
					throw new XLIFFException(String.format(
						"The code id='%s' is a copy of a code that must not be copied (copyOf='%s').",
						ctag.getId(), copyRef));
				}
			}
		}
	}

	/**
	 * Verifies the syntax of a language code.
	 * @param lang the language code to verify.
	 * @return null if it is valid, an error message if it is not valid.
	 */
	static public String validateLang (String lang) {
		if ( Util.isNoE(lang) ) return "Null or empty value.";
		try {
			Language.THE_INSTANCE.checkValid(lang);
		}
		catch ( Throwable e ) {
			if  ( e.getMessage().startsWith("Warning:") ) {
				// Warning only
				return null;
			}
			return e.getMessage();
		}
		return null;
	}

	/**
	 * Indicates if a given module is directly supported by this version of the library.
	 * @param nsUri the namespace URI of the module to query.
	 * @return true if the given module is directly supported, false if it is not and you need to
	 * access its data using the extension mechanism.
	 */
	static public boolean supports (String nsUri) {
		switch ( nsUri ) {
		case Const.NS_XLIFF_MATCHES20:
		case Const.NS_XLIFF_GLOSSARY20:
		case Const.NS_XLIFF_METADATA20:
		case Const.NS_XLIFF_VALIDATION20:
		case Const.NS_XLIFF_TRACKING20:
			return true;
		default:
			return false;
		}
	}

}
