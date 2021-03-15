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

package net.sf.okapi.applications.rainbow.utilities.merging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.applications.rainbow.packages.IReader;
import net.sf.okapi.applications.rainbow.packages.Manifest;
import net.sf.okapi.applications.rainbow.packages.ManifestItem;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.filters.rtf.RTFFilter;
import net.sf.okapi.lib.merge.merge.ITextUnitMerger;
import net.sf.okapi.lib.merge.merge.Parameters;
import net.sf.okapi.lib.merge.merge.TextUnitMerger;

public class Merger {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Manifest manifest;
	private IReader reader;
	private FilterConfigurationMapper mapper;
	private IFilter inpFilter;
	private IFilterWriter outFilter;
	private RTFFilter rtfFilter;
	private LocaleId trgLoc;
	final private ITextUnitMerger textUnitMerger;

	public Merger () {
		// Load the filter configurations
		mapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(mapper, false, true);
		textUnitMerger = new TextUnitMerger();
		// FIXME: Rainbow set TextUnit merge parameters if needed
		Parameters p = new Parameters();
		textUnitMerger.setParameters(p);
		// No need to load custom configuration because we are loading the parameters ourselves
	}

	public void initialize (Manifest manifest) {
		// Close any previous reader
		if ( reader != null ) {
			reader.closeDocument();
			reader = null;
		}
		// Set the manifest and the options
		this.manifest = manifest;
		trgLoc = manifest.getTargetLanguage();
	}
	
	public void execute (int docId) {
		ManifestItem item = manifest.getItem(docId);
		// Skip items not selected for merge
		if ( !item.selected() ) return;

		// Merge or convert depending on the post-processing selected
		if ( item.getPostProcessingType().equals(ManifestItem.POSPROCESSING_TYPE_RTF) ) {
			convertFromRTF(docId, item);
		}
		else { // Default: use the reader-driven process
			merge(docId, item);
		}
	}
	
	private void convertFromRTF (int docId,
		ManifestItem item)
	{
		OutputStreamWriter writer = null;
		try {
			// File to convert
			String fileToConvert = manifest.getFileToMergePath(docId);

			// Instantiate the reader if needed
			if ( rtfFilter == null ) {
				rtfFilter = new RTFFilter();
			}

			logger.info("\nConverting: {}", fileToConvert);
			
			//TODO: get LB info from original
			String lineBreak = Util.LINEBREAK_DOS;
			
			// Open the RTF input
			File f = new File(fileToConvert);
			//TODO: guess encoding based on language
			rtfFilter.open(new RawDocument(f.toURI(), "windows-1252", manifest.getTargetLanguage()));
				
			// Open the output document
			// Initializes the output
			String outputFile = manifest.getFileToGeneratePath(docId);
			Util.createDirectories(outputFile);
			writer = new OutputStreamWriter(new BufferedOutputStream(
				new FileOutputStream(outputFile)), item.getOutputEncoding());
			//TODO: check BOM option from original
			Util.writeBOMIfNeeded(writer, false, item.getOutputEncoding());
				
			// Process
			StringBuilder buf = new StringBuilder();
			while ( rtfFilter.getTextUntil(buf, -1, 0) == 0 ) {
				writer.write(buf.toString());
				writer.write(lineBreak);
			}
			
		}		
		catch ( Exception e ) {
			// Log and move on to the next file
			Throwable e2 = e.getCause();
			logger.error("Conversion error. {}", ((e2!=null) ? e2.getMessage() : e.getMessage()), e);
		}
		finally {
			if ( rtfFilter != null ) {
				rtfFilter.close();
			}
			if ( writer != null ) {
				try {
					writer.close();
				}
				catch ( IOException e ) {
					logger.error("Conversion error when closing file. {}", e.getMessage(), e);
				}
			}
		}
	}
	
	private void merge (int docId, ManifestItem item)
	{
		Event event;
		try {
			// File to merge
			String fileToMerge = manifest.getFileToMergePath(docId);
			// Instantiate a package reader of the proper type
			if ( reader == null ) {
				reader = (IReader)Class.forName(manifest.getReaderClass()).newInstance();
			}
			logger.info("\nMerging: {}", fileToMerge);

			// Original and parameters files
			String originalFile = manifest.getRoot() + File.separator + manifest.getOriginalLocation()
				+ File.separator + String.format("%d.ori", docId);
			String paramsFile = manifest.getRoot() + File.separator + manifest.getOriginalLocation()
				+ File.separator + String.format("%d.fprm", docId);
			// Load the relevant filter
			inpFilter = mapper.createFilter(item.getFilterID(), inpFilter);
			IParameters params = inpFilter.getParameters();
			// Load them only if the filter has parameters
			if ( params != null ) {
				File file = new File(paramsFile);
				params.load(Util.URItoURL(file.toURI()), false);
			}

			reader.openDocument(fileToMerge, manifest.getSourceLanguage(), manifest.getTargetLanguage());
			
			// Initializes the input
			File f = new File(originalFile);
			inpFilter.open(new RawDocument(f.toURI(), item.getInputEncoding(),
				manifest.getSourceLanguage(), trgLoc));
			
			// Initializes the output
			String outputFile = manifest.getFileToGeneratePath(docId);
			Util.createDirectories(outputFile);
			outFilter = inpFilter.createFilterWriter();
			outFilter.setOptions(trgLoc, item.getOutputEncoding());
			outFilter.setOutput(outputFile);
			
			// Process the document
			while ( inpFilter.hasNext() ) {
				event = inpFilter.next();
				if ( event.getEventType() == EventType.TEXT_UNIT ) {
					ITextUnit mergedTu = processTextUnit(event.getTextUnit());
					// write out (possibly) merged TextUnit
					outFilter.handleEvent(new Event(EventType.TEXT_UNIT, mergedTu));		
				} else {
					outFilter.handleEvent(event);
				}
			}
		}
		catch ( Exception e ) {
			// Log and move on to the next file
			Throwable e2 = e.getCause();
			logger.error("Merging error. {}", ((e2!=null) ? e2.getMessage() : e.getMessage()), e);
		}
		finally {
			if ( reader != null ) {
				reader.closeDocument();
				reader = null;
			}
			if ( inpFilter != null ) {
				inpFilter.close();
				inpFilter = null;
			}
			if ( outFilter != null ) {
				outFilter.close();
				outFilter = null;
			}
		}
	}

	private ITextUnit processTextUnit (ITextUnit tuFromSkel) {
		// Skip the non-translatable
		// This means the translate attributes must be the same
		// in the original and the merging files
		if ( !tuFromSkel.isTranslatable() ) return tuFromSkel;

		// find next translation
		while ( true ) {
			if ( !reader.readItem() ) {
				// Problem: 
				logger.warn("There are no more items in the package to merge with id=\"{}\".", tuFromSkel.getId());
				return tuFromSkel;
			}
			break;
		}
		
		// return the (possibly) merged TextUnit
		textUnitMerger.setTargetLocale(trgLoc);
		ITextUnit mergedTu = textUnitMerger.mergeTargets(tuFromSkel, reader.getItem());
				
		// Create or overwrite 'approved' flag is requested
		if ( manifest.updateApprovedFlag() ) {
			mergedTu.getTarget(trgLoc).setProperty(new Property(Property.APPROVED, "yes"));
		}
		
		return mergedTu;
	}
}
