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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * A block consists of a sequence of content chunks, each
 * of which is either BlockMarkup or a Run.
 */
class Block implements XMLEvents, Textual {
	private final List<Chunk> chunks;
	private final StyleOptimisation styleOptimisation;
	private QName runName;
	private QName textName;
	private final boolean hidden;
	private final boolean skipped;
	private final boolean mergeable;
	private final Collection<XMLEvent> deferredEvents;

	Block(
		final List<Chunk> chunks,
		final StyleOptimisation styleOptimisation,
		final QName runName,
		final QName textName,
		final boolean hidden,
		final boolean skipped,
		final boolean mergeable,
		final Collection<XMLEvent> deferredEvents
	) {
		this.chunks = chunks;
		this.styleOptimisation = styleOptimisation;
		this.runName = runName;
		this.textName = textName;
		this.hidden = hidden;
		this.skipped = skipped;
		this.mergeable = mergeable;
		this.deferredEvents = deferredEvents;
	}

	/**
	 * Return the QName of the element that contains run data in this block.
	 */
	QName getRunName() {
		return runName;
	}

	/**
	 * Return the QName of the element that contains text data in this block.
	 */
	QName getTextName() {
		return textName;
	}

	@Override
	public List<XMLEvent> getEvents() {
		List<XMLEvent> events = new ArrayList<>();
		for (XMLEvents chunk : chunks) {
			events.addAll(chunk.getEvents());
		}
		return events;
	}

	public List<Chunk> getChunks() {
		return chunks;
	}

	boolean isHidden() {
		return hidden;
	}

	boolean isSkipped() {
		return skipped;
	}

	boolean isMergeable() {
		return mergeable;
	}

	void mergeWith(final Block mergeableBlock) {
		if (mergeableBlock.chunks.size() <= 2) {
			// there is empty block with markup only
			return;
		}
		final List<Chunk> mergeableChunks = mergeableBlock.chunks.subList(1, mergeableBlock.chunks.size() - 1);
		final ListIterator<Chunk> iterator = this.chunks.listIterator(1);
		final ParagraphBlockProperties properties = ((Markup) this.chunks.get(0)).paragraphBlockProperties();
		boolean refinedInline = false;
		for (final Chunk chunk : mergeableChunks) {
			if (chunk instanceof Run && ((Run) chunk).containsParagraphBlockProperties()) {
				((Run) chunk).refineParagraphBlockProperties(properties);
				refinedInline = true;
			}
			iterator.add(chunk);
		}
		if (refinedInline) {
			((Markup) this.chunks.get(0)).updateOrAddBlockProperties(
				((Markup) mergeableBlock.chunks.get(0)).paragraphBlockProperties()
			);
		}
		if (Objects.isNull(this.runName)) {
			this.runName = mergeableBlock.runName;
		}
		if (Objects.isNull(this.textName)) {
			this.textName = mergeableBlock.textName;
		}
	}

	void optimiseStyles() throws XMLStreamException {
		this.styleOptimisation.applyTo(chunks);
	}

	boolean hasVisibleRunContent() {
		for (Chunk chunk : chunks) {
			if (chunk instanceof Run) {
				if (((Run)chunk).containsVisibleText()) {
					return true;
				}
			}
			else if (chunk instanceof RunContainer) {
				if (((RunContainer)chunk).containsVisibleText()) {
					return true;
				}
			}
		}
		return false;
	}

	Collection<XMLEvent> deferredEvents() {
		return deferredEvents;
	}

	@Override
	public String toString() {
		return "Block [" + chunks + "]";
	}

	static class Markup implements net.sf.okapi.filters.openxml.Markup, Chunk {
		private static final String START_MARKUP_COMPONENT_IS_NOT_FOUND =
			"Unexpected structure: the start markup component is not found";

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

		ParagraphBlockProperties paragraphBlockProperties() {
			for (MarkupComponent markupComponent : components()) {
				if (markupComponent instanceof ParagraphBlockProperties) {
					return (ParagraphBlockProperties) markupComponent;
				}
			}

			return null;
		}

		void updateOrAddBlockProperties(final BlockProperties blockProperties) {
			final ListIterator<MarkupComponent> componentsIterator = components().listIterator();
			while (componentsIterator.hasNext()) {
				final MarkupComponent markupComponent = componentsIterator.next();
				if (markupComponent instanceof BlockProperties) {
					componentsIterator.set(blockProperties);
					return;
				}
			}
			rewindAfterParagraphStartMarkupComponent(componentsIterator);
			componentsIterator.add(blockProperties);
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
	}

	static class Builder implements ChunkContainer {
		private List<Chunk> chunks = new ArrayList<>();
		private StyleOptimisation styleOptimisation;
		private QName runName;
		private QName textName;
		private boolean hidden = false;
		private boolean skipped = false;
		private boolean mergeable = false;
		private Collection<XMLEvent> deferredEvents = new LinkedList<>();
		private MarkupBuilder markupBuilder = new MarkupBuilder(new Markup(new net.sf.okapi.filters.openxml.Markup.General(new ArrayList<>())));

		@Override
		public void add(final Chunk chunk) {
			flushMarkup();
			this.chunks.add(chunk);
		}

		void runName(final QName runName) {
			if (this.runName == null) {
				this.runName = runName;
			}
		}

		void textName(final QName textName) {
			if (this.textName == null) {
				this.textName = textName;
			}
		}

		void styleOptimisation(final StyleOptimisation styleOptimisation) {
			this.styleOptimisation = styleOptimisation;
		}

		boolean hidden() {
			return this.hidden;
		}

		void hidden(final boolean hidden) {
			this.hidden = hidden;
		}

		void skipped(final boolean skipped) {
			this.skipped = skipped;
		}

		void mergeable(final boolean mergeable) {
			if (!this.mergeable) {
				this.mergeable = mergeable;
			}
		}

		void addDeferredEvents(final Collection<XMLEvent> deferredEvents) {
			this.deferredEvents.addAll(deferredEvents);
		}

		void addToMarkup(final XMLEvent event) {
			this.markupBuilder.add(event);
		}

		void addToMarkup(final MarkupComponent markupComponent) {
			this.markupBuilder.add(markupComponent);
		}

		boolean chunksEmpty() {
			return this.chunks.isEmpty();
		}

		void flushMarkup() {
			final Markup markup = (Markup) this.markupBuilder.build();
			if (!markup.components().isEmpty()) {
				this.chunks.add(markup);
				this.markupBuilder = new MarkupBuilder(new Markup(new net.sf.okapi.filters.openxml.Markup.General(new ArrayList<>())));
			}
		}

		Block build() {
			flushMarkup();
			return new Block(
				this.chunks,
				this.styleOptimisation,
				this.runName,
				this.textName,
				this.hidden,
				this.skipped,
				this.mergeable,
				this.deferredEvents
			);
		}
    }
}
