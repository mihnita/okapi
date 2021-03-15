/*===========================================================================
  Copyright (C) 2009-2010 by the Okapi Framework contributors
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

package net.sf.okapi.steps.xliffsplitter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadFilterParametersException;
import net.sf.okapi.common.exceptions.OkapiBadStepInputException;
import net.sf.okapi.common.exceptions.OkapiFileNotFoundException;
import net.sf.okapi.common.exceptions.OkapiUnsupportedEncodingException;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.RawDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@UsingParameters(XliffJoinerParameters.class)
public class XliffJoinerStep extends BasePipelineStep {

	/**
	 * Help class for merging xliff files with different base filenames   
	 */
	private class BaseXliffFile {
		
		private List<XMLEvent> firstFileTempElems = new ArrayList<>();
		private List<String> filesUsed = new ArrayList<>();
		private int fileCount;
		private XMLEventWriter eventWriter = null;
		private String currentFileName = "";

		// </body> and </file> events that are appended when a file finally terminates.
		private List<XMLEvent> fileEndEvents = new ArrayList<>();

		void write(XMLEvent event) throws XMLStreamException {
			eventWriter.add(event);
		}

		/**
		 * Initiates the writer for the first instance of a base filename
		 */
        void initiateWriter(String pOutputFileUri, String pEncoding, boolean pHasUTF8BOM){

        	try {
        		//--this section is for writing the bom--
        		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pOutputFileUri),pEncoding));
        		Util.writeBOMIfNeeded(bw, pHasUTF8BOM, pEncoding);

        		eventWriter = outputFactory.createXMLEventWriter(bw);

        	}catch (UnsupportedEncodingException e) {
        		throw new OkapiUnsupportedEncodingException(e);
        	}catch (FileNotFoundException e) {
        		throw new OkapiFileNotFoundException(e);
        	}catch (XMLStreamException e) {
        		throw new OkapiBadStepInputException(e);
        	}
        }
		
		
		/**
		 * Writes the part after the last file for the first instance of a base filename and closes the writer
		 * @throws XMLStreamException
		 */
		void writeAndClose() throws XMLStreamException{
			writeFileEnd();
			if (eventWriter != null){
				for (XMLEvent ev : firstFileTempElems){
					write(ev);
				}
				eventWriter.flush();
				eventWriter.close();
			}
		}

		void writeFileEnd() throws XMLStreamException {
			// We may not need to do anything here!
			if (currentFileName.isEmpty() || fileEndEvents.isEmpty()) {
				return;
			}
			for (XMLEvent e : fileEndEvents) {
				write(e);
			}
			fileEndEvents.clear();
		}
	}
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private HashMap<String, BaseXliffFile> baseXliffFiles = new HashMap<>();
	
	private XliffJoinerParameters params;
	private URI outputURI;

	XMLInputFactory inputFactory = XMLInputFactory.newInstance();
	XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
	XMLEventFactory  eventFactory = XMLEventFactory.newInstance();
	

	public XliffJoinerStep () {
		params = new XliffJoinerParameters();
		// security concern. Turn off DTD processing
		// https://www.owasp.org/index.php/XML_External_Entity_%28XXE%29_Processing
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);			
	}
	
	@StepParameterMapping(parameterType = StepParameterType.OUTPUT_URI)
	public void setOutputURI(final URI outputURI) {
		this.outputURI = outputURI;
	}
	
	public URI getOutputURI() {
		return outputURI;
	}
	
	@Override
	public String getDescription () {
		return "Join multiple XLIFF documents into one. Expects: raw document. Sends back: raw document.";
	}

	@Override
	public String getName () {
		return "XLIFF Joiner";
	}

	@Override
	public XliffJoinerParameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (XliffJoinerParameters)params;
	}
 
	@Override
	protected Event handleStartBatch(Event event) {
		if(params.getInputFileMarker().trim().length() == 0){
			throw new OkapiBadFilterParametersException("The input file marker cannot be empty");
		}else if(params.getOutputFileMarker().trim().length() == 0){
			logger.warn("Leaving output file marker empty your original file(s) could be overwritten");
		}
		
		return event;
	}
	
	@Override
	protected Event handleEndBatch (Event event) {
		
		for (Map.Entry<String, BaseXliffFile> entry : baseXliffFiles.entrySet()) { 

		    BaseXliffFile baseFile = entry.getValue(); 

			try {
				baseFile.writeAndClose();
			} catch (XMLStreamException e) {
				throw new OkapiBadStepInputException(e);
			}
		} 
		return event;
	}

	@Override
	protected Event handleRawDocument(final Event event) {

		BaseXliffFile baseFile;
		int fileElemNo = 0;
		XMLEvent xmlEvent;
		
		List<XMLEvent> elemsBetweenOrAfterLastFileElem = new ArrayList<>();
		
		final RawDocument rawDoc = event.getRawDocument();		
		
		//--for output filename--
		String outputDir = Util.getDirectoryName(outputURI.getPath());
		String inputFileName = Util.getFilename(rawDoc.getInputURI().getPath(), false);
		String inputFileExtension = Util.getExtension(rawDoc.getInputURI().getPath());
		
		String baseFilename = getBaseFilename(inputFileName, params.getInputFileMarker());
		
		if ( baseFilename == null){
			logger.warn("This file is skipped: Input marker not found in its name.");
			return event;
		}

		//--TODO validate base filename and marker--

		//--detect file properties for each file-
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(rawDoc.getStream(),"utf-8");
		detector.detectBom();

		String encoding = detector.getEncoding();
		boolean hasUTF8BOM = detector.hasUtf8Bom();
		String lineBreak = detector.getNewlineType().toString();

		
		//--check if this is the first file
		if(baseXliffFiles.containsKey(baseFilename)){
			baseFile = baseXliffFiles.get(baseFilename);
			
			baseFile.fileCount++;
			baseFile.filesUsed.add(Util.getFilename(rawDoc.getInputURI().getPath(), true));

		}else{
        	String outputFileUri = outputDir + File.separator + baseFilename 
        	+ params.getOutputFileMarker() + inputFileExtension;

			baseFile = new BaseXliffFile();
			baseFile.initiateWriter(outputFileUri, encoding, hasUTF8BOM);			

			baseXliffFiles.put(baseFilename, baseFile);
			
			baseFile.fileCount=1;
			baseFile.filesUsed.add(Util.getFilename(rawDoc.getInputURI().getPath(), true));
		}
		
	
        //--initiate the reader for each file--
		XMLEventReader eventReader = initiateReader(detector, encoding, rawDoc.getStream());

		while (eventReader.hasNext()) {

			try {
				xmlEvent = eventReader.nextEvent();
				
				if ( baseFile.fileCount == 1){

					if (xmlEvent.getEventType() == XMLEvent.START_DOCUMENT){
						
						baseFile.write(xmlEvent);
						baseFile.write(eventFactory.createSpace(lineBreak));
							
						continue;
					}

					if (isFileStart(xmlEvent)) {
						fileElemNo++;
						writeFilePart(baseFile, xmlEvent.asStartElement(), eventReader, fileElemNo, baseFile.firstFileTempElems);
					}else{
						if(fileElemNo == 0){
							//--writing anything before file
							baseFile.write(xmlEvent);
						}else{
							//--otherwise storing it as either between or after content
							baseFile.firstFileTempElems.add(xmlEvent);
						}
					}
					
				} else {
					
					//--for following files write only the content between the start and closing file--
					if (isFileStart(xmlEvent)) {

						baseFile.write(eventFactory.createSpace(lineBreak));
						
						fileElemNo++;
						writeFilePart(baseFile, xmlEvent.asStartElement(), eventReader, fileElemNo, elemsBetweenOrAfterLastFileElem);
					}else{
						if(fileElemNo > 0){
							//--otherwise storing it as either between or after content
							elemsBetweenOrAfterLastFileElem.add(xmlEvent);
						}
					}
				}
				
			} catch (XMLStreamException e) {
				throw new OkapiBadStepInputException(e);
			}
		}
		
		return event;
	}

	private static boolean isFileStart(XMLEvent xmlEvent) {
		return xmlEvent.getEventType() == XMLEvent.START_ELEMENT && xmlEvent.asStartElement().getName().getLocalPart().equals("file");
	}
	private static boolean isBodyStart(XMLEvent xmlEvent) {
		return xmlEvent.getEventType() == XMLEvent.START_ELEMENT && xmlEvent.asStartElement().getName().getLocalPart().equals("body");
	}
	private static boolean isFileEnd(XMLEvent xmlEvent) {
		return xmlEvent.getEventType() == XMLEvent.END_ELEMENT && xmlEvent.asEndElement().getName().getLocalPart().equals("file");
	}
	private static boolean isBodyEnd(XMLEvent xmlEvent) {
		return xmlEvent.getEventType() == XMLEvent.END_ELEMENT && xmlEvent.asEndElement().getName().getLocalPart().equals("body");
	}

	private XMLEventReader initiateReader(BOMNewlineEncodingDetector detector,
			String encoding, InputStream inputStream) {
		
		XMLEventReader eventReader;

		try {
    		if ( detector.isAutodetected() ) {
    			eventReader = inputFactory.createXMLEventReader(inputStream, encoding);
    		}
    		else {
    			logger.info("Encoding could not be auto-detected. Using default encoding: {}", encoding);
    			eventReader = inputFactory.createXMLEventReader(inputStream);
    		}
    	} catch (XMLStreamException e) {
    		throw new OkapiBadStepInputException(e);
    	}
    	
    	return eventReader;
	}
	
	/**
	 * Return the base filename
	 * @param fileName
	 * @param fileMarker
	 * @return
	 */
	private String getBaseFilename(String fileName, String fileMarker) {

		int index = fileName.lastIndexOf(fileMarker);
		
		if(index == -1)
			return null;
		else 
			return fileName.substring(0,index);
	}

	/**
	 * Writes a <file> section including content between previous ending </file> and current <file>
	 * @param startFileEvent The <file> element
	 * @param eventReader The eventReader for the current file
	 * @param eventWriter The eventWriter for the current base file
	 * @param pFileElemIndex The <file> element index in the current file, supporting multiple file elements
	 * @param pTempElems The content that has been collected since the last </file>
	 * @throws XMLStreamException
	 */
	private void writeFilePart(BaseXliffFile xliffFile, StartElement startFileEvent, XMLEventReader eventReader, int pFileElemIndex, List<XMLEvent> pTempElems) throws XMLStreamException {
		Attribute a = startFileEvent.getAttributeByName(new QName("original"));
		boolean isContinuation = false;
		String originalFileName = "";
		if (a != null) {
			originalFileName = a.getValue();
		}
		else {
			logger.warn("Missing 'original' attribute on <file>");
		}
		// If we are already in this file, we don't need to start it again
		if (originalFileName.trim().isEmpty() || !originalFileName.equals(xliffFile.currentFileName)) {
			// Terminate any existing current file
			xliffFile.writeFileEnd();

			// Start a new <file>
			xliffFile.currentFileName = originalFileName;
			writeFileStart(xliffFile, startFileEvent, pFileElemIndex, pTempElems);
		}
		else {
			isContinuation = true;
			// Skip the <body>, we already have one
			for (XMLEvent p = eventReader.nextEvent(); p != null; p = eventReader.nextEvent()) {
				if (isBodyStart(p)) {
					break;
				}
			}
		}

		//--write remaining <file> content
		while (eventReader.hasNext()) {
			XMLEvent xmlEvent = eventReader.nextEvent();

			if (isBodyEnd(xmlEvent)) {
				if (!isContinuation) {
					xliffFile.fileEndEvents.add(xmlEvent);
				}
				continue;
			}
			if (isFileEnd(xmlEvent)) {
				if (!isContinuation) {
					xliffFile.fileEndEvents.add(xmlEvent);
				}
				return;
			}

			xliffFile.write(xmlEvent);
		}
	}

	private void writeFileStart(BaseXliffFile xliffFile, StartElement startFileEvent, int pFileElemIndex, List<XMLEvent> pTempElems) throws XMLStreamException {
		//--write pre <file> content if needed--
		if (pFileElemIndex > 1) {
			for (XMLEvent tempElem : pTempElems) {
				xliffFile.write(tempElem);
			}
			pTempElems.clear();
		}
		//--write start <file>--
		xliffFile.write(startFileEvent);
	}
}
