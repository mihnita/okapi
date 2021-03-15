/*===========================================================================
  Copyright (C) 2008-2010 by the Okapi Framework contributors
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

package net.sf.okapi.common.encoder;

import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;

/**
 * Provides common methods to encode/escape text to a specific format.
 * <p>
 * <b>Important:</b> Each class implementing this interface must have a Nullary
 * Constructor, so the object can be instantiated using the Class.fromName()
 * methods by the EncoderManager.
 * </p>
 * <ol>
 * <li>Filters (and subfilters) decode any special sequences for their format.
 * The goal is 100% Unicode inside Okapi. This includes normalizing newlines to
 * \n.
 *
 * <li>The exception to #1 is Skeleton and Code.data. This content should remain
 * unaltered to the extent <i>possible</i>. For example, XML processors
 * will decode everything and this is out of our control.
 *
 * <li>For non-problematic formats that use
 * {@link GenericFilterWriter}/{@link GenericSkeleton}, only an IEncoder
 * implementation is needed. Encoding is handled automatically in this case
 * based on {@link MimeTypeMapper}.
 *
 * <li>IEncoder implementation should reside in the encoders package in core.
 * 
 * <li>The IEncoder implementation should take into account
 * {@link EncoderContext}. Normally the encoder shouldn't be run on SKELETON or
 * INLINE content - or only run with a small subset of cases as compared to TEXT
 * (the goal is to keep SKELETON/INLINE as close to the original as possible).
 * 
 * <li>Special {@link IParameters} can be passed to the IEncoder if more
 * configuration is needed.
 * 
 * <li>{@link QuoteMode} is also provided to help guide logic around double and
 * single quotes. Some encoders take more parameters in their constructor
 * ({@link XMLEncoder})
 * 
 * <li>For &quot;problematic&quot; formats an {@link IFilterWriter} should be
 * implemented. This will give the full context of the {@link TextUnit} and
 * surrounding {@link Event}s. Encoding can be applied with more nuance. However
 * an IEncoder can still be implemented for default cases.
 * 
 * <li>ALL encoder logic should reside within the {@link IFilterWriter} and/or
 * IEncoder implementations. Not handled in ad hoc ways.
 * </ol>
 */
public interface IEncoder {

	/**
	 * Reset state in this encoder in preparation for processing new content.
	 */
	void reset();

	/**
	 * Sets the options for this encoder.
	 * 
	 * @param params    the parameters object with all the configuration information
	 *                  specific to this encoder.
	 * @param encoding  the name of the charset encoding to use.
	 * @param lineBreak the type of line break to use.
	 */
	void setOptions(IParameters params, String encoding, String lineBreak);

	/**
	 * Encodes a given text with this encoder.
	 * 
	 * @param text    the text to encode.
	 * @param context the context of the text: 0=text, 1=skeleton, 2=inline.
	 * @return the encoded text.
	 */
	String encode(String text, EncoderContext context);

	/**
	 * Encodes a given code-point with this encoding. If this method is called from
	 * a loop it is assumed that the code point is tested by the caller to know if
	 * it is a supplemental one or not and and any index update to skip the low
	 * surrogate part of the pair is done on the caller side.
	 * 
	 * @param codePoint the code-point to encode.
	 * @param context   the context of the character: 0=text, 1=skeleton, 2=inline.
	 * @return the encoded character (as a string since it can be now made up of
	 *         more than one character).
	 */
	String encode(int codePoint, EncoderContext context);

	/**
	 * Encodes a given character with this encoding.
	 * 
	 * @param value   the character to encode.
	 * @param context the context of the character: 0=text, 1=skeleton, 2=inline.
	 * @return the encoded character 9as a string since it can be now made up of
	 *         more than one character).
	 */
	String encode(char value, EncoderContext context);

	/**
	 * Converts any property values from its standard representation to the native
	 * representation for this encoder.
	 * 
	 * @param propertyName the name of the property.
	 * @param value        the standard value to convert.
	 * @return the native representation of the given value.
	 */
	default String toNative(String propertyName, String value) {
		return value;
	}

	/**
	 * Gets the line-break to use for this encoder.
	 * 
	 * @return the line-break used for this encoder.
	 */
	default String getLineBreak() {
		return "\n";
	}

	/**
	 * Gets the name of the charset encoding to use.
	 * 
	 * @return the charset encoding used for this encoder.
	 */
	default String getEncoding() {
		return StandardCharsets.UTF_8.name();
	}

	/**
	 * Gets the character set encoder used for this encoder.
	 * 
	 * @return the character set encoder used for this encoder. This can be null.
	 */
	default CharsetEncoder getCharsetEncoder() {
		return StandardCharsets.UTF_8.newEncoder();
	}

	/**
	 * Gets the parameters object with all the configuration information specific to
	 * this encoder.
	 * 
	 * @return the parameters object used for this encoder. This can be null.
	 */
	IParameters getParameters();
}
