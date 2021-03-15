/*===========================================================================
  Copyright (C) 2008-2013 by the Okapi Framework contributors
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

package org.w3c.its;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

/**
 * Provides the methods traverse a document and access the ITS data category available on each node.
 */
public interface ITraversal {

	/**
	 * Flag indicating a Left-To-Right directionality.
	 */
	int DIR_LTR              = 0;
	/**
	 * Flag indicating a Right-To-Left directionality.
	 */
	int DIR_RTL              = 1;
	/**
	 * Flag indicating a Left-To-Right directionality override.
	 */
	int DIR_LRO              = 2;
	/**
	 * Flag indicating a Right-To-Left directionality override.
	 */
	int DIR_RLO              = 3;
	
	/**
	 * Flag indicating an element that is not within text.
	 */
	int WITHINTEXT_NO        = 0;
	/**
	 * Flag indicating an element that is within text (inline).
	 */
	int WITHINTEXT_YES       = 1;
	/**
	 * Flag indicating an element that is nested.
	 */
	int WITHINTEXT_NESTED    = 2;
	
	/**
	 * Starts the traversal of the document. This method must be called
	 * once before you call {@link #nextNode()}.
	 */
	void startTraversal ();
	
	/**
	 * Moves to the next node in the traversal of the document.
	 * @return the current node of the traversal. Null if the document is traversed.
	 */
	Node nextNode ();
	
	/**
	 * Indicates whether the current node is found while backtracking. For example,
	 * for an element node, this indicate the equivalent of a closing tag.
	 * @return true if the current node is found while backtracking, false otherwise. 
	 */
	boolean backTracking ();

	/**
	 * Indicates if the current element or one of its attributes is
	 * translatable.
	 * @param attribute the attribute to query or null to query the element.
	 * @return true if the queried element or attribute is translatable, false otherwise.
	 */
	boolean getTranslate (Attr attribute);

	/**
	 * Gets the target pointer for the current element of the traversal or one of its attributes.
	 * @param attribute the attribute to query or null to query the element.
	 * @return the XPath relative to the current element or attribute to the node where the
	 * translation should be placed.
	 */
	String getTargetPointer (Attr attribute);
	
	/**
	 * Gets the id value for the current element of the traversal or one of its attributes.
	 * @param attribute the attribute to query or null to query the element.
	 * This method is used for both the ITS 2.0 feature and the deprecated extension to ITS 1.0.
	 * @return the value of the identifier for the queried part.
	 */
	String getIdValue (Attr attribute);
	
	/**
	 * Gets the directionality for the text of a given attribute of the current 
	 * node of the traversal.
	 * @param attribute the attribute to query or null to query the element.
	 * @return the directionality information
	 * ({@link #DIR_LTR}, {@link #DIR_RTL}, {@link #DIR_LRO} or {@link #DIR_RLO})
	 * for the queried part.
	 */
	int getDirectionality (Attr attribute);

	/**
	 * Gets the external resource reference for the current element of the traversal
	 * or one of its attributes. 
	 * @param attribute the attribute to query or null to query the element.
	 * @return the external resource reference for the queried part, or null.
	 */
	String getExternalResourceRef (Attr attribute);
	
	/**
	 * Gets the standoff location of the Localization Quality Issue records for the current element
	 * or one of its attributes. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @return the standoff location of the records for the queried parts (can be null).
	 */
	String getLocQualityIssuesRef (Attr attribute);
	
	/**
	 * Gets the number of Localization Quality Issue annotations for the current element
	 * or one of its attributes. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @return the number of issues for the queried part.
	 */
	int getLocQualityIssueCount (Attr attribute);
	
	/**
	 * Gets the type of the Localization Quality Issue instance for the current element
	 * or one of its attribute, for the given index. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @param index the index of the issue in the list (zero-based).
	 * @return the type for the issue at the given index for the queried part (can be null).
	 * @see #getLocQualityIssueCount(Attr)
	 */
	String getLocQualityIssueType (Attr attribute, int index);
	
	/**
	 * Gets the comment of the Localization Quality Issue instance for the current element
	 * or one of its attribute, for the given index. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @param index the index of the issue in the list (zero-based).
	 * @return the comment for the issue at the given index for the queried part (can be null).
	 * @see #getLocQualityIssueCount(Attr)
	 */
	String getLocQualityIssueComment (Attr attribute, int index);
	
	/**
	 * Gets the severity of the Localization Quality Issue instance for the current element
	 * or one of its attribute, for the given index. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @param index the index of the issue in the list (zero-based).
	 * @return the severity for the issue at the given index for the queried part (can be null).
	 * @see #getLocQualityIssueCount(Attr)
	 */
	Double getLocQualityIssueSeverity (Attr attribute, int index);
	
	/**
	 * Gets the comment of the Localization Quality Issue instance for the current element
	 * or one of its attribute, for the given index. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @param index the index of the issue in the list (zero-based).
	 * @return the comment for the issue at the given index for the queried part (can be null).
	 * @see #getLocQualityIssueCount(Attr)
	 */
	String getLocQualityIssueProfileRef (Attr attribute, int index);
	
	/**
	 * Gets the enabled/disabled flag of the Localization Quality Issue instance for the current element
	 * or one of its attribute, for the given index. 
	 * @param attribute the attribute to query, or null to query the current element.
	 * @param index the index of the issue in the list (zero-based).
	 * @return the enabled/disabled flag for the issue at the given index for the queried part (can be null).
	 * @see #getLocQualityIssueCount(Attr)
	 */
	Boolean getLocQualityIssueEnabled (Attr attribute, int index);
	
	/**
	 * Gets the element-withinText-related information for the current element.
	 * This data category applies only to elements.
	 * @return One of the WINTINTEXT_* values.
	 */
	int getWithinText ();

	/**
	 * Indicates if a given attribute of the current element of the traversal or
	 * one of its attributes is a term.
	 * @param attribute The attribute to query or null for the element.
	 * @return True if the queried part is a term, false otherwise.
	 */
	boolean getTerm (Attr attribute);
	
	/**
	 * Gets the information associated with a given term node or one of its
	 * attributes.
	 * @param attribute The attribute to query or null for the element.
	 * @return the information associated with the queried part.
	 */
	String getTermInfo (Attr attribute);
	
	/**
	 * Gets the confidence associated with a given term node or one of its
	 * attributes.
	 * @param attribute The attribute to query or null for the element.
	 * @return the confidence associated with the queried part.
	 */
	Double getTermConfidence (Attr attribute);
	
	/**
	 * Gets the localization note of the current element of the traversal or
	 * one of its attributes.
	 * @param attribute the attribute to query or null for the element.
	 * @return The localization note of the queried part.
	 */
	String getLocNote (Attr attribute);
	
	String getLocNoteType (Attr attribute);
	
	/**
	 * Gets the domain or domains for the current element
	 * or one of its attributes.
	 * @param attribute the attribute to query or null to query the current element.
	 * @return a comma-separated string representing the list of domains for the queried part.
	 * See <a href='http://www.w3.org/TR/its20/#domain-implementation'>http://www.w3.org/TR/its20/#domain-implementation</a>
	 * for details on the format of the string.
	 */
	String getDomains (Attr attribute);

	/**
	 * Gets the locale filter information.
	 * @return A a comma-separated list of extended language ranges as defined in 
	 * BCP-47 (and possibly empty). If the first character is '!' the type is 'exclude'
	 * otherwise the type is 'include'.
	 */
	String getLocaleFilter ();
	
	/**
	 * Indicates if the white spaces of the current element of the traversal
	 * or the given attribute must be preserved. 
	 * @return True if the white spaces of the current element or the given attribute must be preserve,
	 * false if they may or may not be preserved.
	 */
	boolean preserveWS (Attr attribute);

	/**
	 * Gets the language for the current element of the traversal and its attributes.
	 * @return The language code for the current element and its attributes. 
	 */
	String getLanguage ();
	
	/**
	 * Gets the storage size for the current element or one of its attributes.
	 * @param attribute the attribute to query or null to query the current element.
	 * @return the storage size for the queried part.
	 */
	Integer getStorageSize(Attr attribute);
	
	/**
	 * Gets the storage encoding for the current element or one of its attributes.
	 * @param attribute the attribute to query or null to query the current element.
	 * @return the storage encoding for the queried part.
	 */
	String getStorageEncoding (Attr attribute);
	
	/**
	 * Gets the storage line-break type for the current element or one of its attributes.
	 * @param attribute the attribute to query or null to query the current element.
	 * @return the storage line-break type for the queried part.
	 */
	String getLineBreakType (Attr attribute);
	
	/**
	 * Gets the pattern of allowed characters for the current element or one of its attributes.
	 * @param attribute the attribute to query or null to query the current element.
	 * @return the pattern of allowed characters for the queried part.
	 */
	String getAllowedCharacters (Attr attribute);

	/**
	 * Gets the tools references associated with the current element of the traversal and its attributes.
	 * <p>The returned value is sorted by data category and hold all data categories within scope
	 * (not just the ones set on the given node).
	 * @return the tools references associated with the current element of the traversal and its attributes.
	 */
	String getAnnotatorsRef ();
	
	/**
	 * Gets the annotator reference for a given data category.
	 * @param dc the name of the data category to look up.
	 * @return the reference for the given data category, or null.
	 */
	String getAnnotatorRef (String dc);
	
	/**
	 * Gets the MT Confidence value for the current element of the traversal or one
	 * of its attributes.
	 * @param attribute the attribute to query or null for the element.
	 * @return the MT Confidence value or null if none is set.
	 */
	Double getMtConfidence (Attr attribute);

	String getTextAnalysisClass (Attr attribute);
	
	String getTextAnalysisSource (Attr attribute);
	
	String getTextAnalysisIdent (Attr attribute);
	
	Double getTextAnalysisConfidence (Attr attribute);

	Double getLocQualityRatingScore (Attr attribute);
	
	Integer getLocQualityRatingVote (Attr attribute);
	
	Double getLocQualityRatingScoreThreshold (Attr attribute);
	
	Integer getLocQualityRatingVoteThreshold (Attr attribute);
	
	String getLocQualityRatingProfileRef (Attr attribute);
	
	String getProvRecordsRef (Attr attribute);
	
	int getProvRecordCount (Attr attribute);
	
	String getProvPerson (Attr attribute, int index);
		
	String getProvOrg (Attr attribute, int index);
		
	String getProvTool (Attr attribute, int index);
		
	String getProvRevPerson (Attr attribute, int index);
		
	String getProvRevOrg (Attr attribute, int index);
		
	String getProvRevTool (Attr attribute, int index);
	
	String getProvRef (Attr attribute, int index);

}
