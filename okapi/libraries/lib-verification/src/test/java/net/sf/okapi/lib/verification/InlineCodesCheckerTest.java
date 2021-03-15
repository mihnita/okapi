package net.sf.okapi.lib.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.common.annotation.SkipCheckAnnotation;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnit;

@RunWith(JUnit4.class)
public class InlineCodesCheckerTest {
	
	private static final LocaleId SOURCE_LOCALE = LocaleId.ENGLISH;
	private static final LocaleId TARGET_LOCALE = LocaleId.FRENCH;
	private InlineCodesChecker checker;
	private List<Issue> issues;

	@Before
	public void setUp() {
		checker = new InlineCodesChecker();
		issues = new ArrayList<>();
	}

	@Test
	public void testOrderNoStrictOrder () {
		Parameters params = new Parameters();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", null, 1);
		srcFrag.append(TagType.OPENING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "a", null, 1);
		
		// No error
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());

		// Wrong nesting (but open/close sequence valid, albeit incorrect)
		trgTc = new TextContainer();
		trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size()); // No strict order, no issue
	}

	@Test
	public void testWithAndWithoutSkip () {
		Parameters params = new Parameters();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		// Source with 2 segments
		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		Segment seg = new Segment("s1");
		TextFragment tf = seg.getContent();
		tf.append(TagType.OPENING, "a", null, 1);
		tf.append(TagType.PLACEHOLDER, "b", null, 2);
		tf.append(TagType.CLOSING, "a", null, 1);
		srcTc.append(seg);
		seg = new Segment("s2");
		tf = seg.getContent();
		tf.append(TagType.OPENING, "c", null, 1);
		tf.append(TagType.CLOSING, "c", null, 1);
		srcTc.append(seg);
		
		TextContainer trgTc = new TextContainer();
		seg = new Segment("s1");
		tf = seg.getContent();
		tf.append(TagType.OPENING, "a", null, 1);
		// Missing placeholder b
		tf.append(TagType.CLOSING, "a", null, 1);
		trgTc.append(seg);
		seg = new Segment("s2");
		tf = seg.getContent();
		tf.append(TagType.OPENING, "c", null, 1);
		// Missing closing c
		trgTc.append(seg);
		tu.setTarget(TARGET_LOCALE, trgTc);

		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(2, issues.size());
		assertEquals("s1", issues.get(0).getSegId());
		assertEquals(IssueType.MISSING_CODE, issues.get(0).getIssueType());
		assertEquals("s2", issues.get(1).getSegId());
		assertEquals(IssueType.MISSING_CODE, issues.get(1).getIssueType());

		// Add skip annotation on the second segment
		TextPart tp  = srcTc.getParts().get(1);
		tp.setAnnotation(new SkipCheckAnnotation());
		
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertEquals("s1", issues.get(0).getSegId());
		assertEquals(IssueType.MISSING_CODE, issues.get(0).getIssueType());

		// Add skip annotation on the first segment
		tp  = srcTc.getParts().get(0);
		tp.setAnnotation(new SkipCheckAnnotation());
		
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());
	}

	@Test
	public void testMissingDeleteableCode () {
		Parameters params = new Parameters();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", null, 1);
		srcFrag.append(TagType.OPENING, "b", null, 2);
		Code c1 = new Code(TagType.PLACEHOLDER, "p", "<p/>");
		c1.setDeleteable(true);
		c1.setId(22);
		srcFrag.append(c1);
		srcFrag.append(TagType.CLOSING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "a", null, 1);
		
		// No error (missing code can be deleted)
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());
	}

	@Test
	public void testMissingNoDeleteableCode () {
		Parameters params = new Parameters();
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", null, 1);
		srcFrag.append(TagType.OPENING, "b", null, 2);
		srcFrag.append(TagType.PLACEHOLDER, "p", "<p/>", 33);
		srcFrag.append(TagType.CLOSING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "a", null, 1);
		
		// Error: missing code cannot be deleted
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("Missing placeholders in the target: \"<p/>\""));
	}

	@Test
	public void testMissingNoDeleteableCodeUsingGeneric () {
		Parameters params = new Parameters();
		params.setUseGenericCodes(true);
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", null, 1);
		srcFrag.append(TagType.OPENING, "b", null, 2);
		srcFrag.append(TagType.PLACEHOLDER, "p", "<p/>", 33);
		srcFrag.append(TagType.CLOSING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "a", null, 1);
		
		// Error: missing code cannot be deleted
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("Missing placeholders in the target: <33/>"));
	}

	@Test
	public void testMissingSimilarUsingGeneric () {
		Parameters params = new Parameters();
		params.setUseGenericCodes(true);
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", "<a>", 1);
		srcFrag.append(TagType.CLOSING, "a", "</a>", 1);
		srcFrag.append(TagType.OPENING, "b", "<b>", 2);
		srcFrag.append(TagType.CLOSING, "b", "</b>", 2);
		srcFrag.append(TagType.OPENING, "b", "<b>", 3);
		srcFrag.append(TagType.CLOSING, "b", "</b>", 3);

		// Missing first <b></b>
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", "<a>", 1);
		trgFrag.append(TagType.CLOSING, "a", "</a>", 1);
		trgFrag.append(TagType.OPENING, "b", "<b>", 3);
		trgFrag.append(TagType.CLOSING, "b", "</b>", 3);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("Missing placeholders in the target: <2>, </2>"));
	}

	@Test
	public void testOrderWithStrictOrder () {
		Parameters params = new Parameters();
		params.setStrictCodeOrder(true);
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", null, 1);
		srcFrag.append(TagType.OPENING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "b", null, 2);
		srcFrag.append(TagType.CLOSING, "a", null, 1);
		
		// No error
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());

		// Wrong nesting
		trgTc = new TextContainer();
		trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("source code ID=1 (OPENING), target code ID=2 (OPENING)"));
	}

	@Test
	public void testLongSequenceWithStrictOrder () {
		Parameters params = new Parameters();
		params.setStrictCodeOrder(true);
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", null, 1);
		srcFrag.append(TagType.OPENING, "b", null, 2);
		srcFrag.append(TagType.PLACEHOLDER, "c", null, 3);
		srcFrag.append(TagType.CLOSING, "b", null, 2);
		srcFrag.append(TagType.PLACEHOLDER, "d", null, 4);
		srcFrag.append(TagType.CLOSING, "a", null, 1);
		
		// No error
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.PLACEHOLDER, "c", null, 3);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.PLACEHOLDER, "d", null, 4);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());

		// Wrong order
		trgTc = new TextContainer();
		trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", null, 1);
		trgFrag.append(TagType.OPENING, "b", null, 2);
		trgFrag.append(TagType.PLACEHOLDER, "c", null, 3);
		trgFrag.append(TagType.PLACEHOLDER, "d", null, 4);
		trgFrag.append(TagType.CLOSING, "b", null, 2);
		trgFrag.append(TagType.CLOSING, "a", null, 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("source code ID=2 (CLOSING), target code ID=4 (PLACEHOLDER)"));
	}

	@Test
	public void testDeletableCodesWithStrictOrder () {
		Parameters params = new Parameters();
		params.setStrictCodeOrder(true);
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append(TagType.OPENING, "a", "<a>", 1);
		Code c1 = new Code(TagType.PLACEHOLDER, "p", "<p/>");
		c1.setDeleteable(true);
		c1.setId(2);
		srcFrag.append(c1);
		srcFrag.append(TagType.PLACEHOLDER, "c", "<c/>", 3);
		srcFrag.append(TagType.CLOSING, "a", "</a>", 1);

		// No error: Missing code can be deleted
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", "<a>", 1);
		trgFrag.append(TagType.PLACEHOLDER, "c", "<c/>", 3);
		trgFrag.append(TagType.CLOSING, "a", "</a>", 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());

		// No error: Same codes
		trgTc = new TextContainer();
		trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", "<a>", 1);
		c1 = new Code(TagType.PLACEHOLDER, "p", "<p/>");
		c1.setDeleteable(true);
		c1.setId(2);
		trgFrag.append(c1);
		trgFrag.append(TagType.PLACEHOLDER, "c", "<c/>", 3);
		trgFrag.append(TagType.CLOSING, "a", "</a>", 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(0, issues.size());

		// Error: Code that can be deleted has been moved
		trgTc = new TextContainer();
		trgFrag = trgTc.getFirstContent();
		trgFrag.append(TagType.OPENING, "a", "<a>", 1);
		trgFrag.append(TagType.PLACEHOLDER, "c", "<c/>", 3);
		c1 = new Code(TagType.PLACEHOLDER, "p", "<p/>");
		c1.setDeleteable(true);
		c1.setId(2);
		trgFrag.append(c1);
		trgFrag.append(TagType.CLOSING, "a", "</a>", 1);
		tu.setTarget(TARGET_LOCALE, trgTc);
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("source code ID=1 (CLOSING), target code ID=2 (PLACEHOLDER)"));
	}

	@Test
	public void testMissingCodeWithEmptyType () {
		Parameters params = new Parameters();
		params.setUseGenericCodes(true);
		checker.startProcess(SOURCE_LOCALE, TARGET_LOCALE, params, issues);

		ITextUnit tu = new TextUnit();
		TextContainer srcTc = tu.getSource();
		TextFragment srcFrag = srcTc.getFirstContent();
		srcFrag.append("src");
		// Code with a type set to empty string (some SDLXLIFF files have such cases)
		srcFrag.append(TagType.OPENING, "", null, 1);
		srcFrag.append(TagType.CLOSING, "", null, 1);
		
		TextContainer trgTc = new TextContainer();
		TextFragment trgFrag = trgTc.getFirstContent();
		// Missing codes
		trgFrag.append("trg");
		tu.setTarget(TARGET_LOCALE, trgTc);
		
		issues.clear();
		checker.processTextUnit(tu);
		assertEquals(1, issues.size());
		assertTrue(issues.get(0).getMessage().contains("Missing placeholders in the target: <1>, </1>"));
	}

}
