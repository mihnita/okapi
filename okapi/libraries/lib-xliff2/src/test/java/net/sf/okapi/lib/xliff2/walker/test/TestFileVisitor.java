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

import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.walker.AbstractFileVisitor;
import net.sf.okapi.lib.xliff2.walker.VisitationContext;

public class TestFileVisitor extends AbstractFileVisitor {
	int fileCount = 0;
	
	@Override
	public void visit(FileNode visitee, VisitationContext context) {
		++fileCount;
	}
	
	public int getCount() {
		return fileCount;
	}
}
	