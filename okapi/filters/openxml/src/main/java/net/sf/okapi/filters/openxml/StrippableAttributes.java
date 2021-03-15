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

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides strippable attributes.
 */
interface StrippableAttributes {
    StartElement strip(final StartElement startElement);

    final class Default implements StrippableAttributes {
        private final Set<String> names;
        private final XMLEventFactory eventFactory;

        Default (final XMLEventFactory eventFactory) {
            this(new HashSet<>(), eventFactory);
        }

        Default(final Set<String> names, final XMLEventFactory eventFactory) {
            this.names = names;
            this.eventFactory = eventFactory;
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            final List<Attribute> attributes = new ArrayList<>();
            final Iterator currentAttributesIterator = startElement.getAttributes();
            while (currentAttributesIterator.hasNext()) {
                final Attribute attribute = (Attribute) currentAttributesIterator.next();
                if (!this.names.contains(attribute.getName().getLocalPart())) {
                    attributes.add(attribute);
                }
            }
            return this.eventFactory.createStartElement(
                startElement.getName(),
                attributes.iterator(),
                startElement.getNamespaces()
            );
        }
    }

    final class DrawingRunProperties implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        DrawingRunProperties(
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory
        ) {
            this(
                new Default(eventFactory),
                conditionalParameters
            );
        }

        DrawingRunProperties(
            final StrippableAttributes.Default defaultStrippableAttributes,
            final ConditionalParameters conditionalParameters
        ) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            final Stream<String> unconditional = Stream.of(
                Name.RunProperty.SPELLING_ERROR.value(),
                Name.RunProperty.NO_PROOFING.value(),
                Name.RunProperty.DIRTY.value(),
                Name.RunProperty.SMART_TAG_CLEAN.value(),
                Name.RunProperty.LANG.value(),
                Name.RunProperty.ALT_LANG.value()
            );
            final Stream<String> merged;
            if (conditionalParameters.getCleanupAggressively()) {
                merged = Stream.concat(unconditional, Stream.of(Name.RunProperty.SPACING.value()));
            } else {
                merged = unconditional;
            }
            this.defaultStrippableAttributes.names.addAll(merged.collect(Collectors.toSet()));
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    final class DrawingDirection implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        DrawingDirection(final XMLEventFactory eventFactory) {
            this(new Default(eventFactory));
        }

        DrawingDirection(final StrippableAttributes.Default defaultStrippableAttributes) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            this.defaultStrippableAttributes.names.add(
                Name.Direction.RTL.value()
            );
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    final class DrawingBodyDirection implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        DrawingBodyDirection(final XMLEventFactory eventFactory) {
            this(new Default(eventFactory));
        }

        DrawingBodyDirection(final StrippableAttributes.Default defaultStrippableAttributes) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            this.defaultStrippableAttributes.names.add(
                Name.Direction.RTL_COL.value()
            );
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    final class WordParagraphRevisions implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        WordParagraphRevisions(final XMLEventFactory eventFactory) {
            this(new Default(eventFactory));
        }

        WordParagraphRevisions(final StrippableAttributes.Default defaultStrippableAttributes) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            this.defaultStrippableAttributes.names.addAll(
                Stream.of(
                    Name.Revision.RPR.value(),
                    Name.Revision.DEL.value(),
                    Name.Revision.R.value(),
                    Name.Revision.P.value(),
                    Name.Revision.R_DEFAULT.value()
                ).collect(Collectors.toSet())
            );
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    final class WordRunRevisions implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        WordRunRevisions(final XMLEventFactory eventFactory) {
            this(new Default(eventFactory));
        }

        WordRunRevisions(final Default defaultStrippableAttributes) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            this.defaultStrippableAttributes.names.addAll(
                Stream.of(
                    Name.Revision.RPR.value(),
                    Name.Revision.DEL.value(),
                    Name.Revision.R.value()
                ).collect(Collectors.toSet())
            );
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    final class WordTableRowRevisions implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        WordTableRowRevisions(final XMLEventFactory eventFactory) {
            this(new Default(eventFactory));
        }

        WordTableRowRevisions(final Default defaultStrippableAttributes) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            this.defaultStrippableAttributes.names.addAll(
                Stream.of(
                    Name.Revision.RPR.value(),
                    Name.Revision.DEL.value(),
                    Name.Revision.R.value(),
                    Name.Revision.TR.value()
                ).collect(Collectors.toSet())
            );
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    final class WordSectionPropertiesRevisions implements StrippableAttributes {
        private final StrippableAttributes.Default defaultStrippableAttributes;

        WordSectionPropertiesRevisions(final XMLEventFactory eventFactory) {
            this(new Default(eventFactory));
        }

        WordSectionPropertiesRevisions(final Default defaultStrippableAttributes) {
            this.defaultStrippableAttributes = defaultStrippableAttributes;
            this.defaultStrippableAttributes.names.addAll(
                Stream.of(
                    Name.Revision.RPR.value(),
                    Name.Revision.DEL.value(),
                    Name.Revision.R.value(),
                    Name.Revision.SECT.value()
                ).collect(Collectors.toSet())
            );
        }

        @Override
        public StartElement strip(final StartElement startElement) {
            return this.defaultStrippableAttributes.strip(startElement);
        }
    }

    /**
     * Provides strippable attributes names.
     */
    interface Name {
        String value();

        /**
         * Provides run property names enumeration.
         */
        enum RunProperty implements Name {
            SPELLING_ERROR("err"),
            NO_PROOFING("noProof"),
            DIRTY("dirty"),
            SMART_TAG_CLEAN("smtClean"),
            LANG(XMLEventHelpers.LOCAL_PROPERTY_LANGUAGE),
            ALT_LANG("altLang"),
            SPACING("spc");

            private final String value;

            RunProperty(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }

        /**
         * Provides direction names enumeration.
         */
        enum Direction implements Name {
            RTL(XMLEventHelpers.LOCAL_RTL),
            RTL_COL(XMLEventHelpers.LOCAL_RTL_COL);

            private final String value;

            Direction(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }

        /**
         * Provides a revision names enumeration.
         */
        enum Revision implements Name {
            RPR("rsidRPr"),
            DEL("rsidDel"),
            R("rsidR"),
            SECT("rsidSect"),
            P("rsidP"),
            R_DEFAULT("rsidRDefault"),
            TR("rsidTr");

            private final String value;

            Revision(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }
}
