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
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Common;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Drawing;
import net.sf.okapi.filters.openxml.ContentTypes.Types.Excel;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import static net.sf.okapi.filters.openxml.ParseType.MSEXCEL;
import static net.sf.okapi.filters.openxml.ParseType.MSWORDDOCPROPERTIES;

class ExcelDocument implements Document {
	private static final String COMMENTS = "/comments";
	private static final String DRAWINGS = "/drawing";
	private static final String CHART = "/chart";
	private static final String DIAGRAM_DATA = "/diagramData";
	private static final String SHARED_STRINGS = "/sharedStrings";
	private static final String STYLES = "/styles";

	private final Document.General generalDocument;
	private Enumeration<? extends ZipEntry> entries;
	private final EncoderManager encoderManager;
	private final IFilter subfilter;
	private final Map<String, String> sharedStrings;
	private LinkedHashMap<ZipEntry, String> postponedParts;

	private SharedStringMap sharedStringMap = new SharedStringMap();

	private List<String> worksheetEntryNames = null;

	private ExcelStyles styles;

	private Relationships workbookRels;

	private Map<String, ExcelWorkbook.Sheet> worksheets = new HashMap<>();

	private Map<String, Boolean> tableVisibility = new HashMap<>();

	private Map<String, String> sheetsByComment = new HashMap<>();

	private Map<String, String> sheetsByDrawing = new HashMap<>();

	private Map<String, String> drawingsByChart = new HashMap<>();

	private Map<String, String> drawingsByDiagramData = new HashMap<>();

	ExcelDocument(final General generalDocument, EncoderManager encoderManager, IFilter subfilter) {
		this.generalDocument = generalDocument;
		this.encoderManager = encoderManager;
		this.subfilter = subfilter;
		this.sharedStrings = new HashMap<>();
		this.postponedParts = new LinkedHashMap<>();
	}

	private boolean isModifiablePart(String contentType) {
		return Excel.STYLES_TYPE.equals(contentType)
				|| Excel.WORKSHEET_TYPE.equals(contentType);
	}

	@Override
	public Event open() throws IOException, XMLStreamException {
		workbookRels = this.generalDocument.relationshipsFor(this.generalDocument.mainPartName());
		worksheetEntryNames = findWorksheets();
		entries = entries();
		styles = parseStyles();

		sheetsByComment = findComments(worksheetEntryNames);
		sheetsByDrawing = findDrawings(worksheetEntryNames);
		drawingsByChart = findCharts(sheetsByDrawing.keySet());
		drawingsByDiagramData = findDiagramData(sheetsByDrawing.keySet());

		return this.generalDocument.startDocumentEvent();
	}

	/**
	 * Do additional reordering of the entries for XLSX files to make
	 * sure that worksheets are parsed in order, followed by the shared
	 * strings table.
	 * @return the sorted enum of ZipEntry
	 * @throws IOException if any error is encountered while reading the stream
	 * @throws XMLStreamException if any error is encountered while parsing the XML
	 */
	private Enumeration<? extends ZipEntry> entries() throws IOException, XMLStreamException {
		Enumeration<? extends ZipEntry> entries = this.generalDocument.entries();
		List<? extends ZipEntry> entryList = Collections.list(entries);
		List<String> worksheetsAndSharedStrings = new ArrayList<>(worksheetEntryNames);
		worksheetsAndSharedStrings.addAll(findSharedStrings());
		entryList.sort(new ZipEntryComparator(worksheetsAndSharedStrings));
		return Collections.enumeration(entryList);
	}

	@Override
	public boolean isStyledTextPart(final ZipEntry entry) {
		final String type = this.generalDocument.contentTypeFor(entry);
		switch (type) {
			case Excel.SHARED_STRINGS_TYPE:
			case Drawing.CHART_TYPE:
			case Drawing.DIAGRAM_TYPE:
			case Excel.DRAWINGS_TYPE:
			case Excel.COMMENT_TYPE:
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean hasPostponedTranslatables() {
		return true;
	}

	@Override
	public void updatePostponedTranslatables(String key, String value) {
		this.sharedStrings.put(key, value);
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
		if (isPartHidden(entry.getName(), contentType)) {
			return new NonModifiablePart(this.generalDocument, entry);
		}

		// find a part based on content type
		if (!isTranslatablePart(entry.getName(), contentType)) {
			if (contentType.equals(Excel.WORKSHEET_TYPE)) {
				if (isSheetHidden(entry.getName())) {
					return new ModifiablePart(
						generalDocument,
						entry,
						new ByteArrayInputStream(
							new ExcelWorksheetWriter(
								this.generalDocument,
								entry,
								sharedStringMap,
								styles,
								tableVisibility,
								findWorksheetNumber(entry.getName()),
								true,
								worksheets.get(entry.getName()).getName()
							)
							.toString().getBytes(OpenXMLFilter.ENCODING)
						)
					);
				} else {
					this.postponedParts.put(
						entry,
						new ExcelWorksheetWriter(
							this.generalDocument,
							entry,
							sharedStringMap,
							styles,
							tableVisibility,
							findWorksheetNumber(entry.getName()),
							false,
							worksheets.get(entry.getName()).getName()
						).toString()
					);
					return nextPart();
				}
			} else if (isModifiablePart(contentType)) {
				return new ModifiablePart(this.generalDocument, entry, this.generalDocument.inputStreamFor(entry));
			}
			return new NonModifiablePart(this.generalDocument, entry);
		}

		final StyleDefinitions styleDefinitions = new StyleDefinitions.Empty();
		final StyleOptimisation styleOptimisation = new StyleOptimisation.Bypass();

		switch (contentType) {
			case Excel.SHARED_STRINGS_TYPE:
				return new SharedStringsPart(this.generalDocument, entry, styleDefinitions,
						styleOptimisation, encoderManager, subfilter, sharedStringMap);
			case Excel.COMMENT_TYPE:
				return new ExcelCommentPart(this.generalDocument, entry, styleDefinitions, styleOptimisation);
			case Excel.DRAWINGS_TYPE:
			case Drawing.CHART_TYPE:
			case Drawing.DIAGRAM_TYPE:
				return new StyledTextPart(
					this.generalDocument,
					entry,
					styleDefinitions,
					styleOptimisation
				);
			default:
				break;
		}

		// find content handler based on parseType
		ParseType parseType = null;
		switch (contentType) {
			case Common.CORE_PROPERTIES_TYPE:
				parseType = MSWORDDOCPROPERTIES;
				break;
			case Excel.MAIN_DOCUMENT_TYPE:
				parseType = MSEXCEL;
				break;
		}

		if (MSWORDDOCPROPERTIES.equals(parseType) || MSEXCEL.equals(parseType)) {
			ContentFilter contentFilter = new ContentFilter(this.generalDocument.conditionalParameters(), entry.getName());
			contentFilter.setUpConfig(parseType);
			return new DefaultPart(this.generalDocument, entry, contentFilter);
		}

		return new ExcelFormulaPart(this.generalDocument, entry, sharedStrings, this.generalDocument.inputStreamFor(entry));
	}

	private boolean isSheetHidden(String entryName) {
		ExcelWorkbook.Sheet sheet = worksheets.get(entryName);

		return sheet != null && !sheet.visible;
	}

	private Part nextPostponedPart() throws IOException, XMLStreamException {
		final Iterator<Map.Entry<ZipEntry, String>> iterator = postponedParts.entrySet().iterator();
		final Map.Entry<ZipEntry, String> mapEntry = iterator.next();
		iterator.remove();
		return new ModifiablePart(
			this.generalDocument,
			mapEntry.getKey(),
			new ByteArrayInputStream(
				new ExcelFormulaPart(
					this.generalDocument,
					mapEntry.getKey(),
					sharedStrings,
					new ByteArrayInputStream(
						mapEntry.getValue().getBytes(OpenXMLFilter.ENCODING)
					)
				)
				.getModifiedContent().getBytes(OpenXMLFilter.ENCODING)
			)
		);
	}

	private boolean isTranslatablePart(String entryName, String contentType) {
		if (Excel.TABLE_TYPE.equals(contentType)) {
			Boolean b = tableVisibility.get(entryName);
			// There should always be a value, but default to hiding tables we don't know about
			return (b != null) ? b : false;
		}
		if (!entryName.endsWith(".xml")) {
			return false;
		}
		switch (contentType) {
			case Excel.SHARED_STRINGS_TYPE:
			case Drawing.CHART_TYPE:
				return true;
			case Excel.MAIN_DOCUMENT_TYPE:
			case Excel.MACRO_ENABLED_MAIN_DOCUMENT_TYPE:
				return this.generalDocument.conditionalParameters().getTranslateExcelSheetNames();
			case Common.CORE_PROPERTIES_TYPE:
				return this.generalDocument.conditionalParameters().getTranslateDocProperties();
			case Excel.COMMENT_TYPE:
				return this.generalDocument.conditionalParameters().getTranslateComments();
			case Excel.DRAWINGS_TYPE:
				return this.generalDocument.conditionalParameters().getTranslateExcelDrawings();
			case Drawing.DIAGRAM_TYPE:
				return this.generalDocument.conditionalParameters().getTranslateExcelDiagramData();
			default:
				return false;
		}
	}

	private ExcelWorkbook parseWorkbook(String partName) throws IOException, XMLStreamException {
		XMLEventReader r = this.generalDocument.inputFactory().createXMLEventReader(this.generalDocument.getPartReader(partName));
		return new ExcelWorkbook().parseFrom(r, this.generalDocument.conditionalParameters());
	}

	private ExcelStyles parseStyles() throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		Relationships.Rel stylesRel = workbookRels.getRelByType(namespaceUri.concat(STYLES)).get(0);
		ExcelStyles styles = new ExcelStyles();
		styles.parse(this.generalDocument.inputFactory().createXMLEventReader(
					 this.generalDocument.getPartReader(stylesRel.target)));
		return styles;
	}

	/**
	 * Examine relationship information to find all worksheets in the package.
	 * Return a list of their entry names, in order.
	 * @return list of entry names.
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	List<String> findWorksheets() throws IOException, XMLStreamException {
		List<String> worksheetNames = new ArrayList<>();
		ExcelWorkbook workbook = parseWorkbook(this.generalDocument.mainPartName());

		List<ExcelWorkbook.Sheet> sheets = workbook.getSheets();
		for (ExcelWorkbook.Sheet sheet : sheets) {
			Relationships.Rel sheetRel = workbookRels.getRelById(sheet.relId);
			worksheetNames.add(sheetRel.target);
			worksheets.put(sheetRel.target, sheet);
		}
		return worksheetNames;
	}

	/**
	 * Parse relationship information to find the shared strings table.
	 * @return an empty list if there is no the shared strings relationship
	 *         or a singleton list of found shared strings entry name otherwise.
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	List<String> findSharedStrings() throws IOException, XMLStreamException {
		Relationships rels = this.generalDocument.relationshipsFor(this.generalDocument.mainPartName());
		final String sharedStringsNamespaceUri = this.generalDocument.documentRelationshipsNamespace()
				.uri().concat(SHARED_STRINGS);
		List<Relationships.Rel> r = rels.getRelByType(sharedStringsNamespaceUri);
		if (r == null) {
			return Collections.emptyList();
		}
		if (r.size() != 1) {
			throw new OkapiBadFilterInputException(
				String.format("%s: %s", Relationships.UNEXPECTED_NUMBER_OF_RELATIONSHIPS, sharedStringsNamespaceUri)
			);
		}
		return Collections.singletonList(r.get(0).target);
	}

	private int findWorksheetNumber(String worksheetEntryName) {
		for (int i = 0; i < worksheetEntryNames.size(); i++) {
			if (worksheetEntryName.equals(worksheetEntryNames.get(i))) {
				return i + 1; // 1-indexed
			}
		}
		throw new IllegalStateException("No worksheet entry with name " +
						worksheetEntryName + " in " + worksheetEntryNames);
	}

	private boolean isPartHidden(String entryName, String contentType) {
	    switch (contentType) {
	        case Excel.COMMENT_TYPE:
                return isCommentHidden(entryName);
	        case Excel.DRAWINGS_TYPE:
                return isDrawingHidden(entryName);
	        case Drawing.CHART_TYPE:
                return isChartHidden(entryName);
	        case Drawing.DIAGRAM_TYPE:
                return isDiagramDataHidden(entryName);
            default:
                return false;
        }
	}

	private boolean isCommentHidden(String entryName) {
		if (!sheetsByComment.containsKey(entryName)) {
			return false;
		}

		String sheetEntryName = sheetsByComment.get(entryName);
		return isSheetHidden(sheetEntryName);
	}

	private boolean isDrawingHidden(String entryName) {
		if (!sheetsByDrawing.containsKey(entryName)) {
			return false;
		}

		String sheetEntryName = sheetsByDrawing.get(entryName);
		return isSheetHidden(sheetEntryName);
	}

	private boolean isChartHidden(String entryName) {
		if (!drawingsByChart.containsKey(entryName)) {
			return false;
		}

		String drawingEntryName = drawingsByChart.get(entryName);
		return isDrawingHidden(drawingEntryName);
	}

	private boolean isDiagramDataHidden(String entryName) {
		if (!drawingsByDiagramData.containsKey(entryName)) {
			return false;
		}

		String drawingEntryName = drawingsByDiagramData.get(entryName);
		return isDrawingHidden(drawingEntryName);
	}

	private Map<String, String> findComments(List<String> sheetEntryNames)
			throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(sheetEntryNames, namespaceUri.concat(COMMENTS));
	}

	private Map<String, String> findDrawings(List<String> sheetEntryNames)
			throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(sheetEntryNames, namespaceUri.concat(DRAWINGS));
	}

	private Map<String, String> findCharts(Set<String> drawingEntryNames)
			throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(new ArrayList<>(drawingEntryNames), namespaceUri.concat(CHART));
	}

	private Map<String, String> findDiagramData(Set<String> drawingEntryNames)
			throws IOException, XMLStreamException {
		final String namespaceUri = this.generalDocument.documentRelationshipsNamespace().uri();
		return this.generalDocument.relsByEntry(new ArrayList<>(drawingEntryNames), namespaceUri.concat(DIAGRAM_DATA));
	}

	@Override
	public void close() throws IOException {
	}
}
