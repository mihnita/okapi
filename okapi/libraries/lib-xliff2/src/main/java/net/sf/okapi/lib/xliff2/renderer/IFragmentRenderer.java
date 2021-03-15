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

import java.util.Stack;

import net.sf.okapi.lib.xliff2.NSContext;
import net.sf.okapi.lib.xliff2.core.Fragment;

/**
 * Provides an iterable interface for the objects making up a fragment.
 * Each object return by the iterator implements {@link IFragmentObject} providing both: 
 * access to the underlying object, and a string representation of that object. 
 */
public interface IFragmentRenderer extends Iterable<IFragmentObject> {

	/**
	 * Sets the fragment to iterate through and the namespace context.
	 * @param fragment the fragment to iterate through.
	 * @param nsStack the namespace context (can be null).
	 */
	void set(Fragment fragment,
			 Stack<NSContext> nsStack);

}
