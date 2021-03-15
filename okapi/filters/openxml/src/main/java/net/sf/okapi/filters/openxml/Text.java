/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

class Text implements Chunk {
    protected final StartElement startElement;
    protected final Characters characters;
    protected final EndElement endElement;

    Text(StartElement startElement, Characters characters, EndElement endElement) {
        this.startElement = startElement;
        this.characters = characters;
        this.endElement = endElement;
    }

    @Override
    public List<XMLEvent> getEvents() {
        List<XMLEvent> events = new ArrayList<>(3);
        events.add(startElement);
        events.add(characters);
        events.add(endElement);
        return events;
    }

    StartElement startElement() {
        return this.startElement;
    }

    Characters characters() {
        return characters;
    }

    EndElement endElement() {
        return this.endElement;
    }

    boolean isEmpty() {
        return characters.getData().isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + XMLEventSerializer.serialize(startElement) + "](" + characters + ")";
    }
}
