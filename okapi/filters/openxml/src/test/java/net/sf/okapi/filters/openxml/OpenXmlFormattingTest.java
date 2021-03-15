/*
 * =============================================================================
 *   Copyright (C) 2010-2019 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static net.sf.okapi.filters.openxml.OpenXMLTestHelpers.textUnitSourceExtractor;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class OpenXmlFormattingTest {

    private final LocaleId locENUS = LocaleId.fromString("en-us");
    private final FileLocation root = FileLocation.fromClass(getClass());
    private final XMLFactories factories = new XMLFactoriesForTest();

    @Test
    public void extractsItalics() {
        ConditionalParameters params = new ConditionalParameters();
        params.setTranslateDocProperties(false);
        OpenXMLFilter filter = new OpenXMLFilter();

        RawDocument doc = new RawDocument(root.in("/formatting/italics2.docx").asUri(), StandardCharsets.UTF_8.name(), locENUS);
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, doc, params);
        List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
        assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactly(
            "This text has italics.<run1> This text is not in italics.</run1>"
        );

        // The first sentence should have italics; the second should not
        assertThat(
            textUnits.get(0).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(2).extracting("type").containsExactly(
            "x-color:000000;highlight:auto;",
            "x-color:000000;highlight:auto;"
        );
    }

    @Test
    public void extractsCaps() {
        final RawDocument doc = new RawDocument(root.in("/formatting/784.docx").asUri(), StandardCharsets.UTF_8.name(), locENUS);
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(
            FilterTestDriver.getEvents(new OpenXMLFilter(), doc, new ConditionalParameters())
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactly(
            "<run1>CAPS property</run1> and UPPERCASE CHARACTERS.",
            "User"
        );
        assertThat(
                textUnits.get(0).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(2).extracting("type").containsExactly(
            "x-caps;",
            "x-caps;"
        );
    }

    @Test
    public void extractsHighlightAndShade() {
        final RawDocument doc = new RawDocument(root.in("/formatting/790.docx").asUri(), StandardCharsets.UTF_8.name(), locENUS);
        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(
                FilterTestDriver.getEvents(new OpenXMLFilter(), doc, new ConditionalParameters())
        );
        assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactly(
            "Highlight variations: <run1>one</run1>, <run2>two</run2> and three.",
            "Shade variations: <run1>one</run1>, <run2>two</run2> and <run3>three</run3>.",
            "A shaded paragraph with <run1>highlighted characters</run1>.",
            "Shaded and highlighted.",
            "User"
        );
        assertThat(
            textUnits.get(0).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(4).extracting("type").containsExactly(
            "x-highlight:darkGray;",
            "x-highlight:darkGray;",
            "x-highlight:lightGray;",
            "x-highlight:lightGray;"
        );
        assertThat(
            textUnits.get(1).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(6).extracting("type").containsExactly(
            "x-highlight:7F7F7F;",
            "x-highlight:7F7F7F;",
            "x-highlight:BFBFBF;",
            "x-highlight:BFBFBF;",
            "x-highlight:FFFFFF;",
            "x-highlight:FFFFFF;"
        );
        assertThat(
            textUnits.get(2).getSource().getParts().get(0).getContent().getCodes()
        ).hasSize(2).extracting("type").containsExactly(
            "x-highlight:darkGray;",
            "x-highlight:darkGray;"
        );
    }

    @Test
    public void optimisesStyles() throws XMLStreamException {
        final ConditionalParameters conditionalParameters = new ConditionalParameters();
        conditionalParameters.setTranslatePowerpointMasters(false);

        final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(
            FilterTestDriver.getEvents(
                new OpenXMLFilter(),
                new RawDocument(
                    root.in("/formatting/803-defrprs-and-rprs.pptx").asUri(),
                    StandardCharsets.UTF_8.name(),
                    locENUS
                ),
                conditionalParameters
            )
        );

        final List<Property> blockProperties = ((Block.Markup)((BlockSkeleton) textUnits.get(0).getSkeleton()).block().getChunks().get(0)).paragraphBlockProperties().properties();
        final Property blockProperty = findByName(blockProperties, Namespaces.DrawingML.getQName("defRPr", Namespace.PREFIX_A));
        Assertions.assertThat(blockProperty).isNotNull();

        final BlockProperties defaultRunProperties = blockPropertiesFrom(blockProperty, conditionalParameters);
        Assertions.assertThat(
            defaultRunProperties.properties()
        ).hasSize(4).extracting(Property::getName).containsExactly(
            Namespaces.DrawingML.getQName("solidFill", Namespace.PREFIX_A),
            Namespaces.DrawingML.getQName("latin", Namespace.PREFIX_A),
            Namespaces.DrawingML.getQName("ea", Namespace.PREFIX_A),
            Namespaces.DrawingML.getQName("cs", Namespace.PREFIX_A)
        );
    }

    private Property findByName(final List<Property> blockProperties, final QName name) {
        for (final Property blockProperty : blockProperties) {
            if (blockProperty.getName().equals(name)) {
                return blockProperty;
            }
        }
        return null;
    }

    private BlockProperties blockPropertiesFrom(
        final Property blockProperty,
        final ConditionalParameters conditionalParameters
    ) throws XMLStreamException {
        final XMLEventReader eventReader = new XMLEventsReader(blockProperty.getEvents());
        final StartElement startElement = eventReader.nextEvent().asStartElement();
        final StartElementContext startElementContext = StartElementContextFactory.createStartElementContext(
            startElement,
            null,
            eventReader,
            factories.getEventFactory(),
            conditionalParameters,
            null
        );

        return new MarkupComponentParser().parseBlockProperties(
            startElementContext,
            new StrippableAttributes.DrawingRunProperties(
                conditionalParameters,
                this.factories.getEventFactory()
            ),
            new SkippableElements.RevisionProperty(
                new SkippableElements.Property(
                    new SkippableElements.Default(
                        SkippableElement.RunProperty.RUN_PROPERTY_RTL_DML,
                        SkippableElement.RunProperty.RUN_PROPERTY_LANGUAGE,
                        SkippableElement.RevisionProperty.RUN_PROPERTY_INSERTED_PARAGRAPH_MARK,
                        SkippableElement.RevisionProperty.RUN_PROPERTY_DELETED_PARAGRAPH_MARK,
                        SkippableElement.RevisionProperty.RUN_PROPERTY_MOVED_PARAGRAPH_TO,
                        SkippableElement.RevisionProperty.RUN_PROPERTY_MOVED_PARAGRAPH_FROM,
                        SkippableElement.RevisionProperty.PARAGRAPH_PROPERTIES_CHANGE,
                        SkippableElement.RevisionProperty.RUN_PROPERTIES_CHANGE
                    ),
                    startElementContext.getConditionalParameters()
                ),
                startElementContext.getConditionalParameters()
            )
        );
    }
}
