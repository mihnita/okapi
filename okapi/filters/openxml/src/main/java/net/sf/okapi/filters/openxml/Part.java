package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.Event;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

interface Part {
    /**
     * Opens this part and performs any initial processing.
     *
     * @return First event for this part
     * @throws IOException if any problem is encountered
     */
    Event open() throws IOException, XMLStreamException;

    boolean hasNextEvent();

    Event nextEvent();

    void close();

    void logEvent(Event e);
}
