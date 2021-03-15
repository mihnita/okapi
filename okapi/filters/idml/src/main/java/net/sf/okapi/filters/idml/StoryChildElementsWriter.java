/*
 * =============================================================================
 *   Copyright (C) 2010-2017 by the Okapi Framework contributors
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

import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

class StoryChildElementsWriter {

    private final StyleRangeEventsGenerator styleRangeEventsGenerator;

    private StyleRanges currentStyleRanges;

    StoryChildElementsWriter(StyleRangeEventsGenerator styleRangeEventsGenerator) {
        this.styleRangeEventsGenerator = styleRangeEventsGenerator;
    }

    List<XMLEvent> write(List<StoryChildElement> storyChildElements) {
        List<XMLEvent> events = new ArrayList<>();

        for (StoryChildElement storyChildElement : storyChildElements) {

            if (!(storyChildElement instanceof StoryChildElement.StyledTextElement)) {
                if (null != currentStyleRanges) {
                    events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeEnd());
                    events.addAll(styleRangeEventsGenerator.generateParagraphStyleRangeEnd());
                }

                events.addAll(storyChildElement.getEvents());

                if (null != currentStyleRanges) {
                    events.addAll(styleRangeEventsGenerator.generateParagraphStyleRangeStart(currentStyleRanges));
                    events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeStart(currentStyleRanges));
                }
                continue;
            }

            if (null == currentStyleRanges) {
                currentStyleRanges = ((StoryChildElement.StyledTextElement) storyChildElement).getStyleRanges();

                events.addAll(styleRangeEventsGenerator.generateParagraphStyleRangeStart(currentStyleRanges));
                events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeStart(currentStyleRanges));
            }

            StyleRanges styleRanges = ((StoryChildElement.StyledTextElement) storyChildElement).getStyleRanges();

            if (!currentStyleRanges.getParagraphStyleRange().equals(styleRanges.getParagraphStyleRange())) {
                events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeEnd());
                events.addAll(styleRangeEventsGenerator.generateParagraphStyleRangeEnd());

                currentStyleRanges = styleRanges;

                events.addAll(styleRangeEventsGenerator.generateParagraphStyleRangeStart(currentStyleRanges));
                events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeStart(currentStyleRanges));
                events.addAll(storyChildElement.getEvents());

                continue;
            }

            if (!currentStyleRanges.getCharacterStyleRange().equals(styleRanges.getCharacterStyleRange())) {
                events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeEnd());

                currentStyleRanges = styleRanges;

                events.addAll(styleRangeEventsGenerator.generateCharacterStyleRangeStart(currentStyleRanges));
                events.addAll(storyChildElement.getEvents());

                continue;
            }

            events.addAll(storyChildElement.getEvents());
        }

        return events;
    }
}
