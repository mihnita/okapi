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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLEventFactory;

import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.fontmappings.FontMappings;
import net.sf.okapi.common.layerprovider.ILayerProvider;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.EndSubfilter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.StartSubfilter;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import static net.sf.okapi.filters.openxml.Namespaces.DrawingML;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DRAWING_ALIGNMENT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DRAWING_ALIGNMENT_LEFT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DRAWING_ALIGNMENT_RIGHT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_BIDIRECTIONAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_BIDI_VISUAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_PROPERTY_LANGUAGE;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_READING_ORDER;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_RIGHT_TO_LEFT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_RTL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_RTL_COL;

class StyledTextSkeletonWriter implements ISkeletonWriter {
	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private static final String EMPTY_STRING = "";
	private static final String TRUE_VALUES_ARE_EMPTY = "True values are empty";
	private static final String FALSE_VALUES_ARE_EMPTY = "False values are empty";

	private final String partName;
	private final LocaleId sourceLocale;
	private LocaleId targetLocale;
	private final ConditionalParameters cparams;
	private final XMLEventFactory eventFactory;
	private final FontMappings applicableFontMappings;
	private final GenericSkeletonWriter genericSkeletonWriter;

	private IdGenerator nestedBlockIds = new IdGenerator(null);
	private Map<String, String> processedReferents = new HashMap<>();
	private Deque<Referring> referrings = new ArrayDeque<>();

	StyledTextSkeletonWriter(
		final String partName,
		final LocaleId sourceLocale,
		final LocaleId targetLocale,
		final ConditionalParameters cparams,
		final XMLEventFactory eventFactory,
		final FontMappings applicableFontMappings,
		final GenericSkeletonWriter genericSkeletonWriter
	) {
		this.partName = partName;
		this.sourceLocale = sourceLocale;
		this.targetLocale = targetLocale;
		this.cparams = cparams;
		this.eventFactory = eventFactory;
		this.applicableFontMappings = applicableFontMappings;
		this.genericSkeletonWriter = genericSkeletonWriter;
	}

	@Override
	public void close() {
	}

	@Override
	public String processStartDocument(LocaleId outputLocale, String outputEncoding, ILayerProvider layer,
			EncoderManager encoderManager, StartDocument resource) {
		this.targetLocale = outputLocale;
		this.genericSkeletonWriter.setOutputLoc(outputLocale);
		return XML_HEADER;
	}

	@Override
	public String processEndDocument(Ending resource) {
		return EMPTY_STRING;
	}

	@Override
	public String processStartSubDocument(StartSubDocument resource) {
		return XML_HEADER;
	}

	@Override
	public String processEndSubDocument(Ending resource) {
		return EMPTY_STRING;
	}

	@Override
	public String processStartGroup(StartGroup resource) {
		return EMPTY_STRING;
	}

	@Override
	public String processEndGroup(Ending resource) {
		return EMPTY_STRING;
	}

	@Override
	public String processTextUnit(ITextUnit tu) {
		if ((!ConditionalParameters.EMPTY_SUBFILTER_CONFIGURATION.equals(cparams.getSubfilter())
                	&& null == tu.getSkeleton())
				|| tu.getSkeleton() instanceof GenericSkeleton) {
			// handle subfiltered text units or text units of StringItems
			return genericSkeletonWriter.processTextUnit(tu);
		}
		TextContainer target = getTargetForOutput(tu);
		// Now I need to transform the block by replacing the run text with
		// the content in the target.
		// I will need to track codes and map them against the run code stack
		// produced in the original mapper.
		// The block contains chunks that look like
		// - [start block]
		// - [good stuff]: 0 or more
		// - [end block]
		// The [good stuff] is all represented somehow in the codes and the text.
		// For every character of text in the target TextContainer, we need to use
		// the most recent open code to indicate our run styling.  If there is none,
		// a default style is used.
		// I'll need to know what run element to use, maybe, which is probably info that should
		// be stored in the block.

		final String serialized;
		// Translatable attribute text TUs has no skeleton, as it's always a referent.
		List<Chunk> chunks = null;
		XMLEventSerializer xmlWriter = new XMLEventSerializer();
		if (tu.getSkeleton() != null) {
			if (tu.getSkeleton() instanceof BlockSkeleton) {
				BlockSkeleton skel = ((BlockSkeleton)tu.getSkeleton());
				Block block = skel.block();
				// This should always have > 2 entries, as otherwise this would have been serialized
				// as a document part.
				chunks = block.getChunks();
				((Markup) chunks.get(0)).apply(this.applicableFontMappings);
				((Markup) chunks.get(chunks.size() - 1)).apply(this.applicableFontMappings);
				Nameable nameableMarkupComponent = ((Markup) chunks.get(0)).nameableComponent();
				final ClarificationContext clarificationContext = new ClarificationContext(
					this.cparams,
					new CreationalParameters(
						eventFactory,
						nameableMarkupComponent.getName().getPrefix(),
						nameableMarkupComponent.getName().getNamespaceURI()
					),
					this.sourceLocale,
					this.targetLocale
				);
				final String propertyDefaultValue = XMLEventHelpers.booleanAttributeTrueValues().stream()
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(TRUE_VALUES_ARE_EMPTY));
				final String propertyDefaultValueWhenAbsent = XMLEventHelpers.booleanAttributeFalseValues().stream()
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(FALSE_VALUES_ARE_EMPTY));
				final ClarifiableAttribute rtlColClarifiableAttribute = new ClarifiableAttribute(
					Namespace.PREFIX_EMPTY,
					LOCAL_RTL_COL,
					XMLEventHelpers.booleanAttributeTrueValues()
				);
				final AttributesClarification bypassAttributesClarification = new AttributesClarification.Bypass();
				final ElementsClarification bypassElementsClarification = new ElementsClarification.Bypass();
				final AttributesClarification rtlAttributesClarification = new AttributesClarification.Default(
					clarificationContext,
					new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_RTL, XMLEventHelpers.booleanAttributeTrueValues())
				);
				final AttributesClarification tablePropertiesAttributesClarification;
				final ElementsClarification tablePropertiesElementsClarification;
				final AttributesClarification textBodyPropertiesAttributesClarification;
				final AttributesClarification paragraphPropertiesAttributesClarification;
				final ElementsClarification paragraphPropertiesElementsClarification;
				final ElementsClarification runPropertiesElementsClarification;
				if (Namespace.PREFIX_A.equals(clarificationContext.creationalParameters().getPrefix())) {
					tablePropertiesAttributesClarification = rtlAttributesClarification;
					tablePropertiesElementsClarification = bypassElementsClarification;
					textBodyPropertiesAttributesClarification = new AttributesClarification.Default(
						clarificationContext,
						rtlColClarifiableAttribute
					);
					paragraphPropertiesAttributesClarification = new AttributesClarification.AlignmentAndRtl(
						clarificationContext,
						Namespace.PREFIX_EMPTY,
						DRAWING_ALIGNMENT,
						DRAWING_ALIGNMENT_LEFT,
						DRAWING_ALIGNMENT_RIGHT,
						LOCAL_RTL,
						XMLEventHelpers.booleanAttributeFalseValues(),
						XMLEventHelpers.booleanAttributeTrueValues()
					);
					paragraphPropertiesElementsClarification = bypassElementsClarification;
					runPropertiesElementsClarification = bypassElementsClarification;
				} else {
					tablePropertiesAttributesClarification = bypassAttributesClarification;
					tablePropertiesElementsClarification = new ElementsClarification.TableBlockPropertyDefault(
						clarificationContext,
						LOCAL_BIDI_VISUAL
					);
					textBodyPropertiesAttributesClarification = new AttributesClarification.Default(
						new ClarificationContext(
							this.cparams,
							new CreationalParameters(
								this.eventFactory,
								Namespace.PREFIX_A,
								DrawingML.getURI() // todo #859: should be a dynamically obtained
							),
							this.sourceLocale,
							this.targetLocale
						),
						rtlColClarifiableAttribute
					);
					paragraphPropertiesAttributesClarification = bypassAttributesClarification;
					paragraphPropertiesElementsClarification = new ElementsClarification.ParagraphBlockPropertyDefault(
						clarificationContext,
						LOCAL_BIDIRECTIONAL,
						propertyDefaultValue,
						propertyDefaultValueWhenAbsent,
						XMLEventHelpers.booleanAttributeFalseValues(),
						XMLEventHelpers.booleanAttributeTrueValues()
					);
					runPropertiesElementsClarification = new ElementsClarification.RunPropertyLang(
						new ElementsClarification.RunPropertyDefault(
							clarificationContext,
							LOCAL_RTL,
							propertyDefaultValue,
							propertyDefaultValueWhenAbsent,
							XMLEventHelpers.booleanAttributeFalseValues(),
							XMLEventHelpers.booleanAttributeTrueValues()
						),
						LOCAL_PROPERTY_LANGUAGE,
						LOCAL_BIDIRECTIONAL
					);
				}
				final BlockPropertiesClarification tablePropertiesClarification =
					new BlockPropertiesClarification.Default(
						clarificationContext,
						BlockProperties.TBL_PR,
						new MarkupComponentClarification.Default(
							tablePropertiesAttributesClarification,
							tablePropertiesElementsClarification
						)
					);
				final BlockPropertiesClarification textBodyPropertiesClarification =
					new BlockPropertiesClarification.Default(
						clarificationContext,
						BlockProperties.BODY_PR,
						new MarkupComponentClarification.Default(
							textBodyPropertiesAttributesClarification,
							bypassElementsClarification
						)
					);
				final BlockPropertiesClarification paragraphPropertiesClarification =
					new BlockPropertiesClarification.Paragraph(
						new BlockPropertiesClarification.Default(
							clarificationContext,
							ParagraphBlockProperties.PPR,
							new MarkupComponentClarification.Default(
								paragraphPropertiesAttributesClarification,
								paragraphPropertiesElementsClarification
							)
						)
					);
				final String rtlPropertyDefaultValue = XMLEventHelpers.booleanAttributeTrueValues().stream()
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(TRUE_VALUES_ARE_EMPTY));
				final String rtlPropertyDefaultValueWhenAbsent = XMLEventHelpers.booleanAttributeFalseValues().stream()
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(FALSE_VALUES_ARE_EMPTY));
				final MarkupClarification markupClarification = new MarkupClarification(
					new MarkupComponentClarification.Default(
						new AttributesClarification.Default(
							clarificationContext,
							new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_RIGHT_TO_LEFT, XMLEventHelpers.booleanAttributeTrueValues())
						),
						bypassElementsClarification
					),
					new MarkupComponentClarification.Default(
						new AttributesClarification.Default(
							clarificationContext,
							new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_READING_ORDER, Collections.singleton(XMLEventHelpers.LOCAL_READING_ORDER_RTL_VALUE))
						),
						bypassElementsClarification
					),
					new MarkupComponentClarification.Default(
						rtlAttributesClarification,
						bypassElementsClarification
					),
					tablePropertiesClarification,
					textBodyPropertiesClarification,
					paragraphPropertiesClarification,
					new StylesClarification.Word(
						tablePropertiesClarification,
						paragraphPropertiesClarification,
						new RunPropertiesClarification.Default(
							clarificationContext,
							new MarkupComponentClarification.Default(
								bypassAttributesClarification,
								new ElementsClarification.RunPropertyLang(
									new ElementsClarification.RunPropertyDefault(
										clarificationContext,
										LOCAL_RTL,
										rtlPropertyDefaultValue,
										rtlPropertyDefaultValueWhenAbsent,
										XMLEventHelpers.booleanAttributeFalseValues(),
										XMLEventHelpers.booleanAttributeTrueValues()
									),
									LOCAL_PROPERTY_LANGUAGE,
									LOCAL_BIDIRECTIONAL
								)
							)
						)
					)
				);
				markupClarification.performFor((Markup) chunks.get(0));
				xmlWriter.add(chunks.get(0));
				new BlockTextUnitWriter(
					cparams,
					eventFactory,
					skel,
					xmlWriter,
					new RunPropertiesClarification.Default(
						clarificationContext,
						new MarkupComponentClarification.Default(
							bypassAttributesClarification,
							runPropertiesElementsClarification
						)
					)
				).write(target);

			} else {
				throw new IllegalArgumentException("TextUnit " + tu.getId() +
						" has no associated block content");
			}

			// Handle the final one
			xmlWriter.add(chunks.get(chunks.size() - 1));
			serialized = xmlWriter.toString();
		}
		else {
			serialized = xmlWriter.getAttributeEncoder().encode(target.toString(), EncoderContext.INLINE);
		}

		return processReferences(tu, serialized);
	}

	private String processReferences(final ITextUnit tu, final String serialized) {
		final String output;

		// If this TU is a referent of something, it means it was part of a nested block.
		// We need to save it up for reinsertion into some other TU later on, when we find
		// the correct reference.
		if (tu.isReferent()) {
			if (TextUnitProperties.integer(tu, TextUnitMapper.REFERENCES) != 0) {
				this.referrings.push(
					new Referring(
						serialized,
						TextUnitProperties.integer(tu, TextUnitMapper.REFERENCES)
					)
				);
				output = EMPTY_STRING;
			} else {
				this.processedReferents.put(nestedBlockIds.createId(), serialized);
				this.referrings.peek().foundReferents++;

				if (this.referrings.peek().isLastFoundReferent()) {
					final String resolved = resolveReferences(this.referrings.pop().serialized);
					if (!this.referrings.isEmpty()) {
						// the current text unit is a referent without its own
						// references, so it is safe to invoke the method
						// to finalise the processing
						output = processReferences(tu, resolved);
					} else {
						output = resolved;
					}
				} else {
					output = EMPTY_STRING;
				}
			}
		}
		else {
			if (TextUnitProperties.integer(tu, TextUnitMapper.REFERENCES) == 0) {
				output = serialized;
			} else {
			    this.referrings.push(
					new Referring(
						serialized,
						TextUnitProperties.integer(tu, TextUnitMapper.REFERENCES)
					)
				);
				output = EMPTY_STRING;
			}
		}

		return output;
	}

	private String resolveReferences(String original) {
		// TODO get the StringBuilder directly from the XMLEvent Serializer
		StringBuilder sb = new StringBuilder(original);
		for (Object[] markerInfo = TextFragment.getRefMarker(sb); markerInfo != null;
					  markerInfo = TextFragment.getRefMarker(sb)) {
			String processedReferent = processedReferents.get(markerInfo[0]);
			sb.replace((int)markerInfo[1], (int)markerInfo[2], processedReferent);
		}
		return sb.toString();
	}

	private TextContainer getTargetForOutput(ITextUnit tu) {
		// disallow empty targets

		if (targetLocale == null) {
			return tu.getSource();
		}

		TextContainer trgCont = tu.getTarget(targetLocale);

		if (trgCont == null || trgCont.isEmpty()) {
			return tu.getSource();
		}

		return trgCont;
	}

	@Override
	public String processDocumentPart(DocumentPart documentPart) {
		if (documentPart.getSkeleton() instanceof GenericSkeleton) {
			return genericSkeletonWriter.processDocumentPart(documentPart);
		}

		MarkupSkeleton markupSkeleton = (MarkupSkeleton) documentPart.getSkeleton();
		Markup markup = markupSkeleton.getMarkup();

		markup.apply(this.applicableFontMappings);
		Nameable nameableMarkupComponent = markup.nameableComponent();

		if (null != nameableMarkupComponent) {
			// do care about a markup with the start markup component only,
			// as otherwise there is nothing to clarify at all
			final ClarificationContext clarificationContext = new ClarificationContext(
				this.cparams,
				new CreationalParameters(
					eventFactory,
					nameableMarkupComponent.getName().getPrefix(),
					nameableMarkupComponent.getName().getNamespaceURI()
				),
				this.sourceLocale,
				this.targetLocale
			);
			final String propertyDefaultValue = XMLEventHelpers.booleanAttributeTrueValues().stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(TRUE_VALUES_ARE_EMPTY));
			final String propertyDefaultValueWhenAbsent = XMLEventHelpers.booleanAttributeFalseValues().stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(FALSE_VALUES_ARE_EMPTY));
			final ClarifiableAttribute rtlColClarifiableAttribute = new ClarifiableAttribute(
				Namespace.PREFIX_EMPTY,
				LOCAL_RTL_COL,
				XMLEventHelpers.booleanAttributeTrueValues()
			);
			final AttributesClarification bypassAttributesClarification = new AttributesClarification.Bypass();
			final ElementsClarification bypassElementsClarification = new ElementsClarification.Bypass();
			final AttributesClarification rtlAttributesClarification = new AttributesClarification.Default(
				clarificationContext,
				new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_RTL, XMLEventHelpers.booleanAttributeTrueValues())
			);
			final AttributesClarification tablePropertiesAttributesClarification;
			final ElementsClarification tablePropertiesElementsClarification;
			final AttributesClarification textBodyPropertiesAttributesClarification;
			final AttributesClarification paragraphPropertiesAttributesClarification;
			final ElementsClarification paragraphPropertiesElementsClarification;
			if (Namespace.PREFIX_A.equals(clarificationContext.creationalParameters().getPrefix())) {
				tablePropertiesAttributesClarification = rtlAttributesClarification;
				tablePropertiesElementsClarification = bypassElementsClarification;
				textBodyPropertiesAttributesClarification = new AttributesClarification.Default(
					clarificationContext,
					rtlColClarifiableAttribute
				);
				paragraphPropertiesAttributesClarification = new AttributesClarification.AlignmentAndRtl(
					clarificationContext,
					Namespace.PREFIX_EMPTY,
					DRAWING_ALIGNMENT,
					DRAWING_ALIGNMENT_LEFT,
					DRAWING_ALIGNMENT_RIGHT,
					LOCAL_RTL,
					XMLEventHelpers.booleanAttributeFalseValues(),
					XMLEventHelpers.booleanAttributeTrueValues()
				);
				paragraphPropertiesElementsClarification = bypassElementsClarification;
			} else {
				tablePropertiesAttributesClarification = bypassAttributesClarification;
				tablePropertiesElementsClarification = new ElementsClarification.TableBlockPropertyDefault(
					clarificationContext,
					LOCAL_BIDI_VISUAL
				);
				textBodyPropertiesAttributesClarification = new AttributesClarification.Default(
					new ClarificationContext(
						this.cparams,
						new CreationalParameters(
							this.eventFactory,
							Namespace.PREFIX_A,
							DrawingML.getURI() // todo #859: should be a dynamically obtained
						),
						this.sourceLocale,
						this.targetLocale
					),
					rtlColClarifiableAttribute
				);
				paragraphPropertiesAttributesClarification = bypassAttributesClarification;
				paragraphPropertiesElementsClarification = new ElementsClarification.ParagraphBlockPropertyDefault(
					clarificationContext,
					LOCAL_BIDIRECTIONAL,
					propertyDefaultValue,
					propertyDefaultValueWhenAbsent,
					XMLEventHelpers.booleanAttributeFalseValues(),
					XMLEventHelpers.booleanAttributeTrueValues()
				);
			}
			final BlockPropertiesClarification tablePropertiesClarification =
				new BlockPropertiesClarification.Default(
					clarificationContext,
					BlockProperties.TBL_PR,
					new MarkupComponentClarification.Default(
						tablePropertiesAttributesClarification,
						tablePropertiesElementsClarification
					)
				);
			final BlockPropertiesClarification textBodyPropertiesClarification =
				new BlockPropertiesClarification.Default(
					clarificationContext,
					BlockProperties.BODY_PR,
					new MarkupComponentClarification.Default(
						textBodyPropertiesAttributesClarification,
						bypassElementsClarification
					)
				);
			final BlockPropertiesClarification paragraphPropertiesClarification =
				new BlockPropertiesClarification.Paragraph(
					new BlockPropertiesClarification.Default(
						clarificationContext,
						ParagraphBlockProperties.PPR,
						new MarkupComponentClarification.Default(
							paragraphPropertiesAttributesClarification,
							paragraphPropertiesElementsClarification
						)
					)
				);
			final MarkupClarification markupClarification = new MarkupClarification(
				new MarkupComponentClarification.Default(
					new AttributesClarification.Default(
						clarificationContext,
						new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_RIGHT_TO_LEFT, XMLEventHelpers.booleanAttributeTrueValues())
					),
					bypassElementsClarification
				),
				new MarkupComponentClarification.Default(
					new AttributesClarification.Default(
						clarificationContext,
						new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_READING_ORDER, Collections.singleton(XMLEventHelpers.LOCAL_READING_ORDER_RTL_VALUE))
					),
					bypassElementsClarification
				),
				new MarkupComponentClarification.Default(
					rtlAttributesClarification,
					bypassElementsClarification
				),
				tablePropertiesClarification,
				textBodyPropertiesClarification,
				paragraphPropertiesClarification,
				new StylesClarification.Word(
					tablePropertiesClarification,
					paragraphPropertiesClarification,
					new RunPropertiesClarification.Default(
						clarificationContext,
						new MarkupComponentClarification.Default(
							bypassAttributesClarification,
							new ElementsClarification.RunPropertyLang(
								new ElementsClarification.RunPropertyDefault(
									clarificationContext,
									LOCAL_RTL,
									propertyDefaultValue,
									propertyDefaultValueWhenAbsent,
									XMLEventHelpers.booleanAttributeFalseValues(),
									XMLEventHelpers.booleanAttributeTrueValues()
								),
								LOCAL_PROPERTY_LANGUAGE,
								LOCAL_BIDIRECTIONAL
							)
						)
					)
				)
			);
			markupClarification.performFor(markup);
		}

		return XMLEventSerializer.serialize(markup);
	}

	@Override
	public String processStartSubfilter(StartSubfilter resource) {
		return genericSkeletonWriter.processStartSubfilter(resource);
	}

	@Override
	public String processEndSubfilter(EndSubfilter resource) {
		return genericSkeletonWriter.processEndSubfilter(resource);
	}

	/**
	 * Provides the referring connotation.
	 * I.e. a serialized text unit may have references to other
	 * text units (referents).
	 */
	private static final class Referring {
		/**
		 * The serialized text unit.
		 */
		private final String serialized;

		/**
		 * The number of references to other text units.
		 */
		private final int references;

		/**
		 * Number of referents found during processing.
		 */
		private int foundReferents;

		/**
		 * Constructs a Referring instance.
		 *
		 * @param serialized The serialized text unit
		 * @param references The number of references to other text units
		 */
		Referring(String serialized, int references) {
			this.serialized = serialized;
			this.references = references;
		}

		/**
		 * Checks whether the found referent is the last one.
		 * @return {@code true} if this is the last found referent
		 *         {@code false} otherwise
		 */
		boolean isLastFoundReferent() {
			return references == foundReferents;
		}
	}
}
