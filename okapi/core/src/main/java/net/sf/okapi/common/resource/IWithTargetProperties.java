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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.Annotations;

public interface IWithTargetProperties {
	/**
	 * Copy all {@link IWithTargetProperties} <i>Unless destination already has the
	 * property.</i> Target {@link LocaleId} not found in the destination are added
	 * then properties are copied. Properties are cloned before copying.
	 * 
	 * @param from source of {@link IWithTargetProperties}
	 * @param to   destination of {@link IWithTargetProperties}
	 */
	static void copy(final IWithTargetProperties from, final IWithTargetProperties to) {
		if (from == null || to == null) {
			return;
		}

		for (final LocaleId trgLocale : from.getTargetLocales()) {
			for (final String propName : from.getTargetPropertyNames(trgLocale)) {
				// never overwrite a property - it may be updated
				if (!to.hasTargetProperty(trgLocale, propName)) {
					to.setTargetProperty(trgLocale, from.getTargetProperty(trgLocale, propName).clone());
				}
			}
		}
	}

	/**
	 * @return {@link Map} of target properties for the implementer of interface 
	 */
	default Map<String, Property> getTargetProperties(final LocaleId locale) {
		final TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) return null;
		final Map<String, Property> trgProps = tpa.get(locale);
		return trgProps;
	}

	/**
	 * Create target {@link Property} without access to the source property
	 * @param locId
	 * @param name
	 * @param overwriteExisting
	 * @param creationOptions
	 * @return
	 */
	default Property createTargetProperty (final LocaleId locId,
			final String name,
			final boolean overwriteExisting,
			final int creationOptions) {
		return createTargetProperty(locId, name, null, overwriteExisting, creationOptions);
	}

	/**
	 * Creates or get a target property based on the corresponding source.
	 * @param locId The target locale to use.
	 * @param name The name of the property to create (or retrieve)
	 * @param sourceProperty The source property of the same name
	 * @param overwriteExisting True to overwrite any existing property.
	 * False to not create a new property if one exists already. 
	 * @param creationOptions Creation options:
	 * <ul><li>CREATE_EMPTY: Creates an empty property, only the read-only flag 
	 * of the source is copied.</li>
	 * <li>COPY_CONTENT: Creates a new property with all its data copied from 
	 * the source.</li></ul>
	 * @return The property that was created, or retrieved. 
	 */
	default Property createTargetProperty (final LocaleId locId,
			final String name,
			final Property sourceProperty,
			final boolean overwriteExisting,
			final int creationOptions)
	{
		TargetPropertiesAnnotation tpa = getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) {
			tpa = new TargetPropertiesAnnotation();
			getAnnotations().set(tpa);
		}
		Map<String, Property> trgProps = tpa.get(locId);
		if ( trgProps == null ) {
			tpa.set(locId, new LinkedHashMap<>());
			trgProps = tpa.get(locId);
		}
		Property trgProp = trgProps.get(name);
		if (( trgProp == null ) || overwriteExisting ) {
			if ( creationOptions > INameable.CREATE_EMPTY ) {
				trgProp = new Property(name, "", false);
			}
			else { // Copy the source
				final Property srcProp = sourceProperty; // use the source
				if ( srcProp == null ) { // No corresponding source
					trgProp = new Property(name, "", false);
				}
				else { // Has a corresponding source
					trgProp = srcProp.clone();
				}
			}
			trgProps.put(name, trgProp); // Add the property to the list
		}
		return trgProp;
	}

	/**
	 * @return {@link Annotations} for the implementer of interface 
	 */
	Annotations getAnnotations();

	/**
	 * Gets all the target locales for this resource.
	 * @return all the target locales for this resource.
	 */
	default Set<LocaleId> getTargetLocales () {
		TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) {
			tpa = new TargetPropertiesAnnotation();
			getAnnotations().set(tpa);
		}
		return tpa.getLocales();
	}

	/**
	 * Gets the target property for a given name and target locale.
	 * @param locId the locale of the property to retrieve.
	 * @param name The name of the property to retrieve. This name is case-sensitive.
	 * @return The property or null if it does not exist.
	 */
	default Property getTargetProperty (final LocaleId locId, final String name)
	{
		if ( getAnnotations() == null ) return null;
		final TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) return null;
		final Map<String, Property> trgProps = tpa.get(locId);
		if ( trgProps == null ) return null;
		return trgProps.get(name);
	}

	/**
	 * Gets the names of all the properties for a given target locale in this resource.
	 * @param locId the target locale to query.
	 * @return all the names of the target properties for the given locale in this resource.
	 */
	default Set<String> getTargetPropertyNames (final LocaleId locId) {
		TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) {
			tpa = new TargetPropertiesAnnotation();
			getAnnotations().set(tpa);
		}
		Map<String, Property> trgProps = tpa.get(locId);
		if ( trgProps == null ) {
			tpa.set(locId, new LinkedHashMap<>());
			trgProps = tpa.get(locId);
		}
		return trgProps.keySet();
	}

	/**
	 * Indicates if a property exists for a given name and target locale.
	 * @param locId the target locale to query.
	 * @param name the name of the property to query.
	 * @return true if a property exists, false otherwise.
	 */
	default boolean hasTargetProperty (final LocaleId locId,
			final String name)
	{
		if ( getAnnotations().isEmpty() ) return false;
		final TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) return false;
		final Map<String, Property> trgProps = tpa.get(locId);
		if ( trgProps == null ) return false;
		return (trgProps.get(name) != null);
	}

	/**
	 * Removes a target property of a given name. If the property does not exists
	 * nothing happens.
	 * @param locId The target locale for which this property should be set.
	 * @param name The name of the property to remove.
	 */
	default void removeTargetProperty (final LocaleId locId,
			final String name)
	{
		if ( !getAnnotations().isEmpty() ) {
			final TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
			if ( tpa != null ) {
				final Map<String, Property> trgProps = tpa.get(locId);
				trgProps.remove(name);
			}
		}
	}

	/**
	 * Sets a target property. If a property already exists it is overwritten.
	 * @param locId The target locale for which this property should be set.
	 * @param property The new property to set. This name is case-sensitive.
	 * @return The property that has been set.
	 */
	default Property setTargetProperty (final LocaleId locId, final Property property)
	{
		TargetPropertiesAnnotation tpa =  getAnnotations().get(TargetPropertiesAnnotation.class);
		if ( tpa == null ) {
			tpa = new TargetPropertiesAnnotation();
			getAnnotations().set(tpa);
		}
		Map<String, Property> trgProps = tpa.get(locId);
		if ( trgProps == null ) {
			tpa.set(locId, new LinkedHashMap<>());
			trgProps = tpa.get(locId);
		}
		trgProps.put(property.getName(), property);
		return property;
	}
}
