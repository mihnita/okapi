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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.text.BreakIterator;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.common.annotation.SkipCheckAnnotation;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;

public class GeneralChecker extends AbstractChecker {
	private String doubledWordExceptions;
	private BreakIterator breakIterator;
	  // The exact regex equivalent of UCharacter.isUWhiteSpace.
	  // Used to test if the stuff between words is spaces only.
	private final static Pattern UWHITESPACE = Pattern.compile("[\\t\\v\\n\\f\\r\\p{Z}]+");

	@Override
	public void startProcess(LocaleId sourceLocale, LocaleId targetLocale, Parameters params, List<Issue> issues) {
		super.startProcess(sourceLocale, targetLocale, params, issues);

		if (params.getDoubledWord()) {
			breakIterator = BreakIterator.getWordInstance(targetLocale.toIcuLocale());
			// Construct the string of doubled-words that are not errors
			// The working patter is the list like this: ";word1;word2;word3;"
			doubledWordExceptions = ";" + params.getDoubledWordExceptions().toLowerCase() + ";";
		}
	}

	@Override
	public void processStartDocument(StartDocument sd, List<String> sigList) {
		super.processStartDocument(sd, sigList);
	}

	@Override
	public void processStartSubDocument(StartSubDocument ssd) {
		super.processStartSubDocument(ssd);
	}

	@Override
	public void processTextUnit(ITextUnit tu) {
		// Skip non-translatable entries
		if (!tu.isTranslatable()) {
			return;
		}

		// Get the containers
		TextContainer srcCont = tu.getSource();
		TextContainer trgCont = tu.getTarget(getTrgLoc());

		// Check if we have a target
		if (trgCont == null) {
			if (!isMonolingual()) { // No report as error for monolingual files
				// No translation available
				addAnnotationAndReportIssue(IssueType.MISSING_TARGETTU, tu, tu.getSource(), null, "Missing translation.", 0, -1, 0, -1,
						Issue.SEVERITY_HIGH, getDisplay(srcCont), "", null);
			}
			return;
		}

		ISegments srcSegs = srcCont.getSegments();
		ISegments trgSegs = trgCont.getSegments();

		for (Segment srcSeg : srcSegs) {
			if ( srcSeg.getAnnotation(SkipCheckAnnotation.class) != null ) {
				continue;
			}
			Segment trgSeg = trgSegs.get(srcSeg.getId());
			if (trgSeg == null) {
				addAnnotationAndReportIssue(IssueType.MISSING_TARGETSEG, tu, srcCont, srcSeg.getId(),
						"The source segment has no corresponding target segment.", 0, -1, 0, -1, Issue.SEVERITY_HIGH,
						getDisplay(srcSeg.getContent()), "", null);
				continue; // Cannot go further for that segment
			}

			// Check for empty target, if requested
			if (getParams().getEmptyTarget()) {
				if (trgSeg.text.isEmpty() && !srcSeg.text.isEmpty()) {
					addAnnotationAndReportIssue(IssueType.EMPTY_TARGETSEG, tu, srcCont, srcSeg.getId(),
							"The target segment is empty, but its source is not empty.", 0, -1, 0, -1,
							Issue.SEVERITY_HIGH, getDisplay(srcSeg.getContent()), "", null);
					continue; // No need to check more if it's empty
				}
			}
			// Check for empty source when target is not empty, if requested
			if (getParams().getEmptySource()) {
				if (srcSeg.text.isEmpty() && !trgSeg.text.isEmpty()) {
					addAnnotationAndReportIssue(IssueType.EMPTY_SOURCESEG, tu, srcCont, srcSeg.getId(),
							"The target segment is not empty, but its source is empty.", 0, -1, 0, -1,
							Issue.SEVERITY_HIGH, getDisplay(srcSeg.getContent()), "", null);
					continue; // No need to check more if the source is empty
				}
			}

			List<PatternItem> patterns = null;
			// Compile the patterns if we use them
			if (getParams().getCheckPatterns()) {
				patterns = getParams().getPatterns();
				boolean hasSame = false;
				for (PatternItem item : patterns) {
					if (item.enabled) {
						// Detect if SAME is actually used
						if ((item.target != null) && item.target.equals(PatternItem.SAME)) {
							hasSame = true;
						}
						item.compile();
					}
				}
				if (!hasSame) patterns = null; // Nothing to check since SAME is not used
			}

			// Check for target is the same as source, if requested
			if (getParams().getTargetSameAsSource()) {
				if (getParams().getTargetSameAsSourceForSameLanguage() || !getSrcLoc().sameLanguageAs(getTrgLoc())) {
					if (hasMeaningfullText(srcSeg.text)) {
						if (srcSeg.text.compareTo(trgSeg.text, getParams().getTargetSameAsSourceWithCodes()) == 0) {
							boolean warn = true;
							// Is the string of the cases where target should be the same? (URL, etc.)
							if (patterns != null) {
								for (PatternItem item : patterns) {
									String ctext = srcSeg.text.getCodedText();
									if (item.enabled && (item.target != null) && item.target.equals(PatternItem.SAME)) {
										Matcher m = item.getSourcePattern().matcher(ctext);
										if (m.find()) {
											warn = !ctext.equals(m.group());
											break;
										}
									}
								}
							}
							if (warn) {
								addAnnotationAndReportIssue(IssueType.TARGET_SAME_AS_SOURCE, tu, srcCont, srcSeg.getId(),
										"Translation is the same as the source.", 0, -1, 0, -1, Issue.SEVERITY_MEDIUM,
										getDisplay(srcSeg.getContent()), getDisplay(trgSeg.getContent()), null);
							}
						}
					}
				}
			}

			// Check all suspect patterns
			checkSuspectPatterns(srcSeg, trgSeg, tu);
		}
		
		// Check for orphan target segments
		for (Segment trgSeg : trgSegs) {
			if ( trgSeg.getAnnotation(SkipCheckAnnotation.class) != null ) {
				continue;
			}
			Segment srcSeg = srcSegs.get(trgSeg.getId());
			if (srcSeg == null) {
				addAnnotationAndReportIssue(IssueType.EXTRA_TARGETSEG, tu, trgCont, trgSeg.getId(),
						String.format("Extra target segment (id=%s).", trgSeg.getId()),
						0, -1, 0, -1, Issue.SEVERITY_HIGH, "", getDisplay(trgSeg.getContent()), null);
				continue; // Cannot go further for that segment
			}
		}

		// Check trailing and leading whitespace characters
		Segment firstSeg = srcSegs.get(0);
		Segment lastSeg = srcSegs.getLast();

		if ( (firstSeg.getAnnotation(SkipCheckAnnotation.class) == null) || (lastSeg.getAnnotation(SkipCheckAnnotation.class) == null) ) {
			TextFragment srcFrag = null;
			if (srcCont.contentIsOneSegment()) {
				srcFrag = srcCont.getFirstContent();
			} else {
				srcFrag = srcCont.getUnSegmentedContentCopy();
			}
	
			TextFragment trgFrag = null;
			if (trgCont.contentIsOneSegment()) {
				trgFrag = trgCont.getFirstContent();
			} else {
				trgFrag = trgCont.getUnSegmentedContentCopy();
			}
			checkWhiteSpaces(srcFrag, trgFrag, tu, firstSeg.getId(), lastSeg.getId());
		}
		
		setAnnotationIds(srcCont, trgCont);
	}

	/**
	 * Indicates if we have at least one character that is part of the character
	 * set for a "word". digits are considered part of a "word".
	 * 
	 * @param frag
	 *            the text fragment to look at.
	 * @return true if a "word" is detected.
	 */
	private boolean hasMeaningfullText(TextFragment frag) {
		if (getParams().getTargetSameAsSourceWithNumbers()) {
			return WORDCHARS.matcher(frag.getCodedText()).find();
		}
		return WORDCHARS_NONUMBERS.matcher(frag.getCodedText()).find();
	}

	private boolean isSpaceWeCareAbout(char c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c);
	}

	private void checkWhiteSpaces(TextFragment srcFrag,
		TextFragment trgFrag,
		ITextUnit tu,
		String firstSegId,
		String lastSegId)
	{
		String srcCT = srcFrag.getCodedText();
		String trgCT = trgFrag.getCodedText();
		
		// Check for leading whitespace characters
		if (getParams().getLeadingWS()) {

			// Skip the check for empty targets (avoiding meaningless issues)
			if ( trgCT.length() == 0 ) {
				return;
			}

			// Missing ones
			for (int i = 0; i < srcCT.length(); i++) {
				if (isSpaceWeCareAbout(srcCT.charAt(i))) {
					if (srcCT.length() > i) {
						if ((trgCT.length() - 1 < i) || (trgCT.charAt(i) != srcCT.charAt(i))) {
							int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(srcFrag, i) : i;
							addAnnotationAndReportIssue(IssueType.MISSINGORDIFF_LEADINGWS, tu, tu.getSource(), firstSegId,
									String.format("Missing or different leading white space at position %d.", pos), pos,
									pos + 1, 0, -1, Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
							break;
						}
					} else {
						int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(srcFrag, i) : i;
						addAnnotationAndReportIssue(IssueType.MISSING_LEADINGWS, tu, tu.getSource(), firstSegId,
								String.format("Missing leading white space at position %d.", pos), pos, pos + 1, 0, -1,
								Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
					}
				} else {
					break;
				}
			}

			// Extra ones
			for (int i = 0; i < trgCT.length(); i++) {
				if (isSpaceWeCareAbout(trgCT.charAt(i))) {
					if (srcCT.length() > i) {
						if ((srcCT.length() - 1 < i) || (srcCT.charAt(i) != trgCT.charAt(i))) {
							int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(trgFrag, i) : i;
							addAnnotationAndReportIssue(IssueType.EXTRAORDIFF_LEADINGWS, tu, tu.getTarget(getTrgLoc()), firstSegId,
									String.format("Extra or different leading white space at position %d.", pos), 0, -1,
									pos, pos + 1, Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
							break;
						}
					} else {
						int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(trgFrag, i) : i;
						addAnnotationAndReportIssue(IssueType.EXTRA_LEADINGWS, tu, tu.getTarget(getTrgLoc()), firstSegId,
								String.format("Extra leading white space at position %d.", pos), 0, -1, pos, pos + 1,
								Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
					}
				} else {
					break;
				}
			}
		}

		// Check for trailing whitespace characters
		if (getParams().getTrailingWS()) {
			// Missing ones
			int j = trgCT.length() - 1;
			for (int i = srcCT.length() - 1; i >= 0; i--) {
				if (isSpaceWeCareAbout(srcCT.charAt(i))) {
					if (j >= 0) {
						int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(srcFrag, i) : i;
						if ((trgCT.length() - 1 < j) || (trgCT.charAt(j) != srcCT.charAt(i))) {
							addAnnotationAndReportIssue(IssueType.MISSINGORDIFF_TRAILINGWS, tu, tu.getSource(), lastSegId,
									String.format("Missing or different trailing white space at position %d", pos), pos,
									pos + 1, 0, -1, Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
							break;
						}
					} else {
						int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(srcFrag, i) : i;
						addAnnotationAndReportIssue(IssueType.MISSING_TRAILINGWS, tu, tu.getSource(), lastSegId,
								String.format("Missing trailing white space at position %d.", pos), pos, pos + 1, 0, -1,
								Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
					}
				} else {
					break;
				}
				j--;
			}

			// Extra ones
			j = srcCT.length() - 1;
			for (int i = trgCT.length() - 1; i >= 0; i--) {
				if (isSpaceWeCareAbout(trgCT.charAt(i))) {
					if (j >= 0) {
						if ((srcCT.length() - 1 < j) || (srcCT.charAt(j) != trgCT.charAt(i))) {
							int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(trgFrag, i) : i;
							addAnnotationAndReportIssue(IssueType.EXTRAORDIFF_TRAILINGWS, tu, tu.getTarget(getTrgLoc()), lastSegId,
									String.format("Extra or different trailing white space at position %d.", pos), 0, -1,
									pos, pos + 1, Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
							break;
						}
					} else {
						int pos = getParams().getUseGenericCodes() ? fromFragmentToGeneric(trgFrag, i) : i;
						addAnnotationAndReportIssue(IssueType.EXTRA_TRAILINGWS, tu, tu.getTarget(getTrgLoc()), lastSegId,
								String.format("Extra white trailing space at position %d.", pos), 0, -1, pos, pos + 1,
								Issue.SEVERITY_LOW, getDisplay(srcFrag), getDisplay(trgFrag), null);
					}
				} else {
					break;
				}
				j--;
			}
		}
	}

	private void checkSuspectPatterns(Segment srcSeg, Segment trgSeg, ITextUnit tu) {
		String trgCText = trgSeg.text.getCodedText();

		if (getParams().getDoubledWord()) {
			breakIterator.setText(trgCText);

			int previousPosition = breakIterator.first();
			int currentPosition = breakIterator.next();
			String previousWord = null;
			while (currentPosition != BreakIterator.DONE) {
				final String currentWord = trgCText.substring(previousPosition, currentPosition);
				// Real word, not "in between words"
				if (breakIterator.getRuleStatus() > BreakIterator.WORD_NONE_LIMIT) {
					if (currentWord.equals(previousWord)) {
						if (!doubledWordExceptions.contains(";" + currentWord.toLowerCase() + ";")) {
							// Compute the display positions
							int dispStart = getParams().getUseGenericCodes()
									? fromFragmentToGeneric(trgSeg.text, previousPosition)
									: TextFragment.fromFragmentToString(trgSeg.text, previousPosition);
							int dispEnd = getParams().getUseGenericCodes()
									? fromFragmentToGeneric(trgSeg.text, currentPosition)
									: TextFragment.fromFragmentToString(trgSeg.text, currentPosition);
							// Add the issue
							addAnnotationAndReportIssue(IssueType.SUSPECT_PATTERN, tu, tu.getTarget(getTrgLoc()), srcSeg.getId(),
									String.format("Double word: \"%s\" found in the target.", currentWord),
									0, -1, dispStart, dispEnd, Issue.SEVERITY_HIGH,
									getDisplay(srcSeg.getContent()), getDisplay(trgSeg.getContent()), null);
						}
					} else {
						previousWord = currentWord;
					}
				} else {
					// We should have only spaces between words to consider them repeated.
					// Here there is "stuff" in between (think "many, many files"), they are not doubles.
					if (!UWHITESPACE.matcher(currentWord).matches()) {
						previousWord = null;
					}
				}
				previousPosition = currentPosition;
				currentPosition = breakIterator.next();
			}
		}
	}
}
