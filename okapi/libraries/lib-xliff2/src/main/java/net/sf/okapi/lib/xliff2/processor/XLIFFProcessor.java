/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.processor;

import java.io.File;
import java.io.InputStream;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

/**
 * High-level class implementing the low-level reading and (optionally) re-writing of a document,
 * with the possibility to easily modify the content.
 */
public class XLIFFProcessor implements Runnable {
	
	private XLIFFReader reader;
	private XLIFFWriter writer;
	private List<IEventHandler> handlers;

	public XLIFFProcessor () {
		handlers = new ArrayList<>();
	}
	
	public void setInput (File inputFile) {
		reader = new XLIFFReader();
		reader.open(inputFile);
	}
	
	public void setInput (String inputText) {
		reader = new XLIFFReader();
		reader.open(inputText);
	}
	
	public void setInput (URI inputURI) {
		reader = new XLIFFReader();
		reader.open(inputURI);
	}
	
	public void setInput (InputStream inputStream) {
		reader = new XLIFFReader();
		reader.open(inputStream);
	}
	
	/**
	 * Sets the output file. 
	 * @param outputFile the output file (Use null to specify no output).
	 */
	public void setOutput (File outputFile) {
		if ( outputFile == null ) {
			writer = null;
		}
		else {
			writer = new XLIFFWriter();
			writer.create(outputFile, null);
		}
	}
	
	public void setOutput (Writer outputWriter) {
		if ( outputWriter == null ) {
			writer = null;
		}
		else {
			writer = new XLIFFWriter();
			writer.create(outputWriter, null);
		}
	}

	/**
	 * Run the processor for a given input file, and (if specified) re-write the modified document 
	 * in a given output file. The two file names must be different.
	 * The input and output are closed at the end of the process.
	 * This methods is an helper method that simply calls {@link #setInput(File)}, 
	 * {@link #setOutput(File)} and then {@link #run()}.
	 * @param inputFile document to modify.
	 * @param outputFile resulting document. Use null to not produce an output file.
	 * @throws InvalidParameterException if the input and output files are the same.
	 */
	public void run (File inputFile,
		File outputFile)
	{
		if ( outputFile != null ) {
			if ( inputFile.equals(outputFile) ) {
				throw new InvalidParameterException("The output file must be different from the input file.");
			}
		}
		setInput(inputFile);
		setOutput(outputFile);
		run();
	}

	/**
	 * Run the processor for a given input file.
	 * @param inputFile document to process.
	 */
	public void run (File inputFile) {
		run(inputFile, null);
	}
	
	/**
	 * Process the document. You must have set the input, output and event handler 
	 * before calling this method.
	 * The input and output are closed at the end of the process. 
	 */
	@Override
	public void run () {
		try {
			// Process the document
			while ( reader.hasNext() ) {
				// Get the event
				Event event = reader.next();
				// Invoke all the handlers
				for ( IEventHandler handler : handlers ) {
					// Handle the event
					switch ( event.getType() ) {
					case END_FILE:
						event = handler.handleEndFile(event);
						break;
					case END_GROUP:
						event = handler.handleEndGroup(event);
						break;
					case END_DOCUMENT:
						event = handler.handleEndDocument(event);
						break;
					case END_XLIFF:
						event = handler.handleEndXliff(event);
						break;
					case INSIGNIFICANT_PART:
						event = handler.handleInsignificantPart(event);
						break;
					case MID_FILE:
						event = handler.handleMidFile(event);
						break;
					case SKELETON:
						event = handler.handleSkeleton(event);
						break;
					case START_FILE:
						event = handler.handleStartFile(event);
						break;
					case START_GROUP:
						event = handler.handleStartGroup(event);
						break;
					case START_DOCUMENT:
						event = handler.handleStartDocument(event);
						break;
					case START_XLIFF:
						event = handler.handleStartXliff(event);
						break;
					case TEXT_UNIT:
						event = handler.handleUnit(event);
						break;
					}
				}
				// Write the modified event (if needed)
				if ( writer != null ) {
					writer.writeEvent(event);
				}
			}
		}
		finally {
			if ( reader != null ) reader.close();
			if ( writer != null ) writer.close();
		}
	}

	/**
	 * Sets a single event handler to make modifications on a selection of events.
	 * All existing handler are removed before setting this one. 
	 * @param handler the handler to set.
	 */
	public void setHandler (IEventHandler handler) {
		handlers.clear();
		handlers.add(handler);
	}

	/**
	 * Adds an event handler to this processor.
	 * Existing handlers are preserved.
	 * The handlers are executed in the order they have been added.
	 * @param handler the handler to add.
	 */
	public void add (IEventHandler handler) {
		handlers.add(handler);
	}

	/**
	 * Removes all handlers from this processor.
	 */
	public void removeAll () {
		handlers.clear();
	}

	/**
	 * Removes a given handler from this processor.
	 * @param handler the handler to remove.
	 */
	public void remove (IEventHandler handler) {
		handlers.remove(handler);
	}
	
	/**
	 * Removes the handler at a given position.
	 * @param index the index of the handler to remove.
	 * @throws IndexOutOfBoundsException if the index in invalid.
	 */
	public void remove (int index) {
		handlers.remove(index);
	}

}
