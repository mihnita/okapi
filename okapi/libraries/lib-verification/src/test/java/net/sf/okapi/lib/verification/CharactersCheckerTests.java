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
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextUnit;

@RunWith(JUnit4.class)
public class CharactersCheckerTests {
	
	private static final LocaleId SOURCE_LOCALE = LocaleId.ENGLISH;
	private static final LocaleId TARGET_LOCALE = LocaleId.FRENCH;
	private CharactersChecker checker;
	private List<Issue> issues;

	private void runTest (TextContainer srcCont,
		TextContainer trgCont,
		Parameters params
	)
	{
		ITextUnit tu = new TextUnit("tu1");
		tu.setSource(srcCont);
		tu.setTarget(TARGET_LOCALE, trgCont);

		issues.clear();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);
		checker.processTextUnit(tu);
	}

	@Before
	public void setUp() {
		checker = new CharactersChecker(); 
		issues = new ArrayList<>();
	}

	@Test
	public void testCharset () {
		// Using the original data for codes
		Parameters params = new Parameters();
		params.setCheckCharacters(true);
		params.setCheckAllowedCharacters(false);
		params.setUseGenericCodes(false);
		params.setCharset("us-ascii");

		TextFragment srcFrag = new TextFragment("a"); //");
		srcFrag.append(TagType.PLACEHOLDER, "br", "[CODE/]");
		srcFrag.append("b \u039E c");
		TextContainer srcCont = new TextContainer(srcFrag);
		
		TextFragment trgFrag = new TextFragment("qwerty");
		trgFrag.append(TagType.PLACEHOLDER, "br", "[CODE/]");
		trgFrag.append("y \u039E z");
		TextContainer trgCont = new TextContainer(trgFrag);
		
		runTest(srcCont, trgCont, params);
		assertEquals(1, issues.size()); // Only target is tested for the character set
		assertEquals(15, issues.get(0).getTargetStart());
		assertEquals(16, issues.get(0).getTargetEnd());
		assertEquals("qwerty[CODE/]y \u039E z", issues.get(0).getTarget());

		// Now using the generic codes
		params.setUseGenericCodes(true);
		
		runTest(srcCont, trgCont, params);
		assertEquals(1, issues.size()); // Only target is tested for the character set
		assertEquals(12, issues.get(0).getTargetStart());
		assertEquals(13, issues.get(0).getTargetEnd());
		assertEquals("qwerty<1/>y \u039E z", issues.get(0).getTarget());
	}

	@Test
	public void testCorruptedChars () {
		// Using the original data for codes
		Parameters params = new Parameters();
		params.setCheckCharacters(false);
		params.setCheckAllowedCharacters(false);
		params.setCorruptedCharacters(true);
		params.setUseGenericCodes(false);

		TextFragment srcFrag = new TextFragment("a"); //");
		srcFrag.append(TagType.PLACEHOLDER, "br", "[CODE/]");
		srcFrag.append("b \u00C3\u00A4 c");
		TextContainer srcCont = new TextContainer(srcFrag);
		
		TextFragment trgFrag = new TextFragment("qwerty");
		trgFrag.append(TagType.PLACEHOLDER, "br", "[CODE/]");
		trgFrag.append("y \u00C3\u00A4 z");
		TextContainer trgCont = new TextContainer(trgFrag);
		
		runTest(srcCont, trgCont, params);
		assertEquals(1, issues.size()); // Only target is tested for the character set
		assertEquals(15, issues.get(0).getTargetStart());
		assertEquals(17, issues.get(0).getTargetEnd());
		assertEquals("qwerty[CODE/]y \u00C3\u00A4 z", issues.get(0).getTarget());

		// Now using the generic codes
		params.setUseGenericCodes(true);
		
		runTest(srcCont, trgCont, params);
		assertEquals(1, issues.size()); // Only target is tested for the character set
		assertEquals(12, issues.get(0).getTargetStart());
		assertEquals(14, issues.get(0).getTargetEnd());
		assertEquals("qwerty<1/>y \u00C3\u00A4 z", issues.get(0).getTarget());
	}

	@Test
	public void testITSAnnotation () {
		// Using the original data for codes
		Parameters params = new Parameters();
		params.setCheckCharacters(false);
		params.setCheckAllowedCharacters(true);
		params.setUseGenericCodes(false);

		TextFragment srcFrag = new TextFragment("a"); //");
		srcFrag.append(TagType.PLACEHOLDER, "br", "[CODE/]");
		srcFrag.append("bcd");
		TextContainer srcCont = new TextContainer(srcFrag);
		GenericAnnotation ga = new GenericAnnotation(GenericAnnotationType.ALLOWEDCHARS);
		ga.setString(GenericAnnotationType.ALLOWEDCHARS_VALUE, "[abc]");
		GenericAnnotation.addAnnotation(srcCont, ga);
		
		TextFragment trgFrag = new TextFragment("qwerty");
		trgFrag.append(TagType.PLACEHOLDER, "br", "[CODE/]");
		trgFrag.append("qwertZ");
		TextContainer trgCont = new TextContainer(trgFrag);
		GenericAnnotation ga2 = new GenericAnnotation(GenericAnnotationType.ALLOWEDCHARS);
		ga2.setString(GenericAnnotationType.ALLOWEDCHARS_VALUE, "[qwerty]");
		GenericAnnotation.addAnnotation(trgCont, ga2);
		
		runTest(srcCont, trgCont, params);
		assertEquals(2, issues.size());
		Issue issue = issues.get(0);
		assertEquals(10, issue.getSourceStart());
		assertEquals(11, issue.getSourceEnd());
		assertEquals("a[CODE/]bcd", issue.getSource());
		issue = issues.get(1);
		assertEquals(18, issue.getTargetStart());
		assertEquals(19, issue.getTargetEnd());
		assertEquals("qwerty[CODE/]qwertZ", issue.getTarget());

		// Now using the generic codes
		params.setUseGenericCodes(true);
		
		runTest(srcCont, trgCont, params);
		assertEquals(2, issues.size());
		issue = issues.get(0);
		assertEquals(7, issue.getSourceStart());
		assertEquals(8, issue.getSourceEnd());
		assertEquals("a<1/>bcd", issue.getSource());
		issue = issues.get(1);
		assertEquals(15, issue.getTargetStart());
		assertEquals(16, issue.getTargetEnd());
		assertEquals("qwerty<1/>qwertZ", issue.getTarget());
	}
}
