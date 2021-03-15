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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

@RunWith(JUnit4.class)
public class AbstractCheckerTests {
	
	@Test
	public void testFromFragmentToGeneric() {
		TextFragment frag = new TextFragment("abc");
		frag.append(TagType.PLACEHOLDER, "ph", "[PHLDR/]");
		frag.append("def");
		frag.append(TagType.PLACEHOLDER, "ph", "[PHLDR/]");
		frag.append("ghi");
		// abc##def##ghi c=2, d=5, e=6, i=12
		// abc<1/>def<2/>ghi c=2, d=7 e=8, i=16
		assertEquals(2, AbstractChecker.fromFragmentToGeneric(frag, 2));
		assertEquals(7, AbstractChecker.fromFragmentToGeneric(frag, 5));
		assertEquals(8, AbstractChecker.fromFragmentToGeneric(frag, 6));
		assertEquals(16, AbstractChecker.fromFragmentToGeneric(frag, 12));
	}

	@Test
	public void testFromOriginalToGeneric() {
		TextFragment frag = new TextFragment("abc");
		frag.append(TagType.PLACEHOLDER, "ph", "[PHLDR/]");
		frag.append("def");
		frag.append(TagType.PLACEHOLDER, "ph", "[PHLDR/]");
		frag.append("ghi");
		// abc[PHLDR/]def[PHLDR/]ghi c=2, d=11, e=12, i=24  
		// abc##def##ghi c=2, d=5, e=6, i=12
		// abc<1/>def<2/>ghi c=2, d=7 e=8, i=16
		assertEquals(2, AbstractChecker.fromOriginalToGeneric(frag, 2));
		assertEquals(7, AbstractChecker.fromOriginalToGeneric(frag, 11));
		assertEquals(8, AbstractChecker.fromOriginalToGeneric(frag, 12));
		assertEquals(16, AbstractChecker.fromOriginalToGeneric(frag, 24));
	}

}
