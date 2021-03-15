/*===========================================================================
  Copyright (C) 2009-2017 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.mymemory;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.LocaleId;

@RunWith(JUnit4.class)
public class MyMemoryTMConnectorTest {
	static final HashMap<String, String> OLD_LANGUAGE_TO_COUNTRY = new HashMap<>();
	// These were older mappings, done "by hand" before refactoring to use ICU ULocale.addLikelySubtags
	// Make sure the behavior does not change
	static {
		OLD_LANGUAGE_TO_COUNTRY.put("en", "us");
		OLD_LANGUAGE_TO_COUNTRY.put("pt", "br");
		OLD_LANGUAGE_TO_COUNTRY.put("el", "gr");
		OLD_LANGUAGE_TO_COUNTRY.put("he", "il");
		OLD_LANGUAGE_TO_COUNTRY.put("ja", "jp");
		OLD_LANGUAGE_TO_COUNTRY.put("ko", "kr");
		OLD_LANGUAGE_TO_COUNTRY.put("ms", "my");
		OLD_LANGUAGE_TO_COUNTRY.put("sl", "si");
		OLD_LANGUAGE_TO_COUNTRY.put("sq", "al");
		OLD_LANGUAGE_TO_COUNTRY.put("sv", "se");
		OLD_LANGUAGE_TO_COUNTRY.put("vi", "vn");
		OLD_LANGUAGE_TO_COUNTRY.put("zh", "cn");
	}

	@Test
	public void testOldToInternalCode() {
		try (MyMemoryTMConnector tmcon = new MyMemoryTMConnector()) {
			for (Entry<String, String> e : OLD_LANGUAGE_TO_COUNTRY.entrySet()) {		
				LocaleId locale = LocaleId.fromString(e.getKey());
				String localeWithRegion = tmcon.toInternalCode(locale);
				assertEquals(e.getKey() + "-" + e.getValue(), localeWithRegion);
			}
		}
	}

	private static String internalCodeFromStringLocaleId(String strLocaleId, MyMemoryTMConnector tmcon) {
		return tmcon.toInternalCode(LocaleId.fromBCP47(strLocaleId)).toLowerCase(Locale.US);
	}

	@Test
	public void testNewToInternalCode() {
		try (MyMemoryTMConnector tmcon = new MyMemoryTMConnector()) {
			// Make sure it does not damage existing country codes
			assertEquals("zh-cn", internalCodeFromStringLocaleId("zh", tmcon));
			assertEquals("zh-cn", internalCodeFromStringLocaleId("zh-CN", tmcon));
			assertEquals("zh-tw", internalCodeFromStringLocaleId("zh-TW", tmcon));

			// It looks like MyMemory does not know how to deal with scripts.
			// In the search combo-box I can only select Serbian, and it returns both Latin and Cyrillic entries 
			// The API doc says "Use ISO standard names or RFC3066". RFC3066 was obsoleted in 2006.
			// (at least this is the status now, March 2019)

			// Make sure it is not script-aware
			assertEquals("sr-rs", internalCodeFromStringLocaleId("sr", tmcon));
			assertEquals("sr-rs", internalCodeFromStringLocaleId("sr-Latn", tmcon));
			assertEquals("sr-rs", internalCodeFromStringLocaleId("sr-Cyrl", tmcon));
			// Mongolian use Cyrillic in Mongolia, but in China uses traditional Mongolian script
			assertEquals("mn-mn", internalCodeFromStringLocaleId("mn", tmcon));
			assertEquals("mn-cn", internalCodeFromStringLocaleId("mn-cn", tmcon));
			assertEquals("mn-mn", internalCodeFromStringLocaleId("mn-Cyrl", tmcon));
			assertEquals("mn-cn", internalCodeFromStringLocaleId("mn-Mong", tmcon));
		}
	}
}
