package net.sf.okapi.lib.xliff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class URIParserTest {

	@Test
	public void testURL1 () {
		URIParser up = new URIParser("#g=abc/u=123", "myFile", null, null);
		assertEquals("myFile", up.getFileId());
		assertEquals("abc", up.getGroupId());
		assertEquals("123", up.getUnitId());
	}

	@Test
	public void testURL2 () {
		URIParser up = new URIParser("#u=123", "myFile", "g1", null);
		assertEquals("myFile", up.getFileId());
		assertNull(up.getGroupId()); // context from group is not needed
		assertEquals("123", up.getUnitId());
	}
	
	@Test
	public void testURL3 ()
		throws MalformedURLException
	{
		assertEquals("u=123", new URL("http://www.test.net/file.xlf#u=123").getRef());
		assertEquals("u=123/m1", new URL("http://www.test.net/file.xlf#u=123/m1").getRef());
		assertEquals("f=xyz/u=u1/d=d1", new URL("http://www.test.net/file.xlf#f=xyz/u=u1/d=d1").getRef());
	}

	@Test
	public void testURI1 ()
		throws URISyntaxException
	{
		assertEquals("", new URIParser("").getURI().toString());
		assertEquals("f=f1/u=123", new URIParser("http://www.test.net/file.xlf#f=f1/u=123").getURI().getFragment());
		assertEquals("f=f1/u=123/m1", new URIParser("http://www.test.net/file.xlf#f=f1/u=123/m1").getURI().getFragment());
		assertEquals("f=xyz/u=u1/d=d1", new URIParser("http://www.test.net/file.xlf#f=xyz/u=u1/d=d1").getURI().getFragment());
	}
	
	@Test
	public void testParser1 () {
		URIParser up = new URIParser("");
		assertEquals("1", up.setURL("#1", "f1", null, "u1").getSourceInlineId());
		assertEquals("f=f1/u=u1/1", up.complementReference().getURI().getFragment());
		assertTrue(up.isXLIFF());
		
		assertEquals("2", up.setURL("#t=2", "f1", null, "u1").getTargetInlineId());
		assertEquals("f=f1/u=u1/t=2", up.complementReference().getURI().getFragment());
		assertTrue(up.isXLIFF());

		assertEquals("3", up.setURL("#u=3", "f1", null, null).getUnitId());
		assertEquals("f=f1/u=3", up.complementReference().getURI().getFragment());
		assertTrue(up.isXLIFF());
		
		assertEquals("4", up.setURL("#g=4", "f1", null, null).getGroupId());
		assertEquals("f=f1/g=4", up.complementReference().getURI().getFragment());
		assertTrue(up.isXLIFF());
		
		assertEquals("5", up.setURL("#n=5", "f1", null, null).getNoteId());
		assertEquals("f=f1/n=5", up.complementReference().getURI().getFragment());
		assertTrue(up.isXLIFF());
		
		assertEquals("6", up.setURL("#f=6", null, null, null).getFileId());
		assertEquals("f=6", up.complementReference().getURI().getFragment());

		assertEquals("6bis", up.setURL("#/f=6bis", null, null, null).getFileId());
		assertEquals("/f=6bis", up.complementReference().getURI().getFragment());

		assertEquals("7", up.setURL("#d=7", "f1", null, "1").getDataId());
		assertEquals("f=f1/u=1/d=7", up.complementReference().getURI().getFragment());

		up = new URIParser("#f=f1/g=g1/u=u1/1");
		assertEquals("f1", up.getFileId());
		assertEquals("g1", up.getGroupId());
		assertEquals("u1", up.getUnitId());
		assertEquals("1", up.getSourceInlineId());
		assertTrue(up.isXLIFF());
	}
	
	@Test
	public void testParser2 () {
		URIParser up = new URIParser("");
		assertEquals("1", up.setURL("#n=1", "f1", "g1", null).getNoteId());
		assertEquals("f=f1/g=g1/n=1", up.complementReference().getURI().getFragment());

		assertEquals("1", up.setURL("#g=3/n=1", "f1", "g1", null).getNoteId());
		assertEquals("f=f1/g=3/n=1", up.complementReference().getURI().getFragment());
	}
	
	@Test
	public void testParser3 () {
		URIParser up = new URIParser("");

		assertNull(up.setURL("myFile.xml#id1", null, null, null).getSourceInlineId());
		assertEquals("id1", up.complementReference().getURI().getFragment());
		assertFalse(up.isXLIFF());
		assertFalse(up.isFragmentOnly());

		assertEquals("id1", up.setURL("myFile.xml#f=f1/u=u1/id1", null, null, null).getSourceInlineId());
		assertEquals("f=f1/u=u1/id1", up.complementReference().getURI().getFragment());
		assertTrue(up.isXLIFF());
		assertFalse(up.isFragmentOnly());
	}
	
	@Test
	public void testRefTypeAndContainer () {
		URIParser up = new URIParser("#f=1/u=2");
		assertEquals("1", up.getFileId());
		assertEquals("2", up.getUnitId());
		assertEquals('u', up.getRefType());
		assertEquals('f', up.getRefContainer());
		
		up = new URIParser("#f=1/u=2/m1");
		assertEquals("2", up.getUnitId());
		assertEquals("m1", up.getSourceInlineId());
		assertEquals('s', up.getRefType());
		assertEquals('u', up.getRefContainer());

		up = new URIParser("#f=1/u=2/t=m1");
		assertEquals("2", up.getUnitId());
		assertEquals("m1", up.getTargetInlineId());
		assertEquals('t', up.getRefType());
		assertEquals('u', up.getRefContainer());
	
		up = new URIParser("#f=1/gls=term1");
		assertEquals("1", up.getFileId());
		assertEquals("term1", up.getExtensionInfo().getKey());
		assertEquals('x', up.getRefType());
		assertEquals('f', up.getRefContainer());
	}
	
	@Test
	public void testValidExtensions () {
		URIParser up = new URIParser("#u=u1/gls=id1", "f=f1", null, null);
		assertEquals("u1", up.getUnitId());
		assertEquals("id1", up.getExtensionInfo().getKey());
		assertEquals(Const.NS_XLIFF_GLOSSARY20, up.getExtensionInfo().getValue().get(0));
		
		up = new URIParser("#u=u1/mtc=c1", "f=f1", null, null);
		assertEquals("u1", up.getUnitId());
		assertEquals("c1", up.getExtensionInfo().getKey());
		assertEquals(Const.NS_XLIFF_MATCHES20, up.getExtensionInfo().getValue().get(0));
	}

	@Test
	public void testValidCustomExtensions () {
		URIParser up = new URIParser("");
		Map<String, String> map = new LinkedHashMap<>();
		map.put("myURI1", "p1");
		map.put("myURITwo", "p2");
		map.put("myURI1bis", "p1");
		up.addPrefixes(map);

		up.setURL("#f=f1/p2=x1", "f1", null, null);
		assertEquals("x1", up.getExtensionInfo().getKey());
		assertEquals("myURITwo", up.getExtensionInfo().getValue().get(0));
	}

	@Test
	public void testNoFragmentId () {
		URIParser up = new URIParser("u=7");
		assertNull(up.getUnitId());
		assertEquals("u=7", up.getURI().toString());
	}

	@Test (expected=InvalidParameterException.class)
	public void testInvalidPrefix1 () {
		new URIParser("#z=7");
	}

	@Test (expected=InvalidParameterException.class)
	public void testInvalidPrefix2 () {
		new URIParser("#u=1/GLS=2"); // Invalid because 'GLS' should be 'gls'
	}

	@Test (expected=InvalidParameterException.class)
	public void testInvalidSpaces () {
		new URIParser("#z = 7");
	}

	@Test (expected=InvalidParameterException.class)
	public void testInvalidRootedFragment () {
		new URIParser("#/u=myId"); // Cannot root on a unit
	}

	@Test (expected=InvalidParameterException.class)
	public void testInvalidNmtoken () {
		new URIParser("#u=not&nmtoken"); // '&' not allowed in NMTOKEN
	}

}
