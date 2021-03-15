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
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static net.sf.okapi.filters.openxml.XMLEventHelpers.DEFAULT_BOOLEAN_ATTRIBUTE_TRUE_VALUE;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DML_HYPERLINK_ACTION;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.WPML_VAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.eventEquals;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getAttributeValue;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.getBooleanAttributeValue;

import net.sf.okapi.common.filters.fontmappings.FontMappings;
import net.sf.okapi.filters.openxml.RunPropertyFactory.SmlPropertyName;

abstract class RunProperty implements Property, ReplaceableRunProperty {

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof RunProperty)) return false;
		return equalsProperty((RunProperty)o);
	}

	protected abstract boolean equalsProperty(RunProperty rp);

	@Override
	public abstract int hashCode();

	@Override
	public abstract List<XMLEvent> getEvents();

	@Override
	public boolean canBeReplaced(ReplaceableRunProperty runProperty) {
		return equalsProperty((RunProperty) runProperty);
	}

	@Override
	public ReplaceableRunProperty replace(ReplaceableRunProperty runProperty) {
		return runProperty;
	}

	@Override
	public RunProperties asRunProperties() throws XMLStreamException {
		throw new IllegalStateException("The operation is not supported");
	}

	@Override
	public void apply(final FontMappings fontMappings) {
	}

	static class GenericRunProperty extends RunProperty {
		private List<XMLEvent> events = new ArrayList<>();

		GenericRunProperty(List<XMLEvent> events) {
			this.events.addAll(events);
		}

		@Override
		protected boolean equalsProperty(RunProperty rp) {
			if (!(rp instanceof GenericRunProperty)) return false;
			return eventEquals(events, ((GenericRunProperty) rp).events);
		}

		@Override
		public int hashCode() {
			return events.hashCode();
		}

		@Override
		public List<XMLEvent> getEvents() {
			return events;
		}

		@Override
		public QName getName() {
			return events.get(0).asStartElement().getName();
		}

		@Override
		public String value() {
			// Attributes are never subject to the default namespace. An attribute without
			// an explicit namespace prefix is considered not to be in any namespace.
			final StartElement startElement = events.get(0).asStartElement();
			final QName name = new QName(
				Namespace.PREFIX_EMPTY.equals(startElement.getName().getPrefix())
					? Namespace.EMPTY
					: startElement.getName().getNamespaceURI(),
				"val",
				startElement.getName().getPrefix()
			);
			return getAttributeValue(startElement, name);
		}

		@Override
		public String toString() {
			return "GenericRunProperty(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	/**
	 * Represents a shade run property.
	 */
	static class ShadeRunProperty extends GenericRunProperty {
		/**
		 * Property name.
		 */
		static final String NAME = "shd";

		/**
		 * Fill attribute name.
		 */
		private static final QName FILL = Namespaces.WordProcessingML.getQName("fill");

		/**
		 * Default fill attribute value.
		 */
		private static final String DEFAULT_FILL_VALUE = "auto";

		ShadeRunProperty(List<XMLEvent> events) {
			super(events);
		}

		/**
		 * Obtains a fill attribute value.
		 *
		 * @return The fill attribute value or the default one if the attribute
		 * does not exist.
		 */
		String fillValue() {
			final String value = getAttributeValue(getEvents().get(0).asStartElement(), FILL);
			return null == value
				? DEFAULT_FILL_VALUE
				: value;
		}

		@Override
		protected boolean equalsProperty(RunProperty rp) {
			if (!(rp instanceof ShadeRunProperty)) return false;
			return super.equalsProperty(rp);
		}

		@Override
		public String toString() {
			return ShadeRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	static class StyleRunProperty extends GenericRunProperty {
		static final String NAME = "rStyle";

		private String value;

		StyleRunProperty(List<XMLEvent> events) {
			super(events);
			value = getAttributeValue(events.get(0).asStartElement(), WPML_VAL);
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof StyleRunProperty)) return false;
			return Objects.equals(value, ((StyleRunProperty) runProperty).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return StyleRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	static class HighlightRunProperty extends GenericRunProperty {
		static final String NAME = "highlight";

		private String value;

		HighlightRunProperty(List<XMLEvent> events) {
			super(events);
			value = getAttributeValue(events.get(0).asStartElement(), WPML_VAL);
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof HighlightRunProperty)) return false;
			return Objects.equals(value, ((HighlightRunProperty) runProperty).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return HighlightRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	static class ColorRunProperty extends GenericRunProperty {
		static final String NAME = "color";

		private String value;

		ColorRunProperty(List<XMLEvent> events) {
			super(events);
			value = getAttributeValue(events.get(0).asStartElement(), WPML_VAL);
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof ColorRunProperty)) return false;
			return Objects.equals(value, ((ColorRunProperty) runProperty).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public String toString() {
			return ColorRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	static class WpmlToggleRunProperty extends GenericRunProperty {
		private boolean value;

		WpmlToggleRunProperty(List<XMLEvent> events) {
			super(events);
			value = getBooleanAttributeValue(events.get(0).asStartElement(), WPML_VAL, DEFAULT_BOOLEAN_ATTRIBUTE_TRUE_VALUE);
		}

		public boolean getToggleValue() {
			return value;
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof WpmlToggleRunProperty)) return false;

			return eventEquals(getEvents().get(0), runProperty.getEvents().get(0))
					&& Objects.equals(value, ((WpmlToggleRunProperty) runProperty).value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public String toString() {
			return WpmlToggleRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	static class SmlRunProperty extends GenericRunProperty {

		private String value;

		SmlRunProperty(List<XMLEvent> events) {
			super(events);
			value = super.value();
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof SmlRunProperty)) return false;

			return eventEquals(getEvents().get(0), runProperty.getEvents().get(0))
					&& Objects.equals(value, ((SmlRunProperty) runProperty).value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public String toString() {
			return SmlRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}

		/**
		 * Get the defined default value of the SpreadsheetML run property.
		 *
		 * @return the default value
		 */
		public String getDefaultValue() {
			return SmlPropertyName.fromString(
				getEvents().get(0).asStartElement().getName().getLocalPart()
			).getDefaultValue();
		}
	}

	// DrawingML may have run properties as embedded attributes on the
	// run property start element. (eg, <a:rPr lang="fr-FR"/>)
	static class AttributeRunProperty extends RunProperty {
		private QName name;
		private String value;

		AttributeRunProperty(QName name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		protected boolean equalsProperty(RunProperty rp) {
			if (!(rp instanceof AttributeRunProperty)) return false;
			AttributeRunProperty other = (AttributeRunProperty) rp;

			return Objects.equals(getName(), other.getName())
					&& Objects.equals(value(), other.value());
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, value);
		}

		@Override
		public List<XMLEvent> getEvents() {
			// There are no events associated with this, since they are part of the
			// RunProperties start element
			return Collections.emptyList();
		}

		@Override
		public QName getName() {
			return name;
		}

		@Override
		public String value() {
			return value;
		}

		@Override
		public String toString() {
			return "AttributeRunProperty(" + name + "=" + value + ")";
		}
	}

	static class BooleanAttributeRunProperty extends AttributeRunProperty {
		private boolean value;

		BooleanAttributeRunProperty(final QName name, final String value) {
			super(name, value);
			this.value = XMLEventHelpers.booleanAttributeTrueValues().contains(value);
		}

		public boolean booleanValue() {
			return this.value;
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof BooleanAttributeRunProperty)) return false;
			BooleanAttributeRunProperty other = (BooleanAttributeRunProperty) runProperty;

			return Objects.equals(getName(), other.getName())
				&& Objects.equals(this.value, other.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(getName(), value);
		}

		@Override
		public String toString() {
			return BooleanAttributeRunProperty.class.getSimpleName() + "(" + getName().toString() + "=" + value + ")";
		}
	}

	/**
	 * This run property handles one of {@link RunPropertyFactory#DML_HYPERLINK_NAMES}.
	 * <p>
	 * Full example of a paragraph that has a {@code hlinkClick}:
	 *
	 * <pre>
 	 * {@code <a:r>
     * 		<a:rPr lang="de-DE" dirty="0" smtClean="0"/>
     * 		<a:t>This is a </a:t>
	 * </a:r>
	 * <a:r>
	 *     <a:rPr lang="de-DE" dirty="0" smtClean="0">
	 *         <a:hlinkClick r:id="rId2" action="ppaction://hlinkpres?slideindex=1&amp;slidetitle="/>
	 *     </a:rPr>
	 *     <a:t>link</a:t>
	 * </a:r>
	 * <a:r>
	 *     	<a:rPr lang="de-DE" dirty="0" smtClean="0"/>
	 *     	<a:t>.</a:t>
	 * </a:r>}
	 * </pre>
	 * </p>
	 */
	static class HyperlinkRunProperty extends GenericRunProperty {
		private static final QName ID = Namespaces.DocumentRelationships.getQName("id");

		HyperlinkRunProperty(List<XMLEvent> events) {
			super(events);
		}

		@Override
		public String value() {
			StartElement startElement = getEvents().get(0).asStartElement();
			// the link target may be set directly as "action"
			if (startElement.getAttributeByName(DML_HYPERLINK_ACTION) != null) {
				return startElement.getAttributeByName(DML_HYPERLINK_ACTION).getValue();
			}
			// or it is added as relation id to be found as "Target" in the matching slideX.xml.rels
			if (startElement.getAttributeByName(ID) != null) {
				// we do not need the exact link value, but we know, we have a link, so we need a
				// value so the link is not skipped
				return startElement.getAttributeByName(ID).getValue();
			}
			return null;
		}
	}

	static class FontsRunProperty extends RunProperty implements MergeableRunProperty {
		private RunFonts runFonts;

		FontsRunProperty(RunFonts runFonts) {
			this.runFonts = runFonts;
		}

		RunFonts getRunFonts() {
			return runFonts;
		}

		public void apply(final FontMappings fontMappings) {
			this.runFonts.apply(fontMappings);
		}

		@Override
		public List<XMLEvent> getEvents() {
			return runFonts.getEvents();
		}

		@Override
		public QName getName() {
			return getEvents().get(0).asStartElement().getName();
		}

		@Override
		public String value() {
			return null;
		}

		@Override
		public boolean canBeMerged(MergeableRunProperty runProperty) {
			if (!(runProperty instanceof FontsRunProperty)) {
				return false;
			}

			return runFonts.canBeMerged(((FontsRunProperty) runProperty).runFonts);
		}

		@Override
		public MergeableRunProperty merge(MergeableRunProperty runProperty) {
			return new FontsRunProperty(
				runFonts.merge(((FontsRunProperty) runProperty).runFonts)
			);
		}

		@Override
		protected boolean equalsProperty(RunProperty runProperty) {
			if (!(runProperty instanceof FontsRunProperty)) {
				return false;
			}

			return runFonts.equals(((FontsRunProperty) runProperty).runFonts);
		}

		@Override
		public int hashCode() {
			return runFonts.hashCode();
		}

		@Override
		public String toString() {
			return FontsRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}

	static final class FontRunProperty extends GenericRunProperty {
		static final Set<String> DML_NAMES = new HashSet<>(
			Arrays.asList(
				"latin",
				"ea",
				"cs",
				"sym"
			)
		);
		static final String SML_NAME = "rFont";
		static final QName TYPEFACE = new QName(
			Namespace.EMPTY,
			"typeface",
			Namespace.PREFIX_EMPTY
		);
		private final QName VAL = new QName(
			Namespace.EMPTY,
			"val",
			Namespace.PREFIX_EMPTY
		);

		private final XMLEventFactory eventFactory;

		FontRunProperty(final List<XMLEvent> events, final XMLEventFactory eventFactory) {
			super(events);
			this.eventFactory = eventFactory;
		}

		@Override
		public void apply(final FontMappings fontMappings) {
			final StartElement startElement = getEvents().get(0).asStartElement();
			final String value = value();
			final String newValue = fontMappings.targetFontFor(value);
			if (value.equals(newValue)) {
				return; // no adjustment needed
			}
			final List<Attribute> attributes = new LinkedList<>();
			final Iterator iterator = startElement.getAttributes();
			while (iterator.hasNext()) {
				final Attribute attribute = (Attribute) iterator.next();
				if (TYPEFACE.equals(attribute.getName()) || VAL.equals(attribute.getName())) {
					attributes.add(
						this.eventFactory.createAttribute(
							attribute.getName(),
							newValue
						)
					);
					continue;
				}
				attributes.add(attribute);
			}
			getEvents().set(
				0,
				this.eventFactory.createStartElement(
					startElement.getName(),
					attributes.iterator(),
					null
				)
			);
		}

		@Override
		public String value() {
			final StartElement startElement = getEvents().get(0).asStartElement();
			if (DML_NAMES.contains(startElement.getName().getLocalPart())) {
				return getAttributeValue(startElement, TYPEFACE);
			} else if (SML_NAME.equals(startElement.getName().getLocalPart())) {
				return getAttributeValue(startElement, VAL);
			}
			throw new UnsupportedOperationException();
		}

		@Override
		protected boolean equalsProperty(final RunProperty rp) {
			if (!(rp instanceof FontRunProperty)) return false;
			return super.equalsProperty(rp);
		}

		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public String toString() {
			return FontRunProperty.class.getSimpleName() + "(" + XMLEventSerializer.serialize(getEvents()) + ")";
		}
	}
}
