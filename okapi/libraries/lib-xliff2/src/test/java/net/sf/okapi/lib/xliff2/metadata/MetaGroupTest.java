package net.sf.okapi.lib.xliff2.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup.AppliesTo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class MetaGroupTest {
	
	@Test
	public void testSetGetMetaGroup () {
		MetaGroup mg  = new MetaGroup();
		assertTrue(mg.isEmpty());
		assertTrue(mg.isGroup());
		mg.add(new Meta("t1"));
		assertEquals(1, mg.size());
		assertFalse(mg.get(0).isGroup());
		assertNull(mg.getId());
		assertNull(mg.getCategory());
		assertEquals(AppliesTo.UNDEFINED, mg.getAppliesTo());
		mg.setId("id");
		mg.setCategory("cat");
		mg.setAppliesTo(AppliesTo.TARGET);
		assertEquals("id", mg.getId());
		assertEquals("cat", mg.getCategory());
		assertEquals(AppliesTo.TARGET, mg.getAppliesTo());
	}

	@Test
	public void testGroupInGroup () {
		MetaGroup mg1 = new MetaGroup("cat1");
		MetaGroup mg2 = (MetaGroup)mg1.add(new MetaGroup());
		mg2.setCategory("cat2");
		mg1.add(new Meta("t1", "d1"));
		assertEquals(2, mg1.size());
		MetaGroup mg2bis = (MetaGroup)mg1.get(0);
		assertSame(mg2, mg2bis);
		assertEquals("cat2", mg2bis.getCategory());
	}

}