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

package net.sf.okapi.filters.openxml;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.sf.okapi.filters.openxml.RunProperties.copiedRunProperties;

/**
 * Provides powerpoint style definitions.
 *
 * @author ccudennec
 * @since 06.09.2017
 */
final class PowerpointStyleDefinitions implements StyleDefinitions {
    static final String DEFAULT_TEXT_STYLE = "defaultTextStyle";
    static final String LST_STYLE = "lstStyle";
    static final String BODY_STYLE = "bodyStyle";
    static final String OTHER_STYLE = "otherStyle";
    static final String TITLE_STYLE = "titleStyle";
    static final String NOTES_STYLE = "notesStyle";
    static final Set<String> NAMES = new HashSet<>(
        Arrays.asList(
            DEFAULT_TEXT_STYLE,
            LST_STYLE,
            BODY_STYLE,
            OTHER_STYLE,
            TITLE_STYLE,
            NOTES_STYLE
        )
    );

    private final XMLEventFactory eventFactory;

    private StartElement startElement;
    private PowerpointStyleDefinition paragraphDefaults;
    private Map<String, PowerpointStyleDefinition> stylesByStyleIds;
    private EndElement endElement;

    PowerpointStyleDefinitions(final XMLEventFactory eventFactory) {
        this.eventFactory = eventFactory;
    }

    @Override
    public void readWith(final StyleDefinitionsReader reader) throws XMLStreamException {
        this.stylesByStyleIds = new LinkedHashMap<>();
        final PowerpointStyleDefinitionsReader styleDefinitionsReader =
            (PowerpointStyleDefinitionsReader) reader;
        this.startElement = styleDefinitionsReader.startElement();
        this.paragraphDefaults = styleDefinitionsReader.paragraphDefaults();
        while (styleDefinitionsReader.nextParagraphLevelAvailable()) {
            place(styleDefinitionsReader.nextParagraphLevel());
        }
        this.endElement = styleDefinitionsReader.endElement();
    }

    private void place(final PowerpointStyleDefinition styleDefinition) {
        this.stylesByStyleIds.put(styleDefinition.id(), styleDefinition);
    }

    @Override
    public void place(final String parentId, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties) {
    }

    @Override
    public String placedId() {
        return null;
    }

    @Override
    public ParagraphBlockProperties combinedParagraphBlockProperties(final ParagraphBlockProperties paragraphBlockProperties) {
        return (this.stylesByStyleIds.containsKey(paragraphBlockProperties.paragraphStyle())
            ? this.stylesByStyleIds.get(paragraphBlockProperties.paragraphStyle()).paragraphProperties()
            : this.paragraphDefaults.paragraphProperties()
        ).mergedWith(paragraphBlockProperties);
    }

    /**
     * Gets the combined run properties by applying the styles in the following order:
     *
     * <ul>
     * <li>paragraph level styles from master</li>
     * <li>direct styles {@link RunProperties}</li>
     * </ul>
     *
     * @param paragraphLevelId The paragraph level ID
     * @param runStyle         <i>Unused</i>
     * @param runProperties    The run properties
     *
     * @return The combined run properties
     */
    @Override
    public RunProperties combinedRunProperties(String paragraphLevelId, String runStyle, RunProperties runProperties) {

        RunProperties combinedRunProperties = copiedRunProperties(getParagraphLevelProperties(paragraphLevelId));

        combinedRunProperties = combinedRunProperties.combineDistinct(copiedRunProperties(runProperties), TraversalStage.DIRECT);

        return combinedRunProperties;
    }

    private RunProperties getParagraphLevelProperties(String paragraphLevelId) {
        final String idAsString = paragraphLevel(paragraphLevelId);
        return this.stylesByStyleIds.containsKey(idAsString)
            ? this.stylesByStyleIds.get(idAsString).runProperties()
            : this.paragraphDefaults.runProperties();
    }

    private String paragraphLevel(final String paragraphLevelId) {
        final int id = null == paragraphLevelId
            ? 1
            : Integer.valueOf(paragraphLevelId) + 1;
        return String.valueOf(id);
    }

    @Override
    public StyleDefinitions mergedWith(final StyleDefinitions other) {
        if (other instanceof StyleDefinitions.Empty) {
            return this;
        }
        if (!(other instanceof PowerpointStyleDefinitions)) {
            throw new IllegalArgumentException("The provided argument is illegal: ".concat(other.getClass().getSimpleName()));
        }
        final PowerpointStyleDefinitions that = (PowerpointStyleDefinitions) other;
        final PowerpointStyleDefinitions merged = new PowerpointStyleDefinitions(
            this.eventFactory
        );
        merged.startElement = this.startElement;
        merged.paragraphDefaults = this.paragraphDefaults.mergedWith(that.paragraphDefaults);
        merged.stylesByStyleIds = new LinkedHashMap<>(this.stylesByStyleIds.size());
        final Map<String, PowerpointStyleDefinition> common = this.stylesByStyleIds.entrySet().stream()
            .filter(e -> that.stylesByStyleIds.containsKey(e.getKey()))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        this.stylesByStyleIds.entrySet().stream()
            .filter(e -> !common.containsKey(e.getKey()))
            .forEach(e -> merged.stylesByStyleIds.put(e.getKey(), e.getValue()));
        that.stylesByStyleIds.entrySet().stream()
            .filter(e -> !common.containsKey(e.getKey()))
            .forEach(e -> merged.stylesByStyleIds.put(e.getKey(), e.getValue()));
        common.forEach(
            (key, value) -> merged.stylesByStyleIds.put(
                key,
                this.stylesByStyleIds.get(key).mergedWith(that.stylesByStyleIds.get(key))
            )
        );
        merged.endElement = this.endElement;
        return merged;
    }

    @Override
    public Markup toMarkup() {
        final Markup markup = new Markup.General(new LinkedList<>());
        markup.addComponent(new MarkupComponent.Start(this.eventFactory, this.startElement));
        markup.addMarkup(this.paragraphDefaults.toMarkup());
        for (final Map.Entry<String, PowerpointStyleDefinition> entry : this.stylesByStyleIds.entrySet()) {
            markup.addMarkup(entry.getValue().toMarkup());
        }
        markup.addComponent(new MarkupComponent.End(this.endElement));
        return markup;
    }
}
