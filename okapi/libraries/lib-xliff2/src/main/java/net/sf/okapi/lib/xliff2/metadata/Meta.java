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

package net.sf.okapi.lib.xliff2.metadata;

import net.sf.okapi.lib.xliff2.InvalidParameterException;

/**
 * Represents a meta element.
 */
public class Meta implements IMetadataItem {

	private String type;
	private String data;
	
	/**
	 * Creates a {@link Meta} object with a given type.
	 * @param type the type of the object (cannot be null).
	 */
	public Meta (String type) {
		setType(type);
	}

	/**
	 * Creates a {@link Meta} object with a given type and data.
	 * @param type the type of the object (cannot be null).
	 * @param data the data of the object (can be null).
	 */
	public Meta (String type,
		String data)
	{
		setType(type);
		setData(data);
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Meta (Meta original) {
		type = original.type;
		data = original.data;
	}

	@Override
	public boolean isGroup () {
		return false;
	}
	
	public String getType () {
		return type;
	}

	public void setType (String type) {
		if ( type == null ) {
			throw new InvalidParameterException("The type of a <meta> must not be null.");
		}
		this.type = type;
	}

	public String getData () {
		return data;
	}

	public void setData (String data) {
		this.data = data;
	}

}
