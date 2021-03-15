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

package net.sf.okapi.lib.xliff2.walker.strategy;

import java.util.List;

import static net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.resolveFileNodeIds;
import static net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.resolveSegments;
import static net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.resolveUnitNodes;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.walker.IXliffVisitor;
import net.sf.okapi.lib.xliff2.walker.VisitationContext;
import net.sf.okapi.lib.xliff2.walker.XliffWalker;
import net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector;
import net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.Pair;

/**
 * Implementation of {@link IXliffWalkerStrategy} which enables traversal over XLIFF document nodes specified by path selectors {@link XliffWalkerPathSelector}.
 * This implementation tries to avoid traversal over not interested document nodes where possible.
 *
 * @author Vladyslav Mykhalets
 */
public class FlexibleXliffWalkerStrategy implements IXliffWalkerStrategy {

    private final List<XliffWalkerPathSelector> pathSelectors;

    FlexibleXliffWalkerStrategy(List<XliffWalkerPathSelector> pathSelectors) {
        this.pathSelectors = pathSelectors;
    }

    @Override
    public void doWalk(XLIFFDocument doc, XliffWalker walker) {

        if (doc == null) {
            throw new IllegalArgumentException("A valid XLIFF document must be provided.");
        }

        for (XliffWalkerPathSelector fileNodePathSelector : pathSelectors) {
            String[] fileNodeIds = resolveFileNodeIds(doc, fileNodePathSelector);

            for (String fileNodeID : fileNodeIds) {
                FileNode fileNode = doc.getFileNode(fileNodeID);

                // visiting file node
                for (IXliffVisitor<FileNode> visitor : walker.getFileNodeVisitors(fileNodeID)) {
                    visitor.visit(fileNode, new VisitationContext(fileNodeID, fileNode));
                }

                for (XliffWalkerPathSelector unitNodePathSelector : fileNodePathSelector.getChildrenNodes()) {
                    UnitNode[] unitNodes = resolveUnitNodes(fileNode, unitNodePathSelector);

                    for (UnitNode unitNode : unitNodes) {

                        // visiting unit node
                        for (IXliffVisitor<UnitNode> visitor : walker.getUnitNodeVisitors(fileNodeID, unitNode.get().getId())) {
                            visitor.visit(unitNode, new VisitationContext(fileNodeID, fileNode, unitNode));
                        }

                        for (XliffWalkerPathSelector segmentPathSelector : unitNodePathSelector.getChildrenNodes()) {
                            Pair<Integer, Segment>[] indexedSegments = resolveSegments(unitNode, segmentPathSelector);

                            for (Pair<Integer, Segment> indexedSegment : indexedSegments) {

                                // visiting segments
                                for (IXliffVisitor<Segment> visitor : walker.getSegmentVisitors(fileNodeID, unitNode.get().getId(), indexedSegment.getLeft())) {
                                    visitor.visit(indexedSegment.getRight(), new VisitationContext(fileNodeID, fileNode, unitNode, indexedSegment.getLeft()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
