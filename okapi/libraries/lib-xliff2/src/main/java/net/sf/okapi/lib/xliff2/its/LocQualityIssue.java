/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Implements the <a href='http://www.w3.org/TR/its20/#lqissue'>Localization Quality Issue</a> data category.
 */
public class LocQualityIssue extends DataCategory {

	private static final String TYPES = ";terminology;mistranslation;omission;untranslated;addition;duplication;"
		+ "inconsistency;grammar;legal;register;locale-specific-content;locale-violation;style;characters;misspelling;"
		+ "typographical;formatting;inconsistent-entities;numbers;markup;pattern-problem;whitespace;internationalization;"
		+ "length;non-conformance;uncategorized;other;";
	
	private String type;
	private String comment;
	private Double severity;
	private String profileRef;
	private boolean enabled = true;
	private String unresolvedGroupRef;
	
	/**
	 * Creates a new {@link LocQualityIssue} object without initial data.
	 */
	public LocQualityIssue () {
		// Constructor without parameters
	}
	
	/**
	 * Creates a new {@link LocQualityIssue} object with a comment.
	 * @param comment the comment to set.
	 */
	public LocQualityIssue (String comment) {
		setComment(comment);
	}
	
	@Override
	public String getDataCategoryName () {
		return DataCategories.LOCQUALITYISSUE;
	}
	
	@Override
	public void validate () {
		if (( comment == null ) && ( type == null )) {
			throw new XLIFFException("LQI must have at least comment or type defined.");
		}
	}

	@Override
	public IITSItem createCopy () {
		LocQualityIssue newItem = new LocQualityIssue(comment);
		newItem.setAnnotatorRef(getAnnotatorRef());

		newItem.enabled = enabled;
		newItem.profileRef = profileRef;
		newItem.severity = severity;
		newItem.type = type;
		newItem.unresolvedGroupRef = unresolvedGroupRef;
		
		return newItem;
	}

	@Override
	public boolean hasUnresolvedGroup () {
		return (unresolvedGroupRef != null);
	}
	
	public String getUnresolvedGroupRef () {
		return unresolvedGroupRef;
	}
	
	public void setUnresolvedGroupRef (String unresolvedGroupRef) {
		this.unresolvedGroupRef = unresolvedGroupRef;
	}
	
	public String getType () {
		return type;
	}

	public void setType (String type) {
		if (( type != null ) && (!TYPES.contains(";" + type + ";"))) {
			throw new InvalidParameterException(String.format("The string '%s' is not a valid type.", type));
		}
		this.type = type;
	}

	public String getComment () {
		return comment;
	}

	public void setComment (String comment) {
		this.comment = comment;
	}

	public Double getSeverity () {
		return severity;
	}

	public void setSeverity (double severity) {
		if (( severity < 0.0 ) || ( severity > 100.0 )) {
			throw new InvalidParameterException(String.format("The value '%f' is out of range.", severity));
		}
		this.severity = severity;
	}

	public String getProfileRef () {
		return profileRef;
	}

	public void setProfileRef (String profileRef) {
		this.profileRef = profileRef;
	}

	public boolean isEnabled () {
		return enabled;
	}

	public void setEnabled (boolean enabled) {
		this.enabled = enabled;
	}

}
