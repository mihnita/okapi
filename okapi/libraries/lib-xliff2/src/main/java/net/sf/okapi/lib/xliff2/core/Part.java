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

import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Represents a part of a unit. A part corresponds to an &lt;ignorable&gt; or a &lt;segment&gt; element.
 * See the class {@link Segment} for a part that is a segment. 
 */
public class Part {

	/**
	 * Options when getting a target which does not exists yet.
	 */
	public enum GetTarget {
		/**
		 * Indicates to not create a target if a target does not exists yet: the target remains null.
		 */
		DONT_CREATE,
		/**
		 * Indicates to create an empty target if a target does not exist yet.
		 */
		CREATE_EMPTY,
		/**
		 * Indicates to clone the source in the target if a target does not exist yet.
		 */
		CLONE_SOURCE
	}
	
	private Store store;
	private Fragment source;
	private Fragment target;
	private String id;
	private int targetOrder;
	private boolean preserveWS = false;

	/**
	 * Copy constructor.
	 * @param original the original part to duplicate.
	 */
	public Part (Part original) {
		// Copy the fields
		store = original.store;
		source = new Fragment(original.source, store, false);
		if ( original.hasTarget() ) {
			target = new Fragment(original.target, store, true);
		}
		id = original.id;
		targetOrder = original.targetOrder;
		preserveWS = original.preserveWS;
	}
	
	/**
	 * Creates a new part with an empty source.
	 * @param store the store associated with the new part (cannot be null).
	 */
	public Part (Store store) {
		this.store = store;
		source = new Fragment(store, false);
	}
	
	/**
	 * Creates a new part with a given plain text source.
	 * @param store the store associated with the new part (cannot be null).
	 * @param sourceContent the plain text source content of the new part.
	 */
	public Part (Store store,
		String sourceContent)
	{
		this.store = store;
		source = new Fragment(store, false, sourceContent);
	}

	/**
	 * Gets the id for this part.
	 * @return the id for this part if one exists, return null otherwise.
	 */
	public String getId () {
		return id;
	}
	
	/**
	 * Gets the id for this part.
	 * @param createIfNeeded true to assign automatically an id if none is already set,
	 * false to get null if there is no id set.
	 * @return the id for this part (can be null if createIfNeeded is false).
	 */
	public String getId (boolean createIfNeeded) {
		// Optionally create an id if there is none defined for this part. 
		if (( id == null ) && createIfNeeded ) {
			id = getStore().suggestId(true);
		}
		return id;
	}
	
	/**
	 * Sets the id for this part.
	 * @param id the new id for this part (can be null).
	 * No check is done for uniqueness.
	 */
	public void setId (String id) {
		this.id = id;
	}
	
	/**
	 * Indicates if the whitespace of this part's content must be preserved.
	 * @return true if the whitespace must be preserved, false if the whitespace can be modified.
	 */
	public boolean getPreserveWS () {
		return preserveWS;
	}
	
	/**
	 * Sets the flag indicating if the whitespace of this part's content must be preserved.
	 * @param preserveWS true to preserve the whitespace, false otherwise.
	 */
	public void setPreserveWS (boolean preserveWS) {
		this.preserveWS = preserveWS;
	}
	
	/**
	 * Gets the source fragment for this part.
	 * @return the source fragment for this part (never null).
	 */
	public Fragment getSource () {
		return source;
	}
	
	/**
	 * Sets the fragment of the source for this part.
	 * The fragment must have the same store as the part.
	 * @param fragment the fragment to set.
	 * @return the new source fragment.
	 */
	public Fragment setSource (Fragment fragment) {
		if ( store != fragment.getStore() ) {
			throw new XLIFFException("The fragment passed in setSource must use the same codes store.");
		}
		source = fragment;
		return source;
	}
	
	/**
	 * Sets the source for this part as a new fragment made of a plain text string.
	 * @param plainText the plain text source content to set.
	 * @return the new source fragment.
	 */
	public Fragment setSource (String plainText) {
		source = new Fragment(store, false, plainText);
		return source;
	}
	
	/**
	 * Indicates if this part has a target.
	 * @return true if this part has a target, false otherwise.
	 */
	public boolean hasTarget () {
		return (target != null);
	}

	/**
	 * Gets the target fragment for this part, and possibly create it if it does not exists yet.
	 * @param creationOption action to take if no target exists yet for this part.
	 * @return the target fragment or null.
	 * @see #getTarget()
	 */
	public Fragment getTarget (GetTarget creationOption) {
		if ( target == null ) {
			switch ( creationOption ) {
			case DONT_CREATE:
				break;
			case CREATE_EMPTY:
				target = new Fragment(store, true);
				break;
			case CLONE_SOURCE:
				target = new Fragment(source, source.getStore(), true);
				break;
			}
			
		}
		return target;
	}
	
	/**
	 * Gets the target fragment for this part.
	 * <ul>
	 * <li>Use this when you know the target exists.</li>
	 * <li>Use {@link #getTarget(GetTarget)} to create the fragment if needed.</li>
	 * </ul>
	 * @return the target fragment or null
	 * @see #getTarget(GetTarget)
	 */
	public Fragment getTarget () {
		return target;
	}
	
	/**
	 * Sets the fragment of the target for this part.
	 * The fragment must have the same store as the part.
	 * @param fragment the fragment to set.
	 * @return the new target fragment.
	 */
	public Fragment setTarget (Fragment fragment) {
		if ( store != fragment.getStore() ) {
			throw new XLIFFException("The fragment passed in setTarget must use the same codes store.");
		}
		target = fragment;
		return target;
	}
	
	/**
	 * Sets the target for this part as a new fragment made of a plain text string.
	 * @param plainText the plain text target content to set.
	 * @return the new target fragment.
	 */
	public Fragment setTarget (String plainText) {
		target = new Fragment(store, true, plainText);
		return target;
	}
	
	/**
	 * Gets all the source tags for the unit where this part is.
	 * @return all the source tags for the unit where this part is.
	 */
	public Tags getSourceTags () {
		return store.getSourceTags();
	}
	
	/**
	 * Gets all the target tags for the unit where this part is.
	 * @return all the target tags for the unit where this part is.
	 */
	public Tags getTargetTags () {
		return store.getTargetTags();
	}
	
	/**
	 * Sets the target order value for this part. Zero means the default source order. 
	 * @param targetOrder the new target order for this part.
	 */
	public void setTargetOrder (int targetOrder) {
		this.targetOrder = targetOrder;
	}
	
	/**
	 * Gets the target order for this part. Zero means the default source order.
	 * @return the target order for this part.
	 */
	public int getTargetOrder () {
		return targetOrder;
	}
	
	/**
	 * Gets the store for this part.
	 * @return the store for this part (never null).
	 */
	public Store getStore () {
		return store;
	}

	/**
	 * Indicates if this part is a segment or an ignorable.
	 * @return true if this part is a segment, false otherwise.
	 */
	public boolean isSegment () {
		return false;
	}

	/**
	 * Removes the markers in the source or target content of this part.
	 * @param target true to remove from the target, false to remove from the source.
	 * @param type the type of markers to remove (or null to remove all markers).
	 */
	public void removeMarkers (boolean target,
		String type)
	{
		Fragment frag;
		Tags tags;
		if ( target ) {
			if ( !hasTarget() ) return;
			frag = getTarget();
			tags = getStore().getTargetTags();
		}
		else {
			frag = getSource();
			tags = getStore().getSourceTags();
		}

		// Loop through the part and remove the annotations
		String ct = frag.getCodedText();
		for ( int i=0; i<ct.length(); i++ ) {
			if ( Fragment.isChar1(ct.charAt(i)) ) {
				Tag tag = tags.get(ct, i);
				if ( tag.isMarker() ) {
					if ( type != null ) { // Skip annotation markers not of the specified type
						if ( !type.equals(tag.getType()) ) continue;
					}
					frag.remove(tag);
					// Index is not incremented: that will adjust it
				}
				else {
					i++;
				}
			}
		}
	}
	
	/**
	 * Expands all {@link PCont} references in this part into normal content.
	 * This is done on both source and target.
	 */
	public void showProtectedContent () {
		getSource().showProtectedContent();
		if ( hasTarget() ) getTarget().showProtectedContent();
	}

}
