/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Represents a collection of tags for a specific content.
 */
public class Tags implements Iterable<Tag> {

	private final Store store;

	private LinkedHashMap<Integer, Tag> tags;
	private LinkedHashMap<Integer, PCont> pspans;
	private LinkedHashMap<Character, Integer> lastValues;
	
	/**
	 * Creates a new {@link Tags} object.
	 * @param store the shared {@link Store} for this object.
	 */
	public Tags (Store store) {
		if ( store == null ) {
			throw new InvalidParameterException("The store parameter cannot be null.");
		}
		this.store = store;
		// Last values for auto-keys (for the tag indexing)
		lastValues = new LinkedHashMap<>();
		resetLastValues();
	}

	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lastValues == null) ? 0 : lastValues.hashCode());
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		result = prime * result + ((pspans == null) ? 0 : pspans.hashCode());
		return result;
	}

	private void resetLastValues () {
		lastValues.put(Fragment.CODE_OPENING, -1);
		lastValues.put(Fragment.CODE_CLOSING, -1);
		lastValues.put(Fragment.CODE_STANDALONE, -1);
		lastValues.put(Fragment.MARKER_OPENING, -1);
		lastValues.put(Fragment.MARKER_CLOSING, -1);
		lastValues.put(Fragment.PCONT_STANDALONE, -1);
	}
	
	protected void resetPContLastValue () {
		lastValues.put(Fragment.PCONT_STANDALONE, -1);
	}

	/**
	 * Indicates if this collection of tags has at least one code with original data.
	 * @return true if the collection has at least one code with original data, false otherwise.
	 */
	public boolean hasCTagWithData () {
		if ( tags != null ) {
			for ( Tag tag : tags.values() ) {
				if ( tag.isMarker() ) continue;
				if ( ((CTag)tag).hasData() ) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Gets the closing tag for a given opening tag.
	 * @param openingTag the opening tag.
	 * @return the closing tag, or null if it is not found.
	 */
	public Tag getClosingTag (Tag openingTag) {
		return getClosingTag(openingTag.getId());
	}
	
	/**
	 * Gets the closing tag for a given tag ID.
	 * @param id the ID of the closing tag to search for.
	 * @return the closing tag, or null if no tag with the given ID is found.
	 */
	public Tag getClosingTag (String id) {
		if ( tags == null ) return null;
		for ( Tag tag : tags.values() ) {
			if ( tag.getId().equals(id) ) {
				if ( tag.getTagType() == TagType.CLOSING ) return tag;
			}
		}
		return null;
	}
	
	/**
	 * Gets the opening tag for a given closing tag.
	 * @param closingTag the closing tag.
	 * @return the opening tag, or null if it is not found.
	 */
	public Tag getOpeningTag (Tag closingTag) {
		return getOpeningTag(closingTag.getId());
	}
	
	/**
	 * Gets the opening tag for a given tag ID.
	 * @param id the ID of the opening tag to search for.
	 * @return the opening tag, or null if no tag with the given ID is found.
	 */
	public Tag getOpeningTag (String id) {
		if ( tags == null ) return null;
		for ( Tag tag : tags.values() ) {
			if ( tag.getId().equals(id) ) {
				if ( tag.getTagType() == TagType.OPENING ) return tag;
			}
		}
		return null;
	}
	
	/**
	 * Gets the opening {@link CTag} for a given id.
	 * @param id the ID of the opening tag to search for.
	 * @return the opening {@link CTag} or null if no tag with the given ID is found.
	 * @throws InvalidParameterException if a tag is found but it is not a {@link CTag} object.
	 */
	public CTag getOpeningCTag (String id) {
		if ( tags == null ) return null;
		for ( Tag tag : tags.values() ) {
			if ( tag.getId().equals(id) ) {
				if ( tag.getTagType() == TagType.OPENING ) {
					if ( !(tag instanceof CTag) ) {
						throw new InvalidParameterException(String.format(
							"The tag id='%s' exists but is not a CTag.", id));
					}
					return (CTag)tag;
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the opening {@link MTag} for a given id.
	 * @param id the ID of the opening tag to search for.
	 * @return the opening {@link MTag} or null if no tag with the given ID is found.
	 * @throws InvalidParameterException if a tag is found but it is not a {@link MTag} object.
	 */
	public MTag getOpeningMTag (String id) {
		if ( tags == null ) return null;
		for ( Tag tag : tags.values() ) {
			if ( tag.getId().equals(id) ) {
				if ( tag.getTagType() == TagType.OPENING ) {
					if ( !(tag instanceof MTag) ) {
						throw new InvalidParameterException(String.format(
							"The tag id='%s' exists but is not a MTag.", id));
					}
					return (MTag)tag;
				}
			}
		}
		return null;
	}
	
	/**
	 * Gets the number of {@link CTag} and {@link MTag} tags in this collection.
	 * @return the number of {@link CTag} and {@link MTag} tags in this collection.
	 */
	public int size () {
		if ( tags == null ) return 0;
		return tags.size();
	}
	
	/**
	 * Gets the number of protected content items in this object.
	 * @return the number of protected content items in this object.
	 */
	public int getPContCount () {
		if ( pspans == null ) return 0;
		return pspans.size();
	}

	/**
	 * Gets the store associated with this collection.
	 * @return the store associated with this collection.
	 */
	public Store getStore () {
		return store;
	}

	/**
	 * Gets the {@link MTag} or {@link CTag} for a given key.
	 * @param key the key of the annotation or code tag to retrieve.
	 * @return the tag for the given key, or null if there is no corresponding tag.
	 */
	public Tag get (int key) {
		if ( tags != null ) {
			return tags.get(key);
		}
		return null;
	}
	
	/**
	 * Gets the {@link MTag} or {@link CTag} for a given reference in a coded text.
	 * @param ctext the coded text (e.g. String or StringBuilder object).
	 * @param pos the position of the first character of the reference.
	 * @return the tag for the given tag reference, or null if there is no corresponding tag.
	 */
	public Tag get (CharSequence ctext,
		int pos)
	{
		return get(Fragment.toKey(ctext.charAt(pos), ctext.charAt(pos+1)));
	}
	
	/**
	 * Gets the {@link CTag} for a given key.
	 * @param key the key of the code tag to retrieve.
	 * @return the tag for the given key, or null if there is no corresponding tag.
	 */
	public CTag getCTag (int key) {
		if ( tags != null ) {
			return (CTag)tags.get(key);
		}
		return null;
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
		return getCTag(Fragment.toKey(ctext.charAt(pos), ctext.charAt(pos+1)));
	}

	/**
	 * Gets the {@link MTag} for a given key.
	 * @param key the key of the code tag to retrieve.
	 * @return the tag for the given key, or null if there is no corresponding tag.
	 */
	public MTag getMTag (int key) {
		if ( tags != null ) {
			return (MTag)tags.get(key);
		}
		return null;
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
		return getMTag(Fragment.toKey(ctext.charAt(pos), ctext.charAt(pos+1)));
	}

	/**
	 * Gets the tag for a given id and tag type.
	 * @param id the id of the tag to retrieve.
	 * @param tagType the tag type of the tag to retrieve.
	 * @return the tag for the given id and tag type, or null if it is not found.
	 */
	public Tag get (String id,
		TagType tagType)
	{
		if ( tags == null ) return null;
		for ( Tag tag : tags.values() ) {
			if ( tag.getId().equals(id) && tag.getTagType().equals(tagType) ) {
				return tag;
			}
		}
		return null; // Not found
	}
	
	/**
	 * Gets the {@link PCont} for a given key.
	 * @param key the key of the protected content tag to retrieve.
	 * @return the tag for the given key, or null if there is no corresponding tag.
	 */
	public PCont getPCont (int key) {
		if ( pspans != null ) {
			return pspans.get(key);
		}
		return null;
	}
	
	/**
	 * Gets the {@link PCont} for a given reference in a coded text.
	 * @param ctext the coded text (e.g. String or StringBuilder object).
	 * @param pos the position of the first character of the reference.
	 * @return the tag for the given key, or null if there is no corresponding tag.
	 */
	public PCont getPCont (CharSequence ctext,
		int pos)
	{
		return getPCont(Fragment.toKey(ctext.charAt(pos), ctext.charAt(pos+1)));
	}
	
	/**
	 * Gets the key for a given code or marker.
	 * @param tag the tag to lookup.
	 * @return the key of the given tag, or -1 if not found.
	 */
	public int getKey (Tag tag) {
		if ( tags != null ) {
			for ( Map.Entry<Integer, Tag> entry : tags.entrySet() ) {
				if ( entry.getValue() == tag ) return entry.getKey();
			}
		}
		return -1;
	}
	
	int add (char mtype,
		Tag tag)
	{
		if ( tags == null ) tags = new LinkedHashMap<>(3);
		int value = lastValues.get(mtype);
		lastValues.put(mtype, ++value);
		int key = Fragment.toKey(mtype, Fragment.TAGREF_BASE+value);
		if ( tags.containsKey(key) ) {
			throw new XLIFFException("The key auto-selected to add this tag exists already.");
		}
		tags.put(key, tag);
		return key;
	}

	/**
	 * Adds a tag to this collection.
	 * @param tag the marker to add.
	 * @return the key of the new marker reference.
	 */
	public int add (Tag tag) {
		boolean isCode = (tag instanceof CTag);
		switch ( tag.getTagType() ) {
		case OPENING:
			return add(isCode ? Fragment.CODE_OPENING : Fragment.MARKER_OPENING, tag); 
		case CLOSING:
			return add(isCode ? Fragment.CODE_CLOSING : Fragment.MARKER_CLOSING, tag);
		case STANDALONE:
			// Fall thru
		}
		return add(Fragment.CODE_STANDALONE, tag);
	}
	
	/**
	 * Adds a protected content tag to this object.
	 * @param tag the marker to add.
	 * @return the key of the new protected content marker reference.
	 */
	public int add (PCont tag) {
		if ( pspans == null ) pspans = new LinkedHashMap<>();
		int value = lastValues.get(Fragment.PCONT_STANDALONE);
		lastValues.put(Fragment.PCONT_STANDALONE, ++value);
		int key = Fragment.toKey(Fragment.PCONT_STANDALONE, Fragment.TAGREF_BASE+value);
		if ( pspans.containsKey(key) ) {
			throw new XLIFFException("The key auto-selected to add this tag exists already.");
		}
		pspans.put(key, tag);
		return key;
	}

	/**
	 * Creates an iterator for the {@link CTag} and {@link MTag} objects in this collection.
	 * @return a new iterator for {@link CTag} and {@link MTag}.
	 */
	public Iterator<Tag> iterator () {
		if ( tags == null ) tags = new LinkedHashMap<>(3);
		return tags.values().iterator();
	}
	
	/**
	 * Creates an iterator for the {@link PCont} objects in this collection.
	 * @return a new iterator for {@link PCont}.
	 */
	public Iterator<PCont> iteratorPCont () {
		if ( pspans == null ) pspans = new LinkedHashMap<>();
		return pspans.values().iterator();
	}

	/**
	 * Removes a tag for a given key.
	 * <p><b>Warning:</b> This method does not remove the corresponding tag reference in the coded text.
	 * @param key the key of the tag to remove.
	 * @throws IndexOutOfBoundsException if the key is not found.
	 */
	public void remove (int key) {
		if ( tags == null ) return;
		Tag t = tags.remove(key);
		if ( t == null ) {
			// Else: error
			throw new IndexOutOfBoundsException("There is no code or marker tag in the list.");
		}
	}
	
	/**
	 * Removes the {@link PCont} object for a given key.
	 * <p><b>Warning:</b> This method does not remove the corresponding reference in the coded text.
	 * @param key the key of the tag to remove.
	 * @throws IndexOutOfBoundsException if the key is not found.
	 */
	public void removePCont (int key) {
		if ( pspans != null ) {
			pspans.remove(key);
			return;
		}
		// Else: error
		throw new IndexOutOfBoundsException("There is no protected content tag in the list.");
	}
	
}
