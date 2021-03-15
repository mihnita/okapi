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

package net.sf.okapi.lib.xliff2.core;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.regex.Pattern;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.NSContext;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.its.AnnotatorsRef;
import net.sf.okapi.lib.xliff2.its.ITSWriter;
import net.sf.okapi.lib.xliff2.its.TermTag;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriterException;

/**
 * Represents a fragment of extracted content.
 * A fragment is the content of a source or target in a {@link Part} or {@link Segment}.
 */
public class Fragment implements Iterable<Object>, Appendable {

	/**
	 * First character of the special pair indicating a reference to the opening tag of a code (a {@link CTag}).
	 */
	public static final char CODE_OPENING = '\uE101';
	/**
	 * First character of the special pair indicating a reference to the closing tag of a code (a {@link CTag}).
	 */
	public static final char CODE_CLOSING = '\uE102';
	/**
	 * First character of the special pair indicating a reference to the standalone tag of a code (a {@link CTag}).
	 */
	public static final char CODE_STANDALONE = '\uE103';
	/**
	 * First character of the special pair indicating a reference to the opening tag of a marker (a {@link MTag}).
	 */
	public static final char MARKER_OPENING = '\uE104';
	/**
	 * First character of the special pair indicating a reference to the closing tag of a marker (a {@link MTag}).
	 */
	public static final char MARKER_CLOSING = '\uE105';
	/**
	 * First character of the special pair indicating a reference to the standalone holder 
	 * for a protected content (a {@link PCont}).
	 */
	public static final char PCONT_STANDALONE = '\uE106';

	/**
	 * Base value for the tag reference index.
	 */
	public static final int TAGREF_BASE = 0xE110;
	/**
	 * Maximum number of tag possible per unit for a given type of tag: 6127.
	 */
	public static final int TAGREF_MAX = (0xF8FF-TAGREF_BASE);
	
	/**
	 * Compiled regular expression for all possible kinds of tag reference (the two characters) in a coded text.
	 */
	public static final Pattern TAGREF_REGEX = Pattern.compile("[\uE101\uE102\uE103\uE104\uE105\uE106].");

	private boolean isTarget;
	private StringBuilder ctext;
	private Tags tags;
	private Directionality dir = Directionality.INHERITED;
	
	private transient ITSWriter itsWriter;

	/**
	 * Inner class for the generic iterable.
	 * @param <T> the class of the object to iterate.
	 */
	private class ContentIterable<T> implements Iterable<T> {

		final private Class<T> theClass;
		
		public ContentIterable (Class<T> theClass) {
			this.theClass = theClass;
		}
		
		@Override
		public Iterator<T> iterator () {
			return new ContentIterator<>(theClass);
		}
		
	}

	/**
	 * Inner class for the generic iterator.
	 * @param <T> the class of the object to iterate.
	 */
	private class ContentIterator<T> implements Iterator<T> {

		final private int mode;
		
		private int start, pos;
		private int returnType; // -1: no more, 0=string, 1=tag, 2=protected content
		
		public ContentIterator (Class<T> typeClass) {
			String typeName = typeClass.getName();
			if ( typeName.equals(Object.class.getName()) ) mode = 0;
			else if ( typeName.equals(String.class.getName()) ) mode = 1;
			else if ( typeName.equals(Tag.class.getName()) ) mode = 2;
			else if ( typeName.equals(CTag.class.getName()) ) mode = 3;
			else if ( typeName.equals(MTag.class.getName()) ) mode = 4;
			else if ( typeName.equals(PCont.class.getName()) ) mode = 5;
			else {
				throw new InvalidParameterException("Unsupported iteration type.");
			}
			start = pos = 0;
			returnType = -1;
			findNext();
		}
		
		@Override
		public boolean hasNext () {
			return (returnType != -1);
		}

		@SuppressWarnings("unchecked")
		@Override
		public T next () {
			int posNow = pos;
			int startNow = start;
			switch ( returnType ) {
			case 0: // String
				findNext();
				return (T)ctext.substring(startNow, posNow);
			case 1: // Tag, CTag and MTag
				pos += 2;
				findNext();
				return (T)tags.get(ctext, posNow);
			case 2: // PCont
				pos += 2;
				findNext();
				return (T)tags.getPCont(ctext, posNow);
			default: // Nothing
				return null;
			}
		}

		@Override
		public void remove () {
			throw new UnsupportedOperationException("The method remove() not supported.");
		}
		
		private void findNext () {
			// Search for next tag
			start = pos;
			for ( ; pos < ctext.length(); pos++ ) {
				char ch = ctext.charAt(pos);
				if ( isChar1(ch) ) {
					// Do we have text before?
					// and if we in 'string' and 'object' modes
					if (( start < pos ) && ( mode <= 1 )) {
						// If we do: string to send will be ctext.substring(start, pos);
						returnType = 0;
						return;
					}
					else { // No string before
						if ( mode == 1 ) {
							// 'string' mode: skip the code and look for next one or end
							pos++;
							start = pos+1; // New start is the first char after the code
							continue;
						}
					}
					// Else: look at the tag
					switch ( ch ) {
					case CODE_OPENING:
					case CODE_CLOSING:
					case CODE_STANDALONE:
						if (( mode == 0 ) || ( mode == 2 ) || ( mode == 3 )) {
							returnType = 1;
							return;
						}
						break;
					case MARKER_OPENING:
					case MARKER_CLOSING:
						if (( mode == 0 ) || ( mode == 2 ) || ( mode == 4 )) {
							returnType = 1;
							return;
						}
						break;
					case PCONT_STANDALONE:
						if (( mode == 0 ) || ( mode == 5 )) {
							returnType = 2;
							return;
						}
						break;
					}
				}
			}
			
			// No tag found: for 'string and 'object' modes we send the remaining of the content
			if ( mode <= 1 ) {
				// Next string is the remaining sub-string ctext.substring(start);
				pos = ctext.length();
				if ( start < pos ) returnType = 0;
				else returnType = -1;
			}
			else {
				returnType = -1; // Nothing left
			}
		}
	}
	
	/**
	 * Converts the first and second character of a tag reference to the key of the tag.
	 * <p>Note that both parameters are passed as integer for convenience, but they are characters.
	 * @param c1 the first character of the reference (the type of inline object and the type of tag).
	 * @param c2 the second character of the reference (the encoded 'index' part of the key)
	 * @return the key for the given tag reference.
	 * @see #toChar1(int)
	 * @see #toChar2(int)
	 * @see #toRef(int)
	 */
	public static int toKey (int c1,
		int c2)
	{
		return ((c1 << 16) | c2);
	}

	/**
	 * Gets the first character of a tag reference from a given tag key.
	 * @param key the key to process.
	 * @return the first character of the tag reference for the given tag key.
	 * @see #toChar2(int)
	 * @see #toRef(int)
	 * @see #toKey(int, int)
	 */
	public static char toChar1 (int key) {
		return (char)(key >> 16);
	}
	
	/**
	 * Gets the second character of a tag reference from a given tag key. 
	 * @param key the key to process.
	 * @return the second character of the tag reference for the given tag key.
	 * @see #toChar1(int)
	 * @see #toRef(int)
	 * @see #toKey(int, int)
	 */
	public static char toChar2 (int key) {
		return (char)key;
	}
	
	/**
	 * Converts a tag key to a reference as used in the coded text
	 * @param key the key to convert
	 * @return the tag reference for the given key.
	 * @see #toKey(int, int)
	 * @see #toChar1(int)
	 * @see #toChar2(int)
	 */
	public static String toRef (int key) {
		return ""+(char)(key >> 16)+(char)key;
	}
	
	/**
	 * Helper method that checks if a given character is the first special character
	 * of a tag reference.
	 * If it is true, the next character is the second character of a tag reference and
	 * the key for the tag can be obtained using {@link #toKey(int, int)}. 
	 * @param value the character to check.
	 * @return true if the given character is the first character of a tag reference.
	 */
	public static boolean isChar1 (char value) {
		switch ( value ) {
		case CODE_STANDALONE:
		case CODE_OPENING:
		case CODE_CLOSING:
		case MARKER_OPENING:
		case MARKER_CLOSING:
		case PCONT_STANDALONE:
			return true;
		}
		return false;
	}

	/**
	 * Indicates if a given character is the first special character of a 
	 * {@link CTag} reference.
	 * @param value the character to check.
	 * @return true if the given character denotes a {@link CTag} reference.
	 */
	public static boolean isCTag (char value) {
		switch ( value ) {
		case CODE_STANDALONE:
		case CODE_OPENING:
		case CODE_CLOSING:
			return true;
		}
		return false;
	}
	
	/**
	 * Indicates if a given coded text has any non-marker tags at or after a given position.
	 * @param codedText the coded text to process.
	 * @param position the position where to start checking.
	 * @return true if there is anything other than marker tags at or after the position,
	 * false if text or inline code tags are found.
	 */
	public static boolean hasContentAfter (String codedText,
		int position)
	{
		for ( int i=position; i<codedText.length(); i++ ) {
			if ( Fragment.isChar1(codedText.charAt(position)) ) {
				switch ( codedText.charAt(position) ) {
				case Fragment.MARKER_OPENING:
				case Fragment.MARKER_CLOSING:
					break;
				default: // Other tags count as content
					return true;
				}
				i++;
			}
			else return true;
		}
		return false;
	}

	/**
	 * Gets the coded text position in a given coded text string for a given plain text position.
	 * The conversion is done as with the current state of the coded text, for example
	 * if the fragment has folded non-translatable text it is represented by a tag reference.
	 * @param codedText the coded text character sequence to use as the base for the conversion.
	 * @param plainTextPosition the plain text position to convert.
	 * @param leftOfTag true to return the left side position of the tag reference
	 * when the plain text position is on a tag reference (e.g. for end of range stopping
	 * before the reference). Use false to get the right side. 
	 * @return the coded text position.
	 * @see #getPlainText()
	 * @see #getCodedTextPosition(int, boolean)
	 */
	public static int getCodedTextPosition (CharSequence codedText,
		int plainTextPosition,
		boolean leftOfTag)
	{
		int ct = 0;
		int pt = 0;
		for ( ; ct<codedText.length(); ct++ ) {
			if ( isChar1(codedText.charAt(ct) ) ) {
				if (( pt == plainTextPosition ) && leftOfTag ) {
					return ct;
				}
				ct++;
				continue;
			}
			if ( pt == plainTextPosition ) return ct;
			pt++;
		}
		return ct;
	}

	/**
	 * Copy constructor.
	 * @param original the original fragment to duplicate.
	 * @param store the store to attach to the new fragment.
	 * @param target true if the new fragment is a target, false if it is a source.
	 */
	public Fragment (Fragment original,
		Store store,
		boolean target)
	{
		this(store, target);
		this.dir = original.dir;

		Tags oriTags = original.getTags();
		Tags destTags = getTags();
		StringBuilder tmp = new StringBuilder(original.getCodedText());
		int key;
		for ( int i=0; i<tmp.length(); i++ ) {
			char ch = tmp.charAt(i);
			if ( isChar1(ch) ) {
				if ( ch == PCONT_STANDALONE ) {
					PCont pcont = oriTags.getPCont(tmp, i);
					StringBuilder pct = new StringBuilder(pcont.getCodedText());
					for ( int j=0; j<pct.length(); j++ ) {
						if ( isChar1(pct.charAt(j)) ) {
							Tag tag = oriTags.get(pct, j);
							key = tags.add(CloneFactory.create(tag, destTags));
							pct.replace(j, j+2, toRef(key)); j++;
						}
					}
					key = tags.add(new PCont(pct.toString()));
				} else {
					Tag tag = oriTags.get(tmp, i);
					key = tags.add(CloneFactory.create(tag, destTags));
				}
				tmp.replace(i, i+2, toRef(key)); i++;
			}
		}
		setCodedText(tmp.toString());
	}
	
	/**
	 * Creates a new source or target {@link Fragment} object.
	 * @param store the shared {@link Store} for this object.
	 * @param target true if this fragment is a target fragment, false if it is a source fragment.
	 */
	public Fragment (Store store,
		boolean target)
	{
		if ( store == null ) {
			throw new InvalidParameterException("The store parameter cannot be null.");
		}
		isTarget = target;
		ctext = new StringBuilder();
		if ( isTarget ) this.tags = store.getTargetTags();
		else this.tags = store.getSourceTags();
	}
	
	/**
	 * Creates a new source or target {@link Fragment} object with a content.
	 * @param store the shared {@link Store} for this object.
	 * @param target true if this fragment is a target fragment, false if it is a source fragment.
	 * @param plainText the content of this fragment.
	 */
	public Fragment (Store store,
		boolean target,
		String plainText)
	{
		this(store, target);
		ctext = new StringBuilder(plainText);
	}

	/**
	 * Returns a string representation of the fragment: the text in coded text format.
	 * <p>the coded text format is made of normal content and a pair of special characters for each tag in the content. 
	 * The method has the same effect as calling {@link #getCodedText()}.
	 */
	@Override
	public String toString () {
		return ctext.toString();
	}
	
	/**
	 * Gets a plain text version (all tag references stripped out) of the content of this fragment.
	 * @return the plain text version of this fragment.
	 */
	public String getPlainText () {
		return TAGREF_REGEX.matcher(new String(ctext)).replaceAll("");
	}
	
//	/**
//	 * Gets a text version of the content of this fragment, including the original codes
//	 * when available. The markers are protected content tags are stripped out.
//	 * <p><b>IMPORTANT</b>: The text parts of this representation are presented as parsed,
//	 * not escaped (if the original format requires some characters to be escaped).
//	 * @return the text version of this fragment.
//	 */
//	public String getText () {
//		// Try fastest way first
//		if ( tags.size() == 0 ) {
//			return ctext.toString();
//		}
//		// If there may be tags: build the string
//		StringBuilder tmp = new StringBuilder(ctext.length());
//		for ( int i=0; i<ctext.length(); i++ ) {
//			char ch = ctext.charAt(i);
//			switch ( ch ) {
//			case CODE_OPENING:
//			case CODE_CLOSING:
//			case CODE_STANDALONE:
//				CTag ct = (CTag)tags.get(ctext, i); i++;
//				if ( ct.getData() != null ) tmp.append(ct.getData());
//				break;
//			case MARKER_OPENING:
//			case MARKER_CLOSING:
//				i++; // Skip it
//				break;
//			case PCONT_STANDALONE:
//				i++; // Skip it
//			default:
//				tmp.append(ch);
//				break;
//			}
//		}
//		return tmp.toString();
//	}
	
	/**
	 * Gets the content of this fragment in coded text format.
	 * <p>the coded text format is made of normal content and a pair of special characters for each tag in the content. 
	 * @return the coded text content for this fragment.
	 */
	public String getCodedText () {
		return ctext.toString();
	}

	/**
	 * Sets the coded text content for this fragment.
	 * IMPORTANT: the corresponding tags should be set too, at the store level, for all the fragments. 
	 * See {@link #getCodedText()} for more information on coded text.
	 * @param codedText the new coded text content to set.
	 */
	public void setCodedText (String codedText) {
		ctext = new StringBuilder(codedText);
	}
	
	/**
	 * Gets the source or target tags for the unit where this segment is located.
	 * @return the source or target tags for the unit where this segment is located. Can be empty but never null.
	 */
	public Tags getTags () {
		return tags;
	}
	
	/**
	 * Creates a list of all the source or target tags in this fragment.
	 * The list is a snapshot of the tags at the time the method is called, not a live list. 
	 * @return a list of the tags for this fragment.
	 */
	public List<Tag> getOwnTags () {
		if ( tags.size() == 0 ) return Collections.emptyList();
		ArrayList<Tag> list = new ArrayList<>();
		for ( int i=0; i<ctext.length(); i++ ) {
			if ( isChar1(ctext.charAt(i)) ) {
				list.add(tags.get(toKey(ctext.charAt(i), ctext.charAt(++i))));
			}
		}
		return list;
	}
	
	/**
	 * Creates a map of the opening and closing tags in this fragment and their corresponding
	 * status as tag: (0=isolated, 1=not-well-formed or 2=well-formed).
	 * Standalone codes are not in the resulting list.
	 * <p>isolated means to counterpart tag within the parent unit.
	 * <p>(not-)well-formed means (not-)well-formed within this fragment.  
	 * @return a map of the opening and closing tags and their status, the map
	 * may be empty but never null.
	 */
	public Map<Tag, Integer> getOwnTagsStatus () {
		List<Tag> ownTags = getOwnTags();
		Map<Tag, Integer> status = new HashMap<>(ownTags.size());
		Stack<Tag> stack = new Stack<>();
		for (Tag tag : ownTags) {
			switch (tag.getTagType()) {
				case OPENING:
					stack.push(tag);
					status.put(tag, 0); // Default
					break;
				case CLOSING:
					if (!stack.isEmpty() && stack.peek().getId().equals(tag.getId())) {
						status.put(stack.pop(), 2); // Well-formed element
						status.put(tag, 2);
					} else { // Can be isolated or not-well-formed
						if (tags.getOpeningTag(tag.getId()) != null) {
							status.put(tag, 1); // Not-well-formed
						} else { // Else: no opening within the unit means isolated
							status.put(tag, 0);
						}
					}
					break;
				case STANDALONE:
					// Nothing to do
					break;
			}
		}
		
		// Now look at what is left in the stack
		while ( !stack.isEmpty() ) {
			Tag tag = stack.pop();
			if ( tags.getClosingTag(tag.getId()) != null ) {
				status.put(tag, 1); // Not-well-formed
			}
			// Else: no closing within the unit means isolated
			// Which is the default status for opening tags
		}
		return status;
	}	

	/**
	 * Gets the {@link MTag} or {@link CTag} for a given reference in a coded text.
	 * @param ctext the coded text (e.g. String or StringBuilder object).
	 * @param pos the position of the first character of the reference.
	 * @return the tag for the given tag reference, or null if there is no corresponding tag.
	 */
	public Tag getTag (CharSequence ctext,
		int pos)
	{
		return tags.get(ctext, pos);
	}
	
	/**
	 * Gets the {@link CTag} for a given reference in a coded text.
	 * @param ctext the coded text (e.g. String or StringBuilder object).
	 * @param pos the position of the first character of the reference.
	 * @return the tag for the given tag reference, or null if there is no corresponding tag.
	 */
	public CTag getCTag (CharSequence ctext,
		int pos)
	{
		return tags.getCTag(ctext, pos);
	}
	
	/**
	 * Gets the {@link MTag} for a given reference in a coded text.
	 * @param ctext the coded text (e.g. String or StringBuilder object).
	 * @param pos the position of the first character of the reference.
	 * @return the tag for the given tag reference, or null if there is no corresponding tag.
	 */
	public MTag getMTag (CharSequence ctext,
		int pos)
	{
		return tags.getMTag(ctext, pos);
	}
	
	/**
	 * Gets the store associated with this fragment.
	 * @return the store associated with this fragment (never null).
	 */
	public Store getStore () {
		return tags.getStore();
	}
	
	/**
	 * Gets the tag for a given key.
	 * @param key the key of the tag to retrieve.
	 * @return the tag for the given key, or null if there is no corresponding tag.
	 */
	public Tag getTag (int key) {
		return tags.get(key);
	}

	/**
	 * Creates an XLIFF output for this fragment.
	 * @param nsStack the namespace context to use (can be null for out-of-context output).
	 * @param context context of the inherited data (can be null for out-of-context output).
	 * @param withOriginalData true to output references to original data, false otherwise.
	 * @return the XLIFF string.
	 */
	public String toXLIFF (Stack<NSContext> nsStack,
		Stack<InheritedData> context,
		boolean withOriginalData)
	{
		StringBuilder tmp = new StringBuilder();
		CTag ctag;
		MTag mtag;
		List<String> verified = new ArrayList<>();
		List<SimpleEntry<String, AnnotatorsRef>> annotRefs = null;

		for ( int i=0; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			if ( ch == CODE_OPENING ) {
				ctag = (CTag)tags.get(ctext, i); i++;
				// Check if the corresponding closing part is in the same fragment
				CTag closing = null;
				closing = (CTag)getWellFormedClosing(ctag, i);
				if ( closing != null ) {
					tmp.append("<pc id=\""+ctag.getId()+"\"");
					verified.add(ctag.getId());
					if ( ctag.getCanOverlap() ) {
						tmp.append(" "+Const.ATTR_CANOVERLAP+"=\"yes\"");
					}
				}
				else {
					// No corresponding closing part
					tmp.append(String.format("<sc id=\"%s\"", ctag.getId()));
					if ( !ctag.getCanOverlap() ) {
						tmp.append(" "+Const.ATTR_CANOVERLAP+"=\"no\"");
					}
					if ( getClosingTag(ctag) == null ) {
						tmp.append(" isolated=\"yes\"");
					}
				}
				printCommonAttributes(ctag, tags, tmp, closing, withOriginalData); // closing can be null
				if ( withOriginalData && ctag.hasData() ) {
					String ending = (closing==null ? "" : "Start");
					tmp.append(String.format(" dataRef%s=\"%s\"", ending,
						tags.getStore().getIdForData(ctag)));
				}
				printExtAttributes(ctag, tmp, nsStack);
				tmp.append(closing==null ? "/>" : ">");
			}
			else if ( ch == CODE_CLOSING ) {
				ctag = (CTag)tags.get(ctext, i); i++;
				if ( verified.contains(ctag.getId()) ) {
					// This pair was verified
					tmp.append("</pc>");
					// No need to remove the CTag from the verified list
					// as it's not used again (no need to waste time cleaning it)
				}
				else { // Not in the verified list: use <ec>
					tmp.append("<ec");
					if ( !unitHasOpening(ctag) ) {
						tmp.append(String.format(" id=\"%s\"", ctag.getId()));
						tmp.append(" isolated=\"yes\"");
					}
					else {
						tmp.append(" "+Const.ATTR_STARTREF+"=\""+ctag.getId()+"\"");
					}
					if ( !ctag.getCanOverlap() ) {
						tmp.append(" "+Const.ATTR_CANOVERLAP+"=\"no\"");
					}
					printCommonAttributes(ctag, tags, tmp, null, false);
					if ( withOriginalData && ctag.hasData() ) {
						tmp.append(String.format(" dataRef=\"%s\"",
							tags.getStore().getIdForData(ctag)));
					}
					printExtAttributes(ctag, tmp, nsStack);
					tmp.append("/>");
				}
			}
			else if ( ch == CODE_STANDALONE ) {
				ctag = (CTag)tags.get(ctext, i); i++;
				tmp.append(String.format("<ph id=\"%s\"", ctag.getId()));
				printCommonAttributes(ctag, tags, tmp, null, false);
				if ( withOriginalData && ctag.hasData() ) {
					tmp.append(String.format(" dataRef=\"%s\"",
						tags.getStore().getIdForData(ctag)));
				}
				printExtAttributes(ctag, tmp, nsStack);
				tmp.append("/>");
			}
			else if ( ch == MARKER_OPENING ) {
				mtag = (MTag)tags.get(ctext, i); i++;
				// Check if the corresponding closing part is in the same fragment
				MTag closing = (MTag)getWellFormedClosing(mtag, i);
				// Start part of the tag
				if ( closing != null ) {
					tmp.append(String.format("<%s id=\"%s\"", Const.ELEM_PAIREDANNO, mtag.getId()));
				}
				else {
					// No corresponding closing part
					tmp.append(String.format("<%s id=\"%s\"", Const.ELEM_OPENINGANNO, mtag.getId()));
				}
				// Optional attributes
				if ( !mtag.getType().equals(MTag.TYPE_DEFAULT) ) {
					tmp.append(" type=\""+mtag.getType()+"\"");
				}
				if ( mtag.getTranslate() != null ) {
					tmp.append(" translate=\""+(mtag.getTranslate() ? "yes" : "no")+"\"");
				}
				if ( !Util.isNoE(mtag.getValue()) ) {
					tmp.append(" value=\""+Util.toXML(mtag.getValue(), true)+"\"");
				}
				if ( !Util.isNoE(mtag.getRef()) ) {
					tmp.append(" ref=\""+Util.toXML(mtag.getRef(), true)+"\"");
				}
				
				// ITS attributes
				if ( itsWriter == null ) itsWriter = new ITSWriter();
				if ( annotRefs == null ) annotRefs = itsWriter.createAnnotatorsRefList(context);
				AnnotatorsRef amAR = itsWriter.createAnnotatorsRef(mtag);
				annotRefs.add(new SimpleEntry<>(mtag.getId(), amAR));
				tmp.append(itsWriter.outputAttributes(mtag, amAR, annotRefs.get(annotRefs.size()-2).getValue()));
				
				printExtAttributes(mtag, tmp, nsStack);
				// End part of the tag
				if ( closing != null ) {
					verified.add(mtag.getId());
					tmp.append(">");
				}
				else {
					tmp.append("/>");
				}
			}
			else if ( ch == MARKER_CLOSING ) {
				mtag = (MTag)tags.get(ctext, i); i++;
				if ( verified.contains(mtag.getId()) ) {
					// This pair was verified
					tmp.append("</"+Const.ELEM_PAIREDANNO+">");
					// No need to remove the MTag from the verified list
					// as it's not used again (no need to waste time cleaning it)
				}
				else { // Not in the verified list
					tmp.append("<"+Const.ELEM_CLOSINGANNO+" "+Const.ATTR_STARTREF+"=\""+mtag.getId()+"\"/>");
				}
				// But we need to remove the annotator-references item (if one exists)
				if ( annotRefs != null ) {
					String id = mtag.getId();
					for ( SimpleEntry<String, AnnotatorsRef> entry : annotRefs ) {
						if ( id.equals(entry.getKey()) ) {
							annotRefs.remove(entry);
							break;
						}
					}
				}
			}
			else if ( ch == PCONT_STANDALONE ) {
				// Show something that will not validate
				tmp.append("<WARNING:HIDDEN-PROTECTED-CONTENT/>");
				i++;
			}
			else {
				// In XML 1.0 the valid characters are:
				// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
				switch ( ch ) {
				case '\r':
					tmp.append("&#13;"); // Literal
					break;
				case '<':
					tmp.append("&lt;");
					break;
				case '&':
					tmp.append("&amp;");
					break;
				case '\n':
				case '\t':
					tmp.append(ch);
					break;
				default:
					if (( ch > 0x001F ) && ( ch < 0xD800 )) {
						// Valid char (most frequent) 
						tmp.append(ch);
					}
					else if ( Character.isHighSurrogate(ch) ) {
						tmp.append(Character.toChars(ctext.codePointAt(i)));
						i++;
					}
					else if (( ch < 0x0020 )
						|| (( ch > 0xD7FF ) && ( ch < 0xE000 ))
						|| ( ch == 0xFFFE )
						|| ( ch == 0xFFFF )) { // Invalid characters
						tmp.append(String.format("<cp hex=\"%04X\"/>", (int)ch));
					}
					else { // Other characters
						tmp.append(ch);
					}
					break;
				}
			}
		}
		return tmp.toString();
	}
	
	/**
	 * Gets the closing tag corresponding to a given opening tag within a unit.
	 * @param tag the opening tag.
	 * @return the closing tag, or null if none is found.
	 */
	public Tag getClosingTag (Tag tag) {
		if ( tags == null ) return null;
		return tags.getClosingTag(tag);
	}
	
	/**
	 * Gets the opening tag corresponding to a given closing tag within a unit.
	 * @param tag the closing tag.
	 * @return the opening tag, or null if none is found.
	 */
	public Tag getOpeningTag (Tag tag) {
		if ( tags == null ) return null;
		return tags.getOpeningTag(tag);
	}
	
	/**
	 * Gets the closing position of a span of content in this fragment for a given opening tag.
	 * If the corresponding closing tag is not present or outside of this fragment, this method returns -1.
	 * @param opening the opening tag.
	 * @return the position of the end of the span
	 * (the index of the first character of the key of the closing tag),
	 * or -1 if not found.
	 */
	public int getClosingPosition (Tag opening) {
		if ( tags == null ) return -1;
		Tag closing = tags.getClosingTag(opening);
		if ( closing == null ) return -1;
		return ctext.indexOf(toRef(tags.getKey(closing)));
	}
	
	/**
	 * Tests if the unit to which this fragment belongs has an opening code
	 * for a given closing code.
	 * @param ctag the closing code to test.
	 * @return true if there is a corresponding opening code. False otherwise.
	 */
	public boolean unitHasOpening (CTag ctag) {
		if ( tags == null ) return false;
		return (tags.getOpeningTag(ctag) != null);
	}
	
	/**
	 * Output the common attributes for a given CTag object.
	 * @param code the code to output.
	 * @param tags the tags collection to which the code belongs (can be null if closing is null).
	 * @param tmp the buffer where to output.
	 * @param closing the closing code if this is a paired-code (can be null).
	 * @param outputDataRefEnd true to output the dataRefEnd attribute if needed.
	 */
	public static void printCommonAttributes (CTag code,
		Tags tags,
		StringBuilder tmp,
		CTag closing,
		boolean outputDataRefEnd)
	{
		if ( code.getType() != null ) {
			tmp.append(" type=\""+code.getType()+"\"");
		}
		if ( code.getSubType() != null ) {
			if ( code.getType() == null ) {
				throw new XLIFFWriterException("You must specify a type if you specify a subType.");
			}
			tmp.append(" subType=\""+code.getSubType()+"\"");
		}
		if ( !code.getCanCopy() ) {
			tmp.append(" "+Const.ATTR_CANCOPY+"=\"no\"");
		}
		if ( !code.getCanDelete() ) {
			tmp.append(" "+Const.ATTR_CANDELETE+"=\"no\"");
		}
		if ( code.getCanReorder() != CanReorder.YES ) {
			tmp.append(String.format(" %s=\"%s\"",
				Const.ATTR_CANREORDER,
				code.getCanReorder()==CanReorder.FIRSTNO ? "firstNo" : "no")
			);
		}

		String ending = (closing == null ? "" : "Start");
		if ( !code.getEquiv().isEmpty() ) {
			tmp.append(String.format(" equiv%s=\"%s\"", ending, Util.toXML(code.getEquiv(), true)));
		}
		if ( code.getDisp() != null ) {
			tmp.append(String.format(" disp%s=\"%s\"", ending, Util.toXML(code.getDisp(), true)));
		}
		if ( code.getSubFlows() != null ) {
			tmp.append(String.format(" subFlows%s=\"%s\"", ending, code.getSubFlows()));
		}
		
		if ( closing != null ) {
			if ( !closing.getEquiv().isEmpty() ) {
				tmp.append(String.format(" equivEnd=\"%s\"", Util.toXML(closing.getEquiv(), true)));
			}
			if ( closing.getDisp() != null ) {
				tmp.append(String.format(" dispEnd=\"%s\"", Util.toXML(closing.getDisp(), true)));
			}
			if ( closing.getSubFlows() != null ) {
				tmp.append(String.format(" subFlowsEnd=\"%s\"", closing.getSubFlows()));
			}

			if ( outputDataRefEnd && closing.hasData() ) {
				tmp.append(String.format(" dataRefEnd=\"%s\"",
					tags.getStore().getIdForData(closing)));
			}
		}
	}
	
	public static void printExtAttributes (Tag tag,
		StringBuilder output,
		Stack<NSContext> nsStack)
	{
		if ( !tag.hasExtAttribute() ) return;
		ExtAttributes attributes = tag.getExtAttributes();
		
		NSContext nsCtx = null;
		if ( nsStack != null ) {
			nsCtx = nsStack.push(nsStack.peek().clone());
//System.out.println("push<tag>:"+nsStack.peek().toString());		
			for ( String namespaceURI : attributes.getNamespaces() ) {
				// Skip empty namespace and namespaces in scope
				if ( !namespaceURI.isEmpty() && ( nsStack.peek().getPrefix(namespaceURI) == null )) {
					String prefix = attributes.getNamespacePrefix(namespaceURI);
					if ( prefix != null ) {
						output.append(" xmlns" + (prefix.isEmpty() ? "" : ":"+prefix) + "=\"" + namespaceURI + "\"");
						nsCtx.put(prefix, namespaceURI);
					}
				}
			}
		}
		else { // No context to look at (because not provided by the caller: this should be rare)
			for ( String namespaceURI : attributes.getNamespaces() ) {
				if ( !namespaceURI.isEmpty() ) {
					String prefix = attributes.getNamespacePrefix(namespaceURI);
					if ( prefix != null ) {
						output.append(" xmlns:" + prefix + "=\"" + namespaceURI + "\"");
					}
				}
			}
		}
		
		for ( ExtAttribute att : attributes ) {
			if ( nsCtx != null ) {
				String prefix = nsCtx.getPrefix(att.getNamespaceURI());
				output.append(" " + (prefix.isEmpty() ? "" : prefix+":") + att.getLocalPart()
					+ "=\"" + Util.toXML(att.getValue(), true) + "\"");
			}
			else {
				output.append(" " + (att.getPrefix().isEmpty() ? "" : att.getPrefix()+":") + att.getLocalPart()
					+ "=\"" + Util.toXML(att.getValue(), true) + "\"");
			}
		}
		if ( nsStack != null ) {
			nsStack.pop();
//System.out.println("pop</tag>:"+nsStack.peek().toString());
		}
	}

	/**
	 * Creates an XLIFF output of this fragment, without handling the inline codes original data,
	 * without namespace content and without inherited data context. 
	 * @return the XLIFF string.
	 */
	public String toXLIFF () {
		return toXLIFF(null, null, false);
	}

	/**
	 * Indicates if this fragment is empty.
	 * @return true if this fragment is empty, false if it has text or inline code.
	 */
	public boolean isEmpty () {
		return (ctext.length()==0);
	}
	
	/**
	 * Indicates if this fragment is a target content.
	 * @return true if this is a target content, false if it is a source content.
	 */
	public boolean isTarget () {
		return isTarget;
	}
	
	/**
	 * Indicates if this fragment contains at least one tag.
	 * @return true if this fragment contains at least one tag, false otherwise.
	 */
	public boolean hasTag () {
		if ( tags.size() == 0 ) return false;
		// Else check the tag references
		// (the tags object is shared across the whole unit)
		for ( int i=0; i<ctext.length(); i++ ) {
			if ( Fragment.isChar1(ctext.charAt(i)) ) return true;
		}
		return false;
	}

	/**
	 * Gets the well-formed closing tag for a given opening one, starting at a given character position.
	 * <p>This will not find closing tag that are not well-formed (e.g. overlapping)
	 * @param opening the opening tag.
	 * @param from the first character position to look at.
	 * @return the corresponding closing tag, or null if none is found. 
	 */
	public Tag getWellFormedClosing (Tag opening,
		int from)
	{
		Stack<String> stack = new Stack<>();
		for ( int i=from; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			Tag tag;
			if (( ch == CODE_OPENING ) || ( ch == MARKER_OPENING )) {
				tag = tags.get(ctext, i); i++;
				stack.push(tag.getId());
			}
			else if (( ch == CODE_CLOSING ) || ( ch == MARKER_CLOSING )) {
				tag = tags.get(ctext, i); i++;
				if ( tag.getId().equals(opening.getId()) ) {
					// Well-formed if the stack is empty
					if ( stack.isEmpty() ) return tag;
					else return null;
				}
				// If it's not our closing tag and the stack is already empty
				// That's not a well-formed pattern
				if ( stack.isEmpty() ) {
					return null;
				}
				// Remove the tag
				// If it's at the top it's like a pop()
				// Otherwise it says the opening was closed
				stack.remove(tag.getId());
			}
			else if ( ch == CODE_STANDALONE ) {
				i++;
			}
		}
		// Closing part not found: not well-formed.
		return null;
	}
	
	/**
	 * Appends a character at the end of this fragment.
	 * @param ch the character to append.
	 * @return the fragment itself.
	 */
	@Override
	public Fragment append (char ch) {
		ctext.append(ch);
		return this;
	}

	/**
	 * Appends a plain text string to this fragment.
	 * If the parameter is null, a string "null" is appended.
	 * @param plainText the string to append.
	 * @return the fragment itself.
	 */
	@Override
	public Fragment append (CharSequence plainText) {
		ctext.append(plainText);
		return this;
	}

	/**
	 * Appends a sub-sequence of a given plain text string to this fragment.
	 * If the parameter is null, a string "null" is appended.
	 * @param plainText the source for the sub-sequence to append.
	 * @param start the index of the first character in the subsequence.
	 * @param end the index of the character following the last character in the subsequence.
	 * @return the fragment itself.
	 */
	@Override
	public Fragment append (CharSequence plainText,
		int start,
		int end)
	{
		// StringBuilder is an Appendable so null is process as expected
		ctext.append(plainText.subSequence(start, end));
		return this;
	}
	
	/**
	 * Appends a tag to this fragment.
	 * @param tag the code or marker tag to append.
	 * @return the added tag (same as the parameter).
	 */
	public Tag append (Tag tag) {
		ctext.append(toRef(tags.add(tag)));
		return tag;
	}
	
//	/**
//	 * Appends the tag of a code to this fragment.
//	 * @param ctag the code tag to append.
//	 * @return the added code tag (same as the parameter).
//	 * @see #append(Tag)
//	 */
//	public CTag append (CTag ctag) {
//		ctext.append(toRef(tags.add(ctag)));
//		return ctag;
//	}

//	/**
//	 * Appends a marker tag to this fragment.
//	 * @param mtag the marker tag to append.
//	 * @return the added marker tag (same as the parameter).
//	 * @see #closeMarkerSpan(String)
//	 * @see #append(Tag)
//	 * @see #annotate(int, int, MTag)
//	 * @see #annotate(int, int, String, String, String)
//	 */
//	public MTag append (MTag mtag) {
//		ctext.append(toRef(tags.add(mtag)));
//		return mtag;
//	}
	
	/**
	 * Appends a tag opening a new marker span.
	 * @param id the ID of the new marker (or null to use an auto-generated ID).
	 * @param type the type of the new marker (or null for the default).
	 * @return the new marker's opening tag.
	 * @see #closeMarkerSpan(String)
	 */
	public MTag openMarkerSpan (String id,
		String type)
	{
		if ( id == null ) {
			// Generate an Id
			id = tags.getStore().suggestId(false);
		}
		MTag mtag = new MTag(id, type);
		ctext.append(toRef(tags.add(mtag)));
		return mtag;
	}
	
	/**
	 * Appends a tag to close the marker of a given id.  
	 * @param id the id of the marker.
	 * @return the closing tag created.
	 * @throws XLIFFException if the opening tag for this marker cannot be found.
	 * @see #openMarkerSpan(String, String)
	 */
	public MTag closeMarkerSpan (String id) {
		if ( tags == null ) {
			throw new XLIFFException("There are no opening tags in this unit.");
		}
		Tag tag = tags.getOpeningTag(id);
		if ( tag == null ) {
			throw new XLIFFException(String.format("Opening tag for id='%s' not found.", id));
		}
		if ( !(tag instanceof MTag) ) {
			throw new XLIFFException(String.format("Opening tag for id='%s' is not for a marker.", id));
		}
		MTag closing = new MTag((MTag)tag);
		ctext.append(toRef(tags.add(closing)));
		return closing;
	}

	/**
	 * Appends a code tag.
	 * @param tagType the {@link TagType} of the code tag (cannot be null).
	 * @param id the id of the tag.
	 * @param data the original data for this tag (can be null)
	 * @param canOverlap true if this code can be overlapped, false otherwise.
	 * This parameter is ignored if the code is created from its counterpart.
	 * @return the added code tag.
	 * @see #appendCode(String, String)
	 */
	public CTag append (TagType tagType,
		String id,
		String data,
		boolean canOverlap)
	{
		// Check ID
		if ( id == null ) {
			// Generate an Id
			id = tags.getStore().suggestId(false);
		}
		// Try to get the counterpart code if it is requested
		Tag opposite = null;
		if ( tagType != TagType.STANDALONE ) {
			if ( tagType == TagType.OPENING ) {
				opposite = tags.getClosingTag(id);
			}
			else { // TagType.CLOSING
				opposite = tags.getOpeningTag(id);
			}
		}
		// Create the new code
		CTag ctag;
		if ( opposite == null ) {
			// This work also for standalone since opposite will always be null for that case
			ctag = new CTag(null, tagType, id, data);
		}
		else {
			// Here id comes from the opposite tag
			CTag tmp = (CTag)opposite;
			ctag = new CTag(tmp, data);
			// Set the canReorder value based on the opposite tag.
			switch ( tmp.getTagType() ) {
			case OPENING:
				if ( tmp.getCanReorder() == CanReorder.FIRSTNO ) {
					ctag.setCanReorder(CanReorder.NO);
					break;
				}
				// Else: fall thru
			default:
				ctag.setCanReorder(tmp.getCanReorder());
			}
		}

		// Add the new code to the collection and add its reference in the coded text
		ctext.append(Fragment.toRef(tags.add(ctag)));
		// Set the canOverlap parameter if the data are created from scratch
		if ( opposite == null ) {
			ctag.setCanOverlap(canOverlap);
		}

		return ctag;
	}

//	/**
//	 * Tries to guess and set some editing hints for a given closing code
//	 * based on its corresponding opening code.
//	 * @param closing the closing code to update.
//	 * @return true if the closing code has been updated, false otherwise.
//	 */
//	public boolean guessClosingCodeProperties (CTag closing) {
//		if ( closing.getTagType() != TagType.CLOSING ) return false;
//		// Get the corresponding opening tag
//		CTag opening = (CTag)getOpeningTag(closing);
//		if ( opening == null ) return false;
//		// Guess some of the properties
//		switch ( opening.getCanReorder() ) {
//		case FIRSTNO:
//		case NO:
//			closing.setCanReorder(CanReorder.NO);
//		case YES:
//			// Do nothing
//			break;
//		}
//		closing.setCanOverlap(opening.getCanOverlap());
//		closing.setCanCopy(opening.getCanCopy());
//		closing.setCanDelete(opening.getCanDelete());
//		return true;
//	}
	
	/**
	 * Appends a fragment to this fragment.
	 * @param fragment the fragment to append (cannot be itself or null).
	 * @return the fragment itself.
	 */
	public Fragment append (Fragment fragment) {
		if ( this == fragment ) {
			throw new XLIFFException("Recursive append() on a fragment.");
		}
		// Copy the string/tags of the content
		for ( Object obj : fragment ) {
			if ( obj instanceof Tag ) {
				// Make sure we duplicate the tag
				append(CloneFactory.create((Tag)obj, getTags()));
			}
			else if ( obj instanceof PCont ) {
				throw new XLIFFException("Fragment.append(Fragment) with hidden protected content is not supported yet.");
//				PTag pm = (PTag)obj;
//				StringBuilder pmtmp = new StringBuilder(pm.getCodedText());
//				for ( int j=0; j<pmtmp.length(); j++ ) {
//					char pmch = pmtmp.charAt(j);
//					if ( isChar1(pmch) ) {
//						Tag tag = oriTags.get(pmtmp, j);
//						key = tags.add(CloneFactory.create(tag, oriTags));
//						pmtmp.replace(j, j+2, toRef(key)); j++;
//					}
//				}
//				key = tags.add(new PTag(pmtmp.toString()));
//				tmp.replace(i, i+2, toRef(key)); i++;
			}
			else { // String
				append((String)obj);
			}
		}
		return this;
	}

	/**
	 * Annotates a span of content in this fragment.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param type the type of the annotation.
	 * If the type is <code>term</code> or <code>its:term-no</code> the marker created
	 * is an instance of {@link TermTag} rather than {@link MTag}.
	 * @param value the value of the <code>value</code> attribute (can be null).
	 * @param ref the value of the <code>ref</code> attribute (can be null).
	 * @return the number of characters added to the coded text.
	 * @see #getOrCreateMarker(int, int, String, String) 
	 */
	public int annotate (int start,
		int end,
		String type,
		String value,
		String ref)
	{
		MTag tag = new MTag(true, getStore().suggestId(false), type);
		// Switch to a TermMarker if the type directs it
		if ( TermTag.TYPE_TERM.equals(type) || TermTag.TYPE_ITSTERMNO.equals(type) ) {
			tag = new TermTag(tag, type, null);
		}
		tag.setValue(value);
		tag.setRef(ref);
		return annotate(start, end, tag);
	}
	
	/**
	 * Annotates a span of content in this fragment.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param opening the start tag of the marker. The end tag will be generated from this tag.
	 * @return the number of characters added to the coded text. 
	 * @see #getOrCreateMarker(int, int, String, String) 
	 */
	public int annotate (int start,
		int end,
		MTag opening)
	{
		int initial = ctext.length();
		if ( end == -1 ) end = ctext.length();
		checkPosition(start);
		checkPosition(end);
		// Auto-generate the id
		int key = tags.add(opening);
		ctext.insert(start, toRef(key));
		
		// Create and insert the end tag
		MTag closing = new MTag(opening);
		key = tags.add(closing);
		ctext.insert(end+2, toRef(key)); // Don't forget to adjust the end position
		// Return the length difference
		return ctext.length()-initial;
	}
	
	/**
	 * Annotates a span of content in this fragment with a {@link Note} object and add the new note to the container.
	 * Both the ID of the note and the ID of the marker are automatically created.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param noteContent the text of the note.
	 * @return the new note. 
	 */
	public Note annotateWithNote (int start,
		int end,
		String noteContent)
	{
		// Get the parent and see if it can hold a note
		// (Normally it always should)
		IWithStore parent = getStore().getParent();
		if ( !(parent instanceof IWithNotes) ) {
			throw new XLIFFException("Cannot attach a note in to "+parent.getClass().getCanonicalName());
		}
		IWithNotes holder = (IWithNotes)parent;
		// Create the markers
		MTag opening = new MTag(true, getStore().suggestId(false), "comment");
		annotate(start, end, opening);
		// Create the new note and add it
		Note note = new Note(noteContent);
		note.setId(UUID.randomUUID().toString());
		holder.addNote(note);
		// Associate the marker with the note
		opening.setRef("#n="+note.getId());
		// return the new note
		return note;
	}

	/**
	 * Get or create an annotation marker for a given span of content.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param matchingType the type of marker that can be reused. Use null to reuse any marker.
	 * @param typeForNew the type of the marker to create of none reusable is found (must not be null).
	 * @return the opening tag of the marker found or created.
	 */
	public MTag getOrCreateMarker (int start,
		int end,
		String matchingType,
		String typeForNew)
	{
		if ( end == -1 ) end = ctext.length();
		checkPosition(start);
		checkPosition(end);

		// Try to find reusable markers
		boolean found = false;
		MTag opening = null;
		Tag closing = null;
		if (( start > 1 ) && ( ctext.charAt(start-2) == MARKER_OPENING )) {
			// The start is just after an opening tag
			opening = (MTag)tags.get(ctext, start-2);
		}
		else if ( ctext.charAt(start) == MARKER_OPENING ) {
			// The start is just on an opening tag
			opening = (MTag)tags.get(ctext, start);
		}
		
		if ( opening != null ) { 
			// Check the corresponding closing tag (if any)
			closing = tags.getClosingTag(opening);
			if ( closing != null ) {
				int pos = ctext.indexOf(toRef(tags.getKey(closing)));
				if (( end == pos ) || ( end == pos+2 )) { // End is just before or before the closing tag
					// We can reuse this annotation
					if ( matchingType != null ) {
						found = matchingType.equals(opening.getType());
					}
					else found = true;
				}
			}
		}

		// Create a new annotation if none reusable is found
		if ( !found ) {
			if ( typeForNew == null ) {
				throw new InvalidParameterException("You must define the typeForNew parameter.");
			}
			// Create a new annotation if none reusable one is found
			opening = new MTag(true, getStore().suggestId(false), typeForNew);
			annotate(start, end, opening);
		}
		
		return opening;
	}
	
	/**
	 * Removes a tag from this fragment (including if the tag is in a protected content).
	 * The tag is removed both from the coded text and from the list of tags.
	 * @param tag the tag to remove.
	 * @return the fragment itself.
	 */
	public Fragment remove (Tag tag) {
		// Search for it
		if ( tags == null ) {
			throw new XLIFFException("There is no tag in this fragment.");
		}
		int key = tags.getKey(tag);
		if ( key == -1 ) {
			throw new XLIFFException(String.format(
				"There is no tag for id='%s' and type='%s' in this fragment.",
				tag.getId(), tag.getType()));
		}

		// Search for the reference in the coded text
		for ( int i=0; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			if ( ch == Fragment.PCONT_STANDALONE ) {
				PCont pcont = tags.getPCont(Fragment.toKey(ch, ctext.charAt(i+1)));
				StringBuilder pct = new StringBuilder(pcont.getCodedText());
				for ( int j=0; j<pct.length(); j++ ) {
					char pch = pct.charAt(j);
					if ( Fragment.isChar1(pch) ) {
						if ( key == Fragment.toKey(pch, pct.charAt(++j)) ) {
							// Remove the reference and the tag
							pct.delete(j-1, j+1);
							pcont.setCodedText(pct.toString());
							tags.remove(key);
							return this; // We are done
						}
					}
				}
			}
			else if ( Fragment.isChar1(ch) ) {
				if ( key == Fragment.toKey(ch, ctext.charAt(++i)) ) {
					// Remove the reference and the tag
					ctext.delete(i-1, i+1);
					tags.remove(key);
					return this; // We are done
				}
			}
		}
		return this;
	}

	/**
	 * Deletes a section of this fragment (including any protected content within the section).
	 * @param start the start index (inclusive)
	 * @param end the end index (exclusive)
	 * @return the fragment itself.
	 */
	public Fragment delete (int start,
		int end)
	{
		checkPosition(start);
		checkPosition(end);
		for ( int i=start; i<end; i++ ) {
			char ch = ctext.charAt(i);
			if ( ch == Fragment.PCONT_STANDALONE ) {
				int pkey = Fragment.toKey(ch, ctext.charAt(++i));
				PCont pcont = tags.getPCont(pkey);
				// Remove the tags in the protected content
				StringBuilder pct = new StringBuilder(pcont.getCodedText());
				for ( int j=0; j<pct.length(); j++ ) {
					char pch = pct.charAt(j);
					if ( isChar1(pch) ) {
						tags.remove(Fragment.toKey(pch, pct.charAt(++j)));
					}
				}
				// Delete this protected content object
				tags.removePCont(pkey);
			}
			else if ( Fragment.isChar1(ch) ) {
				// Remove the tag
				tags.remove(Fragment.toKey(ch, ctext.charAt(++i)));
			}
		}
		// Remove all tag references and text in the section
		ctext.delete(start, end);
		return this;
	}
	
	/**
	 * Creates an iterator for all the different objects composing this fragment.
	 * The objects returned can be of type {@link String}, {@link CTag}, {@link MTag} and {@link PCont}.
	 * To iterate through only one type of object use {@link #getIterable(Class)}.
	 * @return a new iterator for all the objects composing this fragment.
	 * @see #getIterable(Class)
	 */
	@Override
	public Iterator<Object> iterator () {
		return (new ContentIterable<>(Object.class)).iterator();
	}	
//		return new Iterator<Object>() {
//			int pos = 0;
//
//			@Override
//			public void remove () {
//				throw new UnsupportedOperationException("The method remove() not supported.");
//			}
//
//			@Override
//			public Object next () {
//				// Search for next tag
//				int start = pos;
//				for ( ; pos < ctext.length(); pos++ ) {
//					if ( Fragment.isChar1(ctext.charAt(pos)) ) {
//						// Do we have text before?
//						if ( start < pos ) {
//							// If we do: send the sub-string
//							return ctext.substring(start, pos);
//						}
//						else {
//							// If not: move after the tag
//							pos+=2;
//							if ( ctext.charAt(pos-2) == PCONT_STANDALONE ) {
//								return tags.getPTag(ctext, pos-2);
//							}
//							else {
//								return tags.get(ctext, pos-2);
//							}
//						}
//					}
//				}
//				// No tag found: send the remaining sub-string
//				return ctext.substring(start);
//			}
//
//			@Override
//			public boolean hasNext () {
//				return (pos < ctext.length());
//			}
//		};
//	}

//	/**
//	 * Creates an interface to iterate through the {@link PTag} objects (protected content tags) in this fragment.
//	 * @return a new instance of the iterable interface.
//	 */
//	private Iterable<PTag> getPTagIterable () {
//		final Iterable<PTag> iter = new Iterable<PTag>() {
//			@Override
//			public Iterator<PTag> iterator () {
//				return new Iterator<PTag>() {
//					int pos = ctext.indexOf(""+PCONT_STANDALONE);
//			
//					@Override
//					public void remove () {
//						throw new UnsupportedOperationException("The method remove() not supported.");
//					}
//
//					@Override
//					public PTag next () {
//						int tmp = pos;
//						pos = ctext.indexOf(""+PCONT_STANDALONE, pos+1);
//						return tags.getPTag(ctext, tmp);
//					}
//
//					@Override
//					public boolean hasNext () {
//						return (pos != -1);
//					}
//				};
//			}
//		};
//		return iter;
//	}
	
	/**
	 * Creates a new instance of an iterable interface for a given class of objects.
	 * Inline objects in hidden content are not returned by the iterator.
	 * @param <T> the type of iterable.
	 * @param type the class of the object to iterate through. this can be
	 * {@link Object}, {@link String}, {@link Tag}, {@link CTag}, {@link MTag} or {@link PCont}.
	 * @return the new iterable interface.
	 * @see #iterator()
	 */
	public <T> Iterable<T> getIterable (Class<T> type) {
		return new ContentIterable<>(type);
	}

//	/**
//	 * Creates a new iterable interface instance for a given type of content object.
//	 * @param type the class of the type of object to iterate. The classes supported are:
//	 * {@link String}, {@link Tag}, {@link CTag}, {@link MTag} and {@link PTag}.
//	 * @return a new iterable for the specified type of object.
//	 */
//	@SuppressWarnings("unchecked")
//	public <T extends Object> Iterable<T> getIterable (Class<T> type) {
//		if ( type == PTag.class ) {
//			return (Iterable<T>)getPTagIterable();
//		}
//		else if ( type == Object.class ) {
//			return (Iterable<T>)this;
//		}
//		throw new InvalidParameterException(String.format(
//			"The class %s is not a supported type for iterable.", type.getName()));
//	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ctext == null) ? 0 : ctext.toString().hashCode());
		result = prime * result + ((dir == null) ? 0 : dir.hashCode());
		result = prime * result + (isTarget ? 1231 : 1237);
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		return result;
	}

	/**
	 * Indicates if this fragment equals another object.
	 * Use <code>==</code> to see if two fragments are the same object.
	 * @param object the object to compare this fragment with.
	 * @return true if the object and this fragment are equals.
	 */
	@Override
	public boolean equals (Object object) {
		if ( this == object ) return true;
		if ( object == null ) return false;
		if ( getClass() != object.getClass() ) return false;
		
		Fragment fragment =(Fragment)object;
		Iterator<Object> thisIter = this.iterator();
		Iterator<Object> thatIter = fragment.iterator();
		while ( thisIter.hasNext() ) {
			if ( !thatIter.hasNext() ) return false;
			Object thisObj = thisIter.next();
			Object thatObj = thatIter.next();
			if ( thisObj instanceof String ) {
				if ( !(thatObj instanceof String) ) return false;
				if ( !thisObj.equals(thatObj) ) return false;
			}
			else { // Tag
				if ( thatObj instanceof String ) return false;
				if ( !thisObj.equals(thatObj) ) return false;
			}
		}
		if ( thatIter.hasNext() ) return false;
		return true;
	}

	/**
	 * Gets the directionality of this fragment.
	 * @param resolved true to get the resolved value (i.e. based on inheritance), false for the immediate value.
	 * @return the directionality of this fragment.
	 */
	public Directionality getDir (boolean resolved) {
		// Inheritance is resolved by accessing the directionality of the unit-level data store
		// As segments and ignorables have no directionality information (it would be redundant)
		if ( resolved && ( dir == Directionality.INHERITED )) {
			if ( isTarget ) return tags.getStore().getTargetDir();
			else return tags.getStore().getSourceDir();
		}
		else return dir;
	}

	/**
	 * Sets the directionality for this fragment.
	 * @param dir the new directionality.
	 */
	public void setDir (Directionality dir) {
		this.dir = dir;
	}

	/**
	 * Expands all {@link PCont} references in this fragment into normal content.
	 * To hide the protected content use {@link Unit#hideProtectedContent()}.
	 */
	public void showProtectedContent () {
		for ( int i=0; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			if ( ch == Fragment.PCONT_STANDALONE ) {
				int pkey = Fragment.toKey(ch, ctext.charAt(i+1));
				PCont pcont = tags.getPCont(pkey);
				// Replace the tag reference by the hidden content
				ctext.replace(i, i+2, pcont.getCodedText());
				// Adjust the pointer value
				i += (pcont.getCodedText().length())-1;
				// Remove the tag from the list
				tags.removePCont(pkey);
			}
			else if ( Fragment.isChar1(ch) ) {
				i++;
			}
		}
	}

	/**
	 * Verifies if a given position in the coded text is on the second special
	 * character of a tag reference.
	 * @param position the position to check.
	 * @throws InvalidPositionException when position points inside a tag reference.
	 */
	public void checkPosition (int position) {
		if ( position > 0 ) {
			if ( isChar1(ctext.charAt(position-1)) ) {
				throw new InvalidPositionException (
					String.format("Position %d is inside a tag reference.", position));
			}
		}
	}

	/**
	 * Clears the fragment: removes all tags and text.
	 */
	public void clear () {
		// Removes all the tags
		for ( int i=0; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			if ( isChar1(ch) ) {
				if ( ch == PCONT_STANDALONE ) {
					int pkey = toKey(ch, ctext.charAt(++i));
					String pct = tags.getPCont(pkey).getCodedText();
					for ( int j=0; j<pct.length(); j++ ) {
						char pch = pct.charAt(j);
						if ( isChar1(pch) ) {
							tags.remove(toKey(pch, pct.charAt(++j)));
						}
					}
					tags.removePCont(pkey);
				}
				else {
					tags.remove(toKey(ch, ctext.charAt(++i)));
				}
			}
		}
		// Reset the text
		ctext = new StringBuilder();
	}

	/**
	 * Helper methods calling {@link #getCodedTextPosition(CharSequence, int, boolean)} with the coded
	 * text of this fragment as the first parameter.
	 * @param plainTextPosition the plain text position to convert.
	 * @param leftOfTag true to return the left side position of the tag reference
	 * when the plain text position is on a tag reference (e.g. for end of range stopping
	 * before the reference). Use false to get the right side. 
	 * @return the coded text position.
	 * @see #getPlainText()
	 * @see #getCodedTextPosition(CharSequence, int, boolean)
	 */
	public int getCodedTextPosition (int plainTextPosition,
		boolean leftOfTag)
	{
		return Fragment.getCodedTextPosition(ctext, plainTextPosition, leftOfTag);
	}
	
	/**
	 * Inserts a plain text at a given position.
	 * @param plainText the plain text to insert.
	 * @param offset the position where to insert.
	 * @return the fragment itself.
	 * @throws InvalidPositionException if the offset is on a tag reference.
	 * @throws IndexOutOfBoundsException if the offset is invalid.
	 */
	public Fragment insert (CharSequence plainText,
		int offset)
	{
		checkPosition(offset);
		ctext.insert(offset, plainText);
		return this;
	}
	
	/**
	 * Inserts a code at a given position (including the end) of this fragment.
	 * @param tagType the type of tag of the code.
	 * @param type the type of the code (can be null).
	 * @param id the id of the code (if null an new ID is created automatically)
	 * This parameter is ignored when the new tag is created by connecting it with another one.
	 * @param data the original data for the code (can be null).
	 * @param offset the position where to insert the code. Use -1 to append. Other negative values or values greater
	 * then the length of the coded text also cause the code to be appended at the end of the fragment.
	 * @param connect true to connect a new opening code to its closing counterpart, or to connect
	 * a new closing code to its opening counterpart (the counterpart may be in a different fragment).
	 * Use false to create new opening or closing codes.
	 * This option is ignored used if the code is standalone.
	 * @param allowOrphan true to allow the connect option to fail, that is: to not found the counterpart
	 * of the new code. this option is ignore if connect is false or if the new code is a standalone code. 
	 * @return the new tag created.
	 */
	public CTag insert (TagType tagType,
		String type,
		String id, 
		String data,
		int offset,
		boolean connect,
		boolean allowOrphan)
	{
		// Check ID
		if ( id == null ) {
			if ( connect && !allowOrphan ) {
				throw new InvalidParameterException(
					"Cannot have auto-generated ID when requesting to link to an existing code and not allowing orphan.");
			}
			// Generate an Id
			id = tags.getStore().suggestId(false);
		}
		// Check/adjust the offset
		if (( offset < 0 ) || ( offset > ctext.length() )) {
			offset = -1; // append at the end
		}
		else { // Check if the insertion point is valid
			checkPosition(offset);
		}
		// Try to get the counterpart code if it is requested
		Tag opposite = null;
		if ( connect && ( tagType != TagType.STANDALONE )) {
			if ( tagType == TagType.OPENING ) {
				opposite = tags.getClosingTag(id);
			}
			else { // TagType.CLOSING
				opposite = tags.getOpeningTag(id);
			}
			if ( opposite == null ) {
				if ( !allowOrphan ) {
					throw new InvalidParameterException(
						String.format("Cannot add closing/opening code because opening/closing code for id '%s' does not exist.", id));
				}
			}
		}
		// Create the new code
		CTag ctag;
		if ( opposite == null ) {
			// This work also for standalone since opposite will always be null for that case
			ctag = new CTag(null, tagType, id, data);
		}
		else {
			// Here id comes from the opposite tag
			CTag tmp = (CTag)opposite;
			ctag = new CTag(tmp, data);
	
			// The case of the firstNo in closing tag is handled in getReorder()
//			// Set the canReorder value based on the opposite tag.
//			switch ( tmp.getTagType() ) {
//			case OPENING:
//				if ( tmp.getCanReorder() == CanReorder.FIRSTNO ) {
//					ctag.setCanReorder(CanReorder.NO);
//					break;
//				}
//				// Else: fall thru
//			default:
//				ctag.setCanReorder(tmp.getCanReorder());
//			}
			// But we still need to copy the value (maybe should move it to code-common?)
			ctag.setCanReorder(tmp.getCanReorder());
		}

		// Add the new code to the collection and add its reference in the coded text
		if ( offset == -1 ) ctext.append(Fragment.toRef(tags.add(ctag)));
		else ctext.insert(offset, Fragment.toRef(tags.add(ctag)));
		// Set the type only if not null, to avoid overriding type already defined
		// when this new code is created with a counterpart.
		if ( type != null ) {
			ctag.setType(type);
		}
		return ctag;
	}
	
//	/**
//	 * Creates, at the end of this fragment, a new opening or standalone code, or closes an existing opening code.
//	 * Helper method equivalent to <code>insert(tagType, type, null, data, -1, (tagType==TagType.CLOSING), false)</code>.
//	 * @param tagType the type of tag of the code.
//	 * @param type the type of the code.
//	 * @param data the original data for the code.
//	 * @return the new tag created.
//	 */
//	public CTagNew append (TagType tagType,
//		String type, 
//		String data)
//	{
//		return _insert(tagType, type, null, data, -1, (tagType==TagType.CLOSING), false);
//	}

	/**
	 * Appends a standalone code to this fragment.
	 * @param id the id of the code tag (cannot be null).
	 * @param data the original data for this tag (can be null).
	 * @return the new tag.
	 */
	public CTag appendCode (String id,
		String data)
	{
		return insert(TagType.STANDALONE, null, id, data, -1, false, false);
	}

	/**
	 * Creates at the end of this fragment an opening tag for a new code.
	 * @param id the id to use for this code (use null to use an automatic ID).
	 * @param data the original data for the code, e.g. <code>&lt;B&gt;</code> (can be null).
	 * @return the new tag.
	 */
	public CTag openCodeSpan (String id,
		String data)
	{
		return insert(TagType.OPENING, null, id, data, -1, false, false);
	}
	
	/**
	 * Creates at the end of this fragment a closing tag for an existing code.
	 * @param id the id of the code to close (must not be null).
	 * @param data the original data for the code, e.g. <code>&lt;B&gt;</code> (can be null).
	 * @return the new tag.
	 */
	public CTag closeCodeSpan (String id,
		String data)
	{
		return insert(TagType.CLOSING, null, id, data, -1, true, false);
	}
	
}
