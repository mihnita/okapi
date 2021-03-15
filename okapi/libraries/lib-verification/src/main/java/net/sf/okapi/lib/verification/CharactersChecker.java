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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnitUtil;

public class CharactersChecker extends AbstractChecker {
	private Pattern corruption;
	private CharsetEncoder encoder1;
	private Pattern extraCharsAllowed;
	private Pattern itsAllowedChars;
	private String itsAllowedCharsPattern;

	@Override
	public void startProcess(LocaleId sourceLocale, LocaleId targetLocale, Parameters params, List<Issue> issues) {
		super.startProcess(sourceLocale, targetLocale, params, issues);

		// Pattern for corrupted characters
		corruption = null;
		if (params.getCorruptedCharacters()) {
			// Some of the most frequent patterns of corrupted characters
			corruption = Pattern
					.compile("\\u00C3[\\u00A4-\\u00B6]" + "|\\u00C3\\u201E" + "|\\u00C3\\u2026" + "|\\u00C3\\u2013");
		}

		// Characters check
		encoder1 = null;
		extraCharsAllowed = null;
		if (params.getCheckCharacters()) {
			// Encoding
			String charsetName = params.getCharset();
			if (!Util.isEmpty(charsetName)) {
				encoder1 = Charset.forName(charsetName).newEncoder();
			}
			// Extra characters allowed
			if (!params.getExtraCharsAllowed().isEmpty()) {
				extraCharsAllowed = Pattern.compile(params.getExtraCharsAllowed());
			}
		}
		itsAllowedChars = null;
		itsAllowedCharsPattern = "\u0000";
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
		
		if (getParams().getCheckAllowedCharacters()) {
			checkITSAllowedChars(tu, srcCont, true);
		}

		// Check if we have a target
		if (trgCont == null) {
			return;
		}
		
		if (getParams().getCheckAllowedCharacters()) {
			checkITSAllowedChars(tu, trgCont, false);
		}

		String trgOri = null;
		if (trgCont.contentIsOneSegment()) {
			trgOri = trgCont.toString();
		} else {
			trgOri = trgCont.getUnSegmentedContentCopy().toText();
		}

		if (getParams().getCorruptedCharacters()) {
			checkCorruptedCharacters(trgOri, tu, srcCont, trgCont);
		}

		if (getParams().getCheckCharacters()) {
			checkCharacters(trgOri, tu, srcCont, trgCont);
		}

		setAnnotationIds(srcCont, trgCont);
	}

	private void checkITSAllowedChars(ITextUnit tu, TextContainer tc, boolean isSource) {
		if (tc == null) {
			return;
		}
		GenericAnnotations anns = tc.getAnnotation(GenericAnnotations.class);
		if (anns == null) {
			return;
		}
		GenericAnnotation ga = anns.getFirstAnnotation(GenericAnnotationType.ALLOWEDCHARS);
		if (ga == null) {
			return;
		}
		try {
			String pattern = ga.getString(GenericAnnotationType.ALLOWEDCHARS_VALUE);
			// Re-set the compiled pattern if needed
			if ((itsAllowedChars == null) || !itsAllowedCharsPattern.equals(pattern)) {
				itsAllowedCharsPattern = pattern; // Remember for next time
				// Invert the pattern to match on error (character NOT allowed)
				if (pattern.startsWith("[^")) {
					pattern = "[" + pattern.substring(2);
				} else if (pattern.startsWith("[")) {
					pattern = "[^" + pattern.substring(1);
				} else {
					throw new OkapiException("Pattern should start with '[' or '[^'.");
				}
				itsAllowedChars = Pattern.compile(pattern);
			}

			// Get the plain text
			TextFragment tf;
			if (tc.contentIsOneSegment()) {
				tf = tc.getFirstContent();
			} else {
				tf = tc.getUnSegmentedContentCopy();
			}
			String tmp = TextUnitUtil.getText(tf);

			// Verify if we have a counter match
			Matcher m = itsAllowedChars.matcher(tmp);
			if (!m.find()) {
				return; // No error
			} // Else, report the first character not allowed

			int ss=0, ts=0, se=-1, te=-1;
			if ( isSource ) {
				ss = getParams().getUseGenericCodes()
						? fromStrippedToGeneric(tf, m.start())
						: fromStrippedToString(tf, m.start());
				se = getParams().getUseGenericCodes()
						? fromStrippedToGeneric(tf, m.end())
						: fromStrippedToString(tf, m.end());
			}
			else {
				ts = getParams().getUseGenericCodes()
						? fromStrippedToGeneric(tf, m.start())
						: fromStrippedToString(tf, m.start());
				te = getParams().getUseGenericCodes()
						? fromStrippedToGeneric(tf, m.end())
						: fromStrippedToString(tf, m.end());
			}
			addAnnotationAndReportIssue(IssueType.ALLOWED_CHARACTERS, tu, tc, null,
					String.format("Character not allowed: '%s' (pattern: '%s'", m.group(), itsAllowedCharsPattern), ss,
					se, ts, te, Issue.SEVERITY_HIGH, (isSource ? getDisplay(tc) : "N/A"),
					(isSource ? "N/A" : getDisplay(tc)), null);
		} catch (Throwable e) {
			addAnnotationAndReportIssue(IssueType.ALLOWED_CHARACTERS, tu, tc, null,
					String.format("Error when trying to check ITS allowed characters pattern '%s'. " + e.getMessage(),
							itsAllowedCharsPattern),
					0, -1, 0, -1, Issue.SEVERITY_HIGH, (isSource ? getDisplay(tc) : "N/A"),
					(isSource ? "N/A" : getDisplay(tc)), null);
		}
	}

	private void checkCharacters(String trgOri,
		ITextUnit tu,
		TextContainer srcCont,
		TextContainer trgCont)
	{
		StringBuilder badChars = new StringBuilder();
		int pos = -1;
		int badChar = 0;
		int count = 0;
		TextFragment trgFrag = null;

		for (int i = 0; i < trgOri.length(); i++) {
			char ch = trgOri.charAt(i);

			if (encoder1 != null) {
				if (encoder1.canEncode(ch)) {
					continue; // Allowed, move to the next character
				} else { // Not included in the target charset
					// Check if it is included in the extra characters list
					if (extraCharsAllowed != null) {
						Matcher m = extraCharsAllowed.matcher(trgOri.subSequence(i, i + 1));
						if (m.find()) {
							// Part of the extra character list: it's OK
							continue; // Move to the next character
						}
						// Else: not allowed: fall thru
					}
				}
			} else { // Not charset defined, try just the extra characters list
				if (extraCharsAllowed != null) {
					Matcher m = extraCharsAllowed.matcher(trgOri.subSequence(i, i + 1));
					if (m.find()) {
						// Part of the extra character list: it's OK
						continue; // Move to the next character
					}
					// Else: not allowed: fall thru
				}
				// Else: not in charset, nor in extra characters list: not
				// allowed
			}

			// The character is not allowed: add the error
			if (++count > 1) {
				if (( ch != badChar ) && ( badChars.indexOf(String.valueOf(ch)) == -1 )) {
					badChars.append(ch);
				}
			} else {
				pos = i;
				badChar = ch;
			}
		}

		// Do we have one or more errors?
		if (pos > -1) {
			int start = pos;
			if ( getParams().getUseGenericCodes() ) {
				if ( trgFrag == null ) {
					if ( trgCont.contentIsOneSegment() ) trgFrag = trgCont.getFirstContent();
					else trgFrag = trgCont.getUnSegmentedContentCopy();
				}
				// Convert from text with original codes to coded=text
				start = fromOriginalToGeneric(trgFrag, pos);
			}
			if (badChars.length() > 0) {
				addAnnotationAndReportIssue(IssueType.ALLOWED_CHARACTERS, tu, trgCont, null,
						String.format("The character '%c' (U+%04X) is not allowed in the target text."
								+ " Other forbidden characters found: ", badChar, (int) badChar) + badChars.toString(),
						0, -1, start, start + 1, Issue.SEVERITY_MEDIUM, getDisplay(srcCont), getDisplay(trgCont), null);
			} else {
				addAnnotationAndReportIssue(IssueType.ALLOWED_CHARACTERS,
						tu, trgCont, null, String.format("The character '%c' (U+%04X) is not allowed in the target text.",
								badChar, (int) badChar),
						0, -1, start, start + 1, Issue.SEVERITY_MEDIUM, getDisplay(srcCont), getDisplay(trgCont), null);
			}
		}

	}

	private void checkCorruptedCharacters(String trgOri,
		ITextUnit tu,
		TextContainer srcCont,
		TextContainer trgCont)
	{
		Matcher m = corruption.matcher(trgOri);
		if (m.find()) { // Getting one match is enough
			int start = m.start(), end = m.end();
			if ( getParams().getUseGenericCodes() ) {
				TextFragment trgFrag;
				if ( trgCont.contentIsOneSegment() ) trgFrag = trgCont.getFirstContent();
				else trgFrag = trgCont.getUnSegmentedContentCopy();
				// Convert from text with original codes to coded=text
				start = fromOriginalToGeneric(trgFrag, start);
				end = fromOriginalToGeneric(trgFrag, end);
			}
			addAnnotationAndReportIssue(IssueType.SUSPECT_PATTERN, tu, trgCont, null,
					String.format("Possible corrupted characters in the target (for example: \"%s\").", m.group()), 0,
					-1, start, end, Issue.SEVERITY_HIGH, getDisplay(srcCont), getDisplay(trgCont), null);
		}
	}
}
