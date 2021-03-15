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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;

/**
 * Represents the ITS annotatorsRef value set.
 */
public class AnnotatorsRef {

	private Map<String, String> map = new LinkedHashMap<>();
	
	/**
	 * Creates an empty {@link AnnotatorsRef} object.
	 */
	public AnnotatorsRef () {
		// thing to do
	}

	/**
	 * Copy constructor.
	 * @param original the original object to copy.
	 */
	public AnnotatorsRef (AnnotatorsRef original) {
		if ( original == null ) return;
		map.putAll(original.map);
	}
	
	/**
	 * Reads the annotatorsRef value into this object.
	 * @param data the string value of the its:annotatorsRef attribute.
	 */
	public void read (String data) {
		String[] list = data.split(" ", 0);
		for ( String tmp : list ) {
			int n = tmp.indexOf('|');
			if ( n == -1 ) {
				throw new InvalidParameterException(String.format("Invalid annotatorsRef value '%s'", tmp));
			}
			String ident = tmp.substring(0, n);
			Util.checkValueList(DataCategories.LIST, ident, "annotatorsRef");
			String value = tmp.substring(n+1);
			try {
				new URI(value);
			}
			catch (URISyntaxException e) {
				throw new InvalidParameterException(String.format("Invalid IRI value '%s'", value));
			}
			map.put(ident, value);
		}
	}
	
	/**
	 * Sets the annotator reference of a given ITS item (if available).
	 * If the item is null, or if it has no annotator reference, nothing is set.
	 * @param item the item where the new annotator reference may be (can be null).
	 */
	public void set (IITSItem item) {
		if ( item.isGroup() ) return;
		String value = item.getAnnotatorRef();
		if ( value == null ) return;
		String dc = item.getDataCategoryName();
		try {
			new URI(value);
		}
		catch (URISyntaxException e) {
			throw new InvalidParameterException(String.format("Invalid IRI value '%s'", value));
		}
		map.put(dc, value);
	}

	/**
	 * Sets the annotator reference for a given data category.
	 * If the value is null, the entry is not set.
	 * @param dc the data category.
	 * @param value the value to set.
	 */
	public void set (String dc,
		String value)
	{
		if ( value == null ) return;
		map.put(dc, value);
	}

	/**
	 * Output the annotator-references of this set that are not in a given parent set.
	 * @param parentAR the previous {@link AnnotatorsRef} object in the context (can be null if there is none).
	 * @return the ITS annotatorsRef attribute (can be empty but is never null).
	 * @see #print()
	 */
	public String printDifferences (AnnotatorsRef parentAR) {
		if ( parentAR == null ) return print();
		// Else: output the difference between parent and this set
		StringBuilder sb = new StringBuilder();
		for ( String dc : map.keySet() ) {
			String value = map.get(dc);
			if ( value != null ) {
				String parentValue = parentAR.get(dc);
				if ( parentValue != null ) {
					if ( value.equals(parentValue) ) value = null; // No need to output
				}
			}
			if ( value != null ) {
				sb.append(" "+dc+"|"+value);
			}
		}
		if ( sb.length() == 0 ) return ""; // Nothing to output
		return " its:annotatorsRef=\""+sb.toString().trim()+"\"";
	}
	
	/**
	 * Compares a give annotator-reference value and parent's value for a given data category
	 * and output the value if it is difference form the parent's value
	 * or if the parent's value is null. If the value is null an empty string is output.
	 * @param dcIdentifier the data category identifier
	 * @param value the annotator-reference value (can be null)
	 * @param parentAR the annotator-reference parent (can be null)
	 * @return the given value as an annotator-reference or an empty string
	 */
	public static String printDCIfDifferent (String dcIdentifier,
		String value,
		AnnotatorsRef parentAR)
	{
		if ( value == null ) return "";
		if ( parentAR != null ) {
			String parentValue = parentAR.get(dcIdentifier);
			if ( parentValue.equals(value) ) return "";
		}
		// Else: two different values, or value not null but thisValue null
		// Output value
		return " its:annotatorsRef=\""+dcIdentifier+"|"+value+"\"";
	}

	/**
	 * Update an {@link AnnotatorsRef} object with the values of the live object.
	 * Values for data categories not in live object are left alone.
	 * @param ar the object to update.
	 * @param object the live object from where to get the new values.
	 */
	public static void update (AnnotatorsRef ar,
		IWithITSAttributes object)
	{
		if ( ar == null ) return;
		if ( !object.hasITSItem() ) return;
		for ( IITSItem item : object.getITSItems() ) {
			ar.set(item);
		}
	}
	
	/**
	 * Outputs the annotator-references in this set.
	 * @return the its:annotatorsRef attribute for this set (all values)
	 * @see #printDifferences(AnnotatorsRef)
	 */
	public String print () {
		StringBuilder sb = new StringBuilder();
		for ( String dc : map.keySet() ) {
			String value = map.get(dc);
			if ( value != null ) {
				sb.append(" "+dc+"|"+value);
			}
		}
		if ( sb.length() == 0 ) return ""; // Nothing to output
		return " its:annotatorsRef=\""+sb.toString().trim()+"\"";
	}

	/**
	 * Gets the annotator reference value for a given data category.
	 * @param name the name of the data category.
	 * @return the annotator reference for the given data category (can be null).
	 */
	public String get (String name) {
		return map.get(name);
	}

	/**
	 * Adds if needed the annotator reference value for the given data category.
	 * @param out the buffer where to add the value.
	 * @param dc the data category to lookup.
	 * @param value the value to match with.
	 */
	protected void buildDCValue (StringBuilder out, 
		String dc,
		String value)
	{
		if ( value == null ) return;
		String ccValue = map.get(dc);
		if ( !value.equals(ccValue) ) {
			out.append(" "+dc+"|"+value);
		}
	}

	/**
	 * Indicates if this annotators set is empty or nor.
	 * @return true if this annotators set is empty, false otherwise.
	 */
	public boolean isEmpty () {
		return map.isEmpty();
	}

}
