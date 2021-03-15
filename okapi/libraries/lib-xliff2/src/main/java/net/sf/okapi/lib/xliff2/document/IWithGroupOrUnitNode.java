/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.document;

import java.util.Iterator;
import java.util.List;

/**
 * Represents a node that holds groups or units (or both).
 */
public interface IWithGroupOrUnitNode {

	/**
	 * Adds a {@link UnitNode} to this object.
	 * @param node the unit node to add.
	 * @return the added unit node.
	 * @see #addUnitNode(String)
	 */
	UnitNode add(UnitNode node);
	
	/**
	 * Adds a {@link UnitNode} to this object.
	 * @param id the ID of the new unit.
	 * @return the added unit node.
	 */
	UnitNode addUnitNode(String id);

	/**
	 * Adds a {@link GroupNode} to this object.
	 * If the group ID is null, it is automatically set to a UUID.
	 * @param node the group node to add.
	 * @return the added group node.
	 * @see #addGroupNode(String)
	 */
	GroupNode add(GroupNode node);
	
	/**
	 * Adds a {@link GroupNode} to this object.
	 * @param id the ID of the group (can be null). If the ID is null it is automatically set to a UUID.
	 * @return the added group node.
	 */
	GroupNode addGroupNode(String id);

	/**
     * Gets all the {@link UnitNode} elements of this object.
     * All units at any level in this object are returned.
     * @return all the unit nodes for the object. The list will be empty (but never null), if no unit nodes were found.
     */
	List<UnitNode> getUnitNodes();

	/**
	 * Gets the {@link UnitNode} for a given unit id.
	 * The unit can be at any level in this object.
	 * @param id the id to look for.
	 * @return the unit node for the given id, or null if none is found.
	 */
	UnitNode getUnitNode(String id);
	
	/**
	 * Gets a {@link GroupNode} for a given id.
	 * The group can be at any level in this object.
	 * @param id the id to look for.
	 * @return the group node for the given id, or null if none is found.
	 */
	GroupNode getGroupNode(String id);

	/**
	 * Creates an iterator for the group nodes and unit nodes in this object.
	 * @return a new iterator for the nodes in this object.
	 */
	Iterator<IGroupOrUnitNode> createGroupOrUnitIterator();
	
}
