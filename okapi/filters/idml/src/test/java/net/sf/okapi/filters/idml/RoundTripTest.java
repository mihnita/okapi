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
package net.sf.okapi.filters.idml;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.FileLocation.In;
import net.sf.okapi.common.FileLocation.Out;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.ZipXMLFileCompare;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class RoundTripTest {
    private static final LocaleId DEFAULT_LOCALE = LocaleId.ENGLISH;
    private static final String DEFAULT_CHARSET = "UTF-8";
    private FileLocation root;

    @Before
    public void setUp() {
        root = FileLocation.fromClass(this.getClass());
    }

    @Test
    public void documentWithChainedFontMappings() {
        final Parameters parameters = new Parameters();
        parameters.fromString(
            "#v1\n" +
            "fontMappings.number.i=3\n" +
            "fontMappings.0.sourceFontPattern=Times.*\n" +
            "fontMappings.0.targetFont=Arial Unicode MS\n" +
            "fontMappings.1.targetLocalePattern=en\n" +
            "fontMappings.1.sourceFontPattern=The Sims Sans\n" +
            "fontMappings.1.targetFont=Arial Unicode MS\n" +
            "fontMappings.2.targetLocalePattern=en\n" +
            "fontMappings.2.sourceFontPattern=Arial Unicode MS\n" +
            "fontMappings.2.targetFont=Meiryo\n"
        );
        roundTripAndCheck(parameters, "926.idml", "926-chained.idml");
    }

    @Test
    public void documentsWithDefaultParameters() {
        roundTripAndCheck(new Parameters(), "926.idml", "926.idml");
    }

    @Test @Ignore
    public void issue175() {
        roundTripAndCheck(new Parameters(), "Issue_175.idml", "Issue_175.idml");
    }

    private void roundTripAndCheck(final Parameters parameters, final String documentName, final String goldDocumentName) {
        final In input = root.in("/" + documentName);
        final Out actualOutput = root.out("/actual/" + goldDocumentName);
        final In expectedOutput = root.in("/expected/" + goldDocumentName);
		try (final RawDocument rawDocument = new RawDocument(input.asUri(), DEFAULT_CHARSET, DEFAULT_LOCALE);
				final IDMLFilter filter = new IDMLFilter()) {
			filter.setParameters(parameters);
			filter.open(rawDocument, true);
			try (final IFilterWriter filterWriter = filter.createFilterWriter()) {
				filterWriter.setOutput(actualOutput.toString());
				filterWriter.setOptions(DEFAULT_LOCALE, DEFAULT_CHARSET);
				filter.forEachRemaining(filterWriter::handleEvent);
			}
		}
		final ZipXMLFileCompare zfc = new ZipXMLFileCompare();
		assertTrue(zfc.compareFiles(actualOutput.toString(), expectedOutput.toString()));
	}
}
