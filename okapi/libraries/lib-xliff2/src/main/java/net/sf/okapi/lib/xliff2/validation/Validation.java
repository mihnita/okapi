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

package net.sf.okapi.lib.xliff2.validation;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.lib.xliff2.core.BaseList;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.validation.Rule.Type;

/**
 * Represents the &lt;validation> element of the 
 * <a href='http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#validation_module'>Validation module</a>.
 */
public class Validation extends BaseList<Rule> implements IWithExtAttributes {

	private ExtAttributes xattrs;
	private int declarationCount;

	/**
	 * Creates an empty {@link Validation} object.
	 */
	public Validation () {
		// Nothing to set
	}
	
	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 * @param markAsInherited true to mark all rules in the clone as inherited, false to leave them as they are.
	 */
	public Validation (Validation original,
		boolean markAsInherited)
	{
		super(original);
		// Fields specific to this class
		if ( original.hasExtAttribute() ) {
			xattrs = new ExtAttributes(original.xattrs);
		}
		// if need marker rules as inherited
		for ( Rule rule : this ) {
			rule.setInherited(true);
		}
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

	/**
	 * Indicates if this list of rules has at least one that is not inherited.
	 * @return true if there is one or more non=inherited rule, false otherwise.
	 */
	public boolean hasNonInheritedRule () {
		for ( Rule rule : this ) {
			if ( !rule.isInherited() ) return true;
		}
		return false;
	}

	/**
	 * Prepare the rules for performing the validation tasks.
	 * You must call this method before calling {@link #processRules(Unit, String)}.
	 */
	public void prepare () {
		for ( Rule rule : this ) {
			rule.prepare();
		}
	}

	/**
	 * Applies the rules of this object on a given unit.
	 * You must call {@link #prepare()} before calling this method (if a rule has changed
	 * since the last call).
	 * @param unit the unit to validate.
	 * @param fileId the id of the file of the unit.
	 * @return null if no rule was processed or a list of issues that may be empty.
	 */
	public List<Issue> processRules (Unit unit,
		String fileId)
	{
		List<Issue> issues = new ArrayList<>();
		// Validation rules apply to the full content of the unit
		String target = unit.getPlainText(true, false);
		String source = null; // If needed only
		String unitId = unit.getId();
		int ruleCount = 0;

		// Process the rules
		for ( Rule rule : this ) {
			
			if ( !rule.isEnabled() ) continue;
			if ( rule.getType() == Type.CUSTOM ) continue;
			ruleCount++;
			
			// Else: apply the rule
			String effectiveText = applyOptions(target, rule);
			
			String tmp;
			switch ( rule.getType() ) {
			case ENDSWITH:
				if ( !effectiveText.endsWith(rule.getEffectiveData()) ) {
					// Not ending the target
					issues.add(new Issue(fileId, unitId, "endsWith",
						String.format("Target does not ends with '%s'", rule.getEffectiveData()),
						rule.getDisplay()));
					continue;
				}
				break;
			
			case ISNOTPRESENT:
				if (effectiveText.contains(rule.getEffectiveData())) {
					// Present at least once in target
					issues.add(new Issue(fileId, unitId, "isNotPresent",
						String.format("The text '%s' is present in the target", rule.getEffectiveData()),
						rule.getDisplay()));
					continue;
				}
				break;
			
			case ISPRESENT:
				int count = 0;
				tmp = effectiveText;
				int n = tmp.indexOf(rule.getEffectiveData());
				while ( n > -1 ) {
					tmp = tmp.replace(rule.getEffectiveData(), "");
					count++;
					n = tmp.indexOf(rule.getEffectiveData());
				}
				if ( rule.getExistsInSource() ) {
					if ( source == null ) {
						source = unit.getPlainText(false, false);
					}
					String src = applyOptions(source, rule);
					if (!src.contains(rule.getEffectiveData())) {
						// Not present in source
						issues.add(new Issue(fileId, unitId, "isPresent-NotInSource",
							String.format("The text '%s' is not present in the source", rule.getEffectiveData()),
							rule.getDisplay()));
						continue;
					}
				}
				if ( rule.getOccurs() == 0 ) {
					if ( count < 1 ) {
						// Not present at least 1 time in target
						issues.add(new Issue(fileId, unitId, "isPresent-NotInTarget",
							String.format("The text '%s' is not present in the target", rule.getEffectiveData()),
							rule.getDisplay()));
						continue;
					}
				}
				else {
					if ( rule.getOccurs() != count ) {
						// Not present exactly N times in target
						issues.add(new Issue(fileId, unitId, "isPresent-WrongCount",
							String.format("Occurences of text '%s' the target: %d, expected: %d",
								rule.getEffectiveData(), count, rule.getOccurs()), rule.getDisplay()));
						continue;
					}
				}
				break;
			
			case STARTSWITH:
				if ( !effectiveText.startsWith(rule.getEffectiveData()) ) {
					// Not starting the target
					issues.add(new Issue(fileId, unitId, "startsWith",
						String.format("Target does not starts with '%s'.", rule.getEffectiveData()),
						rule.getDisplay()));
					continue;
				}
				break;
				
			case CUSTOM:
				// Do nothing
				break;
			}
		}
		
		// Return null or the array
		if ( ruleCount == 0 ) return null; // No rule processed
		return issues; // May be empty if no issue was detected
	}
	
	private String applyOptions (String text,
		Rule rule)
	{
		String effectiveText = text;
		if ( !rule.isCaseSensitive() ) effectiveText = effectiveText.toLowerCase();
		switch ( rule.getNormalization() ) {
		case NFC:
			effectiveText = Normalizer.normalize(effectiveText, Form.NFC);
			break;
		case NFD:
			effectiveText = Normalizer.normalize(effectiveText, Form.NFD);
			break;
		case NONE:
			// Do nothing
			break;
		}
		return effectiveText;
	}
	
	public int getDeclarationCount () {
		return declarationCount;
	}

	public void addDeclaration () {
		this.declarationCount++;
	}

}
