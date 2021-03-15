/*===========================================================================
  Copyright (C) 2011-2016 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.walker.test;

import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.walker.AbstractSegmentVisitor;
import net.sf.okapi.lib.xliff2.walker.VisitationContext;

public class TestModifyingSegmentVisitor extends AbstractSegmentVisitor {
	int segCount = 0;
	
	@Override
	public void visit(Segment visitee, VisitationContext context) {
		++segCount;
		
		visitee.setTarget("");
		visitee.setSource("");
		visitee.setSubState(TestConstants.SEGMENT_TEST_MARKER);
	}
	
	public int getCount() {
		return segCount;
	}
}
	