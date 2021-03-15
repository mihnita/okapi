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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.InheritedData;
import net.sf.okapi.lib.xliff2.core.Tags;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Unit;

/**
 * Provides utility methods to output ITS attributes and stand-off elements.
 * This assumes the ITS namespace uses "its" as its prefix. 
 */
public class ITSWriter {

	/**
	 * Annotates a given fragment with an ITS item.
	 * @param fragment the fragment to annotate.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param item the ITS item to set (it can be a single instance or a group).
	 * @return the ITS item set.
	 */
	public static IITSItem annotate (Fragment fragment,
		int start,
		int end,
		IITSItem item)
	{
		MTag am = new MTag(fragment.getStore().suggestId(false), "its:any");
		am.getITSItems().add(item);
		fragment.annotate(start, end, am);
		return item;
	}
	
	/**
	 * Tries to reuse an existing annotation to add an ITS item.
	 * If no existing annotation is found for the given span and type, one is created.
	 * @param fragment the fragment to annotate.
	 * @param start the start position (in the coded text)
	 * @param end the position just after the last character of the span (in the coded text).
	 * You can use -1 to indicate the end of the fragment.
	 * @param item the ITS item to set (it can be a single instance or a group).
	 * @param matchingType the type of the existing annotation that can be reused. 
	 * Use null to reuse any type.
	 * @return the opening marker for the annotation.
	 */
	public static MTag annotate (Fragment fragment,
		int start,
		int end,
		IITSItem item,
		String matchingType)
	{
		MTag am = fragment.getOrCreateMarker(start, end, matchingType, "its:any");
		am.getITSItems().add(item);
		return am;
	}

	/**
	 * Adds the namespaces and version information for supporting ITS mapping in an XLIFF 2 document.
	 * @param sxd the {@link StartXliffData} object where to add the mapping.
	 */
	public static void addDeclaration (StartXliffData sxd) {
		sxd.setNamespace(Const.PREFIX_ITS, Const.NS_ITS);
		sxd.setNamespace(Const.PREFIX_ITSXLF, Const.NS_ITSXLF);
		sxd.getExtAttributes().setAttribute(Const.NS_ITS, "version", "2.0");
	}
	
	/**
	 * Outputs any stand-off elements used in the markers for the given unit.
	 * @param indent the base indentation to use.
	 * @param lb the line-break to use.
	 * @param unit the unit to process.
	 * @return the formatted output of the stand-off elements or an empty string if
	 * there is no element to output.
	 */
	public String outputStandOffElements (String indent,
		String lb,
		Unit unit)
	{
		List<DataCategoryGroup<?>> list = null;
		// Check if any of the annotation has ITS data categories requiring a stand-off output
		if ( unit.getStore().hasSourceTag() ) {
			list = checkForGroups(list, unit.getStore().getSourceTags());
		}
		if ( unit.getStore().hasTargetTag() ) {
			list = checkForGroups(list, unit.getStore().getTargetTags());
		}
		// check on the unit itself
		list = checkForGroups(list, unit);
		
		// If there are no stand-off group to output we are done
		if ( list == null ) return "";
		
//		AnnotatorsRef ar = context.peek().getAnnotatorsRef();
		
		// Otherwise: output the groups
		StringBuilder out = new StringBuilder();
		for ( DataCategoryGroup<?> group : list ) {
			if ( group instanceof LocQualityIssues ) {
				LocQualityIssues issues = (LocQualityIssues)group;
				out.append(indent+"<its:" + ITSReader.LOCQUALITYISSUES + " xml:id=\"" + issues.getGroupId() + "\">\n");
				for ( LocQualityIssue lqi : issues.getList() ) {
					out.append(indent+" <its:" + ITSReader.LOCQUALITYISSUE);
					out.append(outputLQIAttributes(lqi, false));
					out.append("/>"+lb);
				}
				out.append(indent+"</its:"+ITSReader.LOCQUALITYISSUES+ ">"+lb);
			}
			else if ( group instanceof Provenances ) {
				Provenances recs = (Provenances)group;
				out.append(indent+"<its:" + ITSReader.PROVENANCERECORDS + " xml:id=\"" + recs.getGroupId() + "\">\n");
				for ( Provenance prov : recs.getList() ) {
					out.append(indent+" <its:" + ITSReader.PROVENANCERECORD);
					out.append(outputProvenanceAttributes(prov, false));
					out.append("/>"+lb);
				}
				out.append(indent+"</its:"+ITSReader.PROVENANCERECORDS+ ">"+lb);
			}
		}
		return out.toString();
	}
	
	//FIXME: document this
	public List<SimpleEntry<String, AnnotatorsRef>> createAnnotatorsRefList (Stack<InheritedData> context) {
		List<SimpleEntry<String, AnnotatorsRef>> list = new ArrayList<>(3); // Unlikely to have many nested annotations
		// Use an invalid ID value for the key, so it never clash with real IDs
		if (( context == null ) || context.isEmpty() ) {
			list.add(new SimpleEntry<>("_#_", null)); // Initial parent for out-of-content
		}
		else {
			list.add(new SimpleEntry<>("_#_", context.peek().getAnnotatorsRef()));
		}
		return list;
	}
	
	public AnnotatorsRef createAnnotatorsRef (IWithITSAttributes object) {
		if ( object instanceof TermTag ) {
			AnnotatorsRef ar = new AnnotatorsRef();
			TermTag tm = (TermTag)object;
			ar.set(tm.getDataCategoryName(), tm.getAnnotatorRef());
			return ar;
		}
		// Else: normal object with ITS attributes
		if ( !object.hasITSItem() ) return null;
		AnnotatorsRef ar = new AnnotatorsRef();
		for ( IITSItem item : object.getITSItems() ) {
			ar.set(item);
		}
		return ar;
	}

	//FIXME: document this
	@SuppressWarnings("unchecked")
	public String outputAttributes (IWithITSAttributes object,
		AnnotatorsRef objectAR,
		AnnotatorsRef parentAR)
	{
		// Case of the TermMarker
		if ( object instanceof TermTag ) {
			return outputTerminologyAttributes((TermTag)object, parentAR);
		}
		// First, build the annotatorsRef attribute
		if ( !object.hasITSItem() ) {
			// No DC on this object, but we still need to output the annotator-references
			if ( objectAR == null ) return ""; // Nothing to do
			// Else: output the difference between the object latest annotator-references and its parent's
			return objectAR.printDifferences(parentAR);
		}
		
        if ( objectAR == null ) {
        	if ( parentAR != null ) {
        		// Return the difference between the live data and the parent's annotator-references
        		return parentAR.printDifferences(null);
        	}
        	// Else: Create the set of annotators from the object and output them
        	return createAnnotatorsRef(object).print();
        }

        // Else: compare with the object's context and its parents
        String arOut = objectAR.printDifferences(parentAR);
		if ( !object.hasITSItem() ) {
			return arOut;
		}

		// Then: process each item
		StringBuilder atOut = new StringBuilder();
		// Format and output the items
		for ( IITSItem item : object.getITSItems() ) {
			// LQI reference
			if ( item instanceof LocQualityIssues ) {
				atOut.append(" its:" + ITSReader.LOCQUALITYISSUESREF + "=\"#its="
					+ ((DataCategoryGroup<LocQualityIssue>)item).getGroupId() + "\"");
				continue;
			}
			// Provenance reference
			if ( item instanceof Provenances ) {
				atOut.append(" its:" + ITSReader.PROVENANCERECORDSREF + "=\"#its="
					+ ((DataCategoryGroup<Provenance>)item).getGroupId() + "\"");
				continue;
			}
			// Single LQI instance
			if ( item instanceof LocQualityIssue ) {
				atOut.append(outputLQIAttributes((LocQualityIssue)item, true));
				continue;
			}
			// Single Provenance instance
			if ( item instanceof Provenance ) {
				atOut.append(outputProvenanceAttributes((Provenance)item, true));
				continue;
			}
			// MT Confidence
			if ( item instanceof MTConfidence ) {
				atOut.append(" its:" + ITSReader.MTCONFIDENCE + "=\""
					+ format(((MTConfidence)item).getMtConfidence()) + "\"");
				continue;
			}
			// Text Analysis
			if ( item instanceof TextAnalysis ) {
				atOut.append(outputTextAnalysisAttributes((TextAnalysis)item, true));
				continue;
			}
			// Domain
			if ( item instanceof Domain ) {
				atOut.append(" " + Const.PREFIXCOL_ITSXLF + ITSReader.DOMAINS + "=\""
					+ Util.toXML(((Domain)item).getDomain(), true) + "\"");
				continue;
			}
		}
		
		if ( !arOut.isEmpty() ) {
			atOut.append(arOut);
		}

		return atOut.toString();
	}
	
	private String outputTerminologyAttributes (TermTag marker,
		AnnotatorsRef parentAR)
	{
		StringBuilder out = new StringBuilder();
		if ( marker.getTermConfidence() != null ) {
			out.append(" " + Const.PREFIXCOL_ITSXLF + ITSReader.TERMCONFIDENCE + "=\""
					+ marker.getTermConfidence().toString() + "\"");
		}

		String arValue = marker.getAnnotatorRef();
		if ( arValue != null ) {
			if ( parentAR != null ) {
				if ( arValue.equals(parentAR.get(marker.getDataCategoryName())) ) {
					// Same value: no need to write it
					arValue = null;
				}
			}
		}
		
		// If arValue is not null we need to write it
		if ( arValue != null ) {
			out.append(" its:annotatorsRef=\""+marker.getDataCategoryName()+"|"+arValue+"\"");
		}

		return out.toString();
	}

	private String outputLQIAttributes (LocQualityIssue lqi,
		boolean withPrefix)
	{
		String front = (withPrefix ? " its:" : " ");
		StringBuilder out = new StringBuilder();
		if ( lqi.getType() != null ) {
			out.append(front + ITSReader.LOCQUALITYISSUETYPE+"=\""
				+ lqi.getType() + "\"");
		}
		if ( lqi.getComment() != null ) {
			out.append(front + ITSReader.LOCQUALITYISSUECOMMENT+"=\""
				+ Util.toXML(lqi.getComment(), true) + "\"");
		}
		if ( !lqi.isEnabled() ) {
			out.append(front + ITSReader.LOCQUALITYISSUEENABLED+"=\"no\"");
		}
		if ( lqi.getSeverity() != null ) {
			out.append(front + ITSReader.LOCQUALITYISSUESEVERITY+"=\"" 
				+ format(lqi.getSeverity()) + "\"");
		}
		if ( lqi.getProfileRef() != null ) {
			out.append(front + ITSReader.LOCQUALITYISSUEPROFILEREF+"=\"" 
				+ Util.toXML(lqi.getProfileRef(), true) + "\"");
		}
		return out.toString();
	}
	
	private String outputProvenanceAttributes (Provenance prov,
		boolean withPrefix)
	{
		String front = (withPrefix ? " its:" : " ");
		StringBuilder out = new StringBuilder();
		// tool or toolRef
		if ( prov.getTool() != null ) {
			out.append(front + ITSReader.PROVTOOL+"=\""
				+ Util.toXML(prov.getTool(), true) + "\"");
		}
		else if ( prov.getToolRef() != null ) {
			out.append(front + ITSReader.PROVTOOLREF+"=\""
				+ prov.getToolRef() + "\"");
		}
		// org or orgRef
		if ( prov.getOrg() != null ) {
			out.append(front + ITSReader.PROVORG+"=\""
				+ Util.toXML(prov.getOrg(), true) + "\"");
		}
		else if ( prov.getOrgRef() != null ) {
			out.append(front + ITSReader.PROVORGREF+"=\""
				+ prov.getOrgRef() + "\"");
		}
		// person or personRef
		if ( prov.getPerson() != null ) {
			out.append(front + ITSReader.PROVPERSON+"=\""
				+ Util.toXML(prov.getPerson(), true) + "\"");
		}
		else if ( prov.getPersonRef() != null ) {
			out.append(front + ITSReader.PROVPERSONREF+"=\""
				+ prov.getPersonRef() + "\"");
		}
		// revTool or revToolRef
		if ( prov.getRevTool() != null ) {
			out.append(front + ITSReader.PROVREVTOOL+"=\""
				+ Util.toXML(prov.getRevTool(), true) + "\"");
		}
		else if ( prov.getRevToolRef() != null ) {
			out.append(front + ITSReader.PROVREVTOOLREF+"=\""
				+ prov.getRevToolRef() + "\"");
		}
		// revOrg or revOrgRef
		if ( prov.getRevOrg() != null ) {
			out.append(front + ITSReader.PROVREVORG+"=\""
				+ Util.toXML(prov.getRevOrg(), true) + "\"");
		}
		else if ( prov.getRevOrgRef() != null ) {
			out.append(front + ITSReader.PROVREVORGREF+"=\""
				+ prov.getRevOrgRef() + "\"");
		}
		// revPerson or revPersonRef
		if ( prov.getRevPerson() != null ) {
			out.append(front + ITSReader.PROVREVPERSON+"=\""
				+ Util.toXML(prov.getRevPerson(), true) + "\"");
		}
		else if ( prov.getRevPersonRef() != null ) {
			out.append(front + ITSReader.PROVREVPERSONREF+"=\""
				+ prov.getRevPersonRef() + "\"");
		}
		// provRef
		if ( prov.getProvRef() != null ) {
			out.append(front + ITSReader.PROVREF+"=\""
					+ prov.getProvRef() + "\"");
		}
		return out.toString();
	}
	
	private String outputTextAnalysisAttributes (TextAnalysis ta,
		boolean withPrefix)
	{
		String front = (withPrefix ? " its:" : " ");
		StringBuilder out = new StringBuilder();
		// taClassRef
		if ( ta.getTaClassRef() != null ) {
			out.append(front + ITSReader.TACLASSREF+"=\""
				+ Util.toXML(ta.getTaClassRef(), true) + "\"");
		}
		// taIdentRef
		if ( ta.getTaIdentRef() != null ) {
			out.append(front + ITSReader.TAIDENTREF+"=\""
				+ Util.toXML(ta.getTaIdentRef(), true) + "\"");
		}
		// taSource
		if ( ta.getTaSource() != null ) {
			out.append(front + ITSReader.TASOURCE+"=\""
				+ Util.toXML(ta.getTaSource(), true) + "\"");
		}
		// taIdent
		if ( ta.getTaIdent() != null ) {
			out.append(front + ITSReader.TAIDENT+"=\""
				+ Util.toXML(ta.getTaIdent(), true) + "\"");
		}
		// taConfidence
		if ( ta.getTaConfidence() != null ) {
			out.append(front + ITSReader.TACONFIDENCE + "=\""
				+ format(ta.getTaConfidence()) + "\"");
		}
		
		return out.toString();
	}
	
	private List<DataCategoryGroup<?>> checkForGroups (List<DataCategoryGroup<?>> list,
		Tags markers)
	{
		for ( Tag bm : markers ) {
			if ( bm.isMarker() && ( bm.getTagType() == TagType.OPENING )) {
				if ( !((MTag)bm).hasITSItem() ) continue;
				MTag am = (MTag)bm;
				for ( IITSItem item : am.getITSItems() ) {
					if ( item.isGroup() ) {
						if ( list == null ) list = new ArrayList<>();
						list.add((DataCategoryGroup<?>)item);
					}
				}
			}
		}
		return list;
	}
	
	private List<DataCategoryGroup<?>> checkForGroups (List<DataCategoryGroup<?>> list,
		IWithITSAttributes parent)
	{
		if ( parent.hasITSItem() ) {
			for ( IITSItem item : parent.getITSItems() ) {
				if ( item.isGroup() ) {
					if ( list == null ) list = new ArrayList<>();
					list.add((DataCategoryGroup<?>)item);
				}
			}
		}
		return list;
	}
	
	/**
	 * Formats a double value so only the significant trailing zeros are displayed.
	 * Removes the decimal period if there are no significant decimal digits.
	 * @param value the double value to format (can be null).
	 * @return the formatted value or an empty string.
	 */
	private String format (Double value) {
		if ( value == null ) return "";
		String tmp = String.format((Locale)null, "%f", value);
		// Remove trailing zeros
		while (( tmp.length() > 1 ) && ( tmp.charAt(tmp.length()-1) == '0' )) {
			tmp = tmp.substring(0, tmp.length()-1);
		}
		// Remove ending period if it's the last character
		if ( tmp.charAt(tmp.length()-1) == '.' ) {
			tmp = tmp.substring(0, tmp.length()-1);
		}
		return tmp;
	}
}
