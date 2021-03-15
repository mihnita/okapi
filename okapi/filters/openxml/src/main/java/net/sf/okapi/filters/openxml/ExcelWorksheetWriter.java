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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;

class ExcelWorksheetWriter {
	private Document.General generalDocument;
	private ZipEntry entry;
	private SharedStringMap ssm;
	private ExcelStyles styles;
	private Map<String, Boolean> tableVisibility;
	private int sheetNumber;
	private boolean isSheetHidden;
	private String sheetName;

	ExcelWorksheetWriter(Document.General generalDocument, ZipEntry entry, SharedStringMap ssm, ExcelStyles styles,
						 Map<String, Boolean> tableVisibility, int sheetNumber,
						 boolean isSheetHidden, String sheetName) {
		this.generalDocument = generalDocument;
		this.entry = entry;
		this.ssm = ssm;
		this.styles = styles;
		this.tableVisibility = tableVisibility;
		this.sheetNumber = sheetNumber;
		this.isSheetHidden = isSheetHidden;
		this.sheetName = sheetName;
	}

	public String toString() {
		try {
			StringWriter sw = new StringWriter();
			Set<String> excludedColumns = this.generalDocument.conditionalParameters().findExcludedColumnsForSheetNumber(sheetNumber);
			XMLEventReader r = this.generalDocument.inputFactory().createXMLEventReader(
					this.generalDocument.getPartReader(this.entry.getName()));
			XMLEventWriter w = this.generalDocument.outputFactory().createXMLEventWriter(sw);
			Relationships worksheetRels = this.generalDocument.relationshipsFor(this.entry.getName());
			new ExcelWorksheet(this.generalDocument.eventFactory(), ssm, styles, worksheetRels,
					tableVisibility, isSheetHidden, excludedColumns, this.generalDocument.conditionalParameters().tsExcelExcludedColors,
					!this.generalDocument.conditionalParameters().getTranslateExcelHidden(), sheetName).parse(r, w);
			return sw.toString();
		}
		catch (IOException | XMLStreamException e) {
			throw new OkapiBadFilterInputException(e);
		}
	}
}
