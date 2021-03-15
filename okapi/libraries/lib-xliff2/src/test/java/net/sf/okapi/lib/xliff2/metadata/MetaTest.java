package net.sf.okapi.lib.xliff2.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import net.sf.okapi.lib.xliff2.InvalidParameterException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetaTest {
	
	@Test
	public void testSetGetMeta () {
		Meta meta  = new Meta("myType");
		assertEquals("myType", meta.getType());
		assertNull(meta.getData());
		meta.setData("data");
		meta.setType("newType");
		assertEquals("newType", meta.getType());
		assertEquals("data", meta.getData());
		meta  = new Meta("t", "d");
		assertEquals("t", meta.getType());
		assertEquals("d", meta.getData());
	}

	@Test (expected=InvalidParameterException.class)
	public void testNullType1 () {
		new Meta((String)null);
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testNullType2 () {
		new Meta(null, "data");
	}
	
	@Test (expected=InvalidParameterException.class)
	public void testNullType3 () {
		Meta meta = new Meta("t", "d");
		meta.setType(null);
	}
	
	public void testEmptyType () {
		Meta meta = new Meta("", "data");
		assertEquals("", meta.getType());
		assertEquals("data", meta.getData());
		meta = new Meta("");
		assertEquals("", meta.getType());
		meta.setType("");
		assertEquals("", meta.getType());
	}
	
	@Test
	public void testCopyConstruct () {
		Meta m1  = new Meta("t1", "d1");
		Meta m2  = new Meta(m1);
		assertNotSame(m1, m2);
		assertEquals(m1.getType(), m2.getType());
		assertEquals(m1.getData(), m2.getData());
	}

}
