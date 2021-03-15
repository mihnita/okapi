package net.sf.okapi.filters.openxml;

import java.util.zip.ZipEntry;

import javax.xml.stream.events.XMLEvent;

class ExcelCommentPart extends StyledTextPart {
	ExcelCommentPart(Document.General generalDocument, ZipEntry entry,
					 StyleDefinitions styleDefinitions, StyleOptimisation styleOptimisation) {
		super(generalDocument, entry, styleDefinitions, styleOptimisation);
	}

	@Override
	protected boolean isStyledBlockStartEvent(XMLEvent e) {
		return XMLEventHelpers.isStartElement(e, "text");
	}
}
