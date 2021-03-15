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

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createEndMarkupComponent;
import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createGeneralMarkupComponent;
import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createStartMarkupComponent;
import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isSectionPropertiesStartEvent;

/**
 * Part handler for styled text (Word document parts, PPTX slides) that
 * follow the styled run model.
 */
class StyledTextPart extends TranslatablePart {
	protected final IdGenerator nestedBlockId;
	protected final IdGenerator textUnitId;
	protected StyleDefinitions styleDefinitions;
	protected StyleOptimisation styleOptimisation;

	private StrippableAttributes drawingDirectionStrippableAttributes;
	private StrippableAttributes drawingBodyDirectionStrippableAttributes;
	private StrippableAttributes drawingRunPropertiesStrippableAttributes;
	private StrippableAttributes tableRowRevisions;
	private StrippableAttributes sectionPropertiesRevisions;

	private SkippableElements tablePropertiesChangeSkippableElements;
	private SkippableElements emptySkippableElements;
	private SkippableElements revisionPropertyChangeSkippableElements;
	private SkippableElements revisionPropertyTableRowInsertedSkippableElements;
	private SkippableElements revisionPropertyTableRowDeletedSkippableElements;
	private SkippableElements bookmarkSkippableElements;
	private SkippableElements moveToRangeSkippableElements;
	private SkippableElements moveFromRangeSkippableElements;
	private SkippableElements moveToRangeEndSkippableElements;

	private Collection<XMLEvent> prioritisedEvents;
	private XMLEventReader defaultEventReader;
	protected XMLEventReader eventReader;

	protected Iterator<Event> filterEventIterator;
	protected String documentId;
	protected String subDocumentId;
	protected LocaleId sourceLocale;

	protected Markup markup;

	StyledTextPart(
			final Document.General generalDocument,
			final ZipEntry entry,
			final StyleDefinitions styleDefinitions,
			final StyleOptimisation styleOptimisation) {
		super(generalDocument, entry);
		this.styleDefinitions = styleDefinitions;
		this.styleOptimisation = styleOptimisation;
		this.nestedBlockId = new IdGenerator(null);
		this.textUnitId = new IdGenerator(entry.getName(), IdGenerator.TEXT_UNIT);

		this.markup = new Block.Markup(new Markup.General(new ArrayList<>()));
	}

	/**
	 * Opens this part and performs any initial processing.  Returns the
	 * first event for this part.  In this case, it's a START_SUBDOCUMENT
	 * event.
	 *
	 * @return Event
	 *
	 * @throws IOException
	 * @throws XMLStreamException
     */
	@Override
	public Event open() throws IOException, XMLStreamException {
		this.documentId = this.generalDocument.documentId();
		this.subDocumentId = this.generalDocument.nextSubDocumentId();
		this.sourceLocale = this.generalDocument.sourceLocale();

		/*
		 * Process the XML event stream, simplifying as we go.  Non-block content is
		 * written as a document part.  Blocks are parsed, then converted into TextUnit structures.
		 */
		final XMLEventReader defaultEventReader = generalDocument.inputFactory().createXMLEventReader(
			new InputStreamReader(
				new BufferedInputStream(generalDocument.inputStreamFor(entry)),
				StandardCharsets.UTF_8
			)
		);

		return open(documentId, subDocumentId, defaultEventReader);
	}

	// Package-private for test.  XXX This is an artifact of the overall PartHandler
	// interface needing work.
	Event open(String documentId, String subDocumentId, XMLEventReader defaultEventReader) throws IOException, XMLStreamException {
		this.prioritisedEvents = new LinkedList<>();
		this.defaultEventReader = defaultEventReader;

		this.eventReader = new PrioritisedXMLEventReader(
			new ConsumableXMLEventsReader(new XMLEventsReader(this.prioritisedEvents)),
			this.defaultEventReader
		);

		this.drawingDirectionStrippableAttributes = new StrippableAttributes.DrawingDirection(
			this.generalDocument.eventFactory()
		);
		this.drawingBodyDirectionStrippableAttributes = new StrippableAttributes.DrawingBodyDirection(
			this.generalDocument.eventFactory()
		);
		this.drawingRunPropertiesStrippableAttributes = new StrippableAttributes.DrawingRunProperties(
			this.generalDocument.conditionalParameters(),
			this.generalDocument.eventFactory()
		);
		tableRowRevisions = new StrippableAttributes.WordTableRowRevisions(
			this.generalDocument.eventFactory()
		);
		sectionPropertiesRevisions = new StrippableAttributes.WordSectionPropertiesRevisions(
			this.generalDocument.eventFactory()
		);

		tablePropertiesChangeSkippableElements = new SkippableElements.Property(
			new SkippableElements.Default(
				SkippableElement.RevisionProperty.TABLE_PROPERTIES_CHANGE
			),
			this.generalDocument.conditionalParameters()
		);
		emptySkippableElements = new SkippableElements.Empty();
		revisionPropertyChangeSkippableElements = new SkippableElements.RevisionProperty(
			new SkippableElements.Property(
				new SkippableElements.Default(
					SkippableElement.RevisionProperty.SECTION_PROPERTIES_CHANGE,
					SkippableElement.RevisionProperty.TABLE_GRID_CHANGE,
					SkippableElement.RevisionProperty.TABLE_PROPERTIES_EXCEPTIONS_CHANGE,
					SkippableElement.RevisionProperty.TABLE_ROW_PROPERTIES_CHANGE,
					SkippableElement.RevisionProperty.TABLE_CELL_PROPERTIES_CHANGE
				),
				this.generalDocument.conditionalParameters()
			),
			this.generalDocument.conditionalParameters()
		);
		this.revisionPropertyTableRowInsertedSkippableElements =
			new SkippableElements.RevisionProperty(
				new SkippableElements.Property(
					new SkippableElements.Default(
						SkippableElement.RevisionProperty.TABLE_ROW_INSERTED
					),
					this.generalDocument.conditionalParameters()
				),
				this.generalDocument.conditionalParameters()
			);
		this.revisionPropertyTableRowDeletedSkippableElements =
			new SkippableElements.RevisionProperty(
				new SkippableElements.Property(
					new SkippableElements.Default(
						SkippableElement.RevisionProperty.TABLE_ROW_DELETED
					),
					this.generalDocument.conditionalParameters()
				),
				this.generalDocument.conditionalParameters()
			);
		this.bookmarkSkippableElements = new SkippableElements.BookmarkCrossStructure(
			new SkippableElements.CrossStructure(
				new SkippableElements.Default(
					SkippableElement.GeneralCrossStructure.BOOKMARK_START,
					SkippableElement.GeneralCrossStructure.BOOKMARK_END
				)
			),
			SkippableElements.BookmarkCrossStructure.SKIPPABLE_BOOKMARK_NAME
		);
		this.moveToRangeSkippableElements = new SkippableElements.RevisionCrossStructure(
			new SkippableElements.CrossStructure(
				new SkippableElements.Default(
					SkippableElement.RevisionCrossStructure.MOVE_TO_RANGE_START,
					SkippableElement.RevisionCrossStructure.MOVE_TO_RANGE_END
				)
			)
		);
		this.moveFromRangeSkippableElements = new SkippableElements.MoveFromRevisionCrossStructure(
			new SkippableElements.RevisionCrossStructure(
				new SkippableElements.CrossStructure(
					new SkippableElements.Default(
							SkippableElement.RevisionCrossStructure.MOVE_FROM_RANGE_START,
							SkippableElement.RevisionCrossStructure.MOVE_FROM_RANGE_END
					)
				)
			),
			""
		);
		this.moveToRangeEndSkippableElements = new SkippableElements.Default(
			SkippableElement.RevisionCrossStructure.MOVE_TO_RANGE_END
		);

		try {
			process();
		}
		finally {
			if (eventReader != null) {
				eventReader.close();
			}
		}
		return createStartSubDocumentEvent(documentId, subDocumentId);
	}

	/**
	 * Check to see if the current element starts a block.  Can be overridden.
	 */
	protected boolean isStyledBlockStartEvent(XMLEvent e) {
		return XMLEventHelpers.isParagraphStartEvent(e);
	}

	protected void preProcess(XMLEvent e) throws IOException, XMLStreamException {
		// can be overridden
	}

	protected boolean isCurrentBlockTranslatable() {
		// can be overridden
		// here blocks are always translatable
		return true;
	}

	private void process() throws IOException, XMLStreamException {
		final TableMarkup delayedTableMarkup = new TableMarkup(new Markup.General(new LinkedList<>()));
		Block mergeableBlock = null;
		StartElement parentStartElement = null;

		while (eventReader.hasNext()) {
			XMLEvent e = eventReader.nextEvent();
			preProcess(e);

			if (isStyledBlockStartEvent(e)) {
				addMarkupToDocumentPart(delayedTableMarkup);
				delayedTableMarkup.removeComponents();
				flushDocumentPart();
				StartElementContext startElementContext = createStartElementContext(
					e.asStartElement(),
					eventReader,
					generalDocument.eventFactory(),
					this.generalDocument.conditionalParameters(),
					sourceLocale
				);
				Block block = new BlockParser(startElementContext, nestedBlockId, styleDefinitions, styleOptimisation).parse();
				if (block.isSkipped()) {
					continue;
				}
				prioritiseXMLEvents(block.deferredEvents());

				if (null != mergeableBlock) {
					block.mergeWith(mergeableBlock);
					mergeableBlock = null;
				}
				if (block.isMergeable()) {
					mergeableBlock = block;
					continue;
				}
				block.optimiseStyles();
				if (block.isHidden()) {
					documentPartEvents.addAll(block.getEvents());
					continue;
				}

				mapToEvents(block);
			}
			else if (XMLEventHelpers.isTableStartEvent(e)) {
				delayedTableMarkup.addComponent(
					createStartMarkupComponent(generalDocument.eventFactory(), e.asStartElement())
				);
			}
			else if (XMLEventHelpers.isTablePropertiesStartEvent(e)) {
				delayedTableMarkup.addComponent(
					new MarkupComponentParser().parseBlockProperties(
						createStartElementContext(
							e.asStartElement(),
							eventReader,
							generalDocument.eventFactory(),
							this.generalDocument.conditionalParameters()
						),
						this.drawingDirectionStrippableAttributes,
						this.tablePropertiesChangeSkippableElements
					)
				);
			}
			else if (XMLEventHelpers.isTableGridStartEvent(e)) {
				parentStartElement = e.asStartElement();
				delayedTableMarkup.addComponent(
					createStartMarkupComponent(generalDocument.eventFactory(), e.asStartElement())
				);
			}
			else if (XMLEventHelpers.isTableGridEndEvent(e)) {
				parentStartElement = null;
				delayedTableMarkup.addComponent(createEndMarkupComponent(e.asEndElement()));
			}
			else if (XMLEventHelpers.isTableRowStartEvent(e)) {
				e = tableRowRevisions.strip(e.asStartElement());
				parentStartElement = e.asStartElement();
				delayedTableMarkup.addComponent(
					createStartMarkupComponent(generalDocument.eventFactory(), e.asStartElement())
				);
			}
			else if (XMLEventHelpers.isTextBodyStartEvent(e)) {
				delayedTableMarkup.addComponent(
					createStartMarkupComponent(generalDocument.eventFactory(), e.asStartElement())
				);
			}
			else if (XMLEventHelpers.isTextBodyPropertiesStartEvent(e)) {
				delayedTableMarkup.addComponent(
					new MarkupComponentParser().parseBlockProperties(
						createStartElementContext(
							this.drawingBodyDirectionStrippableAttributes.strip(e.asStartElement()),
							eventReader,
							generalDocument.eventFactory(),
							this.generalDocument.conditionalParameters()
						),
						this.drawingRunPropertiesStrippableAttributes,
						this.emptySkippableElements
					)
				);
			}
			else if (XMLEventHelpers.isTextBodyEndEvent(e)) {
				final MarkupComponent mc = createEndMarkupComponent(e.asEndElement());
				if (!delayedTableMarkup.empty()) {
					delayedTableMarkup.addComponent(mc);
				} else {
					addMarkupComponentToDocumentPart(mc);
				}
			}
			else if (XMLEventHelpers.isTableRowEndEvent(e)) {
				parentStartElement = null;
				final MarkupComponent mc = createEndMarkupComponent(e.asEndElement());
				if (!delayedTableMarkup.empty()) {
					delayedTableMarkup.addComponent(mc);
				} else {
					addMarkupComponentToDocumentPart(mc);
				}
			}
			else if (XMLEventHelpers.isTableEndEvent(e)) {
				if (!delayedTableMarkup.empty()) {
					// This means that there has not been found any translatable content (block)
					// in the last table - removing it completely.
					delayedTableMarkup.removeComponentsFromLastWith(XMLEventHelpers.LOCAL_TABLE);
				} else {
					addMarkupComponentToDocumentPart(createEndMarkupComponent(e.asEndElement()));
				}
			}
			else if (e.isStartElement()
				&& PowerpointStyleDefinitions.NAMES.contains(e.asStartElement().getName().getLocalPart())) {
				final PowerpointStyleDefinitionsReader reader = new PowerpointStyleDefinitionsReader(
					this.generalDocument.conditionalParameters(),
					this.generalDocument.eventFactory(),
					this.eventReader,
					e.asStartElement(),
					e.asStartElement().getName().getLocalPart()
				);
				final PowerpointStyleDefinitions powerpointStyleDefinitions =
					new PowerpointStyleDefinitions(this.generalDocument.eventFactory());
				powerpointStyleDefinitions.readWith(reader);
				if (!delayedTableMarkup.empty()) {
					delayedTableMarkup.addMarkup(powerpointStyleDefinitions.toMarkup());
				} else {
					addMarkupToDocumentPart(powerpointStyleDefinitions.toMarkup());
				}
			}
			else if (e.isStartElement() && revisionPropertyChangeSkippableElements.canBeSkipped(e.asStartElement(), parentStartElement)) {
				StartElementContext startElementContext = createStartElementContext(e.asStartElement(), parentStartElement, eventReader, generalDocument.eventFactory(), this.generalDocument.conditionalParameters());
				revisionPropertyChangeSkippableElements.skip(startElementContext);
			}
			else if (e.isStartElement() && this.revisionPropertyTableRowInsertedSkippableElements.canBeSkipped(e.asStartElement(), parentStartElement)) {
				final StartElementContext startElementContext = createStartElementContext(e.asStartElement(), parentStartElement, eventReader, generalDocument.eventFactory(), this.generalDocument.conditionalParameters());
				this.revisionPropertyTableRowInsertedSkippableElements.skip(startElementContext);
			}
			else if (e.isStartElement() && this.revisionPropertyTableRowDeletedSkippableElements.canBeSkipped(e.asStartElement(), parentStartElement)) {
				final Iterator<MarkupComponent> iterator =
					delayedTableMarkup.componentsIteratorAtLastWith(XMLEventHelpers.LOCAL_TABLE_ROW);
				final MarkupComponent row = iterator.next();
				iterator.remove();
				delayedTableMarkup.removeComponentsWith(iterator);
				this.revisionPropertyTableRowDeletedSkippableElements.skip(
					createStartElementContext(
						row.getEvents().get(0).asStartElement(),
						parentStartElement,
						eventReader,
						generalDocument.eventFactory(),
						this.generalDocument.conditionalParameters()
					)
				);
				parentStartElement = null;
			}
			else if (e.isStartElement() && this.bookmarkSkippableElements.canBeSkipped(e.asStartElement(), null)) {
				final StartElementContext startElementContext = createStartElementContext(e.asStartElement(), null, eventReader, generalDocument.eventFactory(), this.generalDocument.conditionalParameters());
				this.bookmarkSkippableElements.skip(startElementContext);
			}
			else if (e.isStartElement() && this.moveToRangeSkippableElements.canBeSkipped(e.asStartElement(), null)) {
				final StartElementContext startElementContext = createStartElementContext(e.asStartElement(), null, eventReader, generalDocument.eventFactory(), this.generalDocument.conditionalParameters());
				this.moveToRangeSkippableElements.skip(startElementContext);
			}
			else if (e.isStartElement() && this.moveFromRangeSkippableElements.canBeSkipped(e.asStartElement(), null)) {
				final StartElementContext startElementContext = createStartElementContext(e.asStartElement(), null, eventReader, generalDocument.eventFactory(), this.generalDocument.conditionalParameters());
				this.moveFromRangeSkippableElements.skip(startElementContext);
			}
			else if (e.isStartElement() && this.moveToRangeEndSkippableElements.canBeSkipped(e.asStartElement(), null)) {
				final StartElementContext startElementContext = createStartElementContext(e.asStartElement(), null, eventReader, generalDocument.eventFactory(), this.generalDocument.conditionalParameters());
				this.moveToRangeEndSkippableElements.skip(startElementContext);
			}
			else if (!delayedTableMarkup.empty()) {
				if (delayedTableMarkup.lastComponentGeneral()) {
					delayedTableMarkup.addToLastComponent(e);
				} else {
					delayedTableMarkup.addComponent(
						new MarkupComponent.General(
							new ArrayList<>(Collections.singletonList(e))
						)
					);
				}
			}
			else {
				if (isSectionPropertiesStartEvent(e)) {
					e = sectionPropertiesRevisions.strip(e.asStartElement());
				}
				addEventToDocumentPart(e);
			}
		}
		if (null != mergeableBlock) {
			mergeableBlock.optimiseStyles();
			mapToEvents(mergeableBlock);
		}
		flushDocumentPart();
		filterEvents.add(new Event(EventType.END_SUBDOCUMENT, new Ending(subDocumentId)));
		filterEventIterator = filterEvents.iterator();
	}

	private void mapToEvents(final Block block) {
		final List<ITextUnit> textUnits = new BlockTextUnitMapper(
			textUnitId,
			this.generalDocument.eventFactory(),
			block
		).map();
		if (textUnits.isEmpty() || !isCurrentBlockTranslatable()) {
			addBlockChunksToDocumentPart(block.getChunks());
		} else {
			for (ITextUnit tu : textUnits) {
				filterEvents.add(new Event(EventType.TEXT_UNIT, tu));
			}
		}
	}

	protected void flushDocumentPart() {
		if (!documentPartEvents.isEmpty()) {
			this.markup.addComponent(createGeneralMarkupComponent(documentPartEvents));
			documentPartEvents = new ArrayList<>();
		}

		if (!this.markup.components().isEmpty()) {
			DocumentPart documentPart = new DocumentPart(documentPartIdGenerator.createId(), false);
			documentPart.setSkeleton(new MarkupSkeleton(this.markup));
			this.markup = new Block.Markup(new Markup.General(new ArrayList<>()));

			filterEvents.add(new Event(EventType.DOCUMENT_PART, documentPart));
		}
	}

	private void prioritiseXMLEvents(final Collection<XMLEvent> deferredEvents) {
		this.prioritisedEvents.addAll(deferredEvents);
		this.eventReader = new PrioritisedXMLEventReader(
			new ConsumableXMLEventsReader(new XMLEventsReader(this.prioritisedEvents)),
			defaultEventReader
		);
	}

	private void addMarkupToDocumentPart(final Markup markup) {
		if (!documentPartEvents.isEmpty()) {
			this.markup.addComponent(createGeneralMarkupComponent(documentPartEvents));
			documentPartEvents = new ArrayList<>();
		}
		this.markup.addMarkup(markup);
	}

	private void addMarkupComponentToDocumentPart(MarkupComponent markupComponent) {
		if (!documentPartEvents.isEmpty()) {
			this.markup.addComponent(createGeneralMarkupComponent(documentPartEvents));
			documentPartEvents = new ArrayList<>();
		}
		this.markup.addComponent(markupComponent);
	}

	protected void addBlockChunksToDocumentPart(List<Chunk> chunks) {
		for (Chunk chunk : chunks) {
			if (chunk instanceof Block.Markup) {
				for (MarkupComponent markupComponent : ((Block.Markup) chunk).components()) {
					addMarkupComponentToDocumentPart(markupComponent);
				}
				continue;
			}

			documentPartEvents.addAll(chunk.getEvents());
		}
	}

	@Override
	public boolean hasNextEvent() {
		return filterEventIterator.hasNext();
	}

	@Override
	public Event nextEvent() {
		return filterEventIterator.next();
	}

	@Override
	public void close() {
	}

	@Override
	public void logEvent(Event e) {
	}
}
