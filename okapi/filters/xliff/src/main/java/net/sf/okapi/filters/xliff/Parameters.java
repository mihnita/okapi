/*===========================================================================
  Copyright (C) 2008-2014 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.filters.xliff;

import java.util.Arrays;
import java.util.Optional;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ISimplifierRulesParameters;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.encoder.XMLEncoder;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.core.simplifierrules.ParseException;
import net.sf.okapi.core.simplifierrules.SimplifierRules;


@EditorFor(Parameters.class)
public class Parameters extends StringParameters implements ISimplifierRulesParameters {


	public enum SegmentationType {
		/**
		 * Use segmented target if the original trans-unit was segmented.
		 */
		ORIGINAL(0),

		/**
		 * Always use segmented target regardless of the original trans-unit.
		 */
		SEGMENTED(1),

		/**
		 * Never use segmentation in the output.
		 */
		NOTSEGMENTED(2),

		/**
		 * Only use segments if we have more than one segment in the
		 * translation.
		 */
		ASNEEDED(3);

		private final int value;

		SegmentationType(int value) {
			this.value = value;
		}
		public int getValue() {
			return value;
		}
		public static Optional<SegmentationType> byValue(int value) {
			return Arrays.stream(values()).filter(type -> type.value == value).findFirst();
		}
	}

	/**
	 * Empty CDATA subfilter configuration.
	 */
	static final String EMPTY_CDATA_SUBFILTER_CONFIGURATION = "";

	public static final int TARGETSTATEMODE_IGNORE = 0;
	public static final int TARGETSTATEMODE_EXTRACT = 1;
	public static final int TARGETSTATEMODE_DONOTEXTRACT = 2;

	public static final String ADDALTTRANS = "addAltTrans";
	public static final String ADDALTTRANSGMODE = "addAltTransGMode";
	public static final String EDITALTTRANS = "editAltTrans";
	
	private static final String USECUSTOMPARSER = "useCustomParser";
	private static final String FACTORYCLASS = "factoryClass";
	private static final String FALLBACKTOID = "fallbackToID";
	private static final String ADDTARGETLANGUAGE = "addTargetLanguage";
	private static final String OVERRIDETARGETLANGUAGE = "overrideTargetLanguage";
	private static final String ALLOWEMPTYTARGETS = "allowEmptyTargets";
	private static final String OUTPUTSEGMENTATIONTYPE = "outputSegmentationType";
	private static final String IGNOREINPUTSEGMENTATION = "ignoreInputSegmentation";
	private static final String INCLUDEEXTENSIONS = "includeExtensions";
	private static final String TARGETSTATEMODE = "targetStateMode";
	private static final String TARGETSTATEVALUE = "targetStateValue";
	private static final String INCLUDEITS = "includeIts";
	private static final String BALANCECODES = "balanceCodes";
	private static final String ALWAYSUSESEGSOURCE = "alwaysUseSegSource";
	private static final String PRESERVESPACEBYDEFAULT = "preserveSpaceByDefault";
	private static final String USESDLXLIFFWRITER = "useSdlXliffWriter";
	// SDLSEGLOCKEDVALUE could be a boolean but maybe there are other values besides true or false?
	private static final String SDLSEGLOCKEDVALUE = "sdlSegLockedValue";
	private static final String SDLSEGCONFVALUE = "sdlSegConfValue";
	private static final String SDLSEGORIGINVALUE = "sdlSegOriginValue";
	private static final String SKIPNOMRKSEGSOURCE = "skipNoMrkSegSource";
	private static final String SUBASTEXTUNIT = "subAsTextUnit";

	// Use the Segment-level properties for working with SDL properties
	// If false: use the TextContainer properties (SDL ones and STATE)
	private static final String USESEGSFORSDLPROPS = "useSegsForSdlProps";

	private static final String USEIWSXLIFFWRITER = "useIwsXliffWriter";
	private static final String IWSBLOCKFINISHED = "iwsBlockFinished";
	private static final String IWSTRANSSTATUSVALUE = "iwsTransStatusValue";
	private static final String IWSREMOVETMORIGIN = "iwsRemoveTmOrigin"; // Remove `tm_origin` attribute so any change is picked up by IWS
	private static final String IWSTRANSTYPEVALUE = "iwsTransTypeValue";
	private static final String IWSBLOCKLOCKSTATUS = "iwsBlockLockStatus"; // Use lock_status attribute.
	private static final String IWSBLOCKTMSCORE = "iwsBlockTmScore"; // Use tm_score attribute e.g. tm_score="100.00"
	private static final String IWSBLOCKTMSCOREVALUE = "iwsBlockTmScoreValue"; // TM score in the IWS metadata.
	private static final String IWSINCLUDEMULTIPLEEXACT = "iwsIncludeMultipleExact"; // Use multiple_exact attribute.
	private static final String IWSBLOCKMULTIPLEEXACT = "iwsBlockMultipleExact"; // Use multiple_exact attribute.

	private static final String INLINECDATA = "inlineCdata";

	private static final String USECODEFINDER = "useCodeFinder";
	private static final String CODEFINDERRULES = "codeFinderRules";
	/**
	 * A CDATA subfilter.
	 */
	private static final String CDATA_SUBFILTER = "cdataSubfilter";

	private InlineCodeFinder codeFinder; // initialized in reset()

	public Parameters () {
		super();
	}

	public boolean getUseCustomParser() {
		return getBoolean(USECUSTOMPARSER);
	}

	public void setUseCustomParser(boolean useCustomParser) {
		setBoolean(USECUSTOMPARSER, useCustomParser);
	}

	public String getFactoryClass() {
		return getString(FACTORYCLASS);
	}

	public void setFactoryClass(String factoryClass) {
		setString(FACTORYCLASS, factoryClass);
	}

	public boolean getSubAsTextUnit() {
		return getBoolean(SUBASTEXTUNIT);
	}

	public void setSubAsTextUnit(boolean subAsTextUnit) {
		setBoolean(SUBASTEXTUNIT, subAsTextUnit);
	}

	public boolean getEscapeGT () {
		return getBoolean(XMLEncoder.ESCAPEGT);
	}

	public void setEscapeGT (boolean escapeGT) {
		setBoolean(XMLEncoder.ESCAPEGT, escapeGT);
	}

	public boolean getFallbackToID() {
		return getBoolean(FALLBACKTOID);
	}

	public void setFallbackToID(boolean fallbackToID) {
		setBoolean(FALLBACKTOID, fallbackToID);
	}

	public boolean getAddTargetLanguage () {
		return getBoolean(ADDTARGETLANGUAGE);
	}

	public void setAddTargetLanguage (boolean addTargetLanguage) {
		setBoolean(ADDTARGETLANGUAGE, addTargetLanguage);
	}
	
	public boolean getOverrideTargetLanguage () {
		return getBoolean(OVERRIDETARGETLANGUAGE);
	}

	public void setOverrideTargetLanguage (boolean overrideTargetLanguage) {
		setBoolean(OVERRIDETARGETLANGUAGE, overrideTargetLanguage);
	}
	
	public SegmentationType getOutputSegmentationType () {
		return SegmentationType.byValue(getInteger(OUTPUTSEGMENTATIONTYPE)).orElse(SegmentationType.ORIGINAL);
	}
	
	public void setOutputSegmentationType (SegmentationType segmentationType) {
		setInteger(OUTPUTSEGMENTATIONTYPE, segmentationType.value);
	}

	public boolean getIgnoreInputSegmentation () {
		return getBoolean(IGNOREINPUTSEGMENTATION);
	}
	
	public void setIgnoreInputSegmentation (boolean ignoreInputSegmentation) {
		setBoolean(IGNOREINPUTSEGMENTATION, ignoreInputSegmentation);
	}

	public boolean getAddAltTrans () {
		return getBoolean(ADDALTTRANS);
	}
	
	public void setAddAltTrans (boolean addAltTrans) {
		setBoolean(ADDALTTRANS, addAltTrans);
	}

	public boolean getAddAltTransGMode () {
		return getBoolean(ADDALTTRANSGMODE);
	}
	
	public void setAddAltTransGMode (boolean addAltTransGMode) {
		setBoolean(ADDALTTRANSGMODE, addAltTransGMode);
	}

	public boolean getEditAltTrans () {
		return getBoolean(EDITALTTRANS);
	}
	
	public void setEditAltTrans (boolean editAltTrans) {
		setBoolean(EDITALTTRANS, editAltTrans);
	}

	public boolean getIncludeExtensions () {
		return getBoolean(INCLUDEEXTENSIONS);
	}
	
	public void setIncludeExtensions (boolean includeExtensions) {
		setBoolean(INCLUDEEXTENSIONS, includeExtensions);
	}
	
	public boolean getIncludeIts () {
		return getBoolean(INCLUDEITS);
	}
	
	public void setIncludeIts (boolean includeIts) {
		setBoolean(INCLUDEITS, includeIts);
	}
	
	public boolean getBalanceCodes () {
		return getBoolean(BALANCECODES);
	}
	
	public void setBalanceCodes (boolean balanceCodes) {
		setBoolean(BALANCECODES, balanceCodes);
	}
	
	public boolean getAllowEmptyTargets () {
		return getBoolean(ALLOWEMPTYTARGETS);
	}
	
	public void setAllowEmptyTargets (boolean allowEmptyTargets) {
		setBoolean(ALLOWEMPTYTARGETS, allowEmptyTargets);
	}
	
	public int getTargetStateMode () {
		return getInteger(TARGETSTATEMODE);
	}
	
	public void setTargetStateMode (int targetStateMode) {
		setInteger(TARGETSTATEMODE, targetStateMode);
	}

	public String getTargetStateValue () {
		return getString(TARGETSTATEVALUE);
	}
	
	public void setTargetStateValue (String targetStateValue) {
		setString(TARGETSTATEVALUE, targetStateValue);
	}
	
	public boolean getQuoteModeDefined () {
		return getBoolean(XMLEncoder.QUOTEMODEDEFINED);
	}
	
	public boolean isAlwaysUseSegSource() {
		return getBoolean(ALWAYSUSESEGSOURCE);
	}
	
	public void setAlwaysUseSegSource(boolean alwaysUSeSegSource) {
		setBoolean(ALWAYSUSESEGSOURCE, alwaysUSeSegSource);
	}

	public boolean isPreserveSpaceByDefault() {
		return getBoolean(PRESERVESPACEBYDEFAULT);
	}

	public void setPreserveSpaceByDefault(boolean preserveSpaceByDefault) {
		setBoolean(PRESERVESPACEBYDEFAULT, preserveSpaceByDefault);
	}

	// Not normally writable
	protected void setQuoteModeDefined(boolean defined) {
		setBoolean(XMLEncoder.QUOTEMODEDEFINED, defined);
	}
	
	public int getQuoteMode () {
		return getInteger(XMLEncoder.QUOTEMODE);
	}

	// Not normally writable
	protected void setQuoteMode(int quoteMode) {
		setInteger(XMLEncoder.QUOTEMODE, quoteMode);
	}
	
	public boolean isUseSdlXliffWriter() {
		return getBoolean(USESDLXLIFFWRITER);
	}
	
	public void setUseSdlXliffWriter(boolean useSdlXliffWriter) {
		setBoolean(USESDLXLIFFWRITER, useSdlXliffWriter);
	}
	
	public String getSdlSegLockedValue () {
		return getString(SDLSEGLOCKEDVALUE);
	}
	
	public void setSdlSegLockedValue (String sdlSegLockedvalue) {
		setString(SDLSEGLOCKEDVALUE, sdlSegLockedvalue);
	}
	
	public String getSdlSegConfValue () {
		return getString(SDLSEGCONFVALUE);
	}
	
	public void setSdlSegConfValue (String sdlSegConfvalue) {
		setString(SDLSEGCONFVALUE, sdlSegConfvalue);
	}
	
	public String getSdlSegOriginValue () {
		return getString(SDLSEGORIGINVALUE);
	}
	
	public void setSdlSegOriginValue (String sdlSegOriginvalue) {
		setString(SDLSEGORIGINVALUE, sdlSegOriginvalue);
	}

	public boolean getUseSegsForSdlProps () {
		return getBoolean(USESEGSFORSDLPROPS);
	}
	
	public void setUseSegsForSdlProps (boolean useSegsForSdlProps) {
		setBoolean(USESEGSFORSDLPROPS, useSegsForSdlProps);
	}

	public boolean isInlineCdata() {
		return getBoolean(INLINECDATA);
	}

	public void setInlineCdata(boolean inlineCdata) {
		setBoolean(INLINECDATA, inlineCdata);
	}

	public boolean getSkipNoMrkSegSource() {
		return getBoolean(SKIPNOMRKSEGSOURCE);
	}

	public void setSkipNoMrkSegSource(boolean skipNoMrkSegSource) {
		setBoolean(SKIPNOMRKSEGSOURCE, skipNoMrkSegSource);
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

	public boolean isUseIwsXliffWriter() {
		return getBoolean(USEIWSXLIFFWRITER);
	}

	public void setUseIwsXliffWriter(boolean useIwsXliffWriter) {
		setBoolean(USEIWSXLIFFWRITER, useIwsXliffWriter);
	}

	public boolean isIwsBlockFinished() {
		return getBoolean(IWSBLOCKFINISHED);
	}

	public void setIwsBlockFinished(boolean iwsBlockFinished) {
		setBoolean(IWSBLOCKFINISHED, iwsBlockFinished);
	}

	public String getIwsTransStatusValue() {
		return getString(IWSTRANSSTATUSVALUE);
	}

	public void setIwsTransStatusValue(String iwsTransStatusValue) {
		setString(IWSTRANSSTATUSVALUE, iwsTransStatusValue);
	}

	public boolean isIwsRemoveTmOrigin() {
		return getBoolean(IWSREMOVETMORIGIN);
	}

	public void setIwsRemoveTmOrigin(boolean iwsRemoveTmOrigin) {
		setBoolean(IWSREMOVETMORIGIN, iwsRemoveTmOrigin);
	}


	public String getIwsTransTypeValue() {
		return getString(IWSTRANSTYPEVALUE);
	}

	public void setIwsTransTypeValue(String iwsTransTypeValue) {
		setString(IWSTRANSTYPEVALUE, iwsTransTypeValue);
	}

	public void setIwsBlockLockStatus(boolean iwsBlockLockStatus)
	{
		setBoolean(IWSBLOCKLOCKSTATUS, iwsBlockLockStatus);
	}

	public boolean isIwsBlockLockStatus()
	{
		return getBoolean(IWSBLOCKLOCKSTATUS);
	}

	public void setIwsBlockTmScore(boolean iwsBlockTmScore)
	{
		setBoolean(IWSBLOCKTMSCORE, iwsBlockTmScore);
	}

	public boolean isIwsBlockTmScore()
	{
		return getBoolean(IWSBLOCKTMSCORE);
	}

	public void setIwsBlockTmScoreValue(String iwsBlockTmScoreValue)
	{
		setString(IWSBLOCKTMSCOREVALUE, iwsBlockTmScoreValue);
	}

	public String getIwsBlockTmScoreValue()
	{
		return getString(IWSBLOCKTMSCOREVALUE);
	}

	public void setIwsBlockMultipleExact(boolean iwsBlockMultipleExact)
	{
		setBoolean(IWSBLOCKMULTIPLEEXACT, iwsBlockMultipleExact);
	}

	public boolean isIwsBlockMultipleExact()
	{
		return getBoolean(IWSBLOCKMULTIPLEEXACT);
	}

	public void setIwsIncludeMultipleExact(boolean iwsIncludeMultipleExact)
	{
		setBoolean(IWSINCLUDEMULTIPLEEXACT, iwsIncludeMultipleExact);
	}

	public boolean isIwsIncludeMultipleExact()
	{
		return getBoolean(IWSINCLUDEMULTIPLEEXACT);
	}

	public String getCdataSubfilter() {
		return getString(CDATA_SUBFILTER);
	}

	public void setCdataSubfilter(String subfilter) {
		setString(CDATA_SUBFILTER, subfilter);
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

	public void reset () {
		super.reset();
		setUseCustomParser(true);
		setFactoryClass("com.ctc.wstx.stax.WstxInputFactory"); // Woodstox XML parser
		setFallbackToID(false);
		setEscapeGT(false);
		setAddTargetLanguage(true);
		setOverrideTargetLanguage(false);
		setOutputSegmentationType(SegmentationType.ORIGINAL);
		setIgnoreInputSegmentation(false);
		setAddAltTrans(false);
		setAddAltTransGMode(true);
		setEditAltTrans(false);
		setIncludeExtensions(true);
		setIncludeIts(true);
		setBalanceCodes(true);
		setAllowEmptyTargets(false);
		setTargetStateMode(TARGETSTATEMODE_IGNORE);
		setTargetStateValue("needs-translation");
		setAlwaysUseSegSource(false);
		
		setQuoteModeDefined(true);
		setQuoteMode(0); // no double or single quotes escaped
		setUseSdlXliffWriter(false);
		setPreserveSpaceByDefault(false);
		setSdlSegLockedValue(null); // default is use the original value
		setSdlSegConfValue(null);
		setSdlSegOriginValue(null);
		
		setUseSegsForSdlProps(false); // Use TextContainer for backward compatibility

		setUseIwsXliffWriter(false);
		setIwsBlockFinished(true);
		setIwsTransStatusValue("finished");
		setIwsTransTypeValue("manual_translation");
		setIwsRemoveTmOrigin(false);
		setIwsBlockLockStatus(false);
		setIwsBlockTmScore(false);
		setIwsBlockTmScoreValue("100.00");
		setIwsIncludeMultipleExact(false);
		setIwsBlockMultipleExact(false);
		setInlineCdata(false);

		setSkipNoMrkSegSource(false);
		setSimplifierRules(null);

		setUseCodeFinder(false);
		codeFinder = new InlineCodeFinder();
		codeFinder.setSample("&name; <tag></at><tag/> <tag attr='val'> </tag=\"val\">");
		codeFinder.setUseAllRulesWhenTesting(true);
		codeFinder.addRule("</?([A-Z0-9a-z]*)\\b[^>]*>");

		setSubAsTextUnit(false);
	}

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
