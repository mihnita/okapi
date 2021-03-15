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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.DocumentPart;

import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.List;

class DocumentPartEventBuilder implements Builder<Event> {
    private final IdGenerator documentPartIdGenerator;
    private final Markup markup;

    DocumentPartEventBuilder(
        final IdGenerator documentPartIdGenerator,
        final Markup markup
    ) {
        this.documentPartIdGenerator = documentPartIdGenerator;
        this.markup = markup;
    }

    void addMarkupRange(List<XMLEvent> events) {
        this.markup.add(new MarkupRange.Default(events));
    }

    void addMarkupRangeStartElement(StartElement startElement) {
        this.markup.add(new MarkupRange.Start(startElement));
    }

    void addMarkupRangeEndElement(EndElement endElement) {
        this.markup.add(new MarkupRange.End(endElement));
    }

    void addMarkupRangeElement(StoryChildElement storyChildElement) {
        this.markup.add(storyChildElement);
    }

    @Override
    public Event build() {
        final DocumentPart documentPart = new DocumentPart(documentPartIdGenerator.createId(), false);
        documentPart.setSkeleton(new MarkupSkeleton(this.markup));
        return new Event(EventType.DOCUMENT_PART, documentPart);
    }
}
