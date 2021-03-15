/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.walker.selector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Class for testing of path selector utils.
 *
 * @author Vladyslav Mykhalets
 */
@RunWith(JUnit4.class)
public class PathSelectorUtilsTest {

    @Test
    public void testPathSelector() {

        XliffWalkerPathSelector[] pathSelectors = new XliffWalkerPathSelector.Builder()
                .selector("f1", "tu1")
                .selector("f1", "tu2", 0)
                .selector("f1", "tu3", 1)
                .build();

        assertEquals(1, pathSelectors.length);

        boolean containsFile = PathSelectorUtils.containsFile(pathSelectors[0], "f1");
		assertTrue(containsFile);

        containsFile = PathSelectorUtils.containsFile(pathSelectors[0], "f2");
		assertFalse(containsFile);

        boolean containsUnit = PathSelectorUtils.containsUnit(pathSelectors[0], "f1", "tu1");
		assertTrue(containsUnit);

        containsUnit = PathSelectorUtils.containsUnit(pathSelectors[0], "f1", "tu2");
		assertTrue(containsUnit);

        containsUnit = PathSelectorUtils.containsUnit(pathSelectors[0], "f1", "tu3");
		assertTrue(containsUnit);

        boolean containsSegment = PathSelectorUtils.containsSegment(pathSelectors[0], "f1", "tu1", 0);
		assertTrue(containsSegment);

        containsSegment = PathSelectorUtils.containsSegment(pathSelectors[0], "f1", "tu1", 1);
		assertTrue(containsSegment);

        containsSegment = PathSelectorUtils.containsSegment(pathSelectors[0], "f1", "tu2", 0);
		assertTrue(containsSegment);

        containsSegment = PathSelectorUtils.containsSegment(pathSelectors[0], "f1", "tu2", 1);
		assertFalse(containsSegment);

        containsSegment = PathSelectorUtils.containsSegment(pathSelectors[0], "f1", "tu3", 1);
		assertTrue(containsSegment);

        containsSegment = PathSelectorUtils.containsSegment(pathSelectors[0], "f1", "tu3", 0);
		assertFalse(containsSegment);
    }

}
