/*===========================================================================
  Copyright (C) 2011-2016 by the Okapi Framework contributors
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.glossary.Glossary;
import net.sf.okapi.lib.xliff2.its.DataCategoryGroup;
import net.sf.okapi.lib.xliff2.its.ITSItems;
import net.sf.okapi.lib.xliff2.its.IWithITSAttributes;
import net.sf.okapi.lib.xliff2.its.IWithITSGroups;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.matches.Matches;

/**
 * Represents the XLIFF <code>&lt;unit&gt;</code> element.
 * A unit is made of a list of {@link Part} objects, some of which are {@link Segment} objects.
 */
public class Unit extends CompleteData implements Iterable<Part>, IWithStore, IWithITSAttributes, IWithITSGroups {
	
	private final Store store = new Store(this);
	
	private ArrayList<Part> parts;
	private List<DataCategoryGroup<?>> itsList;
	private ITSItems itsItems;
	private Matches matches;
	private Glossary glossary;

	/**
	 * Internal class for the context to process protected content
	 */
	private static class TransInfo {
		
		public String id;
		public boolean trans;
		
		/**
		 * Creates a new TransInfo object.
		 * @param id the id of the tag (can be empty, not null).
		 * This is used to find the opening item when finding a closing tag.
		 * @param trans true for to-translate, false for not-to-translate.
		 */
		public TransInfo (String id,
			boolean trans)
		{
			this.id = id;
			this.trans = trans;
		}
	}

	/**
	 * Copy constructor.
	 * <p>Important: Most of the time you MUST change the id of the resulting unit as a unit id must be unique
	 * within its parent file element.
	 * @param original the original unit to duplicate.
	 */
	public Unit (Unit original) {
		// Create the new object from the base class copy constructor
		super(original);
		// Copy the Unit-specific fields
		parts = new ArrayList<>(original.getPartCount());
		for ( Part part : original ) {
			parts.add(CloneFactory.create(part));
		}
		if ( original.hasITSGroup() ) {
			for ( DataCategoryGroup<?> group : original.getITSGroups() ) {
				addITSGroup((DataCategoryGroup<?>)group.createCopy());
			}
		}
		if ( original.hasITSItem() ) {
			itsItems = new ITSItems(original.itsItems);
		}
		if ( original.hasMatch() ) {
			matches = new Matches(original.matches);
		}
		if ( original.hasGlossEntry() ) {
			glossary = new Glossary(original.glossary);
		}
	}

	/**
	 * Creates a new {@link Unit} object.
	 * @param id the id of the unit.
	 */
	public Unit (String id) {
		if ( Util.isNoE(id) ) {
			throw new InvalidParameterException("Id cannot be null or empty.");
		}
		setId(id);
		parts = new ArrayList<>(1);
	}
	
	/**
	 * Creates a new {@link Unit} object with a given id and file context.
	 * @param id the id of the unit.
	 * @param startFileData the file context (can be null).
	 */
	public Unit (String id,
		StartFileData startFileData)
	{
		this(id);
		if ( startFileData != null ) {
			setSourceDir(startFileData.getSourceDir());
			setTargetDir(startFileData.getTargetDir());
		}
	}
	
	/**
	 * Creates a new iterator to loop through the segments and ignorables of this unit.
	 * @return a new iterator to loop through the segments and ignorables of this unit.
	 */
	@Override
    public Iterator<Part> iterator () {
		return parts.iterator();
	}

	/**
	 * Gets the number of parts in this unit.
	 * @return the number of parts in this unit.
	 */
	public int getPartCount () {
		return parts.size();
	}

	/**
	 * Gets the number of segments in this unit.
	 * @return the number of segments in this unit.
	 */
	public int getSegmentCount () {
		int count = 0;
		for ( Part part : parts ) {
			if ( part.isSegment() ) count++;
		}
		return count;
	}

	/**
	 * Appends a new empty segment to this unit.
	 * @return the new segment created.
	 */
	public Segment appendSegment () {
		Segment seg = new Segment(store);
		parts.add(seg);
		return seg;
	}
	
	/**
	 * Appends an empty ignorable part to this unit. 
	 * @return the new ignorable part created.
	 */
	public Part appendIgnorable () {
		Part part = new Part(store); 
		parts.add(part);
		return part;
	}

	/**
	 * Gets the part at a given index.
	 * @param partIndex the index of the part to retrieve (between 0 and {@link #getPartCount()}-1).
	 * @return the {@link Part} object at the given index position, the part may or may not be a {@link Segment}.
	 * @throws IndexOutOfBoundsException if the index is invalid.
	 */
	public Part getPart (int partIndex) {
		return parts.get(partIndex);
	}

	/**
	 * Gets the segment at a given index.
	 * Note that the retrieval of the object is not direct, but rely on looping through the parts.
	 * @param segIndex the index of the segment to retrieve (between 0 and {@link #getSegmentCount()}-1).
	 * @return the {@link Segment} object at the given index position.
	 * @throws IndexOutOfBoundsException if the index is invalid.
	 */
	public Segment getSegment (int segIndex) {
		int si = 0;
		for (Part part : parts) {
			if (part.isSegment()) {
				if (si == segIndex) {
					return (Segment) part;
				}
				si++;
			}
		}
		throw new IndexOutOfBoundsException(
			String.format("The index %d is out-of-bound for segments.", segIndex));
	}
	
	/**
	 * Gets the store for this unit.
	 * @return the {@link Store} object for this unit.
	 */
	public Store getStore () {
		return store;
	}

	@Override
	public boolean isIdUsed (String id) {
		return (getObjectFromId(id) != null);
	}
	
	/**
	 * Gets the object associated with a given span-class id in this unit.
	 * <p>The objects checked are: the parts (including segments) and all the 
	 * tags except the {@link PCont} objects.
	 * @param id the id to look for.
	 * @return the object found, or null if not found.
	 */
	public Object getObjectFromId (String id) {
		// Check the part
		for ( Part part : parts ) {
			// The part's id can be null: equals should support that
			if ( id.equals(part.getId()) ) return part;
		}
		// Check the tags
		return store.getTag(id);
	}

	/**
	 * Splits a segment.
	 * @param partIndex the part index of the segment to split.
	 * @param srcStart the start position of the middle new segment for the source (inclusive, in coded text).
	 * @param srcEnd the end position of the middle new segment for the source (exclusive, in coded text),
	 * use -1 for the end of the current segment.
	 * @param trgStart the start position of the middle new segment for the target (inclusive, in coded text).
	 * @param trgEnd the end position of the middle new segment for the target (exclusive, in coded text),
	 * use -1 for the end of the current segment.
	 * @param changeState true to change the state and possibly the subState attributes for the modified or added
	 * segments if the initial segment as a target and its state is other than "initial" and "translated".
	 * Use false to keep the same state and subState. 
	 */
	public void split (int partIndex,
		int srcStart,
		int srcEnd,
		int trgStart,
		int trgEnd,
		boolean changeState)
	{
//		//--- Debug trace
//		System.out.println("before:");
//		List<Part> list = getTargetOrderedParts();
//		for ( Part tmp : list ) {
//			System.out.print("{"+tmp.getTargetOrSource().toXLIFF(null)+"}");
//		}
//		System.out.println("");
//		//--- End debug trace

		Part part = getPart(partIndex);
		if ( !part.isSegment() ) {
			throw new InvalidParameterException("Cannot split a non-segment part.");
		}
		Segment oriSeg = (Segment)part;
		Fragment src = part.getSource();

		String srcCt = src.getCodedText();
		if ( srcEnd == -1 ) srcEnd = srcCt.length();

		// Do various checks
		if ( srcStart > srcEnd ) {
			throw new InvalidParameterException("Invalid source range.");
		}
		if (( srcStart < 0 ) || ( srcCt.length() < srcEnd )) {
			throw new InvalidParameterException("Source range out of bounds.");
		}
//		if ( Fragment.isMarker(ctext.codePointAt(srcStart)) || Fragment.isMarker(ctext.codePointAt(srcEnd)) ) {
//			throw new InvalidParameterException("You cannot split inside a inline marker.");
//		}

		String trgCt = null;
		boolean hasTarget = oriSeg.hasTarget();
		if ( hasTarget ) {
			trgCt = oriSeg.getTarget().getCodedText();
			if ( trgEnd == -1 ) trgEnd = trgCt.length();
			if ( trgStart > trgEnd ) {
				throw new InvalidParameterException("Invalid target range.");
			}
			if (( trgStart < 0 ) || ( trgCt.length() < trgEnd )) {
				throw new InvalidParameterException("Target range out of bounds.");
			}
		}
		
		String srcMid = srcCt.substring(srcStart, srcEnd);
		boolean srcToDo = true;
		if ( srcMid.isEmpty() && (( srcStart == 0 ) || ( srcStart >= srcCt.length() ))) {
			// Middle new part is empty and at of of the ends: There is nothing to split
			srcToDo = false;
		}
		
		String trgMid = "";
		if ( hasTarget ) {
			trgMid = trgCt.substring(trgStart, trgEnd);
			if ( trgMid.isEmpty() && (( trgStart == 0 ) || ( trgStart >= trgCt.length() ))) {
				// Nothing to split for the target
				// If there is nothing to split for the source either we stop here
				if ( !srcToDo ) return;
			}
		}
		
		String srcLeft = srcCt.substring(0, srcStart);
		String srcRight = srcCt.substring(srcEnd); 
		
		String trgLeft = "";
		String trgRight = "";
		if ( hasTarget ) {
			trgLeft = trgCt.substring(0, trgStart);
			trgRight = trgCt.substring(trgEnd);
		}
		
		// Fill empty content by non-empty ones, shifting content to the left
		// (so we start at the right-most part)
		if ( srcMid.isEmpty() ) {
			srcMid = srcRight; srcRight = "";
		}
		if ( srcLeft.isEmpty() ) {
			srcLeft = srcMid; srcMid = "";
		}
		if ( trgMid.isEmpty() ) {
			trgMid = trgRight; trgRight = "";
		}
		if ( trgLeft.isEmpty() ) {
			trgLeft = trgMid; trgMid = "";
		}

		// Re-use the original segment first
		if ( !srcLeft.isEmpty() || !trgLeft.isEmpty() ) {
			part.getSource().setCodedText(srcLeft);
			if ( hasTarget ) part.getTarget().setCodedText(trgLeft);
		}
		int added = 0;
		// Add a first segment if needed (it'll be the new right or middle)
		if ( !srcMid.isEmpty() || !trgMid.isEmpty() ) {
			Segment seg = oriSeg.createAndCopyMetadata();
			seg.getSource().setCodedText(srcMid);
			if ( hasTarget ) seg.getTarget().setCodedText(trgMid);
			parts.add(partIndex+(++added), seg);
		}
		// Add a last segment if needed (it'll be the new right)
		if ( !srcRight.isEmpty() || !trgRight.isEmpty() ) {
			Segment seg = oriSeg.createAndCopyMetadata();
			seg.getSource().setCodedText(srcRight);
			if ( hasTarget ) seg.getTarget().setCodedText(trgRight);
			parts.add(partIndex+(++added), seg);
		}
		
		// If we have added part: we may need to adjust the target orders
		if ( added > 0 ) {
			if ( hasTargetOrder() ) {
				int oriOrder = parts.get(partIndex).getTargetOrder();
				int resolvedOriOrder = (oriOrder > 0 ) ? oriOrder : partIndex+1;
				for ( int i=0; i<parts.size(); i++ ) {
					if ( i == partIndex+1 ) {
						// The first added part is always the order of the original + 1
						parts.get(i).setTargetOrder(resolvedOriOrder+1);
					}
					else if (( added == 2 ) && ( i == partIndex+2 )) {
						// the second added part is always the order of the original + 2
						parts.get(i).setTargetOrder(resolvedOriOrder+2);
					}
					else {
						int order = parts.get(i).getTargetOrder();
						// Calculate the old order
						int oldResolvedOrder;
						if ( order > 0 ) oldResolvedOrder = order;
						else {
							// If it was using the default part position we need to adjust
							// when that part is after the added part(s)
							if ( i <= partIndex ) oldResolvedOrder = i+1;
							else oldResolvedOrder = (i-added)+1;
						}
						// Calculate the new order: the same as before, except when
						// it's after the position of the original part
						int newOrder = oldResolvedOrder;
						if ( oldResolvedOrder > resolvedOriOrder ) {
							newOrder = oldResolvedOrder+added;
						}
						// Now we set the new value or use the default
						if ( i+1 == newOrder ) parts.get(i).setTargetOrder(0); // Same as default
						else parts.get(i).setTargetOrder(newOrder);
					}
				}
			}
			
			if ( changeState && hasTarget ) {
				// Update the state and possibly the subState if needed
				for ( int i=0; i<=added; i++ ) {
					part = parts.get(partIndex+i);
					if ( part.isSegment() ) {
						Segment seg = (Segment)part;
						switch ( seg.getState() ) {
						case INITIAL:
						case TRANSLATED:
							// No change
							break;
						default: // Update the state and subState
							seg.setState(TargetState.TRANSLATED);
							seg.setSubState(null);
							break;
						}
					}
				}
				
			}
			
//			//--- Debug trace
//			System.out.println("after-fix:");
//			list = getTargetOrderedParts();
//			for ( Part tmp : list ) {
//				System.out.print("{"+tmp.getTargetOrSource().toXLIFF(null)+"}");
//			}
//			System.out.println("");
//			//--- End debug trace
		}

	}

	/**
	 * Indicates if this unit has at least one target part not in the same order as the source.
	 * @return true if there is a target-specific order for this unit, false otherwise
	 */
	public boolean hasTargetOrder () {
		for (Part part : parts) {
			if (part.getTargetOrder() > 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Joins two or more parts together into the first one.
	 * @param startPartIndex the index of the first part to join (in the target order)
	 * @param endPartIndex the index of the last part to join (in the target order)
	 * @param restrictedJoin true to throw an exception if one of the segment cannot be merged,
	 * false to allow to merge regardless of the canResegment values (merger mode)
	 * @param adjustTargetIgnorable TODO
	 */
	public void join (int startPartIndex,
		int endPartIndex,
		boolean restrictedJoin,
		boolean adjustTargetIgnorable)
	{
		// check start index
		if (( startPartIndex < 0 ) || ( startPartIndex >= getPartCount() )) {
			throw new InvalidParameterException("Invalid startPartIndex value.");
		}
		// Auto-correct -1 for the end index if needed (same as last part)
		if ( endPartIndex == -1 ) endPartIndex = getPartCount()-1;
		// Same index?
		if ( endPartIndex == startPartIndex ) return;
		// Check the end index
		if (( endPartIndex <= startPartIndex ) || ( endPartIndex >= getPartCount() )) {
			throw new InvalidParameterException("Invalid endPartIndex value.");
		}
		
		// Get the target order
		List<Part> list = getTargetOrderedParts();
		
		// Get the objects where to append (first part)
		Part startPart = list.get(startPartIndex);
		int i;
		
		// Check for segments that cannot be re-segmented
		if ( restrictedJoin  ) {
			if ( startPart.isSegment() && !((Segment)startPart).getCanResegment() ) {
				throw new InvalidParameterException("The first segment cannot be re-segmented.");
			}
			i = startPartIndex+1;
			do {
				Part part = list.get(i);
				if ( part.isSegment() && !((Segment)part).getCanResegment() ) {
					throw new InvalidParameterException("One of more of the segments cannot be re-segmented.");
				}
				i++;
			}
			while ( i <= endPartIndex ); 
		}
		
		Fragment startSource = startPart.getSource();
		Fragment startTarget = startPart.getTarget(); // Get null if no target

		// Append the parts to the first part
		i = startPartIndex+1;
		do {
			Part part = list.get(i);
			Fragment frag = part.getSource();
			startSource.append(frag);
			
			if ( part.hasTarget() ) {
				frag = part.getTarget();
				if ( startTarget != null ) {
					startTarget.append(frag);
				}
				else {
					startPart.setTarget(frag);
				}
			}
			else if ( !part.isSegment() ) {
				if ( adjustTargetIgnorable ) {
					// If this is a non-existing target ignorable: use the source fragment
//TODO: this is not working yet: need a copy of the fragment because of the source/target stores					
//					if ( startTarget != null ) {
//						startTarget.append(checkBidi(frag, startTarget.getDir(true)));
//					}
//					else {
//						startPart.setTarget(frag.);
//					}
				}
				// Else: do not do anything with non-existing target segments
			}
			
			// Make sure the joined part is xml:space='preserve'
			if ( startPart.getPreserveWS() != part.getPreserveWS() ) {
				startPart.setPreserveWS(true);
			}
			
			// Use the "earliest" state and subState
			if ( startPart.isSegment() && part.isSegment() ) {
				Segment startSeg = (Segment)startPart;
				Segment seg = (Segment)part;
				if ( startSeg.getState().compareTo(seg.getState()) > 0 ) {
					startSeg.setState(seg.getState());
					startSeg.setSubState(seg.getSubState());
				}
			}

			i++; // Next part to join
		}
		while ( i <= endPartIndex );
		
		// Remove the collapsed parts
		i = startPartIndex+1;
		do {
			// Keep removing the part just after the start
			Part part = list.get(startPartIndex+1);
			list.remove(startPartIndex+1);
			parts.remove(part);
			i++;
		}
		while ( i <= endPartIndex );
		
		// Correct the target order values if needed
		if ( hasTargetOrder() ) {
			int removedCount = endPartIndex-startPartIndex;
			int srcOrder = 1;
			for ( Part part : parts ) {
				int order = part.getTargetOrder();
//			if ( order > 0 ) {
//				part.setTargetOrder((order-removedCount == srcOrder)
//					? 0 : order-removedCount);
//			}
				if ( order == 0 ) order = srcOrder;
				if ( order > startPartIndex+1 ) {
					part.setTargetOrder((order-removedCount == srcOrder)
						? 0 : order-removedCount);
				}
				srcOrder++;
			}
		}
		
		//TODO make sure we have at least one segment left in unit
	}
	
	public void joinAll (boolean adjustTargetIgnorable) {
		int start = 0;
		List<Part> list = getTargetOrderedParts();
		// Go through the list of ordered parts
		while ( true ) {
			// Get the next start
			while ( start < list.size() ) {
				Part part = list.get(start);
				if ( part.isSegment() ) {
					if ( ((Segment)part).getCanResegment() ) {
						// Found the first segment
						break;
					}
				}
				// Skip over ignorable elements and non-reorderable segments.
				start++;
			}
			// Get out now if the start is the last part
			if ( start >= list.size() ) return;
			
			// Else, get the end 
			int end;
			for ( end=start+1; end<list.size(); end++ ) {
				Part part = list.get(end);
				if ( part.isSegment() ) {
					if ( !((Segment)part).getCanResegment() ) {
						// End before segment that cannot be re-segmented
						break;
					}
				}
			}
			// Case of start==end is handled by the join() method.
			join(start, end-1, true, adjustTargetIgnorable);
			// Get the new list
			list = getTargetOrderedParts();
			start++;
		}
	}
	
	/**
	 * Creates an {@link Iterable} object for the segments in this unit.
	 * <p>Use {@link #getTargetOrderedParts()} to get a list of the parts in target order.
	 * @return a new {@link Iterable} object for the segments in this unit.
	 */
	public Iterable<Segment> getSegments () {
		return () -> new Iterator<Segment>() {
			int current = 0;

			@Override
			public void remove () {
				throw new UnsupportedOperationException("The method remove() not supported.");
			}

			@Override
			public Segment next () {
				while ( current < parts.size() ) {
					Part part = parts.get((++current)-1);
					if ( part.isSegment() ) return (Segment)part;
				}
				return null;
			}

			@Override
			public boolean hasNext () {
				int tmp = current;
				while ( tmp < parts.size() ) {
					if ( parts.get(tmp).isSegment() ) return true;
					tmp++;
				}
				return false;
			}
		};
	}
	
	/**
	 * Gets the list of the parts for this unit in the order specified for the target content.
	 * @return a list of all the parts, in the target order.
	 */
	public List<Part> getTargetOrderedParts () {
		ArrayList<Part> list = new ArrayList<>(parts);
		int index = 1; // Order values are 1-based, real index is 0-based
		for ( Part part : parts ) {
			int order = part.getTargetOrder();
			if ( order == 0 ) order = index; // Default
			list.set(order-1, part);
			index++;
		}
		return list;
	}

	/**
	 * Gets the plain text version of the full content of this unit, 
	 * for either the source or the target.
	 * @param target true to generate the text for the target, false for the source.
	 * @param useSourceForMissingTargetIgnorables true to use the source content when generating the target
	 * text and a target ignorable is missing. this parameter is ignored when generating the source text.
	 * @return the plain text full content requested.
	 */
	public String getPlainText (boolean target,
		boolean useSourceForMissingTargetIgnorables)
	{
		List<Part> tmpParts = ( target ? getTargetOrderedParts() : parts);
		StringBuilder tmp = new StringBuilder();
		for ( Part part : tmpParts ) {
			if ( target ) {
				if ( part.hasTarget() ) {
					tmp.append(part.getTarget().getPlainText());
				}
				else if ( useSourceForMissingTargetIgnorables ) {
					// Fall back on the source
					tmp.append(part.getSource().getPlainText());
				}
			}
			else {
				tmp.append(part.getSource().getPlainText());
			}
		}
		return tmp.toString();
	}
	
	/**
	 * Hides all protected spans of this unit into {@link PCont} objects.
	 * No target parts are created.
	 * @see #showProtectedContent()
	 */
	public void hideProtectedContent () {
		// Do the source
		hideProtectedContent(true);
		// Do the target
		hideProtectedContent(false);
	}

	private void hideProtectedContent (boolean doSource) {
		List<Part> list = (doSource ? parts : getTargetOrderedParts());
		Stack<TransInfo> stack = new Stack<>();
		stack.push(new TransInfo("", getTranslate()));
		for ( Part part : list ) {
			if ( doSource ) hideProtectedContent(part.getSource(), stack);
			else if ( part.hasTarget() ) {
				hideProtectedContent(part.getTarget(), stack);
			}
		}
	}
	
	private void hideProtectedContent (Fragment fragment,
		Stack<TransInfo> stack)
	{
		int start = 0;
		int pos = 0;
		int offset = 0;
		String ct = fragment.getCodedText();
		Tags tags = fragment.getTags();
		StringBuilder tmp = new StringBuilder(ct);
		
		// Process the annotations
		for ( ; pos<ct.length(); pos++ ) {
			if ( Fragment.isChar1(ct.charAt(pos)) ) {
				if ( ct.charAt(pos) == Fragment.PCONT_STANDALONE ) {
					continue; // Do not fold data already folded
				}
				Tag tag = tags.get(ct, pos);
				if ( tag.isMarker() ) {
					boolean prevTrans = stack.peek().trans;
					boolean isOpening = (tag.getTagType() == TagType.OPENING);
					if ( isOpening ) {
						Boolean trans = ((MTag)tag).getTranslate();
						if ( trans == null ) { // Inherit from parent
							stack.push(new TransInfo(tag.getId(), prevTrans));
						}
						else { // Set the new context
							stack.push(new TransInfo(tag.getId(), trans));
						}
					}
					else { // Closing
						// Find the opening in the stack 
						for ( int j=0; j<stack.size(); j++ ) {
							if ( stack.get(j).id.equals(tag.getId()) ) {
								stack.remove(j);
								break;
							}
						}
						// Corresponding opening not found: should not occur
					}
					// Did we change state?
					if ( prevTrans != stack.peek().trans ) {
						// New state
						if ( stack.peek().trans ) { // We were in protected mode before
							if ( !isOpening || ( start < pos )) {
								int last = pos + (isOpening ? 0 : 2);
								// Create the tag and replace the content by its reference
								PCont pm = new PCont(ct.substring(start, last));
								int key = tags.add(pm);
								tmp.delete(start+offset, last+offset);
								tmp.insert(start+offset, Fragment.toRef(key));
								// Compute the offset
								offset -= ((last-start)-2);
							}
							// Set the start for next span
							start = pos;
						}
						else { // We were in translate mode before
							if ( isOpening ) start = pos; // Start tag for protection is included in span
							else start = pos+2; // End tag for non-protected is not in span
						}
					}
				}
				// Skip tag type, now we point to the index
				pos++;
			}				
		} // End of processing the annotation for a given part
		// Look at the text since the last start change
		if ( !stack.peek().trans ) { // Are we in protected mode
			if ( start < pos ) { // If so do we have anything to extract
				// Create the tag and replace the content by its reference
				PCont pm = new PCont(ct.substring(start, pos));
				int key = tags.add(pm);
				tmp.delete(start+offset, pos+offset);
				tmp.insert(start+offset, Fragment.toRef(key));
			}
			// Else: nothing to do
		}
		fragment.setCodedText(tmp.toString()); 
	}
	
	/**
	 * Creates a list of booleans corresponding to the translate state at the end of each part in this unit.
	 * @param doSource true to generates the list from the source viewpoint, false to do it for the target.
	 * @return a list of booleans where true means the state of translate at the end of the corresponding part is 'yes',
	 * and false means it is 'no'.
	 */
	public List<Boolean> getTranslateStateEndings (boolean doSource) {
		List<Boolean> endings = new ArrayList<>();
		List<Part> list = (doSource ? parts : getTargetOrderedParts());
		Stack<TransInfo> stack = new Stack<>();
		stack.push(new TransInfo("", getTranslate()));
		for ( Part part : list ) {
			if ( doSource ) computeTranslateStateEnding(part.getSource(), stack);
			else computeTranslateStateEnding(part.getTarget(GetTarget.CLONE_SOURCE), stack);
			endings.add(stack.peek().trans);
		}
		return endings;
	}

	/**
	 * Traverses the given fragment and update the translate state.
	 * At the end of this call, the top of the stack holds the translate state at the end of the fragment.
	 * @param fragment the fragment to process.
	 * @param stack the stack of the translate state.
	 */
	private void computeTranslateStateEnding (Fragment fragment,
		Stack<TransInfo> stack)
	{
		int pos = 0;
		String ct = fragment.getCodedText();
		Tags tags = fragment.getTags();
		
		// Process the annotations
		for ( ; pos<ct.length(); pos++ ) {
			if ( Fragment.isChar1(ct.charAt(pos)) ) {
				if ( ct.charAt(pos) == Fragment.PCONT_STANDALONE ) {
					continue;
				}
				Tag tag = tags.get(ct, pos);
				if ( tag.isMarker() ) {
					boolean prevTrans = stack.peek().trans;
					boolean isOpening = (tag.getTagType() == TagType.OPENING);
					if ( isOpening ) {
						Boolean trans = ((MTag)tag).getTranslate();
						if ( trans == null ) { // Inherit from parent
							stack.push(new TransInfo(tag.getId(), prevTrans));
						}
						else { // Set the new context
							stack.push(new TransInfo(tag.getId(), trans));
						}
					}
					else { // Closing
						// Find the opening in the stack 
						for ( int j=0; j<stack.size(); j++ ) {
							if ( stack.get(j).id.equals(tag.getId()) ) {
								stack.remove(j);
								break;
							}
						}
						// Corresponding opening not found: should not occur
					}
				}
				// Skip tag type, now we point to the index
				pos++;
			}				
		} // End of processing the annotation for a given part
	}
	
	/**
	 * Show all {@link PCont} references in this unit into content.
	 * @see Unit#hideProtectedContent()
	 */
	public void showProtectedContent () {
		for ( Part part : parts ) {
			part.showProtectedContent();
		}
		// Reset the values for references
		getStore().getSourceTags().resetPContLastValue();
		getStore().getSourceTags().resetPContLastValue();
	}
	
	/**
	 * Verifies that all opening tags in the source or targt content of this unit 
	 * are located before their closing counterparts.
	 * @param target true to verify the target, false to verify the source. 
	 */
	public void verifyOpeningsBeforeClosings (boolean target) {
		List<Part> list = parts;
		HashMap<String, Boolean> openings = new HashMap<>();
		ArrayList<String> isolated = new ArrayList<>();
		if ( target ) list = getTargetOrderedParts();
		String ct;
		Tags tags;
		for (Part part : list) {
			// Get the coded text for this part
			if (target) {
				// Possibly none for the target
				if (!part.hasTarget()) continue;
				ct = part.getTarget().getCodedText();
				tags = store.getTargetTags();
			} else {
				ct = part.getSource().getCodedText();
				tags = store.getSourceTags();
			}
			for (int j = 0; j < ct.length(); j++) {
				// Go through the inline tags
				if (Fragment.isChar1(ct.charAt(j))) {
					Tag m = tags.get(ct, j);
					j++;
					if (m.getTagType() == TagType.OPENING) {
						openings.put(m.getId(), true);
					} else if (m.getTagType() == TagType.CLOSING) {
						if (openings.containsKey(m.getId())) {
							openings.remove(m.getId());
						} else {
							if (m.isCode()) { // Possibly an isolated code
								isolated.add(m.getId());
							} else { // No isolated tag for annotation
								throw new InvalidMarkerOrderException(String.format("Closing marker tag id='%s' is placed before its corresponding opening tag.", m.getId()));
							}
						}
					}
				}
			}
		}
		// Check isolated candidates against the list of opening markers left
		for ( String isoId : isolated ) {
			if ( openings.containsKey(isoId) ) {
				// If there is a corresponding closing: it's not an isolated closing but a misplaced one
				throw new InvalidMarkerOrderException(String.format("Closing code tag id='%s' is placed before its corresponding opening tag.", isoId));
			}
		}
	}

	/**
	 * Verifies if the non-removable tags in the source content of this unit are present in the target.
	 * Note that only the source tags that are in parts with an existing target are verified
	 * (as the absence of target is allowed).
	 */
	public void verifyReadOnlyTags () {
		Tags srcTags = getStore().getSourceTags(); 
		Tags trgTags = getStore().getTargetTags(); 
		for ( Part part : parts ) {
			if ( !part.hasTarget() ) continue;
			String ct = part.getSource().getCodedText();
			verifyContentForReadOnlyTags(ct, srcTags, trgTags);
		}
	}
	
	/**
	 * Verifies if the non-removable tags in a given source coded text are present in the target.
	 * @param codedText the source coded text to verify.
	 * @param srcTags the tags of the source.
	 * @param trgTags the tags of the target.
	 */
	private void verifyContentForReadOnlyTags (String codedText,
		Tags srcTags,
		Tags trgTags)
	{
		for ( int i=0; i<codedText.length(); i++ ) {
			char ch = codedText.charAt(i);
			switch ( ch ) {
			case Fragment.PCONT_STANDALONE:
				PCont pcont = srcTags.getPCont(codedText, i); i++;
				verifyContentForReadOnlyTags(pcont.getCodedText(), srcTags, trgTags);
				break;
			case Fragment.CODE_OPENING:
			case Fragment.CODE_CLOSING:
			case Fragment.CODE_STANDALONE:
				CTag ctag = srcTags.getCTag(codedText, i); i++;
				if ( !ctag.getCanDelete() ) {
					if ( trgTags.get(ctag.getId(), ctag.getTagType()) == null ) {
						throw new XLIFFException(String.format("Code id='%s' (%s) is non-removable but missing from the target content.",
							ctag.getId(), ctag.getTagType()));
					}
				}
				break;
			default:
				if ( Fragment.isChar1(ch) ) i++;
				break;
			}
		}
	}
	
	/**
	 * Gets the span-like object associated with a given id in this unit.
	 * @param ref the reference of the object.
	 * @return the object or null if not found.
	 */
	public Object getSourceOrTargetReference (String ref) {
		int pos = ref.lastIndexOf('#');
		if ( pos == -1 ) {
			throw new InvalidParameterException(String.format("The reference '%s' has no fragment id.", ref));
		}
		String refId = ref.substring(pos+1);
		// test for target reference
		if ( refId.startsWith("t=") ) { // Target reference
			refId = refId.substring(2);
			if ( store.hasTargetTag() ) {
				for ( Tag tag : store.getTargetTags() ) {
					if ( refId.equals(tag.getId()) ) return tag;
				}
			}
		}
		else { // Source reference
			// Check the parts
			for ( Part part : parts ) {
				// The part's id can be null: equals should support that
				if ( refId.equals(part.getId()) ) return part;
			}
			// Check the source markers
			if ( store.hasSourceTag() ) {
				for ( Tag tag : store.getSourceTags() ) {
					if ( refId.equals(tag.getId()) ) return tag;
				}
			}
		}
		return null;
	}
	
	/**
	 * Creates the list of {@link CTag} in the source or target content, in their
	 * respective order.
	 * This method does not check content inside protected text.
	 * @param target true to create the target list (in target order).
	 * @return the list of codes.
	 */
	public List<CTag> getOrderedCTags (boolean target) {
		ArrayList<CTag> res = new ArrayList<>();
		Tags tags;
		String ct;
		List<Part> list = parts;
		if ( target ) {
			list = getTargetOrderedParts();
			tags = getStore().getTargetTags();
		}
		else {
			tags = getStore().getSourceTags();
		}
		// Go through all the parts
		for ( Part part : list ) {
			if ( target ) {
				if ( !part.hasTarget() ) {
					continue;
				}
				ct = part.getTarget().getCodedText();
			}
			else {
				ct = part.getSource().getCodedText();
			}
			// Lookup the coded text
			for ( int i=0; i<ct.length(); i++ ) {
				if ( Fragment.isChar1(ct.charAt(i)) ) {
					if ( Fragment.isCTag(ct.charAt(i)) ) {
						res.add((CTag)tags.get(ct, i));
					}
					i++;
				}
			}
		}
		return res;
	}
	
	/**
	 * Indicates if all parts (segments and ignorables) of this unit that have a non-empty source 
	 * have also a non-empty target.
	 * @return true if all source parts that are not empty have a non-empty target too, false otherwise.
	 */
	public boolean doNonEmptySourcesHaveNonEmptyTargets () {
		for ( Part part : parts ) {
			// Skip empty parts
			if ( part.getSource().isEmpty() ) continue;
			// Check if there is a target
			if ( !part.hasTarget() ) return false;
			// Check if that target is not empty
			if ( part.getTarget().isEmpty() ) return false;
		}
		return true;
	}

	/**
	 * Removes all the annotation markers from the source and target 
	 * (if a target is available) in this unit.
	 */
	public void removeMarkers () {
		for ( Part part : parts ) {
			part.removeMarkers(false, null);
			part.removeMarkers(true, null);
		}
	}

	@Override
	public DataCategoryGroup<?> addITSGroup (DataCategoryGroup<?> group) {
		if ( itsList == null ) {
			itsList = new ArrayList<>();
		}
		itsList.add(group);
		return group;
	}

	@Override
	public boolean hasITSGroup () {
		if ( itsList == null ) return false;
		return !itsList.isEmpty();
	}
	
	@Override
	public List<DataCategoryGroup<?>> getITSGroups () {
		if ( itsList == null ) {
			itsList = new ArrayList<>();
		}
		return itsList;
	}

	@Override
	public boolean hasITSItem () {
		if ( itsItems == null ) return false;
		return !itsItems.isEmpty();
	}

	@Override
	public ITSItems getITSItems () {
		if ( itsItems == null ) {
			itsItems = new ITSItems();
		}
		return itsItems;
	}

	@Override
	public void setITSItems (ITSItems itsItems) {
		this.itsItems = itsItems;
	}

	/**
	 * Indicates if this unit has at least one match.
	 * @return true if this unit has at least one match, false if not.
	 */
	public boolean hasMatch () {
		if ( matches == null ) return false;
		return !matches.isEmpty();
	}
	
	/**
	 * Gets the {@link Matches} object for this unit, creates an empty of if there is none.
	 * @return the {@link Matches} object for this unit (can be empty, but never null).
	 */
	public Matches getMatches () {
		if ( matches == null ) matches = new Matches();
		return matches;
	}
	
	/**
	 * sets the {@link Matches} object for this unit.
	 * @param matches the new {@link Matches} object for this unit.
	 */
	public void setMatches (Matches matches) {
		this.matches = matches;
	}

	/**
	 * Indicates if this unit has at least one glossary entry.
	 * @return true if this unit has at least one glossary entry, false if not.
	 */
	public boolean hasGlossEntry () {
		if ( glossary == null ) return false;
		return !glossary.isEmpty();
	}
	
	/**
	 * Gets the {@link Glossary} object for this unit, creates an empty of if there is none.
	 * @return the {@link Glossary} object for this unit (can be empty, but never null).
	 */
	public Glossary getGlossary () {
		if ( glossary == null ) glossary = new Glossary();
		return glossary;
	}
	
	/**
	 * sets the {@link Glossary} object for this unit.
	 * @param glossary the new {@link Glossary} object for this unit.
	 */
	public void setGlossary (Glossary glossary) {
		this.glossary = glossary;
	}

	/**
	 * Creates a list of the annotated spans in this unit.
	 * <p>The {@link Part} and {@link MTag} objects in the list are live references,
	 * but any static data (e.g. the start and end position of the span) are a snapshot of the
	 * information at the moment of the call, any change to the content may make that information
	 * out-of-date and invalid.
	 * <p>The spans are listed in the order their opening markers appears in the coded text. 
	 * <p>The call is expected to be made on a unit with no hidden protected content.
	 * @param target true to lookup the target, false to lookup the source.
	 * @return a list of {@link AnnotatedSpan} objects for this unit.
	 */
	public List<AnnotatedSpan> getAnnotatedSpans (boolean target) {
		List<AnnotatedSpan> list = new ArrayList<>();
		List<AnnotatedSpan> trace = new ArrayList<>();
		for ( Part part : parts ) {
			String ct;
			Tags tags;
			boolean contentBefore = false;
			if ( target ) {
				if ( !part.hasTarget() ) continue;
				tags = getStore().getTargetTags();
				ct = part.getTarget().getCodedText();
			}
			else {
				ct = part.getSource().getCodedText();
				tags = getStore().getSourceTags();
			}
			
			// Process the coded text
			for ( int i=0; i<ct.length(); i++ ) {
				char ch = ct.charAt(i);
				if ( Fragment.isChar1(ch) ) {
					switch ( ch ) {
					case Fragment.MARKER_OPENING:
						MTag opening = (MTag)tags.get(ct, i);
						AnnotatedSpan aspan = new AnnotatedSpan(opening, part, i+2);
						aspan.setFullContent(!contentBefore); // Temporary setting until the end of the span
						list.add(aspan);
						trace.add(aspan);
						break;
					case Fragment.MARKER_CLOSING:
						MTag closing = (MTag)tags.get(ct, i);
						for ( AnnotatedSpan item : trace ) {
							if ( closing.getId().equals(item.getMarker().getId()) ) {
								item.setEndPart(part);
								// When creating the span: we use the end to store the last start
								item.append(ct.substring(item.getEnd(), i));
								item.setEnd(i); // Then we set the real ending position
								if ( item.isFullContent() ) { // Correct temporary full-content property
									if ( Fragment.hasContentAfter(ct, i) ) {
										item.setFullContent(false);
									} // Else: keep full-content set to true
								}
								trace.remove(item);
								break;
							}
						}
						break;
					case Fragment.PCONT_STANDALONE:
						throw new XLIFFException("For now getAnnotatedSpans() expects a unit without hidden protected content.");
					default: // Codes
						contentBefore = true;
						break;
					}
					// For all tags: skip over next
					i++;
				}
				else {
					contentBefore = true;
				}
			}
			// This fragment is done:
			// Copy the text to the spans not done yet
			// And reset their next start (we use the end position as a temporary variable)
			for ( AnnotatedSpan item : trace ) {
				item.append(ct.substring(item.getEnd()));
				item.setEnd(0); // For next part
				item.setPartCount(item.getPartCount()+1);
			}
			
		}
		return list;
	}

	/**
	 * Retrieves all exact matches from this unit, i.e. similarity &gt;= 100.0. 
	 * 
	 * @return An array list with the relevant matches
	 */
	public List<Match> getAllExactMatches () {
		double minSim = 100.0;
		return getFilteredListForMinimumSimilarity(minSim);
	}

	/**
	 * Retrieves all matches from this unit with the given minimum similarity. 
	 * 
	 * @param minSim The minimum similarity as a percentage, e.g. 75.0 for all matches with 75% or higher
	 *  
	 * @return An array list with the relevant matches
	 */
	public List<Match> getMatchesByMinimumSimilarity (double minSim) {
		return getFilteredListForMinimumSimilarity(minSim);
	}

	/**
	 * Retrieves all matches from this unit in the given similarity range. 
	 * 
	 * @param minSim The minimum similarity as a percentage, e.g. 75.0 for all matches with 75% or higher
	 * @param maxSim The maximum similarity
	 *   
	 * @return An array list with the relevant matches
	 */
	public List<Match> getMatchesBySimilarityRange (double minSim, double maxSim) {
		return getFilteredListForSimilarityRange(minSim, maxSim);
	}

	/**
	 * Retrieves all matches from this unit for the given segment (zero-based index) 
	 * 
	 * @param segIdx The index of the segment (zero-based)
	 *   
	 * @return An array list with the relevant matches
	 */
	public List<Match> getMatchesForSegment(int segIdx) {
		if ( matches == null ) {
			return Collections.emptyList();
		}
		List<Match> matchesForSegment = new ArrayList<>();
		Set<String> matchIds = new HashSet<>();
		
		for (Tag tag : this.getSegment(segIdx).getSource().getOwnTags()) {
			if (Match.ELEM_MTC_MATCH.equals(tag.getType())) {
				matchIds.add(Match.MATCH_REF_PREFIX + tag.getId());
			}
		}

		// OPT MW: This (and the filtering below) could be simplified to a one-liner using the Guava library with its Predicates: 
		// http://google.github.io/guava/releases/16.0/api/docs/com/google/common/collect/Iterators.html#filter%28java.util.Iterator,%20com.google.common.base.Predicate%29
		// Or we create our own implementation of a FilterIterator...
		for (Match match : matches) {
			if (matchIds.contains(match.getRef())) {
				matchesForSegment.add(match);
			}
		}
		return matchesForSegment;
	}

	/**
	 * Retrieves all matches from this unit for the given ref attribute value. 
	 * 
	 * @param ref The value of the ref attribute, with or without the "#" prefix
	 *   
	 * @return An array list with the relevant matches
	 */
	public List<Match> getMatchesByRef (String ref) {
		if ( matches == null ) {
			return Collections.emptyList();
		}
		if ( !ref.startsWith(Match.MATCH_REF_PREFIX) ) {
			ref = Match.MATCH_REF_PREFIX + ref; // Allow parameter both with and without the prefix
		}
		List<Match> filteredMatches = new ArrayList<>();

		for (Match match : matches) {
			if (match.getRef().equals(ref)) {
				filteredMatches.add(match);
			}
		}
		
		return filteredMatches;
	}

	
	// Private helper methods
	
	private List<Match> getFilteredListForMinimumSimilarity (double minSim) {
		if ( matches == null ) {
			return Collections.emptyList();
		}
		List<Match> filteredMatches = new ArrayList<>();
		for (Match match : matches) {
			if (match.getSimilarity() >= minSim) {
				filteredMatches.add(match);
			}
		}
		
		return filteredMatches;
	}
	
	private List<Match> getFilteredListForSimilarityRange (double minSim, double maxSim) {
		if ( matches == null ) {
			return Collections.emptyList();
		}
		List<Match> filteredMatches = new ArrayList<>();

		for (Match match : matches) {
			if (match.getSimilarity() >= minSim && match.getSimilarity() <= maxSim) {
				filteredMatches.add(match);
			}
		}
		
		return filteredMatches;
	}

}
