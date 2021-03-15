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

package net.sf.okapi.lib.xliff2.its;

/**
 * Implements a group of {@link LocQualityIssue} objects.
 */
public class LocQualityIssues extends DataCategoryGroup<LocQualityIssue> {

	public LocQualityIssues (String id) {
		super(id);
	}

	@Override
	public String getDataCategoryName () {
		// A group returns the same name as its type
		return DataCategories.LOCQUALITYISSUE;
	}

	@Override
	public IITSItem createCopy () {
		LocQualityIssues newItem = new LocQualityIssues(getGroupId());
		for ( LocQualityIssue item : getList() ) {
			newItem.getList().add((LocQualityIssue)item.createCopy());
		}
		return newItem;
	}

}
