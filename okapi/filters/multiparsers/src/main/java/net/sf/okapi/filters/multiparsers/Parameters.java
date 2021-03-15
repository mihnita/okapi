/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.multiparsers;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.CheckboxPart;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.SpinInputPart;
import net.sf.okapi.common.uidescription.TextInputPart;

@EditorFor(Parameters.class)
public class Parameters extends StringParameters implements IEditorDescriptionProvider {

	public static final String CSV_NOEXTRACTCOLS = "csvNoExtractCols";
	public static final String CSV_FORMATCOLS = "csvFormatCols";
	public static final String CSV_STARTINGROW = "csvStartingRow";
	public static final String CSV_AUTODETECTCOLUMNTYPES = "csvAutoDetectColumnTypes";
	public static final String CSV_AUTODETECTCOLUMNTYPESROW = "csvAutoDetectColumnTypesRow";

	public Parameters () {
		super();
	}
	
	@Override
	public void reset() {
		super.reset();
		setCsvStartingRow(1);
		setCsvAutoDetectColumnTypes(false);
		setCsvAutoDetectColumnTypesRow(2);
	}

	/**
	 * Gets the comma-separated list of the indexes of the columns not to extract (0-based index).
	 * You can have whitespace between values. Example: "2,4"
	 * @return the comma-separated list of the columns not to extract, or null when all columns are extractable.
	 */
	public String getCsvNoExtractCols () {
		return getString(CSV_NOEXTRACTCOLS);
	}

	public void setCsvNoExtractCols (String csvNoExtractCols) {
		setString(CSV_NOEXTRACTCOLS, csvNoExtractCols);
	}

	/**
	 * Gets the comma-separated list of the indexes of the columns to process with a non-plain-text filter (0-based index).
	 * The format to use for the given column is entered after the index and a separator ':'.
	 * You can have whitespace between values. Example: "1:okf_markdown, 5:okf_html"
	 * @return the comma-separated list of the columns to process with a given sub-filter, null when all columns are plain-text.
	 */
	public String getCsvFormatCols () {
		return getString(CSV_FORMATCOLS);
	}

	public void setCsvFormatCols (String csvFormatCols) {
		setString(CSV_FORMATCOLS, csvFormatCols);
	}
	
	/**
	 * Gets the row number where the extraction should start (1-based index).
	 * @return the row number where the extraction should start.
	 */
	public int getCsvStartingRow () {
		return getInteger(CSV_STARTINGROW);
	}

	public void setCsvStartingRow (int csvStartingRow) {
		setInteger(CSV_STARTINGROW, csvStartingRow);
	}
	
	/**
	 * Gets the detection mode used when determining each column's extraction type. 'False' to use values in the .fprm file, 'true' to auto-detect 
	 * the extraction types for each column using the designated row in the CSV. This auto-detect row might look like this: "notrans,text,okf_html,okf_markdown,text".
	 * @return boolean value which governs the auto-detection mode.
	 */
	public boolean getCsvAutoDetectColumnTypes () {
		return getBoolean(CSV_AUTODETECTCOLUMNTYPES);
	}
	
	public void setCsvAutoDetectColumnTypes (boolean csvAutoDetectColumnTypes) {
		setBoolean(CSV_AUTODETECTCOLUMNTYPES, csvAutoDetectColumnTypes);
	}
	
	/**
	 * Gets the row number to use when using 'auto-detection' to determine column types.
	 * The auto-detection row might look like this: "notrans,text,okf_html,okf_markdown,text".
	 * @return int value corresponding to the line/row number containing the "column types" data
	 */
	public int getCsvAutoDetectColumnTypesRow () {
		return getInteger(CSV_AUTODETECTCOLUMNTYPESROW);
	}
	
	public void setCsvAutoDetectColumnTypesRow (int csvAutoDetectColumnTypesRow) {
		setInteger(CSV_AUTODETECTCOLUMNTYPESROW, csvAutoDetectColumnTypesRow);
	}
	
	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(CSV_AUTODETECTCOLUMNTYPES, "Use auto-detection (a special row in each input file)", null);
		desc.add(CSV_AUTODETECTCOLUMNTYPESROW, "Row at which the special configuration information is (first=1)", null);
		desc.add(CSV_NOEXTRACTCOLS, "Comma-separated list of the colums not to extract (first=0)", null);
		desc.add(CSV_FORMATCOLS, "Comma-separated list of the colums with non-plain-text content (e.g. '3:okf_markdown', first=0)", null);
		desc.add(CSV_STARTINGROW, "Row at which the extraction starts (first=1)", null);
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription (ParametersDescription paramDesc) {
		EditorDescription desc = new EditorDescription("Multi-Parsers Filter Parameters", true, false);
		
		CheckboxPart cp = desc.addCheckboxPart(paramDesc.get(CSV_AUTODETECTCOLUMNTYPES));
		
		SpinInputPart sip = desc.addSpinInputPart(paramDesc.get(CSV_AUTODETECTCOLUMNTYPESROW));
		sip.setRange(1, 9999999);
		sip.setMasterPart(cp, true);

		TextInputPart tip = desc.addTextInputPart(paramDesc.get(CSV_NOEXTRACTCOLS));
		tip.setAllowEmpty(true);
		tip.setMasterPart(cp, false);
		
		tip = desc.addTextInputPart(paramDesc.get(CSV_FORMATCOLS));
		tip.setAllowEmpty(true);
		tip.setMasterPart(cp, false);
		
		sip = desc.addSpinInputPart(paramDesc.get(CSV_STARTINGROW));
		sip.setRange(1, 9999999);
		sip.setMasterPart(cp, false);
		
		return desc;
	}
}
