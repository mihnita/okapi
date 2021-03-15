/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

import net.sf.okapi.common.DefaultLocalePair;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiFileNotFoundException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiNotImplementedException;
import net.sf.okapi.common.filters.fontmappings.FontMappings;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.common.skeleton.ZipSkeleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static net.sf.okapi.filters.openxml.Namespaces.DrawingML;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DRAWING_ALIGNMENT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DRAWING_ALIGNMENT_LEFT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.DRAWING_ALIGNMENT_RIGHT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_BIDIRECTIONAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_BIDI_VISUAL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_PROPERTY_LANGUAGE;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_READING_ORDER;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_RIGHT_TO_LEFT;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_RTL;
import static net.sf.okapi.filters.openxml.XMLEventHelpers.LOCAL_RTL_COL;

/**
 * <p>Implements the IFilterWriter interface for the OpenXMLFilter, which
 * filters Microsoft Office Word, Excel, and Powerpoint Documents. OpenXML 
 * is the format of these documents.
 * 
 * <p>Since OpenXML files are Zip files that contain XML documents,
 * this filter writer handles writing out the zip file, and
 * uses OpenXMLContentSkeletonWriter to output the XML documents.
 * 
 */

public class OpenXMLFilterWriter implements IFilterWriter {
	private static final String TRUE_VALUES_ARE_EMPTY = "True values are empty";
	private static final String FALSE_VALUES_ARE_EMPTY = "False values are empty";

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	private ConditionalParameters cparams;
	private final XMLInputFactory inputFactory;
	private final XMLOutputFactory outputFactory;
	private final XMLEventFactory eventFactory;

	private String outputPath;
	private Document.General document;
	private ZipOutputStream zipOut;
	private byte[] buffer;
	private LocaleId sourceLocale;
	private LocaleId targetLocale;
	private FontMappings fontMappings;
	private File tempFile;
	private File tempZip;

	private EncoderManager encoderManager;
	private ZipEntry subDocEntry;
	private IFilterWriter subDocWriter;
	private ISkeletonWriter subSkelWriter;
	private TreeMap<Integer, SubDocumentValues> tmSubDoc = new TreeMap<>();
	private int ndxSubDoc = 0;
	private OutputStream outputStream;

	/**
	 * No-arg constructor in case it's needed.  Create local factory instances.
	 */
	public OpenXMLFilterWriter() {
		this.inputFactory = XMLInputFactory.newInstance();
		this.outputFactory = XMLOutputFactory.newInstance();
		this.eventFactory = XMLEventFactory.newInstance();

		OpenXMLFilter.configureInputFactory(inputFactory);
	}

	OpenXMLFilterWriter(ConditionalParameters cparams, XMLInputFactory inputFactory,
						XMLOutputFactory outputFactory, XMLEventFactory eventFactory) {
		this.cparams = cparams;

		this.inputFactory = inputFactory;
		// security concern. Turn off DTD processing
		// https://www.owasp.org/index.php/XML_External_Entity_%28XXE%29_Processing
		this.inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		this.outputFactory = outputFactory;
		this.eventFactory = eventFactory;
	}

	/**
	 * Cancels processing of a filter; yet to be implemented.
	 */
	public void cancel () {
		//TODO: implement cancel()
	}
	
	/**
	 * Closes the zip file.
	 */
	public void close () {
		if ( zipOut == null ) return;
		IOException err = null;
		InputStream orig = null;
		OutputStream dest = null;
		try {
			// Closing reference to the original input stream 
			if (document != null){
				document.close();
				document = null;
			}
			
			// Close the output
			zipOut.close();
			zipOut = null;

			// If it was in a temporary file, copy it over the existing one
			// If the IFilter.close() is called before IFilterWriter.close()
			// this should allow to overwrite the input.
			if ( tempZip != null ) {
				dest = new FileOutputStream(outputPath);
				orig = new FileInputStream(tempZip); 
				int len;
				while ( (len = orig.read(buffer)) > 0 ) {
					dest.write(buffer, 0, len);
				}
			}
			buffer = null;
		}
		catch ( IOException e ) {
			err = e;
		}
		finally {
			// Make sure we close both files
			if ( dest != null ) {
				try {
					dest.close();
				}
				catch ( IOException e ) {
					err = e;
				}
				dest = null;
			}
			if ( orig != null ) {
				try {
					orig.close();
				} catch ( IOException e ) {
					err = e;
				}
				orig = null;
				if ( err != null ) {
					throw new OkapiIOException("Error closing MS Office 2007 file.");
				} else {
					if ( tempZip != null ) {
						tempZip.delete();
						tempZip = null;
					}
				}
			}
		}
	}

	/**
	 * Gets the name of the filter writer.
	 */
	public String getName () {
		return "OpenXMLZipFilterWriter"; 
	}

	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
			encoderManager.setMapping(OpenXMLFilter.MIME_TYPE, "net.sf.okapi.common.encoder.XMLEncoder");
			encoderManager.setDefaultOptions(null, OpenXMLFilter.ENCODING.name(), OpenXMLFilter.LINE_BREAK);
		}
		return encoderManager;
	}
	
	@Override
	public ISkeletonWriter getSkeletonWriter () {
		return subSkelWriter;
	}

	/**
	 * Handles an event.  Passes all but START_DOCUMENT, END_DOCUMENT,
               * and DOCUMENT_PART to subdocument processing.
	 * @param event the event to process
	 */
	@Override
	public Event handleEvent (Event event) {
		switch ( event.getEventType() ) {
		case START_DOCUMENT:
			processStartDocument((StartDocument)event.getResource());
			break;
		case DOCUMENT_PART:
			processDocumentPart(event);
			break;
		case END_DOCUMENT:
			processEndDocument();
			break;
		case START_SUBDOCUMENT:
			processStartSubDocument((StartSubDocument)event.getResource());
			break;
		case END_SUBDOCUMENT:
			processEndSubDocument((Ending)event.getResource());
			break;
		case TEXT_UNIT:
		case START_GROUP:
		case END_GROUP:
		case START_SUBFILTER:
		case END_SUBFILTER:
			try {
				subDocWriter.handleEvent(event);
			} catch(Throwable e) {
				String mess = e.getMessage();
				throw new OkapiNotImplementedException(mess, e); // kludge
			}
			break;
		case CANCELED:
			break;
		}
		return event;
	}

	public void setOptions (LocaleId language,
		String defaultEncoding)
	{
		targetLocale = language;
	}

	public void setOutput (String path) {
		outputPath = path;
	}

	public void setOutput (OutputStream output) {
		this.outputStream = output;
	}

	/**
	 * Processes the start document for the whole zip file by
               * initializing a temporary output file, and and output stream.
	 * @param res a resource for the start document
	 */

	private void processStartDocument (StartDocument res) {
		try {
			buffer = new byte[2048];
			sourceLocale = res.getLocale();
			this.fontMappings = this.cparams.fontMappings().applicableTo(
				new DefaultLocalePair(this.sourceLocale, this.targetLocale)
			);
			ZipSkeleton skel = (ZipSkeleton)res.getSkeleton();
			ZipFile zipTemp = skel.getOriginal(); // if OpenXML filter was closed, this ZipFile has been marked for close
			File fZip = new File(zipTemp.getName()); // so get its name
			document = new Document.General(
					cparams,
					inputFactory,
					outputFactory,
					eventFactory,
					res.getFilterId(),
					fZip.toURI(),
					sourceLocale,
					res.getEncoding(),
					null,
					null,
					null
			);
			document.open();
              // *** this might not work if the ZipFile was from a URI that was not a normal file path ***
			tempZip = null;
			// Create the output stream from the path provided
			boolean useTemp = false;
			File f;
			OutputStream os;
			if (outputStream == null) {							
				f = new File(outputPath);
				if ( f.exists() ) {
					// If the file exists, try to remove
					useTemp = !f.delete();				
				}
				if (useTemp) {
					// Use a temporary output if we can overwrite for now
					// If it's the input file, IFilter.close() will free it before we
					// call close() here (that is if IFilter.close() is called correctly!)
					tempZip = File.createTempFile("~okapi-24_zfwTmpZip_", null);
					os = new FileOutputStream(tempZip.getAbsolutePath());
				} else {
					Util.createDirectories(outputPath);
					os = new FileOutputStream(outputPath);
				}
			} else {
				os = outputStream;
			}
			
			// create zip output
			zipOut = new ZipOutputStream(os);		
		}
		catch ( FileNotFoundException e ) {
			throw new OkapiFileNotFoundException("Existing file could not be overwritten.", e);
		}
		catch ( IOException | XMLStreamException e) {
			throw new OkapiIOException("File could not be written.", e);
		}
	}
	
	private void processEndDocument () {
		close();
	}
	
	/**
	 * This passes a file that doesn't need processing from the input zip file to the output zip file.
	 *
	 * @param event corresponding to the file to be passed through
	 */
	private void processDocumentPart (Event event) {
		DocumentPart documentPart = (DocumentPart) event.getResource();

		if ( documentPart.getSkeleton() instanceof ZipSkeleton ) {
			ZipSkeleton skeleton = (ZipSkeleton) documentPart.getSkeleton();

			if (skeleton instanceof MarkupZipSkeleton) {
				clarifyMarkup(((MarkupZipSkeleton) skeleton).getMarkup());
			}

			// Copy the entry data
			try {
				zipOut.putNextEntry(new ZipEntry(skeleton.getEntry().getName()));

				// If the contents were modified by the filter, write out the new data
				String modifiedContents = skeleton.getModifiedContents();

				if (modifiedContents != null) {
					zipOut.write(modifiedContents.getBytes(StandardCharsets.UTF_8));
				}
				else {
					InputStream input = document.inputStreamFor(skeleton.getEntry());
					int len;
					while ( (len = input.read(buffer)) > 0 ) {
						zipOut.write(buffer, 0, len);
					}
					input.close();
				}
				zipOut.closeEntry();
			}
			catch ( IOException e ) {
				throw new OkapiIOException("Error writing zip file entry.");
			}
		}
		else { // Otherwise it's a normal skeleton event
			subDocWriter.handleEvent(event);
		}
	}

	private void clarifyMarkup(Markup markup) {
		markup.apply(this.fontMappings);
		Nameable nameableMarkupComponent = markup.nameableComponent();

		if (null != nameableMarkupComponent) {
			final ClarificationContext clarificationContext = new ClarificationContext(
				this.cparams,
				new CreationalParameters(
					this.eventFactory,
					nameableMarkupComponent.getName().getPrefix(),
					nameableMarkupComponent.getName().getNamespaceURI()
				),
				this.sourceLocale,
				this.targetLocale
			);
			final String propertyDefaultValue = XMLEventHelpers.booleanAttributeTrueValues().stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(TRUE_VALUES_ARE_EMPTY));
			final String propertyDefaultValueWhenAbsent = XMLEventHelpers.booleanAttributeFalseValues().stream()
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(FALSE_VALUES_ARE_EMPTY));
			final ClarifiableAttribute rtlColClarifiableAttribute = new ClarifiableAttribute(
				Namespace.PREFIX_EMPTY,
				LOCAL_RTL_COL,
				XMLEventHelpers.booleanAttributeTrueValues()
			);
			final AttributesClarification bypassAttributesClarification = new AttributesClarification.Bypass();
			final ElementsClarification bypassElementsClarification = new ElementsClarification.Bypass();
			final AttributesClarification rtlAttributesClarification = new AttributesClarification.Default(
				clarificationContext,
				new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_RTL, XMLEventHelpers.booleanAttributeTrueValues())
			);
			final AttributesClarification tablePropertiesAttributesClarification;
			final ElementsClarification tablePropertiesElementsClarification;
			final AttributesClarification textBodyPropertiesAttributesClarification;
			final AttributesClarification paragraphPropertiesAttributesClarification;
			final ElementsClarification paragraphPropertiesElementsClarification;
			if (Namespace.PREFIX_A.equals(clarificationContext.creationalParameters().getPrefix())) {
				tablePropertiesAttributesClarification = rtlAttributesClarification;
				tablePropertiesElementsClarification = bypassElementsClarification;
				textBodyPropertiesAttributesClarification = new AttributesClarification.Default(
					clarificationContext,
					rtlColClarifiableAttribute
				);
				paragraphPropertiesAttributesClarification = new AttributesClarification.AlignmentAndRtl(
					clarificationContext,
					Namespace.PREFIX_EMPTY,
					DRAWING_ALIGNMENT,
					DRAWING_ALIGNMENT_LEFT,
					DRAWING_ALIGNMENT_RIGHT,
					LOCAL_RTL,
					XMLEventHelpers.booleanAttributeFalseValues(),
					XMLEventHelpers.booleanAttributeTrueValues()
				);
				paragraphPropertiesElementsClarification = bypassElementsClarification;
			} else {
				tablePropertiesAttributesClarification = bypassAttributesClarification;
				tablePropertiesElementsClarification = new ElementsClarification.TableBlockPropertyDefault(
					clarificationContext,
					LOCAL_BIDI_VISUAL
				);
				textBodyPropertiesAttributesClarification = new AttributesClarification.Default(
					new ClarificationContext(
						this.cparams,
						new CreationalParameters(
							this.eventFactory,
							Namespace.PREFIX_A,
							DrawingML.getURI() // todo #859: should be dynamically obtained
						),
						this.sourceLocale,
						this.targetLocale
					),
					rtlColClarifiableAttribute
				);
				paragraphPropertiesAttributesClarification = bypassAttributesClarification;
				paragraphPropertiesElementsClarification = new ElementsClarification.ParagraphBlockPropertyDefault(
					clarificationContext,
					LOCAL_BIDIRECTIONAL,
					propertyDefaultValue,
					propertyDefaultValueWhenAbsent,
					XMLEventHelpers.booleanAttributeFalseValues(),
					XMLEventHelpers.booleanAttributeTrueValues()
				);
			}
			final BlockPropertiesClarification tablePropertiesClarification =
				new BlockPropertiesClarification.Default(
					clarificationContext,
					BlockProperties.TBL_PR,
					new MarkupComponentClarification.Default(
						tablePropertiesAttributesClarification,
						tablePropertiesElementsClarification
					)
				);
			final BlockPropertiesClarification textBodyPropertiesClarification =
				new BlockPropertiesClarification.Default(
					clarificationContext,
					BlockProperties.BODY_PR,
					new MarkupComponentClarification.Default(
						textBodyPropertiesAttributesClarification,
						bypassElementsClarification
					)
				);
			final BlockPropertiesClarification paragraphPropertiesClarification =
				new BlockPropertiesClarification.Paragraph(
					new BlockPropertiesClarification.Default(
						clarificationContext,
						ParagraphBlockProperties.PPR,
						new MarkupComponentClarification.Default(
							paragraphPropertiesAttributesClarification,
							paragraphPropertiesElementsClarification
						)
					)
				);
            final MarkupClarification markupClarification = new MarkupClarification(
            	new MarkupComponentClarification.Default(
            		new AttributesClarification.Default(
						clarificationContext,
						new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_RIGHT_TO_LEFT, XMLEventHelpers.booleanAttributeTrueValues())
					),
					bypassElementsClarification
				),
				new MarkupComponentClarification.Default(
					new AttributesClarification.Default(
						clarificationContext,
						new ClarifiableAttribute(Namespace.PREFIX_EMPTY, LOCAL_READING_ORDER, Collections.singleton(XMLEventHelpers.LOCAL_READING_ORDER_RTL_VALUE))
					),
					bypassElementsClarification
				),
				new MarkupComponentClarification.Default(
					rtlAttributesClarification,
					bypassElementsClarification
				),
				tablePropertiesClarification,
				textBodyPropertiesClarification,
				paragraphPropertiesClarification,
				new StylesClarification.Word(
					tablePropertiesClarification,
					paragraphPropertiesClarification,
					new RunPropertiesClarification.Default(
						clarificationContext,
						new MarkupComponentClarification.Default(
							bypassAttributesClarification,
							new ElementsClarification.RunPropertyLang(
								new ElementsClarification.RunPropertyDefault(
									clarificationContext,
									LOCAL_RTL,
									propertyDefaultValue,
									propertyDefaultValueWhenAbsent,
									XMLEventHelpers.booleanAttributeFalseValues(),
									XMLEventHelpers.booleanAttributeTrueValues()
								),
								LOCAL_PROPERTY_LANGUAGE,
								LOCAL_BIDIRECTIONAL
							)
						)
					)
				)
			);
            markupClarification.performFor(markup);
        }
	}

	/**
	 * Starts processing a new file withing the zip file.  It looks for the 
               * element type of "filetype" in the yaml parameters which need to
               * be set before handleEvent is called, and need to be the same as
               * the parameters on the START_SUBDOCUMENT event from the
               * OpenXMLFilter (by calling setParameters).  Once the type of the
               * file is discovered from the Parameters, a subdoc writer is 
               * created from OpenXMLContentSkeletonWriter, and a temporary
               * output file is created.
	 * @param res resource of the StartSubDocument
	 */
	private void processStartSubDocument (StartSubDocument res) {
		ndxSubDoc++; // DWH 1-10-2013 subDoc map

		// Set the temporary path and create it
		try {
			tempFile = File.createTempFile("~okapi-25_zfwTmp"+ndxSubDoc+"_", null);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error opening temporary zip output file.");
		}

		ISkeleton skel = res.getSkeleton();
		ConditionalParameters conditionalParameters = (ConditionalParameters) res.getFilterParameters();
		if (skel instanceof ZipSkeleton) {
			subDocEntry = ((ZipSkeleton) res.getSkeleton()).getEntry();
			if (document.isStyledTextPart(subDocEntry)) {
				subSkelWriter = new StyledTextSkeletonWriter(
					subDocEntry.getName(),
					this.sourceLocale,
					this.targetLocale,
					conditionalParameters,
					eventFactory,
					this.fontMappings,
					innerGenericSkeletonWriter()
				);
			} else {
				subSkelWriter = new GenericSkeletonWriter();
			}
		} else {
			subDocEntry = new ZipEntry(res.getName());
			subSkelWriter = new GenericSkeletonWriter();
		}

		subDocWriter = new GenericFilterWriter(subSkelWriter, getEncoderManager()); // YS 12-20-09
		subDocWriter.setOptions(targetLocale, OpenXMLFilter.ENCODING.name());
		subDocWriter.setOutput(tempFile.getAbsolutePath());
		
		StartDocument sd = new StartDocument("sd");
		sd.setLineBreak(OpenXMLFilter.LINE_BREAK);
		sd.setSkeleton(res.getSkeleton());
		sd.setLocale(sourceLocale);
		subDocWriter.handleEvent(new Event(EventType.START_DOCUMENT, sd));
		SubDocumentValues subDocumentValues = new SubDocumentValues(
			subDocEntry,
			subDocWriter,
			subSkelWriter,
			tempFile
		);
		tmSubDoc.put(ndxSubDoc, subDocumentValues);
	}

	private GenericSkeletonWriter innerGenericSkeletonWriter() {
		return new GenericSkeletonWriter(
				sourceLocale,
				targetLocale,
				null,
				getEncoderManager(),
				false,
				false,
				new LinkedHashMap<>(),
				new Stack<>(),
				OpenXMLFilter.ENCODING.name(),
				0,
				null
		);
	}

	/**
	 * Finishes writing the subdocument temporary file, then adds it as an
               * entry in the temporary zip output file.
	 * @param res resource of the end subdocument
	 */
	private void processEndSubDocument (Ending res) {
		try {
			SubDocumentValues subDocumentValues = tmSubDoc.get(ndxSubDoc--);
			subDocEntry = subDocumentValues.zipEntry;
			subDocWriter = subDocumentValues.filterWriter;
			subSkelWriter = subDocumentValues.skeletonWriter;
			tempFile = subDocumentValues.tempFile;
			// Finish writing the sub-document
			subDocWriter.handleEvent(new Event(EventType.END_DOCUMENT, res));
			subDocWriter.close();

			// Create the new entry from the temporary output file
			zipOut.putNextEntry(new ZipEntry(subDocEntry.getName()));
			InputStream input = new FileInputStream(tempFile); 
			int len;
			while ( (len = input.read(buffer)) > 0 ) {
				zipOut.write(buffer, 0, len);
			}
			input.close();
			zipOut.closeEntry();
			// Delete the temporary file
			tempFile.delete();
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error closing zip output file.");
		}
	}
	public void setParameters(IParameters params) // DWH 7-16-09
	{
		this.cparams = (ConditionalParameters)params;
	}
	public ConditionalParameters getParameters() // DWH 7-16-09
	{
		return cparams;
	}

	private static class SubDocumentValues {
		private final ZipEntry zipEntry;
		private final IFilterWriter filterWriter;
		private final ISkeletonWriter skeletonWriter;
		private final File tempFile;

		SubDocumentValues(
			final ZipEntry zipEntry,
			final IFilterWriter filterWriter,
			final ISkeletonWriter skeletonWriter,
			final File tempFile
		) {
			this.zipEntry = zipEntry;
			this.filterWriter = filterWriter;
			this.skeletonWriter = skeletonWriter;
			this.tempFile = tempFile;
		}
	}
}
