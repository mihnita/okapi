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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.ContentTypes.Types.Common.PACKAGE_RELATIONSHIPS;
import static net.sf.okapi.filters.openxml.ContentTypes.Types.Common.CORE_PROPERTIES_TYPE;
import static net.sf.okapi.filters.openxml.ContentTypes.Types.Drawing;
import static net.sf.okapi.filters.openxml.ContentTypes.Types.Word;
import static net.sf.okapi.filters.openxml.ParseType.MSWORDDOCPROPERTIES;

class WordDocument implements Document {
	private static final String TARGET_REL_FILE = "document.xml.rels";
	private static final String GLOSSARY_DOCUMENT = "/glossaryDocument";
	private static final String STYLES = "/styles";

	private final Document.General generalDocument;
	private Enumeration<? extends ZipEntry> entries;
	private String glossaryPartName;
	private StyleDefinitions mainStyleDefinitions;
	private StyleDefinitions glossaryStyleDefinitions;
	private LinkedHashMap<ZipEntry, Markup> postponedParts;

	WordDocument(final Document.General generalDocument) {
		this.generalDocument = generalDocument;
	}

	@Override
	public Event open() throws IOException, XMLStreamException {
		this.postponedParts = new LinkedHashMap<>();
		this.entries = entries();
		initialiseGlossaryPartName();
		initialiseStyleDefinitions();
		return this.generalDocument.startDocumentEvent();
	}

	private Enumeration<? extends ZipEntry> entries() {
		List<? extends ZipEntry> list = Collections.list(this.generalDocument.entries());
		List<String> additionalParts = new ArrayList<>();
		additionalParts.add("word/_rels/document.xml.rels");
		additionalParts.add("word/document.xml");
		list.sort(new ZipEntryComparator(additionalParts));
		return Collections.enumeration(list);
	}

	private void initialiseGlossaryPartName() throws IOException, XMLStreamException {
		final String glossaryDocumentSourceType =  this.generalDocument.documentRelationshipsNamespace()
				.uri().concat(GLOSSARY_DOCUMENT);
		this.glossaryPartName = this.generalDocument.relationshipTargetFor(glossaryDocumentSourceType);
	}

	private void initialiseStyleDefinitions() throws IOException, XMLStreamException {
		final String styles = this.generalDocument.documentRelationshipsNamespace().uri().concat(STYLES);

		String partPath = this.generalDocument.relationshipTargetFor(styles);
		this.mainStyleDefinitions = styleDefinitions(partPath);

		partPath = glossaryRelationshipTargetFor(styles);
		this.glossaryStyleDefinitions = styleDefinitions(partPath);
	}

	private String glossaryRelationshipTargetFor(final String relationshipType) throws IOException, XMLStreamException {
		if (null == this.glossaryPartName) {
			return null;
		}
		final Relationships relationships = this.generalDocument.relationshipsFor(this.glossaryPartName);
		final List<Relationships.Rel> rels = relationships.getRelByType(relationshipType);

		if (null == rels) {
			return null;
		}
		return rels.get(0).target;
	}

	private StyleDefinitions styleDefinitions(final String partPath) throws IOException, XMLStreamException {
		if (null == partPath) {
			return new StyleDefinitions.Empty();
		}
		try (final Reader reader = this.generalDocument.getPartReader(partPath)) {
			final StyleDefinitions styleDefinitions = new WordStyleDefinitions(
				this.generalDocument.conditionalParameters(),
				this.generalDocument.eventFactory()
			);
			styleDefinitions.readWith(
				new WordStyleDefinitionsReader(
					this.generalDocument.conditionalParameters(),
					this.generalDocument.eventFactory(),
					this.generalDocument.inputFactory().createXMLEventReader(reader)
				)
			);
			return styleDefinitions;
		}
	}

	@Override
	public boolean isStyledTextPart(final ZipEntry entry) {
		final String type = this.generalDocument.contentTypeFor(entry);
		return (
			type.equals(Word.MAIN_DOCUMENT_TYPE) ||
			type.equals(Word.MACRO_ENABLED_MAIN_DOCUMENT_TYPE) ||
			type.equals(Word.HEADER_TYPE) ||
			type.equals(Word.FOOTER_TYPE) ||
			type.equals(Word.ENDNOTES_TYPE) ||
			type.equals(Word.FOOTNOTES_TYPE) ||
			type.equals(Word.COMMENTS_TYPE) ||
			type.equals(Drawing.DIAGRAM_TYPE) ||
			type.equals(Drawing.CHART_TYPE) ||
			isGlossaryStyledTextPart(entry)
		);
	}

	private boolean isGlossaryStyledTextPart(final ZipEntry entry) {
		final String type = this.generalDocument.contentTypeFor(entry);
		return type.equals(Word.GLOSSARY_DOCUMENT_TYPE);
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
		return this.entries.hasMoreElements() || !this.postponedParts.isEmpty();
	}

	@Override
	public Part nextPart() throws IOException, XMLStreamException {
		if (!this.entries.hasMoreElements()) {
			return nextPostponedPart();
		}
		final ZipEntry entry = this.entries.nextElement();
		final String contentType = this.generalDocument.contentTypeFor(entry);
		if (PACKAGE_RELATIONSHIPS.equals(contentType)
				&& entry.getName().endsWith(TARGET_REL_FILE)
				&& this.generalDocument.conditionalParameters().getExtractExternalHyperlinks()) {
			return new RelationshipsPart(this.generalDocument, entry);
		}

		if (!isTranslatablePart(entry)) {
			if (isStylesPart(contentType)) {
				this.postponedParts.put(entry, new Markup.Empty());
				return nextPart();
			}
			return new NonModifiablePart(this.generalDocument, entry);
		}

		if (isStyledTextPart(entry)) {
			final StyleDefinitions styleDefinitions = styleDefinitionsFor(entry);
			final StyleOptimisation styleOptimisation = styleOptimisationsFor(entry, styleDefinitions);
			return new StyledTextPart(
				this.generalDocument,
				entry,
				styleDefinitions,
				styleOptimisation
			);
		}

		ContentFilter contentFilter = new ContentFilter(this.generalDocument.conditionalParameters(), entry.getName());
		ParseType parseType = ParseType.MSWORD;
		if (Word.SETTINGS_TYPE.equals(contentType)) {
			contentFilter.setBInSettingsFile(true);
		}
		else if (CORE_PROPERTIES_TYPE.equals(contentType)) {
			parseType = MSWORDDOCPROPERTIES;
		}
		contentFilter.setUpConfig(parseType);

		return new DefaultPart(this.generalDocument, entry, contentFilter);
	}

	private Part nextPostponedPart() throws IOException, XMLStreamException {
		final Iterator<Map.Entry<ZipEntry, Markup>> iterator = postponedParts.entrySet().iterator();
		final Map.Entry<ZipEntry, Markup> mapEntry = iterator.next();
		iterator.remove();

		final String contentType = this.generalDocument.contentTypeFor(mapEntry.getKey());
		if (isStylesPart(contentType)) {
			return new MarkupModifiablePart(
				this.generalDocument,
				mapEntry.getKey(),
				styleDefinitions(mapEntry.getKey()).toMarkup()
			);
		}
		return new MarkupModifiablePart(
			this.generalDocument,
			mapEntry.getKey(),
			mapEntry.getValue()
		);
	}

	private StyleDefinitions styleDefinitions(final ZipEntry entry) throws IOException, XMLStreamException {
		final String styles = this.generalDocument.documentRelationshipsNamespace().uri().concat(STYLES);
		final String partPath = glossaryRelationshipTargetFor(styles);

		if (entry.getName().equals(partPath)) {
			return this.glossaryStyleDefinitions;
		}
		return this.mainStyleDefinitions;
	}

	private StyleDefinitions styleDefinitionsFor(final ZipEntry entry) {
		if (isGlossaryStyledTextPart(entry)) {
			return this.glossaryStyleDefinitions;
		}
		return this.mainStyleDefinitions;
	}

	private StyleOptimisation styleOptimisationsFor(final ZipEntry entry, final StyleDefinitions styleDefinitions) throws IOException, XMLStreamException {
		final Namespace namespace = namespacesOf(entry).forPrefix(Namespace.PREFIX_W);
		if (null == namespace) {
			return new StyleOptimisation.Bypass();
		}
		return new StyleOptimisation.Default(
			new StyleOptimisation.Bypass(),
			this.generalDocument.conditionalParameters(),
			this.generalDocument.eventFactory(),
			new QName(namespace.uri(), ParagraphBlockProperties.PPR, namespace.prefix()),
			new QName(namespace.uri(), RunProperties.RPR, namespace.prefix()),
			Collections.singletonList(
				new QName(namespace.uri(), RunProperty.StyleRunProperty.NAME, namespace.prefix())
			),
			styleDefinitions
		);
	}

	private Namespaces2 namespacesOf(final ZipEntry entry) throws IOException, XMLStreamException {
		try (final Reader reader = new InputStreamReader(this.generalDocument.inputStreamFor(entry))) {
			final Namespaces2 namespaces = new Namespaces2.Default(
					this.generalDocument.inputFactory()
			);
			namespaces.readWith(reader);
			return namespaces;
		}
	}

	private boolean isTranslatablePart(final ZipEntry entry) {
		final String type = this.generalDocument.contentTypeFor(entry);
		if (!entry.getName().endsWith(".xml")) return false;
		if (type.equals(Word.MAIN_DOCUMENT_TYPE)) return true;
		if (type.equals(Word.MACRO_ENABLED_MAIN_DOCUMENT_TYPE)) return true;
		if (this.generalDocument.conditionalParameters().getTranslateDocProperties() && type.equals(CORE_PROPERTIES_TYPE)) return true;
		if (type.equals(Word.HEADER_TYPE) || type.equals(Word.FOOTER_TYPE)) {
			return this.generalDocument.conditionalParameters().getTranslateWordHeadersFooters();
		}
		if (type.equals(Word.COMMENTS_TYPE)) {
			return this.generalDocument.conditionalParameters().getTranslateComments();
		}
		if (type.equals(Word.SETTINGS_TYPE)) return true;
		if (type.equals(Drawing.CHART_TYPE)) return true;
		if (isStyledTextPart(entry)) return true;
		return false;
	}

	private boolean isStylesPart(final String type) {
		return Word.STYLES_TYPE.equals(type);
	}

	@Override
	public void close() throws IOException {
	}
}
