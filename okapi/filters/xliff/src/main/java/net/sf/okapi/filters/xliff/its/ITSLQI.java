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

import net.sf.okapi.common.Namespaces;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.IssueAnnotation;
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.filters.xliff.XLIFFITSFilterExtension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the <its:locQualityIssue/> element.
 */
public class ITSLQI {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private IssueAnnotation ann = new IssueAnnotation();

	public ITSLQI (Iterator<Attribute> attrs) {

		String itsType = null;
		while ( attrs.hasNext() ) {
			Attribute attr = attrs.next();
			String prefix = attr.getName().getPrefix();
			String name = attr.getName().getLocalPart();
			String ns = attr.getName().getNamespaceURI();
			String value = attr.getValue();
			String prefixedName = name;
			if ( !prefix.isEmpty() && !prefix.equals("its") ) {
				prefixedName = prefix + ":" + name;
			}

			// Read ITS attributes
			// (We assume empty namespace is ITS since this is inside a standoff item)
			if ( ns.isEmpty() || ns.equals(Namespaces.ITS_NS_URI)  ) {
				switch (name) {
					case "locQualityIssueType":
						ann.setITSType(value);
						itsType = value; // Remember the original ITS type

						break;
					case "locQualityIssueComment":
						ann.setComment(value);
						break;
					case "locQualityIssueSeverity":
						ann.setSeverity(Double.parseDouble(value));
						break;
					case "locQualityIssueProfileRef":
						ann.setProfileRef(value);
						break;
					case "locQualityIssueEnabled":
						ann.setEnabled(value.equals("yes"));
						break;
					default:
						logger.warn("Unknown attribute '{}'.", prefixedName);
						break;
				}
			}
			else if ( ns.equals(Namespaces.NS_XLIFFOKAPI) ) {
				// Read the extensions
				switch (name) {
					case "lqiType":
						ann.setIssueType(IssueType.valueOf(value));
						// There an ITS type was set before, make sure we keep that value
						if (itsType != null) {
							ann.setITSType(itsType);
						}
						break;
					case "lqiPos":
						int[] positions = XLIFFITSFilterExtension.parseXLQIPos(value, logger);
						ann.setSourcePosition(positions[0], positions[1]);
						ann.setTargetPosition(positions[2], positions[3]);
						break;
					case "lqiCodes":
						ann.setCodes(value);
						break;
					case "lqiSegId":
						ann.setSegId(value);
						break;
					default:
						logger.warn("Unknown attribute '{}'.", prefixedName);
						break;
				}
			}
			else {
				logger.warn("Unknown attribute '{}'.", prefixedName);
			}
		}
	}
	
	public ITSLQI (IssueAnnotation ann) {
		this.ann = (IssueAnnotation) ann.clone();
	}

	public String getType () {
		return ann.getITSType();
	}

	public double getSeverity () {
		return ann.getSeverity();
	}

	public String getComment () {
		return ann.getComment();
	}

	public String getProfileRef () {
		return ann.getProfileRef();
	}

	public boolean getEnabled () {
		return ann.getEnabled();
	}
	
	/**
	 * Gets the annotation for this LQI object.
	 * @return the annotation, or null if there is no fields set (annotation is empty).
	 */
	public GenericAnnotation getAnnotation () {
		if ( ann.getFieldCount() == 0 ) return null;
		else return ann;
	}
}