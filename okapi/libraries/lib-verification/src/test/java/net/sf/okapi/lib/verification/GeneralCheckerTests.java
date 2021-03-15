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
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.common.annotation.SkipCheckAnnotation;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

@RunWith(JUnit4.class)
public class GeneralCheckerTests {
	
	private static final LocaleId SOURCE_LOCALE = LocaleId.ENGLISH;
	private static final LocaleId TARGET_LOCALE = LocaleId.FRENCH;
	private GeneralChecker checker;
	private List<Issue> issues;

	private void runTest (String srcText,
		String trgText,
		List<PatternItem> patterns,
		boolean withNumbers
	)
	{
		ITextUnit tu = new TextUnit("tu1", srcText);
		tu.setTargetContent(TARGET_LOCALE, new TextFragment(trgText));

		Parameters params = new Parameters();
		params.setTargetSameAsSourceWithNumbers(withNumbers);
		if ( patterns != null ) {
			params.setCheckPatterns(true);
			params.setPatterns(patterns);
		}

		issues.clear();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);
		checker.processTextUnit(tu);
	}

	@Before
	public void setUp() {
		checker = new GeneralChecker(); 
		issues = new ArrayList<>();
	}

	@Test
	public void testTargetSameAsSourceNoPatterns() {
		// No issue (target different from source)
		runTest("Text aaa", "Text bbb", null, true);
		assertEquals(0, issues.size());

		// With issue (target same as source)
		runTest("Text aaa", "Text aaa", null, true);
		assertEquals(1, issues.size());
		assertEquals(IssueType.TARGET_SAME_AS_SOURCE, issues.get(0).getIssueType());
	}

	@Test
	public void testWithAndWithoutSkip() {
		ITextUnit tu = new TextUnit("tu1", "text");
		tu.setTargetContent(TARGET_LOCALE, new TextFragment("text"));

		issues.clear();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, new Parameters(), issues);
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertEquals(IssueType.TARGET_SAME_AS_SOURCE, issues.get(0).getIssueType());
		
		// Add the annotation and re-check
		tu.getSource().getFirstSegment().setAnnotation(new SkipCheckAnnotation());

		issues.clear();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, new Parameters(), issues);
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());
	}

	@Test
	public void testTargetSameAsSourceWithPattern () {
		// Pattern does not use SAME
		PatternItem item = new PatternItem("aaa", "bbb", true, 10, false, "error");
		List<PatternItem> patterns = new ArrayList<>();
		patterns.add(item);
		runTest("aaa", "aaa", patterns, true);
		assertEquals(1, issues.size());
		assertEquals(IssueType.TARGET_SAME_AS_SOURCE, issues.get(0).getIssueType());

		// Pattern does use SAME
		item = new PatternItem("aaa", PatternItem.SAME, true, 10, false, "error");
		patterns = new ArrayList<>();
		patterns.add(item);
		runTest("aaa", "aaa", patterns, true);
		assertEquals(0, issues.size()); // No error because we expect this to be the same
	}

	@Test
	public void testTargetSameAsSourceWithComplexPatterns () {
		// Pattern does not use SAME
		PatternItem item = new PatternItem("aaa", null, true, 10, true, "error");
		item.singlePattern = true; // thus, target pattern is null
		List<PatternItem> patterns = new ArrayList<>();
		patterns.add(item);
		item = new PatternItem("aaa", PatternItem.SAME, true, 10, false, "error");
		patterns.add(item);
		runTest("aaa", "aaa", patterns, true);
		assertEquals(0, issues.size()); // Pattern with SAME, so source==target is expected, not an error
	}

	@Test
	public void testTargetSameAsSourceHavingNumbers () {
		runTest("123", "123", null, true); // Only numbers but option includes them
		assertEquals(1, issues.size());
		assertEquals(IssueType.TARGET_SAME_AS_SOURCE, issues.get(0).getIssueType());

		runTest("123", "123", null, false); // Only number and option exclude them
		assertEquals(0, issues.size());

		runTest("(123.,-+)*/", "(123.,-+)*/", null, false); // Only number and signs and option exclude them
		assertEquals(0, issues.size());

		runTest("123a", "123a", null, true); // Option excludes number, but the string is not only numbers
		assertEquals(1, issues.size());
		assertEquals(IssueType.TARGET_SAME_AS_SOURCE, issues.get(0).getIssueType());
	}

}
