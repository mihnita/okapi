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
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * A markup structure, such as a hyperlink and smartTag , that can contain
 * multiple child runs and nested containers.
 *
 * Runs within this container can be simplified and consolidated, but
 * can't be consolidated with runs outside the container.  When exposed
 * as ITextUnit content, the container boundaries should appear as a single
 * set of paired codes.
 */
final class RunContainer implements Chunk {
	private static final EnumSet<Type> RUN_CONTAINER_TYPES = EnumSet.of(
		Type.HYPERLINK,
		Type.SMART_TAG,
		Type.STRUCTURED_DOCUMENT_TAG
	);

	private static final String STRUCTURAL_DOCUMENT_TAG_CONTENT = "sdtContent";

	private final XMLEventFactory eventFactory;
	private final StartElement startElement;
	private final Type type;
	private final Markup startMarkup;
	private final List<Chunk> chunks;
	private final Markup endMarkup;

	private RunContainer(
		final XMLEventFactory eventFactory,
		final StartElement startElement,
		final Type type,
		final Markup startMarkup,
		final List<Chunk> chunks,
		final Markup endMarkup
	) {
		this.eventFactory = eventFactory;
		this.startElement = startElement;
		this.type = type;
		this.startMarkup = startMarkup;
		this.chunks = chunks;
		this.endMarkup = endMarkup;
	}

	static boolean isStart(XMLEvent e) {
		return e.isStartElement() && RUN_CONTAINER_TYPES.contains(RunContainer.Type.fromString(e.asStartElement().getName().getLocalPart()));
	}

	static boolean isPropertiesStart(XMLEvent e) {
		return e.isStartElement() && (
			BlockProperties.SMART_TAG_PROPERTIES.equals(e.asStartElement().getName().getLocalPart())
				|| BlockProperties.STRUCTURAL_DOCUMENT_TAG_PROPERTIES.equals(e.asStartElement().getName().getLocalPart())
				|| BlockProperties.STRUCTURAL_DOCUMENT_TAG_END_PROPERTIES.equals(e.asStartElement().getName().getLocalPart())
		);
	}

	static boolean isContentStart(XMLEvent e) {
		return e.isStartElement() && STRUCTURAL_DOCUMENT_TAG_CONTENT.equals(e.asStartElement().getName().getLocalPart());
	}

	static boolean isContentEnd(XMLEvent e) {
		return e.isEndElement() && STRUCTURAL_DOCUMENT_TAG_CONTENT.equals(e.asEndElement().getName().getLocalPart());
	}

	Type type() {
		return type;
	}

	List<XMLEvent> startMarkupEvents() {
		return this.startMarkup.getEvents();
	}

	List<Chunk> getChunks() {
		return this.chunks;
	}

	List<XMLEvent> endMarkupEvents() {
		return this.endMarkup.getEvents();
	}

	boolean containsVisibleText() {
		for (Chunk chunk : getChunks()) {
			if (chunk instanceof Run) {
				if (((Run) chunk).containsVisibleText()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The container assumes the properties of the first run are its "default",
	 * so that no additional code is needed to write them.
	 */
	RunProperties getDefaultRunProperties() {
		return (chunks.size() > 0 && chunks.get(0) instanceof Run)
			? ((Run) chunks.get(0)).getProperties()
			: new RunProperties.Default(
				this.eventFactory,
				this.startElement.getName().getPrefix(),
				this.startElement.getName().getNamespaceURI(),
				RunProperties.RPR
			);
	}

	RunProperties getDefaultCombinedRunProperties() {
		return (chunks.size() > 0 && chunks.get(0) instanceof Run)
			? ((Run) chunks.get(0)).getCombinedProperties()
			: new RunProperties.Default(
				this.eventFactory,
				this.startElement.getName().getPrefix(),
				this.startElement.getName().getNamespaceURI(),
				RunProperties.RPR
			);
	}

	@Override
	public List<XMLEvent> getEvents() {
		List<XMLEvent> events = new ArrayList<>();
		events.addAll(this.startMarkup.getEvents());
		for (Chunk chunk: chunks) {
			events.addAll(chunk.getEvents());
		}
		events.addAll(this.endMarkup.getEvents());
		return events;
	}

	@Override
	public String toString() {
		return "RunContainer(" + this.type + ", "+ chunks.size() +
				")[" + chunks  + "]";
	}

	/**
	 * Provides run container types.
	 */
	enum Type {
		HYPERLINK("hyperlink"),
		SMART_TAG("smartTag"),
		STRUCTURED_DOCUMENT_TAG("sdt"),
		UNSUPPORTED("");

		private final String value;

		Type(String value) {
			this.value = value;
		}

		String value() {
			return value;
		}

		static Type fromString(String tagName) {
			if (tagName == null) {
				return UNSUPPORTED;
			}

			for (Type type: values()) {
				if (type.value().equals(tagName)) {
					return type;
				}
			}
			return Type.UNSUPPORTED;
		}
	}

    static final class Builder implements ChunkContainer {
		private final XMLEventFactory eventFactory;
		private final StartElement startElement;
		private Type type;
		private Markup startMarkup = new Block.Markup(new Markup.General(new ArrayList<>()));
		private List<Chunk> chunks = new ArrayList<>();
		private Markup endMarkup = new Block.Markup(new Markup.General(new ArrayList<>()));

		Builder(final XMLEventFactory eventFactory, final StartElement startElement) {
			this.eventFactory = eventFactory;
			this.startElement = startElement;
		}

		void addType(final Type type) {
			this.type = type;
		}

		void addToStartMarkup(final MarkupComponent component) {
			this.startMarkup.addComponent(component);
		}

		void add(final List<Chunk> chunks) {
			this.chunks.addAll(chunks);
		}

		@Override
		public void add(final Chunk chunk) {
			this.chunks.add(chunk);
		}

		void addToEndMarkup(final MarkupComponent component) {
			this.endMarkup.addComponent(component);
		}

		RunContainer build() {
			return new RunContainer(
				this.eventFactory,
				this.startElement,
				this.type,
				this.startMarkup,
				this.chunks,
				this.endMarkup
			);
		}
	}
}
