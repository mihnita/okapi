/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.tex;

import java.util.LinkedList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.BOMNewlineEncodingDetector;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.AbstractFilter;
import net.sf.okapi.common.filters.EventBuilder;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.filters.tex.parser.TEXParser;
import net.sf.okapi.filters.tex.parser.TEXToken;
import net.sf.okapi.filters.tex.parser.TEXTokenType;

@UsingParameters() // No parameters are used
// Should parameters be used, define like this @UsingParameters(Parameters.class)
public class TEXFilter extends AbstractFilter {

	public static final String TEX_MIME_TYPE = "text/x-tex-text";
	
	private EncoderManager encoderManager;
	private RawDocument input;
	private EventBuilder eventBuilder;
	private BOMNewlineEncodingDetector detector;
	private TEXParser parser;
    private LinkedList<String> oneArgInlineText = new LinkedList<>();
    private LinkedList<String> oneArgParaText = new LinkedList<>();
    private LinkedList<String> oneArgNoText = new LinkedList<>();
    private LinkedList<String> accentedChars = new LinkedList<>();
    private LinkedList<String> accentedCharsNonLetters = new LinkedList<>();
    private boolean isHeaderMode = true; //everything until 
    private boolean isLastCommandNewline = true;
	
	public TEXFilter () {
		super();
        this.parser = new TEXParser(); // System default newline; No params
        setMimeType(TEX_MIME_TYPE);
        setMultilingual(false);
        setName("okf_tex");
        setDisplayName("TEX Filter (BETA)");
        getEncoderManager();//setParameters();
        encoderManager.updateEncoder(TEX_MIME_TYPE);
        addConfiguration(new FilterConfiguration(getName(), TEX_MIME_TYPE,
                getClass().getName(), "Tex",
                "Tex files", null, ".tex"));
        setFilterWriter(createFilterWriter());

        // Text after those is part of surrounding paragraph, just differently styled
        // here also possible format {\command text}
        oneArgInlineText.add("\\bf");
        oneArgInlineText.add("\\em");
        oneArgInlineText.add("\\emph");
        oneArgInlineText.add("\\footnote");
        oneArgInlineText.add("\\hbox");
        oneArgInlineText.add("\\mbox");
        oneArgInlineText.add("\\textbackit");
        oneArgInlineText.add("\\textbf");
        oneArgInlineText.add("\\texttt");
        oneArgInlineText.add("\\textsf");
        oneArgInlineText.add("\\textit");
        oneArgInlineText.add("\\tt");
        oneArgInlineText.add("\\vbox");

        // Text after this command in {} can be translated as separate paragraph
        oneArgParaText.add("\\author"); // @TODO processing of \and and other unformatted text
        oneArgParaText.add("\\Chapter");
        oneArgParaText.add("\\chapter");
        oneArgParaText.add("\\index"); // \index{Miks, Toms}
        oneArgParaText.add("\\typeout");
        oneArgParaText.add("\\title");
        oneArgParaText.add("\\titlerunning");
        oneArgParaText.add("\\section");
        oneArgParaText.add("\\subsection");
        oneArgParaText.add("\\caption");
        
        // Text after this command in {} should NOT be translated
        oneArgNoText.add("\\begin");
        oneArgNoText.add("\\cite");
        oneArgNoText.add("\\citealt");
        oneArgNoText.add("\\documentclass");
        oneArgNoText.add("\\end");
        oneArgNoText.add("\\hspace");
        oneArgNoText.add("\\hskip");
        oneArgNoText.add("\\includegraphics");
        oneArgNoText.add("\\newcite");
        oneArgNoText.add("\\label");
        oneArgNoText.add("\\put");
        oneArgNoText.add("\\pageref");
        oneArgNoText.add("\\pagestyle");
        oneArgNoText.add("\\ref");
        oneArgNoText.add("\\thispagestyle");
        oneArgNoText.add("\\usepackage");
        oneArgNoText.add("\\vspace");
        oneArgNoText.add("\\vskip");
        
        // Accented Chars and some national symbols
		accentedChars.add("\\oe");
		accentedChars.add("\\OE");
		accentedChars.add("\\ae");
		accentedChars.add("\\AE");
		accentedChars.add("\\aa");
		accentedChars.add("\\AA");
		accentedChars.add("\\o");
		accentedChars.add("\\O");
		accentedChars.add("\\l");
		accentedChars.add("\\L");
		accentedChars.add("\\ss");
		accentedChars.add("\\j");
		
		// Text-mode accents
		accentedCharsNonLetters.add("\\`");
		accentedCharsNonLetters.add("\\\'");
		accentedCharsNonLetters.add("\\^");
		accentedCharsNonLetters.add("\\\"");
		accentedCharsNonLetters.add("\\~");
		accentedCharsNonLetters.add("\\=");
		accentedCharsNonLetters.add("\\.");
		accentedCharsNonLetters.add("\\%");
		accentedChars.add("\\u");
		accentedChars.add("\\v");
		accentedChars.add("\\H");
		accentedChars.add("\\c");
		accentedChars.add("\\d");
		accentedChars.add("\\k");
		accentedChars.add("\\i");
//		accentedChars.add("\\%");
//		accentedChars.add("\\{");
//		accentedChars.add("\\}");
	}
	
    @Override
	protected boolean isUtf8Bom () {
		return detector != null && detector.hasUtf8Bom();
	}
    
    @Override
	protected boolean isUtf8Encoding () {
		return detector != null && detector.hasUtf8Encoding();
	}

    @Override
	public void close () {
		if ( input != null ) {
			input.close();
			detector = null;
			eventBuilder = null;
		}
	}

    @Override
	public IParameters getParameters () {
		return null; // Not used
	}

    @Override
	public boolean hasNext () {
		return eventBuilder.hasQueuedEvents();
	}

    @Override
	public Event next () {
		if ( hasNext() ) {
			return eventBuilder.next();
		}
		throw new IllegalStateException("No events available");
	}
    
	private void generateTokens () {
    	parser.setNewline(getNewlineType());
		try (Scanner scanner = new Scanner(input.getReader())) {
			scanner.useDelimiter("\\A"); // Using this delimiter, text is returned in one block
			if ( scanner.hasNext() ) {
				parser.parse(scanner.next());
			}
		}
    }
    
    /**
     * Check if text in {} after given command is translatable or not
     * @param commandText
     * @return the type of command: OneArgNoText, OneArgInlineText, OneArgParText
     * or AccentedChar or UnknownCommand
     */
    private String getCommandType (String commandText) {
    	// Command types: 
    	// If command has params ("[]") after it, those are included in command text
//    	String commandText = commandTextRaw;
//    	String commandText = commandTextRaw.split("\\[")[0];
		if ( oneArgNoText.contains(commandText) ) {
			return "OneArgNoText";
		}
		if ( oneArgInlineText.contains(commandText) ) {
			return "OneArgInlineText";
		}
		if ( oneArgParaText.contains(commandText) ) {
			return "OneArgParText";
		}
		if ( accentedChars.contains(commandText.trim()) ) {
			return "AccentedChar";
		}
    	// If command starts with symbol `'^\"~=." assume it to be part of accented letter like \\=a
    	for ( String accentedChar : accentedCharsNonLetters ) {
        	if ( commandText.startsWith(accentedChar.trim()) ) {
        		return "AccentedChar";
        	}
    	}
    	return "UnknownCommand";
    }
    
    /**
     * Generate events and keep them in eventBuilder object
     */
	private void generateEvents () {
    	TEXToken token;
    	TEXToken next_token;
    	
        while ( parser.hasNextToken() ) {

        	token = parser.getNextToken();
        	next_token = parser.peekNextToken();
        	
			if ( token.getType() == TEXTokenType.COMMENT ) {
				// entire line is document part
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
				// add following newline to this document part as well
				if ( parser.hasNextToken() && next_token.getType() == TEXTokenType.NEWLINE ) {
					addDocumentPartToEventBuilder(parser.getNextToken().getContent());
				}
			}
			else if ( token.getType() == TEXTokenType.NEWLINE ) {
				// second or more newline paragraph ends;
				if ( isLastCommandNewline && eventBuilder.isCurrentTextUnit() ) {
					eventBuilder.endTextUnit();
				}
				isLastCommandNewline = true;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.COMMAND ) {
				isLastCommandNewline = false;
				processCommand(token);
			}
			else if ( token.getType() == TEXTokenType.DOLLAR ) {
				isLastCommandNewline = false;
				String text = processMath(token.getContent());
				addDocumentPartToEventBuilder(text);
			}
			else if ( token.getType() == TEXTokenType.AMPERSAND ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.HASHTAG ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.CARET ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.UNDERSCORE ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.IGNORED_CHAR ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.TILDE ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.OPEN_CURLY ) {
				isLastCommandNewline = false;
				processOpenCurly(token);
			}
			else if ( token.getType() == TEXTokenType.CLOSE_CURLY ) {
				System.out.println("Found closing curly in main loop without opening and next is_"
					+ next_token.getContent() + "_");
			}
			else {
				// token is not COMMENT,NEWLINE,COMMAND or symbol -> its TEXT
				isLastCommandNewline = false;
				String text = token.getContent();
				// no translatable text in header
				if ( isHeaderMode ) {
					addDocumentPartToEventBuilder(text);
				}
				else {
					addTextToTextUnit(text);
				}
			}
        }
    }
    
	private TEXToken processTextBlock () {
    	// we are inside inline text or paragraph text, and supposedly inside TextUnit event

    	TEXToken token = null;
    	TEXToken next_token = null;
    	
		while ( parser.hasNextToken() ) {

			token = parser.getNextToken();
			next_token = parser.peekNextToken();
        	
			if ( token.getType() == TEXTokenType.COMMENT ) {
				// entire line is document part
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
				// add following newline to this document part as well
				if ( parser.hasNextToken() && next_token.getType() == TEXTokenType.NEWLINE ) {
					addDocumentPartToEventBuilder(
						parser.getNextToken().getContent());
				}
			}
    		else if ( token.getType() == TEXTokenType.NEWLINE ) {
    			//second or more newline paragraph ends; 
				if ( isLastCommandNewline && eventBuilder.isCurrentTextUnit() ) {
					eventBuilder.endTextUnit();
				}
        		isLastCommandNewline = true;
        		addDocumentPartToEventBuilder(token.getContent());
    		}
			else if ( token.getType() == TEXTokenType.COMMAND ) {
				isLastCommandNewline = false;
				processCommand(token);
			}
			else if ( token.getType() == TEXTokenType.DOLLAR ) {
				isLastCommandNewline = false;
				String text = processMath(token.getContent());
				addDocumentPartToEventBuilder(text);
			}
			else if ( token.getType() == TEXTokenType.AMPERSAND ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.HASHTAG ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.CARET ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.UNDERSCORE ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.IGNORED_CHAR ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.TILDE ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else if ( token.getType() == TEXTokenType.OPEN_CURLY ) {
				isLastCommandNewline = false;
				processOpenCurly(token);
			}
			else if ( token.getType() == TEXTokenType.CLOSE_CURLY ) {
				return token;
			}
			else if ( token.getType() == TEXTokenType.INVALID_CHAR ) {
				isLastCommandNewline = false;
				addDocumentPartToEventBuilder(token.getContent());
			}
			else {
				// token is not COMMENT,NEWLINE,COMMAND or symbol -> its TEXT
				isLastCommandNewline = false;
				addTextToTextUnit(token.getContent());
			}
        }
        return token; //last token
    }
    
	private void processOpenCurly (TEXToken token) {
		// if next is command, this may be differently styled inline text block
		TEXToken next_token = parser.peekNextToken();
		if ( !isHeaderMode && next_token.getType() == TEXTokenType.COMMAND) {
			processOneArgInlineText(token);
			return;
		}
		// else it is non-translatable text between brackets; may contain nesting
		String text = token.getContent();
		int nestingLevel = 1; // text already contains {
    	while ( parser.hasNextToken() ) {
    		token = parser.getNextToken();
    		text = text + token.getContent();
			if ( token.getType() == TEXTokenType.OPEN_CURLY ) {
				nestingLevel++;
    		}
			if ( token.getType() == TEXTokenType.CLOSE_CURLY ) {
    			nestingLevel--;
    			if ( nestingLevel < 1 ) {
        			break;
    			}
    		}
    	}
		addDocumentPartToEventBuilder(text);
		return;
	}
	
    
    /**
     * Return processed UTF-8 character or string
     */
    private String processAccentedChar (TEXToken token) {
    	String text = token.getContent(); // example: "\="
    	int nestingLevel = 0;
    	// Replace those with unicode letters
		while (parser.hasNextToken()) {
			//token = parser.getNextToken();
			token = parser.peekNextToken(); // dont remove token yet
			if (token.getType() == TEXTokenType.OPEN_CURLY) {
				nestingLevel++;
				text = text + token.getContent();
			}
			else if (token.getType() == TEXTokenType.CLOSE_CURLY) {
				nestingLevel--;
				if (nestingLevel < 0) {
					return text; // found closing curly wihtout matching opening - example //textit{30. j\\=ulijs}
				}
				text = text + token.getContent();
				break;
			}
			else if (token.getType() == TEXTokenType.TEXT) {
				text = text + token.getContent();
				if (nestingLevel < 1) {
					break;
				}
			}
			else { 
				if (token.getType() == TEXTokenType.COMMAND && getCommandType(token.getContent()).equals("AccentedChar")) {
					// only /i,/{,/} or accented char expected here, any other command is error
					text = text + token.getContent();
					parser.getNextToken();
					continue;
				}
				// next is unexpected symbol return what we have
                return text;
			}
			parser.getNextToken(); //remove processed token
		}
		parser.getNextToken(); //remove processed token
        return text;
    }
    
    /**
     * Add text unit to eventBuilder
     */
    private void processOneArgParText (TEXToken token) {
		if ( eventBuilder.isCurrentTextUnit() ) {
			eventBuilder.endTextUnit();
		} // End previous TextUnit
        
        GenericSkeleton startMarker = new GenericSkeleton(token.getContent());
		if ( parser.peekNextToken().getType() == TEXTokenType.OPEN_CURLY ) {
			startMarker.add(parser.getNextToken().getContent());
		}
		
        // Another, presumably inline command after curly
		if ( parser.peekNextToken().getType() == TEXTokenType.COMMAND ) {
			startMarker.add(parser.getNextToken().getContent());
		}

        // Avoid colliding command and text after translation by inserting space into command
		if ( parser.peekNextToken().getContent().startsWith(" ") ) {
			startMarker.add(" ");
		}

		eventBuilder.startTextUnit(startMarker);
        token = processTextBlock();// returns } or, if EOF, last token
        
        GenericSkeleton endMarker = new GenericSkeleton(token.getContent());
		if ( eventBuilder.isCurrentTextUnit() ) {
			eventBuilder.endTextUnit(endMarker);
		}
		else {
			System.out.println("No TextUnit to close with end marker, inserting as document part " + token.getContent());
			addDocumentPartToEventBuilder(token.getContent());
		}
    }
    
    /**
     * Add command and text as inline text
     * @param token token to process
     */
    private void processOneArgInlineText (TEXToken token) {
		// start new paragraph if necessary
		// add command and {} as document parts, and part between as text 
    	
        if (!eventBuilder.isCurrentTextUnit()) {
            eventBuilder.startTextUnit();
        }
        String openingTag = token.getContent();
        // format \command{text} OR format {\command text}
        if (parser.peekNextToken().getType() == TEXTokenType.OPEN_CURLY | token.getType() == TEXTokenType.OPEN_CURLY) {
        	openingTag = openingTag + parser.getNextToken().getContent();
        }
        // add space to opening tag, so it does not get lost in translation
        if (parser.peekNextToken().getContent().startsWith(" ")) {
        	openingTag = openingTag + " ";
        }
        addOpening(openingTag);
        
        token = processTextBlock(); //returns last token - } expected
        if (eventBuilder.isCurrentTextUnit()) {
        	addClosing(token.getContent());
        } else {
        	System.out.println("No TextUnit to close with closing code, inserting as document part"+token.getContent());
        	addDocumentPartToEventBuilder(token.getContent());
        }
    }
    
    private void processCommand (TEXToken token) {
    	String commandType = getCommandType(token.getContent());
			switch (commandType) {
				case "AccentedChar": {
					String text = processAccentedChar(token);
					addTextToTextUnit(text);
					break;
				}
				case "OneArgNoText":
				case "UnknownCommand": {
					// after this kind of command other_comand or {.*} are expected
					// this is also \begin{table} command, which may turn tabular mode on
					// exit from this mode is only \end{table} command for now
					// if not in contentMode (inside document header), add all text after
					String text = token.getContent();
					int nestingLevel = 0;

					if (token.getContent().contains("|")) {
						// Text between pipes are not translatable - add those to document part
						while (parser.hasNextToken()) {
							token = parser.getNextToken();
							text = text + token.getContent();
							if (token.getContent().contains("|")) {
								break;
							}
						}
						addDocumentPartToEventBuilder(text);
						return;
					} else if (parser.hasNextToken() && (parser.peekNextToken().getType()
							!= TEXTokenType.OPEN_CURLY)) {
						// Assume command without arguments
						// check if there is space after command, and add to ensure it
						// stays there after translation
						if (parser.peekNextToken().getContent().startsWith(" ")) {
							addDocumentPartToEventBuilder(token.getContent() + " ");
						} else {
							addDocumentPartToEventBuilder(token.getContent());
						}
						return;
					}

					while (parser.hasNextToken()) {
						token = parser.getNextToken();
						if (token.getType() == TEXTokenType.COMMAND) {
							text = text + token.getContent();
						} else if (token.getType() == TEXTokenType.OPEN_CURLY) {
							nestingLevel++;
							text = text + token.getContent();
						} else if (token.getType() == TEXTokenType.CLOSE_CURLY) {
							// System.out.println("Processing close curly and text is_" + text +"_");
							nestingLevel--;
							text = text + token.getContent();
							if (text.matches(".*\\\\begin\\{.*document.*\\}")) {
								isHeaderMode = false;
							}
							if (text.matches(".*\\\\end\\{.*document.*\\}")) {
								isHeaderMode = true;
							}
							if (nestingLevel < 1) {
								if (text.contains("\\begin{")) {
									if (eventBuilder.isCurrentTextUnit()) {
										eventBuilder.endTextUnit();
									}
								}
								// table longtable table*
								if (text.matches(".*\\\\begin\\{.*table.*\\}")) {
									text = processTable(text);
								}
								if (text.matches(".*\\\\begin\\{.*equation.*\\}")) {
									text = processEquation(text);
								}
								if (text.matches(".*\\\\begin\\{.*figure.*\\}")) {
									text = processFigure(text);
								}
//						if (text.contains("\\begin{centered}")) {
//							text = processEquation(text);
//						}
								// ordinary command ends
								break;
							}
						} else if (token.getType() == TEXTokenType.NEWLINE) {
							text = text + token.getContent();
						} else if (nestingLevel < 1) {
							// Instead of expected other_comand or { something else happened
							System.out.println("TEXFilter @processCommand found broken " + text);
							break;
						} else {
							// Add to non-translatable command text everything between curly brackets
							text = text + token.getContent();
						}
					}

					// replace latin1 encoding with utf8
					text = text.replaceAll(Pattern.quote("\\usepackage[latin1]{inputenc}"),
							Matcher.quoteReplacement("\\usepackage[utf8]{inputenc}"));

					addDocumentPartToEventBuilder(text);
					break;
				}
				case "OneArgInlineText":
					processOneArgInlineText(token);
					break;
				case "OneArgParText":
					// this kind of command (like \title) means separate paragraph
					processOneArgParText(token);
					break;
			}
		return;
    }
    
//    /**
//     * Alternative to process header, table, figure etc, where only content of oneArgParText commands may be translated
//     * @param text text to process
//     */
//    private void processRestricted (String text) {
//    	// TODO
//    }
    
    private String processTable(String text) {
    	// grab all until /end{table} is found
    	TEXToken token;
    	while (parser.hasNextToken()) {
    		token = parser.getNextToken();
    		text = text + token.getContent();
			if ( Pattern.compile(".*\\\\end\\{.*table.*\\}").matcher(text).find() ) {
    			return text;
    		}
    	}
    	// End of document reached, no table ending, return all text
    	return text;
    }
    
    private String processEquation (String text) {
    	// grab all until /end{equation} is found
    	TEXToken token;
    	while (parser.hasNextToken()) {
    		token = parser.getNextToken();
    		text = text + token.getContent();
			if ( Pattern.compile(".*\\\\end\\{.*equation.*\\}").matcher(text).find() ) {
    			return text;
    		}
    	}
    	// end of document reached, no equation ending, return all text
    	return text;
    }
    
    private String processFigure(String text) {
    	// grab all until /end{figure} is found
    	TEXToken token;
    	while (parser.hasNextToken()) {
    		token = parser.getNextToken();
    		text = text + token.getContent();
			if ( Pattern.compile(".*\\\\end\\{.*figure.*\\}").matcher(text).find() ) {
    			return text;
    		}
    	}
    	// end of document reached, no equation ending, return all text
    	return text;
    }
    
    private String processMath(String text) {
		// enter math or centered math mode. Nothing to translate
		boolean lastTokenDollar = true;
		boolean centeredMathMode = false;
		TEXToken token;

		while (parser.hasNextToken()) {
			token = parser.getNextToken();
			if (token.getType() == TEXTokenType.DOLLAR) {
				if (lastTokenDollar && centeredMathMode) {
					//in centeredMathMode and found second dollar, exit centeredMathMode
					text = text + token.getContent();
					break;
				} else if (!lastTokenDollar && centeredMathMode) {
					// in centeredMathMode and found first ending dollar
					lastTokenDollar = true;
					text = text + token.getContent();
				} else if (lastTokenDollar && !centeredMathMode) {
					//enter centeredMathMode
					centeredMathMode = true;
					text = text + token.getContent();
				} else {
					// math mode and previous was not dollar - exit
					text = text + token.getContent();
					break;
				}
			} else {
				lastTokenDollar = false;
				text = text + token.getContent();
			}
		}
		// either end of document or math mode ended
		return text;
    }
    
	/**
	 * Add as code.opening
	 */
	private void addOpening(String text){
        if (!eventBuilder.isCurrentTextUnit()) {
            eventBuilder.startTextUnit();
        }
        eventBuilder.addToTextUnit(
            new Code(TextFragment.TagType.OPENING, TEXTokenType.COMMAND.name(), text));
	}
	
	/**
	 * Add as code.closing
	 */
	private void addClosing(String text){
        eventBuilder.addToTextUnit(
            new Code(TextFragment.TagType.CLOSING, TEXTokenType.COMMAND.name(), text));
	}
	
	/**
	 * Add text to open textUnit as code, or as standalone DocumentPart
	 */
	private void addDocumentPartToEventBuilder(String text){
		if ( eventBuilder.isCurrentTextUnit() ) {
			// Add to the already-existing text unit
			eventBuilder.addToTextUnit(new Code(TextFragment.TagType.PLACEHOLDER,
				TEXTokenType.COMMAND.name(), text));
		}
		else {
			// No need to create a text unit starting with a code, so create document part instead
			eventBuilder.addToDocumentPart(text);
//			 eventBuilder.startDocumentPart(text);
//			 eventBuilder.endDocumentPart();
		}
	}
	
	/**
	 * Convert codes to accented characters and 
	 * add text to text unit, 
	 * @param text
	 */
	private void addTextToTextUnit(String text) {
        if (!eventBuilder.isCurrentTextUnit()) {
            eventBuilder.startTextUnit();
        }
    	text = encoderManager.encode(text,EncoderContext.TEXT);
    	eventBuilder.addToTextUnit(text);
	}

	public void open (RawDocument input) {
		open(input, true);
	}
	
	@Override
    public void open (RawDocument input, boolean generateSkeleton){
		this.input = input;
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(input.getStream(), "UTF-8");
		detector.detectAndRemoveBom();
		setNewlineType(detector.getNewlineType().toString());
		
		input.setEncoding(detector.getEncoding());
		String encoding = input.getEncoding();
		setEncoding(encoding);
        setOptions(input.getSourceLocale(), input.getTargetLocale(), encoding, generateSkeleton);

		generateTokens();
		// Tokens may be accessed using parser.getNextToken() and used in generateEvents() method

		// Create EventBuilder with document name as rootId
		if ( eventBuilder == null ) {
			eventBuilder = new EventBuilder(getParentId(), this);
		}
		else {
			eventBuilder.reset(getParentId(), this);
		}
		eventBuilder.setPreserveWhitespace(true);

        eventBuilder.addFilterEvent(createStartFilterEvent());
        generateEvents();

        // close any open events
		if ( eventBuilder.isCurrentTextUnit() ) {
			eventBuilder.endTextUnit();
		}
		if ( eventBuilder.hasUnfinishedSkeleton() ) {
			eventBuilder.endDocumentPart();
		}
		// add end filter event
		eventBuilder.addFilterEvent(createEndFilterEvent());
	}

	@Override
	public void setParameters (IParameters params) {
		// No parameters for this filter
	}
	
	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
		}
		// if created EncoderManager without params, need to initialize Encoder and Writer
		encoderManager.setMapping(TEX_MIME_TYPE, new TEXEncoder());
		return encoderManager;
	}
	
	@Override
	public IFilterWriter createFilterWriter() {
		return new GenericFilterWriter(new TEXSkeletonWriter(), getEncoderManager());
	}
}
