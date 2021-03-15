package net.sf.okapi.steps.whitespacecorrection;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.steps.whitespacecorrection.WhitespaceCorrector.Punctuation;
import static net.sf.okapi.steps.whitespacecorrection.WhitespaceCorrector.ALL_WHITESPACE;

@RunWith(JUnit4.class)
public class WhitespaceCorrectionStepParametersTest {
    private static final String PARAMETERS = "#v1\npunctuation=FULL_STOP,COMMA,EXCLAMATION_MARK,QUESTION_MARK\nwhitespace=LINE_FEED,LINE_TABULATION,FORM_FEED,CARRIAGE_RETURN,NEXT_LINE,LINE_SEPARATOR,PARAGRAPH_SEPARATOR,CHARACTER_TABULATION,SPACE,NO_BREAK_SPACE,EN_QUAD,EM_QUAD,EN_SPACE,EM_SPACE,THREE_PER_EM_SPACE,FOUR_PER_EM_SPACER,SIX_PER_EM_SPACE,FIGURE_SPACE,PUNCUATION_SPACE,THIS_SPACE,HAIR_SPACE,NAORROW_NO_BREAK_SPACE,MEDIUM_MATHEMATICAL_SPACE,IDEOGRAPHIC_SPACE,ZERO_WIDTH_SPACE,ZERO_WIDTH_NON_BREAKING_SPACE";

    @Test
    public void testSave() {
        WhitespaceCorrectionStepParameters params = new WhitespaceCorrectionStepParameters();
        params.setPunctuation(Arrays.asList(Punctuation.values()));
        assertEquals(PARAMETERS, params.toString());
    }

    @Test
    public void testLoad() {
        WhitespaceCorrectionStepParameters params = new WhitespaceCorrectionStepParameters();
        params.fromString(PARAMETERS);
        EnumSet<Punctuation> punctuation = EnumSet.allOf(Punctuation.class);
        assertEquals(punctuation, params.getPunctuation());
    }

    @Test
    public void testWhitespace() {
        WhitespaceCorrectionStepParameters params = new WhitespaceCorrectionStepParameters();
        assertEquals(ALL_WHITESPACE, params.getWhitespace());
        assertTrue(params.getVerticalWhitespace());
        assertTrue(params.getHorizontalTabs());
        assertTrue(params.getOther());
        assertTrue(params.getSpace());
        assertTrue(params.getNonbreakingWhitespace());
        params.setVerticalWhitespace(false);
        params.setNonbreakingWhitespace(false);
        params.setSpace(false);
        params.setOther(false);
        params.setHorizontalTabs(false);
        assertFalse(params.getVerticalWhitespace());
        assertFalse(params.getHorizontalTabs());
        assertFalse(params.getOther());
        assertFalse(params.getSpace());
        assertFalse(params.getNonbreakingWhitespace());
        assertEquals(Collections.emptySet(), params.getWhitespace());
    }
}
