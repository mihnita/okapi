/*===========================================================================
  Copyright (C) 2016-2020 by the Okapi Framework contributors
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

import java.util.Comparator;

public class TextPartComparator implements Comparator<TextPart> {
	/**
	 * Match on id and type (segment or not) or TextFragment. If no id exists match on TextFragment (including
	 * all codes)
	 */
	@Override
	public int compare(final TextPart orig, final TextPart trans) {
		if (orig == null && trans == null) {
			return 0;
		}

		// we know both can't be null at this point
		if (orig == null || trans == null) {
			return -1;
		}

		// the two parts must be the same type
		if (orig.isSegment() != trans.isSegment()) {
			return -1;
		}

		// if both have id's then we consider this the strongest match
		if (orig.id != null && orig.id.equals(trans.getId())) {
			return 0;
		}
		// otherwise match on the text only
		return orig.text.compareTo(trans.text, true);
	}
}
