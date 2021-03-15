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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.exceptions.OkapiException;

@RunWith(JUnit4.class)
public class ModelMapUtilTest {
    private ModelMapUtil util;

    @Before
    public void setup() {
        util = new ModelMapUtil();
    }

    @Test
    public void testGetModelResourceName() {
        String modelMap = "{\"en-US/ja-JP\": \"projects/my-project/locations/us-central1/models/ABC123\", "
                + "\"en-US/de-DE\": \"projects/my-project/locations/us-central1/models/DEF456\"}";
        util.setMap(modelMap);

        assertEquals("projects/my-project/locations/us-central1/models/ABC123",
            util.getModelResourceName("en-US", "ja-JP"));
        assertEquals("projects/my-project/locations/us-central1/models/DEF456",
            util.getModelResourceName("en-US", "de-DE"));
        assertEquals("projects/my-project/locations/us-central1/models/DEF456",
            util.getModelResourceName("eN-us", "DE-de")); // Not case-sensitive
    }

    @Test(expected = OkapiException.class)
    public void testGetModelResourceNameFailure() {
        String modelMap = "{\"en-US/ja-JP\": \"projects/my-project/locations/us-central1/models/ABC123\", "
                + "\"en-US/de-DE\": \"projects/my-project/locations/us-central1/models/DEF456\"}";
        util.setMap(modelMap);
        util.getModelResourceName("en-US", "fr-FR");
    }
}
