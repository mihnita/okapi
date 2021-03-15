package net.sf.okapi.lib.xliff2.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import net.sf.okapi.lib.xliff2.test.U;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TagsTest {

	@Test
	public void testGet () {
		Tags m = createSource();
		Tag bm = m.get("m1", TagType.CLOSING);
		assertSame(bm, m.get(U.kCA(0)));
	}

	@Test
	public void testAdd () {
		Tags m = createSource();
		Tag bm = new CTag(TagType.STANDALONE, "c1", "[ph/]");
		m.add(bm);
		assertEquals(3, m.size());
		assertSame(bm, m.get(U.kSC(0)));
	}

	@Test
	public void testRemove () {
		Tags m = createSource();
		m.remove(U.kCA(0));
		assertEquals(1, m.size());
		assertNull(m.get("m1", TagType.CLOSING));
	}

	private Tags createSource () {
		Unit unit = new Unit("1");
		Tags m = unit.getStore().getSourceTags();
		MTag sm = new MTag("m1", "comment");
		m.add(sm);
		m.add(new MTag(sm));
		return m;
	}
}
