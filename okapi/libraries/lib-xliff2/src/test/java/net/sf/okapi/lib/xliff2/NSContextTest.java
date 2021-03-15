/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.lib.xliff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import javax.xml.XMLConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class NSContextTest {

	@Test
	public void testDefaults () {
		NSContext nsc = new NSContext();
		// getPrefix
		assertEquals(XMLConstants.XML_NS_PREFIX, nsc.getPrefix(XMLConstants.XML_NS_URI));
		assertEquals(XMLConstants.XMLNS_ATTRIBUTE, nsc.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI));
		assertNull(nsc.getPrefix(XMLConstants.NULL_NS_URI));

		// getPrefixes
		Iterator<String> iter = nsc.getPrefixes(XMLConstants.XML_NS_URI);
		assertEquals(iter.next(), XMLConstants.XML_NS_PREFIX);
		assertFalse(iter.hasNext());
		iter = nsc.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
		assertEquals(iter.next(), XMLConstants.XMLNS_ATTRIBUTE);
		assertFalse(iter.hasNext());
		iter = nsc.getPrefixes(XMLConstants.NULL_NS_URI);
		assertFalse(iter.hasNext());
		
		// Namespaces
		assertEquals(XMLConstants.XML_NS_URI, nsc.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
		assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, nsc.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE));
		assertEquals(XMLConstants.NULL_NS_URI, nsc.getNamespaceURI("not_a_prefix"));
	}
	
	@Test
	public void testAdd () {
		NSContext nsc = new NSContext("abc", "abcURI");
		nsc.put("xyz", "xyzURI");
		nsc.put("abc2", "abcURI");
		assertEquals("abcURI", nsc.getNamespaceURI("abc"));
		assertEquals("abcURI", nsc.getNamespaceURI("abc2"));
		assertEquals("xyz", nsc.getPrefix("xyzURI"));
		// If there are multiple occurrences of the URI, we have random prefix
		String res = nsc.getPrefix("abcURI");
		assertTrue(res.equals("abc")||res.equals("abc2"));
		// Test getPrefixes
		Iterator<String> iter = nsc.getPrefixes("xyzURI");
		assertEquals(iter.next(), "xyz");
		assertFalse(iter.hasNext());
		
		iter = nsc.getPrefixes("abcURI");
		String res1 = iter.next();
		String res2 = iter.next();
		assertTrue(res1.equals("abc")||res1.equals("abc2"));
		assertTrue(res2.equals("abc")||res2.equals("abc2"));
		assertNotEquals(res1, res2);
		assertFalse(iter.hasNext());
		// Contain a given pair
		assertTrue(nsc.containsPair("xyz", "xyzURI"));
		assertTrue(nsc.containsPair("abc", "abcURI"));
		assertTrue(nsc.containsPair("abc2", "abcURI"));
		assertFalse(nsc.containsPair("zzz", "abcURI"));
		assertFalse(nsc.containsPair("abc", "zzzURI"));
	}

}
