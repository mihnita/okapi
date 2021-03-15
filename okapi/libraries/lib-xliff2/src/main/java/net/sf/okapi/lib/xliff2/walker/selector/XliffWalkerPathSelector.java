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

package net.sf.okapi.lib.xliff2.walker.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class to select node paths (file node --> unit node --> segment node) in XLIFF document to build {@link FlexibleXliffWalkerStrategy} which
 * is used to traverse XLIFF document via {@link XliffWalker}
 *
 * @author Vladyslav Mykhalets
 */
public class XliffWalkerPathSelector {

    public static final String ALL_NODES_SELECTOR = "*";

    private String nodeId;

    private NodeType nodeType;

    private List<XliffWalkerPathSelector> childrenNodes;

    public String getNodeId() {
        return nodeId;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public List<XliffWalkerPathSelector> getChildrenNodes() {
        return Collections.unmodifiableList(childrenNodes);
    }

    /**
     * Enumerations for XLIFF document node type
     *
     * @author Vladyslav Mykhalets
     */
    public enum NodeType {
        FILE,
        UNIT,
        SEGMENT
    }

    /**
     * Builder class for {@link XliffWalkerPathSelector}
     *
     * @author Vladyslav Mykhalets
     */
    public static class Builder {

        Map<String, Map<String, List<String>>> selectorMap = new LinkedHashMap<>();

        public Builder allPaths() {
            return this.selector(ALL_NODES_SELECTOR);
        }

        public Builder selector(String fileId) {
            return this.selector(fileId, ALL_NODES_SELECTOR);
        }

        public Builder selector(String fileId, String unitId) {
            return this.selector(fileId, unitId, ALL_NODES_SELECTOR);
        }

        public Builder selector(String fileId, String unitId, Integer segmentIndex) {
            return this.selector(fileId, unitId, String.valueOf(segmentIndex));
        }

        private Builder selector(String fileId, String unitId, String segmentIndex) {

            selectorMap.computeIfAbsent(fileId, k -> new LinkedHashMap<>());

            selectorMap.get(fileId).computeIfAbsent(unitId, k -> new ArrayList<>());

            selectorMap.get(fileId).get(unitId).add(segmentIndex);

            return this;
        }

        public XliffWalkerPathSelector[] build() {
            List<XliffWalkerPathSelector> fileSelectorList = new ArrayList<>();

            for (String fileId : selectorMap.keySet()) {

                XliffWalkerPathSelector fileIdPathSelector = new XliffWalkerPathSelector();
                fileIdPathSelector.nodeId = fileId;
                fileIdPathSelector.nodeType = NodeType.FILE;
                fileIdPathSelector.childrenNodes = new ArrayList<>();

                for (String unitId : selectorMap.get(fileId).keySet()) {

                    XliffWalkerPathSelector unitIdPathSelector = new XliffWalkerPathSelector();
                    unitIdPathSelector.nodeId = unitId;
                    unitIdPathSelector.nodeType = NodeType.UNIT;
                    unitIdPathSelector.childrenNodes = new ArrayList<>();

                    for (String segmentIndex : selectorMap.get(fileId).get(unitId)) {

                        XliffWalkerPathSelector segmentIndexPathSelector = new XliffWalkerPathSelector();
                        segmentIndexPathSelector.nodeId = segmentIndex;
                        segmentIndexPathSelector.nodeType = NodeType.SEGMENT;

                        unitIdPathSelector.childrenNodes.add(segmentIndexPathSelector);
                    }

                    fileIdPathSelector.childrenNodes.add(unitIdPathSelector);
                }

                fileSelectorList.add(fileIdPathSelector);
            }

            return fileSelectorList.toArray(new XliffWalkerPathSelector[0]);
        }
    }
}
