/*===========================================================================
  Copyright (C) 2013-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.reader;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.ExtAttribute;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.ExtChildType;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.IExtChild;
import net.sf.okapi.lib.xliff2.core.IWithExtElements;
import net.sf.okapi.lib.xliff2.core.IWithNotes;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.Tags;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.glossary.GlossEntry;
import net.sf.okapi.lib.xliff2.glossary.Translation;
import net.sf.okapi.lib.xliff2.matches.Match;

/**
 * Represents the context for URI fragment identifier resolution in XLIFF. 
 */
public class URIContext implements Cloneable {

	private String fileId;
	private String groupId;
	private String unitId;

	@Override
	public URIContext clone () {
		URIContext tmp = new URIContext();
		tmp.setFileId(fileId);
		tmp.setGroupId(groupId);
		tmp.setUnitId(unitId);
		return tmp;
	}
	
	/**
	 * Gets the id of the file selector.
	 * @return the id of the file selector (can be null).
	 */
	public String getFileId () {
		return fileId;
	}
	
	/**
	 * Sets the the id of the file selector.
	 * @param fileId the new id of the file selector (can be null).
	 */
	public void setFileId (String fileId) {
		this.fileId = fileId;
	}
	
	/**
	 * Gets the id of the group selector.
	 * @return the id of the group selector (can be null).
	 */
	public String getGroupId () {
		return groupId;
	}
	
	/**
	 * Sets the id of the group selector. 
	 * @param groupId the new id of the group selector (can be null).
	 */
	public void setGroupId (String groupId) {
		this.groupId = groupId;
	}

	/**
	 * Gets the id of the unit selector.
	 * @return the id of the unit selector (can be null).
	 */
	public String getUnitId () {
		return unitId;
	}
	
	/**
	 * Sets the id of the unit selector.
	 * @param unitId the new id of the unit selector (can be null).
	 */
	public void setUnitId (String unitId) {
		this.unitId = unitId;
	}

	public Object matches (Event event,
		URIParser up)
	{
		String scope = up.getScope();
		if ( scope.isEmpty() ) return null;
		char refType = up.getRefType();
		Object obj = null;
		
		for ( int i=0; i<scope.length(); i++ ) {
			switch ( scope.charAt(i) ) {
			case 'f':
				if ( !up.getFileId().equals(fileId) ) return null;
				if ( refType == 'f' ) return event.getResource();
				else if ( event.isMidFile() && ( up.getRefContainer()=='f' )) {
					if ( refType == 'n' ) {
						obj = searchNotes(event.getMidFileData(), up.getNoteId());
						if ( obj != null ) return obj;
					}
					else if ( refType == 'x' ) {
						obj = searchExtensions(event.getMidFileData(), up.getExtensionInfo());
						if ( obj != null ) return obj;
					}
				}
				break;
			case 'g':
				if ( !up.getGroupId().equals(groupId) ) return null;
				if ( refType == 'g' ) return event.getResource();
				else if (( refType == 'n' ) && ( up.getRefContainer()=='g' )) {
					obj = searchNotes(event.getStartGroupData(), up.getNoteId());
					if ( obj != null ) return obj;
				}
				else if (( refType == 'x' ) && ( up.getRefContainer()=='g' )) {
					obj = searchExtensions(event.getStartGroupData(), up.getExtensionInfo());
					if ( obj != null ) return obj;
				}
				break;
			case 'u':
				if ( !up.getUnitId().equals(unitId) ) return null;
				switch ( refType ) {
				case 'u':
					return event.getResource();
				case 's':
					obj = searchInlineSource(event.getUnit(), up.getSourceInlineId());
					if ( obj != null ) return obj;
					break;
				case 't':
					obj = searchInlineTarget(event.getUnit(), up.getTargetInlineId());
					if ( obj != null ) return obj;
					break;
				case 'n':
					if ( up.getRefContainer()=='u' ) {
						obj = searchNotes(event.getUnit(), up.getNoteId());
						if ( obj != null ) return obj;
					}
					break;
				case 'x':
					if ( up.getRefContainer()=='u' ) {
						obj = searchExtensions(event.getUnit(), up.getExtensionInfo());
						if ( obj != null ) return obj;
					}
					break;
				case 'd':
					obj = searchData(event.getUnit(), up.getDataId());
					if ( obj != null ) return obj;
					break;
				}
				break;
			}
		}
		return null;
	}
	
	/**
	 * Searches an {@link IWithNotes} object for a note with a given id.
	 * @param parent the object where to search.
	 * @param id the id of the note to search for.
	 * @return the note if found or null if not found.
	 */
	static public Note searchNotes (IWithNotes parent,
		String id)
	{
		if ( parent == null ) return null;
		for ( int i=0; i<parent.getNoteCount(); i++ ) {
			Note note = parent.getNotes().get(i);
			if ( id.equals(note.getId()) ) {
				return note;
			}
		}
		return null;
	}
	
	/**
	 * Searches a source inline code, segment or ignorable in a unit.
	 * @param unit the unit where to search.
	 * @param id the id of the object to search for.
	 * @return the object if found or null if not found.
	 */
	static public Object searchInlineSource (Unit unit,
		String id)
	{
		// Look in segment and ignorable
		for ( Part part : unit ) {
			if ( id.equals(part.getId()) ) {
				if ( part.isSegment() ) return part;
				else return part;
			}
		}
		// Then look in inline codes
		Tags markers = unit.getStore().getSourceTags();
		for ( Tag m : markers ) {
			if ( m.getId().equals(id) ) {
				if ( m.isMarker() ) return m;
				else return m;
			}
		}
		return null;
	}

	/**
	 * Searches a target inline code in a unit.
	 * @param unit the unit where to search.
	 * @param id the id of the object to search for.
	 * @return the object if found or null if not found.
	 */
	static public Object searchInlineTarget (Unit unit,
		String id)
	{
		// Look in inline codes
		Tags markers = unit.getStore().getTargetTags();
		for ( Tag m : markers ) {
			if ( m.getId().equals(id) ) {
				if ( m.isMarker() ) return m;
				else return m;
			}
		}
		return null;
	}
	
	/**
	 * Searches recursively a {@link Unit} object for a module or an extension object with a given id.
	 * @param unit the unit where to search.
	 * @param extensionInfo the information for the object to search for.
	 * @return the object if found or null if not found.
	 */
	static public Object searchUnit (Unit unit,
		SimpleEntry<String, List<String>> extensionInfo)
	{
		Object obj;
		// Search the Translation Candidates module
		if ( unit.hasMatch() ) {
			for ( Match match : unit.getMatches() ) {
				// Check Translation Candidates namespace
				for ( String searchNs : extensionInfo.getValue() ) {
					if ( searchNs.equals(Const.NS_XLIFF_MATCHES20) ) {
						if ( Util.compareAllowingNull(match.getId(), extensionInfo.getKey()) == 0 ) {
							return match;
						}
					}
				}
				// Or extensions
				obj = searchExtensions(match, extensionInfo);
				if ( obj != null ) return obj;
			}
		}
		// Search the Glossary module
		if ( unit.hasGlossEntry() ) {
			for ( GlossEntry entry : unit.getGlossary() ) {
				// Check the Glossary namespace
				for ( String searchNs : extensionInfo.getValue() ) {
					if ( searchNs.equals(Const.NS_XLIFF_GLOSSARY20) ) {
						// Check glossEntry
						if ( Util.compareAllowingNull(entry.getId(), extensionInfo.getKey()) == 0 ) {
							return entry;
						}
						// Check translation objects
						for ( Translation trans : entry ) {
							if ( Util.compareAllowingNull(trans.getId(), extensionInfo.getKey()) == 0 ) {
								return trans;
							}
						}
					}
				}
				// Or extensions
				obj = searchExtensions(entry, extensionInfo);
				if ( obj != null ) return obj;
			}
		}
//		// Search the ITS data
//		if ( unit.hasITSGroup() ) {
//			for ( DataCategoryGroup<?> dcg : unit.getITSGroups() ) {
//				for ( String searchNs : extensionInfo.getValue() ) {
//					if ( searchNs.equals(Const.NS_ITS) ) {
//						if ( dcg.getGroupId().equals(extensionInfo.getKey()) ) {
//							return dcg;
//						}
//					}
//				}
//			}
//		}
		return searchExtensions(unit, extensionInfo);
	}

	/**
	 * Searches recursively an {@link IWithExtElements} object for an extension object with a given id.
	 * @param parent the object where to search.
	 * @param extensionInfo the information for the object to search for.
	 * @return the object if found or null if not found.
	 */
	static public Object searchExtensions (IWithExtElements parent,
		SimpleEntry<String, List<String>> extensionInfo)
	{
		// Else: search just the extensions
		ExtElements elems = parent.getExtElements();
		for ( ExtElement elem : elems ) {
			Object obj = searchMatch(elem, extensionInfo);
			if ( obj != null ) return obj; // Found it
		}
		return null;
	}
	
	static public Object searchMatch (ExtElement elem,
		SimpleEntry<String, List<String>> extensionInfo)
	{
		// Search for an id in this element
		for ( String searchNs : extensionInfo.getValue() ) {
			if ( searchNs.equals(elem.getQName().getNamespaceURI()) ) {
				ExtAttributes attrs = elem.getExtAttributes();
				// Try the current namespace
				ExtAttribute attr = attrs.getAttribute("", "id");
				if ( attr != null ) {
					if ( extensionInfo.getKey().equals(attr.getValue()) ) return elem;
				}
				// Try xml:id
				attr = attrs.getAttribute(Const.NS_XML, "id");
				if ( attr != null ) {
					if ( extensionInfo.getKey().equals(attr.getValue()) ) return elem;
				}
			}
		}
		// Then move to children
		for ( IExtChild child : elem.getChildren() ) {
			if ( child.getType() == ExtChildType.ELEMENT ) {
				Object obj = searchMatch((ExtElement)child, extensionInfo);
				if ( obj != null ) return obj; // Found it
			}
		}
		return null;
	}

	/**
	 * Searches an original data object in a given unit.
	 * @param unit the unit where to search.
	 * @param dataId the id of the data to search for.
	 * @return the object if found or null if not found.
	 */
	static public Object searchData (Unit unit,
		String dataId)
	{
		Tags markers = unit.getStore().getSourceTags();
		for ( Tag m : markers ) {
			if ( m.isMarker() ) continue;
			CTag cm = (CTag)m;
			if ( cm.getDataRef().equals(dataId) ) {
				return cm;
			}
		}
		markers = unit.getStore().getTargetTags();
		for ( Tag m : markers ) {
			if ( m.isMarker() ) continue;
			CTag cm = (CTag)m;
			if ( cm.getDataRef().equals(dataId) ) {
				return cm;
			}
		}
		return null;
	}

}
