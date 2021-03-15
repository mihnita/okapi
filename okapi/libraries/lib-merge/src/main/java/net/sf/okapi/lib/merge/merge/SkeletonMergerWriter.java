/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

package net.sf.okapi.lib.merge.merge;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiMergeException;
import net.sf.okapi.common.exceptions.OkapiUnexpectedResourceTypeException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import java.io.OutputStream;


/**
 * Basic class for skeleton-based merging. <b>Override for specific behaviors.</b>
 * <p>
 * Takes a skeleton file and
 * {@link Event}s from a translated document. Translated segments are merged
 * into the skeleton {@link TextUnit} events and written out using the default
 * {@link IFilterWriter}
 * 
 * @author jimh
 *
 */
public class SkeletonMergerWriter implements IFilterWriter {
	private IFilter skeletonFilter;
	private String outputPath;
	private OutputStream outputStream;
	private LocaleId targetLocale;
	private String encoding;
	private IFilterWriter writer;
	private Parameters params;
	private ITextUnitMerger textUnitMerger;
	
	public SkeletonMergerWriter() {
		params = new Parameters();
		textUnitMerger = new TextUnitMerger();
	}

	public SkeletonMergerWriter(IFilter skeletonFilter) {
		this();
		this.skeletonFilter = skeletonFilter;
	}

	/**
	 * Use specific {@link IFilter} and {@link IFilterWriter} implementations
	 * @param skeletonFilter - {@link IFilter} used to read skeleton, can be serialized
	 * events or original source file. <b>ASSUME FILTER OPEN ALREADY CALLED</b>
	 * @param writer - override the writer specified in the skeleton {@link StartDocument} 
	 * event. Can use null value for writer to use the default writer.
	 * @param textUnitMerger user created {@link ITextUnitMerger}
	 */
	public SkeletonMergerWriter(IFilter skeletonFilter, IFilterWriter writer, ITextUnitMerger textUnitMerger) {
		this(skeletonFilter);
		this.writer = writer;
		if (textUnitMerger != null) {
			this.textUnitMerger = textUnitMerger;
		}
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public void setOptions(LocaleId targetLocale, String defaultEncoding) {
		this.targetLocale = targetLocale;
		encoding = defaultEncoding;
		textUnitMerger.setTargetLocale(targetLocale);
	}

	@Override
	public void setOutput(String path) {
		outputPath = path;
	}

	@Override
	public void setOutput(OutputStream output) {
		outputStream = output;
	}

	/*
	 * Return the passed in event unaltered. 
	 */
	@Override
	public Event handleEvent(Event event) {
		switch (event.getEventType()) {
		case START_DOCUMENT:
			processStartDocument();
			break;
		case TEXT_UNIT:
			processTextUnit(event);
			break;
		case END_DOCUMENT:
			processEndDocument();
			break;
		case NO_OP:
		case START_SUBDOCUMENT:
		case END_SUBDOCUMENT:
		case CANCELED:
		case CUSTOM:
		case DOCUMENT_PART:
		case END_BATCH:
		case END_BATCH_ITEM:
		case END_GROUP:
		case END_SUBFILTER:
		case MULTI_EVENT:
		case PIPELINE_PARAMETERS:
		case RAW_DOCUMENT:
		case START_BATCH:
		case START_BATCH_ITEM:
		case START_GROUP:
		case START_SUBFILTER:
		default:
			break;
		}
		return event;
	}

	@Override
	public void close() {
		if (writer != null) writer.close();
		// must null so new writer type will be 
		// instantiated on next use
		writer = null;
		skeletonFilter.close();
	}

	@Override
	public Parameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (Parameters)params;
		textUnitMerger.setParameters(this.params);
	}

	@Override
	public void cancel() {
		close();
	}

	@Override
	public EncoderManager getEncoderManager() {
		return null;
	}

	@Override
	public ISkeletonWriter getSkeletonWriter() {
		return null;
	}

	/**
	 * Use the skeleton {@link StartDocument} event to initialize the
	 * {@link IFilterWriter}. Initialize the {@link ITextUnitMerger}
	 */
	protected void processStartDocument() {
		Event skelEvent = skeletonFilter.next();

		// Process the start document in the document we just open
		if ((skelEvent == null) || (!skelEvent.isStartDocument())) {
			throw new OkapiUnexpectedResourceTypeException(
					"The start document event can't be found in the skeleton file.");
		} else {
			StartDocument sd = skelEvent.getStartDocument();
			
			// Create and setup the writer, unless one has already been passed to us
			if (writer == null) {
				writer = sd.getFilterWriter();
			}
			writer.setOptions(targetLocale, encoding);
			if (outputStream != null) {
				writer.setOutput(outputStream);
			} else if (!Util.isEmpty(outputPath)) {
				writer.setOutput(outputPath);
			} else {
				throw new OkapiIOException("Output path or stream not defined for filter writer");
			}

			// Write the initial event
			writer.handleEvent(skelEvent);
		}
	}

	/**
	 * Take the translated {@link TextUnit} and match it up with its corresponding
	 * skeleton version. Call {@link ITextUnitMerger} to merge the translated segments 
	 * into the skeleton {@link TextUnit}
	 * @param event - the translated version of the {@link ITextUnit} event
	 */
	protected void processTextUnit(Event event) {
		// Get the unit from the translation file
		ITextUnit tuFromTrans = event.getTextUnit();

		// search for the corresponding event in the original
		Event oriEvent = processUntilTextUnit();
		if (oriEvent == null) {
			throw new OkapiMergeException(String.format(
					"No corresponding text unit for id='%s' in the skeleton file.", tuFromTrans.getId()));
		}

		// Get the actual text unit of the skeleton
		ITextUnit tuFromSkel = oriEvent.getTextUnit();
		if (!tuFromSkel.isTranslatable()) {
			// if not translatable write out skeleton version
			writer.handleEvent(new Event(EventType.TEXT_UNIT, tuFromSkel));
			return;
		}

		// even if the skeleton source is empty the target may not be
		// so merge it. Also the tuFromTrans may have skeleton we 
		// don't want. Merge will take care if this too
		
		// return the (possibly) merged TextUnit
		ITextUnit mergedTu = textUnitMerger
				.mergeTargets(tuFromSkel, tuFromTrans);

		// write out (possibly) merged TextUnit
		writer.handleEvent(new Event(EventType.TEXT_UNIT, mergedTu));		
	}

	/**
	 * Get events in the original document until the next text unit. Any event
	 * before is passed to the writer.
	 * 
	 * @return the event of the next text unit, or null if no next text unit is
	 *         found.
	 */
	protected Event processUntilTextUnit() {
		Event event = null;
		while (skeletonFilter.hasNext()) {
			event = skeletonFilter.next();

			// No more events
			if (event == null) {
				return event;
			}

			// Process that event
			if (event.isTextUnit()) {
				return event;
			}

			// write out the non-TextUnit event
			writer.handleEvent(event);
		}

		return event;
	}

	/**
	 * There are no more {@link TextUnit}s. Read the remaining skeleton
	 * events and write them out.
	 *
	 */
	protected void processEndDocument() {
		flushFilterEvents();
	}

	private void flushFilterEvents() {
		try {
			// Finish the skeleton events
			Event event;
			while (skeletonFilter.hasNext()) {
				event = skeletonFilter.next();
				if (event.isTextUnit()) {
					throw new OkapiMergeException(String.format(
							"No corresponding text unit for id='%s' in the skeleton file.", event.getTextUnit().getId()));
				}
				writer.handleEvent(event);
			} 
		} finally {
			writer.close();
		}
	}

	/**
	 * Set the {@link IFilterWriter} used to write out the skeleton events.
	 * This will override the internal writer as defined by {@link StartDocument}
	 * in the skeleton.
	 * <p>
	 * <b>Must be called immediately after construction!!</b>
	 * 
	 * @param writer - {@link IFilterWriter} used to write out the skeleton events
	 */
	public void setWriter(IFilterWriter writer) {
		this.writer = writer;
	}
}
