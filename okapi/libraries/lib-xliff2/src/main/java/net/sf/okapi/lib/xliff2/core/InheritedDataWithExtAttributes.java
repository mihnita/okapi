/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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
 * Implements an object implements the {@link IWithInheritedData}, {@link IWithExtAttributes}
 * and {@link IWithMetadata} interfaces. It also provides methods for id access.
 */
class InheritedDataWithExtAttributes extends InheritedData implements IWithExtAttributes {

	private ExtAttributes xattrs;
	private String id;
	
	/**
	 * Creates an empty {@link InheritedDataWithExtAttributes} object.
	 */
	protected InheritedDataWithExtAttributes () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	protected InheritedDataWithExtAttributes (InheritedDataWithExtAttributes original) {
		// Create the new object from the base class copy constructor
		super(original);
		// Copy the InheritedDataWithExtAttributes-specific fields
		id = original.id;
		if ( original.hasExtAttribute() ) {
			xattrs = new ExtAttributes(original.xattrs);
		}
	}

	/**
	 * Sets the id for this object.
	 * @param id the id for this object.
	 */
	public void setId (String id) {
		this.id = id;
	}
	
	/**
	 * Gets the id for this object.
	 * @return the id for this object.
	 */
	public String getId () {
		return id;
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
