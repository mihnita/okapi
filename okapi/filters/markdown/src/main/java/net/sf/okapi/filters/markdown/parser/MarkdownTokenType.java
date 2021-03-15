/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.markdown.parser;

import java.util.EnumSet;

public enum MarkdownTokenType {
    /* Core types */
    AUTO_LINK,
    BLANK_LINE,
    BLOCK_QUOTE,
    BULLET_LIST,
    BULLET_LIST_ITEM,
    CODE,
    EMPHASIS,
    FENCED_CODE_BLOCK,
    FENCED_CODE_BLOCK_INFO,
    HARD_LINE_BREAK,
    HEADING_PREFIX,
    HEADING_UNDERLINE,
    HTML_BLOCK,
    HTML_COMMENT_BLOCK,
    HTML_ENTITY,
    HTML_INLINE,
    HTML_INLINE_COMMENT,
    HTML_INNER_BLOCK,
    HTML_INNER_BLOCK_COMMENT,
    IMAGE,
    IMAGE_REF,
    INDENTED_CODE_BLOCK,
    LINK,
    LINK_REF,
    MAIL_LINK,
    ORDERED_LIST,
    ORDERED_LIST_ITEM,
    REFERENCE,
    SOFT_LINE_BREAK,
    STRONG_EMPHASIS,
    TEXT,
    THEMATIC_BREAK,
    WHITE_SPACE,

    /* Table types */
    TABLE_PIPE, // The vertical bar "|" that separates columns of the header or ordinary row.
    TABLE_SEPARATOR, // | --- | --- | that separates the header and the body of the table.

    /* YAML Header types */
    YAML_METADATA_HEADER,

    /* ~subscript~ and ~~striketrhough~~ */
    STRIKETHROUGH,
    SUBSCRIPT, // Note: GitHub renders ~xxxx~ as strike through but we pretends there's a distinction.

    /* Pseudo types */
    END_TEXT_UNIT,
    LINE_PREFIX, // Pseudo token to tell the new prefix. The current TU should be closed.
    ;
    
    /**
     * Returns true if this token type represents a Markdown expression that does not
     * break a run of text (i.e. should not start a new Text Unit) such as 
     * "*" of "*emphasized text*", "__" of "__strongly emphasized text__" etc.
     * @return true if the token type is for the token that should not start a new TextUnit
     */
    public boolean isInline() {
        return inlineTokenTypes.contains(this);
    }
    
    private static EnumSet<MarkdownTokenType> inlineTokenTypes
    	= EnumSet.of(EMPHASIS, STRONG_EMPHASIS, STRIKETHROUGH, SUBSCRIPT,
    		HARD_LINE_BREAK, // It is an inline code because it is allowed to appear between a pair of inline codes.
    		CODE, IMAGE, IMAGE_REF, LINK, LINK_REF);
}
