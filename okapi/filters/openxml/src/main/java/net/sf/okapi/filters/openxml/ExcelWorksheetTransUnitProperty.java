/*===========================================================================
  Copyright (C) 2016-2019 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enum of property keys
 */
public enum ExcelWorksheetTransUnitProperty
{
    CELL_REFERENCE("cellReference"),
    SHEET_NAME("sheetName");

    private String keyName;

    ExcelWorksheetTransUnitProperty(String cellReference)
    {
        this.keyName = cellReference;
    }

    public String getKeyName()
    {
        return keyName;
    }

    private static final Pattern CELL_REFERENCE_PATTERN = Pattern.compile("([A-Z]+)([0-9]+)");

    /**
     * Returns the Column Index part from the Cell Reference.
     *  'A1' -> A
     *  'Z1' -> Z
     * @param cellReference a cell reference.
     * @return the column reference.
     */
    public static String getColumnIndexFromCellRef(String cellReference) {
        Matcher matcher = CELL_REFERENCE_PATTERN.matcher(cellReference);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Cell Reference: " + cellReference);
        }
        return matcher.group(1);
    }

    /**
     * Returns the Row Number part from the Cell Reference.
     *  'A1' -> 1
     *  'B1' -> 2
     * @param cellReference a cell reference.
     * @return the column reference.
     */
    public static Integer getRowNumberFromCellRef(String cellReference) {
        Matcher matcher = CELL_REFERENCE_PATTERN.matcher(cellReference);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Cell Reference: " + cellReference);
        }

        return Integer.parseInt(matcher.group(2));
    }

    /**
     * Converts a column reference from ALPHA-26 number format to 0-based base 10.
     *  'A' -> 0
     *  'Z' -> 25
     * @return zero based column index
     */
    public static int getColumnIndexFromColumnRef(String columnReference) {
        int retval=0;
        char[] refs = columnReference.toUpperCase(Locale.ROOT).toCharArray();
        for (int k=0; k<refs.length; k++) {
            char thechar = refs[k];
            if (thechar == '$') {
                if (k != 0) {
                    throw new IllegalArgumentException("Invalid Column Reference: " + columnReference);
                }
                continue;
            }

            // Character is uppercase letter, find relative value to A
            retval = (retval * 26) + (thechar - 'A' + 1);
        }
        return retval-1;
    }
}
