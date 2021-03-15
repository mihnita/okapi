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

package net.sf.okapi.filters.tex.parser;

import java.util.Deque;
import java.util.LinkedList;

public class TEXParser {

	public enum CharType {
		ESCAPE,
		START_GROUP,
		END_GROUP,
		MATH_SHIFT,
		ALIGN_TAB,
		EOL,
		PARAM,
		SUPERSCRIPT,
		SUBSCRIPT,
		WHITESPACE,
		ALPHANUM,
		DEFAULT,
		ACTIVE,
		COMMENT,
		IGNORED,
		INVALID
	};
	
	private String newline = System.lineSeparator();
    private Deque<TEXToken> tokenQueue = new LinkedList<>();

    /**
     * Create a new {@link TEXParser} that uses the platform-specific newline.
     */
	public TEXParser () {
	}

    /**
     * Create a new {@link TEXParser} that uses the specified string as a newline.
     * @param newline The newline type that this parser will use
     */
    public TEXParser (String newline) {
        this.newline = newline;
    }
    
    /**
     * Returns the type to which the given character corresponds
     * See http://www.ctex.org/documents/shredder/src/texbook.pdf for reference.
     * @param character to lookup.
     * @return code for the given character
     */
    private CharType getCharType (String cc) {
		if ( cc.equals("\\") ) {
			// Escape character (control sequence follows)
			return CharType.ESCAPE;
		}
		else if ( cc.equals("{") ) {
			// Start of group
			return CharType.START_GROUP;
		}
		else if ( cc.equals("}") ) {
			// End of group
			return CharType.END_GROUP;
		}
		else if ( cc.equals("$") ) {
			// Math shift
			return CharType.MATH_SHIFT;
		}
		else if ( cc.equals("&") ) {
			// Alignment tab (table column separator)
			return CharType.ALIGN_TAB;
		}
		else if ( cc.matches("[\n]") ) {
			// end of line [\r\n] ; \r is ignored and dropped
			return CharType.EOL;
		}
		else if ( cc.equals("#") ) {
			// Parameter
			return CharType.PARAM;
		}
		else if ( cc.equals("^") ) {
			// Superscript
			return CharType.SUPERSCRIPT;
		}
		else if ( cc.equals("_") ) {
			// Subscript
			return CharType.SUBSCRIPT;
		}
		else if ( cc.matches("[\r]") ) {
			// Ignored character \r is ignored and dropped
			return CharType.IGNORED;
		}
		else if ( cc.matches("[ \t]") ) {
			// Simple whitespace
			return CharType.WHITESPACE;
		}
		else if ( cc.matches("[a-zA-Z0-9]") ) {
			return CharType.ALPHANUM;
		}
		else if ( cc.equals("~") ) {
			// Active character
			return CharType.ACTIVE;
		}
		else if ( cc.equals("%") ) {
			// Comment
			return CharType.COMMENT;
		}
		else if ( cc.equals("\000") ) {
			// Invalid character
			return CharType.INVALID;
		}
		// Default
		return CharType.DEFAULT;
    }
    
    /**
     * Parse the given TEX content into tokens that can be then retrieved with
     * calls to {@link TEXParser#getNextToken()}. Any existing tokens from
     * previous calls to {@link TEXParser#parse(String)} will be discarded.
     *
     * @param content The TEX content to parse into tokens
     */
    public void parse (String content) {
        tokenQueue.clear();
        StringBuilder sb = new StringBuilder();
        StringBuilder comment = new StringBuilder();
        
        // Split the content into strings of one character
        // (Note Java 8 now does not produce empty lead string)
        String[] chars = content.split("");
        
        int index = 0;
		while ( index < chars.length ) {

			String cc = chars[index];
			CharType type = getCharType(cc);

			if ( type == CharType.ESCAPE ) { // "/"
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				// parse control sequence
				// See https://en.wikibooks.org/wiki/LaTeX/Basics
				StringBuilder cmd = new StringBuilder();
				cmd.append(cc);
				index++;
				while ( index < chars.length ) {
					// cmd tagad satur "/" komandā var būt tikai a-Z0-9 un arī []
					String cmdc = chars[index];

					if ( getCharType(cmdc) == CharType.ALPHANUM ) {
						cmd.append(cmdc); // Alphanumeric char "[a-zA-Z0-9]"
					}
					else if ( getCharType(cmdc) == CharType.DEFAULT ) {
						// [] and all others
						cmd.append(cmdc);
						// If cmd contains [ then everything until ] is also command
						if ( cmdc.equals("[") ) {
							// Last char was [, append everything until ] found
							index++;
							while ( index < chars.length ) {
								cmdc = chars[index];
								if ( getCharType(cmdc) == CharType.IGNORED ) {
									index++;
									continue;
								}
								if ( cmdc.equals("]") ) {
									cmd.append(cmdc);
									break;
								}
								cmd.append(cmdc);
								index++;
							}
						}
					}
					else if ( getCharType(cmdc) == CharType.IGNORED ) {
						// Ignored character
						index++;
						continue;
					}
					else if ( cmd.length() == 1 ) {
						// "/" un kāds specsimbols
						cmd.append(cmdc);
						break;
					}
					else if ( getCharType(cmdc) == CharType.WHITESPACE ) {
						// Command is done, space gets added at the end of command
						cmd.append(cmdc);
						break;
					}
					else { // command is done
						// System.out.println(index);
						index--;
						break;
					}
					index++;
				}
				//TODO: Check this
				tokenQueue.addLast(new TEXToken(cmd.toString(), false, TEXTokenType.COMMAND));
			}
			else if ( type == CharType.START_GROUP ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(	new TEXToken("{", false, TEXTokenType.OPEN_CURLY));
			}
			else if ( type == CharType.END_GROUP ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("}", false, TEXTokenType.CLOSE_CURLY));
			}
			else if ( type == CharType.MATH_SHIFT ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("$", false, TEXTokenType.DOLLAR));
			}
			else if ( type == CharType.ALIGN_TAB ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("&", false, TEXTokenType.AMPERSAND));
			}
			else if ( type == CharType.EOL ) { // \r\n or [\r\n]
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("\n", false, TEXTokenType.NEWLINE));
			}
			else if ( type == CharType.PARAM ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("#", false, TEXTokenType.HASHTAG));
			}
			else if ( type == CharType.SUPERSCRIPT ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("^", false, TEXTokenType.CARET));
			}
			else if ( type == CharType.SUBSCRIPT ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("_", false, TEXTokenType.UNDERSCORE));
			}
			else if ( type == CharType.IGNORED ) {
				// Ignored character
//				if ( sb.length() > 0 ) {
//					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
//					sb.setLength(0);
//				}
//				tokenQueue.addLast(new TEXToken("\000", false, TEXTokenType.IGNORED_CHAR));
			}
			else if ( type == CharType.WHITESPACE ) {
				sb.append(cc);
			}
			else if ( type == CharType.ALPHANUM ) {
				sb.append(cc);
			}
			else if ( type == CharType.ACTIVE ) { // "~"
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("~", false, TEXTokenType.TILDE));
			}
			else if ( type == CharType.COMMENT ) { // "%"
				if ( sb.length() > 0 ) {
					tokenQueue.addLast( new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				// Construct the comment
				comment.setLength(0);
				comment.append(cc);
				index++;

				while (index < chars.length) {
					if ( getCharType(chars[index]) == CharType.EOL ) {
						break;
					}
					if ( getCharType(chars[index]) == CharType.IGNORED ) {
						index++;
						continue;
					}
					String commentc = chars[index];
					comment.append(commentc);
					index++;
				}
				tokenQueue.addLast(new TEXToken(comment.toString(), false, TEXTokenType.COMMENT));
				if ( index < chars.length ) {
					tokenQueue.addLast(new TEXToken(chars[index], false, TEXTokenType.NEWLINE));
				}
			}
			else if ( type == CharType.INVALID ) {
				if ( sb.length() > 0 ) {
					tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
					sb.setLength(0);
				}
				tokenQueue.addLast(new TEXToken("\000", false, TEXTokenType.INVALID_CHAR));
			}
			else { // Non-control char(,:- etc.) - process as ordinary char == no spaces here
				sb.append(cc);
			}
			index++;
		}
		
		// Done looping, save token, if last token was text
		if ( sb.length() > 0 ) {
			tokenQueue.addLast(new TEXToken(sb.toString(), true, TEXTokenType.TEXT));
		}
	}

	public boolean hasNextToken () {
		return !tokenQueue.isEmpty();
	}

    /**
     * Returns the next available token and removes it from parser
     *
     * @return The next token
     * @throws IllegalStateException If no more tokens are remaining
     */
    public TEXToken getNextToken() {
        if (!hasNextToken()) {
            throw new IllegalStateException("No more tokens remaining");
        }
        return tokenQueue.removeFirst();
    }
    
    /**
     * Returns the next available token and keeps it in parser 
     * @return The next token null If no more tokens are remaining
     */
    public TEXToken peekNextToken() {
        return tokenQueue.peekFirst();
    }
    
    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

// Not used
//    /**
//     * Dumps all tokens. This is for development.
//     * @return String representation of all TEXTokens generated.
//     */
//    private String dumpTokens() {
//        StringBuilder builder = new StringBuilder();
//        for (TEXToken tok: tokenQueue) {
//            builder.append(tok).append(newline);
//        }
//        return builder.toString();
//    }

}
