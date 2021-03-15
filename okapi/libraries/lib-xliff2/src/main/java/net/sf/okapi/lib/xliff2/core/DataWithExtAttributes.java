/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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

import java.util.Iterator;
import java.util.Objects;

/**
 * Implements the {@link IWithExtAttributes} interface.
 */
public class DataWithExtAttributes implements IWithExtAttributes {

	private ExtAttributes xattrs;

	/**
	 * Creates an empty {@link DataWithExtAttributes} object.
	 */
	protected DataWithExtAttributes () {
		// Nothing to do
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	protected DataWithExtAttributes (DataWithExtAttributes original) {
		if ( original.hasExtAttribute() ) {
			setExtAttributes(new ExtAttributes(original.xattrs));
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

	/**
	 * Indicates if this object is equals to a given one.
	 * @param other the other object to compare.
	 * @return true if the two objects are identical.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (!(other instanceof  DataWithExtAttributes)) return false;

		DataWithExtAttributes that = (DataWithExtAttributes) other;
		if (this.xattrs == that.xattrs) return true;
		if (this.xattrs == null || that.xattrs == null) return false;

		Iterator<ExtAttribute> thisIter = this.xattrs.iterator();
		Iterator<ExtAttribute> thatIter = that.xattrs.iterator();
		while (thisIter.hasNext() ) {
			if (!thatIter.hasNext() ) return false;
			ExtAttribute thisObj = thisIter.next();
			ExtAttribute thatObj = thatIter.next();
			if (!Objects.equals(thisObj, thatObj)) return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return Objects.hash(xattrs);
	}
}
