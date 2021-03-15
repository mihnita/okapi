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
import net.sf.okapi.lib.xliff2.document.UnitNode;

/**
 * Provides the context for visiting the 
 * visitee during traversal of the XLIFF tree.
 * 
 * The context consists of file, unit, and segment index, 
 * of which only the file is required. The other may be null.  
 * 
 * @author Martin Wunderlich
 *
 */
public class VisitationContext {

	private final Integer segmentIndex;
	private final String fileNodeId;
	private final FileNode fileNode;
	private final UnitNode unitNode;


	public VisitationContext(String fileNodeId, FileNode fileNode) {
		this.fileNodeId = fileNodeId;
		this.fileNode = fileNode;
		this.unitNode = null;
		this.segmentIndex = null;
	}

	public VisitationContext(String fileNodeId, FileNode fileNode, UnitNode unitNode) {
		this.fileNodeId = fileNodeId;
		this.fileNode = fileNode;
		this.unitNode = unitNode;
		this.segmentIndex = null;
	}

	public VisitationContext(String fileNodeId, FileNode fileNode, UnitNode unitNode, int segmentIndex) {
		this.fileNodeId = fileNodeId;
		this.fileNode = fileNode;
		this.unitNode = unitNode;
		this.segmentIndex = segmentIndex;
	}

	public String getFileNodeId() {
		return fileNodeId;
	}

	public FileNode getFile() {
		return fileNode;
	}

	public UnitNode getUnit() {
		return unitNode;
	}

	public Integer getSegmentIndex() {
		return segmentIndex;
	}
}
