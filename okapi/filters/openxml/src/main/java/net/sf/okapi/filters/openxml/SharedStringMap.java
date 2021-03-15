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
import java.util.List;
import java.util.Objects;

public class SharedStringMap {
	private ArrayList<Entry> entries = new ArrayList<>();
	private int nextIndex = 0;

	public static class Entry {
		private int origIndex, newIndex;
		private boolean excluded;
		private String cell;
		private String worksheet;

		Entry(int origIndex, int newIndex, boolean excluded) {
			this.origIndex = origIndex;
			this.newIndex = newIndex;
			this.excluded = excluded;
		}

		Entry(int origIndex, int newIndex, boolean excluded, String cell, String worksheet) {
			this.origIndex = origIndex;
			this.newIndex = newIndex;
			this.excluded = excluded;
			this.cell = cell;
			this.worksheet = worksheet;
		}

		public int getOriginalIndex() {
			return origIndex;
		}
		public int getNewIndex() {
			return newIndex;
		}
		public boolean getExcluded() {
			return excluded;
		}
		public String getCell() { return cell; }
		public String getWorksheet() { return worksheet; }

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof Entry)) return false;
			Entry e = (Entry)o;
			return origIndex == e.origIndex && newIndex == e.newIndex && excluded == e.excluded;
		}
		@Override
		public int hashCode() {
			return Objects.hash(origIndex, newIndex, excluded);
		}
		@Override
		public String toString() {
			return "SS Entry(" + origIndex + " --> " + newIndex + ", " +
						(excluded ? "excluded" : "visible") + ", " +
				        cell + ", " +
				        worksheet + ")";
		}
	}

	public Entry createEntryForString(int origIndex, boolean excluded) {
		Entry e = new Entry(origIndex, nextIndex++, excluded, null, null);
		entries.ensureCapacity(nextIndex);
		entries.add(e.newIndex, e);
		return e;
	}

	public Entry createEntryForString(int origIndex, boolean excluded, String cell, String worksheet) {
		Entry e = new Entry(origIndex, nextIndex++, excluded, cell, worksheet);
		entries.ensureCapacity(nextIndex);
		entries.add(e.newIndex, e);
		return e;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public boolean isStringVisible(int index) {
		return !entries.get(index).excluded;
	}

	public String getStringCell(int index) {
		return entries.get(index).getCell();
	}

	public String getStringWorksheet(int index) {
		return entries.get(index).getWorksheet();
	}
}
