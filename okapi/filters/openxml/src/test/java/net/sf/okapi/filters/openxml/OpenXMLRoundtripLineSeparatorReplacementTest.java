package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.LocaleId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests roundtrip of a line break replaced by a character.
 */
@RunWith(Parameterized.class)
public class OpenXMLRoundtripLineSeparatorReplacementTest extends AbstractOpenXMLRoundtripTest {

    private String resourceName;

    public OpenXMLRoundtripLineSeparatorReplacementTest(String resourceName) {
        this.resourceName = resourceName;
    }

    @Parameters(name = "{0}")
    public static String[] data() {
        return new String[] {
                "leading_line_breaks.pptx",
                "special-chars-and-linebreaks.docx"
        };
    }

    @Test
    public void test() {
        ConditionalParameters params = new ConditionalParameters();
        params.setAddLineSeparatorCharacter(true);
        params.setTranslateDocProperties(false);
        params.setTranslatePowerpointMasters(false);

        runOneTest(resourceName, true, false, params, "lineseparator/");
        assertTrue(this.allGood);
    }
}
