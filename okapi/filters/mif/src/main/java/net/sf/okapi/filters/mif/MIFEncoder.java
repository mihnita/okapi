/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.IEncoder;

/**
 * Implements {@link IEncoder} for Adobe FrameMaker MIF format.
 */
public class MIFEncoder implements IEncoder {

    private CharsetEncoder charsetEncoder;
    private IParameters parameters;
    private String encoding;
    private String lineBreak;

    @Override
    public void reset() {
    }

    @Override
    public void setOptions(IParameters params, String encoding, String lineBreak) {
        this.parameters = params;
        this.encoding = encoding;
        this.lineBreak = lineBreak;
        this.charsetEncoder = Charset.forName(encoding).newEncoder();
    }

    @Override
    public String encode(String text, EncoderContext context) {
        final StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            escaped.append(encode(text.charAt(i), context));
        }
        return escaped.toString();
    }

    @Override
    public String encode(char value, EncoderContext context) {
        switch (value) {
            case '\t':
                return "\\t";
            case '\n':
                return "\\n";
            case '>':
                return "\\>";
            case '\'':
                return "\\q";
            case '`':
                return "\\Q";
            case '\\':
                return "\\\\";
            default:
                return String.valueOf(value);
        }
    }

    @Override
    public String encode(int value, EncoderContext context) {
        if (Character.isSupplementaryCodePoint(value)) {
            return encode(new String(Character.toChars(value)), context);
        }
        return encode((char) value, context);
    }

    @Override
    public String toNative(String propertyName, String value) {
        return value;
    }

    @Override
    public String getLineBreak() {
        return this.lineBreak;
    }

    @Override
    public CharsetEncoder getCharsetEncoder() {
        return this.charsetEncoder;
    }

    @Override
    public IParameters getParameters() {
        return this.parameters;
    }

    @Override
    public String getEncoding() {
        return encoding;
    }
}
