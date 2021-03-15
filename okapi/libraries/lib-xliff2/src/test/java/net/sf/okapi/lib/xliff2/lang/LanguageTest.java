package net.sf.okapi.lib.xliff2.lang;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.lib.xliff2.InvalidParameterException;

@SuppressWarnings("static-method")
@RunWith(JUnit4.class)
public class LanguageTest {
	private static Language language = Language.THE_INSTANCE;

	@Test
	public void testCheckValid() {
		language.checkValid("sr");
		language.checkValid("sr-Latn");
		language.checkValid("sr-RS");
		language.checkValid("sr-Latn-RS");
		language.checkValid("es-419");
	}

	@Test(expected = InvalidParameterException.class)
	public void testCheckInvalidLanguageId() {
		// The language subtag dummy is not a valid IANA language part of a language tag.
		language.checkValid("dummy");
	}

	@Test(expected = InvalidParameterException.class)
	public void testCheckInvalidStartWithMinus() {
		// Language tag must not start with HYPHEN-MINUS.
		language.checkValid("-sr");
	}

	@Test(expected = InvalidParameterException.class)
	public void testCheckInvalidOmitDefaultScript() {
		// Language tag should omit the default script for the language.
		language.checkValid("es-Latn");
	}

	@Test(expected = InvalidParameterException.class)
	public void testCheckInvalidEmptyString() {
		// The empty string is not a valid language tag.
		language.checkValid("");
	}

	@Test(expected = InvalidParameterException.class)
	public void testCheckInvalidDeprecated() {
		// The language tag zh-cmn is deprecated. Use “cmn” instead.
		language.checkValid("zh-cmn");
	}
}
