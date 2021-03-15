/*===========================================================================
  Copyright (C) 2009-2019 by the Okapi Framework contributors
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

/**
 * Base implementation of an annotation that can be used on inline codes.
 * Inline annotations must have a {@link #toString()} and {@link #fromString(String)} 
 * methods to write and read themselves to and from a string.
 * <p>This basic annotation has only a string data. Its usage depends of the type 
 * of the annotation.
 */
public class InlineAnnotation {
	public static final String CLASSNAME_SEPARATOR = "\u009A";
	protected String data;

	/**
	 * Creates a new InlineAnnotation object from a storage string.
	 * @param storage the serialized representation of the object.
	 * @return a new InlineAnnotation object.
	 */
	static public InlineAnnotation createFromString (String storage) {
		InlineAnnotation annotation;
		if ( storage == null ) {
			return new InlineAnnotation(); // With null data
		}
		// Create a non-base class if there is a class name in the storage
		// string (e.g. GenericAnnotations)
		if ( storage.startsWith(CLASSNAME_SEPARATOR) ) {
			// Get the end of the class name and get the class name
			int p = storage.indexOf(CLASSNAME_SEPARATOR, CLASSNAME_SEPARATOR.length());
			String className = storage.substring(CLASSNAME_SEPARATOR.length(), p);
			try {
				// Instantiate an object for the given class
				Object obj = Class.forName(className).newInstance();
				// That object must be derived from InlineAnnotation, it is a requirement
				annotation = (InlineAnnotation)obj;
			}
			catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		else { // Just a simple InlineAnnotation object
			annotation = new InlineAnnotation();
		}
		annotation.fromString(storage);
		return annotation;
	}

	/**
	 * Creates an empty annotation object.
	 */
	public InlineAnnotation () {
	}
	
	/**
	 * Creates a new annotation object with some initial data.
	 * @param data The data to set.
	 */
	public InlineAnnotation (String data) {
		this.data = data;
	}
	
	/**
	 * Clones this annotation.
	 * @return A new InlineAnnotation object that is a copy of this one.
	 */
	@Override
	public InlineAnnotation clone () {
		return new InlineAnnotation(this.data);
	}
	
	/**
	 * Gets a storage string representation of the whole annotation that can
	 * be used for serialization.
	 * @return The storage string representation of this annotation.
	 */
	@Override
	public String toString () {
		// this annotation has just one string.
		return data;
	}
	
	/**
	 * Initializes this annotation from a storage string originally obtained
	 * from {@link #toString()}.
	 * @param storage The storage string to use for the initialization.
	 */
	public void fromString (String storage) {
		// This annotation has just one string.
		this.data = storage;
	}

	/**
	 * Gets the data for this annotation.
	 * @return The data of this annotation.
	 */
	public String getData () {
		return data;
	}
	
	/**
	 * Sets the data for this annotation.
	 * @param data The data to set.
	 */
	public void setData (String data) {
		this.data = data;
	}
}
