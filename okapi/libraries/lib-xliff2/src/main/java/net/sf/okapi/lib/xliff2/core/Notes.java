/*===========================================================================
  Copyright (C) 2012-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.core;

/**
 * Represents a list of {@link Note} objects.
 */
public class Notes extends BaseList<Note> implements IWithExtAttributes {

	private ExtAttributes xattrs;

	/**
	 * Creates an empty {@link Notes} object.
	 */
	public Notes () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Notes (Notes original) {
		super(original);
		if ( original.hasExtAttribute() ) {
			xattrs = new ExtAttributes(original.xattrs);
		}
	}
	
	@Override
	public void setExtAttributes (ExtAttributes attributes) {
		this.xattrs = attributes;
	}

	@Override
	public ExtAttributes getExtAttributes () {
		if ( xattrs == null ) {
			xattrs = new ExtAttributes();
		}
		return xattrs;
	}

	@Override
	public boolean hasExtAttribute () {
		if ( xattrs == null ) return false;
		return !xattrs.isEmpty();
	}

	@Override
	public String getExtAttributeValue (String namespaceURI,
		String localName)
	{
		if ( xattrs == null ) return null;
		return xattrs.getAttributeValue(namespaceURI, localName);
	}

}
