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

package net.sf.okapi.lib.xliff2.its;

import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Implements the <a href='http://www.w3.org/TR/its20/#provenance'>Provenance</a> data category.
 */
public class Provenance extends DataCategory {

	private String tool;
	private String toolRef;
	private String revTool;
	private String revToolRef;
	private String org;
	private String orgRef;
	private String revOrg;
	private String revOrgRef;
	private String person;
	private String personRef;
	private String revPerson;
	private String revPersonRef;
	private String provRef;
	private String unresolvedGroupRef;

	/**
	 * Creates a new {@link Provenance} object without initial data.
	 */
	public Provenance () {
		// Nothing to do
	}

	@Override
	public String getDataCategoryName () {
		return DataCategories.PROVENANCE;
	}
	
	@Override
	public void validate () {
		if (( tool != null ) && ( toolRef != null )) {
			throw new XLIFFException("ITS tool and toolRef must not be used at the same time.");
		}
		if (( revTool != null ) && ( revToolRef != null )) {
			throw new XLIFFException("ITS revTool and revToolRef must not be used at the same time.");
		}
		if (( org != null ) && ( orgRef != null )) {
			throw new XLIFFException("ITS org and orgRef must not be used at the same time.");
		}
		if (( revOrg != null ) && ( revOrgRef != null )) {
			throw new XLIFFException("ITS revOrg and revOrgRef must not be used at the same time.");
		}
		if (( person != null ) && ( personRef != null )) {
			throw new XLIFFException("ITS person and personRef must not be used at the same time.");
		}
		if (( revPerson != null ) && ( revPersonRef != null )) {
			throw new XLIFFException("ITS revPerson and revPersonRef must not be used at the same time.");
		}
	}

	@Override
	public boolean hasUnresolvedGroup () {
		return (unresolvedGroupRef != null);
	}
	
	@Override
	public IITSItem createCopy () {
		Provenance newItem = new Provenance();
		newItem.setAnnotatorRef(getAnnotatorRef());

		newItem.org = org;
		newItem.orgRef = orgRef;
		newItem.person = person;
		newItem.personRef = personRef;
		newItem.provRef = provRef;
		newItem.revOrg = revOrg;
		newItem.revOrgRef = revOrgRef;
		newItem.revPerson = revPerson;
		newItem.revPersonRef = revPersonRef;
		newItem.revTool = revTool;
		newItem.revToolRef = revToolRef;
		newItem.tool = tool;
		newItem.toolRef = toolRef;
		newItem.unresolvedGroupRef = unresolvedGroupRef;
		
		return newItem;
	}

	public String getUnresolvedGroupRef () {
		return unresolvedGroupRef;
	}
	
	public void setUnresolvedGroupRef (String unresolvedGroupRef) {
		this.unresolvedGroupRef = unresolvedGroupRef;
	}
	
	/**
	 * Gets the tool attribute.
	 * @return the tool attribute (can be null).
	 */
	public String getTool () {
		return tool;
	}

	/**
	 * Sets a new tool attribute.
	 * @param tool the new tool attribute (can be null).
	 */
	public void setTool (String tool) {
		this.tool = tool;
	}

	public String getToolRef () {
		return toolRef;
	}

	public void setToolRef (String toolRef) {
		this.toolRef = toolRef;
	}

	/**
	 * Gets the revision tool attribute.
	 * @return the revision tool attribute (can be null).
	 */
	public String getRevTool () {
		return revTool;
	}

	/**
	 * sets a new revision tool attribute.
	 * @param revTool the new revision tool attribute.
	 */
	public void setRevTool (String revTool) {
		this.revTool = revTool;
	}

	public String getRevToolRef () {
		return revToolRef;
	}

	public void setRevToolRef (String revToolRef) {
		this.revToolRef = revToolRef;
	}

	public String getOrg () {
		return org;
	}

	public void setOrg (String org) {
		this.org = org;
	}

	public String getOrgRef () {
		return orgRef;
	}

	public void setOrgRef (String orgRef) {
		this.orgRef = orgRef;
	}

	public String getRevOrg () {
		return revOrg;
	}

	public void setRevOrg (String revOrg) {
		this.revOrg = revOrg;
	}

	public String getRevOrgRef () {
		return revOrgRef;
	}

	public void setRevOrgRef (String revOrgRef) {
		this.revOrgRef = revOrgRef;
	}

	public String getPerson () {
		return person;
	}

	public void setPerson (String person) {
		this.person = person;
	}

	public String getPersonRef () {
		return personRef;
	}

	public void setPersonRef (String personRef) {
		this.personRef = personRef;
	}

	public String getRevPerson () {
		return revPerson;
	}

	public void setRevPerson (String revPerson) {
		this.revPerson = revPerson;
	}

	public String getRevPersonRef () {
		return revPersonRef;
	}

	public void setRevPersonRef (String revPersonRef) {
		this.revPersonRef = revPersonRef;
	}

	public String getProvRef () {
		return provRef;
	}

	public void setProvRef (String provRef) {
		this.provRef = provRef;
	}

}
