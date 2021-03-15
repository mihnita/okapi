/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.its;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.okapi.lib.xliff2.InvalidParameterException;

/**
 * Represents a collection of {@link IITSItem} objects.
 */
public class ITSItems implements Iterable<IITSItem> {

	private final ConcurrentHashMap<String, IITSItem> map = new ConcurrentHashMap<>();

	/**
	 * Creates an empty {@link ITSItems} object.
	 */
	public ITSItems () {
		// Argument-less constructor
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ITSItems (ITSItems original) {
		for ( IITSItem item : original ) {
			add(item.createCopy());
		}
	}
	
	/**
	 * Returns an iterator for the items in this collection.
	 * @return the iterator.
	 */
	@Override
	public Iterator<IITSItem> iterator () {
		return map.values().iterator();
	}

	/**
	 * Adds (or sets) a new ITS data category instance in this collection.
	 * <ul>
	 * <li>If the data category is always single instance (like MT Confidence) this method
	 * replaces any existing instance by the new one.</li>
	 * <li>If the data category allows for multiple instances (i.e. Provenance or Localization Quality Issue),
	 * this methods add the new instance. If an instance is already present, the existing instance is moved to a group
	 * and the new one is added to the group.</li>
	 * </ul>
	 * @param item the new item to set.
	 */
	public void add (IITSItem item) {
		if ( item instanceof LocQualityIssues ) {
			addSpecial((LocQualityIssues)item);
			return;
		}
		if ( item instanceof LocQualityIssue ) {
			addSpecial((LocQualityIssue)item);
			return;
		}
		if ( item instanceof Provenances ) {
			addSpecial((Provenances)item);
			return;
		}
		if ( item instanceof Provenance ) {
			addSpecial((Provenance)item);
			return;
		}
		// Else: it's a single instance case: just replace if needed
		map.put(item.getClass().getName(), item);
	}
	
	/**
	 * Indicates if there is at least one entry in this collection.
	 * @return true if there is at least one entry in this collection, false otherwise.
	 */
	public boolean isEmpty () {
		return map.isEmpty();
	}

	/**
	 * Removes all entries in this collection.
	 */
	public void clear () {
		map.clear();
	}
	
	/**
	 * Removes a given item from this collection.
	 * If the item is not found, nothing is done.
	 * For Provenance and Localization Quality Issue the item can be a group or an instance.
	 * @param item the item to remove.
	 */
	public void remove (IITSItem item) {
		IITSItem old = map.remove(item.getClass().getName());
		if ( old != null ) return; //Done
		// Else: For LocQualityIssue and Provenance the item may be a group
		DataCategoryGroup<?> group = null;
		if ( item instanceof LocQualityIssue ) {
			group = (DataCategoryGroup<?>)map.get(LocQualityIssues.class.getName());
		}
		if ( item instanceof Provenance ) {
			group = (DataCategoryGroup<?>)map.get(Provenances.class.getName());
		}
		if ( group != null ) {
			group.getList().remove(item);
		}
	}
	
	/**
	 * Gets the number of distinct data categories in this collection.
	 * Only one entry is counted for LocQualityIssue and Provenance, even if there are several.
	 * @return the number of distinct data categories in this collection.
	 */
	public int size () {
		return map.size();
	}

	/**
	 * Gets the item for a given data category name.
	 * This method calls {@link #get(Class)} after mapping the name to the class.
	 * @param dcName the name of the data category (as used in its:annotatorsRef).
	 * @return Same as for {@link #get(Class)}.
	 * @throws InvalidParameterException if the name is not valid.
	 */
	public IITSItem get (String dcName) {
		switch ( dcName ) {
		case DataCategories.LOCQUALITYISSUE:
			return get(LocQualityIssue.class);
		case DataCategories.PROVENANCE:
			return get(Provenance.class);
		case DataCategories.DOMAIN:
			return get(Domain.class);
		case DataCategories.TEXTANALYSIS:
			return get(LocQualityIssue.class);
		case DataCategories.MTCONFIDENCE:
			return get(MTConfidence.class);
		}
		throw new InvalidParameterException("Unexpected data category name "+dcName);
	}
	
	/**
	 * Gets the item for a given type of data category.
	 * @param <A> the type of data category.
	 * @param dcType the data category type.
	 * @return the item for the given type of data category, or null if there is none.
	 * The item returned is a group if there are several instances for the given data category
	 * (i.e. for multiple Provenance or Localization Quality Issue).
	 * That is: you may get a {@link Provenances} or a {@link LocQualityIssues} object when you query for
	 * the classes {@link Provenance} or {@link LocQualityIssue}.
	 */
	public <A extends DataCategory> IITSItem get (Class<A> dcType) {
		IITSItem item = map.get(dcType.getName());
		if ( item == null ) {
			// For LocQualityIssue and Provenance the item may be a group
			if ( dcType == LocQualityIssue.class ) {
				return map.get(LocQualityIssues.class.getName());
			}
			if ( dcType == Provenance.class ) {
				return map.get(Provenances.class.getName());
			}
		}
		return item;
	}

	private IITSItem addSpecial (LocQualityIssues newGroup) {
		// Do we have an existing single instance of this data category?
		LocQualityIssue old = (LocQualityIssue)map.get(LocQualityIssue.class.getName());
		if ( old != null ) {
			// There is already such an item: add it to the new group
			newGroup.getList().add(0, old); // Add at the front since it's older
			map.remove(LocQualityIssue.class.getName());
			map.put(LocQualityIssues.class.getName(), newGroup);
			return newGroup; // Done
		}
		// Else: No single item, but maybe there is already a group
		LocQualityIssues oldGroup = (LocQualityIssues)map.get(LocQualityIssues.class.getName());
		if ( oldGroup != null ) {
			// Add the new items after the existing ones
			oldGroup.getList().addAll(newGroup.getList());
			return oldGroup; // Done
		}
		// Neither a single instance nor a group exist, so we just set this group
		map.put(LocQualityIssues.class.getName(), newGroup);
		return newGroup;
	}
	
	private IITSItem addSpecial (Provenances newGroup) {
		// Do we have an existing single instance of this data category?
		Provenance old = (Provenance)map.get(Provenance.class.getName());
		if ( old != null ) {
			// There is already such an item: add it to the new group
			newGroup.getList().add(0, old); // Add at the front since it's older
			map.remove(Provenance.class.getName());
			map.put(Provenances.class.getName(), newGroup);
			return newGroup; // Done
		}
		// Else: No single item, but maybe there is already a group
		Provenances oldGroup = (Provenances)map.get(Provenances.class.getName());
		if ( oldGroup != null ) {
			// Add the new items after the existing ones
			oldGroup.getList().addAll(newGroup.getList());
			return oldGroup; // Done
		}
		// Neither a single instance nor a group exist, so we just set this group
		map.put(Provenances.class.getName(), newGroup);
		return newGroup;
	}
	
	private IITSItem addSpecial (LocQualityIssue dc) {
		LocQualityIssues group = null;
		// Do we have an existing single instance of this data category?
		IITSItem old = map.get(dc.getClass().getName());
		if ( old != null ) {
			// There is already such an item: create a group to hold both
			group = new LocQualityIssues(UUID.randomUUID().toString());
			group.getList().add((LocQualityIssue)old);
			map.remove(LocQualityIssue.class.getName()); // Remove the old instance
			map.put(LocQualityIssues.class.getName(), group); // Add the new group
		}
		else {
			// No single item, but maybe there is already a group
			IITSItem grpItem = map.get(LocQualityIssues.class.getName());
			if ( grpItem != null ) group = (LocQualityIssues)grpItem;
		}
		// Look if there an existing group (newly created or not)
		if ( group != null ) {
			// If there is a group add the new item to it
			group.getList().add(dc);
		}
		else {
			// If there is no group: set the new item as a single instance
			map.put(dc.getClass().getName(), dc);
		}
		// In all case return the new item
		return dc;
	}

	private IITSItem addSpecial (Provenance dc) {
		Provenances group = null;
		// Do we have an existing single instance of this data category?
		IITSItem old = map.get(dc.getClass().getName());
		if ( old != null ) {
			// There is already such an item: create a group to hold both
			group = new Provenances(UUID.randomUUID().toString());
			group.getList().add((Provenance)old);
			map.remove(Provenance.class.getName()); // Remove the old instance
			map.put(Provenance.class.getName(), group); // Add the new group
		}
		else {
			// No single item, but maybe there is already a group
			IITSItem grpItem = map.get(Provenances.class.getName());
			if ( grpItem != null ) group = (Provenances)grpItem;
		}
		// Look if there an existing group (newly created or not)
		if ( group != null ) {
			// If there is a group add the new item to it
			group.getList().add(dc);
		}
		else {
			// If there is no group: set the new item as a single instance
			map.put(dc.getClass().getName(), dc);
		}
		// In all case return the new item
		return dc;
	}

}

/*
public class ITSItems extends ConcurrentHashMap<Class<? extends ITSItem>, ITSItem> {

	private static final long serialVersionUID = 1L;

	@Override
	public ITSItem put (Class<? extends ITSItem> key, ITSItem value) {
		throw new UnsupportedOperationException("Put is not supported: Use add or set.");
	}
	
//	@Override
//	public ITSItem get (Object key) {
//		return null;
//	}
	
	public ITSItem get (Class<? extends ITSItem> key) {
		return null;
	}
	
	public ITSItem add (ITSItem<?> item) {
		if ( item.isGroup() ) {
			
		}
		else {
			DataCategory dc = item.getInstance();
			if ( dc instanceof LocQualityIssue ) {
				return addSpecial((LocQualityIssue)dc);
			}
			if ( dc instanceof Provenance ) {
				return addSpecial((Provenance)dc);
			}
			super.put(item.getClass(), item);
			return item;
		}
	}
	
	private ITSItem addSpecial (LocQualityIssue item) {
		// Do we have an existing single instance of this data category?
		LocQualityIssue existing = (LocQualityIssue)get(item.getClass());
		LocQualityIssues group;
		if ( existing != null ) {
			// There is already such an item: create a group to hold both
			group = new LocQualityIssues(UUID.randomUUID().toString());
			group.getList().add(existing);
		}
		else {
			// No single item, but maybe there is already a group
			group = (LocQualityIssues)get(LocQualityIssues.class);
		}
		// Look if there an existing group (newly created or not)
		if ( group != null ) {
			// If there is a group add the new item to it
			group.getList().add(item);
		}
		else {
			// If there is no group: set the new item as a single instance
			super.put(item.getClass(), item);
		}
		// In all case return the new item
		return item;
	}

	private ITSItem addSpecial (Provenance item) {
		// Do we have an existing single instance of this data category?
		Provenance existing = (Provenance)get(item.getClass());
		Provenances group;
		if ( existing != null ) {
			// There is already such an item: create a group to hold both
			group = new Provenances(UUID.randomUUID().toString());
			group.getList().add(existing);
		}
		else {
			// No single item, but maybe there is already a group
			group = (Provenances)get(Provenances.class);
		}
		// Look if there an existing group (newly created or not)
		if ( group != null ) {
			// If there is a group add the new item to it
			group.getList().add(item);
		}
		else {
			// If there is no group: set the new item as a single instance
			super.put(item.getClass(), item);
		}
		// In all case return the new item
		return item;
	}

}
*/
//	implements Iterable<ITSItem> {
//}
//
//	private ConcurrentHashMap<Class<? extends ITSItem>, ITSItem> map;
//
//	public boolean hasItem () {
//		if ( map == null ) return false;
//		return !map.isEmpty();
//	}
//
//	@Override
//	public Iterator<ITSItem> iterator () {
//		if ( map == null ) map = new ConcurrentHashMap<>();
//		return map.values().iterator();
//	}
//
//	public ITSItem add (ITSItem item) {
//		if ( map == null ) map = new ConcurrentHashMap<>();
//		if (( item instanceof LocQualityIssue ) || ( item instanceof Provenance )) {
//			// This item may have several instances
//			// Look if we have already a single instance
//			ITSItem old = map.get(item.getClass());
//			if ( old != null ) {
//				// Make it a group
//				
//				return;
//			}
//			else { // Look if we have several instances
//				if ( item instanceof LocQualityIssue ) {
//					LocQualityIssues group = (LocQualityIssues)map.get(LocQualityIssues.class);
//					if ( group != null ) {
//						group.add((LocQualityIssue)item);
//						return item;
//					}
//					// Else: no group: fall thru to add the item as a single instance
//				}
//				else { // Provenance case
//					Provenances group = (Provenances)map.get(Provenances.class);
//					if ( group != null ) {
//						group.add((Provenance)item);
//						return item;
//					}
//				}
//				if ( group != null ) {
//					// Add the new instance to the group
//					group.
//					return;
//				}
//			}
//			// Else: just add the single instance
//			// Fall thru
//		}
//
//		// Just set the new item
//		map.put(item.getClass(), item);
//		return item;
//	}
//	
//	public <A extends ITSItem> A get (Class<A> itemClass) {
//		if ( map == null ) return null;
//		return itemClass.cast(map.get(itemClass));
//	}
//
//	public <A extends ITSItem> A remove (Class<A> itemClass) {
//		if ( map == null ) return null;
//		return itemClass.cast(map.remove(itemClass));
//	}
//
//	public void clear () {
//		if ( map != null ) map.clear();
//	}
//
//}
