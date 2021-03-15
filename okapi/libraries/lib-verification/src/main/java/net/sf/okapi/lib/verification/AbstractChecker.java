/*===========================================================================
  Copyright (C) 2016 by the Okapi Framework contributors
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

package net.sf.okapi.lib.verification;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.annotation.ITSLQIAnnotations;
import net.sf.okapi.common.annotation.IssueAnnotation;
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

public abstract class AbstractChecker {

	public final static Pattern WORDCHARS = Pattern.compile("[\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lo}\\p{Nd}]");
	public final static Pattern WORDCHARS_NONUMBERS = Pattern.compile("[\\p{Ll}\\p{Lu}\\p{Lt}\\p{Lo}]");

	protected final GenericContent genCont = new GenericContent();
	
	private LocaleId srcLoc;
	private LocaleId trgLoc;
	private URI currentDocId;
	private String currentSubDocId;
	private Parameters params;
	private List<Issue> issues;
	private List<String> sigList;
	private boolean monolingual;

	/**
	 * Converts a position in a coded-text string 
	 * into the same position when the string display uses generic codes.
	 * @param frag the text-fragment displayed.
	 * @param pos the position in the coded-text string
	 * @return the position when the codes' generic representations are used
	 */
	static public int fromFragmentToGeneric (TextFragment frag,
		int pos)
	{
		// No codes means no correction
		if (( pos < 1 ) || !frag.hasCode() ) return pos;

		// Else: correct the position
		int len = 0;
		String text = frag.getCodedText();
		for ( int i=0; i<text.length(); i++ ) {
			if ( i >= pos ) {
				return len;
			}
			if ( TextFragment.isMarker(text.charAt(i)) ) {
				Code code = frag.getCode(text.charAt(++i));
				len += (""+code.getId()).length(); // Length of the ID in all cases
				switch ( text.codePointAt(i-1) ) {
				case TextFragment.MARKER_OPENING:
					len += 2; // Length for < and > (<ID>)
					break;
				case TextFragment.MARKER_CLOSING:
					len += 3; // Length for <, / and > (</ID>)
					break;
				case TextFragment.MARKER_ISOLATED:
					if ( code.getTagType() == TagType.OPENING ) {
						len += 4; // Length for <, b , / and > (<bID/>)
					}
					else if ( code.getTagType() == TagType.CLOSING ) {
						len += 4; // Length for <, e , / and > (<eID/>)
					}
					else {
						len += 3; // Length for <, / and > (<ID/>)
					}
					break;
				}
			}
			else {
				len++;
			}			
		}
		return len;
	}

	/**
	 * Converts a position in a string display using the original code data 
	 * into the same position when the string display uses generic codes.
	 * @param frag the text-fragment displayed.
	 * @param pos the position when the codes' original data are used
	 * @return the position when the codes' generic representations are used
	 */
	static public int fromOriginalToGeneric (TextFragment frag,
		int pos)
	{
		// No codes means no correction
		if (( pos < 1 ) || !frag.hasCode() ) return pos;

		// Else: correct the position
		int len = 0;
		String text = frag.getCodedText();
		for ( int i=0; i<text.length(); i++ ) {
			if ( i >= pos ) {
				return len;
			}
			if ( TextFragment.isMarker(text.charAt(i)) ) {
				Code code = frag.getCode(text.charAt(++i));
				
				// Adjust the position (subtract the length of the original data)
				String data = code.getData();
				if ( data != null ) {
					pos -= data.length();
					pos += 2; // coded-text place-holder
				}
				if ( i >= pos ) {
					return len;
				}
				
				len += (""+code.getId()).length(); // Length of the ID in all cases
				switch ( text.codePointAt(i-1) ) {
				case TextFragment.MARKER_OPENING:
					len += 2; // Length for < and > (<ID>)
					break;
				case TextFragment.MARKER_CLOSING:
					len += 3; // Length for <, / and > (</ID>)
					break;
				case TextFragment.MARKER_ISOLATED:
					if ( code.getTagType() == TagType.OPENING ) {
						len += 4; // Length for <, b , / and > (<bID/>)
					}
					else if ( code.getTagType() == TagType.CLOSING ) {
						len += 4; // Length for <, e , / and > (<eID/>)
					}
					else {
						len += 3; // Length for <, / and > (<ID/>)
					}
					break;
				}
			}
			else {
				len++;
			}			
		}
		return len;
	}

	/**
	 * Converts a position in a string display where codes are stripped 
	 * into the same position when the string display uses generic codes.
	 * @param frag the text-fragment displayed.
	 * @param pos the position when the codes are stripped.
	 * @return the position when the codes' generic representations are used
	 */
	static public int fromStrippedToGeneric (TextFragment frag,
		int pos)
	{
		// No codes means no correction
		if (( pos < 1 ) || !frag.hasCode() ) return pos;

		// Else: correct the position
		int len = 0;
		String text = frag.getCodedText();
		for ( int i=0; i<text.length(); i++ ) {
			if ( i >= pos ) {
				return len;
			}
			if ( TextFragment.isMarker(text.charAt(i)) ) {
				Code code = frag.getCode(text.charAt(++i));
				
				pos += 2;
				
				len += (""+code.getId()).length(); // Length of the ID in all cases
				switch ( text.codePointAt(i-1) ) {
				case TextFragment.MARKER_OPENING:
					len += 2; // Length for < and > (<ID>)
					break;
				case TextFragment.MARKER_CLOSING:
					len += 3; // Length for <, / and > (</ID>)
					break;
				case TextFragment.MARKER_ISOLATED:
					if ( code.getTagType() == TagType.OPENING ) {
						len += 4; // Length for <, b , / and > (<bID/>)
					}
					else if ( code.getTagType() == TagType.CLOSING ) {
						len += 4; // Length for <, e , / and > (<eID/>)
					}
					else {
						len += 3; // Length for <, / and > (<ID/>)
					}
					break;
				}
			}
			else {
				len++;
			}			
		}
		return len;
	}

	/**
	 * Converts a position in a string display where codes are stripped 
	 * into the same position when the string display uses original data.
	 * @param frag the text-fragment displayed.
	 * @param pos the position when the codes are stripped
	 * @return the position when the codes' original data are used.
	 */
	public static int fromStrippedToString (TextFragment frag,
		int pos)
	{
		// No codes means no correction
		if (( pos < 1 ) || !frag.hasCode() ) return pos;

		// Else: correct the position
		int len = 0;
		String text = frag.getCodedText();
		for ( int i=0; i<text.length(); i++ ) {
			if ( i >= pos ) {
				return len;
			}
			if ( TextFragment.isMarker(text.charAt(i)) ) {
				pos += 2;
				Code code = frag.getCode(text.charAt(++i));
				len += code.getData().length();
				continue;
			}
			else {
				len++;
			}
		}
		return len;
	}
	
	public void startProcess(LocaleId sourceLocale, LocaleId targetLocale, Parameters params, List<Issue> issues) {
		this.srcLoc = sourceLocale;
		this.trgLoc = targetLocale;
		this.params = params;
		this.issues = issues;
	}

	public void processStartDocument(StartDocument sd, List<String> sigList) {
		currentDocId = (new File(sd.getName())).toURI();
		this.sigList = sigList;
		monolingual = !sd.isMultilingual();
		currentSubDocId = null;
	}

	public void processStartSubDocument(StartSubDocument ssd) {
		currentSubDocId = ssd.getName();
		if (currentSubDocId == null) {
			currentSubDocId = ssd.getId();
		}
	}

	public abstract void processTextUnit(ITextUnit tu);

	public void addAnnotationAndReportIssue(IssueType issueType, ITextUnit tu, TextContainer tc, String segId,
			String comment, int srcStart, int srcEnd, int trgStart, int trgEnd,
			double severity, String srcOri, String trgOri, List<Code> codes) {
		addAnnotationAndReportIssue(issueType, tu, tc, segId, comment,
				srcStart, srcEnd, trgStart, trgEnd, severity, srcOri, trgOri, codes, null);
	}

	public void addAnnotationAndReportIssue(IssueType issueType, ITextUnit tu, TextContainer tc, String segId,
			String comment, int srcStart, int srcEnd, int trgStart, int trgEnd,
			double severity, String srcOri, String trgOri, List<Code> codes, String itsType) {
		reportIssue(issueType, tu, segId, comment, srcStart, srcEnd, trgStart, trgEnd, severity, srcOri, trgOri, codes);
		addAnnotation(tc, segId, issueType, comment, srcStart, srcEnd, trgStart, trgEnd, severity, codes, itsType);
	}

	public void addAnnotation(TextContainer tc, String segId, IssueType issueType, String comment, int srcStart,
			int srcEnd, int trgStart, int trgEnd, double severity, List<Code> codes) {
		IssueAnnotation ann = new IssueAnnotation(issueType, comment, severity, segId, srcStart, srcEnd, trgStart,
				trgEnd, codes);
		ITSLQIAnnotations.addAnnotations(tc, ann);
	}

	public void addAnnotation(TextContainer tc, String segId, IssueType issueType, String comment, int srcStart,
			int srcEnd, int trgStart, int trgEnd, double severity, List<Code> codes, String itsType) {
		IssueAnnotation ann = new IssueAnnotation(issueType, comment, severity, segId, srcStart, srcEnd, trgStart,
				trgEnd, codes);
		ITSLQIAnnotations.addAnnotations(tc, ann);
		if (itsType != null)
			ann.setITSType(itsType);
	}

	public void reportIssue(IssueType issueType, ITextUnit tu, String segId, String message, int srcStart,
			int srcEnd, int trgStart, int trgEnd, double severity, String srcOri, String trgOri, List<Code> codes) {
		Issue issue = new Issue(currentDocId, currentSubDocId, issueType, tu.getId(), segId, message, srcStart, srcEnd,
				trgStart, trgEnd, severity, tu.getName());
		issue.setCodes(codes);
		issues.add(issue);
		issue.setEnabled(true);
		issue.setSource(srcOri);
		issue.setTarget(trgOri);
		
		// Add the source and target containers if requested
		if (( params != null ) && params.getIncludeTextContainers() ) {
			issue.setContainers(tu, trgLoc);
		}

		if (sigList != null) {
			// Disable any issue for which we have the signature in the list
			issue.setEnabled(!sigList.contains(issue.getSignature()));
		}
	}

	/**
	 * This method does not seem to be used anywhere and its last parameter is not used.
	 * We may remove this method at some point in the future. Use the other
	 * {@link #reportIssue(IssueType, ITextUnit, String, String, int, int, int, int, double, String, String, List)}
	 * method instead.
	 */
	@Deprecated
	public void reportIssue(Issue init, ITextUnit tu, String srcOri, String trgOri, Object extra) {
		Issue issue = new Issue(currentDocId, currentSubDocId, init.getIssueType(), tu.getId(), init.getSegId(),
				init.getMessage(), init.getSourceStart(), init.getSourceEnd(), init.getTargetStart(),
				init.getTargetEnd(), init.getSeverity(), tu.getName());
		issue.setCodes(init.getCodes());
		issues.add(issue);
		issue.setEnabled(true);
		issue.setSource(srcOri);
		issue.setTarget(trgOri);

		if (sigList != null) {
			// Disable any issue for which we have the signature in the list
			issue.setEnabled(!sigList.contains(issue.getSignature()));
		}
	}

	public void setAnnotationIds(TextContainer srcCont, TextContainer trgCont) {
		// Make sure the annotation sets have IDs
		GenericAnnotations anns = srcCont.getAnnotation(GenericAnnotations.class);
		if (anns != null) {
			anns.setData(Util.makeId(UUID.randomUUID().toString()));
		}
		anns = trgCont.getAnnotation(GenericAnnotations.class);
		if (anns != null) {
			anns.setData(Util.makeId(UUID.randomUUID().toString()));
		}
	}

	public LocaleId getSrcLoc() {
		return srcLoc;
	}

	public LocaleId getTrgLoc() {
		return trgLoc;
	}

	public URI getCurrentDocId() {
		return currentDocId;
	}

	public String getCurrentSubDocId() {
		return currentSubDocId;
	}

	public Parameters getParams() {
		return params;
	}

	public List<Issue> getIssues() {
		return issues;
	}

	public List<String> getSigList() {
		return sigList;
	}

	public boolean isMonolingual() {
		return monolingual;
	}

	/**
	 * Generates the display string for a source or target text-fragment (e.g. a segment).
	 * @param frag the fragment to display.
	 * @return the string representation based on the options (e.g. tags: original data or generic)
	 */
	protected String getDisplay (TextFragment frag) {
		if ( params.getUseGenericCodes() ) {
			return genCont.setContent(frag).toString();
		}
		else {
			return frag.toText();
		}
	}

	/**
	 * Generates the display string for a source or target text=container.
	 * @param tc the text-container to display.
	 * @return the string representation based on the options (e.g. tags: original data or generic)
	 */
	protected String getDisplay (TextContainer tc) {
		if ( params.getUseGenericCodes() ) {
			return genCont.printSegmentedContent(tc, false, false);
		}
		else {
			return tc.toString();
		}
	}
}
