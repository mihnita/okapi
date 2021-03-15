/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.filters.mif;

import org.slf4j.Logger;

/**
 * Provides a hexadecimal.
 */
final class Hexadecimal {
    static final String ILC_START = "\u169b"; // Rarely used character
    static final String ILC_END = "\u169c"; // Rarely used character

    private final int value;
    private final Logger logger;

    Hexadecimal(final int value, final Logger logger) {
        this.value = value;
        this.logger = logger;
    }

    /**
     * Converts to a corresponding Unicode value or inline code.
     *
     * Inline codes are bracketed with {@link #ILC_START} and {@link #ILC_END} characters.
     *
     * @return A string
     */
    @Override
    public String toString() {
        switch (this.value) {
            case 0x04:
                return "\u00ad"; // Discretionary hyphen
            case 0x05:
                return "\u200d"; // No hyphen
            case 0x06:
                return ""; // we remove these
            case 0x08:
                return "\t"; // Tab
            case 0x09:
                return "\n"; // Forced return/line-break
            case 0x0a:
                // todo represent this as
                // <ParaLine 
                // > # end of ParaLine
                return ILC_START + "\\x0a " + ILC_END; // End of paragraph
            case 0x0b:
                // todo represent this as
                //  <ParaLine
                //  > # end of ParaLine
                // > # end of Para
                // <Para
                return ILC_START + "\\x0b " + ILC_END; // End of flow
            case 0x10:
                return "\u2007"; // Numeric space
            case 0x11:
                return "\u00a0"; // Non-breaking space
            case 0x12:
                return "\u2009"; // Thin space
            case 0x13:
                return "\u2002"; // En space
            case 0x14:
                return "\u2003"; // Em space
            case 0x15:
                return "\u2011"; // Non-breaking/hard hyphen
            default:
                logger.warn("Unknown hexadecimal '{}' will be extracted as an inline code.", this.value);
                return String.format("%s\\x%02x %s", ILC_START, this.value, ILC_END);
        }
    }
}
