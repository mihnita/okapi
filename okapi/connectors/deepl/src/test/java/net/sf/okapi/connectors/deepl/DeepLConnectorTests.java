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

package net.sf.okapi.connectors.deepl;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.query.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class DeepLConnectorTests {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	@Ignore // It looks like the old 'free' API for DeepL is no longer accessible
	public void simpleTest () {
		DeepLConnector conn = new DeepLConnector();
		conn.open();
		conn.setLanguages(LocaleId.ENGLISH, LocaleId.FRENCH);
		conn.query("This is an example");
		assertTrue(conn.hasNext());
		while ( conn.hasNext() ) {
			QueryResult qr = conn.next();
			logger.info("- src: {}", qr.source.toText());
			logger.info("  trg: {}", qr.target.toText());
			assertTrue(qr.fromMT());
		}
		conn.close();
	}
	
}