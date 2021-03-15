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

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.getAttributeValue;

/**
 * Representation of the data in an <w:rFonts> tag, which can be
 * merged with the font information from other runs in some cases.
 */
class RunFonts implements XMLEvents {
    static final String NAME = "rFonts";

    /**
     * A maximum number of events per rFonts tag.
     */
    private static final int MAX_NUMBER_OF_EVENTS = 2;

    /**
     * Within a single run, there can be up to four types of content present
     * which shall each be allowed to use a unique font.
     */
    private static final int MAX_NUMBER_OF_CONTENT_CATEGORIES = 4;

    private static final EnumSet<ContentCategory> FONT_CONTENT_CATEGORIES = EnumSet.of(
            ContentCategory.ASCII,
            ContentCategory.HIGH_ANSI,
            ContentCategory.COMPLEX_SCRIPT,
            ContentCategory.EAST_ASIAN,
            ContentCategory.HINT);

    private static final EnumMap<ContentCategory, ContentCategory> fontThemeContentCategories = new EnumMap<>(ContentCategory.class);
    static {
        fontThemeContentCategories.put(ContentCategory.ASCII, ContentCategory.ASCII_THEME);
        fontThemeContentCategories.put(ContentCategory.HIGH_ANSI, ContentCategory.HIGH_ANSI_THEME);
        fontThemeContentCategories.put(ContentCategory.COMPLEX_SCRIPT, ContentCategory.COMPLEX_SCRIPT_THEME);
        fontThemeContentCategories.put(ContentCategory.EAST_ASIAN, ContentCategory.EAST_ASIAN_THEME);
    }

    private static final Map<String, EnumSet<ContentCategory>> contentCategoriesByHints = new HashMap<>();
    static {
        contentCategoriesByHints.put(Hint.DEFAULT.toString(), EnumSet.of(ContentCategory.HIGH_ANSI_THEME, ContentCategory.HIGH_ANSI));
        contentCategoriesByHints.put(Hint.COMPLEX_SCRIPT.toString(), EnumSet.of(ContentCategory.COMPLEX_SCRIPT_THEME, ContentCategory.COMPLEX_SCRIPT));
        contentCategoriesByHints.put(Hint.EAST_ASIAN.toString(), EnumSet.of(ContentCategory.EAST_ASIAN_THEME, ContentCategory.EAST_ASIAN));
    }

    private final XMLEventFactory eventFactory;
    private final StartElement startElement;
    private final EnumMap<ContentCategory, String> fonts;

    /**
     * The content categories that were detected during parse.
     *
     * E.g. if the text contains characters from the ASCII range this {@link Set} will have {@link ContentCategory#ASCII} or
     * {@link ContentCategory#ASCII_THEME}.
     */
    private final Set<ContentCategory> detectedContentCategories;

    private RunFonts(
        final XMLEventFactory eventFactory,
        final StartElement startElement,
        final EnumMap<ContentCategory, String> fonts,
        final Set<ContentCategory> detectedContentCategories
    ) {
        this.eventFactory = eventFactory;
        this.startElement = startElement;
        this.fonts = fonts;
        this.detectedContentCategories = detectedContentCategories;
    }

    static RunFonts createRunFonts(StartElementContext startElementContext) throws XMLStreamException {
        EnumMap<ContentCategory, String> fonts = new EnumMap<>(ContentCategory.class);

        for (ContentCategory contentCategory : ContentCategory.values()) {
            fonts.put(
                contentCategory,
                getAttributeValue(
                    startElementContext.getStartElement(),
                    new QName(
                        startElementContext.getStartElement().getName().getNamespaceURI(),
                        contentCategory.toString(),
                        startElementContext.getStartElement().getName().getPrefix()
                    )
                )
            );
        }
        new SkippableElements.Default().skip(startElementContext);

        return new RunFonts(
            startElementContext.getEventFactory(),
            startElementContext.getStartElement(),
            fonts,
            EnumSet.noneOf(ContentCategory.class)
        );
    }


    private EnumMap<ContentCategory, String> getFonts() {
        return fonts;
    }

    private boolean isDetectedContentCategory(ContentCategory category) {
        return detectedContentCategories.contains(category);
    }

    boolean containsDetectedContentCategories() {
        return !this.detectedContentCategories.isEmpty();
    }

    void addDetectedContentCategory(final ContentCategory contentCategory) {
        this.detectedContentCategories.add(contentCategory);
    }

    ContentCategory getContentCategory(ContentCategory contentCategory, ContentCategory defaultContentCategory) {
        return fonts.get(contentCategory) == null ? defaultContentCategory : contentCategory;
    }

    /**
     * Fonts can be merged if they contain no contradictory font information. This means
     * all content categories either have the same value, or else be specified for at most one
     * of the two font objects.
     *
     * @param runFonts The run fonts to check against
     *
     * @return {@code true} if current run fonts can be merged with another
     */
    boolean canBeMerged(RunFonts runFonts) {
        for (ContentCategory fontContentCategory : FONT_CONTENT_CATEGORIES) {

            if (!canContentCategoriesBeMerged(fontContentCategory, runFonts)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns {@code false} if the given {@code category} is effectively used by this and the other
     * {@link RunFonts} and they have different values. If one of the {@link RunFonts} does
     * not need the category because it does not contain text that is within the category's
     * character range, the value can be discarded and the categories can be merged.
     *
     * @return {@code false} if the given {@code category} is effectively used by this and the other
     *      {@link RunFonts} and they have different values
     */
    @SuppressWarnings("RedundantIfStatement")
    private boolean canContentCategoriesBeMerged(ContentCategory contentCategory, RunFonts runFonts) {
        if (ContentCategory.HINT == contentCategory) {
            return canHintsBeMerged(runFonts);
        }
        ContentCategory fontThemeCategory = fontThemeContentCategories.get(contentCategory);

        if (isDetectedContentCategory(fontThemeCategory) && runFonts.isDetectedContentCategory(fontThemeCategory)) {
            return Objects.equals(fonts.get(fontThemeCategory), runFonts.fonts.get(fontThemeCategory));
        }
        if (isDetectedContentCategory(fontThemeCategory) && runFonts.isDetectedContentCategory(contentCategory)) {
            return Objects.equals(fonts.get(fontThemeCategory), runFonts.fonts.get(contentCategory));
        }
        if (isDetectedContentCategory(contentCategory) && runFonts.isDetectedContentCategory(fontThemeCategory)) {
            return Objects.equals(fonts.get(contentCategory), runFonts.fonts.get(fontThemeCategory));
        }
        if (isDetectedContentCategory(contentCategory) && runFonts.isDetectedContentCategory(contentCategory)) {
            return Objects.equals(fonts.get(contentCategory), runFonts.fonts.get(contentCategory));
        }
        return true;
    }

    private boolean canHintsBeMerged(final RunFonts runFonts) {
        if (null == fonts.get(ContentCategory.HINT) && null == runFonts.fonts.get(ContentCategory.HINT)) {
            return true;
        }
        if (null != fonts.get(ContentCategory.HINT) && null != runFonts.fonts.get(ContentCategory.HINT)) {
            return Objects.equals(fonts.get(ContentCategory.HINT), runFonts.fonts.get(ContentCategory.HINT));
        }
        if (null != fonts.get(ContentCategory.HINT) && null == runFonts.fonts.get(ContentCategory.HINT)) {
            // the current run fonts does not contain content category for hint
            return !containsContentCategoryFor(fonts.get(ContentCategory.HINT));
        }
        if (null == fonts.get(ContentCategory.HINT) && null != runFonts.fonts.get(ContentCategory.HINT)) {
            // another run fonts does not contain content category for hint
            return !runFonts.containsContentCategoryFor(runFonts.fonts.get(ContentCategory.HINT));
        }
        return false;
    }

    private boolean containsContentCategoryFor(final String hint) {
        for (final ContentCategory category : RunFonts.contentCategoriesByHints.get(hint)) {
            if (null != fonts.get(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Merges another run fonts object into this one. Returns the merged instance,
     * which may not be the same as this.
     *
     * @param runFonts The run fonts to merge with
     *
     * @return Merged current run fonts
     */
    RunFonts merge(RunFonts runFonts) {
        final EnumMap<ContentCategory, String> newFonts = new EnumMap<>(ContentCategory.class);

        for (ContentCategory category : ContentCategory.values()) {
            newFonts.put(category, mergeContentCategories(category, runFonts));
        }

        final Set<ContentCategory> newDetectedContentCategories = EnumSet.noneOf(ContentCategory.class);
        newDetectedContentCategories.addAll(this.detectedContentCategories);
        newDetectedContentCategories.addAll(runFonts.detectedContentCategories);

        return new RunFonts(
            this.eventFactory,
            this.startElement,
            newFonts,
            newDetectedContentCategories
        );
    }

    /**
     * Returns the merged category value. Considers the effective categories of both
     * {@link RunFonts}.
     *
     * @param contentCategory the content category
     * @param runFonts the run fonts to merge with
     * @return the merged category value
     *         or {@code null} if the content category does not belong to any of effective content categories
     */
    private String mergeContentCategories(ContentCategory contentCategory, RunFonts runFonts) {
        if (ContentCategory.HINT == contentCategory) {
            return null == fonts.get(ContentCategory.HINT)
                    ? runFonts.getFonts().get(ContentCategory.HINT)
                    : fonts.get(ContentCategory.HINT);
        }
        if (Objects.equals(fonts.get(contentCategory), runFonts.getFonts().get(contentCategory))) {
            return fonts.get(contentCategory);
        }
        if (isDetectedContentCategory(contentCategory)) {
            return fonts.get(contentCategory);
        }
        if (runFonts.isDetectedContentCategory(contentCategory)) {
            return runFonts.fonts.get(contentCategory);
        }
        return null;
    }

    @Override
    public List<XMLEvent> getEvents() {
        List<XMLEvent> events = new ArrayList<>(MAX_NUMBER_OF_EVENTS);

        events.add(eventFactory.createStartElement(startElement.getName(), getAttributes(startElement), startElement.getNamespaces()));
        events.add(eventFactory.createEndElement(startElement.getName(), startElement.getNamespaces()));

        return events;
    }

    private Iterator<Attribute> getAttributes(final StartElement startElement) {
        List<Attribute> attributes = new ArrayList<>(MAX_NUMBER_OF_CONTENT_CATEGORIES);

        for (ContentCategory category : ContentCategory.values()) {
            String value = fonts.get(category);

            if (value != null) {
                attributes.add(eventFactory.createAttribute(
                    startElement.getName().getPrefix(),
                    startElement.getName().getNamespaceURI(),
                    category.toString(),
                    value
                ));
            }
        }

        return attributes.iterator();
    }

    void apply(final FontMappings fontMappings) {
        this.fonts.entrySet().stream()
            .filter(e -> Objects.nonNull(e.getValue()))
            .forEach(e -> e.setValue(fontMappings.targetFontFor(e.getValue())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RunFonts runFonts = (RunFonts) o;
        return Objects.equals(fonts, runFonts.fonts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fonts);
    }

    enum ContentCategory {
        ASCII("ascii"),
        ASCII_THEME("asciiTheme"),
        HIGH_ANSI("hAnsi"),
        HIGH_ANSI_THEME("hAnsiTheme"),
        COMPLEX_SCRIPT("cs"),
        COMPLEX_SCRIPT_THEME("cstheme"),
        EAST_ASIAN("eastAsia"),
        EAST_ASIAN_THEME("eastAsiaTheme"),
        HINT("hint");

    	private final String value;

        ContentCategory(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    enum Hint {
        DEFAULT("default"),
        COMPLEX_SCRIPT("cs"),
        EAST_ASIAN("eastAsia");

        private final String value;

        Hint(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
