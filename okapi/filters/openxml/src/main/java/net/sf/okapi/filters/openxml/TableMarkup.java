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
package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.filters.fontmappings.FontMappings;

import javax.xml.stream.events.XMLEvent;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

final class TableMarkup implements Markup {
    private final Markup markup;

    TableMarkup(final Markup markup) {
        this.markup = markup;
    }

    @Override
    public void addComponent(final MarkupComponent component) {
        this.markup.addComponent(component);
    }

    @Override
    public void addComponents(final List<MarkupComponent> components) {
        this.markup.addComponents(components);
    }

    @Override
    public void addMarkup(final Markup markup) {
        this.markup.addMarkup(markup);
    }

    @Override
    public List<MarkupComponent> components() {
        return this.markup.components();
    }

    @Override
    public Nameable nameableComponent() {
        return this.markup.nameableComponent();
    }

    @Override
    public void apply(final FontMappings fontMappings) {
        this.markup.apply(fontMappings);
    }

    @Override
    public List<XMLEvent> getEvents() {
        return this.markup.getEvents();
    }

    boolean empty() {
        return this.markup.components().isEmpty();
    }

    Iterator<MarkupComponent> componentsIteratorAtLastWith(final String name) {
        if (empty()) {
            throw new UnsupportedOperationException("The table markup components are empty");
        }
        final ListIterator<MarkupComponent> iterator =
            this.markup.components().listIterator(this.markup.components().size() - 1);
        while (iterator.hasPrevious()) {
            final MarkupComponent mc = iterator.previous();
            if (mc instanceof Nameable && ((Nameable) mc).getName().getLocalPart().equals(name)) {
                return iterator;
            }
        }
        throw new IllegalArgumentException(
            String.format(
                "The requested table markup component with '%s' name could not be found",
                name
            )
        );
    }

    void removeComponents() {
        this.markup.components().clear();
    }

    void removeComponentsFromLastWith(final String name) {
        if (empty()) {
            return;
        }
        removeComponentsWith(
            componentsIteratorAtLastWith(name)
        );
    }

    void removeComponentsWith(final Iterator<MarkupComponent> iterator) {
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    boolean lastComponentGeneral() {
        if (empty()) {
            return false;
        }
        return this.markup.components().get(this.markup.components().size() - 1) instanceof MarkupComponent.General;
    }

    void addToLastComponent(final XMLEvent event) {
        this.markup.components().get(this.markup.components().size() - 1).getEvents().add(event);
    }
}
