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

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;

/**
 * Represents an opening, closing or standalone tag for an original inline code.
 */
public class CTag extends Tag {

	protected CTagCommon cc = null;

	private String data;
	private boolean initialWithData;
	private String dataRef;
	private Directionality dataDir = Directionality.AUTO;
	private String disp;
	private String equiv = "";
	private String subFlows;
	private CanReorder canReorder = CanReorder.YES;

	CTag (CTagCommon cc,
		TagType tagType,
		String id,
		String data)
	{
		if ( cc == null ) this.cc = new CTagCommon(id);
		else this.cc = cc;
		this.tagType = tagType;
		this.data = data;
	}

	/**
	 * Creates a new code tag (without any link to another tag).
	 * @param tagType the tag type.
	 * @param id the id (should not be null).
	 * @param data the data (can be null).
	 */
	public CTag (TagType tagType,
		String id,
		String data)
	{
		this(null, tagType, id, data);
	}

	CTag (CTag opposite,
		String data)
	{
		switch ( opposite.tagType ) {
		case CLOSING:
			this.tagType = TagType.OPENING;
			break;
		case OPENING:
			this.tagType = TagType.CLOSING;
			break;
		case STANDALONE:
		default:
			throw new InvalidParameterException("Counterpart must be an opening or closing tag.");
		}
		this.cc = opposite.cc;
		this.data = data;
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to copy.
	 * @param opposite the opening/closing tag to connect with this new tag.
	 * this parameter must be created already. 
	 */
	CTag (CTag original,
		CTag opposite)
	{
		super(original);
		
		if ( opposite == null ) this.cc = new CTagCommon(original.cc);
		else this.cc = opposite.cc;
		
		this.data = original.data;
		this.dataDir = original.dataDir;
		this.dataRef = original.dataRef;
		this.initialWithData = original.initialWithData;
		this.canReorder = original.canReorder;
		this.disp = original.disp;
		this.equiv = original.equiv;
		this.subFlows = original.subFlows;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) return false;
		if (this == other) return true;
		if (!(other instanceof CTag)) return false;
		if (!super.equals(other)) return false;

		CTag cTag = (CTag) other;
		return initialWithData == cTag.initialWithData &&
				Objects.equals(cc, cTag.cc) &&
				Objects.equals(data, cTag.data) &&
				Objects.equals(dataRef, cTag.dataRef) &&
				dataDir == cTag.dataDir &&
				Objects.equals(disp, cTag.disp) &&
				Objects.equals(equiv, cTag.equiv) &&
				Objects.equals(subFlows, cTag.subFlows) &&
				canReorder == cTag.canReorder;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), cc, data, initialWithData, dataRef, dataDir, disp, equiv, subFlows, canReorder);
	}

	@Override
	public boolean isCode () {
		return true;
	}
	
	@Override
	public boolean isMarker () {
		return false;
	}

	@Override
	public String getId () {
		return cc.getId();
	}

	@Override
	public String getType () {
		return cc.getType();
	}

	@Override
	public void setType (String type) {
		cc.setType(type);
	}

	/**
	 * Gets the sub-type of this code.
	 * @return the sub-type of this code.
	 */
	public String getSubType () {
		return cc.getSubType();
	}

	/**
	 * Sets the sub-type of this code (for both opening/closing tags).
	 * @param subType the new sub-type of this code.
	 */
	public void setSubType (String subType) {
		cc.setSubType(subType);
	}

	/**
	 * Indicates if this code can be copied.
	 * @return true if this code can be copied, false if it cannot.
	 */
	public boolean getCanCopy () {
		return cc.getCanCopy();
	}

	/**
	 * Sets the flag indicating if this code can be copied (for both opening/closing tags).
	 * @param canCopy true to allow this code to be copied, false otherwise.
	 */
	public void setCanCopy (boolean canCopy) {
		cc.setCanCopy(canCopy);
	}

	/**
	 * Indicates if this spanning code can overlap others (i.e. be not well-formed).
	 * This information applies only to paired codes, not to standalone codes.
	 * @return true if this code can overlap others, false otherwise.
	 */
	public boolean getCanOverlap () {
		return cc.getCanOverlap();
	}

	/**
	 * sets the flag indicating if this code can overlap others (for both opening/closing tags).
	 * @param canOverlap true to allow this code to overlap others, false otherwise.
	 */
	public void setCanOverlap (boolean canOverlap) {
		cc.setCanOverlap(canOverlap);
	}

	/**
	 * Indicates if this code can be deleted.
	 * @return true if this code can be deleted, false otherwise.
	 */
	public boolean getCanDelete () {
		return cc.getCanDelete();
	}

	/**
	 * Sets the flag indicating if this code can be deleted (for both opening/closing tags).
	 * @param canDelete true to allow this code to be deleted, false otherwise.
	 */
	public void setCanDelete (boolean canDelete) {
		cc.setCanDelete(canDelete);
	}

	/**
	 * Indicates if this code can be re-ordered.
	 * @return one of the {@link CanReorder} values. If the tag is a closing tag and
	 * the code is set to {@link CanReorder#FIRSTNO} this method return {@link CanReorder#NO} as the
	 * closing tag of a code cannot be a first-no (only an opening tag can be). 
	 */
	public CanReorder getCanReorder () {
		if (( tagType == TagType.CLOSING ) && ( canReorder == CanReorder.FIRSTNO )) {
			return CanReorder.NO;
		}
		return canReorder;
	}

	/**
	 * Sets the flag indicating if this code can be re-ordered.
	 * If the new value is different from {@link CanReorder#YES} the <code>canDelete</code> and <code>canCopy</code>
	 * fields are set automatically to false.
	 * @param canReorder the new value for the flag indicating if this code can be re-ordered.
	 */
	public void setCanReorder (CanReorder canReorder) {
		this.canReorder = canReorder;
		if ( canReorder != CanReorder.YES ) {
			setCanDelete(false);
			setCanCopy(false);
		}
	}

	/**
	 * Gets the ID of the code from which this code is a copy. 
	 * @return the ID of the code from which this code was copied (can be null).
	 */
	public String getCopyOf () {
		return cc.getCopyOf();
	}

	/**
	 * Sets the ID of the code from which this code is a copy (for both opening/closing tags).
	 * @param id the ID of the code from which this code was copied (can be null).
	 */
	public void setCopyOf (String id) {
		cc.setCopyOf(id);
	}

	/**
	 * Indicates if this code's tag has some content for its original data.
	 * @return true if the original data is neither null nor empty.
	 */
	public boolean hasData () {
		return !Util.isNoE(data);
	}
	
	/**
	 * Gets the original data of this code's tag.
	 * @return the original data of this code's tag (can be null).
	 */
	public String getData () {
		return data;
	}

	/**
	 * Sets the original data of this code's tag.
	 * @param data the original data of this code's tag (can be null).
	 */
	public void setData (String data) {
		this.data = data;
	}

	/**
	 * Gets the ID of the element where this code's tag is stored.
	 * @return the ID of the element where this code's tag is stored (can be null).
	 */
	public String getDataRef () {
		return dataRef;
	}

	/**
	 * Sets the ID of the element where this code's tag is stored.
	 * @param dataRef the ID of the element where this code's tag is stored (can be null).
	 */
	public void setDataRef (String dataRef) {
		this.dataRef = dataRef;
	}
	
	/**
	 * Indicates if this code's tag had an original data initially (when read from the document).
	 * @return true if this code's tag had an original data initially.
	 */
	public boolean isInitialWithData () {
		return initialWithData;
	}

	/**
	 * Sets the flag indicating if this code's tag had an original data initially.
	 * @param initialWithData true to indicate that this code's tag had an original data initially.
	 */
	public void setInitialWithData (boolean initialWithData) {
		this.initialWithData = initialWithData;
	}

	/**
	 * Gets the directionality of the original data of this code's tag.
	 * @return the directionality of the original data of this code's tag.
	 */
	public Directionality getDataDir () {
		return dataDir;
	}

	/**
	 * Sets the directionality of the original data of this code's tag.
	 * @param dir the new directionality of the original data of this code's tag.
	 */
	public void setDataDir (Directionality dir) {
		this.dataDir = dir;
	}

	/**
	 * Gets the user-friendly representation of the data of this code's tag. 
	 * @return the user-friendly representation of the data of this code's tag (can be null).
	 */
	public String getDisp () {
		return disp;
	}

	/**
	 * Sets the user-friendly representation of the data of this code's tag.
	 * @param disp the new user-friendly representation of the data of this code's tag.
	 */
	public void setDisp (String disp) {
		this.disp = disp;
	}

	/**
	 * Gets the text equivalent representation of the data of this code's tag.
	 * @return the text equivalent representation of the data of this code's tag (can be empty but not null).
	 */
	public String getEquiv () {
		return equiv;
	}

	/**
	 * Sets the text equivalent representation of the data of this code's tag.
	 * @param equiv the new text equivalent representation of the data of this code's tag.
	 * A null value will result in an empty string representation.
	 */
	public void setEquiv (String equiv) {
		if ( equiv == null ) this.equiv = "";
		this.equiv = equiv;
	}

	/**
	 * Gets the IDs of the units representing the sub-flows for this code's tag. 
	 * @return the IDs of the units representing the sub-flows for this code's tag.
	 */
	public String getSubFlows () {
		return subFlows;
	}

	/**
	 * Sets the IDs of the units representing the sub-flows for this code's tag.
	 * The list is automatically normalized.
	 * @param subFlows the new IDs of the units representing the sub-flows for this code's tag.
	 */
	public void setSubFlows (String subFlows) {
		if ( subFlows == null ) {
			this.subFlows = null;
			return;
		}
		// Else: normalize the value
		String[] ids = subFlows.split("[\\t\\n ]+", -1);
		String value = "";
		for ( String id : ids ) {
			if ( id.isEmpty() ) continue; // For leading/trailing cases
			if ( !value.isEmpty() ) value += " ";
			value += id;
		}
		this.subFlows = value;
	}

	/**
	 * Gets an array of the IDs of the units representing the sub-flows for this code's tag.
	 * @return an array of IDs (can be empty but is never null).
	 */
	public String[] getSubFlowsIds () {
		if ( subFlows == null ) return new String[0];
		return subFlows.split(" ", -1);
	}
	
	/**
	 * Gets the directionality of the content of this code.
	 * @return the directionality of the content of this code.
	 */
	public Directionality getDir () {
		return cc.getDir();
	}

	/**
	 * Sets the directionality of the content of this code (for both opening/closing tags).
	 * @param dir the new directionality of the content of this code.
	 */
	public void setDir (Directionality dir) {
		cc.setDir(dir);
	}

	/**
	 * Verifies the type and sub-type values.
	 * @throws InvalidParameterException if a value is invalid.
	 */
	public void verifyTypeSubTypeValues () {
		if ( cc.getSubType() == null ) return;
		String type = getType();
		switch ( cc.getSubType() ) {
		case "xlf:lb":
		case "xlf:pb":
		case "xlf:b":
		case "xlf:i":
		case "xlf:u":
			if (( type == null ) || !type.equals("fmt") ) {
				throw new InvalidParameterException(String.format("When subType is '%s', type must be 'fmt'.", cc.getSubType()));
			}
			break;
		case "xlf:var":
			if (( type == null ) || !type.equals("ui") ) {
				throw new InvalidParameterException(String.format("When subType is '%s', type must be 'fmt'.", cc.getSubType()));
			}
			break;
		}
	}

}
