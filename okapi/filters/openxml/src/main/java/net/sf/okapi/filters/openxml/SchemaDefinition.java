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

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Provides XML schema definition.
 */
final class SchemaDefinition {
    private static final String COMPONENT_NAME_DOES_NOT_EXIST = "The component name does not exist";

    enum Composition {
        SEQUENCE("Sequence"),
        CHOICE("Choice"),
        ALL("All"),
        NONE("");

        private final String type;

        Composition(final String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }

    interface Component {

        QName name();

        Composition composition();

        ListIterator<Component> listIterator();

        default ListIterator<Component> listIteratorAfter(final QName name) {
            final ListIterator<Component> iterator = this.listIterator();
            boolean found = false;
            while (iterator.hasNext()) {
                final Component component = iterator.next();
                if (component.name().equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(COMPONENT_NAME_DOES_NOT_EXIST);
            }
            return iterator;
        }
    }

    static final class Element implements Component {
        private final QName name;

        Element(final QName name) {
            this.name = name;
        }

        @Override
        public QName name() {
            return this.name;
        }

        @Override
        public Composition composition() {
            return Composition.NONE;
        }

        @Override
        public ListIterator<Component> listIterator() {
            return java.util.Collections.emptyListIterator();
        }
    }

    static final class Group implements Component {
        private static final QName DEFAULT_NAME = new QName("");

        private final QName name;
        private final Composition composition;
        private final List<Component> components;

        Group(final QName name, final Composition composition, final Component... components) {
            this.name = name;
            this.composition = composition;
            this.components = Arrays.asList(components);
        }

        @Override
        public QName name() {
            return this.name;
        }

        @Override
        public Composition composition() {
            return this.composition;
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.components.listIterator();
        }
    }

    static final class FillProperties implements Component {
        private final Group group;

        FillProperties(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "noFill", prefix)),
                new Element(new QName(namespaceUri, "solidFill", prefix)),
                new Element(new QName(namespaceUri, "gradFill", prefix)),
                new Element(new QName(namespaceUri, "blipFill", prefix)),
                new Element(new QName(namespaceUri, "pattFill", prefix)),
                new Element(new QName(namespaceUri, "grpFill", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class EffectProperties implements Component {
        private final Group group;

        EffectProperties(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "effectLst", prefix)),
                new Element(new QName(namespaceUri, "effectDag", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextUnderlineLine implements Component {
        private final Group group;

        TextUnderlineLine(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "uLnTx", prefix)),
                new Element(new QName(namespaceUri,"uLn", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextUnderlineFill implements Component {
        private final Group group;

        TextUnderlineFill(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "uFillTx", prefix)),
                new Element(new QName(namespaceUri, "uFill", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextBulletColor implements Component {
        private final Group group;

        TextBulletColor(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "buClrTx", prefix)),
                new Element(new QName(namespaceUri, "buClr", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextBulletSize implements Component {
        private final Group group;

        TextBulletSize(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "buSzTx", prefix)),
                new Element(new QName(namespaceUri, "buSzPct", prefix)),
                new Element(new QName(namespaceUri, "buSzPsts", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextBulletTypeface implements Component {
        private final Group group;

        TextBulletTypeface(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "buFontTx", prefix)),
                new Element(new QName(namespaceUri, "buFont", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextBullet implements Component {
        private final Group group;

        TextBullet(final String namespaceUri, final String prefix) {
            this.group = new Group(
                Group.DEFAULT_NAME,
                Composition.CHOICE,
                new Element(new QName(namespaceUri, "buNone", prefix)),
                new Element(new QName(namespaceUri, "buAutoNum", prefix)),
                new Element(new QName(namespaceUri, "buChar ", prefix)),
                new Element(new QName(namespaceUri, "buBlip ", prefix))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextCharacterProperties implements Component {
        private final Group group;

        TextCharacterProperties(final QName name) {
            this.group = new Group(
                name,
                Composition.SEQUENCE,
                new Element(new QName(name.getNamespaceURI(), "ln", name.getPrefix())),
                new FillProperties(name.getNamespaceURI(), name.getPrefix()),
                new EffectProperties(name.getNamespaceURI(), name.getPrefix()),
                new Element(new QName(name.getNamespaceURI(), "highlight", name.getPrefix())),
                new TextUnderlineLine(name.getNamespaceURI(), name.getPrefix()),
                new TextUnderlineFill(name.getNamespaceURI(), name.getPrefix()),
                new Element(new QName(name.getNamespaceURI(), "latin", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "ea", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "cs", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "sym", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "hlinkClick", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "hlinkMouseOver", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "rtl", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "extList", name.getPrefix()))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return this.group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }

    static final class TextParagraphProperties implements Component {
        private final Group group;

        TextParagraphProperties(final QName name) {
            this.group = new Group(
                name,
                Composition.SEQUENCE,
                new Element(new QName(name.getNamespaceURI(), "lnSpc", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "spcBef", name.getPrefix())),
                new Element(new QName(name.getNamespaceURI(), "spcAlt", name.getPrefix())),
                new TextBulletColor(name.getNamespaceURI(), name.getPrefix()),
                new TextBulletSize(name.getNamespaceURI(), name.getPrefix()),
                new TextBulletTypeface(name.getNamespaceURI(), name.getPrefix()),
                new TextBullet(name.getNamespaceURI(), name.getPrefix()),
                new Element(new QName(name.getNamespaceURI(), "tabLst", name.getPrefix())),
                new TextCharacterProperties(
                    new QName(name.getNamespaceURI(), "defRPr", name.getPrefix())
                ),
                new Element(new QName(name.getNamespaceURI(), "extLst", name.getPrefix()))
            );
        }

        @Override
        public QName name() {
            return this.group.name();
        }

        @Override
        public Composition composition() {
            return group.composition();
        }

        @Override
        public ListIterator<Component> listIterator() {
            return this.group.listIterator();
        }
    }
}
