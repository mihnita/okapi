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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Represents a presentation slide.
 *
 * Knows about the start element only at the moment.
 */
class PresentationSlide {

    /**
     * An unexpected presentation slide structure error message.
     */
    private static final String UNEXPECTED_PRESENTATION_SLIDE_STRUCTURE = "Unexpected presentation slide structure";

    /**
     * Slide element name.
     */
    private static final String SLIDE = "sld";

    /**
     * Show attribute name.
     */
    private static final QName SHOW = new QName("show");

    /**
     * The start element.
     */
    private final StartElement startElement;

    /**
     * Constructs a presentation slide.
     *
     * @param startElement The start element
     */
    PresentationSlide(final StartElement startElement) {
        this.startElement = startElement;
    }

    /**
     * Creates a presentation slide from an XML event reader.
     *
     * @param xmlEventReader An XML event reader
     * @return The presentation slide
     * @throws XMLStreamException if anything other than space characters are encountered
     * at the beginning of
     */
    static PresentationSlide fromXMLEventReader(final XMLEventReader xmlEventReader) throws XMLStreamException {
        final XMLEvent event = xmlEventReader.nextTag();

        if (!event.isStartElement()) {
            throw new IllegalStateException(UNEXPECTED_PRESENTATION_SLIDE_STRUCTURE);
        }

        if (!event.asStartElement().getName().getLocalPart().equals(SLIDE)) {
            throw new IllegalStateException(UNEXPECTED_PRESENTATION_SLIDE_STRUCTURE);
        }

        return new PresentationSlide(event.asStartElement());
    }

    /**
     * Checks whether the presentation slide is hidden.
     *
     * @return {@code true} if the slide is hidden
     *         {@code false} - otherwise
     */
    boolean isHidden() {
        return !XMLEventHelpers.getBooleanAttributeValue(this.startElement, SHOW, XMLEventHelpers.DEFAULT_BOOLEAN_ATTRIBUTE_TRUE_VALUE);
    }
}
