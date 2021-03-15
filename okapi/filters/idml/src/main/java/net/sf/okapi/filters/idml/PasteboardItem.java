/*
 * =============================================================================
 *   Copyright (C) 2010-2013 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */

package net.sf.okapi.filters.idml;

import javax.xml.stream.XMLEventFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

class PasteboardItem {
    private static final double DEFAULT_COORDINATE = 5.0;

    private final SpreadItem.TextualSpreadItem textualSpreadItem;
    private final List<Point> anchorPoints;

    PasteboardItem(SpreadItem.TextualSpreadItem textualSpreadItem, List<Point> anchorPoints) {
        this.textualSpreadItem = textualSpreadItem;
        this.anchorPoints = anchorPoints;
    }

    static PasteboardItem fromTextualSpreadItemAndParentTransformations(
        final SpreadItem.TextualSpreadItem spreadItem,
        final Deque<OrderingIdioms.TransformationMatrix> transformationMatrices,
        final XMLEventFactory eventFactory
    ) {
        PasteboardItemBuilder pasteboardItemBuilder = new PasteboardItemBuilder();
        pasteboardItemBuilder.setSpreadItem(spreadItem);

        Property.PathGeometryProperty property = (Property.PathGeometryProperty) spreadItem.getProperties().stream()
            .filter(p -> p instanceof Property.PathGeometryProperty)
            .findFirst()
            .orElseGet(() -> new Property.PathGeometryProperty(
                null,
                Collections.singletonList(
                    new GeometryPath(
                        null,
                        null,
                        Arrays.asList(
                            new PathPoint.Default(
                                new Point.Default(-DEFAULT_COORDINATE, -DEFAULT_COORDINATE),
                                new Point.Default(-DEFAULT_COORDINATE, -DEFAULT_COORDINATE),
                                new Point.Default(-DEFAULT_COORDINATE, -DEFAULT_COORDINATE),
                                eventFactory
                            ),
                            new PathPoint.Default(
                                new Point.Default(-DEFAULT_COORDINATE, DEFAULT_COORDINATE),
                                new Point.Default(-DEFAULT_COORDINATE, DEFAULT_COORDINATE),
                                new Point.Default(-DEFAULT_COORDINATE, DEFAULT_COORDINATE),
                                eventFactory
                            ),
                            new PathPoint.Default(
                                new Point.Default(DEFAULT_COORDINATE, DEFAULT_COORDINATE),
                                new Point.Default(DEFAULT_COORDINATE, DEFAULT_COORDINATE),
                                new Point.Default(DEFAULT_COORDINATE, DEFAULT_COORDINATE),
                                eventFactory
                            ),
                            new PathPoint.Default(
                                new Point.Default(DEFAULT_COORDINATE, -DEFAULT_COORDINATE),
                                new Point.Default(DEFAULT_COORDINATE, -DEFAULT_COORDINATE),
                                new Point.Default(DEFAULT_COORDINATE, -DEFAULT_COORDINATE),
                                eventFactory
                            )
                        ),
                        null,
                        null
                    )
                ),
                null,
                null
            ));
        property.getGeometryPaths().stream()
            .map(gp -> gp.pathPoints())
            .flatMap(Collection::stream)
            .forEach(pp -> pasteboardItemBuilder.addAnchorPoint(pp.anchor().transformedWith(transformationMatrices)));
        return pasteboardItemBuilder.build();
    }

    SpreadItem.TextualSpreadItem getTextualSpreadItem() {
        return textualSpreadItem;
    }

    List<Point> getAnchorPoints() {
        return anchorPoints;
    }

    Point getMinAnchorPointByDirection(OrderingIdioms.Direction direction) {
        Comparator<Point> anchorPointComparator = new Point.Comparator(direction);

        return Collections.min(anchorPoints, anchorPointComparator);
    }

    static class PasteboardItemBuilder implements Builder<PasteboardItem> {

        private SpreadItem.TextualSpreadItem spreadItem;
        private List<Point> anchorPoints = new ArrayList<>();

        PasteboardItemBuilder setSpreadItem(SpreadItem.TextualSpreadItem spreadItem) {
            this.spreadItem = spreadItem;
            return this;
        }

        PasteboardItemBuilder addAnchorPoint(Point anchorPoint) {
            anchorPoints.add(anchorPoint);
            return this;
        }

        @Override
        public PasteboardItem build() {
            return new PasteboardItem(spreadItem, anchorPoints);
        }
    }

    static class PasteboardItemComparator implements Comparator<PasteboardItem> {

        private final OrderingIdioms.Direction direction;

        PasteboardItemComparator(OrderingIdioms.Direction direction) {
            this.direction = direction;
        }

        @Override
        public int compare(PasteboardItem pasteboardItem, PasteboardItem anotherPasteboardItem) {
            int result;

            if (pasteboardItem.getAnchorPoints().isEmpty() && anotherPasteboardItem.getAnchorPoints().isEmpty()) {
                return 0;
            }

            if (pasteboardItem.getAnchorPoints().isEmpty()) {
                return -1;
            }

            if (anotherPasteboardItem.getAnchorPoints().isEmpty()) {
                return 1;
            }

            result = Double.compare(pasteboardItem.getMinAnchorPointByDirection(direction).y(),
                    anotherPasteboardItem.getMinAnchorPointByDirection(direction).y());

            if (0 != result) {
                return result;
            }

            if (OrderingIdioms.Direction.RIGHT_TO_LEFT == direction) {
                return Double.compare(anotherPasteboardItem.getMinAnchorPointByDirection(direction).x(),
                        pasteboardItem.getMinAnchorPointByDirection(direction).x());
            }

            return Double.compare(pasteboardItem.getMinAnchorPointByDirection(direction).x(),
                    anotherPasteboardItem.getMinAnchorPointByDirection(direction).x());
        }
    }

    static class VisibilityFilter {

        private static final String LAYER_DOES_NOT_EXIST = "Layer does not exist";

        private final List<Layer> layers;
        private final boolean extractHiddenLayers;
        private final boolean extractHiddenPasteboardItems;

        VisibilityFilter(
            final List<Layer> layers,
            final boolean extractHiddenLayers,
            final boolean extractHiddenPasteboardItems
        ) {
            this.layers = layers;
            this.extractHiddenLayers = extractHiddenLayers;
            this.extractHiddenPasteboardItems = extractHiddenPasteboardItems;
        }

        List<PasteboardItem> filterVisible(List<PasteboardItem> pasteboardItems) {

            List<PasteboardItem> visiblePasteboardItems = new ArrayList<>(pasteboardItems.size());

            for (PasteboardItem pasteboardItem : pasteboardItems) {
                Layer layer = getLayerById(pasteboardItem.getTextualSpreadItem().getLayerId(), layers);

                if (!extractHiddenLayers && !layer.isVisible()) {
                    continue;
                }

                if (!extractHiddenPasteboardItems && !pasteboardItem.getTextualSpreadItem().isVisible()) {
                    continue;
                }

                visiblePasteboardItems.add(pasteboardItem);
            }

            return visiblePasteboardItems;
        }

        private Layer getLayerById(String layerId, List<Layer> layers) {

            for (Layer layer : layers) {
                if (layerId.equals(layer.getId())) {
                    return layer;
                }
            }

            throw new IllegalStateException(LAYER_DOES_NOT_EXIST);
        }
    }
}
