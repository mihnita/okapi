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
import java.util.Set;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.annotation.Annotations;
import net.sf.okapi.common.annotation.IAnnotation;

public interface IWithAnnotations {
	/**
	 * Copy all {@link IWithAnnotations} <i>Unless destination already has the
	 * annotation.</i>
	 * <p>
	 * <i>WARNING: Annotations are not cloned before copying. {@link IAnnotation}
	 * should define a clone method</i>
	 * 
	 * @param from source of {@link IWithAnnotations}
	 * @param to   destination of {@link IWithAnnotations}
	 */
	static void copy(final IWithAnnotations from, final IWithAnnotations to) {
		if (from == null || to == null) {
			return;
		}

		for (final IAnnotation ann : from.getAnnotations()) {
			// never overwrite an annotation - it may be updated
			if (to.getAnnotation(ann.getClass()) == null) {
				// FIXME: IAnnotation should add clone method
				to.setAnnotation(ann);
			}
		}
	}

	/**
	 * @return {@link Annotations} for the implementer of interface
	 */
	Annotations getAnnotations();

	/**
	 * Gets the annotation object for a given class for this resource.
	 * 
	 * @param <A>
	 * @param annotation
	 * @return
	 */
	default <A extends IAnnotation> A getAnnotation(final Class<A> annotationType) {
		return annotationType.cast(getAnnotations().get(annotationType));
	}

	/**
	 * Sets an annotation object for this resource.
	 * @param annotation the annotation object to set.
	 */
	default void setAnnotation(final IAnnotation annotation) {
		getAnnotations().set(annotation);
	}

	/**
	 * Gets an iterator of the annotations for this resource.
	 * 
	 * @return an iterator list of the annotations for this resource.
	 */
	default Iterator<IAnnotation> annotationIterator() {
		return getAnnotations().iterator();
	}

	/**
	 * Gets all the types of annotations for this {@link IResource}.
	 * 
	 * @return a set of all the types of annotations for this code, or null if there
	 *         are no annotations.
	 */
	/**
	 * @return
	 */
	default Set<Class<? extends IAnnotation>> getAnnotationsTypesAsSet() {
		return getAnnotations().getAnnotationsTypes();
	}

	/**
	 * Remove the designated {@link IAnnotation}
	 * 
	 * @param <A>
	 * @param annotationType the class of the annotation object to retrieve.
	 */
	default <A extends IAnnotation> void remove(final Class<A> annotationType) {
		getAnnotations().remove(annotationType);
	}

	/**
	 * Does this resource have any annotations?
	 * 
	 * @return true if there are annotations on this {@link IResource}
	 */
	default boolean hasAnnotations() {
		return getAnnotations().isEmpty();
	}

	/**
	 * Does this resource have any annotations?
	 * 
	 * @param <A>
	 * @return true if there are annotations on this {@link IResource}
	 */
	default <A extends IAnnotation> boolean hasAnnotation(final Class<A> annotationType) {
		return getAnnotations().get(annotationType) != null;
	}

	/**
	 * Empty all {@link IAnnotation}'s
	 */
	default void clear() {
		getAnnotations().clear();
	}
}
