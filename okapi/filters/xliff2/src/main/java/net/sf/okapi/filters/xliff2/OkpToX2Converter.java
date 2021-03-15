/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.filters.xliff2;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.exceptions.OkapiMergeException;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.filters.xliff2.model.XLIFF2PropertyStrings;
import net.sf.okapi.filters.xliff2.util.NotesMapper;
import net.sf.okapi.filters.xliff2.util.PropertiesMapper;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.EventType;

/**
 * This class is designed to convert the Okapi Core structure back into Xliff Toolkit structure.
 */
public class OkpToX2Converter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    PropertiesMapper propertiesMapper;



    private static final Map<TextFragment.TagType, TagType> codeTagTypeMap = new HashMap<>();

	private static final String ID = "id";
	
    static {
        codeTagTypeMap.put(TextFragment.TagType.OPENING, TagType.OPENING);
        codeTagTypeMap.put(TextFragment.TagType.CLOSING, TagType.CLOSING);
        codeTagTypeMap.put(TextFragment.TagType.PLACEHOLDER, TagType.STANDALONE);
    }


    public OkpToX2Converter() {
        propertiesMapper = new PropertiesMapper();
    }


    /**
     * Takes an Okapi Core event and produces a list of XLIFF Toolkit {@link Event}s. A list of events is produced
     * because Okapi Core isn't a 1 to 1 map of the XLIFF Toolkit.
     *
     * @param okapiEvent         The Okapi Core event
     * @param xliff2FilterWriter The filter writer being used to write the XLIFF 2.0 file
     * @return The XLIFF Toolkit events that can be saved out to a file.
     */
    public List<Event> handleEvent(net.sf.okapi.common.Event okapiEvent, XLIFF2FilterWriter xliff2FilterWriter) {
        final net.sf.okapi.common.EventType eventType = okapiEvent.getEventType();

        switch (eventType) {

            case START_DOCUMENT:
                return startDocument(okapiEvent.getStartDocument(), xliff2FilterWriter);
            case END_DOCUMENT:
                return endDocument(okapiEvent.getEnding());
            case START_SUBDOCUMENT:
                return startSubDocument(okapiEvent.getStartSubDocument());
            case END_SUBDOCUMENT:
                return endSubDocument();
            case START_GROUP:
                return startGroup(okapiEvent.getStartGroup());
            case END_GROUP:
                return endGroup(okapiEvent.getEndGroup());
            case TEXT_UNIT:
                return textUnit(okapiEvent.getTextUnit(), xliff2FilterWriter.getTargetLocale());
            case DOCUMENT_PART:
                return documentPart(okapiEvent.getDocumentPart());
            case CUSTOM:
                return Collections.emptyList();
            default:
                throw new OkapiException(
                    "Event " + okapiEvent.getEventType() + " is not implemented in XLIFF 2.0 Filter Writer");
        }

    }

	private List<Event> documentPart(DocumentPart documentPart) {
		// check for metadata and skeleton cases
		if (documentPart.getSkeleton() != null) {
			if (documentPart.getSkeleton() instanceof MetadataSkeleton) {
				final MidFileData xliffMidFileData = new MidFileData();
				xliffMidFileData.setMetadata(((MetadataSkeleton) documentPart.getSkeleton()).getMetaData());
				final Event event = new Event(EventType.MID_FILE, null, xliffMidFileData);
				NotesMapper.setNotes(documentPart, xliffMidFileData);
				return Collections.singletonList(event);
			} else if (documentPart.getSkeleton() instanceof Xliff2Skeleton) {
				final Event event = new Event(EventType.SKELETON, null, ((Xliff2Skeleton) documentPart.getSkeleton()).getXliff2Skeleton());
				return Collections.singletonList(event);
			}
		}
		
		final MidFileData xliffMidFileData = new MidFileData();
		final Event event = new Event(EventType.MID_FILE, null, xliffMidFileData);
		NotesMapper.setNotes(documentPart, xliffMidFileData);
		return Collections.singletonList(event);
	}

    private List<Event> textUnit(ITextUnit okapiTextUnit, LocaleId targetLocale) {
		final Set<String> sourceCodeIds = new HashSet<>();
		final Set<String> targetCodeIds = new HashSet<>();
        final Unit xliffTextUnit = new Unit(okapiTextUnit.getId());
        final Event event = new Event(EventType.TEXT_UNIT, null, xliffTextUnit);
        final Set<LocaleId> availableTargetLocales = okapiTextUnit.getTargetLocales();

        if (okapiTextUnit.getSkeleton() != null) {
        	xliffTextUnit.setMetadata(((MetadataSkeleton) okapiTextUnit.getSkeleton()).getMetaData());
        }

        PropertiesMapper.setTextUnitProperties(okapiTextUnit, xliffTextUnit);

        NotesMapper.setNotes(okapiTextUnit, xliffTextUnit);

        // Iterate over both the target the source parts together.
        final TextContainer okapiSources = okapiTextUnit.getSource();
        final TextContainer okapiTargets;
        if (targetLocale != null && availableTargetLocales.contains(targetLocale)) {
            okapiTargets = okapiTextUnit.getTarget(targetLocale);
            if(okapiTargets.count() != 0 && okapiSources.count() != okapiTargets.count()){
                logger.warn("Target count doesn't match source count. It's very likely there will be some misalignment.");
            }
        } else {
            okapiTargets = null;
        }

        for (int partIndex = 0; partIndex < okapiSources.count(); partIndex++) {
            // Add the source segments and ignorables
            TextPart okapiSourcePart = okapiSources.get(partIndex);
            final Part xliffPart;
            if (okapiSourcePart.isSegment()) {
                xliffPart = xliffTextUnit.appendSegment();
                if (xliffPart.getSource() == null) {
                    xliffPart.setSource("");
                }
            } else {
                xliffPart = xliffTextUnit.appendIgnorable();
            }
            xliffPart.setId(okapiSourcePart.originalId);
			copyOver(okapiSourcePart.getContent(), xliffPart.getSource(), sourceCodeIds);


            // Add the targets and target ignorables
            // If the target count doesn't match the source count, it's very likely to be some misalignment, since we
            // assume the index of the targets matches the sources.
            TextPart okapiTargetPart = null;
            if (okapiTargets != null && partIndex < okapiTargets.count()) {
                okapiTargetPart = okapiTargets.get(partIndex);

                if (okapiTargetPart != null) {

                    final Part xliffTargetPart = xliffTextUnit.getPart(partIndex);
                    if (xliffTargetPart.getTarget() == null) {
                        xliffTargetPart.setTarget("");
                    }
                    xliffPart.setId(okapiTargetPart.originalId);
					copyOver(okapiTargetPart.getContent(), xliffTargetPart.getTarget(), targetCodeIds);

                }
            }
            PropertiesMapper.setPartProperties(okapiSourcePart, xliffPart, okapiTextUnit);
        }
        return Collections.singletonList(event);
    }


    private List<Event> endGroup(Ending endGroup) {

        final Event event = new Event(EventType.END_GROUP, null);

        return Collections.singletonList(event);
    }

    private List<Event> startGroup(StartGroup startGroup) {

        final Property propertyId = startGroup.getProperty(ID);
        final String propertyIdString = (propertyId == null) ? null : propertyId.getValue();

        final StartGroupData startGroupData = new StartGroupData(propertyIdString);
        final Event event = new Event(EventType.START_GROUP, null, startGroupData);

        if (startGroup.getSkeleton() != null) {
        	startGroupData.setMetadata(((MetadataSkeleton) startGroup.getSkeleton()).getMetaData());
        }
        
        PropertiesMapper.setGroupProperties(startGroup, startGroupData);
        NotesMapper.setNotes(startGroup, startGroupData);

        return Collections.singletonList(event);
    }

    private List<Event> startDocument(StartDocument okapiEvent, XLIFF2FilterWriter xliff2FilterWriter) {
        final Event startDocumentEvent = new Event(EventType.START_DOCUMENT, null);
        final LocaleId locale = okapiEvent.getLocale();

        final Property xliffVersionProperty = okapiEvent.getProperty(XLIFF2PropertyStrings.VERSION);
        final String xliffVersion = xliffVersionProperty != null ? xliffVersionProperty.getValue() : "2.0";

        final StartXliffData startXliffData = new StartXliffData(xliffVersion);
        final Event startXliffEvent = new Event(EventType.START_XLIFF, null, startXliffData);

        xliff2FilterWriter.initializeWriter(locale);

        PropertiesMapper.setStartXliffProperties(okapiEvent, startXliffData);

        return Arrays.asList(startDocumentEvent, startXliffEvent);
    }

    private List<Event> endDocument(Ending okapiEvent) {

        final Event endXliffEvent = new Event(EventType.END_XLIFF, null);

        final Event endDocumentEvent = new Event(EventType.END_DOCUMENT, null);

        return Arrays.asList(endXliffEvent, endDocumentEvent);
    }

    private List<Event> startSubDocument(StartSubDocument okapiEvent) {

        final List<Event> events = new ArrayList<>();
        final StartFileData startFileData = new StartFileData(okapiEvent.getId());
        startFileData.setOriginal(okapiEvent.getName());
        PropertiesMapper.setStartFileProperties(okapiEvent, startFileData);
        NotesMapper.setNotes(okapiEvent, startFileData);

        final Event startFileEvent = new Event(EventType.START_FILE, null, startFileData);

        events.add(startFileEvent);

        return events;
    }

    private List<Event> endSubDocument() {

        final Event event = new Event(EventType.END_FILE, null);
        return Collections.singletonList(event);
    }


    /**
	 * Copies the text and codes from Okapi Core {@link TextFragment} to an XLIFF
	 * Toolkit {@link Fragment}.
	 * 
	 * @param source        The text to read.
	 * @param out           The destination of the read text.
	 * @param targetCodeIds
	 */
	private void copyOver(TextFragment source, Fragment out, Set<String> existingCodeIds) {
        final String codedText = source.getCodedText();

        int nextCodeIndex = -1;
        int nextCodePosition = -1;
        if (source.hasCode()) {
            nextCodeIndex = 0;
            nextCodePosition = source.getCodePosition(nextCodeIndex);
        }

        for (int i = 0; i < codedText.length(); i++) {
            final char c = codedText.charAt(i);

            if (nextCodePosition == i) {
                final char codePosition = codedText.charAt(i + 1);
                final Code okapiCode = source.getCode(codePosition);
                final TagType tagType = codeTagTypeMap.get(okapiCode.getTagType());

				String id = okapiCode.getOriginalId();
				if (id == null) {
					// use okapi code id with a warning
					id = String.valueOf(okapiCode.getId());
					logger.warn("Code id was null in segment: {}. " + "Using Okapi code internal id instead: {}",
							source, id);
				}
				final CTag xliff2Ctag = new CTag(tagType, id, okapiCode.getData());
				// Checks if Placeholder use the same ID as other placeholder in the text unit
				// THE XLIFF Toolkit writer should have done this check, but it doesn't. So we
				// have to prevent it from writing out invalid XLIFF 2.0
				if (tagType.equals(TagType.CLOSING) || tagType.equals(TagType.STANDALONE)) {
					if (existingCodeIds.contains(id)) {
						throw new OkapiMergeException("Tried writing placeholder to XLIFF 2 with the same ID as "
								+ "another placeholder in the same text unit. Previous ID: " + okapiCode.getId()
								+ " | XLIFF 2 ID: " + id + " | Placeholder: " + okapiCode);
					}
					existingCodeIds.add(id);
				}

                PropertiesMapper.setCodeProperties(okapiCode, xliff2Ctag);

                out.append(xliff2Ctag);

                nextCodeIndex += 1;
                nextCodePosition = source.getCodePosition(nextCodeIndex);
                i++;
            } else {
                out.append(c);
            }
        }

    }


}
