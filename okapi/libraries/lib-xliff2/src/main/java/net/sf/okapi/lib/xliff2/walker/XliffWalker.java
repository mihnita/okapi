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

package net.sf.okapi.lib.xliff2.walker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.walker.selector.PathSelectorUtils;
import net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector;
import net.sf.okapi.lib.xliff2.walker.strategy.DefaultXliffWalkerStrategy;
import net.sf.okapi.lib.xliff2.walker.strategy.IXliffWalkerStrategy;

/**
 * Allows for traversal of the tree structure of an XLIFF document,
 * in a fashion similar to visitor pattern.
 *
 * Three types of visitor can be added to the XliffWalker: File visitors, Unit visitors, and Segment visitors.
 * The exact order in which the visit() method of those visitors is called is determined by the {@link IXliffWalkerStrategy},
 * for which a default implementation, flexible implementation and ordered pipeline implementations are provided.
 *
 * @author Martin Wunderlich
 * @author Vladyslav Mykhalets
 */
public class XliffWalker {

    private final IXliffWalkerStrategy strategy;

    private Map<String, VisitPlaceAwareXliffVisitor<FileNode>> fileVisitors = new LinkedHashMap<>();

    private Map<String, VisitPlaceAwareXliffVisitor<UnitNode>> unitVisitors = new LinkedHashMap<>();

    private Map<String, VisitPlaceAwareXliffVisitor<Segment>> segmentVisitors = new LinkedHashMap<>();

    private Set<String> linkedVisitorsSet = new LinkedHashSet<>();

    public XliffWalker() {
        this.strategy = new DefaultXliffWalkerStrategy();
    }

    public XliffWalker(IXliffWalkerStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("A valid strategy object must be provided.");
        }
        this.strategy = strategy;
    }

    /**
     * Main method for running the traversal. Should only be called after
     * some visitors have been added to the Walker. Otherwise, an exception
     * will be thrown.
     */
    public void doWalk(XLIFFDocument xlf) {
        if (this.linkedVisitorsSet.size() == 0) {
            throw new IllegalStateException("At least one visitor must be added before running the xliff doc traversal");
        }
        this.strategy.doWalk(xlf, this);
    }

    public Iterator<String> visitorsIterator() {
        return linkedVisitorsSet.iterator();
    }

    // Setters / Adders for visitors
    public void setVisitors(List<IXliffVisitor<FileNode>> fileVisitors, List<IXliffVisitor<UnitNode>> unitVisitors, List<IXliffVisitor<Segment>> segmentVisitors) {

        if (fileVisitors != null) {
            for (IXliffVisitor<FileNode> fileVisitor : fileVisitors) {
                this.addFileVisitor(fileVisitor);
            }
        }

        if (unitVisitors != null) {
            for (IXliffVisitor<UnitNode> unitVisitor : unitVisitors) {
                this.addUnitVisitor(unitVisitor);
            }
        }

        if (segmentVisitors != null) {
            for (IXliffVisitor<Segment> segmentVisitor : segmentVisitors) {
                this.addSegmentVisitor(segmentVisitor);
            }
        }
    }

    // Removers for visitors
    public void removeFileVisitor(String id) {
        if (id == null) {
            throw new IllegalArgumentException("A valid ID must be provided.");
        }

        this.fileVisitors.remove(id);
        this.linkedVisitorsSet.remove(id);
    }

    public void removeUnitVisitor(String id) {
        if (id == null) {
            throw new IllegalArgumentException("A valid ID must be provided.");
        }

        this.unitVisitors.remove(id);
        this.linkedVisitorsSet.remove(id);
    }

    public void removeSegmentVisitor(String id) {
        if (id == null) {
            throw new IllegalArgumentException("A valid ID must be provided.");
        }

        this.segmentVisitors.remove(id);
        this.linkedVisitorsSet.remove(id);
    }

    public int getVisitorCount() {
        return linkedVisitorsSet.size();
    }

    /**
     * Method returns {@link VisitPlaceAwareXliffVisitor} for {@link FileNode} by its id.
     *
     * @param id id of the visitor
     *
     * @return instance of {@link VisitPlaceAwareXliffVisitor} for {@link FileNode}
     */
    public VisitPlaceAwareXliffVisitor<FileNode> getFileVisitor(String id) {
        return fileVisitors.get(id);
    }

    /**
     * Method returns {@link VisitPlaceAwareXliffVisitor} for {@link UnitNode} by its id.
     *
     * @param id id of the visitor
     *
     * @return instance of {@link VisitPlaceAwareXliffVisitor} for {@link UnitNode}
     */
    public VisitPlaceAwareXliffVisitor<UnitNode> getUnitVisitor(String id) {
        return unitVisitors.get(id);
    }

    /**
     * Method returns {@link VisitPlaceAwareXliffVisitor} for {@link Segment} by its id.
     *
     * @param id id of the visitor
     *
     * @return instance of {@link VisitPlaceAwareXliffVisitor} for {@link Segment}
     */
    public VisitPlaceAwareXliffVisitor<Segment> getSegmentVisitor(String id) {
        return segmentVisitors.get(id);
    }

    /**
     * Method returns visitors which are configured to be executed on specific file id.
     *
     * @param fileId file node id
     *
     * @return list of {@link IXliffVisitor} for {@link FileNode}
     */
    public List<IXliffVisitor<FileNode>> getFileNodeVisitors (String fileId) {
        List<IXliffVisitor<FileNode>> visitors = new ArrayList<>();
        for (VisitPlaceAwareXliffVisitor<FileNode> visitor : fileVisitors.values()) {
            if (visitor.getSelectors() == null) {
                visitors.add(visitor);
            }
            else {
                for (XliffWalkerPathSelector pathSelector : visitor.getSelectors()) {
                    if (PathSelectorUtils.containsFile(pathSelector, fileId)) {
                        visitors.add(visitor);
                        break;
                    }
                }
            }
        }
        return visitors;
    }

    /**
     * Method returns visitors which are configured to be executed on specific file id and unit id.
     *
     * @param fileId file node id
     * @param unitId unit node id
     *
     * @return list of  {@link IXliffVisitor} for {@link UnitNode}
     */
    public List<IXliffVisitor<UnitNode>> getUnitNodeVisitors (String fileId, String unitId) {
        List<IXliffVisitor<UnitNode>> visitors = new ArrayList<>();
        for (VisitPlaceAwareXliffVisitor<UnitNode> visitor : unitVisitors.values()) {
            if (visitor.getSelectors() == null) {
                visitors.add(visitor);
            }
            else {
                for (XliffWalkerPathSelector pathSelector : visitor.getSelectors()) {
                    if (PathSelectorUtils.containsUnit(pathSelector, fileId, unitId)) {
                        visitors.add(visitor);
                        break;
                    }
                }
            }
        }
        return visitors;
    }

    /**
     * Method returns visitors which are configured to be executed on specific file id, unit id and segment index.
     *
     * @param fileId file node id
     * @param unitId unit node id
     * @param segmentIndex segment index
     *
     * @return list of  {@link IXliffVisitor} for {@link Segment}
     */
    public List<IXliffVisitor<Segment>> getSegmentVisitors (String fileId, String unitId, int segmentIndex) {
        List<IXliffVisitor<Segment>> visitors = new ArrayList<>();
        for (VisitPlaceAwareXliffVisitor<Segment> visitor : segmentVisitors.values()) {
            if (visitor.getSelectors() == null) {
                visitors.add(visitor);
            }
            else {
                for (XliffWalkerPathSelector pathSelector : visitor.getSelectors()) {
                    if (PathSelectorUtils.containsSegment(pathSelector, fileId, unitId, segmentIndex)) {
                        visitors.add(visitor);
                        break;
                    }
                }
            }
        }
        return visitors;
    }

    public List<IXliffVisitor<FileNode>> getAllFileNodeVisitors() {
        return new ArrayList<>(fileVisitors.values());
    }

    public List<IXliffVisitor<UnitNode>> getAllUnitNodeVisitors() {
        return new ArrayList<>(unitVisitors.values());
    }

    public List<IXliffVisitor<Segment>> getAllSegmentVisitors() {
        return new ArrayList<>(segmentVisitors.values());
    }

    public String addFileVisitor(VisitPlaceAwareXliffVisitor<FileNode> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("A valid file visitor must be provided.");
        }

        String id = UUID.randomUUID().toString();

        this.fileVisitors.put(id, visitor);
        this.linkedVisitorsSet.add(id);

        return id;
    }

    public String addFileVisitor(IXliffVisitor<FileNode> visitor, XliffWalkerPathSelector... pathSelectors) {
        if (visitor == null) {
            throw new IllegalArgumentException("A valid file visitor must be provided.");
        }

        return this.addFileVisitor(new VisitPlaceAwareXliffVisitor<>(visitor, pathSelectors));
    }

    public String addUnitVisitor(VisitPlaceAwareXliffVisitor<UnitNode> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("A valid unit visitor must be provided.");
        }

        String id = UUID.randomUUID().toString();

        this.unitVisitors.put(id, visitor);
        this.linkedVisitorsSet.add(id);

        return id;
    }

    public String addUnitVisitor(IXliffVisitor<UnitNode> visitor, XliffWalkerPathSelector... pathSelectors) {
        if (visitor == null) {
            throw new IllegalArgumentException("A valid unit visitor must be provided.");
        }

        return this.addUnitVisitor(new VisitPlaceAwareXliffVisitor<>(visitor, pathSelectors));
    }

    public String addSegmentVisitor(VisitPlaceAwareXliffVisitor<Segment> visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("A valid segment visitor must be provided.");
        }

        String id = UUID.randomUUID().toString();

        this.segmentVisitors.put(id, visitor);
        this.linkedVisitorsSet.add(id);

        return id;
    }

    public String addSegmentVisitor(IXliffVisitor<Segment> visitor, XliffWalkerPathSelector... pathSelectors) {
        if (visitor == null) {
            throw new IllegalArgumentException("A valid segment visitor must be provided.");
        }

        return this.addSegmentVisitor(new VisitPlaceAwareXliffVisitor<>(visitor, pathSelectors));
    }

    public void removeVisitors() {
        this.fileVisitors.clear();
        this.unitVisitors.clear();
        this.segmentVisitors.clear();
        this.linkedVisitorsSet.clear();
    }

    /**
     * Xliff visitor {@link IXliffVisitor} decorator class aware of visit place defined by 
     * path selector {@link XliffWalkerPathSelector}
     *
     * @author Vladyslav Mykhalets
     */
    public static class VisitPlaceAwareXliffVisitor<T> implements IXliffVisitor<T> {

        private final List<XliffWalkerPathSelector> selectors;

        private final IXliffVisitor<T> delegate;

        public VisitPlaceAwareXliffVisitor(IXliffVisitor<T> delegate, XliffWalkerPathSelector... selectors) {
            this.delegate = delegate;

            if (selectors != null && selectors.length > 0) {
                this.selectors = Arrays.asList(selectors);
            } else {
                this.selectors = null;
            }
        }

        public List<XliffWalkerPathSelector> getSelectors() {
            return this.selectors;
        }

        @Override
        public void visit(T visitee, VisitationContext context) {
            delegate.visit(visitee, context);
        }
    }

}
