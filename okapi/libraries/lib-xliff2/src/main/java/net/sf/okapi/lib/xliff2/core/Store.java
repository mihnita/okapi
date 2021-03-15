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

import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Represents the store where code and markers, as well as other information,
 * are stored and shared for a given object implementing {@link IWithStore} (e.g. a {@link Unit}). 
 */
public class Store {

	private final IWithStore parent;

	private Tags srcTags;
	private Tags trgTags;
	private int lastSuggested;
	private int lastSegSuggested;
	
	private transient Map<String, String> map;
	
	/**
	 * Creates a new store and associates it to a given parent object
	 * (e.g. a {@link Unit} or a {@link Match}).
	 * @param parent the parent object to associate this store with (cannot be null).
	 */
	public Store (IWithStore parent) {
		if ( parent == null ) {
			throw new InvalidParameterException("Parent parameter must not be null.");
		}
		this.parent = parent;
	}
	
	/**
	 * Indicates if there is at least one code with original data in this store.
	 * @return true if there is at least one code with original data in this store.
	 */
	public boolean hasCTagWithData () {
		if ( srcTags != null ) {
			if ( srcTags.hasCTagWithData() ) return true;
		}
		if ( trgTags != null ) {
			if ( trgTags.hasCTagWithData() ) return true;
		}
		return false;
	}
	
	/**
	 * Indicates if there is at least one tag (for code or marker) in the source content for this store.
	 * @return true if there is one, false otherwise.
	 */
	public boolean hasSourceTag () {
		return (( srcTags != null ) && ( srcTags.size() > 0 ));
	}
	
	/**
	 * Indicates if there is at least one tag (for code or marker) in the target content for this store.
	 * @return true if there is one, false otherwise.
	 */
	public boolean hasTargetTag () {
		return (( trgTags != null ) && ( trgTags.size() > 0 ));
	}
	
	/**
	 * Gets the source tags for this store.
	 * @return the source tags for this store (can be empty but never null).
	 */
	public Tags getSourceTags () {
		if ( srcTags == null ) srcTags = new Tags(this);
		return srcTags;
	}
	
	/**
	 * Gets the target tags for this store.
	 * @return the target tags for this store (can be empty but never null).
	 */
	public Tags getTargetTags () {
		if ( trgTags == null ) trgTags = new Tags(this);
		return trgTags;
	}

	public void calculateDataToIdsMap () {
		map = new LinkedHashMap<>(); // LinkedHashMap to keep the order (not mandatory, but nicer)
		int mapId = 0;
		String tmp;

		if ( srcTags != null ) {
			for ( Tag tag : srcTags ) {
				if ( tag.isMarker() ) continue;
				CTag code = (CTag)tag;
				tmp = code.getData();
				if (( tmp == null ) || tmp.isEmpty() ) continue;
				tmp += code.getDataDir().getPrefix();
				if ( !map.containsKey(tmp) ) {
					// No item like this yet: create one
					map.put(tmp, "d"+(++mapId));
				}
			}
		}
		if ( trgTags != null ) {
			for ( Tag tag : trgTags ) {
				if ( tag.isMarker() ) continue;
				CTag code = (CTag)tag;
				tmp = code.getData();
				if (( tmp == null ) || tmp.isEmpty() ) continue;
				tmp += code.getDataDir().getPrefix();
				if ( !map.containsKey(tmp) ) {
					// No item like this yet: create one
					map.put(tmp, "d"+(++mapId));
				}
			}
		}
	}

	public void setOutsideRepresentationMap (Map<String, String> map) {
		this.map = map;
	}

	public Map<String, String> getOutsideRepresentationMap () {
		return map;
	}

	public String getIdForData (CTag ctag) {
		// Compute the map if needed
		if ( map == null ) {
			calculateDataToIdsMap();
		}
		// The key is made of the original data + a/l/r for the directionality
		String key = (ctag.getData()==null ? "" : ctag.getData())
			+ (ctag.getDataDir().getPrefix());
		// Try to find it
		if ( !map.containsKey(key) ) {
			// If we don't find it on the first try, refresh the map, just in case
			calculateDataToIdsMap();
			// Then try a second time
			if ( !map.containsKey(key) ) {
				throw new XLIFFException(String.format("No id found for the original data '%s'.", ctag.getData()));
			}
		}
		// Else: all is fine
		return map.get(key); // Id for the given data
	}

	/**
	 * Gets the tag for a given id and type (from the whole store).
	 * @param id the id to look for.
	 * @param tagType the type of the tag to look for.
	 * @return the tag if found, null if not found.
	 */
	public Tag getTag (String id,
		TagType tagType)
	{
		if ( srcTags != null ) {
			for ( Tag tag : srcTags ) {
				if ( id.equals(tag.getId()) ) {
					if ( tag.getTagType() == tagType ) return tag;
				}
			}
		}
		if ( trgTags != null ) {
			for ( Tag tag : trgTags ) {
				if ( id.equals(tag.getId()) ) {
					if ( tag.getTagType() == tagType ) return tag;
				}
			}
		}
		return null;
	}

	/**
	 * Indicates if a given id value is already used among the existing tags 
	 * (not the full scope of a store).
	 * <p>This method does not look for {@link PCont} or for IDs of {@link Part}.
	 * @param id the id to verify.
	 * @return true if the given id is already used among the existing tags.
	 */
	public boolean isIdUsedInTags (String id) {
		return (getTag(id) != null);
	}
	
	/**
	 * Indicates if a given id value is already in use in the object associated with this store
	 * (i.e. for a {@link Part} or for a {@link Tag}). 
	 * @param id the id value to lookup.
	 * @return true if the value is already used, false otherwise.
	 */
	public boolean isIdUsed (String id) {
		return parent.isIdUsed(id);
	}
	
	/**
	 * Gets the tag object for a given id.
	 * <p>This method does not look for {@link PCont}.
	 * @param id the id to look for.
	 * @return the {@link Tag} object for the given id, or null if not found.
	 */
	public Tag getTag (String id) {
		if ( srcTags != null ) {
			for ( Tag tag : srcTags ) {
				if ( id.equals(tag.getId()) ) return tag;
			}
		}
		if ( trgTags != null ) {
			for ( Tag tag : trgTags ) {
				if ( id.equals(tag.getId()) ) return tag;
			}
		}
		return null;
	}

	/**
	 * Gets a suggested id for code, annotation, ignorable or segment.
	 * @param forSegment true for a segment id, false for other elements.
	 * @return a suggested id which does not exist in this unit.
	 */
	public String suggestId (boolean forSegment) {
		String id;
		while ( true ) {
			if ( forSegment ) {
				id = "s" + (++lastSegSuggested);
			}
			else {
				id = String.valueOf(++lastSuggested);
			}
			if ( !parent.isIdUsed(id) ) return id;
			// Else: try another one
		}
	}

	/**
	 * Gets the source directionality for this store.
	 * @return the source directionality for this store.
	 */
	public Directionality getSourceDir () {
		return parent.getSourceDir();
	}

	/**
	 * Sets the source directionality for this store.
	 * @param dir the new source directionality to set.
	 */
	public void setSourceDir (Directionality dir) {
		parent.setSourceDir(dir);
	}

	/**
	 * Gets the target directionality for this store.
	 * @return the target directionality for this store.
	 */
	public Directionality getTargetDir () {
		return parent.getTargetDir();
	}

	/**
	 * Sets the target directionality for this store.
	 * @param dir the new target directionality to set.
	 */
	public void setTargetDir (Directionality dir) {
		parent.setTargetDir(dir);
	}

	/**
	 * Gets the main object associated with this store.
	 * @return the main object associated with this store.
	 */
	public IWithStore getParent () {
		return parent;
	}

}
