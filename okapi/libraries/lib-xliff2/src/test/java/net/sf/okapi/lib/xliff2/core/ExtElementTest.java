package net.sf.okapi.lib.xliff2.core;

import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(JUnit4.class)
public class ExtElementTest {

	@Test
	public void testCreate () {
		ExtElement elem = new ExtElement(new QName("ns", "elem", "p"));
		assertEquals("ns", elem.getQName().getNamespaceURI());
		assertEquals("elem", elem.getQName().getLocalPart());
		assertEquals("p", elem.getQName().getPrefix());
	}
	
	@Test
	public void testAdd () {
		ExtElement elem = new ExtElement(new QName("ns", "elem", "p"));
		
		elem.addContent("content");
		assertEquals("content", elem.getFirstContent().getText());
		
		elem.addElement("elem2");
		assertEquals("ns", elem.getFirstElement().getQName().getNamespaceURI());
		assertEquals("elem2", elem.getFirstElement().getQName().getLocalPart());
		assertEquals("p", elem.getFirstElement().getQName().getPrefix());
		
		elem.addElement("ns2", "elem3", "q");
		ExtElement e = (ExtElement)elem.getChildren().get(2);
		assertEquals("ns2", e.getQName().getNamespaceURI());
		assertEquals("elem3", e.getQName().getLocalPart());
		assertEquals("q", e.getQName().getPrefix());
	}
	
	@Test
	public void testGetOrCreate () {
		Unit unit = new Unit("u1");
		unit.getExtElements().add("n1", "elem1", "my");
		
		// Make sure find() works
		ExtElements elems = unit.getExtElements();
		List<ExtElement> list = elems.find("n1", "elem1");
		assertEquals(1, list.size());
		
		// Get or create the existing element
		ExtElement res1 = elems.getOrCreate("n1", "elem1", "abc");
		assertSame(list.get(0), res1);
		
		// Get or create a non-existing element
		ExtElement res2 = elems.getOrCreate("n2", "elem1", "xyz");
		list = elems.find("n2", "elem1");
		assertEquals(1, list.size());
		assertSame(list.get(0), res2);
	}
	
}
