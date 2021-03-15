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

package net.sf.okapi.filters.multiparsers;

import net.sf.okapi.common.encoder.DefaultEncoder;
import net.sf.okapi.common.encoder.EncoderContext;

public class CsvEncoder extends DefaultEncoder {

	@Override
	public String encode (String text,
		EncoderContext context)
	{
		if ( text == null ) return "";
		if ( text.indexOf('"') > -1 ) {
			return text.replace("\"", "\"\"");
		}
		return text;
	}

	@Override
	public String encode (char value,
		EncoderContext context)
	{
		if ( value == '"' ) return "\"\"";
		return String.valueOf(value);
	}

	@Override
	public String encode (int value, EncoderContext context) {
		if ( Character.isSupplementaryCodePoint(value) ) {
			return new String(Character.toChars(value));
		}
		return encode((char)value, context);
	}

}
