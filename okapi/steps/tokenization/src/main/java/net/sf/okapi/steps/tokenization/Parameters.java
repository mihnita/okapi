/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.steps.tokenization;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ListUtil;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.CheckListPart;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenization step parameters
 */

@EditorFor(Parameters.class)
public class Parameters extends StringParameters implements IEditorDescriptionProvider {
	private static final String TOKENIZESOURCE = "tokenizeSource";
	private static final String TOKENIZETARGETS = "tokenizeTargets";
	private static final String FILTEREDLOCALES = "filteredLocales";

	private boolean tokenizeSource;
	private boolean tokenizeTargets;
	private List<String> includedTokenNames;
	private String filterLocales;
	private boolean WORD;
	private boolean HYPHENATED_WORD;
	private boolean NUMBER;
	private boolean WHITESPACE;
	private boolean PUNCTUATION;
	private boolean DATE;
	private boolean TIME;
	private boolean CURRENCY;
	private boolean ABBREVIATION;
	private boolean MARKUP;
	private boolean EMAIL;
	private boolean INTERNET;
	private boolean EMOTICON;
	private boolean EMOJI;
	private boolean OTHER_SYMBOL;
	private boolean KANA;
	private boolean IDEOGRAM;

	@Override
	public void reset() {
		super.reset();
		includedTokenNames = new ArrayList<>();
		includedTokenNames.add(ITokenizer.TokenType.WORD.name());
		includedTokenNames.add(ITokenizer.TokenType.HYPHENATED_WORD.name());
		includedTokenNames.add(ITokenizer.TokenType.NUMBER.name());
		setTokenizeSource(true);
		setTokenizeTargets(false);
		setFilteredLocales(null);
		resetTokens();
	}

	private void resetTokens() {
		setWORD(false);
		setHYPHENATED_WORD(false);
		setNUMBER(false);
		setPUNCTUATION(false);
		setWHITESPACE(false);
		setABBREVIATION(false);
		setINTERNET(false);
		setEMAIL(false);
		setEMOTICON(false);
		setEMOJI(false);
		setDATE(false);
		setCURRENCY(false);
		setTIME(false);
		setMARKUP(false);
		setOTHER_SYMBOL(false);
		setKANA(false);
		setIDEOGRAM(false);
	}

	public boolean isWORD() {
		WORD = buffer.getBoolean(ITokenizer.TokenType.WORD.name());
		return WORD;
	}

	public void setWORD(boolean bWORD) {
		WORD = bWORD;
		buffer.setBoolean(ITokenizer.TokenType.WORD.name(), bWORD);
	}

	public boolean isHYPHENATED_WORD() {
		HYPHENATED_WORD = buffer.getBoolean(ITokenizer.TokenType.HYPHENATED_WORD.name());
		return HYPHENATED_WORD;
	}

	public void setHYPHENATED_WORD(boolean HYPHENATED_WORD) {
		this.HYPHENATED_WORD = HYPHENATED_WORD;
		buffer.setBoolean(ITokenizer.TokenType.HYPHENATED_WORD.name(), HYPHENATED_WORD);
	}

	public boolean isNUMBER() {
		NUMBER = buffer.getBoolean(ITokenizer.TokenType.NUMBER.name());
		return NUMBER;
	}

	public void setNUMBER(boolean NUMBER) {
		this.NUMBER = NUMBER;
		buffer.setBoolean(ITokenizer.TokenType.NUMBER.name(), NUMBER);
	}

	public boolean isWHITESPACE() {
		WHITESPACE = buffer.getBoolean(ITokenizer.TokenType.WHITESPACE.name());
		return WHITESPACE;
	}

	public void setWHITESPACE(boolean WHITESPACE) {
		this.WHITESPACE = WHITESPACE;
		buffer.setBoolean(ITokenizer.TokenType.WHITESPACE.name(), WHITESPACE);
	}

	public boolean isPUNCTUATION() {
		PUNCTUATION = buffer.getBoolean(ITokenizer.TokenType.PUNCTUATION.name());
		return PUNCTUATION;
	}

	public void setPUNCTUATION(boolean PUNCTUATION) {
		this.PUNCTUATION = PUNCTUATION;
		buffer.setBoolean(ITokenizer.TokenType.PUNCTUATION.name(), PUNCTUATION);
	}

	public boolean isDATE() {
		DATE = buffer.getBoolean(ITokenizer.TokenType.DATE.name());
		return DATE;
	}

	public void setDATE(boolean DATE) {
		this.DATE = DATE;
		buffer.setBoolean(ITokenizer.TokenType.DATE.name(), DATE);
	}

	public boolean isTIME() {
		TIME = buffer.getBoolean(ITokenizer.TokenType.TIME.name());
		return TIME;
	}

	public void setTIME(boolean TIME) {
		this.TIME = TIME;
		buffer.setBoolean(ITokenizer.TokenType.TIME.name(), TIME);
	}

	public boolean isCURRENCY() {
		CURRENCY = buffer.getBoolean(ITokenizer.TokenType.CURRENCY.name());
		return CURRENCY;
	}

	public void setCURRENCY(boolean CURRENCY) {
		this.CURRENCY = CURRENCY;
		buffer.setBoolean(ITokenizer.TokenType.CURRENCY.name(), CURRENCY);
	}

	public boolean isABBREVIATION() {
		ABBREVIATION = buffer.getBoolean(ITokenizer.TokenType.ABBREVIATION.name());
		return ABBREVIATION;
	}

	public void setABBREVIATION(boolean ABBREVIATION) {
		this.ABBREVIATION = ABBREVIATION;
		buffer.setBoolean(ITokenizer.TokenType.ABBREVIATION.name(), ABBREVIATION);
	}

	public boolean isMARKUP() {
		MARKUP = buffer.getBoolean(ITokenizer.TokenType.MARKUP.name());
		return MARKUP;
	}

	public void setMARKUP(boolean MARKUP) {
		this.MARKUP = MARKUP;
		buffer.setBoolean(ITokenizer.TokenType.MARKUP.name(), MARKUP);
	}

	public boolean isEMAIL() {
		EMAIL = buffer.getBoolean(ITokenizer.TokenType.EMAIL.name());
		return EMAIL;
	}

	public void setEMAIL(boolean EMAIL) {
		this.EMAIL = EMAIL;
		buffer.setBoolean(ITokenizer.TokenType.EMAIL.name(), EMAIL);
	}

	public boolean isINTERNET() {
		INTERNET = buffer.getBoolean(ITokenizer.TokenType.INTERNET.name());
		return INTERNET;
	}

	public void setINTERNET(boolean INTERNET) {
		this.INTERNET = INTERNET;
		buffer.setBoolean(ITokenizer.TokenType.INTERNET.name(), INTERNET);
	}

	public boolean isEMOTICON() {
		EMOTICON = buffer.getBoolean(ITokenizer.TokenType.EMOTICON.name());
		return EMOTICON;
	}

	public void setEMOTICON(boolean EMOTICON) {
		this.EMOTICON = EMOTICON;
		buffer.setBoolean(ITokenizer.TokenType.EMOTICON.name(), EMOTICON);
	}

	public boolean isEMOJI() {
		EMOJI = buffer.getBoolean(ITokenizer.TokenType.EMOJI.name());
		return EMOJI;
	}

	public void setEMOJI(boolean EMOJI) {
		this.EMOJI = EMOJI;
		buffer.setBoolean(ITokenizer.TokenType.EMOJI.name(), EMOJI);
	}

	public boolean isOTHER_SYMBOL() {
		OTHER_SYMBOL = buffer.getBoolean(ITokenizer.TokenType.EMOJI.name());
		return OTHER_SYMBOL;
	}

	public void setOTHER_SYMBOL(boolean OTHER_SYMBOL) {
		this.OTHER_SYMBOL = OTHER_SYMBOL;
		buffer.setBoolean(ITokenizer.TokenType.OTHER_SYMBOL.name(), OTHER_SYMBOL);
	}

	public boolean isKANA() {
		KANA = buffer.getBoolean(ITokenizer.TokenType.KANA.name());
		return KANA;
	}

	public void setKANA(boolean KANA) {
		this.KANA = KANA;
		buffer.setBoolean(ITokenizer.TokenType.KANA.name(), KANA);
	}

	public boolean isIDEOGRAM() {
		IDEOGRAM = buffer.getBoolean(ITokenizer.TokenType.IDEOGRAM.name());
		return IDEOGRAM;
	}

	public void setIDEOGRAM(boolean IDEOGRAM) {
		this.IDEOGRAM = IDEOGRAM;
		buffer.setBoolean(ITokenizer.TokenType.IDEOGRAM.name(), IDEOGRAM);
	}

	public boolean isTokenizeSource() {
		return getBoolean(TOKENIZESOURCE);
	}

	public void setTokenizeSource(boolean tokenizeSource) {
		setBoolean(TOKENIZESOURCE, tokenizeSource);
	}

	public String getFilteredLocales() {
		return getString(FILTEREDLOCALES);
	}

	public void setFilteredLocales(String filteredLocales) {
		setString(FILTEREDLOCALES, filteredLocales);
	}

	public boolean isTokenizeTargets() {
		return getBoolean(TOKENIZETARGETS);
	}

	public void setTokenizeTargets(boolean tokenizeTargets) {
		setBoolean(TOKENIZETARGETS, tokenizeTargets);
	}

	/**
	 * TokenType that will be filtered from the results of tokenization
	 **/
	public List<String> getIncludedTokenNames() {
		includedTokenNames.clear();
		if (WORD) {
			includedTokenNames.add(ITokenizer.TokenType.WORD.name());
		}
		if (HYPHENATED_WORD) {
			includedTokenNames.add(ITokenizer.TokenType.HYPHENATED_WORD.name());
		}
		if (NUMBER) {
			includedTokenNames.add(ITokenizer.TokenType.NUMBER.name());
		}
		if (WHITESPACE) {
			includedTokenNames.add(ITokenizer.TokenType.WHITESPACE.name());
		}
		if (PUNCTUATION) {
			includedTokenNames.add(ITokenizer.TokenType.PUNCTUATION.name());
		}
		if (DATE) {
			includedTokenNames.add(ITokenizer.TokenType.DATE.name());
		}
		if (TIME) {
			includedTokenNames.add(ITokenizer.TokenType.TIME.name());
		}
		if (CURRENCY) {
			includedTokenNames.add(ITokenizer.TokenType.CURRENCY.name());
		}
		if (ABBREVIATION) {
			includedTokenNames.add(ITokenizer.TokenType.ABBREVIATION.name());
		}
		if (MARKUP) {
			includedTokenNames.add(ITokenizer.TokenType.MARKUP.name());
		}
		if (EMAIL) {
			includedTokenNames.add(ITokenizer.TokenType.EMAIL.name());
		}
		if (INTERNET) {
			includedTokenNames.add(ITokenizer.TokenType.INTERNET.name());
		}
		if (EMOTICON) {
			includedTokenNames.add(ITokenizer.TokenType.EMOTICON.name());
		}
		if (EMOJI) {
			includedTokenNames.add(ITokenizer.TokenType.EMOJI.name());
		}
		if (OTHER_SYMBOL) {
			includedTokenNames.add(ITokenizer.TokenType.OTHER_SYMBOL.name());
		}
		if (IDEOGRAM) {
			includedTokenNames.add(ITokenizer.TokenType.IDEOGRAM.name());
		}
		if (KANA) {
			includedTokenNames.add(ITokenizer.TokenType.KANA.name());
		}
		return includedTokenNames;
	}

	public void setIncludedTokenNames(String... includedTokenNames) {
		this.includedTokenNames = ListUtil.arrayAsList(includedTokenNames);
		resetTokens();
		for (String n : includedTokenNames) {
			ITokenizer.TokenType t = ITokenizer.TokenType.valueOf(n);
			switch (t) {
			case WORD:
				setWORD(true);
				break;
			case HYPHENATED_WORD:
				setHYPHENATED_WORD(true);
				break;
			case NUMBER:
				setNUMBER(true);
				break;
			case WHITESPACE:
				setWHITESPACE(true);
				break;
			case PUNCTUATION:
				setPUNCTUATION(true);
				break;
			case DATE:
				setDATE(true);
				break;
			case TIME:
				setTIME(true);
				break;
			case CURRENCY:
				setCURRENCY(true);
				break;
			case ABBREVIATION:
				setABBREVIATION(true);
				break;
			case MARKUP:
				setMARKUP(true);
				break;
			case EMAIL:
				setEMAIL(true);
				break;
			case INTERNET:
				setINTERNET(true);
				break;
			case EMOTICON:
				setEMOTICON(true);
				break;
			case EMOJI:
				setEMOJI(true);
				break;
			case OTHER_SYMBOL:
				setOTHER_SYMBOL(true);
				break;
			case IDEOGRAM:
				setIDEOGRAM(true);
				break;
			case KANA:
				setKANA(true);
				break;
			default:
				// unknown token ignore
				break;
			}
		}
	}

	@Override
	public ParametersDescription getParametersDescription() {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(TOKENIZESOURCE, "Tokenize Source", "Create selected tokens for source locale");
		desc.add(TOKENIZETARGETS, "Tokenize Targets", "Create selected tokens for target locales");

		for (ITokenizer.TokenType t : ITokenizer.TokenType.values()) {
			String name = t.name();
			String description = t.getDescription();
			desc.add(name, name, description);
		}

		desc.add(FILTEREDLOCALES, "Locales to Filter",
				"Locale names or patterns used that are excluded from tokenization");

		return desc;
	}

	@Override
	public EditorDescription createEditorDescription(ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("Tokenizer", true, false);
		desc.addCheckboxPart(paramsDesc.get(TOKENIZESOURCE));
		desc.addCheckboxPart(paramsDesc.get(TOKENIZETARGETS));

		CheckListPart clp = desc.addCheckListPart("TokenType to Extract", 100);
		for (ITokenizer.TokenType t : ITokenizer.TokenType.values()) {
			String name = t.name();
			clp.addEntry(paramsDesc.get(name));
		}

		desc.addTextInputPart(paramsDesc.get(FILTEREDLOCALES));

		return desc;
	}
}
