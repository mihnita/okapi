/*
 * =============================================================================
 *   Copyright (C) 2010-2020 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */
package net.sf.okapi.filters.mif;

import net.sf.okapi.common.FileLocation;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;

@RunWith(JUnit4.class)
public class ExtractsTest {
    private final Common common;
    private final FileLocation fileLocation;

    public ExtractsTest() {
        this.common = new Common(new Parameters());
        this.fileLocation = FileLocation.fromClass(getClass());
    }

    @Test
    public void gathersExtractsFromEveryResourceUnderTest() {
        final File dir = this.fileLocation.in("/").asFile();
        final String[] documentsNames = dir.list((d, n) -> n.endsWith(".mif"));
        Assertions.assertThat(documentsNames).isNotNull();
        Arrays.stream(documentsNames)
            .sorted()
            .forEach(this::gatherExtractsFrom);
    }

    private void gatherExtractsFrom(final String documentName) {
        final Charset charset = StandardCharsets.UTF_8;
        try (
            final Reader reader = new BufferedReader(
                new InputStreamReader(this.fileLocation.in("/".concat(documentName)).asInputStream(), charset)
            )
        ) {
            final Extracts extracts = new Extracts(
                this.common.parameters(),
                new FontTags()
            );
            extracts.from(new Document.Default(new Statements(reader), reader, new LinkedList<>()));
        } catch (Exception e) {
            Assertions.fail("Error at iterating through ".concat(documentName).concat(" : ").concat(e.getMessage()));
        }
    }
}
