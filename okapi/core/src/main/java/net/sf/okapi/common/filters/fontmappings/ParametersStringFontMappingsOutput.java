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

import java.util.Iterator;

public final class ParametersStringFontMappingsOutput implements FontMappings.Output<ParametersString> {
    @Override
    public ParametersString writtenWith(final Iterator<FontMapping> fontMappingsIterator) {
        final ParametersString ps = new ParametersString();
        int number = 0;
        while (fontMappingsIterator.hasNext()) {
            ps.setGroup(
                String.valueOf(number),
                fontMappingsIterator.next().writtenTo(new ParametersStringFontMappingOutput())
            );
            number++;
        }
        if (0 != number) {
            ps.setInteger(ParametersStringFontMappingsInput.NUMBER, number);
        }
        final ParametersString root = new ParametersString();
        root.setGroup(ParametersStringFontMappingsInput.NAME, ps);
        return root;
    }
}
