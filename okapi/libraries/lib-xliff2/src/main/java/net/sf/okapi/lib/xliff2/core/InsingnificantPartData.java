/*===========================================================================
  Copyright (C) 2012-2013 by the Okapi Framework contributors
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

/**
 * Represents the information associated with an insignificant part
 * (from the XLIFF viewpoint).
 */
public class InsingnificantPartData {

	/**
	 * Types of insignificant parts.
	 */
	public enum InsignificantPartType {
		/**
		 * Normal text content (e.g indentation, etc).
		 */
		TEXT,
		/**
		 * XML Comment.
		 */
		COMMENT,
		/**
		 * XML processing Instruction.
		 */
		PI
	}

	private String data;
	private InsignificantPartType type;
	
	/**
	 * Creates a new {@link InsingnificantPartData} object.
	 * @param type the type of the object.
	 * @param data the data for the new part.
	 */
	public InsingnificantPartData (InsignificantPartType type,
		String data)
	{
		this.type = type;
		this.data = data;
	}

	/**
	 * Gets the data for this {@link InsingnificantPartData} object.
	 * @return the data for this {@link InsingnificantPartData} object.
	 */
	public String getData () {
		return data;
	}
	
	/**
	 * Gets the type for this {@link InsingnificantPartData} object.
	 * @return the type for this {@link InsingnificantPartData} object.
	 */
	public InsignificantPartType getType () {
		return type;
	}

}
