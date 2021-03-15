/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xliff.its;

import java.util.Iterator;

import javax.xml.stream.events.Attribute;

import net.sf.okapi.common.annotation.GenericAnnotationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of a &lt;its:provenanceRecord/> element.
 */
public class ITSProvenance {
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private String person, org, tool, revPerson, revOrg, revTool, provRef;

	public ITSProvenance (Iterator<Attribute> attrs) {
		while (attrs.hasNext()) {
			Attribute attr = attrs.next();
			String prefix = attr.getName().getPrefix();
			String name = attr.getName().getLocalPart();
			String value = attr.getValue();
			if ( !prefix.isEmpty() && !prefix.equals("its") ) {
				name = prefix + ":" + name;
			}

			switch (name) {
				case "person":
					setPerson(value);
					break;
				case "personRef":
					setPerson(GenericAnnotationType.REF_PREFIX + value);
					break;
				case "org":
					setOrg(value);
					break;
				case "orgRef":
					setOrg(GenericAnnotationType.REF_PREFIX + value);
					break;
				case "tool":
					setTool(value);
					break;
				case "toolRef":
					setTool(GenericAnnotationType.REF_PREFIX + value);
					break;
				case "revPerson":
					setRevPerson(value);
					break;
				case "revPersonRef":
					setRevPerson(GenericAnnotationType.REF_PREFIX + value);
					break;
				case "revOrg":
					setRevOrg(value);
					break;
				case "revOrgRef":
					setRevOrg(GenericAnnotationType.REF_PREFIX + value);
					break;
				case "revTool":
					setRevTool(value);
					break;
				case "revToolRef":
					setRevTool(GenericAnnotationType.REF_PREFIX + value);
					break;
				case "provRef":
					this.provRef = value;
					break;
				default:
					logger.warn("Unrecognized ITS Provenance attribute: {}", name);
					break;
			}
		}
	}
	
	public ITSProvenance(String person, String org, String tool, String revPerson, 
			String revOrg, String revTool, String provRef) {
		this();
		setPerson(person);
		setOrg(org);
		setTool(tool);
		setRevPerson(revPerson);
		setRevOrg(revOrg);
		setRevTool(revTool);
		setProvRef(provRef);
	}
	
	public ITSProvenance() {		
	}
	
	public String getPerson() {
		return person;
	}

	public final void setPerson(String person) {
		if (this.person != null) {
			logger.warn("Provenance person redefined from \"{}\" to \"{}\"", this.person, person);
		}
		this.person = person;
	}

	public String getOrg() {
		return org;
	}

	public final void setOrg(String org) {
		if (this.org != null) {
			logger.warn("Provenance org redefined from \"{}\" to \"{}\"", this.org, org);
		}
		this.org = org;
	}

	public String getTool() {
		return tool;
	}

	public final void setTool(String tool) {
		if (this.tool != null) {
			logger.warn("Provenance tool redefined from \"{}\" to \"{}\"", this.tool, tool);
		}
		this.tool = tool;
	}

	public String getRevPerson() {
		return revPerson;
	}

	public final void setRevPerson(String revPerson) {
		if (this.revPerson != null) {
			logger.warn("Provenance revPerson redefined from \"{}\" to \"{}\"", this.revPerson, revPerson);
		}
		this.revPerson = revPerson;
	}

	public String getRevOrg() {
		return revOrg;
	}

	public final void setRevOrg(String revOrg) {
		if (this.revOrg != null) {
			logger.warn("Provenance revOrg redefined from \"{}\" to \"{}\"", this.revOrg, revOrg);
		}
		this.revOrg = revOrg;
	}

	public String getRevTool() {
		return revTool;
	}

	public final void setRevTool(String revTool) {
		if (this.revTool != null) {
			logger.warn("Provenance revTool redefined from \"{}\" to \"{}\"", this.revTool, revTool);
		}
		this.revTool = revTool;
	}

	public String getProvRef() {
		return provRef;
	}
	
	public final void setProvRef(String provRef) {
		this.provRef = provRef;
	}
}
