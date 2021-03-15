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

import net.sf.okapi.common.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static net.sf.okapi.filters.openxml.MarkupComponentFactory.createEndMarkupComponent;
import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.WPML_PROPERTY_VANISH;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.gatherEvents;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isLineBreakStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isParagraphEndEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isParagraphPropertiesStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isRunStartEvent;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isWhitespace;

/**
 * Given an event stream and a block start element, this will parse and return
 * the block object.
 */
final class BlockParser implements Parser<Block> {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private static final String LOCAL_SIMPLE_FIELD = "fldSimple";

	private final StartElementContext startElementContext;
	private final IdGenerator nestedBlockIdGenerator;
	private final StyleDefinitions styleDefinitions;
	private final StyleOptimisation styleOptimisation;

	private final StrippableAttributes drawingRunPropertiesStrippableAttributes;
	private final StrippableAttributes wordParagraphRevisions;
	private final SkippableElements emptySkippableElements;
	private final SkippableElements defaultSkippableElements;
	private final SkippableElements blockPropertiesSkippableElements;
	private final BlockSkippableElements blockSkippableElements;
	private final RunBuilderSkipper runBuilderSkipper;
	private final Block.Builder builder;
	private final Queue<XMLEvent> deferredEvents;
	private String paragraphStyle;

	BlockParser(StartElementContext startElementContext, IdGenerator nestedBlockIdGenerator,
				StyleDefinitions styleDefinitions, StyleOptimisation styleOptimisation) {
		this.startElementContext = startElementContext;
		this.nestedBlockIdGenerator = nestedBlockIdGenerator;
		this.styleDefinitions = styleDefinitions;
		this.styleOptimisation = styleOptimisation;

		this.drawingRunPropertiesStrippableAttributes = new StrippableAttributes.DrawingRunProperties(
			startElementContext.getConditionalParameters(),
			startElementContext.getEventFactory()
		);
		this.wordParagraphRevisions = new StrippableAttributes.WordParagraphRevisions(
			startElementContext.getEventFactory()
		);

		this.emptySkippableElements = new SkippableElements.Empty();
		this.defaultSkippableElements = new SkippableElements.Default();
		this.blockPropertiesSkippableElements = new SkippableElements.RevisionProperty(
			new SkippableElements.Property(
				new SkippableElements.Default(
					SkippableElement.RunProperty.RUN_PROPERTY_RTL_DML,
					SkippableElement.RunProperty.RUN_PROPERTY_LANGUAGE,
					SkippableElement.RunProperty.RUN_PROPERTY_NO_SPELLING_OR_GRAMMAR,
					SkippableElement.RevisionProperty.RUN_PROPERTY_INSERTED_PARAGRAPH_MARK,
					SkippableElement.RevisionProperty.RUN_PROPERTY_MOVED_PARAGRAPH_TO,
					SkippableElement.RevisionProperty.RUN_PROPERTY_MOVED_PARAGRAPH_FROM,
					SkippableElement.RevisionProperty.PARAGRAPH_PROPERTIES_CHANGE,
					SkippableElement.RevisionProperty.RUN_PROPERTIES_CHANGE
				),
				startElementContext.getConditionalParameters()
			),
			startElementContext.getConditionalParameters()
		);

		blockSkippableElements = new BlockSkippableElements(startElementContext);
		runBuilderSkipper = new RunBuilderSkipper();

		builder = new Block.Builder();
		deferredEvents = new LinkedList<>();
	}

	private static boolean isSimpleFieldStartEvent(XMLEvent e) {
		return XMLEventHelpers.isStartElement(e, LOCAL_SIMPLE_FIELD);
	}

	private void addRunsToBuilder(Block.Builder builder, RunMerger runMerger) throws XMLStreamException {
		for (Chunk chunk : runMerger.getRuns()) {
				builder.add(chunk);
		}
		runMerger.reset();
	}

	private void parseRunContainer(ChunkContainer chunkContainer, StartElement runContainerStart) throws XMLStreamException {
		RunContainer.Builder rcb = new RunContainer.Builder(
			this.startElementContext.getEventFactory(),
			runContainerStart
		);
		RunMerger runMerger = new RunMerger();

		rcb.addType(RunContainer.Type.fromString(runContainerStart.getName().getLocalPart()));
		rcb.addToStartMarkup(
			MarkupComponentFactory.createStartMarkupComponent(
				this.startElementContext.getEventFactory(),
				runContainerStart
			)
		);

		while (startElementContext.getEventReader().hasNext()) {
			XMLEvent e = startElementContext.getEventReader().nextEvent();
			// Check for end of container
			if (e.isEndElement() && runContainerStart.getName().equals(e.asEndElement().getName())) {
				rcb.add(runMerger.getRuns());
				runMerger.reset();
				rcb.addToEndMarkup(
					MarkupComponentFactory.createEndMarkupComponent(e.asEndElement())
				);
				chunkContainer.add(rcb.build());
				return;
			} else if (isRunStartEvent(e)) {
				processRun(runMerger, e.asStartElement());
			} else if (RunContainer.isStart(e)) {
				rcb.add(runMerger.getRuns());
				runMerger.reset();
				parseRunContainer(rcb, e.asStartElement());
			} else if (RunContainer.isPropertiesStart(e)) {
				rcb.addToStartMarkup(
					new MarkupComponentParser().parseBlockProperties(
						createStartElementContext(
							e.asStartElement(),
							startElementContext
						),
						this.drawingRunPropertiesStrippableAttributes,
						this.emptySkippableElements
					)
				);
			} else if (RunContainer.isContentStart(e)) {
				rcb.addToStartMarkup(
					MarkupComponentFactory.createStartMarkupComponent(
						this.startElementContext.getEventFactory(),
						e.asStartElement()
					)
				);
			} else if (RunContainer.isContentEnd(e)) {
				rcb.addToEndMarkup(
					MarkupComponentFactory.createEndMarkupComponent(e.asEndElement())
				);
			}
		}
		throw new IllegalStateException("Invalid content? Unterminated run container");
	}

	public Block parse() throws XMLStreamException {
		log("startBlock: " + startElementContext.getStartElement());
		builder.addToMarkup(
			new MarkupComponent.ParagraphStart(
				startElementContext.getEventFactory(),
				styleDefinitions,
				wordParagraphRevisions.strip(startElementContext.getStartElement())
			)
		);
		RunMerger runMerger = new RunMerger();
		while (startElementContext.getEventReader().hasNext()) {
			XMLEvent e = startElementContext.getEventReader().nextEvent();
			if (isParagraphPropertiesStartEvent(e)) {
				StartElementContext blockPropertiesElementContext = createStartElementContext(
					e.asStartElement(),
					startElementContext
				);
				final ParagraphBlockProperties blockProperties = new MarkupComponentParser()
						.parseParagraphBlockProperties(
							blockPropertiesElementContext,
							this.drawingRunPropertiesStrippableAttributes,
							this.blockPropertiesSkippableElements
						);

				if (!blockProperties.isEmpty()) {
					if (blockProperties.containsRunPropertyDeletedParagraphMark()) {
						builder.mergeable(true);
					} else {
						builder.addToMarkup(blockProperties);
					}
				}

				paragraphStyle = blockProperties.paragraphStyle();
				runMerger.setParagraphStyle(paragraphStyle);

				// Handle Style exclude or include
				if (paragraphStyle != null) {
					if (startElementContext.getConditionalParameters().getTranslateWordInExcludeStyleMode()) {
						builder.hidden(startElementContext.getConditionalParameters().isWordExcludedStyle(paragraphStyle));
					} else {
						builder.hidden(!startElementContext.getConditionalParameters().isWordExcludedStyle(paragraphStyle));
					}
				}

				// If present at block level, handle highlight excluded
				String highlightColor = blockProperties.highlightColor();
				if (highlightColor != null) {
					if (startElementContext.getConditionalParameters().getTranslateWordInExcludeHighlightMode() && !builder.hidden()) {
						builder.hidden(startElementContext.getConditionalParameters().isWordHighlightColor(highlightColor));
					} else {
						builder.hidden(!startElementContext.getConditionalParameters().isWordHighlightColor(highlightColor));
					}
				}

				// If present at block level, handle text color excluded
				String textColor = blockProperties.textColor();
				if (textColor != null) {
					if (startElementContext.getConditionalParameters().getTranslateWordExcludeColors() && !builder.hidden()) {
						builder.hidden(startElementContext.getConditionalParameters().isWordExcludedColor(textColor));
					}
				}
			}
			else if (isRunStartEvent(e)) {
				processRun(runMerger, e.asStartElement());

				if (!this.deferredEvents.isEmpty() && isParagraphEndEvent(this.deferredEvents.peek())) {
					addRunsToBuilder(this.builder, runMerger);
					this.builder.addToMarkup(createEndMarkupComponent(this.deferredEvents.poll().asEndElement()));
					this.builder.addDeferredEvents(this.deferredEvents);
					this.builder.styleOptimisation(this.styleOptimisation);
					return builder.build();
				}
			}
			else if (startElementContext.getConditionalParameters().getAddLineSeparatorCharacter() &&
					 isLineBreakStartEvent(e) && runMerger.hasRunBuilder()) {
				runMerger.addToRunTextInRunBuilder(String.valueOf(startElementContext.getConditionalParameters().getLineSeparatorReplacement()));
				defaultSkippableElements.skip(createStartElementContext(e.asStartElement(), startElementContext));
			}
			else if (RunContainer.isStart(e)) {
				StartElement runContainerStart = e.asStartElement();
				// Flush previous run, if any
				addRunsToBuilder(builder, runMerger);
				// Build the run container and add it as a single chunk
				parseRunContainer(builder, runContainerStart);
			}
			else if (isSimpleFieldStartEvent(e)) {
				addRunsToBuilder(builder, runMerger);
				StartElementContext simpleFieldElementContext = createStartElementContext(e.asStartElement(), startElementContext);
				for (XMLEvent fldEvent : gatherEvents(simpleFieldElementContext)) {
					builder.addToMarkup(fldEvent);
				}
				// Flush it so it will all end up as a single code with nothing else
				builder.flushMarkup();
			}
			else if (e.isStartElement() && RunProperties.END_PARA_RPR.equals(e.asStartElement().getName().getLocalPart())) {
				final RunProperties endParaRunProperties = new RunPropertiesParser(
					StartElementContextFactory.createStartElementContext(
						e.asStartElement(),
						this.startElementContext
					),
					new RunSkippableElements(startElementContext)
				).parse();
				addRunsToBuilder(builder, runMerger);
				if (builder.chunksEmpty()) {
					builder.flushMarkup();
				}
				builder.addToMarkup(endParaRunProperties);
			}
			else {
				if (blockSkippableElements.skip(e)) {
					if (blockSkippableElements.isBorderCrossed()) {
						builder.skipped(true);
						builder.styleOptimisation(new StyleOptimisation.Bypass());
						return builder.build();
					}
					continue;
				}

				// Trim non-essential whitespace
				if (!isWhitespace(e)) {
					// Flush any outstanding run if there's any markup
					addRunsToBuilder(builder, runMerger);

					// Check for end of block
					if (e.isEndElement() && startElementContext.getStartElement().getName().equals(e.asEndElement().getName())) {
						log("End block: " + e);
						if (builder.chunksEmpty()) {
							builder.flushMarkup();
						}
						builder.addToMarkup(createEndMarkupComponent(e.asEndElement()));
						builder.styleOptimisation(styleOptimisation);
						return builder.build();
					} else {
						builder.addToMarkup(e);
					}
				}
			}
		}
		throw new IllegalStateException("Invalid content? Unterminated paragraph");
	}

	private void processRun(RunMerger runMerger, StartElement startEl) throws XMLStreamException {
		StartElementContext runElementContext = createStartElementContext(startEl, startElementContext);
		RunBuilder runBuilder = new RunParser(runElementContext, nestedBlockIdGenerator, styleDefinitions, styleOptimisation,
			paragraphStyle, builder.hidden()).parse();

		this.deferredEvents.addAll(runBuilder.deferredEvents());

		if (runBuilderSkipper.canSkip(runBuilder)) {
			return;
		}

		clarifyVisibility(runBuilder);

		builder.runName(startEl.getName());
		builder.textName(runBuilder.getTextName());
		builder.mergeable(runBuilder.containsMergeableMarkup());

		runMerger.add(runBuilder);
	}

	private void clarifyVisibility(RunBuilder runBuilder) {
		// If translateWordHidden parameter is turned on, no runs should be hidden:
		if (startElementContext.getConditionalParameters().getTranslateWordHidden()){
			return;
		}

		final List<Property> combinedRunProperties = styleDefinitions.combinedRunProperties(paragraphStyle, runBuilder.getRunStyle(), runBuilder.getRunProperties()).properties();
		for (final Property property : combinedRunProperties) {
			// Skip all unrelated properties:
			if (!WPML_PROPERTY_VANISH.getLocalPart().equals(property.getName().getLocalPart())) {
				continue;
			}
			// If vanish property is present but the value is false, run shouldn't be hidden:
			if (property instanceof RunProperty.WpmlToggleRunProperty
                    && !((RunProperty.WpmlToggleRunProperty) property).getToggleValue()) {
				return;
			}
			// If vanish property is present and the value is not false, run should be hidden:
			runBuilder.setHidden(true);
			return;
		}
	}

	private void log(String s) {
		LOGGER.debug(s);
	}
}
