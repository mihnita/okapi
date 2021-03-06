package net.sf.okapi.filters.openxml;

import java.io.InputStreamReader;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.FileLocation;

import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

@RunWith(JUnit4.class)
public class TestExcelWorksheet {
	private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void test() throws Exception {
		SharedStringMap ssm = new SharedStringMap();
		ExcelWorksheet parser = new ExcelWorksheet(XMLEventFactory.newInstance(), ssm, new ExcelStyles(),
								new Relationships(inputFactory), new HashMap<>(), false,
				new HashSet<>(), new HashSet<>(), true);
		String output = parseWorksheet(parser, "/xlsx_parts/sheet1.xml");
		assertXMLEqual(new InputStreamReader(
						root.in("/xlsx_parts/gold/Rewritten_sheet1.xml").asInputStream(),
						StandardCharsets.UTF_8), new StringReader(output));
	}

	@Test
	public void testExcludeColors() throws Exception {
		SharedStringMap ssm = new SharedStringMap();
		ExcelStyles styles = new ExcelStyles();
		styles.parse(inputFactory.createXMLEventReader(
				root.in("/xlsx_parts/rgb_styles.xml").asInputStream(), "UTF-8"));
		Set<String> excludedColors = new HashSet<>();
		excludedColors.add("FF800000");
		excludedColors.add("FFFF0000");
		ExcelWorksheet parser = new ExcelWorksheet(XMLEventFactory.newInstance(), ssm, styles,
				new Relationships(inputFactory), new HashMap<>(), false,
				new HashSet<>(), excludedColors, true);
		parseWorksheet(parser, "/xlsx_parts/rgb_sheet1.xml");
		List<SharedStringMap.Entry> entries = ssm.getEntries();
		assertTrue(entries.get(0).getExcluded());  // excluded due to FF800000
		assertTrue(entries.get(1).getExcluded());  // excluded due to FFFF0000
		assertFalse(entries.get(2).getExcluded());
		assertFalse(entries.get(3).getExcluded());
		assertFalse(entries.get(4).getExcluded());
		assertFalse(entries.get(5).getExcluded());
		assertFalse(entries.get(6).getExcluded());
		assertFalse(entries.get(7).getExcluded());
		assertFalse(entries.get(8).getExcluded());
		assertFalse(entries.get(9).getExcluded());
	}

	@Test
	public void testExcludeHiddenCells() throws Exception {
		SharedStringMap ssm = new SharedStringMap();
		ExcelWorksheet parser = new ExcelWorksheet(XMLEventFactory.newInstance(), ssm, new ExcelStyles(),
				new Relationships(inputFactory), new HashMap<>(), false,
				new HashSet<>(), new HashSet<>(), true);
		parseWorksheet(parser, "/xlsx_parts/worksheet-hiddenCells.xml");
		List<SharedStringMap.Entry> entries = ssm.getEntries();
		assertFalse(entries.get(0).getExcluded());
		assertTrue(entries.get(1).getExcluded());
		assertTrue(entries.get(2).getExcluded());
		assertTrue(entries.get(3).getExcluded());
	}

	@Test
	public void testExposeHiddenCells() throws Exception {
		SharedStringMap ssm = new SharedStringMap();
		ExcelWorksheet parser = new ExcelWorksheet(XMLEventFactory.newInstance(), ssm, new ExcelStyles(),
				new Relationships(inputFactory), new HashMap<>(), false,
				new HashSet<>(), new HashSet<>(), false);
		parseWorksheet(parser, "/xlsx_parts/worksheet-hiddenCells.xml");
		List<SharedStringMap.Entry> entries = ssm.getEntries();
		assertFalse(entries.get(0).getExcluded());
		assertFalse(entries.get(1).getExcluded());
		assertFalse(entries.get(2).getExcluded());
		assertFalse(entries.get(3).getExcluded());
	}

	private String parseWorksheet(ExcelWorksheet parser, String resourceName) throws Exception {
		XMLEventReader reader = inputFactory.createXMLEventReader(
				root.in(resourceName).asInputStream(), "UTF-8");
		StringWriter sw = new StringWriter();
		XMLEventWriter writer = outputFactory.createXMLEventWriter(sw);
		parser.parse(reader, writer);
		reader.close();
		writer.close();
		return sw.toString();
	}

	@Test
	public void testIndexToColumnName() {
		assertEquals("A", ExcelWorksheet.indexToColumnName(1));
		assertEquals("Z", ExcelWorksheet.indexToColumnName(26));
		assertEquals("AA", ExcelWorksheet.indexToColumnName(27));
		assertEquals("AZ", ExcelWorksheet.indexToColumnName(52));
		assertEquals("BA", ExcelWorksheet.indexToColumnName(53));
	}
}
