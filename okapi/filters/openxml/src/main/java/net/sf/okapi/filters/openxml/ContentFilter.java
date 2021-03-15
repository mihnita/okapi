/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

import java.net.URL;
import java.util.List;
import java.util.Set;

import net.htmlparser.jericho.EndTag;
import net.htmlparser.jericho.StartTag;
import net.htmlparser.jericho.Tag;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.PropertyTextUnitPlaceholder;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.filters.abstractmarkup.AbstractMarkupFilter;
import net.sf.okapi.filters.abstractmarkup.config.TaggedFilterConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Filters Microsoft Office Word, Excel, and Powerpoint Documents.
 * OpenXML is the format of these documents.
 *
 * <p>Since OpenXML files are Zip files that contain XML documents,
 * <b>OpenXMLFilter</b> handles opening and processing the zip file, and
 * instantiates this filter to process the XML documents.
 *
 * <p>This filter extends AbstractBaseMarkupFilter, which extends
 * AbstractBaseFilter.  It uses the Jericho parser to analyze the
 * XML files.
 *
 * <p>The filter exhibits slightly differnt behavior depending on whether
 * the XML file is Word, Excel, Powerpoint, or a chart in Word.  The
 * tags in these files are configured in yaml configuration files that
 * specify the behavior of the tags.  These configuration files are
 * <ul>
 * <li>wordConfiguration.yml</li>
 * <li>excelConfiguration.yml</li>
 * <li>powerpointConfiguration.yml</li>
 * <li>wordChartConfiguration.yml</li>
 * </ul>
 *
 * In Word and Powerpoint, text is always surrounded by paragraph tags
 * &lt;w:p&gt; or &lt;a:p&gt;, which signal the beginning and end of the text unit
 * for this filter, and are marked as TEXT_UNIT_ELEMENTs in the configuration
 * files.  Inside these are one or more text runs surrounded by &lt;w:r&gt; or &lt;a:r&gt;
 * tags and marked as TEXT_RUN_ELEMENTS by the configuration files.  The text
 * itself occurs between text marker tags &lt;w:t&gt; or &lt;a:t&gt; tags, which are
 * designated TEXT_MARKER_ELEMENTS by the configuration files.  Tags between
 * and including &lt;w:r&gt; and &lt;w:t&gt; (which usually include a &lt;w:rPr&gt; tag sequence
 * for character style) are consolidated into a single MARKER_OPENING code.  Tags
 * between and including &lt;/w:t&gt; and &lt;/w:r&gt;, which sometimes include graphics
 * tags, are consolidated into a single MARKER_CLOSING code.  If there is no
 * text between &lt;w:r&gt; and &lt;/w:r&gt;, a single MARKER_PLACEHOLDER code is created
 * for the text run.  If there is no character style information,
 * &lt;w:r&gt;&lt;w:t&gt;text&lt;/w:t&gt;&lt;/w:r&gt; is not surrounded by MARKER_OPENING or
 * MARKER_CLOSING codes, to simplify things for translators; these are supplied
 * by OpenXMLContentSkeletonWriter during output.  The same is true for text
 * runs marked by &lt;a:r&gt; and &lt;a:t&gt; in Powerpoint files.
 *
 * Excel files are simpler, and only mark text by &lt;v&gt;, &lt;t&gt;, and &lt;text&gt; tags
 * in worksheet, sharedString, and comment files respectively.  These tags
 * work like TEXT_UNIT, TEXT_RUN, and TEXT_MARKER elements combined.
 */
public class ContentFilter extends AbstractMarkupFilter {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private ParseType configurationType;
	//	private Package p=null;
	private ParseType filetype=ParseType.MSWORD; // DWH 4-13-09
	private String sConfigFileName; // DWH 10-15-08
	private StringBuilder sInExclusion = new StringBuilder();
	private boolean bInTextRun = false; // DWH 4-10-09
	private boolean bBetweenTextMarkers=false; // DWH 4-14-09
	private boolean bInSettingsFile = false; // DWH 4-12-10
	private ConditionalParameters filterParams = null;
	private YamlParameters params=null; // DWH 7-16-09
	private TaggedFilterConfiguration config=null; // DWH 7-16-09
	private String partName;
	private String pendingTagName;
	private String pendingTagText;
	private boolean bInPowerpointComment = false;

	public ContentFilter(ConditionalParameters filterParams, String partName) {
		super(); // 1-6-09
		this.filterParams = filterParams;
		this.partName = partName;
		setMimeType(MimeTypeMapper.XML_MIME_TYPE);
		setFilterWriter(createFilterWriter());
	}

	/**
	 * Logs information about the event fir the log level is FINEST.
	 * @param event event to log information about
	 */
	public void displayOneEvent(Event event) // DWH 4-22-09 LOGGER
	{
		Set<String> setter;
		if (LOGGER.isTraceEnabled())
		{
			String etyp=event.getEventType().toString();
			if (event.getEventType() == EventType.TEXT_UNIT) {
				//			assertTrue(event.getResource() instanceof TextUnit);
			} else if (event.getEventType() == EventType.DOCUMENT_PART) {
				//			assertTrue(event.getResource() instanceof DocumentPart);
			} else if (event.getEventType() == EventType.START_GROUP
					|| event.getEventType() == EventType.END_GROUP) {
				//			assertTrue(event.getResource() instanceof StartGroup || event.getResource() instanceof Ending);
			} else if (event.getEventType() == EventType.START_SUBFILTER
					|| event.getEventType() == EventType.END_SUBFILTER) {
				//				assertTrue(event.getResource() instanceof StartSubfilter || event.getResource() instanceof Ending);
			}
			if (etyp.equals("START"))
				LOGGER.trace("\n");
			LOGGER.trace("{}: ", etyp);
			if (event.getResource() != null) {
				LOGGER.trace("({})", event.getResource().getId());
				if (event.getResource() instanceof DocumentPart) {
					setter = ((DocumentPart) event.getResource()).getSourcePropertyNames();
					for(String seti : setter)
						LOGGER.trace(seti);
				} else {
					LOGGER.trace(event.getResource().toString());
				}
				if (event.getResource().getSkeleton() != null) {
					LOGGER.trace("*Skeleton: \n{}", event.getResource().getSkeleton().toString());
				}
			}
		}
	}

	public ParseType getParseType() {
		return filetype;
	}

	/**
	 * Sets the name of the Yaml configuration file for the current file type, reads the file, and sets the parameters.
	 * @param filetype type of XML in the current file
	 */
	public void setUpConfig(ParseType filetype)
	{
		this.filetype = filetype; // DWH 5-13-09
		switch(filetype)
		{
			case MSEXCEL:
				sConfigFileName = "excelConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = ParseType.MSEXCEL;
				break;
			case MSWORDDOCPROPERTIES: // DWH 5-13-09
				sConfigFileName = "wordDocPropertiesConfiguration.yml"; // DWH 5-25-09
				configurationType = ParseType.MSWORDDOCPROPERTIES;
				break;
			case MSPOWERPOINTCOMMENTS:
				sConfigFileName = "powerpointCommentConfiguration.yml";
				break;
			case MSWORD:
			default:
				sConfigFileName = "wordConfiguration.yml"; // DWH 1-5-09 groovy -> yml
				configurationType = ParseType.MSWORD;
				break;
		}
		URL urlConfig = ContentFilter.class.getResource(sConfigFileName); // DWH 3-9-09
		config = new TaggedFilterConfiguration(urlConfig);
//		setDefaultConfig(urlConfig); // DWH 7-16-09 no longer needed; AbstractMarkup now calls getConfig everywhere
		try
		{
			setParameters(new YamlParameters(urlConfig));
			// DWH 3-9-09 it doesn't update automatically from setDefaultConfig 7-16-09 YamlParameters
		}
		catch(Exception e)
		{
			throw new OkapiIOException("Can't read MS Office Filter Configuration File.");
		}
	}

	/**
	 * Handles text.  If in a text run, it ends the text run and
	 * adds the tags that were in it as a single MARKER_OPENING code.
	 * This would correspond to &lt;w:r&gt;...&lt;w:t&gt; in MSWord.  It will
	 * then start a new text run anticipating &lt;/w:t&gt;...&lt;/w:r&gt;.  If
	 * text is found that was not in a text run, i.e. it was not between
	 * text markers, it is not text to be processed by a user, so it
	 * becomes part of a new text run which will become part of a
	 * code.  If the text is not in a text unit, then it is added to a
	 * document part.
	 * @param text the text to be handled
	 */
	@Override
	protected void handleText(CharSequence text) {
		if (text==null) // DWH 4-14-09
			return;
		startDelayedTextUnit();
		String txt=text.toString();
		handleSomeText(txt, isWhiteSpace(text)); // DWH 5-14-09
	}

	private void handleSomeText(String tixt, boolean bWhiteSpace) // DWH 6-25-09 tixt was txt
	{
		String txt=tixt; // DWH 6-25-09 added this so txt can be changed for Excel index to shared strings
		if (getRuleState().isExludedState()) {
			sInExclusion.append(tixt);
			return;
		}
		// check for ignorable whitespace and add it to the skeleton
		// The Jericho html parser always pulls out the largest stretch of text
		// so standalone whitespace should always be ignorable if we are not
		// already processing inline text
//		if (text.isWhiteSpace() && !isInsideTextRun()) {
		if (bWhiteSpace && !isInsideTextRun()) {
			addToDocumentPart(txt);
			return;
		}
		if (canStartNewTextUnit())
		{
			addToDocumentPart(txt);
		}
		else
		{
			if (bInTextRun) // DWH 4-20-09 whole if revised
			{
				if (bBetweenTextMarkers)
				{
					addToTextUnit(txt); // adds the text
					bInTextRun = true;
				}
			}
			else if (bInPowerpointComment) {
				addToTextUnit(txt);
			}
		}
	}

	/**
	 * Handles a start tag.  TEXT_UNIT_ELEMENTs start a new TextUnit.  TEXT_RUN_ELEMENTs
	 * start a new text run.  TEXT_MARKER_ELEMENTS set a flag that any following
	 * text will be between text markers.  ATTRIBUTES_ONLY tags have translatable text
	 * in the attributes, so within a text unit, it is added within a text run; otherwise it
	 * becomes a DocumentPart.
	 * @param startTag the start tag to process
	 */
	@Override
	protected void handleStartTag(StartTag startTag) {
		String sTagName;
		String sTagString;

		// If we were waiting to start a text unit, do so
		startDelayedTextUnit();
		sTagName = startTag.getName(); // DWH 2-26-09
		sTagString = startTag.toString(); // DWH 2-26-09

		if (sTagName.equals("p:text")) {
			bInPowerpointComment = true;
		}
		switch (getConfig().getElementRuleTypeCandidate(sTagName)) {
			// DWH 1-23-09
			case INLINE_ELEMENT:
				addToDocumentPart(sTagString);
				break;

			case ATTRIBUTES_ONLY:
				// we assume we have already ended any (non-complex) TextUnit in
				// the main while loop above
				List<PropertyTextUnitPlaceholder> propertyTextUnitPlaceholders;

				// Excel: Skip sheet names of hidden sheets if hidden sheets in general should be skipped
				if (!filterParams.getTranslateExcelHidden()
						&& partName.equals("xl/workbook.xml")
						&& sTagName.equals("sheet")
						&& "hidden".equals(startTag.getAttributeValue("state"))) {
					addToDocumentPart(sTagString);
					break;
				}

				propertyTextUnitPlaceholders = createPropertyTextUnitPlaceholders(startTag); // 1-29-09
				if (propertyTextUnitPlaceholders != null && !propertyTextUnitPlaceholders.isEmpty()) { // 1-29-09
					startDocumentPart(sTagString, sTagName, propertyTextUnitPlaceholders);
					// DWH 1-29-09
					endDocumentPart();
				} else {
					// no attributes that need processing - just treat as skeleton
					addToDocumentPart(sTagString);
				}
				break;
			case GROUP_ELEMENT:
				if (bInSettingsFile) // DWH 4-12-10 else is for <v:textbox ...> in settings.xml file
					addToDocumentPart(sTagString); // DWH 4-12-10 for <v:textbox ...> in settings.xml file
				break;
			case EXCLUDED_ELEMENT:
				getRuleState().pushExcludedRule(sTagName);
				sInExclusion.append(sTagString);
				break;
			case TEXT_UNIT_ELEMENT:
				// Don't start the text-unit just yet -- it might be empty.  Note that we
				// can't just save the tag because of how Jericho implements its tag data as
				// pointers into the stream -- if we try to hold a Tag object too long, it
				// will become invalid.
				pendingTagName = startTag.getName();
				pendingTagText = startTag.toString();
				break;
			default:
				addToDocumentPart(sTagString); // 1-5-09
				break;
		}
	}
	private void startDelayedTextUnit() {
		if (pendingTagName != null) {
			getRuleState().pushTextUnitRule(pendingTagName);
			startTextUnit(new GenericSkeleton(pendingTagText));
			if (configurationType==ParseType.MSEXCEL ||
					configurationType==ParseType.MSWORDDOCPROPERTIES)
			// DWH 4-16-09 Excel and Word Charts don't have text runs or text markers
			{
				bInTextRun = true;
				bBetweenTextMarkers = true;
			}
			else
			{
				bInTextRun = false;
				bBetweenTextMarkers = false;
			}
			pendingTagName = null;
			pendingTagText = null;
		}
	}

	/**
	 * Handles end tags.  These either add to current text runs
	 * or end text runs or text units as appropriate.
	 * @param endTag the end tag to process
	 */
	@Override
	protected void handleEndTag(EndTag endTag) {
		// if in excluded state everything is skeleton including text
		String sTagName; // DWH 2-26-09
		String sTagString; // DWH 4-14-09
		sTagName = endTag.getName(); // DWH 2-26-09
		sTagString = endTag.toString(); // DWH 2-26-09
		if (getRuleState().isExludedState()) {
			sInExclusion.append(sTagString);
			// process these tag types to update parser state
			switch (getConfig().getElementRuleTypeCandidate(sTagName)) {
				// DWH 1-23-09
				case EXCLUDED_ELEMENT:
					getRuleState().popExcludedIncludedRule();
					break;
			}
			if (sTagName.equals("p:text")) {
				bInPowerpointComment = false;
			}
			if (!getRuleState().isExludedState()) { // we just popped the topmost excluded element
				addToDocumentPart(sInExclusion.toString()); // 1-5-09
				sInExclusion = new StringBuilder();
			}
			return;
		}
		switch (getConfig().getElementRuleTypeCandidate(sTagName)) {
			// DWH 1-23-09
			case INLINE_ELEMENT:
				addToDocumentPart(sTagString); // DWH 5-29-09
				break;
			case TEXT_UNIT_ELEMENT: // $$$
				// If we never started the text unit, flush it all as document part
				if (pendingTagName != null) {
					// XXX Should sanity check that this tag and pending one match
					addToDocumentPart(pendingTagText);
					addToDocumentPart(sTagString);
					pendingTagName = null;
					pendingTagText = null;
				}
				else {
					if (bInTextRun)
					{
						bInTextRun = false;
					} // otherwise this is an illegal element, so just ignore it
					bBetweenTextMarkers = true; // DWH 4-16-09 ???
					try
					{
						getRuleState().popTextUnitRule(); // DWH 6-19-10 could die if not in text unit
					}
					catch(Exception e) {}; // will do its best to recover anyway
					endTextUnit(new GenericSkeleton(sTagString)); // DWH 8-17-09
				}
				break;
			default:
				addToDocumentPart(sTagString); // not in text unit, so add to skeleton
				break;
		}
	}

	/**
	 * Treats XML comments as DocumentParts.
	 * @param tag comment tag
	 */
	@Override
	protected void handleComment(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Treats XML doc type declaratons as DocumentParts.
	 * @param tag doc type declaration tag
	 */
	@Override
	protected void handleDocTypeDeclaration(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Treats XML markup declaratons as DocumentParts.
	 * @param tag markup declaration tag
	 */
	@Override
	protected void handleMarkupDeclaration(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Treats XML processing instructions as DocumentParts.
	 * @param tag processing instruction tag
	 */
	@Override
	protected void handleProcessingInstruction(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Treats XML server common tags as DocumentParts.
	 * @param tag server common tag
	 */
	@Override
	protected void handleServerCommon(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Treats server common escaped tags as DocumentParts.
	 * @param tag server common escaped tag
	 */
	@Override
	protected void handleServerCommonEscaped(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Treats XML declaratons as DocumentParts.
	 * @param tag XML declaration tag
	 */
	@Override
	protected void handleXmlDeclaration(Tag tag) {
		handleDocumentPart(tag);
	}

	/**
	 * Returns name of the filter.
	 * @return name of the filter
	 */
	public String getName() {
		return "OpenXMLContentFilter";
	}
	/**
	 * Normalizes naming of attributes whose values are the
	 * encoding or a language name, so that they can be
	 * automatically changed to the output encoding and output.
	 * Unfortunately, this hard codes the tags to look for.
	 * @param attrName name of the attribute
	 * @param attrValue, value of the attribute
	 * @param tag tag that contains the attribute
	 * @return a normalized name for the attribute
	 */
	@Override
	protected String normalizeAttributeName(String attrName, String attrValue, Tag tag) {
		// normalize values for HTML
		String normalizedName = attrName;
		String tagName; // DWH 2-19-09 */

		// <w:lang w:val="en-US" ...>
		tagName = tag.getName();
		if (tagName.equals("w:lang") || tagName.equals("w:themefontlang")) // DWH 4-3-09 themeFontLang
		{
			StartTag st = (StartTag) tag;
			if (st.getAttributeValue("w:val") != null)
			{
				normalizedName = Property.LANGUAGE;
				return normalizedName;
			}
		}
		return normalizedName;
	}

	public ParseType getConfigurationType()
	{
		return configurationType;
	}
	protected void setBInSettingsFile(boolean bInSettingsFile) // DWH 4-12-10 for <v:textbox
	{
		this.bInSettingsFile = bInSettingsFile;
	}
	protected boolean getBInSettingsFile() // DWH 4-12-10 for <v:textbox
	{
		return bInSettingsFile;
	}

	@Override
	protected TaggedFilterConfiguration getConfig() {
		return config; // this may be bad if AbstractMarkup calls it too soon !!!!
	}

	public YamlParameters getParameters() { // DWH 7-16-09
		return params;
	}

	public void setParameters(IParameters params) { // DWH 7-16-09
		this.params = (YamlParameters)params;
	}

	public ConditionalParameters getFilterParameters() {
		return filterParams;
	}

	@Override
	public String toString() {
		return "OpenXMLContentFilter [" + partName + "]";
	}
}
