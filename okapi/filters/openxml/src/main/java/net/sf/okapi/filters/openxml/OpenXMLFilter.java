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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipException;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import com.ctc.wstx.api.WstxInputProperties;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileUtil;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.StreamUtil;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.encoder.QuoteMode;
import net.sf.okapi.common.encoder.XMLEncoder;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import org.codehaus.stax2.XMLInputFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Filters Microsoft Office Word, Excel, and Powerpoint Documents.
 * OpenXML is the format of these documents.
 *
 * <p>Since OpenXML files are Zip files that contain XML documents,
 * this filter handles opening and processing them.
 */
@UsingParameters(ConditionalParameters.class)
public class OpenXMLFilter implements IFilter {

	private final Logger LOGGER = LoggerFactory.getLogger(getClass());

	/**
	 * An error message for empty subfilter configurations.
	 */
	private static final String SUBFILTER_CONFIGURATION_HAS_NOT_BEEN_PROVIDED = "Subfilter configuration has not been provided.";

	private enum NextAction {
		OPEN_DOCUMENT, NEXT_IN_DOCUMENT, NEXT_IN_SUB_DOCUMENT, DONE
	}

	final static String MIME_TYPE = MimeTypeMapper.XML_MIME_TYPE;
	final static String FILTER_ID = "okf_openxml";
	final static Charset ENCODING = StandardCharsets.UTF_8;
	final static String LINE_BREAK = "\n";

	private static final String START_DOCUMENT_ID = "sd";
	private static final String END_DOCUMENT_ID = "ed";

	private Document.General document;
	private Part subDocument;
	private File tempFile;
	private NextAction nextAction;
	private URI documentUri;
	private LocaleId srcLang;
	private ConditionalParameters cparams;
	private ParseType nFileType;
	private AbstractTranslator translator;
	private LocaleId sOutputLanguage;
	private String encoding;
	private EncoderManager encoderManager;
	private IFilterConfigurationMapper filterConfigurationMapper;
	private IFilter subfilter;
	private XMLInputFactory inputFactory;
	private XMLOutputFactory outputFactory;
	private XMLEventFactory eventFactory;
	private RawDocument rawDocument;

	public OpenXMLFilter () {
		this(null, LocaleId.US_ENGLISH);
	}

	/**
	 * Creating the class with these two parameters allows automatic
	 * manipulation of text within TextUnits.  A copy of a source
	 * TextFragment is the parameter to the translator, and it
	 * can change the text.  The new text fragment is added to the
	 * TextUnit in the specified output language.
	 * @param translator the class that translates the text of a text fragment
	 * @param sOutputLanguage the locale of the output language, in the form en-US
	 */
	public OpenXMLFilter(AbstractTranslator translator, LocaleId sOutputLanguage) {
		this.translator = translator;
		this.sOutputLanguage = sOutputLanguage;
        this.nFileType = ParseType.MSWORD;
		this.encoding = ENCODING.name();
		this.cparams = new ConditionalParameters();
		this.inputFactory = XMLInputFactory.newInstance();
		setPropertyIfSupported(inputFactory, WstxInputProperties.P_MAX_ATTRIBUTE_SIZE, cparams.getMaxAttributeSize());
		configureInputFactory(inputFactory);
		this.outputFactory = XMLOutputFactory.newInstance();
		this.eventFactory = XMLEventFactory.newInstance();
	}

	static void configureInputFactory(XMLInputFactory inputFactory) {
		// security concern. Turn off DTD processing
		// https://www.owasp.org/index.php/XML_External_Entity_%28XXE%29_Processing
		inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

		// disable location preversation for lower memory usage
		setPropertyIfSupported(inputFactory, XMLInputFactory2.P_PRESERVE_LOCATION, false);
		setPropertyIfSupported(inputFactory, XMLInputFactory2.P_INTERN_NAMES, true);
		setPropertyIfSupported(inputFactory, XMLInputFactory2.P_INTERN_NS_URIS, true);
	}

	static void setPropertyIfSupported(XMLInputFactory inputFactory, String propertyName, Object propertyValue) {
		if (inputFactory.isPropertySupported(propertyName)) {
			inputFactory.setProperty(propertyName, propertyValue);
		}
	}

	/**
	 * Closes the input zip file and completes the filter.
	 */
	public void close () {
		if (rawDocument != null) {
			rawDocument.close();
		}
		if (tempFile != null) {
			tempFile.delete();
		}

		try {
			nextAction = NextAction.DONE;
			if ( document != null ) {
				document.close();
				document = null;
			}
		}
		catch (IOException e) {
			throw new OkapiIOException("Error closing zipped output file.");
		}
	}

	/**
	 * Creates the skeleton writer for use with this filter.
	 * Null return means implies GenericSkeletonWriter.
	 * @return the skeleton writer
	 */
	public ISkeletonWriter createSkeletonWriter () {
		return null; // There is no corresponding skeleton writer
	}

	/**
	 * Creates the filter writer for use with this filter.
	 * @return the filter writer
	 */
	public IFilterWriter createFilterWriter () {
		return new OpenXMLFilterWriter(cparams, inputFactory, outputFactory, eventFactory);
	}

	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
			encoderManager.setMapping(MIME_TYPE, "net.sf.okapi.common.encoder.XMLEncoder");
			encoderManager.setDefaultOptions(
				new XMLEncoder.Parameters(
					true,
					true,
					false,
					QuoteMode.UNESCAPED
				),
				OpenXMLFilter.ENCODING.name(),
				OpenXMLFilter.LINE_BREAK
			);
			encoderManager.updateEncoder(MIME_TYPE);
		}
		return encoderManager;
	}

	public String getName () {
		return FILTER_ID;
	}

	public String getDisplayName () {
		return "OpenXML Filter";
	}

	public String getMimeType () {
		return MIME_TYPE;
	}

	public List<FilterConfiguration> getConfigurations () {
		List<FilterConfiguration> list = new ArrayList<>();
		list.add(new FilterConfiguration(getName(),
				MIME_TYPE,
			getClass().getName(),
			"Microsoft Office Document",
			"Microsoft Office documents (DOCX, DOCM, DOTX, DOTM, PPTX, PPTM, PPSX, PPSM, POTX, POTM, XLSX, XLSM, XLTX, XLTM, VSDX, VSDM).",
			null,
			".docx;.docm;.dotx;.dotm;.pptx;.pptm;.ppsx;.ppsm;.potx;.potm;.xlsx;.xlsm;.xltx;.xltm;.vsdx;.vsdm;"));
		return list;
	}

	/**
	 * Returns the current IParameters object.
	 * @return the current IParameters object
	 */
	public ConditionalParameters getParameters () {
		return cparams;
	}

	/**
	 * Returns true if the filter has a next event.
	 * @return whether or not the filter has a next event
	 */
	public boolean hasNext () {
		return nextAction != NextAction.DONE;
	}

	/**
	 * Returns the next zip filter event.
	 * @return the next zip filter event
	 */
	public Event next () {
		try {
			Event e;
			// When the queue is empty: process next action
			switch ( nextAction ) {
			case OPEN_DOCUMENT:
				return openDocument();
			case NEXT_IN_DOCUMENT:
				return nextInDocument();
			case NEXT_IN_SUB_DOCUMENT:
				e = nextInSubDocument();
				if (e != null) {
					return e;
				}
				// That subdoc is done; call another.  XXX This is hacky
				// since it's a special case for handling NonTranslatablePart.
				nextAction = NextAction.NEXT_IN_DOCUMENT;
				return next();
			default:
				throw new OkapiException("Invalid next() call.");
			}
		}
		catch (IOException | XMLStreamException e) {
			throw new OkapiException("An error occurred during extraction", e);
		}
	}

	/**
	 * Opens a RawDocument for filtering, defaulting to generating the skeleton
	 * @param input a Raw Document to open and filter
	 */
	public void open (RawDocument input) {
		open(input, true);
	}

	/**
	 * Opens a RawDocument for filtering
	 * @param rawDocument a Raw Document to open and filter
	 * @param generateSkeleton true if a skeleton should be generated
	 */
	public void open (RawDocument rawDocument,
		boolean generateSkeleton)
	{
		if (rawDocument==null)
			throw new OkapiException("RawDocument is null");

		createSubfilter();

		// save reference for cleanup in close
		this.rawDocument = rawDocument;

		setOptions(rawDocument.getSourceLocale(), rawDocument.getTargetLocale(),
			rawDocument.getEncoding(), generateSkeleton);
		if ( rawDocument.getInputCharSequence() != null ) {
			open(rawDocument.getInputCharSequence());
		}
		else if ( rawDocument.getInputURI() != null ) {
			open(rawDocument.getInputURI());
			LOGGER.debug("\nOpening {}", rawDocument.getInputURI().toString());
		}
		else if ( rawDocument.getStream() != null ) {
			open(rawDocument.getStream());
		}
		else {
			throw new OkapiException("InputResource has no input defined.");
		}
	}

	private void createSubfilter() {
		final String subfilterConfiguration = cparams.getSubfilter();
		if (ConditionalParameters.EMPTY_SUBFILTER_CONFIGURATION.equals(subfilterConfiguration)) {
			return;
		}
		if (null == filterConfigurationMapper
				|| null == filterConfigurationMapper.getConfiguration(subfilterConfiguration)) {
			throw new IllegalStateException(SUBFILTER_CONFIGURATION_HAS_NOT_BEEN_PROVIDED);
		}
		subfilter = filterConfigurationMapper.createFilter(subfilterConfiguration);
	}

	/**
	 * Opens an input stream for filtering
	 * @param input an input stream to open and filter
	 */
	public void open (InputStream input) {
//		// Not supported for this filter
//		throw new UnsupportedOperationException(
//			"Method is not supported for this filter.");\

		// Create a temp file for the stream content
		tempFile = FileUtil.createTempFile("~okapi-23_OpenXMLFilter_");
    	StreamUtil.copy(input, tempFile);
    	open(Util.toURI(tempFile.getAbsolutePath()));
	}

	/**
	 * Opens a character sequence for filtering
	 * @param inputText character sequence to open and filter
	 */
	private void open (CharSequence inputText) {
		// Not supported for this filter
		throw new UnsupportedOperationException(
			"Method is not supported for this filter.");
	}

	/**
	 * Opens a URI for filtering
	 * @param inputURI cURI to open and filter
	 */
	public void open (URI inputURI) {
		documentUri = inputURI;
		nextAction = NextAction.OPEN_DOCUMENT;
		LOGGER.debug("\nOpening {}", inputURI.toString());
	}

	/**
	 * Sets language, encoding, and generation options for the filter.
	 * @param sourceLanguage source language in en-US format
	 * @param defaultEncoding encoding, such as "UTF-8"
	 * @param generateSkeleton true if skeleton should be generated
	 */
	public void setOptions (LocaleId sourceLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		setOptions(sourceLanguage, null, defaultEncoding, generateSkeleton);
	}

	/**
	 * Sets language, encoding, and generation options for the filter.
	 * @param sourceLanguage source language in en-US format
	 * @param targetLanguage target language in de-DE format
	 * @param defaultEncoding encoding, such as "UTF-8"
	 * @param generateSkeleton true if skeleton should be generated
	 */
	public void setOptions (LocaleId sourceLanguage,
		LocaleId targetLanguage,
		String defaultEncoding,
		boolean generateSkeleton)
	{
		srcLang = sourceLanguage;
		encoding = defaultEncoding; // issue 104
	}

	@Override
	public void setFilterConfigurationMapper (IFilterConfigurationMapper filterConfigurationMapper) {
		this.filterConfigurationMapper = filterConfigurationMapper;
	}

	@Override
	public void setParameters (IParameters params) {
		this.cparams = (ConditionalParameters)params;
	}

	/**
	 * Opens the document at the URI specified in the call to open(..),
	 * looks through the names of the XML files inside to determine
	 * the type, and creates a StartDocument Event.
	 */
	private Event openDocument() {
		try
		{
			document = new Document.General(
				cparams,
				inputFactory,
				outputFactory,
				eventFactory,
				START_DOCUMENT_ID,
				this.documentUri,
				this.srcLang,
				this.encoding,
				getEncoderManager(),
				this.subfilter,
				createFilterWriter()
			);
			nextAction = NextAction.NEXT_IN_DOCUMENT;

			return document.open();
		}
		catch ( ZipException e )
		{
			throw new OkapiIOException("Error opening zipped input file.", e);
		}
		catch ( IOException e )
		{
			throw new OkapiIOException("Error reading zipped input file.", e);
		}
		catch (XMLStreamException e)
		{
			throw new OkapiIOException("Error parsing XML content", e);
		}
	}

	private Event nextInDocument() throws IOException, XMLStreamException {
		while (document.hasNextPart()) {
			subDocument = document.nextPart();
			nextAction = NextAction.NEXT_IN_SUB_DOCUMENT;

			return subDocument.open();
		}

		close();
		Ending ending = new Ending(END_DOCUMENT_ID);

		return new Event(EventType.END_DOCUMENT, ending);
	}

	/**
	 * Returns the next subdocument event.  If it is a TEXT_UNIT event,
	 * it invokes the translator to manipulate the text before sending
	 * on the event.  If it is an END_SUBDOCUMENT event, it closes the
	 * current subdocument.
	 * @return a subdocument event
	 */
	private Event nextInSubDocument() {
		Event event;
		while (subDocument.hasNextEvent()) {
			event = subDocument.nextEvent();
			switch (event.getEventType()) {
				case TEXT_UNIT:
					if (translator!=null)
					{
						translator.addToReferents(event);
						ITextUnit tu = event.getTextUnit();
						// We can use getFirstPartContent() because nothing is segmented yet
						TextFragment tfSource = tu.getSource().getFirstContent();
						String torg = translator.translate(tfSource,LOGGER,nFileType); // DWH 5-7-09 nFileType
						TextFragment tfTarget = tfSource.clone();
						tfTarget.setCodedText(torg);
						TextContainer tc = new TextContainer();
						tc.setContent(tfTarget);
						tu.setTarget(sOutputLanguage, tc);
						if (this.document.hasPostponedTranslatables()) {
							this.document.updatePostponedTranslatables(tfSource.getCodedText(), torg);
						}
						tfSource = null;
					}
					subDocument.logEvent(event);
					return event;
				case END_SUBDOCUMENT:
					nextAction = NextAction.NEXT_IN_DOCUMENT;
					subDocument.close();
					return event;
				case DOCUMENT_PART:
				case START_GROUP:
				case START_SUBFILTER:
						if (translator!=null)
							translator.addToReferents(event);
						// purposely falls through to default
				default: // Else: just pass the event through
					subDocument.logEvent(event);
					return event;
			}
		}
		// We can fall through to here if a part handler runs out of events.
		return null;
	}

	public void cancel() {
		// TODO Auto-generated method stub
	}
}
