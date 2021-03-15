/*
 * =============================================================================
 *   Copyright (C) 2010-2013 by the Okapi Framework contributors
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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.skeleton.ZipSkeleton;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static net.sf.okapi.filters.idml.OrderingIdioms.getOrderedPasteboardItems;
import static net.sf.okapi.filters.idml.OrderingIdioms.getOrderedStoryIds;
import static net.sf.okapi.filters.idml.OrderingIdioms.getOrderedStoryPartNames;
import static net.sf.okapi.filters.idml.ParsingIdioms.MASTER_SPREAD;
import static net.sf.okapi.filters.idml.ParsingIdioms.SPREAD;

interface Document {
    Event open() throws XMLStreamException, IOException;
    boolean hasNextSubDocument();
    SubDocument nextSubDocument();
    void close() throws IOException;

    class Default implements Document {
        private final Parameters parameters;
        private final XMLInputFactory inputFactory;
        private final XMLOutputFactory outputFactory;
        private final XMLEventFactory eventFactory;
        private final String startDocumentId;
        private final URI uri;
        private final LocaleId sourceLocale;
        private final String encoding;
        private final String lineBreak;
        private final IFilterWriter filterWriter;

        private ZipFile zipFile;
        private ZipInput<XMLEventReader> zipInputReader;

        private DesignMap designMap;
        private StyleDefinitions styleDefinitions;
        private List<String> nonTranslatableSubDocuments;
        private Enumeration<? extends ZipEntry> zipFileEntries;
        private int currentSubDocumentId;

        Default(
            final Parameters parameters,
            final XMLInputFactory inputFactory,
            final XMLOutputFactory outputFactory,
            final XMLEventFactory eventFactory,
            final String startDocumentId,
            final URI uri,
            final LocaleId sourceLocale,
            final String encoding,
            final String lineBreak,
            final IFilterWriter filterWriter
        ) {
            this.parameters = parameters;
            this.inputFactory = inputFactory;
            this.outputFactory = outputFactory;
            this.eventFactory = eventFactory;
            this.startDocumentId = startDocumentId;
            this.uri = uri;
            this.sourceLocale = sourceLocale;
            this.encoding = encoding;
            this.lineBreak = lineBreak;
            this.filterWriter = filterWriter;
        }

        @Override
        public Event open() throws XMLStreamException, IOException {
            zipFile = new ZipFile(new File(uri.getPath()), ZipFile.OPEN_READ);

            final ZipInput<InputStream> zipInputStream = new ZipInput.Stream(this.zipFile);
            final MimeType mimeType = new MimeType(zipInputStream);
            mimeType.from(this.zipFile.getEntry(Document.Default.PartNames.MIME_TYPE));

            if (!IDMLFilter.MIME_TYPE.equals(mimeType.toString(this.encoding))) {
                throw new OkapiBadFilterInputException("IDML filter tried to initialise a file that is not supported.");
            }

            this.zipInputReader = new ZipInput.Reader(zipInputStream, this.encoding, this.inputFactory);

            this.designMap = new DesignMapParser(this.zipInputReader).parse(this.zipFile.getEntry(PartNames.DESIGN_MAP));
            Preferences preferences = new PreferencesParser(this.zipInputReader).parse(this.zipFile.getEntry(designMap.getPreferencesPartName()));

            this.styleDefinitions = new StyleDefinitions.Default(
                new Markup.Default(new LinkedList<>()),
                this.zipInputReader,
                this.eventFactory
            );
            styleDefinitions.from(this.zipFile.getEntry(designMap.getStylesPartName()));

            List<PasteboardItem> pasteboardItems = new ArrayList<>();
            List<PasteboardItem> invisiblePasteboardItems = new ArrayList<>();

            List<Spread> masterSpreads = getSpreads(designMap.getMasterSpreadPartNames(), designMap.getActiveLayerId(), MASTER_SPREAD);
            List<PasteboardItem> masterSpreadPasteboardItems = getOrderedPasteboardItems(
                masterSpreads,
                preferences.getStoryPreference().getStoryDirection(),
                eventFactory
            );

            if (parameters.getExtractMasterSpreads()) {
                pasteboardItems.addAll(masterSpreadPasteboardItems);
            } else {
                invisiblePasteboardItems.addAll(masterSpreadPasteboardItems);
            }

            List<Spread> spreads = getSpreads(designMap.getSpreadPartNames(), designMap.getActiveLayerId(), SPREAD);
            pasteboardItems.addAll(getOrderedPasteboardItems(spreads, preferences.getStoryPreference().getStoryDirection(), eventFactory));

            List<PasteboardItem> visiblePasteboardItems = getVisiblePasteboardItems(designMap, pasteboardItems);
            invisiblePasteboardItems.addAll(getInvisiblePasteboardItems(pasteboardItems, visiblePasteboardItems));

            List<String> storyIds = getOrderedStoryIds(visiblePasteboardItems);
            List<String> storyPartNames = getOrderedStoryPartNames(designMap.getStoryPartNames(), storyIds);

            List<String> invisibleStoryIds = getOrderedStoryIds(invisiblePasteboardItems);
            List<String> invisibleStoryPartNames = getOrderedStoryPartNames(designMap.getStoryPartNames(), invisibleStoryIds);

            nonTranslatableSubDocuments = PartNames.getPartNames(designMap, invisibleStoryPartNames);

            zipFileEntries = getZipFileEntries(designMap, storyPartNames);
            currentSubDocumentId = 0;

            return getStartDocumentEvent(uri, sourceLocale, filterWriter);
        }

        private List<Spread> getSpreads(List<String> spreadPartNames, String activeLayerId, QName spreadName) throws IOException, XMLStreamException {
            List<Spread> spreads = new ArrayList<>();
            final SpreadParser sp = new SpreadParser(this.zipInputReader, spreadName, eventFactory, activeLayerId);
            for (String spreadPartName : spreadPartNames) {
                Spread spread = sp.parse(zipFile.getEntry(spreadPartName));
                spreads.add(spread);
            }

            return spreads;
        }

        private List<PasteboardItem> getVisiblePasteboardItems(DesignMap designMap, List<PasteboardItem> pasteboardItems) {
            return new PasteboardItem.VisibilityFilter(
                designMap.getLayers(),
                parameters.getExtractHiddenLayers(),
                parameters.getExtractHiddenPasteboardItems()
            ).filterVisible(pasteboardItems);
        }

        private List<PasteboardItem> getInvisiblePasteboardItems(List<PasteboardItem> pasteboardItems, List<PasteboardItem> visiblePasteboardItems) {
            List<PasteboardItem> invisiblePasteboardItems = new ArrayList<>(pasteboardItems);
            invisiblePasteboardItems.removeAll(visiblePasteboardItems);

            return invisiblePasteboardItems;
        }

        private Enumeration<? extends ZipEntry> getZipFileEntries(DesignMap designMap, List<String> storyPartNames) throws IOException, XMLStreamException {
            List<? extends ZipEntry> entryList = list(zipFile.entries());
            entryList.sort(new ZipEntryComparator(PartNames.getPartNames(designMap, storyPartNames)));

            return enumeration(entryList);
        }

        private Event getStartDocumentEvent(URI uri, LocaleId sourceLocale, IFilterWriter filterWriter) {
            StartDocument startDoc = new StartDocument(startDocumentId);
            startDoc.setName(uri.getPath());
            startDoc.setLocale(sourceLocale);
            startDoc.setMimeType(IDMLFilter.MIME_TYPE);
            startDoc.setFilterWriter(filterWriter);
            startDoc.setFilterId(IDMLFilter.FILTER_ID);
            startDoc.setFilterParameters(parameters);
            startDoc.setLineBreak(lineBreak);
            startDoc.setEncoding(encoding, false);  // IDML files don't have UTF8BOM
            ZipSkeleton skel = new ZipSkeleton(zipFile, null);

            return new Event(EventType.START_DOCUMENT, startDoc, skel);
        }

        @Override
        public boolean hasNextSubDocument() {
            return zipFileEntries.hasMoreElements();
        }

        @Override
        public SubDocument nextSubDocument() {
            final ZipEntry zipEntry = zipFileEntries.nextElement();

            if (!isTranslatableSubDocument(zipEntry.getName())) {
                if (isStylesSubDocument(zipEntry.getName())) {
                    return new MarkupModifiableSubDocument(
                        this.zipFile,
                        zipEntry,
                        this.outputFactory,
                        this.encoding,
                        this.styleDefinitions
                    );
                }
                return new NonModifiableSubDocument(zipFile, zipEntry);
            }

            return new StorySubDocument(
                this.parameters,
                this.zipInputReader,
                this.eventFactory,
                this.zipFile,
                zipEntry,
                this.startDocumentId,
                String.valueOf(++this.currentSubDocumentId)
            );
        }

        private boolean isTranslatableSubDocument(final String partName) {
            return !nonTranslatableSubDocuments.contains(partName);
        }

        private boolean isStylesSubDocument(final String entryName) {
            return this.designMap.getStylesPartName().equals(entryName);
        }

        @Override
        public void close() throws IOException {
            zipFile.close();
        }

        private static class PartNames {
            private static final String MIME_TYPE = "mimetype";
            private static final String DESIGN_MAP = "designmap.xml";
            private static final String CONTAINER = "META-INF/container.xml";
            private static final String METADATA = "META-INF/metadata.xml";

            static List<String> getPartNames(DesignMap designMap, List<String> storyPartNames) {
                List<String> partNames = new ArrayList<>(Arrays.asList(MIME_TYPE, DESIGN_MAP, CONTAINER, METADATA));

                partNames.add(designMap.getGraphicPartName());
                partNames.add(designMap.getFontsPartName());
                partNames.add(designMap.getStylesPartName());
                partNames.add(designMap.getPreferencesPartName());
                partNames.add(designMap.getTagsPartName());
                partNames.add(designMap.getMappingPartName());
                partNames.addAll(designMap.getMasterSpreadPartNames());
                partNames.addAll(designMap.getSpreadPartNames());
                partNames.add(designMap.getBackingStoryPartName());
                partNames.addAll(storyPartNames);

                return partNames;
            }
        }

        private static class ZipEntryComparator implements Comparator<ZipEntry> {
            private List<String> partNames;

            ZipEntryComparator(List<String> partNames) {
                this.partNames = partNames;
            }

            @Override
            public int compare(ZipEntry o1, ZipEntry o2) {
                int index1 = partNames.indexOf(o1.getName());
                int index2 = partNames.indexOf(o2.getName());

                if (index1 == -1) {
                    index1 = Integer.MAX_VALUE;
                }

                if (index2 == -1) {
                    index2 = Integer.MAX_VALUE;
                }

                return Integer.compare(index1, index2);
            }
        }
    }
}
