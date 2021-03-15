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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.startElementEquals;

/**
 * Representation of the parsed properties of a text run.  Immutable.
 */
abstract class RunProperties implements MarkupComponent {
	static final String RPR = "rPr";
	static final String DEF_RPR = "defRPr";
	static final String END_PARA_RPR = "endParaRPr";

	abstract List<Property> properties();
	abstract RunProperties combineDistinct(RunProperties otherProperties, StyleDefinitions.TraversalStage traversalStage);
	abstract RunProperties minified(final RunProperties preCombined);
	abstract RunProperties mergedWith(final RunProperties runProperties);

	public int count() {
		return properties().size();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof RunProperties)) return false;
		return equalsProperties((RunProperties) o);
	}

	protected abstract boolean equalsProperties(RunProperties rp);

	@Override
	public abstract int hashCode();

	@Override
	public abstract List<XMLEvent> getEvents();

	/**
	 * Checks whether there is a property with a specific name.
	 *
	 * @param name The name of the property
	 * @return {@code true} if the property is present
	 *         {@code false} otherwise
	 */
	boolean contains(final String name) {
		return properties().stream()
			.anyMatch(p -> p.getName().getLocalPart().equals(name));
	}

	/**
	 * Checks whether there is a property with a specific name.
	 *
	 * @param name The name of the property
	 * @return {@code true} if the property is present
	 *         {@code false} otherwise
	 */
	boolean contains(final QName name) {
		return properties().stream()
			.anyMatch(p -> p.getName().equals(name));
	}

	/**
	 * Checks whether there is a run property.
	 *
	 * @param runProperty The run property
	 * @return {@code true} if the property is present
	 *         {@code false} otherwise
	 */
	boolean contains(final Property runProperty) {
		return properties().stream()
			.anyMatch(p -> p.equals(runProperty));
	}

	/**
	 * Gets the run style property.
	 *
	 * @return The run style proprety
	 */
	RunProperty.StyleRunProperty getRunStyleProperty() {
		for (final Property property : properties()) {
			if (property instanceof RunProperty.StyleRunProperty) {
				return (RunProperty.StyleRunProperty) property;
			}
		}

		return null;
	}

	/**
	 * Gets the run's highlight property.
	 *
	 * @return The run's hightlight proprety
	 */
	RunProperty.HighlightRunProperty getHighlightProperty() {
		for (final Property property : properties()) {
			if (property instanceof RunProperty.HighlightRunProperty) {
				return (RunProperty.HighlightRunProperty) property;
			}
		}

		return null;
	}

	/**
	 * Gets the run's color property.
	 *
	 * @return The run's color proprety
	 */
	RunProperty.ColorRunProperty getRunColorProperty() {
		for (final Property property : properties()) {
			if (property instanceof RunProperty.ColorRunProperty) {
				return (RunProperty.ColorRunProperty) property;
			}
		}

		return null;
	}

	/**
	 * Gets mergeable run properties.
	 *
	 * @return Mergeable run properties
	 */
	List<Property> getMergeableRunProperties() {
		List<Property> properties = new ArrayList<>(properties().size());

		for (Property property : properties()) {
			if (property instanceof MergeableRunProperty) {
				properties.add(property);
			}
		}

		return properties;
	}

	List<Property> attributeRunProperties() {
		return properties()
			.stream()
			.filter(runProperty -> runProperty instanceof RunProperty.AttributeRunProperty)
			.collect(Collectors.toList());
	}

	/**
	 * Refines run properties by updating exiting or adding new
	 * and aligning with their schema definition.
	 *
	 * @param commonRunProperties The run properties to check against
	 */
	void refine(final List<Property> commonRunProperties) {
		for (final Property commonRunProperty : commonRunProperties) {
			updateOrAdd(commonRunProperty);
		}
	}

	private void updateOrAdd(final Property commonRunProperty) {
		final ListIterator<Property> iterator = properties().listIterator();
		while (iterator.hasNext()) {
			final Property currentRunProperty = iterator.next();
			if (currentRunProperty.getName().equals(commonRunProperty.getName())) {
				iterator.set(commonRunProperty);
				return;
			}
		}
		iterator.add(commonRunProperty);
	}

	/**
	 * Aligns run properties with the provided schema definition.
	 *
	 * @param schemaDefinition    The schema definition to align with
	 */
	void alignWith(final SchemaDefinition.Component schemaDefinition) {
		if (properties().isEmpty()) {
			// there is nothing to align with
			return;
		}
		final List<Property> copiedProperties = new ArrayList<>(properties());
		final List<Property> attributeRunProperties = attributeRunProperties();
		properties().retainAll(attributeRunProperties);
		copiedProperties.removeAll(attributeRunProperties);

		final ListIterator<SchemaDefinition.Component> iterator = schemaDefinition.listIterator();
		while (iterator.hasNext() || !copiedProperties.isEmpty()) {
			final SchemaDefinition.Component component = iterator.next();
			switch (component.composition()) {
				case CHOICE:
				case SEQUENCE:
				case ALL:
					findAndAppendMany(copiedProperties, component);
					break;
				case NONE:
					findAndAppendOne(copiedProperties, component);
			}
		}
	}

	private void findAndAppendMany(final List<Property> copiedProperties, final SchemaDefinition.Component component) {
		final Iterator<SchemaDefinition.Component> componentsIterator = component.listIterator();
		while (componentsIterator.hasNext()) {
			final SchemaDefinition.Component innerComponent = componentsIterator.next();
			switch (innerComponent.composition()) {
				case CHOICE:
				case SEQUENCE:
				case ALL:
					findAndAppendMany(copiedProperties, innerComponent);
					break;
				case NONE:
					findAndAppendOne(copiedProperties, innerComponent);
			}
		}
	}

	private void findAndAppendOne(final List<Property> copiedProperties, final SchemaDefinition.Component component) {
		final Iterator<Property> copiedPropertiesIterator = copiedProperties.iterator();
		while (copiedPropertiesIterator.hasNext()) {
			final Property runProperty = copiedPropertiesIterator.next();
			if (runProperty.getName().equals(component.name())) {
				properties().add(runProperty);
				copiedPropertiesIterator.remove();
				return;
			}
		}
	}

	/**
	 * Removes run property by name.
	 *
	 * @param runProperty The run property
	 */
	void remove(final Property runProperty) {
		final Iterator<Property> propertiesIterator = properties().iterator();
		while (propertiesIterator.hasNext()) {
			final Property currentRunProperty = propertiesIterator.next();
			if (currentRunProperty.getName().equals(runProperty.getName())) {
				propertiesIterator.remove();
				return;
			}
		}
	}

	/**
	 * Create a copy of an exiting RunProperties object, optionally stripping the 
	 * <w:vertAlign> or <w:rStyle> or toggle property.
	 *
	 * @param existingProperties Existing properties
	 * @param stripVerticalAlign Strip vertical align property flag
	 * @param stripRunStyle      Strip run style property flag
	 * @param stripToggle        Strip toggle property flag
	 *
	 * @return A possibly stripped copy of run properties
	 */
	static RunProperties copiedRunProperties(RunProperties existingProperties, boolean stripVerticalAlign, boolean stripRunStyle, boolean stripToggle) {
		final List<Property> newRunProperties = new ArrayList<>();

		for (final Property p : existingProperties.properties()) {
			// Ack!
			if (stripToggle && p instanceof RunProperty.WpmlToggleRunProperty) {
				continue;
			}
			if (stripRunStyle && p instanceof RunProperty.StyleRunProperty) {
				continue;
			}
			if (stripVerticalAlign && p instanceof RunProperty.GenericRunProperty) {
				if (SkippableElement.RunProperty.RUN_PROPERTY_VERTICAL_ALIGNMENT_WPML.toName().equals(p.getName())
					|| SkippableElement.RunProperty.RUN_PROPERTY_VERTICAL_ALIGNMENT_SML.toName().equals(p.getName())) {
					continue; // skip it!
				}
			}
			newRunProperties.add(p);
		}

		return new Default(
			((Default) existingProperties).eventFactory,
			((Default) existingProperties).startElement,
			((Default) existingProperties).endElement,
			newRunProperties
		);
	}

	/**
	 * Creates copied run properties.
	 *
	 * @param runProperties Run properties
	 *
	 * @return Copied run properties
	 */
	static RunProperties copiedRunProperties(RunProperties runProperties) {
		final List<Property> properties = new ArrayList<>(runProperties.properties());

		return new Default(
			((Default) runProperties).eventFactory,
			((Default) runProperties).startElement,
			((Default) runProperties).endElement,
			properties
		);
	}

	/**
	 * Creates copied toggle run properties.
	 *
	 * @param runProperties Run properties
	 *
	 * @return Copied toggle run properties
	 */
	static RunProperties copiedToggleRunProperties(RunProperties runProperties) {
		final List<Property> properties = new ArrayList<>(runProperties.count());

		for (final Property property : runProperties.properties()) {
			if (property instanceof RunProperty.WpmlToggleRunProperty) {
				properties.add(property);
			}
		}

		return new Default(
			((Default) runProperties).eventFactory,
			((Default) runProperties).startElement,
			((Default) runProperties).endElement,
			properties
		);
	}

	/**
	 * Checks whether current run properties are the subset of others.
	 *
	 * Empty run properties are not a subset of non-empty others.
	 *
	 * @param other Other run properties
	 *
	 * @return {@code true} - if current run properties are the subset of others
	 *         {@code false} - otherwise
	 */
	boolean isSubsetOf(RunProperties other) {
		if (properties().isEmpty() && !other.properties().isEmpty()) {
			return false;
		}

		// Algorithmically inefficient, but the number of properties in play is
		// generally so small that it should be fine.
outer:	for (final Property myProperty : properties()) {
			for (final Property otherProperty : other.properties()) {
				if (otherProperty.equals(myProperty)) {
					continue outer;
				}
			}
			return false;
		}
		return true;
	}

	static class Default extends RunProperties implements Nameable {
		private static final Set<String> OMITTED_WITH_NONE_OR_NIL = new HashSet<>(
			Arrays.asList(
				"brd", // WPML
				"effect", // WPML
				"em", // WPML
				"highlight", // WPML
				"u", // WPML, DML, SML
				"cap", // DML
				"scheme" // SML
			)
		);
		private static final Set<String> NONE_AND_NIL = new HashSet<>(Arrays.asList("none", "nil"));
		private static final Set<String> OMITTED_WITH_ZERO = new HashSet<>(
			Arrays.asList(
				"kern", // WPML, DML
				"position", // WPML
				"baseline", // DML
				"spc" // DML
			)
		);
		private static final String ZERO = "0";
		private static final Set<String> OMITTED_WITH_HUNDRED = Collections.singleton(
			"w" // WPML
		);
		private static final String HUNDRED = "100";
		private static final Set<String> OMITTED_WITH_BASELINE = Collections.singleton(
			"vertAlign" // WPML, DML, SML
		);
		private static final String BASELINE = "baseline";
		private static final Set<String> OMITTED_WITH_NO_STRIKE = Collections.singleton(
			"strike" // DML
		);
		private static final String NO_STRIKE = "noStrike";

		private final XMLEventFactory eventFactory;
		private final StartElement startElement;
		private final EndElement endElement;
		private final List<Property> properties;

		Default(
			final XMLEventFactory eventFactory,
			final String prefix,
			final String namespaceUri,
			final String name
		) {
			this(
				eventFactory,
				eventFactory.createStartElement(prefix, namespaceUri, name),
				eventFactory.createEndElement(prefix, namespaceUri, name)
			);
		}

		Default(
			final XMLEventFactory eventFactory,
			final StartElement startElement,
			final EndElement endElement
		) {
			this(
				eventFactory,
				startElement,
				endElement,
				new ArrayList<>()
			);
		}

		Default(
			final XMLEventFactory eventFactory,
			final StartElement startElement,
			final EndElement endElement,
			final List<Property> properties
		) {
			this.eventFactory = eventFactory;
			this.startElement = startElement;
			this.endElement = endElement;
			this.properties = properties;
		}

		@Override
		public List<Property> properties() {
			return properties;
		}

		/**
		 * Obtains minified run properties in the context of the pre-combined ones
		 * throughout the styles hierarchy.
		 * <ol>
		 *   <li>
		 *       Any property is omitted if it is already available in the styles
		 *       hierarchy of a current run (pre-combined properties).
		 *   </li>
		 *   <li>
		 *       A directly specified property has a clearing-formatting value (false, off, none,
		 *       nil etc.) and it is not available in the styles hierarchy of a current run.
		 *   </li>
		 * </ol>
		 * @param preCombined The pre-combined properties throughout the styles hierarchy
		 * @return The minified run properties
		 */
		RunProperties minified(final RunProperties preCombined) {
			final List<Property> minified = properties().stream()
				.filter(p ->
						!(
							// @todo #948: remove restriction for SpreadsheetML
							!(p instanceof RunProperty.SmlRunProperty)
							&& (
							preCombined.contains(p)
							|| (
								p instanceof RunProperty.WpmlToggleRunProperty
									&& !((RunProperty.WpmlToggleRunProperty) p).getToggleValue()
								||
								p instanceof RunProperty.BooleanAttributeRunProperty
									&& !((RunProperty.BooleanAttributeRunProperty) p).booleanValue()
								||
								OMITTED_WITH_NONE_OR_NIL.contains(p.getName().getLocalPart())
									&& NONE_AND_NIL.contains(p.value())
								||
								OMITTED_WITH_ZERO.contains(p.getName().getLocalPart())
									&& ZERO.equals(p.value())
								||
								OMITTED_WITH_HUNDRED.contains(p.getName().getLocalPart())
									&& HUNDRED.equals(p.value())
								||
								OMITTED_WITH_BASELINE.contains(p.getName().getLocalPart())
									&& BASELINE.equals(p.value())
								||
								OMITTED_WITH_NO_STRIKE.contains(p.getName().getLocalPart())
									&& NO_STRIKE.equals(p.value())
							)
							&& !preCombined.contains(p.getName())
							)
						)
				)
				.collect(Collectors.toList());

			return new RunProperties.Default(
					this.eventFactory,
					this.eventFactory.createStartElement(this.startElement.getName(), null, null),
					this.endElement,
					minified
				);
		}

		@Override
		RunProperties mergedWith(final RunProperties runProperties) {
			return RunProperties.copiedRunProperties(this).combineDistinct(
				runProperties,
				StyleDefinitions.TraversalStage.DIRECT
			);
		}

		@Override
		public void apply(final FontMappings fontMappings) {
			this.properties.forEach(p -> {
				if (p instanceof RunProperty.FontsRunProperty) {
					p.apply(fontMappings);
				} else if (p instanceof RunProperty.FontRunProperty) {
					p.apply(fontMappings);
				}
			});
		}

		@Override
		public List<XMLEvent> getEvents() {
			if (properties().isEmpty()) {
				return Collections.emptyList();
			}
			final List<XMLEvent> events = new ArrayList<>();
			final List<Property> attributeRunProperties = attributeRunProperties();
			final List<Property> otherRunProperties = this.properties
				.stream()
				.filter(runProperty -> !attributeRunProperties.contains(runProperty))
				.collect(Collectors.toList());
			events.add(eventFactory.createStartElement(startElement.getName(), toAttributes(attributeRunProperties()).iterator(), startElement.getNamespaces()));
			for (final Property property : otherRunProperties) {
				events.addAll(property.getEvents());
			}
			events.add(endElement);
			return events;
		}

		private List<Attribute> toAttributes(List<Property> properties) {
			return properties
				.stream()
				.map(property -> eventFactory.createAttribute(property.getName(), property.value()))
				.collect(Collectors.toList());
		}

		@Override
		public QName getName() {
			return this.startElement.getName();
		}

		/**
		 * Combines current properties with other properties.
		 *
		 * If a property is found in the list of others, it is replaced by the found one and the found one is removed.
		 * All non-matched other properties are added to the current list of properties.
		 *
		 * @param otherProperties Other properties to match against
		 * @param traversalStage  The traversal stage
		 *
		 * @return Current run properties
		 */
		@Override
		public RunProperties combineDistinct(RunProperties otherProperties, StyleDefinitions.TraversalStage traversalStage) {
			final ListIterator<Property> runPropertyIterator = properties.listIterator();

			while (runPropertyIterator.hasNext()) {
				final Property runProperty = runPropertyIterator.next();
				// cache start element name in order not to reconstruct it for some properties (e.g. FontsRunProperty)
				QName runPropertyStartElementName = runProperty.getName();

				final Iterator<Property> otherRunPropertyIterator = otherProperties.properties().iterator();

				while (otherRunPropertyIterator.hasNext()) {
					final Property otherRunProperty = otherRunPropertyIterator.next();

					if (runPropertyStartElementName.equals(otherRunProperty.getName())) {
						replace(runPropertyIterator, otherRunPropertyIterator, runProperty, otherRunProperty, traversalStage);
						break;
					}
				}

				if (otherProperties.properties().isEmpty()) {
					break;
				}
			}

			if (!otherProperties.properties().isEmpty()) {
				properties.addAll(otherProperties.properties());
			}

			return this;
		}

		private void replace(
			ListIterator<Property> runPropertyIterator,
			Iterator<Property> otherRunPropertyIterator,
			Property runProperty,
			Property otherRunProperty,
			StyleDefinitions.TraversalStage traversalStage
		) {

			if (runProperty instanceof RunProperty.WpmlToggleRunProperty) {

				if (StyleDefinitions.TraversalStage.VERTICAL == traversalStage) {
					boolean runPropertyValue = ((RunProperty.WpmlToggleRunProperty) runProperty).getToggleValue();
					boolean otherRunPropertyValue = ((RunProperty.WpmlToggleRunProperty) otherRunProperty).getToggleValue();

					if (!(runPropertyValue ^ otherRunPropertyValue)) {
						// exclusive OR resulted to "false", which means that the property can be removed, as it is the default value
						runPropertyIterator.remove();
						otherRunPropertyIterator.remove();
						return;
					}

					if (runPropertyValue) {
						// run property value is equal to "true" and other is "false", as the previous condition happens
						// only if both values are "true" or "false"
						otherRunPropertyIterator.remove();
						return;
					}

					// run property value is equal to "false" and other is "true",
					// move on to the default processing of all other types of properties
				}

				if (StyleDefinitions.TraversalStage.DOCUMENT_DEFAULT == traversalStage) {
					boolean runPropertyValue = ((RunProperty.WpmlToggleRunProperty) runProperty).getToggleValue();
					boolean otherRunPropertyValue = ((RunProperty.WpmlToggleRunProperty) otherRunProperty).getToggleValue();

					if (runPropertyValue && otherRunPropertyValue
							|| runPropertyValue) {
						// if run property value is equal to "true" and other run property value is equal to "whatever" value
						otherRunPropertyIterator.remove();
						return;
					}
					// run property value is equal to "false" and other is "true",
					// move on to the default processing of all other types of properties
				}

				// The MS Word does not follow up the flow of toggle properties processing and does substitute ANY value
				// in the styles hierarchy by ANY value if it has been specified later.
				// So, StyleDefinitions.TraversalStage.HORIZONTAL case is processed as all other types of properties.

				// StyleDefinitions.TraversalStage.DIRECT case is processed as all other types of properties
			}

			runPropertyIterator.set(otherRunProperty);
			otherRunPropertyIterator.remove();
		}

		@Override
		protected boolean equalsProperties(RunProperties o) {
			if (!(o instanceof Default)) return false;
			// Compare start events - this ensures the element/namespace is the same,
			// and also will compare attributes in the DrawingML case.
			Default rp = (Default)o;
			if (!startElementEquals(startElement, rp.startElement)) {
				return false;
			}
			// TODO handle out of order properties
			return properties.equals(rp.properties);
		}

		@Override
		public int hashCode() {
			return Objects.hash(startElement, endElement, properties);
		}

		@Override
		public String toString() {
			return this.startElement.getName().getLocalPart()
				.concat("(")
				.concat(String.valueOf(this.properties.size()))
				.concat(")[")
				.concat(this.properties.toString())
				.concat("]");
		}
	}
}
