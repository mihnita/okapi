/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.renderer;

import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.MTag;

/*
 * Represents an object among those composing a fragment. 
 */
public interface IFragmentObject {

	/**
	 * Generates the output for a given format.
	 * @return the string representation of this inline object.
	 */
	String render();
	
	/**
	 * Gets the text of this object, if the object is a String.
	 * @return the text.
	 * @throws ClassCastException if the object is not a String.
	 */
	String getText();
	
	/**
	 * Gets the {@link CTag} of this object, if the object is of that type
	 * @return the CTag of this object.
	 * @throws ClassCastException if the object is not a CTag.
	 */
	CTag getCTag();
	
	/**
	 * Gets the {@link MTag} of this object, if the object is of that type
	 * @return the MTag of this object.
	 * @throws ClassCastException if the object is not a MTag.
	 */
	MTag getMTag();

	/**
	 * Gets the original object.
	 * @return the original object.
	 */
	Object getObject();

}
