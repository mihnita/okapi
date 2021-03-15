package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MTagTest {

	@Test
	public void testSimple () {
		MTag anno = new MTag("1", "term");
		assertEquals("term", anno.getType());
		assertNull(anno.getTranslate());
	}
	
	@Test
	public void testEquals () {
		assertEquals(new MTag("1", "p:test1"), new MTag("1", "p:test1"));
		assertNotEquals(new MTag("1", "p:test2"), new MTag("2", "p:test2"));
	}
	
	@Test
	public void testType () {
		MTag am1 = new MTag("a1", "comment");
		assertEquals("comment", am1.getType());
		MTag am2 = new MTag("a2", "myPrefix:test");
		assertEquals("myPrefix:test", am2.getType());
	}

	@Test
	public void testCopyConstructor () {
		MTag am1 = new MTag("a1", "myPrefix:myType");
		am1.setValue("myValue");
		am1.setRef("ref");
		am1.setTranslate(false);
		// Create a deep clone and compare
		MTag am2 = new MTag(am1);
		assertEquals(am2, am2);
		assertNotSame(am2, am1);
	}

	@Test
	public void testExtensionAttributes () {
		MTag am = new MTag("a1", null);
		am.setValue("value");
		am.getExtAttributes().setAttribute("myNSURI", "myAttr", "xValue");
		assertEquals("xValue", am.getExtAttributes().getAttributeValue("myNSURI", "myAttr"));
	}
	
}
