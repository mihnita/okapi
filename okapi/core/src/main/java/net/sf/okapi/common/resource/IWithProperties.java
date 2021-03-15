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

package net.sf.okapi.common.resource;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public interface IWithProperties {
	/**
	 * Copy all {@link IWithProperties} <i>Unless destination already has the property.</i>
	 * Properties are cloned before copying.
	 * @param from source of {@link IWithProperties}
	 * @param to destination of {@link IWithProperties}
	 */
	static void copy(final IWithProperties from, final IWithProperties to) {
		if (from == null || to == null) {
			return;
		}

		for (final String propName : from.getPropertyNames()) {
			// never overwrite a property - it may be updated
			if (!to.hasProperty(propName)) {
				to.setProperty(from.getProperty(propName).clone());
			}
		}
	}

	/**
	 * @return {@link Map} of properties for the implementer of interface
	 */
	Map<String, Property> getProperties();

	/**
	 * Gets the resource-level property for a given name.
	 * 
	 * @param name Name of the property to retrieve.
	 * @return The property or null if it does not exist.
	 */
	default Property getProperty(final String name) {
		return getProperties().get(name);
	}

	/**
	 * Gets the names of all the resource-level properties for this resource.
	 * @return All the names of the resource-level properties for this resource.
	 */
	default Set<String> getPropertyNames() {
		return getProperties().keySet();
	}

	/**
	 * Indicates if a resource-level property exists for a given name.
	 * @param name The name of the resource-level property to query.
	 * @return True if a resource-level property exists, false otherwise.
	 */
	default boolean hasProperty(final String name) {
		return getProperties().containsKey(name);
	}

	/**
	 * Removes a resource-level property of a given name. If the property does not exists
	 * nothing happens.
	 * @param name The name of the property to remove.
	 */
	default void removeProperty(final String name) {
		getProperties().remove(name);
	}

	/**
	 * Sets a resource-level property. If a property already exists it is overwritten.
	 * @param property The new property to set.
	 * @return The property that has been set.
	 */
	default Property setProperty(final Property property) {
		getProperties().put(property.getName(), property);
		return property;
	}

	/**
	 * Gets an iterator of the properties for this resource.
	 * 
	 * @return an iterator list of the properties for this resource.
	 */
	default Iterator<Property> propertyIterator() {
		return getProperties().values().iterator();
	}
}
