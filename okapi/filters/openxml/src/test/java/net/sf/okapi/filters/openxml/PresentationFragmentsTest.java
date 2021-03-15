package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.FileLocation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(JUnit4.class)
public class PresentationFragmentsTest {
	private final XMLInputFactory inputFactory;
	private final XMLEventFactory eventFactory;
	private final FileLocation root;
	private PresentationFragments presentationFragments;

	public PresentationFragmentsTest() {
		this.inputFactory = XMLInputFactory.newInstance();
		this.eventFactory = XMLEventFactory.newInstance();
		this.root = FileLocation.fromClass(getClass());
	}

	@Before
	public void setUp() throws XMLStreamException, IOException {
		final Relationships relationships = new Relationships(inputFactory);
		relationships.parseFromXML("/ppt/_rels/presentation.xml.rels", readerFor("/presentation.xml.rels"));
		this.presentationFragments = new PresentationFragments.Default(
			new ConditionalParameters(),
			this.eventFactory,
			relationships
		);
		try (final Reader reader = readerFor("/presentation.xml")) {
			this.presentationFragments.readWith(this.inputFactory.createXMLEventReader(reader));
		}
	}

	private Reader readerFor(final String resource) {
		InputStream input = root.in(resource).asInputStream();
		return new InputStreamReader(input, StandardCharsets.UTF_8);
	}

	@Test
	public void slideMasterNamesDetermined() {
		final List<String> slideMasterNames = this.presentationFragments.slideMasterNames();
		assertEquals(1, slideMasterNames.size());
		assertEquals("/ppt/slideMasters/slideMaster1.xml", slideMasterNames.get(0));
	}

	@Test
	public void notesMasterNamesDetermined() {
		final List<String> notesMasterNames = this.presentationFragments.notesMasterNames();
		assertEquals(1, notesMasterNames.size());
		assertEquals("/ppt/notesMasters/notesMaster1.xml", notesMasterNames.get(0));
	}

	@Test
	public void slideNamesDetermined() {
		final List<String> slideNames = this.presentationFragments.slideNames();
		assertEquals(4, slideNames.size());
		assertEquals("/ppt/slides/slide1.xml", slideNames.get(0));
		assertEquals("/ppt/slides/slide2.xml", slideNames.get(1));
		assertEquals("/ppt/slides/slide3.xml", slideNames.get(2));
		assertEquals("/ppt/slides/slide4.xml", slideNames.get(3));
	}

	@Test
	public void defaultTextStyleDetermined() {
		final StyleDefinitions styleDefinitions = this.presentationFragments.defaultTextStyle();
		assertNotNull(styleDefinitions);
	}
}
