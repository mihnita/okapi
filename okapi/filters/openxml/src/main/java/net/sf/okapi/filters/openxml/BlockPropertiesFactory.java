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
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.List;

/**
 * Provides a block properties factory.
 */
class BlockPropertiesFactory {

    /**
     * Creates block properties.
     *
     * @param conditionalParameters  Conditional parameters
     * @param creationalParameters   Creational parameters
     * @param startElementAttributes Start element attributes
     * @param blockProperties        Block properties
     *
     * @return Block properties
     */
    static BlockProperties createBlockProperties(
        ConditionalParameters conditionalParameters,
        CreationalParameters creationalParameters,
        String startElementLocalName,
        List<Attribute> startElementAttributes,
        List<Property> blockProperties
    ) {
        StartElement startElement = creationalParameters.getEventFactory().createStartElement(
                creationalParameters.getPrefix(), creationalParameters.getNamespaceUri(), startElementLocalName, startElementAttributes.iterator(), null);
        EndElement endElement = creationalParameters.getEventFactory().createEndElement(
                creationalParameters.getPrefix(), creationalParameters.getNamespaceUri(), startElementLocalName);

        return createBlockProperties(conditionalParameters, creationalParameters.getEventFactory(), startElement, endElement, blockProperties);
    }

    static BlockProperties createBlockProperties(
        ConditionalParameters conditionalParameters,
        XMLEventFactory eventFactory,
        StartElement startElement,
        EndElement endElement,
        List<Property> properties
    ) {
        if (ParagraphBlockProperties.PPR.equals(startElement.getName().getLocalPart())
            || PowerpointStyleDefinition.DEF_PPR.equals(startElement.getName().getLocalPart())
            || PowerpointStyleDefinition.PARAGRAPH_LEVELS.contains(startElement.getName().getLocalPart())) {
            if (Namespace.PREFIX_A.equals(startElement.getName().getPrefix())) {
                return new ParagraphBlockProperties.Drawing(
                    new BlockProperties.Default(eventFactory, startElement, endElement, properties),
                    conditionalParameters,
                    eventFactory,
                    new StrippableAttributes.DrawingRunProperties(
                        conditionalParameters,
                        eventFactory
                    ),
                    SchemaDefinitions.of(startElement)
                );
            }
            return new ParagraphBlockProperties.Word(
                new BlockProperties.Default(eventFactory, startElement, endElement, properties),
                conditionalParameters,
                eventFactory,
                new StrippableAttributes.DrawingRunProperties(
                    conditionalParameters,
                    eventFactory
                )
            );
        }
        return new BlockProperties.Default(eventFactory, startElement, endElement, properties);
    }
}
