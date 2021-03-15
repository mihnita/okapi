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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;

final class RunSkippableElements {

    private final StartElementContext startElementContext;
    private final SkippableElements lastRenderedPageBreakSkippableElements;
    private final SkippableElements softHyphenSkippableElements;
    private final SkippableElements alternateContentFallbackSkippableElements;
    private final SkippableElements runPropertiesSkippableElements;

    RunSkippableElements(StartElementContext startElementContext) {
        this.startElementContext = startElementContext;

        this.lastRenderedPageBreakSkippableElements = new SkippableElements.Inline(
            new SkippableElements.Default(
                SkippableElement.GeneralInline.LAST_RENDERED_PAGE_BREAK
            )
        );
        this.softHyphenSkippableElements = new SkippableElements.Inline(
            new SkippableElements.Default(
                SkippableElement.GeneralInline.SOFT_HYPHEN
            )
        );
        this.alternateContentFallbackSkippableElements = new SkippableElements.Inline(
            new SkippableElements.Default(
                SkippableElement.GeneralInline.ALTERNATE_CONTENT_FALLBACK
            )
        );
        this.runPropertiesSkippableElements = new SkippableElements.RevisionProperty(
            new SkippableElements.Property(
                new SkippableElements.Default(
                    SkippableElement.RunProperty.RUN_PROPERTY_RTL_DML,
                    SkippableElement.RunProperty.RUN_PROPERTY_LANGUAGE,
                    SkippableElement.RunProperty.RUN_PROPERTY_NO_SPELLING_OR_GRAMMAR,
                    SkippableElement.RevisionProperty.RUN_PROPERTIES_CHANGE
                ),
                startElementContext.getConditionalParameters()
            ),
            startElementContext.getConditionalParameters()
        );
    }

    boolean skip(final XMLEvent e) throws XMLStreamException {
        if (e.isStartElement() && lastRenderedPageBreakSkippableElements.canBeSkipped(e.asStartElement(), startElementContext.getStartElement())) {
            lastRenderedPageBreakSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext.getEventReader(), null, startElementContext.getConditionalParameters()));
            return true;
        } else if (startElementContext.getConditionalParameters().getIgnoreSoftHyphenTag()
                && e.isStartElement() && softHyphenSkippableElements.canBeSkipped(e.asStartElement(), startElementContext.getStartElement())) {
            // Ignore soft hyphens
            softHyphenSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext.getEventReader(), null, startElementContext.getConditionalParameters()));
            return true;
        } else if (e.isStartElement() && alternateContentFallbackSkippableElements.canBeSkipped(e.asStartElement(), startElementContext.getStartElement())) {
            alternateContentFallbackSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext.getEventReader(), null, startElementContext.getConditionalParameters()));
            return true;
        } else if (skipProperties(e, startElementContext)) {
            return true;
        }
        return false;
    }

    boolean skipProperties(final XMLEvent e, final StartElementContext startElementContext) throws XMLStreamException {
        if (e.isStartElement() && runPropertiesSkippableElements.canBeSkipped(e.asStartElement(), startElementContext.getStartElement())) {
            runPropertiesSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext));
            return true;
        }
        return false;
    }
}
