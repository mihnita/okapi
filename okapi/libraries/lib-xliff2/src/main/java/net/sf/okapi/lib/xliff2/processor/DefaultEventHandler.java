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

package net.sf.okapi.lib.xliff2.processor;

import net.sf.okapi.lib.xliff2.reader.Event;

/**
 * Implements a default event handler for {@link XLIFFProcessor}.
 * Each handler does nothing. You can derive your own event handler from this class
 * and define only the handlers you need.
 */
public class DefaultEventHandler implements IEventHandler {

	@Override
	public Event handleStartDocument (Event event) {
		return event;
	}

	@Override
	public Event handleEndDocument (Event event) {
		return event;
	}

	@Override
	public Event handleStartXliff (Event event) {
		return event;
	}

	@Override
	public Event handleEndXliff (Event event) {
		return event;
	}

	@Override
	public Event handleStartFile (Event event) {
		return event;
	}

	@Override
	public Event handleSkeleton (Event event) {
		return event;
	}

	@Override
	public Event handleMidFile (Event event) {
		return event;
	}

	@Override
	public Event handleEndFile (Event event) {
		return event;
	}

	@Override
	public Event handleStartGroup (Event event) {
		return event;
	}

	@Override
	public Event handleEndGroup (Event event) {
		return event;
	}

	@Override
	public Event handleInsignificantPart (Event event) {
		return event;
	}

	@Override
	public Event handleUnit (Event event) {
		return event;
	}
	
}
