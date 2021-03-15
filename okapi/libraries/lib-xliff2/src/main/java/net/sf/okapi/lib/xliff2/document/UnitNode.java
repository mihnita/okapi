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

package net.sf.okapi.lib.xliff2.document;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.Unit;

/**
 * Represents a unit node.
 */
public class UnitNode implements IGroupOrUnitNode {

	private Unit data;

	/**
	 * Creates a new {@link UnitNode} with a given {@link Unit} resource.
	 * @param data the unit to set for this unit node (must not be null).
	 */
	public UnitNode (Unit data) {
		if ( data == null ) {
			throw new InvalidParameterException("The data associated with the new unit node must not be null.");
		}
		this.data = data;
	}
	
	/**
	 * Gets the {@link Unit} associated with this unit node.
	 * @return the unit object associated with this unit node.
	 */
	public Unit get () {
		return data;
	}

	@Override
	public boolean isUnit () {
		return true;
	}

}
