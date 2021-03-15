package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.sf.okapi.lib.xliff2.test.U;
import net.sf.okapi.lib.xliff2.reader.Event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FileDataTest {

	@Test
	public void testDefaults () {
		StartFileData sfd = new StartFileData("id1");
		assertEquals("id1", sfd.getId());
		assertTrue(sfd.getCanResegment());
		assertTrue(sfd.getTranslate());
		assertEquals(Directionality.AUTO, sfd.getSourceDir());
		assertEquals(Directionality.AUTO, sfd.getTargetDir());
		assertNull(sfd.getOriginal());
		assertTrue(sfd.getExtAttributes().isEmpty());
		
		MidFileData mfd = new MidFileData();
		assertTrue(mfd.getExtElements().isEmpty());
		assertEquals(0, mfd.getNoteCount());
		assertTrue(mfd.getExtElements().isEmpty());
	}
	
	@Test
	public void testMidFileAlwaysGenerated () {
		String text = U.STARTDOCWITHITS
			+ "<file id=\"f1\">\n"
			+ "<unit id=\"id\">\n"
			+ "<segment>\n<source>source</source>\n"
			+ "<target>source</target>\n</segment>\n"
			+ "</unit>\n"
			+ "</file>\n</xliff>\n";
		List<Event> events = U.getEvents(text);
		boolean hasEmptyMidFile = false;
		for ( Event event : events ) {
			if ( event.isMidFile() ) {
				MidFileData mfd = event.getMidFileData();
				assertFalse(mfd.hasExtElements());
				assertEquals(0, mfd.getNoteCount());
				hasEmptyMidFile = true;
				break;
			}
		}
		assertTrue(hasEmptyMidFile);
	}
	
}
