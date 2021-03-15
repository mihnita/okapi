/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.multiparsers;

/**
 * Provide access to the result of a parser call.
 * The token is either code or text and if it is text it can have an associated filter configuration.
 * The code parts are escaped and with delimiters is needed, the text part is not escaped.
 */
public class Token {

	private String data;
	private boolean isText;
	private String filterConfigId;

	public Token (String data,
		boolean isText,
		String filterConfigId)
	{
		this.data = data;
		this.isText = isText;
		this.filterConfigId = filterConfigId;
	}
	
	public String getData () {
		return data;
	}
	
	public boolean isText () {
		return isText;
	}
	
	public String getFilterConfigId () {
		return filterConfigId;
	}

}
