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

package net.sf.okapi.lib.xliff2.walker;

import net.sf.okapi.lib.xliff2.document.FileNode;

/** 
 * Abstract class for Segment visitors to be derived from. 
 * 
 * This is a bit of a work-around, because it is not possible in 
 * Java generics to limit the type of an interface to a set of 
 * given interfaces. 
 * 
 * @author Martin Wunderlich
 *
 */
public abstract class AbstractFileVisitor implements IXliffVisitor<FileNode>{

	@Override
	abstract public void visit(FileNode visitee, VisitationContext context);
}
