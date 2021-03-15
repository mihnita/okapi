/*
 * =============================================================================
 * Copyright (C) 2010-2020 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================================
 */
package net.sf.okapi.common.filters.fontmappings;

import net.sf.okapi.common.ParametersString;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class ParametersStringFontMappingsInput implements FontMappings.Input {
    static final String NAME = "fontMappings";
    static final String NUMBER = "number";
    private static final int DEFAULT_NUMBER_VALUE = 0;
    private static final String DEFAULT_STRING_VALUE = "";

    private final ParametersString parametersString;
    private List<FontMapping> fontMappings;

    public ParametersStringFontMappingsInput(final ParametersString parametersString) {
        this.parametersString = parametersString;
    }

    @Override
    public Iterator<FontMapping> read() {
        if (null != this.fontMappings) {
            return Collections.emptyIterator();
        }
        this.fontMappings = new LinkedList<>();
        final ParametersString ps = new ParametersString(
            this.parametersString.getGroup(
                NAME,
                DEFAULT_STRING_VALUE
            )
        );
        final int numberOfMappings = ps.getInteger(
            NUMBER,
            DEFAULT_NUMBER_VALUE
        );
        for (int i = 0; i < numberOfMappings; i++) {
            final String mappingString = ps.getGroup(
                String.valueOf(i),
                DEFAULT_STRING_VALUE
            );
            if (DEFAULT_STRING_VALUE.equals(mappingString)) {
                continue;
            }
            this.fontMappings.add(new ParametersStringFontMapping(new ParametersString(mappingString)));
        }
        return this.fontMappings.iterator();
    }
}
