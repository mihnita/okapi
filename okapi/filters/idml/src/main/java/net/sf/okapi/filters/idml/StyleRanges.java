/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.sf.okapi.filters.idml.StyleRange.getDefaultCharacterStyleRange;
import static net.sf.okapi.filters.idml.StyleRange.getDefaultParagraphStyleRange;

class StyleRanges {

    private final StyleRange paragraphStyleRange;
    private final StyleRange characterStyleRange;

    StyleRanges(StyleRange paragraphStyleRange, StyleRange characterStyleRange) {
        this.paragraphStyleRange = paragraphStyleRange;
        this.characterStyleRange = characterStyleRange;
    }

    static StyleRanges defaultStyleRanges(XMLEventFactory eventFactory) {
        return new StyleRanges(getDefaultParagraphStyleRange(eventFactory), getDefaultCharacterStyleRange(eventFactory));
    }

    StyleRange getParagraphStyleRange() {
        return paragraphStyleRange;
    }

    StyleRange getCharacterStyleRange() {
        return characterStyleRange;
    }

    // TODO: rewrite when full styles hierarchy is available
    StyleRange getCombinedStyleRange() {

        StyleRange.StyleRangeBuilder styleRangeBuilder = new StyleRange.StyleRangeBuilder();

        List<Attribute> attributes = new ArrayList<>();

        for (Attribute attribute : paragraphStyleRange.getAttributes()) {
            if (StyleRange.APPLIED_PARAGRAPH_STYLE.equals(attribute.getName().getLocalPart())) {
                continue;
            }
            attributes.add(attribute);
        }

        for (Attribute attribute : characterStyleRange.getAttributes()) {
            if (StyleRange.APPLIED_CHARACTER_STYLE.equals(attribute.getName().getLocalPart())) {
                continue;
            }
            attributes.add(attribute);
        }

        styleRangeBuilder.setAttributes(attributes);

        List<Property> properties = new ArrayList<>(paragraphStyleRange.getProperties().properties());
        properties.addAll(characterStyleRange.getProperties().properties());

        Properties.Builder propertiesBuilder = new Properties.Builder();
        propertiesBuilder.setStartElement(paragraphStyleRange.getProperties().startElement())
                .setEndElement(paragraphStyleRange.getProperties().endElement())
                .addProperties(properties);

        return styleRangeBuilder.setProperties(propertiesBuilder.build()).build();
    }

    boolean isSubsetOf(StyleRanges other) {
        return getParagraphStyleRange().isSubsetOf(other.getParagraphStyleRange())
                && getCharacterStyleRange().isSubsetOf(other.getCharacterStyleRange());
    }

    int amount() {
        return paragraphStyleRange.getAttributes().size()
            + paragraphStyleRange.getProperties().properties().size()
            + characterStyleRange.getAttributes().size()
            + characterStyleRange.getProperties().properties().size();
    }

    void apply(final FontMappings fontMappings) {
        this.paragraphStyleRange.apply(fontMappings);
        this.characterStyleRange.apply(fontMappings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (null == o || getClass() != o.getClass()) return false;

        StyleRanges that = (StyleRanges) o;

        return Objects.equals(getParagraphStyleRange(), that.getParagraphStyleRange()) &&
                Objects.equals(getCharacterStyleRange(), that.getCharacterStyleRange());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParagraphStyleRange(), getCharacterStyleRange());
    }
}
