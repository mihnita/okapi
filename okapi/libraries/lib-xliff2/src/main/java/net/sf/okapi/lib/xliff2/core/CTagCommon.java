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

import java.util.Objects;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;

/**
 * Represents common data for the opening and closing {@link CTag}.
 */
class CTagCommon {

	private static final int CANCOPY = 0x01;
	private static final int CANDELETE = 0x02;

	private String id;
	private String type;
	private int hints = (CANCOPY | CANDELETE);
	private boolean canOverlap;
	private String subType;
	private String copyOf;
	private Directionality dir = Directionality.INHERITED;
	
	CTagCommon (String id) {
		if ( id == null ) {
			throw new InvalidParameterException("The id parameter cannot be null.");
		}
		this.id = id;
	}

	/**
	 * Copy constructor.
	 * @param original the original object to copy.
	 */
	CTagCommon (CTagCommon original) {
		this.canOverlap = original.canOverlap;
		this.copyOf = original.copyOf;
		this.dir = original.dir;
		this.hints = original.hints;
		this.id = original.id;
		this.subType = original.subType;
		this.type = original.type;
	}
	
	public String getId () {
		return id;
	}

	public void setId (String id) {
		this.id = id;
	}

	public String getType () {
		return type;
	}

	public void setType (String type) {
		Util.checkValueList("fmt;ui;quote;link;image;other", type, Const.ATTR_TYPE);
		// Allows null value
		this.type = type;
	}

	public String getSubType () {
		return subType;
	}

	public void setSubType (String subType) {
		if ( subType != null ) {
			int n = subType.indexOf(':');
			if (( n == -1 ) || ( n == 0 ) || ( n == subType.length()-1 )) {
				throw new InvalidParameterException(String.format("Invalid value '%s' for subType.", subType));
			}
			if ( subType.startsWith("xlf:") ) {
				Util.checkValueList("xlf:lb;xlf:pb;xlf:b;xlf:i;xlf:u;xlf:var", subType, Const.ATTR_SUBTYPE);
			}
		}
		this.subType = subType;
	}

	public boolean getCanCopy () {
		return (( hints & CANCOPY ) == CANCOPY);
	}

	public void setCanCopy (boolean canCopy) {
		if ( canCopy ) hints |= CANCOPY;
		else hints &= ~CANCOPY;
	}

	public boolean getCanDelete () {
		return (( hints & CANDELETE ) == CANDELETE);
	}

	public void setCanDelete (boolean canDelete) {
		if ( canDelete ) hints |= CANDELETE;
		else hints &= ~CANDELETE;
	}

	public boolean getCanOverlap () {
		return canOverlap;
	}

	public void setCanOverlap (boolean canOverlap) {
		this.canOverlap = canOverlap;
	}

	public String getCopyOf () {
		return copyOf;
	}

	public void setCopyOf (String copyOf) {
		this.copyOf = copyOf;
	}

	public Directionality getDir () {
		return dir;
	}

	public void setDir (Directionality dir) {
		this.dir = dir;
	}

	/**
	 * Indicates if this object is equals to a given one.
	 * @param other the other object to compare.
	 * @return true if the two objects are identical.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (!(other instanceof CTagCommon)) return false;

		CTagCommon that = (CTagCommon) other;
		return hints == that.hints &&
				canOverlap == that.canOverlap &&
				Objects.equals(id, that.id) &&
				Objects.equals(type, that.type) &&
				Objects.equals(subType, that.subType) &&
				Objects.equals(copyOf, that.copyOf) &&
				dir == that.dir;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, type, hints, canOverlap, subType, copyOf, dir);
	}
}
