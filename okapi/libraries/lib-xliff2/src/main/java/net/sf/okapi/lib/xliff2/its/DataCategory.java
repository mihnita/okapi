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
 * Provides a base implementation of {@link IITSItem} for a given data category.
 */
public abstract class DataCategory implements IITSItem {

	private String annotatorRef;
	
	@Override
	public void setAnnotatorRef (String annotatorRef) {
		this.annotatorRef = annotatorRef;
	}

	@Override
	public void setAnnotatorRef (AnnotatorsRef ar) {
		if ( ar == null ) return;
		String ref = ar.get(getDataCategoryName());
		if ( ref != null ) {
			annotatorRef = ref;
		}
	}

	@Override
	public String getAnnotatorRef () {
		return annotatorRef;
	}

	@Override
	public boolean isGroup () {
		return false;
	}

	@Override
	public boolean hasUnresolvedGroup () {
		return false;
	}

}
