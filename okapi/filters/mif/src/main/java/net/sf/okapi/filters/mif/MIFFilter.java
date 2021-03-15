/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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

package net.sf.okapi.filters.mif;

import net.sf.okapi.common.BOMAwareInputStream;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.exceptions.OkapiIllegalFilterOperationException;
import net.sf.okapi.common.exceptions.OkapiUnsupportedEncodingException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.resource.TextUnitUtil;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

@UsingParameters(Parameters.class)
public class MIFFilter implements IFilter {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final String DEFAULT_ENCODING = StandardCharsets.UTF_8.name();
	private static final String LINE_FEED = "\n";
	private static final String HARD_RETURN = "\\n";

	private static final String TOPSTATEMENTSTOSKIP = "ColorCatalog;ConditionCatalog;BoolCondCatalog;"
		+ "CombinedFontCatalog;ElementDefCatalog;FmtChangeListCatalog;DefAttrValuesCatalog;"
		+ "AttrCondExprCatalog;FontCatalog;RulingCatalog;TblCatalog;KumihanCatalog;Views;"
		+ "MarkerTypeCatalog;Document;BookComponent;InitialAutoNums;Dictionary;"; // Must end with ';'

	private static final String IMPORTOBJECT = "ImportObject";

	private Parameters params;
	private String lineBreak;
	private String docName;
	private BufferedReader reader;
	private Document document;
	private StringBuilder tagBuffer;
	private StringBuilder strBuffer;
	private int tuId;
	private int otherId;
	private int grpId;
	private boolean canceled;
	private LinkedList<Event> queue;
	private LocaleId srcLang;
	private GenericSkeleton skel;
	private boolean hasNext;
	private EncoderManager encoderManager;
	private int inBlock;
	private boolean inPgfCatalog;
	private int pgfCatalogLevel;
	private boolean inPgf;
	private boolean extractedPgfNumFormat;
	private int blockLevel;
	private int paraLevel;
	private StringBuilder paraSkelBuf;
	private StringBuilder paraTextBuf;
	private StringBuilder paraCodeBuf;
	private StringBuilder paraCodeTypes;
	private int tableGroupLevel;
	private int rowGroupLevel;
	private int cellGroupLevel;
	private int fnoteGroupLevel;
	private Stack<String> parentIds;
	private Extracts extracts;
	private MIFEncoder encoder;
	private String encoding;
	private int footnotesLevel;
	private int textFlowNumber;
	private Deque<ITextUnit> referentTextUnits;
	private RawDocument rawDocument;
	private boolean inXRef;
	private boolean newParaLine;
	private boolean charProcessed;
	private boolean stringAfterCharProcessed;

	public MIFFilter () {
		params = new Parameters();
	}

	@Override
	public void cancel () {
		canceled = true;
	}

	@Override
	public void close () {
		if (rawDocument != null) {
			rawDocument.close();
		}
		try {
			if ( reader != null ) {
				reader.close();
				reader = null;
			}
			hasNext = false;
			docName = null;
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	@Override
	public String getName () {
		return "okf_mif";
	}

	@Override
	public String getDisplayName () {
		return "MIF Filter";
	}

	@Override
	public String getMimeType () {
		return MimeTypeMapper.MIF_MIME_TYPE;
	}

	@Override
	public List<FilterConfiguration> getConfigurations () {
		List<FilterConfiguration> list = new ArrayList<>();
		list.add(new FilterConfiguration(getName(),
			MimeTypeMapper.MIF_MIME_TYPE,
			getClass().getName(),
			"MIF (BETA)",
			"Adobe FrameMaker MIF documents",
			null,
			".mif;"));
		return list;
	}

	@Override
	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
			encoderManager.setMapping(MimeTypeMapper.MIF_MIME_TYPE, "net.sf.okapi.filters.mif.MIFEncoder");
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

	@Override
	public void open (RawDocument input) {
		open(input, true);
	}

	@Override
	public void open (RawDocument rawDocument,
		boolean generateSkeleton)
	{
		this.rawDocument = rawDocument;

		if (( rawDocument.getInputURI() == null ) && ( rawDocument.getInputCharSequence() == null )) {
			// Cannot do this currently because of the double pass
			throw new OkapiBadFilterInputException("Direct stream input not supported for MIF.");
		}

		srcLang = rawDocument.getSourceLocale();
		if ( rawDocument.getInputURI() != null ) {
			docName = rawDocument.getInputURI().getPath();
		}

		try {
			//--- First pass: gather information
			BOMAwareInputStream bis = new BOMAwareInputStream(rawDocument.getStream(), DEFAULT_ENCODING);
			encoding = bis.detectEncoding();
			reader = new BufferedReader(new InputStreamReader(bis, this.encoding));

			initialize();

			this.extracts = new Extracts(
				this.params,
				new FontTags()
			);
			this.extracts.from(this.document);

			lineBreak = this.extracts.lineBreak();
			encoder = new MIFEncoder();
			encoder.setOptions(this.params, this.encoding, this.lineBreak);

			reader.close();
			rawDocument.close();

			//--- Second pass: extract

			// The base encoding was detected before the first pass, so the decoder is already set
			// But we do call the detectEncoding to handle the possible BOM
			bis = new BOMAwareInputStream(rawDocument.getStream(), DEFAULT_ENCODING);
			this.encoding = bis.detectEncoding();

			reader = new BufferedReader(new InputStreamReader(bis, this.encoding));

			initialize();
			this.referentTextUnits = new LinkedList<>();
			String sdId = rawDocument.getId();
			if ( Util.isEmpty(sdId) ) sdId = "sd1";
			parentIds.push(sdId);

			// Compile code finder rules
			if ( params.getUseCodeFinder() ) {
				this.params.getCodeFinder().addRules(
					this.extracts.additionalInlineCodeFinderRules()
				);
				params.getCodeFinder().compile();
			}

			queue = new LinkedList<>();
			StartDocument startDoc = new StartDocument(sdId);
			startDoc.setName(docName);
			startDoc.setLineBreak(lineBreak);
			startDoc.setEncoding(encoding, false);
			// We assume no BOM in all case for MIF
			startDoc.setLocale(srcLang);
			startDoc.setFilterId(getName());
			startDoc.setFilterParameters(getParameters());
			startDoc.setFilterWriter(createFilterWriter());
			startDoc.setType(getMimeType());
			startDoc.setMimeType(getMimeType());

			queue.add(new Event(EventType.START_DOCUMENT, startDoc));
		}
		catch ( UnsupportedEncodingException e ) {
			throw new OkapiUnsupportedEncodingException("Error reading MIF input.", e);
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error reading MIF input.", e);
		}
	}

	private void initialize () {
		document = new Document.Default(new Statements(reader), reader, new LinkedList<>());
		tagBuffer = new StringBuilder();
		strBuffer = new StringBuilder();
		paraSkelBuf = new StringBuilder();
		paraCodeBuf = new StringBuilder();
		paraCodeTypes = new StringBuilder();
		paraTextBuf = new StringBuilder();
		tuId = 0;
		otherId = 0;
		grpId = 0;
		canceled = false;
		hasNext = true;
		inBlock = 0;
		blockLevel = 0;
		tableGroupLevel = -1;
		rowGroupLevel = -1;
		cellGroupLevel = -1;
		fnoteGroupLevel = -1;
		parentIds = new Stack<>();
		footnotesLevel = -1;
		textFlowNumber = 0;
	}

	@Override
	public void setFilterConfigurationMapper (IFilterConfigurationMapper fcMapper) {
		// Not used
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	public Event next () {
		// Treat cancel
		if ( canceled ) {
			queue.clear();
			queue.add(new Event(EventType.CANCELED));
			hasNext = false;
		}
		// Fill the queue if it's empty
		if ( queue.isEmpty() ) {
			read();
		}
		// Update hasNext flag on the FINISHED event
		if ( queue.peek().getEventType() == EventType.END_DOCUMENT ) {
			hasNext = false;
		}
		// Return the head of the queue
		return queue.poll();
	}

	@Override
	public ISkeletonWriter createSkeletonWriter () {
		return new GenericSkeletonWriter();
	}

	@Override
	public IFilterWriter createFilterWriter () {
		return new GenericFilterWriter(createSkeletonWriter(), getEncoderManager());
	}

	/**
	 * Top-level read
	 */
	private void read () {
		try {
			skel = new GenericSkeleton();
			int c;

			// Check if we are still processing a TextFlow
			if ( inBlock > 0 ) {
				processBlock(inBlock);
				return;
			}

			while ( (c = reader.read()) != -1 ) {
				switch ( c ) {
				case '#':
					skel.append((char)c);
					readComment(true, null);
					break;

				case '<': // Start of statement
					skel.append((char)c);
					blockLevel++;
					String tag = readTag(true, true, null);
					if (TOPSTATEMENTSTOSKIP.contains(tag + ";")) {
						skipOverContent(true, null);
						blockLevel--;
					}
					else if ( "PgfCatalog".equals(tag) ) {
						this.inPgfCatalog = true;
						this.pgfCatalogLevel = blockLevel;
						continue;
					}
					else if ( this.inPgfCatalog && "Pgf".equals(tag) ) {
						if ( startBlock(blockLevel, BlockType.PARAGRAPH_STYLE) ) return;
					}
					else if ( "TextFlow".equals(tag) ) {
						textFlowNumber++;
						if ( startBlock(blockLevel, BlockType.TEXT_FLOW) ) return;
					}
					else if ( "Tbls".equals(tag) ) {
						// Do nothing, but do not skip.
						// The tables will be read in Tbl tags
						continue;
					}
					else if ( "Tbl".equals(tag) ) {
						if ( startBlock(blockLevel, BlockType.TABLE) ) return;
					}
					else if ( "VariableFormats".equals(tag) ) {
						if ( params.getExtractVariables() ) {
							processFormats("Variable");
						}
						else {
							skipOverContent(true, null);
							blockLevel--;
						}
					}
					else if ( "XRefFormats".equals(tag) ) {
						if ( params.getExtractReferenceFormats() ) {
							processFormats("XRef");
						}
						else {
							skipOverContent(true, null);
							blockLevel--;
						}
					}
					else if ( "Page".equals(tag) ) {
						processPage(blockLevel);
					}
					else if ( "AFrames".equals(tag) ) {
						processFramesAndTextLines(blockLevel);
					}
					else {
						// Default: skip over
						skipOverContent(true, null);
						blockLevel--;
					}
					// Flush the skeleton from time to time to allow very large files
					queue.add(new Event(EventType.DOCUMENT_PART,
						new DocumentPart(String.valueOf(++otherId), false),
						skel));
					return;

				case '>': // End of statement
					skel.append((char)c);
					blockLevel--;
					if (this.inPgfCatalog && this.pgfCatalogLevel > blockLevel) {
						this.inPgfCatalog = false;
					}
					// Return skeleton
					DocumentPart dp = new DocumentPart(String.valueOf(++otherId), false, skel);
					queue.add(new Event(EventType.DOCUMENT_PART, dp));
					return;

				default:
					skel.append((char)c);
					break;
				}
			}

			// We are done
			Ending ending = new Ending(String.valueOf(++otherId));
			queue.add(new Event(EventType.END_DOCUMENT, ending, skel));
		}
		catch ( IOException e ) {
			throw new OkapiIOException(e);
		}
	}

	/**
	 * Skips over the content of the current statement.
	 * Normally "<token" has been processed and level for after '<'
	 * @param store true to store in the skeleton
	 * @param buffer the StringBuilder object where to copy the content, or null to not copy.
	 * @throws IOException if an error occurs.
	 */
	private void skipOverContent (boolean store,
		StringBuilder buffer)
		throws IOException
	{
		int baseLevel = 1;
		int state = 0;
		int c;

		while ( (c = reader.read()) != -1 ) {
			// Store if needed
			if ( store ) {
				if ( buffer != null ) buffer.append((char)c);
				else skel.append((char)c);
			}

			// Parse according current state
			switch ( state ) {
			case 0:
				switch ( c ) {
				case '`':
					state = 1; // In string
					continue;
				case '\\':
					state = 2; // In escape
					continue;
				case '<':
					baseLevel++;
					tagBuffer.setLength(0);
					state = 3; // In tag buffer mode
					continue;
				case '>':
					baseLevel--;
					if ( baseLevel == 0 ) {
						return;
					}
					continue;
				}
				// Else do nothing
				continue;

			case 1: // In string
				if ( c == '\'' ) state = 0;
				continue;

			case 2: // In escape
				state = 0; // Go back to normal
				continue;

			case 3: // In tag buffer mode
				switch ( c ) {
				case '>':
					baseLevel--;
					if ( baseLevel == 0 ) {
						return;
					}
					// Fall thru
				case ' ':
				case '\t':
					if ( tagBuffer.toString().equals(IMPORTOBJECT) ) {
						skipOverImportObject(store, buffer);
						baseLevel--;
					}
					state = 0;
					continue;
				default:
					tagBuffer.append((char)c);
					continue;
				}
			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException(
			String.format("Unexpected end of input at state = %d", state));
	}

	private void readComment (boolean store,
		StringBuilder sb)
		throws IOException
	{
		int c;
		while ( (c = reader.read()) != -1 ) {
			if ( store ) {
				if ( sb != null ) sb.append((char)c);
				else skel.append((char)c);
			}
			switch ( c ) {
			case '\r':
			case '\n':
				return;
			}
		}
		// A comment can end the file
	}

	private boolean startBlock (int stopLevel,
		BlockType type)
		throws IOException
	{
		if ( type == BlockType.TABLE ) {
			// Get the table id
			String tag = readUntil("TblID;", true, null, stopLevel, true);
			if ( tag == null ) {
				// Error: ID missing
				throw new OkapiIOException("Missing id for the table.");
			}
			Token token = firstLiteralTokenInStatement(true, true);
			if (token.toString().isEmpty()) {
				throw new OkapiIOException("Missing id value for the table.");
			}
			// If the table is not listed as to be extracted: we skip it
			if (!this.extracts.tableExtractable(token.toString())) {
				skipOverContent(true, null);
				blockLevel--;
				return false;
			}
			tableGroupLevel = blockLevel;
			StartGroup sg = new StartGroup(parentIds.peek());
			sg.setId(parentIds.push(String.valueOf(++grpId)));
			sg.setType("table");
			queue.add(new Event(EventType.START_GROUP, sg));
			// If tables==null it's because we didn't have any page, so we extract by default
			// Else: extract: use fall thru code
		}
		else if ( type == BlockType.TEXT_FLOW ) {
			// If the text flow is not listed as to be extracted: we skip it
			if (!this.extracts.textFlowExtractable(String.valueOf(textFlowNumber))) {
				skipOverContent(true, null);
				blockLevel--;
				return false;
			}
			// If textFlows==null it's because we didn't have any page, so we extract by default
			// Else: extract: use fall thru code
		}
		else if ( type == BlockType.PARAGRAPH_STYLE) {
			String tag = readUntil("PgfTag;", true, null, stopLevel, true);
			if ( tag == null ) {
				throw new OkapiIOException("Missing PgfTag for the Pgf.");
			}
			Token token = firstLiteralTokenInStatement(true, true);
			if (token.toString().isEmpty()) {
				throw new OkapiIOException("Missing the PgfTag value.");
			}
			if (!this.extracts.paragraphFormatTagExtractable(token.toString())) {
				skipOverContent(true, null);
				blockLevel--;
				return false;
			} else {
				processBlock(stopLevel);
				return true;
			}
		}

		// Extract
		processBlock(stopLevel);
		return true;
	}

	/**
	 * Process the first or next entry of a TextFlow statement.
	 * @throws IOException if a low-level error occurs.
	 */
	private void processBlock (int stopLevel)
		throws IOException
	{
		// Process one Pgf statement at a time
		if ( this.inPgfCatalog ) {
			processPara();
			blockLevel--; // Closing the Pgf statement here
			inBlock = 0; // We are done
		}
		else {
			if ( readUntil("Para;", true, null, stopLevel, false) != null ) {
				processPara();
				blockLevel--; // Closing the Para statement here
				inBlock = stopLevel; // We are not done yet with this TextFlow statement
			}
			else { // Done
				inBlock = 0; // We are done
				// Note that the end-group for a table is send when we detect the closing '>'
			}
		}

		// If needed, create a document part and return
		if ( !skel.isEmpty() ) {
			queue.add(new Event(EventType.DOCUMENT_PART,
				new DocumentPart(String.valueOf(++otherId), false),
				skel));
		}
	}

	private void processPara ()
		throws IOException
	{
		TextFragment tf = new TextFragment();
		boolean first = true;
		paraLevel = 1;
		paraSkelBuf.setLength(0);
		paraTextBuf.setLength(0);
		paraCodeBuf.setLength(0);
		paraCodeTypes.setLength(0);
		String endString = "";
		final Deque<Code> codes = new LinkedList<>();
		boolean extractedReferent = false;
		String extractedStringTag = "";
		boolean forceStringClosing = false;

		// Go to the first ParaLine
		int res = readUntilText(first, false);
		while ( res > 0 ) {

			// Get the text to append
			switch ( res ) {
				case 2: // Extracted marker
					while (!this.referentTextUnits.isEmpty()) {
						final Code code = new Code(
							TagType.PLACEHOLDER,
							"index",
							endString.concat(TextFragment.makeRefMarker(this.referentTextUnits.poll().getId()))
						);
						code.setReferenceFlag(true);
						codes.add(code);
						extractedReferent = true;
					}
					break;
				case 3:
					if (!this.params.getExtractPgfNumFormatsInline() && !this.inPgfCatalog && !this.referentTextUnits.isEmpty()) {
						while (!this.referentTextUnits.isEmpty()) {
							final Code code = new Code(
								TagType.PLACEHOLDER,
								Code.TYPE_REFERENCE,
								TextFragment.makeRefMarker(this.referentTextUnits.poll().getId())
							);
							code.setReferenceFlag(true);
							codes.add(code);
						}
						extractedReferent = true;
					} else {
						extractedReferent = false;
					}
					extractedStringTag = "<PgfNumFormat `";
					endString = "'>";
					break;
				default:
					extractedReferent = false;
					extractedStringTag = "<String `";
					endString = "'>";
			}

			if ( first ) {
				if ( paraSkelBuf.length() > 0 ) {
					skel.append(paraSkelBuf.toString());
					if ( paraCodeBuf.length() > 0 ) {
						final String type = paraCodeTypes.length() > 0
							? paraCodeTypes.toString()
							: "code";
						final Code code2 = new Code(
							TagType.PLACEHOLDER,
							type,
							paraCodeBuf.toString().concat(extractedStringTag)
						);
						paraCodeBuf.setLength(0);
						tf.append(code2);
					} else if (codes.isEmpty()) {
						skel.append(extractedStringTag);
					}
				}
			}
			if ( paraCodeBuf.length() > 0 ) {
				final String type = paraCodeTypes.length() > 0
					? paraCodeTypes.toString()
					: "code";
				final String data;
				if (tf.hasCode()) {
					final String codedText = tf.getCodedText();
					if (TextFragment.isMarker(codedText.charAt(codedText.length() - 2))) {
						data = "";
					} else {
						data = endString;
					}
				} else {
					data = endString;
				}
				final Code code2 = new Code(
					TagType.PLACEHOLDER,
					type,
					data.concat(paraCodeBuf.toString()).concat(extractedStringTag)
				);
				tf.append(code2);
			}

			while (!codes.isEmpty()) {
				tf.append(codes.poll());
			}
			if ( paraTextBuf.length() > 0 ) {
				if (this.params.getExtractHardReturnsAsText() || !paraTextBuf.toString().contains(LINE_FEED)) {
					if (this.inXRef && !first && (0 == this.paraCodeBuf.length() || this.newParaLine) && !this.charProcessed && !this.stringAfterCharProcessed) {
						if (this.params.getExtractHardReturnsAsText()) {
							tf.append(LINE_FEED);
						} else {
							checkInlineCodes(tf);
							if (!tf.hasText()) {
								skel.append(HARD_RETURN);
								skel.append(toMIFString(tf));
								tf = new TextFragment();
							} else {
								final ITextUnit tu = textUnit(tf, "", "", skel);
								this.queue.add(new Event(EventType.TEXT_UNIT, tu));
								skel.append(HARD_RETURN);
								tf = new TextFragment();
								skel = new GenericSkeleton();
							}
						}
					}
					tf.append(paraTextBuf.toString());
				} else {
					if (this.inXRef && !first && (0 == this.paraCodeBuf.length() || this.newParaLine) && !this.charProcessed && !this.stringAfterCharProcessed) {
						skel.append(HARD_RETURN);
					}
					for (int i = 0; i < paraTextBuf.length(); i++) {
						if ('\n' != paraTextBuf.charAt(i)) {
							tf.append(paraTextBuf.charAt(i));
							continue;
						}
						// we faced a hard return
						forceStringClosing = true;
						if (tf.isEmpty()) {
							skel.append(HARD_RETURN);
							continue;
						}
						checkInlineCodes(tf);
						if (!tf.hasText()) {
							skel.append(toMIFString(tf));
							skel.append(HARD_RETURN);
							tf = new TextFragment();
							continue;
						}
						addTextUnit(tf, "", "", skel);
						skel.append(HARD_RETURN);
						tf = new TextFragment();
						skel = new GenericSkeleton();
					}
				}
				if (this.stringAfterCharProcessed) {
					this.stringAfterCharProcessed = false;
				}
			}

			first = false;
			this.newParaLine = false;
			// Reset the codes buffer for next sequence
			paraSkelBuf.setLength(0);
			paraTextBuf.setLength(0);
			paraCodeBuf.setLength(0);
			paraCodeTypes.setLength(0);
			// Move to the next text
			res = readUntilText(first, extractedReferent);
		}

		checkInlineCodes(tf);

		if ( !tf.isEmpty() ) {
			if ( tf.hasText() || extractedReferent ) {
				addTextUnit(tf, "", "", skel);
				if (!extractedReferent) {
					skel.append(endString);
				}
			}
			else { // No text (only codes and/or white spaces) Put back the content/codes in skeleton
				// We need to escape the text parts (white spaces like tabs)
				skel.append(toMIFString(tf));
				skel.append(endString);
			}
		} else {
			if (forceStringClosing) {
				skel.append(endString);
				forceStringClosing = false;
			}
		}

		// Ending part
		if ( paraSkelBuf.length() > 0 ) {
			skel.append(paraSkelBuf.toString());
		}
		if ( paraCodeBuf.length() > 0 ) {
			skel.append(paraCodeBuf.toString());
		}

		if (tf.hasText() || extractedReferent) {
			// New skeleton object for the next parts of the parent statement
			skel = new GenericSkeleton();
		}
	}
	
	private Token firstLiteralTokenInStatement(boolean store, boolean updateBlockLevel) throws IOException {
		final Statement statement = this.document.currentMarkup();
		if (store) {
			this.skel.add(statement.toString());
		}
		if (updateBlockLevel) {
			this.blockLevel--;
		}
		return statement.firstTokenOf(Token.Type.LITERAL);
	}

	/**
	 * Processes a <Marker entry.
	 * @param startTag A start tag
	 * @return A string builder of the skeleton
	 */
	private StringBuilder processMarker(final String startTag)
		throws IOException
	{
		int level = blockLevel;
		final StringBuilder sb = new StringBuilder(startTag);

		String tag = readUntil("MTypeName;", true, sb, -1, true);
		if ( tag == null ) {
			logger.warn("Marker without type or text found. It will be skipped.");
			skipOverContent(true, sb);
			return sb;
		}

		// Is it a marker we need to extract?
		String type = processString(true, sb);
		String resType = null;
		if ( "Index".equals(type) ) {
			if ( params.getExtractIndexMarkers() ) resType = "x-index";
		}
		else if ( "Hypertext".equals(type) ) {
			if ( params.getExtractLinks() ) resType = "link";
		}

		if ( resType == null ) {
			// Not to extract
			skipOverContent(true, sb);
			blockLevel = level;
			return sb;
		}

		// Else: it is to extract: get the string
		tag = readUntil("MText;", true, sb, -1, true);
		if ( tag == null ) {
			skipOverContent(true, sb);
			blockLevel = level;
			return sb; // Nothing to extract
		}

		TextFragment tf = new TextFragment(processString(true, sb));
		if (tf.isEmpty()) {
			// Store the remaining part of the marker
			skipOverContent(true, sb);
			blockLevel= level;
			return sb;
		}
		checkInlineCodes(tf);
		if (!tf.hasText() ) {
			skipOverContent(true, sb);
			blockLevel= level;
			return sb;
		}
		addReferentTextUnits(tf, sb, resType);
		blockLevel = level;
		return sb;
	}

	private void addReferentTextUnits(
		final TextFragment textFragment,
		final StringBuilder stringBuilder,
		final String type
	) throws IOException {
		int n = stringBuilder.lastIndexOf("`");
		stringBuilder.delete(n + 1, stringBuilder.length());
		GenericSkeleton skel = new GenericSkeleton(stringBuilder.toString());
		if (this.params.getExtractHardReturnsAsText() || !textFragment.toString().contains(LINE_FEED)) {
			final ITextUnit tu = referentTextUnit(textFragment, "", type, skel);
			this.referentTextUnits.add(tu);
			this.queue.add(new Event(EventType.TEXT_UNIT, tu, skel));
		} else {
			TextFragment tf = new TextFragment();
			for (int i = 0; i < textFragment.length(); i++) {
				if (TextFragment.isMarker(textFragment.charAt(i))) {
					tf.append(textFragment.getCode(TextFragment.toIndex(textFragment.charAt(++i))));
					continue;
				}
				if ('\n' != textFragment.charAt(i)) {
					tf.append(textFragment.charAt(i));
					continue;
				}
				if (tf.isEmpty()) {
					skel.append(HARD_RETURN);
					continue;
				}
				checkInlineCodes(tf);
				if (!tf.hasText()) {
					skel.append(toMIFString(tf));
					skel.append(HARD_RETURN);
					tf = new TextFragment();
					continue;
				}
				final ITextUnit tu = referentTextUnit(tf, "", type, skel);
				skel.append(HARD_RETURN);
				this.referentTextUnits.add(tu);
				this.queue.add(new Event(EventType.TEXT_UNIT, tu, skel));
				tf = new TextFragment();
				skel = new GenericSkeleton();
			}
		}
		stringBuilder.setLength(0);
		stringBuilder.append("'>");
		skipOverContent(true, stringBuilder);
		((GenericSkeleton) this.referentTextUnits.peekLast().getSkeleton()).add(stringBuilder.toString());
		stringBuilder.setLength(0);
	}

	private ITextUnit referentTextUnit(
		final TextFragment textFragment,
		final String name,
		final String type,
		final GenericSkeleton skeleton
	) {
		final ITextUnit tu = textUnit(textFragment, name, type, skeleton);
		tu.setIsReferent(true);
		return tu;
	}

	private ITextUnit textUnit(
		final TextFragment textFragment,
		final String name,
		final String type,
		final GenericSkeleton skeleton
	) {
		final ITextUnit tu = new TextUnit(String.valueOf(++tuId));
		tu.setPreserveWhitespaces(true);
		tu.setSourceContent(textFragment);
		tu.setName(name);
		tu.setType(type);
		tu.setMimeType(MimeTypeMapper.MIF_MIME_TYPE);
		processILC(tu);
		skeleton.addContentPlaceholder(tu);
		tu.setSkeleton(skeleton);
		TextUnitUtil.simplifyCodes(tu, this.params.getSimplifierRules(), true);
		return tu;
	}

	private void addTextUnit(
		final TextFragment textFragment,
		final String name,
		final String type,
		final GenericSkeleton skeleton
	) {
		final ITextUnit tu = textUnit(textFragment, name, type, skeleton);
		this.queue.add(new Event(EventType.TEXT_UNIT, tu));
	}

	/**
	 * Reads until the next text.
	 * @param startOfPara true to indicate a start of paragraph.
	 * @param significant indicates if the current buffer is significant or can be removed.
	 * Set always to false except sometimes when called recursively.
	 * @return 0=end of paragraph, 1,3=text, 2=marker
	 */
	private int readUntilText (boolean startOfPara,
		boolean significant)
		throws IOException
	{
		StringBuilder sb;
		if ( startOfPara ) sb = paraSkelBuf;
		else sb = paraCodeBuf;

		int c;
		while ( (c = reader.read()) != -1 ) {
			switch ( c ) {
			case '#':
				sb.append((char)c);
				readComment(true, sb);
				break;

			case '<': // Start of statement
				paraLevel++;
				sb.append((char)c);
				String tag = readTag(true, false, sb);
				if ( "ParaLine".equals(tag)  || "Pgf".equals(tag) ) {
					if ("Pgf".equals(tag)) {
						this.inPgf = true;
						this.extractedPgfNumFormat = false;
					} else {
						this.newParaLine = true;
					}
					if ( !startOfPara ) {
						int n = sb.lastIndexOf("<");
						if ( significant ) {
							if ( !this.extractedPgfNumFormat ) {
								sb.delete(n, sb.length());
							}
						}
						else {
							sb.setLength(0);
						}
					}
					return readUntilText(startOfPara, significant);
				}
				else if ( "String".equals(tag) || "PgfNumFormat".equals(tag) ) {
					String text = processString(true, sb);
					paraLevel--;
					if ( !Util.isEmpty(text) ) {
						if ("String".equals(tag)) {
							int n = sb.lastIndexOf("<".concat(tag));
							if ( significant ) sb.delete(n, sb.length());
							else sb.setLength(0);
							paraTextBuf.append(text);
							if (this.charProcessed) {
								this.charProcessed = false;
								this.stringAfterCharProcessed = true;
							}
							return 1;
						} else {
							if (this.params.getExtractPgfNumFormatsInline() || this.inPgfCatalog) {
								int n = sb.lastIndexOf("<".concat(tag));
								if ( significant ) sb.delete(n, sb.length());
								else sb.setLength(0);
								paraTextBuf.append(text);
								this.extractedPgfNumFormat = true;
								return 3;
							} else {
								TextFragment tf = new TextFragment(text);
								checkInlineCodes(tf);
								if (tf.hasText()) {
									addReferentTextUnits(tf, sb, "x-referent");
									paraLevel--;
									this.inPgf = false;
									this.extractedPgfNumFormat = true;
									return 3;
								} else {
									// store the remaining part
									skipOverContent(true, sb);
									paraLevel--;
									this.inPgf = false;
								}
							}
						}
					}
					// Else: continue. This basically remove the empty string
				}
				else if ( "Char".equals(tag) ) {
					final String text = new CharLiteralToken(
						firstLiteralTokenInStatement(false, false),
						logger
					).toString();
					if ( !significant ) sb.setLength(0);
					paraLevel--;
					if (!text.isEmpty()) {
						paraTextBuf.append(text);
						this.charProcessed = true;
						return 1;
					}
				}
				else if ( "Marker".equals(tag) ) {
					int n = sb.lastIndexOf("<".concat(tag));
					String startTag = sb.substring(n);
					if ( significant ) sb.delete(n, sb.length());
					else sb.setLength(0);
					final StringBuilder msb = processMarker(startTag);
					significant = true;
					if ( paraCodeTypes.length() > 0 ) paraCodeTypes.append(";");
					paraCodeTypes.append(tag.toLowerCase());
					paraLevel--;
					if (0 == msb.length()) { // We have text units
						return 2;
					}
					// No text units: nothing to extract
					sb.append(msb);
				}
				// Default: skip over
				else {
					if ("XRef".equals(tag)) {
						this.inXRef = true;
					}
					if ("XRefEnd".equals(tag)) {
						this.inXRef = false;
					}
					skipOverContent(true, sb);
					significant = true;
					if ( paraCodeTypes.length() > 0 ) paraCodeTypes.append(";");
					paraCodeTypes.append(tag.toLowerCase());
					paraLevel--;
				}

				if ( startOfPara ) {
					// Check for inline codes: tags that should be inline even when they are leading before any text
					if ("Font;Marker;Conditional;Unconditional;ATbl;AFrame;Note;Variable;XRef;XRefEnd;".contains(tag)) {
						// Switch buffer
						int n = sb.lastIndexOf("<".concat(tag));
						paraCodeBuf.append(sb.substring(n));
						sb.delete(n, sb.length()); // Remove from buffer since it's in the code now
						sb = paraCodeBuf;
						paraCodeTypes.setLength(0); // Rest for inline codes
						paraCodeTypes.append(tag.toLowerCase());
						startOfPara = false; // Done
					}
				}
				break;

			case '>': // End of statement
				paraLevel--;
				if ( paraLevel == 1 && this.inPgf ) {
					sb.append((char)c);
					significant = true;
					this.inPgf = false;
				}
				else if ( paraLevel != 1 && !this.inPgf ) { // Exclude closing ParaLine
					sb.append((char)c);
					significant = true;
				}
				if ( paraLevel == 0 && this.inPgfCatalog ) {
					return 0;
				}
				else if ( paraLevel == 0 ) {
					// Add final close of ParaLine
					int n = sb.lastIndexOf(" # end of ParaLine");
					// Do it before the corresponding comment if possible
					if ( n > -1 ) {
						sb.insert(n, '>');
					}
					else {
						sb.append(" # end of ParaLine").append(lineBreak).append(">");
					}
					return 0;
				}
				break;

			default:
				sb.append((char)c);
				break;
			}
		}
		return 0;
	}

	/**
	 * Reads until the first occurrence of one of the given statements, or (if stopLevel
	 * is -1) at the end of the current level, or at the end of the given level.
	 * @param tagNames the list of tag names to stop at (separated and ending with ';')
	 * @param store true if we store the parsed characters into the skeleton.
	 * @param stopLevel -1=return if the end of the current blockLevel is reached.
	 * @param skipNotesBlock
	 * other values=return if the blockLevel get lower than that value
	 * False to stop when it reaches 0.
	 * @return the name of the tag found, or null if none was found.
	 * @throws IOException if a low-level error occurs.
	 */
	private String readUntil (String tagNames,
		boolean store,
		StringBuilder sb,
		int stopLevel,
		boolean skipNotesBlock)
		throws IOException
	{
		int endNow = stopLevel;
		if ( stopLevel == -1 ) {
			endNow = blockLevel;
		}

		int c;
		while ( (c = reader.read()) != -1 ) {
			if ( store ) {
				if ( sb == null ) skel.append((char)c);
				else sb.append((char)c);
			}
			switch ( c ) {
			case '#':
				readComment(store, sb);
				break;

			case '<': // Start of statement
				while ( true ) {
					blockLevel++;
					String tag = readTag(store, true, sb);
					if (tagNames.contains(tag + ";")) {
						if ( !skipNotesBlock || ( footnotesLevel == -1) ) {
							return tag;
						}
						break;
					}
					else if ( "Tbl".equals(tag) ) {
						tableGroupLevel = blockLevel;
						// Note that the start-group event is send from the startBlock() method
						// But the end-group event is send from this method.
						break;
					}
					else if ( "Row".equals(tag) ) {
						rowGroupLevel = blockLevel;
						StartGroup sg = new StartGroup(parentIds.peek());
						sg.setId(parentIds.push(String.valueOf(++grpId)));
						sg.setType("row");
						queue.add(new Event(EventType.START_GROUP, sg));
						break;
					}
					else if ( "Cell".equals(tag) ) {
						cellGroupLevel = blockLevel;
						StartGroup sg = new StartGroup(parentIds.peek(), String.valueOf(++grpId));
						sg.setType("cell");
						queue.add(new Event(EventType.START_GROUP, sg));
						break;
					}
					else if ( "Notes".equals(tag) ) {
						footnotesLevel = blockLevel;
						break;
					}
					else if ( "Note".equals(tag) ) {
						if ( footnotesLevel > 0 ) {
							fnoteGroupLevel = blockLevel;
							StartGroup sg = new StartGroup(parentIds.peek(), String.valueOf(++grpId));
							sg.setType("fn");
							queue.add(new Event(EventType.START_GROUP, sg));
						}
						break;
					}
					else if ( IMPORTOBJECT.equals(tag) ) {
						skipOverImportObject(store, sb);
						blockLevel--;
						break;
					}
					else { // Default: skip over
						if ( !readUntilOpenOrClose(store, sb) ) {
							blockLevel--;
							break;
						}
						// Else: re-process the next tag
					}
					// Else: re-process the next tag
				}
				break;
				
			case '>': // End of statement
				if ( tableGroupLevel == blockLevel ) {
					tableGroupLevel = -1;
					queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
					parentIds.pop();
				}
				else if ( rowGroupLevel == blockLevel ) {
					rowGroupLevel = -1;
					queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
					parentIds.pop();
				}
				else if ( cellGroupLevel == blockLevel ) {
					cellGroupLevel = -1;
					queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
				}
				else if ( footnotesLevel == blockLevel ) {
					footnotesLevel = -1;
				}
				else if ( fnoteGroupLevel == blockLevel ) {
					if ( footnotesLevel > 0 ) {
						fnoteGroupLevel = -1;
						queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
					}
				}
				blockLevel--;
				if ( blockLevel < endNow ) {
					return null;
				}
				break;
			}
		}
		//TODO: we shouldn't exit this way, except when starting at 0
		return null;
	}

	private void skipOverImportObject (boolean store,
		StringBuilder buffer)
		throws IOException
	{
		// At the point only the tag has been read
		// We should leave after the corresponding '>' is found
		// The content may have one or more inset data (start with line-break and '&' per line)
		int state = 0;
		int c;
		int baseLevel = 1;

		while ( (c = reader.read()) != -1 ) {
			// Store if needed
			if ( store ) {
				if ( buffer != null ) buffer.append((char)c);
				else skel.append((char)c);
			}

			// Parse according current state
			switch ( state ) {
			case 0: // In facet mode wait for line-break
				switch ( c ) {
				case '`':
					state = 1; // In string
					continue;
				case '<':
					baseLevel++;
					continue;
				case '>':
					baseLevel--;
					if ( baseLevel == 0 ) {
						// We are done
						return;
					}
				case '\r':
				case '\n':
					state = 3;
					continue;
				}
				// Else: stay in this current state
				continue;

			case 1: // In string
				if ( c == '\'' ) {
					state = 0; // Back to normal
				}
				continue;

			case 2: // In escape
				state = 0; // Back to normal
				continue;

			case 3: // After \r or \r: wait for & or =
				switch ( c ) {
				case '&':
					state = 4; // In facet line
					continue;
				case '<':
					state = 0;
					baseLevel++;
					continue;
				case '>':
					state = 0;
					baseLevel--;
					if ( baseLevel == 0 ) {
						return; // Done
					}
					continue;

				case '\n':
				case '\r':
					// Stay in this current state
					continue;
				default:
					// Back to within an ImportObject (after a line-break)
					state = 0;
					continue;
				}

			case 4: // Inside a facet line, waiting for end-of-line
				if (( c == '\r' ) || ( c == '\n' )) {
					state = 3; // Back to after a line-break state
				}
				continue;

			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException(
			String.format("Unexpected end of input at state = %d", state));
	}

	/**
	 * Reads until the next opening or closing statement.
	 * @param store
	 * @return true if stops on opening, false if stops on closing.
	 * @throws IOException if the end of file occurs.
	 */
	private boolean readUntilOpenOrClose (boolean store,
		StringBuilder sb)
		throws IOException
	{
		int c;
		boolean inEscape = false;
		boolean inString = false;
		while ( (c = reader.read()) != -1 ) {
			if ( store ) {
				if ( sb == null ) skel.append((char)c);
				else sb.append((char)c);
			}
			// Parse a string content
			if ( inString ) {
				if ( c == '\'' ) inString = false;
				continue;
			}
			// Else: we are outside a string
			if ( inEscape ) {
				inEscape = false;
			}
			else {
				switch ( c ) {
				case '`':
					inString = true;
					break;
				case '\\':
					inEscape = true;
					break;
				case '<':
					return true;
				case '>':
					return false;
				}
			}
		}
		// Unexpected end
		throw new OkapiIllegalFilterOperationException("Unexpected end of input.");
	}

	/**
	 * Reads a tag name.
	 * @param store true to store the tag codes
	 * @param storeCharStatement true to store if it's a Char statement.
	 * @param sb Not null to store there, null to store in the skeleton.
	 * @return The name of the tag.
	 * @throws IOException
	 */
	private String readTag (boolean store,
		boolean storeCharStatement,
		StringBuilder sb)
		throws IOException
	{
		tagBuffer.setLength(0);
		int c;
		int wsStart = ((sb != null ) ? sb.length()-1 : -1);
		boolean leadingWSDone = false;
		// Skip and whitespace between '<' and the name
		do {
			switch ( c = reader.read() ) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				if ( store ) {
					if ( sb != null ) sb.append((char)c);
					else skel.add((char)c);
				}
				break;
			case -1:
			default:
				leadingWSDone = true;
				break;
			}
		}
		while ( !leadingWSDone );

		// Now read the name
		while ( true ) {
			switch ( c ) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				if ( store ) {
					if ( !storeCharStatement && tagBuffer.toString().equals("Char") ) {
						// Special case for <Char...>: we don't store it
						if ( wsStart > 0 ) {
							sb.delete(wsStart, sb.length());
						}
					}
					else {
						if ( sb != null ) {
							sb.append(tagBuffer.toString());
							sb.append((char)c);
						}
						else {
							skel.append(tagBuffer.toString());
							skel.append((char)c);
						}
					}
				}
				return tagBuffer.toString();

			case -1:
				throw new OkapiIllegalFilterOperationException("Unexpected end of input.");

			default:
				tagBuffer.append((char)c);
				break;
			}
			c = reader.read();
		}
	}

	private void processFormats(final String name) throws IOException {
		// We are inside Formats
		// blockLevel should be 1
		boolean startGroupDone = false;
		String tag = null;

		do {
			tag = readUntil(name.concat("Format;"), true, null, blockLevel-1, true);
			if ( tag != null ) {
				tag = readUntil(name.concat("Name;"), true, null, blockLevel-1, true);
				if ( tag != null) {
					final String formatName = processString(true, null);
					if (name.equals("XRef") && !this.extracts.referenceFormatTagExtractable(formatName)) {
						continue;
					}
					tag = readUntil(name.concat("Def;"), true, null, blockLevel - 1, true);
					if (tag != null) {
						String text = processString(false, null);
						TextFragment tf = new TextFragment();
						skel.append("`");
						if (this.params.getExtractHardReturnsAsText() || !text.contains(LINE_FEED)) {
							tf.append(text);
							checkInlineCodes(tf);
							// If we have only white spaces and/or codes
							if (tf.hasText()) {
								if (!startGroupDone) {
									addStartGroup(name);
									startGroupDone = true;
								}
								addTextUnit(tf, formatName, name.concat("Format"), skel);
								skel = new GenericSkeleton();
							}
							else { // Put back the text in the skeleton
								skel.append(toMIFString(tf));
							}
						} else {
							for (int i = 0; i < text.length(); i++) {
								if ('\n' != text.charAt(i)) {
									tf.append(text.charAt(i));
									continue;
								}
								if (tf.isEmpty()) {
									skel.append(HARD_RETURN);
									continue;
								}
								checkInlineCodes(tf);
								if (!tf.hasText()) {
									skel.append(toMIFString(tf));
									skel.append(HARD_RETURN);
									tf = new TextFragment();
									continue;
								}
								if (!startGroupDone) {
									addStartGroup(name);
									startGroupDone = true;
								}
								addTextUnit(tf, formatName, name.concat("Format"), skel);
								skel.append(HARD_RETURN);
								tf = new TextFragment();
								skel = new GenericSkeleton();
							}
						}
						skel.append("'>");
					}
				}
			}
		}
		while ( tag != null );

		if ( startGroupDone ) {
			// Send the end-group if needed
			queue.add(new Event(EventType.END_GROUP, new Ending(String.valueOf(++grpId))));
		}

	}

	private void addStartGroup(final String name) {
		final StartGroup sg = new StartGroup(parentIds.peek());
		sg.setId(String.valueOf(++grpId));
		sg.setType(name.concat("s"));
		queue.add(new Event(EventType.START_GROUP, sg));
	}

	private void processPage(final int stopLevel) throws IOException {
		String tag = readUntil("PageType;", true, null, stopLevel, true);
		if ( tag == null ) {
			throw new OkapiIOException("Missing PageType of Page");
		}
		Token token = firstLiteralTokenInStatement(true, true);
		if (token.toString().isEmpty()) {
			throw new OkapiIOException("Missing PageType value of Page");
		}
		if (!this.extracts.pageTypeExtractable(token.toString())) {
			skipOverContent(true, null);
			blockLevel--;
			return;
		}
		processFramesAndTextLines(stopLevel);
	}

	private void processFrame(final int stopLevel) throws IOException {
		String tag = readUntil("Unique;", true, null, stopLevel, true);
		if ( tag == null ) {
			throw new OkapiIOException("Missing Unique of Frame");
		}
		Token token = firstLiteralTokenInStatement(true, true);
		if (token.toString().isEmpty()) {
			throw new OkapiIOException("Missing Unique value of Frame");
		}
		if (!this.extracts.frameExtractable(token.toString())) {
			skipOverContent(true, null);
			blockLevel--;
			return;
		}
		processFramesAndTextLines(stopLevel);
	}

	private void processFramesAndTextLines(final int stopLevel) throws IOException {
		String tag;
		do {
			tag = readUntil("Frame;TextLine;", true, null, stopLevel, true);
			if ("Frame".equals(tag)) {
				processFrame(stopLevel + 1);
			} else if ("TextLine".equals(tag)) {
				processTextLine(stopLevel + 1);
			}
		} while (null != tag);
	}

	private void processTextLine(final int stopLevel) throws IOException {
		String tag = readUntil("String;", true, null, stopLevel, true);
		if (null == tag) {
			return;
		}
		String text = processString(false, null);
		TextFragment tf = new TextFragment();
		skel.append("`");
		if (this.params.getExtractHardReturnsAsText() || !text.contains(LINE_FEED)) {
			tf.append(text);
			checkInlineCodes(tf);
			if (tf.hasText()) {
				addTextUnit(tf, "","TextLine", skel);
				skel = new GenericSkeleton();
			} else {
				skel.append(toMIFString(tf));
			}
		} else {
			for (int i = 0; i < text.length(); i++) {
				if ('\n' != text.charAt(i)) {
					tf.append(text.charAt(i));
					continue;
				}
				if (tf.isEmpty()) {
					skel.append(HARD_RETURN);
					continue;
				}
				checkInlineCodes(tf);
				if (!tf.hasText()) {
					skel.append(toMIFString(tf));
					skel.append(HARD_RETURN);
					tf = new TextFragment();
					continue;
				}
				addTextUnit(tf, "","TextLine", skel);
				skel.append(HARD_RETURN);
				tf = new TextFragment();
				skel = new GenericSkeleton();
			}
		}
		skel.append("'>");
		blockLevel--;
	}

	private String toMIFString (TextFragment tf) {
		String ctext = tf.getCodedText();
		StringBuilder tmp = new StringBuilder();
		for ( int i=0; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			if ( TextFragment.isMarker(ch) ) {
				tmp.append(tf.getCode(ctext.charAt(++i)));
			}
			else {
				tmp.append(encoder.encode(ch, EncoderContext.TEXT));
			}
		}
		return tmp.toString();
	}

	private void checkInlineCodes (TextFragment tf) {
		if ( params.getUseCodeFinder() ) {
			params.getCodeFinder().process(tf);
		}
		// Escape inline code content
		List<Code> codes = tf.getCodes();
		for ( Code code : codes ) {
			// Escape the data of the new inline code (and only them)
			if ( code.getType().equals(InlineCodeFinder.TAGTYPE) ) {
				code.setData(encoder.encode(code.getData(), EncoderContext.INLINE));
			}
		}
	}

	private String processString (boolean store,
		StringBuilder sb)
		throws IOException
	{
		strBuffer.setLength(0);
		int c;
		int state = 0;

		while ( (c = reader.read()) != -1 ) {

			if ( store ) {
				if ( sb == null ) skel.append((char)c);
				else sb.append((char)c);
			}

			switch ( state ) {
			case 0: // Outside the string
				switch ( c ) {
				case '`':
					state = 1; // In string
					continue;
				case '>':
					return strBuffer.toString();
				}
				continue;

			case 1: // In string
				switch ( c ) {
				case '\'': // End of string
					state = 0;
					continue;
				case '\\':
					state = 2; // In escape mode
					continue;
				default:
					strBuffer.append((char)c);
					continue;
				}

			case 2: // In escape mode (after a backslash)
				state = 1; // Reset to in-string state
				switch ( c ) {
				case '\\':
				case '>':
					strBuffer.append((char)c);
					continue;
				case 't':
					strBuffer.append('\t');
					continue;
				case 'n':
					strBuffer.append('\n');
					continue;
				case 'Q':
					strBuffer.append('`');
					continue;
				case 'q':
					strBuffer.append('\'');
					continue;
				case 'u':
					c = readHexa(4, false, store, sb);
					if ( c == Integer.MAX_VALUE ) {
						continue; // warning already logged
					}
					strBuffer.append((char)c);
					continue;
				case 'x':
					c = readHexa(2, true, store, sb);
					if ( c == Integer.MAX_VALUE ) {
						continue; // warning already logged
					}
					strBuffer.append(new Hexadecimal(c, logger).toString());
				}
			}

		}
		// Else: Missing end of string error
		throw new OkapiIllegalFilterOperationException("End of string is missing.");
	}

	private int readHexa (int length,
		boolean readExtraSpace,
		boolean store,
		StringBuilder sb)
		throws IOException
	{
		tagBuffer.setLength(0);
		int c;
		// Fill the buffer
		for ( int i=0; i<length; i++ ) {
			c = reader.read();
			if ( c == -1 ) {
				throw new OkapiIllegalFilterOperationException("Unexpected end of file.");
			}
			if ( store ) {
				if ( sb == null ) skel.append((char)c);
				else sb.append((char)c);
			}
			tagBuffer.append((char)c);
		}
		if ( readExtraSpace ) {
			reader.read(); // the followed-by space is added later on conversion
		}

		// Try to convert
		try {
			int n = Integer.valueOf(tagBuffer.toString(), 16);
			return n;
		}
		catch ( NumberFormatException e ) {
			// Log warning
			logger.warn("Invalid escape sequence found: '{}'", tagBuffer.toString());
		}

		// Error
		return Integer.MAX_VALUE;
	}

	/**
	 * Look a the source content of a text unit to see if any part of the text
	 * is bracketed by ILC_START/ILC_END and needs conversion to inline codes.
	 * @param tu the text unit to update.
	 */
	private void processILC (ITextUnit tu) {
		TextFragment tf = tu.getSource().getFirstContent();
		String ct = tf.getCodedText();
		int start = 0;
		int diff = 0; // No code found
		
		// Convert each ILC span into inlinbe code
		while ( true ) {
			start = ct.indexOf(Hexadecimal.ILC_START, start);
			if ( start == -1 ) break; // No more markers
			int end = ct.indexOf(Hexadecimal.ILC_END, start);
			if ( end == -1 ) {
				throw new OkapiIllegalFilterOperationException("Expected ILC_END marker not found.");
			}
			diff = tf.changeToCode(start, end+1, TagType.PLACEHOLDER, "ctrl");
			start = end+diff;
			ct = tf.getCodedText();
		}

		// Remove the markers if needed
		if ( diff != 0 ) { // This means we have at least one code
			for ( Code code : tf.getCodes() ) {
				if ( code.getData().startsWith(Hexadecimal.ILC_START) ) {
					String data = code.getData();
					// Trim both start and end markers
					code.setData(data.substring(1, data.length()-1));
				}
			}
		}
	}

	private enum BlockType {
		TEXT_FLOW,
		TABLE,
		PARAGRAPH_STYLE
	}
}
