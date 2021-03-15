package net.sf.okapi.common.encoder;

import net.sf.okapi.common.IParameters;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

public class RegexEncoder implements IEncoder {
  private boolean removeBSlashEscapes;
  private CharsetEncoder chsEnc;
  private IParameters params;

  private char last = Character.MAX_VALUE;


  public RegexEncoder() {
    removeBSlashEscapes = false;
    chsEnc = StandardCharsets.UTF_8.newEncoder();
  }

  @Override
  public void reset() {
    last = Character.MAX_VALUE;
  }

  @Override
  public void setOptions(IParameters params, String encoding, String lineBreak) {
    chsEnc = Charset.forName(encoding).newEncoder();
    this.params = params;
    if ( params != null ) {
      removeBSlashEscapes = params.getBoolean("removeBSlashEscape");
    }
  }

  @Override
  public String encode(String text, EncoderContext context) {
    if ( text == null ) return "";

    StringBuilder sbTmp = new StringBuilder(text.length());
    for ( int i=0; i<text.length(); i++ ) {
      sbTmp.append(encode(text.charAt(i), context));
    }
    return sbTmp.toString();
  }

  @Override
  public String encode(int value, EncoderContext context) {
    if ( Character.isSupplementaryCodePoint(value) ) {
      String tmp = new String(Character.toChars(value));
      if (!chsEnc.canEncode(tmp) ) {
        return String.format("\\u%04x\\u%04x",
                (int)tmp.charAt(0), (int)tmp.charAt(1));
      }
      return tmp;
    }
    return encode((char)value, context);
  }

  @Override
  public String encode(char value, EncoderContext context) {
    String rv = _encode(value, context);
    this.last = value;
    return rv;
  }

  protected String _encode (char value,
                            EncoderContext context)
  {
    if ( value > 127 ) {
      // Store high surrogate for future use
      if ( Character.isHighSurrogate(value) ) {
        return "";
      }
      // Combine stored surrogate with current char to make a single codepoint
      if ( Character.isHighSurrogate(last) ) {
        int cp = Character.toCodePoint(last, value);
        String tmp = new String(Character.toChars(cp));
        if (!chsEnc.canEncode(tmp) ) {
          return String.format("\\u%04x\\u%04x",
                  (int)tmp.charAt(0), (int)tmp.charAt(1));
        }
        else {
          return tmp;
        }
      }
      if (!chsEnc.canEncode(value) ) {
        return String.format("\\u%04x", (int)value);
      }
      else {
        return String.valueOf(value);
      }
    } else {
      if (removeBSlashEscapes) {
        switch (value) {
          case '\b':
            return "\\b";
          case '\f':
            return "\\f";
          case '\n':
            return "\\n";
          case '\r':
            return "\\r";
          case '\t':
            return "\\t";
          case '"':
          case '\\':
            return "\\" + value;
          default:
            return String.valueOf(value);
        }
      } else {
        return String.valueOf(value);
      }
    }
  }

  @Override
  public IParameters getParameters() {
    return params;
  }
}
