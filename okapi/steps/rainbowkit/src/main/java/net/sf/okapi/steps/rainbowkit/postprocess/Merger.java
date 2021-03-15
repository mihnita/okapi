/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

package net.sf.okapi.steps.rainbowkit.postprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.MultiEvent;
import net.sf.okapi.common.resource.PipelineParameters;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.rainbowkit.Manifest;
import net.sf.okapi.filters.rainbowkit.MergingInfo;
import net.sf.okapi.lib.merge.merge.ITextUnitMerger;
import net.sf.okapi.lib.merge.merge.TextUnitMerger;

public class Merger {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final Parameters parameters;
	private final IFilterConfigurationMapper fcMapper;
	private final Manifest manifest;
	private final LocaleId trgLoc;

	private IFilter filter;
	private IFilterWriter writer;
	private boolean skipEmptySourceEntries;
	private boolean useSubDoc;
	private int errorCount;
	private RawDocument rawDoc;
	private ITextUnitMerger textUnitMerger;

	/**
	 * Creates a Merger object.
	 * @param parameters The merging step parameters
	 * @param fcMapper The filter mapper to use
	 * @param manifest The manifest to process
	 * @param forcedTargetLocale The target locale to merge with
	 */
	public Merger(
		final Parameters parameters,
		final IFilterConfigurationMapper fcMapper,
		final Manifest manifest,
		final LocaleId forcedTargetLocale
	) {
		this.parameters = parameters;
		this.fcMapper = fcMapper;
		this.manifest = manifest;
		trgLoc = forcedTargetLocale;
		// FIXME: Rainbow set TextUnitMerge parameters if needed
		textUnitMerger = new TextUnitMerger();
	}

	public void close () {
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
		if ( filter != null ) {
			filter.close();
			filter = null;
		}
	}

	public Event handleEvent (Event event) {
		switch ( event.getEventType() ) {
		case TEXT_UNIT:
			processTextUnit(event);
			if ( parameters.getReturnRawDocument() ) {
				event = Event.createNoopEvent();
			}
			break;
		case START_SUBDOCUMENT:
			if ( parameters.getReturnRawDocument() ) {
				useSubDoc = true;
				event = Event.createNoopEvent();
			}
			break;
		case END_DOCUMENT:
			flushFilterEvents();
			close();
			if ( parameters.getReturnRawDocument() && !useSubDoc ) {
				event = createMultiEvent();
			}
			break;
		default:
			if ( parameters.getReturnRawDocument() ) {
				event = Event.createNoopEvent();
			}
		}
		
		return event;
	}
	
	private Event createMultiEvent () {
		List<Event> list = new ArrayList<>();
		
		// Change the pipeline parameters for the raw-document-related data
		PipelineParameters pp = new PipelineParameters();
		pp.setOutputURI(rawDoc.getInputURI()); // Use same name as this output for now
		pp.setSourceLocale(rawDoc.getSourceLocale());
		pp.setTargetLocale(rawDoc.getTargetLocale());
		pp.setOutputEncoding(rawDoc.getEncoding()); // Use same as the output document
		pp.setInputRawDocument(rawDoc);
		// Add the event to the list
		list.add(new Event(EventType.PIPELINE_PARAMETERS, pp));

		// Add raw-document related events
		list.add(new Event(EventType.RAW_DOCUMENT, rawDoc));
		
		// Return the list as a multiple-event event
		return new Event(EventType.MULTI_EVENT, new MultiEvent(list));
	}

	public Event startMerging (MergingInfo info,
		Event event)
	{
		errorCount = 0;
		useSubDoc = false;
		logger.info("Merging: {}", info.getRelativeInputPath());
		// Create the filter for this original file
		filter = fcMapper.createFilter(info.getFilterId(), filter);
		if ( filter == null ) {
			throw new OkapiBadFilterInputException(String.format("Filter cannot be created (%s).", info.getFilterId()));
		}
		IParameters fprm = filter.getParameters();
		if ( fprm != null ) {
			fprm.fromString(info.getFilterParameters());
		}

		File file = new File(manifest.getTempOriginalDirectory() + info.getRelativeInputPath());
		RawDocument rd = new RawDocument(file.toURI(), info.getInputEncoding(),
			manifest.getSourceLocale(), trgLoc);
		
		filter.open(rd);
		writer = filter.createFilterWriter();
		writer.setOptions(trgLoc, info.getTargetEncoding());
		String outPath = getOutputPath(info);
		writer.setOutput(outPath);
		
		// Skip entries with empty source for PO
		skipEmptySourceEntries = ( info.getExtractionType().equals(Manifest.EXTRACTIONTYPE_PO)
			|| info.getExtractionType().equals(Manifest.EXTRACTIONTYPE_TRANSIFEX)
			|| info.getExtractionType().equals(Manifest.EXTRACTIONTYPE_TABLE) );
		
		// Process the start document in the document we just open
		Event internalEvent = null;
		if ( filter.hasNext() ) {
			// Should be the start-document event
			internalEvent = filter.next();
		}
		if (( internalEvent == null ) || ( internalEvent.getEventType() != EventType.START_DOCUMENT )) {
			errorCount++;
			logger.error("The start document event is missing when parsing the original file.");
		}
		else {
			writer.handleEvent(internalEvent);
		}
		
		// Compute what event to return
		if ( parameters.getReturnRawDocument() ) {
			if ( event.getStartDocument().isMultilingual() ) {
				rawDoc = new RawDocument(new File(outPath).toURI(), info.getTargetEncoding(),
					manifest.getSourceLocale(), manifest.getTargetLocale());
			}
			else {
				// Otherwise: the previous target is now the source (and still the target)
				rawDoc = new RawDocument(new File(outPath).toURI(), info.getTargetEncoding(),
					manifest.getTargetLocale(), manifest.getTargetLocale());
			}
			event = Event.createNoopEvent();
		}
		else {
			event = internalEvent;
		}
		
		return event;
	}
	
	private String getOutputPath (MergingInfo info) {
		if ( Util.isEmpty(parameters.getOverrideOutputPath()) ) {
			return manifest.getMergeDirectory() + info.getRelativeTargetPath();
		}
		else {
			return Util.ensureSeparator(parameters.getOverrideOutputPath(), false) + info.getRelativeTargetPath();
		}
	}

	private void flushFilterEvents () {
		// Finish to go through the original file
		while ( filter.hasNext() ) {
			writer.handleEvent(filter.next());
		}
		writer.close();
	}
	
	private ITextUnit mergeTextUnit(ITextUnit tuFromSkel, ITextUnit tuFromTrans) {
		// Skip the non-translatable
		// This means the translate attributes must be the same
		// in the original and the merging files
		if (!tuFromSkel.isTranslatable())
			return tuFromSkel;

		// return the (possibly) merged TextUnit
		textUnitMerger.setTargetLocale(trgLoc);
		ITextUnit mergedTu = textUnitMerger.mergeTargets(tuFromSkel, tuFromTrans);

		return mergedTu;
	}

	private void processTextUnit (Event event) {
		// Get the unit from the translation file
		ITextUnit traTu = event.getTextUnit();
	
		// search for the corresponding event in the original
		Event oriEvent = processUntilTextUnit();
		if ( oriEvent == null ) {
			errorCount++;
			logger.error("No corresponding text unit for id='{}' in the original file.", traTu.getId());
			return;
		}
		// Get the actual text unit object of the original
		ITextUnit oriTu = oriEvent.getTextUnit();

		ITextUnit mergedTu = mergeTextUnit(oriTu, traTu);
		
		// Create or overwrite 'approved' flag is requested
		if (manifest.getUpdateApprovedFlag()) {
			mergedTu.getTarget(trgLoc).setProperty(new Property(Property.APPROVED, "yes"));
		}
		
		// Output the translation
		oriEvent.setResource(mergedTu);
		writer.handleEvent(oriEvent);
	}

	/**
	 * Get events in the original document until the next text unit.
	 * Any event before is passed to the writer.
	 * @return the event of the next text unit, or null if no next text unit is found.
	 */
	private Event processUntilTextUnit () {
		while ( filter.hasNext() ) {
			Event event = filter.next();
			if ( event.getEventType() == EventType.TEXT_UNIT ) {
				ITextUnit tu = event.getTextUnit();
				if ( !tu.isTranslatable() ) {
					// Do not merge the translation for non-translatable
					writer.handleEvent(event);
					continue;
				}
				if ( skipEmptySourceEntries && tu.isEmpty() ) {
					// For some types of package: Do not merge the translation for non-translatable
					writer.handleEvent(event);
					continue;
				}
				return event;
			}
			// Else: write out the event
			writer.handleEvent(event);
		}
		// This text unit is extra in the translated file
		return null;
	}

	/**
	 * Gets the number of errors since the last call to {@link #startMerging(MergingInfo, Event)}.
	 * @return the number of errors. 
	 */
	public int getErrorCount () {
		return errorCount;
	}
}
