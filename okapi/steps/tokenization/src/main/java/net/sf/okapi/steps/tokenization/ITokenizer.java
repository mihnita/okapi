package net.sf.okapi.steps.tokenization;

import net.sf.okapi.common.LocaleId;

public interface ITokenizer {
	boolean hasNext();

	net.sf.okapi.steps.tokenization.Token next();

	void init(String text, LocaleId language);

	enum TokenType {
		WORD("A run of characters constituting a word of the given language."),
		HYPHENATED_WORD("A word including hyphens of various types"),
		NUMBER("Numbers, including any commas or points symbols."),
		WHITESPACE("Whitespace characters as defined by the Unicode Consortium standards."),
		PUNCTUATION("Punctuation characters as defined by the Unicode Consortium standards."),
		DATE("Dates in the format MM/DD/YYYY."),
		TIME("Time separated by either \":\" or \".\" (24 hour, 12 hour with AM or PM)."),
		CURRENCY("Sums in various currencies"),
		ABBREVIATION("Limited types of English abbreviations like pct in 3.3pct, U.S., USD."),
		MARKUP("A run that begins with \"<\" and ends with \">\" like in HTML and XML."),
		EMAIL("E-mail addresses."),
		INTERNET("An Internet address (URI or IP): http://www.somesite.org/foo/index.html, 192.168.0.5."),
		EMOTICON("Emoticon sequences like \":-)\"."),
		EMOJI("All emoji characters defined in Unicode"),
		OTHER_SYMBOL("VVarious Unicode symbols (mathematical etc.)"),
		IDEOGRAM("Ideograms as defined by the Unicode Consortium standards."),
		KANA("Hiragana, Katakana (Japanese).");

		private String description;
		
		TokenType(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}
