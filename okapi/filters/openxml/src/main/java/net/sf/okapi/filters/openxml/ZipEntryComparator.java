/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;

class ZipEntryComparator implements Comparator<ZipEntry> {
	private final List<String> names;

	ZipEntryComparator(List<String> additionalPartNames) {
		names = new ArrayList<>(
			Arrays.asList(
				ContentTypes.PART_NAME,
				Relationships.ROOT_RELS_PART_NAME
			)
		);
		names.addAll(additionalPartNames);
	}

	@Override
	public int compare(ZipEntry o1, ZipEntry o2) {
		int index1 = names.indexOf(o1.getName());
		int index2 = names.indexOf(o2.getName());
		if (index1 == -1) index1 = Integer.MAX_VALUE;
		if (index2 == -1) index2 = Integer.MAX_VALUE;
		return Integer.compare(index1, index2);
	}
}