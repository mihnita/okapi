/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.steps.tokenization;

import net.sf.okapi.common.Range;

/**
 * TokenType generated by the {@link RbbiTokenizer}
 */

public class Token {

    public int id;
    private final String value;
    private final String name;
    private final String description;
    private final Range range;

    public Token(int id, String value, String name, String description, int start, int end) {
        this.id = id;
        this.value = value;
        this.name = name;
        this.description =description;
        range = new Range(start, end);
    }

    public Range getRange() {
        return range;
    }

    public int getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("%-15s\t%d\t%-20s\t%4d, %4d", name, id, value, range.start, range.end);
    }
}