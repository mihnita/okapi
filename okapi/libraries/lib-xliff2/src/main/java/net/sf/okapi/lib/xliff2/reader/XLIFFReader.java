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

package net.sf.okapi.lib.xliff2.reader;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;
import net.sf.okapi.lib.xliff2.changeTracking.Item;
import net.sf.okapi.lib.xliff2.changeTracking.Revision;
import net.sf.okapi.lib.xliff2.changeTracking.Revisions;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.CanReorder;
import net.sf.okapi.lib.xliff2.core.Directionality;
import net.sf.okapi.lib.xliff2.core.ExtAttribute;
import net.sf.okapi.lib.xliff2.core.ExtAttributes;
import net.sf.okapi.lib.xliff2.core.ExtContent;
import net.sf.okapi.lib.xliff2.core.ExtElement;
import net.sf.okapi.lib.xliff2.core.ExtElements;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.IExtChild;
import net.sf.okapi.lib.xliff2.core.IWithChangeTrack;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithInheritedData;
import net.sf.okapi.lib.xliff2.core.IWithMetadata;
import net.sf.okapi.lib.xliff2.core.IWithNotes;
import net.sf.okapi.lib.xliff2.core.IWithValidation;
import net.sf.okapi.lib.xliff2.core.InheritedData;
import net.sf.okapi.lib.xliff2.core.InsingnificantPartData;
import net.sf.okapi.lib.xliff2.core.InsingnificantPartData.InsignificantPartType;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Notes;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.ProcessingInstruction;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Skeleton;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Store;
import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.TagType;
import net.sf.okapi.lib.xliff2.core.Tags;
import net.sf.okapi.lib.xliff2.core.TargetState;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.glossary.Definition;
import net.sf.okapi.lib.xliff2.glossary.GlossEntry;
import net.sf.okapi.lib.xliff2.glossary.Glossary;
import net.sf.okapi.lib.xliff2.glossary.Translation;
import net.sf.okapi.lib.xliff2.its.AnnotatorsRef;
import net.sf.okapi.lib.xliff2.its.DataCategories;
import net.sf.okapi.lib.xliff2.its.ITSReader;
import net.sf.okapi.lib.xliff2.its.TermTag;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.matches.Matches;
import net.sf.okapi.lib.xliff2.metadata.IWithMetaGroup;
import net.sf.okapi.lib.xliff2.metadata.Meta;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup.AppliesTo;
import net.sf.okapi.lib.xliff2.metadata.Metadata;
import net.sf.okapi.lib.xliff2.validation.Rule;
import net.sf.okapi.lib.xliff2.validation.Rule.Normalization;
import net.sf.okapi.lib.xliff2.validation.Rule.Type;
import net.sf.okapi.lib.xliff2.validation.Validation;

/**
 * Implements a stream-based and event-driven XLIFF reader.
 */
public class XLIFFReader implements Closeable {

	/**
	 * Performs as little validation as possible.
	 */
	public static final int VALIDATION_MINIMAL = 0x00;
	/**
	 * Performs as much validation as possible with the reader
	 * (which may not be a full validation for the modules, depending on the modules supported).
	 */
	public static final int VALIDATION_MAXIMAL = 0xFF;
	/**
	 * Includes schemas-based validation.
	 */
	public static final int VALIDATION_INCLUDE_SCHEMAS = 0x01;
	/**
	 * Includes validation of the fragment identification values.
	 */
	public static final int VALIDATION_INCLUDE_FRAGIDPREFIX = 0x02;

	private static final String NOTE_NS = Const.NS_XLIFF_CORE20+"_n";
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final SchemaValidator schValidator;
	private final LocationValidator locValidator;
	
	private XMLStreamReader reader;
	private LinkedList<Event> queue;
	private StartXliffData docData;
	private StartFileData startFileData;
	private boolean isMidFile;
	private boolean isInFile;
	private Stack<InheritedData> inheritedData;
	private Stack<String> groups;
	private Stack<URIContext> uriContext;
	private Stack<Validation> valContext;
	private URIParser uriParser;
	private boolean checkTargetOrder;
	private Unit unit;
	private List<CTag> srcIsolated;
	private List<CTag> trgIsolated;
	private Segment segment;
	private Part ignorable;
	private Stack<String> xmlAttributes; // Format: 'd|p' for xml:space, followed by xml:lang value
	private boolean reportUnsingnificantParts = false;
	private HashMap<String, Boolean> fileIds;
	private HashMap<String, Boolean> unitIds;
	private HashMap<String, Boolean> groupIds;
	private ArrayList<String> subFlowIds;
	private Stack<HashMap<String, ArrayList<String>>> specialIds;
	private int fileCount;
	private int warningCount;
	private int fileLevelUnitOrGroupCount;
	private ITSReader itsReader;

	// Internal class used to read the originalData element
	private static class DataElementContent {
		
		String content;
		Directionality dir;
		
		DataElementContent (String content,
			Directionality dir)
		{
			this.content = content;
			this.dir = dir;
		}
	}
	
	/**
	 * Creates a new XLIFFReader object with full validation.
	 */
	public XLIFFReader () {
		this(XLIFFReader.VALIDATION_MAXIMAL, null);
	}
	
	/**
	 * Creates a new XLIFFReader object.
	 * You can choose the type of validation to perform.
	 * @param validation one of the VALIDATION_* constants or a ORed combination.
	 */
	public XLIFFReader (int validation) {
		this(validation, null);
	}
	
	/**
	 * Creates a new XLIFFReader object.
	 * @param validation one of the VALIDATION_* constants or a ORed combination.
	 * @param uriParserToUse URI parser to use when processing references (e.g. value of ref in an &lt;mrk> element).
	 * If it is set to null, a URIParser object that handles default prefixes will be used.
	 */
	public XLIFFReader (int validation,
		URIParser uriParserToUse)
	{
		if ( (validation & VALIDATION_INCLUDE_SCHEMAS) == VALIDATION_INCLUDE_SCHEMAS ) {
			schValidator = new SchemaValidator();
			locValidator = new LocationValidator();
			locValidator.load(getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules.xml"));
		}
		else {
			schValidator = null;
			locValidator = null;
		}
		
		if ( uriParserToUse == null ) uriParser = new URIParser();
		else uriParser = uriParserToUse;
		// Set the validation option
		uriParser.setErrorOnUnknownPrefix((validation & VALIDATION_INCLUDE_FRAGIDPREFIX) == VALIDATION_INCLUDE_FRAGIDPREFIX);
	}
	
	/**
	 * Validates an XLIFF document passed as a File object.
	 * @param file the XLIFF document to validate.
	 */
	public static void validate (File file) {
		validate(null, file, null, null, null);
	}
	
	/**
	 * Validates an XLIFF document passed as a File object.
	 * @param file the XLIFF document to validate.
	 * @param uriParser the URI parser to use when processing references (can be null).
	 */
	public static void validate (File file,
		URIParser uriParser)
	{
		validate(uriParser, file, null, null, null);
	}
	
	/**
	 * Validates an XLIFF document passed as a URI.
	 * @param inputURI the URI to process.
	 * @param uriParser the URI parser to use when processing references (can be null).
	 */
	public static void validate (URI inputURI,
		URIParser uriParser)
	{
		validate(uriParser, null, inputURI, null, null);
	}
	
	/**
	 * Validates an XLIFF document passed as a string.
	 * @param input the content to process.
	 * @param uriParser the URI parser to use when processing references (can be null).
	 */
	public static void validate (String input,
		URIParser uriParser)
	{
		validate(uriParser, null, null, input, null);
	}
	
	/**
	 * Validates an XLIFF document passed as a stream.
	 * @param inputStream the stream to process.
	 * @param uriParser the URI parser to use when processing references (can be null).
	 */
	public static void validate (InputStream inputStream,
		URIParser uriParser)
	{
		validate(uriParser, null, null, null, inputStream);
	}
	
	private static void validate (URIParser uriParser,
		File file,
		URI uri,
		String string,
		InputStream stream)
	{
		try ( XLIFFReader reader = new XLIFFReader(VALIDATION_MAXIMAL, uriParser) ) {
			reader.open(file, uri, string, stream);
			while ( reader.hasNext() ) {
				reader.next();
			}
		}
	}
	
	/**
	 * Opens an XLIFF document by its File object.
	 * @param file the file to process.
	 */
	public void open (File file) {
		open(file, null, null, null);
	}
	
	/**
	 * Opens an XLIFF document by its URI.
	 * @param inputURI the URI to process.
	 */
	public void open (URI inputURI) {
		open(null, inputURI, null, null);
	}
	
	/**
	 * Opens an XLIFF document passed as a string.
	 * @param input the string containing the document to process.
	 */
	public void open (String input) {
		open(null, null, input, null);
	}
	
	/**
	 * Opens an XLIFF document by its stream.
	 * Note that if a schema validation is done, this call will create a temporary copy of the content.
	 * If you can use {@link #open(File)}, {@link #open(URI)} or {@link #open(String)}. 
	 * @param inputStream the input stream to process.
	 */
	public void open (InputStream inputStream) {
		open(null, null, null, inputStream);
	}
	
	/**
	 * Gets the input stream to process, and, if requested, perform the validation.
	 * One and only one of the arguments must not be null.
	 * @param file the File to process (or null).
	 * @param uri the URI to process (or null).
	 * @param string the String holding the content to process (or null).
	 * @param stream the InputStream to open (or null).
	 * @return the input stream to process (after reset if needed).
	 */
	private StreamSource validateAndGetInput (File file,
		URI uri,
		String string,
		InputStream stream)
	{
		StreamSource inputSource = null;
		try {
			if ( file != null ) {
				inputSource = new StreamSource(file); 
			}
			else if ( uri != null ) {
				inputSource = new StreamSource(new BufferedInputStream(uri.toURL().openStream()));
			}
			else if ( string != null ) {
				inputSource = new StreamSource(new StringReader(string));
			}
			else {
				if ( schValidator != null ) {
					// If we get directly a stream, we need to create some kind of copy of the content
					// to be able to process it twice (Note that the validator also closes the stream).
					inputSource = new StreamSource(createByteArrayInputStream(stream));
					
				}
				else { // If we need the stream only once, just use it.
					inputSource = new StreamSource(stream);
				}
			}
		}
		catch ( IOException e ) {
			error("Cannot create input stream from input. "+e.getLocalizedMessage());
		}
	
		if ( schValidator == null ) {
			return inputSource;
		}
		
		// Else: Do the validation
		schValidator.validate(inputSource);
		
		// Then reset the input for processing
		try {
			if ( file != null ) {
				inputSource = new StreamSource(file);
			}
			else if ( uri != null ) {
				inputSource = new StreamSource(new BufferedInputStream(uri.toURL().openStream()));
			}
			else if ( string != null ) {
				inputSource = new StreamSource(new StringReader(string));
			}
			else { // It's our temporary byte array input stream
				// We reset it
				inputSource.getInputStream().reset();
			}
		}
		catch ( IOException e ) {
			error("Cannot reset the input after schema validation. "+e.getLocalizedMessage());
		}
		
		return inputSource;
	}

	/**
	 * Creates a temporary byte array input stream.
	 * The close() method can be called in the validator and has no effect.
	 * @param inputStream the original input stream.
	 * @return the new input stream.
	 */
	private ByteArrayInputStream createByteArrayInputStream (InputStream inputStream) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			int nRead;
			byte[] data = new byte[10240];
			while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
				baos.write(data, 0, nRead);
			}
			baos.flush();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			bais.mark(Integer.MAX_VALUE);
			return bais;
		} catch ( IOException e ) {
			throw new XLIFFReaderException("Cannot create temporary input stream for schema validation.", e);
		}
	}
	
	/**
	 * Opens the document, and start by validating it if requested.
	 * One and only one of the arguments must not be null.
	 * @param file the File to process (or null).
	 * @param uri the URI to process (or null).
	 * @param string the String holding the content to process (or null).
	 * @param stream the InputStream to open (or null).
	 */
	private void open (File file,
		URI uri,
		String string,
		InputStream stream)
	{
		try {
			// Close any previous stream
			close();
			
			// Do the validation if needed and get the input
			StreamSource inputSource = validateAndGetInput(file, uri, string, stream);
			
			// Start the parsing
			XMLInputFactory fact = XMLInputFactory.newInstance();
			//TODO: Revisit the settings to all reporting of CDATA and document-trailing whitespace
			fact.setProperty(XMLInputFactory.IS_COALESCING, true);
			fact.setProperty(XMLInputFactory.SUPPORT_DTD, false);

			reader = fact.createXMLStreamReader(inputSource);
			groups = new Stack<>();
			
			inheritedData = new Stack<>();
			inheritedData.push(new InheritedData());
			
			uriContext = new Stack<>();
			uriContext.push(new URIContext());

			valContext = new Stack<>();
			valContext.push(new Validation());
			
			xmlAttributes = new Stack<>();
			xmlAttributes.push("d"); // Default is false
			
			queue = new LinkedList<>();
			queue.add(new Event(EventType.START_DOCUMENT, null));
		}
		catch ( XMLStreamException e ) {
			error("Cannot open the XLIFF stream. "+e.getMessage());
		}
	}
	
	/**
	 * Closes the document.
	 */
	@Override
	public void close () {
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
		}
		catch ( XMLStreamException e ) {
			error("Closing error. "+e.getMessage());
		}
	}

	/**
	 * Indicates if there is another event.
	 * @return true if there is another event, false otherwise.
	 */
	public boolean hasNext () {
		try {
			return reader.hasNext();
		}
		catch (XMLStreamException e) {
			error("Reading error. "+e.getMessage());
		}
		return false;
	}
	
	/**
	 * Gets the next filter event.
	 * @return the next filter event.
	 */
	public Event next () {
		if ( queue.isEmpty() ) {
			readNext();
		}
		if ( queue.peek().getType() == EventType.END_DOCUMENT ) {
			// Check stack of preserveWS: it should be 1
			if ( xmlAttributes.size() != 1 ) {
				warning(String.format("Stack for xml:space is at %d instead of 1.", xmlAttributes.size()));
			}
		}
		return queue.poll();
	}
	
	/**
	 * Sets the flag to report or not parts of the documents that are not significant.
	 * For example white space between structural elements.
	 * <p>By default those parts are not reported.
	 * @param reportUnsingnificantParts true to report those parts, false to not report them.
	 */
	public void setReportUnsingnificantParts (boolean reportUnsingnificantParts) {
		this.reportUnsingnificantParts = reportUnsingnificantParts;
	}

	/**
	 * Reads the next event.
	 */
	private void readNext () {
		MidFileData midFileData = null;
		StartGroupData startGroupData = null;
		
		try {
			String tmp, nsUri;
			while ( reader.hasNext() ) {
				int type = reader.next();
				switch ( type ) {
				case XMLStreamReader.START_DOCUMENT:
					// This event is not always triggered, so we trigger START_INPUT in open()
					break;
					
				case XMLStreamReader.END_DOCUMENT:
					queue.add(new Event(EventType.END_DOCUMENT, null));
					return;
					
				case XMLStreamReader.START_ELEMENT:
					tmp = reader.getLocalName();
					nsUri = reader.getNamespaceURI();
					pushXMLAttributes();
					switch (nsUri) {
						case Const.NS_XLIFF_CORE20:
							switch (tmp) {
								case Const.ELEM_UNIT:
									if (isMidFile) { // Make sure we always generate a MID_FILE event
										midFileData = ensureMidFileData(midFileData);
										isMidFile = false;
									}
									processUnit();
									return;
								case Const.ELEM_GROUP:
									if (isMidFile) { // Make sure we always generate a MID_FILE event
										midFileData = ensureMidFileData(midFileData);
										isMidFile = false;
									}
									startGroupData = processStartGroup();
									// If we have inherited validation rules we attached them now
									// If the group has its own rules they will overwrite these
									if (!valContext.peek().isEmpty()) {
										startGroupData.setValidation(valContext.peek());
									}
									// Return only when we reach the first unit/group within this group.
									break;
								case Const.ELEM_FILE:
									processStartFile();
									return;
								case Const.ELEM_XLIFF:
									processXliff();
									return;
								case Const.ELEM_SKELETON:
									processSkeleton();
									return;
								case Const.ELEM_NOTES:
									if (groups.isEmpty()) {
										if (isMidFile) {
											midFileData = ensureMidFileData(midFileData);
											processNotes(midFileData);
										} else { // Passed unit/group
											// No notes after units/groups
											error("Notes for a <file> must be before its first <unit> or <group>.");
										}
									} else { // The notes are for the current start-group
										if (startGroupData == null)
											error("Notes for a <group> must be before its first <unit> or <group>.");
										else processNotes(startGroupData);
									}
									break;
								default:
									error(String.format("Invalid element found: '%s'", tmp));
									break;
							}
							break;
						case Const.NS_XLIFF_METADATA20:
							if (groups.isEmpty()) {
								if (isMidFile) {
									midFileData = ensureMidFileData(midFileData);
									processMetadata(midFileData);
									continue;
								}
							} else {
								processMetadata(startGroupData);
								continue;
							}
							// Else: invalid
							error("Invalid extension and module element " + reader.getName().toString());
							break;
						case Const.NS_XLIFF_TRACKING20:
							if (groups.isEmpty()) {
								if (isMidFile) {
									midFileData = ensureMidFileData(midFileData);
									processChangeTracking(midFileData);
									continue;
								}
							} else {
								processChangeTracking(startGroupData);
								continue;
							}
							// Else: invalid
							error("Invalid extension and module element " + reader.getName().toString());
							break;
						case Const.NS_XLIFF_VALIDATION20:
							if (groups.isEmpty()) {
								if (isMidFile) {
									midFileData = ensureMidFileData(midFileData);
									processValidation(midFileData);
									continue;
								}
							} else {
								processValidation(startGroupData);
								continue;
							}
							// Else: invalid
							error("Invalid extension and module element " + reader.getName().toString());
							break;
						default:
							// Else: it's an extension or a module supported as an extension
							if (groups.isEmpty()) {
								if (isMidFile) {
									midFileData = ensureMidFileData(midFileData);
									processExtElement("file", midFileData.getExtElements());
								} else if (isInFile) { // End of file
									error("Extension and module elements of a <file> must be before its first <unit> or <group>.");
								} else { // Outside a file: in the xliff element
									error("No element allowed outside a <file> element.");
								}
							} else { // Extension or modules for the current group
								processExtElement("group", startGroupData.getExtElements());
							}
							break;
					}
					break;
					
				case XMLStreamReader.END_ELEMENT:
					tmp = reader.getLocalName();
					nsUri = reader.getNamespaceURI();
					popXMLAttributes();
					if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
						if ( tmp.equals(Const.ELEM_GROUP) ) {
							// Don't test groups as empty groups are allowed
							queue.add(new Event(EventType.END_GROUP, null));
							groups.pop();
							popInheritedData();
							uriContext.pop();
							// then we pop the validation context
							valContext.pop();
							popSpecialIds();
							return;
						}
						if ( tmp.equals(Const.ELEM_FILE) ) {
							if ( fileLevelUnitOrGroupCount < 1 ) {
								error("There must be at least one <unit> or <group> in a <file>.");
							}
							checkSubFlows();
							queue.add(new Event(EventType.END_FILE, null));
							isInFile = false;
							popInheritedData();
							uriContext.pop();
							valContext.pop();
							popSpecialIds();
							return;
						}
						if ( tmp.equals(Const.ELEM_XLIFF) ) {
							// Check if we have at least one file
							if ( fileCount < 1 ) {
								error("There must be at least one <file> per XLIFF document.");
							}
							queue.add(new Event(EventType.END_XLIFF, uriContext.peek()));
							return;
						}
					}
					break;

//TODO: insignificant data causes issue for end-group and end-file events					
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.SPACE:
					if ( reportUnsingnificantParts ) {
						queue.add(new Event(EventType.INSIGNIFICANT_PART,
							uriContext.peek(),
							new InsingnificantPartData(InsignificantPartType.TEXT, reader.getText())));
						if ( startGroupData == null ) return;
					}
					break;
				case XMLStreamReader.COMMENT:
					if ( reportUnsingnificantParts ) {
						queue.add(new Event(EventType.INSIGNIFICANT_PART,
							uriContext.peek(),
							new InsingnificantPartData(InsignificantPartType.COMMENT,
								"<!--"+reader.getText()+"-->")));
						if ( startGroupData == null ) return;
					}
					break;
				case XMLStreamReader.PROCESSING_INSTRUCTION:
					if ( reportUnsingnificantParts ) {
						queue.add(new Event(EventType.INSIGNIFICANT_PART,
							uriContext.peek(),
							new InsingnificantPartData(InsignificantPartType.PI,
								"<?"+reader.getText()+"?>")));
						if ( startGroupData == null ) return;
					}
					break;
				}
			}
		}
		catch ( XMLStreamException e ) {
			error("Reading error. "+e.getMessage());
		}
	}

	/**
	 * Create a MidFileData object (and initializes it) the given one is null.
	 * @param current the current MidFileData object.
	 * @return the new or existing MidFileData object.
	 */
	private MidFileData ensureMidFileData (MidFileData current) {
		if ( current == null ) {
			current = new MidFileData();
			queue.add(new Event(EventType.MID_FILE, uriContext.peek(), current));
		}
		return current;
	}
	
	private void checkSubFlows () {
		for ( String id : subFlowIds ) {
			if ( !unitIds.containsKey(id) ) {
				error(String.format("There is a reference to a subFlow id='%s' with no corresponding <unit>.", id));
			}
		}
	}
	
	private void pushXMLAttributes () {
		// Default is inherited
		String newStates = xmlAttributes.peek();
		// Update xml:space if present
		String tmp = reader.getAttributeValue(Const.NS_XML, "space");
		if ( !Util.isNoE(tmp) ) {
			newStates = (tmp.equals("preserve") ? "p" : "d")+newStates.substring(1);
		}
		// Update xml:lang is present
		tmp = reader.getAttributeValue(Const.NS_XML, "lang");
		if ( !Util.isNoE(tmp) ) {
			String msg;
			if ( (msg = Util.validateLang(tmp)) != null ) {
				error(String.format("The xml:lang value '%s' is invalid.\n"+msg, tmp));
			}
			newStates = ""+newStates.charAt(0)+tmp;
		}
		// Push new states
		xmlAttributes.push(newStates);
	}
	
	private void popXMLAttributes () {
		xmlAttributes.pop();
	}
	
	private void processXliff () {
		// Get version
		String version = reader.getAttributeValue("", Const.ATTR_VERSION);
		cannotBeNullOrEmpty(Const.ATTR_VERSION, version);
		if ( !version.startsWith("2.") ) {
			error(String.format("Not a XLIFF 2.x document (version='%s').", version));
		}
		docData = new StartXliffData(version);

		// Get srcLang
		String value = reader.getAttributeValue("", Const.ATTR_SRCLANG);
		cannotBeNullOrEmpty(Const.ATTR_SRCLANG, value);
		docData.setSourceLanguage(value);

		// Get the namespaces
		docData.setExtAttributes(gatherNamespaces(null));
		
		// Get other attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String ns = reader.getAttributeNamespace(i);
			String locName = reader.getAttributeLocalName(i);
			value = reader.getAttributeValue(i);
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case Const.ATTR_VERSION:
						continue;
					case Const.ATTR_SRCLANG:
						continue;
					case Const.ATTR_TRGLANG:
						cannotBeEmpty(Const.ATTR_TRGLANG, value);
						docData.setTargetLanguage(value);
						break;
					default:  // Invalid attribute in core namespace
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			else { // Other namespaces than the core -> extension attributes
				addExtAttribute(docData, i, false);
			}
		}
		
		fileIds = new HashMap<>();
		fileCount = 0;
		warningCount = 0;
		isInFile = false;
		queue.add(new Event(EventType.START_XLIFF, uriContext.peek(), docData));
	}
	
	/**
	 * Gets the number of warnings.
	 * @return the number of warnings.
	 */
	public int getWarningCount () {
		return warningCount;
	}
	
	private void error (String message) {
		reportIssue(message, false);
	}

	private void warning (String message) {
		warningCount++;
		reportIssue(message, true);
	}

	private void reportIssue (String message,
		boolean warningOnly)
	{
		String fpart = "";
		if ( fileCount > 0 ) {
			fpart = "Error in <file> ";
			if ( startFileData != null ) {
				fpart = fpart + String.format("id='%s'", startFileData.getId());
			}
			else {
				fpart = String.format("number %d", fileCount);
			}
			if ( unit != null ) {
				fpart += String.format(", <unit> id='%s'", unit.getId());
			}
			else if ( !groups.isEmpty() ) {
				if ( groups.peek() == null ) {
					fpart += String.format(", <group> level %d", groups.size());
				}
				else {
					fpart += String.format(", <group> id='%s'", groups.peek());
				}
			}
			if (( reader != null ) && reader.hasName() ) {
				QName qn = reader.getName();
				if ( qn != null ) {
					fpart += "\nLast element read: '"+qn.toString()+"'";
				}
			}
			fpart += ":\n";
		}
		if ( warningOnly ) {
			logger.warn(fpart+message);
		}
		else {
			throw new XLIFFReaderException(fpart+message);
		}
	}

	private void processStartFile () {
		fileCount++;
		fileLevelUnitOrGroupCount = 0;
		
		startFileData = new StartFileData(null);
		isMidFile = true; // In case there is no skeleton
		isInFile = true;
		unitIds = new HashMap<>(); // Reset list of used ids for units
		groupIds = new HashMap<>(); // Reset list of used ids for groups
		subFlowIds = new ArrayList<>();
		specialIds = new Stack<>();
		itsReader = new ITSReader(reader);

		readAndPushInheritedAttributes(startFileData);
		uriContext.push(uriContext.peek().clone());
		valContext.push(new Validation(valContext.peek(), true));
		pushSpecialIds();
		if ( locValidator != null ) locValidator.reset();
		
		// Get the namespaces
		startFileData.setExtAttributes(gatherNamespaces(startFileData.getExtAttributes()));

		// Get other attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String ns = reader.getAttributeNamespace(i);
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case Const.ATTR_CANRESEGMENT:
						continue;
					case Const.ATTR_TRANSLATE:
						continue;
					case Const.ATTR_SRCDIR:
						continue;
					case Const.ATTR_TRGDIR:
						continue;
					case Const.ATTR_ID:
						cannotBeNullOrEmpty(Const.ATTR_ID, value);
						// Verify it is unique within the document
						if (fileIds.containsKey(value)) {
							error(String.format("The value '%s' is used as the id of two or more <file> elements.", value));
						} else {
							fileIds.put(value, false);
						}
						uriContext.peek().setFileId(value);
						startFileData.setId(value);
						break;
					case Const.ATTR_ORIGINAL:
						cannotBeEmpty(Const.ATTR_ORIGINAL, value);
						startFileData.setOriginal(value);
						break;
					default:  // Invalid attribute in core namespace
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			//TODO ITS ???
			else { // Other namespaces than the core -> extension attributes
				addExtAttribute(startFileData, i, false);
			}
		}

		// Check id
		if ( startFileData.getId() == null ) {
			error("The <file> element must have an id.");
		}
		// We are done
		queue.add(new Event(EventType.START_FILE, uriContext.peek(), startFileData));
	}
	
	private void pushSpecialIds () {
		HashMap<String, ArrayList<String>> ids = new HashMap<>();
		specialIds.push(ids);
	}
	
	private void popSpecialIds () {
		specialIds.pop();
	}
	
	private void checkAndAddSpecialId (String idNs,
		String id)
	{
		// Do not check uniqueness for the Core
		// Notes should be checked, but they are associated different temporary namespace URI
		if ( idNs.equals(Const.NS_XLIFF_CORE20) ) return;
		
		HashMap<String, ArrayList<String>> ids = specialIds.peek();
		ArrayList<String> list = ids.get(idNs);
		if ( list != null ) {
			if ( list.contains(id) ) {
				// Duplicate: throw an error
				if ( idNs.equals(NOTE_NS) ) error(String.format("Duplicate id '%s' for a <note> element.", id));
				else error(String.format("Duplicate id '%s' for the module or extension '%s'.", id, idNs));
			}
		}
		else {
			list = new ArrayList<>();
			ids.put(idNs, list);
		}
		list.add(id);
	}

	private void processSkeleton ()
		throws XMLStreamException
	{
		boolean hasHref = false;
		Skeleton skelData = new Skeleton();
		QName qn = new QName(Const.NS_XLIFF_CORE20, Const.ELEM_SKELETON);
		
		// Process attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			// Element is in XLIFF name space so attribute not prefixed are XLIFF
			if ( Util.isNoE(reader.getAttributeNamespace(i)) ) {
				if ( locName.equals(Const.ATTR_HREF) ) {
					if ( cannotBeEmpty(Const.ATTR_HREF, value) ) {
						skelData.setHref(value);
						hasHref = true;
					}
				}
				else {
					error(String.format("Invalid attribute '%s' in <skeleton>.", locName));
				}
			}
			else {
				error(String.format("Invalid attribute '%s' in <skeleton>.", locName));
			}
		}

		// Process the content
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				if ( hasHref ) {
					error("You cannot have both an href attribute and a content in <skeleton>.");
				}
				pushXMLAttributes();
				skelData.addChild(readExtElement());
				break;
			case XMLStreamReader.END_ELEMENT:
				if ( reader.getName().equals(qn) ) {
					popXMLAttributes();
					// Validate the skeleton data
					if ( hasHref ) { // Has href: skeleton must be empty
						if (( skelData.getChildren() != null ) && ( !skelData.getChildren().isEmpty() )) {
							error("You must not have a content in <skeleton> if there is an href attribute.");
						}
					}
					else { // No href: skeleton must not be empty
						if (( skelData.getChildren() == null ) || ( skelData.getChildren().isEmpty() )) {
							error("You must have a content in <skeleton> if there is no href attribute.");
						}
					}
					queue.add(new Event(EventType.SKELETON, uriContext.peek(), skelData));
					return;
				}
				break;
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.SPACE:
				skelData.addChild(new ExtContent(reader.getText()));
				break;
			case XMLStreamReader.CDATA:
				skelData.addChild(new ExtContent(reader.getText(), true));
				break;
			case XMLStreamReader.PROCESSING_INSTRUCTION:
				skelData.addChild(new ProcessingInstruction("<?"+reader.getPITarget()+" "+reader.getPIData()+"?>"));
				break;
			case XMLStreamReader.COMMENT:
				//TODO: remember comments
				break;
			}
		}
	}

	private void readAndPushInheritedAttributes (IWithInheritedData object) {
		// Create the new context with inherited values
		InheritedData indat = new InheritedData(inheritedData.peek());
		
		// Get translate if present
		String tmp = reader.getAttributeValue("", Const.ATTR_TRANSLATE);
		if ( canBeYesOrNo(Const.ATTR_TRANSLATE, tmp) ) {
			indat.setTranslate(tmp.equals(Const.VALUE_YES));
		}

		// Get canResegment if present
		tmp = reader.getAttributeValue("", Const.ATTR_CANRESEGMENT);
		if ( canBeYesOrNo(Const.ATTR_CANRESEGMENT, tmp) ) {
			indat.setCanResegment(tmp.equals(Const.VALUE_YES));
		}

		// Get source directionality if present
		tmp = reader.getAttributeValue("", Const.ATTR_SRCDIR);
		if ( canBeAutoOrLtrOrRtl(Const.ATTR_SRCDIR, tmp) ) {
			switch ( tmp ) {
			case Const.VALUE_AUTO:
				indat.setSourceDir(Directionality.AUTO);
				break;
			case Const.VALUE_LTR:
				indat.setSourceDir(Directionality.LTR);
				break;
			case Const.VALUE_RTL:
				indat.setSourceDir(Directionality.RTL);
				break;
			}
		}
		
		// Get target directionality if present
		tmp = reader.getAttributeValue("", Const.ATTR_TRGDIR);
		if ( canBeAutoOrLtrOrRtl(Const.ATTR_TRGDIR, tmp) ) {
			switch ( tmp ) {
			case Const.VALUE_AUTO:
				indat.setTargetDir(Directionality.AUTO);
				break;
			case Const.VALUE_LTR:
				indat.setTargetDir(Directionality.LTR);
				break;
			case Const.VALUE_RTL:
				indat.setTargetDir(Directionality.RTL);
				break;
			}
		}
		
		AnnotatorsRef ar = itsReader.readAnnotatorsRef(false, indat.getAnnotatorsRef());
		if ( ar != null ) indat.setAnnotatorsRef(ar);
		
		object.setInheritableData(indat);
		inheritedData.push(indat);
	}
	
	private void popInheritedData () {
		inheritedData.pop();
	}
	
	/**
	 * Processes the start of a group. When this method returns the event queue is set with the
	 * proper event, but the return to the caller should be done only when the first unit/group 
	 * element inside this group is reached (so extensions, modules, notes can be attached to 
	 * this start of group).
	 * @return the data for this start of group.
	 */
	private StartGroupData processStartGroup () {
		// Get the Id if present
		String tmp = reader.getAttributeValue("", Const.ATTR_ID);

		// Update the current unit/group count
		if ( groups.isEmpty() ) fileLevelUnitOrGroupCount++;
		// Push the new group
		groups.push(tmp);

		// Process id
		mustBeValidNmtoken(Const.ATTR_ID, tmp, false);
		// Check if it's unique
		if ( groupIds.containsKey(tmp) ) {
			error(String.format("Duplicated group id value '%s' detected.", tmp));
		}
		else { // Remember the id
			groupIds.put(tmp, null);
		}
		// Push the group context and set the new group id
		uriContext.push(uriContext.peek().clone());
		uriContext.peek().setGroupId(tmp);
		pushSpecialIds();
		valContext.push(new Validation(valContext.peek(), true));
		if ( locValidator != null ) locValidator.reset();
		
		StartGroupData sgd = new StartGroupData(tmp);

		readAndPushInheritedAttributes(sgd);

		// Get the namespaces
		sgd.setExtAttributes(gatherNamespaces(sgd.getExtAttributes()));
		
		// Process other attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);

			// Element is in XLIFF namespace so attribute not prefixed are XLIFF
			if ( Util.isNoE(ns) ) {
				// Skip attributes already processed
				switch (locName) {
					case Const.ATTR_ID:
						continue;
					case Const.ATTR_TRANSLATE:
						continue;
					case Const.ATTR_CANRESEGMENT:
						continue;
					case Const.ATTR_SRCDIR:
						continue;
					case Const.ATTR_TRGDIR:
						continue;
				}
				// Process other attributes
				if ( locName.equals(Const.ATTR_NAME) ) {
					if ( cannotBeEmpty(Const.ATTR_NAME, value) ) {
						sgd.setName(value);
					}
				}
				else if ( locName.equals(Const.ATTR_TYPE) ) {
					if ( cannotBeEmpty(Const.ATTR_TYPE, value) ) {
						sgd.setType(value);
					}
				}
				else { // Invalid attribute in core namespace
					error(String.format("Invalid attribute '%s'.", locName));
				}
			}
			else { // Other namespaces than the core -> extension attributes
				addExtAttribute(sgd, i, false);
			}
		}
		
		// We are done
		queue.add(new Event(EventType.START_GROUP, uriContext.peek(), sgd));
		return sgd;
	}

	private int checkIntegerValue (String name,
		String value,
		int min,
		int max)
	{
		if ( value == null ) return -1;
		if ( value.isEmpty() ) {
			error(String.format("Empty attribute '%s'", name));
		}
		try {
			int tmp = Integer.parseInt(value);
			if (( tmp < min ) || ( tmp > max )) {
				error(String.format("Invalid value for attribute '%s'", name));
			}
			return tmp;
		}
		catch ( NumberFormatException e ) {
			error(String.format("Invalid syntax for attribute '%s'", name));
		}
		return -1;
	}
	
	private void checkInlineIds (Unit unit) {
		HashMap<String, Integer> ids = new HashMap<>();
		// Check the segments and ignorable
		for ( Part part : unit ) {
			String id = part.getId();
			if ( id != null ) {
				if ( ids.containsKey(id) ) {
					error(String.format("The id '%s' is used incorrectly more than once in the unit id='%s'",
						id, unit.getId()));
				}
				else {
					ids.put(id, -1);
				}
			}
		}
		// Check the source markers
		Tags markers = unit.getStore().getSourceTags();
		for ( Tag bm : markers ) {
			if ( bm.getTagType() != TagType.CLOSING ) {
				String id = bm.getId();
				if ( ids.containsKey(id) ) {
					if ( ids.get(id) == -1 ) {
						// Already used by segment or ignorable
						error(String.format("The id '%s' is already used for a segment or an ignorable element in the unit id='%s'",
							id, unit.getId()));
					}
					else {
						// 0: Already used in the source
						error(String.format("The id '%s' is already used in the source content of the unit id='%s'",
							id, unit.getId()));
					}
				}
				else { // Allowed, and will be allowed once in the target
					ids.put(id, 0);
				}
			}
		}
		// Check the target markers
		markers = unit.getStore().getTargetTags();
		for ( Tag bm : markers ) {
			if ( bm.getTagType() != TagType.CLOSING ) {
				String id = bm.getId();
				if ( ids.containsKey(id) ) {
					switch ( ids.get(id) ) {
					case 0: // Allowed to re-use once for target (should not re-use again)
						ids.put(id, 1); 
						continue;
					case -1:
						// Used by a segment or ignorable: cannot re-use
						error(String.format("The id '%s' is already used for a segment or ignorable element in the unit id='%s'",
							id, unit.getId()));
						break;
					default: // 1: Already re-used once
						error(String.format("The id '%s' exists twice or more in the target content in the unit id='%s'",
							id, unit.getId()));
						break;
					}
				}
				else { // Target-only id: should be used only once
					ids.put(id, 1);
				}
			}
		}
	}
	
	private void checkTargetAndState (Unit unit) {
		for ( Part part : unit ) {
			if ( !part.isSegment() ) continue;
			Segment seg = (Segment)part;
			if ( seg.getState() != TargetState.INITIAL ) {
				if ( !seg.hasTarget() ) {
					warning("The state is not 'initial', but there is no <target> element.");
				}
				else if ( !seg.getSource().isEmpty() && seg.getTarget().isEmpty() ) {
					warning("The state is not 'initial' but the <target> is empty while the <source> is not.");
				}
			}
		}
	}
	
	/**
	 * Throws an exception if the value is null or empty.
	 * @param name name of the attribute.
	 * @param value value being checked.
	 */
	private void cannotBeNullOrEmpty (String name,
		String value)
	{
		if ( Util.isNoE(value) ) {
			error(String.format("Missing or empty attribute '%s'", name));
		}
	}
	
	private boolean mustBeValidNmtoken (String name,
		String value,
		boolean allowNull)
	{
		if ( value == null ) {
			if ( allowNull ) return false; // OK but no value
			error(String.format("Missing attribute '%s'", name));
		}
		if ( !Util.isValidNmtoken(value) ) {
			error(String.format("Value '%s' is not a valid NMTOKEN for '%s'.", value, name));
		}
		return true; // Value available
	}
	
	/**
	 * Gets the directionality from an attribute value.
	 * @param name the name of the attribute.
	 * @param value the value (can be null).
	 * @param defValue the default value to use if the value is null.
	 * @return the directionality for the attribute.
	 * @throws XLIFFReaderException if an error occurs.
	 */
	private Directionality getDirectionality (String name,
		String value,
		Directionality defValue)
	{
		if ( value == null ) return defValue;
		switch ( value ) {
		case Const.VALUE_AUTO:
			return Directionality.AUTO;
		case Const.VALUE_LTR:
			return Directionality.LTR;
		case Const.VALUE_RTL:
			return Directionality.RTL;
		}
		// Else: error
		error(String.format("Invalid attribute value for '%s' (must be '%s', '%s' or '%s')",
			name, Const.VALUE_AUTO, Const.VALUE_LTR, Const.VALUE_RTL));
		return Directionality.AUTO; // Never used
	}
	
	private boolean getYesOrNo (String name,
		String value,
		boolean defValue)
	{
		if ( value == null ) return defValue;
		if ( value.isEmpty() || ( !value.equals(Const.VALUE_YES) && !value.equals(Const.VALUE_NO) )) {
			error(String.format("Invalid attribute value for '%s' (must be '%s' or '%s')",
				name, Const.VALUE_YES, Const.VALUE_NO));
		}
		return value.equals(Const.VALUE_YES);
	}
	
	/**
	 * Throws an exception if the value is empty.
	 * @param name name of the attribute.
	 * @param value value to check.
	 * @return true if the value is not null. False for null value.
	 */
	private boolean cannotBeEmpty (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if ( value.isEmpty() ) {
			error(String.format("Empty attribute '%s'", name));
		}
		return true;
	}
	
	/**
	 * Checks if the given value is "yes" or "no"
	 * @param name the name of the attribute.
	 * @param value the value to check.
	 * @return true if either "yes" or "no" was the value,
	 * false if value was null.
	 * @throws XLIFFReaderException if there is an invalid value.
	 */
	private boolean canBeYesOrNo (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if ( value.isEmpty() || ( !value.equals(Const.VALUE_YES) && !value.equals(Const.VALUE_NO) )) {
			error(String.format("Invalid attribute value for '%s' (must be '%s' or '%s')",
				name, Const.VALUE_YES, Const.VALUE_NO));
		}
		return true;
	}
	
	private boolean canBeYesOrNoOrFirstNo (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		if ( value.isEmpty() ||
			( !value.equals(Const.VALUE_YES) && !value.equals(Const.VALUE_NO) && !value.equals(Const.VALUE_FIRSTNO) ))
		{
			error(String.format("Invalid attribute value for '%s' (must be '%s', '%s' or '%s')",
				name, Const.VALUE_YES, Const.VALUE_NO, Const.VALUE_FIRSTNO));
		}
		return true;
	}
	
	private boolean canBeAutoOrLtrOrRtl (String name,
		String value)
	{
		if ( value == null ) return false; // Allowed but nothing to set
		switch ( value ) {
		case Const.VALUE_AUTO:
		case Const.VALUE_LTR:
		case Const.VALUE_RTL:
			return true;
		}
		error(String.format("Invalid attribute value for '%s' (must be '%s', '%s' or '%s')",
			name, Const.VALUE_AUTO, Const.VALUE_LTR, Const.VALUE_RTL));
		return false;
	}
	
	private void processUnit ()
		throws XMLStreamException
	{
		// Update the current unit/group count
		if ( groups.isEmpty() ) fileLevelUnitOrGroupCount++;
			
		// New unit
		String tmp = reader.getAttributeValue("", Const.ATTR_ID);
		mustBeValidNmtoken(Const.ATTR_ID, tmp, false);
		// Check if it's unique
		if ( unitIds.containsKey(tmp) ) {
			error(String.format("Duplicated unit id value '%s' detected.", tmp));
		}
		else { // Remember the id
			unitIds.put(tmp, null);
		}
		// Create the new unit
		unit = new Unit(tmp);
		Map<String, DataElementContent> unitODM = null;
		srcIsolated = new ArrayList<>();
		trgIsolated = new ArrayList<>();
		
		// Push the new URI context and set the unit id
		uriContext.push(uriContext.peek().clone());
		uriContext.peek().setUnitId(tmp);
		valContext.push(new Validation(valContext.peek(), true));
		// If we have inherited validation rules we attached them now
		if ( !valContext.peek().isEmpty() ) {
			unit.setValidation(valContext.peek());
		}
		
		pushSpecialIds();
		
		// Get the inherited attributes
		readAndPushInheritedAttributes(unit);
		
		// Get the namespaces
		unit.setExtAttributes(gatherNamespaces(unit.getExtAttributes()));
		
		boolean needsITSFetch = itsReader.readAttributes(unit, unit, inheritedData.peek().getAnnotatorsRef());

		// Process other attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			
			// Element is in XLIFF namespace so attribute not prefixed are XLIFF
			if ( Util.isNoE(ns) ) {
				// Skip attributes already processed
				switch (locName) {
					case Const.ATTR_ID:
						continue;
					case Const.ATTR_TRANSLATE:
						continue;
					case Const.ATTR_CANRESEGMENT:
						continue;
					case Const.ATTR_SRCDIR:
						continue;
					case Const.ATTR_TRGDIR:
						continue;
				}
				
				// Process the other attributes
				if ( locName.equals(Const.ATTR_NAME) ) {
					if ( cannotBeEmpty(Const.ATTR_NAME, value) ) {
						unit.setName(value);
					}
				}
				else if ( locName.equals(Const.ATTR_TYPE) ) {
					if ( cannotBeEmpty(Const.ATTR_TYPE, value) ) {
						unit.setType(value);
					}
				}
				else { // Invalid attribute in core namespace
					error(String.format("Invalid attribute '%s'.", locName));
				}
			}
			else if ( ns.equals(Const.NS_ITSXLF) || ns.equals(Const.NS_ITS) ) {
				// Should have been handled when reading the ITS attributes
			}
			else { // Other namespaces than the core -> extension attributes
				addExtAttribute(unit, i, false);
			}
		}
		
		ExtElements unitExtElems = null;
		checkTargetOrder = false;
		String nsUri;
		boolean hasOneSegment = false;
		HashMap<String, Boolean> partIds = new HashMap<>();
		boolean inExtensionPoint = true;
		boolean notesAllowed = true;
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				pushXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					inExtensionPoint = false;
					switch (tmp) {
						case Const.ELEM_SEGMENT:
							hasOneSegment = true;
							notesAllowed = false;
							processPart(true, partIds);
							break;
						case Const.ELEM_IGNORABLE:
							notesAllowed = false;
							processPart(false, partIds);
							break;
						case Const.ELEM_ORIGINALDATA:
							unitODM = processOriginalData(); // unit-level original data

							break;
						case Const.ELEM_NOTES:
							if (notesAllowed) processNotes(unit);
							else error("Notes for a <unit> must be before its first <segment> or <ignorable>.");
							break;
						default:
							error(String.format("Unexpected element '%s' in <unit>", tmp));
							break;
					}
				}
				else {
					// Else: it's an extension or a module
					if ( inExtensionPoint ) {
						switch (nsUri) {
							case Const.NS_ITS:
								itsReader.readStandOffElements(tmp, unit, inheritedData.peek().getAnnotatorsRef());
								popXMLAttributes(); // Pop states for the initial elements

								break;
							case Const.NS_XLIFF_MATCHES20:
								processMatches();
								break;
							case Const.NS_XLIFF_GLOSSARY20:
								processGlossary();
								break;
							case Const.NS_XLIFF_METADATA20:
								processMetadata(unit);
								break;
							case Const.NS_XLIFF_VALIDATION20:
								processValidation(unit);
								break;
							case Const.NS_XLIFF_TRACKING20:
								processChangeTracking(unit);
								break;
							default:
								if (locValidator != null) locValidator.reset();
								unitExtElems = processExtElement("unit", unitExtElems);
								break;
						}
					}
					else {
						error(String.format("Invalid element '%s'. Extensions and modules must come before core elements.", tmp));
					}
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				popXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					if ( tmp.equals(Const.ELEM_UNIT) ) { // End of this unit
						// Attach the extension elements if any
						if ( unitExtElems != null ) {
							unit.setExtElements(unitExtElems);
						}
						// Check that we have at least one segment
						if ( !hasOneSegment ) {
							error("No <segment> in <unit>.");
						}
						if ( needsITSFetch ) {
							itsReader.fetchUnresolvedITSGroups(unit);
						}
						// Verify the inline IDs are unique
						checkInlineIds(unit);
						// Copy the potential original data stored outside
						copyOriginalDataToCodes(unit.getStore(), unitODM);
						// Check attributes consistency in opening/closing codes
						checkPairsConsistency(unit.getStore().getSourceTags());
						checkPairsConsistency(unit.getStore().getTargetTags());
						checkSourceTargetCorrespondence(unit.getStore().getSourceTags(),
							unit.getStore().getTargetTags());
						checkTargetOrder(unit);
						checkIsolatedAttributes(unit.getStore().getSourceTags(), srcIsolated);
						checkIsolatedAttributes(unit.getStore().getTargetTags(), trgIsolated);
						checkMatchReferences();
						checkGlossaryReferences();
						checkRevisions(unit);
						checkTargetAndState(unit);
						try {
							Util.validateCopyOf(unit);
							unit.verifyOpeningsBeforeClosings(false);
							unit.verifyOpeningsBeforeClosings(true);
							// Check the markers re-ordering between source and target
							if ( unit.doNonEmptySourcesHaveNonEmptyTargets() ) {
								Util.verifyReordering(unit.getOrderedCTags(false), unit.getOrderedCTags(true), true);
							}
							else { // Check for missing firstNo (without doing comparison)
								Util.createFixedSequences(unit.getOrderedCTags(false), true);
								Util.createFixedSequences(unit.getOrderedCTags(true), true);
							}
							unit.verifyReadOnlyTags();
						}
						catch ( Exception e ) {
							error(e.getMessage());
						}
						// Push to the queue
						queue.add(new Event(EventType.TEXT_UNIT, uriContext.peek(), unit));
						popInheritedData();
						uriContext.pop();
						valContext.pop();
						popSpecialIds();
						unit = null;
						return;
					}
				}
				break;

			}
		}
	}
	
	private void checkRevisions (IWithChangeTrack parent) {
		if ( !parent.hasChangeTrack() ) return;
		// Else: check the content
		for ( Revisions revs : unit.getChangeTrack() ) {
			String appliesTo = revs.getAppliesTo();
			if ( appliesTo == null ) {
				error(String.format("The element '%s' must have a '%s' attribute.",
					Revisions.TAG_NAME, Revisions.APPLIES_TO_ATTR_NAME));
			}
			String ref = revs.getRef();
			if ( ref != null ) {
				//TODO
			}
		}
	}
	private void checkMatchReferences () {
		if ( !unit.hasMatch() ) return;
		for ( Match match : unit.getMatches() ) {
			String ref = match.getRef();
			if ( unit.getSourceOrTargetReference(ref) == null ) {
				error(String.format("The ref value '%s' for <match> does not point to an existing span in it parent <unit>.", ref));
			}
		}
	}
	
	private void checkGlossaryReferences () {
		if ( !unit.hasGlossEntry() ) return;
		// The reference is allowed to be null for glossary entries
		for ( GlossEntry entry : unit.getGlossary() ) {
			String ref = entry.getRef();
			if (( ref != null ) && ( unit.getSourceOrTargetReference(ref) == null )) {
				error(String.format("The ref value '%s' for <glossEntry> does not point to an existing span in it parent <unit>.", ref));
			}
			for ( Translation trans : entry ) {
				ref = trans.getRef();
				if (( ref != null ) && ( unit.getSourceOrTargetReference(ref) == null )) {
					error(String.format("The ref value '%s' for <translation> does not point to an existing span in it parent <unit>.", ref));
				}
			}
		}
	}
	
	private void checkSourceTargetCorrespondence (Tags srcTags,
		Tags trgTags)
	{
		if ( srcTags == null ) return;
		if ( trgTags == null ) return;
		for ( Tag stag : srcTags ) {
			// Search for two tags with same ID
			String id = stag.getId();
			Tag ttag = trgTags.get(id, stag.getTagType());
			if ( ttag == null ) {
				// Maybe an error
				for ( Tag tag : trgTags ) {
					if ( tag.getId().equals(id) ) {
						error(String.format("The codes id='%s' are of different tag-types in source and target.", id));
					}
				}
			}
			// If we get here, that means a tag with the same ID does not exist in the target
			if ( ttag == null ) continue;
			// Otherwise:
			// if it is a code, we have already checked the tag-type
			// we just have to look at the marker case
			if ( stag.isMarker() ) {
				MTag smtag = (MTag)stag;
				MTag tmtag = (MTag)ttag;
				if ( !Util.equals(smtag.getTranslate(), tmtag.getTranslate()) ) {
					error(String.format("The translate value is different between source and target marker id='%s'.", id));
				}
				if ( !Util.equals(smtag.getType(), tmtag.getType()) ) {
					error(String.format("The translate value is different between source and target marker id='%s'.", id));
				}
				// Not checking the value attribute: It could change in target
				// Not checking the ref attribute: It could change in target
			}
		}
	}
		
	private void checkPairsConsistency (Tags tags) {
		// Start/end markers must have the same canCopy/canDelete/canReorder/canOverlap
		if ( tags == null ) return;
		for ( Tag tag : tags ) {
			if ( tag.getTagType() != TagType.OPENING ) continue;
			if ( tag.isMarker() ) { // Start marker
				Tag em = tags.getClosingTag(tag);
				if ( em == null ) {
					error(String.format("The <sm> for id='%s' has no corresponding <em/>.", tag.getId()));
				}
				continue; // No other checks for markers
			}
			CTag ctag = (CTag)tag;
			CTag closing = (CTag)tags.getClosingTag(ctag);
			if ( closing == null ) continue; // Isolated opening
			
			//--- start of checks normally not needed
			// For now we keep these checks
			// They should not been needed as the fields come from the same code-common object
			// but we keep this for now, to check if the code-common object is the same
			if ( closing.getCanOverlap() != ctag.getCanOverlap() ) {
				error(String.format("The <ec> and <sc> for id='%s' must have the same canOverlap value.", ctag.getId()));
			}
			if ( closing.getCanCopy() != ctag.getCanCopy() ) {
				error(String.format("The <ec> and <sc> for id='%s' must have the same canCopy value.", ctag.getId()));
			}
			if ( closing.getCanDelete() != ctag.getCanDelete() ) {
				error(String.format("The <ec> and <sc> for id='%s' must have the same canDelete value.", ctag.getId()));
			}
			//-- end of checks normally not needed
			
			if ( ctag.getCanReorder() == CanReorder.FIRSTNO ) {
				if ( closing.getCanReorder() != CanReorder.NO ) {
					error(String.format("The <ec> for id='%s' must be set to canReorder='no'.", ctag.getId()));
				}
			}
			else if ( closing.getCanReorder() != ctag.getCanReorder() ) {
				error(String.format("The <ec> for id='%s' must have the same canReorder value as its corresponding <sc>.", ctag.getId()));
			}
		}
	}
	
	private void checkIsolatedAttributes (Tags markers,
		List<CTag> list)
	{
		for ( Tag bm : markers ) {
			if ( bm.isMarker() ) continue;
			switch ( bm.getTagType() ) {
			case OPENING:
				if ( markers.getClosingTag(bm) == null ) {
					// No match found: this should have an isolated flag
					if ( !list.contains(bm) ) {
						error(String.format("Missing isolated='yes' for opening code id='%s'.", bm.getId()));
					}
				}
				else { // Not isolated
					if ( list.contains(bm) ) {
						error(String.format("Invalid isolated='yes' for opening code id='%s'.", bm.getId()));
					}
				}
				break;
			case CLOSING:
				// This part should really not be triggered as reading the id/startRef will trigger an error before.
				if ( markers.getOpeningTag(bm) == null ) {
					// No match found: this should have an isolated flag
					if ( !list.contains(bm) ) {
						error(String.format("Missing isolated='yes' for closing code id='%s'.", bm.getId()));
					}
				}
				else { // Not isolated
					if ( list.contains(bm) ) {
						error(String.format("Invalid isolated='yes' for closing code id='%s'.", bm.getId()));
					}
				}
				break;
			case STANDALONE:
				// Nothing to check
			}
		}
	}
	
	private void checkTargetOrder (Unit unit) {
		if ( checkTargetOrder ) { // Do it only if it's needed
			int max = unit.getPartCount();
			int i = 0;
			int order2;
			for ( Part part : unit ) {
				int order1 = part.getTargetOrder();
				if ( order1 == 0 ) order1 = i+1; // Default
				if (( order1 < 1 ) || ( order1 > max )) {
					error(String.format("Invalid target order '%d'.", order1));
				}
				// Check for duplicates
				for ( int j=0; j<max; j++ ) {
					Part part2 = unit.getPart(j);
					order2 = part2.getTargetOrder();
					if ( order2 == 0 ) order2 = j+1;
					if (( i != j ) && ( order1 == order2 )) {
						error(String.format("The parts %d and %d have the same target order: '%d'.",
							i+1, j+1, order1));
					}
				}
				i++;
			}
		}
	}
	
	/**
	 * Copies the original data from a map into the specific codes.
	 * <p>During the normal lifetime of an object, the original data are with each code.
	 * This transfer from the store to the code are done only when reading.
	 * @param originalDataMap the map to copy from.
	 */
	private void copyOriginalDataToCodes (Store store,
		Map<String, DataElementContent> originalDataMap)
	{
		if ( store == null ) return;
		// Transfer in the source
		if ( store.hasSourceTag() ) {
			for ( Tag marker : store.getSourceTags() ) {
				if ( marker.isMarker() ) continue; // Not a code
				CTag code = (CTag)marker;
				String nid = code.getDataRef();
				if ( nid != null ) {
					if ( originalDataMap == null ) {
						error(String.format("The code id='%s' refers to the <data> id='%s', but no <originalData> is declared.",
							code.getId(), nid));
					}
					if ( !originalDataMap.containsKey(nid) ) {
						error(String.format("No original data found for the id '%s'.", nid));
					}
					code.setData(originalDataMap.get(nid).content);
					code.setDataDir(originalDataMap.get(nid).dir);
				}
			}
		}
		
		// Transfer in the target
		if ( store.hasTargetTag() ) {
			for ( Tag marker : store.getTargetTags() ) {
				if ( marker.isMarker() ) continue; // Not a code
				CTag code = (CTag)marker;
				String nid = code.getDataRef();
				if ( nid != null ) {
					if ( originalDataMap == null ) {
						error(String.format("The code id='%s' refers to the <data> id='%s', but no <originalData> is declared.",
							code.getId(), nid));
					}
					if ( !originalDataMap.containsKey(nid) ) {
						error(String.format("No original data found for the id '%s'.", nid));
					}
					code.setData(originalDataMap.get(nid).content);
					code.setDataDir(originalDataMap.get(nid).dir);
				}
			}
		}
	}
	
	private void processNote (IWithNotes parent)
		throws XMLStreamException
	{
		Note note = new Note();

		// Get the namespaces
		note.setExtAttributes(gatherNamespaces(null));

		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String ns = reader.getAttributeNamespace(i);
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case Const.ATTR_APPLIESTO:
						cannotBeEmpty(Const.ATTR_APPLIESTO, value);
						if (value.equals("source")) {
							note.setAppliesTo(Note.AppliesTo.SOURCE);
						} else if (value.equals("target")) {
							note.setAppliesTo(Note.AppliesTo.TARGET);
						} else {
							error(String.format("Invalid appliesTo value ('%s').", value));
						}
						// Else: default is set by default
						break;
					case Const.ATTR_ID:
						mustBeValidNmtoken(Const.ATTR_ID, value, true);
						checkAndAddSpecialId(NOTE_NS, value);
						note.setId(value);
						break;
					case Const.ATTR_PRIORITY:
						cannotBeEmpty(Const.ATTR_PRIORITY, value);
						int num = checkIntegerValue(Const.ATTR_PRIORITY, value, 1, 10);
						if (num > -1) note.setPriority(num);
						// Else default is set already
						break;
					case Const.ATTR_CATEGORY:
						cannotBeEmpty(Const.ATTR_CATEGORY, value);
						note.setCategory(value);
						break;
					default:  // Invalid attribute in core namespace
						error(String.format("Invalid attribute '%s' in <note>.", locName));
						break;
				}
			}
			else { // Other namespaces than the core -> extension attributes
				addExtAttribute(note, i, false);
			}
		}

		StringBuilder sb = new StringBuilder();
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CHARACTERS:
				sb.append(reader.getText());
				break;
				
			case XMLStreamReader.START_ELEMENT:
				error("The <note> element has only text content.");
				
			case XMLStreamReader.END_ELEMENT:
				// Can only be the end of the <note>
				popXMLAttributes();
				note.setText(sb.toString());
				parent.addNote(note);
				return;
			}
		}
	}
	
	private void processNotes (IWithNotes parent)
		throws XMLStreamException
	{
		// This creates the Notes object for the parent
		Notes notes = parent.getNotes();
		boolean hasNotes = false;
		
		// Gather namespaces
		notes.setExtAttributes(gatherNamespaces(null));
		// No attributes on notes
		if ( reader.getAttributeCount() > 0 ) {
			error("No attributes are allowed on the <notes> element.");
		}
		
		String tmp, nsUri;
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				pushXMLAttributes();
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) { 
					if ( tmp.equals(Const.ELEM_NOTE) ) { // End of this notes element
						processNote(parent);
						hasNotes = true;
					}
					else {
						error("Only <note> elements are allowed in <notes> elements.");
					}
				}
				else {
					error("Only <note> elements are allowed in <notes> elements.");
				}				
				break;
				
			case XMLStreamReader.END_ELEMENT:
				popXMLAttributes();
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) { 
					if ( tmp.equals(Const.ELEM_NOTES) ) { // End of this notes element
						if ( !hasNotes ) {
							error("A <notes> element must have at least one <note>.");
						}
						return;
					}
				}
				break;
			}
		}
	}
		
	private Map<String, DataElementContent> processOriginalData ()
		throws XMLStreamException
	{
		String tmp, nsUri;
		Map<String, DataElementContent> map = new LinkedHashMap<>();
		StringBuilder content = new StringBuilder();
		String id = null;
		Directionality dir = null;
		boolean inData = false;

		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CHARACTERS:
				if ( id != null ) {
					content.append(reader.getText());
				}
				break;
				
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				pushXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					if ( tmp.equals(Const.ELEM_DATA) ) {
						// Get the id
						id = reader.getAttributeValue("", Const.ATTR_ID);
						mustBeValidNmtoken(Const.ATTR_ID, id, false);
						if ( map.containsKey(id) ) {
							error(String.format("Duplicated id '%s' in original data table.", id));
						}
						// Get the directionality
						dir = getDirectionality(Const.ATTR_DIR,
							reader.getAttributeValue("", Const.ATTR_DIR), Directionality.AUTO);
						inData = true;
					}
					else if ( tmp.equals(Const.ELEM_CP) ) {
						tmp = reader.getAttributeValue("", Const.ATTR_HEX);
						cannotBeNullOrEmpty(Const.ATTR_HEX, tmp);
						content.append(convertHexAttribute(tmp));
					}
					else {
						if ( inData ) error("Only text is allowed in <data> elements.");
						else error("Only <data> is allowed in <originalData> elements.");
					}
				}
				else {
					if ( inData ) error("Only text is allowed in <data> elements.");
					else error("Only <data> is allowed in <originalData> elements.");
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				popXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					if ( tmp.equals(Const.ELEM_DATA) ) {
						map.put(id, new DataElementContent(content.toString(), dir));
						// Reset values for next data element
						id = null;
						dir = null;
						content.setLength(0);
						inData = false;
					}
					else if ( tmp.equals(Const.ELEM_ORIGINALDATA) ) {
						// Do we have at least one data element
						if ( map.isEmpty() ) {
							error("There must be at least one <data> in a <originalData>.");
						}
						return map;
					}
					// Else: could be end of ELEM_CP: nothing to do
				}
				break;
			}
		}
		
		return null;
	}
	
	private void processPart (boolean isSegment,
		HashMap<String, Boolean> partIds)
		throws XMLStreamException
	{
		String tmp, nsUri;
		Part part;
		
		if ( isSegment ) {
			segment = unit.appendSegment();
			part = segment;
			// Set the defaults for inheritable data
			segment.setCanResegment(inheritedData.peek().getCanResegment());
			
			for ( int i=0; i<reader.getAttributeCount(); i++ ) {
				String ns = reader.getAttributeNamespace(i);
				String locName = reader.getAttributeLocalName(i);
				String value = reader.getAttributeValue(i);
				if ( Util.isNoE(ns) ) { // Core namespace
					// Id
					switch (locName) {
						case Const.ATTR_ID:
							continue; // Will be done late (common with ignorable)


							// CanResegment
						case Const.ATTR_CANRESEGMENT:
							if (canBeYesOrNo(Const.ATTR_CANRESEGMENT, value)) {
								segment.setCanResegment(value.equals(Const.VALUE_YES));
							}
							break;
						// State
						case Const.ATTR_STATE:
							segment.setState(value);
							break;
						// Sub state
						case Const.ATTR_SUBSTATE:
							// Make sure we have a state too
							if (reader.getAttributeValue("", Const.ATTR_STATE) == null) {
								error("If <segment> has a subState, state must be set explicitly.");
							}
							segment.setSubState(value);
							break;
						default:
							error(String.format("Invalid attribute '%s' in <segment>.", locName));
							break;
					}
				}
				else {
					error(String.format("Invalid attribute '%s' in <segment>.", locName));
				}
			}
		}
		else {
			ignorable = unit.appendIgnorable();
			part = ignorable;
		}

		// Get id if present
		tmp = reader.getAttributeValue("", Const.ATTR_ID);
		if ( mustBeValidNmtoken(Const.ATTR_ID, tmp, true) ) {
			if ( partIds.containsKey(tmp) ) {
				error(String.format(
					"The id value '%s' is used more than once for <segment> or <ignorable>.", tmp));
			}
			else {
				partIds.put(tmp, false);
			}
			part.setId(tmp);
		}
		
		Boolean srcPreserveWS = null;
		Boolean trgPreserveWS = null;
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				pushXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					if ( tmp.equals(Const.ELEM_SOURCE) ) {
						if ( srcPreserveWS != null ) {
							error("Cannot have more than one <source> per <segment> or <ignorable>.");
						}
						srcPreserveWS = xmlAttributes.peek().startsWith("p");
						if ( isSegment ) processContent(segment, false, false);
						else processContent(ignorable, false, false);
					}
					else if ( tmp.equals(Const.ELEM_TARGET) ) {
						if ( trgPreserveWS != null ) {
							error("Cannot have more than one <target> per <segment> or <ignorable>.");
						}
						trgPreserveWS = xmlAttributes.peek().startsWith("p");
						if ( isSegment ) processContent(segment, true, false);
						else processContent(ignorable, true, false);
						if ( docData.getTargetLanguage() == null ) {
							error("No target language defined in a file with a target entry.");
						}
					}
					else {
						error("Only <source> and <target> are allowed in <segment> or <ignorable> elements.");
					}
				}
				else {
					error("Only <source> and <target> are allowed in <segment> or <ignorable> elements.");
				}
				break;
				
			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				popXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					// Check if we have at least a source
					if ( srcPreserveWS == null ) {
						error("Missing <source> element.");
					}
					// Verify that both source and target are set to the same whitespace handling option
					if (( srcPreserveWS != null ) && ( trgPreserveWS != null )) {
						if ( !srcPreserveWS.equals(trgPreserveWS) ) {
							error("Source and target must be set to the same whitespace handling option.");
						}
					}
					// We are done
					return;
				}
				break;
			}
		}
	}
	
	private void processContent (Part partToFill,
		boolean isTarget,
		boolean allowAnyTargetLang)
		throws XMLStreamException
	{
		Fragment frag = new Fragment(partToFill.getStore(), isTarget);
		String tmp;
		CTag code = null;
		boolean inTextContent = true;
		String dataRef = null;
		Stack<Tag> pairs = new Stack<>();
		
		// Set the context for the directionality
		Stack<Directionality> dirCtx = new Stack<>();
		dirCtx.push(isTarget ? partToFill.getStore().getTargetDir()
			: partToFill.getStore().getSourceDir() );
		
		// Verify the language
		String lang = xmlAttributes.peek().substring(1);
		if ( !lang.isEmpty() ) {
			if ( isTarget ) {
				if ( docData.getTargetLanguage() == null ) {
					error("You must define a target language (trgLang) in the <xliff> element.");
				}
				if ( !allowAnyTargetLang && !lang.equals(docData.getTargetLanguage()) ) {
					error(String.format("Invalid target language ('%s') set or inherited. It should be '%s'.",
						lang, docData.getTargetLanguage()));
				}
			}
			else if ( !lang.equals(docData.getSourceLanguage()) ) {
				error(String.format("Invalid source language ('%s') set or inherited. It should be '%s'.",
					lang, docData.getSourceLanguage()));
			}
		}
		// Else: the value is inherited from srcLang and trgLang and therefore OK.
		
		// Update the white space state
		// The check that source and target are the same for this is done later
		partToFill.setPreserveWS(xmlAttributes.peek().startsWith("p"));
		
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);

			// Element is in XLIFF name space so attribute not prefixed are XLIFF
			if ( Util.isNoE(ns) ) {
				// Order (for target only)
				if ( isTarget && locName.equals(Const.ATTR_ORDER) ) {
					try {
						// Get the value: range validation is done later.
						int order = Integer.parseInt(value);
						partToFill.setTargetOrder(order);
						checkTargetOrder = true;
					}
					catch ( NumberFormatException e ) {
						error(String.format("Invalid numeric value '%s' for order attribute.", value));
					}
				}
				else { // Invalid attribute in core namespace
					if ( isTarget ) error(String.format("Invalid attribute '%s' in <target>.", locName));
					else error(String.format("Invalid attribute '%s' <source>.", locName));
				}
			}
			else {
				if ( ns.equals(Const.NS_XML) ) {
					switch ( locName ) {
					case "space":
					case "lang":
						// Already processed
						continue;
					}
				}
				// Else: no extension attributes
				if ( isTarget ) error(String.format("Invalid attribute '%s' in <target>.", locName));
				else error(String.format("Invalid attribute '%s' <source>.", locName));
			}
		}
		
		String currentElem = null;
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CHARACTERS:
				if ( inTextContent ) {
					frag.append(reader.getText());
				}
				else {
					error(String.format(
						"The <%s> element must be empty.", currentElem));
				}
				break;
				
			case XMLStreamReader.COMMENT:
			case XMLStreamReader.PROCESSING_INSTRUCTION:
				// Ignored and stripped out
				break;
				
			case XMLStreamReader.START_ELEMENT:
				currentElem = tmp = reader.getLocalName();
				String id = reader.getAttributeValue("", Const.ATTR_ID);

				if ( reader.getNamespaceURI().equals(Const.NS_XLIFF_CORE20) ) {
					// Element <sc/>
					switch (tmp) {
						case Const.ELEM_OPENINGCODE:
							mustBeValidNmtoken(Const.ATTR_ID, id, false);
							code = frag.append(TagType.OPENING, id, null, true);
							dataRef = setDataRef(code);
							inTextContent = false;
							setOtherInlineAttributes(Fragment.CODE_OPENING, code, null, false, isTarget, false, null, dirCtx);
							break;
						// Element <ec/>
						case Const.ELEM_CLOSINGCODE:
							String isolated = reader.getAttributeValue("", Const.ATTR_ISOLATED);
							String startRef = reader.getAttributeValue("", Const.ATTR_STARTREF);
							if ((id != null) && (startRef != null)) {
								error("You cannot have both id and startRef attributes on a <ec/> element.");
							}
							if (isolated != null) {
								canBeYesOrNo(Const.ATTR_ISOLATED, isolated);
								mustBeValidNmtoken(Const.ATTR_ID, id, false);
							} else {
								mustBeValidNmtoken(Const.ATTR_STARTREF, startRef, false);
								id = startRef;
							}
							code = frag.append(TagType.CLOSING, id, null, true); // id = id or startRef

							dataRef = setDataRef(code);
							inTextContent = false;
							setOtherInlineAttributes(Fragment.CODE_CLOSING, code, null, false, isTarget,
									(isolated != null), (CTag) frag.getOpeningTag(code), dirCtx);
							break;
						// Element <ph/>
						case Const.ELEM_PLACEHOLDER:
							mustBeValidNmtoken(Const.ATTR_ID, id, false);
							code = frag.appendCode(id, null);
							dataRef = setDataRef(code);
							inTextContent = false;
							setOtherInlineAttributes(Fragment.CODE_STANDALONE, code, null, false, isTarget, false, null, dirCtx);
							break;
						// Element <pc>
						case Const.ELEM_PAIREDCODES: {
							mustBeValidNmtoken(Const.ATTR_ID, id, false);
							code = frag.append(TagType.OPENING, id, null, false);
							setOtherInlineAttributes(Fragment.CODE_OPENING, code, null, true, isTarget, false, null, dirCtx);
							// Check for dataRefStart
							tmp = reader.getAttributeValue("", Const.ATTR_DATAREFSTART);
							if (cannotBeEmpty(Const.ATTR_DATAREF, tmp)) {
								dataRef = tmp;
								code.setDataRef(dataRef);
							}
							code.setInitialWithData((dataRef != null));
							code = null; // We nullify the code


							// Closing code
							CTag closing = new CTag(TagType.CLOSING, id, null);
							setOtherInlineAttributes(Fragment.CODE_CLOSING, closing, null, true, isTarget, false, null, dirCtx);
							// Check for dataRefEnd
							tmp = reader.getAttributeValue("", Const.ATTR_DATAREFEND);
							if (((tmp != null) && (dataRef == null)) || ((tmp == null) && (dataRef != null))) {
								// If we have dataRefEnd, we should have dataRefStart and vice-versa
								warning(String.format("Both '%s' and '%s' should be present or absent.",
										Const.ATTR_DATAREFSTART, Const.ATTR_DATAREFEND));
							}
							if (cannotBeEmpty(Const.ATTR_DATAREF, tmp)) {
								closing.setDataRef(tmp);
							}
							closing.setInitialWithData((dataRef != null));
							dataRef = null;
							pairs.push(closing);
							break;
						}
						// Element <sm/>
						case Const.ELEM_OPENINGANNO: {
							mustBeValidNmtoken(Const.ATTR_ID, id, false);
							MTag ann = new MTag(id, null);
							// Make sure we re-assign the marker as it may have been switched
							ann = (MTag) setOtherInlineAttributes(Fragment.MARKER_OPENING, null, ann, false, isTarget, false, null, dirCtx);
							frag.append(ann);
							break;
						}
						// Element <em/>
						case Const.ELEM_CLOSINGANNO: {
							id = reader.getAttributeValue("", Const.ATTR_STARTREF);
							mustBeValidNmtoken(Const.ATTR_STARTREF, id, false);
							MTag ann = frag.closeMarkerSpan(id);
							setOtherInlineAttributes(Fragment.MARKER_CLOSING, null, ann, false, isTarget, false, null, dirCtx);
							break;
						}
						// Element <mrk>
						case Const.ELEM_PAIREDANNO: {
							mustBeValidNmtoken(Const.ATTR_ID, id, false);
							MTag ann = new MTag(id, MTag.TYPE_DEFAULT);
							// Make sure we re-assign the marker as it may have been switched
							ann = (MTag) setOtherInlineAttributes(Fragment.MARKER_OPENING, null, ann, true, isTarget, false, null, dirCtx);
							frag.append(ann);
							// Closing marker
							MTag closing = new MTag(ann);
							setOtherInlineAttributes(Fragment.MARKER_CLOSING, null, closing, true, isTarget, false, null, dirCtx);
							pairs.push(closing);
							break;
						}
						// Element <cp/>
						case Const.ELEM_CP:
							readCP(inTextContent, frag);
							break;
						default:  // Invalid element
							error(String.format("Invalid element in inline content: '%s'",
									reader.getName().toString()));
							break;
					}
				}
				else { // Not the core namespace
					error(String.format("Invalid element in inline content: '%s'",
						reader.getName().toString()));
				}
				break;

			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				if ( !reader.getNamespaceURI().equals(Const.NS_XLIFF_CORE20) ) {
					error("Only the core namespace is allowed in content.");
				}
				switch (tmp) {
					case Const.ELEM_OPENINGCODE:
					case Const.ELEM_CLOSINGCODE:
					case Const.ELEM_PLACEHOLDER:
						code.setInitialWithData((dataRef != null));
						inTextContent = true; // Back to text content

						dataRef = null;
						break;
					case Const.ELEM_PAIREDCODES:
						frag.append(pairs.pop());
						break;
					case Const.ELEM_PAIREDANNO:
						frag.append(pairs.pop());
						break;
					case Const.ELEM_SOURCE:
						partToFill.setSource(frag);
						popXMLAttributes();
						return;
					case Const.ELEM_TARGET:
						partToFill.setTarget(frag);
						popXMLAttributes();
						return;
				}
				break;
			}
		}
	}

	/**
	 * Checks the constraints for an annotation.
	 * @param marker the marker to check.
	 * @param expliciteTranslate true if the marker had the translate attribute set explicitly.
	 */
	private void checkAnnotation (MTag marker,
		boolean expliciteTranslate)
	{
		// If we have a ref, check the general syntax
		String ref = marker.getRef();
		URIContext ctx = null;
		if ( ref != null ) {
			ctx = uriContext.peek();
			uriParser.setURL(ref, ctx.getFileId(), ctx.getGroupId(), ctx.getUnitId());
		}
		
		// Specific checks for the comment annotations
		switch ( marker.getType() ) {
		case "comment":
			// Check mrk type='comment': either value or ref needs to be set
			// We do not check if we have both ref and value at the same time as is not explicitly forbidden 
			String val = marker.getValue();
			if (( val == null ) && ( ref == null )) {
				error("A comment annotation must have value or ref specified.");
			}
			if (( val != null ) && ( ref != null )) {
				error("A comment annotation must use either the value or the ref attribute, not both.");
			}
			if ( ref != null ) { // If there is a ref attribute it must point to a note inside the unit
				String nid = uriParser.getNoteId();
				if ( nid == null ) {
					error(String.format("The ref value of a comment annotation must be a note, but '%s' is not.", ref));
				}
				uriParser.complementReference(); // Convert to an absolute reference to check the reference container (it should be a unit).
				if ( !uriParser.getUnitId().equals(ctx.getUnitId()) || !uriParser.getFileId().equals(ctx.getFileId())
					|| ( uriParser.getRefContainer() != 'u' )) {
					error(String.format("The ref value of a comment annotation must be a note in the same unit, but '%s' is not.", ref));
				}
				// If we get here: the syntax is OK
				boolean found = false;
				if ( unit.getNoteCount() > 0 ) {
					for ( Note note : unit.getNotes() ) {
						if ( note.getId().equals(nid) ) {
							found = true;
							break;
						}
					}
				}
				if ( !found ) {
					error(String.format("No note with id='%s' found in the unit for '%s'.", nid, ref));
				}
			}
			break;
			
		case "generic":
			// translate must be present: that is check when reading
			if ( !expliciteTranslate ) {
				error("Annotation of type='generic' must have the translate attribute set explicitly.");
			}
			break;
		}
	}
	
	private void readCP (boolean inTextContent,
		Fragment frag)
	{
		if ( !inTextContent ) {
			error(String.format(
				"The <%s> element must be empty.", Const.ELEM_CP));
		}
		
		String tmp = reader.getAttributeValue("", Const.ATTR_HEX);
		cannotBeNullOrEmpty(Const.ATTR_HEX, tmp);
		char[] chars = convertHexAttribute(tmp);
		for ( char c : chars ) {
			frag.append(c);
		}
	}

	private char[] convertHexAttribute (String value) {
		try {
			int cp = Integer.valueOf(value, 16);
			if ( Util.isValidInXML(cp) ) {
				// Value is in the range of valid characters: it should not use cp
				error(String.format("Code-point U+%04X is valid in XML and must not be encoded with <cp/>.", cp));			
			}
			// Else: invalid and therefore OK
			return Character.toChars(cp);
		}
		catch ( IllegalArgumentException e ) { // This catches also NumberFormatException
			error(String.format("Invalid code-point value in '%s': '%s'", Const.ATTR_HEX, value));
		}
		return null;
	}

	private void checkECValueAgainstSC (String attName,
		String scValue,
		String ecValue)
	{
		if ( scValue == null ) {
			if ( ecValue != null ) {
				error(String.format("The value for '%s' is not defined in the <ec> element, but is defined in the <ec/> element.", attName));
			}
		}
		else if ( !scValue.equals(ecValue) ) {
			error(String.format("The value '%s' for '%s' in <ec/> is not matching the value '%s' in <sc/>.", ecValue, attName, scValue));
		}
	}
	
	private void checkECCanReorderAgainstSC (String attName,
		CanReorder scValue,
		String ecValue)
	{
		if ( scValue == CanReorder.FIRSTNO ) {
			if ( ecValue.equals(Const.VALUE_YES) ) {
				error(String.format("The value '%s' for '%s' in <ec/> is not matching the value '%s' in <sc/>.", ecValue, attName, scValue));
			}
		}
		else if ( !scValue.toString().equals(ecValue) ) {
			error(String.format("The value '%s' for '%s' in <ec/> is not matching the value '%s' in <sc/>.", ecValue, attName, scValue));
		}
	}
	
	private void checkECValueAgainstSC (String attName,
		boolean scValue,
		boolean ecValue)
	{
		if ( scValue != ecValue ) {
			error(String.format("The value '%s' for '%s' in <ec/> is not matching the value '%s' in <sc/>.",
				(ecValue ? "yes" : "no"), attName, (scValue ? "yes" : "no")));
		}
	}
	
	private Tag setOtherInlineAttributes (char inlineType,
		CTag ctag,
		MTag mtag,
		boolean paired,
		boolean isTarget,
		boolean closingIsolated,
		CTag opening,
		Stack<Directionality> dirCtx)
	{
		boolean openingIsolated = false; // Just a local flag
		boolean expliciteTranslate = false;
		Tag tag = null;
		if ( ctag != null ) tag = ctag;
		else tag = mtag; // Assumes mtag is not null
		boolean closingNonIsolated = (( inlineType == Fragment.CODE_CLOSING ) && ( opening!=null ));
		// Get the namespaces
		tag.setExtAttributes(gatherNamespaces(tag.getExtAttributes()));
		
		boolean typeChk, canCopyChk, canDeleteChk, canOverlapChk, subTypeChk, copyOfChk, dirChk, canReorderChk;
		typeChk = canCopyChk = canDeleteChk = canOverlapChk = subTypeChk = copyOfChk = dirChk = canReorderChk = false;
		
		// Get the type in case we must switch the marker class
		String value = reader.getAttributeValue("", Const.ATTR_TYPE);
		if ( value != null ) {
			switch ( inlineType ) {
			case Fragment.CODE_OPENING:
			case Fragment.CODE_CLOSING:
			case Fragment.CODE_STANDALONE:
				if ( closingNonIsolated ) {
					checkECValueAgainstSC(Const.ATTR_TYPE, opening.getType(), value);
					typeChk = true;
				}
				else ctag.setType(value);
				break;
			case Fragment.MARKER_OPENING: // Switch the marker
				if ( value.equals(TermTag.TYPE_TERM) || value.equals(TermTag.TYPE_ITSTERMNO) ) {
					// Switch the marker to a TermTag
					mtag = new TermTag(mtag, value, inheritedData.peek().getAnnotatorsRef());
					tag = mtag;
					break;
				}
				// Else: fall thru (normal annotation)
				mtag.setType(value);
				break;
			case Fragment.MARKER_CLOSING:
				// Nothing to set (the value will always be set from <sm>
				break;
			}
		}
		
		// Get the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			value = reader.getAttributeValue(i);

			// Element is in XLIFF name space so attribute not prefixed are XLIFF
			if ( Util.isNoE(reader.getAttributeNamespace(i)) ) {
				// Attributes already set
				switch (locName) {
					case Const.ATTR_ID:
						//TODO: handle cases where the attribute is invalid
						continue;
					case Const.ATTR_STARTREF:
						//TODO: handle cases where the attribute is invalid
						continue;
					case Const.ATTR_DATAREF:
						//TODO: handle cases where the attribute is invalid
						continue;
					case Const.ATTR_DATAREFSTART:
						//TODO: handle cases where the attribute is invalid
						continue;
					case Const.ATTR_DATAREFEND:
						//TODO: handle cases where the attribute is invalid
						continue;
				}
				
				// Otherwise set when needed or generate an error
				switch (locName) {
					case Const.ATTR_CANCOPY:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_CLOSING:
							case Fragment.CODE_STANDALONE:
								if (canBeYesOrNo(Const.ATTR_CANCOPY, value)) {
									if (closingNonIsolated) {
										checkECValueAgainstSC(Const.ATTR_CANCOPY, opening.getCanCopy(), value.equals(Const.VALUE_YES));
										canCopyChk = true;
									} else ctag.setCanCopy(value.equals(Const.VALUE_YES));
								}
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_CANDELETE:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_CLOSING:
							case Fragment.CODE_STANDALONE:
								if (canBeYesOrNo(Const.ATTR_CANDELETE, value)) {
									if (closingNonIsolated) {
										checkECValueAgainstSC(Const.ATTR_CANDELETE, opening.getCanDelete(), value.equals(Const.VALUE_YES));
										canDeleteChk = true;
									} else ctag.setCanDelete(value.equals(Const.VALUE_YES));
								}
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_CANREORDER:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_CLOSING:
							case Fragment.CODE_STANDALONE:
								if (canBeYesOrNoOrFirstNo(Const.ATTR_CANREORDER, value)) {
									if (closingNonIsolated) {
										checkECCanReorderAgainstSC(Const.ATTR_CANREORDER, opening.getCanReorder(), value);
										canReorderChk = true;
									} else {
										switch (value) {
											case Const.VALUE_FIRSTNO:
												ctag.setCanReorder(CanReorder.FIRSTNO);
												break;
											case Const.VALUE_NO:
												ctag.setCanReorder(CanReorder.NO);
												break;
											default:
												ctag.setCanReorder(CanReorder.YES);
												break;
										}
										// Check canCopy/canDelete for defaults
										if (ctag.getCanReorder() != CanReorder.YES) {
											if ((reader.getAttributeValue("", Const.ATTR_CANCOPY) == null)
													|| (reader.getAttributeValue("", Const.ATTR_CANDELETE) == null)) {
												// Defaults for canCopy/canDelete are not ok
												error("Both canCopy and canDelete must be set to 'no' if canReorder is not set to 'yes'.");
											}
										}
									}
								}
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_COPYOF:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_STANDALONE:
							case Fragment.CODE_CLOSING:
								if (cannotBeEmpty(Const.ATTR_COPYOF, value)) {
									if (closingNonIsolated) {
										checkECValueAgainstSC(Const.ATTR_COPYOF, opening.getCopyOf(), value);
										copyOfChk = true;
									} else ctag.setCopyOf(value);
								}
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_TYPE:
						// This was  done at the start of the method
						// Nothing to do
						break;
					case Const.ATTR_SUBTYPE:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_CLOSING:
							case Fragment.CODE_STANDALONE:
								// Make sure we have a type too
								if (reader.getAttributeValue("", Const.ATTR_TYPE) == null) {
									error("An inline code with a subType attribute must also have a type attribute.");
								}
								if (closingNonIsolated) {
									checkECValueAgainstSC(Const.ATTR_SUBTYPE, opening.getSubType(), value);
									subTypeChk = true;
								} else ctag.setSubType(value);
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_DIR:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_CLOSING:
								//TODO: Implement dir in pc and sc.
								Directionality dir = getDirectionality(Const.ATTR_DIR, value, dirCtx.peek());
								if (closingNonIsolated) {
									if (opening.getDir() != dir) {
										error(String.format("The value '%s' for 'dir' in <ec/> is not matching the value '%s' in <sc/>.",
												dir.toString(), dirCtx.peek().toString()));
									}
									dirChk = true;
								} else {
									ctag.setDir(dir);
								}
								// Update context
								//TODO: push/pop for isolated tags may cause problem here
								if (inlineType == Fragment.CODE_OPENING) dirCtx.push(dir);
								else {
									if (!dirCtx.isEmpty()) dirCtx.pop();
								}
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_VALUE:
						switch (inlineType) {
							case Fragment.MARKER_OPENING:
							case Fragment.MARKER_CLOSING:
								mtag.setValue(value);
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_REF:
						switch (inlineType) {
							case Fragment.MARKER_OPENING:
							case Fragment.MARKER_CLOSING:
								mtag.setRef(value);
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_TRANSLATE:
						switch (inlineType) {
							case Fragment.MARKER_OPENING:
							case Fragment.MARKER_CLOSING:
								canBeYesOrNo(Const.ATTR_TRANSLATE, value);
								mtag.setTranslate(Const.VALUE_YES.equals(value));
								expliciteTranslate = true;
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_ISOLATED:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
								// Just check the value. It is not stored in the code
								// Because isolation is determine at output time
								canBeYesOrNo(Const.ATTR_ISOLATED, value);
								openingIsolated = true;
								break;
							case Fragment.CODE_CLOSING:
								// Handled in the caller method
								// And passed as a parameter (but let's set it anyway)
								closingIsolated = true;
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_CANOVERLAP:
						switch (inlineType) {
							case Fragment.CODE_OPENING:
							case Fragment.CODE_CLOSING:
								boolean yon = getYesOrNo(Const.ATTR_CANOVERLAP, value, !paired);
								if (closingNonIsolated) {
									checkECValueAgainstSC(Const.ATTR_CANOVERLAP, opening.getCanOverlap(), yon);
									canOverlapChk = true;
								} else ctag.setCanOverlap(yon);
								break;
							default:
								error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_EQUIV:
						if (!paired) {
							switch (inlineType) {
								case Fragment.CODE_OPENING:
								case Fragment.CODE_CLOSING:
								case Fragment.CODE_STANDALONE:
									ctag.setEquiv(value);
									continue;
							}
						}
						error(String.format("Invalid attribute '%s'.", locName));
						break;
					case Const.ATTR_EQUIVSTART:
						if (paired) {
							switch (inlineType) {
								case Fragment.CODE_OPENING:
									ctag.setEquiv(value);
									break;
								case Fragment.CODE_CLOSING:
									// Valid but don't set
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_EQUIVEND:
						if (paired) {
							switch (inlineType) {
								case Fragment.CODE_CLOSING:
									ctag.setEquiv(value);
									break;
								case Fragment.CODE_OPENING:
									// Valid but don't set
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_DISP:
						if (!paired) {
							switch (inlineType) {
								case Fragment.CODE_OPENING:
								case Fragment.CODE_CLOSING:
								case Fragment.CODE_STANDALONE:
									ctag.setDisp(value);
									continue;
							}
						}
						error(String.format("Invalid attribute '%s'.", locName));
						break;
					case Const.ATTR_DISPSTART:
						if (paired) {
							switch (inlineType) {
								case Fragment.CODE_OPENING:
									ctag.setDisp(value);
									break;
								case Fragment.CODE_CLOSING:
									// Valid but don't set
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_DISPEND:
						if (paired) {
							switch (inlineType) {
								case Fragment.CODE_CLOSING:
									ctag.setDisp(value);
									break;
								case Fragment.CODE_OPENING:
									// Valid but don't set
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_SUBFLOWS:
						if (!paired) {
							switch (inlineType) {
								case Fragment.CODE_OPENING:
								case Fragment.CODE_CLOSING:
								case Fragment.CODE_STANDALONE:
									ctag.setSubFlows(value);
									for (String id : ctag.getSubFlowsIds()) {
										subFlowIds.add(id);
									}
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_SUBFLOWSSTART:
						if (paired) {
							switch (inlineType) {
								case Fragment.CODE_OPENING:
									ctag.setSubFlows(value);
									for (String id : ctag.getSubFlowsIds()) {
										subFlowIds.add(id);
									}
									break;
								case Fragment.CODE_CLOSING:
									// Valid but don't set
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					case Const.ATTR_SUBFLOWSEND:
						if (paired) {
							switch (inlineType) {
								case Fragment.CODE_CLOSING:
									ctag.setSubFlows(value);
									for (String id : ctag.getSubFlowsIds()) {
										subFlowIds.add(id);
									}
									break;
								case Fragment.CODE_OPENING:
									// Valid but don't set
									break;
							}
						} else {
							error(String.format("Invalid attribute '%s'.", locName));
						}
						break;
					default:  // Invalid attribute in core namespace
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			else { // Other namespaces than the core
				QName qname = reader.getAttributeName(i);
				if ( ctag != null ) {
					// For inline codes, this is restricted to modules only
					if ( !qname.getNamespaceURI().startsWith(Const.NS_XLIFF_MODSTART) ) {
						// This will be triggered on the opening element in the case of paired markers
						error(String.format(
							"Invalid extension attribute (%s). " +
							"Only attributes from core and modules can be used in inline codes.",
							qname.toString()));
					}
					if ( inlineType == Fragment.CODE_CLOSING ) {
						// Do not copy modules attributes in closing case of a paired code
						if ( !paired ) {
							// Module extension are OK, but only not in non-isolated closing markers
							if ( !closingIsolated ) {
								error(String.format(
									"Invalid module attribute %s. " +
									"It is allowed in <ec> only when the element is isolated.",
									qname.toString()));
							}
							// Else: OK
							addExtAttribute(ctag, i, false);
						}
					}
					else { // Opening and placeholder cases
						addExtAttribute(ctag, i, false);
					}
				}
				else { // Any extension attribute is allowed for the opening annotations
					if ( inlineType == Fragment.MARKER_CLOSING ) {
						if ( !paired ) {
							error(String.format(
								"Invalid attribute %s. " +
								"No module or extension attribute is allowed in <em>.",
								qname.toString()));
						}
						// Else: Do nothing we are processing </mrk>
						// Extensions/modules are not copied
					}
					else { // Opening annotation
						// Process ITS attributes
						if ( tag instanceof TermTag ) {
							itsReader.readTerminology((TermTag)mtag, inheritedData.peek().getAnnotatorsRef());
						}
						else { // Other ITS annotation
							itsReader.readAttributes(unit, mtag, inheritedData.peek().getAnnotatorsRef());
						}
						// Process any other
						addExtAttribute(mtag, i, true); // Assumes am is not null
					}
				}
			}
		} // End of for all attributes

		// Code validations
		if ( ctag != null ) {
			// If we do not have canReorder='yes'
			// then canRemove and canCopy must be 'no'
			if ( ctag.getCanReorder() != CanReorder.YES ) {
				if ( ctag.getCanCopy() || ctag.getCanDelete() ) {
					error("If canReorder is not set to 'yes' then canCopy and canDelete must be set to 'no'.");
				}
			}
			// Check type/subType values
			try {
				ctag.verifyTypeSubTypeValues();
			}
			catch ( InvalidParameterException e ) {
				error(e.getLocalizedMessage());
			}
			// Store isolated information
			// this will be used when finishing to process the unit for validation
			if ( closingIsolated || openingIsolated ) {
				if ( isTarget ) trgIsolated.add(ctag);
				else srcIsolated.add(ctag);
			}
			
			// Check default values of ec
			if ( closingNonIsolated ) {
				// For any attributes not specified: checks the default against the opening
				if ( !canCopyChk ) {
					if ( !ctag.getCanCopy() ) {
						error(String.format(
							"In <ec> with startRef='%s', the default canCopy value does not match the one of the corresponding <sc> ('%s').",
							ctag.getId(), (opening.getCanCopy() ? "yes" : "no")));
					}
				}
				if ( !canDeleteChk ) {
					if ( !ctag.getCanDelete() ) {
						error(String.format(
							"In <ec> with startRef='%s', the default canDelete value does not match the one of the corresponding <sc> ('%s').",
							ctag.getId(), (opening.getCanDelete() ? "yes" : "no")));
					}
				}
				if ( !typeChk ) {
					if ( ctag.getType() != null ) {
						error(String.format(
							"In <ec> with startRef='%s', the default type value does not match the one of the corresponding <sc> ('%s').",
							ctag.getId(), opening.getType()));
					}
				}
				if ( !subTypeChk ) {
					if ( ctag.getSubType() != null ) {
						error(String.format(
							"In <ec> with startRef='%s', the default subType value does not match the one of the corresponding <sc> ('%s').",
							ctag.getId(), opening.getSubType()));
					}
				}
				if ( !copyOfChk ) {
					if ( ctag.getCopyOf() != null ) {
						error(String.format(
							"In <ec> with startRef='%s', the default copyOf value does not match the one of the corresponding <sc> ('%s').",
							ctag.getId(), opening.getCopyOf()));
					}
				}
				if ( !canReorderChk ) {
					if ( ctag.getCanReorder() != CanReorder.YES ) {
						error(String.format(
							"In <ec> with startRef='%s', the default canReorder value is not valid for the corresponding <sc> value ('%s').",
							ctag.getId(), opening.getCanReorder()));
					}
				}
//				if ( !dirChk ) {
//					if ( ctag.getDir() != dirCtx.peek() ) {
//						error(String.format(
//							"In <ec> with startRef='%s', the default dir value does not match the one of the corresponding <sc> ('%s').",
//							ctag.getId(), opening.getDir()));
//					}
//				}
			}
			
		}
		else { // Annotation validation
			if ( mtag.getTagType() == TagType.OPENING ) {
				checkAnnotation(mtag, expliciteTranslate);
			}
		}
		
		return tag;
	}
	
	private String setDataRef (CTag code) {
		// Try to see if there are outside data defined
		String tmp = reader.getAttributeValue("", Const.ATTR_DATAREF);
		if ( cannotBeEmpty(Const.ATTR_DATAREF, tmp) ) {
			// The actual original data may not be available yet.
			// We just set the id to use.
			// the copy is done when we finish to parse the block
			code.setDataRef(tmp);
			return tmp;
		}
		return null;
	}
	
	private ExtAttributes gatherExtAttributes (boolean isExtElement) {
		ExtAttributes attrs = null;
		// Get the namespaces
		for ( int i=0; i<reader.getNamespaceCount(); i++ ) {
			String namespaceURI = reader.getNamespaceURI(i);
			// Don't store this namespace because the write always create them
			//TODO: Find a better solution for this
			if ( !namespaceURI.equals(Const.NS_XLIFF_CORE20) ) {
				if ( attrs == null ) {
					attrs = new ExtAttributes();
				}
				attrs.setNamespace(reader.getNamespacePrefix(i), namespaceURI);
			}
		}
		// Get the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			QName qname = reader.getAttributeName(i);
			if ( !isExtElement && qname.getNamespaceURI().isEmpty() ) {
				// Skip attribute with empty namespace if they are from a core element
				// (they should be set already)
				continue;
			}
			if ( attrs == null ) {
				attrs = new ExtAttributes();
			}
			attrs.setAttribute(new ExtAttribute(qname, reader.getAttributeValue(i)));
			// Check for id and xml:id attribute
			if ( qname.getLocalPart().equals("id") &&
				( qname.getNamespaceURI().equals(Const.NS_XML) || qname.getNamespaceURI().isEmpty() ))
			{
				// If it's id or xml:id: verify that it is an NMTOKEN
				mustBeValidNmtoken(qname.toString(), reader.getAttributeValue(i), false);
				// Check for duplicates and then add it to the current list of IDs for that namespace
				checkAndAddSpecialId(reader.getNamespaceURI(), reader.getAttributeValue(i));
			}
		}
		if (( attrs == null ) || ( attrs.isEmpty() )) return null;
		return attrs;
	}
	
	/**
	 * Gets the namespace declarations in the current element.
	 * @param attrs the {@link ExtAttributes} object where to put them (or null).
	 * @return the updated attrs parameter (may be a new object if the initial one was null).
	 */
	private ExtAttributes gatherNamespaces (ExtAttributes attrs) {
		// Get the namespaces
		for ( int i=0; i<reader.getNamespaceCount(); i++ ) {
			String namespaceURI = reader.getNamespaceURI(i);
			// Don't store this namespace because the write always create it
			//TODO: Find a better solution for this
			if ( !namespaceURI.equals(Const.NS_XLIFF_CORE20) ) {
				if ( attrs == null ) {
					attrs = new ExtAttributes();
				}
				attrs.setNamespace(reader.getNamespacePrefix(i), namespaceURI);
			}
		}
		if (( attrs == null ) || ( attrs.isEmpty() )) return null;
		return attrs;
	}
	
	/**
	 * Verifies and add an extension attribute.
	 * @param object the object where to add the extension attribute.
	 * @param attributeIndex the index of the attribute to add.
	 * @param skipITS true to not add (skip) ITS or ITSXLF namespaces.
	 */
	private void addExtAttribute (IWithExtAttributes object,
		int attributeIndex,
		boolean skipITS)
	{
		QName qname = reader.getAttributeName(attributeIndex);
		// Skip over ITS attributes
		String ns = qname.getNamespaceURI();
		if ( skipITS && ( ns != null ) && ( ns.equals(Const.NS_ITS) || ns.equals(Const.NS_ITSXLF) )) {
			return;
		}
		// Verify modules
		if ( locValidator != null ) {
			String parentName = reader.getLocalName();
			switch ( locValidator.verify(parentName, qname, true) ) {
			case LocationValidator.NO_MODULES:
				error(String.format("No modules are allowed in '%s'.", parentName));
			case LocationValidator.NOT_ALLOWED:
				error(String.format("Attribute '%s' is not allowed in '%s'.",
					qname, parentName));
			}
		}
		// If valid: add the attribute
		object.getExtAttributes().setAttribute(
			new ExtAttribute(qname, reader.getAttributeValue(attributeIndex)));
	}

	/**
	 * Reads the extensions element for a given parent.
	 * @param parentName the parent name: local name for the core, with prefix for modules.
	 * @param extElements the object where to put the extensions elements.
	 * @return the object holding the extension elements.
	 */
	private ExtElements processExtElement (String parentName,
		ExtElements extElements)
	{
		if ( extElements == null ) {
			extElements = new ExtElements();
		}
		// Check for allowed modules
		if ( locValidator != null ) {
			switch ( locValidator.verify(parentName, reader.getName(), false) ) {
			case LocationValidator.NO_MODULES:
				error(String.format("No modules are allowed in '%s'.", parentName));
			case LocationValidator.TOO_MANY:
				error(String.format("Too many occurrences of '%s' in '%s'.", reader.getName(), parentName));
			case LocationValidator.NOT_ALLOWED:
				error(String.format("Element '%s' is not allowed in '%s'.", reader.getName(), parentName));
			}
		}
		// Read the element and its children recursively
		extElements.add(readExtElement());
		return extElements;
	}

	private ExtElement readExtElement () {
		try {
			ExtElement elem = new ExtElement(reader.getName());
			elem.setExtAttributes(gatherExtAttributes(true));
			List<IExtChild> children = elem.getChildren();
			
			while ( reader.hasNext() ) {
				switch ( reader.next() ) {
				case XMLStreamReader.START_ELEMENT:
					// Else: this is a child element: read it recursively
					pushXMLAttributes();
					children.add(readExtElement());
					break;
					
				case XMLStreamReader.END_ELEMENT:
					if ( reader.getName().equals(elem.getQName()) ) {
						// Done
						popXMLAttributes();
						return elem;
					}
					break;
					
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.SPACE:
					children.add(new ExtContent(reader.getText()));
					break;
					
				case XMLStreamReader.CDATA:
					children.add(new ExtContent(reader.getText(), true));
					break;
					
				case XMLStreamReader.PROCESSING_INSTRUCTION:
					children.add(new ProcessingInstruction("<?"+reader.getPITarget()+" "+reader.getPIData()+"?>"));
					break;
				}
			}
			throw new IOException("Unexpected end of file.");
		}
		catch ( XMLStreamException | IOException e ) {
			error("Error adding an extension element. "+e.getMessage());
		}
		return null;
	}

	private void processChangeTracking (IWithChangeTrack parent)
		throws XMLStreamException
	{
		if ( !reader.getLocalName().equals(ChangeTrack.TAG_NAME) ) {
			error(String.format("Invalid element '%s'", reader.getName().toString()));
		}
		if ( parent.hasChangeTrack() ) {
			error(String.format("Too many elements '%s'", reader.getName().toString()));
		}

		ChangeTrack changeTrack = new ChangeTrack();
		while (reader.hasNext()) {
			switch (reader.next()) {
			case XMLStreamReader.START_ELEMENT:
				processRevisions(changeTrack);
				break;
			case XMLStreamReader.END_ELEMENT:
				popXMLAttributes();
				if ( changeTrack.isEmpty() ) {
					error("You must have at least one <" + Revisions.TAG_NAME
						+ "> element in a <" + ChangeTrack.TAG_NAME
						+ "> element.");
				}
				parent.setChangeTrack(changeTrack);
				return;
			default:
				break;
			}
		}
	}

	private void processRevisions (ChangeTrack changeTrack)
		throws XMLStreamException
	{
		String tmp = reader.getLocalName();
		String nsUri = reader.getNamespaceURI();
		if ( !tmp.equals(Revisions.TAG_NAME)
				|| !nsUri.equals(Const.NS_XLIFF_TRACKING20) ) {
			error(String.format("Invalid element '%s' in <"
					+ ChangeTrack.TAG_NAME + ">.", reader.getName().toString()));
		}
		// Match match = new Match();
		Revisions revisions = new Revisions();
		pushXMLAttributes();

		// Get the namespaces
		revisions.setExtAttributes(gatherNamespaces(revisions.getExtAttributes()));

		// Process the attributes
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			// Element is in the ctr namespace so attribute not prefixed are ctr
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case Revisions.APPLIES_TO_ATTR_NAME:
						revisions.setAppliesTo(value);
						break;
					case Revisions.REF_ATTR_NAME:
						revisions.setRef(value);
						break;
					case Revisions.CURRENT_VERSION_ATTR_NAME:
						revisions.setCurrentVersion(value);
						break;
					default:  // Invalid attribute in ctr namespace
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			else { // Other namespaces than ctr -> extension attributes
				addExtAttribute(revisions, i, false);
			}
		}
		if ( revisions.getAppliesTo() == null ) {
			error("Attribute " + Revisions.APPLIES_TO_ATTR_NAME
					+ " is required for <" + Revisions.TAG_NAME + "> element.");
		}

		// Read the elements
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				processRevision(revisions);
				break;
			case XMLStreamReader.END_ELEMENT:
				popXMLAttributes();
				if ( revisions.isEmpty() ) {
					error("You must have at least one <" + Revision.TAG_NAME
						+ "> element in a <" + Revisions.TAG_NAME
						+ "> element.");
				}
				// Check current version
				if ( revisions.getCurrentVersion() != null
					&& !revisions.getCurrentVersion().isEmpty() )
				{
					boolean found = false;
					for ( Revision rev : revisions ) {
						String ver = rev.getVersion();
						if (( ver != null ) && ver.equals(revisions.getCurrentVersion()) ) {
							found = true;
							break;
						}
					}
					if ( !found ) {
						error(String.format("The '%s' '%s' was not found in the list of '%s' elements.",
							Revisions.CURRENT_VERSION_ATTR_NAME, revisions.getCurrentVersion(), Revision.TAG_NAME));
					}
				}
				changeTrack.add(revisions);
				return;
			default:
				break;
			}
		}
	}

	private void processRevision (Revisions revisions)
		throws XMLStreamException
	{
		String tmp = reader.getLocalName();
		String nsUri = reader.getNamespaceURI();
		if ( !tmp.equals(Revision.TAG_NAME)
				|| !nsUri.equals(Const.NS_XLIFF_TRACKING20) ) {
			error(String.format("Invalid element '%s' in <"
					+ Revisions.TAG_NAME + ">.", reader.getName().toString()));
		}

		Revision revision = new Revision();
		pushXMLAttributes();

		// Get the namespaces
		revision.setExtAttributes(gatherNamespaces(revision.getExtAttributes()));

		// Process the attributes
		for (int i = 0; i < reader.getAttributeCount(); i++) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			// Element is in the ctr namespace so attribute not prefixed are ctr
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case Revision.AUTHOR_ATTR_NAME:
						revision.setAuthor(value);
						break;
					case Revision.DATETIME_ATTR_NAME:
						revision.setDatetime(value);
						break;
					case Revision.VERSION_ATTR_NAME:
						revision.setVersion(value);
						break;
					default:  // Invalid attribute in ctr namespace
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			else { // Other namespaces than ctr -> extension attributes
				addExtAttribute(revision, i, false);
			}
		}

		// Read the elements
		while (reader.hasNext()) {
			switch (reader.next()) {
			case XMLStreamReader.START_ELEMENT:
				processItem(revision);
				break;
			case XMLStreamReader.END_ELEMENT:
				popXMLAttributes();
				if ( revision.isEmpty() ) {
					error(String.format("You must have at least one '%s' element in a '%s' element.",
						Item.TAG_NAME, Revision.TAG_NAME));
				}
				revisions.add(revision);
				return;
			default:
				break;
			}
		}

	}

	private void processItem (Revision revision)
		throws XMLStreamException
	{
		String tmp = reader.getLocalName();
		String nsUri = reader.getNamespaceURI();
		if ( !tmp.equals(Item.TAG_NAME)
				|| !nsUri.equals(Const.NS_XLIFF_TRACKING20) ) {
			error(String.format("Invalid element '%s' in '%s'.",
				reader.getName().toString(), Revision.TAG_NAME));
		}

		Item item = new Item();
		pushXMLAttributes();

		// Get the namespaces
		item.setExtAttributes(gatherNamespaces(item.getExtAttributes()));

		// Process the attributes
		for (int i=0; i < reader.getAttributeCount(); i++) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			// Element is in the ctr namespace so attribute not prefixed are ctr
			if ( Util.isNoE(ns) ) {
				if ( locName.equals(Item.PROPERTY_ATTR_NAME) ) {
					item.setProperty(value);
				}
				else { // Invalid attribute in ctr namespace
					error(String.format("Invalid attribute '%s'.", locName));
				}
			}
			else { // Other namespaces than ctr -> extension attributes
				addExtAttribute(item, i, false);
			}
		}

		StringBuilder sb = new StringBuilder();
		while ( reader.hasNext() ) {
			switch (reader.next()) {
			case XMLStreamReader.CHARACTERS:
				sb.append(reader.getText());
				break;
			case XMLStreamReader.START_ELEMENT:
				error(String.format("The '%s' element has only text content.", Item.TAG_NAME));
			case XMLStreamReader.END_ELEMENT:
				// Can only be the end of the <item>
				popXMLAttributes();
				item.setText(sb.toString());
				revision.add(item);
				return;
			}
		}
	}

	protected void processMatches ()
		throws XMLStreamException
	{
		if ( !reader.getLocalName().equals("matches") ) {
			error(String.format("Invalid element '%s'", reader.getName().toString()));
		}
		pushSpecialIds();
		Matches matches = new Matches();
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				processMatch(matches);
				break;
			case XMLStreamReader.END_ELEMENT: // can be only the end of matches
				popXMLAttributes();
				if ( matches.isEmpty() ) {
					error("You must have at least one <match> element in a <matches> element.");
				}
				unit.setMatches(matches);
				popSpecialIds();
				// Clear the list of isolated element because the one found in matches may conflict
				// with the ones in the segments 9and we check only the ones in segments)
				srcIsolated.clear();
				trgIsolated.clear();
				return;
			}
		}
	}

	private void processMatch (Matches matches)
		throws XMLStreamException
	{
		String tmp = reader.getLocalName();
		String nsUri = reader.getNamespaceURI();
		if ( !tmp.equals("match") || !nsUri.equals(Const.NS_XLIFF_MATCHES20) ) {
			error(String.format("Invalid element '%s' in <matches>.", reader.getName().toString()));
		}
		Match match = new Match();
		Map<String, DataElementContent> matchODM = null;
		ExtElements extElems = null;

		pushXMLAttributes();

		// Get the namespaces
		match.setExtAttributes(gatherNamespaces(match.getExtAttributes()));
		// Get the current annotatorsRef data
		AnnotatorsRef ar = inheritedData.peek().getAnnotatorsRef();
		if ( ar != null ) match.setAnnoatorRef(ar.get(DataCategories.MTCONFIDENCE));
		
		// Process the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			// Element is in the mtc namespace so attribute not prefixed are mtc
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case "ref":
						match.setRef(value);
						break;
					case "type":
						match.setType(value);
						break;
					case "subType":
						if (value != null) {
							// type must be explicitly set
							if (reader.getAttributeValue("", "type") == null) {
								error("If <match> has a subType, type must be set explicitly.");
							}
							match.setSubType(value);
						}
						break;
					case "id":
						mustBeValidNmtoken(Const.ATTR_ID, value, true);
						checkAndAddSpecialId(Const.NS_XLIFF_MATCHES20, value);
						match.setId(value);
						break;
					case "similarity":
						match.setSimilarity(Double.valueOf(value));
						break;
					case "matchQuality":
						match.setMatchQuality(Double.valueOf(value));
						break;
					case "matchSuitability":
						match.setMatchSuitability(Double.valueOf(value));
						break;
					case "origin":
						match.setOrigin(value);
						break;
					case "reference":
						if (canBeYesOrNo("reference", value)) {
							match.setReference(value.equals(Const.VALUE_YES));
						}
						break;
					default:  // Invalid attribute in mtc namespace
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			else if ( ns.equals(Const.NS_XML) ) {
				if ( locName.equals("lang") ) {
					error("The attribute xml:lang is not allowed in <match>.");
				}
			}
			else if ( ns.equals(Const.NS_ITS) ) {
				if ( locName.equals(ITSReader.ANNOTATORSREF) ) {
					AnnotatorsRef tmpAR = itsReader.readAnnotatorsRef(false, ar);
					if ( tmpAR != null ) {
						match.setAnnoatorRef(tmpAR.get(DataCategories.MTCONFIDENCE));
					}
				}
				else {
					warning("Unexpected ITS attribute: "+locName);
				}
			}
			else { // Other namespaces than mtc -> extension attributes
				addExtAttribute(match, i, false);
			}
		}
		
		// Temporary part to read the source and target fragments
		Part part = new Part(match.getStore());
		
		int state = 0; // optional meta, then optional originalData, then source then target, then extensions
		// Read the elements
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				pushXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_METADATA20) ) {
					if ( state > 0 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
					processMetadata(match);
					state = 1;
				}
				else if ( nsUri.equals(Const.NS_XLIFF_CORE20) ) {
					switch ( tmp ) {
					case Const.ELEM_ORIGINALDATA: // OK when state < 2
						if ( state > 1 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
						matchODM = processOriginalData();
						state = 2;
						break;
					case Const.ELEM_SOURCE: // OK when state < 3
						if ( state > 2 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
						processContent(part, false, false);
						match.setSource(part.getSource());
						state = 3;
						break;
					case Const.ELEM_TARGET: // OK when state < 4
						if ( state > 3 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
						processContent(part, true, match.isReference());
						match.setTarget(part.getTarget());
						state = 4;
						break;
					default:
						error(String.format("Invalid element '%s'", reader.getName().toString()));
					}
				}
				else { // OK when state > 3
					if ( state < 4 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
					if ( locValidator != null ) locValidator.reset();
					extElems = processExtElement("mtc:match", extElems);
				}
				break;

			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				if ( nsUri.equals(Const.NS_XLIFF_MATCHES20) ) { // Can be only the end of match
					if ( state < 4 ) error("Missing element(s) in <match>.");
					copyOriginalDataToCodes(match.getStore(), matchODM);
					matches.add(match);
					if ( extElems != null ) match.setExtElements(extElems);
					popXMLAttributes();
					return;
				}
				
				break;
			}
		}
	}

	private void processGlossary ()
		throws XMLStreamException
	{
		if ( !reader.getLocalName().equals("glossary") ) {
			error(String.format("Invalid element '%s'", reader.getName().toString()));
		}
		pushSpecialIds();
		Glossary glossary = new Glossary();
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				processGlossEntry(glossary);
				break;
			case XMLStreamReader.END_ELEMENT: // can be only the end of matches
				popXMLAttributes();
				if ( glossary.isEmpty() ) {
					error("You must have at least one <glossEntry> element in a <glossary> element.");
				}
				unit.setGlossary(glossary);
				popSpecialIds();
				return;
			}
		}
	}

	private void processGlossEntry (Glossary glossary)
		throws XMLStreamException
	{
		String tmp = reader.getLocalName();
		String nsUri = reader.getNamespaceURI();
		if ( !tmp.equals("glossEntry") || !nsUri.equals(Const.NS_XLIFF_GLOSSARY20) ) {
			error(String.format("Invalid element '%s' in <glossary>.", reader.getName().toString()));
		}
		GlossEntry entry = new GlossEntry();
		ExtElements extElems = null;
		pushXMLAttributes();

		// Get the namespaces
		entry.setExtAttributes(gatherNamespaces(entry.getExtAttributes()));
		
		// Process the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			// Element is in the gls namespace so attribute not prefixed are gls
			if ( Util.isNoE(ns) ) {
				if ( locName.equals("ref") ) entry.setRef(value);
				else if ( locName.equals("id") ) {
					mustBeValidNmtoken(Const.ATTR_ID, value, true);
					checkAndAddSpecialId(Const.NS_XLIFF_GLOSSARY20, value);
					entry.setId(value);
				}
				else { // Invalid attribute in gls namespace
					error(String.format("Invalid attribute '%s'.", locName));
				}
			}
			else { // Other namespaces than gls -> extension attributes
				addExtAttribute(entry, i, false);
			}
		}
		
		int state = 0; // term, optional translation(s), optional definition, then extensions
		// Read the elements
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				pushXMLAttributes();
				if ( nsUri.equals(Const.NS_XLIFF_GLOSSARY20) ) {
					switch ( tmp ) {
					case "term": // OK when state == 0
						if ( state != 0 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
						// Get the attributes
						for ( int i=0; i<reader.getAttributeCount(); i++ ) {
							String locName = reader.getAttributeLocalName(i);
							String value = reader.getAttributeValue(i);
							String ns = reader.getAttributeNamespace(i);
							if ( Util.isNoE(ns) ) {
								if ( locName.equals("source") ) entry.getTerm().setSource(value);
								else { // Invalid attribute in gls namespace
									error(String.format("Invalid attribute '%s'.", locName));
								}
							}
							else { // Other namespaces than gls -> extension attributes
								addExtAttribute(entry.getTerm(), i, false);
							}
						}
						// Get the content
						entry.getTerm().setText(readTextContent(true));
						state = 1;
						break;
					case "translation": // OK when state == 1
						if ( state != 1 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
						Translation trans = new Translation((String)null);
						// Get the attributes
						for ( int i=0; i<reader.getAttributeCount(); i++ ) {
							String locName = reader.getAttributeLocalName(i);
							String value = reader.getAttributeValue(i);
							String ns = reader.getAttributeNamespace(i);
							if ( Util.isNoE(ns) ) {
								switch (locName) {
									case "ref":
										trans.setRef(value);
										break;
									case "source":
										trans.setSource(value);
										break;
									case "id":
										mustBeValidNmtoken(Const.ATTR_ID, value, true);
										checkAndAddSpecialId(Const.NS_XLIFF_GLOSSARY20, value);
										trans.setId(value);
										break;
									default:  // Invalid attribute in gls namespace
										error(String.format("Invalid attribute '%s'.", locName));
										break;
								}
							}
							else { // Other namespaces than gls -> extension attributes
								addExtAttribute(trans, i, false);
							}
						}
						// Get the content
						trans.setText(readTextContent(true));
						entry.getTranslations().add(trans);
						// State will change when we see an element different from translation
						break;
					case "definition": // OK when state == 1 or 2
						if (( state < 1 ) || ( state > 2 )) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
						entry.setDefinition(new Definition("")); // Default
						// Get the attributes
						for ( int i=0; i<reader.getAttributeCount(); i++ ) {
							String locName = reader.getAttributeLocalName(i);
							String value = reader.getAttributeValue(i);
							String ns = reader.getAttributeNamespace(i);
							if ( Util.isNoE(ns) ) {
								if ( locName.equals("source") ) entry.getDefinition().setSource(value);
								else { // Invalid attribute in gls namespace
									error(String.format("Invalid attribute '%s'.", locName));
								}
							}
							else { // Other namespaces than gls -> extension attributes
								addExtAttribute(entry.getDefinition(), i, false);
							}
						}
						// Get the content
						entry.getDefinition().setText(readTextContent(true));
						state = 3;
						break;
					default:
						error(String.format("Invalid element '%s'", reader.getName().toString()));
					}
				}
				else { // OK when state == 1, 2 or 3
					if ( state < 1 ) error(String.format("Element '%s' misplaced.", reader.getName().toString()));
					if ( locValidator != null ) locValidator.reset();
					extElems = processExtElement("gls:glossEntry", extElems);
				}
				break;

			case XMLStreamReader.END_ELEMENT:
				tmp = reader.getLocalName();
				nsUri = reader.getNamespaceURI();
				if ( nsUri.equals(Const.NS_XLIFF_GLOSSARY20) ) { // Can be only the end of match
					if ( state < 1 ) error("Missing element(s) in <glossEntry>.");
					if ((( entry.getDefinition() == null ) || Util.isNoE(entry.getDefinition().getText()) )
						&& entry.getTranslations().isEmpty() )
					{
						error("A <glossEntry> must have at least a <translation> or a <definition>.");
					}
					glossary.add(entry);
					if ( extElems != null ) entry.setExtElements(extElems);
					popXMLAttributes();
					return;
				}
				break;
			}
		}
	}

	private void processMetadata (IWithMetadata parent)
		throws XMLStreamException
	{
		if ( !reader.getLocalName().equals("metadata") ) {
			error(String.format("Invalid element '%s'", reader.getName().toString()));
		}
		if ( parent.hasMetadata() ) {
			error(String.format("Too many elements '%s'", reader.getName().toString()));
		}
		pushSpecialIds();

		Metadata metadata = new Metadata();
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			if ( Util.isNoE(reader.getAttributeNamespace(i)) ) {
				if ( locName.equals("id") ) {
					String value = reader.getAttributeValue(i);
					mustBeValidNmtoken(Const.ATTR_ID, value, true);
					checkAndAddSpecialId(Const.NS_XLIFF_METADATA20, value);
					metadata.setId(value);
				}
				else { // Invalid attribute
					error(String.format("Invalid attribute '%s'.", locName));
				}
			}
			else { // Other namespaces -> invalid
				error(String.format("Invalid attribute '%s'.", locName));
			}
		}
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				if ( !Const.NS_XLIFF_METADATA20.equals(reader.getNamespaceURI()) 
					|| !"metaGroup".equals(reader.getLocalName()) ) {
					error(String.format("Invalid element '%s' in <metadata>.", reader.getName().toString()));
				}
				processMetaGroup(metadata);
				break;
			case XMLStreamReader.END_ELEMENT: // can be only the end of matches
				popXMLAttributes();
				if ( metadata.isEmpty() ) {
					error("You must have at least one entry a <metadata> element.");
				}
				parent.setMetadata(metadata);
				popSpecialIds();
				return;
			}
		}
	}
	
	private void processMetaGroup (IWithMetaGroup parent)
		throws XMLStreamException
	{
		MetaGroup group = new MetaGroup();
		// Process the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			if ( Util.isNoE(ns) ) {
				switch (locName) {
					case "category":
						group.setCategory(value);
						break;
					case "appliesTo":
						group.setAppliesTo(AppliesTo.fromString(value));
						break;
					case "id":
						mustBeValidNmtoken(Const.ATTR_ID, value, true);
						checkAndAddSpecialId(Const.NS_XLIFF_METADATA20, value);
						group.setId(value);
						break;
					default:  // Invalid attribute
						error(String.format("Invalid attribute '%s'.", locName));
						break;
				}
			}
			else { // Other namespaces -> invalid
				error(String.format("Invalid attribute '%s'.", locName));
			}
		}
		
		// Process the content of the group
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				if ( Const.NS_XLIFF_METADATA20.equals(reader.getNamespaceURI()) ) {
					switch ( reader.getLocalName() ) {
					case "metaGroup":
						processMetaGroup(group); // Recursive call
						continue;
					case "meta":
						// Attributes
						String type = null;
						for ( int i=0; i<reader.getAttributeCount(); i++ ) {
							String locName = reader.getAttributeLocalName(i);
							if ( Util.isNoE(reader.getAttributeNamespace(i)) ) {
								if ( locName.equals("type") ) {
									type = reader.getAttributeValue(i);
								}
								else { // Invalid attribute
									error(String.format("Invalid attribute '%s'.", locName));
								}
							}
							else { // Other namespaces -> invalid
								error(String.format("Invalid attribute '%s'.", locName));
							}
						}
						if ( type == null ) {
							error("The <meta> element must have a type attribute.");
						}
						group.add(new Meta(type, readTextContent(false)));
						continue;
					}
					// Else: falls thru to error
				}
				// Else: error
				error(String.format("Invalid element '%s' in <metaGroup>.", reader.getName().toString()));
				break;
			case XMLStreamReader.END_ELEMENT:
				if ( group.isEmpty() ) {
					error("You must have at least one entry a <metaGroup> element.");
				}
				parent.addGroup(group);
				return;
			}
		}
	}
	
	private void processValidation (IWithValidation parent)
		throws XMLStreamException
	{
		if ( !reader.getLocalName().equals("validation") ) {
			error(String.format("Invalid element '%s'", reader.getName().toString()));
		}
		if ( parent.hasValidation() && parent.getValidation().getDeclarationCount()>0 ) {
			error(String.format("Too many elements '%s'", reader.getName().toString()));
		}
		//pushSpecialIds();

		// Check if the parent has rules already, if so: use those
		// otherwise start with the ones in context
		Validation validation;
		if ( parent.hasValidation() ) validation = parent.getValidation();
		else validation = valContext.peek();
		validation.addDeclaration();
		
		// Read the extension attributes
		validation.setExtAttributes(gatherExtAttributes(false));
		
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.START_ELEMENT:
				if ( !Const.NS_XLIFF_VALIDATION20.equals(reader.getNamespaceURI()) 
					|| !"rule".equals(reader.getLocalName()) ) {
					error(String.format("Invalid element '%s' in <validation>.", reader.getName().toString()));
				}
				processValidationRule(validation);
				break;
			case XMLStreamReader.END_ELEMENT: // can be only the end of validation
				popXMLAttributes();
				if ( validation.isEmpty() ) {
					error("You must have at least one entry a <validation> element.");
				}
				parent.setValidation(validation);
				//popSpecialIds();
				return;
			}
		}
	}
		
	private void processValidationRule (Validation validation)
		throws XMLStreamException
	{
		Rule rule = new Rule("isPresent", null);
		String type = null;
		String occurs = null;
		String existsInSource = null;
		String caseSensitive = null;
		String disabled = null;
		String normalization = null;

		// Process the attributes
		for ( int i=0; i<reader.getAttributeCount(); i++ ) {
			String locName = reader.getAttributeLocalName(i);
			String value = reader.getAttributeValue(i);
			String ns = reader.getAttributeNamespace(i);
			if ( Util.isNoE(ns) ) {
				switch ( locName ) {
				case "isPresent":
				case "isNotPresent":
				case "startsWith":
				case "endsWith":
					if ( type != null ) {
						error(String.format("Attribute %s cannot be used is %s is also used.", locName, type));
					}
					cannotBeEmpty(locName, value);
					type = locName;
					rule.setType(Rule.Type.fromString(type));
					rule.setData(value);
					break;
					
				case "occurs":
					occurs = value;
					break;
					
				case "caseSensitive":
					caseSensitive = value;
					break;
					
				case "normalization":
					normalization = value;
					break;
					
				case "existsInSource":
					existsInSource = value;
					break;
					
				case "disabled":
					disabled = value;
					break;

				default: // Invalid attribute
					error(String.format("Invalid attribute '%s'.", locName));
				}
			}
			else { // Other namespaces
				addExtAttribute(rule, i, true);
			}
		}
		// Check the attributes
		if ( type == null ) {
			if ( !rule.hasExtAttribute() ) {
				error("Missing the type of the rule (isPresent, etc.)");
			}
			rule.setType(Type.CUSTOM);
		}
		if ( canBeYesOrNo("caseSensitive", caseSensitive) ) {
			rule.setCaseSensitive(caseSensitive.equals("yes"));
		}
		if ( normalization != null ) {
			rule.setNormalization(Normalization.fromString(normalization));
		}
		if ( canBeYesOrNo("disabled", disabled) ) {
			rule.setEnabled(disabled.equals("no"));
		}
		if ( existsInSource != null ) {
			switch ( rule.getType() ) {
			case ISPRESENT:
			case STARTSWITH:
			case ENDSWITH:
				canBeYesOrNo("existsInSource", existsInSource);
				rule.setExistsInSource(existsInSource.equals("yes"));
				break;
			case ISNOTPRESENT:
			case CUSTOM:
				error("The attribute existsInSource is not valid for this type of rule.");
				break;
			}		
		}
		if ( occurs != null ) {
			// Used only with isPresent, but not invalid otherwise
			try {
				int number = Integer.parseInt(occurs);
				if ( number < 1 ) {
					error("The value of the attribute 'occurs' must be 1 or higer.");
				}
				rule.setOccurs(number);
			}
			catch ( NumberFormatException e ) {
				error(String.format("Invalid syntax for attribute 'occurs' (%s).", occurs));
			}
		}
		
		// Process the content (must be empty)
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.END_ELEMENT:
				// Add the rule to the list and override or not
				// Check each existing rule
				boolean add = true;
				int i=0;
				for ( Rule existing : validation ) {
					// Do not override non-inherited rules
					// The specification (strangely) has text about overriding rules only across scopes
					// nothing about overriding rules within the same validation element
					if ( !existing.isInherited() ) continue;
					// Skip custom rule because we have no clue what constitute the 'sameness'
					if ( existing.getType() == Type.CUSTOM ) continue;
					// Check the type and data
					if ( existing.getType() == rule.getType() ) {
						if ( existing.getData().equals(rule.getData()) ) {
							// Overriding means we replace
							rule.prepare();
							validation.set(i, rule);
							add = false;
						}
					}
					i++;
				}
				// Add the new rule if needed
				if ( add ) {
					rule.prepare();
					validation.add(rule);
				}
				return;
			}
		}
	}

	/**
	 * Reads the text nodes until the next end of element.
	 * @return the text read.
	 */
	private String readTextContent (boolean popXMLAttributes)
		throws XMLStreamException
	{
		StringBuilder tmp = new StringBuilder();
		while ( reader.hasNext() ) {
			switch ( reader.next() ) {
			case XMLStreamReader.CDATA:
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.SPACE:
				tmp.append(reader.getText());
				break;
			case XMLStreamReader.PROCESSING_INSTRUCTION:
			case XMLStreamReader.COMMENT:
				// Ignore those events
				break;
			case XMLStreamReader.END_ELEMENT:
				// We are done
				if ( popXMLAttributes ) popXMLAttributes();
				return tmp.toString();
			default: // Anything else is an error
				 error(String.format("Invalid element '%s' in text content.", reader.getName().toString()));
			}
		}
		return null; // Should never reach this
	}
}
