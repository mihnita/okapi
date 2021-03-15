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

import java.util.ArrayList;
import java.util.List;

import static net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector.ALL_NODES_SELECTOR;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector.NodeType;

/**
 * Class for path selector utilities.
 *
 * @author Vladyslav Mykhalets
 */
public class PathSelectorUtils {

    public static boolean containsFile(XliffWalkerPathSelector pathSelector, String fileId) {

        return containsNode(pathSelector, NodeType.FILE, fileId);
    }

    public static boolean containsUnit(XliffWalkerPathSelector pathSelector, String fileId, String unitId) {

        boolean containsFile = containsNode(pathSelector, NodeType.FILE, fileId);
        if (containsFile) {

            for (XliffWalkerPathSelector unitPathSelector : pathSelector.getChildrenNodes()) {
                boolean containsUnit = containsNode(unitPathSelector, NodeType.UNIT, unitId);
                if (containsUnit) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean containsSegment(XliffWalkerPathSelector pathSelector, String fileId, String unitId, int segmentIndex) {

        boolean containsFile = containsNode(pathSelector, NodeType.FILE, fileId);
        if (containsFile) {

            for (XliffWalkerPathSelector unitPathSelector : pathSelector.getChildrenNodes()) {
                boolean containsUnit = containsNode(unitPathSelector, NodeType.UNIT, unitId);
                if (containsUnit) {

                    for (XliffWalkerPathSelector segmentPathSelector : unitPathSelector.getChildrenNodes()) {
                        boolean containsSegment = containsNode(segmentPathSelector, NodeType.SEGMENT, String.valueOf(segmentIndex));
                        if (containsSegment) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static String[] resolveFileNodeIds(XLIFFDocument doc, XliffWalkerPathSelector pathSelector) {

        if (pathSelector.getNodeType() != XliffWalkerPathSelector.NodeType.FILE) {
            throw new IllegalStateException("Expected path selector of NodeType " + XliffWalkerPathSelector.NodeType.FILE);
        }

        String[] fileNodeIds;

        if (pathSelector.getNodeId().equals(ALL_NODES_SELECTOR)) {
            List<String> fileNodeIdsList = doc.getFileNodeIds();
            fileNodeIds = fileNodeIdsList.toArray(new String[0]);
        } else {
            fileNodeIds = new String[]{pathSelector.getNodeId()};
        }

        return fileNodeIds;
    }

    public static UnitNode[] resolveUnitNodes(FileNode fileNode, XliffWalkerPathSelector pathSelector) {

        if (pathSelector.getNodeType() != XliffWalkerPathSelector.NodeType.UNIT) {
            throw new IllegalStateException("Expected path selector of NodeType " + XliffWalkerPathSelector.NodeType.UNIT);
        }

        UnitNode[] unitNodes;

        if (pathSelector.getNodeId().equals(ALL_NODES_SELECTOR)) {
            List<UnitNode> unitNodesList = fileNode.getUnitNodes();
            unitNodes = unitNodesList.toArray(new UnitNode[0]);
        } else {
            unitNodes = new UnitNode[]{fileNode.getUnitNode(pathSelector.getNodeId())};
        }

        return unitNodes;
    }

    @SuppressWarnings("unchecked")
    public static Pair<Integer, Segment>[] resolveSegments(UnitNode unitNode, XliffWalkerPathSelector pathSelector) {

        if (pathSelector.getNodeType() != XliffWalkerPathSelector.NodeType.SEGMENT) {
            throw new IllegalStateException("Expected path selector of NodeType " + XliffWalkerPathSelector.NodeType.SEGMENT);
        }

        Unit unit = unitNode.get();
        Pair<Integer, Segment>[] indexedSegments;

        if (pathSelector.getNodeId().equals(ALL_NODES_SELECTOR)) {
            int segmentIndex = 0;
            List<Pair<Integer, Segment>> indexedSegmentsList = new ArrayList<>();
            for (Segment segment : unit.getSegments()) {
                indexedSegmentsList.add(new Pair<>(segmentIndex, segment));
                segmentIndex++;
            }
            indexedSegments = indexedSegmentsList.toArray(new Pair[0]);
        } else {
            int segmentIndex = Integer.parseInt(pathSelector.getNodeId());
            Pair<Integer, Segment> pair = new Pair<>(segmentIndex, unit.getSegment(segmentIndex));
            indexedSegments = new Pair[]{pair};
        }

        return indexedSegments;
    }

    public static class Pair<L, R> {

        private L left;
        private R right;

        Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }
    }

    private static boolean containsNode(XliffWalkerPathSelector pathSelector, NodeType nodeType, String nodeId) {

        if (pathSelector == null || nodeType == null || nodeId == null) {
            throw new IllegalArgumentException("Method arguments can not be NULL");
        }

        return pathSelector.getNodeType() == nodeType && (ALL_NODES_SELECTOR.equals(pathSelector.getNodeId()) || nodeId.equals(pathSelector.getNodeId()));
    }
}
