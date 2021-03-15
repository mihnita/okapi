/*===========================================================================
  Copyright (C) 2013-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.reader;

import java.io.InputStream;
import java.util.HashMap;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.XLIFFException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Performs the validation of the names and location of the modules in the extension points
 * that the schema-based validation cannot do because the core schema does not have references
 * to the modules.
 */
class LocationValidator {

	/**
	 * The attribute or element for the module is allowed at the given location.
	 */
	public static final int ALLOWED = 0;
	/**
	 * No module is defined at the given location.
	 */
	public static final int NO_MODULES = 1;
	/**
	 * The attribute or element for the module is invalid or not allowed at the given location.
	 */
	public static final int NOT_ALLOWED = 2;
	/**
	 * The given location does not allow more than one of the given element. 
	 */
	public static final int TOO_MANY = 3;
	
	private HashMap<String, AllowedModules> map;

	/**
	 * Loads the file containing the definitions of what module's elements and attributes are
	 * allowed in what extension points.
	 * @param inputStream the input stream of the file to load.
	 */
	public void load (InputStream inputStream) {
		try {
			map = new HashMap<>();
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(inputStream);
			
			NodeList list1 = doc.getDocumentElement().getElementsByTagName("location");
			if (( list1 == null ) || ( list1.getLength() == 0 )) return;
			
			for ( int i=0; i<list1.getLength(); i++ ) {
				Element elem = (Element)list1.item(i);
				String name = elem.getAttribute("name");
				if ( name.isEmpty() ) throw new XLIFFReaderException("Invalid empty name in modules file.");
				// Create the list where to put the allowed modules
				AllowedModules mods = new AllowedModules();
				// Gather the list of allowed attributes
				NodeList list2 = elem.getElementsByTagName("attribute");
				for ( int j=0; j<list2.getLength(); j++ ) {
					Element elem2 = (Element)list2.item(j);
					String qString = elem2.getAttribute("name");
					mods.addAttribute(qString);
				}
				// Gather the list of allowed elements
				list2 = elem.getElementsByTagName("element");
				for ( int j=0; j<list2.getLength(); j++ ) {
					Element elem2 = (Element)list2.item(j);
					String qString = elem2.getAttribute("name");
					boolean zeroOrMore = elem2.getAttribute("zeroOrMore").equals("yes");
					mods.addElement(qString, zeroOrMore);
				}
				// Set the information for the given element
				map.put(name, mods);
			}
		}
		catch ( Throwable e ) {
			throw new XLIFFException(e.getLocalizedMessage());
		}
	}
	
	/**
	 * Verifies if a given element or attribute is allowed.
	 * @param parentName the locale name of the parent element.
	 * @param qName the qualified name of the candidate.
	 * @param attribute true if the candidate to verify is an attribute.
	 * @return one of the following values: {@link #ALLOWED}, {@link #NO_MODULES}, {@link #NOT_ALLOWED} or {@link #TOO_MANY}.
	 */
	public int verify (String parentName,
		QName qName,
		boolean attribute)
	{
		if ( !qName.getNamespaceURI().startsWith(Const.NS_XLIFF_MODSTART) ) {
			// Not a module: no check
			return ALLOWED;
		}
		// Get the list of allowed elements or attributes for this parent
		AllowedModules mods = map.get(parentName);
		if ( mods == null ) {
			return NO_MODULES; // Not allowed (nothing defined for this parent element
		}
		if ( attribute ) {
			return mods.isAllowedAttribute(qName);
		}
		// Else: it's an element
		
		return mods.isAllowedElement(qName);
	}

	/**
	 * Resets the counters for the allowed elements.
	 * This should be called at the end or the start of processing the extension elements of a given element.
	 */
	public void reset () {
		for ( AllowedModules mods : map.values() ) {
			mods.reset();
		}
	}
	
}
