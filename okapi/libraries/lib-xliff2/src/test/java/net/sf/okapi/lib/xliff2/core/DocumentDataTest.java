package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DocumentDataTest {

	@Test
	public void testDefaults () {
		StartXliffData dd = new StartXliffData(null);
		assertEquals("2.0", dd.getVersion());
		assertNull(dd.getSourceLanguage());
		assertNull(dd.getTargetLanguage());
		assertTrue(dd.getExtAttributes().isEmpty());
	}
	
}
