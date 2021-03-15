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
 * Implements the <a href='http://www.w3.org/TR/its20/#domain'>Domain</a> data category.
 */
public class Domain extends DataCategory {

	private String domain;

	/**
	 * Creates a new {@link Domain} object without initial data.
	 */
	public Domain () {
		// Needed in some cases
	}

	/**
	 * Creates a new {@link Domain} object with a value.
	 * @param domain the value to set.
	 */
	public Domain (String domain) {
		setDomain(domain);
	}

	@Override
	public String getDataCategoryName () {
		return "domain";
	}
	
	@Override
	public void validate () {
		// Nothing to validate
	}

	@Override
	public IITSItem createCopy () {
		Domain newItem = new Domain(domain);
		newItem.setAnnotatorRef(getAnnotatorRef());
		
		return newItem;
	}

	public String getDomain () {
		return domain;
	}

	public void setDomain (String domain) {
		this.domain = domain;
	}

}
