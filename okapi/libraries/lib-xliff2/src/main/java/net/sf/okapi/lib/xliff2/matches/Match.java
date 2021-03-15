/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.matches;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.Directionality;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtElements;
import net.sf.okapi.lib.xliff2.core.IWithMetadata;
import net.sf.okapi.lib.xliff2.core.IWithStore;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Store;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.metadata.Metadata;

/**
 * Represents the &lt;match&gt; element of the 
 * <a href='http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#candidates'>Translation Candidates module</a>.
 */
public class Match implements IWithExtElements, IWithExtAttributes, IWithMetadata, IWithStore {

	public static final String ELEM_MTC_MATCH = "mtc:match";
	public static final String MATCH_REF_PREFIX = "#";

	/**
	 * Default value for the <code>type</code> attribute of &lt;match&gt;.
	 */
	public static final String DEFAULT_TYPE = "tm";
	
	private final Store store = new Store(this);

	private String id;
	private String type = DEFAULT_TYPE;
	private String subType;
	private Double similarity;
	private Double matchQuality;
	private Double matchSuitability;
	private String ref;
	private String origin;
	private boolean reference;
	private Fragment source;
	private Fragment target;
	private ExtElements xelems;
	private ExtAttributes xattrs;
	private String annotatorRef;
	private Metadata metadata;

	/**
	 * Tries to reuse an existing <code>mtc:match</code> annotation to add a given match.
	 * If no existing annotation is found for the given span, one is created.
	 * The <code>ref</code> attribute of the match is set to the id of the annotation,
	 * and the match object is added to the list of matches for the unit.
	 * @param fragment the fragment to annotate.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param match the {@link Match} to set with the annotation.
	 * @return the match that was added.
	 */
	public static Match annotate (Fragment fragment,
		int start,
		int end,
		Match match)
	{
		MTag am = fragment.getOrCreateMarker(start, end, ELEM_MTC_MATCH, ELEM_MTC_MATCH);
		match.setRef(MATCH_REF_PREFIX+am.getId());
		Unit unit = (Unit)fragment.getStore().getParent();
		unit.getMatches().add(match);
		return match;
	}
	
	/**
	 * Creates a new empty {@link Match} object.
	 */
	public Match () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor
	 * @param original the original object to duplicate.
	 */
	public Match (Match original) {
		id = original.id;
		type = original.type;
		subType = original.subType;
		similarity = original.similarity;
		matchQuality = original.matchQuality;
		matchSuitability = original.matchSuitability;
		ref = original.ref;
		origin = original.origin;
		reference = original.reference;
		annotatorRef = original.annotatorRef;
		if ( original.source != null ) {
			source = new Fragment(original.source, store, false);
		}
		if ( original.target != null ) {
			target = new Fragment(original.target, store, true);
		}
		if ( original.hasExtElements() ) {
			xelems = new ExtElements(original.xelems);
		}
		if ( original.hasMetadata() ) {
			metadata = new Metadata(original.metadata);
		}
		if ( original.hasExtAttribute() ) {
			xattrs = new ExtAttributes(original.xattrs);
		}
	}

	/**
	 * Gets the {@link Store} object associated with this match.
	 * @return the store associated with this match (never null).
	 */
	public Store getStore () {
		return store;
	}
	
	/**
	 * Gets the id for this match.
	 * @return the id for this match (can be null).
	 */
	public String getId () {
		return id;
	}

	/**
	 * Sets the id for this match.
	 * @param id the new id to set (can be null).
	 */
	public void setId (String id) {
		this.id = id;
	}

	/**
	 * Gets the type for this match.
	 * @return the type for this match (never null).
	 */
	public String getType () {
		return type;
	}

	/**
	 * Sets the type for this match.
	 * @param type the type to set (Use null to set the default ({@link #DEFAULT_TYPE})).
	 */
	public void setType (String type) {
		if ( type == null ) this.type = DEFAULT_TYPE;
		else {
			Util.checkValueList("am;mt;icm;idm;tb;tm;other", type, "match type");
			this.type = type;
		}
	}

	/**
	 * Gets the subType for this match.
	 * @return the subType for this match (can be null).
	 */
	public String getSubType () {
		return subType;
	}

	/**
	 * Sets the subType for this match.
	 * @param subType the new subType to set (can be null).
	 */
	public void setSubType (String subType) {
		if ( subType != null ) {
			int n = subType.indexOf(':');
			if (( n == -1 ) || ( n == 0 ) || ( n == subType.length()-1 )) {
				throw new InvalidParameterException(String.format("Invalid value '%s' for match subType.", subType));
			}
		}
		this.subType = subType;
	}

	/**
	 * Gets the similarity value for this match
	 * @return the similarity value for this match (between 0.0 and 100.0 or can be null). 
	 */
	public Double getSimilarity () {
		return similarity;
	}

	/**
	 * Sets the similarity value for this match.
	 * @param similarity the new similarity value to set (between 0.0 and 100.0 or can be null).
	 */
	public void setSimilarity (Double similarity) {
		if ( similarity != null ) {
			if (( similarity < 0.0 ) || ( similarity > 100.0 )) {
				throw new InvalidParameterException(String.format("The value '%f' is out of the [0.0 to 100.0] range.", similarity));
			}
		}
		this.similarity = similarity;
	}

	/**
	 * Gets the quality for this match.
	 * @return the quality for this match (between 0.0 and 100.0 or can be null).
	 * @see #getMTConfidence()
	 */
	public Double getMatchQuality () {
		return matchQuality;
	}

	/**
	 * Sets the quality for this match.
	 * @param matchQuality the new quality to set (between 0.0 and 100.0 or can be null).
	 * @see #setMTConfidence(Double)
	 */
	public void setMatchQuality (Double matchQuality) {
		if ( matchQuality != null ) {
			if (( matchQuality < 0.0 ) || ( matchQuality > 100.0 )) {
				throw new InvalidParameterException(String.format("The value '%f' is out of the [0.0 to 100.0] range.", matchQuality));
			}
		}
		this.matchQuality = matchQuality;
	}
	
	/**
	 * Gets the suitability for this match.
	 * @return the suitability for this match (between 0.0 and 100.0 or can be null).
	 */
	public Double getMatchSuitability () {
		return matchSuitability;
	}

	/**
	 * Sets the suitability for this match.
	 * @param matchSuitability the new suitability to set (between 0.0 and 100.0 or can be null).
	 */
	public void setMatchSuitability (Double matchSuitability) {
		if ( matchSuitability != null ) {
			if (( matchSuitability < 0.0 ) || ( matchSuitability > 100.0 )) {
				throw new InvalidParameterException(String.format("The value '%f' is out of the [0.0 to 100.0] range.", matchSuitability));
			}
		}
		this.matchSuitability = matchSuitability;
	}

	/**
	 * Gets the ref for this match.
	 * @return the ref for this match (an IRI or can be null)
	 */
	public String getRef () {
		return ref;
	}

	/**
	 * Sets the ref for this match.
	 * @param ref the new ref value to set (an IRI or can be null)
	 */
	public void setRef (String ref) {
		this.ref = ref;
	}

	/**
	 * Gets the origin for this match.
	 * @return the origin for this match (can be null).
	 */
	public String getOrigin () {
		return origin;
	}

	/**
	 * Sets the origin for this match.  
	 * @param origin the new origin value for this match (can be null).
	 */
	public void setOrigin (String origin) {
		this.origin = origin;
	}

	/**
	 * Gets the reference for this match.
	 * @return the reference for this match.
	 */
	public boolean isReference () {
		return reference;
	}

	/**
	 * Sets the reference for this match.
	 * @param reference the new reference value for this match.
	 */
	public void setReference (boolean reference) {
		this.reference = reference;
	}

	/**
	 * Gets the source fragment for this match.
	 * @return the source fragment for this match.
	 */
	public Fragment getSource () {
		return source;
	}

	/**
	 * Sets the source fragment for this match.
	 * @param source the new source fragment for this match.
	 */
	public void setSource (Fragment source) {
		this.source = source;
	}

	/**
	 * Gets the target fragment for this match.
	 * @return the target fragment for this match.
	 */
	public Fragment getTarget () {
		return target;
	}

	/**
	 * Sets the target fragment for this match.
	 * @param target the new target fragment for this match.
	 */
	public void setTarget (Fragment target) {
		this.target = target;
	}

	@Override
	public ExtElements getExtElements () {
		if ( xelems == null ) xelems = new ExtElements();
		return xelems;
	}

	@Override
	public boolean hasExtElements () {
		if ( xelems == null ) return false;
		return (xelems.size() > 0);
	}

	@Override
	public ExtElements setExtElements (ExtElements elements) {
		this.xelems = elements;
		return getExtElements();
	}

	@Override
	public boolean isIdUsed (String id) {
		// We only need to check the markers for the Match object
		// (there is only one source and one target (not multiple parts)
		return store.isIdUsedInTags(id);
	}

	@Override
	public Directionality getSourceDir () {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSourceDir (Directionality dir) {
		// TODO Auto-generated method stub
	}

	@Override
	public Directionality getTargetDir () {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTargetDir (Directionality dir) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setExtAttributes (ExtAttributes attributes) {
		this.xattrs = attributes;
	}

	@Override
	public ExtAttributes getExtAttributes () {
		if ( xattrs == null ) {
			xattrs = new ExtAttributes();
		}
		return xattrs;
	}

	@Override
	public boolean hasExtAttribute () {
		if ( xattrs == null ) return false;
		return !xattrs.isEmpty();
	}

	@Override
	public String getExtAttributeValue (String namespaceURI,
		String localName)
	{
		if ( xattrs == null ) return null;
		return xattrs.getAttributeValue(namespaceURI, localName);
	}

	/**
	 * Gets the <a href='http://www.w3.org/TR/its20/#its-tool-annotation'>ITS annotator reference</a> for this match.
	 * @return the ITS annotator reference for this match (can be null).
	 */
	public String getAnnotatorRef () {
		return annotatorRef;
	}
	
	/**
	 * Sets the the <a href='http://www.w3.org/TR/its20/#its-tool-annotation'>ITS annotator reference</a> for this match.
	 * @param annotatorRef the new ITS annotator reference (can be null).
	 */
	public void setAnnoatorRef (String annotatorRef) {
		this.annotatorRef = annotatorRef;
	}

	/**
	 * Gets the <a href='http://www.w3.org/TR/its20/#mtconfidence'>ITS MT Confidence</a> for this match.
	 * This value is {@link #getMatchQuality()} divided by 100.0.
	 * @return the ITS MT Confidence for this match.
	 * @see #getMatchQuality()
	 */
	public Double getMTConfidence () {
		if ( matchQuality != null ) {
			return matchQuality / 100.0;
		}
		return null;
	}

	/**
	 * Sets the <a href='http://www.w3.org/TR/its20/#mtconfidence'>ITS MT Confidence</a> for this match.
	 * This value is the same as the {@link #getMatchQuality()} divided by 100.0. 
	 * @param mtConfidence the new MT Confidence value to set (can be null).
	 * @see #setMatchQuality(Double)
	 */
	public void setMTConfidence (Double mtConfidence) {
		if ( mtConfidence == null ) {
			this.matchQuality = null;
			return;
		}
		// Else:
		if (( mtConfidence < 0.0 ) || ( mtConfidence > 1.0 )) {
			throw new InvalidParameterException(String.format("The value '%f' is out of the [0.0 to 1.0] range.", mtConfidence));
		}
		this.matchQuality = (mtConfidence*100.0);
	}

	@Override
	public boolean hasMetadata () {
		if ( metadata == null ) return false;
		return !metadata.isEmpty();
	}

	@Override
	public Metadata getMetadata () {
		if ( metadata == null ) metadata = new Metadata();
		return metadata;
	}

	@Override
	public void setMetadata (Metadata metadata) {
		this.metadata = metadata;
	}

}
