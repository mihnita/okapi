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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Represent the module elements and attributes at a given extension point.
 * This is used for validating modules.
 */
class AllowedModules {

	private final List<String> attributes;
	private final List<ElementInfo> elements;
	
	private static class ElementInfo {
		
		String qString;
		boolean zeroOrMore;
		int count;
		
		ElementInfo (String qString, boolean zeroOrMore) {
			this.qString = qString;
			this.zeroOrMore = zeroOrMore;
		}
	}

	public AllowedModules () {
		attributes = new ArrayList<>();
		elements = new ArrayList<>();
	}

	public void addAttribute (String qString) {
		attributes.add(qString);
	}

	public void addElement (String qString,
		boolean zeroOrMore)
	{
		elements.add(new ElementInfo(qString, zeroOrMore));
	}

	public int isAllowedAttribute (QName qName) {
		return attributes.contains(qName.toString()) ? LocationValidator.ALLOWED : LocationValidator.NOT_ALLOWED;
	}

	public int isAllowedElement (QName qName) {
		// Get or create the counter for the given element
		for ( ElementInfo info : elements ) {
			if ( info.qString.equals(qName.toString()) ) {
				info.count++;
				if (( info.count > 1 ) && !info.zeroOrMore ) {
					return LocationValidator.TOO_MANY; // Not allowed because occurring too many times
				}
				return LocationValidator.ALLOWED; // Allowed
			}
		}
		return LocationValidator.NOT_ALLOWED; // Not allowed
	}
	
	/**
	 * Resets the counters for each elements allowed in the context.
	 */
	public void reset () {
		for ( ElementInfo info : elements ) {
			info.count = 0;
		}
	}

}
