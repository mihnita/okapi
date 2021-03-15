package net.sf.okapi.lib.xliff2.validation;

import java.io.StringWriter;
import java.util.List;

import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.test.U;
import net.sf.okapi.lib.xliff2.validation.Rule.Normalization;
import net.sf.okapi.lib.xliff2.validation.Rule.Type;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ValidationTest {
	
	@Test
	public void testSimple () {
		Validation val = new Validation();
		Rule rule = val.add(new Rule("isPresent", "abc"));
		assertSame(rule, val.get(0));
		assertEquals(1, val.size());
		val.remove(rule);
		assertTrue(val.isEmpty());
	}

	@Test
	public void testWriter () {
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			StringWriter sr = new StringWriter();
			writer.setLineBreak("\n");
			writer.create(sr, "en");

			Unit unit = new Unit("u1");
			unit.appendSegment().getSource().append("source");
			Validation val = unit.getValidation();
			val.add(new Rule(Type.ISPRESENT.toString(), "abc"));
			Rule r = val.add(new Rule(Type.ISPRESENT.toString(), "XYZ"));
			r.setCaseSensitive(false);
			r.setNormalization(Normalization.NONE);
			r.setOccurs(2);
			r.setEnabled(false);
			r.setExistsInSource(true);
			writer.writeUnit(unit);
			writer.close();
			
			// Done: compare with expected output
			String expected = "<?xml version=\"1.0\"?>\n"
				+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
				+ " version=\"2.0\" srcLang=\"en\">\n"
				+ "<file id=\"f1\">\n"
				+ "<unit id=\"u1\">\n"
				+ "<val:validation xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
				+ "<val:rule isPresent=\"abc\"/>\n"
				+ "<val:rule isPresent=\"XYZ\" disabled=\"yes\" caseSensitive=\"no\" normalization=\"none\" occurs=\"2\" existsInSource=\"yes\"/>\n"
				+ "</val:validation>\n"
				+ "<segment>\n<source>source</source>\n</segment>\n"
				+ "</unit>\n"
				+ "</file>\n</xliff>\n";
			assertEquals(expected, sr.toString());
			XLIFFReader.validate(sr.toString(), null);
		}
	}
	
	@Test
	public void testReadWrite () {
		String snippet = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule isPresent=\"ABC\"/>\n"
			+ "<val:rule isPresent=\"XYZ\" disabled=\"yes\" caseSensitive=\"no\" normalization=\"none\" occurs=\"2\" existsInSource=\"yes\"/>\n"
			+ "</val:validation>\n"
			+ "<segment>\n<source>source</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(snippet);
		Unit unit = U.getUnit(events);
		assertTrue(unit.hasValidation());
		Validation val = unit.getValidation();
		Rule r = val.get(0);
		assertEquals("ABC", r.getData());
		assertEquals("ABC", r.getEffectiveData());
		String output = U.writeEvents(events);
		XLIFFReader.validate(output, null);
	}

	@Test
	public void testInheritance1 () {
		String snippet = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule isPresent=\"rule1\"/>\n"
			+ "<val:rule isPresent=\"rule2\"/>\n"
			+ "</val:validation>\n"
			+ "<group id=\"g1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule isPresent=\"rule3\"/>\n"
			+ "<val:rule isPresent=\"rule2\" caseSensitive=\"no\"/>\n"
			+ "<val:rule isPresent=\"rule1\" occurs=\"10\"/>\n"
			+ "</val:validation>\n"
			+ "<segment>\n<source>source</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(snippet);
		for ( Event event : events ) {
			switch ( event.getType() ) {
			case MID_FILE:
				Validation val1 = event.getMidFileData().getValidation();
				assertEquals(2, val1.size());
				assertEquals("rule1", val1.get(0).getData());
				assertTrue(val1.get(0).isCaseSensitive());
				assertFalse(val1.get(0).isInherited());
				assertEquals("rule2", val1.get(1).getData());
				assertFalse(val1.get(1).isInherited());
				assertEquals(0, val1.get(1).getOccurs());
				break;
			case START_GROUP:
				Validation val2 = event.getStartGroupData().getValidation();
				assertEquals(2, val2.size());
				// Inherited rule1
				assertEquals("rule1", val2.get(0).getData());
				assertTrue(val2.get(0).isCaseSensitive());
				assertTrue(val2.get(0).isInherited());
				// Inherited rule2
				assertEquals("rule2", val2.get(1).getData());
				assertTrue(val2.get(1).isInherited());
				assertEquals(0, val2.get(1).getOccurs());
				break;
			case TEXT_UNIT:
				Validation val3 = event.getUnit().getValidation();
				assertEquals(3, val3.size());
				// Overridden rule1
				assertEquals("rule1", val3.get(0).getData());
				assertEquals(10, val3.get(0).getOccurs());
				assertFalse(val3.get(0).isInherited());
				// Overridden rule2
				assertEquals("rule2", val3.get(1).getData());
				assertFalse(val3.get(1).isCaseSensitive());
				assertFalse(val3.get(1).isInherited());
				// New ruel3
				assertEquals("rule3", val3.get(2).getData());
				break;
			default: // Do nothing
				break;
			}
		}
		// Re-write
		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule isPresent=\"rule1\"/>\n"
			+ "<val:rule isPresent=\"rule2\"/>\n"
			+ "</val:validation>\n"
			+ "<group id=\"g1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<val:validation>\n" // Note the order change here (to the in-memory order after applying the overrides)
			+ "<val:rule isPresent=\"rule1\" occurs=\"10\"/>\n"
			+ "<val:rule isPresent=\"rule2\" caseSensitive=\"no\"/>\n"
			+ "<val:rule isPresent=\"rule3\"/>\n"
			+ "</val:validation>\n"
			+ "<segment>\n<source>source</source>\n</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeEvents(events));
	}

	@Test
	public void testInheritance2 () {
		String snippet = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" trgLang=\"fr\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule startsWith=\"*T\"/>\n"
			+ "</val:validation>\n"
			+ "<group id=\"g1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<segment>\n"
			+ "<source>*source</source>\n"
			+ "<target>*target</target>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(snippet);
		for ( Event event : events ) {
			switch ( event.getType() ) {
			case MID_FILE:
				Validation val1 = event.getMidFileData().getValidation();
				assertEquals(1, val1.size());
				assertEquals("*T", val1.get(0).getData());
				assertTrue(val1.get(0).isCaseSensitive());
				assertFalse(val1.get(0).isInherited());
				break;
			case START_GROUP:
				Validation val2 = event.getStartGroupData().getValidation();
				assertEquals(1, val2.size());
				assertEquals("*T", val2.get(0).getData());
				assertTrue(val2.get(0).isCaseSensitive());
				assertTrue(val2.get(0).isInherited());
				break;
			case TEXT_UNIT:
				Unit unit = event.getUnit();
				Validation val3 = unit.getValidation();
				assertEquals(1, val3.size());
				assertEquals("*T", val3.get(0).getData());
				assertTrue(val3.get(0).isCaseSensitive());
				assertTrue(val3.get(0).isInherited());
				List<Issue> list = val3.processRules(unit, "f1");
				assertEquals(1, list.size());
				break;
			default: // Do nothing
				break;
			}
		}
		// Re-write
		String expected = "<?xml version=\"1.0\"?>\n"
			+ "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:2.0\""
			+ " version=\"2.0\" srcLang=\"en\" trgLang=\"fr\" xmlns:val=\"urn:oasis:names:tc:xliff:validation:2.0\">\n"
			+ "<file id=\"f1\">\n"
			+ "<val:validation>\n"
			+ "<val:rule startsWith=\"*T\"/>\n"
			+ "</val:validation>\n"
			+ "<group id=\"g1\">\n"
			+ "<unit id=\"u1\">\n"
			+ "<segment>\n"
			+ "<source>*source</source>\n"
			+ "<target>*target</target>\n"
			+ "</segment>\n"
			+ "</unit>\n"
			+ "</group>\n"
			+ "</file>\n</xliff>\n";
		assertEquals(expected, U.writeEvents(events));
	}

}
