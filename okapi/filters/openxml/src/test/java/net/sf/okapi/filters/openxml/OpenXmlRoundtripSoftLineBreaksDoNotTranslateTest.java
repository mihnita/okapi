package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.LocaleId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests the combination of enabled {@link ConditionalParameters#ADDLINESEPARATORASCHARACTER} and
 * excluded styles from translation.
 */
@RunWith(Parameterized.class)
public class OpenXmlRoundtripSoftLineBreaksDoNotTranslateTest extends AbstractOpenXMLRoundtripTest {

    private String filename;

    public OpenXmlRoundtripSoftLineBreaksDoNotTranslateTest(String filename) {
        this.filename = filename;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static String[] data() {
        return new String[] {
                "OpenXmlRoundtripSoftLineBreaksDoNotTranslateTestParagraphStyle.docx",
                "OpenXmlRoundtripSoftLineBreaksDoNotTranslateTestCharacterStyle.docx"
        };
    }

    @Test
    public void test() {
        ConditionalParameters params = new ConditionalParameters();
        params.setAddLineSeparatorCharacter(true);
        params.tsExcludeWordStyles.add("tw4winExternal");

        this.allGood = true;
        runOneTest(filename, true, false, params, "softLineBreakDoNotTranslate/");
        assertTrue(this.allGood);
    }
}
