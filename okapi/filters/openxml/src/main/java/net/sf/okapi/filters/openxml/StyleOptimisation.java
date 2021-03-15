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
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides a style optimisation.
 */
interface StyleOptimisation {
    /**
     * Applies the style optimisation to the chunks of a block.
     *
     * @param chunks The chunks
     * @return The chunks with the optimised style
     */
    List<Chunk> applyTo(final List<Chunk> chunks) throws XMLStreamException;

    /**
     * Provides a bypass style optimisation.
     */
    final class Bypass implements StyleOptimisation {
        @Override
        public List<Chunk> applyTo(final List<Chunk> chunks) {
            return chunks;
        }
    }

    /**
     * Provides a default style optimisation.
     */
    final class Default implements StyleOptimisation {
        private final Bypass bypassOptimisation;
        private final ConditionalParameters conditionalParameters;
        private final XMLEventFactory eventFactory;
        private final QName blockPropertiesName;
        private final QName innerBlockPropertyName;
        private final List<QName> exclusions;
        private final StyleDefinitions styleDefinitions;

        Default(
            final Bypass bypassOptimisation,
            final ConditionalParameters conditionalParameters,
            final XMLEventFactory eventFactory,
            final QName blockPropertiesName,
            final QName innerBlockPropertyName,
            final List<QName> exclusions,
            final StyleDefinitions styleDefinitions
        ) {
            this.bypassOptimisation = bypassOptimisation;
            this.conditionalParameters = conditionalParameters;
            this.eventFactory = eventFactory;
            this.blockPropertiesName = blockPropertiesName;
            this.innerBlockPropertyName = innerBlockPropertyName;
            this.exclusions = exclusions;
            this.styleDefinitions = styleDefinitions;
        }

        @Override
        public List<Chunk> applyTo(final List<Chunk> chunks) throws XMLStreamException {
            if (chunks.size() <= 2) {
                // applying bypass as there is nothing to optimise (an empty block)
                return bypassOptimisation.applyTo(chunks);
            }

            final List<Chunk> innerChunks = chunks.subList(1, chunks.size() - 1);
            if (innerChunksContainExclusions(innerChunks)) {
                return this.bypassOptimisation.applyTo(chunks);
            }
            final List<Property> commonRunProperties = commonRunPropertiesOf(innerChunks);
            if (commonRunProperties.isEmpty()) {
                // there is nothing to optimise (the run properties are all different)
                return bypassOptimisation.applyTo(chunks);
            }

            final Block.Markup firstMarkup = (Block.Markup) chunks.get(0);
            final ParagraphBlockProperties paragraphBlockProperties = paragraphBlockPropertiesOf(firstMarkup);
            this.styleDefinitions.place(
                paragraphBlockProperties.paragraphStyle(),
                paragraphBlockProperties.withoutParagraphStyle(),
                runProperties(commonRunProperties)
            );
            paragraphBlockProperties.refine(
                this.innerBlockPropertyName,
                this.styleDefinitions.placedId(),
                commonRunProperties
            );
            firstMarkup.updateOrAddBlockProperties(paragraphBlockProperties);
            refineRuns(innerChunks, commonRunProperties);

            return chunks;
        }

        private boolean innerChunksContainExclusions(final List<Chunk> chunks) {
            for (final Chunk chunk : chunks) {
                if (chunk instanceof RunContainer) {
                    if (innerChunksContainExclusions(((RunContainer) chunk).getChunks())) {
                        return true;
                    }
                }
                if (chunk instanceof Run) {
                    final List<QName> names = ((Run) chunk).getProperties().properties()
                        .stream()
                        .map(p -> p.getName())
                        .collect(Collectors.toList());
                    names.retainAll(this.exclusions);
                    if (!names.isEmpty()) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Obtains {@link ParagraphBlockProperties} out of {@link Block.Markup}.
         *
         * @param markup The markup
         * @return The obtained block properties or new empty block properties
         */
        private ParagraphBlockProperties paragraphBlockPropertiesOf(final Block.Markup markup) {
            final ParagraphBlockProperties blockProperties = markup.paragraphBlockProperties();
            if (null == blockProperties) {
                final StartElement startElement = eventFactory.createStartElement(
                    this.blockPropertiesName.getPrefix(),
                    this.blockPropertiesName.getNamespaceURI(),
                    this.blockPropertiesName.getLocalPart()
                );
                final EndElement endElement = eventFactory.createEndElement(
                    this.blockPropertiesName.getPrefix(),
                    this.blockPropertiesName.getNamespaceURI(),
                    this.blockPropertiesName.getLocalPart()
                );
                return (ParagraphBlockProperties) BlockPropertiesFactory.createBlockProperties(
                    this.conditionalParameters,
                    this.eventFactory,
                    startElement,
                    endElement,
                    new ArrayList<>()
                );
            }
            return blockProperties;
        }

        private RunProperties runProperties(final List<Property> commonRunProperties) {
            return new RunProperties.Default(
                this.eventFactory,
                this.eventFactory.createStartElement(
                    this.innerBlockPropertyName,
                    null,
                    null
                ),
                this.eventFactory.createEndElement(
                    this.innerBlockPropertyName,
                    null
                ),
                new ArrayList<>(commonRunProperties)
            );
        }

        private List<Property> commonRunPropertiesOf(final List<Chunk> chunks) {
            final List<Property> commonRunProperties = new ArrayList<>();
            boolean added = false;
            for (final Chunk chunk : chunks) {
                if (chunk instanceof RunContainer) {
                    final List<Property> crp = commonRunPropertiesOf(
                        ((RunContainer) chunk).getChunks()
                    );
                    if (crp.isEmpty()) {
                        commonRunProperties.clear();
                        break;
                    }
                    if (!added) {
                        commonRunProperties.addAll(crp);
                        added = true;
                    } else {
                        commonRunProperties.retainAll(crp);
                    }
                }
                if (chunk instanceof Run) {
                    final Run run = (Run) chunk;
                    if (run.getProperties().properties().isEmpty()) {
                        commonRunProperties.clear();
                        break;
                    }
                    if (!added) {
                        commonRunProperties.addAll(run.getProperties().properties());
                        added = true;
                    } else {
                        commonRunProperties.retainAll(run.getProperties().properties());
                    }
                }
            }
            return commonRunProperties;
        }

        private void refineRuns(final List<Chunk> chunks, final List<Property> commonRunProperties) {
            for (final Chunk chunk : chunks) {
                if (chunk instanceof RunContainer) {
                    refineRuns(((RunContainer) chunk).getChunks(), commonRunProperties);
                }
                if (chunk instanceof Run) {
                    ((Run) chunk).refineRunProperties(commonRunProperties);
                }
            }
        }
    }
}
