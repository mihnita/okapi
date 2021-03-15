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

import java.util.Iterator;

import static net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.resolveFileNodeIds;
import static net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.resolveSegments;
import static net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.resolveUnitNodes;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.walker.VisitationContext;
import net.sf.okapi.lib.xliff2.walker.XliffWalker;
import net.sf.okapi.lib.xliff2.walker.XliffWalker.VisitPlaceAwareXliffVisitor;
import net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils.Pair;
import net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector;

/**
 * Xliff walker strategy with ordered pipeline of visitors.
 * Visitors will be applied in the order they were added to the Xliff walker.
 * It may be useful when next visitor execution depends on the results of execution of the previous visitor,
 * i.e. unit visitor execution may depend on changes done by segment visitor.
 *
 * @author Vladyslav Mykhalets
 */
public class PipelineXliffWalkerStrategy implements IXliffWalkerStrategy {

    @Override
    public void doWalk(XLIFFDocument doc, XliffWalker walker) {

        if (doc == null) {
            throw new IllegalArgumentException("A valid XLIFF document must be provided.");
        }

        Iterator<String> visitorIterator = walker.visitorsIterator();

        while (visitorIterator.hasNext()) {
            String visitorId = visitorIterator.next();

            VisitPlaceAwareXliffVisitor<FileNode> fileVisitor = walker.getFileVisitor(visitorId);
            if (fileVisitor != null) {
                doWalkFileNodes(doc, fileVisitor);
                continue;
            }

            VisitPlaceAwareXliffVisitor<UnitNode> unitVisitor = walker.getUnitVisitor(visitorId);
            if (unitVisitor != null) {
                doWalkUnitNodes(doc, unitVisitor);
                continue;
            }

            VisitPlaceAwareXliffVisitor<Segment> segmentVisitor = walker.getSegmentVisitor(visitorId);
            if (segmentVisitor != null) {
                doWalkSegments(doc, segmentVisitor);
            }
        }
    }

    public static void doWalkFileNodes(XLIFFDocument doc, VisitPlaceAwareXliffVisitor<FileNode> visitor) {

        if (visitor.getSelectors() != null) {

            for (XliffWalkerPathSelector fileNodePathSelector : visitor.getSelectors()) {
                String[] fileNodeIds = resolveFileNodeIds(doc, fileNodePathSelector);

                for (String fileNodeID : fileNodeIds) {
                    FileNode fileNode = doc.getFileNode(fileNodeID);
                    visitor.visit(fileNode, new VisitationContext(fileNodeID, fileNode));
                }
            }
        } else {
            for (String fileNodeID : doc.getFileNodeIds()) {
                FileNode fileNode = doc.getFileNode(fileNodeID);
                visitor.visit(fileNode, new VisitationContext(fileNodeID, fileNode));
            }
        }
    }

    public static void doWalkUnitNodes(XLIFFDocument doc, VisitPlaceAwareXliffVisitor<UnitNode> visitor) {

        if (visitor.getSelectors() != null) {

            for (XliffWalkerPathSelector fileNodePathSelector : visitor.getSelectors()) {
                String[] fileNodeIds = resolveFileNodeIds(doc, fileNodePathSelector);

                for (String fileNodeID : fileNodeIds) {
                    FileNode fileNode = doc.getFileNode(fileNodeID);

                    for (XliffWalkerPathSelector unitNodePathSelector : fileNodePathSelector.getChildrenNodes()) {
                        UnitNode[] unitNodes = resolveUnitNodes(fileNode, unitNodePathSelector);

                        for (UnitNode unitNode : unitNodes) {
                            visitor.visit(unitNode, new VisitationContext(fileNodeID, fileNode, unitNode));
                        }
                    }
                }
            }
        } else {
            for (String fileNodeID : doc.getFileNodeIds()) {
                FileNode fileNode = doc.getFileNode(fileNodeID);

                for (UnitNode unitNode : fileNode.getUnitNodes()) {
                    visitor.visit(unitNode, new VisitationContext(fileNodeID, fileNode, unitNode));
                }
            }
        }
    }

    public static void doWalkSegments(XLIFFDocument doc, VisitPlaceAwareXliffVisitor<Segment> visitor) {

        if (visitor.getSelectors() != null) {

            for (XliffWalkerPathSelector fileNodePathSelector : visitor.getSelectors()) {
                String[] fileNodeIds = resolveFileNodeIds(doc, fileNodePathSelector);

                for (String fileNodeID : fileNodeIds) {
                    FileNode fileNode = doc.getFileNode(fileNodeID);

                    for (XliffWalkerPathSelector unitNodePathSelector : fileNodePathSelector.getChildrenNodes()) {
                        UnitNode[] unitNodes = resolveUnitNodes(fileNode, unitNodePathSelector);

                        for (UnitNode unitNode : unitNodes) {

                            for (XliffWalkerPathSelector segmentPathSelector : unitNodePathSelector.getChildrenNodes()) {
                                Pair<Integer, Segment>[] indexedSegments = resolveSegments(unitNode, segmentPathSelector);

                                for (Pair<Integer, Segment> indexedSegment : indexedSegments) {
                                    visitor.visit(indexedSegment.getRight(), new VisitationContext(fileNodeID, fileNode, unitNode, indexedSegment.getLeft()));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for (String fileNodeID : doc.getFileNodeIds()) {
                FileNode fileNode = doc.getFileNode(fileNodeID);

                for (UnitNode unitNode : fileNode.getUnitNodes()) {
                    Unit unit = unitNode.get();

                    int segmentIndex = 0;
                    for (Segment segment : unit.getSegments()) {
                        visitor.visit(segment, new VisitationContext(fileNodeID, fileNode, unitNode, segmentIndex));
                        segmentIndex++;
                    }
                }
            }
        }
    }
}
