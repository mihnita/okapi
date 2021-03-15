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
import javax.xml.stream.events.StartElement;
import java.util.EnumSet;

/**
 * Provides schema definitions.
 */
final class SchemaDefinitions {
    private static final QName DEFAULT_NAME = Namespaces.Empty.getQName("");
    private static final Namespaces defaultNamespace = Namespaces.DrawingML;
    private static final String PREFIX = Namespace.PREFIX_A;

    private static final EnumSet<TextParagraphPropertiesName> textParagraphPropertiesNames = EnumSet.range(
        TextParagraphPropertiesName.PARAGRAPH_PROPERTIES,
        TextParagraphPropertiesName.LEVEL_9_PARAGRAPH_PROPERTIES
    );

    private static final EnumSet<TextCharacterPropertiesName> textCharacterPropertiesNames = EnumSet.range(
            TextCharacterPropertiesName.DEFAULT_TEXT_RUN_PROPERTIES,
            TextCharacterPropertiesName.RUN_PROPERTIES
    );

    static SchemaDefinition.Component of(final StartElement startElement) {
        final SchemaDefinition.Component component;
        if (textParagraphPropertiesNames.contains(TextParagraphPropertiesName.fromQName(startElement.getName()))) {
            component = new SchemaDefinition.TextParagraphProperties(startElement.getName());
        } else if (textCharacterPropertiesNames.contains(TextCharacterPropertiesName.fromQName(startElement.getName()))) {
            component = new SchemaDefinition.TextCharacterProperties(startElement.getName());
        } else {
            component = new SchemaDefinition.Element(SchemaDefinitions.DEFAULT_NAME);
        }
        return component;
    }

    private enum TextParagraphPropertiesName {
        PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("pPr", SchemaDefinitions.PREFIX)),
        DEFAULT_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("defPPr", SchemaDefinitions.PREFIX)),
        LEVEL_1_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl1pPr", SchemaDefinitions.PREFIX)),
        LEVEL_2_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl2pPr", SchemaDefinitions.PREFIX)),
        LEVEL_3_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl3pPr", SchemaDefinitions.PREFIX)),
        LEVEL_4_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl4pPr", SchemaDefinitions.PREFIX)),
        LEVEL_5_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl5pPr", SchemaDefinitions.PREFIX)),
        LEVEL_6_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl6pPr", SchemaDefinitions.PREFIX)),
        LEVEL_7_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl7pPr", SchemaDefinitions.PREFIX)),
        LEVEL_8_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl8pPr", SchemaDefinitions.PREFIX)),
        LEVEL_9_PARAGRAPH_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("lvl9pPr", SchemaDefinitions.PREFIX)),
        UNSUPPORTED(SchemaDefinitions.DEFAULT_NAME);

        private final QName name;

        TextParagraphPropertiesName(final QName name) {
            this.name = name;
        }

        static TextParagraphPropertiesName fromQName(final QName name) {
            for (final TextParagraphPropertiesName textParagraphPropertiesName : values()) {
                if (textParagraphPropertiesName.name.equals(name)) {
                    return textParagraphPropertiesName;
                }
            }
            return UNSUPPORTED;
        }

        @Override
        public String toString() {
            return Names.toString(this.name);
        }
    }

    private enum TextCharacterPropertiesName {
        DEFAULT_TEXT_RUN_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("defRPr", SchemaDefinitions.PREFIX)),
        END_PARAGRAPH_RUN_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("endParaRPr", SchemaDefinitions.PREFIX)),
        RUN_PROPERTIES(SchemaDefinitions.defaultNamespace.getQName("rPr", SchemaDefinitions.PREFIX)),
        UNSUPPORTED(SchemaDefinitions.DEFAULT_NAME);

        private final QName name;

        TextCharacterPropertiesName(final QName name) {
            this.name = name;
        }

        static TextCharacterPropertiesName fromQName(final QName name) {
            for (final TextCharacterPropertiesName textCharacterPropertiesName : values()) {
                if (textCharacterPropertiesName.name.equals(name)) {
                    return textCharacterPropertiesName;
                }
            }
            return UNSUPPORTED;
        }

        @Override
        public String toString() {
            return Names.toString(this.name);
        }
    }

    private static class Names {
        static String toString(final QName name) {
            final StringBuilder builder = new StringBuilder();
            if (!name.getPrefix().isEmpty()) {
                builder.append(name.getPrefix()).append(":");
            }
            builder.append(name.getLocalPart());
            return builder.toString();
        }
    }
}
