/*===========================================================================
  Copyright (C) 2011-2013 by the Okapi Framework contributors
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
 * Implements the {@link IWithExtElements} interface.
 */
class DataWithExtElements implements IWithExtElements {

	private ExtElements xelems;

	@Override
	public ExtElements getExtElements () {
		if ( xelems == null ) xelems = new ExtElements();
		return xelems;
	}

	@Override
	public boolean hasExtElements () {
		if ( xelems == null ) return false;
		return (xelems.size() > 0);
	}

	@Override
	public ExtElements setExtElements (ExtElements elements) {
		this.xelems = elements;
		return getExtElements();
	}

}
