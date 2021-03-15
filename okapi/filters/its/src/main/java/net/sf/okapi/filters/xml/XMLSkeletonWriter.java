/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xml;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;

/**
 * Implements ISkeletonWriter for the ITS filters 
 */
public class XMLSkeletonWriter extends GenericSkeletonWriter {

	/**
	 * Overrides the default behaviour to force "UTF-16" as declared XML encoding (not "UTF-16LE"
	 * or "UTF-16BE").
	 */
	@Override
	protected String getPropertyValue (INameable resource,
			String name,
			LocaleId locToUse,
			EncoderContext context) {
		String result = super.getPropertyValue(resource, name, locToUse, context);
		if (!Property.ENCODING.equals(name)) {
			return result;
		}

		if (result != null && result.startsWith("UTF-16")) {
			result = "UTF-16";
		}
		return result;
	}

}
