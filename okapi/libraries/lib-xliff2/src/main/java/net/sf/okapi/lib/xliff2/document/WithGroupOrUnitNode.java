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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.Unit;

public abstract class WithGroupOrUnitNode implements IWithGroupOrUnitNode {

	private LinkedHashMap<String, IGroupOrUnitNode> nodes;

	/**
	 * Creates a new {@link WithGroupOrUnitNode} object.
	 */
	public WithGroupOrUnitNode () {
		nodes = new LinkedHashMap<>();
	}

	@Override
	public UnitNode add (UnitNode node) {
		nodes.put("u"+node.get().getId(), node);
		return node;
	}
	
	@Override
	public UnitNode addUnitNode (String id) {
		return add(new UnitNode(new Unit(id)));
	}

	@Override
	public GroupNode add (GroupNode node) {
		String id = node.get().getId();
		if ( id == null ) id = UUID.randomUUID().toString();
		nodes.put("g"+id, node);
		return node;
	}
	
	@Override
	public GroupNode addGroupNode (String id) {
		return add(new GroupNode(null, new StartGroupData(id)));
	}

	@Override
    public List<UnitNode> getUnitNodes () {
		List<UnitNode> unitNodes = new ArrayList<>();
		for ( String nodeKey : this.nodes.keySet() ) {
			IGroupOrUnitNode node = this.nodes.get(nodeKey);
			if ( node.isUnit() )
				unitNodes.add((UnitNode)node);
			else {
				GroupNode groupNode = (GroupNode)node;
				unitNodes.addAll(groupNode.getUnitNodes());
			}
		}
		return unitNodes;
    }

	@Override
	public UnitNode getUnitNode (String id) {
		// Try at this level
		UnitNode item = (UnitNode)nodes.get("u"+id);
		if ( item != null ) return item;
		// Else: try recursively
		for ( IGroupOrUnitNode node : nodes.values() ) {
			if ( !node.isUnit() ) {
				item = ((GroupNode)node).getUnitNode(id);
				if ( item != null ) return item;
			}
		}
		// Not found
		return null;
	}
	
	@Override
	public GroupNode getGroupNode (String id) {
		// Try at this level
		GroupNode item = (GroupNode)nodes.get("g"+id);
		if ( item != null ) return item;
		// Else: try recursively
		for ( IGroupOrUnitNode node : nodes.values() ) {
			if ( !node.isUnit() ) {
				item = ((GroupNode)node).getGroupNode(id);
				if ( item != null ) return item;
			}
		}
		// Not found
		return null;
	}

	@Override
	public Iterator<IGroupOrUnitNode> createGroupOrUnitIterator () {
		return nodes.values().iterator();
	}

}
