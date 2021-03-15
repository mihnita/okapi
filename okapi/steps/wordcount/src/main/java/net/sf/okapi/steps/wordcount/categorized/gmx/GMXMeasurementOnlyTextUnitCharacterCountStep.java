/*===========================================================================
  Copyright (C) 2015 by the Okapi Framework contributors
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

package net.sf.okapi.steps.wordcount.categorized.gmx;

import net.sf.okapi.steps.tokenization.Tokens;
import net.sf.okapi.steps.wordcount.categorized.CategoryGroup;
import net.sf.okapi.steps.wordcount.categorized.CategoryHandler;
import net.sf.okapi.steps.wordcount.common.GMX;
import net.sf.okapi.steps.wordcount.common.TokenCharacterCountStep;

public class GMXMeasurementOnlyTextUnitCharacterCountStep extends TokenCharacterCountStep implements CategoryHandler {
	
	public static final String METRIC = GMX.MeasurementOnlyTextUnitCharacterCount;
	
	@Override
	protected String[] getTokenNames() {
		return new String[] {"DATE", "TIME", "CURRENCY"};
	}

	@Override
	protected Tokens filterTokens(Tokens allTokens) {
		Tokens timeTokens = allTokens.getFilteredList("TIME");
		Tokens singleTokens = allTokens.getFilteredList(new String[] { "DATE", "CURRENCY" });
		Tokens whiteSpace = allTokens.getFilteredList("WHITESPACE");
		if ((allTokens.size() - whiteSpace.size() - singleTokens.size()) != timeTokens.size()) {
			// Return empty tokens (zero count) if the text did not consist solely of
			// alphanumeric (and possibly whitespace) tokens.
			return new Tokens();
		}
		return allTokens.getFilteredList(getTokenNames());
	}

	@Override
	public String getName() {
		return "GMX Measurement Only Character Count";
	}

	@Override
	public String getDescription() {
		return "An accumulation of the character count from measurement-only text units."
		+ " Expects: filter events. Sends back: filter events.";
	}

	@Override
	public String getMetric() {
		return METRIC;
	}

	@Override
	protected boolean countOnlyTranslatable() {
		return true;
	}

	@Override
	public CategoryGroup getCategoryGroup() {
		return CategoryGroup.GMX_CHARACTER_COUNTS;
	}
}
