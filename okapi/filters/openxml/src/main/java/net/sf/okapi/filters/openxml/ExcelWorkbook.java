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
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Class to parse XLSX workbook files.
 */
class ExcelWorkbook {
	static class Sheet {
		String name;
		String id;
		String relId;
		boolean visible;

		String getName() { return name; }
	}

	private static final String WORKBOOK = "workbook";
	private static final String SHEET = "sheet";
	private static final QName SHEET_NAME = new QName("name");
	private static final QName SHEET_ID = new QName("sheetId");
	private static final QName SHEET_STATE = new QName("state");
	private static final String ID = "id";

	private QName sheet;
	private QName id;
	private List<Sheet> sheets = new ArrayList<>();

	List<Sheet> getSheets() {
		return sheets;
	}

	private void addSheet(String name, String id, String relId, boolean visible) {
		Sheet sheet = new Sheet();
		sheet.name = name;
		sheet.id = id;
		sheet.relId = relId;
		sheet.visible = visible;
		sheets.add(sheet);
	}
	
	ExcelWorkbook parseFrom(XMLEventReader reader, ConditionalParameters params) throws XMLStreamException {
		while (reader.hasNext()) {
			XMLEvent event = reader.nextEvent();
			if (event.isStartElement()) {
				StartElement e = event.asStartElement();
				if (e.getName().getLocalPart().equals(WORKBOOK)) {
					qualifyNames(e);
				}
				else if (e.getName().equals(this.sheet)) {
					if (null == this.id) {
						qualifyIdName(e);
					}
					String state = XMLEventHelpers.getAttributeValue(e, SHEET_STATE);
					boolean visible = (params.getTranslateExcelHidden() || !"hidden".equals(state));
					addSheet(e.getAttributeByName(SHEET_NAME).getValue(),
									  e.getAttributeByName(SHEET_ID).getValue(),
									  e.getAttributeByName(this.id).getValue(),
									  visible);
				}
			}
		}
		return this;
	}

	private void qualifyNames(final StartElement startElement) {
		this.sheet = new QName(
			startElement.getName().getNamespaceURI(),
			SHEET,
			startElement.getName().getPrefix()
		);
		qualifyIdName(startElement);
	}

	private void qualifyIdName(final StartElement startElement) {
		final String namespaceUri = startElement.getNamespaceURI(Namespace.PREFIX_R);
		if (null == namespaceUri) {
			return;
		}
		this.id = new QName(
			namespaceUri, ID, Namespace.PREFIX_R
		);
	}
}
