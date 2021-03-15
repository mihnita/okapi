package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.sf.okapi.lib.xliff2.InvalidParameterException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GroupDataTest {

	@Test
	public void testDefaults () {
		StartGroupData sgd = new StartGroupData("id1");
		assertEquals("id1", sgd.getId());
		assertTrue(sgd.getCanResegment());
		assertTrue(sgd.getTranslate());
		assertNull(sgd.getName());
		assertNull(sgd.getType());
		assertEquals(Directionality.AUTO, sgd.getSourceDir());
		assertEquals(Directionality.AUTO, sgd.getTargetDir());
		assertTrue(sgd.getExtAttributes().isEmpty());
		assertEquals(0, sgd.getNoteCount());
		assertTrue(sgd.getExtElements().isEmpty());
	}
	
	@Test
	public void testChanges () {
		StartGroupData sgd = new StartGroupData("id1");
		sgd.setId("newId");
		assertEquals("newId", sgd.getId());
		sgd.setCanResegment(false);
		assertFalse(sgd.getCanResegment());
		sgd.setTranslate(false);
		assertFalse(sgd.getTranslate());
		sgd.setName("name");
		assertEquals("name", sgd.getName());
		sgd.setType("my:type");
		assertEquals("my:type", sgd.getType());
		sgd.setSourceDir(Directionality.RTL);
		assertEquals(Directionality.RTL, sgd.getSourceDir());
		sgd.setTargetDir(Directionality.LTR);
		assertEquals(Directionality.LTR, sgd.getTargetDir());
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testInvalidTypeValue () {
		StartGroupData sgd = new StartGroupData("id");
		sgd.setType("badValue");
	}
	
}
