/*===========================================================================
 * Adapted from Henri Sivonen's code
 * --------------------------------------------------------------------------
 * Copyright (c) 2006 Henri Sivonen
 * Copyright (c) 2007-2010 Mozilla Foundation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 * -------------------------------------------------------------------------
 * The adapted code has bug fixes
 ===========================================================================*/

package net.sf.okapi.lib.xliff2.lang;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Implement verification for language tags.
 */
public final class Language {

    /**
     * The singleton instance.
     */
    public static final Language THE_INSTANCE = new Language();

    private static final Pattern HYPHEN = Pattern.compile("-");
    private static final boolean WARN = true;

    private static String[] languages = null;
    private static String[] extlangs = null;
    private static String[] scripts = null;
    private static String[] regions = null;
    private static String[] variants = null;
    private static String[] grandfathered = null;
    private static String[] redundant = null;
    private static String[] deprecated = null;
    private static String[] deprecatedLang = null;
    private static int[] suppressedScriptByLanguage = null;
    private static Map<String, String> preferredValueByLanguageMap = new HashMap<>();
    private static String[][][] prefixesByVariant = null;
    private static int[] prefixByExtlang = null;

    static {
        try {
            LanguageData data = new LanguageData();
            languages = data.getLanguages();
            extlangs = data.getExtlangs();
            scripts = data.getScripts();
            regions = data.getRegions();
            variants = data.getVariants();
            grandfathered = data.getGrandfathered();
            redundant = data.getRedundant();
            deprecated = data.getDeprecated();
            deprecatedLang = data.getDeprecatedLang();
            suppressedScriptByLanguage = data.getSuppressedScriptByLanguage();
            prefixByExtlang = data.getPrefixByExtlang();
            preferredValueByLanguageMap = data.getPreferredValueByLanguageMap();
            prefixesByVariant = data.getPrefixesByVariant();
		}
		catch ( IOException e ) {
			throw new XLIFFException(e);
		}
    }

    /**
     * Package-private constructor
     */
    private Language() {
        super();
    }

    /**
     * Verify the validity of a given language tag.
     * @param literal the language tag to verify.
     * @throws InvalidParameterException if the tag is invalid or if there is a warning for it.
     * If the issue is just a warning, the error message starts with "Warning: ".
     */
    public void checkValid (String literal) {
        if (literal.length() == 0) {
        	throw new InvalidParameterException("The empty string is not a valid language tag.");
        }
        literal = toAsciiLowerCase(literal);
		if ( isGrandfathered(literal) ) {
			if ( WARN && isDeprecated(literal) ) {
            	throw new InvalidParameterException("Warning: The grandfathered language tag "+
                        literal+ " is deprecated." + " Use \u201C"
                                + preferredValueByLanguageMap.get(literal)
                                + "\u201D instead.");
            }
            return;
        }
		if ( isRedundant(literal) ) {
			if ( isDeprecated(literal) && WARN ) {
            	throw new InvalidParameterException("Warning: The language tag "+ literal+
                        " is deprecated." + " Use \u201C"
                                + preferredValueByLanguageMap.get(literal)
                                + "\u201D instead.");
            }
            return;
        }
		if ( literal.startsWith("-") ) {
        	throw new InvalidParameterException("Language tag must not start with HYPHEN-MINUS.");
        }
		if ( literal.endsWith("-") ) {
        	throw new InvalidParameterException("Language tag must not end with HYPHEN-MINUS.");
        }

        String[] subtags = HYPHEN.split(literal);

		for (String s : subtags) {
			int len = s.length();
			if (len == 0) {
				throw new InvalidParameterException("Zero-length subtag.");
			} else if (len > 8) {
				throw new InvalidParameterException("Subtags must not exceed 8 characters in length.");
			}
		}

        // Language

        int i = 0;
        String subtag = subtags[i];
        int len = subtag.length();
        if ("x".equals(subtag)) {
            checkPrivateUse(i, subtags);
            return;
        }
		if ( (len == 2 || len == 3) && isLowerCaseAlpha(subtag) ) {
			if ( !isLanguage(subtag) ) {
            	throw new InvalidParameterException("The language subtag "+ subtag+
                        " is not a valid ISO language part of a language tag.");
            }
			if ( isDeprecatedLang(subtag) && WARN ) {
            	throw new InvalidParameterException("Warning: The language subtag "+ subtag+
                        " is deprecated." + " Use \u201C"
                                + preferredValueByLanguageMap.get(literal)
                                + "\u201D instead.");
            }
            i++;
			if ( i == subtags.length ) {
                return;
            }
            subtag = subtags[i];
            len = subtag.length();
		}
		else if ( len == 4 && isLowerCaseAlpha(subtag) ) {
        	throw new InvalidParameterException("Found reserved language tag: "+ subtag+
                    ".");
		}
		else if ( len >= 5 && isLowerCaseAlpha(subtag) ) {
			if ( !isLanguage(subtag) ) {
            	throw new InvalidParameterException("The language subtag "+ subtag+
                        " is not a valid IANA language part of a language tag.");
            }
			if ( isDeprecatedLang(subtag) && WARN ) {
            	throw new InvalidParameterException("Warning: The language subtag "+ subtag+
                        " is deprecated." + " Use \u201C"
                                + preferredValueByLanguageMap.get(literal)
                                + "\u201D instead.");
            }
            i++;
			if ( i == subtags.length ) {
                return;
            }
            subtag = subtags[i];
            len = subtag.length();
		}
		else {
        	throw new InvalidParameterException("The language subtag "+ subtag+
                    " is not a valid language subtag.");
        }

        // extlang

		if ( "x".equals(subtag) ) {
            checkPrivateUse(i, subtags);
            return;
        }
		if ( subtag.length() == 3 && isLowerCaseAlpha(subtag) ) {
			if ( !isExtlang(subtag) ) {
            	throw new InvalidParameterException("Bad extlang subtag "+ subtag+ ".");
            }
			if ( !usesPrefixByExtlang(subtags[0], subtag) ) {
                // IANA language tags are never correct prefixes.
            	throw new InvalidParameterException("Extlang subtag "+ subtag+
                        " has an incorrect prefix.");
            }
            i++;
            if (i == subtags.length) {
                return;
            }
            subtag = subtags[i];
            len = subtag.length();
        }

        // Script?

		if ( "x".equals(subtag) ) {
            checkPrivateUse(i, subtags);
            return;
        }
		if ( subtag.length() == 4 & isLowerCaseAlpha(subtag) ) {
			if ( !isScript(subtag) ) {
            	throw new InvalidParameterException("Bad script subtag.");
            }
			if ( WARN && isDeprecated(subtag) ) {
            	throw new InvalidParameterException("Warning: The script subtag "+ subtag+
                        " is deprecated." + " Use \u201C"
                                + preferredValueByLanguageMap.get(literal)
                                + "\u201D instead.");
            }
			if ( WARN && shouldSuppressScript(subtags[0], subtag) ) {
            	throw new InvalidParameterException("Warning: Language tag should omit the default script for the language.");
            }
            i++;
			if ( i == subtags.length ) {
                return;
            }
            subtag = subtags[i];
            len = subtag.length();
        }

        // Region

        if ((len == 3 && isDigit(subtag))
                || (len == 2 && isLowerCaseAlpha(subtag))) {
            if (!isRegion(subtag)) {
            	throw new InvalidParameterException("Bad region subtag.");
            }
            if (isDeprecated(subtag) && WARN) {
                throw new InvalidParameterException("Warning: The region subtag "+ subtag+
                        " is deprecated." + " Use \u201C"
                                + preferredValueByLanguageMap.get(literal)
                                + "\u201D instead.");
            }
            i++;
            if (i == subtags.length) {
                return;
            }
            subtag = subtags[i];
            len = subtag.length();
        }

        // Variant

        for (;;) {
            if ("x".equals(subtag)) {
                checkPrivateUse(i, subtags);
                return;
            }
            // cutting corners here a bit since there are no extensions at this
            // time
            if (len == 1 && isLowerCaseAlphaNumeric(subtag)) {
                throw new InvalidParameterException("Unknown extension "+ subtag+ ".");
            }
            else if ((len == 4 && isDigit(subtag.charAt(0)) && isLowerCaseAlphaNumeric(subtag))
                    || (len >= 5 && isLowerCaseAlphaNumeric(subtag))) {
                if (!isVariant(subtag)) {
                    throw new InvalidParameterException("Bad variant subtag "+ subtag+ ".");
                }
				if ( isDeprecated(subtag) && WARN ) {
                    throw new InvalidParameterException("Warning: The variant subtag "+ subtag+
                            " is deprecated." + " Use \u201C"
                                    + preferredValueByLanguageMap.get(literal)
                                    + "\u201D instead.");
                }
				if ( !hasGoodPrefix(subtags, i) ) {
                    throw new InvalidParameterException("Variant "+ subtag+ " lacks required prefix.");
                }
            }
            else {
                throw new InvalidParameterException("The subtag "+ subtag+
                        " does not match the format for any permissible subtag type.");
            }
            i++;
            if (i == subtags.length) {
                return;
            }
            subtag = subtags[i];
            len = subtag.length();
        }
    }
    
    private boolean hasGoodPrefix(String[] subtags, int i) {
        String variant = subtags[i];
        int index = Arrays.binarySearch(variants, variant);
        assert index >= 0;
        String[][] prefixes = prefixesByVariant[index];
        if (prefixes.length == 0) {
            return true;
        }
		for (String[] prefix : prefixes) {
			if (prefixMatches(prefix, subtags, i)) {
				return true;
			}
		}
        return false;
    }

    private boolean prefixMatches(String[] prefix, String[] subtags, int limit) {
		for (String prefixComponent : prefix) {
			if (!subtagsContainPrefixComponent(prefixComponent, subtags, limit)) {
				return false;
			}
		}
        return true;
    }

    private boolean subtagsContainPrefixComponent(String prefixComponent,
            String[] subtags, int limit) {
        for (int i = 0; i < limit; i++) {
            String subtag = subtags[i];
            if (subtag.equals(prefixComponent)) {
                return true;
            }
        }
        return false;
    }

    private boolean usesPrefixByExtlang(String language, String extlang) {
        int langIndex = Arrays.binarySearch(languages, language);
        int extlangIndex = Arrays.binarySearch(extlangs, extlang);
        assert langIndex > -1;
        int prefixExpected = prefixByExtlang[extlangIndex];
        return prefixExpected == langIndex;
    }

    private boolean shouldSuppressScript(String language, String script) {
        int langIndex = Arrays.binarySearch(languages, language);
        assert langIndex > -1;
        int scriptIndex = suppressedScriptByLanguage[langIndex];
        if (scriptIndex < 0) {
            return false;
        } else {
            return scripts[scriptIndex].equals(script);
        }
    }

    private boolean isVariant(String subtag) {
        return (Arrays.binarySearch(variants, subtag) > -1);
    }

    private boolean isRegion(String subtag) {
        return (Arrays.binarySearch(regions, subtag) > -1)
                || "aa".equals(subtag)
                || ("qm".compareTo(subtag) <= 0 && "qz".compareTo(subtag) >= 0)
                || ("xa".compareTo(subtag) <= 0 && "xz".compareTo(subtag) >= 0)
                || "zz".equals(subtag);
    }

    private boolean isScript(String subtag) {
        return (Arrays.binarySearch(scripts, subtag) > -1)
                || ("qaaa".compareTo(subtag) <= 0 && "qabx".compareTo(subtag) >= 0);
    }

    private boolean isExtlang(String subtag) {
        return (Arrays.binarySearch(extlangs, subtag) > -1);
    }

    private boolean isLanguage(String subtag) {
        return (Arrays.binarySearch(languages, subtag) > -1)
                || ("qaa".compareTo(subtag) <= 0 && "qtz".compareTo(subtag) >= 0);
    }

    private void checkPrivateUse (int i,
    	String[] subtags)
    {
        int len = subtags.length;
        i++;
        if (i == len) {
            throw new InvalidParameterException("No subtags in private use sequence.");
        }
        while (i < len) {
            String subtag = subtags[i];
			if ( !isLowerCaseAlphaNumeric(subtag) ) {
                throw new InvalidParameterException("Bad character in private use subtag "+ subtag+ ".");
            }
            i++;
        }
    }

	private final boolean isLowerCaseAlphaNumeric (char c) {
        return isLowerCaseAlpha(c) || isDigit(c);
    }

	private final boolean isLowerCaseAlphaNumeric (String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isLowerCaseAlphaNumeric(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private final boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private final boolean isDigit(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private final boolean isLowerCaseAlpha(char c) {
        return (c >= 'a' && c <= 'z');
    }

    private final boolean isLowerCaseAlpha(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!isLowerCaseAlpha(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isGrandfathered (String literal) {
        return Arrays.binarySearch (grandfathered, literal) > -1;
    }

    private boolean isRedundant(String literal) {
        return Arrays.binarySearch (redundant, literal) > -1;
    }

    private boolean isDeprecated(String subtag) {
        return Arrays.binarySearch (deprecated, subtag) > -1;
    }

    private boolean isDeprecatedLang (String subtag) {
        return Arrays.binarySearch(deprecatedLang, subtag) > -1;
    }

	private String toAsciiLowerCase (CharSequence str) {
		if ( str == null ) {
			return null;
		}
		char[] buf = new char[str.length()];
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			if ( c >= 'A' && c <= 'Z' ) {
				c += 0x20;
			}
			buf[i] = c;
		}
		return new String(buf);
    }    

}