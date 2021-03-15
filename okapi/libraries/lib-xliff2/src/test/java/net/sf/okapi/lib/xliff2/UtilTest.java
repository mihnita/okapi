package net.sf.okapi.lib.xliff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.xml.namespace.QName;

import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class UtilTest {

	@Test
	public void testIsValidNmtoken () {
		String supChar = new StringBuilder().appendCodePoint(0x20B9F).toString();
		// Valid
		assertTrue(Util.isValidNmtoken("123"));
		assertTrue(Util.isValidNmtoken("abc"));
		assertTrue(Util.isValidNmtoken("\u0100etc"));
		assertTrue(Util.isValidNmtoken(".id"));
		assertTrue(Util.isValidNmtoken("id" + supChar));
		// Invalid
		assertFalse(Util.isValidNmtoken("#id"));
		assertFalse(Util.isValidNmtoken("val/"));
		assertFalse(Util.isValidNmtoken("id&"));
		assertFalse(Util.isValidNmtoken("~ab"));
		assertFalse(Util.isValidNmtoken("a=b"));
		assertFalse(Util.isValidNmtoken("id$"));
		assertFalse(Util.isValidNmtoken(supChar + "@"));
		assertFalse(Util.isValidNmtoken("id|"));
	}
	
	@Test
	public void testRemoveExtensions () {
		Unit unit = new Unit("u1");
		Segment seg = unit.appendSegment();
		seg.setSource("text");
		unit.getExtAttributes().setAttribute("myNS", "attr1", "val1");
		unit.getExtAttributes().setAttribute(Const.NS_XLIFF_FS20, "fs", "p");
		unit.getExtAttributes().setAttribute("myNS", "attr3", "val3");
		unit.getExtAttributes().setAttribute("myNS", "attr4", "val4");
		unit.getExtElements().add(new ExtElement(new QName("myNS", "elem1")));
		unit.getExtElements().add(new ExtElement(new QName("myNS", "elem2")));
		unit.getExtElements().add(new ExtElement(new QName(Const.NS_XLIFF_GLOSSARY20, "gloss"))); // Invalid, but it's just for test
		unit.getExtElements().add(new ExtElement(new QName("myNS", "elem4")));

		assertTrue(unit.hasExtAttribute());
		assertEquals(4, unit.getExtAttributes().size());
		assertTrue(unit.hasExtElements());
		assertEquals(4, unit.getExtElements().size());
		
		Util.removeExtensions(unit);
		
		assertEquals(1, unit.getExtAttributes().size());
		assertEquals("p", unit.getExtAttributeValue(Const.NS_XLIFF_FS20, "fs"));
		assertEquals(1, unit.getExtElements().size());
		assertEquals("gloss", unit.getExtElements().get(0).getQName().getLocalPart());
	}

	@Test
	public void testLang () {
		// Valid
		assertNull(Util.validateLang("en"));
		assertNull(Util.validateLang("en-us"));
		assertNull(Util.validateLang("i-klingon"));
		assertNull(Util.validateLang("mN-cYrL-Mn"));
		assertNull(Util.validateLang("en-x-US"));
		assertNull(Util.validateLang("es-419"));
		assertNull(Util.validateLang("az-Arab-x-AZE-derbend"));
		assertNull(Util.validateLang("sl-Latn-IT-rozaj"));
		assertNull(Util.validateLang("zh-cmn-Hant-HK"));
		assertNull(Util.validateLang("en-Latn-GB-boont-r-extended-sequence-x-private"));
		
		// Not valid
		assertNotNull(Util.validateLang(null));
		assertNotNull(Util.validateLang(""));
		assertNotNull(Util.validateLang("f-Latn"));
		assertNotNull(Util.validateLang("fra-FX"));
		assertNotNull(Util.validateLang("zh-Latm-CN"));
		assertNotNull(Util.validateLang("de-DE-1902"));
		assertNotNull(Util.validateLang("fr-shadok"));
	}
	
	@Test
	public void testSupports () {
		// Supported directly
		assertTrue(Util.supports(Const.NS_XLIFF_MATCHES20));
		assertTrue(Util.supports(Const.NS_XLIFF_METADATA20));
		assertTrue(Util.supports(Const.NS_XLIFF_GLOSSARY20));
		assertTrue(Util.supports(Const.NS_XLIFF_VALIDATION20));
		assertTrue(Util.supports(Const.NS_XLIFF_TRACKING20));
		// Not directly supported
		assertFalse(Util.supports(Const.NS_XLIFF_FS20));
		assertFalse(Util.supports(Const.NS_XLIFF_RESDATA20));
		assertFalse(Util.supports(Const.NS_XLIFF_SIZE20));
	}

	@Test
	public void testIsValidXML () {
		assertTrue(Util.isValidInXML('c'));
		assertTrue(Util.isValidInXML(0x10FFFF));
		assertFalse(Util.isValidInXML(0x7FFFFFFF));
		assertFalse(Util.isValidInXML(0x8FFFFFFF));
		assertFalse(Util.isValidInXML(0x110000));
		assertFalse(Util.isValidInXML('\u000C'));
	}

}
