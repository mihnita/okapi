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

package net.sf.okapi.connectors.googleautoml.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PredictAPIUtilTest {
    private PredictAPIUtil util;

    @Before
    public void setup() {
        util = new PredictAPIUtil();
    }

    @Test
    public void testGetPredictRequestsNoSplit() {
        String sourceText = "Source text";
        List<JSONObject> predictRequests = util.getPredictRequests(sourceText);
        assertEquals(1, predictRequests.size());
        assertTrue(predictRequests.get(0).toJSONString().contains(String.format("\"content\":\"%s\"", sourceText)));
    }

    @Test
    public void testGetPredictRequestsWithSplit() {
        String part1 = getRepeatedString(PredictAPIUtil.CONTENT_CHAR_LIMIT, 'a');
        String part2 = getRepeatedString(PredictAPIUtil.CONTENT_CHAR_LIMIT / 2, 'b');
        String sourceText = part1 + part2;

        List<JSONObject> predictRequests = util.getPredictRequests(sourceText);
        assertEquals(2, predictRequests.size());
        assertTrue(predictRequests.get(0).toJSONString().contains(String.format("\"content\":\"%s\"", part1)));
        assertTrue(predictRequests.get(1).toJSONString().contains(String.format("\"content\":\"%s\"", part2)));
    }

    @Test
    public void testExtractTranslation() throws ParseException {
        String json = "{\"payload\":[{\"translation\":{\"translatedContent\":{\"content\":\"Hello world!\"}}}]}";
        assertEquals("Hello world!", util.extractTranslation(new ByteArrayInputStream(json.getBytes())));
    }

    /**
     * Creates a new string with 'n' occurrences of the character 'c'.
     */
    private String getRepeatedString(int n, char c) {
        char[] array = new char[n];
        Arrays.fill(array, c);
        return new String(array);
    }
}
