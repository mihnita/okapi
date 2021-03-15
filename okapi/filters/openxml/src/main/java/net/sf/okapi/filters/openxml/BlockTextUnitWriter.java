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

import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_BREAK;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_TAB;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.createQName;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.events.Attribute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;

class BlockTextUnitWriter implements TextUnitWriter {
	private final Logger LOGGER = LoggerFactory.getLogger(BlockTextUnitWriter.class);

	private final XMLEventFactory eventFactory;
	private final QName runName;
	private final QName textName;
	private final RunProperties baseRunProperties;
	private final List<XMLEvents> hiddenCodes;
	private final Map<Integer, XMLEvents> visibleCodes;
	private final XMLEventSerializer xmlWriter;
	private final RunPropertiesClarification runPropertiesClarification;
	private final ConditionalParameters cparams;

	private Deque<RunProperties> currentRunProperties = new ArrayDeque<>();
	private StringBuilder textContent = new StringBuilder();

	BlockTextUnitWriter(
		final ConditionalParameters cparams,
		final XMLEventFactory eventFactory,
		final BlockSkeleton blockSkeleton,
		final XMLEventSerializer xmlWriter,
		final RunPropertiesClarification runPropertiesClarification
	) {
		this.cparams = cparams;
		this.eventFactory = eventFactory;
		this.runName = blockSkeleton.block().getRunName();
		this.textName = blockSkeleton.block().getTextName();
		this.baseRunProperties = blockSkeleton.baseRunProperties();
		this.hiddenCodes = blockSkeleton.hiddenCodes();
		this.visibleCodes = blockSkeleton.visibleCodes();
		this.xmlWriter = xmlWriter;
		this.runPropertiesClarification = runPropertiesClarification;
	}

	public void write(TextContainer tc) {
		boolean firstSegmentWritten = false;

		for (Segment segment : tc.getSegments()) {
			if (!firstSegmentWritten) {
				writeFirstSegment(tc.getFirstSegment());
				firstSegmentWritten = true;
				continue;
			}
			writeSegment(segment);
		}
		flushText(true);
	}

	private void writeFirstSegment(final Segment segment) {
		for (final XMLEvents events : this.hiddenCodes) {
			this.xmlWriter.add(events);
		}
		writeSegment(segment);
	}

	private void writeSegment(Segment segment) {
		try {
			TextFragment content = segment.getContent();
			String codedText = content.getCodedText();
			List<Code> codes = content.getCodes();
			for (int i = 0; i < codedText.length(); i++) {
				char c = codedText.charAt(i);
				if (TextFragment.isMarker(c)) {
					int codeIndex = TextFragment.toIndex(codedText.charAt(++i));
					writeCode(codes.get(codeIndex));
				}
				else {
					writeChar(c);
				}
			}
		}
		catch (Exception e) {
			LOGGER.error("Threw {} writing segment id {} '{}'", e.getClass().getSimpleName(),
						 segment.getId(), segment.toString());
			throw e;
		}
	}

	private void writeChar(char c) {
		if (!runIsOpen && currentRunProperties.isEmpty()) {
			writeRunStart(baseRunProperties);
			runIsOpen = true;
		}
		textContent.append(c);
	}

	private void writeCode(Code code) {
		// is the Code really an escaped Okapi Marker?
        if (code.isMarkerMasking()) {
			String data = code.getData();
			for (int i = 0; i < data.length(); i++) {
				char c = data.charAt(i);
				writeChar(c);
			}
			return;
		}

		// Cases:
		// - Open
		//   - Terminate current run
		//   - Do something content-dependent:   
		//	    - If it's RunProperties, update run properties
		//	    - If it's a RunContainer, write opening tag
		// - Closed
		//   - Terminate current run
		//   - Handling this is actually optional in many cases
		//	 - If it's a RunContainer, write the closing tag
		// - Isolated
		//   - Terminate current run
		//   - Write out the corresponding markup (for Run, Run.RunMarkup, Block.BlockMarkup
		int id = code.getId();
		XMLEvents codeEvents = visibleCodes.get(id);
		switch (code.getTagType()) {
			case OPENING:
				flushText(true);
				if (codeEvents instanceof RunProperties) {
					currentRunProperties.push((RunProperties)codeEvents);
				}
				else if (codeEvents instanceof RunContainer) {
					RunContainer rc = (RunContainer)codeEvents;
					xmlWriter.addAll(rc.startMarkupEvents());
					currentRunProperties.push(rc.getDefaultRunProperties());
				}
				else {
					throw new IllegalStateException("Unexpected code contents for opening code '" +
													code.toString() + "':" + codeEvents );
				}
				break;
			case PLACEHOLDER:
				// If this is RunMarkup (markup contained within a run), we should
				// keep the current run open.  Otherwise, close it.
				boolean isRunMarkup = (codeEvents instanceof Run.Markup);
				if (isRunMarkup) {
					flushRunStart();
				}
				flushText(!isRunMarkup);
				xmlWriter.add(codeEvents);
				break;
			case CLOSING:
				flushText(true);
				if (codeEvents instanceof RunProperties) {
					// XXX What if it's not on the top of the stack?  It's probably a corrupt target.
					currentRunProperties.pop();
				}
				else if (codeEvents instanceof RunContainer) {
					RunContainer rc = (RunContainer) codeEvents;
					xmlWriter.addAll(rc.endMarkupEvents());
					currentRunProperties.pop(); // Pop RunContainer properties
				}
				else {
					throw new IllegalStateException("Unexpected code contents for closing code '" +
													code.toString() + "':" + codeEvents );
				}

				break;
		}
	}

	private boolean runIsOpen = false;

	private void flushRunStart() {
		if (!runIsOpen) {
			writeRunStart(currentRunProperties.isEmpty() ? baseRunProperties : currentRunProperties.peek());
			runIsOpen = true;
		}
	}

	private void flushText(boolean terminateRun) {
		if (textContent.length() > 0) {
			flushRunStart();
			String text = textContent.toString();
			writeRunText(text);
			textContent = new StringBuilder();
		}
		if (terminateRun && runIsOpen) {
			writeRunEnd();
			runIsOpen = false;
		}
	}

	private void writeRunStart(final RunProperties properties) {
		if (runName == null) {
			throw new IllegalStateException("no run name set");
		}

		xmlWriter.add(eventFactory.createStartElement(runName, null, null));
		runPropertiesClarification.performFor(properties);
		xmlWriter.add(properties);
	}

	// Would be better to have a separate hierarcy for the MS Word BlockTextUnitWrite.java and for the Excel
	// BlockTextUnitWrite.java but...
	private void writeRunText(String text) {
		if (textName == null) {
			throw new IllegalStateException("no text name set");
		}

		// MS Excel doesn't support the line breaks inside text run
		// We should save a content as is
		// Current implementation of ms excel text run has <t> without prefix
		// We are using this fact to catch an excel text runs
		if (textName.getPrefix().isEmpty()) {
			writeText(text);
			return;
		}

		// MS Word text runs can contain the line breaks
		// The text run of ms word looks like <w:t>
		// The prefix "w" says us that is ms word text tun
		StringBuilder sb = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (c == cparams.getLineSeparatorReplacement() && cparams.getAddLineSeparatorCharacter()) {
				writeTextIfNeeded(sb);
				sb.setLength(0);
				writeLineBreak();
			} else if (c == '\t' && cparams.getAddTabAsCharacter() &&
						Namespaces.WordProcessingML.containsName(textName)) {
				writeTextIfNeeded(sb);
				sb.setLength(0);
				writeTab();
			} else {
				sb.append(c);
			}
		}
		writeTextIfNeeded(sb);
	}

	private void writeTextIfNeeded(StringBuilder buffer) {
		if (buffer.length() > 0) {
			writeText(buffer.toString());
		}
	}

	private void writeTab() {
		QName br = createQName(LOCAL_TAB, textName);
		xmlWriter.add(eventFactory.createStartElement(br, null, null));
		xmlWriter.add(eventFactory.createEndElement(br, null));
	}

	private void writeLineBreak() {
		// Word seems to always start a new run before the break element. Although this is not enforced by specification
		// we behave like Word. This prevents some strange behaviour and broken documents.
		writeRunEnd();
		writeRunStart(currentRunProperties.isEmpty() ? baseRunProperties : currentRunProperties.peek());

		QName br = createQName(LOCAL_BREAK, textName);
		xmlWriter.add(eventFactory.createStartElement(br, null, null));
		xmlWriter.add(eventFactory.createEndElement(br, null));
	}

	private void writeText(String text) {
		boolean needsPreserveSpace = needsXmlSpacePreserve(text);
		ArrayList<Attribute> attrs = new ArrayList<>();
		// DrawingML <a:t> does not use the xml:space="preserve" attribute
		if (needsPreserveSpace && !Namespaces.DrawingML.containsName(textName)) {
			attrs.add(eventFactory.createAttribute("xml", Namespaces.XML.getURI(), "space", "preserve"));
		}
		xmlWriter.add(eventFactory.createStartElement(textName, attrs.iterator(), null));
		xmlWriter.add(eventFactory.createCharacters(text));
		xmlWriter.add(eventFactory.createEndElement(textName, null));
	}

	private void writeRunEnd() {
		xmlWriter.add(eventFactory.createEndElement(runName, null));
	}

	/**
	 * Returns true if the given text contains a space, tab or no-break space. In that case you
	 * have to add {@code xml:space="preserve"} to the {@code &lt;w:t&gt;} element.
	 *
	 * @param text text
	 * @return true if the given text contains a space, tab or no-break space
	 */
	static boolean needsXmlSpacePreserve(String text) {
		for (char c : text.toCharArray()) {
			// This catches things like ideographic space (U+3000).  NBSP
			// isn't flagged as whitespace in unicode, so we have to special-case it.
			if (Character.isWhitespace(c) || c == '\u00A0') return true;
		}
		return false;
	}
}