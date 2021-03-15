/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.glossary;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtElements;

/**
 * Represents the &lt;glossEntry&gt; element of the 
 * <a href='http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#glossary-module'>Glossary module</a>.
 */
public class GlossEntry implements IWithExtAttributes, IWithExtElements, Iterable<Translation> {

	private String id;
	private String ref;
	private Term term;
	private Definition definition;
	private List<Translation> translations;
	private ExtElements xelems;
	private ExtAttributes xattrs;
	
	/**
	 * Creates a new {@link GlossEntry} object.
	 */
	public GlossEntry () {
		term = new Term("");
		translations = new ArrayList<>(1);
		// definition can be null
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public GlossEntry (GlossEntry original) {
		this.id = original.id;
		this.ref = original.ref;
		this.term = new Term(original.term);
		this.definition = new Definition(original.definition);
		
		translations = new ArrayList<>(original.translations.size());
		for ( Translation trans : original.translations ) {
			translations.add(new Translation(trans));
		}
		if ( original.hasExtAttribute() ) {
			xattrs = new ExtAttributes(original.xattrs);
		}
		if ( original.hasExtElements() ) {
			xelems = new ExtElements(original.xelems);
		}
	}

	/**
	 * Gets the id for this entry.
	 * @return the id for this entry (can be null).
	 */
	public String getId () {
		return id;
	}

	/**
	 * Sets the id for this entry.
	 * @param id the new id for this entry (can be null).
	 */
	public void setId (String id) {
		this.id = id;
	}

	/**
	 * Gets the reference for this entry.
	 * @return the reference for this entry (can be null).
	 */
	public String getRef () {
		return ref;
	}

	/**
	 * Sets the reference for this entry.
	 * @param ref the new reference for this entry (can be null).
	 */
	public void setRef (String ref) {
		this.ref = ref;
	}

	/**
	 * Gets the term for this entry.
	 * @return the term for this entry.
	 */
	public Term getTerm () {
		return term;
	}

	/**
	 * Sets a new term for this entry.
	 * @param term the new term for this entry (must not be null).
	 * @return the new {@link Term} object.
	 */
	public Term setTerm (Term term) {
		this.term = term;
		return this.term;
	}

	/**
	 * Gets the definition for this entry.
	 * @return the definition for this entry (can be null).
	 */
	public Definition getDefinition () {
		return definition;
	}

	/**
	 * Sets a new definition for this entry.
	 * @param definition the new definition for this entry (can be null).
	 * @return the new {@link Definition} object.
	 */
	public Definition setDefinition (Definition definition) {
		this.definition = definition;
		return this.definition;
	}

	/**
	 * Adds a translation to this entry.
	 * @param text the text of the translation to add.
	 * @return the new {@link Translation} object.
	 */
	public Translation addTranslation (String text) {
		Translation trans = new Translation(text);
		getTranslations().add(trans);
		return trans;
	}

	@Override
	public ExtElements getExtElements () {
		if ( xelems == null ) xelems = new ExtElements();
		return xelems;
	}

	@Override
	public boolean hasExtElements () {
		if ( xelems == null ) return false;
		return (xelems.size() > 0);
	}

	@Override
	public ExtElements setExtElements (ExtElements elements) {
		this.xelems = elements;
		return getExtElements();
	}

	@Override
	public void setExtAttributes (ExtAttributes attributes) {
		this.xattrs = attributes;
	}

	@Override
	public ExtAttributes getExtAttributes () {
		if ( xattrs == null ) {
			xattrs = new ExtAttributes();
		}
		return xattrs;
	}

	@Override
	public boolean hasExtAttribute () {
		if ( xattrs == null ) return false;
		return !xattrs.isEmpty();
	}

	@Override
	public String getExtAttributeValue (String namespaceURI,
		String localName)
	{
		if ( xattrs == null ) return null;
		return xattrs.getAttributeValue(namespaceURI, localName);
	}

	@Override
	public Iterator<Translation> iterator () {
		return translations.iterator();
	}

	/**
	 * Gets the list of the {@link Translation} objects for this entry.
	 * @return the list of the {@link Translation} objects for this entry (never null).
	 */
	public List<Translation> getTranslations () {
		return translations;
	}

}
