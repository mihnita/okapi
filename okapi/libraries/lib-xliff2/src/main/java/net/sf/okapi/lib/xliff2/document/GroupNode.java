/*===========================================================================
  Copyright (C) 2013-2017 by the Okapi Framework contributors
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
import java.util.Stack;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.EventType;
import net.sf.okapi.lib.xliff2.reader.URIContext;

/**
 * Represents a group node.
 */
public class GroupNode extends WithGroupOrUnitNode implements IGroupOrUnitNode, IWithGroupOrUnitNode {

	private GroupNode parent;
	private StartGroupData data;

	/**
	 * Creates a new {@link GroupNode} object with a given {@link StartGroupData} resource and parent. 
	 * @param parent the parent of this new group node (use null for top-level groups).
	 * @param data the data for this new group node.
	 */
	public GroupNode (GroupNode parent,
		StartGroupData data)
	{
		super();
		if ( data == null ) {
			throw new InvalidParameterException("The data associated with the new group node must not be null.");
		}
		this.parent = parent;
		this.data = data;
	}
	
	/**
	 * Gets the {@link StartGroupData} resource for this group node.
	 * @return the resource for this group node.
	 */
	public StartGroupData get () {
		return data;
	}

	@Override
	public boolean isUnit () {
		return false;
	}

	/**
	 * Gets the parent for this group node.
	 * @return the parent for this group node, or null for a top-level group.
	 */
	public GroupNode getParent () {
		return parent;
	}
	
	/**
	 * Creates an iterator for the events in this group node.
	 * @param uriContext the URI context.
	 * @return a new iterator for the events in this group node.
	 */
	public Iterator<Event> createEventIterator (Stack<URIContext> uriContext) {
		EventIterator ei = new EventIterator () {
			
			private Iterator<IGroupOrUnitNode> topIter = createGroupOrUnitIterator();
			private Stack<Iterator<IGroupOrUnitNode>> groups = new Stack<>();
			private Iterator<IGroupOrUnitNode> groupIter = null;
			private int state = 0;
			
			@Override
			public boolean hasNext () {
				switch ( state ) {
				case 0:
					return true;
				case -1: // All done
					return false;
				}
				// State 1: normal items
				if ( groupIter != null ) {
					if ( groupIter.hasNext() ) return true;
					// Else: Done with this group: pop
					groups.pop();
					// Send an end-group event (for sub-group)
					state = 2;
					if ( groups.isEmpty() ) {
						groupIter = null;
					}
					else {
						groupIter = groups.peek();
					}
					return true;
				}
				// Else: top level items
				if ( topIter.hasNext() ) {
					return true;
				}
				// Done
				state = 3; // Send an end-group for this group
				return true;
			}

			@Override
			public Event next () {
				switch ( state ) {
				case 0: // Start of this group
					state = 1; // Next is normal items
					uriContext.push(uriContext.peek().clone());
					uriContext.peek().setGroupId(data.getId());
					return new Event(EventType.START_GROUP, uriContext.peek(), data);
				case 2: // End-group of sub-groups
					state = 1; // Next is normal items
					// The groupIter is already set to the previous group
					uriContext.pop();
					return new Event(EventType.END_GROUP, null);
				case 3: // End-group for this group
					state = -1;
					uriContext.pop();
					return new Event(EventType.END_GROUP, null);
				}
				
				// State 1: normal items
				if ( groupIter != null ) {
					IGroupOrUnitNode node = groupIter.next();
					if ( node.isUnit() ) {
						uriContext.push(uriContext.peek().clone());
						uriContext.peek().setUnitId(((UnitNode)node).get().getId());
						Event event = new Event(EventType.TEXT_UNIT, uriContext.peek(), ((UnitNode)node).get());
						uriContext.pop();
						return event;
					}
					// Else: it's a group node
					GroupNode gn = (GroupNode)node;
					groups.push(gn.createGroupOrUnitIterator());
					groupIter = groups.peek();
					uriContext.push(uriContext.peek().clone());
					uriContext.peek().setGroupId(gn.get().getId());
					// Send the start-group of the sub-group
					return new Event(EventType.START_GROUP, uriContext.peek(), gn.get());
				}
				
				// Else: top nodes
				IGroupOrUnitNode node = topIter.next();
				if ( node.isUnit() ) {
					uriContext.push(uriContext.peek().clone());
					uriContext.peek().setUnitId(((UnitNode)node).get().getId());
					Event event = new Event(EventType.TEXT_UNIT, uriContext.peek(), ((UnitNode)node).get());
					uriContext.pop();
					return event;
				}
				// Else: it's a group node
				GroupNode gn = (GroupNode)node;
				groups.push(gn.createGroupOrUnitIterator());
				groupIter = groups.peek();
				// Send the start-group of the sub-group
				uriContext.push(uriContext.peek().clone());
				uriContext.peek().setGroupId(gn.get().getId());
				return new Event(EventType.START_GROUP, uriContext.peek(), gn.get());
			}
		};
		ei.setURIContext(uriContext);
		return ei;
	}

}
