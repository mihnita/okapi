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

package net.sf.okapi.lib.xliff2.core;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Provides the common methods for accessing and manipulating list-type objects
 * such as the notes, the extension attributes, glossary, etc.
 * Classes used as the type for the {@link BaseList} must be supported in the {@link CloneFactory}. 
 * @param <T> the type of item for this list (e.g. Note, Match, etc.).
 */
public abstract class BaseList<T> implements Iterable<T> {

	private ArrayList<T> list = new ArrayList<>(2);

	/**
	 * Creates an empty {@link BaseList} object.
	 */
	protected BaseList () {
		// Do nothing
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate (can be null).
	 */
	@SuppressWarnings("unchecked")
	protected BaseList (BaseList<T> original) {
		if ( original == null ) return;
		for ( T object : original ) {
			add((T)CloneFactory.create(object));
		}
	}
	
	/**
	 * Gets the number of objects in this list.
	 * @return the number of objects in this list.
	 */
	public int size () {
		return list.size();
	}
	
	/**
	 * Indicates if this list is empty.
	 * @return true if there is no object in this list, false if there is one or more.
	 */
	public boolean isEmpty () {
		return (list.size()==0);
	}

	@Override
	public Iterator<T> iterator () {
		return list.iterator();
	}

	/**
	 * Removes all objects in this list.
	 */
	public void clear () {
		list.clear();
	}
	
	/**
	 * Adds an object to this list.
	 * @param object the object to add.
	 * @return the object that was added.
	 */
	public T add (T object) {
		list.add(object);
		return object;
	}
	
	/**
	 * Removes from this list the object at the given index position. 
	 * @param index the index position.
	 * @throws IndexOutOfBoundsException if the index is invalid.
	 */
	public void remove (int index) {
		list.remove(index);
	}

	/**
	 * Removes a given object from this list.
	 * If the object is not in the list nothing changes.
	 * @param object the object to remove.
	 */
	public void remove (T object) {
		list.remove(object);
	}

	/**
	 * Gets the object at a given index position.
	 * @param index the index position.
	 * @return the object at the given index.
	 * @throws IndexOutOfBoundsException if the index is invalid.
	 */
	public T get (int index) {
		return list.get(index);
	}

	/**
	 * Replaces an existing object by a new one at a given index position.
	 * @param index the index position.
	 * @param object the new object to set.
	 * @return the object that was set.
	 * @throws IndexOutOfBoundsException if the index is invalid.
	 */
	public T set (int index,
		T object)
	{
		list.set(index, object);
		return list.get(index);
	}

}
