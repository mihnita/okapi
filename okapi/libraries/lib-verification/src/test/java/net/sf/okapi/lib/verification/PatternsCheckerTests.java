/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

@RunWith(JUnit4.class)
public class PatternsCheckerTests {
	
	private static final LocaleId SOURCE_LOCALE = LocaleId.ENGLISH;
	private static final LocaleId TARGET_LOCALE = LocaleId.FRENCH;
	private PatternsChecker checker;
	private List<Issue> issues;

	@Before
	public void setUp() {
		checker = new PatternsChecker();
		issues = new ArrayList<>();
	}

	@Test
	public void testNormalSameInTarget() {
		// Without issue
		runPatternTest("Text aaa", "Texte aaa", "aaa", "<same>", true, "test", false);
		assertEquals(0, issues.size());

		// With issue
		runPatternTest("Text aaa", "Texte AaA", "aaa", "<same>", true, "test=@@", false);
		assertEquals(1, issues.size());
		assertEquals("test=aaa",
			issues.get(0).getMessage());
	}

	@Test
	public void testSinglePatternTarget () {
		// With issue (using description as the issue note)
		runPatternTest("Text", "Texte aaa", null, "aaa", false, "test=@@", true);
		assertEquals(1, issues.size());
		assertEquals("test=aaa", issues.get(0).getMessage());

		// With issue (standard issue note)
		runPatternTest("Text", "Texte aaa", null, "aaa", false, "test", true);
		assertEquals(1, issues.size());
		assertEquals("\"aaa\" found in target. (from rule: test).", issues.get(0).getMessage());

		// With issue (several times)
		runPatternTest("Text", "Texte a A a", "NOT USED", "(?i)a", false, "test", true);
		assertEquals(3, issues.size());
		
		Issue issue = issues.get(0);
		assertEquals("\"a\" found in target. (from rule: test).", issue.getMessage());
		assertEquals(6, issue.getTargetStart());
		assertEquals(7, issue.getTargetEnd());
		
		issue = issues.get(1);
		assertEquals("\"A\" found in target. (from rule: test).", issue.getMessage());
		assertEquals(8, issue.getTargetStart());
		assertEquals(9, issue.getTargetEnd());
				
		issue = issues.get(2);
		assertEquals("\"a\" found in target. (from rule: test).", issue.getMessage());
		assertEquals(10, issue.getTargetStart());
		assertEquals(11, issue.getTargetEnd());
	}

	@Test
	public void testSinglePatternSource () {
		// With issue (using description as the issue note)
		runPatternTest("Text aaa", "Texte", "aaa", null, true, "test=@@", true);
		assertEquals(1, issues.size());
		assertEquals("test=aaa", issues.get(0).getMessage());

		// With issue (standard issue note)
		runPatternTest("Text aaa", "Texte", "aaa", null, true, "test", true);
		assertEquals(1, issues.size());
		assertEquals("\"aaa\" found in source. (from rule: test).", issues.get(0).getMessage());

		// With issue (several times)
		runPatternTest("Text a A a", "Texte", "(?i)a", "NOT USED", true, "test", true);
		assertEquals(3, issues.size());
		
		Issue issue = issues.get(0);
		assertEquals("\"a\" found in source. (from rule: test).", issue.getMessage());
		assertEquals(5, issue.getSourceStart());
		assertEquals(6, issue.getSourceEnd());
		
		issue = issues.get(1);
		assertEquals("\"A\" found in source. (from rule: test).", issue.getMessage());
		assertEquals(7, issue.getSourceStart());
		assertEquals(8, issue.getSourceEnd());
				
		issue = issues.get(2);
		assertEquals("\"a\" found in source. (from rule: test).", issue.getMessage());
		assertEquals(9, issue.getSourceStart());
		assertEquals(10, issue.getSourceEnd());
	}

	@Test
	public void testNormalSameInSource() {
		// Without issue
		runPatternTest("Text aaa", "Texte aaa", "<same>", "aaa", false, "test", false);
		assertEquals(0, issues.size());

		// With issue
		runPatternTest("Text BBB", "Texte aaa", "<same>", "aaa", false, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The target part \"aaa\" is not in the source. (from rule: test).",
			issues.get(0).getMessage());
	}

	@Test
	public void testNormalWithLiteralSame() {
		// Without issue
		runPatternTest("Text <same>", "Texte <same>", "\\u003Csame>", "\\u003Csame>", true, "test", false);
		assertEquals(0, issues.size());

		// With issue
		runPatternTest("Text <same>", "Texte <SAme>", "\\u003Csame>", "<same>", true, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The source part \"<same>\" is not in the target (from rule: test).",
			issues.get(0).getMessage());
	}

	@Test
	public void testSameWithExpressionInTarget() {
		// Without issue
		runPatternTest("Text.", "Texte.zz", "\\p{P}$", "<same>zz$", true, "test", false);
		assertEquals(0, issues.size());
		// Without issue
		runPatternTest("Text.", "Texte.zz", "zz$", "<same>$", true, "test", false);
		assertEquals(0, issues.size());

		// With issue
		runPatternTest("Text.", "Texte,", "\\p{P}$", "<same>$", true, "test (@@)", false);
		assertEquals(1, issues.size());
		assertEquals("test (.)", issues.get(0).getMessage());
		// With issue
		runPatternTest("Text.", "Texte.Z", "\\p{P}$", "<same>$", true, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The source part \".\" has no correspondence in the target (from rule: test).",
			issues.get(0).getMessage());
	}

	@Test
	public void testSameWithExpressionInSource() {
		// Without issue
		runPatternTest("Text.zz", "Texte.", "<same>zz$", "\\p{P}$", false, "test", false);
		assertEquals(0, issues.size());
		// Without issue
		runPatternTest("Text.zz", "Texte.", "<same>$", "zz$", false, "test", false);
		assertEquals(0, issues.size());

		// With issue
		runPatternTest("Text,", "Texte.", "<same>$", "\\p{P}$", false, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The target part \".\" has no correspondence in the source. (from rule: test).",
			issues.get(0).getMessage());
		// With issue
		runPatternTest("Text.Z", "Texte.", "<same>$", "\\p{P}$", false, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The target part \".\" has no correspondence in the source. (from rule: test).",
			issues.get(0).getMessage());
	}

	@Test
	public void testCapturesInTarget() {
		runPatternTest("Text.zz", "Texte.zz", "(\\p{P})(.*?)$", "$1$2", true, "test", false);
		assertEquals(0, issues.size());

		runPatternTest("Text.zz", "Texte.ZZ", "(\\p{P})(.*?)$", "$1$2", true, "test", false);
		assertEquals(1, issues.size());

		runPatternTest("Text.", "Texte.", "\\p{P}$", "<same>$", true, "test", false);
		assertEquals(0, issues.size());

		runPatternTest("abcdef", "fedcba", "(a)(b)(c)(d)(e)(f)", "$6$5$4$3$2$1", true, "test", false);
		assertEquals(0, issues.size());
	}

	@Test
	public void testEscapingInSame() {
		String needingEscape = "<([{\\^-=$!|]})?*+.>";
		
		// Test that the syntax of the pattern is correct
		runPatternTest("z"+needingEscape+"Xz", "_"+needingEscape+"_", "(\\Q"+needingEscape+"\\E)X", "_<same>_", true, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The source part \"<([{\\^-=$!|]})?*+.>X\" has no correspondence in the target (from rule: test).",
			issues.get(0).getMessage());

		// Test that is works
		runPatternTest("z"+needingEscape+"Xz", "_"+needingEscape+"X_", "(\\Q"+needingEscape+"\\E)X", "_<same>_", true, "test", false);
		assertEquals(0, issues.size());
	}

	@Test
	public void testComplexPatternWithGroups () {
		runPatternTest("test 1056 test", "test",
				"([0-9]{1})[.,[\\u0020\\u0007\\u00a0]]?([0-9]{1,3})([.,[\\u0020\\u0007\\u00a0]]?([0-9]{1,3}))?",
				"$1$2$3",
				true, "test", false);
		assertEquals(1, issues.size());
		assertEquals("The source part \"1056\" has no correspondence in the target (from rule: test).", issues.get(0).getMessage());
	}

	private void runPatternTest (String srcText,
		String trgText,
		String srcExpression,
		String trgExpression,
		boolean fromSource,
		String message,
		boolean singlePattern)
	{
		ITextUnit tu = new TextUnit("tu1", srcText);
		tu.setTargetContent(TARGET_LOCALE, new TextFragment(trgText));

		Parameters params = new Parameters();
		
		List<PatternItem> patterns = new ArrayList<>();
		PatternItem item = new PatternItem(srcExpression, trgExpression, true, 10, fromSource, message);
		item.singlePattern = singlePattern;
		patterns.add(item);
		params.setPatterns(patterns);
		
		if ( message.contains("@@") ) {
			params.setShowOnlyPatternDescription(true);
		}
		
		issues.clear();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);
		checker.processTextUnit(tu);
	}
}
