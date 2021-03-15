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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import com.twelvemonkeys.io.ole2.CompoundDocument;
import com.twelvemonkeys.io.ole2.CorruptDocumentException;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiEncryptedDataException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Excel;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Powerpoint;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Word;

interface Document {
	Event open() throws IOException, XMLStreamException;
	boolean isStyledTextPart(final ZipEntry entry);
	boolean hasPostponedTranslatables();
	void updatePostponedTranslatables(final String key, final String value);
	boolean hasNextPart();
	Part nextPart() throws IOException, XMLStreamException;
	void close() throws IOException;

	class General implements Document {
		private static final String UNSUPPORTED_MAIN_DOCUMENT_PART = "Unsupported main document part";

		private static final String OFFICE_DOCUMENT = "/officeDocument";
		private static final String DOCUMENT = "/document";

		private final ConditionalParameters conditionalParameters;
		private final XMLInputFactory inputFactory;
		private final XMLOutputFactory outputFactory;
		private final XMLEventFactory eventFactory;
		private final String startDocumentId;
		private final URI uri;
		private final LocaleId sourceLocale;
		private final String encoding;
		private final EncoderManager encoderManager;
		private final IFilter subfilter;
		private final IFilterWriter filterWriter;

		private ZipFile zipFile;
		private int currentSubDocumentId;

		private ContentTypes contentTypes;
		private String mainPartName;
		private Namespace documentRelationshipsNamespace;
		private Document categorisedDocument;

		General(
			final ConditionalParameters conditionalParameters,
			final XMLInputFactory inputFactory,
			final XMLOutputFactory outputFactory,
			final XMLEventFactory eventFactory,
			final String startDocumentId,
			final URI uri,
			final LocaleId sourceLocale,
			final String encoding,
			final EncoderManager encoderManager,
			final IFilter subfilter,
			final IFilterWriter filterWriter
		) {
			this.conditionalParameters = conditionalParameters;
			this.inputFactory = inputFactory;
			this.outputFactory = outputFactory;
			this.eventFactory = eventFactory;
			this.startDocumentId = startDocumentId;
			this.uri = uri;
			this.sourceLocale = sourceLocale;
			this.encoding = encoding;
			this.encoderManager = encoderManager;
			this.subfilter = subfilter;
			this.filterWriter = filterWriter;
		}

		@Override
		public Event open() throws IOException, XMLStreamException {
			File fZip = new File(uri.getPath());

			if (isEncrypted(fZip)) {
				throw new OkapiEncryptedDataException();
			}

			this.zipFile = new ZipFile(new File(uri.getPath()), ZipFile.OPEN_READ);
			this.currentSubDocumentId = 0;

			initializeContentTypes();
			initializeMainPartNameAndDocumentRelationshipsNamespace();
			initializeCategorisedDocument();

			return this.categorisedDocument.open();
		}

		private boolean isEncrypted(File file) throws IOException {
			// If you pass the File parameter to the CompoundDocument constructor it
			// opens a file pointer that is not released properly and remains open for
			// the entire lifetime of the application. Using a regular InputStream
			// *also* leaks inside CompoundDocument (see https://github.com/haraldk/TwelveMonkeys/issues/438).
			// By using ImageInputStream we take control of the file pointer and release
			// it properly after the check.
			try (ImageInputStream is = new FileImageInputStream(file)) {
				is.setByteOrder(ByteOrder.LITTLE_ENDIAN);
				new CompoundDocument(is);
				return true;
			} catch (CorruptDocumentException e) {
				return false;
			}
		}

		private void initializeContentTypes() throws XMLStreamException, IOException {
			this.contentTypes = new ContentTypes(this.inputFactory);
			this.contentTypes.parseFromXML(getPartReader(ContentTypes.PART_NAME));
		}

		private void initializeMainPartNameAndDocumentRelationshipsNamespace() throws IOException, XMLStreamException {
			final Relationships relationships = getRelationships(Relationships.ROOT_RELS_PART_NAME);
			final String officeDocumentSourceType = Namespace.DOCUMENT_RELATIONSHIPS.concat(OFFICE_DOCUMENT);
			final String strictOfficeDocumentSourceType = Namespace.STRICT_DOCUMENT_RELATIONSHIPS.concat(OFFICE_DOCUMENT);
			final String visioDocumentSourceType = Namespace.VISIO_DOCUMENT_RELATIONSHIPS.concat(DOCUMENT);

			if (relationships.hasRelType(officeDocumentSourceType)) {
				this.mainPartName = relationships.getRelByType(officeDocumentSourceType).get(0).target;
				this.documentRelationshipsNamespace = new Namespace.Default(Namespace.DOCUMENT_RELATIONSHIPS);
			} else if (relationships.hasRelType(strictOfficeDocumentSourceType)) {
				this.mainPartName = relationships.getRelByType(strictOfficeDocumentSourceType).get(0).target;
				this.documentRelationshipsNamespace = new Namespace.Default(Namespace.STRICT_DOCUMENT_RELATIONSHIPS);
			} else if (relationships.hasRelType(visioDocumentSourceType)) {
				this.mainPartName = relationships.getRelByType(visioDocumentSourceType).get(0).target;
				this.documentRelationshipsNamespace = new Namespace.Default(Namespace.VISIO_DOCUMENT_RELATIONSHIPS);
			} else {
				throw new OkapiBadFilterInputException(UNSUPPORTED_MAIN_DOCUMENT_PART);
			}
		}

		/**
		 * Initialises a categorised document which can be one of the following types:
		 *   WordProcessingML or Word,
		 *   PresentationML or Powerpoint,
		 *   SpreadsheetML or Excel,
		 *   Visio
		 * @throws IOException        if any error is encountered while reading the stream
		 * @throws XMLStreamException if any error is encountered while parsing the XML
		 */
		private void initializeCategorisedDocument() throws XMLStreamException, IOException {
			switch (contentTypes.getContentType(mainPartName)) {
				case Word.MAIN_DOCUMENT_TYPE:
				case Word.MACRO_ENABLED_MAIN_DOCUMENT_TYPE:
				case Word.TEMPLATE_DOCUMENT_TYPE:
				case Word.MACRO_ENABLED_TEMPLATE_DOCUMENT_TYPE:
					this.categorisedDocument = new WordDocument(this);
					break;

				case Powerpoint.MAIN_DOCUMENT_TYPE:
				case Powerpoint.MACRO_ENABLED_MAIN_DOCUMENT_TYPE:
				case Powerpoint.SLIDE_SHOW_DOCUMENT_TYPE:
				case Powerpoint.MACRO_ENABLED_SLIDE_SHOW_DOCUMENT_TYPE:
				case Powerpoint.TEMPLATE_DOCUMENT_TYPE:
				case Powerpoint.MACRO_ENABLED_TEMPLATE_DOCUMENT_TYPE:
					this.categorisedDocument = new PowerpointDocument(this);
					break;

				case Excel.MAIN_DOCUMENT_TYPE:
				case Excel.MACRO_ENABLED_MAIN_DOCUMENT_TYPE:
				case Excel.TEMPLATE_DOCUMENT_TYPE:
				case Excel.MACRO_ENABLED_TEMPLATE_DOCUMENT_TYPE:
					this.categorisedDocument = new ExcelDocument(this, encoderManager, subfilter);
					break;

				case ContentTypes.Types.Visio.MAIN_DOCUMENT_TYPE:
				case ContentTypes.Types.Visio.MACRO_ENABLED_MAIN_DOCUMENT_TYPE:
					this.categorisedDocument = new VisioDocument(this);
					break;

				default:
					throw new OkapiBadFilterInputException(String.format("%s: %s", UNSUPPORTED_MAIN_DOCUMENT_PART, mainPartName));
			}
		}

		Event startDocumentEvent() {
			StartDocument startDoc = new StartDocument(this.startDocumentId);
			startDoc.setName(this.uri.getPath());
			startDoc.setLocale(this.sourceLocale);
			startDoc.setMimeType(OpenXMLFilter.MIME_TYPE);
			startDoc.setLineBreak(OpenXMLFilter.LINE_BREAK);
			startDoc.setEncoding(this.encoding, false);  // Office 2007 files don't have UTF8BOM
			startDoc.setFilterWriter(this.filterWriter);
			startDoc.setFilterId(OpenXMLFilter.FILTER_ID);
			startDoc.setFilterParameters(this.conditionalParameters);
			ZipSkeleton skel = new ZipSkeleton(this.zipFile, null);

			return new Event(EventType.START_DOCUMENT, startDoc, skel);
		}

		@Override
		public boolean isStyledTextPart(final ZipEntry entry) {
			return this.categorisedDocument.isStyledTextPart(entry);
		}

		@Override
		public boolean hasPostponedTranslatables() {
			return this.categorisedDocument.hasPostponedTranslatables();
		}

		@Override
		public void updatePostponedTranslatables(final String key, final String value) {
			this.categorisedDocument.updatePostponedTranslatables(key, value);
		}

		@Override
		public boolean hasNextPart() {
			return this.categorisedDocument.hasNextPart();
		}

		@Override
		public Part nextPart() throws IOException, XMLStreamException {
			return this.categorisedDocument.nextPart();
		}

		@Override
		public void close() throws IOException {
			if (null != this.categorisedDocument) {
				this.categorisedDocument.close();
			}
			if (null != this.categorisedDocument) {
				zipFile.close();
			}
		}

		ConditionalParameters conditionalParameters() {
			return this.conditionalParameters;
		}

		XMLInputFactory inputFactory() {
			return inputFactory;
		}

		XMLOutputFactory outputFactory() {
			return outputFactory;
		}

		XMLEventFactory eventFactory() {
			return eventFactory;
		}

		String documentId() {
			return this.startDocumentId;
		}

		String nextSubDocumentId() {
			return String.valueOf(++this.currentSubDocumentId);
		}

		LocaleId sourceLocale() {
			return sourceLocale;
		}

		String encoding() {
			return this.encoding;
		}

		ZipFile zipFile() {
			return zipFile;
		}

		InputStream inputStreamFor(final ZipEntry entry) throws IOException {
			return zipFile.getInputStream(entry);
		}

		Enumeration<? extends ZipEntry> entries() {
			return zipFile.entries();
		}

		String contentTypeFor(final ZipEntry entry) {
			return this.contentTypes.getContentType("/".concat(entry.getName()));
		}

		String mainPartName() {
			return mainPartName;
		}

		Namespace documentRelationshipsNamespace() {
			return this.documentRelationshipsNamespace;
		}

		Document categorisedDocument() {
			return this.categorisedDocument;
		}

		Namespaces2 namespacesOf(final ZipEntry entry) throws IOException, XMLStreamException {
			try (final Reader reader = new InputStreamReader(inputStreamFor(entry))) {
				final Namespaces2 namespaces = new Namespaces2.Default(
					this.inputFactory
				);
				namespaces.readWith(reader);
				return namespaces;
			}
		}

		/**
		 * Return a reader for the named document part. The encoding passed to
		 * the constructor will be used to decode the content.  Bad things will
		 * happen if you call this on a binary part.
		 *
		 * @param partName name of the part. Should not contain a leading '/'.
		 * @return Reader
		 * @throws IOException if any error is encountered while reading the from the zip file
		 */
		Reader getPartReader(String partName) throws IOException {
			ZipEntry entry = zipFile.getEntry(partName);
			if (entry == null) {
				throw new OkapiBadFilterInputException("File is missing " + partName);
			}
			return new InputStreamReader(zipFile.getInputStream(entry), encoding);
		}

		/**
		 * Parse the named document part as a relationships file and return the parsed
		 * relationships data.
		 *
		 * @param relsPartName name of the part. Should not contain a leading '/'.
		 * @return {@link Relationships} instance
		 * @throws IOException        if any error is encountered while reading the stream
		 * @throws XMLStreamException if any error is encountered while parsing the XML
		 */
		private Relationships getRelationships(String relsPartName) throws IOException, XMLStreamException {
			Relationships rels = new Relationships(inputFactory);
			if (isPartAvailable(relsPartName)) {
				rels.parseFromXML(relsPartName, getPartReader(relsPartName));
			}
			return rels;
		}

		private boolean isPartAvailable(String partName) {
			return zipFile.getEntry(partName) != null;
		}

		String relationshipTargetFor(String relationshipType) throws IOException, XMLStreamException {
			Relationships relationships = relationshipsFor(this.mainPartName);
			List<Relationships.Rel> rels = relationships.getRelByType(relationshipType);

			if (null == rels) {
				return null;
			}

			return rels.get(0).target;
		}

		/**
		 * Find the relationships file for the named part and then parse the relationships.
		 * If no relationships file exists for the specified part, an empty Relationships
		 * object is returned.
		 *
		 * @param partName
		 * @return
		 * @throws IOException
		 * @throws XMLStreamException
		 */
		Relationships relationshipsFor(String partName) throws IOException, XMLStreamException {
			int lastSlash = partName.lastIndexOf("/");
			if (lastSlash == -1) {
				return getRelationships("_rels/" + partName + ".rels");
			}
			String relPart = partName.substring(0, lastSlash) + "/_rels" + partName.substring(lastSlash) + ".rels";
			return getRelationships(relPart);
		}

		/**
		 * Initializes the relationships of the given type.
		 */
		Map<String, String> relsByEntry(List<String> entryNames, String relType)
				throws IOException, XMLStreamException {
			Map<String, String> result = new HashMap<>();
			for (String entryName : entryNames) {
				List<Relationships.Rel> rels =
						relationshipsFor(entryName).getRelByType(relType);
				if (rels != null && !rels.isEmpty()) {
					for (Relationships.Rel rel : rels) {
						result.put(rel.target, entryName);
					}
				}
			}
			return result;
		}
	}
}
