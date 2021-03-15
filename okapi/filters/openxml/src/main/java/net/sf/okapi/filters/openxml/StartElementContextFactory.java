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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import net.sf.okapi.common.LocaleId;

/**
 * Provides a start element context factory.
 */
class StartElementContextFactory {

    static StartElementContext createStartElementContext(
        final StartElement startElement,
        final StartElement parentStartElement,
        final XMLEventReader eventReader,
        final XMLEventFactory eventFactory,
        final ConditionalParameters conditionalParameters,
        final LocaleId sourceLanguage
    ) {
        return new StartElementContext(
            startElement,
            parentStartElement,
            eventReader,
            eventFactory,
            conditionalParameters,
            sourceLanguage
        );
    }

    static StartElementContext createStartElementContext(
        final StartElement startElement,
        final StartElement parentStartElement,
        final XMLEventReader eventReader,
        final XMLEventFactory eventFactory,
        final ConditionalParameters conditionalParameters
    ) {
        return new StartElementContext(
            startElement,
            parentStartElement,
            eventReader,
            eventFactory,
            conditionalParameters,
            null
        );
    }

    static StartElementContext createStartElementContext(
        final StartElement startElement,
        final XMLEventReader eventReader,
        final XMLEventFactory eventFactory,
        final ConditionalParameters conditionalParameters
    ) {
        return createStartElementContext(
            startElement,
            null,
            eventReader,
            eventFactory,
            conditionalParameters,
            null
        );
    }

    static StartElementContext createStartElementContext(
        final StartElement startElement,
        final XMLEventReader eventReader,
        final XMLEventFactory eventFactory,
        final ConditionalParameters conditionalParameters,
        final LocaleId sourceLanguage
    ) {
        return createStartElementContext(
            startElement,
            null,
            eventReader,
            eventFactory,
            conditionalParameters,
            sourceLanguage
        );
    }

    static StartElementContext createStartElementContext(
        final StartElement startElement,
        final StartElementContext startElementContext
    ) {
        return createStartElementContext(
            startElement,
            startElementContext.getStartElement(),
            startElementContext.getEventReader(),
            startElementContext.getEventFactory(),
            startElementContext.getConditionalParameters(),
            startElementContext.getSourceLanguage()
        );
    }
}
