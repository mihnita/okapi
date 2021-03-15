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

import java.util.Iterator;
import java.util.Stack;

import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.URIContext;

/**
 * Represents the base class for events iterator for documents, files, groups and units.
 */
class EventIterator implements Iterator<Event> {

	protected Stack<URIContext> uriContext;

	/**
	 * Sets the URI context for this iterator.
	 * @param uriContext the context to set.
	 */
	public void setURIContext (Stack<URIContext> uriContext) {
		this.uriContext = uriContext;
	}

	@Override
	public boolean hasNext () {
		return false;
	}

	@Override
	public Event next () {
		return null;
	}

	@Override
	public void remove () {
		throw new UnsupportedOperationException("Remove is not supported.");
	}

}
