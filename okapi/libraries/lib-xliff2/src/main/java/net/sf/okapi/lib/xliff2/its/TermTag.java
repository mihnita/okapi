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

package net.sf.okapi.lib.xliff2.its;

import java.util.Objects;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.TagType;

/**
 * Implements the XLIFF term annotation and the ITS 
 * <a href='http://www.w3.org/TR/its20/#terminology'>Terminology</a> data category.
 */
public class TermTag extends MTag {

	public final static String TYPE_TERM = "term";
	public final static String TYPE_ITSTERMNO = "its:term-no";
	
	private String annotatorRef;
	private Double termConfidence;

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public TermTag (TermTag original) {
		// Create the new object from the copy constructor
		super(original);
		// Copy the TermTag-specific fields
		annotatorRef = original.annotatorRef;
		termConfidence = original.termConfidence;
	}
	
	/**
	 * Creates a new {@link TermTag} object.
	 * @param id the id of the new term tag (cannot be null).
	 */
	public TermTag (String id) {
		super(id, TYPE_TERM);
	}

	/**
	 * Creates a new opening {@link TermTag} object from an existing marker tag.
	 * @param tag the marker tag to use.
	 * @param type the type of the annotation.
	 * @param ar the annotator-references for this marker (can be null).
	 */
	public TermTag (MTag tag,
		String type,
		AnnotatorsRef ar)
	{
		super(tag, null);
		if ( tag.getTagType() != TagType.OPENING ) {
			throw new InvalidParameterException("the original tag must be an opening tag.");
		}
		setType(type);
		this.setAnnotatorRef(ar);
	}
	
	/**
	 * Gets the the id/name of this data category.
	 * @return the id/name of this data category.
	 */
	public String getDataCategoryName () {
		return "terminology";
	}

	/**
	 * Validates this data category.
	 */
	public void validate () {
		if (( termConfidence != null ) && ( getAnnotatorRef() == null )) {
			throw new XLIFFException("An annotator reference must be defined when termConfidence is defined.");
		}
	}

	@Override
	public void setType (String type) {
		if ( type == null ) {
			super.setType(TYPE_TERM);
		}
		else if ( !type.equals(TYPE_TERM) && !type.equals(TYPE_ITSTERMNO) ) {
			throw new InvalidParameterException("Type must be 'term' or 'its:term-no'");
		}
		else {
			super.setType(type);
		}
	}

	/**
	 * Indicates if this is a term.
	 * @return true if this annotation is a term, false otherwise.
	 */
	public boolean isTerm () {
		return super.getType().equals(TYPE_TERM);
	}

	/**
	 * Sets the flag indicating if this is a term. 
	 * @param term true if this is a term, false otherwise.
	 */
	public void setTerm (boolean term) {
		if ( term ) super.setType(TYPE_TERM);
		else super.setType(TYPE_ITSTERMNO);
	}
	
	/**
	 * Gets the confidence on whether this is a term or not.
	 * @return the confidence on whether this is a term or not.
	 */
	public Double getTermConfidence () {
		return termConfidence;
	}
	
	/**
	 * Sets the confidence on whether this is a term or not.
	 * @param termConfidence the confidence on whether this is a term or not (between 0.0 and 1.0).
	 */
	public void setTermConfidence (Double termConfidence) {
		if ( termConfidence != null ) {
			if (( termConfidence < 0.0 ) || ( termConfidence > 1.0 )) {
				throw new InvalidParameterException(String.format("The termConfidence value '%f' is out of the [0.0 to 1.0] range.",
					termConfidence));
			}
		}
		this.termConfidence = termConfidence;
	}

	@Override
	public boolean hasITSItem () {
		return false;
	}

	/**
	 * This method always return null: Only the Terminology data category can be used on a term annotation.
	 * @return always null
	 */
	@Override
	public ITSItems getITSItems () {
		return null;
	}

	/**
	 * This method always throws an UnsupportedOperationException exception:
	 * Only the Terminology data category can be used on a term annotation.
	 * @throws UnsupportedOperationException in all cases.
	 */
	@Override
	public void setITSItems (ITSItems itsItems) {
		throw new UnsupportedOperationException("Only the Terminology data category can be used on a term annotation.");
	}

	/**
	 * Sets the annotator reference information for this data category.
	 * @param annotatorRef the reference string to set (can be null).
	 */
	public void setAnnotatorRef (String annotatorRef) {
		this.annotatorRef = annotatorRef;
	}

	/**
	 * Sets the annotator reference information for this data category.
	 * @param ar the set of references read from <code>its:annotatorsRef</code>.
	 * If it is null, or if there is no reference for the relevant data category: no change is made. 
	 */
	public void setAnnotatorRef (AnnotatorsRef ar) {
		if ( ar == null ) return;
		String ref = ar.get(getDataCategoryName());
		if ( ref != null ) {
			annotatorRef = ref;
		}
	}

	/**
	 * Gets the annotator reference currently set for this data category.
	 * @return the annotator reference currently set for this data category.
	 */
	public String getAnnotatorRef () {
		return annotatorRef;
	}

	/**
	 * Gets the term information for this marker.
	 * This is the same as calling {@link #getValue()}.
	 * @return the term information for this marker (can be null).
	 */
	public String getTermInfo () {
		return getValue();
	}
	
	/**
	 * Sets the term information for this marker.
	 * Note that this call automatically calls <code>setRef(null);</code> 
	 * because ITS terms can have only either a reference or a value but not both.
	 * @param termInfo the new information to set (can be null).
	 */
	public void setTermInfo (String termInfo) {
		setValue(termInfo);
		setRef(null);
	}
	
	/**
	 * Gets the term information reference for this marker.
	 * This is the same as calling {@link #getRef()}.
	 * @return the term information reference for this marker (can be null).
	 */
	public String getTermInfoRef () {
		return getRef();
	}
	
	/**
	 * Sets the term information reference for this marker.
	 * Note that this call automatically calls <code>setValue(null);</code> 
	 * because ITS terms can have only either a reference or a value but not both.
	 * @param termInfoRef the new information reference to set.
	 */
	public void setTermInfoRef (String termInfoRef) {
		setRef(termInfoRef);
		setValue(null);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (!(other instanceof TermTag)) return false;
		if (!super.equals(other)) return false;

		TermTag termTag = (TermTag) other;
		return Objects.equals(annotatorRef, termTag.annotatorRef) &&
				Objects.equals(termConfidence, termTag.termConfidence);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), annotatorRef, termConfidence);
	}
}
