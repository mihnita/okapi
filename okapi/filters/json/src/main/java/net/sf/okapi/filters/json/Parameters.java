/*===========================================================================
  Copyright (C) 2009-2013 by the Okapi Framework contributors
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

package net.sf.okapi.filters.json;

import net.sf.okapi.common.ISimplifierRulesParameters;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.core.simplifierrules.ParseException;
import net.sf.okapi.core.simplifierrules.SimplifierRules;

public class Parameters extends StringParameters implements ISimplifierRulesParameters {

	private static final String EXTRACTISOLATEDSTRINGS = "extractIsolatedStrings";
	private static final String EXTRACTALLPAIRS = "extractAllPairs";
	private static final String EXCEPTIONS = "exceptions";
	private static final String USEKEYASNAME = "useKeyAsName";
	private static final String USECODEFINDER = "useCodeFinder";
	private static final String USEFULLKEYPATH = "useFullKeyPath";
	private static final String USELEADINGSLASHONKEYPATH = "useLeadingSlashOnKeyPath";
	private static final String CODEFINDERRULES = "codeFinderRules";
	private static final String SUBFILTER = "subfilter";
	private static final String ESCAPEFORWARDSLASHES = "escapeForwardSlashes";
	private static final String NOTERULES = "noteRules";

	// new rule types supersede and override older exception rules if present
	private static final String EXTRACTIONRULES = "extractionRules";

	// id rules (overrides useKeyAsName) if present
	private static final String IDRULES = "idRules";

	// metadata rules
	private static final String GENERICMETARULES = "genericMetaRules";

	private InlineCodeFinder codeFinder; // Initialized in reset()

	public Parameters() {
		super();
	}

	public boolean getExtractStandalone() {
		return getBoolean(EXTRACTISOLATEDSTRINGS);
	}

	public void setExtractStandalone(boolean extractStandalone) {
		setBoolean(EXTRACTISOLATEDSTRINGS, extractStandalone);
	}

	public boolean getExtractAllPairs() {
		return getBoolean(EXTRACTALLPAIRS);
	}

	public void setExtractAllPairs(boolean extractAllPairs) {
		setBoolean(EXTRACTALLPAIRS, extractAllPairs);
	}

	public String getExceptions() {
		return getString(EXCEPTIONS);
	}

	public void setExceptions(String exceptions) {
		setString(EXCEPTIONS, exceptions);
	}

	public boolean getUseKeyAsName() {
		return getBoolean(USEKEYASNAME);
	}

	public void setUseKeyAsName(boolean useKeyAsName) {
		setBoolean(USEKEYASNAME, useKeyAsName);
	}

	public boolean getUseFullKeyPath() {
		return getBoolean(USEFULLKEYPATH);
	}

	public void setUseFullKeyPath(boolean useFullKeyPath) {
		setBoolean(USEFULLKEYPATH, useFullKeyPath);
	}

	public boolean getUseLeadingSlashOnKeyPath() {
		return getBoolean(USELEADINGSLASHONKEYPATH);
	}

	public void setUseLeadingSlashOnKeyPath(boolean useLeadingSlashOnKeyPath) {
		setBoolean(USELEADINGSLASHONKEYPATH, useLeadingSlashOnKeyPath);
	}

	public boolean getEscapeForwardSlashes() {
		return getBoolean(ESCAPEFORWARDSLASHES);
	}

	public void setEscapeForwardSlashes(boolean escapeForwardSlashes) {
		setBoolean(ESCAPEFORWARDSLASHES, escapeForwardSlashes);
	}

	/**
	 * A regex representing keys whose values should be saved to appear as a
	 * &lt;note&gt; element in XLIFF.
	 * 
	 * @return the regex, or "" (not null) if none.
	 */
	public String getNoteRules() {
		return getString(NOTERULES);
	}

	/**
	 * @see #getNoteRules()
	 * @param noteRules regex, or "". It is trimmed before saving.
	 */
	public void setNoteRules(String noteRules) {
		if (noteRules == null || noteRules.trim().isEmpty()) {
			setString(NOTERULES, "");
		} else {
			setString(NOTERULES, noteRules.trim());
		}
	}

	/**
	 * A regex representing extraction rules (matching keys or key paths values are
	 * extracted)
	 * 
	 * @return the regex, or "" (not null) if none.
	 */
	public String getExtractionRules() {
		return getString(EXTRACTIONRULES);
	}

	/**
	 * @see #getExtractionRules()
	 * @param extractionRules regex, or "". It is trimmed before saving.
	 */
	public void setExtractionRules(String extractionRules) {
		if (extractionRules == null || extractionRules.trim().isEmpty()) {
			setString(EXTRACTIONRULES, "");
		} else {
			setString(EXTRACTIONRULES, extractionRules.trim());
		}
	}

	/**
	 * A regex representing id rules (matching keys or key paths values are used as
	 * TextUnit ids)
	 * 
	 * @return the regex, or "" (not null) if none.
	 */
	public String getIdRules() {
		return getString(IDRULES);
	}

	/**
	 * @see #getIdRules()
	 * @param idRules regex, or "". It is trimmed before saving.
	 */
	public void setIdRules(String idRules) {
		if (idRules == null || idRules.trim().isEmpty()) {
			setString(IDRULES, "");
		} else {
			setString(IDRULES, idRules.trim());
		}
	}

	/**
	 * A regex representing generic metadata rules (matching keys or key paths
	 * values)
	 * 
	 * @return the regex, or "" (not null) if none.
	 */
	public String getGenericMetaRules() {
		return getString(GENERICMETARULES);
	}

	/**
	 * @see #getGenericMetaRules()
	 * @param genericMetaRules regex, or "". It is trimmed before saving.
	 */
	public void setGenericMetaRules(String genericMetaRules) {
		if (genericMetaRules == null || genericMetaRules.trim().isEmpty()) {
			setString(GENERICMETARULES, "");
		} else {
			setString(GENERICMETARULES, genericMetaRules.trim());
		}
	}
	
	public boolean getUseCodeFinder() {
		return getBoolean(USECODEFINDER);
	}

	public void setUseCodeFinder(boolean useCodeFinder) {
		setBoolean(USECODEFINDER, useCodeFinder);
		if (getUseCodeFinder()) {
			setSubfilter("");
		}
	}

	public InlineCodeFinder getCodeFinder() {
		return codeFinder;
	}

	public String getSubfilter() {
		return getString(SUBFILTER);
	}

	public void setSubfilter(String subfilter) {
		setString(SUBFILTER, subfilter);
		if (!"".equals(getSubfilter())) {
			setUseCodeFinder(false);
		}
	}

	public String getCodeFinderData() {
		return codeFinder.toString();
	}

	public void setCodeFinderData(String data) {
		codeFinder.fromString(data);
	}

	public void reset() {
		super.reset();
		setExtractStandalone(false);
		setExtractAllPairs(true);
		setExceptions("");
		setUseKeyAsName(true);
		setUseFullKeyPath(false);
		setUseLeadingSlashOnKeyPath(true);
		setEscapeForwardSlashes(true);
		setUseCodeFinder(false);
		setSubfilter(null);
		codeFinder = new InlineCodeFinder();
		codeFinder.setSample("&name; <tag></at><tag/> <tag attr='val'> </tag=\"val\">");
		codeFinder.setUseAllRulesWhenTesting(true);
		codeFinder.addRule("</?([A-Z0-9a-z]*)\\b[^>]*>");
		setSimplifierRules(null);
		setNoteRules("");
		setExtractionRules("");
		setIdRules("");
		setGenericMetaRules("");
	}

	public void fromString(String data) {
		super.fromString(data);
		codeFinder.fromString(buffer.getGroup(CODEFINDERRULES, ""));
	}

	@Override
	public String toString() {
		buffer.setGroup(CODEFINDERRULES, codeFinder.toString());
		return super.toString();
	}

	@Override
	public String getSimplifierRules() {
		return getString(SIMPLIFIERRULES);
	}

	@Override
	public void setSimplifierRules(String rules) {
		setString(SIMPLIFIERRULES, rules);
	}

	@Override
	public void validateSimplifierRules() throws ParseException {
		SimplifierRules r = new SimplifierRules(getSimplifierRules(), new Code());
		r.parse();
	}
}
