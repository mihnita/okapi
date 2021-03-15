package net.sf.okapi.steps.whitespacecorrection;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

import static net.sf.okapi.steps.whitespacecorrection.WhitespaceCorrector.*;

@EditorFor(WhitespaceCorrectionStepParameters.class)
public class WhitespaceCorrectionStepParameters extends StringParameters implements IEditorDescriptionProvider {
    private static final String PUNCTUATION = "punctuation";
    private static final String WHITESPACE = "whitespace";

    private EnumSet<Punctuation> punctuation = EnumSet.allOf(Punctuation.class);
    private EnumSet<Whitespace> whitespace = EnumSet.copyOf(ALL_WHITESPACE);
    
    @Override
    public void reset() {
        super.reset();
        punctuation = EnumSet.allOf(Punctuation.class);
        whitespace = EnumSet.copyOf(ALL_WHITESPACE);
    }

    public void setWhiteSpace(Collection<Whitespace> whitespace) {
        this.whitespace.clear();
        this.whitespace.addAll(whitespace);
    }
    
    public void setPunctuation(Collection<Punctuation> punctuation) {
        this.punctuation.clear();
        this.punctuation.addAll(punctuation);
    }
    
    public EnumSet<Punctuation> getPunctuation() {
        return punctuation;
    }
    
    public EnumSet<Whitespace> getWhitespace() {
        return whitespace;
    }

    @Override
    public String toString() {
        String ps = punctuation.stream().map(Enum::toString).collect(Collectors.joining(","));
        buffer.setString(PUNCTUATION, ps);
        
        String ws = whitespace.stream().map(Enum::toString).collect(Collectors.joining(","));
        buffer.setString(WHITESPACE, ws);
        return super.toString();
    }

    @Override
    public void fromString(String data) {
        super.fromString(data);
        loadPunctuation(buffer.getString(PUNCTUATION));
        loadWhitespace(buffer.getString(WHITESPACE));
    }

    private void loadPunctuation(String s) {
    	if (s.equals("")) return;
        punctuation.clear();
        for (String ps : s.split(",")) {
            Punctuation p = Punctuation.valueOf(ps);
            if (ps != null) {
                punctuation.add(p);
            }
        }
    }
    
    private void loadWhitespace(String s) {
    	if (s.equals("")) return;
        whitespace.clear();
        for (String ws : s.split(",")) {
            Whitespace w = Whitespace.valueOf(ws);
            if (ws != null) {
                whitespace.add(w);
            }
        }
    }

    public boolean getFullStop() {
        return punctuation.contains(Punctuation.FULL_STOP);
    }

    public void setFullStop(boolean value) {
        set(Punctuation.FULL_STOP, value);
    }

    public boolean getComma() {
        return punctuation.contains(Punctuation.COMMA);
    }

    public void setComma(boolean value) {
        set(Punctuation.COMMA, value);
    }

    public boolean getExclamationPoint() {
        return punctuation.contains(Punctuation.EXCLAMATION_MARK);
    }

    public void setExclamationPoint(boolean value) {
        set(Punctuation.EXCLAMATION_MARK, value);
    }

    public boolean getQuestionMark() {
        return punctuation.contains(Punctuation.QUESTION_MARK);
    }

    public void setQuestionMark(boolean value) {
        set(Punctuation.QUESTION_MARK, value);
    }

    public boolean getVerticalWhitespace() {
        return whitespace.containsAll(VERTICAL_WHITESPACE);
    }

    public void setVerticalWhitespace(boolean value) {
        set(VERTICAL_WHITESPACE, value);
    }

    public boolean getNonbreakingWhitespace() {
        return whitespace.containsAll(NONBREAKING_SPACES);
    }

    public void setNonbreakingWhitespace(boolean value) {
        set(NONBREAKING_SPACES, value);
    }

    public boolean getHorizontalTabs() {
        return whitespace.containsAll(HORIZONTAL_TABS);
    }

    public void setHorizontalTabs(boolean value) {
        set(HORIZONTAL_TABS, value);
    }

    public boolean getSpace() {
        return whitespace.containsAll(SPACE);
    }

    public void setSpace(boolean value) {
        set(SPACE, value);
    }

    public boolean getOther() {
        return whitespace.containsAll(OTHER);
    }

    public void setOther(boolean value) {
        set(OTHER, value);
    }

    private void set(Punctuation p, boolean value) {
        if (value) {
            punctuation.add(p);
        }
        else {
            punctuation.remove(p);
        }
    }

    private void set(Set<Whitespace> ws, boolean value) {
        if (value) {
            whitespace.addAll(ws);
        }
        else {
            whitespace.removeAll(ws);
        }
    }

    @Override
    public ParametersDescription getParametersDescription () {
        ParametersDescription desc = new ParametersDescription(this);
        desc.add("fullStop", "Full Stop", null);
        desc.add("comma", "Comma", null);
        desc.add("exclamationPoint", "Exclamation Point", null);
        desc.add("questionMark", "Question Mark", null);
        desc.add("verticalWhitespace", "Vertical White Space", null);
        desc.add("nonbreakingWhitespace", "Nonbreaking Whitespace", null);
        desc.add("horizontalTabs", "Horizontal Tabs", null);
        desc.add("space", "Standard White Space", null);
        desc.add("other", "All Other Whitespace", null);
        return desc;
    }

    @Override
    public EditorDescription createEditorDescription (ParametersDescription paramDesc) {
        EditorDescription desc = new EditorDescription("Correct whitespace following", true, false);
        desc.addCheckboxPart(paramDesc.get("fullStop"));
        desc.addCheckboxPart(paramDesc.get("comma"));
        desc.addCheckboxPart(paramDesc.get("exclamationPoint"));
        desc.addCheckboxPart(paramDesc.get("questionMark"));
        desc.addSeparatorPart();
        desc.addTextLabelPart("Whitespace To Be Deleted");
        desc.addCheckboxPart(paramDesc.get("verticalWhitespace"));
        desc.addCheckboxPart(paramDesc.get("nonbreakingWhitespace"));
        desc.addCheckboxPart(paramDesc.get("horizontalTabs"));
        desc.addCheckboxPart(paramDesc.get("space"));
        desc.addCheckboxPart(paramDesc.get("other"));

        return desc;
    }
}
