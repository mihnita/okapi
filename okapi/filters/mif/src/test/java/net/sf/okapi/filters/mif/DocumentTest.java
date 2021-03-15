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

import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(JUnit4.class)
public class DocumentTest {

    private final FileLocation fileLocation;

    public DocumentTest() {
        this.fileLocation = FileLocation.fromClass(getClass());;
    }

    @Test
    public void iteratesThroughTheStatementsOfASample() {
        String snippet = "< # comment 0\r\n" +
            "# comment 1\r\n" +
            "MIFFile # comment 2\r\n" +
            "9.00 # comment 3\r\n" +
            ">#comment 4\r\n" +
            "define (Bold, <Font <FWeight `Bold'>>)\r\n" +
            "include (template.mif)\r\n" +
            "<TextFlow \r\n" +
            " <Notes \r\n" +
            " > # end of Notes\r\n" +
            " <Para \r\n" +
            "  <Unique 998389>\r\n" +
            "  <Pgf \r\n" +
            "   <PgfTag `Header'>\r\n" +
            "   <PgfPDFStructureLevel 0>\r\n" +
            "  > # end of Pgf\r\n" +
            "  <ParaLine \r\n" +
            "   <String `Paragraph 1.'>\r\n" +
            "  > # end of ParaLine\r\n" +
            " > # end of Para\r\n" +
            "> # end of TextFlow\r\n";
        try (final Reader reader = new StringReader(snippet)) {
            final List<Statement> statements = new LinkedList<>();
            final Document document = new Document.Default(new Statements(reader), reader, new LinkedList<>());
            while (document.hasNext()) {
                statements.add(document.next());
            }
            Assertions.assertThat(statements.size()).isEqualTo(11);
            Assertions.assertThat(statements.stream().map(s -> s.toString()).collect(Collectors.joining()))
                .isEqualTo(snippet);
        } catch (IOException e) {
            Assertions.fail("I/O error: ".concat(e.getMessage()));
        }
    }

    @Test
    public void iteratesThroughTheStatementsOfEveryResourceUnderTest() {
        final File dir = this.fileLocation.in("/").asFile();
        final String[] documentsNames = dir.list((d, n) -> n.endsWith(".mif"));
        Assertions.assertThat(documentsNames).isNotNull();
        Arrays.stream(documentsNames)
            .sorted()
            .forEach(this::iterateThroughStatementsAndCompare);
    }

    private void iterateThroughStatementsAndCompare(final String documentName) {
        final Charset charset = StandardCharsets.UTF_8;
        try (
            final Reader reader = new BufferedReader(
                new InputStreamReader(this.fileLocation.in("/".concat(documentName)).asInputStream(), charset)
            );
            final Writer writer = new BufferedWriter(
                new OutputStreamWriter(this.fileLocation.out("/read/".concat(documentName)).asOutputStream(), charset)
            )
        ) {
            final Document document = new Document.Default(new Statements(reader), reader, new LinkedList<>());
            while (document.hasNext()) {
                writer.write(document.next().toString());
            }
        } catch (Exception e) {
            Assertions.fail("Error at iterating through ".concat(documentName).concat(" : ").concat(e.getMessage()));
        }

        try (
            final InputStream origin = this.fileLocation.in("/".concat(documentName)).asInputStream();
            final InputStream read = this.fileLocation.in("/out/read/".concat(documentName)).asInputStream();
        ) {
            Assertions.assertThat(new FileCompare().filesExactlyTheSame(origin, read)).isTrue();
        } catch (IOException e) {
            Assertions.fail("I/O error at comparing read statements of ".concat(documentName).concat(" : ").concat(e.getMessage()));
        }
    }
}
