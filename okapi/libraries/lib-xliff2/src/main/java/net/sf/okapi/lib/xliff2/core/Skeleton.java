/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the information associated with the skeleton.
 */
public class Skeleton {
	
	private String href;
	private ArrayList<IExtChild> children;

	/**
	 * Sets the href for this skeleton.
	 * @param href the new href to set (can be null).
	 */
	public void setHref (String href) {
		this.href = href;
	}
	
	/**
	 * Gets the href for this skeleton.
	 * @return the href for this skeleton (can be null).
	 */
	public String getHref () {
		return href;
	}

	/**
	 * Adds an extension child to this skeleton.
	 * @param child the extension child object to add.
	 */
	public void addChild (IExtChild child) {
		if ( children == null ) children = new ArrayList<>(2);
		children.add(child);
	}

	/**
	 * Gets the list of extension children for this skeleton.
	 * @return the list of extension children for this skeleton (can be null).
	 */
	public List<IExtChild> getChildren () {
		return children;
	}
	
}
