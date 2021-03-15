/*===========================================================================
  Copyright (C) 2012-2019 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.microsoft;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.query.QueryResult;
import net.sf.okapi.common.resource.TextFragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class MicrosoftBatchTokenConnectorTest {

	private final static String azureKey = "";//TODO: Set a valid key. Clear it before commit.

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Test
	public void paramTest () {
		try (MicrosoftMTConnector mmtc = new MicrosoftMTConnector()) {
			Parameters params = mmtc.getParameters();
			params.setAzureKey("testAzureKey");
			assertEquals("testAzureKey", params.getAzureKey());
		}
	}


	@Test
	public void testShort () {
		if (azureKey.isEmpty()) {
			logger.warn("Skipping manualTest() because azureKey is not given...");
			return;
		}
		int lenny;
		int lynn;
		QueryResult result;
		TextFragment frag;
		List<QueryResult> franz;
		List<List<QueryResult>> liszt;
		ArrayList<TextFragment> froggies;
		String sTranslation="";
		MicrosoftMTConnector mmtc = new MicrosoftMTConnector();
		Parameters params = mmtc.getParameters();
		// Add ClientId and Secret to test
		params.setAzureKey(azureKey);
		params.setCategory("general");
		
		// test query
		mmtc.open();
		mmtc.setLanguages(new LocaleId("en", "US"), new LocaleId("es", "ES"));
		mmtc.setThreshold(0);
		frag = new TextFragment("Hello!");
		mmtc.query(frag);
		if (mmtc.hasNext()) {
			result = mmtc.next();
			frag = result.target;
			sTranslation = frag.getText();
		}
		assertEquals("¡Hola!", sTranslation);
		
		froggies = new ArrayList<>();
		froggies.add(new TextFragment("Where is the bank?"));
		froggies.add(new TextFragment("What time is it?"));
		froggies.add(new TextFragment("Thank you and good bye."));
		liszt = mmtc.batchQuery(froggies);
		lynn = liszt.size();
		sTranslation = "";
		for(int i=0; i<lynn; i++) {
			franz = liszt.get(i);
			lenny = franz.size();
			for(int j=0; j<lenny; j++) {
				sTranslation += "$" + franz.get(j).target.getText();				
			}
		}
		assertEquals("$¿Dónde está el Banco?$¿Qué horas son?$Gracias y adiós.", sTranslation);
		mmtc.close();
	}
	
	@Test
	public void testManyPieces() {
		if (azureKey.isEmpty()) {
			logger.warn("Skipping manualTest() because azureKey is not given...");
			return;
		}
		// Run batch query that that has the number of pieces of text exceeding the API's limit.
		// This test uses the batchQueryText method.
		MicrosoftMTConnector mmtc = new MicrosoftMTConnector();
		Parameters params = mmtc.getParameters();
		// Add ClientId and Secret to test
		params.setAzureKey(azureKey);
		params.setCategory("general");
		
		// test query
		mmtc.open();
		mmtc.setLanguages(new LocaleId("en", "US"), new LocaleId("es", "ES"));
		mmtc.setThreshold(0);
		
		int numTexts = 102; // Unfortunately, the test method has no way to know which API is in use.
							// Here 102 exceeds the limit of both v2 and v3.
		List<String> texts = new ArrayList<>(numTexts);
		for(int i=0; i<numTexts; ++i) {
			texts.add(String.format("%d sheep", i));
		}
		
		List<List<QueryResult>> translatedTexts = mmtc.batchQueryText(texts);
		
		assertEquals(numTexts, translatedTexts.size());
		assertEquals(1, translatedTexts.get(0).size());
		assertEquals("0 ovejas", translatedTexts.get(0).get(0).target.getText());
		assertEquals("101 ovejas", translatedTexts.get(numTexts-1).get(0).target.getText());

		mmtc.close();
	}
}
