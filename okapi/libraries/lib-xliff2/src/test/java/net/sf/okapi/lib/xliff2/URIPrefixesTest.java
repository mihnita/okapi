package net.sf.okapi.lib.xliff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.FileLocation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class URIPrefixesTest {

	private FileLocation root = FileLocation.fromClass(URIPrefixesTest.class);
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testResolveWithDefaults () {
		URIPrefixes upr = new URIPrefixes();
		assertEquals(Const.NS_XLIFF_GLOSSARY20, upr.resolve("gls").get(0));
		assertEquals(Const.NS_XLIFF_MATCHES20, upr.resolve("mtc").get(0));
		assertEquals("urn:oasis:names:tc:xliff:resourcedata:2.0", upr.resolve("res").get(0));
	}

	@Test
	public void testResolveUnknown () {
		assertNull(new URIPrefixes().resolve("xyz"));
		assertNull(new URIPrefixes().resolve("GLS"));
	}
	
	@Test
	public void testCustomPrefixes () {
		URIPrefixes upr = new URIPrefixes(root.in("/extra-prefixes.properties").asFile());
		List<String> list = upr.resolve("tbx");
		assertEquals(1, list.size());
		assertEquals("urn:iso:std:iso:30042:ed-1:v1:en", list.get(0));
		// Additions
		list = upr.resolve("gls");
		assertEquals(2, list.size());
		assertEquals(Const.NS_XLIFF_GLOSSARY20, list.get(0));
		assertEquals("testGLSv2.x", list.get(1));
	}

	@Test
	public void testManualCustomPrefixes () {
		URIPrefixes upr = new URIPrefixes();
		Map<String, String> map = new LinkedHashMap<>();
		map.put("myURI1", "p1");
		map.put("myURITwo", "p2");
		map.put("myURI1bis", "p1");
		upr.add(map);
		List<String> list = upr.resolve("p2");
		assertEquals("myURITwo", list.get(0));
		list = upr.resolve("p1");
		assertEquals(2, list.size());
		assertEquals("myURI1", list.get(0));
		assertEquals("myURI1bis", list.get(1));
	}
	
	@Test (expected = XLIFFException.class)
	public void testBadCustomPrefix_TooShort () {
		URIPrefixes upr = new URIPrefixes(root.in("/bad-prefix-tooshort.properties").asFile());
		upr.resolve("xyz"); // Trigger the load
	}

	@Test (expected = XLIFFException.class)
	public void testBadCustomPrefix_NotNMTOKEN () {
		URIPrefixes upr = new URIPrefixes(root.in("/bad-prefix-notnmtoken.properties").asFile());
		upr.resolve("xyz"); // Trigger the load
	}

	@Test
	public void testBadCustomPrefix_DuplicatedURIs () {
		URIPrefixes upr = new URIPrefixes(root.in("/extra-prefixes.properties").asFile());
		// In case of a URI assigned to two different prefixes, the last one wins
		List<String> uris = upr.resolve("notTakenIntoAccount"); // Trigger the load
		assertNull(uris);
		uris = upr.resolve("my");
		assertEquals("myNS", uris.get(0));
	}

	@Test
	public void testRegistryLoader () {
		URIPrefixes upr = new URIPrefixes();
		try {
			Map<String, List<String>> map = upr.loadFromRegistry();
			assertTrue(map.size()>0);
			List<String> uris = map.get("my");
			assertNotNull(uris);
			assertEquals(1, uris.size());
			assertEquals("http://example.org/myXLIFFExtensionURI", uris.get(0));
		}
		catch ( XLIFFException e ) {
			logger.warn("WARNING: Error when accessing the prefix registry: {}", e.getMessage());
		}
	}

}
