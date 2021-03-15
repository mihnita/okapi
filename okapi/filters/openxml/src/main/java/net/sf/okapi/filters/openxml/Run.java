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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Representation of a parsed text run.
 */
class Run implements Chunk {
	private static final String START_MARKUP_COMPONENT_IS_NOT_FOUND =
			"Unexpected structure: the start markup component is not found";

	private StartElement startEvent;
	private EndElement endEvent;
	private RunProperties runProperties;
	private RunProperties combinedProperties;
	private List<Chunk> bodyChunks;
	private List<Textual> nestedTextualItems;
	private boolean isHidden;

	Run(StartElement startEvent, EndElement endEvent, RunProperties runProperties, RunProperties combinedProperties,
		List<Chunk> bodyChunks, List<Textual> nestedTextualItems, boolean isHidden) {

		this.startEvent = startEvent;
		this.endEvent = endEvent;
		this.runProperties = runProperties;
		this.combinedProperties = combinedProperties;
		this.bodyChunks = bodyChunks;
		this.nestedTextualItems = nestedTextualItems;
		this.isHidden = isHidden;
	}

	RunProperties getProperties() {
		return runProperties;
	}

	/**
	 * Refines run properties.
	 *
	 * @param runProperties The run properties
	 */
	void refineRunProperties(final List<Property> runProperties) {
		for (final Property runProperty : runProperties) {
			this.runProperties.remove(runProperty);
		}
	}

	RunProperties getCombinedProperties() {
		return combinedProperties;
	}

	List<Chunk> getBodyChunks() {
		return bodyChunks;
	}

	List<Textual> getNestedTextualItems() {
		return nestedTextualItems;
	}

	/**
	 * Return true if this run contains visible text.
	 */
	boolean containsVisibleText() {
		if (isHidden) {
			return false;
		}
		for (Chunk c : bodyChunks) {
			if (c instanceof RunText) {
				return true;
			}
		}
		return false;
	}

	boolean containsParagraphBlockProperties() {
		if (this.bodyChunks.get(0) instanceof Run.Markup) {
			return ((Markup) this.bodyChunks.get(0)).components().stream()
				.anyMatch(mc -> mc instanceof ParagraphBlockProperties);
		}
		return false;
	}

	void refineParagraphBlockProperties(final ParagraphBlockProperties paragraphBlockProperties) {
		final ListIterator<MarkupComponent> componentsIterator = ((Run.Markup) this.bodyChunks.get(0)).components()
				.listIterator();
		while (componentsIterator.hasNext()) {
			final MarkupComponent markupComponent = componentsIterator.next();
			if (markupComponent instanceof ParagraphBlockProperties) {
				if (null == paragraphBlockProperties) {
					componentsIterator.remove();
				} else {
					componentsIterator.set(paragraphBlockProperties);
				}
				return;
			}
		}
		rewindAfterParagraphStartMarkupComponent(componentsIterator);
		componentsIterator.add(paragraphBlockProperties);
	}

	private static void rewindAfterParagraphStartMarkupComponent(final ListIterator<MarkupComponent> iterator) {
		while (iterator.hasPrevious()) {
			if (iterator.previous() instanceof MarkupComponent.ParagraphStart) {
				iterator.next();
				return;
			}
		}
		throw new IllegalStateException(START_MARKUP_COMPONENT_IS_NOT_FOUND);
	}

	@Override
	public List<XMLEvent> getEvents() {
		List<XMLEvent> events = new ArrayList<>();
		events.add(startEvent);
		events.addAll(runProperties.getEvents());
		for (XMLEvents chunk : bodyChunks) {
			events.addAll(chunk.getEvents());
		}
		events.add(endEvent);
		return events;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + XMLEventSerializer.serialize(getEvents()) + "]";
	}

	static class RunText extends Text implements RunChunk {
		RunText(StartElement startElement, Characters text, EndElement endElement) {
			super(startElement, text, endElement);
		}
	}

	/**
	 * Marker interface to distinguish XMLEvents implementation that
	 * can be added to a Run as body content.
	 */
	interface RunChunk extends Chunk { }

	static class Markup implements net.sf.okapi.filters.openxml.Markup, RunChunk {
		private final Markup.General generalMarkup;

		Markup(final Markup.General generalMarkup) {
			this.generalMarkup = generalMarkup;
		}

		@Override
		public List<XMLEvent> getEvents() {
			return this.generalMarkup.getEvents();
		}

		@Override
		public void apply(final FontMappings fontMappings) {
			this.generalMarkup.apply(fontMappings);
		}

		@Override
		public void addComponent(final MarkupComponent component) {
			this.generalMarkup.addComponent(component);
		}

		@Override
		public void addComponents(final List<MarkupComponent> components) {
			this.generalMarkup.addComponents(components);
		}

		@Override
		public void addMarkup(final net.sf.okapi.filters.openxml.Markup markup) {
			this.generalMarkup.addMarkup(markup);
		}

		@Override
		public List<MarkupComponent> components() {
			return this.generalMarkup.components();
		}

		@Override
		public Nameable nameableComponent() {
			return this.generalMarkup.nameableComponent();
		}
	}
}
