/*
 * =============================================================================
 *   Copyright (C) 2010-2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.idml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A story child elements merger.
 *
 * Merges consequential story child elements basing on provided
 * parameters and style ranges of these elements (e.g. kerning, tracking, etc).
 */
final class StoryChildElementsMerger {

    /**
     * An unexpected element type error message.
     */
    private static final String UNEXPECTED_ELEMENT_TYPE = "Unexpected element type: ";

    /**
     * A minimum number of child elements to merge.
     */
    private static final int MIN_NUMBER_OF_CHILD_ELEMENTS_TO_MERGE = 2;

    /**
     * The current element index.
     */
    private static final int CURRENT_ELEMENT_INDEX = 0;

    /**
     * The next element index.
     */
    private static final int NEXT_ELEMENT_INDEX = 1;

    /**
     * Parameters.
     */
    private final Parameters parameters;
    private final XMLEventFactory eventFactory;

    /**
     * Creates a story child elements merger.
     *
     * @param parameters The parameters
     * @param eventFactory The event factory
     */
    StoryChildElementsMerger(final Parameters parameters, final XMLEventFactory eventFactory) {
        this.parameters = parameters;
        this.eventFactory = eventFactory;
    }

    /**
     * Merges story child elements.
     *
     * @param storyChildElements The story child elements
     * @return Merged story child elements or the same elements
     *          if there is no need for the merge
     */
    List<StoryChildElement> merge(final List<StoryChildElement> storyChildElements) {
        if (!neededToBeMerged(storyChildElements)) {
            return storyChildElements;
        }

        final List<StoryChildElement> mergedStoryChildElements = new ArrayList<>(storyChildElements.size());

        final Iterator<StoryChildElement> elementIterator = storyChildElements.iterator();
        StoryChildElement currentElement = elementIterator.next();

        do {
            StoryChildElement nextElement = elementIterator.next();
            boolean elementsStyleRangesCanBeMerged = canElementsStyleRangesBeMerged(currentElement, nextElement);

            if (canElementsBeMerged(currentElement, nextElement, elementsStyleRangesCanBeMerged)) {
                currentElement = mergeElements(currentElement, nextElement);
            } else if (elementsStyleRangesCanBeMerged) {
                final List<StoryChildElement> elements = mergeElementsStyleRanges(currentElement, nextElement);
                mergedStoryChildElements.add(elements.get(CURRENT_ELEMENT_INDEX));
                currentElement = elements.get(NEXT_ELEMENT_INDEX);
            } else {
                mergedStoryChildElements.add(currentElement);
                currentElement = nextElement;
            }
        } while (elementIterator.hasNext());

        mergedStoryChildElements.add(currentElement);

        return mergedStoryChildElements;
    }

    /**
     * Checks whether the story elements are needed to be merged.
     *
     * @param storyChildElements The story child elements
     * @return {@code true} if there is such a need and
     *         {@code false} otherwise
     */
    private boolean neededToBeMerged(final List<StoryChildElement> storyChildElements) {
        return MIN_NUMBER_OF_CHILD_ELEMENTS_TO_MERGE <= storyChildElements.size()
            && (this.parameters.getIgnoreCharacterKerning()
                || this.parameters.getIgnoreCharacterTracking()
                || this.parameters.getIgnoreCharacterLeading()
                || this.parameters.getIgnoreCharacterBaselineShift());
    }

    /**
     * Checks whether 2 consequential story child elements can be merged.
     *
     * @param currentElement                 The current element
     * @param nextElement                    The next element
     * @param elementsStyleRangesCanBeMerged The elements style ranges can be merged
     * @return {@code true} if the elements can be merged and
     *         {@code false} otherwise
     */
    private boolean canElementsBeMerged(final StoryChildElement currentElement,
                                        final StoryChildElement nextElement,
                                        final boolean elementsStyleRangesCanBeMerged) {
        return isContent(currentElement) && isContent(nextElement)
            && elementsStyleRangesCanBeMerged;
    }

    /**
     * Checks whether the element is of
     * the {@link StoryChildElement.StyledTextElement.Content} type.
     *
     * @param element The element
     * @return {@code true} if the element is content and
     *         {@code false} otherwise
     */
    private boolean isContent(final StoryChildElement element) {
        return element instanceof StoryChildElement.StyledTextElement.Content;
    }

    /**
     * Checks whether the elements style ranges can be merged.
     *
     * @param currentElement The current element
     * @param nextElement    The next element
     * @return {@code true} if the elements style ranges can be merged and
     *         {@code false} otherwise
     */
    private boolean canElementsStyleRangesBeMerged(StoryChildElement currentElement, StoryChildElement nextElement) {
        if (!hasElementStyleRanges(currentElement) || !hasElementStyleRanges(nextElement)) {
            return false;
        }
        return canStyleRangesBeMerged(
                elementStyleRanges(currentElement).getCombinedStyleRange(),
                elementStyleRanges(nextElement).getCombinedStyleRange()
        );
    }

    /**
     * Checks whether the element has style rangers.
     *
     * @param element The element
     * @return {@code true} if the element has style ranges and
     *         {@code false} otherwise
     */
    private boolean hasElementStyleRanges(StoryChildElement element) {
        return element instanceof StoryChildElement.StyledTextElement
            && null != ((StoryChildElement.StyledTextElement) element).getStyleRanges(); // this is for the Table.Cell
    }

    /**
     * Forms element style ranges.
     *
     * The {@link IllegalArgumentException} is thrown if the element does not
     * have style ranges.
     *
     * @param element The element
     * @return The element style ranges
     * @see StoryChildElementsMerger#hasElementStyleRanges
     */
    private StyleRanges elementStyleRanges(final StoryChildElement element) {
        if (!hasElementStyleRanges(element)) {
            throw new IllegalArgumentException(UNEXPECTED_ELEMENT_TYPE + element.getClass().getTypeName());
        }
        return ((StoryChildElement.StyledTextElement) element).getStyleRanges();
    }

    /**
     * Checks whether style ranges can be merged.
     *
     * Every style range has attributes and properties.
     * So, this method checks both of them.
     *
     * @param currentStyleRange The current style range
     * @param nextStyleRange    The next style range
     * @return {@code true} if style ranges can be merged and
     *         {@code false} otherwise
     */
    private boolean canStyleRangesBeMerged(final StyleRange currentStyleRange, final StyleRange nextStyleRange) {
        return canAttributesBeMerged(currentStyleRange.getAttributes(), nextStyleRange.getAttributes())
                && canPropertiesBeMerged(currentStyleRange.getProperties(), nextStyleRange.getProperties());
    }

    /**
     * Checks whether attributes can be merged.
     *
     * There are attributes with and without ignorances.
     * The method checks both variations.
     *
     * @param currentAttributes The current attributes
     * @param nextAttributes    The next attributes
     * @return {@code true} if attributes can be merged and
     *         {@code false} otherwise
     */
    private boolean canAttributesBeMerged(final List<Attribute> currentAttributes, final List<Attribute> nextAttributes) {
        final Map<QName, Attribute> currentAttributeIgnorancesByName = attributeIgnorancesByName(currentAttributes);
        final Map<QName, Attribute> nextAttributeIgnorancesByName = attributeIgnorancesByName(nextAttributes);

        final Set<Attribute> currentAttributesWithoutIgnorances =
                attributesWithoutIgnorances(currentAttributes, currentAttributeIgnorancesByName);
        final Set<Attribute> nextAttributesWithoutIgnorances =
                attributesWithoutIgnorances(nextAttributes, nextAttributeIgnorancesByName);

        return canAttributesWithoutIgnorancesBeMerged(currentAttributesWithoutIgnorances, nextAttributesWithoutIgnorances)
                && canAttributesWithIgnorancesBeMerged(currentAttributeIgnorancesByName, nextAttributeIgnorancesByName);
    }

    /**
     * Forms attribute ignorances by name.
     *
     * Filtering condition is based on whether an attribute name is present
     * in the style ignorances.
     *
     * @param attributes The attributes
     * @return Attribute ignorances by name
     */
    private Map<QName, Attribute> attributeIgnorancesByName(final List<Attribute> attributes) {
        return attributes
            .stream()
            .filter(attribute -> this.parameters.styleIgnorances().isAttributeNamePresent(attribute.getName()))
            .collect(Collectors.toMap(Attribute::getName, Function.identity()));
    }

    /**
     * Forms attributes without ignorances.
     *
     * @param attributes                The attributes
     * @param attributeIgnorancesByName The attribute ignorances by name
     * @return Attributes without ignorances
     */
    private static Set<Attribute> attributesWithoutIgnorances(final List<Attribute> attributes,
                                                              final Map<QName, Attribute> attributeIgnorancesByName) {
        return attributes
                .stream()
                .filter(attribute -> !attributeIgnorancesByName.containsKey(attribute.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Checks whether the attributes without ignorances can be merged.
     *
     * Currently, it is required that the attributes without ignorances
     * be equal.
     *
     * @param currentAttributesWithoutIgnorances The current attributes without ignorances
     * @param nextAttributesWithoutIgnorances    The next attributes without ignorances
     * @return Attributes without ignorances
     */
    private static boolean canAttributesWithoutIgnorancesBeMerged(final Set<Attribute> currentAttributesWithoutIgnorances,
                                                                  final Set<Attribute> nextAttributesWithoutIgnorances) {
        return currentAttributesWithoutIgnorances.equals(nextAttributesWithoutIgnorances);
    }

    /**
     * Checks whether the attributes with ignorances be merged.
     *
     * This includes the check for common and distinct attributes
     * with ignorances.
     *
     * @param currentAttributeIgnorancesByName The current attribute ignorances by name
     * @param nextAttributeIgnorancesByName    The next attribute ignorances by name
     * @return {@code true} if attributes with ignorances can be merged and
     *         {@code false} otherwise
     */
    private boolean canAttributesWithIgnorancesBeMerged(final Map<QName, Attribute> currentAttributeIgnorancesByName,
                                                        final Map<QName, Attribute> nextAttributeIgnorancesByName) {
        final Set<QName> commonAttributeNameIgnorances = currentAttributeIgnorancesByName.keySet()
                .stream()
                .filter(nextAttributeIgnorancesByName::containsKey)
                .collect(Collectors.toSet());

        return canCommonAttributeNameIgnorancesBeMerged(currentAttributeIgnorancesByName, nextAttributeIgnorancesByName,
                commonAttributeNameIgnorances)
            && canDistinctAttributeNameIgnorancesBeMerged(currentAttributeIgnorancesByName, commonAttributeNameIgnorances)
            && canDistinctAttributeNameIgnorancesBeMerged(nextAttributeIgnorancesByName, commonAttributeNameIgnorances);
    }

    /**
     * Checks whether the common attribute name ignorances can be merged.
     *
     * @param currentAttributeIgnorancesByName The current attribute ignorances by name
     * @param nextAttributeIgnorancesByName    The next attribute ignorances by name
     * @param commonAttributeNameIgnorances    The common attribute name ignorances
     * @return {@code true} if common attribute name ignorances can be merged and
     *         {@code false} otherwise
     */
    private boolean canCommonAttributeNameIgnorancesBeMerged(final Map<QName, Attribute> currentAttributeIgnorancesByName,
                                                             final Map<QName, Attribute> nextAttributeIgnorancesByName,
                                                             final Set<QName> commonAttributeNameIgnorances) {
        for (QName name : commonAttributeNameIgnorances) {
            if (!canValuesBeIgnored(
                    name,
                    currentAttributeIgnorancesByName.get(name).getValue(),
                    nextAttributeIgnorancesByName.get(name).getValue()
            )) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether distinct attribute name ignorances can be merged.
     *
     * Covers the case when there are no common attribute name ignorances
     * present. I.e. there is current or next value only.
     *
     * @param attributeIgnorancesByName     The attribute ignorances by name
     * @param commonAttributeNameIgnorances The common attribute name ignorances
     * @return {@code true} if distinct attribute name ignorances can be merged and
     *         {@code false} otherwise
     */
    private boolean canDistinctAttributeNameIgnorancesBeMerged(final Map<QName, Attribute> attributeIgnorancesByName,
                                                               final Set<QName> commonAttributeNameIgnorances) {
        final Set<QName> attributeNameIgnorances = attributeIgnorancesByName.keySet()
                .stream()
                .filter(name -> !commonAttributeNameIgnorances.contains(name))
                .collect(Collectors.toSet());

        for (QName name : attributeNameIgnorances) {
            if (!canValuesBeIgnored(
                    name,
                    attributeIgnorancesByName.get(name).getValue(),
                    attributeIgnorancesByName.get(name).getValue()
            )) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether values be ignored.
     *
     * The values are considered depending on threshold type for the name
     * of ignorance attribute or property.
     *
     * @param name         The name of ignorance attribute or property
     * @param currentValue The current value
     * @param nextValue    The next value
     * @return {@code true} if values can be ignored
     *         {@code false} otherwise
     */
    private boolean canValuesBeIgnored(final QName name, final String currentValue, final String nextValue) {
        final StyleIgnorances.Thresholds thresholds = this.parameters.styleIgnorances().thresholds(name);

        switch (thresholds.type()) {
            case INTEGER:
                return canIntegerValuesBeIgnored(Integer.valueOf(currentValue), Integer.valueOf(nextValue), thresholds);
            case DOUBLE:
                return canDoubleValuesBeIgnored(Double.valueOf(currentValue), Double.valueOf(nextValue), thresholds);
            case STRING:
                return canStringValuesBeIgnored(thresholds);
            default:
                return false;
        }
    }

    /**
     * Checks whether integer values can be ignored.
     *
     * @param currentValue The current value
     * @param nextValue    The next value
     * @param thresholds   The thresholds
     * @return {@code true}
     *  if the min threshold is not empty and the max threshold is empty
     *  and the min threshold is less or equal to the current or the next values;
     *  or if the min threshold is empty and the max threshold is not empty
     *  and the max threshold is grater or equal to the current or the next values;
     *  or if the min and max thresholds are not empty
     *  and the current and the next values are between those thresholds;
     *  or if the min and max thresholds are empty;
     *         {@code false} otherwise
     */
    private boolean canIntegerValuesBeIgnored(final int currentValue, final int nextValue,
                                              final StyleIgnorances.Thresholds thresholds) {
        if (!thresholds.min().isEmpty() && thresholds.max().isEmpty()) {
            int minThreshold = Integer.valueOf(thresholds.min());
            return minThreshold <= currentValue && minThreshold <= nextValue;
        }
        if (thresholds.min().isEmpty() && !thresholds.max().isEmpty()) {
            int maxThreshold = Integer.valueOf(thresholds.max());
            return currentValue <= maxThreshold && nextValue <= maxThreshold;
        }
        if (!thresholds.min().isEmpty() && !thresholds.max().isEmpty()) {
            int minThreshold = Integer.valueOf(thresholds.min());
            int maxThreshold = Integer.valueOf(thresholds.max());
            return minThreshold <= currentValue && minThreshold <= nextValue
                    && currentValue <= maxThreshold && nextValue <= maxThreshold;
        }
        // thresholds.min().isEmpty() && thresholds.max().isEmpty()
        return true;
    }

    /**
     * Checks whether double values can be ignored.
     *
     * @param currentValue The current value
     * @param nextValue    The next value
     * @param thresholds   The thresholds
     * @return {@code true}
     *  if the min threshold is not empty and the max threshold is empty
     *  and the min threshold is less or equal to the current or the next values;
     *  or if the min threshold is empty and the max threshold is not empty
     *  and the max threshold is grater or equal to the current or the next values;
     *  or if the min and max thresholds are not empty
     *  and the current and the next values are between those thresholds;
     *  or if the min and max thresholds are empty;
     *         {@code false} otherwise
     */
    private boolean canDoubleValuesBeIgnored(final double currentValue, final double nextValue,
                                             final StyleIgnorances.Thresholds thresholds) {
        if (!thresholds.min().isEmpty() && thresholds.max().isEmpty()) {
            double minThreshold = Double.valueOf(thresholds.min());
            return minThreshold <= currentValue && minThreshold <= nextValue;
        }
        if (thresholds.min().isEmpty() && !thresholds.max().isEmpty()) {
            double maxThreshold = Double.valueOf(thresholds.max());
            return currentValue <= maxThreshold && nextValue <= maxThreshold;
        }
        if (!thresholds.min().isEmpty() && !thresholds.max().isEmpty()) {
            double minThreshold = Double.valueOf(thresholds.min());
            double maxThreshold = Double.valueOf(thresholds.max());
            return minThreshold <= currentValue && minThreshold <= nextValue
                    && currentValue <= maxThreshold && nextValue <= maxThreshold;
        }
        // thresholds.min().isEmpty() && thresholds.max().isEmpty()
        return true;
    }

    /**
     * Checks whether string values can be ignored.
     *
     * @param thresholds The thresholds
     * @return {@code true} if the thresholds are empty and
     *         {@code false} otherwise
     */
    private boolean canStringValuesBeIgnored(final StyleIgnorances.Thresholds thresholds) {
        return thresholds.areEmpty();
    }

    /**
     * Checks whether properties can be merged.
     *
     * There are properties with and without ignorances.
     * The method checks both variations.
     *
     * @param currentProperties The current properties
     * @param nextProperties    The next properties
     * @return {@code true} if properties can be merged and
     *         {@code false} otherwise
     */
    private boolean canPropertiesBeMerged(final Properties currentProperties, final Properties nextProperties) {
        final Map<QName, Property> currentPropertyIgnorancesByName = propertyIgnorancesByName(currentProperties);
        final Map<QName, Property> nextPropertyIgnorancesByName = propertyIgnorancesByName(nextProperties);

        final Set<Property> currentPropertiesWithoutIgnorances =
                propertiesWithoutIgnorances(currentProperties, currentPropertyIgnorancesByName);
        final Set<Property> nextAttributesWithoutIgnorances =
                propertiesWithoutIgnorances(nextProperties, nextPropertyIgnorancesByName);

        return canPropertiesWithoutIgnorancesBeMerged(currentPropertiesWithoutIgnorances, nextAttributesWithoutIgnorances)
                && canPropertiesWithIgnorancesBeMerged(currentPropertyIgnorancesByName, nextPropertyIgnorancesByName);
    }

    /**
     * Forms property ignorances by name.
     *
     * Filtering condition is based on whether an property name is present
     * in the style ignorances.
     *
     * @param properties The properties
     * @return Property ignorances by name
     */
    private Map<QName, Property> propertyIgnorancesByName(final Properties properties) {
        return properties.properties()
                .stream()
                .filter(property -> this.parameters.styleIgnorances().isPropertyNamePresent(property.getName()))
                .collect(Collectors.toMap(Property::getName, Function.identity()));
    }

    /**
     * Forms properties without ignorances.
     *
     * @param properties               The properties
     * @param propertyIgnorancesByName The property ignorances by name
     * @return Properties without ignorances
     */
    private static Set<Property> propertiesWithoutIgnorances(final Properties properties,
                                                              final Map<QName, Property> propertyIgnorancesByName) {
        return properties.properties()
                .stream()
                .filter(attribute -> !propertyIgnorancesByName.containsKey(attribute.getName()))
                .collect(Collectors.toSet());
    }

    /**
     * Checks whether the properties without ignorances can be merged.
     *
     * Currently, it is required that the properties without ignorances
     * be equal.
     *
     * @param currentPropertiesWithoutIgnorances The current properties without ignorances
     * @param nextPropertiesWithoutIgnorances    The next properties without ignorances
     * @return Properties without ignorances
     */
    private static boolean canPropertiesWithoutIgnorancesBeMerged(final Set<Property> currentPropertiesWithoutIgnorances,
                                                                  final Set<Property> nextPropertiesWithoutIgnorances) {
        return currentPropertiesWithoutIgnorances.equals(nextPropertiesWithoutIgnorances);
    }

    /**
     * Checks whether the properties with ignorances be merged.
     *
     * This includes the check for common and distinct properties
     * with ignorances.
     *
     * @param currentPropertyIgnorancesByName The current property ignorances by name
     * @param nextPropertyIgnorancesByName    The next property ignorances by name
     * @return {@code true} if properties with ignorances can be merged and
     *         {@code false} otherwise
     */
    private boolean canPropertiesWithIgnorancesBeMerged(final Map<QName, Property> currentPropertyIgnorancesByName,
                                                        final Map<QName, Property> nextPropertyIgnorancesByName) {
        final Set<QName> commonPropertyNameIgnorances = currentPropertyIgnorancesByName.keySet()
                .stream()
                .filter(nextPropertyIgnorancesByName::containsKey)
                .collect(Collectors.toSet());

        return canCommonPropertyNameIgnorancesBeMerged(currentPropertyIgnorancesByName, nextPropertyIgnorancesByName,
                commonPropertyNameIgnorances)
                && canDistinctPropertyNameIgnorancesBeMerged(currentPropertyIgnorancesByName, commonPropertyNameIgnorances)
                && canDistinctPropertyNameIgnorancesBeMerged(nextPropertyIgnorancesByName, commonPropertyNameIgnorances);
    }

    /**
     * Checks whether the common property name ignorances can be merged.
     *
     * @param currentPropertyIgnorancesByName The current property ignorances by name
     * @param nextPropertyIgnorancesByName    The next property ignorances by name
     * @param commonPropertyNameIgnorances    The common property name ignorances
     * @return {@code true} if common property name ignorances can be merged and
     *         {@code false} otherwise
     */
    private boolean canCommonPropertyNameIgnorancesBeMerged(final Map<QName, Property> currentPropertyIgnorancesByName,
                                                            final Map<QName, Property> nextPropertyIgnorancesByName,
                                                            final Set<QName> commonPropertyNameIgnorances) {
        for (QName name : commonPropertyNameIgnorances) {
            if (!canInnerEventsBeIgnored(
                    name,
                    currentPropertyIgnorancesByName.get(name).innerEvents(),
                    nextPropertyIgnorancesByName.get(name).innerEvents()
            )) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether distinct property name ignorances can be merged.
     *
     * If style ignorance thresholds are empty (contain no min and max values),
     * then they are eligible for the merge. Otherwise, the threshold non-empty
     * values can not be matched against the absence of them on the other side.
     *
     * @param propertyIgnorancesByName     The property ignorances by name
     * @param commonPropertyNameIgnorances The common property name ignorances
     * @return {@code true} if distinct property name ignorances can be merged and
     *         {@code false} otherwise
     */
    private boolean canDistinctPropertyNameIgnorancesBeMerged(final Map<QName, Property> propertyIgnorancesByName,
                                                              final Set<QName> commonPropertyNameIgnorances) {
        final Set<QName> propertyNameIgnorances = propertyIgnorancesByName.keySet()
                .stream()
                .filter(name -> !commonPropertyNameIgnorances.contains(name))
                .collect(Collectors.toSet());

        for (QName name : propertyNameIgnorances) {
            if (!this.parameters.styleIgnorances().thresholds(name).areEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether inner events can be ignored.
     *
     * @param name                       The property name
     * @param currentPropertyInnerEvents The current property inner events
     * @param nextPropertyInnerEvents    The next property inner events
     * @return {@code true} if inner events can be ignored and
     *         {@code false} otherwise
     */
    private boolean canInnerEventsBeIgnored(final QName name, final List<XMLEvent> currentPropertyInnerEvents,
                                            final List<XMLEvent> nextPropertyInnerEvents) {
        return canValuesBeIgnored(
                name,
                eventsToString(currentPropertyInnerEvents),
                eventsToString(nextPropertyInnerEvents)
        );
    }

    /**
     * Transforms {@link XMLEvent}s to their {@link String} representation.
     *
     * @param events The events
     * @return A string representation
     */
    private static String eventsToString(List<XMLEvent> events) {
        return events.stream()
                .map(event -> event.asCharacters().getData())
                .collect(Collectors.joining());
    }

    /**
     * Merges 2 elements.
     *
     * @param currentElement The current element
     * @param nextElement    The next element
     * @return A merged element
     */
    private StoryChildElement mergeElements(final StoryChildElement currentElement, final StoryChildElement nextElement) {

        return new StoryChildElement.StyledTextElement.Content(
            currentElement.startElement(),
            mergeEvents(currentElement.innerEvents(), nextElement.innerEvents()),
            currentElement.endElement(),
            this.eventFactory,
            mergeStyleRanges(
                ((StoryChildElement.StyledTextElement) currentElement).getStyleRanges(),
                ((StoryChildElement.StyledTextElement) nextElement).getStyleRanges()
            )
        );
    }

    /**
     * Merges 2 {@link XMLEvent} lists.
     *
     * @param currentEvents The current events
     * @param nextEvents    The next events
     * @return The merged {@link XMLEvent) list
     */
    private static List<XMLEvent> mergeEvents(final List<XMLEvent> currentEvents,
                                              final List<XMLEvent> nextEvents) {
        return Stream.concat(currentEvents.stream(), nextEvents.stream())
                .collect(Collectors.toList());
    }

    /**
     * Merges 2 elements style ranges.
     *
     * @param currentElement The current element
     * @param nextElement    The next element
     * @return A list of 2 elements with merged style ranges,
     * where the 1st element of the list is the current and 2nd is the next.
     */
    private static List<StoryChildElement> mergeElementsStyleRanges(final StoryChildElement currentElement,
                                                                    final StoryChildElement nextElement) {
        return createElementsWithMergedStyleRanges(
            currentElement,
            nextElement,
            mergeStyleRanges(
                ((StoryChildElement.StyledTextElement) currentElement).getStyleRanges(),
                ((StoryChildElement.StyledTextElement) nextElement).getStyleRanges()
            )
        );
    }

    /**
     * Merges style ranges.
     *
     * Actually, it does not create new ones but just returns the existing,
     * depending on the amount value.
     *
     * @param currentStyleRanges The current style ranges
     * @param nextStyleRanges    The next style ranges
     * @return If the current style ranges amount is less or equal to the
     * next style ranges, then the current style ranges are returned.
     * Otherwise, the next style ranges are returned.
     */
    private static StyleRanges mergeStyleRanges(final StyleRanges currentStyleRanges,
                                                     final StyleRanges nextStyleRanges) {
        return currentStyleRanges.amount() <= nextStyleRanges.amount()
                ? currentStyleRanges
                : nextStyleRanges;
    }

    /**
     * Creates 2 elements with the merged style ranges.
     *
     * The 1st one in the list is the current, the 2nd one is the next.
     *
     * @param currentElement   The current element
     * @param nextElement      The next element
     * @param styleRanges The style ranges
     * @return A created list of 2 elements with the merged style ranges
     */
    private static List<StoryChildElement> createElementsWithMergedStyleRanges(final StoryChildElement currentElement,
                                                                                    final StoryChildElement nextElement,
                                                                                    final StyleRanges styleRanges) {
        return Arrays.asList(
            ((StoryChildElement.StyledTextElement) currentElement).copyWith(styleRanges),
            ((StoryChildElement.StyledTextElement) nextElement).copyWith(styleRanges)
        );
    }
}
