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

import java.util.Map;
import java.util.Set;

import net.sf.okapi.common.LocaleId;

public interface IWithSourceProperties {
	/**
	 * Copy all {@link IWithSourceProperties} <i>Unless destination already has the property.</i>
	 * Target {@link LocaleId} not found in the destination are added then properties are copied.
	 * Properties are cloned before copying.
	 * @param from source of {@link IWithSourceProperties}
	 * @param to destination of {@link IWithSourceProperties}
	 */
	static void copy(final IWithSourceProperties from, final IWithSourceProperties to) {
		if (from == null || to == null) {
			return;
		}

		for (final String propName : from.getSourcePropertyNames()) {
			// never overwrite a property - it may be updated
			if (!to.hasSourceProperty(propName)) {
				to.setSourceProperty(from.getSourceProperty(propName).clone());
			}
		}
	}

	/**
	 * @return {@link Map} of source properties for the implementer of interface 
	 */
	Map<String, Property> getSourceProperties();

	/**
	 * Gets the source property for a given name.
	 * @param name The name of the source property to retrieve.
	 * @return The property or null if it does not exist.
	 */
	default Property getSourceProperty (final String name) {
		return getSourceProperties().get(name);
	}

	/**
	 * Gets the names of all the source properties for this resource.
	 * @return All the names of the source properties for this resource.
	 */
	default Set<String> getSourcePropertyNames () {
		return getSourceProperties().keySet();
	}

	/**
	 * Indicates if a source property exists for a given name.
	 * @param name The name of the source property to query.
	 * @return True if a source property exists, false otherwise.
	 */
	default boolean hasSourceProperty (final String name) {
		return getSourceProperties().containsKey(name);
	}

	/**
	 * Removes a source property of a given name. If the property does not exists
	 * nothing happens.
	 * @param name The name of the property to remove.
	 */
	default void removeSourceProperty (final String name) {
		getSourceProperties().remove(name);
	}

	/**
	 * Sets a source property. If a property already exists it is overwritten. 
	 * @param property The new property to set.
	 * @return The property that has been set.
	 */
	default Property setSourceProperty (final Property property) {
		getSourceProperties().put(property.getName(), property);
		return property;
	}
}
