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

import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides a markup.
 */
interface Markup extends XMLEvents {
    void addComponent(final MarkupComponent component);
    void addComponents(final List<MarkupComponent> components);
    void addMarkup(final Markup markup);
    List<MarkupComponent> components();
    Nameable nameableComponent();
    void apply(final FontMappings fontMappings);

    class Empty implements Markup {
        @Override
        public void addComponent(MarkupComponent component) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addComponents(List<MarkupComponent> components) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addMarkup(Markup markup) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<MarkupComponent> components() {
            return Collections.emptyList();
        }

        @Override
        public Nameable nameableComponent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void apply(final FontMappings fontMappings) {
        }

        @Override
        public List<XMLEvent> getEvents() {
            return Collections.emptyList();
        }
    }

    class General implements Markup {
        private final List<MarkupComponent> components;

        General(final List<MarkupComponent> components) {
            this.components = components;
        }

        @Override
        public void addComponent(final MarkupComponent component) {
            this.components.add(component);
        }

        @Override
        public void addComponents(final List<MarkupComponent> components) {
            this.components.addAll(components);
        }

        @Override
        public void addMarkup(final Markup markup) {
            this.components.addAll(markup.components());
        }

        @Override
        public List<MarkupComponent> components() {
            return this.components;
        }

        @Override
        public void apply(final FontMappings fontMappings) {
            this.components.forEach(c -> c.apply(fontMappings));
        }

        @Override
        public Nameable nameableComponent() {
            for (MarkupComponent markupComponent : this.components) {
                if (markupComponent instanceof Nameable) {
                    return (Nameable) markupComponent;
                }
            }

            return null;
        }

        @Override
        public List<XMLEvent> getEvents() {
            List<XMLEvent> events = new ArrayList<>();

            for (MarkupComponent component : this.components) {
                events.addAll(component.getEvents());
            }

            return events;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + this.components.size() + ") " + components;
        }
    }
}
