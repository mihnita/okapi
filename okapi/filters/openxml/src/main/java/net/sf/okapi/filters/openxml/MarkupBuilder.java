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

import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

final class MarkupBuilder {
    private final Markup markup;
    private List<XMLEvent> currentMarkupComponentEvents;

    MarkupBuilder(final Markup markup) {
        this.markup = markup;
        this.currentMarkupComponentEvents = new ArrayList<>();
    }

    void add(final XMLEvent event) {
        this.currentMarkupComponentEvents.add(event);
    }

    void add(MarkupComponent markupComponent) {
        if (!this.currentMarkupComponentEvents.isEmpty()) {
            this.markup.addComponent(
                MarkupComponentFactory.createGeneralMarkupComponent(
                    this.currentMarkupComponentEvents
                )
            );
            this.currentMarkupComponentEvents = new ArrayList<>();
        }
        this.markup.addComponent(markupComponent);
    }

    XMLEvent peekCurrentMarkupComponentEvent() {
        if (this.currentMarkupComponentEvents.isEmpty()) {
            return null;
        }
        return this.currentMarkupComponentEvents.get(
            this.currentMarkupComponentEvents.size() - 1
        );
    }

    Markup build() {
        if (!this.currentMarkupComponentEvents.isEmpty()) {
            this.markup.addComponent(
                MarkupComponentFactory.createGeneralMarkupComponent(
                    this.currentMarkupComponentEvents
                )
            );
            this.currentMarkupComponentEvents = new ArrayList<>();
        }
        return this.markup;
    }
}
