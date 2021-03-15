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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiException;

/**
 * Class to parse an individual worksheet and update the shared string
 * data based on worksheet cells and exclusion information.
 */
class ExcelWorksheet {
	private static final String WORKSHEET = "worksheet";
	private static final String ROW = "row";
	private static final String COL = "col";
	private static final String CELL = "c";
	private static final String VALUE = "v";
	private static final String TABLE_PART = "tablePart";
	private static final String MERGE_CELL = "mergeCell";
	private static final QName CELL_LOCATION = new QName("r");
	private static final QName ROW_NUMBER = CELL_LOCATION;
	private static final QName CELL_TYPE = new QName("t");
	private static final QName CELL_STYLE = new QName("s");
	private static final QName HIDDEN = new QName("hidden");
	private static final QName MIN = new QName("min");
	private static final QName MAX = new QName("max");
	private static final QName REF = new QName("ref");
	private static final String ID = "id";

	private static final char COLUMN_INDEX_PART_MINIMUM = 'A';
	private static final char COLUMN_INDEX_PART_MAXIMUM = 'Z';

	private static final Pattern cellCoordinatePattern = Pattern.compile("[A-Z]{1,3}(\\d+)");

	private final XMLEventFactory eventFactory;
	private final SharedStringMap stringTable;
	private final ExcelStyles styles;
	private final Relationships worksheetRels;
	private final Map<String, Boolean> tableVisibilityMap;
	private final boolean isSheetHidden;
	private final Set<String> excludedColumns;
	private final Set<String> excludedColors;
	private final boolean excludeHiddenRowsAndColumns;
	private Set<Integer> excludedRows;
	private List<MergeArea> mergeAreas;
	private String cellValue;
	private String name;

	private QName row;
	private QName col;
	private QName cell;
	private QName value;
	private QName tablePart;
	private QName mergeCell;
	private QName id;

	ExcelWorksheet(XMLEventFactory eventFactory, SharedStringMap stringTable,
						  ExcelStyles styles, Relationships worksheetRels, Map<String, Boolean> tableVisibilityMap,
						  boolean isSheetHidden, Set<String> excludedColumns, Set<String> excludedColors,
						  boolean excludeHiddenRowsAndColumns) {
		this.eventFactory = eventFactory;
		this.stringTable = stringTable;
		this.styles = styles;
		this.worksheetRels = worksheetRels;
		this.tableVisibilityMap = tableVisibilityMap;
		this.isSheetHidden = isSheetHidden;
		this.excludedColumns = new HashSet<>(excludedColumns); // We may need to modify this locally
		this.excludedColors = excludedColors;
		this.excludeHiddenRowsAndColumns = excludeHiddenRowsAndColumns;
		this.excludedRows = new HashSet<>();
		this.mergeAreas = new ArrayList<>();
	}

	ExcelWorksheet(XMLEventFactory eventFactory, SharedStringMap stringTable,
						  ExcelStyles styles, Relationships worksheetRels, Map<String, Boolean> tableVisibilityMap,
						  boolean isSheetHidden, Set<String> excludedColumns, Set<String> excludedColors,
						  boolean excludeHiddenRowsAndColumns,
						  String name) {
		this(
			eventFactory,
			stringTable,
			styles,
			worksheetRels,
			tableVisibilityMap,
			isSheetHidden,
			excludedColumns,
			excludedColors,
			excludeHiddenRowsAndColumns
		);
		this.name = name;
	}

	void parse(XMLEventReader reader, XMLEventWriter writer) throws XMLStreamException {
		boolean excluded = false;
		boolean inValue = false;
		boolean isSharedString = false;
		XmlEventCollector collector = collectMergeHiddenData(reader);
		Iterator<XMLEvent> iterator = collector.getEvents().iterator();

		while (iterator.hasNext()) {
			XMLEvent e = iterator.next();
			if (e.isStartElement()) {
				StartElement el = e.asStartElement();
				if (el.getName().equals(this.cell)) {
					// We only care about cells with @t="s", indicating a shared string
					Attribute typeAttr = el.getAttributeByName(CELL_TYPE);
					if (typeAttr != null && typeAttr.getValue().equals("s")) {
						cellValue = el.getAttributeByName(CELL_LOCATION).getValue();
						excluded = isCellHidden(cellValue);
						isSharedString = true;
						Attribute styleAttr = el.getAttributeByName(CELL_STYLE);
						if (styleAttr != null) {
							int styleIndex = Integer.parseInt(styleAttr.getValue());
							ExcelStyles.CellStyle style = styles.getCellStyle(styleIndex);
							// I'm going to start with a naive implementation that should
							// basically be fine, but not ideal if we're excluding large numbers
							// of colors.
							for (String excludedColor : excludedColors) {
								if (style.fill.matchesColor(excludedColor)) {
									excluded = true;
									break;
								}
							}
						}
					}
				}
				else if (el.getName().equals(this.value)) {
					inValue = true;
				}
				else if (el.getName().equals(this.tablePart)) {
					String relId = XMLEventHelpers.getAttributeValue(el, this.id);
					Relationships.Rel tableRel = worksheetRels.getRelById(relId);
					tableVisibilityMap.put(tableRel.target, !isSheetHidden);
				}
			}
			else if (e.isEndElement()) {
				EndElement el = e.asEndElement();
				if (el.getName().equals(this.cell)) {
					excluded = false;
					isSharedString = false;
				}
				else if (el.getName().equals(this.value)) {
					inValue = false;
				}
			}
			else if (e.isCharacters() && inValue && isSharedString) {
				int origIndex = getSharedStringIndex(e.asCharacters().getData());
				int newIndex = stringTable.createEntryForString(origIndex, excluded, cellValue, name).getNewIndex();
				// Replace the event with one that contains the new index
				e = eventFactory.createCharacters(String.valueOf(newIndex));
			}
			writer.add(e);
		}
	}

	private void qualifyNames(final StartElement startElement) {
		this.row = new QName(
			startElement.getName().getNamespaceURI(),
			ROW,
			startElement.getName().getPrefix()
		);
		this.col = new QName(
			startElement.getName().getNamespaceURI(),
			COL,
			startElement.getName().getPrefix()
		);
		this.cell = new QName(
			startElement.getName().getNamespaceURI(),
			CELL,
			startElement.getName().getPrefix()
		);
		this.value = new QName(
			startElement.getName().getNamespaceURI(),
			VALUE,
			startElement.getName().getPrefix()
		);
		this.tablePart = new QName(
			startElement.getName().getNamespaceURI(),
			TABLE_PART,
			startElement.getName().getPrefix()
		);
		this.mergeCell = new QName(
			startElement.getName().getNamespaceURI(),
			MERGE_CELL,
			startElement.getName().getPrefix()
		);
		this.id = new QName(
			startElement.getNamespaceURI(Namespace.PREFIX_R),
			ID,
			Namespace.PREFIX_R
		);
	}


	private XmlEventCollector collectMergeHiddenData(XMLEventReader xmlEventReader) throws XMLStreamException {
		XmlEventCollector collector = new XmlEventCollector();
		while (xmlEventReader.hasNext()) {
			XMLEvent e = xmlEventReader.nextEvent();
			collector.addEvent(e);

			if (!e.isStartElement()) {
				continue;
			}
			StartElement el = e.asStartElement();
			if (el.getName().getLocalPart().equals(WORKSHEET)) {
				qualifyNames(el);
			} else if (el.getName().equals(this.mergeCell)) {
				mergeAreas.add(new MergeArea(e.asStartElement().getAttributeByName(REF).getValue()));
			} else if (el.getName().equals(this.row)) {
				if (isHidden(el)) {
					Integer numberOfHiddenRow = Integer.parseInt(el.getAttributeByName(ROW_NUMBER).getValue());
					excludedRows.add(numberOfHiddenRow);
				}
			} else if (el.getName().equals(this.col)) {
				if (isHidden(el)) {
					// Column info blocks span one or more columns, which are referred to
					// via 1-indexed min/max values.
					excludedColumns.addAll(extractColumnNames(el));
				}
			}
		}
		return collector;
	}

	private boolean isHidden(StartElement el) {
		return excludeHiddenRowsAndColumns &&
				(isSheetHidden || parseOptionalBooleanAttribute(el, HIDDEN, false));
	}

	/**
	 * Check for an attribute that conforms to the XML Schema boolean datatype.  If it is present
	 * (and the value conforms), return the value.  If it is not present, or the value is
	 * non-conforming, return the specified default value.
	 * @param el
	 * @param attrName
	 * @param defaultValue
	 * @return
	 */
	private boolean parseOptionalBooleanAttribute(StartElement el, QName attrName, boolean defaultValue) {
		Attribute a = el.getAttributeByName(attrName);
		if (a == null) return defaultValue;
		String v = a.getValue();
		if ("true".equals(v) || "1".equals(v)) return true;
		if ("false".equals(v) || "0".equals(v)) return false;
		return defaultValue;
	}

	/**
	 * Convert the min and max attributes of a &lt;col&gt; element into a list
	 * of column names.  For example, "min=2; max=2" => [ "B" ].
	 * @param el
	 * @return
	 */
	private List<String> extractColumnNames(StartElement el) {
		try {
			List<String> names = new ArrayList<>();
			int min = Integer.parseInt(el.getAttributeByName(MIN).getValue());
			int max = Integer.parseInt(el.getAttributeByName(MAX).getValue());
			for (int i = min; i <= max; i++) {
				names.add(indexToColumnName(i));
			}
			return names;
		}
		catch (NumberFormatException | NullPointerException e) {
			throw new OkapiBadFilterInputException("Invalid <col> element", e);
		}
	}

	static String indexToColumnName(int index) {
		StringBuilder sb = new StringBuilder();

	    while (index > 0) {
	        int modulo = (index - 1) % 26;
	        sb.insert(0, (char)(65 + modulo));
	        index = (index - modulo) / 26;
	    }

	    return sb.toString();
	}

	private static int getSharedStringIndex(String value) {
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			throw new IllegalStateException("Unexpected shared string index '" + value + "'");
		}
	}

	private static String getColumn(String location) {
		char[] buf = location.toCharArray();
		for (int i = 0; i < buf.length; i++) {
			if (Character.isDigit(buf[i])) {
				return location.substring(0, i);
			}
		}
		// I don't think this should never happen, so fail fast
		throw new IllegalStateException("Unexpected worksheet cell location '" + location + "'");
	}

	private String getRow(String location) {
		Matcher matcher = cellCoordinatePattern.matcher(location);
		matcher.find();
		return matcher.group(1);
	}

	private boolean isCellHidden(String location) {
		String currentColumn = getColumn(location);
		String currentRow = getRow(location);

		boolean excluded = excludedColumns.contains(currentColumn) || excludedRows.contains(Integer.parseInt(currentRow));

		if (!excluded) {
			return false;
		}

		MergeArea mergedArea = getMergedArea(currentRow, currentColumn);
		if (mergedArea == null) {
			return true;
		}

		Intersection intersection = getIntersectionWithHiddenArea(mergedArea);

		return Intersection.PARTIAL != intersection;
	}

	private Intersection getIntersectionWithHiddenArea(MergeArea mergedArea) {
		List<String> columnsRange = getColumnsRange(mergedArea.getLeftColumn(), mergedArea.getRightColumn());
		Iterator<String> columnIterator = columnsRange.iterator();

		int mergedColumnNumber = 0;
		int intersectedWithHiddenColumns = 0;

		while(columnIterator.hasNext()) {
			mergedColumnNumber++;
			if (excludedColumns.contains(columnIterator.next())) {
				intersectedWithHiddenColumns++;
			}
		}
		if (mergedColumnNumber == intersectedWithHiddenColumns) {
			return Intersection.FULL;
		}

		int mergedRowNumber = 0;
		int intersectedWithHiddenRows = 0;


		int topRow = Integer.parseInt(mergedArea.getTopRow());
		int bottomRow = Integer.parseInt(mergedArea.getBottomRow());
		for(int i = topRow; i <= bottomRow; i++) {
			mergedRowNumber++;
			if (excludedRows.contains(i)) {
				intersectedWithHiddenRows++;
			}
		}

		if (mergedRowNumber == intersectedWithHiddenRows) {
			return Intersection.FULL;
		}

		if (mergedColumnNumber > intersectedWithHiddenColumns
				&& mergedRowNumber > intersectedWithHiddenRows) {
			return Intersection.PARTIAL;
		}

		if (mergedColumnNumber != 0 && intersectedWithHiddenColumns == 0
				&& mergedRowNumber != 0 && intersectedWithHiddenRows == 0) {
			return Intersection.NONE;
		}

		throw new OkapiException("The merge area has a wrong configuration");
	}

	private List<String> getColumnsRange(String startColumnIndex, String endColumnIndex) {
		List<String> columns = new ArrayList<>();
		String columnIndex = startColumnIndex;

		columns.add(columnIndex);

		while (!columnIndex.equals(endColumnIndex)) {
			columnIndex = incrementColumnIndex(columnIndex);
			columns.add(columnIndex);
		}

		return columns;
	}

	private String incrementColumnIndex(String columnIndex) {
		return incrementColumnIndexPart(columnIndex.toCharArray(), columnIndex.length() - 1);
	}

	private String incrementColumnIndexPart(char[] columnIndexParts, int partPosition) {
		if (0 > partPosition) {
			return COLUMN_INDEX_PART_MINIMUM + new String(columnIndexParts);
		}

		char part = columnIndexParts[partPosition];

		if (COLUMN_INDEX_PART_MAXIMUM == part) {
			columnIndexParts[partPosition] = COLUMN_INDEX_PART_MINIMUM;

			return incrementColumnIndexPart(columnIndexParts, --partPosition);
		}

		columnIndexParts[partPosition] = ++part;

		return new String(columnIndexParts);
	}

	private MergeArea getMergedArea(String currentRow, String currentColumn) {
		for(MergeArea area: mergeAreas) {
			if (compareColumns(area.getLeftColumn(), currentColumn) <= 0
					&& compareColumns(currentColumn, area.getRightColumn()) <= 0
					&& Integer.parseInt(area.getTopRow()) <= Integer.parseInt(currentRow)
					&& Integer.parseInt(currentRow) <= Integer.parseInt(area.getBottomRow())) {
				return area;
			}
		}
		return null;
	}

	private int compareColumns(String column1, String column2) {
		if (column1.compareTo(column2) == 0) {
			return 0;
		} else if (column1.length() < column2.length()) {
			return -1;
		} else if (column1.length() == column2.length()) {
			return column1.compareTo(column2);
		} else if (column1.length() > column2.length()) {
			return 1;
		}
		throw new OkapiException("Matching columns have a wrong format");
	}

	private enum Intersection {
		FULL,
		PARTIAL,
		NONE
	};

	static class XmlEventCollector implements XMLEvents {
		private List<XMLEvent> xmlEvents;

		XmlEventCollector() {
			xmlEvents = new ArrayList<>();
		}

		@Override
		public List<XMLEvent> getEvents() {
			return xmlEvents;
		}

		void addEvent(XMLEvent event) {
			xmlEvents.add(event);
		}
	}

	private class MergeArea {
		private String leftColumn;
		private String rightColumn;
		private String topRow;
		private String bottomRow;

		MergeArea(String area) {
			String[] cornerCoordinates = area.split(":");

			if (cornerCoordinates.length != 2) {
				return;
			}

			topRow = getRow(cornerCoordinates[0]);
			leftColumn = getColumn(cornerCoordinates[0]);

			bottomRow = getRow(cornerCoordinates[1]);
			rightColumn = getColumn(cornerCoordinates[1]);
		}

		String getLeftColumn() {
			return leftColumn;
		}

		String getRightColumn() {
			return rightColumn;
		}

		String getTopRow() {
			return topRow;
		}

		String getBottomRow() {
			return bottomRow;
		}
	}
}
