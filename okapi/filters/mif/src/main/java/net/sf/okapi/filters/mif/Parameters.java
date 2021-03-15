/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.filters.mif;

import net.sf.okapi.common.ISimplifierRulesParameters;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.core.simplifierrules.ParseException;
import net.sf.okapi.core.simplifierrules.SimplifierRules;

public class Parameters extends StringParameters implements ISimplifierRulesParameters {

	private static final String EXTRACTBODYPAGES = "extractBodyPages";
	private static final String EXTRACTREFERENCEPAGES = "extractReferencePages";
	private static final String EXTRACTMASTERPAGES = "extractMasterPages";
	private static final String EXTRACTHIDDENPAGES = "extractHiddenPages";
	private static final String USECODEFINDER = "useCodeFinder";
	private static final String CODEFINDERRULES = "codeFinderRules";
	private static final String EXTRACTVARIABLES = "extractVariables";
	private static final String EXTRACTINDEXMARKERS = "extractIndexMarkers";
	private static final String EXTRACTLINKS = "extractLinks";
	private static final String EXTRACT_PGF_NUM_FORMATS_INLINE = "extractPgfNumFormatsInline";
	private static final String EXTRACT_REFERENCE_FORMATS = "extractReferenceFormats";
	private static final String EXTRACT_HARD_RETURNS_AS_TEXT = "extractHardReturnsAsText";
	
	private InlineCodeFinder codeFinder; // Initialized in reset()

	public Parameters () {
		super();
	}
	
	public boolean getUseCodeFinder () {
		return getBoolean(USECODEFINDER);
	}

	public void setUseCodeFinder (boolean useCodeFinder) {
		setBoolean(USECODEFINDER, useCodeFinder);
	}

	public InlineCodeFinder getCodeFinder () {
		return codeFinder;
	}

	public String getCodeFinderData () {
		return codeFinder.toString();
	}

	public void setCodeFinderData (String data) {
		codeFinder.fromString(data);
	}
	
	public boolean getExtractReferencePages () {
		return getBoolean(EXTRACTREFERENCEPAGES);
	}
	
	public void setExtractReferencePages (boolean extractReferencePages) {
		setBoolean(EXTRACTREFERENCEPAGES, extractReferencePages);
	}

	public boolean getExtractMasterPages () {
		return getBoolean(EXTRACTMASTERPAGES);
	}
	
	public void setExtractMasterPages (boolean extractMasterPages) {
		setBoolean(EXTRACTMASTERPAGES, extractMasterPages);
	}
	
	public boolean getExtractHiddenPages () {
		return getBoolean(EXTRACTHIDDENPAGES);
	}

	public void setExtractHiddenPages (boolean extractHiddenPages) {
		setBoolean(EXTRACTHIDDENPAGES, extractHiddenPages);
	}

	public boolean getExtractBodyPages () {
		return getBoolean(EXTRACTBODYPAGES);
	}

	public void setExtractBodyPages (boolean extractBodyPages) {
		setBoolean(EXTRACTBODYPAGES, extractBodyPages);
	}

	public boolean getExtractVariables () {
		return getBoolean(EXTRACTVARIABLES);
	}
	
	public void setExtractVariables (boolean extractVariables) {
		setBoolean(EXTRACTVARIABLES, extractVariables);
	}
	
	public boolean getExtractIndexMarkers () {
		return getBoolean(EXTRACTINDEXMARKERS);
	}
	
	public void setExtractIndexMarkers (boolean extractIndexMarkers) {
		setBoolean(EXTRACTINDEXMARKERS, extractIndexMarkers);
	}
	
	public boolean getExtractLinks () {
		return getBoolean(EXTRACTLINKS);
	}
	
	public void setExtractLinks (boolean extractLinks) {
		setBoolean(EXTRACTLINKS, extractLinks);
	}

	public boolean getExtractPgfNumFormatsInline() {
		return getBoolean(EXTRACT_PGF_NUM_FORMATS_INLINE);
	}

	public void setExtractPgfNumFormatsInline(boolean extractPgfNumFormatsInline) {
		setBoolean(EXTRACT_PGF_NUM_FORMATS_INLINE, extractPgfNumFormatsInline);
	}

	public boolean getExtractReferenceFormats() {
		return getBoolean(EXTRACT_REFERENCE_FORMATS);
	}

	public void setExtractReferenceFormats(boolean extractReferenceFormats) {
		setBoolean(EXTRACT_REFERENCE_FORMATS, extractReferenceFormats);
	}

	public boolean getExtractHardReturnsAsText() {
		return getBoolean(EXTRACT_HARD_RETURNS_AS_TEXT);
	}

	public void setExtractHardReturnsAsText(boolean extractHardReturnsAsText) {
		setBoolean(EXTRACT_HARD_RETURNS_AS_TEXT, extractHardReturnsAsText);
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
	
	@Override
	public void reset () {
		super.reset();
		setExtractBodyPages(true);
		setExtractMasterPages(true);
		setExtractReferencePages(true);
		setExtractHiddenPages(true);
		setExtractVariables(true);
		setExtractIndexMarkers(true);
		setExtractLinks(false);
		setExtractPgfNumFormatsInline(false);
		setExtractReferenceFormats(false);
		setExtractHardReturnsAsText(true);
		setUseCodeFinder(true);
		
		codeFinder = new InlineCodeFinder();
		codeFinder.setSample("H: •\\\\t<n\\><n+\\><n=1\\><a\\><a+\\><a=1\\><A\\><A+\\><A=1\\><r" +
			"\\><r+\\><r=1\\><R\\><R+\\><R=1\\><$volnum\\><$chapnum\\><$sectionnum" +
			"\\><$subsectionnum\\>< \\><zenkaku a\\><zenkaku a+\\><zenkaku a=1\\><zenkaku " +
			"A\\><zenkaku A+\\><zenkaku A=1\\><zenkaku n\\><zenkaku n+\\><zenkaku n=1\\><kanji " +
			"kazu\\><kanji kazu+\\><kanji kazu=1\\><kanji n\\><kanji n+\\><kanji " +
			"n=1\\><daiji\\><daiji+\\><daiji=1\\><hira iroha\\><hira iroha+\\><hira " +
			"iroha=1\\><kata iroha\\><kata iroha+\\><kata iroha=1\\><hira gojuon\\><hira " +
			"gojuon+\\><hira gojuon=1\\><kata gojuon\\><kata gojuon+\\><kata " +
			"gojuon=1\\><full-width a\\><full-width a+\\><full-width a=1\\><full-width " +
			"A\\><full-width A+\\><full-width A=1\\><full-width n\\><full-width n+\\><full-width " +
			"n=1\\><chinese n\\><chinese n+\\><chinese n=1\\><Indic n\\><Indic n=1\\><Indic " +
			"n+\\><Farsi n\\><Farsi n=1\\><Farsi n+\\><Farsi a\\><Farsi a=1\\><Farsi a+\\><Hebrew" +
			" n\\><Hebrew n=1\\><Hebrew n+\\><Hebrew a\\><Hebrew a=1\\><Hebrew a+\\><Abjad " +
			"n\\><Abjad n=1\\><Abjad n+\\><Alif Ba Ta n\\><Alif Ba Ta n=1\\><Alif Ba Ta " +
			"n+\\><Thai n\\><Thai n=1\\><Thai n+\\><Thai a\\><Thai a=1\\><Thai a+\\> text " +
			"<$varName\\> text <$varName[PgfTag]\\> <Default ¶ Font\\>");
		codeFinder.setUseAllRulesWhenTesting(true);
		codeFinder.addRule("^[A-Z]{1}:");
		codeFinder.addRule("•");
		codeFinder.addRule("\\\\t");
		codeFinder.addRule("<[naArR ]{1}[+]*\\>");
		codeFinder.addRule("<[naArR]{1}=[0-9]+\\>");
		codeFinder.addRule("<\\$.*?>");
		codeFinder.addRule("<Default ¶ Font\\>");
		codeFinder.addRule("<(zenkaku|kanji|full-width|chinese|Indic|Farsi|Hebrew|Abjad|Alif Ba Ta|Thai) [naA]{1}[+]*\\>");
		codeFinder.addRule("<(zenkaku|kanji|full-width|chinese|Indic|Farsi|Hebrew|Abjad|Alif Ba Ta|Thai) [naA]{1}=[0-9]+\\>");
		codeFinder.addRule("<(kanji kazu|daiji|hira iroha|kata iroha|hira gojuon|kata gojuon)[+]*\\>");
		codeFinder.addRule("<(kanji kazu|daiji|hira iroha|kata iroha|hira gojuon|kata gojuon)=[0-9]+\\>");
		setSimplifierRules(null);
	}

	@Override
	public void fromString (String data) {
		super.fromString(data);
		codeFinder.fromString(buffer.getGroup(CODEFINDERRULES, ""));
	}
	
	@Override
	public String toString () {
		buffer.setGroup(CODEFINDERRULES, codeFinder.toString());
		return super.toString();
	}

}
