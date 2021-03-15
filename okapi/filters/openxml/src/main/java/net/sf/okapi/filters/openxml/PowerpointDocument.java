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

import static net.sf.okapi.filters.openxml.ContentTypes.Types.Common.CORE_PROPERTIES_TYPE;
import static net.sf.okapi.filters.openxml.ContentTypes.Types.Common.PACKAGE_RELATIONSHIPS;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import net.sf.okapi.common.Event;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Drawing;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Powerpoint;
import net.sf.okapi.filters.openxml.Relationships.Rel;

class PowerpointDocument implements Document {
	private static final String SLIDE_MASTER = "/slideMaster";
	private static final String SLIDE_LAYOUT = "/slideLayout";
	private static final String COMMENTS = "/comments";
	private static final String NOTES_SLIDE = "/notesSlide";
	private static final String NOTES_MASTER = "/notesMaster";
	private static final String CHART = "/chart";
	private static final String DIAGRAM_DATA = "/diagramData";

	private static final Pattern RELS_NAME_PATTERN = Pattern.compile(".+slide\\d+\\.xml\\.rels");
	private Matcher relsNameMatcher = RELS_NAME_PATTERN.matcher("").reset();

	private final Document.General generalDocument;
	private PresentationFragments presentationFragments;
	private Enumeration<? extends ZipEntry> entries;
	private Map<String, SlideFragments> slideMasterFragmentsByName;
	private Map<String, SlideFragments> slideLayoutFragmentsByName;
	private Map<String, SlideFragments> slideFragmentsByName;
	private Map<String, SlideFragments> notesMasterFragmentsByName;
	private Map<String, SlideFragments> notesSlideFragmentsByName;

	/**
	 * Uses the slide name as key and the comment name as value.
	 */
	private Map<String, String> slidesByComment;

	/**
	 * Uses the slide name as key and the note name as value.
	 */
	private Map<String, String> slidesByNote;

	/**
	 * Uses the slide name as key and the chart name as value.
	 */
	private Map<String, String> slidesByChart;

	/**
	 * Uses the slide name as key and the diagram name as value.
	 */
	private Map<String, String> slidesByDiagramData ;

	PowerpointDocument(final Document.General generalDocument) {
		this.generalDocument = generalDocument;
	}

	@Override
	public Event open() throws IOException, XMLStreamException {
		this.presentationFragments = presentationFragments();
		this.entries = entries();
		this.slideMasterFragmentsByName = slideMasterFragments();
		this.slideLayoutFragmentsByName = slideLayoutFragments();
		this.slideFragmentsByName = slideFragments();
		this.notesMasterFragmentsByName = notesMasterFragments();
		this.notesSlideFragmentsByName = notesSlideFragments();
		this.slidesByComment = comments();
		this.slidesByNote = notes();
		this.slidesByChart = charts();
		this.slidesByDiagramData = diagramDatas();

		return this.generalDocument.startDocumentEvent();
	}

	private PresentationFragments presentationFragments() throws IOException, XMLStreamException {
		final PresentationFragments pf = new PresentationFragments.Default(
			this.generalDocument.conditionalParameters(),
			this.generalDocument.eventFactory(),
			this.generalDocument.relationshipsFor(this.generalDocument.mainPartName())
		);
		try (final Reader reader = this.generalDocument.getPartReader(this.generalDocument.mainPartName())) {
			pf.readWith(this.generalDocument.inputFactory().createXMLEventReader(reader));
		}
		return pf;
	}

	/**
	 * Do additional reordering of the entries for PPTX files to make
	 * sure that slides are parsed in the correct order.  This is done
	 * by scraping information from one of the rels files and the
	 * presentation itself in order to determine the proper order, rather
	 * than relying on the order in which things appeared in the zip.
	 * @return the sorted enum of ZipEntry
	 * @throws IOException if any error is encountered while reading the stream
	 * @throws XMLStreamException if any error is encountered while parsing the XML
	 */
	private Enumeration<? extends ZipEntry> entries() throws IOException, XMLStreamException {
		Enumeration<? extends ZipEntry> entries = this.generalDocument.entries();
		List<? extends ZipEntry> entryList = Collections.list(entries);
		entryList.sort(new ZipEntryComparator(reorderedPartNames()));
		return Collections.enumeration(entryList);
	}

	private List<String> reorderedPartNames() throws IOException, XMLStreamException {
		final List<String> names = new LinkedList<>();
		for (final String slideName : this.presentationFragments.slideNames()) {
			names.add(slideName);
			if (this.generalDocument.conditionalParameters().getReorderPowerpointNotesAndComments()) {
				final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
				names.addAll(slideRelationshipTargetsForType(slideName, namespaceUri.concat(NOTES_SLIDE)));
				names.addAll(slideRelationshipTargetsForType(slideName, namespaceUri.concat(COMMENTS)));
			}
		}
		return names;
	}

	private List<String> slideRelationshipTargetsForType(final String slideName, final String typeUri) throws IOException, XMLStreamException {
		List<Rel> rels = this.generalDocument.relationshipsFor(slideName).getRelByType(typeUri);
		return rels == null ? Collections.emptyList() :
				rels.stream()
						.map(r -> r.target)
						.collect(Collectors.toList());
	}

	private Map<String, SlideFragments> slideMasterFragments() throws IOException, XMLStreamException {
		final Map<String, SlideFragments> slideMasterFragments =
			new HashMap<>(this.presentationFragments.slideMasterNames().size());
		for (final String name : this.presentationFragments.slideMasterNames()) {
			slideMasterFragments.put(name, slideMasterFragmentsOf(name));
		}
		return slideMasterFragments;
	}

	private SlideFragments slideMasterFragmentsOf(final String partName) throws IOException, XMLStreamException {
		try (final Reader reader = this.generalDocument.getPartReader(partName)) {
			final XMLEventReader eventReader =
				this.generalDocument.inputFactory().createXMLEventReader(reader);
			while (eventReader.hasNext()) {
				final XMLEvent e = eventReader.nextEvent();
				if (e.isEndElement() && SlideMasterFragments.SLD_MASTER.equals(e.asEndElement().getName().getLocalPart())) {
					break;
				}
				if (!e.isStartElement()) {
					continue;
				}
				if (SlideMasterFragments.SLD_MASTER.equals(e.asStartElement().getName().getLocalPart())) {
					final SlideTemplateFragments slideTemplateFragments = new SlideMasterFragments(
						e.asStartElement(),
						this.generalDocument.conditionalParameters(),
						this.generalDocument.eventFactory()
					);
					slideTemplateFragments.readWith(eventReader);
					return slideTemplateFragments;
				}
			}
		}
		return new SlideMasterFragments.Empty();
	}

	private Map<String, SlideFragments> slideLayoutFragments() throws IOException, XMLStreamException {
		final List<String> slideLayoutNames = slideLayoutNames();
		final Map<String, SlideFragments> slideLayoutFragments = new HashMap<>(slideLayoutNames.size());
		for (final String name : slideLayoutNames) {
			slideLayoutFragments.put(name, slideLayoutFragmentsOf(name));
		}
		return slideLayoutFragments;
	}

	/**
	 * Examine relationship information to find all layouts that are used in
	 * a slide in this document.  Return a list of their entry names, in order.
	 * @return list of entry names.
	 * @throws XMLStreamException See {@link Document.General#relationshipsFor(String)}
	 * @throws IOException See {@link Document.General#relationshipsFor(String)}
	 */
	private List<String> slideLayoutNames() throws IOException, XMLStreamException {
		List<String> layouts = new ArrayList<>();
		final String typeUri = this.generalDocument.documentRelationshipsNamespace().uri().concat(SLIDE_LAYOUT);
		for (String slideName : this.presentationFragments.slideNames()) {
			List<Relationships.Rel> rels =
				this.generalDocument.relationshipsFor(slideName).getRelByType(typeUri);
			if (null != rels && !rels.isEmpty()) {
				layouts.add(rels.get(0).target);
			}
		}
		return layouts;
	}

	private SlideFragments slideLayoutFragmentsOf(final String partName) throws IOException, XMLStreamException {
		final List<Relationships.Rel> rels = this.generalDocument.relationshipsFor(partName)
			.getRelByType(
				this.generalDocument.documentRelationshipsNamespace().uri().concat(SLIDE_MASTER)
			);
		final SlideFragments slideMasterFragments =
			null != rels && !rels.isEmpty() && this.slideMasterFragmentsByName.containsKey(rels.get(0).target)
				? this.slideMasterFragmentsByName.get(rels.get(0).target)
				: new SlideTemplateFragments.Empty();
		try (final Reader reader = this.generalDocument.getPartReader(partName)) {
			final XMLEventReader eventReader = this.generalDocument.inputFactory().createXMLEventReader(reader);
			while (eventReader.hasNext()) {
				final XMLEvent e = eventReader.nextEvent();
				if (e.isEndElement() && SlideFragments.C_SLD.equals(e.asEndElement().getName().getLocalPart())) {
					break;
				}
				if (!e.isStartElement()) {
					continue;
				}
				if (SlideFragments.C_SLD.equals(e.asStartElement().getName().getLocalPart())) {
					final SlideFragments slideLayoutFragments = new SlideFragments.Default(
						e.asStartElement(),
						this.generalDocument.conditionalParameters(),
						this.generalDocument.eventFactory(),
						slideMasterFragments
					);
					slideLayoutFragments.readWith(eventReader);
					return slideLayoutFragments;
				}
			}
		}
		return new SlideFragments.Empty(slideMasterFragments);
	}

	private Map<String, SlideFragments> slideFragments() throws IOException, XMLStreamException {
		final Map<String, SlideFragments> slideFragments =
			new HashMap<>(this.presentationFragments.slideNames().size());
		for (final String name : this.presentationFragments.slideNames()) {
			slideFragments.put(name, slideFragmentsOf(name));
		}
		return slideFragments;
	}

	private SlideFragments slideFragmentsOf(final String partName) throws IOException, XMLStreamException {
		final List<Relationships.Rel> rels = this.generalDocument.relationshipsFor(partName)
			.getRelByType(
				this.generalDocument.documentRelationshipsNamespace().uri().concat(SLIDE_LAYOUT)
			);
		final SlideFragments slideLayoutFragments;
		if (null != rels && !rels.isEmpty() && this.slideLayoutFragmentsByName.containsKey(rels.get(0).target)) {
			slideLayoutFragments = this.slideLayoutFragmentsByName.get(rels.get(0).target);
		} else {
			slideLayoutFragments = new SlideFragments.Empty(
				new SlideTemplateFragments.Empty()
			);
		}
		try (final Reader reader = this.generalDocument.getPartReader(partName)) {
			final XMLEventReader eventReader =
				this.generalDocument.inputFactory().createXMLEventReader(reader);
			while (eventReader.hasNext()) {
				final XMLEvent e = eventReader.nextEvent();
				if (e.isEndElement() && SlideFragments.C_SLD.equals(e.asEndElement().getName().getLocalPart())) {
					break;
				}
				if (!e.isStartElement()) {
					continue;
				}
				if (SlideFragments.C_SLD.equals(e.asStartElement().getName().getLocalPart())) {
					final SlideFragments slideFragments = new SlideFragments.Default(
						e.asStartElement(),
						this.generalDocument.conditionalParameters(),
						this.generalDocument.eventFactory(),
						slideLayoutFragments
					);
					slideFragments.readWith(eventReader);
					return slideFragments;
				}
			}
		}
		return new SlideFragments.Empty(slideLayoutFragments);
	}

	private Map<String, SlideFragments> notesMasterFragments() throws IOException, XMLStreamException {
		final Map<String, SlideFragments> notesMasterFragments =
			new HashMap<>(this.presentationFragments.notesMasterNames().size());
		for (final String name : this.presentationFragments.notesMasterNames()) {
			notesMasterFragments.put(name, notesMasterFragmentsOf(name));
		}
		return notesMasterFragments;
	}

	private SlideFragments notesMasterFragmentsOf(final String partName) throws IOException, XMLStreamException {
		try (final Reader reader = this.generalDocument.getPartReader(partName)) {
			final XMLEventReader eventReader = this.generalDocument.inputFactory().createXMLEventReader(reader);
			while (eventReader.hasNext()) {
				final XMLEvent e = eventReader.nextEvent();
				if (e.isEndElement() && NotesMasterFragments.NOTES_MASTER.equals(e.asEndElement().getName().getLocalPart())) {
					break;
				}
				if (!e.isStartElement()) {
					continue;
				}
				if (NotesMasterFragments.NOTES_MASTER.equals(e.asStartElement().getName().getLocalPart())) {
					final NotesMasterFragments notesMasterFragments = new NotesMasterFragments(
						e.asStartElement(),
						this.generalDocument.conditionalParameters(),
						this.generalDocument.eventFactory()
					);
					notesMasterFragments.readWith(eventReader);
					return notesMasterFragments;
				}
			}
		}
		return new NotesMasterFragments.Empty();
	}

	private Map<String, SlideFragments> notesSlideFragments() throws IOException, XMLStreamException {
		final List<String> notesSlideNames = notesSlideNames();
		final Map<String, SlideFragments> slideFragments = new HashMap<>(notesSlideNames.size());
		for (final String name : notesSlideNames) {
			slideFragments.put(name, notesSlideFragmentsOf(name));
		}
		return slideFragments;
	}

	private List<String> notesSlideNames() throws IOException, XMLStreamException {
		final List<String> names = new ArrayList<>();
		final String typeUri = this.generalDocument.documentRelationshipsNamespace().uri().concat(NOTES_SLIDE);
		for (String slideName : this.presentationFragments.slideNames()) {
			List<Relationships.Rel> rels = this.generalDocument.relationshipsFor(slideName).getRelByType(typeUri);
			if (null != rels && !rels.isEmpty()) {
				names.add(rels.get(0).target);
			}
		}
		return names;
	}

	private SlideFragments notesSlideFragmentsOf(final String partName) throws IOException, XMLStreamException {
		final List<Relationships.Rel> rels = this.generalDocument.relationshipsFor(partName)
			.getRelByType(
				this.generalDocument.documentRelationshipsNamespace().uri().concat(NOTES_MASTER)
			);
		final SlideFragments notesMasterFragments =
			null !=rels && !rels.isEmpty() && this.notesMasterFragmentsByName.containsKey(rels.get(0).target)
				? this.notesMasterFragmentsByName.get(rels.get(0).target)
				: new NotesMasterFragments.Empty();
		try (final Reader reader = this.generalDocument.getPartReader(partName)) {
			final XMLEventReader eventReader = this.generalDocument.inputFactory().createXMLEventReader(reader);
			while (eventReader.hasNext()) {
				final XMLEvent e = eventReader.nextEvent();
				if (e.isEndElement() && SlideFragments.C_SLD.equals(e.asEndElement().getName().getLocalPart())) {
					break;
				}
				if (!e.isStartElement()) {
					continue;
				}
				if (SlideFragments.C_SLD.equals(e.asStartElement().getName().getLocalPart())) {
					final SlideFragments slideFragments = new SlideFragments.Default(
						e.asStartElement(),
						this.generalDocument.conditionalParameters(),
						this.generalDocument.eventFactory(),
						notesMasterFragments
					);
					slideFragments.readWith(eventReader);
					return slideFragments;
				}
			}
		}
		return new SlideFragments.Empty(notesMasterFragments);
	}

	/**
	 * Obtains the relationships of type {@link #COMMENTS}.
	 */
	private Map<String, String> comments() throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(this.presentationFragments.slideNames(), namespaceUri.concat(COMMENTS));
	}

	/**
	 * Obtains the relationships of type {@link #NOTES_SLIDE}.
	 */
	private Map<String, String> notes() throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(this.presentationFragments.slideNames(), namespaceUri.concat(NOTES_SLIDE));
	}

	/**
	 * Obtains the relationships of type {@link #CHART}.
	 */
	private Map<String, String> charts() throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(this.presentationFragments.slideNames(), namespaceUri.concat(CHART));
	}

	/**
	 * Obtains the relationships of type {@link #DIAGRAM_DATA}.
	 */
	private Map<String, String> diagramDatas() throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(this.presentationFragments.slideNames(), namespaceUri.concat(DIAGRAM_DATA));
	}

	@Override
	public boolean hasPostponedTranslatables() {
		return false;
	}

	@Override
	public void updatePostponedTranslatables(final String key, final String value) {
	}

	@Override
	public boolean hasNextPart() {
		return this.entries.hasMoreElements();
	}

	@Override
	public Part nextPart() throws IOException, XMLStreamException {
		final ZipEntry entry = this.entries.nextElement();
		final String contentType = this.generalDocument.contentTypeFor(entry);

		relsNameMatcher.reset(entry.getName());
		if (isRelationshipsPart(contentType) && relsNameMatcher.matches() && this.generalDocument.conditionalParameters().getExtractExternalHyperlinks()) {
			return new RelationshipsPart(
					this.generalDocument,
					entry
			);
		}

		if (!isTranslatablePart(entry)) {
			if (isModifiablePart(contentType)) {
				return new ModifiablePart(this.generalDocument, entry, this.generalDocument.inputStreamFor(entry));
			}
			return new NonModifiablePart(this.generalDocument, entry);
		}

		if (isStyledTextPart(entry)) {
			if (isSlidablePart(entry.getName(), contentType)) {
				return new SlidablePart(
					this.generalDocument,
					entry,
					slideFragmentsFor(entry.getName(), contentType)
				);
			}
			final StyleDefinitions styleDefinitions = new StyleDefinitions.Empty();
			final StyleOptimisation styleOptimisation = styleOptimisationFor(entry, styleDefinitions);
			return new StyledTextPart(
				this.generalDocument,
				entry,
				styleDefinitions,
				styleOptimisation
			);
		}

		ContentFilter contentFilter = new ContentFilter(this.generalDocument.conditionalParameters(), entry.getName());
		ParseType parseType = getParseType(contentType);
		this.generalDocument.conditionalParameters().nFileType = parseType;

		contentFilter.setUpConfig(parseType);

		// Other configuration
		return new DefaultPart(this.generalDocument, entry, contentFilter);
	}

	private static boolean isRelationshipsPart(String contentType) {
		return PACKAGE_RELATIONSHIPS.equals(contentType);
	}

	private StyleOptimisation styleOptimisationFor(final ZipEntry entry, final StyleDefinitions styleDefinitions) throws IOException, XMLStreamException {
		final Namespace namespace = this.generalDocument.namespacesOf(entry).forPrefix(Namespace.PREFIX_A);
		if (null == namespace) {
			return new StyleOptimisation.Bypass();
		}
		return new StyleOptimisation.Default(
			new StyleOptimisation.Bypass(),
			this.generalDocument.conditionalParameters(),
			this.generalDocument.eventFactory(),
			new QName(namespace.uri(), ParagraphBlockProperties.PPR, namespace.prefix()),
			new QName(namespace.uri(), RunProperties.DEF_RPR, namespace.prefix()),
			Collections.emptyList(),
			styleDefinitions
		);
	}

    private ParseType getParseType(String contentType) {
        ParseType parseType;
		if (contentType.equals(CORE_PROPERTIES_TYPE)) {
			parseType = ParseType.MSWORDDOCPROPERTIES;
		}
		else if (contentType.equals(Powerpoint.COMMENTS_TYPE)) {
			parseType = ParseType.MSPOWERPOINTCOMMENTS;
		}
		else {
			throw new IllegalStateException("Unexpected content type " + contentType);
		}

		return parseType;
	}

	private boolean isTranslatablePart(final ZipEntry entry) throws IOException, XMLStreamException {
		final String type = this.generalDocument.contentTypeFor(entry);
		if (!entry.getName().endsWith(".xml")) return false;
        if (isExcluded(entry.getName(), type)) return false;
		if (isHidden(entry.getName(), type)) return false;
		if (this.generalDocument.conditionalParameters().getTranslateDocProperties() && type.equals(CORE_PROPERTIES_TYPE)) return true;
		if (this.generalDocument.conditionalParameters().getTranslateComments() && type.equals(Powerpoint.COMMENTS_TYPE)) return true;
		if (isStyledTextPart(entry)) return true;
		return false;
	}

	private static boolean isModifiablePart(String contentType) {
		// @todo add parts which have been hidden or excluded
		return Powerpoint.MAIN_DOCUMENT_TYPE.equals(contentType)
			|| Powerpoint.THEME_TYPE.equals(contentType);
	}

	@Override
	public boolean isStyledTextPart(final ZipEntry entry) {
		final String type = this.generalDocument.contentTypeFor(entry);
		if (type.equals(Drawing.DIAGRAM_TYPE)) return true;
		if (type.equals(Drawing.CHART_TYPE)) return true;
		if (isSlidablePart(entry.getName(), type)) return true;
		return false;
	}

	private boolean isSlidablePart(String entryName, String type) {
		return null != slideFragmentsFor(entryName, type);
	}

	private SlideFragments slideFragmentsFor(final String entryName, final String type) {
		// @todo fully exclude hidden, see net.sf.okapi.filters.openxml.PowerpointDocument.isHidden
		if (this.generalDocument.conditionalParameters().getTranslatePowerpointMasters()) {
			if (Powerpoint.SLIDE_MASTER_TYPE.equals(type) && this.slideMasterFragmentsByName.containsKey(entryName)) {
				// translating slide masters which are in use by slide layouts
				return this.slideMasterFragmentsByName.get(entryName);
			}
			if (Powerpoint.SLIDE_LAYOUT_TYPE.equals(type) && this.slideLayoutFragmentsByName.containsKey(entryName)) {
				// translating slide layouts which are in use by slides
				return this.slideLayoutFragmentsByName.get(entryName);
			}
		}
		if (Powerpoint.SLIDE_TYPE.equals(type) && this.slideFragmentsByName.containsKey(entryName)) {
			return this.slideFragmentsByName.get(entryName);
		}
		if (this.generalDocument.conditionalParameters().getTranslatePowerpointNotes()) {
			if (this.generalDocument.conditionalParameters().getTranslatePowerpointMasters()) {
				if (Powerpoint.NOTES_MASTER_TYPE.equals(type) && this.notesMasterFragmentsByName.containsKey(entryName)) {
					// translating notes masters which are in use by notes slides
					return this.notesMasterFragmentsByName.get(entryName);
				}
			}
			if (Powerpoint.NOTES_SLIDE_TYPE.equals(type) && this.notesSlideFragmentsByName.containsKey(entryName)) {
				return this.notesSlideFragmentsByName.get(entryName);
			}
		}
		return null;
	}

	/**
	 * @param entryName ZIP entry name
	 * @param contentType the entry's content type
	 * @return {@code true} if the entry is to be excluded due to
	 * {@link ConditionalParameters#getPowerpointIncludedSlideNumbersOnly()} and
	 * {@link ConditionalParameters#tsPowerpointIncludedSlideNumbers}
	 */
	private boolean isExcluded(String entryName, String contentType) {
		return isExcludedSlide(entryName, contentType)
				|| isExcludedNote(entryName, contentType)
				|| isExcludedComment(entryName, contentType)
				|| isExcludedChart(entryName, contentType)
				|| isExcludedDiagramData(entryName, contentType);
	}

	/**
	 * @param entryName the entry name
	 * @param contentType the entry's content type
	 * @return {@code true} if the given entry represents a slide that was not included using
	 * option {@link ConditionalParameters#tsPowerpointIncludedSlideNumbers}
	 */
	private boolean isExcludedSlide(String entryName, String contentType) {
		if (!Powerpoint.SLIDE_TYPE.equals(contentType)) {
			return false;
		}

		if (!this.generalDocument.conditionalParameters().getPowerpointIncludedSlideNumbersOnly()) {
			return false;
		}

		int slideIndex = this.presentationFragments.slideNames().indexOf(entryName);
		if (slideIndex == -1) {
			return false;
		}

		int slideNumber = slideIndex + 1; // human readable / 1-based slide numbers
		return !this.generalDocument.conditionalParameters().tsPowerpointIncludedSlideNumbers.contains(slideNumber);
	}

	/**
	 * @param entryName the entry name
	 * @param contentType the entry's content type
	 * @return {@code true} if the given entry represents a note that is used on a slide that was
	 * not included using option {@link ConditionalParameters#tsPowerpointIncludedSlideNumbers}
	 */
	private boolean isExcludedNote(String entryName, String contentType) {
		if (!Powerpoint.NOTES_SLIDE_TYPE.equals(contentType)
				|| !slidesByNote.containsKey(entryName)) {
			return false;
		}

		String slideName = slidesByNote.get(entryName);
		return isExcludedSlide(slideName, Powerpoint.SLIDE_TYPE);
	}

	/**
	 * @param entryName the entry name
	 * @param contentType the entry's content type
	 * @return {@code true} if the given entry represents a comment that is used on a slide that was
	 * not included using option {@link ConditionalParameters#tsPowerpointIncludedSlideNumbers}
	 */
	private boolean isExcludedComment(String entryName, String contentType) {
		if (!Powerpoint.COMMENTS_TYPE.equals(contentType)
				|| !slidesByComment.containsKey(entryName)) {
			return false;
		}

		String slideName = slidesByComment.get(entryName);
		return isExcludedSlide(slideName, Powerpoint.SLIDE_TYPE);
	}

	/**
	 * @param entryName the entry name
	 * @param contentType the entry's content type
	 * @return {@code true} if the given entry represents a chart that is used on a slide that was
	 * not included using option {@link ConditionalParameters#tsPowerpointIncludedSlideNumbers}
	 */
	private boolean isExcludedChart(String entryName, String contentType) {
		if (!Drawing.CHART_TYPE.equals(contentType)
				|| !slidesByChart.containsKey(entryName)) {
			return false;
		}

		String slideName = slidesByChart.get(entryName);
		return isExcludedSlide(slideName, Powerpoint.SLIDE_TYPE);
	}

	/**
	 * "Diagram data" is used by SmartArt, for example.
	 *
	 * @param entryName the entry name
	 * @param contentType the entry's content type
	 * @return {@code true} if the given entry represents a diagram that is used on a slide that was
	 * not included using option {@link ConditionalParameters#tsPowerpointIncludedSlideNumbers}
	 */
	private boolean isExcludedDiagramData(String entryName, String contentType) {
		if (!Drawing.DIAGRAM_TYPE.equals(contentType)
				|| !slidesByDiagramData.containsKey(entryName)) {
			return false;
		}

		String slideName = slidesByDiagramData.get(entryName);
		return isExcludedSlide(slideName, Powerpoint.SLIDE_TYPE);
	}

	private boolean isHidden(String entryName, String type) throws IOException, XMLStreamException {
		if (!this.generalDocument.conditionalParameters().getTranslatePowerpointHidden() && Powerpoint.SLIDE_TYPE.equals(type)) {
			return isHiddenSlide(entryName);
		}
		return false;
	}

	private boolean isHiddenSlide(String entryName) throws IOException, XMLStreamException {
		XMLEventReader eventReader = null;
		try {
			eventReader = this.generalDocument.inputFactory().createXMLEventReader(this.generalDocument.getPartReader(entryName));
			return PresentationSlide.fromXMLEventReader(eventReader).isHidden();
		} finally {
			if (null != eventReader) {
				eventReader.close();
			}
		}
	}

	@Override
	public void close() throws IOException {
	}
}
