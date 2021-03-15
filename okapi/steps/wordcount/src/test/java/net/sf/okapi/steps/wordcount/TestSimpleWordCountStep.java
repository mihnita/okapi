/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.wordcount;

import static org.junit.Assert.assertEquals;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestSimpleWordCountStep {

	@Test
	public void testTextUnitCounts() {
		SimpleWordCountStep step = new SimpleWordCountStep();	
		step.setSourceLocale(LocaleId.ENGLISH);
		StartDocument sd = new StartDocument("sd");
		sd.setLocale(LocaleId.ENGLISH);
		step.handleEvent(new Event(EventType.START_DOCUMENT, sd));

		ITextUnit tu = new TextUnit("tu");
		TextContainer tc = tu.getSource();
		ISegments segments = tc.getSegments();
		segments.append(new TextFragment("The number of words in this segment is 9."));
		segments.append(new TextFragment("The number of words in this second segment is 10."));
		segments.append(new TextFragment("And the number of words in this third segment is 11."));
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(30, WordCounter.getCount(tu.getSource()));
		
		tu.setSourceContent(new TextFragment(""));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(0, WordCounter.getCount(tu.getSource()));
		
		tu.setSourceContent(new TextFragment("..."));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(0, WordCounter.getCount(tu.getSource()));
		
		tu.setSourceContent(new TextFragment("Joe Pickett."));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(2, WordCounter.getCount(tu.getSource()));
		
		tu.setSourceContent(new TextFragment("Joe Pickett"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(2, WordCounter.getCount(tu.getSource()));

		// We count number as word
		tu.setSourceContent(new TextFragment("123 456 789"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(3, WordCounter.getCount(tu.getSource()));

		tu.setSourceContent(new TextFragment("The quick (\\\"brown\\\") fox can jump 32 feet, right?"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(9, WordCounter.getCount(tu.getSource()));

		// In EN apostrophe are not separator, nor is period
		tu.setSourceContent(new TextFragment("The quick (\\\"brown\\\") fox can't jump 32.2 feet, right?"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(9, WordCounter.getCount(tu.getSource()));

		// Period is not a separator
		tu.setSourceContent(new TextFragment("Born in the U.S.A"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(4, WordCounter.getCount(tu.getSource()));

		// Slash and back-slash are separators
		tu.setSourceContent(new TextFragment("Alpha/Beta\\Gamma"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(3, WordCounter.getCount(tu.getSource()));
	}

	@Test
	public void testTextUnitCountsFrench () {
		SimpleWordCountStep step = new SimpleWordCountStep();	
		step.setSourceLocale(LocaleId.FRENCH);
		StartDocument sd = new StartDocument("sd");
		sd.setLocale(LocaleId.FRENCH);
		step.handleEvent(new Event(EventType.START_DOCUMENT, sd));
		ITextUnit tu = new TextUnit("tu");

		// Apostrophe in French doesn't count as separator
		tu.setSourceContent(new TextFragment("L'objectif est defini"));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(3, WordCounter.getCount(tu.getSource()));
		
		// Avec accents and curly apostrophe: not separators
		tu.setSourceContent(new TextFragment("Elle a \u00E9t\u00E9 la " + 
			"premi\u00E8re Fran\u00E7aise d\u2019une famille d\u2019\u00E9migr\u00E9s."));
		tu.getSource().getAnnotations().clear();
		step.handleEvent(new Event(EventType.TEXT_UNIT, tu));
		assertEquals(9, WordCounter.getCount(tu.getSource()));
		
	}
}
