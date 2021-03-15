package net.sf.okapi.filters.openxml;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.FileLocation;

import static org.junit.Assert.*;

/**
 * Misc tests related to XLSX processing.
 */
@RunWith(JUnit4.class)
public class TestExcelDocument {
	private XMLFactoriesForTest factories = new XMLFactoriesForTest();
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testGetWorksheets() throws Exception {
		// Use a file with multiple sheets that appear out-of-order in the zip
		Document.General doc = generalDocument("/ordering.xlsx", new ConditionalParameters());
		doc.open();
		List<String> worksheets = ((ExcelDocument) doc.categorisedDocument()).findWorksheets();
		List<String> expected = new ArrayList<>();
		expected.add("xl/worksheets/sheet1.xml");
		expected.add("xl/worksheets/sheet2.xml");
		expected.add("xl/worksheets/sheet3.xml");
		doc.close();
		assertEquals(expected, worksheets);
	}

	@Test
	public void testGetSharedStrings() throws Exception {
		final Document.General doc = generalDocument("/ordering.xlsx", new ConditionalParameters());
		try {
			doc.open();
			final List<String> sharedStings = ((ExcelDocument) doc.categorisedDocument()).findSharedStrings();
			assertEquals(1, sharedStings.size());
			assertEquals("xl/sharedStrings.xml", sharedStings.get(0));
		} finally {
			doc.close();
		}
	}

	@Test
	public void acceptsAbsentSharedStrings() throws Exception {
		final Document.General doc = generalDocument("/850.xlsx", new ConditionalParameters());
		try {
			doc.open();
			assertEquals(0, ((ExcelDocument) doc.categorisedDocument()).findSharedStrings().size());
		} finally {
			doc.close();
		}
	}

	private Document.General generalDocument(String resource, ConditionalParameters params) throws Exception {
		return new Document.General(
			params,
			factories.getInputFactory(),
			factories.getOutputFactory(),
			factories.getEventFactory(),
			"sd",
			root.in(resource).asUri(),
			LocaleId.ENGLISH,
			OpenXMLFilter.ENCODING.name(),
			null,
			null,
			null
		);
	}
}
