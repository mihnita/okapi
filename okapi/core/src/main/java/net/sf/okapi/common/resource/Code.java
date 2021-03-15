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

package net.sf.okapi.common.resource;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.resource.TextFragment.TagType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Represents an abstracted in-line code used in a TextFragment object.
 * For example, a <code>&lt;b&gt;</code> tag in an HTML paragraph.
 */
public class Code implements IWithProperties, Cloneable {

	/**
	 * Code type value for null type, to prevent it from ever being truly null.
	 */
	public static final String TYPE_NULL = "null";
	/**
	 * Code type value for bold.
	 */
	public static final String TYPE_BOLD = "bold";
	/**
	 * Code type value for italic.
	 */
	public static final String TYPE_ITALIC = "italic";
	/**
	 * Code type value for underline.
	 */
	public static final String TYPE_UNDERLINED = "underlined";
	/**
	 * Code type value for line-break.
	 */
	public static final String TYPE_LB = "lb";
	/**
	 * Code type value for link.
	 */
	public static final String TYPE_LINK = "link";
	/**
	 * Code type value for image.
	 */
	public static final String TYPE_IMAGE = "image";
	/**
	 * Code type value for comment.
	 */
	public static final String TYPE_COMMENT = "comment";
	/**
	 * Code type value for CDATA.
	 */
	public static final String TYPE_CDATA = "cdata";
	/**
	 * Code type value for processing instruction.
	 */
	public static final String TYPE_XML_PROCESSING_INSTRUCTION = "processing-instruction";
	/**
	 * Code type value for reference.
	 */
	public static final String TYPE_REFERENCE = "ref";
	/**
	 * Special type value indicating the code/marker is only there to hold an annotation.
	 */
	public static final String TYPE_ANNOTATION_ONLY = "_annotation_";
	/**
	 * Special type value indicating the code/marker is masking an Okapi marker
	 */
	public static final String TYPE_OKAPI_MARKER = "_okp-marker_";


	/**
	 * An extended code type prefix.
	 */
	public static final String EXTENDED_CODE_TYPE_PREFIX = "x-";

	/**
	 * An extended code type delimiter.
	 *
	 * Should be considered the following usage scheme:
	 * x-bold;
	 * x-bold;underlined;highlighted;
	 */
	public static final String EXTENDED_CODE_TYPE_DELIMITER = ";";

	/**
	 * An extended code type value delimiter.
	 *
	 * Should be considered the following usage scheme:
	 * x-color:red;
	 */
	public static final String EXTENDED_CODE_TYPE_VALUE_DELIMITER = ":";

	/**
	 * Initial capacity for data and outerData objects.
	 * Keeping it small to save space.
	 */
	private static final int DATA_DEFAULT_SIZE = 10;

	/** Initial capacity for creating annotations maps.
	 * Keeping it small to save space.
	 */
	private static final int ANNOTATIONS_INITCAP = 1;

	/**
	 * Indicates that this code has one reference or more in its data part.
	 */
	protected static final int HASREF       = 0x01;

	/**
	 * Indicates that this code can be duplicated in the text.
	 */
	public static final int CLONEABLE    = 0x02;

	/**
	 * Indicates that this code can be removed from the text.
	 */
	public static final int DELETEABLE   = 0x04;

	/**
	 * Indicates that this code was added to the text post-extraction.
	 */
	public static final int ADDED   = 0x08;

	/**
	 * Indicates that this code was merged with another code.
	 */
	public static final int MERGED   = 0x10;

	/**
	 * Indicates that this code was merged with another code.
	 */
	public static final int MARKER_MASKING   = 0x20;

	// Separators for codes stored as a string
	public static final String SEP_CODEFIELD = "\u009C";
	public static final String SEP_CODE = "\u009D";
	public static final String SEP_CODEANNOTATIONKEY = "\u009E";
	public static final String SEP_CODEANNOTATIONVALUE = "\u009F";
	/**
	 * Tag type for this code.
	 */
	protected TagType tagType;

	/**
	 * Original id for this code. Some formats use non-numeric id's which we need to
	 * preserve
	 */
	protected String originalId;

	/**
	 * Id for this code.
	 */
	protected int id;
	/**
	 * Type of the code.
	 * It MUST NEVER be null (so internal compare can be fast), null is mapped to {@link #TYPE_NULL}
	 */
	protected String type;
	/**
	 * Native data for this code.
	 * This is used to generate the text output, except if outerData is not null.
	 */
	protected StringBuilder data;

	/**
	 * serialized data for this merged code.
	 * Combines serialized string from multiple codes using Code.codesToString()
	 */
	protected String mergedData;

	/**
	 * Outer data. It must be null (not just empty) for data to be used.
	 * Outer data is reserved to store inline native codes in formats that are extraction formats
	 * such as XLIFF, TS, etc.
	 */
	protected StringBuilder outerData;

	/**
	 * Display text that is displayed as hint to the end user.
	 */
	protected String displayText;

	/**
	 * Flag for this code.
	 * This flag holds various information about the code (is it deletable, does it have a reference, etc.)
	 */
	protected int flag;
	/**
	 * InlineAnnotations for this code.
	 */

	protected LinkedHashMap<String, InlineAnnotation> inlineAnnotations;
	private Map<String, Property> properties;

	/**
	 * Helper method to convert a list of codes into a string.
	 * This method preserves the outerData in the codes.
	 * @param list the list of the codes to flatten into a string.
	 * @return the string with all the codes.
	 * @see #codesToString(List, boolean)
	 * @see #stringToCodes(String)
	 */
	public static String codesToString(final List<Code> list) {
		return codesToString(list, false); // Keep outerData
	}

	/**
	 * Helper method to convert a list of codes into a string.
	 * This method allows optionally to strip the outerData in the codes. 
	 * @param list the list of the codes to flatten into a string.
	 * @param stripOuterData true to remove the outerData in the storage string, false to keep it. 
	 * @return the string with all the codes.
	 * @see #codesToString(List)
	 * @see #stringToCodes(String)
	 */
	public static String codesToString(final List<Code> list, final boolean stripOuterData)
	{
		final StringBuilder tmp = new StringBuilder();
		for (final Code code : list) {
			final StringJoiner sj = new StringJoiner(SEP_CODEFIELD, "", SEP_CODE).add(code.tagType.name())
					.add("" + code.id).add(code.type).add(code.data).add("" + code.flag);
			// for backwards compatibility do not write out originalId if it is null
			if ( code.originalId != null ) {
				sj.add(code.originalId);
			}
			sj.add(stripOuterData ? null : code.outerData)
					.add(annotationsToString(code.inlineAnnotations));
			tmp.append(sj.toString());
		}
		return tmp.toString();
	}

	/**
	 * Indicates if two codes-storing strings have the same codes or not.
	 * @param codes1 the first codes-storing string.
	 * @param codes2 the second codes-storing string.
	 * @return true if both codes-storing strings are identical.
	 */
	public static boolean sameCodes(final List<Code> codes1, final List<Code> codes2)
	{
		if ( codes1.size() != codes2.size() ) return false;
		Code code1, code2;
		for ( int i=0; i<codes1.size(); i++ ) {
			code1 = codes1.get(i);
			code2 = codes2.get(i);
			if ( code1.id != code2.id ) return false;

			if ( (code1.data != null) && !code1.data.toString().equals(code2.data.toString()) ) return false;
		}
		return true;
	}

	/**
	 * Gets the index in a list of codes for the id of a given code.
	 * @param codes the list of codes to lookup.
	 * @param forClosing true to get the index of the closing code.
	 * @param id the id to search for.
	 * @return the index of the first opening or closing code with the given id, or -1 if not found.
	 */
	public static int getIndex(final List<Code> codes, final boolean forClosing, final int id)
	{
		if ( codes == null ) return -1;
		int i = 0;
		for (final Code code : codes) {
			if ( code.id == id ) {
				if ( forClosing ) {
					if ( code.tagType == TagType.CLOSING ) {
						return i;
					}
				}
				else {
					return i;
				}
			}
			i++;
		}
		return -1;
	}

	/**
	 * Gets a string storage representation of the annotations of a Code.
	 * @param map the list of annotations.
	 * @return the storage string.
	 */
	private static String annotationsToString(final LinkedHashMap<String, InlineAnnotation> map) {
		if (( map == null ) || map.isEmpty() ) return TYPE_NULL;
		final StringBuilder tmp = new StringBuilder();
		InlineAnnotation annotation;
		for (final String key : map.keySet()) {
			tmp.append(key).append(SEP_CODEANNOTATIONKEY);
			annotation = map.get(key);
			tmp.append((annotation==null) ? "" : annotation.toString());
			tmp.append(SEP_CODEANNOTATIONVALUE);
		}
		return tmp.toString();
	}

	/**
	 * Helper method to convert a storage string into a list of codes.
	 * @param data the storage string to convert (can be null).
	 * @return a list of the codes in the storage string.
	 * @see #codesToString(List)
	 * @see #codesToString(List, boolean)
	 */
	public static List<Code> stringToCodes(final String data) {
		final ArrayList<Code> list = new ArrayList<>();
		if ( data != null ) {
			final String[] tmpCodes = data.split(SEP_CODE, 0);
			for (final String tmp : tmpCodes) {
				if ( tmp.length() == 0 ) continue;
				final String[] tmpFields = tmp.split(SEP_CODEFIELD, -1);
				final Code code = new Code(TagType.valueOf(tmpFields[0]), tmpFields[2], tmpFields[3]);
				code.id = Integer.parseInt(tmpFields[1]);
				code.flag = Integer.parseInt(tmpFields[4]);

				int outerDataIndex = 5;
				int annotationIndex = 6;

				// test if we stored the originalId
				// normally this is not stored unless the id was a non-integer
				if (tmpFields.length >= 8) {
					// must now increment the following indexes
					outerDataIndex++;
					annotationIndex++;
					if ( !tmpFields[5].equals(TYPE_NULL) ) {
						code.originalId = tmpFields[5];
					}					
				}

				if ( !tmpFields[outerDataIndex].equals(TYPE_NULL) ) {
					if (code.outerData == null) {
						code.outerData = new StringBuilder(DATA_DEFAULT_SIZE);
					}
					code.outerData.setLength(0);
					code.outerData.append(tmpFields[outerDataIndex]);
				}
				if ( tmpFields.length > annotationIndex ) {
					code.inlineAnnotations = stringToAnnotations(tmpFields[annotationIndex]);
				}
				else
					code.inlineAnnotations = null;
				list.add(code);
			}
		}
		return list;
	}

	private static LinkedHashMap<String, InlineAnnotation> stringToAnnotations(final String data) {
		//TODO: ISSUE here: the annotations of the closing code should point to the same as the opening code!				
		if (( data == null ) || ( data.length() == 0 ) || TYPE_NULL.equals(data)) return null;
		// Create the map with low initial capacity
		final LinkedHashMap<String, InlineAnnotation> map = new LinkedHashMap<>(ANNOTATIONS_INITCAP);
		InlineAnnotation annotation;
		final String[] tmpEntries = data.split(SEP_CODEANNOTATIONVALUE, -1);
		for (final String tmp : tmpEntries) {
			if ( tmp.length() == 0 ) continue;
			final String[] tmpPair = tmp.split(SEP_CODEANNOTATIONKEY, -1);
			if ( tmpPair.length > 1 ) {
				final boolean isGA = GenericAnnotationType.GENERIC.equals(tmpPair[0]);
				annotation = isGA ? new GenericAnnotations() : new InlineAnnotation();
				annotation.fromString(tmpPair[1]);
			}
			else annotation = null;
			map.put(tmpPair[0], annotation);
		}
		return map;
	}

	public Code() {
		this(/*type*/ null);
	}

	/**
	 * Creates a new code. By default codes can be both deleted and cloned.
	 * @param tagType the tag type.
	 * @param type the type of code (e.g. the name of the tag). The type must be
	 * exactly the same between the opening and closing codes.
	 * @param data the content of the code.
	 */
	public Code(final TagType tagType, final String type, final String data)
	{
		id = -1;
		this.tagType = tagType;
		flag = 0; // Default: not cloneable, not deleteable

		// Never let the type to be null
		this.type = type == null ? TYPE_NULL : type;

		// Using the setter to make sure the reference flag is set if needed
		setData(data);
	}

	/**
	 * Creates a new code with empty data.
	 * @param tagType the tag type.
	 * @param type the type of code (e.g. the name of the tag).
	 */
	public Code(final TagType tagType, final String type)
	{
		this(tagType, type, /*data*/ "");
	}

	/**
	 * Creates a new code with a null tag type and empty data. You must set the tag type later on.	 
	 * @param type the type of code (e.g. the name of the tag).
	 */
	public Code(final String type) {
		this(/*tagType*/ null, type, /*data*/ "");
	}

	/**
	 * Appends data to the current code data
	 * @param data the data to append.
	 */
	public void append(final String data) {
		if ( data != null ) {
			this.data.append(data);
			if (data.contains(TextFragment.REFMARKER_START)) {
				setReferenceFlag(true);
			}
		}
	}

	/**
	 * Appends data to the current code outerData
	 * @param outerData the outer data to append.
	 */
	public void appendOuterData(final String outerData) {
		if ( outerData == null ) return;
		// Else: append the value
		if ( this.outerData == null ) {
			this.outerData = new StringBuilder(DATA_DEFAULT_SIZE);
		}
		this.outerData.append(outerData);
	}

	/**
	 * Appends a reference marker to the current code data.
	 * @param id the identifier of the referent resource.
	 */
	public void appendReference(final String id) {
		data.append(TextFragment.makeRefMarker(id));
		setReferenceFlag(true);
	}

	/**
	 * Appends a reference marker for a given property to the current code data.
	 * @param id the identifier of the referent resource where the property is located.
	 * @param propertyName the name of the property.
	 */
	public void appendReference(final String id, final String propertyName)
	{
		data.append(TextFragment.makeRefMarker(id, propertyName));
		setReferenceFlag(true);
	}

	/**
	 * Gets the string representation of this code: its data.
	 * @return the raw data of this code.
	 */
	@Override
	public String toString () {
		return data.toString();
	}

	/**
	 * Indicates if this code has any type of annotation.
	 * 
	 * @return true when this code has any type of annotation.
	 */
	public boolean hasAnnotation () {
		return ((inlineAnnotations != null) && (inlineAnnotations.size() > 0));
	}

	/**
	 * Indicates if this code has a given type of annotation.
	 * 
	 * @param type the type of annotation.
	 * @return true if the code has an annotation of the requested type.
	 */
	public boolean hasAnnotation(final String type) {
		if (inlineAnnotations == null)
			return false;
		return inlineAnnotations.containsKey(type);
	}

	/**
	 * Indicates if the code is only a marker for an annotation.
	 * @return true if the code has an annotation (not null) and its type is set to Code.TYPE_ANNOTATION_ONLY.
	 * False if there is no annotation (even with a type set to Code.TYPE_ANNOTATION_ONLY), or if the type is not Code.TYPE_ANNOTATION_ONLY.
	 */
	public boolean hasOnlyAnnotation () {
		if (inlineAnnotations == null)
			return false;
		return type.equals(Code.TYPE_ANNOTATION_ONLY);
	}

	/**
	 * Indicates if this code has data (i.e. data is empty, as data is never null)
	 * @return true if this code has data.
	 */
	public boolean hasData () {
		// Data is never null
		return ( data.length() > 0 );
	}

	/**
	 * Indicates if this code has an original id (non-numeric string)
	 * @return true if this code has an original id
	 */
	public boolean hasOriginalId () {
		return ( !Util.isEmpty(originalId) );
	}

	/**
	 * Indicates if this code has outer data (i.e. outerData is not null and not empty)
	 * @return true if this code has outer data.
	 */
	public boolean hasOuterData () {
		return ( !Util.isEmpty(outerData) );
	}

	/**
	 * Clone the code. Note that this method does not check if this code can be
	 * duplicated or not. Use {@link #isCloneable()} to check.
	 * @return a copy of the object.
	 */
	@Override
	public Code clone () {
		final Code newCode = new Code(tagType, type, data == null ? "" : data.toString());
		newCode.id = id;
		newCode.originalId = originalId;
		newCode.outerData = ((outerData == null) ? null : new StringBuilder(outerData));
		newCode.displayText = displayText;
		newCode.flag = flag;
		newCode.mergedData = mergedData;
		// Clone the inline annotations
		if (inlineAnnotations != null) {
			InlineAnnotation annot;
			newCode.inlineAnnotations = new LinkedHashMap<>();
			for (final String type : inlineAnnotations.keySet()) {
				annot = inlineAnnotations.get(type);
				if (annot == null)
					newCode.inlineAnnotations.put(type, null);
				else
					newCode.inlineAnnotations.put(type, annot.clone());
			}
		}

		IWithProperties.copy(this, newCode);
		return newCode;
	}

	/**
	 * Gets the tag type of the code.
	 * @return the tag type of the code.
	 */
	public TagType getTagType () {
		return tagType;
	}

	/**
	 * Sets the tag type for the code.
	 * @param value the new tag type to apply. The value must be one of the
	 * values of {@link TagType}.
	 */
	public void setTagType(final TagType value) {
		tagType = value;
	}

	/**
	 * Gets the abstract type for the code. For example: "bold".
	 * @return the abstract type of the code. This value should never be null (it is set to {@link #TYPE_NULL} if not otherwise set).
	 */
	public String getType () {
		return type;
	}

	/**
	 * Sets the abstract type of the code. This member is used to match up
	 * together opening and closing codes.
	 * @param value the new abstract type of the code. Null is mapped to {@link #TYPE_NULL}.
	 */
	public void setType(final String value) {
		if ( value == null ) type = TYPE_NULL;
		else type = value;
	}

	/**
	 * Gets the raw data for the code. This does not build a string
	 * with sub-flows content.
	 * @return the raw data of the code.
	 */
	public String getData () {
		return data.toString();
	}

	/**
	 * Sets the raw data for the code.
	 * @param value the new raw data of the code.
	 */
	public void setData(final String value) {
		if (data == null) {
			data = new StringBuilder(DATA_DEFAULT_SIZE);
		} else {
			data.setLength(0);
		}
		setReferenceFlag(false); // clear the reference, the new data might not have a reference
		append(value); // this will set the reference flag if then new data has a reference
	}

	public String getMergedData() {
		return mergedData;
	}

	public void setMergedData(final String mergedData) {
		if (mergedData != null) {
			setMerged(true);
		}
		this.mergedData = mergedData;
	}

	/**
	 * Id for this code.*
	 */
public int getId() {
		return id;
	}

	/**
	 * Sets the identifier of the code. Be aware that identifiers for in-line codes
	 * are generated automatically when used in {@link TextFragment} and overriding
	 * the values may result in codes with duplicate IDs.
	 * 
	 * @param index the new index to be applied.
	 */
	public void setId(final int index) {
		id = index;
	}

	/**
	 * The original id is from the filtered format
	 */
	public String getOriginalId() {
		return originalId;
	}

	/**
	 * The original id is from the filtered format
	 */
	public void setOriginalId(final String originalId) {
		this.originalId = originalId;
	}

	/**
	 * Indicates whether the code has at least one sub-flow part. A sub-flow is a text
	 * unit contained within the code. For example: the text of the ALT attribute in the
	 * HTML IMG element: If the IMG tag is a code, the value of ALT is one of its
	 * sub-flows.
	 * @return true if the code has at least one sub-flow.
	 */
	public boolean hasReference () {
		return ((flag & HASREF) == HASREF);
	}

	/**
	 * Sets the flag that indicates if this code has a reference (sub-flow) or not. 
	 * @param value the new value to apply.
	 */
	public void setReferenceFlag(final boolean value) {
		if ( value ) flag |= HASREF;
		else flag &= ~HASREF;
	}

	/**
	 * Sets the complete data for this in-line code (inner data and outer).
	 * Outer data is used for format that implements in-line codes like TMX or XLIFF.
	 * For example "&lt;ph id='1'&gt;code&lt;/ph&gt;" is the outer data, and "code" in the
	 * inner data.
	 * @param value the data to set (can be null).
	 */
	public void setOuterData(final String value) {
		if ( value == null ) {
			outerData = null;
			return;
		}
		// Else: the value needs to be set
		if (outerData == null) {
			outerData = new StringBuilder(DATA_DEFAULT_SIZE);
		} else {
			outerData.setLength(0);
		}
		outerData.append(value);
	}

	/**
	 * Gets the outer data for this in-line code. If there is no outer data,
	 * the inner data is returned (same as {@link #getData()}).
	 * <p>Use {@link #hasOuterData()} to know if there is true outer data.
	 * @return the outer data or, if there is none, the inner data.
	 */
	public String getOuterData () {
		if ( outerData != null ) return outerData.toString();
		else return data.toString(); // Returns data if no outer-data is set
	}

	/**
	 * Indicates if this in-line code can be duplicated in the text.
	 * For example a HTML bold element could be duplicated, while a <code>%s</code> should not.
	 * @return true if this in-line code can be duplicated.
	 */
	public boolean isCloneable () {
		return ((flag & CLONEABLE) == CLONEABLE);
	}

	/**
	 * Sets the flag of this in-line code to indicate if it can be duplicated or not.
	 * @param value true to allow duplication, false to forbid it.
	 */
	public void setCloneable(final boolean value) {
		if ( value ) flag |= CLONEABLE;
		else flag &= ~CLONEABLE;
	}

	/**
	 * Indicates if this in-line code can be removed from the text.
	 * For example a HTML bold element could be removed, while a <code>%s</code> should not.
	 * @return true if this in-line code can be removed.
	 */
	public boolean isDeleteable () {
		return ((flag & DELETEABLE) == DELETEABLE);
	}

	/**
	 * Sets the flag of this in-line code to indicate if it can be removed or not.
	 * @param value true to allow deletion, false to forbid it.
	 */
	public void setDeleteable(final boolean value) {
		if ( value ) flag |= DELETEABLE;
		else flag &= ~DELETEABLE;
	}

	/**
	 * Indicates if this in-line code was added to the text (not found in the source).
	 * For example a HTML bold element added after extraction.
	 * @return true if this in-line code can be added.
	 */
	public boolean isAdded() {
		return ((flag & ADDED) == ADDED);
	}

	/**
	 * Sets the flag of this in-line code to indicate if the code was added (not found in the source).
	 * @param value true to allow adding, false to forbid it.
	 */
	public void setAdded(final boolean value) {
		if ( value ) flag |= ADDED;
		else flag &= ~ADDED;
	}

	/**
	 * Indicates if this in-line code was merged with another code.
	 * @return true if this in-line code was merged.
	 */
	public boolean isMerged() {
		return ((flag & MERGED) == MERGED);
	}

	/**
	 * Sets the flag of this in-line code to indicate if the code was merged.
	 * @param value true if merged, false otherwise.
	 */
	public void setMerged(final boolean value) {
		if ( value ) flag |= MERGED;
		else flag &= ~MERGED;
	}

	/**
	 * Indicates if this in-line code is masking an Okapi marker.
	 * @return true if this in-line code is masking an Okapi marker.
	 */
	public boolean isMarkerMasking() {
		return ((flag & MARKER_MASKING) == MARKER_MASKING);
	}

	/**
	 * Sets the flag of this in-line code to indicate if the code is masking an Okapi marker.
	 * @param value true if masking an Okapi marker, false otherwise.
	 */
	public void setMarkerMasking(final boolean value) {
		if ( value ) flag |= MARKER_MASKING;
		else flag &= ~MARKER_MASKING;
	}

	/**
	 * Sets the annotation for this code.
	 * 
	 * @param type       type of the annotation to set.
	 * @param annotation the annotation to set. This parameter can be null for
	 *                   example when the annotation is used like a boolean flag.
	 */
	public void setAnnotation(final String type, final InlineAnnotation annotation)
	{
		// Use a small initial capacity to save space
		if (inlineAnnotations == null) {
			inlineAnnotations = new LinkedHashMap<>(ANNOTATIONS_INITCAP);
		}
		inlineAnnotations.put(type, annotation);
	}

	/**
	 * Gets the annotation of a given type.
	 * 
	 * @param type the type of annotation to retrieve.
	 * @return the annotation of the given type, or null if there is no such
	 *         annotation for this code.
	 */
	public InlineAnnotation getAnnotation(final String type) {
		if (inlineAnnotations == null)
			return null;
		return inlineAnnotations.get(type);
	}

	/**
	 * Gets all the types of annotations for this code.
	 * 
	 * @return a set of all the types of annotations for this code, or null if there
	 *         are no annotations.
	 */
	public Set<String> getAnnotationsTypes () {
		if (inlineAnnotations == null)
			return Collections.emptySet();
		return inlineAnnotations.keySet();
	}

	/**
	 * Removes all annotations from this code.
	 *
	 */
	public void removeAnnotations () {
		if (inlineAnnotations != null) {
			inlineAnnotations.clear();
			inlineAnnotations = null;
		}
		//TODO: update closing code if needed (when they'll be linked)
	}

	/**
	 * Removes the annotation of a given type in this code.
	 * 
	 * @param type the type of annotation to remove.
	 */
	public void removeAnnotation(final String type) {
		if (inlineAnnotations != null) {
			inlineAnnotations.remove(type);
		}
		//TODO: update closing code if needed (when they'll be linked)
	}

	/**
	 * Gets the generic annotation set assigned to the code if any.
	 * 
	 * @return the annotation set, or null if there is none.
	 */
	public GenericAnnotations getGenericAnnotations () {
		return (GenericAnnotations)getAnnotation(GenericAnnotationType.GENERIC);
	}

	/**
	 * Gets the value of a string field for the first generic annotation of a given type
	 * in the annotation set associated with this code.
	 * If several annotations of the given type exist, this method return only the value for 
	 * the given field for the first one.
	 * @param type the type of generic annotation to lookup
	 * @param fieldName the name of the field to retrieve.
	 * @return the string value of the field 
	 * or null if there is no such field, annotation, or annotation set.
	 */
	public String getGenericAnnotationString(final String type, final String fieldName)
	{
		final GenericAnnotations anns = getGenericAnnotations();
		if ( anns == null ) return null;
		final GenericAnnotation ann = anns.getFirstAnnotation(type);
		if ( ann == null ) return null;
		return ann.getString(fieldName);
	}

	/**
	 * Set the display equivalent text for this code. This is intended to be
	 * an explanatory hint displayed to the user.
	 * @param text The display text for this code
	 */
	public void setDisplayText(final String text) {
		displayText = text;
	}

	/**
	 * Get the display equivalent text for this code.
	 * @return The display text for this code, or null if none exists
	 */
	public String getDisplayText() {
		return displayText;
	}

	public int getFlag() {
		return flag;
	}

	public void setFlag(final int flag) {
		this.flag = flag;
	}

	@Override
	public Map<String, Property> getProperties() {
		if (properties == null) {
			properties = new HashMap<>();
		}
		return properties;
	}
}
