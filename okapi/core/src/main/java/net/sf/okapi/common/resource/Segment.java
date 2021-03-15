/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource;

import net.sf.okapi.common.IResource;

/**
 * Implement a special content part that is a segment.
 * A segment is a {@link TextPart} with an identifier.
 */
public class Segment extends TextPart {

	public static String REF_MARKER = "$segment$";

	/**
	 * Creates an empty Segment object with a null identifier.
	 */
	public Segment () {
		super();
	}

	/**
	 * Creates an empty Segment object with a given identifier.
	 * @param id identifier for the new segment (Can be null).
	 */
	public Segment(String id) {
		this(id, new TextFragment());
	}

	/**
	 * Creates a Segment object with a given identifier and a given
	 * text fragment.
	 * @param id identifier for the new segment (Can be null).
	 * @param text text fragment for the new segment.
	 */
	public Segment(String id, TextFragment text)
	{
		this();
		this.id = id;
		this.text = text;
	}

	@Override
	public Segment clone () {
		Segment newSeg = new Segment(id, getContent().clone());
		newSeg.originalId = originalId;
		newSeg.whitespaceSrategy = whitespaceSrategy;
		IWithProperties.copy(this, newSeg);
		IWithAnnotations.copy(this, newSeg);
		return newSeg;
	}

	@Override
	public boolean isSegment () {
		return true;
	}

	public static String makeRefMarker(String segId) {
		return TextFragment.makeRefMarker(segId, REF_MARKER);
	}

	/**
	 * 
	 * @param id
	 * @deprecated use {@link IResource#setId(String)}
	 */
	@Deprecated
	public void forceId(String id) {
		setId(id);
	}
}
