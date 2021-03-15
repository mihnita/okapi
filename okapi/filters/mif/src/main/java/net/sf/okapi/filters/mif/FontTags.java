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
package net.sf.okapi.filters.mif;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class FontTags {
    private static final String FONT = "Font";
    private static final String F_TAG = "FTag";

    private final Set<String> tags;

    FontTags() {
        this(new LinkedHashSet<>());
    }

    FontTags(final Set<String> tags) {
        this.tags = tags;
    }

    void fromCatalog(final Statement statement) {
        this.tags.clear();
        this.tags.addAll(
            statement.statementsWith(FONT).stream()
                .map(s -> s.statementsWith(F_TAG))
                .flatMap(Collection::stream)
                .map(s -> s.firstTokenOf(Token.Type.LITERAL).toString())
                .collect(Collectors.toSet())
        );
    }

    Set<String> toInlineCodeFinderRules() {
        return this.tags.stream()
            .map(t -> "<".concat(Pattern.quote(t)).concat("\\>"))
            .collect(Collectors.toSet());
    }
}
