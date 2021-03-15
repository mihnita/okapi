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

// based on MarkdownTokenType
public enum TEXTokenType {

    TEXT,
    THEMATIC_BREAK,
    WHITE_SPACE,
	COMMAND,
	OPEN_CURLY,
	CLOSE_CURLY,
	DOLLAR,
	AMPERSAND,
	NEWLINE,
	HASHTAG,
	CARET,
	UNDERSCORE,
	IGNORED_CHAR,
	INVALID_CHAR,
	TILDE,
	COMMENT,
	OPEN_SQUARE,
	CLOSE_SQUARE,
    
    
    /* Table types */
    TABLE_PIPE,

    /* YAML Header types */
    YAML_METADATA_HEADER,

    /* ~subscript~ and ~~striketrhough~~ */
    STRIKETHROUGH,
    SUBSCRIPT // Note: GitHub renders ~xxxx~ as strike through but we pretends there's a distinction.
}
