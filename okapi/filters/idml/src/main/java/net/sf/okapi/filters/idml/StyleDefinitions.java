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

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;

interface StyleDefinitions extends Markup {
    void from(final ZipEntry entry) throws IOException, XMLStreamException;

    class Default implements StyleDefinitions {
        private final Markup.Default defaultMarkup;
        private final ZipInput<XMLEventReader> zipInputReader;
        private final XMLEventFactory eventFactory;

        Default(
            final Markup.Default defaultMarkup,
            final ZipInput<XMLEventReader> zipInputReader,
            final XMLEventFactory eventFactory
        ) {
            this.defaultMarkup = defaultMarkup;
            this.zipInputReader = zipInputReader;
            this.eventFactory = eventFactory;
        }

        @Override
        public void from(final ZipEntry entry) throws IOException, XMLStreamException {
            XMLEventReader eventReader = null;
            try {
                eventReader = this.zipInputReader.of(entry);
                final StyleDefinitionsReader reader = new StyleDefinitionsReader.Cached(
                    new StyleDefinitionsReader.Default(
                        eventReader,
                        this.eventFactory
                    )
                );
                this.defaultMarkup.add(new MarkupRange.Start(reader.startDocument()));
                this.defaultMarkup.add(new MarkupRange.Start(reader.startElement()));
                this.defaultMarkup.add(reader.rootCharacterStyleGroup());
                this.defaultMarkup.add(reader.rootParagraphStyleGroup());
                reader.tablesOfContents().forEach(r -> this.defaultMarkup.add(r));
                this.defaultMarkup.add(reader.rootCellStyleGroup());
                this.defaultMarkup.add(reader.rootTableStyleGroup());
                this.defaultMarkup.add(reader.rootObjectStyleGroup());
                reader.trapPresets().forEach(r -> this.defaultMarkup.add(r));
                this.defaultMarkup.add(new MarkupRange.End(reader.endElement()));
                this.defaultMarkup.add(new MarkupRange.End(reader.endDocument()));
            } finally {
                if (null != eventReader) {
                    eventReader.close();
                }
            }
        }

        @Override
        public void add(final MarkupRange markupRange) {
            this.defaultMarkup.add(markupRange);
        }

        @Override
        public Iterator<MarkupRange> iterator() {
            return this.defaultMarkup.iterator();
        }

        @Override
        public void apply(final FontMappings fontMappings) {
            this.defaultMarkup.apply(fontMappings);
        }

        @Override
        public List<XMLEvent> getEvents() {
            return this.defaultMarkup.getEvents();
        }
    }
}
