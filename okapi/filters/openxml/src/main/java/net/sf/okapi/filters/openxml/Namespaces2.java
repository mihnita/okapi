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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

interface Namespaces2 {
    void readWith(final Reader reader) throws XMLStreamException;
    void readFrom(final StartElement startElement);
    Namespace forPrefix(final String prefix);

    class Default implements Namespaces2 {
        private final XMLInputFactory inputFactory;
        private Map<String, Namespace> namespaces;

        Default(final XMLInputFactory inputFactory) {
            this.inputFactory = inputFactory;
        }

        @Override
        public void readWith(final Reader reader) throws XMLStreamException {
            final XMLEventReader eventReader = this.inputFactory.createXMLEventReader(reader);
            while (eventReader.hasNext()) {
                final XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    readFrom(event.asStartElement());
                    return;
                }
            }
            this.namespaces = new HashMap<>();
        }

        @Override
        public void readFrom(final StartElement startElement) {
            this.namespaces = new HashMap<>();
            final Iterator iterator = startElement.getNamespaces();
            while (iterator.hasNext()) {
                final javax.xml.stream.events.Namespace namespace = (javax.xml.stream.events.Namespace) iterator.next();
                namespaces.put(
                    namespace.getPrefix(),
                    new Namespace.Default(namespace.getPrefix(), namespace.getNamespaceURI())
                );
            }
        }

        @Override
        public Namespace forPrefix(final String prefix) {
            return this.namespaces.get(prefix);
        }
    }
}
