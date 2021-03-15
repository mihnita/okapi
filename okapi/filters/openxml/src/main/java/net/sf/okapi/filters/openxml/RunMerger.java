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

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.filters.openxml.RunFonts.ContentCategory;
import net.sf.okapi.filters.openxml.RunProperty.FontsRunProperty;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

class RunMerger {

    /**
     * A message when unsupported run chunks provided.
     */
    private static final String UNSUPPORTED_RUN_CHUNKS_PROVIDED = "Unsupported run chunks provided";

    private static final Pattern ASCII_CHARACTERS = Pattern.compile(".*[\u0000-\u007F].*");

    /**
     * All latin character ranges according to
     * <a href="https://blogs.msdn.microsoft.com/officeinteroperability/2013/04/22/office-open-xml-themes-schemes-and-fonts/">
     * Office Open XML Themes, Schemes and Fonts</a>.
     */
    private static final Pattern LATIN_CHARACTERS = Pattern.compile(
        ".*[\u0080-\u00A6\u00A9-\u00AF\u00B2-\u00B3\u00B5-\u00D6\u00D8-\u00F6"
        + "\u00F8-\u058F\u10A0-\u10FF\u1200-\u137F\u13A0-\u177F\u1D00-\u1D7F\u1E00-\u1FFF"
        + "\u2000-\u200B\u2010-\u2017\u201F-\u2029\u2030-\u2046\u204A-\u245F\u27C0-\u2BFF\uD835"
        + "\uFB00-\uFB17\uFE50-\uFE6F].*"
    );

    /**
     * Symbols according to
     * <a href="https://blogs.msdn.microsoft.com/officeinteroperability/2013/04/22/office-open-xml-themes-schemes-and-fonts/">
     * Office Open XML Themes, Schemes and Fonts</a>.
     */
    private static final Pattern SYMBOLS = Pattern.compile(".*[\uF000-\uF0FF].*");

    /**
     * In dependence of the {@code w:lang} property, the characters selected by this selector may be
     * considered as latin or east asian characters. Further information can be found under
     * <a href="https://blogs.msdn.microsoft.com/officeinteroperability/2013/04/22/office-open-xml-themes-schemes-and-fonts/">
     * Office Open XML Themes, Schemes and Fonts</a>.
     */
    private static final Pattern SHARED_CHARACTERS = Pattern.compile(".*[\u2018-\u201E].*");

    /**
     * All complex script character ranges according to
     * <a href="https://blogs.msdn.microsoft.com/officeinteroperability/2013/04/22/office-open-xml-themes-schemes-and-fonts/">
     * Office Open XML Themes, Schemes and Fonts</a>.
     */
    private static final Pattern COMPLEX_SCRIPT_CHARACTERS = Pattern.compile(
        ".*[\u0590-\u074F\u0780-\u07BF\u0900-\u109F\u1780-\u18AF"
        + "\u200C-\u200F\u202A-\u202F\u2670-\u2671\uFB1D-\uFB4F].*"
    );

    /**
     * Simple east asia character selector according to
     * <a href="https://blogs.msdn.microsoft.com/officeinteroperability/2013/04/22/office-open-xml-themes-schemes-and-fonts/">
     * Office Open XML Themes, Schemes and Fonts</a>.
     * <p>
     * Note that also any character not matching a certain selector may be considered as east asia,
     * too.
     */
    private static final Pattern EAST_ASIA_CHARACTERS = Pattern.compile(".*[\u3099-\u309A].*");

    /**
     * Contains the {@link LocaleId locales} of the east asian languages that influence the
     * effective content categories for
     * {@link #SHARED_CHARACTERS quotation characters}.
     */
    private static final List<LocaleId> EAST_ASIAN_LOCALES = asList(
            LocaleId.fromBCP47("ii-CN"),
            LocaleId.fromBCP47("ja-JP"),
            LocaleId.fromBCP47("ko-KR"),
            LocaleId.fromBCP47("zh-CN"),
            LocaleId.fromBCP47("zh-HK"),
            LocaleId.fromBCP47("zh-MO"),
            LocaleId.fromBCP47("zh-SG"),
            LocaleId.fromBCP47("zh-TW")
    );

    private String paragraphStyle;
    private RunBuilder runBuilder;
    private List<Chunk> completedRuns = new ArrayList<>();

    void setParagraphStyle(String paragraphStyle) {
        this.paragraphStyle = paragraphStyle;
    }

    boolean hasRunBuilder() {
        return runBuilder != null;
    }

    List<Chunk> getRuns() throws XMLStreamException {
        if (runBuilder != null) {
            completedRuns.add(runBuilder.build());
            runBuilder = null;
        }

        return completedRuns;
    }

    void add(final RunBuilder otherRunBuilder) throws XMLStreamException {
        if (null == runBuilder) {
            otherRunBuilder.resetCombinedRunProperties(paragraphStyle); // to be consistent with other run builders
            runBuilder = otherRunBuilder;

            return;
        }

        if (canMergeWith(otherRunBuilder)) {
            mergeWith(otherRunBuilder);
        } else {
            completedRuns.add(runBuilder.build());
            runBuilder = otherRunBuilder;
        }
    }

    private boolean canMergeWith(final RunBuilder otherRunBuilder) {
        RunProperties combinedRunProperties = runBuilder.getCombinedRunProperties(paragraphStyle);
        RunProperties combinedOtherRunProperties = otherRunBuilder.getCombinedRunProperties(paragraphStyle);

        if (runBuilder.isHidden() || otherRunBuilder.isHidden()){
            return false;
        }

        // Merging runs in the math namespace can sometimes corrupt formulas,
        // so don't do it.
        if (Namespaces.Math.containsName(runBuilder.getStartElementContext().getStartElement().getName())) {
            return false;
        }
        // XXX Don't merge runs that have nested blocks, to avoid having to go
        // back and renumber the references in the skeleton. I should probably
        // fix this at some point.  Note that we check for the existence of -any-
        // nested block, not just ones with text.
        // The reason for this pre-caution is because when we merge runs, we
        // re-parse the xml eventReader.  However, it doesn't cover all the cases.
        // We might be able to remove this restriction if we could clean up the
        // way the run body eventReader are parsed during merging.
        if (runBuilder.containsNestedItems() || otherRunBuilder.containsNestedItems()) {
            return false;
        }
        // Don't merge stuff involving complex codes
        if (runBuilder.containsComplexFields() || otherRunBuilder.containsComplexFields()) {
            return false;
        }

        detectRunFontsContentCategories(runBuilder, combinedRunProperties);
        detectRunFontsContentCategories(otherRunBuilder, combinedOtherRunProperties);

        return canRunPropertiesBeMerged(combinedRunProperties, combinedOtherRunProperties);
    }

    /**
     * Analyzes run texts {@code runText} and detects run fonts {@link ContentCategory}s.
     *
     * @param runBuilder the run builder
     * @param combinedRunProperties the combined run properties
     */
    private void detectRunFontsContentCategories(RunBuilder runBuilder, RunProperties combinedRunProperties) {
        LocaleId sourceLanguage = runBuilder.getStartElementContext().getSourceLanguage();
        Run.RunText runText = runBuilder.firstRunText();

        if (runText == null) {
            return;
        }

        for (final Property runProperty : combinedRunProperties.properties()) {

            if (runProperty instanceof FontsRunProperty) {
                FontsRunProperty fontsRunProperty = (FontsRunProperty) runProperty;
                RunFonts runFonts = fontsRunProperty.getRunFonts();
                if (!runFonts.containsDetectedContentCategories()) {
                    detectAndAddRunFontsContentCategories(
                        sourceLanguage,
                        runText.characters().getData(),
                        runFonts
                    );
                }
            }
        }
    }

    private void detectAndAddRunFontsContentCategories(LocaleId sourceLanguage, String runText, RunFonts runFonts) {
        if (ASCII_CHARACTERS.matcher(runText).matches()) {
            runFonts.addDetectedContentCategory(
                runFonts.getContentCategory(ContentCategory.ASCII_THEME, ContentCategory.ASCII)
            );
        }
        if (SHARED_CHARACTERS.matcher(runText).matches()) {
            if (EAST_ASIAN_LOCALES.contains(sourceLanguage)) {
                runFonts.addDetectedContentCategory(
                    runFonts.getContentCategory(ContentCategory.EAST_ASIAN_THEME, ContentCategory.EAST_ASIAN)
                );
            } else {
                runFonts.addDetectedContentCategory(
                    runFonts.getContentCategory(ContentCategory.HIGH_ANSI_THEME, ContentCategory.HIGH_ANSI)
                );
            }
        }
        if (LATIN_CHARACTERS.matcher(runText).matches() || SYMBOLS.matcher(runText).matches()) {
            runFonts.addDetectedContentCategory(
                runFonts.getContentCategory(ContentCategory.HIGH_ANSI_THEME, ContentCategory.HIGH_ANSI)
            );
        }
        if (COMPLEX_SCRIPT_CHARACTERS.matcher(runText).matches()) {
            runFonts.addDetectedContentCategory(
                runFonts.getContentCategory(ContentCategory.COMPLEX_SCRIPT_THEME, ContentCategory.COMPLEX_SCRIPT)
            );
        }
        if (EAST_ASIA_CHARACTERS.matcher(runText).matches()
                || matchesOtherCharacters(runText)) {
            runFonts.addDetectedContentCategory(
                runFonts.getContentCategory(ContentCategory.EAST_ASIAN_THEME, ContentCategory.EAST_ASIAN)
            );
        }
    }

    private boolean matchesOtherCharacters(final String text) {
        return !ASCII_CHARACTERS.matcher(text).matches()
            && !LATIN_CHARACTERS.matcher(text).matches()
            && !SYMBOLS.matcher(text).matches()
            && !SHARED_CHARACTERS.matcher(text).matches()
            && !COMPLEX_SCRIPT_CHARACTERS.matcher(text).matches()
            && !EAST_ASIA_CHARACTERS.matcher(text).matches();
    }

    private boolean canRunPropertiesBeMerged(RunProperties currentProperties, RunProperties otherProperties) {
        if (currentProperties.count() != otherProperties.count()) {
            return false;
        }

        int numberOfMatchedProperties = 0;

        for (final Property currentProperty : currentProperties.properties()) {
            QName currentPropertyStartElementName = currentProperty.getName();

            for (final Property otherProperty : otherProperties.properties()) {
                QName otherPropertyStartElementName = otherProperty.getName();

                if (!currentPropertyStartElementName.equals(otherPropertyStartElementName)) {
                    continue;
                }

                if (currentProperty instanceof MergeableRunProperty && otherProperty instanceof MergeableRunProperty) {
                    if (!((MergeableRunProperty) currentProperty).canBeMerged((MergeableRunProperty) otherProperty)) {
                        return false;
                    }
                } else {
                    if (currentProperty instanceof ReplaceableRunProperty && otherProperty instanceof ReplaceableRunProperty) {
                        if (!((ReplaceableRunProperty) currentProperty).canBeReplaced((ReplaceableRunProperty) otherProperty)) {
                            return false;
                        }
                    }
                }
                numberOfMatchedProperties++;
                break;
            }
        }

        if (numberOfMatchedProperties < currentProperties.count()) {
            return false;
        }

        return true;
    }

    /**
     * Merges the run builder with another run builder.
     *
     * That implies the merge of run properties and run body chunks.
     *
     * @param otherRunBuilder The other run builder to merge with
     */
    private void mergeWith(final RunBuilder otherRunBuilder) {

        runBuilder.setRunProperties(mergeRunProperties(runBuilder.getRunProperties(), otherRunBuilder.getRunProperties(),
                runBuilder.getCombinedRunProperties(paragraphStyle), otherRunBuilder.getCombinedRunProperties(paragraphStyle)));

        runBuilder.resetCombinedRunProperties(paragraphStyle);

        runBuilder.setTextPreservingWhitespace(runBuilder.isTextPreservingWhitespace() || otherRunBuilder.isTextPreservingWhitespace());
        runBuilder.setRunBodyChunks(mergeRunBodyChunks(runBuilder.getRunBodyChunks(), otherRunBuilder.getRunBodyChunks()));
    }

    private RunProperties mergeRunProperties(RunProperties runProperties, RunProperties otherRunProperties,
                                             RunProperties combinedRunProperties, RunProperties otherCombinedRunProperties) {
        // try to reduce the set of properties
        final List<Property> mergeableRunProperties = runProperties.getMergeableRunProperties();
        final List<Property> otherMergeableRunProperties = otherRunProperties.getMergeableRunProperties();

        if (mergeableRunProperties.isEmpty() && otherMergeableRunProperties.isEmpty()) {
            return runProperties.count() <= otherRunProperties.count()
                    ? runProperties
                    : otherRunProperties;
        }

        if (mergeableRunProperties.size() >= otherMergeableRunProperties.size()) {
            final List<Property> remainedOtherMergeableRunProperties = mergeMergeableRunProperties(mergeableRunProperties, otherMergeableRunProperties);
            runProperties.refine(mergeableRunProperties);
            runProperties.properties().addAll(remainedOtherMergeableRunProperties);

            clarifyFontsRunProperties(runProperties, mergeCombinedRunProperties(combinedRunProperties, otherCombinedRunProperties));

            return runProperties;
        }

        final List<Property> remainedMergeableRunProperties = mergeMergeableRunProperties(otherMergeableRunProperties, mergeableRunProperties);
        otherRunProperties.refine(otherMergeableRunProperties);
        otherRunProperties.properties().addAll(remainedMergeableRunProperties);

        clarifyFontsRunProperties(otherRunProperties, mergeCombinedRunProperties(combinedRunProperties, otherCombinedRunProperties));

        return otherRunProperties;
    }

    private RunProperties mergeCombinedRunProperties(RunProperties combinedRunProperties, RunProperties otherCombinedRunProperties) {
        final List<Property> mergeableCombinedRunProperties = combinedRunProperties.getMergeableRunProperties();
        final List<Property> otherMergeableCombinedRunProperties = otherCombinedRunProperties.getMergeableRunProperties();

        if (mergeableCombinedRunProperties.size() >= otherMergeableCombinedRunProperties.size()) {
            final List<Property> remainedOtherMergeableCombinedRunProperties = mergeMergeableRunProperties(mergeableCombinedRunProperties, otherMergeableCombinedRunProperties);
            combinedRunProperties.refine(mergeableCombinedRunProperties);
            combinedRunProperties.properties().addAll(remainedOtherMergeableCombinedRunProperties);

            return combinedRunProperties;
        }

        final List<Property> remainedMergeableCombinedRunProperties = mergeMergeableRunProperties(otherMergeableCombinedRunProperties, mergeableCombinedRunProperties);
        otherCombinedRunProperties.refine(otherMergeableCombinedRunProperties);
        otherCombinedRunProperties.properties().addAll(remainedMergeableCombinedRunProperties);

        return otherCombinedRunProperties;
    }

    private List<Property> mergeMergeableRunProperties(List<Property> mergeableRunProperties, List<Property> otherMergeableRunProperties) {
        final List<Property> remainedOtherMergeableRunProperties = new ArrayList<>(otherMergeableRunProperties);

        final ListIterator<Property> mergeableRunPropertiesIterator = mergeableRunProperties.listIterator();
        while (mergeableRunPropertiesIterator.hasNext()) {
            final Property runProperty = mergeableRunPropertiesIterator.next();
            final QName currentPropertyStartElementName = runProperty.getName();

            final Iterator<Property> remainedOtherMergeableRunPropertyIterator = remainedOtherMergeableRunProperties.iterator();

            while (remainedOtherMergeableRunPropertyIterator.hasNext()) {
                final Property otherRunProperty = remainedOtherMergeableRunPropertyIterator.next();
                QName otherPropertyStartElementName = otherRunProperty.getName();

                if (!currentPropertyStartElementName.equals(otherPropertyStartElementName)) {
                    continue;
                }

                mergeableRunPropertiesIterator.set(
                    (Property) ((MergeableRunProperty) runProperty).merge((MergeableRunProperty) otherRunProperty)
                );
                remainedOtherMergeableRunPropertyIterator.remove();
                break;
            }
        }

        return remainedOtherMergeableRunProperties;
    }

    private void clarifyFontsRunProperties(RunProperties runProperties, RunProperties combinedRunProperties) {
        for (final Property combinedRunProperty : combinedRunProperties.properties()) {

            if (combinedRunProperty instanceof FontsRunProperty) {
                final ListIterator<Property> runPropertyIterator = runProperties.properties().listIterator();

                while (runPropertyIterator.hasNext()) {
                    final Property runProperty = runPropertyIterator.next();

                    if (runProperty instanceof FontsRunProperty) {
                        runPropertyIterator.set(combinedRunProperty);

                        return;
                    }
                }

                runPropertyIterator.add(combinedRunProperty);

                return;
            }
        }
    }

    private List<Chunk> mergeRunBodyChunks(final List<Chunk> chunks, final List<Chunk> otherChunks) {
        if (chunks.isEmpty()) {
            return otherChunks;
        }
        if (otherChunks.isEmpty()) {
            return chunks;
        }

        final List<Chunk> mergedChunks = new ArrayList<>(chunks.size() + otherChunks.size());

        final ListIterator<Chunk> chunksIterator = chunks.listIterator(chunks.size() - 1);
        final ListIterator<Chunk> otherChunksIterator = otherChunks.listIterator(0);

        final Chunk chunk = chunksIterator.next();
        final Chunk otherChunk = otherChunksIterator.next();

        if (!canRunBodyChunksBeMerged(chunk, otherChunk)) {
            mergedChunks.addAll(chunks);
            mergedChunks.addAll(otherChunks);

            return mergedChunks;
        }

        if (-1 < chunksIterator.previousIndex()) {
            mergedChunks.addAll(chunks.subList(0, chunksIterator.previousIndex()));
        }

        mergedChunks.add(mergeRunBodyChunks(chunk, otherChunk));

        if (otherChunks.size() > otherChunksIterator.nextIndex()) {
            mergedChunks.addAll(otherChunks.subList(otherChunksIterator.nextIndex(), otherChunks.size()));
        }

        return mergedChunks;
    }

    private static boolean canRunBodyChunksBeMerged(final Chunk chunk, final Chunk otherChunk) {
        return chunk instanceof Run.Markup && otherChunk instanceof Run.Markup
                || chunk instanceof Run.RunText && otherChunk instanceof Run.RunText;
    }

    private Chunk mergeRunBodyChunks(final Chunk chunk, final Chunk otherChunk) {
        if (chunk instanceof Run.Markup && otherChunk instanceof Run.Markup) {
            return mergeRunMarkups((Run.Markup) chunk, (Run.Markup) otherChunk);
        }
        if (chunk instanceof Run.RunText && otherChunk instanceof Run.RunText) {
            return mergeRunTexts((Run.RunText) chunk, (Run.RunText) otherChunk);
        }
        throw new IllegalArgumentException(UNSUPPORTED_RUN_CHUNKS_PROVIDED);
    }

    private Run.Markup mergeRunMarkups(final Run.Markup markup, final Run.Markup otherMarkup) {
        final Run.Markup runMarkup = new Run.Markup(
            new Markup.General(
                new ArrayList<>(markup.components().size() + otherMarkup.components().size())
            )
        );
        runMarkup.addComponents(markup.components());
        runMarkup.addComponents(otherMarkup.components());

        return runMarkup;
    }

    private Chunk mergeRunTexts(final Run.RunText text, final Run.RunText otherText) {
        return new Run.RunText(
                mergeStartElements(text.startElement(), otherText.startElement()),
                mergeCharacters(text.characters(), otherText.characters()),
                text.endElement()
        );
    }

    private static StartElement mergeStartElements(final StartElement startElement, final StartElement otherStartElement) {
        return numberOfIterables(startElement.getAttributes()) >= numberOfIterables(otherStartElement.getAttributes())
                ? startElement
                : otherStartElement;
    }

    private static int numberOfIterables(Iterator iterator) {
        int number = 0;

        while (iterator.hasNext()) {
            iterator.next();
            number++;
        }

        return number;
    }

    private Characters mergeCharacters(final Characters characters, final Characters otherCharacters) {
        return runBuilder.getStartElementContext().getEventFactory()
                .createCharacters(characters.getData().concat(otherCharacters.getData()));
    }

    void reset() {
        completedRuns.clear();
        runBuilder = null;
    }

    /**
     * Adds text to the {@link Run.RunText} in the run builder.
     *
     * @param text The text to add
     */
    void addToRunTextInRunBuilder(String text) {
        runBuilder.addToFirstRunText(text);
    }
}
