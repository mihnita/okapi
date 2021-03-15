/*===========================================================================
  Copyright (C) 2011-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.walker.strategy;

import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.walker.IXliffVisitor;
import net.sf.okapi.lib.xliff2.walker.VisitationContext;
import net.sf.okapi.lib.xliff2.walker.XliffWalker;

/**
 * Implementation of {@link IXliffWalkerStrategy} which enables traversal over all XLIFF document nodes.
 *
 * @author Martin Wunderlich
 */
public class DefaultXliffWalkerStrategy implements IXliffWalkerStrategy {

    @Override
    public void doWalk(XLIFFDocument doc, XliffWalker walker) {

        if (doc == null) {
            throw new IllegalArgumentException("A valid XLIFF document must be provided.");
        }

        for (String fileNodeID : doc.getFileNodeIds()) {
            FileNode fileNode = doc.getFileNode(fileNodeID);

            for (IXliffVisitor<FileNode> visitor : walker.getFileNodeVisitors(fileNodeID)) {
                visitor.visit(fileNode, new VisitationContext(fileNodeID, fileNode));
            }

            for (UnitNode unitNode : fileNode.getUnitNodes()) {
                Unit unit = unitNode.get();

                for (IXliffVisitor<UnitNode> visitor : walker.getUnitNodeVisitors(fileNodeID, unit.getId())) {
                    visitor.visit(unitNode, new VisitationContext(fileNodeID, fileNode, unitNode));
                }

                int segmentIndex = 0;
                for (Segment segment : unit.getSegments()) {
                    for (IXliffVisitor<Segment> visitor : walker.getSegmentVisitors(fileNodeID, unit.getId(), segmentIndex)) {
                        visitor.visit(segment, new VisitationContext(fileNodeID, fileNode, unitNode, segmentIndex));
                    }

                    segmentIndex++;
                }
            }
        }
    }
}
