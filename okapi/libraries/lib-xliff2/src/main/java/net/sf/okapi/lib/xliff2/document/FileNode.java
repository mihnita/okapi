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

import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Skeleton;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.EventType;
import net.sf.okapi.lib.xliff2.reader.URIContext;

/**
 * Represents a &lt;file&gt; node.
 */
public class FileNode extends WithGroupOrUnitNode implements IWithGroupOrUnitNode {

	private StartFileData startData;
	private Skeleton skelData;
	private MidFileData midData;

	/**
	 * Creates a new {@link FileNode} object with a given {@link StartFileData} resource.
	 * @param data the resource to attach to this node.
	 */
	public FileNode (StartFileData data) {
		super();
		this.startData = data;
	}
	
	/**
	 * Gets the {@link StartFileData} object for this file node.
	 * @return the {@link StartFileData} object for this file node.
	 */
	public StartFileData getStartData () {
		return startData;
	}
	
	/**
	 * Sets the {@link MidFileData} resource for this file node.
	 * @param data the {@link MidFileData} object for this file node.
	 */
	public void setMidData (MidFileData data) {
		this.midData = data;
	}
	
	/**
	 * Gets the {@link MidFileData} resource for this file node. 
	 * @return the {@link MidFileData} for this file node.
	 */
	public MidFileData getMidData () {
		return midData;
	}
	
	/**
	 * Sets the {@link Skeleton} resource for this file node.
	 * @param data the {@link Skeleton} object for this file node.
	 */
	public void setSkeletonData (Skeleton data) {
		this.skelData = data;
	}
	
	/**
	 * Gets the {@link Skeleton} for this file node.
	 * @return the {@link Skeleton} for this file node.
	 */
	public Skeleton getSkeletonData () {
		return skelData;
	}

	/**
	 * Create an iterator for the event for this file node.
	 * @param uriContext the URI context.
	 * @return a new iterator for the events in this file node.
	 */
	public Iterator<Event> createEventIterator (Stack<URIContext> uriContext) {
		EventIterator ei = new EventIterator () {

			private Iterator<IGroupOrUnitNode> iter = createGroupOrUnitIterator();
			private Iterator<Event> eventIter = null;
			private int state = 0; // start-file event
			
			@Override
			public boolean hasNext () {
				switch ( state ) {
				case 0: // start-file
					return true;
				case -1: // We are done
					return false;
				case 1: // Skeleton
					if ( skelData != null ) return true;
					// Else: try mid-file
					state = 2; // Fall thru
				case 2: // Mid-file
					if ( midData != null ) return true;
					// Else: move to entries
					state = 3;
					// And fall thru
				}
				
				if ( eventIter != null ) {
					if ( eventIter.hasNext() ) return true;
					// Else: This group is done
					eventIter = null;
					// Fall thru to next item in the file
				}
				if ( iter.hasNext() ) {
					return true;
				}
				// Else: no more entries in this file
				// last event is the end-file
				state = 4;
				return true;
			}
			
			@Override
			public Event next () {
				switch ( state ) {
				case 0: // start-file
					state = 1; // Next is skeleton
					uriContext.push(uriContext.peek().clone());
					uriContext.peek().setFileId(startData.getId());
					return new Event(EventType.START_FILE, uriContext.peek(), startData);
				case 1: // Skeleton
					state = 2; // next is mid-file
					return new Event(EventType.SKELETON, uriContext.peek(), skelData);
				case 2: // Mid-file
					state = 3; // Normal entries
					return new Event(EventType.MID_FILE, uriContext.peek(), midData);
				case 4: // end-file
					state = -1; // Nothing after that
					uriContext.pop();
					return new Event(EventType.END_FILE, null);
				}
				// Otherwise: state = 3: entries
				// Use the events iterator if available
				if ( eventIter != null ) {
					return eventIter.next();
				}
				// Otherwise, 
				IGroupOrUnitNode node = iter.next();
				if ( node.isUnit() ) {
					uriContext.push(uriContext.peek().clone());
					uriContext.peek().setUnitId(((UnitNode)node).get().getId());
					Event event = new Event(EventType.TEXT_UNIT, uriContext.peek(), ((UnitNode)node).get());
					uriContext.pop();
					return event;
				}
				// Else: it's a group node
				eventIter = ((GroupNode)node).createEventIterator(uriContext);
				eventIter.hasNext(); // Call once to prime the iterator
				return eventIter.next();
			}
			
		};
		ei.setURIContext(uriContext);
		return ei;
	}

}
