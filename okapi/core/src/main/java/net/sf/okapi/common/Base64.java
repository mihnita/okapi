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

package net.sf.okapi.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import net.sf.okapi.common.exceptions.OkapiIOException;

/**
* Base64 Encoder/Decoder for all VM.
* @deprecated Use the {@link java.util.Base64}, available since JDK 8
*/
@Deprecated
public class Base64 {
	private static final String ENCSTR = "#BeNcStr";
	private static final Charset CSUTF8 = StandardCharsets.UTF_8;

	/**
	 * Encodes a string into UTF-8 Base64 format.
	 * No blanks or line breaks are inserted.
	 * @param str the String to be encoded.
	 * @return the String with the Base64 encoded data.
	 */
	public static String encodeString (String str) {
		Encoder encoder = java.util.Base64.getEncoder();
		return new String(encoder.encode(str.getBytes(CSUTF8)), CSUTF8); 
	}

	/**
	* Encodes a byte array into Base64 format.
	* No blanks or line breaks are inserted.
	* @param data the array containing the data bytes to be encoded.
	* @return the character array with the Base64 encoded data.
	*/
	public static char[] encode (byte[] data) {
		Encoder encoder = java.util.Base64.getEncoder();
		return new String(encoder.encode(data), CSUTF8).toCharArray();
	}

	/**
	* Encodes a byte array into Base64 format.
	* No blanks or line breaks are inserted.
	* @param data the array containing the data bytes to be encoded.
	* @param iLen the number of bytes to process in <code>in</code>.
	* @return the character array with the Base64 encoded data.
	*/
	public static char[] encode (byte[] data, int iLen) {
		if (iLen > data.length) {
			iLen = data.length;
		}
		if (iLen == data.length) {
			return encode(data);
		}
		return encode(Arrays.copyOf(data, iLen));
	}

	/**
	* Decodes a string from a UTF-8 Base64 string.
	* The coded string may have line breaks.
	* @param str the Base64 String to be decoded.
	* @return the String containing the decoded data.
	* @throws IllegalArgumentException if the input is not valid Base64 encoded data.
	*/
	public static String decodeString (String str) {
		str = str.replaceAll("[\r\n]", "");
		return new String(decode(str), CSUTF8);
	}
	
	/**
	* Decodes a byte array from Base64 format.
	* The coded string may have line breaks.
	* @param str the Base64 String to be decoded.
	* @return the array containing the decoded data bytes.
	* @throws IllegalArgumentException if the input is not valid Base64 encoded data.
	*/
	public static byte[] decode (String str) {
		str = str.replaceAll("[\r\n]", "");
		return decode(str.toCharArray());
	}

	/**
	* Decodes a byte array from Base64 format.
	* No blanks or line breaks are allowed within the Base64 encoded data.
	* @param data the character array containing the Base64 encoded data.
	* @return the array containing the decoded data bytes.
	* @throws IllegalArgumentException if the input is not valid Base64 encoded data.
	*/
	public static byte[] decode (char[] data) {
		Decoder decoder = java.util.Base64.getDecoder();
		return decoder.decode(new String(data));
	}

	public static String encode (InputStream is) {
		if (is == null)
			throw new IllegalArgumentException("Input stream for Base64 encoding cannot be null.");

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		byte[] buffer = new byte[2048];
		int bytesRead = 0;
		try {
			while ((bytesRead = is.read(buffer)) > 0) {
				os.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			throw new OkapiIOException ("I/O exception while reading data for Base64 encoding.", e);
		}

		return new String(encode(os.toByteArray()));
	}

	/**
	 * Encode a password-type string value.
	 * @param password the password (in clear).
	 * @return the masked value for the given string.
	 * @see #decodePassword(String)
	 */
	public static String encodePassword (String password) {
		return ENCSTR+Base64.encodeString(password);
	}
	
	/**
	 * Decode a string value that is possibly encoded into a clear string.
	 * @param password the string to decode. It may be a clear string too.
	 * @return the decoded string.
	 */
	public static String decodePassword (String password) {
		if ( password.startsWith(ENCSTR) ) {
			return Base64.decodeString(password.substring(ENCSTR.length()));
		}
		else {
			return password;
		}
	}

}


