/*===========================================================================
  Copyright (C) 2016-2018 by the Okapi Framework contributors
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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a color with its name and available codes.
 */
public enum Color {

    BLUE("blue", "FF0070C0", "0070C0"),
    LIGHT_BLUE("light blue", "FF00B0F0", "00B0F0"),
    GREEN("green", "FF00B050", "00B050"),
    PURPLE("purple", "FF7030A0", "7030A0"),
    RED("red", "FFFF0000", "FF0000"),
    YELLOW("yellow", "FFFFFF00", "FFFF00"),
    DARK_RED("dark red", "FFC00000", "C00000"),
    LIGHT_GREEN("light green", "FF92D050", "92D050"),
    ORANGE("orange", "FFFFC000", "FFC000"),
    DARK_BLUE("dark blue", "FF002060", "002060");

    /**
     * Internal mapping of the default colors.
     */
    private static final Map<String, Color> map = new HashMap<>();

    static {
        for (Color color : Color.values()) {
            map.put(color.getName(), color);
        }
    }

    private final String name;
    private final String excelColorCode;
    private final String wordColorCode;

    /**
     * Default constructor.
     *
     * @param name the name of the color
     * @param excelColorCode the Excel color code.
     * @param wordColorCode the Word color code.
     */
    Color(String name, String excelColorCode, String wordColorCode) {
        this.name = name;
        this.excelColorCode = excelColorCode;
        this.wordColorCode = wordColorCode;
    }

    /**
     * Get a color based on its name.
     *
     * @param name the name to find.
     * @return the <code>Colors</code> or null if not found.
     */
    public static Color fromName(String name) {
        return map.get(name);
    }

    /**
     * Get a color based on its color code (either Word or Excel).
     *
     * @param code the code to find.
     * @return the <code>Colors</code> or null if not found.
     */
    public static Color fromCode(String code) {
        for (Color color : map.values()) {
            if (color.getExcelColorCode().equals(code) || color.getWordColorCode().equals(code)) {
                return color;
            }
        }
        return null;
    }

    /**
     * Get the color's name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the color's Excel Color Code.
     *
     * @return the Excel color code.
     */
    public String getExcelColorCode() {
        return excelColorCode;
    }

    /**
     * Get the color's Word Color Code.
     *
     * @return the Excel color code.
     */
    public String getWordColorCode() {
        return wordColorCode;
    }

}
