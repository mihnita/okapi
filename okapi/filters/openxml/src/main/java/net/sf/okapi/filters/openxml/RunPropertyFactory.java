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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.gatherEvents;

/**
 * Provides a run property factory.
 */
class RunPropertyFactory {

    private static final int DEFAULT_EVENTS_SIZE = 2;

    /**
     * WordprocessingML toggle property names.
     */
    private static final EnumSet<WpmlTogglePropertyName> WPML_TOGGLE_PROPERTY_NAMES =
        EnumSet.complementOf(EnumSet.of(WpmlTogglePropertyName.UNSUPPORTED));

    /**
     * DrawingML boolean property names.
     */
    private static final Set<String> DML_BOOLEAN_PROPERTY_NAMES =
        new HashSet<>(
            Arrays.asList(
                "b", "dirty", "err", "i", "kumimoji", "noProof", "normalizeH", "smtClean"
            )
        );

    private static final Set<String> DML_HYPERLINK_NAMES =
            new HashSet<>(Arrays.asList("hlinkClick", "hlinkMouseOver"));

    RunPropertyFactory() {
    }

    /**
     * Creates a run property.
     *
     * @param startElementContext   Contains XML event factory, XML event reader and StartElement
     *
     * @return A created run property
     *
     * @throws XMLStreamException
     */
    static RunProperty createRunProperty(StartElementContext startElementContext) throws XMLStreamException {
        final QName startElementName = startElementContext.getStartElement().getName();
        switch (startElementName.getPrefix()) {
            case Namespace.PREFIX_A:
                if (DML_HYPERLINK_NAMES.contains(startElementName.getLocalPart())) {
                    return new RunProperty.HyperlinkRunProperty(gatherEvents(startElementContext));
                } else if (RunProperty.FontRunProperty.DML_NAMES.contains(startElementName.getLocalPart())) {
                    return new RunProperty.FontRunProperty(
                        gatherEvents(startElementContext),
                        startElementContext.getEventFactory()
                    );
                }
                break;
            case Namespace.PREFIX_W:
                if (RunFonts.NAME.equals(startElementName.getLocalPart())) {
                    return new RunProperty.FontsRunProperty(RunFonts.createRunFonts(startElementContext));
                } else if (RunProperty.StyleRunProperty.NAME.equals(startElementName.getLocalPart())) {
                    return new RunProperty.StyleRunProperty(gatherEvents(startElementContext));
                } else if (WPML_TOGGLE_PROPERTY_NAMES.contains(WpmlTogglePropertyName.fromString(startElementName.getLocalPart()))) {
                    return new RunProperty.WpmlToggleRunProperty(gatherEvents(startElementContext));
                } else if (RunProperty.HighlightRunProperty.NAME.equals(startElementName.getLocalPart())) {
                    return new RunProperty.HighlightRunProperty(gatherEvents(startElementContext));
                } else if (RunProperty.ColorRunProperty.NAME.equals(startElementName.getLocalPart())) {
                    return new RunProperty.ColorRunProperty(gatherEvents(startElementContext));
                } else if (RunProperty.ShadeRunProperty.NAME.equals(startElementName.getLocalPart())) {
                    return new RunProperty.ShadeRunProperty(gatherEvents(startElementContext));
                }
                break;
            case Namespace.PREFIX_EMPTY:
                return new RunProperty.SmlRunProperty(gatherEvents(startElementContext));
        }
        return new RunProperty.GenericRunProperty(gatherEvents(startElementContext));
    }

    /**
     * Creates a run property.
     *
     * @param attribute An attribute
     *
     * @return A created run property
     */
    static RunProperty createRunProperty(Attribute attribute) {
        return createRunProperty(attribute.getName(), attribute.getValue());
    }

    /**
     * Creates a run property.
     *
     * @param name A name
     * @param name A value
     *
     * @return A created run property
     */
    static RunProperty createRunProperty(QName name, String value) {
        if (DML_BOOLEAN_PROPERTY_NAMES.contains(name.getLocalPart())) {
            return new RunProperty.BooleanAttributeRunProperty(name, value);
        }
        return new RunProperty.AttributeRunProperty(name, value);
    }

    /**
     * Creates a run property.
     *
     * @param creationalParameters Creational parameters
     * @param localName            A local name
     * @param attributes           Attributes
     *
     * @return A created run property
     */
    static RunProperty createRunProperty(CreationalParameters creationalParameters, String localName, Map<String, String> attributes) {
        List<XMLEvent> events = new ArrayList<>(DEFAULT_EVENTS_SIZE);

        List<Attribute> attributeList = new ArrayList<>(attributes.size());

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            attributeList.add(creationalParameters.getEventFactory().createAttribute(
                    creationalParameters.getPrefix(), creationalParameters.getNamespaceUri(), attribute.getKey(), attribute.getValue()));
        }

        events.add(creationalParameters.getEventFactory().createStartElement(
                creationalParameters.getPrefix(), creationalParameters.getNamespaceUri(), localName, attributeList.iterator(), null));
        events.add(creationalParameters.getEventFactory().createEndElement(
                creationalParameters.getPrefix(), creationalParameters.getNamespaceUri(), localName));

        return new RunProperty.GenericRunProperty(events);
    }

    /**
     * Provides boolean property names from the WordprocessingML.
     */
    enum WpmlTogglePropertyName {
        BOLD("b"),
        COMPLEX_SCRIPT_BOLD("bCs"),
        CAPS("caps"),
        EMBOSS("emboss"),
        ITALICS("i"),
        COMPLEX_SCRIPT_ITALICS("iCs"),
        IMPRINT("imprint"),
        OUTLINE("outline"),
        SHADOW("shadow"),
        SMALL_CAPS("smallCaps"),
        STRIKE_THROUGH("strike"),
        DOUBLE_STRIKE_THROUGH("dstrike"),
        VANISH("vanish"),
        SPEC_VANISH("specVanish"),
        NO_PROOF("noProof"),
        O_MATH("oMath"),
        COMPLEX_SCRIPT("cs"),
        RIGHT_TO_LEFT("rtl"),
        SNAP_TO_GRID("snapToGrid"),
        WEB_HIDDEN("webHidden"),

        UNSUPPORTED("");

        private final String name;

        WpmlTogglePropertyName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        static WpmlTogglePropertyName fromString(String value) {
            if (null == value) {
                return UNSUPPORTED;
            }

            for (WpmlTogglePropertyName propertyName : values()) {
                if (propertyName.toString().equals(value)) {
                    return propertyName;
                }
            }

            return UNSUPPORTED;
        }
    }

    /**
     * Provides property names from the SpreadsheetML.
     */
    enum SmlPropertyName {
        // boolean properties (defaulting to true)
        BOLD("b", "true"),
        ITALICS("i", "true"),
        SHADOW("shadow", "true"),
        STRIKE_THROUGH("strike", "true"),

        // other properties with specific default
        UNDERLINE("u", "single"),

        UNSUPPORTED("", "");

    	private final String name;
        private final String defaultValue;

        SmlPropertyName(final String name, final String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        String getDefaultValue() {
            return defaultValue;
        }

        static SmlPropertyName fromString(final String name) {
            for (SmlPropertyName propertyName : values()) {
                if (propertyName.name.equals(name)) {
                    return propertyName;
                }
            }
            return UNSUPPORTED;
        }
    }
}
