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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.XLIFFException;
import net.sf.okapi.lib.xliff2.reader.XLIFFReaderException;

/**
 * Provides utility methods to read ITS attributes and stand-off elements.
 */
public class ITSReader {

	public static final String ANNOTATORSREF = "annotatorsRef";
	
	public static final String MTCONFIDENCE = "mtConfidence";
	
	public static final String DOMAINS = "domains"; // The extra s is part of the mapping

	public static final String TACONFIDENCE = "taConfidence";
	public static final String TACLASSREF = "taClassRef";
	public static final String TASOURCE = "taSource";
	public static final String TAIDENT = "taIdent";
	public static final String TAIDENTREF = "taIdentRef";
	
	public static final String LOCQUALITYISSUES = "locQualityIssues";
	public static final String LOCQUALITYISSUE = "locQualityIssue";
	public static final String LOCQUALITYISSUETYPE = "locQualityIssueType";
	public static final String LOCQUALITYISSUECOMMENT = "locQualityIssueComment";
	public static final String LOCQUALITYISSUEENABLED = "locQualityIssueEnabled";
	public static final String LOCQUALITYISSUESEVERITY = "locQualityIssueSeverity";
	public static final String LOCQUALITYISSUEPROFILEREF = "locQualityIssueProfileRef";
	public static final String LOCQUALITYISSUESREF = "locQualityIssuesRef";
	
	public static final String PROVENANCERECORDS = "provenanceRecords";
	public static final String PROVENANCERECORD = "provenanceRecord";
	public static final String PROVTOOL = "tool";
	public static final String PROVTOOLREF = "toolRef";
	public static final String PROVREVTOOL = "revTool";
	public static final String PROVREVTOOLREF = "revToolRef";
	public static final String PROVPERSON = "person";
	public static final String PROVPERSONREF = "personRef";
	public static final String PROVREVPERSON = "revPerson";
	public static final String PROVREVPERSONREF = "revPersonRef";
	public static final String PROVORG = "org";
	public static final String PROVORGREF = "orgRef";
	public static final String PROVREVORG = "revOrg";
	public static final String PROVREVORGREF = "revOrgRef";
	public static final String PROVREF = "provRef";
	public static final String PROVENANCERECORDSREF = "provenanceRecordsRef";

	public static final String TERMCONFIDENCE = "termConfidence";

	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final XMLStreamReader reader;

	// Temporary variable holding the object where the stand-off group is located
	private IWithITSGroups groupHolder;
	
	/**
	 * Creates a new {@link ITSReader} object for a given reader.
	 * @param reader the XML stream reader to associate with this object.
	 */
	public ITSReader (XMLStreamReader reader) {
		this.reader = reader;
	}

	/**
	 * Reads the stand-off ITS elements
	 * @param initialLocName name of the element on which the method is called.
	 * @param groupHolder object where the group is located.
	 * @param ar the annotator reference context.
	 * @throws XMLStreamException if a reading error occurs.
	 */
	public void readStandOffElements (String initialLocName,
		IWithITSGroups groupHolder,
		AnnotatorsRef ar)
		throws XMLStreamException 
	{
		this.groupHolder = groupHolder;
		boolean isLQI = false;
		LocQualityIssues lqIssues = null;
		Provenances provRecs = null;
		String id = reader.getAttributeValue(Const.NS_XML, "id");
		if ( id == null ) {
			throw new XLIFFReaderException("A stand-off element must have an xml:id attribute.");
		}
		switch ( initialLocName ) {
		case LOCQUALITYISSUES:
			lqIssues = new LocQualityIssues(id);
			isLQI = true;
			break;
		case PROVENANCERECORDS:
			provRecs = new Provenances(id);
			break;
		default:
			throw new XLIFFReaderException(String.format("Element '%s' is not ITS or is misplaced.", initialLocName));
		}

		String tmp, nsUri;
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				if ( !nsUri.equals(Const.NS_ITS) ) {
					// Ignore those entries
					logger.warn("Ignoring unexpected non-ITS element '{}'.", reader.getName());
					continue;
				}
				if ( tmp.equals(LOCQUALITYISSUE) && isLQI ) {
					IITSItem item = readLQI(true, ar);
					if ( item == null ) {
						throw new XLIFFException("Invalid <locQualityIssue> element in stand-off group.");
					}
					lqIssues.getList().add((LocQualityIssue)item);
					continue;
				}
				if ( tmp.equals(PROVENANCERECORD)&& !isLQI ) {
					IITSItem item = readProvenance(true, ar);
					if ( item == null ) {
						throw new XLIFFException("Invalid <provenanceRecord> element in stand-off group.");
					}
					provRecs.getList().add((Provenance)item);
					continue;
				}
				// Else: unexpected ITS element
				logger.warn("Ignoring unexpected ITS element '{}'.", reader.getName());
				break;
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				if ( nsUri.equals(Const.NS_ITS) ) {
					if ( tmp.equals(initialLocName) ) {
						groupHolder.addITSGroup(lqIssues==null ? provRecs : lqIssues);
						return;
					}
				}
				break;
			}
		}
	}

	/**
	 * Reads the ITS attributes for the current element.
	 * @param groupHolder the object where any stand-off group for the object being looked at is expected to be.
	 * @param object the object where to read from.
	 * @param ar the current annotators references (can be null).
	 * @return true if one or more of the ITS items read has an unresolved stand-off reference. 
	 */
	public boolean readAttributes (IWithITSGroups groupHolder,
		IWithITSAttributes object,
		AnnotatorsRef ar)
	{
		boolean hasUnresolvedGroup = false;
		this.groupHolder = groupHolder;
		ITSItems list = new ITSItems();

		AnnotatorsRef objAr = readAnnotatorsRef(false, ar);
		if ( objAr == null ) objAr = ar; // Inherits
		
		// Localization Quality Issue
		IITSItem item = readLQI(false, objAr);
		if ( item != null ) {
			list.add(item);
			if ( item.hasUnresolvedGroup() ) hasUnresolvedGroup = true;
		}
		
		// Provenance
		if ( (item = readProvenance(false, objAr)) != null ) {
			list.add(item);
			if ( item.hasUnresolvedGroup() ) hasUnresolvedGroup = true;
		}
		
		// MT Confidence
		if ( (item = readMtConfidence(objAr)) != null ) {
			list.add(item);
		}
		
		// Text Analysis
		if ( (item = readTextAnalysis(objAr)) != null ) {
			list.add(item);
		}
		
		// Domain
		if ( (item = readDomain(objAr)) != null ) {
			list.add(item);
		}

		// Set the attributes if any was read
		if ( !list.isEmpty() ) {
			object.setITSItems(list);
		}
		
		return hasUnresolvedGroup;
	}
	
	/**
	 * Reads the ITS attributes for terminology.
	 * @param marker the marker where to set the information
	 * @param ar the current annotators references (can be null).
	 */
	public void readTerminology (TermTag marker,
		AnnotatorsRef ar)
	{
		AnnotatorsRef objAr = readAnnotatorsRef(false, ar);
		marker.setAnnotatorRef(objAr==null ? ar : objAr); // Inherits

		// Term confidence
		String value;
		if ( (value = reader.getAttributeValue(Const.NS_ITSXLF, TERMCONFIDENCE)) != null ) {
			marker.setTermConfidence(Double.parseDouble(value));
		}
	}
	
	/**
	 * Tries to resolve any un-resolved reference to ITS stand-off group.
	 * @param parent the object where the attribute(s) referencing the stand-off group is.
	 */
	public void fetchUnresolvedITSGroups (IWithITSAttributes parent) {
		if ( !parent.hasITSItem() ) return;
		ITSItems items = parent.getITSItems();

		// Check for Provenance
		IITSItem item = items.get(Provenance.class);
		if (( item != null ) && item.hasUnresolvedGroup() ) {
			Provenance prov = (Provenance)item;
			String ref = prov.getUnresolvedGroupRef();
			DataCategoryGroup<?> group = findAndMoveReference(ref, Provenances.class.getName());
			if ( group == null ) {
				throw new XLIFFReaderException(String.format(
					"No stand-off group for provenanceRecordsRef='%s' found at the expected location.", ref));
			}
			prov.setUnresolvedGroupRef(null);
			items.remove(prov);
			items.add(group);
		}
		
		// Check for Localization Quality Issue
		item = items.get(LocQualityIssue.class);
		if (( item != null ) && item.hasUnresolvedGroup() ) {
			LocQualityIssue lqi = (LocQualityIssue)item;
			String ref = lqi.getUnresolvedGroupRef();
			DataCategoryGroup<?> group = findAndMoveReference(ref, LocQualityIssues.class.getName());
			if ( group == null ) {
				throw new XLIFFReaderException(String.format(
					"No stand-off group for locQualityIssuesRef='%s' found at the expected location.", ref));
			}
			lqi.setUnresolvedGroupRef(null);
			items.remove(lqi);
			items.add(group);
		}
	}
	
	/**
	 * Reads the attributes for the Localization Quality Issue data category.
	 * If a reference to a stand-off group is found, the group is expected to be on the unit: it is 
	 * fetched and moved from the unit and expected to be attached to the caller.
	 * @param local true to read the attribute in the local namespace, false to use the ITS namespace.
	 * @return the data category object, or null if the attributes are not present.
	 */
	private IITSItem readLQI (boolean local,
		AnnotatorsRef ar)
	{
		LocQualityIssue lqi = new LocQualityIssue();
		lqi.setAnnotatorRef(ar);
		
		boolean hasData = false;
		String ns = (local ? "" : Const.NS_ITS);
		
		String value = reader.getAttributeValue(ns, LOCQUALITYISSUEENABLED);
		if ( canBeYesOrNo(LOCQUALITYISSUEENABLED, value) ) {
			lqi.setEnabled(value.equals(Const.VALUE_YES));
			hasData = true;
		}

		if ( (value = reader.getAttributeValue(ns, LOCQUALITYISSUETYPE)) != null ) {
			lqi.setType(value);
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(ns, LOCQUALITYISSUECOMMENT)) != null ) {
			lqi.setComment(value);
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(ns, LOCQUALITYISSUEPROFILEREF)) != null ) {
			lqi.setProfileRef(value);
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(ns, LOCQUALITYISSUESEVERITY)) != null ) {
			lqi.setSeverity(Double.parseDouble(value));
			hasData = true;
		}
		
		String ref = reader.getAttributeValue(ns, LOCQUALITYISSUESREF);
		
		AnnotatorsRef localAR = readAnnotatorsRef(local, ar);
		if ( localAR != null ) lqi.setAnnotatorRef(localAR);

		if ( hasData ) {
			if ( ref != null ) {
				throw new XLIFFReaderException("You cannot have locQualityIssuesRef with other LQI attributes.");
			}
			if (( lqi.getComment() == null ) && ( lqi.getType() == null )) {
				throw new XLIFFReaderException("The locQualityIssue element must have at least a comment or a type.");
			}
			return lqi;
		}

		// Only ref
		if ( ref != null ) {
			// Reference should be in the groupHolder object
			// But if the element being read is the same, the stand-off group has not been read yet
			// basically: if the element is not mrk: we have an unresolved group reference
			if ( !reader.getName().getLocalPart().equals("mrk") ) {
				lqi.setUnresolvedGroupRef(ref);
				return lqi;
			}
			// Else: we can fetch
			@SuppressWarnings("unchecked")
			DataCategoryGroup<LocQualityIssue> group = (DataCategoryGroup<LocQualityIssue>)findAndMoveReference(
				ref, LocQualityIssues.class.getName());
			if ( group == null ) {
				throw new XLIFFReaderException(String.format(
					"No stand-off group for locQualityIssuesRef='%s' found at the expected location.", ref));
			}
			return group;
		}
		return null;
	}

	/**
	 * Reads the annotatorsRef attribute from the current element.
	 * @param local true to use the local namespace, false to use ITS prefix.
	 * @param parentAR optional parent values to inherit (can be null).
	 * @return the {@link AnnotatorsRef} object read, or null if none was present.
	 */
	public AnnotatorsRef readAnnotatorsRef (boolean local,
		AnnotatorsRef parentAR)
	{
		String ns = (local ? "" : Const.NS_ITS);
		String values = reader.getAttributeValue(ns, ANNOTATORSREF);
		if ( values != null ) {
			AnnotatorsRef ar = new AnnotatorsRef(parentAR);
			ar.read(values);
			return ar;
		}
		return null;
	}
	
	private IITSItem readProvenance (boolean local,
		AnnotatorsRef ar)
	{
		Provenance prov = new Provenance();
		prov.setAnnotatorRef(ar);
		boolean hasData = false;
		String ns = (local ? "" : Const.NS_ITS);
		
		String[] res = getProvenanceFeature(PROVTOOL, ns);
		if ( res != null ) {
			if ( res[0] != null ) prov.setTool(res[0]);
			else prov.setToolRef(res[1]);
			hasData = true;
		}

		res = getProvenanceFeature(PROVORG, ns);
		if ( res != null ) {
			if ( res[0] != null ) prov.setOrg(res[0]);
			else prov.setOrgRef(res[1]);
			hasData = true;
		}

		res = getProvenanceFeature(PROVPERSON, ns);
		if ( res != null ) {
			if ( res[0] != null ) prov.setPerson(res[0]);
			else prov.setPersonRef(res[1]);
			hasData = true;
		}

		res = getProvenanceFeature(PROVREVTOOL, ns);
		if ( res != null ) {
			if ( res[0] != null ) prov.setRevTool(res[0]);
			else prov.setRevToolRef(res[1]);
			hasData = true;
		}

		res = getProvenanceFeature(PROVREVORG, ns);
		if ( res != null ) {
			if ( res[0] != null ) prov.setRevOrg(res[0]);
			else prov.setRevOrgRef(res[1]);
			hasData = true;
		}

		res = getProvenanceFeature(PROVREVPERSON, ns);
		if ( res != null ) {
			if ( res[0] != null ) prov.setRevPerson(res[0]);
			else prov.setRevPersonRef(res[1]);
			hasData = true;
		}

		String value = reader.getAttributeValue(ns, PROVREF);
		if ( value != null ) {
			prov.setProvRef(value);
			hasData = true;
		}

		String ref = reader.getAttributeValue(ns, PROVENANCERECORDSREF);

		AnnotatorsRef localAR = readAnnotatorsRef(local, ar);
		if ( localAR != null ) prov.setAnnotatorRef(localAR);

		if ( hasData ) {
			if ( ref != null ) {
				throw new XLIFFReaderException("You cannot have provenanceRecordsRef with other Provenance attributes.");
			}
			return prov;
		}
		// Only ref
		if ( ref != null ) {
			// Reference should be in the groupHolder object
			// But if the element being read is the same, the stand-off group has not been read yet
			// basically: if the element is not mrk: we have an unresolved group reference
			if ( !reader.getName().getLocalPart().equals("mrk") ) {
				prov.setUnresolvedGroupRef(ref);
				return prov;
			}
			// Else: we can fetch
			@SuppressWarnings("unchecked")
			DataCategoryGroup<Provenance> group = (DataCategoryGroup<Provenance>)findAndMoveReference(
				ref, Provenances.class.getName());
			if ( group == null ) {
				throw new XLIFFReaderException(String.format(
					"No stand-off group for provenanceRecordsRef='%s' found at the expected location.", ref));
			}
			return group;
		}
		return null;
	}
	
	private String[] getProvenanceFeature (String baseName,
		String ns)
	{
		String[] res = new String[2];
		String value = reader.getAttributeValue(ns, baseName);
		String ref = reader.getAttributeValue(ns, baseName+"Ref");
		if ( value != null ) {
			if ( ref != null ) {
				throw new XLIFFReaderException(String.format("You cannot specific '%s' and '%s at the samw time.",
					baseName, baseName+"Ref"));
			}
			res[0] = value;
		}
		else if ( ref != null ) {
			res[1] = ref;
		}
		else {
			return null; // No entry
		}
		return res;
	}
		
	private MTConfidence readMtConfidence (AnnotatorsRef ar) {
		String value;
		if ( (value = reader.getAttributeValue(Const.NS_ITS, MTCONFIDENCE)) != null ) {
			MTConfidence item = new MTConfidence();
			item.setMtConfidence(Double.parseDouble(value));
			item.setAnnotatorRef(ar);
			item.validate();
			return item;
		}
		return null;
	}

	private Domain readDomain (AnnotatorsRef ar) {
		String value;
		if ( (value = reader.getAttributeValue(Const.NS_ITSXLF, DOMAINS)) != null ) {
			Domain item = new Domain(value);
			item.setAnnotatorRef(ar);
			item.validate();
			return item;
		}
		return null;
	}

	private TextAnalysis readTextAnalysis (AnnotatorsRef ar) {
		TextAnalysis item = new TextAnalysis();
		item.setAnnotatorRef(ar);
		boolean hasData = false;
		
		String value;
		if ( (value = reader.getAttributeValue(Const.NS_ITS, TACONFIDENCE)) != null ) {
			item.setTaConfidence(Double.parseDouble(value));
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(Const.NS_ITS, TACLASSREF)) != null ) {
			item.setTaClassRef(value);
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(Const.NS_ITS, TASOURCE)) != null ) {
			item.setTaSource(value);
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(Const.NS_ITS, TAIDENT)) != null ) {
			item.setTaIdent(value);
			hasData = true;
		}
		
		if ( (value = reader.getAttributeValue(Const.NS_ITS, TAIDENTREF)) != null ) {
			item.setTaIdentRef(value);
			hasData = true;
		}
		
		if ( hasData ) {
			item.validate();
			return item;
		}
		return null;
	}

	private boolean canBeYesOrNo (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if ( value.isEmpty() || ( !value.equals(Const.VALUE_YES) && !value.equals(Const.VALUE_NO) )) {
			throw new XLIFFReaderException(String.format("Invalid attribute value for '%s' (must be '%s' or '%s')",
				name, Const.VALUE_YES, Const.VALUE_NO));
		}
		return true;
	}

	/**
	 * Finds the group for a given reference, remove that group from the parent and return it.
	 * @param ref the reference to find.
	 * @param className name of the class the reference should be (if null: no check is done).
	 * @return the group or null if not found.
	 */
	private DataCategoryGroup<?> findAndMoveReference (String ref,
		String className)
	{
		if ( !groupHolder.hasITSGroup() ) return null;
		// The stand-off element is expected to be in the unit
		// so the URI should start with '#'
		if ( ref.charAt(0) != '#' ) {
			throw new XLIFFReaderException(String.format("Missing '#' in the reference '%s'", ref));
		}
		// And ends with 'its=<id>'
		int pos = ref.lastIndexOf("its=");
		if ( pos == -1 ) {
			throw new XLIFFReaderException(String.format("Invalid fragment identifier syntax (missing ITS prefix) '%s'", ref));
		}
		//TODO: we should perform a true validation of the fragment identifier
		String id = ref.substring(pos+4);
		
		for ( DataCategoryGroup<?> group : groupHolder.getITSGroups() ) {
			if ( group.getGroupId().equals(id) ) {
				if ( className != null ) {
					if ( !className.equals(group.getClass().getName()) ) {
						throw new XLIFFReaderException(String.format("The reference '%s' was found, but for '%s', not '%s'",
							ref, group.getClass().getName(), className));
					}
				}
				// Remove the group from the list
				groupHolder.getITSGroups().remove(group);
				// Return it
				return group;
			}
		}
		return null;
	}

}
