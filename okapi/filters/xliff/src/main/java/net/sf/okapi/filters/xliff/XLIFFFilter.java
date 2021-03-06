/*===========================================================================
  Copyright (C) 2008-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xliff;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.stax.WstxInputFactory;
import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.ClassUtil;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Namespaces;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.XmlInputStreamReader;
import net.sf.okapi.common.annotation.AltTranslation;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.annotation.ITSLQIAnnotations;
import net.sf.okapi.common.annotation.ITSProvenanceAnnotations;
import net.sf.okapi.common.annotation.Note;
import net.sf.okapi.common.annotation.NoteAnnotation;
import net.sf.okapi.common.annotation.XLIFFContextGroup;
import net.sf.okapi.common.annotation.XLIFFPhase;
import net.sf.okapi.common.annotation.XLIFFPhaseAnnotation;
import net.sf.okapi.common.annotation.XLIFFTool;
import net.sf.okapi.common.annotation.XLIFFToolAnnotation;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiIllegalFilterOperationException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filters.SubFilter;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.filterwriter.ITSAnnotatorsRefContext;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.query.MatchType;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.INameable;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.PipelineParameters;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.filters.xliff.its.IITSDataStore;
import net.sf.okapi.filters.xliff.its.ITSDefaultDataStore;
import net.sf.okapi.filters.xliff.iws.IwsProperty;
import net.sf.okapi.filters.xliff.iws.IwsSegmentMetadataAttribute;
import net.sf.okapi.filters.xliff.iws.IwsStatusAttribute;
import net.sf.okapi.filters.xliff.iws.IwsXliffSkeletonWriter;
import org.codehaus.stax2.XMLInputFactory2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UsingParameters(Parameters.class)
public class XLIFFFilter implements IFilter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * An error message for empty subfilter configurations.
	 */
	private static final String CDATA_SUBFILTER_CONFIGURATION_HAS_NOT_BEEN_PROVIDED =
		"CDATA subfilter configuration has not been provided.";

	static final Charset ENCODING = StandardCharsets.UTF_8;

	// Used to check if a namespace starts with an "XLIFF 1.*" URI
	// instead of checking with 3 different values, for each version.
	private static final String NS_XLIFF_PREFIX = "urn:oasis:names:tc:xliff:document:1.";
	
	public static final String PROP_BUILDNUM = "build-num";
	public static final String PROP_EXTRADATA = "extradata";
	public static final String PROP_WASSEGMENTED = "wassegmented";
	public static final String PROP_REPETITION = "repetition";
	/**
	 * On StartDocument to indicate that the "flavor" of the document is possible
	 */
	public static final String PROP_XLIFF_FLAVOR = "xliffFlavor";
	
	public static final String XLIFF_FLAVOR_DEFAULT = "default"; // Default (no specific flavor detected)
	public static final String XLIFF_FLAVOR_SDLXLIFF = "sdlxliff"; // Trados SDLXLIFF document
	public static final String XLIFF_FLAVOR_XTM = "xtm"; // XLIFF exported from XTM

	private static final String ALTTRANSTYPE_PROPOSAL = "proposal";

	// some status values (aka "state") used in xliff
	public static final String FINAL = "final";
	public static final String SIGNED_OFF = "signed-off";
	public static final String NEW = "new";
	public static final String TRANSLATED = "translated";

	// some status-quality values used in xliff
	public static final String EXACT_MATCH = "exact-match";
	public static final String FUZZY_MATCH = "fuzzy-match";
	public static final String ID_MATCH = "id-match";
	public static final String LEVERAGED_MT = "leveraged-mt";
	public static final String LEVERAGED_TM = "leveraged-tm";
	public static final String MT_SUGGESTION = "mt-suggestion";
	public static final String TM_SUGGESTION = "tm-suggestionn";
	public static final String LEVERAGED_INHERITED = "leveraged-inherited";

	public static final String STATE_QUALIFIER = "state-qualifier";

	// xliff "alttranstype" now added as a MatchType
	private static final String ACCEPTED = "accepted";

	private static final String GROUP = "group";
	private static final String BIN_UNIT = "bin-unit";
	private static final String TRANS_UNIT = "trans-unit";
	private static final String NOTE = "note";
	private static final String FILE = "file";
	private static final String XLIFF = "xliff";

	public static final String CDATA_START = "<![CDATA[";
	public static final String CDATA_END = "]]>";

	private boolean hasNext;
	private XMLStreamReader xliffReader;
	private boolean xliffPreRead;
	private RawDocument input;
	private String docName;
	private int tuId;
	private IdGenerator otherId;
	private IdGenerator groupId;
	private String startDocId;
	private LocaleId srcLang;
	private LocaleId trgLang;
	private LinkedList<Event> queue;
	private boolean canceled;
	private GenericSkeleton skel;
	private GenericSkeletonPart itsLQISource, itsLQITarget, itsProvSource, itsProvTarget, itsMtConfTarget;
	private Stack<String> inITSStandoff = new Stack<>();
	private StartSubDocument startSubDoc;
	private ITextUnit tu;
	private int approved; // -1=no property, 0=no, 1=yes
	private Parameters params;
	private boolean sourceDone;
	private boolean targetDone;
	private boolean altTransDone;
	private boolean noteDone;
	private boolean segSourceDone;
	private String encoding;
	private Stack<String> parentIds;
	private List<String> groupUsedIds;
	private AltTranslationsAnnotation altTrans;
	private int altTransQuality;
	private MatchType altTransMatchType;
	private String altTransOrigin;
	private String altTransEngine;
	private String altTransExType;
	private boolean inAltTrans;
	private boolean processAltTrans;
	private Stack<Boolean> preserveSpaces;
	private String lineBreak;
	private boolean hasUTF8BOM;
	private EncoderManager encoderManager;
	private IFilterConfigurationMapper filterConfigurationMapper;
	private IFilter cdataSubfilter;
	private int cdataSubfilterIndex;
	private int autoMid;
	private XLIFFITSFilterExtension itsFilterHandler;
	private ITSAnnotatorsRefContext annotatorsRef;
	private int extraId;
	private StartDocument startDoc;
	private Map<String, SdlTagDef> sdlTagDefs;
	private String alttranstype;
	private	InputStreamReader inStreamReader = null;
	private boolean inIwsSegmentMetadata;
	private String xliffFlavor;
	private HashMap<String, Integer> sdlReps;
	private Segment lastSegForRep;

	/**
	 * Stack context for the translate state
	 */
	private Stack<Boolean> translateCtx;

	private SdlFileInfo sdlFileInfo;

	public XLIFFFilter () {
		params = new Parameters();
	}

	@Override
	public void cancel () {
		canceled = true;
	}

	@Override
	public void close () {
		try {
			if ( input != null ) {
				input.close();
				input = null;
			}

			if (inStreamReader != null) {
				try {
					inStreamReader.close();
				} catch (IOException e) {
					logger.warn("Cannot close inStreamReader. Memory leak.");
				}
			}

			if ( xliffReader != null ) {
				xliffReader.close();
				xliffReader = null;
			}
			hasNext = false;
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public String getName () {
		return "okf_xliff";
	}

	@Override
	public String getDisplayName () {
		return "XLIFF Filter";
	}

	@Override
	public String getMimeType () {
		return MimeTypeMapper.XLIFF_MIME_TYPE;
	}

	@Override
	public List<FilterConfiguration> getConfigurations () {
		List<FilterConfiguration> list = new ArrayList<>();
		list.add(new FilterConfiguration(getName(),
			MimeTypeMapper.XLIFF_MIME_TYPE,
			getClass().getName(),
			"XLIFF",
			"Configuration for XML Localisation Interchange File Format (XLIFF) documents.",
			null,
			".xlf;.xliff;.mxliff;.mqxliff;"));
		list.add(new FilterConfiguration(getName()+"-sdl",
				MimeTypeMapper.XLIFF_MIME_TYPE,
				getClass().getName(),
				"SDLXLIFF",
				"Configuration for SDL XLIFF documents. Supports SDL specific metadata",
				"sdl.fprm",
				".sdlxliff;"));
		list.add(new FilterConfiguration(getName()+"-iws",
				MimeTypeMapper.XLIFF_MIME_TYPE,
				getClass().getName(),
				"IWSXLIFF",
				"Configuration for IWS XLIFF documents. Supports IWS specific metadata",
				"iws.fprm",
				".xlf;"));
		return list;
	}

	@Override
	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
			encoderManager.setMapping(MimeTypeMapper.XLIFF_MIME_TYPE, "net.sf.okapi.common.encoder.XMLEncoder");
		}
		return encoderManager;
	}

	@Override
	public Parameters getParameters () {
		return params;
	}

	@Override
	public boolean hasNext () {
		return hasNext;
	}

	public LocaleId getCurrentSourceLocale() {
		return srcLang;
	}

	public LocaleId getCurrentTargetLocale() {
		return trgLang;
	}

	public SdlFileInfo getCurrentSdlFileInfo() {
		return sdlFileInfo;
	}

	@Override
	public Event next () {
		try {
			// Check for cancellation first
			if ( canceled ) {
				queue.clear();
				queue.add(new Event(EventType.CANCELED));
				hasNext = false;
			}

			// Parse next if nothing in the queue
			if ( queue.isEmpty() ) {
				if ( !read() ) {
					Ending ending = new Ending(otherId.createId());
					ending.setSkeleton(skel);
					queue.add(new Event(EventType.END_DOCUMENT, ending));
				}
			}

			// Return the head of the queue
			if ( queue.peek().getEventType() == EventType.END_DOCUMENT ) {
				hasNext = false;
			}
			return queue.poll();
		}
		catch ( XMLStreamException|IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public void open (RawDocument input) {
		open(input, true);
	}

	@Override
	public void open (RawDocument input,
		boolean generateSkeleton)
	{
		open(input, generateSkeleton, new ITSDefaultDataStore());
	}

	/**
	 * Opens the input document described in a give RawDocument object, optionally creates skeleton information,
	 * and set the default data store to use to harvest ITS standoff annotations.
	 * @param input the RawDocument object to use to open the document.
	 * @param generateSkeleton true to generate the skeleton data, false otherwise.
	 * @param datastore the data store to use for the ITS standoff annotations (cannot be null).
	 */
	public void open (RawDocument input,
		boolean generateSkeleton,
		IITSDataStore datastore)
	{
		try {
			createCdataSubfilter();
			canceled = false;
			this.input = input;

			XMLInputFactory fact = null;
			if ( params.getUseCustomParser() ) {
				Class<?> factClass = ClassUtil.getClass(params.getFactoryClass());
				fact = (XMLInputFactory)factClass.newInstance();
			}
			else {
				fact = XMLInputFactory.newInstance();
			}
			logger.debug("XMLInputFactory: {}", fact.getClass().getName());

			fact.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
			fact.setProperty(XMLInputFactory2.P_REPORT_CDATA, Boolean.TRUE);
			//Removed for Java 1.6: fact.setProperty(XMLInputFactory2.P_REPORT_PROLOG_WHITESPACE, true);
			if (fact instanceof WstxInputFactory) {
				// the following setting avoid splitting of long CDATA sections while still
				// reporting them as CDATA and not as CHARACTERS (as coalescing would do)
				fact.setProperty(WstxInputProperties.P_MIN_TEXT_SEGMENT, Integer.MAX_VALUE);
			}

			//fact.setXMLResolver(new DefaultXMLResolver());
			//TODO: Resolve the re-construction of the DTD, for now just skip it
			// security concern. Turn off DTD processing
			// https://www.owasp.org/index.php/XML_External_Entity_%28XXE%29_Processing
			fact.setProperty(XMLInputFactory.SUPPORT_DTD, false);

			// Determine encoding based on BOM, if any
			input.setEncoding(ENCODING.name()); // Default for XML, other should be auto-detected
			BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(input.getStream(), input.getEncoding());
			detector.detectBom();

			String inStreamCharset = ENCODING.name();
			if ( detector.isAutodetected() ) {
				inStreamCharset = detector.getEncoding();
			}
			xliffFlavor = null; // Not a specific flavor by default

			XMLStreamReader sdlxliffReader = null;
			try {
				inStreamReader = createStreamReader(input, inStreamCharset);
				// 1st parse; find SDL specific metadata. Return early if sdl namespace not found.
				if ( null != input.getInputURI() ) {
					sdlxliffReader = fact.createXMLStreamReader(input.getInputURI().toString(), inStreamReader);
				}
				else {
					sdlxliffReader = fact.createXMLStreamReader(inStreamReader);
				}
				XliffSdlFilterExtension sdlFilterHandler = new XliffSdlFilterExtension();
				sdlFileInfo = new SdlFileInfo();
				sdlTagDefs = sdlFilterHandler.parse(sdlxliffReader, params, sdlFileInfo);
			} finally {
				try {
					inStreamReader.close();
				} catch (IOException e) {
					logger.warn("Cannot close inStreamReader. Memory leak.");
				}
				sdlxliffReader.close();
			}
			if ( sdlTagDefs != null ) {
				xliffFlavor = XLIFF_FLAVOR_SDLXLIFF;
			}

			XMLEventReader eventReader = null;
			try {
				inStreamReader = createStreamReader(input, inStreamCharset);
				// 2nd parse; find ITS standoff references and store for future resolution.
				if ( null != input.getInputURI() ) {
					eventReader = fact.createXMLEventReader(input.getInputURI().toString(), inStreamReader);
				}
				else {
					eventReader = fact.createXMLEventReader(inStreamReader);
				}
				itsFilterHandler = new XLIFFITSFilterExtension(fact, datastore, input.getInputURI(), this);
				itsFilterHandler.parseInDocumentITSStandoff(eventReader, inStreamCharset);
			} finally {
				try {
					inStreamReader.close();
				} catch (IOException e) {
					logger.warn("Cannot close inStreamReader. Memory leak.");
				}
				eventReader.close();
			}

			// third parse, input stream will automatically be reset
			// inStreamReader will be closed when reader is closed
			inStreamReader = createStreamReader(input, inStreamCharset);
			// When possible, make sure we have a filename associated with the stream
			if ( null != input.getInputURI() ) {
				xliffReader = fact.createXMLStreamReader(input.getInputURI().toString(), inStreamReader);
			}
			else {
				xliffReader = fact.createXMLStreamReader(inStreamReader);
			}

			itsFilterHandler.setXLIFFReader(xliffReader);
			String realEnc = xliffReader.getCharacterEncodingScheme();
			if ( realEnc != null ) encoding = realEnc;
			else encoding = input.getEncoding();

			annotatorsRef = new ITSAnnotatorsRefContext(xliffReader);

			if (input.getSourceLocale() == null || input.getTargetLocale() == null) {
				input.close();
				try {
					// otherwise eclipse complains of resource leak
					inStreamReader.close();
				} catch (IOException e) {
				}
				xliffReader.close();
				throw new OkapiIllegalFilterOperationException("Source or Target language is null.");
			}
			srcLang = input.getSourceLocale();
			trgLang = input.getTargetLocale();
			hasUTF8BOM = detector.hasUtf8Bom();
			lineBreak = detector.getNewlineType().toString();
			if ( input.getInputURI() != null ) {
				docName = input.getInputURI().getPath();
			}

			// Now that the line break has been determined, set the encoder manager options
			getEncoderManager().setDefaultOptions(params, encoding, lineBreak);
			getEncoderManager().updateEncoder(MimeTypeMapper.XLIFF_MIME_TYPE);

			preserveSpaces = new Stack<>();
			preserveSpaces.push(params.isPreserveSpaceByDefault());
			translateCtx = new Stack<>();
			translateCtx.push(true); // Translatable by default
			parentIds = new Stack<>();
			parentIds.push("p0"); // Base parent
			tuId = 0;
			groupId = new IdGenerator(null, "g");
			otherId = new IdGenerator(null, "d");
			// Set the start event
			hasNext = true;
			queue = new LinkedList<>();
			groupUsedIds = new ArrayList<>();

			startDocId = otherId.createId();
			startDoc = new StartDocument(startDocId);
			startDoc.setName(docName);
			startDoc.setEncoding(encoding, hasUTF8BOM);
			startDoc.setLocale(srcLang);
			startDoc.setFilterId(getName());
			startDoc.setFilterParameters(getParameters());
			startDoc.setFilterWriter(createFilterWriter());
			startDoc.setType(MimeTypeMapper.XLIFF_MIME_TYPE);
			startDoc.setMimeType(MimeTypeMapper.XLIFF_MIME_TYPE);
			startDoc.setMultilingual(true);
			startDoc.setLineBreak(lineBreak);
			queue.add(new Event(EventType.START_DOCUMENT, startDoc));
			
			sdlReps = new HashMap<>();
			lastSegForRep = null;

			// The XML declaration is not reported by the parser, so we need to
			// create it as a document part when starting
			skel = new GenericSkeleton();
			startDoc.setProperty(new Property(Property.ENCODING, encoding, false));
			skel.append("<?xml version=\"1.0\" encoding=\"");
			skel.addValuePlaceholder(startDoc, Property.ENCODING, LocaleId.EMPTY);
			skel.append("\"?>"+lineBreak);
			startDoc.setSkeleton(skel);

			if (params.getUseCodeFinder()) {
				params.getCodeFinder().compile();
			}
		}
		catch ( XMLStreamException | IllegalAccessException | InstantiationException e) {
			throw new OkapiIOException("Cannot open XML document.\n"+e.getMessage(), e);
		}
	}

	private void createCdataSubfilter() {
		final String configuration = this.params.getCdataSubfilter();
		if (Parameters.EMPTY_CDATA_SUBFILTER_CONFIGURATION.equals(configuration)) {
			return;
		}
		if (null == this.filterConfigurationMapper
			|| null == this.filterConfigurationMapper.getConfiguration(configuration)) {
			throw new IllegalStateException(CDATA_SUBFILTER_CONFIGURATION_HAS_NOT_BEEN_PROVIDED);
		}
		this.cdataSubfilter = this.filterConfigurationMapper.createFilter(configuration);
	}

	@Override
	public void setFilterConfigurationMapper (IFilterConfigurationMapper filterConfigurationMapper) {
		this.filterConfigurationMapper = filterConfigurationMapper;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	public ISkeletonWriter createSkeletonWriter() {
		XLIFFSkeletonWriter writer;
		if (params.isUseSdlXliffWriter()) {
			writer = new SdlXliffSkeletonWriter(params);
		} else if (params.isUseIwsXliffWriter()) {
			writer = new IwsXliffSkeletonWriter(params);
		} else {
			writer = new XLIFFSkeletonWriter(params);
		}
		if (itsFilterHandler != null && itsFilterHandler.getITSStandoffManager() != null) {
			writer.setITSStandoffManager(itsFilterHandler.getITSStandoffManager());
		}
		return writer;
	}

	public IFilterWriter createFilterWriter () {
		return new GenericFilterWriter(createSkeletonWriter(), getEncoderManager());
	}

	public InputStreamReader createStreamReader(RawDocument input, String charset) {
		InputStreamReader inputStreamReader;
		try {
			inputStreamReader = new XmlInputStreamReader(input.getStream(), charset);
		} catch (java.io.UnsupportedEncodingException e) {
			logger.warn("Invalid encoding '{}', using default.", charset);
			inputStreamReader = new XmlInputStreamReader(input.getStream());
		}
		return inputStreamReader;
	}

	private boolean read () throws XMLStreamException, IOException {
		skel = new GenericSkeleton();
		int eventType;

		while ( xliffReader.hasNext() ) {
			eventType = this.xliffPreRead
				? xliffReader.getEventType()
				: xliffReader.next();
			this.xliffPreRead = false;
			switch ( eventType ) {
			case XMLStreamConstants.START_ELEMENT:
				String name = xliffReader.getLocalName();
				if ( TRANS_UNIT.equals(name) ) {
					return processTransUnit();
				}
				else if ( FILE.equals(name) ) {
					return processStartFile();
				}
				else if ( GROUP.equals(name) ) {
					if ( processStartGroup() ) return true;
				}
				else if ( XLIFF.equals(name) ) {
					processStartXliff();
					storeStartElement(false, false, false);
				}
				else if ( BIN_UNIT.equals(name) ) {
					if ( processStartBinUnit() ) return true;
				}
				else {
					storeStartElement(false, false, false);
				}
				break;

			case XMLStreamConstants.END_ELEMENT:
				storeEndElement();
				name = xliffReader.getLocalName();
				if ( FILE.equals(name) ) {
					return processEndFile();
				}
				else if ( GROUP.equals(name) ) {
					return processEndGroup();
				}
				else if ( BIN_UNIT.equals(name) ) {
					return processEndBinUnit();
				}
				break;

			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CDATA:
				if (inITSStandoff.isEmpty()) {
					// CDATA content should be preserved as CDATA in skeleton
					skel.append(CDATA_START);
					skel.append(xliffReader.getText().replace("\n", lineBreak));
					skel.append(CDATA_END);
				}
				break;
			case XMLStreamConstants.CHARACTERS: //TODO: escape unsupported chars
				if (inITSStandoff.isEmpty()) {
					skel.append(Util.escapeToXML(xliffReader.getText().replace("\n", lineBreak), 0, params.getEscapeGT(), null));
				}
				break;

			case XMLStreamConstants.COMMENT:
				skel.append("<!--"+ xliffReader.getText().replace("\n", lineBreak) + "-->");
				break;

			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				skel.append("<?"+ xliffReader.getPITarget() + " " + xliffReader.getPIData() + "?>");
				break;

			case XMLStreamConstants.DTD:
				//TODO: Reconstruct the DTD declaration
				// but how? nothing is available to do that
				break;

			case XMLStreamConstants.ENTITY_REFERENCE:
			case XMLStreamConstants.ENTITY_DECLARATION:
			case XMLStreamConstants.NAMESPACE:
			case XMLStreamConstants.NOTATION_DECLARATION:
			case XMLStreamConstants.ATTRIBUTE:
				break;
			case XMLStreamConstants.START_DOCUMENT:
				break;
			case XMLStreamConstants.END_DOCUMENT:
//				if (( sdlReps != null ) && !sdlReps.isEmpty() ) {
//					logger.info("Repetitions:");
//					for ( String id : sdlReps.keySet() ) {
//						int count = sdlReps.get(id);
//						logger.info("ID=" + id + "  count=" + count);
//					}
//				}
				break;
			}
		}
		return false;
	}
	
	private void processStartXliff () {
		// First chance to detect the flavor
		for ( int i=0; i<xliffReader.getNamespaceCount(); i++ ) {
			if ( XliffXtmFilterExtension.XTM_NAMESPACE_URI.equals(xliffReader.getNamespaceURI(i)) ) {
				xliffFlavor = XLIFF_FLAVOR_XTM;
				return;
			}
		}
	}

	private boolean processStartFile () {
		// Make a document part with skeleton between the previous event and now.
		// Spaces can go with the file element to reduce the number of events.
		// This allows to have only the file skeleton parts with the sub-document event
		if ( !skel.isEmpty(true) ) {
			DocumentPart dp = new DocumentPart(otherId.createId(), false, skel);
			skel = new GenericSkeleton(); // And create a new skeleton for the next event
			queue.add(new Event(EventType.DOCUMENT_PART, dp));
		}

		startSubDoc = new StartSubDocument(startDocId, otherId.createId());
		storeStartElementFile(startSubDoc);

		String tmp = getXliffAttributeValue("original");
		if ( tmp == null ) throw new OkapiIllegalFilterOperationException("Missing attribute 'original'.");
		else startSubDoc.setName(tmp);

		// Try detecting flavor if it's not done yet
		if ( xliffFlavor == null ) {
			if ( (xliffReader.getAttributeValue(
					XliffXtmFilterExtension.XTM_NAMESPACE_URI, XliffXtmFilterExtension.XTM_POPULATE_TRG_WITH_SRC) != null) ) {
				xliffFlavor = XLIFF_FLAVOR_XTM;
			}
			else {
				xliffFlavor = XLIFF_FLAVOR_DEFAULT;
			}
		}
		// Add the property
		startSubDoc.setProperty(new Property(PROP_XLIFF_FLAVOR, xliffFlavor));

		// Check the source language
		tmp = getXliffAttributeValue("source-language");
		if ( tmp == null ) throw new OkapiIllegalFilterOperationException("Missing attribute 'source-language'.");
		LocaleId srcXliffLang = LocaleId.fromString(tmp);
		if ( !LocaleId.EMPTY.equals(input.getSourceLocale()) && !srcXliffLang.equals(srcLang) ) { // Warn about source language
			logger.warn("The source language declared in <file> is '{}' not '{}'.", tmp, srcLang);
		}

		// Check the target language
		Property prop = startSubDoc.getProperty("targetLanguage");
		LocaleId trgXliffLang = LocaleId.EMPTY;
		if ( prop != null ) {
			trgXliffLang = LocaleId.fromString(prop.getValue());
			if ( params.getOverrideTargetLanguage() ) {
				prop.setValue(trgLang.toBCP47());
			} else { // If we do not override the target
				if ( !LocaleId.EMPTY.equals(input.getTargetLocale()) && !trgXliffLang.sameLanguageAs(trgLang) ) {
					// Warn about target language
					logger.warn("The target language declared in <file> is '{}' not '{}'. '{}' will be used.",
						prop.getValue(), trgLang, prop.getValue());
					trgLang = trgXliffLang;
				}
			}
		}

		// Get datatype property to use for mime-type
		tmp = getXliffAttributeValue("datatype");
		if ( tmp != null ) {
			// make sure this is in-synch with XLIFFWriter
			switch (tmp) {
				case "x-undefined":
					tmp = null;
					break;
				case "html":
					tmp = "text/html";
					break;
				case "xml":
					tmp = "text/xml";
					break;
			}
			//else if ( tmp.startsWith("x-") ) {
			//	tmp = tmp.substring(2);
			//}
			startSubDoc.setMimeType(tmp);
		}

		// Get build-num as read-only property
		tmp = getXliffAttributeValue(PROP_BUILDNUM);
		if ( tmp != null ) {
			startSubDoc.setProperty(new Property(PROP_BUILDNUM, tmp, true));
		}

		// ITS annotatorsRef
		if ( annotatorsRef.peek() != null ) {
			GenericAnnotations.addAnnotations(startSubDoc,
				new GenericAnnotations(annotatorsRef.getAnnotation())
			);
		}

		try {
			int eventType;
			Stack<String> xmlDOMPosition = new Stack<>();
			xmlDOMPosition.push(FILE);
			boolean addedToolProp = false;
			XLIFFToolAnnotation toolAnn = null;
			XLIFFTool tool = null;
			boolean addedPhaseProp = false;
			XLIFFPhaseAnnotation phaseAnn = null;
			XLIFFPhase phase = null;

			ITSProvenanceAnnotations prov = itsFilterHandler.readITSProvenance();
			ITSProvenanceAnnotations.addAnnotations(startSubDoc, prov);

			while (xliffReader.hasNext()) {
				eventType = xliffReader.next();
				switch (eventType) {
					case XMLStreamConstants.START_ELEMENT:
						String startElementName = xliffReader.getLocalName();
						if ( "tool".equals(startElementName) ) {
							removeEndingSkeletonWhitespace();
							tool = new XLIFFTool(getXliffAttributeValue("tool-id"),
								getXliffAttributeValue("tool-name"));
							tool.setVersion(getXliffAttributeValue("tool-version"));
							tool.setCompany(getXliffAttributeValue("tool-company"));
							toolAnn = (startSubDoc.getAnnotation(XLIFFToolAnnotation.class) == null) ?
								new XLIFFToolAnnotation() : startSubDoc.getAnnotation(XLIFFToolAnnotation.class);
							toolAnn.add(tool, startSubDoc);
							startSubDoc.setAnnotation(toolAnn);
						}
						else if ( "phase-group".equals(startElementName) ) {
							removeEndingSkeletonWhitespace();
						}
						else if ( "phase".equals(startElementName) ) {
							removeEndingSkeletonWhitespace();
							phase = new XLIFFPhase(getXliffAttributeValue("phase-name"),
								getXliffAttributeValue("process-name"));
							phase.setCompanyName(getXliffAttributeValue("company-name"));
							phase.setToolId(getXliffAttributeValue("tool-id"));
							phase.setJobId(getXliffAttributeValue("job-id"));
							phase.setContactName(getXliffAttributeValue("contact-name"));
							phase.setContactEmail(getXliffAttributeValue("contact-email"));
							phase.setContactPhone(getXliffAttributeValue("contact-phone"));
							phaseAnn = (startSubDoc.getAnnotation(XLIFFPhaseAnnotation.class) == null) ?
								new XLIFFPhaseAnnotation() : startSubDoc.getAnnotation(XLIFFPhaseAnnotation.class);
							phaseAnn.add(phase, startSubDoc);
							startSubDoc.setAnnotation(phaseAnn);
							if (!addedPhaseProp) {
								addPhasePropertyPlaceholder(skel, startSubDoc, phaseAnn);
								addedPhaseProp = true;
							}
						}
						else if ( !"sk1".equals(startElementName) && !"glossary".equals(startElementName)
							&& !"reference".equals(startElementName) && !"count-group".equals(startElementName)
							&& !"prop-group".equals(startElementName) && !NOTE.equals(startElementName)
							&& xmlDOMPosition.peek().equals("header"))
						{
							if (!addedToolProp) {
								addToolPropertyPlaceholder(skel, startSubDoc, toolAnn);
								addedToolProp = true;
							}
							storeStartElement(false, false, false);
						}
						else if ( "body".equals(startElementName) ) {
							storeStartElement(false, false, false);
							startSubDoc.setSkeleton(skel);
							queue.add(new Event(EventType.START_SUBDOCUMENT, startSubDoc));
							PipelineParameters pp = null;
							if (LocaleId.EMPTY.equals(input.getSourceLocale()) ||
								LocaleId.EMPTY.equals(input.getTargetLocale()))
							{
								pp = new PipelineParameters(startDoc, input, null, null);
							}

							// use the locales defined in the xliff
							// prepare the pipelineparameter
							if (LocaleId.EMPTY.equals(input.getSourceLocale())) {
								srcLang = srcXliffLang;
								pp.setSourceLocale(srcLang);
							}

							if (LocaleId.EMPTY.equals(input.getTargetLocale())) {
								trgLang = trgXliffLang;
								pp.setTargetLocale(trgLang);
							}

							// make sure the event for updated source or target happens after the startdocument event
							if (pp != null) {
								queue.add(new Event(EventType.PIPELINE_PARAMETERS, pp));
							}

							return true;
						}
						else if ( TRANS_UNIT.equals(startElementName) || GROUP.equals(startElementName) ) {
							throw new OkapiIOException("Missing <body> element.");
						}
						else {
							storeStartElement(false, false, false);
						}
						xmlDOMPosition.push(startElementName);
						break;
					case XMLStreamConstants.END_ELEMENT:
						String endElementName = xliffReader.getLocalName();
						if ( xmlDOMPosition.peek().equals("tool") ) {
							if ( !endElementName.equals("tool") ) {
								StringBuilder endElementXML = new StringBuilder();
								String prefix = xliffReader.getPrefix();
								endElementXML.append("</")
									.append((prefix != null) && (prefix.length() > 0) ? prefix + ":" : "")
									.append(endElementName).append(">");
								tool.addSkeletonContent(endElementXML.toString());
							}
						}
						else if ( xmlDOMPosition.peek().equals("phase-group") ) {
							// Do nothing
						}
						else if ( xmlDOMPosition.peek().equals("phase") ) {
							if ( !endElementName.equals("phase") ) {
								StringBuilder endElementXML = new StringBuilder();
								String prefix = xliffReader.getPrefix();
								endElementXML.append("</")
									.append((prefix != null) && (prefix.length() > 0) ? prefix + ":" : "")
									.append(endElementName).append(">");
								phase.addSkeletonContent(endElementXML.toString());
							}
						}
						else if ( endElementName.equals("header") ) {
							if ( !addedToolProp ) {
								addToolPropertyPlaceholder(skel, startSubDoc, toolAnn);
								addedToolProp = true;
							}
							storeEndElement();
						}
						else {
							storeEndElement();
						}
						String startElement = xmlDOMPosition.pop();
						if ( !startElement.equals(endElementName) ) {
							throw new OkapiIOException(
								"Mismatch in start and end element XML stream events: "
								+startElement+" != "+endElementName);
						}
						break;
					case XMLStreamConstants.SPACE:
						String cdata = xliffReader.getText().replace("\n", lineBreak);
						if ( xmlDOMPosition.peek().equals("tool") ) {
							tool.addSkeletonContent(cdata);
						}
						else if (xmlDOMPosition.peek().equals("phase")) {
							phase.addSkeletonContent(cdata);
						}
						else {
							skel.append(cdata);
						}
						break;
					case XMLStreamConstants.CDATA:
						// CDATA content in headers, etc, should be preserved as CDATA in skeleton
						skel.append(CDATA_START);
						skel.append(xliffReader.getText().replace("\n", lineBreak));
						skel.append(CDATA_END);
						break;
					case XMLStreamConstants.CHARACTERS:
						String skelChars = Util.escapeToXML(xliffReader.getText().replace("\n", lineBreak), 0, params.getEscapeGT(), null);
						if ( xmlDOMPosition.peek().equals("tool") ) {
							tool.addSkeletonContent(skelChars);
						}
						else if ( xmlDOMPosition.peek().equals("phase") ) {
							phase.addSkeletonContent(skelChars);
						}
						else {
							skel.append(skelChars);
						}
						break;
					case XMLStreamConstants.COMMENT:
						String skelComment = "<!--" + xliffReader.getText().replace("\n", lineBreak) + "-->";
						if ( xmlDOMPosition.peek().equals("tool") ) {
							tool.addSkeletonContent(skelComment);
						}
						else if ( xmlDOMPosition.peek().equals("phase") ) {
							phase.addSkeletonContent(skelComment);
						}
						else {
							skel.append(skelComment);
						}
						break;
					case XMLStreamConstants.PROCESSING_INSTRUCTION:
						String skelPI = "<?" + xliffReader.getPITarget() + " " + xliffReader.getPIData() + "?>";
						if ( xmlDOMPosition.peek().equals("tool") ) {
							tool.addSkeletonContent(skelPI);
						}
						else if ( xmlDOMPosition.peek().equals("phase") ) {
							phase.addSkeletonContent(skelPI);
						}
						else {
							skel.append(skelPI);
						}
						break;
					default:
						break;
				}
			}
		}
		catch ( Throwable e ) {
			throw new OkapiIOException(e);
		}

		return true;
	}

	/**
	 * Add a Tool Property ref marker to a skeleton.
	 * @param skeleton - Skeleton to add a Property ref marker
	 * @param startSubDoc - Object containing the Tool Property
	 * @param toolAnn - Initialize the tool property with the XML representation of this annotation
	 */
	private void addToolPropertyPlaceholder(GenericSkeleton skeleton, StartSubDocument startSubDoc, XLIFFToolAnnotation toolAnn) {
		addPropertyPlaceholder(skeleton, Property.XLIFF_TOOL, startSubDoc, toolAnn != null ? toolAnn.toXML() : "");
	}

	/**
	 * Add a Phase Property ref marker to a skeleton
	 * @param skeleton - Skeleton to add a Property ref marker
	 * @param startSubDoc - Object containing the Phase Property
	 * @param phaseAnn - Initialize the phase property with the XML representation of this annotation.
	 */
	private void addPhasePropertyPlaceholder(GenericSkeleton skeleton, StartSubDocument startSubDoc, XLIFFPhaseAnnotation phaseAnn) {
		addPropertyPlaceholder(skeleton, Property.XLIFF_PHASE, startSubDoc, phaseAnn != null ? phaseAnn.toXML() : "");
	}

	private void addPropertyPlaceholder(GenericSkeleton skeleton, String propertyName, INameable referent, String initialValue) {
		skeleton.addValuePlaceholder(referent, propertyName, LocaleId.EMPTY);
		Property property = referent.getProperty(propertyName) != null
			? referent.getProperty(propertyName)
			: new Property(propertyName, "");
		if (initialValue != null) {
			property.setValue(initialValue);
		}
		referent.setProperty(property);
	}

	private boolean processEndFile () {
		Ending ending = new Ending(otherId.createId());
		ending.setSkeleton(skel);
		queue.add(new Event(EventType.END_SUBDOCUMENT, ending));
		return true;
	}
	
	private void removeEndingSkeletonWhitespace() {
		GenericSkeletonPart part = skel.getLastPart();
		if (part != null) {
			part.setData(part.toString().replaceAll("[\\s\\n\\r]+$", ""));
		}
	}

	private void storeStartElement (boolean updateLangWithTarget,
		boolean addApprovedIfNeeded,
		boolean mtConfTarget) // True to apply the special process for the mt-confidence in target
	{
		annotatorsRef.readAndPush();
		String prefix = xliffReader.getPrefix();
		if ( !Namespaces.ITS_NS_PREFIX.equals(prefix) ) {
			storeElementName(prefix);

			String attrPrefix, attrName, attrValue;
			boolean ps = preserveSpaces.peek();
			String itsLQIRef = "", itsProvRef = "";
			boolean needToCheckforITSDecl = true;

			int count = xliffReader.getAttributeCount();
			for (int i = 0; i < count; i++) {
				if (!xliffReader.isAttributeSpecified(i)) {
					continue; // Skip defaults
				}
				attrPrefix = xliffReader.getAttributePrefix(i);
				attrName = (((attrPrefix == null) || (attrPrefix.length() == 0)) ? "" : attrPrefix + ":")
					+ xliffReader.getAttributeLocalName(i);
				attrValue = xliffReader.getAttributeValue(i);

				if (attrName.equals("xml:lang") && updateLangWithTarget) {
					attrValue = trgLang.toBCP47();
				}
				if (attrName.equals("xml:space")) {
	                ps = isPreserveSpaceAttributeValue(attrValue);
	            }

				if (Property.APPROVED.equals(xliffReader.getAttributeLocalName(i))) {
					skel.addValuePlaceholder(tu, Property.APPROVED, trgLang);
					addApprovedIfNeeded = false;
				}
				else if ( attrName.equals("its:locQualityIssuesRef") ) {
					itsLQIRef = " its:locQualityIssuesRef=\""+attrValue+"\"";
				}
				else if ( attrName.startsWith("its:locQuality") ) {
					itsLQIRef = " its:locQualityIssuesRef=\"\""; // ID will be set on writing
				}
				else if ( attrName.equals("its:provenanceRecordsRef") ) {
					itsProvRef = " its:provenanceRecordsRef=\""+attrValue+"\"";
				}
				else if ( mtConfTarget && attrName.equals("its:mtConfidence") ) {
					// Output done in skeleton with property
				}
				else if ( mtConfTarget && attrName.equals("its:annotatorsRef") && attrValue.startsWith("mt-confidence|") ) {
					// Output done in skeleton with property
					// this is far from perfect, but it'll work in most cases
				}
				else if ( attrName.startsWith("its:rev")
					|| attrName.startsWith("its:person")
					|| attrName.startsWith("its:org")
					|| attrName.startsWith("its:tool")
					|| attrName.startsWith("its:prov") )
				{
					itsProvRef = " its:provenanceRecordsRef=\"\""; // ID will be set on writing
				}
				else {
					skel.append(" ");
					skel.append(attrName);
					skel.append("=\"");
					skel.append(Util.escapeToXML(attrValue.replace("\n", lineBreak), 3, params.getEscapeGT(), null));
					skel.append("\"");
				}
			}

			switch (xliffReader.getLocalName()) {
				case TRANS_UNIT:
					GenericSkeletonPart part = skel.addValuePlaceholder(tu, Property.ITS_LQI, LocaleId.EMPTY);
					if (needToCheckforITSDecl) {
						needToCheckforITSDecl = !checkForITSNamespace(part);
					}
					tu.setProperty(new Property(Property.ITS_LQI, itsLQIRef));
					part = skel.addValuePlaceholder(tu, Property.ITS_PROV, LocaleId.EMPTY);
					if (needToCheckforITSDecl) {
						needToCheckforITSDecl = !checkForITSNamespace(part);
					}
					tu.setProperty(new Property(Property.ITS_PROV, itsProvRef));
					break;
				case "source":
					itsLQISource = skel.addValuePlaceholder(tu, Property.ITS_LQI, LocaleId.EMPTY);
					if (needToCheckforITSDecl) {
						needToCheckforITSDecl = !checkForITSNamespace(itsLQISource);
					}
					itsProvSource = skel.addValuePlaceholder(tu, Property.ITS_PROV, LocaleId.EMPTY);
					if (needToCheckforITSDecl) {
						needToCheckforITSDecl = !checkForITSNamespace(itsProvSource);
					}
					break;
				case "target":
					if (mtConfTarget) {
						itsMtConfTarget = skel
								.addValuePlaceholder(tu, Property.ITS_MTCONFIDENCE, LocaleId.EMPTY);
					}
					itsLQITarget = skel.addValuePlaceholder(tu, Property.ITS_LQI, LocaleId.EMPTY);
					if (needToCheckforITSDecl) {
						needToCheckforITSDecl = !checkForITSNamespace(itsLQITarget);
					}
					itsProvTarget = skel.addValuePlaceholder(tu, Property.ITS_PROV, LocaleId.EMPTY);
					if (needToCheckforITSDecl) {
						needToCheckforITSDecl = !checkForITSNamespace(itsProvTarget);
					}
					break;
			}

			// Add properties not set but that are writable
			if ( addApprovedIfNeeded ) {
				skel.addValuePlaceholder(tu, Property.APPROVED, trgLang);
			}

			skel.append(">");
			preserveSpaces.push(ps);
		} else {
			removeEndingSkeletonWhitespace();
			inITSStandoff.push(prefix);
		}
	}

	/**
	 * Checks if the current element is within the context of the ITS namespace (with 'its' as the prefix)
	 * and add a marker if not.
	 * @param part the part to mark up if needed.
	 * @return true if not further check/marker is need for this element.
	 **/
	private boolean checkForITSNamespace (GenericSkeletonPart part) {
		String p = xliffReader.getNamespaceContext().getPrefix(Namespaces.ITS_NS_URI);
		if ( p != null ) {
			if ( "its".equals(p) ) {
				return true; // Is within context of ITS namespace with 'its' prefix: nothing to do
			}
			// Else: we need the right prefix
		}
		// Else: add a marker to know we need to add the declaration when writting out ITS data
		part.getData().insert(0, XLIFFSkeletonWriter.ITSNSDECL);
		return true;
	}

	private void storeStartElementFile (StartSubDocument startSubDoc) {
		annotatorsRef.readAndPush();
		storeElementName(xliffReader.getPrefix());

		String attrPrefix, attrName, attrValue;
		boolean ps = preserveSpaces.peek();
		boolean hasTargetlanguage = false;
		String itsProvRef = "";

		int count = xliffReader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !xliffReader.isAttributeSpecified(i) ) continue; // Skip defaults
			attrPrefix = xliffReader.getAttributePrefix(i);
			attrName = (((attrPrefix==null)||(attrPrefix.length()==0)) ? "" : attrPrefix+":")
					+ xliffReader.getAttributeLocalName(i);
			attrValue = xliffReader.getAttributeValue(i);

			if ( xliffReader.getAttributeLocalName(i).equals("target-language") ) {
				// Create a property
				hasTargetlanguage = true;
				startSubDoc.setProperty(new Property("targetLanguage", attrValue, false));
				skel.append(" ");
				skel.append(attrName);
				skel.append("=\"");
				skel.addValuePlaceholder(startSubDoc, "targetLanguage", LocaleId.EMPTY);
				skel.append("\"");
			}
			else if ( xliffReader.getAttributeLocalName(i).equals("source-language") ) {
				startSubDoc.setProperty(new Property("sourceLanguage", attrValue));
				skel.append(" ");
				skel.append(attrName);
				skel.append("=\"");
				skel.addValuePlaceholder(startSubDoc, "sourceLanguage", LocaleId.EMPTY);
				skel.append("\"");
			}
			//TODO: test should not hard-code prefix
			else if ( attrName.equals("its:provenanceRecordsRef") ) {
				itsProvRef = " its:provenanceRecordsRef=\""+attrValue+"\"";
			}
			//TODO: test should not hard-code prefix
			else if ( attrName.startsWith("its:rev")
				|| attrName.startsWith("its:person")
				|| attrName.startsWith("its:org")
				|| attrName.startsWith("its:tool")
				|| attrName.startsWith("its:prov")
			) {
				itsProvRef = " its:provenanceRecordsRef=\"\""; // ID will be set on writing
			}
			else {
				skel.append(" ");
				skel.append(attrName);
				skel.append("=\"");
				skel.append(Util.escapeToXML(attrValue.replace("\n", lineBreak), 3, params.getEscapeGT(), null));
				skel.append("\"");
				if (attrName.equals("xml:space")) {
	                ps = isPreserveSpaceAttributeValue(attrValue);
	            }
			}
		}

		skel.addValuePlaceholder(startSubDoc, Property.ITS_PROV, LocaleId.EMPTY);
		startSubDoc.setProperty(new Property(Property.ITS_PROV, itsProvRef));

		if ( params.getAddTargetLanguage() && !hasTargetlanguage ) {
			// Create the attribute (as a property) if not there yet
			startSubDoc.setProperty(new Property("targetLanguage", trgLang.toBCP47(), false));
			skel.append(" target-language=\"");
			skel.addValuePlaceholder(startSubDoc, "targetLanguage", LocaleId.EMPTY);
			skel.append("\"");
		}

		skel.append(">");
		preserveSpaces.push(ps);
	}

	private void storeStartElementGroup (StartGroup group) {
		annotatorsRef.readAndPush();
		storeElementName(xliffReader.getPrefix());

		String attrPrefix, attrName, attrValue;
		boolean ps = preserveSpaces.peek();
		String itsProvRef = "";

		int count = xliffReader.getAttributeCount();
		for (int i = 0; i < count; i++) {
			if (!xliffReader.isAttributeSpecified(i)) {
				continue; // Skip defaults
			}
			attrPrefix = xliffReader.getAttributePrefix(i);
			attrName = (((attrPrefix == null) || (attrPrefix.length() == 0)) ? "" : attrPrefix + ":")
				+ xliffReader.getAttributeLocalName(i);
			attrValue = xliffReader.getAttributeValue(i);

			if (attrName.equals("xml:space")) {
				ps = isPreserveSpaceAttributeValue(attrValue);
			}

			//TODO: test should not hard-code prefix
			if ( attrName.equals("its:provenanceRecordsRef") ) {
				itsProvRef = " its:provenanceRecordsRef=\"" + attrValue + "\"";
			}
			//TODO: test should not hard-code prefix
			else if ( attrName.startsWith("its:rev")
				|| attrName.startsWith("its:person")
				|| attrName.startsWith("its:org")
				|| attrName.startsWith("its:tool")
				|| attrName.startsWith("its:prov")
			) {
				itsProvRef = " its:provenanceRecordsRef=\"\""; // ID will be set on writing
			}
			else {
				skel.append(" ");
				skel.append(attrName);
				skel.append("=\"");
				skel.append(Util.escapeToXML(attrValue.replace("\n", lineBreak), 3, params.getEscapeGT(), null));
				skel.append("\"");
			}
		}

		skel.addValuePlaceholder(group, Property.ITS_PROV, LocaleId.EMPTY);
		group.setProperty(new Property(Property.ITS_PROV, itsProvRef));

		skel.append(">");
		preserveSpaces.push(ps);
	}

	private void storeElementName(String prefix) {
		if ((prefix == null) || (prefix.length() == 0)) {
			skel.append("<" + xliffReader.getLocalName());
		} else {
			skel.append("<" + prefix + ":" + xliffReader.getLocalName());
		}

		int count = xliffReader.getNamespaceCount();
		for (int i = 0; i < count; i++) {
			prefix = xliffReader.getNamespacePrefix(i);
			skel.append(" xmlns");
			if (!Util.isEmpty(prefix)) {
				skel.append(":" + prefix);
			}
			skel.append("=\"");
			skel.append(xliffReader.getNamespaceURI(i));
			skel.append("\"");
		}
	}

	private void storeEndElement () {
		String prefix = xliffReader.getPrefix();
		if (!Namespaces.ITS_NS_PREFIX.equals(prefix)) {
			if ((prefix != null) && (prefix.length() > 0)) {
				skel.append("</" + prefix + ":" + xliffReader.getLocalName() + ">");
			} else {
				skel.append("</" + xliffReader.getLocalName() + ">");
			}
			preserveSpaces.pop();
		} else {
			inITSStandoff.pop();
		}
		annotatorsRef.pop();
	}

	private void storeSdlSeg() {
		String attrPrefix, attrName, attrValue;

		storeElementName(xliffReader.getPrefix());

		String segId = xliffReader.getAttributeValue(null, "id");

		int count = xliffReader.getAttributeCount();
		boolean conf = false;
		boolean origin = false;
		boolean useSegsForSdlProp = params.getUseSegsForSdlProps();
		for (int i = 0; i < count; i++) {
			if (!xliffReader.isAttributeSpecified(i)) {
				continue; // Skip defaults
			}
			attrPrefix = xliffReader.getAttributePrefix(i);
			attrName = (((attrPrefix == null) || (attrPrefix.length() == 0)) ? "" : attrPrefix + ":")
				+ xliffReader.getAttributeLocalName(i);
			attrValue = xliffReader.getAttributeValue(i);

			skel.append(" ");
			skel.append(attrName);
			skel.append("=\"");
			switch (attrName) {
				case "locked":
					skel.append(useSegsForSdlProp
						? SdlXliffSkeletonWriter.SDL_SEG_LOCKED_STARTMARKER+segId+SdlXliffSkeletonWriter.SDL_SEG_PROP_ENDMARKER
						: SdlXliffSkeletonWriter.SDL_SEG_LOCKED_MARKER);
					break;
				case "conf":
					skel.append(useSegsForSdlProp
						? SdlXliffSkeletonWriter.SDL_SEG_CONF_STARTMARKER+segId+SdlXliffSkeletonWriter.SDL_SEG_PROP_ENDMARKER
						: SdlXliffSkeletonWriter.SDL_SEG_CONF_MARKER);
					conf = true;
					break;
				case "origin":
					skel.append(useSegsForSdlProp
						? SdlXliffSkeletonWriter.SDL_SEG_ORIGIN_STARTMARKER+segId+SdlXliffSkeletonWriter.SDL_SEG_PROP_ENDMARKER
						: SdlXliffSkeletonWriter.SDL_SEG_ORIGIN_MARKER);
					origin = true;
					break;
				default:
					skel.append(
							Util.escapeToXML(attrValue.replace("\n", lineBreak), 3, params.getEscapeGT(), null));
					break;
			}
			skel.append("\"");
		}

		if ( !params.getUseSegsForSdlProps() ) {
			// check if we need to add missing conf or origin attribute placeholders
			// only add these if the config defines them and they aren't in the original sdl:seg
			if (!conf && !Util.isEmpty(getParameters().getSdlSegConfValue()) &&  params.isUseSdlXliffWriter()) {
				skel.append(" ");
				skel.append("conf");
				skel.append("=\"");
				skel.append(SdlXliffSkeletonWriter.SDL_SEG_CONF_MARKER);
				skel.append("\"");
			}

			if (!origin && !Util.isEmpty(getParameters().getSdlSegOriginValue()) && params.isUseSdlXliffWriter()) {
				skel.append(" ");
				skel.append("origin");
				skel.append("=\"");
				skel.append(SdlXliffSkeletonWriter.SDL_SEG_ORIGIN_MARKER);
				skel.append("\"");
			}
		}

		skel.append(">");
	}

	/**
	 * Store information from an <code><iws:status></code> element.
	 */
	private void storeIwsStatus() {
		String attrPrefix, attrName, attrValue;
		boolean transStatus = false;
		boolean transType = false;
		boolean blockedTransUnit = !tu.isTranslatable();

		storeElementName(xliffReader.getPrefix());
		int count = xliffReader.getAttributeCount();
		for (int i = 0; i < count; i++) {
			if (!xliffReader.isAttributeSpecified(i)) {
				continue; // Skip defaults
			}
			attrPrefix = xliffReader.getAttributePrefix(i);
			attrName = (((attrPrefix == null) || (attrPrefix.length() == 0)) ? "" : attrPrefix + ":")
					+ xliffReader.getAttributeLocalName(i);
			attrValue = xliffReader.getAttributeValue(i);

			skel.append(" ");
 		    skel.append(attrName);
			skel.append("=\"");

			if (!blockedTransUnit && !inAltTrans
				&& new IwsStatusAttribute(IwsProperty.TRANSLATION_STATUS).getAttributeName().equals(attrName)
				&& !Util.isEmpty(getParameters().getIwsTransStatusValue())) {
				skel.append(IwsXliffSkeletonWriter.IWS_TRANS_STATUS_MARKER);
				transStatus = true;
			} else if (!blockedTransUnit && !inAltTrans
				&& new IwsStatusAttribute(IwsProperty.TRANSLATION_TYPE).getAttributeName().equals(attrName)
				&& !Util.isEmpty(getParameters().getIwsTransTypeValue())) {
				skel.append(IwsXliffSkeletonWriter.IWS_TRANS_TYPE_MARKER);
				transType = true;
			} else {
				skel.append(Util.escapeToXML(attrValue.replace("\n", lineBreak), 3, params.getEscapeGT(), null));
			}
			skel.append("\"");
		}

		// Add translation_status if not included in source file
		if (!blockedTransUnit && !transStatus && !inAltTrans
			&& getParameters().isUseIwsXliffWriter()
			&& !Util.isEmpty(getParameters().getIwsTransStatusValue())) {
			skel.append(" ");
			skel.append(new IwsStatusAttribute(IwsProperty.TRANSLATION_STATUS).getAttributeName());
            skel.append("=\"");
			skel.append(IwsXliffSkeletonWriter.IWS_TRANS_STATUS_MARKER);
			skel.append("\"");
		}
		// Add translation_type if not included in source file
		if (!blockedTransUnit && !transType && !inAltTrans
			&& getParameters().isUseIwsXliffWriter()
			&& !Util.isEmpty(getParameters().getIwsTransTypeValue())) {
			skel.append(" ");
			skel.append(new IwsStatusAttribute(IwsProperty.TRANSLATION_TYPE).getAttributeName());
			skel.append("=\"");
			skel.append(IwsXliffSkeletonWriter.IWS_TRANS_TYPE_MARKER);
			skel.append("\"");
		}
		skel.append(">");

	}

	private boolean processTransUnit () throws IOException {
		try {
			// Make a document part with skeleton between the previous event and now.
			// Spaces can go with trans-unit to reduce the number of events.
			// This allows to have only the trans-unit skeleton parts with the TextUnit event
			if ( !skel.isEmpty(true) ) {
				DocumentPart dp = new DocumentPart(otherId.createId(), false, skel);
				skel = new GenericSkeleton(); // And create a new skeleton for the next event
				queue.add(new Event(EventType.DOCUMENT_PART, dp));
			}

			// Process trans-unit
			sourceDone = false;
			targetDone = false;
			altTransDone = false;
			noteDone = false;
			segSourceDone = false;
			altTrans = null;
			processAltTrans = false;
			inAltTrans = false;
			segSourceDone = false;
			extraId = Integer.MAX_VALUE; // For extra code generated when processing sub elements
			tu = new TextUnit(String.valueOf(++tuId));
			tu.setSkeleton(skel);

			storeStartElement(false, true, false);

			tu.setIsTranslatable(isTranslatable(getXliffAttributeValue("translate")));

			String tmp = getXliffAttributeValue("id");
			if ( tmp == null ) throw new OkapiIllegalFilterOperationException("Missing attribute 'id'.");
			tu.setId(tmp);

			tmp = getXliffAttributeValue("resname");
			if ( tmp != null ) tu.setName(tmp);
			else if ( params.getFallbackToID() ) {
				tu.setName(tu.getId());
			}

			tmp = getXliffAttributeValue("phase-name");
			if (tmp != null) {
				XLIFFPhaseAnnotation phaseAnn = startSubDoc.getAnnotation(XLIFFPhaseAnnotation.class);
				if (phaseAnn != null && phaseAnn.get(tmp) != null) {
					XLIFFPhaseAnnotation tuPhaseAnn = new XLIFFPhaseAnnotation();
					tuPhaseAnn.add(phaseAnn.get(tmp));
					tu.setAnnotation(tuPhaseAnn);
				}
			}
			tmp = getXliffAttributeValue(PROP_EXTRADATA);
			if ( tmp != null ) {
				tu.setProperty(new Property(PROP_EXTRADATA, tmp, true));
			}

			approved = -1;
			tmp = getXliffAttributeValue(Property.APPROVED);
			if ( tmp != null ) {
				approved = 0;
				if ( tmp.equals("yes") ) {
					approved = 1;
				}
			}

			// Process the text unit-level ITS attributes (attached them as annotations)
			itsFilterHandler.readTextUnitITSAttributes(tu);

			// Set restype (can be null)
			tu.setType(getXliffAttributeValue("restype"));

			addLengthConstraints(tu);

			// Get the content
			int eventType;
			while ( xliffReader.hasNext() ) {
				eventType = xliffReader.next();
				String name;
				switch ( eventType ) {
				case XMLStreamConstants.START_ELEMENT:
					// Skip MQ extensions because them may include XLIFF mis-used elements
					if ( xliffReader.getNamespaceURI().startsWith("MQXliff") ) {
						storeWholeElement(xliffReader.getName());
						break;
					}
					name = xliffReader.getLocalName();
					if ( "source".equals(name) ) {
						if ( inAltTrans ) {
							if ( !params.getEditAltTrans() ) storeStartElement(false, false, false);
						}
						else storeStartElement(false, false, false);
						processSource(false);
						if ( inAltTrans ) {
							if ( !params.getEditAltTrans() ) storeEndElement();
						}
						else storeEndElement();
					}
					else if ( "target".equals(name) ) {
						addSegSourceIfNeeded();
						if ( inAltTrans ) {
							if ( !params.getEditAltTrans() ) storeStartElement(params.getOverrideTargetLanguage(), false, false);
						}
						else storeStartElement(params.getOverrideTargetLanguage(), false, true);
						processTarget();
						if ( inAltTrans ) {
							if ( !params.getEditAltTrans() ) storeEndElement();
						}
						else storeEndElement();
					}
					else if ( "seg-source".equals(name) ) {
						// Store the seg-source skeleton in a isolated part
						skel.add(XLIFFSkeletonWriter.SEGSOURCEMARKER);
						skel.attachParent(tu);
						storeStartElement(false, false, false);
						processSource(true);
						storeEndElement();
						skel.flushPart(); // Close the part for the seg-source
						segSourceDone = true;
						if ( tu.getSource().hasBeenSegmented() ) {
							tu.setProperty(new Property(PROP_WASSEGMENTED, "true", true));
						}
					}
					else if ( NOTE.equals(name) ) {
						addTargetIfNeeded();
						//storeStartElement(false, false, false);
						processNote();
						//storeEndElement();
					}
					else if ( "alt-trans".equals(name) ) {
						addTargetIfNeeded();
						if ( !params.getEditAltTrans() ) storeStartElement(false, false, false);
						processStartAltTrans();
					}
					else if (IwsSegmentMetadataAttribute.ELEMENT_NAME.equals(name) && IwsProperty.IWS_NAMESPACE.equals(xliffReader.getPrefix())) {
						inIwsSegmentMetadata = true;
						storeStartElement(false, false, false);
						processIwsSegmentMetadata();
					}
					else if (IwsProperty.MARKUP_SEG_ELEMENT_NAME.equals(name) && IwsProperty.IWS_NAMESPACE.equals(xliffReader.getPrefix())) {
						processIwsMarkupSeg();
					}
					else if (IwsStatusAttribute.ELEMENT_NAME.equals(name) && IwsProperty.IWS_NAMESPACE.equals(xliffReader
						.getPrefix())) {
						processIwsStatus();
						skel.flushPart();
						storeIwsStatus();
					}
					else if ("seg".equals(name) && "sdl".equals(xliffReader.getPrefix()) && params.isUseSdlXliffWriter()) {
						processSdlSeg();
						skel.flushPart();
						storeSdlSeg();
					}
					else if ( "rep".equals(name) && "sdl".equals(xliffReader.getPrefix()) ) {
						processAndStoreSdlRep();
					}
					else if (XLIFFContextGroup.ELEMENT_NAME.equals(name)) {
						if (inAltTrans) {
							readContextGroupFor(altTrans.getLast().getEntry());
							if (!params.getEditAltTrans()) {
								final XLIFFContextGroup contextGroup = altTrans.getLast().getEntry()
										.getAnnotation(XLIFFContextGroup.class);
								skel.add(contextGroup.toString());
							}
						} else {
							readContextGroupFor(tu);
						}
					}
					else {
						addTargetIfNeeded();
						storeStartElement(false, false, false);
					}
					break;

				case XMLStreamConstants.END_ELEMENT:
					name = xliffReader.getLocalName();
					if ( TRANS_UNIT.equals(name) ) {
						addTargetIfNeeded();
						storeEndElement();
						if ( altTrans != null ) {
							// make sure the entries are ordered
							altTrans.sort();
						}
						if ( params.getIgnoreInputSegmentation() ) {
							tu.removeAllSegmentations();
						}
						tu.setMimeType(MimeTypeMapper.XLIFF_MIME_TYPE);
						
						if ( xliffFlavor != null ) {
							switch ( xliffFlavor ) {
							case XLIFF_FLAVOR_XTM:
								XliffXtmFilterExtension.consolidateProperties(tu, trgLang);
								break;
							}
						}

						if (XLIFFFilter.isUnsegmentedTextUnit(tu, params)) {
							// Add as document part instead of text unit
							DocumentPart dp = new DocumentPart(otherId.createId(), false, tu.getSkeleton());
							dp.getSkeleton().setParent(tu);
							queue.add(new Event(EventType.DOCUMENT_PART, dp));
						} else {
							queue.add(new Event(EventType.TEXT_UNIT, postProcessTextUnit(tu)));
						}
						return true;
					}
					else if ( "alt-trans".equals(name) ) {
						inAltTrans = false;
						if ( !params.getEditAltTrans() ) storeEndElement();
					}
					else if ("seg".equals(name) && "sdl".equals(xliffReader.getPrefix())) {
						skel.append("</sdl:seg>");
						skel.flushPart();
					}
					else if (IwsStatusAttribute.ELEMENT_NAME.equals(name) && IwsProperty.IWS_NAMESPACE.equals(xliffReader.getPrefix())) {
						skel.append("</iws:status>");
						skel.flushPart();
					}
					else if (IwsSegmentMetadataAttribute.ELEMENT_NAME.equals(name) && IwsProperty.IWS_NAMESPACE.equals(xliffReader.getPrefix())) {
						inIwsSegmentMetadata = false;
						skel.append("</iws:segment-metadata>");
						skel.flushPart();
					}
					else {
						// Just store the end
						storeEndElement();
					}
					break;

				case XMLStreamConstants.SPACE:
				case XMLStreamConstants.CHARACTERS:
					if ( !targetDone ) {
						// Faster that separating XMLStreamConstants.SPACE
						// from other data in the all process
						tmp = xliffReader.getText();
						for ( int i=0; i<tmp.length(); i++ ) {
							if ( !Character.isWhitespace(tmp.charAt(i)) ) {
								addTargetIfNeeded();
								break;
							}
						}
					}
					//TODO: escape unsupported chars
					skel.append(Util.escapeToXML(xliffReader.getText().replace("\n", lineBreak), 0, params.getEscapeGT(), null));
					break;

				case XMLStreamConstants.CDATA:
					skel.append(CDATA_START);
					skel.append(xliffReader.getText().replace("\n", lineBreak));
					skel.append(CDATA_END);
					break;

				case XMLStreamConstants.COMMENT:
					//addTargetIfNeeded();
					skel.append("<!--"+ xliffReader.getText().replace("\n", lineBreak) + "-->");
					break;

				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					//addTargetIfNeeded();
					skel.append("<?"+ xliffReader.getPITarget() + " " + xliffReader.getPIData() + "?>");
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
		return false;
	}

	private ITextUnit postProcessTextUnit(ITextUnit textUnit) {
		if ( params.getUseCodeFinder() ) {
			int srcCodeCount = 0;
			int trgCodeCount = 0;
			encoderManager.updateEncoder(textUnit.getMimeType());
			for (TextPart part : textUnit.getSource().getParts()) {
				part.text = applyCodeFinder(new TextFragment(part.text));
				srcCodeCount += part.text.getCodes().size();
			}

			// Handle code protection in the target text, if the codes are balanced between source and target.
			if (textUnit.hasTarget(trgLang)) {
				TextContainer tmpTarget = textUnit.getTarget(trgLang).clone();
				for (TextPart part : tmpTarget.getParts()) {
					part.text = applyCodeFinder(new TextFragment(part.text));
					trgCodeCount += part.text.getCodes().size();
				}
				if (srcCodeCount == trgCodeCount) {
					textUnit.setTarget(trgLang, tmpTarget);
				} else {
					logger.warn("Code mismatch between source and target. Target will not be protected (tu id='{}'): {} != {}",
						tu.getId(), srcCodeCount, trgCodeCount);
				}
			}
		}
		return textUnit;
	}

	private TextFragment applyCodeFinder(TextFragment text) {
		params.getCodeFinder().process(text);
		// HACK: force the codes array into the order they appear in the text. This correction
		// is performed incrementally in calls to remove(), but the final code inserted by
		// the codefinder may be out of order in the codes list relative to its position in the
		// coded text. This will mess up our "is it in CDATA?" calculation below, so we have
		// to force a full reordering.
		text = text.subSequence(0, -1);
		boolean escapeCodeData = true;
		// Pre-emptively re-encode anything we parsed out, since it won't happen otherwise
		for (Code code : text.getCodes()) {
			if (params.isInlineCdata() && Code.TYPE_CDATA.equals(code.getType())) {
				// We need to disable code data escaping while inside a CDATA section
				if (code.getTagType() == TagType.OPENING) {
					escapeCodeData = false;
				}
				else if (code.getTagType() == TagType.CLOSING) {
					escapeCodeData = true;
				}
			}
			else if (escapeCodeData){
				code.setData(encoderManager.encode(code.getData(), EncoderContext.TEXT));
			}
		}
		return text;
	}

	/**
	 * Returns true if a given translate value for the current context is true or false.
	 * @param trans the value of a translate attribute
	 * @return true if the given value is yes or if it's null but the context is true,
	 * false if the given value is no, or if it's null and the context is false.
	 */
	private boolean isTranslatable (String trans) {
		if ( trans != null ) {
			return "yes".equals(trans);
		}
		return translateCtx.peek();
	}

	/**
	 * Store an element and its content (including child elements).
	 * This supports comments, PIs, nested elements, and characters.
	 * @param elemName name of the element to store.
	 * @throws XMLStreamException if an error occurs.
	 */
	private void storeWholeElement (QName elemName)
		throws XMLStreamException
	{
		// Store the start element
		storeStartElement(false, false, false);
		int count = 1; // Support nested elements

		// Read and store until we reach the corresponding end
		int eventType;
		while ( xliffReader.hasNext() ) {
			eventType = xliffReader.next();
			switch ( eventType ) {
			case XMLStreamConstants.START_ELEMENT:
				if ( xliffReader.getName().equals(elemName) ) {
					count++;
				}
				storeStartElement(false, false, false);
				break;

			case XMLStreamConstants.END_ELEMENT:
				if ( xliffReader.getName().equals(elemName) ) {
					count--;
				}
				storeEndElement();
				if ( count == 0 ) return; // Done
				break;

			case XMLStreamConstants.SPACE:
			case XMLStreamConstants.CHARACTERS:
				//TODO: escape unsupported chars
				skel.append(Util.escapeToXML(xliffReader.getText().replace("\n", lineBreak), 0, params.getEscapeGT(), null));
				break;

			case XMLStreamConstants.CDATA:
				skel.append(Util.escapeToXML(xliffReader.getText().replace("\n", lineBreak), 0, params.getEscapeGT(), null));
				break;

			case XMLStreamConstants.COMMENT:
				skel.append("<!--"+ xliffReader.getText().replace("\n", lineBreak) + "-->");
				break;

			case XMLStreamConstants.PROCESSING_INSTRUCTION:
				skel.append("<?"+ xliffReader.getPITarget() + " " + xliffReader.getPIData() + "?>");
				break;
			}
		}
	}

	private void processAndStoreSdlRep () {
		// Process it
		String hashValue = xliffReader.getAttributeValue(null, "id");
		int count = 1;
		if ( sdlReps.containsKey(hashValue) ) {
			count = sdlReps.get(hashValue);
			count++;
		}
		sdlReps.put(hashValue, count);
		if (( count > 1 ) && ( lastSegForRep != null )) {
			lastSegForRep.setProperty(new Property(XLIFFFilter.PROP_REPETITION, hashValue));
		}
		
		// Store it
		storeStartElement(false, false, false);
	}
	
	private void processSdlSeg() {
		lastSegForRep = null;
		if (!tu.hasTarget(trgLang)) {
			return;
		}

		// We might be even stricter here by using an SDL namespace.
		// Something similar to getXliffAttributeValue.
		// But I am not sure we know all the possible SDL namespaces, and how stable they are.
		// So being too strict might make this more fragile.
		String conf = xliffReader.getAttributeValue(null, "conf");
		String locked = xliffReader.getAttributeValue(null, "locked");
		String origin = xliffReader.getAttributeValue(null, "origin");
		String originSystem = xliffReader.getAttributeValue(null, "origin-system");
		String percent = xliffReader.getAttributeValue(null, "percent");
		String textMatch = xliffReader.getAttributeValue(null, "text-match");
		TextContainer t = tu.getTarget(trgLang);

		// if conf and origin do not exist in the original, but the config defines
		// them then add the property anyway with a null value we will add these
		// attributes in the merger
		if (!Util.isEmpty(conf) || !Util.isEmpty(getParameters().getSdlSegConfValue())) {
			t.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_CONF, conf, false));
		}
		if (!Util.isEmpty(origin) || !Util.isEmpty(getParameters().getSdlSegOriginValue())) {
			t.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN, origin, false));
		}
		if (!Util.isEmpty(locked)) {
			t.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_LOCKED, locked, false));
		}
		// origin-system and percent are not writable for now

		// Add the properties to the segments
		String segId = xliffReader.getAttributeValue(null, "id");
		ISegments segs = t.getSegments();
		lastSegForRep = segs.get(segId);
		if ( lastSegForRep != null ) {
			if (!Util.isEmpty(conf) || !Util.isEmpty(getParameters().getSdlSegConfValue())) {
				lastSegForRep.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_CONF, conf, false));
			}
			if (!Util.isEmpty(origin) || !Util.isEmpty(getParameters().getSdlSegOriginValue())) {
				lastSegForRep.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN, origin, false));
			}
			if (!Util.isEmpty(locked)) {
				lastSegForRep.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_LOCKED, locked, false));
			}
			// read-only
			if (!Util.isEmpty(originSystem)) {
				lastSegForRep.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_ORIGIN_SYSTEM, originSystem, true));
			}
			if (!Util.isEmpty(textMatch)) {
				lastSegForRep.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_TEXT_MATCH, textMatch, true));
			}
			if (!Util.isEmpty(percent)) {
				lastSegForRep.setProperty(new Property(SdlXliffSkeletonWriter.PROP_SDL_PERCENT, percent, true));
			}
		}

		// Add state value to the target text container (see Issue #597)
		if (SdlXliffConfLevel.isValidConfValue(conf)) {
			SdlXliffConfLevel confLevel = SdlXliffConfLevel.fromConfValue(conf);
			t.setProperty(new Property(Property.STATE, confLevel.getStateValue(), false));
		} else if (!Util.isEmpty(conf)){
			// Detected conf value is not valid for SDLXLIFF, map to state by prepending 'x-'
			t.setProperty(new Property(Property.STATE, "x-" + conf, false));
		}
		// Also keep track of the original conf value
		if (!Util.isEmpty(conf)) {
			t.setProperty(new Property(SdlXliffSkeletonWriter.PROP_ORIG_SDL_SEG_CONF, conf, false));
		}
	}

	/**
	 * Process an Idiom WorldServer <code><iws:status></code> element.
	 */
	private void processIwsStatus() {
		if (!inIwsSegmentMetadata) {
			return;
		}

		TextContainer t = tu.getTarget(trgLang);
		if (t == null) {
			return;
		}

		// We might be even stricter here by using an IWS namespace.
		// Something similar to getXliffAttributeValue.
		// But I am not sure we know all the possible IWS namespaces, and how stable they are.
		// So being too strict might make this more fragile.
		Parameters param = getParameters();
		String iwsTmScoreProperty = new IwsSegmentMetadataAttribute(IwsProperty.TM_SCORE).getAttributeNamePrefixed();
		boolean blockedTransUnit =
			// block translation_status="finished"
			(param.isIwsBlockFinished()
				&& attributeValueEquals(IwsProperty.TRANSLATION_STATUS, "finished"))
			|| (param.isIwsBlockLockStatus()
				&& attributeValueEquals(IwsProperty.LOCK_STATUS, "locked"))
			// block tm_score="{specified value}" and optionally leave multiple_exact="has_multiple_exact" translatable
			|| (param.isIwsBlockTmScore()
				&& tu.hasTargetProperty(trgLang, iwsTmScoreProperty)
				&& param.getIwsBlockTmScoreValue().equals(tu.getTargetProperty(trgLang, iwsTmScoreProperty).getValue())
				&& !(param.isIwsIncludeMultipleExact() && attributeValueEquals(IwsProperty.MULTIPLE_EXACT, "has_multiple_exact")))
			// block multiple_exact="has_multiple_exact"
			|| (param.isIwsBlockMultipleExact()
				&& attributeValueEquals(IwsProperty.MULTIPLE_EXACT, "has_multiple_exact"));


		if (blockedTransUnit) {
			tu.setTargetProperty(trgLang, new Property(Property.APPROVED, "yes", false));
			tu.setIsTranslatable(false);
		}

		int count = xliffReader.getAttributeCount();
		for (int i = 0; i < count; i++) {
			// Add all the metadata as properties
			t.setProperty(new Property(
				new IwsStatusAttribute(xliffReader.getAttributeLocalName(i)).getAttributeNamePrefixed(),
				xliffReader.getAttributeValue(i), true));
		}

	}

	/**
	 * Process an Idiom WorldServer <code><iws:segment-metadata></code> element.
	 */
	private void processIwsSegmentMetadata() {
		TextContainer t = tu.getTarget(trgLang);
		if (t == null) {
			return;
		}

		int count = xliffReader.getAttributeCount();
		for (int i = 0; i < count; i++) {
			// Add all the metadata as properties
			t.setProperty(new Property(
				new IwsSegmentMetadataAttribute(xliffReader.getAttributeLocalName(i)).getAttributeNamePrefixed(),
				xliffReader.getAttributeValue(i), true));
		}

	}

	/**
	 * Process an Idiom WorldServer <code><iws:markup-seg></code> element.
	 */
	private void processIwsMarkupSeg() {
		storeStartElement(false, false, false);
        try {
			StringBuffer tmp = new StringBuffer();
			int eventType;
			while ( xliffReader.hasNext() ) {
				eventType = xliffReader.next();
				switch ( eventType ) {
					case XMLStreamConstants.CHARACTERS:
					case XMLStreamConstants.CDATA:
					case XMLStreamConstants.SPACE:
						tmp.append(xliffReader.getText());
						break;
					case XMLStreamConstants.END_ELEMENT:
						String name = xliffReader.getLocalName();
						if ( name.equals(IwsProperty.MARKUP_SEG_ELEMENT_NAME) ) {
							TextContainer t = tu.getTarget(trgLang);
							if (t == null) {
								return;
							}

							if (!Util.isEmpty(tmp.toString())) {
								t.setProperty(new Property(IwsProperty.MARKUP_SEG , Util.escapeToXML(tmp.toString(),0, params.getEscapeGT(), null), false));
							}
							skel.append(Util.escapeToXML(tmp.toString(),0, params.getEscapeGT(), null));
							storeEndElement();
							skel.flushPart();
							return;
						}
						break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}

	}

	private void processSource (boolean isSegSource) throws IOException {
		TextContainer tc;
		if ( sourceDone ) { // Case of an alt-trans entry
			// Get the language
			String tmp = xliffReader.getAttributeValue(XMLConstants.XML_NS_URI, "lang");
			LocaleId lang;
			if ( tmp == null ) lang = srcLang; // Use default
			else lang = LocaleId.fromString(tmp);
			// Get the text content
			boolean tmpStore = true;
			if ( inAltTrans ) {
				if ( params.getEditAltTrans() ) tmpStore = false;
			}
			tc = processContent(isSegSource ? "seg-source" : "source", tmpStore);
			// Put the source in the alt-trans annotation
			if ( !preserveSpaces.peek() ) {
				tc.unwrap(true, false);
			}
			// Store in altTrans only when we are within alt-trans
			if ( inAltTrans ) {
				if ( processAltTrans ) {
					if ( isSegSource ) {
						logger.warn("Segmented content in <alt-trans> is not supported (entry id='{}').", tu.getId());
					}
					else {
						// Add the source, no target yet
						AltTranslation alt = altTrans.add(lang, null, null, tc.getFirstContent(), null,
							altTransMatchType, 0, altTransOrigin);
						alt.setAltTransType(alttranstype);
						alt.getEntry().setPreserveWhitespaces(preserveSpaces.peek());
						alt.setEngine(altTransEngine);
						alt.setExType(altTransExType);
						XLIFFToolAnnotation toolAnn = startSubDoc.getAnnotation(XLIFFToolAnnotation.class);
						if (toolAnn != null) {
							alt.setTool(toolAnn.get(altTrans.getCurrentToolId()));
						}
						if ( altTransQuality > 0 ) {
							alt.setCombinedScore(altTransQuality);
						}
					}
				}
			}
			else { // It's seg-source just after a <source> (not in alt-trans)
				TextContainer cont = tc.clone();
				cont.getSegments().joinAll();
				if ( !params.isAlwaysUseSegSource() && cont.compareTo(tu.getSource(), true) != 0 ) {
					logger.error("The <seg-source> content for the entry id='{}' is different from its <source>. The un-segmented content of <source> will be used.", tu.getId());
				}
				else { // Same content: use the segmented one
					GenericAnnotations.addAnnotations(tc, tu.getSource().getAnnotation(GenericAnnotations.class));
					ITSLQIAnnotations.addAnnotations(tc, tu.getSource().getAnnotation(ITSLQIAnnotations.class));
					ITSProvenanceAnnotations.addAnnotations(tc, tu.getSource().getAnnotation(ITSProvenanceAnnotations.class));
					tc.setProperty(tu.getSource().getProperty(Property.ITS_LQI));
					tc.setProperty(tu.getSource().getProperty(Property.ITS_PROV));
					tc.setHasBeenSegmentedFlag(true); // Force entries without mrk to single segment entries
					tu.setSource(tc);
					itsLQISource.setParent(tc);
					itsProvSource.setParent(tc);
				}
			}
		}
		else { // Main source of the trans-unit
			// Get the coord attribute if available
			String tmp = getXliffAttributeValue("coord");
			if ( tmp != null ) {
				tu.setSourceProperty(new Property(Property.COORDINATES, tmp, true));
			}
			// Get the ITS annotations for the source
			GenericAnnotations anns = itsFilterHandler.readTextContainerITSAttributes();
			ITSLQIAnnotations lqiAnn = itsFilterHandler.readITSLQI();
			ITSProvenanceAnnotations provAnn = itsFilterHandler.readITSProvenance();
			String itsLQIRef = xliffReader.getAttributeValue(Namespaces.ITS_NS_URI, "locQualityIssuesRef");
			String itsProvRef = xliffReader.getAttributeValue(Namespaces.ITS_NS_URI, "provenanceRecordsRef");

			skel.addContentPlaceholder(tu);
			tc = processContent(isSegSource ? "seg-source" : "source", false);
			if ( !preserveSpaces.peek() ) {
				tc.unwrap(true, false);
			}
			tu.setPreserveWhitespaces(preserveSpaces.peek());
			tc.setProperty(new Property(Property.ITS_LQI,
				(lqiAnn != null && itsLQIRef != null) ?
				" its:locQualityIssuesRef=\""+itsLQIRef+"\"" :
				""));
			tc.setProperty(new Property(Property.ITS_PROV,
				(provAnn != null && itsProvRef != null) ?
				" its:provenanceRecordsRef=\""+itsProvRef+"\"" :
				""));
			// Attach the annotation if needed
			GenericAnnotations.addAnnotations(tc, anns);
			ITSLQIAnnotations.addAnnotations(tc, lqiAnn);
			ITSProvenanceAnnotations.addAnnotations(tc, provAnn);
			itsLQISource.setParent(tc);
			itsProvSource.setParent(tc);

			tu.setSource(tc);
			sourceDone = true;
		}
	}

	private void processTarget () throws IOException {
		TextContainer tc;
		AltTranslation alt = null;

		// Get the state attribute if available
		//TODO: Need to standardize target-state properties
		String stateValue = getXliffAttributeValue("state");
		String stateQualifier = getXliffAttributeValue(STATE_QUALIFIER);
		// Get the coord attribute if available
		String coordValue = getXliffAttributeValue("coord");

		if ( targetDone ) { // Case of an alt-trans entry
			// Get the language
			String tmp = xliffReader.getAttributeValue(XMLConstants.XML_NS_URI, "lang");
			LocaleId lang;
			if ( tmp == null ) lang = trgLang; // Use default
			else lang = LocaleId.fromString(tmp);
			// Get the text content
			boolean tmpStore = true;
			if ( inAltTrans ) {
				if ( params.getEditAltTrans() ) tmpStore = false;
			}
			tc = processContent("target", tmpStore);
			// Put the target in the alt-trans annotation
			if ( !preserveSpaces.peek() ) {
				tc.unwrap(true, false);
			}

//			// Fix to be sure the parent of this skeleton part is set properly
//			tc.setProperty(new Property(Property.ITS_MTCONFIDENCE, "")); // Place-holder to add/update
//			itsMtConfTarget.setParent(tc);

			if ( inAltTrans ) {
				if ( processAltTrans ) {
					// Set the target alternate entry
					alt = altTrans.getLast();
					// If we have a target locale already set, it means that entry was used already
					// and we are in an entry without source, so we need to create a new entry
					if (( alt != null ) && ( alt.getTargetLocale() != null )) {
						alt = null; // Behave like it's a first entry
					}
					if ( alt == null ) {
						alt = altTrans.add(srcLang, null, null, null, null,
							altTransMatchType, 0, altTransOrigin);
						alt.getEntry().setPreserveWhitespaces(preserveSpaces.peek());
						alt.setEngine(altTransEngine);
						alt.setAltTransType(alttranstype);
						alt.setExType(altTransExType);
						XLIFFToolAnnotation toolAnn = startSubDoc.getAnnotation(XLIFFToolAnnotation.class);
						if (toolAnn != null) {
							alt.setTool(toolAnn.get(altTrans.getCurrentToolId()));
						}
						if ( altTransQuality > 0 ) {
							alt.setCombinedScore(altTransQuality);
						}
					}
					if ( tc.contentIsOneSegment() ) {
						alt.setTarget(lang, tc.getFirstContent());
					}
					else {
						alt.setTarget(lang, tc.getUnSegmentedContentCopy());
					}
					alt.getEntry().setPreserveWhitespaces(preserveSpaces.peek());
					alt.setFromOriginal(true);

					// update matchtype if needed
					// Adjust UNKNOWN type if we can
					if ( alt.getType().equals(MatchType.UKNOWN)) {
						// order matters
						if (FINAL.equals(stateValue) || SIGNED_OFF.equals(stateValue))
							altTransMatchType = MatchType.ACCEPTED;
						else if (EXACT_MATCH.equals(stateQualifier))
							altTransMatchType = MatchType.EXACT;
						else if (FUZZY_MATCH.equals(stateQualifier))
							altTransMatchType = MatchType.FUZZY;
						else if (MT_SUGGESTION.equals(stateQualifier))
							altTransMatchType = MatchType.MT;
						else if (ID_MATCH.equals(stateQualifier))
							altTransMatchType = MatchType.EXACT_UNIQUE_ID;

						alt.setType(altTransMatchType);
					}
				}
			}
		} else {
			// Get the ITS annotations for the target
			GenericAnnotations anns = itsFilterHandler.readTextContainerITSAttributes();
			ITSLQIAnnotations lqiAnn = itsFilterHandler.readITSLQI();
			ITSProvenanceAnnotations provAnn = itsFilterHandler.readITSProvenance();
			String itsLQIRef = xliffReader.getAttributeValue(Namespaces.ITS_NS_URI, "locQualityIssuesRef");
			String itsProvRef = xliffReader.getAttributeValue(Namespaces.ITS_NS_URI, "provenanceRecordsRef");

			// Get the target itself
			skel.addContentPlaceholder(tu, trgLang);
			tc = processContent("target", false);
			// Set the target, even if it's an empty one.
			if ( !preserveSpaces.peek() ) {
				tc.unwrap(true, false);
			}
			tu.setPreserveWhitespaces(preserveSpaces.peek());

			tc.setProperty(new Property(Property.ITS_MTCONFIDENCE, "")); // Place-holder to add/update
			tc.setProperty(new Property(Property.ITS_LQI,
				(lqiAnn != null && itsLQIRef != null) ?
					" its:locQualityIssuesRef=\""+itsLQIRef+"\"" :
					""));
			tc.setProperty(new Property(Property.ITS_PROV,
				(provAnn != null && itsProvRef != null) ?
					" its:provenanceRecordsRef=\""+itsProvRef+"\"" :
					""));
			// Attach the annotation if needed
			GenericAnnotations.addAnnotations(tc, anns);
			ITSLQIAnnotations.addAnnotations(tc, lqiAnn);
			ITSProvenanceAnnotations.addAnnotations(tc, provAnn);
			itsMtConfTarget.setParent(tc);
			itsLQITarget.setParent(tc);
			itsProvTarget.setParent(tc);

			tu.setTarget(trgLang, tc);

			if ( approved > -1 ) {
				// Note that this property is set to the target at the resource-level
				tu.setTargetProperty(trgLang, new Property(Property.APPROVED, (approved==1 ? "yes" : "no"), false));
			}

			targetDone = true;
		}

		// Set the target properties (after the target container has been set)
		ITextUnit tmpTu = tu;

		// if alt is not null then we add the properties to the alttranslation
		if (alt != null) {
			tmpTu = alt.getEntry();
		}

		if ( stateValue != null ) {
			tmpTu.setTargetProperty(trgLang, new Property(Property.STATE, stateValue, true)); // Read-only for now
		}

		if ( stateQualifier != null ) {
			tmpTu.setTargetProperty(trgLang, new Property(Property.STATE_QUALIFIER, stateQualifier, false));
		}
		if ( coordValue != null ) {
			tmpTu.setTargetProperty(trgLang, new Property(Property.COORDINATES, coordValue, true)); // Read-only for now
		}
	}

	/**
	 * Makes sure that opening and closing codes use the same id if they use the same originalId
	 * <strong>across all text parts</strong> of the given {@link TextContainer}.
	 *
	 * @param tc the text container
	 */
	private void matchIdsAcrossParts(TextContainer tc) {
		Map<String, Code> openingCodeByOriginalIdMap = createOpeningCodeByOriginalIdMap(tc);

		List<TextPart> parts = tc.getParts();
		for (int i = parts.size() - 1; i > 0; i--) {
			// get the coded content
			TextFragment partContent = parts.get(i).getContent();
			List<Code> clonedCodes = partContent.getClonedCodes();
			boolean codeChanged = false;

			// any closing tag may need a new ID
			for (Code clonedCode : clonedCodes) {
				if (clonedCode.getTagType() == TagType.CLOSING
						&& clonedCode.getOriginalId() != null) {
					String originalId = clonedCode.getOriginalId();
					Code openingCode = openingCodeByOriginalIdMap.get(originalId);
					if (openingCode != null) {
						clonedCode.setId(openingCode.getId());
						codeChanged = true;
					}
				}
			}

			// recode content since IDs may have changed
			if (codeChanged) {
				partContent.setCodedText(partContent.getCodedText(), clonedCodes);
			}
		}

	}

	private Map<String, Code> createOpeningCodeByOriginalIdMap(TextContainer tc) {
		Map<String, Code> openingCodeIdsByOriginalId = new HashMap<>();
		for (TextPart part : tc.getParts()) {
			for (Code code : part.getContent().getCodes()) {
				if (code.getTagType() == TagType.OPENING && code.getOriginalId() != null) {
					openingCodeIdsByOriginalId.put(code.getOriginalId(), code);
				}
			}
		}
		return openingCodeIdsByOriginalId;
	}

	static final Pattern MATCH_QUALITY_PATTERN = Pattern.compile("\\s*(-?\\d+)(?:\\.\\d\\d)?%?\\s*");
	void parseMatchQualityValue(String rawValue) {
		if (rawValue == null) return;
		Matcher m = MATCH_QUALITY_PATTERN.matcher(rawValue);
		if (m.matches()) {
			try {
				altTransQuality = Integer.valueOf(m.group(1));
				if (altTransQuality < 1) {
					altTransQuality = -1;
				}
			}
			catch (NumberFormatException e) {
				logger.warn("Invalid match-quality value: {}", rawValue);
			}
		}
	}

	private void processStartAltTrans () {
		inAltTrans = true;
		processAltTrans = true;
		String tmp;

		// if no alttranstype attribute the default is proposal
		alttranstype = ALTTRANSTYPE_PROPOSAL;
		// JEH: we process all alt-trans, not just proposal
		if ( getXliffAttributeValue("alttranstype") != null ) {
			alttranstype = getXliffAttributeValue("alttranstype");
		}

		// Get possible mid for segment
		String mid = getXliffAttributeValue("mid");
		// Get possible score (it will be set when we create the entry) -1 or 0 means: don't set it
		altTransQuality = -1;
		parseMatchQualityValue(getXliffAttributeValue("match-quality"));

		// Get the Okapi match-type if one is present
		altTransMatchType = MatchType.UKNOWN;
		tmp = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, XLIFFWriter.OKP_MATCHTYPE);
		if ( !Util.isEmpty(tmp) ) {
			altTransMatchType = MatchType.valueOf(tmp);
		}

		// Adjust UNKNOWN type if we can
		if ( altTransMatchType.equals(MatchType.UKNOWN)) {
			String alttranstype = getXliffAttributeValue("alttranstype");
			// order matters
			if (ACCEPTED.equals(alttranstype))
				altTransMatchType = MatchType.ACCEPTED;
			else if ( altTransQuality > 99)
				altTransMatchType = MatchType.EXACT;
			else if ( altTransQuality > 0)
				altTransMatchType = MatchType.FUZZY;
		}

		// Get the origin if present
		altTransOrigin = AltTranslation.ORIGIN_SOURCEDOC;
		tmp = getXliffAttributeValue("origin");
		if ( !Util.isEmpty(tmp) ) {
			altTransOrigin = tmp;
		}

		// Get the extype attribute if present (gets null if not present)
		altTransExType = getXliffAttributeValue("extype");
		
		// Get the engine if present
		altTransEngine = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, XLIFFWriter.OKP_ENGINE);

		// Look where the annotation needs to go: segment or container?
		// Get the target (and possibly creates it if needed)
		TextContainer tc = tu.getTarget(trgLang);
		if ( tc == null ) {
			// Create a target from the source if needed
			tc = tu.createTarget(trgLang, false, IResource.COPY_SEGMENTATION); // was COPY_CONTENT before ITextUnit
//			// Make sure it's empty, but that segments are preserved
//			for ( Segment seg : tc.getSegments() ) {
//				seg.text.clear();
//			}
		}

		// Decide where to attach the annotation: the segment or the container
		if ( mid == null ) { // Annotation should be attached on the container
			altTrans = tc.getAnnotation(AltTranslationsAnnotation.class);
			if ( altTrans == null ) {
				// If none exists: create one
				altTrans = new AltTranslationsAnnotation();
				tc.setAnnotation(altTrans);
			}
			altTrans.setCurrentToolId(getXliffAttributeValue("tool-id"));
		}
		else { // Annotation should be attached to its corresponding segment
			Segment seg = tc.getSegments().get(mid);
			if ( seg == null ) {
				// No corresponding segment found. We drop that entry
				logger.warn("An <alt-trans> element for an unknown segment '{}' was detected. It will be ignored.", mid);
				processAltTrans = false;
				return;
			}
			// Else: get possible existing annotation
			altTrans = seg.getAnnotation(AltTranslationsAnnotation.class);
			if ( altTrans == null ) {
				// If none exists: create one
				altTrans = new AltTranslationsAnnotation();
				seg.setAnnotation(altTrans);
			}
			altTrans.setCurrentToolId(getXliffAttributeValue("tool-id"));
		}
	}

	private void addSegSourceIfNeeded () {
		// Add skeleton part for the seg-source if it's was not there
		if ( !segSourceDone ) {
			// Add an empty part of the potential seg-source to add
			skel.add(XLIFFSkeletonWriter.SEGSOURCEMARKER);
			skel.attachParent(tu);
			skel.flushPart(); // Close the part for the seg-source
			segSourceDone = true;
		}
	}

	private void addAltTransMarker () {
		if ( altTransDone ) return;
		// Add skeleton part for the alt-trans to be added
		// This is in addition to the existing ones
		// Add an empty part of the potential alt-trans to add
		skel.add(XLIFFSkeletonWriter.ALTTRANSMARKER);
		skel.attachParent(tu);
		skel.flushPart(); // Close the part for the seg-source
		altTransDone = true;
	}

	private void addNoteMarker() {
	    if ( noteDone ) return;
	    skel.add(XLIFFSkeletonWriter.NOTEMARKER);
	    skel.attachParent(tu);
	    skel.flushPart();
	    noteDone = true;
	}

	private void addTargetIfNeeded () {
		if ( !sourceDone ) {
			throw new OkapiIllegalFilterOperationException("Element <source> missing or not placed properly.");
		}
		if ( targetDone ) {
			addAltTransMarker();
			addNoteMarker();
			return; // Nothing to add
		}

		// Add the seg-source part if needed
		addSegSourceIfNeeded();

		// If the target language is the same as the source, we should not create new <target>
		if ( srcLang.equals(trgLang) ) return;
		//Else: this trans-unit has no target, we add it here in the skeleton
		// so we can merge target data in it when writing out the skeleton
		skel.append("<target xml:lang=\"");
		skel.append(trgLang.toString());
		skel.append("\">");
		skel.addContentPlaceholder(tu, trgLang);
		skel.append("</target>");
		skel.append(lineBreak);
		targetDone = true;
		addAltTransMarker();
        addNoteMarker();
	}

	/**
	 * Processes a segment content.
	 * @param tagName the name of the element content that is being processed.
	 * @param store true if the data must be stored in the skeleton. This is used to merge later on.
	 * @return a new TextContainer object with the parsed content.
	 * @throws IOException
	 */
	private TextContainer processContent (String tagName,
		boolean store) throws IOException
	{
		try {
			boolean changeFirstPart = false;
			TextContainer content = new TextContainer();
			ISegments segments = content.getSegments();
			int id = 0;
			autoMid = -1;
			Stack<Integer> idStack = new Stack<>();
			// used for g codes only (to sdltagdef codes)
			Stack<String> originalIdStack = new Stack<>();
			List<Integer> annIds = new ArrayList<>();
			idStack.push(id);
			CodeTypeForPairedTagsHelper codeTypeForPairedTagsHelper = new CodeTypeForPairedTagsHelper();

			int eventType;
			String name;
			String tmp;
			Code code;
			Segment segment = null;
			int segIdStack = -1;
			// The current variable points either to content or segment depending on where
			// we are currently storing the parsed data, the segments are part of the content
			// at the end, so all can use the same code/skeleton
			TextFragment current = new TextFragment();
			current.invalidate(); // To handle bracketing open/close cases

			while ( xliffReader.hasNext() ) {
				eventType = xliffReader.next();
				switch ( eventType ) {

				case XMLStreamConstants.CDATA:
					if (params.isInlineCdata()) {
						current.append(new Code(TagType.OPENING, Code.TYPE_CDATA, CDATA_START));
					}
					if (!this.sourceDone && !store && this.cdataSubfilter != null) {
						try (
							final SubFilter sf = new SubFilter(
								this.cdataSubfilter,
								this.params.isInlineCdata() ? null : this.encoderManager.getEncoder(),
								++cdataSubfilterIndex,
								this.tu.getId(),
								this.tu.getName()
							)
						) {
							this.queue.addAll(sf.getEvents(new RawDocument(this.xliffReader.getText(), this.srcLang)));
							current.append(sf.createRefCode());
						}
					} else {
						current.append(xliffReader.getText());
					}
					if (params.isInlineCdata()) {
						current.append(new Code(TagType.CLOSING, Code.TYPE_CDATA, CDATA_END));
					}

					if ( store ) {
						if (params.isInlineCdata()) {
							skel.append(CDATA_START + xliffReader.getText() + CDATA_END);
						}
						else {
							skel.append(Util.escapeToXML(xliffReader.getText(), 0, params.getEscapeGT(), null));
						}
					}
					break;

				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.SPACE:
					current.append(xliffReader.getText(), code1 -> { code1.setId(extraId--); return code1; });
					if ( store ) { //TODO: escape unsupported chars
						skel.append(Util.escapeToXML(xliffReader.getText(), 0, params.getEscapeGT(), null));
					}
					break;

				case XMLStreamConstants.END_ELEMENT:
					name = xliffReader.getLocalName();
					if ( name.equals(tagName) ) {
						if ( !current.isEmpty() ) {
							content.append(current, !content.hasBeenSegmented(), !params.getBalanceCodes());

							matchIdsAcrossParts(content);
						}
						return content;
					}
					if ( name.equals("mrk") ) { // Check of end of segment
						if ( idStack.peek() == segIdStack ) {
							current = new TextFragment(); // Point back to content
							current.invalidate(); // To handle bracketing open/close cases
							idStack.pop(); // Pop only after test is true
							segIdStack = -1; // Reset to not trigger segment ending again
							// Add the segment to the content (no collapsing, except when no segments exist yet. Keep empty segments)
							String oriId = segment.getId();
							segments.append(segment, !content.hasBeenSegmented());
							if ( changeFirstPart && ( content.count()==2 )) {
								// Change the initial part into a non-segment
								changeFirstPart = false;
								content.changePart(0);
								segment.forceId(oriId); // Make sure we use the ID defined in the XLIFF
								// We need to do this because if a non-segment part was before it was seen 9so far)
								// as the first segment and its ID may have been the same as the one of the real
								// first segment
							}
							if ( store ) storeEndElement();
							else annotatorsRef.pop();
							continue;
						}
						if ( store ) {
							storeEndElement();
						}
						else {
							annotatorsRef.pop();
						}
						code = current.append(TagType.CLOSING, name, "");
						// We do know the id since the content must be well-formed
						id = idStack.pop(); code.setId(id);
						tmp = xliffReader.getPrefix();
						if (( tmp != null ) && ( tmp.length()>0 )) {
							code.setOuterData("</"+tmp+":"+name+">");
						}
						else {
							code.setOuterData("</"+name+">");
						}
						int n;
						if (( n = annIds.indexOf(id)) != -1 ) {
							annIds.remove(n);
							Code oc = current.getCode(current.getIndex(id));
							GenericAnnotations.addAnnotations(code, oc.getGenericAnnotations());
							code.setType(Code.TYPE_ANNOTATION_ONLY);
						}
					}
					// Other cases
					if ( name.equals("g") ) {
						if ( store ) {
							storeEndElement();
						}
						else {
							annotatorsRef.pop();
						}
						// We do know the id since the content must be well-formed
						id = idStack.pop();

						String type = name;
						String elementName = getElementNameWithOptionalPrefix(name,
								xliffReader.getPrefix());
						int index = current.getIndex(id);
						if (index > -1) {
							Code currentCode = current.getCode(index);
							if (currentCode != null) {
								type = currentCode.getType();
							}
						}
						else {
							// defaults to name
							int closingTagCount = countClosingTags(current);
							type = findMatchingCType(elementName, content, closingTagCount);
						}
						code = current.append(TagType.CLOSING, type, "");
						code.setId(id);
						code.setOuterData("</" + elementName + ">");
						// g end code
						String originalEndId = originalIdStack.pop();
						code.setOriginalId(originalEndId);
						addSdlCodeData(originalEndId, code);
					}
					break;

				case XMLStreamConstants.START_ELEMENT:
					if ( store ) storeStartElement(false, false, false);
					else annotatorsRef.readAndPush();
					name = xliffReader.getLocalName();
					if ( name.equals("mrk") ) { // Check for start of segment
						String type = getXliffAttributeValue("mtype");
						if (( type != null ) && ( type.equals("seg") )) {
							if ( !current.isEmpty() ) { // Append non-segment part
								content.append(current, !content.hasBeenSegmented());
								// If this is have a first part that was not a segment, appending it
								// will make it a segment because a container has always one segment.
								// So we need to fix later when closing this first segment.
								changeFirstPart = !content.hasBeenSegmented(); //(content.count() == 1);
							}
							idStack.push(++id);
							segIdStack = id;
							segment = new Segment();
							segment.id = getXliffAttributeValue("mid");
							current = segment.text; // Segment is now being built
							current.invalidate(); // To handle bracketing open/close cases
							GenericAnnotations anns = itsFilterHandler.readInlineCodeITSAttributes();
							segment.setAnnotation(anns);
							continue;
						}
						else if (( type != null ) && type.equals("protected") ) {
							String originalId = getXliffAttributeValue("mid");
							int mid = retrieveId(id, originalId, false, true);
							code = appendCode(TagType.PLACEHOLDER, mid, name, name, store, current);
							code.setDeleteable(false);
							code.setOriginalId(getOriginalIdOrNull(originalId));
							continue;
						}
					}
					// Other cases
					switch (name) {
						case "g": {
							String originalId = getXliffAttributeValue("id");
							String ctype = getXliffAttributeValue("ctype");
							String equivText = getXliffAttributeValue("equiv-text");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							id = retrieveId(id, originalId, false, false);
							idStack.push(id);
							String codeType = ctype != null ? ctype : name;
							code = current.append(TagType.OPENING, codeType, "", id);
							code.setOriginalId(getOriginalIdOrNull(originalId));
							code.setDisplayText(equivText);
							// Get the outer code
							code.setOuterData(buildStartCode());
							if (merged != null) {
								code.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							addSdlCodeData(originalId, code);
							originalIdStack.push(originalId);
							break;
						}
						case "mrk": {
							String originalId = getXliffAttributeValue("mid");
							int mid = retrieveId(id, originalId, false, true);
							idStack.push(mid);
							code = current.append(TagType.OPENING, name, "", mid);
							// Get the annotations
							GenericAnnotations anns = itsFilterHandler.readInlineCodeITSAttributes();
							if (anns != null) {
								annIds.add(mid);
								GenericAnnotations.addAnnotations(code, anns);
								code.setType(Code.TYPE_ANNOTATION_ONLY);
							}
							// Get the outer code
							code.setOuterData(buildStartCode());
							break;
						}
						case "x": {
							String originalId = getXliffAttributeValue("id");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String ctype = getXliffAttributeValue("ctype");
							String equivText = getXliffAttributeValue("equiv-text");
							id = retrieveId(id, originalId, false, false);
							String codeType = ctype != null ? ctype : name;
							Code c = appendCode(TagType.PLACEHOLDER, id, name, codeType, store, current);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							addSdlCodeData(originalId, c);
							break;
						}
						case "bx": {
							String originalId = getXliffAttributeValue("id");
							String rid = getXliffAttributeValue("rid");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String ctype = getXliffAttributeValue("ctype");
							String equivText = getXliffAttributeValue("equiv-text");
							id = retrieveId(id, originalId, false, false);
							String codeType = codeTypeForPairedTagsHelper.store(rid, originalId, ctype);
							Code c = appendCode(TagType.OPENING, id, name, codeType, store, current);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							break;
						}
						case "ex": {
							// No support for overlapping codes (use -1 as default)
							String originalId = getXliffAttributeValue("id");
							String rid = getXliffAttributeValue("rid");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String equivText = getXliffAttributeValue("equiv-text");
							id = retrieveId(id, originalId, true, false);
							String codeType = codeTypeForPairedTagsHelper.find(rid, originalId);
							Code c = appendCode(TagType.CLOSING, id, name, codeType, store, current);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							break;
						}
						case "bpt": {
							String originalId = getXliffAttributeValue("id");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String ctype = getXliffAttributeValue("ctype");
							String rid = getXliffAttributeValue("rid");
							String equivText = getXliffAttributeValue("equiv-text");
							String codeType = codeTypeForPairedTagsHelper.store(rid, originalId, ctype);
							id = retrieveId(id, originalId, false, false);
							Code c = appendCode(TagType.OPENING, id, name, codeType, store, current);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							break;
						}
						case "ept": {
							String originalId = getXliffAttributeValue("id");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String rid = getXliffAttributeValue("rid");
							String equivText = getXliffAttributeValue("equiv-text");
							String codeType = codeTypeForPairedTagsHelper.find(rid, originalId);
							// No support for overlapping codes (use -1 as default)
							id = retrieveId(id, originalId, true, false);
							Code c = appendCode(TagType.CLOSING, id, name, codeType, store, current);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							break;
						}
						case "ph": {
							String originalId = getXliffAttributeValue("id");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String equivText = getXliffAttributeValue("equiv-text");
							id = retrieveId(id, originalId, false, false);
							Code c = appendCode(TagType.PLACEHOLDER, id, name, name, store, current);
							addSdlCodeData(originalId, c);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							break;
						}
						case "it": {
							String originalId = getXliffAttributeValue("id");
							String merged = xliffReader.getAttributeValue(Namespaces.NS_XLIFFOKAPI, "merged");
							String equivText = getXliffAttributeValue("equiv-text");
							id = retrieveId(id, originalId, false, false);
							tmp = getXliffAttributeValue("pos");
							TagType tt = TagType.PLACEHOLDER;
							if (tmp == null) {
								logger.error("Missing pos attribute for <it> element.");
							} else if (tmp.equals("close")) {
								tt = TagType.CLOSING;
							} else if (tmp.equals("open")) {
								tt = TagType.OPENING;
							} else {
								// Try TMX values for tools not capable of writing correct XLIFF:
								if (tmp.equals("end"))
									tt = TagType.CLOSING;
								else if (tmp.equals("begin"))
									tt = TagType.OPENING;
								// Log an error (or a warning if we were able to get a possible value)
								if (tt == TagType.PLACEHOLDER)
									logger.error("Invalid value '{}' for pos attribute.", tmp);
								else
									logger.warn("Invalid value '{}' for pos attribute.", tmp);
							}
							Code c = appendCode(tt, id, name, name, store, current);
							addSdlCodeData(originalId, c);
							c.setOriginalId(getOriginalIdOrNull(originalId));
							c.setDisplayText(equivText);
							if (merged != null) {
								c.setMergedData(Util.unescapeWhitespaceForXML(merged));
							}
							break;
						}
					}
					break;
				}
			}

			// current should be content at the end
			return content;
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}

	private int countClosingTags(TextFragment current) {
		int closingTagCount = 0;
		for (Code code : current.getCodes()) {
			if (code.getTagType() == TagType.CLOSING) {
				closingTagCount++;
			}
		}
		return closingTagCount;
	}

	/**
	 * wrapper method to join the namespace prefix with the element name
	 *
	 * @param elementName the element name
	 * @param namespacePrefix the optional namespace prefix
	 * @return "elementName", if prefix is empty or null, otherwise "namespacePrefix:elementName"
	 */
	private String getElementNameWithOptionalPrefix(String elementName, String namespacePrefix) {
		String attributeName = elementName;
		if (namespacePrefix != null && namespacePrefix.length() > 0) {
			attributeName = namespacePrefix + ":" + elementName;
		}
		return attributeName;
	}

	private String findMatchingCType(String name, TextContainer content, int closingTagCount) {
		for (int j = content.getParts().size() - 1; j >= 0; j--) {
			TextPart textPart = content.getParts().get(j);
			if (textPart.isSegment()) {
				continue;
			}
			List<Code> codes = textPart.getContent().getCodes();
			for (int i = codes.size() - 1; i >= 0; i--) {
				Code code = codes.get(i);
				if (code.getTagType() == TagType.CLOSING) {
					closingTagCount++;
					continue;
				}
				else if (code.getTagType() == TagType.OPENING) {
					if (closingTagCount > 0) {
						closingTagCount--;
						continue;
					}
				}
				if (!code.getOuterData().startsWith("<" + name)) {
					continue;
				}

				return code.getType();
			}
		}
		// no matching ctype found --> stick with the tag name
		return name;
	}

	// Save space by not repeating the id if not needed
	// only return originalId if it is not an integer, otherwise
	// Code.id holds the value
	private String getOriginalIdOrNull(String originalId) {
		try {
			Util.fastParseInt(originalId);
			return null;
		} catch (NumberFormatException e) {
			return originalId;
		}
	}

	private void addSdlCodeData(String id, Code c) {
		if (sdlTagDefs == null || sdlTagDefs.isEmpty()) {
			return;
		}

		if (sdlTagDefs.containsKey(id)) {
			SdlTagDef td = sdlTagDefs.get(id);
			if (!Util.isEmpty(td.equiv_text))
				c.setDisplayText(td.equiv_text);

			switch (c.getTagType()) {
			case OPENING:
				if (td.bpt != null) {
					c.setData(td.bpt.getData());
					c.setType(td.bpt.getType());
				} else {
					logger.debug("We have SDL Tag Defs and found the tag def, but no matching bpt data: {}", id);
				}
				break;

			case CLOSING:
				if (td.ept != null) {
					c.setData(td.ept.getData());
					c.setType(td.ept.getType());
				} else if (!Util.isEmpty(td.name)) {
					// there was a ept in the tagdef, but no content
					// <ept name="embed" word-end="false"/>
					c.setType(td.name);
				} else {
					logger.debug("We have SDL Tag Defs and found the tag def, but no matching ept data or name: {}", id);
				}
				break;

			case PLACEHOLDER:
				if (td.ph != null) {
					c.setData(td.ph.getData());
					c.setType(td.ph.getType());
				} else if (td.it != null) {
					c.setData(td.it.getData());
					c.setType(td.it.getType());
				} else if (td.st != null) {
					c.setData(td.st.getData());
					c.setType(td.st.getType());
				}else {
					logger.debug("We have SDL Tag Defs and found the tag def, but no matching ph/it data: {}", id);
				}
				break;

			default:
				break;
			}
		} else {
			logger.debug("We have SDL Tag Defs but could not find code id: {}", id);
		}
	}

	private String buildStartCode () {
		return buildStartCode(xliffReader);
	}

	public String buildStartCode (XMLStreamReader reader) {
		String prefix = reader.getPrefix();
		StringBuilder tmpg = new StringBuilder();
		boolean isMrk = false;
		if (( prefix != null ) && ( prefix.length()>0 )) {
			tmpg.append("<"+prefix+":"+reader.getLocalName());
		}
		else {
			tmpg.append("<"+reader.getLocalName());
			isMrk = "mrk".equals(reader.getLocalName());
		}
		int count = reader.getNamespaceCount();
		for ( int i=0; i<count; i++ ) {
			prefix = reader.getNamespacePrefix(i);
			tmpg.append(" xmlns");
			if (!Util.isEmpty(prefix))
				tmpg.append(":" + prefix);
			tmpg.append("=\"");
			tmpg.append(reader.getNamespaceURI(i));
			tmpg.append("\"");
		}
		count = reader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !reader.isAttributeSpecified(i) ) continue; // Skip defaults
			prefix = reader.getAttributePrefix(i);
			String attName = reader.getAttributeLocalName(i);
			if ( isMrk ) {
				if ( "comment".equals(attName) ) continue;
				if ( Namespaces.ITSXLF_NS_URI.equals(reader.getAttributeNamespace(i)) ) continue;
				if ( Namespaces.ITS_NS_URI.equals(reader.getAttributeNamespace(i)) ) continue;
				if ( Namespaces.XML_NS_URI.equals(reader.getAttributeNamespace(i)) ) {
					if ( "lang".equals(attName) || "preserve".equals(attName) ) continue;
				}
			}
			tmpg.append(" ");
			if ((prefix!=null) && (prefix.length()!=0))
				tmpg.append(prefix + ":");
			tmpg.append(attName);
			tmpg.append("=\"");
			tmpg.append(Util.escapeToXML(reader.getAttributeValue(i), 3, params.getEscapeGT(), null));
			tmpg.append("\"");
		}
		tmpg.append(">");
		return tmpg.toString();
	}

	private int retrieveId (int currentIdValue,
		String id,
		boolean useMinusOneasDefault,
		boolean useAutoMid)
	{
		if (( id == null ) || ( id.length() == 0 )) {
			if ( useAutoMid ) return --autoMid;
			else {
				logger.warn("Missing id attribute in inline code. An auto-id is used instead, but may not provide a proper source/target alignment.");
				return (useMinusOneasDefault ? -1 : ++currentIdValue);
			}
		}
		try {
			return Util.fastParseInt(id);
		}
		catch ( NumberFormatException e ) {
			// Falls back to the hash-code
			//TODO: At some point code id needs to support a string
			return id.hashCode();
		}
	}

	/**
	 * Appends a code, using the content of the node. Do not use for <g>-type tags.
	 * @param tagType The type of in-line code.
	 * @param id the id of the code to add.
	 * @param tagName the tag name of the in-line element to process.
	 * @param type the type of code (bpt and ept must use the same one so they can match!)
	 * @param store true if we need to store the data in the skeleton.
	 * @param content the object where to put the code.
	 * @return the code that was appended.
	 */
	private Code appendCode (TagType tagType,
		int id,
		String tagName,
		String type,
		boolean store,
		TextFragment content)
	{
		try {
			int endStack = 1;
			StringBuilder innerCode = new StringBuilder();
			StringBuilder outerCode = new StringBuilder();;
			String tagPrefix = xliffReader.getPrefix();
			if ( !Util.isEmpty(tagPrefix) ) {
				outerCode.append("<" + tagPrefix + ":" + tagName);
			}
			else {
				outerCode.append("<" + tagName);
			}
			int count = xliffReader.getAttributeCount();
			String prefix;
			for ( int i=0; i<count; i++ ) {
				if ( !xliffReader.isAttributeSpecified(i) ) continue; // Skip defaults
				prefix = xliffReader.getAttributePrefix(i);
				outerCode.append(" ");
				if ( !Util.isEmpty(prefix) )
					outerCode.append(prefix + ":");
				outerCode.append(xliffReader.getAttributeLocalName(i));
				outerCode.append("=\"");
				outerCode.append(Util.escapeToXML(xliffReader.getAttributeValue(i), 3, params.getEscapeGT(), null));
				outerCode.append("\"");
			}
			outerCode.append(">");
			boolean handleSubAsTu = params.getSubAsTextUnit();
			boolean inSub = false;
			boolean hasSub = false;

			int eventType;
			int subIndex = 0;
			while ( xliffReader.hasNext() ) {
				eventType = xliffReader.next();
				switch ( eventType ) {
				case XMLStreamConstants.START_ELEMENT:
					if ( inSub ) {
						// Should not occur
						throw new OkapiException("Unexpected state in processing sub.");
					}
					if ( store ) storeStartElement(false, false, false);
					else annotatorsRef.readAndPush();

					if ( !inSub && xliffReader.getLocalName().equals("sub") ) {
						inSub = true;
					}
					else if ( tagName.equals(xliffReader.getLocalName()) ) {
						endStack++; // Take embedded elements into account
					}

					String tmpg = buildStartCode();
					if ( !inSub) innerCode.append(tmpg.toString());
					outerCode.append(tmpg.toString());

					if ( inSub ) {
						// Store the inner/out codes before the subflow text
						if ( !handleSubAsTu ) {
							Code code = content.append(tagType, type, innerCode.toString(), id);
							code.setOuterData(outerCode.toString());
						}
						final String subId = "okp-" + tuId + "-" + tagTypeToChr(tagType) +  id + "-" + subIndex;
						List<Object> chunks = processSub(xliffReader, handleSubAsTu, subId);
						if ( !handleSubAsTu ) {
							for (Object obj : chunks) {
								if ( obj instanceof String ) {
									content.append((String) obj);
								} else if ( obj instanceof Code ) {
									content.append((Code) obj);
								}
							}
							innerCode.setLength(0);
							outerCode.setLength(0);
						} else {
							outerCode.append(TextFragment.makeRefMarker(subId));
							appendSubTu(chunks, subId);
						}
						outerCode.append("</sub>");
						subIndex++;
						inSub = false;
						hasSub = true;
					}
					break;

				case XMLStreamConstants.END_ELEMENT:
					if ( inSub ) { // Should not occur
						throw new OkapiException("Unexpected state in processing sub.");
					}
					if ( store ) storeEndElement();
					else annotatorsRef.pop();

					prefix = xliffReader.getPrefix();
					if ( tagName.equals(xliffReader.getLocalName()) ) {
						if ( --endStack == 0 ) {
							// Use extraId if the code had a sub element
							Code code = content.append(tagType, type, innerCode.toString(), (hasSub && !handleSubAsTu ? --extraId : id));
							if ( !hasSub && ( innerCode.length() == 0 )) {
								// Replace '>' by '/>'
								outerCode.insert(outerCode.length()-1, '/');
							}
							else if ( !Util.isEmpty(prefix) ) {
								outerCode.append("</" + prefix + ":" + tagName + ">");
							}
							else {
								outerCode.append("</" + tagName + ">");
							}
							code.setOuterData(outerCode.toString());
							code.setReferenceFlag(handleSubAsTu && hasSub);
							return code;
						}
						// Else: fall thru
					}
					// Else store the close tag in the outer code
					if ( Util.isEmpty(prefix) ) {
						innerCode.append("</"+xliffReader.getLocalName()+">");
						outerCode.append("</"+xliffReader.getLocalName()+">");
					}
					else {
						innerCode.append("</"+prefix+":"+xliffReader.getLocalName()+">");
						outerCode.append("</"+prefix+":"+xliffReader.getLocalName()+">");
					}
					break;

				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.SPACE:
					innerCode.append(xliffReader.getText());//TODO: escape unsupported chars
					outerCode.append(Util.escapeToXML(xliffReader.getText(), 0, params.getEscapeGT(), null));
					if ( store ) {
						skel.append(Util.escapeToXML(xliffReader.getText(), 0, params.getEscapeGT(), null));
					}
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
		return null; // Not used as the exit is in the loop.
	}

	private char tagTypeToChr(TagType tagType) {
		switch ( tagType ) {
		case CLOSING:
			return 'c';
		case OPENING:
			return 'o';
		case PLACEHOLDER:
			return 'p';
		}
		return '_';
	}

	private void appendSubTu(List<Object> chunks, String tuId) {
		TextFragment subTuContent = new TextFragment();
		for (Object obj : chunks) {
			if ( obj instanceof String ) {
				subTuContent.append((String) obj);
			} else if ( obj instanceof Code ) {
				subTuContent.append((Code) obj);
			}
		}
		boolean addedToExistingTu = false;
		if ( sourceDone ) {
			// in target: find the appropiate TU from the source to add the sub flow to
			ListIterator<Event> lit = queue.listIterator(queue.size());
			while ( lit.hasPrevious() ) {
				Event event = lit.previous();
				if ( event.getEventType() == EventType.TEXT_UNIT && event.getResource().getId().equals(tuId) ) {
					TextUnit tu = (TextUnit) event.getResource();
					tu.setTarget(trgLang, new TextContainer(subTuContent));
					addedToExistingTu = true;
					break;
				}
			}
		}
		if ( !addedToExistingTu ) {
			TextUnit tu = new TextUnit(tuId, null, true);
			tu.getSource().setContent(subTuContent);
			tu.setReferenceCount(2); // source and clone for target - there is likely a better way to do this (?)
			queue.add(new Event(EventType.TEXT_UNIT, tu));
		}
	}

	/**
	 * Process the content of a sub element. It assumes the content is valid.
	 *
	 * @param reader - XmlStreamReader passed in. We assume
	 * @return a list of strings and codes corresponding to the text/code chunks processed.
	 * @throws XMLStreamException if a read error occurs.
	 */
	public List<Object> processSub(XMLStreamReader reader) throws XMLStreamException {
		return processSub(reader, false, null);
	}

	private List<Object> processSub(XMLStreamReader reader, boolean handleSubAsTu, String parentId)
			throws XMLStreamException {
		List<Object> chunks = new ArrayList<>();
		boolean inText = true;
		StringBuilder buf = new StringBuilder();
		StringBuilder bufOuter = new StringBuilder();
		int eventType;
		int subIndex = 0;
		while ( reader.hasNext() ) {
			eventType = reader.next();
			switch ( eventType ) {
			case XMLStreamConstants.START_ELEMENT:
				if ( inText ) { // Only inline code elements should occur when inside a sub
					// Save the previous text and reset the buffer
					chunks.add(buf.toString()); buf.setLength(0);
					// Switch mode to non-text
					inText = false;
					// Store the start tag
					bufOuter.append(buildStartTag());
				}
				else { // In code mode, only sub should occur
					// Create the code for the previous code
					// Create the code
					if ( !handleSubAsTu ) {
						Code code = new Code(TagType.PLACEHOLDER, "x-ph", buf.toString());
						code.setId(--extraId);
						code.setOuterData(bufOuter.toString());
						chunks.add(code);
						buf.setLength(0); bufOuter.setLength(0);
						// Store the start tag for <sub>
						code = new Code(TagType.PLACEHOLDER, "x-ph", null);
						code.setId(--extraId);
						code.setOuterData(buildStartTag());
						// Process the content of this new sub recursively
						chunks.addAll(processSub(reader, handleSubAsTu, null));
						// We are still in code mode in this scope
						// Store the end tag for </sub> as outer code
						buf.setLength(0); bufOuter.setLength(0);
					} else {
						String tuId = parentId + "-" + subIndex;
						bufOuter.append(buildStartTag());
						bufOuter.append(TextFragment.makeRefMarker(tuId));
						List<Object> subChunks = processSub(reader, handleSubAsTu, tuId);
						appendSubTu(subChunks, tuId);
					}
					bufOuter.append("</sub>");
					subIndex++;
				}
				break;
			case XMLStreamConstants.END_ELEMENT:
				if ( inText ) { // Only a </sub> should occur in text mode
					// Create the text chunk
					chunks.add(buf.toString()); buf.setLength(0);
					return chunks;
				}
				else { // in non-text mode, this is the end of a code
					// Store end tag
					bufOuter.append("</"+reader.getLocalName()+">");
					// Create the code
					Code code = new Code(TagType.PLACEHOLDER, "x-ph", buf.toString());
					code.setId(--extraId);
					code.setOuterData(bufOuter.toString());
					chunks.add(code);
					// Switch mode and reset the buffers
					inText = true;
					buf.setLength(0); bufOuter.setLength(0);
				}
				break;

			case XMLStreamConstants.CHARACTERS:
			case XMLStreamConstants.CDATA:
			case XMLStreamConstants.SPACE:
				if ( inText ) {
					buf.append(reader.getText());
				}
				else { // In inner code
					String data = reader.getText();
					buf.append(data);
					bufOuter.append(Util.escapeToXML(data, 0, params.getEscapeGT(), null));
				}
				break;
			}
		}
		return null; // Should not occur
	}

	private String buildStartTag () {
		StringBuilder tmp = new StringBuilder("<");
		String prefix = xliffReader.getPrefix();
		if (( prefix == null ) || ( prefix.length()==0 )) {
			tmp.append(xliffReader.getLocalName());
		}
		else {
			tmp.append(prefix+":"+xliffReader.getLocalName());
		}
		int count = xliffReader.getNamespaceCount();
		for ( int i=0; i<count; i++ ) {
			prefix = xliffReader.getNamespacePrefix(i);
			tmp.append(" xmlns");
			if (!Util.isEmpty(prefix))
				tmp.append(":" + prefix);
			tmp.append("=\"");
			tmp.append(xliffReader.getNamespaceURI(i));
			tmp.append("\"");
		}
		count = xliffReader.getAttributeCount();
		for ( int i=0; i<count; i++ ) {
			if ( !xliffReader.isAttributeSpecified(i) ) continue; // Skip defaults
			prefix = xliffReader.getAttributePrefix(i);
			tmp.append(" ");
			if ((prefix!=null) && (prefix.length()!=0))
				tmp.append(prefix + ":");
			tmp.append(xliffReader.getAttributeLocalName(i));
			tmp.append("=\"");
			tmp.append(Util.escapeToXML(xliffReader.getAttributeValue(i), 3, params.getEscapeGT(), null));
			tmp.append("\"");
		}
		tmp.append(">");
		return tmp.toString();
	}

	private Note createNote() {
		Note n = new Note();
		try {
		    for (int i = 0; i < xliffReader.getAttributeCount(); i++) {
		    	switch ( xliffReader.getAttributeLocalName(i) ) {
					case "annotates":
						n.setAnnotates(Note.Annotates.fromString(xliffReader.getAttributeValue(i)));
						break;
					case "from":
						n.setFrom(xliffReader.getAttributeValue(i));
						break;
					case "priority":
						n.setPriority(Note.Priority.fromInt(Integer.parseInt(xliffReader.getAttributeValue(i))));
						break;
					default:
						break;
		    	}
		    }
	    } catch (IllegalArgumentException e) {
			logger.warn("XLIFF note attribute: {}", e.getMessage());
		}
	    return n;
	}

	private void processNote () {
		try {
			// Check the destination of the property
			String dest = getXliffAttributeValue("annotates");
			if ( dest == null ) dest = ""; // like 'general'
			NoteAnnotation notes = null;
			StringBuilder tmp = new StringBuilder();
			if ( dest.equals("source") ) {
				notes = tu.getSource().getAnnotation(NoteAnnotation.class);
			}
			else if ( dest.equals("target") ) {
				if (!tu.hasTarget(trgLang)) {
					// Rare case: note refers to non-existing target. we want to keep the note's text
					tu.setTarget(trgLang, new TextContainer());
				}
				notes = tu.getTarget(trgLang).getAnnotation(NoteAnnotation.class);
			}
			else {
				notes = tu.getAnnotation(NoteAnnotation.class);
			}
			if ( notes == null ) {
				notes = new NoteAnnotation();
			}

			Note aNote = createNote();

			// Get the content
			int eventType;
			while ( xliffReader.hasNext() ) {
				eventType = xliffReader.next();
				switch ( eventType ) {
				case XMLStreamConstants.CHARACTERS:
				case XMLStreamConstants.CDATA:
				case XMLStreamConstants.SPACE:
					tmp.append(xliffReader.getText());
					break;
				case XMLStreamConstants.END_ELEMENT:
					String name = xliffReader.getLocalName();
					if ( name.equals(NOTE) ) {
						aNote.setNoteText(tmp.toString());
						notes.add(aNote);
						if ( dest.equals("source") ) {
							tu.getSource().setAnnotation(notes);
						}
						else if ( dest.equals("target") ) {
							tu.getTarget(trgLang).setAnnotation(notes);
						}
						else {
							tu.setAnnotation(notes);
						}
						return;
					}
					// Else: This should be an error as note are text only.
					break;
				}
			}
		}
		catch ( XMLStreamException e) {
			throw new OkapiIOException(e);
		}
	}

	private boolean processStartGroup () throws IOException, XMLStreamException {
		// Check if it's a 'merge-trans' group (v1.2)
		String mergeTrans = getXliffAttributeValue("merge-trans");
		if ( mergeTrans != null && mergeTrans.equals("yes")) {
			// If it's a 'merge-trans' group we do not treat it as a normal group.
			// The group element was not generated by the extractor.
			storeStartElement(false, false, false);
			parentIds.push(null); // Was -1 when Id where numbers
			return false;
		}

		createGroupId("id");

		// Else: it's a structural group
		StartGroup group = new StartGroup(parentIds.peek(), groupId.getLastId());
		group.setSkeleton(skel);
		parentIds.push(groupId.getLastId());
		queue.add(new Event(EventType.START_GROUP, group));

		// Update the translate state for this new group
		// inherits from the parent group if needed
		group.setIsTranslatable(isTranslatable(getXliffAttributeValue("translate")));
		translateCtx.push(group.isTranslatable());

		// Get resname (can be null)
		String tmp = getXliffAttributeValue("resname");
		if ( tmp != null ) group.setName(tmp);
		else if ( params.getFallbackToID() ) {
			// Use the true original id that can be null
			group.setName(getXliffAttributeValue("id"));
		}

		// Get restype (can be null)
		group.setType(getXliffAttributeValue("restype"));

		addLengthConstraints(group);

		storeStartElementGroup(group);
		ITSProvenanceAnnotations prov = itsFilterHandler.readITSProvenance();
		ITSProvenanceAnnotations.addAnnotations(group, prov);
		preReadTo(XLIFFContextGroup.ELEMENT_NAME, Collections.emptyList());
		if (!XLIFFContextGroup.ELEMENT_NAME.equals(xliffReader.getLocalName())) {
			return true;
		}
		readContextGroupFor(group);
		return true;
	}

	private void createGroupId(final String attributeName) {
		// Try to get the existing id from the XLIFF group
		String grpId = getXliffAttributeValue(attributeName);
		if ((grpId == null) || (groupUsedIds.contains(grpId))) {
			// If it does not exists, or if it has been used already
			// we create a new id that is not a duplicate
			grpId = groupId.createIdNotInList(groupUsedIds);
		} else {
			groupId.setLastId(grpId);
		}
		// Update the list with the new used identifier
		groupUsedIds.add(grpId);
	}

	private void readContextGroupFor(final INameable nameable) throws XMLStreamException {
		final XLIFFContextGroup contextGroup = new XLIFFContextGroup(
			getEncoderManager(),
			xliffReader.getAttributeValue(null, XLIFFContextGroup.NAME),
			xliffReader.getAttributeValue(null, XLIFFContextGroup.PURPOSE),
			xliffReader.getAttributeValue(null, XLIFFContextGroup.CRC),
			new LinkedList<>()
		);
		contextGroup.readWith(xliffReader);
		nameable.setAnnotation(contextGroup);
		final GenericSkeleton skel = (GenericSkeleton) nameable.getSkeleton();
		if (null != skel) {
			skel.add(XLIFFSkeletonWriter.CONTEXT_GROUP_MARKER);
			skel.attachParent(nameable);
			skel.flushPart();
		}
	}

	/**
	 * Prereads while a specified start element name found or one of boundaries is met.
	 *
	 * If the boundaries are empty, reads to the first start or end element found
	 * (sets the {@link this.xliffPreRead} in this case as well).
	 *
	 * @param name       The start element name to read to
	 * @param boundaries The boundaries not to cross over
	 * @throws XMLStreamException
	 */
	private void preReadTo(final String name, final List<String> boundaries) throws XMLStreamException {
		while (xliffReader.hasNext()) {
			final int eventType = xliffReader.next();
			if (XMLStreamConstants.END_ELEMENT == eventType) {
				if (boundaries.isEmpty() || boundaries.contains(xliffReader.getLocalName())) {
					this.xliffPreRead = true;
					break;
				}
				storeEndElement();
			} else if (XMLStreamConstants.START_ELEMENT == eventType) {
				if (name.equals(xliffReader.getLocalName())) {
					return;
				}
				if (boundaries.isEmpty() || boundaries.contains(xliffReader.getLocalName())) {
					this.xliffPreRead = true;
					break;
				}
				storeStartElement(false, false, false);
			} else if (XMLStreamConstants.CHARACTERS == eventType) {
				skel.append(Util.escapeToXML(xliffReader.getText().replace("\n", lineBreak), 0, params.getEscapeGT(), null));
			} else if (XMLStreamConstants.SPACE == eventType
					|| XMLStreamConstants.CDATA == eventType) {
				skel.append(CDATA_START);
				skel.append(xliffReader.getText().replace("\n", lineBreak));
				skel.append(CDATA_END);
			} else if (XMLStreamConstants.COMMENT == eventType) {
				skel.append("<!--".concat(xliffReader.getText().replace("\n", lineBreak)).concat("-->"));
			} else if (XMLStreamConstants.PROCESSING_INSTRUCTION == eventType) {
				skel.append(
					"<?".concat(xliffReader.getPITarget()).concat(" ").concat(xliffReader.getPIData()).concat("?>")
				);
			}
			// skipping other types
		}
	}

	private boolean processEndGroup () {
		// Pop and checks the value for this group
		String id = parentIds.pop();
		if ( id == null ) {
			// This closes a 'merge-trans' non-structural group
			return false;
		}

		// Else: it's a structural group
		// Pop the translate context
		translateCtx.pop();
		// Create the ending
		Ending ending = new Ending(id);
		ending.setSkeleton(skel);
		queue.add(new Event(EventType.END_GROUP, ending));
		return true;
	}

	private boolean processStartBinUnit () throws XMLStreamException {
		storeStartElement(false, false, false);

		String grpId = getXliffAttributeValue("id");
		if ( grpId == null ) {
			throw new OkapiIllegalFilterOperationException("Missing attribute 'id'.");
		}
		if ( groupUsedIds.contains(grpId) ) {
			// If it does not exists, or if it has been used already
			// we create a new id that is not a duplicate
			grpId = groupId.createIdNotInList(groupUsedIds);
		}
		else {
			groupId.setLastId(grpId);
		}
		// Update the list with the new used identifier
		groupUsedIds.add(grpId);

		StartGroup group = new StartGroup(parentIds.peek().toString(), grpId);
		group.setSkeleton(skel);
		parentIds.push(groupId.getLastId());
		queue.add(new Event(EventType.START_GROUP, group));

		// Update the translate state for this new group
		// inherits from the parent group if needed
		group.setIsTranslatable(isTranslatable(getXliffAttributeValue("translate")));
		translateCtx.push(group.isTranslatable());

		// Get id for resname
		String tmp = getXliffAttributeValue("resname");
		if ( tmp != null ) group.setName(tmp);
		else if ( params.getFallbackToID() ) {
			group.setName(getXliffAttributeValue("id"));
		}

		// Get restype (can be null)
		group.setType(getXliffAttributeValue("restype"));

		preReadTo(XLIFFContextGroup.ELEMENT_NAME, Arrays.asList(BIN_UNIT, NOTE, TRANS_UNIT));
		if (!XLIFFContextGroup.ELEMENT_NAME.equals(xliffReader.getLocalName())) {
			return true;
		}
		readContextGroupFor(group);
		return true;
	}

	private boolean processEndBinUnit () {
		// Pop and checks the value for this group
		String id = parentIds.pop();
		// Pop the translate context
		translateCtx.pop();
		// Create the ending
		Ending ending = new Ending(id);
		ending.setSkeleton(skel);
		queue.add(new Event(EventType.END_GROUP, ending));
		return true;
	}

	private boolean isPreserveSpaceAttributeValue(String attrValue) {
        return params.isPreserveSpaceByDefault() || attrValue.equals("preserve");
	}

	/**
	 * Adds new {@link Property}s for length restriction attributes: maxwidth, size-unit, maxheight.
	 *
	 * @param resource the resource
	 */
	private void addLengthConstraints(INameable resource) {
		String maxWidth = getXliffAttributeValue("maxwidth");
		if (maxWidth != null) {
			Property property = new Property(Property.MAX_WIDTH, maxWidth);
			resource.setProperty(property);
		}

		String sizeUnit = getXliffAttributeValue("size-unit");
		if (sizeUnit != null) {
			Property property = new Property(Property.SIZE_UNIT, sizeUnit);
			resource.setProperty(property);
		}

		String maxHeight = getXliffAttributeValue("maxheight");
		if (maxHeight != null) {
			Property property = new Property(Property.MAX_HEIGHT, maxHeight);
			resource.setProperty(property);
		}
	}

	/**
	 * Gets the ITS annotators references context object.
	 * @return the ITS annotators references context object.
	 */
	public ITSAnnotatorsRefContext getAnnotatorsRefContext () {
		return annotatorsRef;
	}

	/**
	 * Returns true if the text unit is considered unsegmented.
	 * @param tu the text unit to check
	 * @param params xliff parameters
	 * @return if the text unit is considered unsegmented
	 */
	static boolean isUnsegmentedTextUnit(ITextUnit tu, Parameters params) {
		// Check for single segment with no <mrk> element if enabled in params
		return params.getSkipNoMrkSegSource()
				&& tu.getSource().getSegments().count() == 1
				&& !tu.getSkeleton().toString().contains("</mrk>");
	}

	/**
	 * Gets the value for a given XLIFF attribute, making sure we only look at the attributes in the XLIFF namespace.
	 * This might be explicit (<... xlf:id ...>) or inheriting the default namespace from the <xliff> element. 
	 * @param attributeName the name of the XLIFF attribute
	 * @return the value or null if not found.
	 */
	private String getXliffAttributeValue(String attributeName) {

		String result = null;

		// If the default namespace of <xliff> is "urn:oasis:...:1.*"
		// then the attribute with "" namespace means using the default one.
		// We should not do getAttributeValue(null, ...), as that means
		// "the namespace is not checked for equality", so we might pick up
		// attributes from random namespaces.
		String nsUri = xliffReader.getNamespaceURI();
		if (nsUri.isEmpty() || nsUri.startsWith(NS_XLIFF_PREFIX)) {
			result = xliffReader.getAttributeValue("", attributeName);
			if (result != null) {
				return result;
			}
		}

		// Attribute with explicit xliff namespace
		result = xliffReader.getAttributeValue(Namespaces.NS_XLIFF12, attributeName);
		if (result == null)
			result = xliffReader.getAttributeValue(Namespaces.NS_XLIFF11, attributeName);
		if (result == null)
			result = xliffReader.getAttributeValue(Namespaces.NS_XLIFF10, attributeName);

		if (result != null) { // Found one
			return result;
		}

		// We can be less strict and more backward-compatible if here we call
		//   xliffReader.getAttributeValue(null, attributeName);
		// But that would also be less correct. Let's see how it goes...
		return null;
	}

	private boolean attributeValueEquals(String attributeLocalName, String compareTo) {
		return compareTo.equals(xliffReader.getAttributeValue(null, attributeLocalName));
	}
}
