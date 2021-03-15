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

package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Code to parse the _rels files present in Office OpenXML documents.
 */
class Relationships {
	static final String UNEXPECTED_NUMBER_OF_RELATIONSHIPS = "Unexpected number of relationships";
	static final String ROOT_RELS_PART_NAME = "_rels/.rels";

	private static final String RELATIONSHIPS = "Relationships";
	private static final String RELATIONSHIP = "Relationship";
	private static final QName ID_ATTR = new QName("Id");
	private static final QName TARGET_ATTR = new QName("Target");
	private static final QName TYPE_ATTR = new QName("Type");
	private static final QName TARGET_MODE = new QName("TargetMode");

	static class Rel {
		String target;
		String targetMode;
		String id;
		String type;
		Rel(String target, String targetMode, String type, String id) {
			this.target = target;
			this.targetMode = targetMode;
			this.id = id;
			this.type = type;
		}
		@Override
		public boolean equals(Object o) {
			if (o == this) return true;
			if (!(o instanceof Rel)) return false;
			Rel r = (Rel)o;
			return Objects.equals(target, r.target) &&
				   Objects.equals(targetMode, r.targetMode) &&
				   Objects.equals(id, r.id) &&
				   Objects.equals(type, r.type);
		}
		@Override
		public int hashCode() {
			return Objects.hash(target, targetMode, id, type);
		}
	}
	
	private final XMLInputFactory factory;
	private Map<String, Rel> relsById = new HashMap<>();
	private Map<String, List<Rel>> relsByType = new HashMap<>();
	private String targetBase;
	private QName relationshipTagName;

	public Relationships(XMLInputFactory factory) {
		this.factory = factory;
	}

	static List<String> mapRelsToTargets(List<Relationships.Rel> rels) {
		if (rels == null || rels.isEmpty()) {
			throw new OkapiBadFilterInputException(UNEXPECTED_NUMBER_OF_RELATIONSHIPS);
		}

		List<String> targets = new ArrayList<>(rels.size());

		for (Relationships.Rel rel : rels) {
			targets.add(rel.target);
		}

		return targets;
	}

	private void addRelationship(String id, String type, String target, String targetMode) {
		String normalizedTarget = target;
		if (!"External".equals(targetMode)) {
			normalizedTarget = normalizeTarget(target);
		}
		Rel rel = new Rel(normalizedTarget, targetMode, type, id);
		List<Rel> rels = relsByType.get(type);
		if (rels == null) {
			rels = new ArrayList<>();
			relsByType.put(type, rels);
		}
		rels.add(rel);
		relsById.put(id, rel);
	}
	
	Rel getRelById(String id) {
		return relsById.get(id);
	}

	List<Rel> getRelByType(String typeURI) {
		return relsByType.get(typeURI);
	}

	boolean hasRelType(String typeUri) {
		return relsByType.containsKey(typeUri);
	}

	/**
	 * Load the relationships from a given .rels part.  Because the "target" values 
	 * in a relationships file are relative to the .rels that defines them, we must
	 * also know the name of the rels part itself in order to resolve the targets into
	 * canonical/absolute part names.
	 *  
	 * @param relsPartName relationships part path 
	 * @param reader the {@link Reader} to read from
	 * @throws XMLStreamException if any error is encountered while parsing the XML
	 */
	void parseFromXML(String relsPartName, Reader reader) throws XMLStreamException {
		this.targetBase = findTargetBase(relsPartName);
		
		XMLEventReader eventReader = factory.createXMLEventReader(reader);
		while (eventReader.hasNext()) {
			XMLEvent e = eventReader.nextEvent();
			
			if (e.isStartElement()) {
				StartElement el = e.asStartElement();
				if (el.getName().getLocalPart().equals(RELATIONSHIPS)) {
					qualifyNames(el);
				}
				else if (el.getName().equals(this.relationshipTagName)) {
					Attribute target = el.getAttributeByName(TARGET_ATTR);
					Attribute targetMode = el.getAttributeByName(TARGET_MODE);
					Attribute id = el.getAttributeByName(ID_ATTR);
					Attribute type = el.getAttributeByName(TYPE_ATTR);
					if (target != null && id != null && type != null) {
						addRelationship(id.getValue(), type.getValue(), target.getValue(),
								targetMode != null ? targetMode.getValue() : null);
					}
				}
			}
		}
	}

	private void qualifyNames(final StartElement startElement) {
		this.relationshipTagName = new QName(
			startElement.getName().getNamespaceURI(),
			RELATIONSHIP,
			startElement.getName().getPrefix()
		);
	}

	private String findTargetBase(String relsPartName) {
		if (relsPartName.equals(Relationships.ROOT_RELS_PART_NAME)) {
			return "";
		}
		int i = relsPartName.lastIndexOf("/_rels/");
		if (i == -1) {

			throw new IllegalStateException(
					"Unexpected relationships part path: " + relsPartName);
		}
		return relsPartName.substring(0, i);
	}
	
	// Possible issues with this code: 
	// - only handles leading '../'
	// - doesn't handle './', if that's allowed
	private String normalizeTarget(String target) {
		if (target.startsWith("/")) {
			return target.substring(1);
		}
		String base = targetBase;
		while (target.startsWith("../")) {
			int i = base.lastIndexOf("/");
			if ( i == -1 ) {
				if ( base.isEmpty() ) {
					throw new IllegalStateException(
						String.format("Unable to resolve '%s' against path '%s'.", target, targetBase));
				}
				else { // Root case
					base = "";
				}
			}
			else {
				base = base.substring(0, i);
			}
			target = target.substring(3);
		}
		return base + (base.isEmpty() ? "" : "/") + target;
	}
	
}
