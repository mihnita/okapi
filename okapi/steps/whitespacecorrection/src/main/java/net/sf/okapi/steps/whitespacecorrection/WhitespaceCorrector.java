package net.sf.okapi.steps.whitespacecorrection;

import static net.sf.okapi.common.LocaleId.CHINA_CHINESE;
import static net.sf.okapi.common.LocaleId.JAPANESE;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextPart;

public class WhitespaceCorrector {
	public enum Whitespace {
		//vertical space
		LINE_FEED('\n'),
		LINE_TABULATION('\u000B'),
		FORM_FEED('\u000C'),
		CARRIAGE_RETURN('\r'),
		NEXT_LINE('\u0085'), 
		LINE_SEPARATOR('\u2028'),
		PARAGRAPH_SEPARATOR('\u2029'),
		
		//horizontal space
		CHARACTER_TABULATION('\u0009'),
		SPACE('\u0020'), 
		NO_BREAK_SPACE('\u00A0'), 
		EN_QUAD('\u2000'), 
		EM_QUAD('\u2001'), 
		EN_SPACE('\u2002'), 
		EM_SPACE('\u2003'), 
		THREE_PER_EM_SPACE('\u2004'), 
		FOUR_PER_EM_SPACER('\u2005'), 
		SIX_PER_EM_SPACE('\u2006'), 
		FIGURE_SPACE('\u2007'), 
		PUNCUATION_SPACE('\u2008'), 
		THIS_SPACE('\u2009'), 
		HAIR_SPACE('\u200A'), 
		NAORROW_NO_BREAK_SPACE('\u202F'), 
		MEDIUM_MATHEMATICAL_SPACE('\u205F'), 
		IDEOGRAPHIC_SPACE('\u3000'), 
		ZERO_WIDTH_SPACE('\u200B'), 
		ZERO_WIDTH_NON_BREAKING_SPACE('\uFEFF');
		
		private final char whitespace;
		
		private Whitespace(char whitespace) {
			this.whitespace = whitespace;
		}

		public char getWhitespace() {
			return whitespace;
		}
	}
	
    public enum Punctuation {
        // U+3002 IDEOGRAPHIC FULL STOP, U+FF0E FULLWIDTH FULL STOP
        FULL_STOP('.', '\u3002', '\uFF0E'),
        // U+3001 IDEOGRAPHIC COMMA, U+FF0C FULLWIDTH COMMA
        COMMA(',', '\u3001', '\uFF0C'), 
        // U+FF01 FULLWIDTH EXCLAMATION MARK
        EXCLAMATION_MARK('!', '\uff01'),
        // U+FF1F FULLWIDTH QUESTION MARK
        QUESTION_MARK ('?', '\uff1f');

        private final char[] whitespaceNonAcceptingForm;
        private final char whitespaceAcceptingForm;

        private Punctuation(char whitespaceAcceptingForm, char... whitespaceNonAcceptingForms) {
            this.whitespaceAcceptingForm = whitespaceAcceptingForm;
            this.whitespaceNonAcceptingForm = whitespaceNonAcceptingForms.clone();
        }

        public char getWhitespaceAcceptingForm() {
            return whitespaceAcceptingForm;
        }

        public char[] getWhitespaceNonAcceptingForm() {
        	return whitespaceNonAcceptingForm.clone();
        }
    }

    public static final Set<Whitespace> VERTICAL_WHITESPACE = Collections.unmodifiableSet(EnumSet.of(Whitespace.LINE_FEED,
    		Whitespace.LINE_TABULATION,
    		Whitespace.FORM_FEED,
    		Whitespace.CARRIAGE_RETURN,
    		Whitespace.NEXT_LINE, 
    		Whitespace.LINE_SEPARATOR,
    		Whitespace.PARAGRAPH_SEPARATOR));

    public static final Set<Whitespace> NONBREAKING_SPACES = Collections.unmodifiableSet(EnumSet.of(Whitespace.NO_BREAK_SPACE,
    		Whitespace.ZERO_WIDTH_NON_BREAKING_SPACE, 
    		Whitespace.NAORROW_NO_BREAK_SPACE));

    public static final Set<Whitespace> SPACE = Collections.unmodifiableSet(EnumSet.of(Whitespace.SPACE));

    public static final Set<Whitespace> ALL_WHITESPACE = Collections.unmodifiableSet(EnumSet.allOf(Whitespace.class));

    public static final Set<Whitespace> OTHER = Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(Whitespace.LINE_FEED,
    		Whitespace.LINE_TABULATION,
    		Whitespace.FORM_FEED,
    		Whitespace.CARRIAGE_RETURN,
    		Whitespace.NEXT_LINE, 
    		Whitespace.LINE_SEPARATOR,
    		Whitespace.PARAGRAPH_SEPARATOR,
    		Whitespace.NO_BREAK_SPACE,
    		Whitespace.ZERO_WIDTH_NON_BREAKING_SPACE, 
    		Whitespace.NAORROW_NO_BREAK_SPACE,
    		Whitespace.SPACE,
    		Whitespace.CHARACTER_TABULATION
    		)));

    public static final Set<Whitespace> HORIZONTAL_TABS = Collections.unmodifiableSet(EnumSet.of(Whitespace.CHARACTER_TABULATION));
    
    protected static final char WHITESPACE = ' ';

    protected LocaleId sourceLocale;
    protected LocaleId targetLocale;
    protected Set<Punctuation> punctuation;
    protected Set<Whitespace> whitespace;

    public WhitespaceCorrector(LocaleId sourceLocale, LocaleId targetLocale, 
    		Set<Punctuation> punctuation, Set<Whitespace> whitespace) {
        this.sourceLocale = sourceLocale;
        this.targetLocale = targetLocale;
        this.punctuation = punctuation;
        this.whitespace = whitespace;
    }

    static boolean isSpaceDelimitedLanguage(LocaleId localeId) {
        return !JAPANESE.sameLanguageAs(localeId) && !CHINA_CHINESE.sameLanguageAs(localeId);
    }

    public ITextUnit correctWhitespace(ITextUnit tu) {
        // target TextContainer may be null if the translation unit has no target element
        if (tu.getTarget(targetLocale) != null) {
            if (isSpaceDelimitedLanguage(sourceLocale) && !isSpaceDelimitedLanguage(targetLocale)) {
                removeTrailingWhitespace(tu);
            } else if (!isSpaceDelimitedLanguage(sourceLocale) && isSpaceDelimitedLanguage(targetLocale)) {
                addTrailingWhitespace(tu);
            }
        }
        return tu;
    }

    protected void removeTrailingWhitespace(ITextUnit textUnit) {
        TextContainer targetTextContainer = textUnit.getTarget(targetLocale);
        /*
         * If whitespace trimming was enabled during segmentation, the
         * whitespace will be trapped in non-Segment TextParts.  So
         * we need to check everything in the container, not just the
         * results of tu.getTargetSegments();
         */
        for (TextPart targetTextPart : targetTextContainer.getParts()) {
            TextFragment textFragment = findAndRemoveWhitespacesAfterPunctuation(targetTextPart.getContent());
            targetTextPart.setContent(textFragment);
        }
    }

    protected void addTrailingWhitespace(ITextUnit textUnit) {
        TextContainer sourceTextContainer = textUnit.getSource();
        TextContainer targetTextContainer = textUnit.getTarget(targetLocale);

        Iterator<TextPart> sourceTextPartsIterator = sourceTextContainer.getParts().iterator();
        Iterator<TextPart> targetTextPartsIterator = targetTextContainer.getParts().iterator();

        while (sourceTextPartsIterator.hasNext() && targetTextPartsIterator.hasNext()) {
            TextPart sourceTextPart = sourceTextPartsIterator.next();
            TextPart targetTextPart = targetTextPartsIterator.next();

            String sourceText = sourceTextPart.getContent().getText();
            if (sourceText.isEmpty() || !isNonSpaceDelimitedPunctuation(lastChar(sourceText))) {
                // the text does not end with punctuation requiring conversion
                continue;
            }

            if (isWhitespace(lastChar(targetTextPart.getContent().getText()))) {
                // the whitespace is present at the end
                continue;
            }

            targetTextPart.getContent().append(WHITESPACE);
        }
    }

    protected boolean isWhitespace(char c) {
        for (Whitespace ws : whitespace) {
        	if (c == ws.whitespace) {
        		return true;
        	}
        }
        return false;
    }

    private char lastChar(String s) {
        return s.charAt(s.length() - 1);
    }

    protected boolean isSpaceDelimitedPunctuation(char c) {
        for (Punctuation p : punctuation) {
            if (c == p.whitespaceAcceptingForm) {
                return true;
            }
        }
        return false;
    }

    protected boolean isNonSpaceDelimitedPunctuation(char c) {
        for (Punctuation p : punctuation) {
            for (char form : p.whitespaceNonAcceptingForm) {
                if (form == c) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private TextFragment findAndRemoveWhitespacesAfterPunctuation(TextFragment textFragment) {        
        TextFragment newTextFragment = new TextFragment();
        char[] chars = textFragment.getCodedText().toCharArray();

        for (int i = 0; i < chars.length; i++) {

            if (TextFragment.isMarker(chars[i])) {
                int codeIndex = TextFragment.toIndex(chars[++i]);
                newTextFragment.append(textFragment.getCode(codeIndex));

                continue;
            }

            newTextFragment.append(chars[i]);

            if (isNonSpaceDelimitedPunctuation(chars[i]) &&
                    i + 1 < chars.length &&
                    isWhitespace(chars[i + 1])) {
                i = getLastWhitespacePosition(chars, i + 1);                
            }
        }

        return newTextFragment;
    }

    private int getLastWhitespacePosition(char[] chars, int position) {
        do {
            position++;
        } while (position < chars.length && isWhitespace(chars[position]));

        return --position;
    }
}
