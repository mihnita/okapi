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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

interface PresentationFragments {
	List<String> slideMasterNames();
	List<String> notesMasterNames();
	List<String> slideNames();
	StyleDefinitions defaultTextStyle();
	void readWith(final XMLEventReader eventReader) throws XMLStreamException;

	class Default implements PresentationFragments {
		private static final String PRESENTATION = "presentation";
		private static final String SLIDE_MASTER_ID = "sldMasterId";
		private static final String NOTES_MASTER_ID = "notesMasterId";
		private static final String SLIDE_ID = "sldId";
		private static final String ID = "id";

		private final ConditionalParameters conditionalParameters;
		private final XMLEventFactory eventFactory;
		private final Relationships relationships;
		private final List<String> slideMasterNames;
		private final List<String> notesMasterNames;
		private final List<String> slideNames;
		private StyleDefinitions defaultTextStyle;

		private QName slideMasterId;
		private QName notesMasterId;
		private QName slideId;
		private QName id;

		Default(
			final ConditionalParameters conditionalParameters,
			final XMLEventFactory eventFactory,
			final Relationships relationships
		) {
			this(
				conditionalParameters,
				eventFactory,
				relationships,
				new ArrayList<>(),
				new ArrayList<>(),
				new ArrayList<>()
			);
		}

		Default(
			final ConditionalParameters conditionalParameters,
			final XMLEventFactory eventFactory,
			final Relationships relationships,
			final List<String> slideMasterNames,
			final List<String> notesMasterNames,
			final List<String> slideNames
		) {
			this.conditionalParameters = conditionalParameters;
			this.eventFactory = eventFactory;
			this.relationships = relationships;
			this.slideMasterNames = slideMasterNames;
			this.notesMasterNames = notesMasterNames;
			this.slideNames = slideNames;
		}

		@Override
		public List<String> slideMasterNames() {
			return this.slideMasterNames;
		}

		@Override
		public List<String> notesMasterNames() {
			return this.notesMasterNames;
		}

		@Override
		public List<String> slideNames() {
			return slideNames;
		}

		@Override
		public StyleDefinitions defaultTextStyle() {
			return null == this.defaultTextStyle
				? new StyleDefinitions.Empty()
				: this.defaultTextStyle;
		}

		@Override
		public void readWith(final XMLEventReader eventReader) throws XMLStreamException {
			while (eventReader.hasNext()) {
				final XMLEvent e = eventReader.nextEvent();
				if (!e.isStartElement()) {
					continue;
				}
				final StartElement el = e.asStartElement();
				if (Default.PRESENTATION.equals(el.getName().getLocalPart())) {
					qualifyNames(el);
				} else if (el.getName().equals(this.slideMasterId)) {
					addRelationshipTargetFor(el, this.slideMasterNames);
				} else if (el.getName().equals(this.notesMasterId)) {
					addRelationshipTargetFor(el, this.notesMasterNames);
				} else if (el.getName().equals(this.slideId)) {
					addRelationshipTargetFor(el, this.slideNames);
				} else if (PowerpointStyleDefinitions.DEFAULT_TEXT_STYLE.equals(el.getName().getLocalPart())) {
					this.defaultTextStyle = new PowerpointStyleDefinitions(this.eventFactory);
					this.defaultTextStyle.readWith(
						new PowerpointStyleDefinitionsReader(
							this.conditionalParameters,
							this.eventFactory,
							eventReader,
							el,
							el.getName().getLocalPart()
						)
					);
				}
			}
		}

		private void qualifyNames(final StartElement startElement) {
			this.slideMasterId = new QName(
				startElement.getNamespaceURI(Namespace.PREFIX_P),
				SLIDE_MASTER_ID,
				Namespace.PREFIX_P
			);
			this.notesMasterId = new QName(
				startElement.getNamespaceURI(Namespace.PREFIX_P),
				NOTES_MASTER_ID,
				Namespace.PREFIX_P
			);
			this.slideId = new QName(
				startElement.getNamespaceURI(Namespace.PREFIX_P),
				SLIDE_ID,
				Namespace.PREFIX_P
			);
			this.id = new QName(
				startElement.getNamespaceURI(Namespace.PREFIX_R),
				ID,
				Namespace.PREFIX_R
			);
		}

		private void addRelationshipTargetFor(final StartElement startElement, final List<String> names) {
			final Attribute id = startElement.getAttributeByName(this.id);
			if (id != null) {
				names.add(relationshipTargetFor(id.getValue()));
			}
		}

		private String relationshipTargetFor(final String id) {
			final Relationships.Rel rel = this.relationships.getRelById(id);
			if (rel == null) {
				throw new IllegalStateException(
					"A non-existent relationship is requested: " + id
				);
			}
			return rel.target;
		}
	}
}
