package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.filters.openxml.Relationships.Rel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestRelationships {
	private XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
	private XMLEventFactory eventFactory = XMLEventFactory.newInstance();
	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testBasicRels() throws Exception {
		Reader reader = getReader("/presentation.xml.rels");
		Relationships rels = new Relationships(inputFactory);
		rels.parseFromXML("/ppt/_rels/presentation.xml.rels", reader);
		reader.close();
		checkRel(rels, "/ppt/presProps.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/presProps", "rId2");
		checkRel(rels, "/ppt/theme/theme2.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme", "rId1");
		checkRel(rels, "/ppt/slideMasters/slideMaster1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster", "rId4");
		checkRel(rels, "/ppt/tableStyles.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/tableStyles", "rId3");
		checkRel(rels, "/ppt/slides/slide4.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide", "rId9");
		checkRel(rels, "/ppt/slides/slide1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide", "rId6");
		checkRel(rels, "/ppt/notesMasters/notesMaster1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesMaster", "rId5");
		checkRel(rels, "/ppt/slides/slide3.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide", "rId8");
		checkRel(rels, "/ppt/slides/slide2.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide", "rId7");
	}

	@Test
	public void testNormalizedRels() throws Exception {
		Reader reader = getReader("/slide1.xml.rels");
		Relationships rels = new Relationships(inputFactory);
		rels.parseFromXML("/ppt/slides/_rels/slide1.xml.rels", reader);
		reader.close();
		checkRel(rels, "/ppt/notesSlides/notesSlide1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesSlide", "rId2");
		checkRel(rels, "/ppt/slideLayouts/slideLayout1.xml", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout", "rId1");
	}

	@Test
	public void testByType() throws Exception {
		Reader reader = getReader("/slide1.xml.rels");
		Relationships rels = new Relationships(inputFactory);
		rels.parseFromXML("/ppt/slides/_rels/slide1.xml.rels", reader);
		reader.close();
		checkRelByType(rels,
				"http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesSlide",
				Collections.singletonList(new Relationships.Rel("/ppt/notesSlides/notesSlide1.xml", null,
				"http://schemas.openxmlformats.org/officeDocument/2006/relationships/notesSlide", "rId2")));
		checkRelByType(rels,
				"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout",
				Collections.singletonList(new Relationships.Rel("/ppt/slideLayouts/slideLayout1.xml", null,
				"http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout", "rId1")));
	}

	@Test
	public void testMissingRels() throws Exception {
		Document.General document = new Document.General(
			new ConditionalParameters(),
			inputFactory,
			outputFactory,
			eventFactory,
			"sd",
			root.in("/sampleMore.xlsx").asUri(),
			LocaleId.ENGLISH,
			StandardCharsets.UTF_8.name(),
			null,
			null,
			null
		);
		document.open();
		Relationships worksheetRels = document.relationshipsFor("xl/worksheets/sheet4.xml");
		assertNotNull(worksheetRels);
	}

	private Reader getReader(String resource) throws IOException {
		InputStream input = root.in(resource).asInputStream();
		return new InputStreamReader(input, StandardCharsets.UTF_8);
	}

	private void checkRel(Relationships rels, String target, String type, String id) {
		Rel r = rels.getRelById(id);
		assertNotNull(r);
		assertEquals(target, r.target);
		assertEquals(type, r.type);
		assertEquals(id, r.id);
	}

	private void checkRelByType(Relationships rels, String type, List<Rel> expected) {
		List<Rel> r = rels.getRelByType(type);
		assertNotNull(r);
		assertEquals(expected, r);
	}
}
