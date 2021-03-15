/*===========================================================================
  Copyright (C) 2011-2013 by the Okapi Framework contributors
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

import net.sf.okapi.lib.xliff2.InvalidParameterException;

/**
 * Represents a segment object.
 */
public class Segment extends Part {

	/**
	 * Default value for the target state of a segment.
	 */
	public static final TargetState STATE_DEFAULT = TargetState.INITIAL;
	
	private boolean canResegment = true;
	private TargetState state = STATE_DEFAULT;
	private String subState;
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public Segment (Segment original) {
		// Create the new object from its base class copy constructor
		super(original);
		// Segment-specific fields
		canResegment = original.canResegment;
		state = original.state;
		subState = original.subState;
	}
	
	/**
	 * Creates a new {@link Segment} object.
	 * @param store the shared {@link Store} for this object.
	 */
	public Segment (Store store) {
		super(store);
	}

	/**
	 * Indicates if this segment can be re-segmented.
	 * @return true if this segment can be re-segmented, false otherwise.
	 */
	public boolean getCanResegment () {
		return canResegment;
	}
	
	/**
	 * Sets the flag indicating if this segment can be re-segmented.
	 * @param canResegment true to indicate that this segment can be re-segmented.
	 */
	public void setCanResegment (boolean canResegment) {
		this.canResegment = canResegment;
	}

	/**
	 * Gets the state for this segment.
	 * @return the state for this segment.
	 */
	public TargetState getState () {
		return state;
	}
	
	/**
	 * Sets the state for this segment.
	 * @param state the new state for this segment.
	 * The value must be "initial", "translated", "reviewed" or "final".
	 * It can also be null and in that case the value is set to {@link #STATE_DEFAULT}.
	 */
	public void setState (String state) {
		if ( state == null ) {
			this.state = STATE_DEFAULT;
		}
		else {
			this.state = TargetState.fromString(state);
		}
	}

	/**
	 * Sets the state for this segment.
	 * @param state the new state to set.
	 */
	public void setState (TargetState state) {
		this.state = state;
	}

	/**
	 * Gets the sub-state for this segment.
	 * @return the sub-state for this segment (can be null).
	 */
	public String getSubState () {
		return subState;
	}
	
	/**
	 * Sets the sub-state for this segment.
	 * @param subState the new sub-state for this segment.
	 * The string must be in the format "prefix:value" or it can be null.
	 */
	public void setSubState (String subState) {
		if ( subState != null ) {
			int n = subState.indexOf(':');
			if (( n == -1 ) || ( n == 0 ) || ( n == subState.length()-1 )) {
				throw new InvalidParameterException(String.format("Invalid value '%s' for subState.", subState));
			}
		}
		this.subState = subState;
	}
	
	@Override
	public boolean isSegment () {
		return true;
	}

	/**
	 * Creates a new empty segment based on this one.
	 * <p>The new segment uses the same store and has the same core metadata 
	 * (translate, can-re-segment, state, sub-state) as this segment.
	 * The metadata of the source and target are also copied, but not their content.
	 * The new segment has an empty target if this one has a target and it has no target if this one has no target. 
	 * The new segment has also has a new id value.
	 * @return the new segment.
	 */
	public Segment createAndCopyMetadata () {
		// Create the new segment
		Segment seg = new Segment(getStore());
		// Copy the metadata for the source
		seg.getSource().setDir(getSource().getDir(false));
		// Make sure we have a target if the original segment has one
		if ( hasTarget() ) {
			// Create the target
			// and at the same time copy the metadata for the target
			seg.getTarget(GetTarget.CREATE_EMPTY).setDir(getTarget().getDir(false));
		}
		// Copy xml:space info (source/target level in XLIFF but stored in part in library)
		seg.setPreserveWS(getPreserveWS());
		// Copy the metadata for the segment
		seg.setCanResegment(getCanResegment());
		seg.setState(getState());
		seg.setSubState(getSubState());
		// Update ID value
		seg.setId(seg.getStore().suggestId(true));
		return seg;
	}

}
