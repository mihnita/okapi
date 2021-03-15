/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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
package net.sf.okapi.common.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextPart;

/**
 * Convenient methods for filter unit tests.
 *
 */
public class FilterTestUtil {
    /**
     * Asserts that the given string is found in a translatable Text Unit in the list.
     * @param tus List of Text Units
     * @param expectedString The string that should be found in a translatable TU
     */
    public static void assertTUListContains(List<ITextUnit> tus, String expectedString) {
        for(ITextUnit tu: tus) {
            if (tu.isTranslatable()) {
                for(TextPart tp: tu.getSource().getParts()) {
                    if (tp.text.getText().contains(expectedString)) {
                        return;
                    }
                }
            }
        }
        fail(String.format("Some Text Unit should contain \"%s\" in their translatable parts." , expectedString));
    }

    /**
     * Asserts that the given string is not found in any translatable Text Unit in the list.
     * @param tus List of Text Units
     * @param excludedString The string that should not be found
     */
    public static void assertTUListDoesNotContain(List<ITextUnit> tus, String excludedString) {
        for(ITextUnit tu: tus) {
            if (tu.isTranslatable()) {
                for(TextPart tp: tu.getSource().getParts()) {
                    if (tp.text.getText().contains(excludedString)) {
                        fail(String.format("No Text Unit should NOT contain \"%s\" in their translatable parts" , excludedString));
                    }
                }
            }
        }
    }

    /**
     * Asserts that the event is a translatable Text Unit of the given string and codes.
     * @param event The Event that should be a Text Unit
     * @param content The string that the Text Unit's toString() is expected to return
     * @param codes The list of expected codes
     */
    public static void assertTextUnit(Event event, String content, String... codes) {
        assertEquals(EventType.TEXT_UNIT, event.getEventType());
        assertTrue(event.getTextUnit().isTranslatable());
        assertTextUnit(event.getTextUnit(), content, codes);
    }

    /**
     * Asserts that the Text Unit is of the given string and codes.
     * @param tu The Text Unit to test
     * @param content The string that the Text Unit's toString() is expected to return
     * @param codes The list of expected codes
     */
    public static void assertTextUnit(ITextUnit tu, String content, String... codes) {
        assertEquals("text unit content", content, tu.toString());
        assertEquals("#codes", codes.length, getNumCodes(tu));
        for (int i = 0; i < codes.length; i++) {
            assertEquals("code#" + i, codes[i], getCodeString(tu, i));
        }
    }

    private static int getNumCodes(ITextUnit tu) {
        return tu.getSource().getFirstContent().getCodes().size();
    }

    private static String getCodeString(ITextUnit tu, int index) {
        return tu.getSource().getFirstContent().getCodes().get(index).toString();
    }

    /**
     * Asserts the event is a document part with the given content.
     * @param event The Event that should be a Document Part
     * @param content The expected string of the Document Part
     */
    public static void assertDocumentPart(Event event, String content) {
        assertEquals(EventType.DOCUMENT_PART, event.getEventType());
        assertEquals(content, event.getDocumentPart().toString());
    }

}
