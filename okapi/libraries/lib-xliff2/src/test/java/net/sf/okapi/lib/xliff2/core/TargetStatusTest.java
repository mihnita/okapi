package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TargetStatusTest {

	@Test
	public void testSimple () {
		assertEquals("initial", TargetState.INITIAL.toString());
		assertEquals("translated", TargetState.TRANSLATED.toString());
		assertEquals("reviewed", TargetState.REVIEWED.toString());
		assertEquals("final", TargetState.FINAL.toString());
	}
	
	@Test
	public void testCompare () {
		assertTrue(TargetState.INITIAL.compareTo(TargetState.TRANSLATED) < 0);
		assertTrue(TargetState.INITIAL.compareTo(TargetState.REVIEWED) < 0);
		assertTrue(TargetState.TRANSLATED.compareTo(TargetState.REVIEWED) < 0);
		assertTrue(TargetState.REVIEWED.compareTo(TargetState.FINAL) < 0);
		assertTrue(TargetState.INITIAL.compareTo(TargetState.FINAL) < 0);
		assertTrue(TargetState.TRANSLATED.compareTo(TargetState.FINAL) < 0);
	}

	@Test
	public void testToFromString () {
		assertEquals(TargetState.INITIAL, TargetState.fromString(TargetState.INITIAL.toString()));
		assertEquals(TargetState.TRANSLATED, TargetState.fromString(TargetState.TRANSLATED.toString()));
		assertEquals(TargetState.REVIEWED, TargetState.fromString(TargetState.REVIEWED.toString()));
		assertEquals(TargetState.FINAL, TargetState.fromString(TargetState.FINAL.toString()));
	}

}
