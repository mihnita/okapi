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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static net.sf.okapi.filters.openxml.StartElementContextFactory.createStartElementContext;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isEndElement;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.isWhitespace;

class RunPropertiesParser implements Parser<RunProperties>{
	private final StartElementContext startElementContext;
	private final RunSkippableElements runSkippableElements;
	private final List<Property> runProperties;
	private EndElement runPropsEndElement;

	RunPropertiesParser(final StartElementContext startElementContext, final RunSkippableElements runSkippableElements) {
		this.startElementContext = createStartElementContext(
			new StrippableAttributes.DrawingRunProperties(
				startElementContext.getConditionalParameters(),
				startElementContext.getEventFactory()
			).strip(
				new StrippableAttributes.WordRunRevisions(
					startElementContext.getEventFactory()
				).strip(startElementContext.getStartElement())
			),
			startElementContext
		);
		this.runSkippableElements = runSkippableElements;
		this.runProperties = new ArrayList<>();
	}

	public RunProperties parse() throws XMLStreamException {
		// DrawingML properties contain inline property attributes that we must parse out
		if (Namespace.PREFIX_A.equals(startElementContext.getStartElement().getName().getPrefix())) {

			@SuppressWarnings("rawtypes") Iterator attrs = startElementContext.getStartElement().getAttributes();
			while (attrs.hasNext()) {
				// XXX Don't support hidden styles in DrawingML yet
				runProperties.add(RunPropertyFactory.createRunProperty((Attribute)attrs.next()));
			}
		}

		while (startElementContext.getEventReader().hasNext()) {
			XMLEvent e = startElementContext.getEventReader().nextEvent();
			if (isEndElement(e, startElementContext.getStartElement())) {
				endRunProperties(e.asEndElement());
				return buildRunProperties();
			}
			if (e.isStartElement()) {
				if (!runSkippableElements.skipProperties(e, startElementContext)) {
					// This gathers the whole event.
					addRunProperty(e.asStartElement());
				}
			}
			// Discard -- make sure we're not discarding meaningful data
			else if (e.isCharacters() && !isWhitespace(e)) {
				throw new IllegalStateException(
						"Discarding non-whitespace rPr characters " + e.asCharacters().getData());
			}
		}
		throw new IllegalStateException("Invalid content? Unterminated run properties");
	}

	private void addRunProperty(StartElement startElement) throws XMLStreamException {
		// Gather elements up to the end
		StartElementContext runPropertiesElementContext = createStartElementContext(startElement, startElementContext);
		runProperties.add(RunPropertyFactory.createRunProperty(runPropertiesElementContext));
	}

	private void endRunProperties(EndElement e) {
		runPropsEndElement = e;
	}

	private RunProperties buildRunProperties() {
		return new RunProperties.Default(
			startElementContext.getEventFactory(),
			startElementContext.getStartElement(),
			runPropsEndElement,
			runProperties
		);
	}
}
