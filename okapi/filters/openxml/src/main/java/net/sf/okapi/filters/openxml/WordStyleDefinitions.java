/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.IdGenerator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides word style definitions.
 */
class WordStyleDefinitions implements StyleDefinitions {
    static final String STYLES = "styles";
    static final String DOC_DEFAULTS = "docDefaults";
    static final String LATENT_STYLES = "latentStyles";
    static final String STYLE = "style";

    private final ConditionalParameters conditionalParameters;
    private final XMLEventFactory eventFactory;

    private StartDocument startDocument;
    private StartElement startElement;
    private StyleDefinition documentDefaults;
    private StyleDefinition latentStyles;

    /**
     * Default styles by style types.
     */
    private Map<StyleType, String> defaultStylesByStyleTypes;

    /**
     * Style types by style IDs.
     */
    private Map<String, StyleType> styleTypesByStyleIds;

    /**
     * Parent styles by style IDs.
     */
    private Map<String, String> parentStylesByStyleIds;

    /**
     * Linked styles by style IDs.
     */
    private Map<String, String> linkedStylesByStyleIds;

    /**
     * Style definitions by style IDs.
     */
    private Map<String, WordStyleDefinition> stylesByStyleIds;

    private EndElement endElement;
    private EndDocument endDocument;
    private Ids ids;
    private String placedId;

    WordStyleDefinitions(
        final ConditionalParameters conditionalParameters,
        final XMLEventFactory eventFactory
    ) {
        this.conditionalParameters = conditionalParameters;
        this.eventFactory = eventFactory;
    }

    @Override
    public void readWith(final StyleDefinitionsReader reader) throws XMLStreamException {
        this.defaultStylesByStyleTypes = new EnumMap<>(StyleType.class);
        this.styleTypesByStyleIds = new HashMap<>();
        this.parentStylesByStyleIds = new HashMap<>();
        this.linkedStylesByStyleIds = new HashMap<>();
        this.stylesByStyleIds = new LinkedHashMap<>();
        this.ids = new Ids(
            new IdGenerator(WordStyleDefinitions.STYLE, WordStyleDefinitions.STYLE),
            this
        );
        final WordStyleDefinitionsReader styleDefinitionsReader =
            (WordStyleDefinitionsReader) reader;
        this.startDocument = styleDefinitionsReader.startDocument();
        this.startElement = styleDefinitionsReader.startElement();
        this.documentDefaults = styleDefinitionsReader.documentDefaults();
        this.latentStyles = styleDefinitionsReader.latent();
        while (styleDefinitionsReader.nextGeneralAvailable()) {
            place(styleDefinitionsReader.nextGeneral());
        }
        this.endElement = styleDefinitionsReader.endElement();
        this.endDocument = styleDefinitionsReader.endDocument();
    }

    private void place(final WordStyleDefinition styleDefinition) {
        if (styleDefinition.isDefault()) {
            this.defaultStylesByStyleTypes.put(styleDefinition.type(), styleDefinition.id());
        }
        this.styleTypesByStyleIds.put(styleDefinition.id(), styleDefinition.type());
        this.parentStylesByStyleIds.put(styleDefinition.id(), styleDefinition.parentId());
        this.linkedStylesByStyleIds.put(styleDefinition.id(), styleDefinition.linkedId());
        this.stylesByStyleIds.put(styleDefinition.id(), styleDefinition);
    }

    @Override
    public void place(final String parentId, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties) {
        this.placedId = this.ids.next(parentId, paragraphBlockProperties, runProperties);
        if (this.stylesByStyleIds.containsKey(this.placedId)) {
            // matched with the already available
            return;
        }
        final WordStyleDefinition styleDefinition = new WordStyleDefinition.General(
            this.conditionalParameters,
            this.eventFactory,
            startElementForGeneral(this.placedId)
        );
        styleDefinition.parentId(this.ids.parent());
        styleDefinition.paragraphProperties(
            new ParagraphBlockProperties.Word(
                new BlockProperties.Default(
                    this.eventFactory,
                    this.startElement.getName().getPrefix(),
                    this.startElement.getName().getNamespaceURI(),
                    ParagraphBlockProperties.PPR
                ),
                this.conditionalParameters,
                this.eventFactory,
                new StrippableAttributes.Default(this.eventFactory)
            )
        );
        styleDefinition.runProperties(runProperties);
        place(styleDefinition);
    }

    @Override
    public String placedId() {
        return this.placedId;
    }

    private StartElement startElementForGeneral(final String id) {
        final List<Attribute> attributes = Arrays.asList(
            this.eventFactory.createAttribute(
                new QName(
                    this.startElement.getName().getNamespaceURI(),
                    WordStyleDefinition.General.TYPE,
                    this.startElement.getName().getPrefix()
                ),
                StyleType.PARAGRAPH.toString()
            ),
            this.eventFactory.createAttribute(
                new QName(
                    this.startElement.getName().getNamespaceURI(),
                    WordStyleDefinition.General.STYLE_ID,
                    this.startElement.getName().getPrefix()
                ),
                id
            )
        );
        return this.eventFactory.createStartElement(
            new QName(
                this.startElement.getName().getNamespaceURI(),
                WordStyleDefinitions.STYLE,
                this.startElement.getName().getPrefix()
            ),
            attributes.iterator(),
            null
        );
    }

    @Override
    public ParagraphBlockProperties combinedParagraphBlockProperties(final ParagraphBlockProperties paragraphBlockProperties) {
        final String paragraphStyle = paragraphBlockProperties.paragraphStyle();
        final ParagraphBlockProperties paragraphProperties = paragraphBlockProperties.withoutParagraphStyle();
        if (null == this.stylesByStyleIds.get(paragraphStyle)) {
            final String defaultStyle = this.defaultStylesByStyleTypes.get(StyleType.PARAGRAPH);
            if (null == defaultStyle) {
                return this.documentDefaults.paragraphProperties().mergedWith(paragraphProperties);
            }
            return this.documentDefaults.paragraphProperties().mergedWith(
                combinedThroughoutParentStylesProperties(defaultStyle, paragraphProperties)
            );
        }
        return this.documentDefaults.paragraphProperties().mergedWith(
            combinedThroughoutParentStylesProperties(paragraphStyle, paragraphProperties)
        );
    }

    private ParagraphBlockProperties combinedThroughoutParentStylesProperties(
        final String styleId,
        final ParagraphBlockProperties paragraphProperties
    ) {
        if (null == this.stylesByStyleIds.get(styleId)) {
            return paragraphProperties;
        }
        if (null == this.parentStylesByStyleIds.get(styleId)
            || this.styleTypesByStyleIds.get(this.parentStylesByStyleIds.get(styleId)) != StyleType.PARAGRAPH) {
            // if there is no parent style
            // or the style types do not match
            return this.stylesByStyleIds.get(styleId).paragraphProperties().mergedWith(paragraphProperties);
        }
        return combinedThroughoutParentStylesProperties(
            this.parentStylesByStyleIds.get(styleId),
            this.stylesByStyleIds.get(styleId).paragraphProperties()
        ).mergedWith(paragraphProperties);
    }

    /**
     * Gets run properties combined through a semi-full style hierarchy.
     *
     * Here "semi-full" means that there is no table style involved.
     *
     * Firstly, the document defaults are applied to all runs and paragraphs in the document. Next, the table style
     * properties are applied to each table in the document, following the conditional formatting inclusions and
     * exclusions specified per table. Next, numbered item and paragraph properties are applied to each paragraph
     * formatted with a numbering style (we do not need this currently). Next, paragraph and run properties are applied
     * to each paragraph as defined by the paragraph style (pStyle). Next, run properties are applied to each run with a
     * specific character style applied (rStyle). Finally, we apply direct formatting (paragraph or run properties not
     * from styles).
     *
     * In oder to build up the resulting style, a consumer must trace the hierarchy (following each basedOn value) back
     * to a style which has no basedOn element (is not based on another style). The resulting style is then constructed
     * by following each level in the tree, applying the specified paragraph and/or character properties as appropriate.
     * When properties conflict, they are overridden by each subsequent level (this includes turning OFF a property set
     * at an earlier level). Properties which are not specified simply do not change those specified at earlier levels.
     *
     * As for the default attribute of the style element, it specifies that this style is the default for this style
     * type and is applied to objects of the same type that do not explicitly declare the style.
     *
     * In addition, the linked styles, which are groupings of paragraph and character styles, are merged into one if and
     * only if a paragraph and a character styles have been specified in the scope of one paragraph. If the current
     * style type is paragraph, then only all parent style run properties are combined. If the current style type is
     * character, then all linked style run properties are combined first (they are of paragraph type now) and then
     * merged with (overridden by) the current style run properties (which are also combined throughout the whole
     * hierarchy).
     *
     * And the last but not least, the toggle properties are combined in their own way. Firstly, if multiple instances
     * of a toggle property appear at the horizontal traversal stage (i.e paragraph or character) in the style hierarchy,
     * then the first closest to the root value encountered is used. Then, at the vertical traversal stage the already
     * gathered toggle properties are joined by XORing their values. After all that proceeded, at the document default
     * traversal stage, the document default toggle properties are joined by ORing their values. The absence of a toggle
     * property is corresponding to the "false" value. And finally, the directly specified toggle properties substitute
     * any other fond throughout the hierarchy.
     *
     * @param paragraphStyle A paragraph style
     * @param runStyle       A run style
     * @param runProperties  Run properties
     *
     * @return Run properties which are combined through the whole style hierarchy
     */
    @Override
    public RunProperties combinedRunProperties(String paragraphStyle, String runStyle, RunProperties runProperties) {
        // get all but strip toggle properties to apply them later
        RunProperties combinedRunProperties = RunProperties.copiedRunProperties(this.documentDefaults.runProperties(), false, false, true);

        combinedRunProperties = combinedRunProperties.combineDistinct(getParagraphStyleProperties(paragraphStyle), TraversalStage.VERTICAL);
        combinedRunProperties = combinedRunProperties.combineDistinct(getRunStyleProperties(runStyle, paragraphStyle), TraversalStage.VERTICAL);

        // apply previously stripped toggle properties
        combinedRunProperties = combinedRunProperties.combineDistinct(RunProperties.copiedToggleRunProperties(this.documentDefaults.runProperties()), TraversalStage.DOCUMENT_DEFAULT);

        // combine with the exclusion of the RunStyleProperty
        combinedRunProperties = combinedRunProperties.combineDistinct(RunProperties.copiedRunProperties(runProperties, false, true, false), TraversalStage.DIRECT);

        return combinedRunProperties;
    }

    private RunProperties getParagraphStyleProperties(String paragraphStyle) {
        return getPropertiesByTypeAndStyle(StyleType.PARAGRAPH, paragraphStyle, null);
    }

    private RunProperties getRunStyleProperties(String runStyle, String linkedStyle) {
        return getPropertiesByTypeAndStyle(StyleType.CHARACTER, runStyle, linkedStyle);
    }

    private RunProperties getPropertiesByTypeAndStyle(StyleType styleType, String styleId, String linkedStyleId) {
        if (null == styleId
                || !styleTypesByStyleIds.containsKey(styleId)
                || !styleTypesByStyleIds.get(styleId).equals(styleType)) {
            // if there is no style specified
            // or the style does not exist
            // or the style types do not match
            // return default properties for the specified style type

            String defaultStyleId = defaultStylesByStyleTypes.get(styleType);

            return combineParentStyleProperties(
                styleType,
                defaultStyleId,
                new RunProperties.Default(
                    this.eventFactory,
                    this.startElement.getName().getPrefix(),
                    this.startElement.getName().getNamespaceURI(),
                    RunProperties.RPR
                )
            );
        }

        return combineLinkedAndParentStyleProperties(
            styleType,
            styleId,
            linkedStyleId,
            new RunProperties.Default(
                this.eventFactory,
                this.startElement.getName().getPrefix(),
                this.startElement.getName().getNamespaceURI(),
                RunProperties.RPR
            )
        );
    }

    private RunProperties combineLinkedAndParentStyleProperties(StyleType styleType, String styleId, String linkedStyleId, RunProperties runProperties) {
        if (null == linkedStyleId
                || StyleType.CHARACTER != styleType
                || null == linkedStylesByStyleIds.get(linkedStyleId)
                || StyleType.PARAGRAPH != styleTypesByStyleIds.get(linkedStyleId)) {
            // if the linked style is not specified
            // or the style type is not of the linkable style type (character)
            // or the linked style is not present
            // or the linked style type is not a paragraph

            return combineParentStyleProperties(styleType, styleId, runProperties);
        }

        RunProperties paragraphProperties = combineParentStyleProperties(
            StyleType.PARAGRAPH,
            linkedStylesByStyleIds.get(linkedStyleId),
            new RunProperties.Default(
                this.eventFactory,
                this.startElement.getName().getPrefix(),
                this.startElement.getName().getNamespaceURI(),
                RunProperties.RPR
            )
        );
        RunProperties characterProperties = combineParentStyleProperties(
            StyleType.CHARACTER,
            styleId,
            new RunProperties.Default(
                this.eventFactory,
                this.startElement.getName().getPrefix(),
                this.startElement.getName().getNamespaceURI(),
                RunProperties.RPR
            )
        );

        return paragraphProperties.combineDistinct(characterProperties, TraversalStage.HORIZONTAL);
    }

    private RunProperties combineParentStyleProperties(StyleType styleType, String styleId, RunProperties runProperties) {
        if (null == stylesByStyleIds.get(styleId)) {
            return runProperties;
        }

        if (null == parentStylesByStyleIds.get(styleId)
                || styleTypesByStyleIds.get(parentStylesByStyleIds.get(styleId)) != styleType) {
            // if there is no parent style
            // or the style types do not match

            return RunProperties.copiedRunProperties(stylesByStyleIds.get(styleId).runProperties()).combineDistinct(runProperties, TraversalStage.HORIZONTAL);
        }

        return combineParentStyleProperties(styleType, parentStylesByStyleIds.get(styleId),  RunProperties.copiedRunProperties(stylesByStyleIds.get(styleId).runProperties()))
                .combineDistinct(runProperties, TraversalStage.HORIZONTAL);
    }


    @Override
    public StyleDefinitions mergedWith(final StyleDefinitions other) {
        throw new UnsupportedOperationException("The operation is unsupported");
    }

    @Override
    public Markup toMarkup() {
        final Markup markup = new Markup.General(new LinkedList<>());
        markup.addComponent(new MarkupComponent.General(Collections.singletonList(this.startDocument)));
        markup.addComponent(new MarkupComponent.Start(this.eventFactory, this.startElement));
        markup.addMarkup(this.documentDefaults.toMarkup());
        markup.addMarkup(this.latentStyles.toMarkup());
        for (final Map.Entry<String, WordStyleDefinition> entry : this.stylesByStyleIds.entrySet()) {
            markup.addMarkup(entry.getValue().toMarkup());
        }
        markup.addComponent(new MarkupComponent.End(this.endElement));
        markup.addComponent(new MarkupComponent.General(Collections.singletonList(this.endDocument)));

        return markup;
    }

    private static class Ids {
        private final IdGenerator idGenerator;
        private final WordStyleDefinitions styleDefinitions;
        private String parent;

        Ids(final IdGenerator idGenerator, final WordStyleDefinitions styleDefinitions) {
            this.idGenerator = idGenerator;
            this.styleDefinitions = styleDefinitions;
        }

        String next(final String paragraphStyle, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties) {
            if (null != paragraphStyle && this.styleDefinitions.stylesByStyleIds.containsKey(paragraphStyle)) {
                return parentBased(StyleType.PARAGRAPH, paragraphStyle, paragraphBlockProperties, runProperties);
            }
            return defaultBased(StyleType.PARAGRAPH, paragraphBlockProperties, runProperties);
        }

        private String parentBased(final StyleType type, final String parentId, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties) {
            this.parent = parentId;
            final Optional<String> existing = this.styleDefinitions.stylesByStyleIds.entrySet()
                .stream()
                .filter(e -> type == e.getValue().type())
                .filter(e -> parentId.equals(e.getValue().parentId()))
                .filter(e -> paragraphBlockProperties.mergeableWith(e.getValue().paragraphProperties()))
                .filter(e -> runProperties.equals(e.getValue().runProperties()))
                .map(e -> e.getKey())
                .findFirst();
            return existing.orElseGet(
                () -> parentBasedGenerated(parentId)
            );
        }

        private String parentBasedGenerated(final String parentId) {
            String parentBasedId;
            do {
                parentBasedId = this.idGenerator.createId(parentId);
            } while (this.styleDefinitions.stylesByStyleIds.containsKey(parentBasedId));
            return parentBasedId;
        }

        private String defaultBased(final StyleType type, final ParagraphBlockProperties paragraphBlockProperties, final RunProperties runProperties) {
            final String defaultStyle = this.styleDefinitions.defaultStylesByStyleTypes.get(type);
            if (null != defaultStyle) {
                return parentBased(type, defaultStyle, paragraphBlockProperties, runProperties);
            }
            return documentDefaultBased(type, runProperties);
        }

        private String documentDefaultBased(final StyleType type, final RunProperties runProperties) {
            this.parent = null;
            final Optional<String> existing = this.styleDefinitions.stylesByStyleIds.entrySet()
                .stream()
                .filter(e -> type == e.getValue().type())
                .filter(e -> null == e.getValue().parentId())
                .filter(e -> runProperties.equals(e.getValue().runProperties()))
                .map(e -> e.getKey())
                .findFirst();
            return existing.orElseGet(
                () -> documentDefaultBasedGenerated()
            );
        }

        private String documentDefaultBasedGenerated() {
            return this.idGenerator.createIdNotInList(
                new ArrayList<>(this.styleDefinitions.stylesByStyleIds.keySet())
            );
        }

        String parent() {
            return this.parent;
        }
    }
}
