/*
 * =============================================================================
 *   Copyright (C) 2009-2017 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */

package net.sf.okapi.filters.idml;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.Sanitiser;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.StringSanitiser;
import net.sf.okapi.common.filters.fontmappings.FontMappings;
import net.sf.okapi.common.filters.fontmappings.DefaultFontMappings;
import net.sf.okapi.common.filters.fontmappings.ParametersStringFontMappingsInput;
import net.sf.okapi.common.filters.fontmappings.ParametersStringFontMappingsOutput;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

@EditorFor(Parameters.class)
public class Parameters extends StringParameters implements IEditorDescriptionProvider {

    /**
     * An empty string.
     */
    private static final String EMPTY = "";

    /**
     * A "not valid" message.
     */
    private static final String VALUE_IS_NOT_VALID = " \"%s\" value is not valid";

    /**
     * As there might be present attribute values which go beyond any limit (e.g. Preferences > PrintPreference > PrintRecord),
     * providing a parameter to adjust the default maximum attribute size.
     */
    private static final String MAX_ATTRIBUTE_SIZE = "maxAttributeSize";

    private static final String UNTAG_XML_STRUCTURES = "untagXmlStructures";
    private static final String EXTRACT_NOTES = "extractNotes";
    private static final String EXTRACT_MASTER_SPREADS = "extractMasterSpreads";
    private static final String EXTRACT_HIDDEN_LAYERS = "extractHiddenLayers";
    private static final String EXTRACT_HIDDEN_PASTEBOARD_ITEMS = "extractHiddenPasteboardItems";
    private static final String SKIP_DISCRETIONARY_HYPHENS = "skipDiscretionaryHyphens";
    private static final String EXTRACT_BREAKS_INLINE = "extractBreaksInline";

    /**
     * An ignorance of a character kerning style parameter name.
     */
    private static final String IGNORE_CHARACTER_KERNING = "ignoreCharacterKerning";

    /**
     * An optional character kerning minimum ignorance threshold parameter name.
     */
    private static final String CHARACTER_KERNING_MIN_IGNORANCE_THRESHOLD =
            "characterKerningMinIgnoranceThreshold";

    /**
     * An optional character kerning maximum ignorance threshold parameter name.
     */
    private static final String CHARACTER_KERNING_MAX_IGNORANCE_THRESHOLD =
            "characterKerningMaxIgnoranceThreshold";

    /**
     * An ignorance of a character tracking style parameter name.
     */
    private static final String IGNORE_CHARACTER_TRACKING = "ignoreCharacterTracking";

    /**
     * An optional character tracking minimum ignorance threshold parameter name.
     */
    private static final String CHARACTER_TRACKING_MIN_IGNORANCE_THRESHOLD =
            "characterTrackingMinIgnoranceThreshold";

    /**
     * An optional character tracking maximum ignorance threshold parameter name.
     */
    private static final String CHARACTER_TRACKING_MAX_IGNORANCE_THRESHOLD =
            "characterTrackingMaxIgnoranceThreshold";

    /**
     * An ignorance of a character leading style parameter name.
     */
    private static final String IGNORE_CHARACTER_LEADING = "ignoreCharacterLeading";

    /**
     * An optional character leading minimum ignorance threshold parameter name.
     */
    private static final String CHARACTER_LEADING_MIN_IGNORANCE_THRESHOLD =
            "characterLeadingMinIgnoranceThreshold";

    /**
     * An optional character leading maximum ignorance threshold parameter name.
     */
    private static final String CHARACTER_LEADING_MAX_IGNORANCE_THRESHOLD =
            "characterLeadingMaxIgnoranceThreshold";

    /**
     * An ignorance of a character baseline shift style parameter name.
     */
    private static final String IGNORE_CHARACTER_BASELINE_SHIFT = "ignoreCharacterBaselineShift";

    /**
     * An optional character baseline shift minimum ignorance threshold parameter name.
     */
    private static final String CHARACTER_BASELINE_SHIFT_MIN_IGNORANCE_THRESHOLD =
            "characterBaselineShiftMinIgnoranceThreshold";

    /**
     * An optional character baseline shift maximum ignorance threshold parameter name.
     */
    private static final String CHARACTER_BASELINE_SHIFT_MAX_IGNORANCE_THRESHOLD =
            "characterBaselineShiftMaxIgnoranceThreshold";

    /**
     * Input label messages.
     */
    private static final String CHARACTER_KERNING_MINIMUM_IGNORANCE_THRESHOLD = "Character kerning minimum ignorance threshold";
    private static final String CHARACTER_KERNING_MAXIMUM_IGNORANCE_THRESHOLD = "Character kerning maximum ignorance threshold";
    private static final String CHARACTER_TRACKING_MINIMUM_IGNORANCE_THRESHOLD = "Character tracking minimum ignorance threshold";
    private static final String CHARACTER_TRACKING_MAXIMUM_IGNORANCE_THRESHOLD = "Character tracking maximum ignorance threshold";
    private static final String CHARACTER_LEADING_MINIMUM_IGNORANCE_THRESHOLD = "Character leading minimum ignorance threshold";
    private static final String CHARACTER_LEADING_MAXIMUM_IGNORANCE_THRESHOLD = "Character leading maximum ignorance threshold";
    private static final String CHARACTER_BASELINE_SHIFT_MINIMUM_IGNORANCE_THRESHOLD = "Character baseline shift minimum ignorance threshold";
    private static final String CHARACTER_BASELINE_SHIFT_MAXIMUM_IGNORANCE_THRESHOLD = "Character baseline shift maximum ignorance threshold";

    // initialized in net.sf.okapi.filters.idml.Parameters.reset
    private StyleIgnorances styleIgnorances;
    private FontMappings fontMappings;

    public Parameters() {
        super();
    }

    public void reset() {
        styleIgnorances = new StyleIgnorances(
                new EnumMap<>(StyleIgnorances.AttributeName.class),
                new EnumMap<>(StyleIgnorances.PropertyName.class)
        );
        fontMappings = new DefaultFontMappings(new LinkedList<>());
        super.reset();
        setMaxAttributeSize(4 * 1024 * 1024); // 4MB
        setUntagXmlStructures(true);
        setExtractNotes(false);
        setExtractMasterSpreads(true);
        setExtractHiddenLayers(false);
        setExtractHiddenPasteboardItems(false);
        setSkipDiscretionaryHyphens(false);
        setExtractBreaksInline(false);
        setIgnoreCharacterKerning(false);
        setIgnoreCharacterTracking(false);
        setIgnoreCharacterLeading(false);
        setIgnoreCharacterBaselineShift(false);
    }

    public int getMaxAttributeSize() {
        return getInteger(MAX_ATTRIBUTE_SIZE);
    }

    public void setMaxAttributeSize(int maxAttributeSize) {
        setInteger(MAX_ATTRIBUTE_SIZE, maxAttributeSize);
    }

    public boolean getUntagXmlStructures() {
        return getBoolean(UNTAG_XML_STRUCTURES);
    }

    public void setUntagXmlStructures(boolean untagXmlStructures) {
        setBoolean(UNTAG_XML_STRUCTURES, untagXmlStructures);
    }

    public boolean getExtractNotes() {
        return getBoolean(EXTRACT_NOTES);
    }

    public void setExtractNotes(boolean extractNotes) {
        setBoolean(EXTRACT_NOTES, extractNotes);
    }

    public boolean getExtractMasterSpreads() {
        return getBoolean(EXTRACT_MASTER_SPREADS);
    }

    public void setExtractMasterSpreads(boolean extractMasterSpreads) {
        setBoolean(EXTRACT_MASTER_SPREADS, extractMasterSpreads);
    }

    public boolean getExtractHiddenLayers() {
        return getBoolean(EXTRACT_HIDDEN_LAYERS);
    }

    public void setExtractHiddenLayers(boolean extractHiddenLayers) {
        setBoolean(EXTRACT_HIDDEN_LAYERS, extractHiddenLayers);
    }

    public boolean getExtractHiddenPasteboardItems() {
        return getBoolean(EXTRACT_HIDDEN_PASTEBOARD_ITEMS);
    }

    public void setExtractHiddenPasteboardItems(boolean extractHiddenPasteboardItems) {
        setBoolean(EXTRACT_HIDDEN_PASTEBOARD_ITEMS, extractHiddenPasteboardItems);
    }

    public void setSkipDiscretionaryHyphens(boolean skipDiscretionaryHyphens) {
        setBoolean(SKIP_DISCRETIONARY_HYPHENS, skipDiscretionaryHyphens);
    }

    public boolean getSkipDiscretionaryHyphens() {
        return getBoolean(SKIP_DISCRETIONARY_HYPHENS);
    }

    public void setExtractBreaksInline(boolean extractBreaksInline) {
        setBoolean(EXTRACT_BREAKS_INLINE, extractBreaksInline);
    }

    public boolean getExtractBreaksInline() {
        return getBoolean(EXTRACT_BREAKS_INLINE);
    }

    public boolean getIgnoreCharacterKerning() {
        return getBoolean(IGNORE_CHARACTER_KERNING);
    }

    public void setIgnoreCharacterKerning(boolean ignore) {
        setBoolean(IGNORE_CHARACTER_KERNING, ignore);
        if (ignore) {
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.KERNING_METHOD,
                    StyleIgnorances.Thresholds.empty()
            );
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.KERNING_VALUE,
                    StyleIgnorances.Thresholds.empty()
            );
        } else {
            this.styleIgnorances.removeAttribute(StyleIgnorances.AttributeName.KERNING_METHOD);
            this.styleIgnorances.removeAttribute(StyleIgnorances.AttributeName.KERNING_VALUE);
        }
    }

    public String getCharacterKerningMinIgnoranceThreshold() {
        return getString(CHARACTER_KERNING_MIN_IGNORANCE_THRESHOLD);
    }

    public void setCharacterKerningMinIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterKerning()) {
            return;
        }
        sanitiseAsDoubleAndSet(
                CHARACTER_KERNING_MIN_IGNORANCE_THRESHOLD,
                threshold,
                CHARACTER_KERNING_MINIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putAttribute(
                StyleIgnorances.AttributeName.KERNING_VALUE,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        threshold,
                        getCharacterKerningMaxIgnoranceThreshold()
                )
        );
    }

    public String getCharacterKerningMaxIgnoranceThreshold() {
        return getString(CHARACTER_KERNING_MAX_IGNORANCE_THRESHOLD);
    }

    public void setCharacterKerningMaxIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterKerning()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_KERNING_MAX_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_KERNING_MAXIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putAttribute(
                StyleIgnorances.AttributeName.KERNING_VALUE,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        getCharacterKerningMinIgnoranceThreshold(),
                        threshold
                )
        );
    }

    public boolean getIgnoreCharacterTracking() {
        return getBoolean(IGNORE_CHARACTER_TRACKING);
    }

    public void setIgnoreCharacterTracking(boolean ignore) {
        setBoolean(IGNORE_CHARACTER_TRACKING, ignore);
        if (ignore) {
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.TRACKING,
                    StyleIgnorances.Thresholds.empty()
            );
        } else {
            this.styleIgnorances.removeAttribute(StyleIgnorances.AttributeName.TRACKING);
        }
    }

    public String getCharacterTrackingMinIgnoranceThreshold() {
        return getString(CHARACTER_TRACKING_MIN_IGNORANCE_THRESHOLD);
    }

    public void setCharacterTrackingMinIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterTracking()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_TRACKING_MIN_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_TRACKING_MINIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putAttribute(
                StyleIgnorances.AttributeName.TRACKING,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        threshold,
                        getCharacterTrackingMaxIgnoranceThreshold()
                )
        );
    }

    public String getCharacterTrackingMaxIgnoranceThreshold() {
        return getString(CHARACTER_TRACKING_MAX_IGNORANCE_THRESHOLD);
    }

    public void setCharacterTrackingMaxIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterTracking()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_TRACKING_MAX_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_TRACKING_MAXIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putAttribute(
                StyleIgnorances.AttributeName.TRACKING,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        getCharacterTrackingMinIgnoranceThreshold(),
                        threshold
                )
        );
    }

    public boolean getIgnoreCharacterLeading() {
        return getBoolean(IGNORE_CHARACTER_LEADING);
    }

    public void setIgnoreCharacterLeading(boolean ignore) {
        setBoolean(IGNORE_CHARACTER_LEADING, ignore);
        if (ignore) {
            this.styleIgnorances.putProperty(
                    StyleIgnorances.PropertyName.LEADING,
                    StyleIgnorances.Thresholds.empty()
            );
        } else {
            this.styleIgnorances.removeProperty(StyleIgnorances.PropertyName.LEADING);
        }
    }

    public String getCharacterLeadingMinIgnoranceThreshold() {
        return getString(CHARACTER_LEADING_MIN_IGNORANCE_THRESHOLD);
    }

    public void setCharacterLeadingMinIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterLeading()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_LEADING_MIN_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_LEADING_MINIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putProperty(
                StyleIgnorances.PropertyName.LEADING,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        threshold,
                        getCharacterLeadingMaxIgnoranceThreshold()
                )
        );
    }

    public String getCharacterLeadingMaxIgnoranceThreshold() {
        return getString(CHARACTER_LEADING_MAX_IGNORANCE_THRESHOLD);
    }

    public void setCharacterLeadingMaxIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterLeading()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_LEADING_MAX_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_LEADING_MAXIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putProperty(
                StyleIgnorances.PropertyName.LEADING,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        getCharacterLeadingMinIgnoranceThreshold(),
                        threshold
                )
        );
    }

    public boolean getIgnoreCharacterBaselineShift() {
        return getBoolean(IGNORE_CHARACTER_BASELINE_SHIFT);
    }

    public void setIgnoreCharacterBaselineShift(boolean ignore) {
        setBoolean(IGNORE_CHARACTER_BASELINE_SHIFT, ignore);
        if (ignore) {
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.BASELINE_SHIFT,
                    StyleIgnorances.Thresholds.empty()
            );
        } else {
            this.styleIgnorances.removeAttribute(StyleIgnorances.AttributeName.BASELINE_SHIFT);
        }
    }

    public String getCharacterBaselineShiftMinIgnoranceThreshold() {
        return getString(CHARACTER_BASELINE_SHIFT_MIN_IGNORANCE_THRESHOLD);
    }

    public void setCharacterBaselineShiftMinIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterBaselineShift()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_BASELINE_SHIFT_MIN_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_BASELINE_SHIFT_MINIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putAttribute(
                StyleIgnorances.AttributeName.BASELINE_SHIFT,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        threshold,
                        getCharacterBaselineShiftMaxIgnoranceThreshold()
                )
        );
    }

    public String getCharacterBaselineShiftMaxIgnoranceThreshold() {
        return getString(CHARACTER_BASELINE_SHIFT_MAX_IGNORANCE_THRESHOLD);
    }

    public void setCharacterBaselineShiftMaxIgnoranceThreshold(String threshold) {
        if (!getIgnoreCharacterBaselineShift()) {
            return;
        }
        sanitiseAsDoubleAndSet(
            CHARACTER_BASELINE_SHIFT_MAX_IGNORANCE_THRESHOLD,
            threshold,
            CHARACTER_BASELINE_SHIFT_MAXIMUM_IGNORANCE_THRESHOLD + VALUE_IS_NOT_VALID
        );
        this.styleIgnorances.putAttribute(
                StyleIgnorances.AttributeName.BASELINE_SHIFT,
                new StyleIgnorances.Thresholds(
                        StyleIgnorances.Thresholds.Type.DOUBLE,
                        getCharacterBaselineShiftMinIgnoranceThreshold(),
                        threshold
                )
        );
    }

    private void sanitiseAsIntegerAndSet(final String name, final String value, final String errorMessageFormat) {
        setString(name, sanitiseAsInteger(value, errorMessageFormat));
    }

    private void sanitiseAsDoubleAndSet(final String name, final String value, final String errorMessageFormat) {
        setString(name, sanitiseAsDouble(value, errorMessageFormat));
    }

    private String sanitiseAsInteger(final String value, final String errorMessageFormat) {
        return sanitiseAs(Integer.class, value, errorMessageFormat);
    }

    private String sanitiseAsDouble(final String value, final String errorMessageFormat) {
        return sanitiseAs(Double.class, value, errorMessageFormat);
    }

    private String sanitiseAs(final Class type, final String value, final String errorMessageFormat) {
        if (EMPTY.equals(value.trim())) {
            return EMPTY;
        }

        final List<Sanitiser.Filter<String>> filters = new ArrayList<>(2);
        filters.add(new StringSanitiser.TrimmingFilter());

        switch (type.getSimpleName()) {
            case "Integer":
                filters.add(new StringSanitiser.IntegerParsingFilter(errorMessageFormat));
                break;
            case "Double":
                filters.add(new StringSanitiser.DoubleParsingFilter(errorMessageFormat));
                break;
            default:
                // intentionally left blank
        }

        return new StringSanitiser(filters).sanitise(value);
    }

    @Override
    public ParametersDescription getParametersDescription() {
        ParametersDescription desc = new ParametersDescription(this);

        desc.add(UNTAG_XML_STRUCTURES, "Untag XML structures", null);
        desc.add(EXTRACT_NOTES, "Extract notes", null);
        desc.add(EXTRACT_MASTER_SPREADS, "Extract master spreads", null);
        desc.add(EXTRACT_HIDDEN_LAYERS, "Extract hidden layers", null);
        desc.add(EXTRACT_HIDDEN_PASTEBOARD_ITEMS, "Extract hidden pasteboard items", null);
        desc.add(SKIP_DISCRETIONARY_HYPHENS, "Skip discretionary hyphens", null);
        desc.add(EXTRACT_BREAKS_INLINE, "Extract breaks inline", null);

        desc.add(IGNORE_CHARACTER_KERNING, "Ignore character kerning", null);
        desc.add(CHARACTER_KERNING_MIN_IGNORANCE_THRESHOLD, CHARACTER_KERNING_MINIMUM_IGNORANCE_THRESHOLD, null);
        desc.add(CHARACTER_KERNING_MAX_IGNORANCE_THRESHOLD, CHARACTER_KERNING_MAXIMUM_IGNORANCE_THRESHOLD, null);

        desc.add(IGNORE_CHARACTER_TRACKING, "Ignore character tracking", null);
        desc.add(CHARACTER_TRACKING_MIN_IGNORANCE_THRESHOLD, CHARACTER_TRACKING_MINIMUM_IGNORANCE_THRESHOLD, null);
        desc.add(CHARACTER_TRACKING_MAX_IGNORANCE_THRESHOLD, CHARACTER_TRACKING_MAXIMUM_IGNORANCE_THRESHOLD, null);

        desc.add(IGNORE_CHARACTER_LEADING, "Ignore character leading", null);
        desc.add(CHARACTER_LEADING_MIN_IGNORANCE_THRESHOLD, CHARACTER_LEADING_MINIMUM_IGNORANCE_THRESHOLD, null);
        desc.add(CHARACTER_LEADING_MAX_IGNORANCE_THRESHOLD, CHARACTER_LEADING_MAXIMUM_IGNORANCE_THRESHOLD, null);

        desc.add(IGNORE_CHARACTER_BASELINE_SHIFT, "Ignore character baseline shift", null);
        desc.add(CHARACTER_BASELINE_SHIFT_MIN_IGNORANCE_THRESHOLD, CHARACTER_BASELINE_SHIFT_MINIMUM_IGNORANCE_THRESHOLD, null);
        desc.add(CHARACTER_BASELINE_SHIFT_MAX_IGNORANCE_THRESHOLD, CHARACTER_BASELINE_SHIFT_MAXIMUM_IGNORANCE_THRESHOLD, null);

        return desc;
    }

    @Override
    public EditorDescription createEditorDescription(ParametersDescription paramsDesc) {
        EditorDescription desc = new EditorDescription("IDML Filter", true, false);

        desc.addCheckboxPart(paramsDesc.get(UNTAG_XML_STRUCTURES));
        desc.addCheckboxPart(paramsDesc.get(EXTRACT_NOTES));
        desc.addCheckboxPart(paramsDesc.get(EXTRACT_MASTER_SPREADS));
        desc.addCheckboxPart(paramsDesc.get(EXTRACT_HIDDEN_LAYERS));
        desc.addCheckboxPart(paramsDesc.get(EXTRACT_HIDDEN_PASTEBOARD_ITEMS));
        desc.addCheckboxPart(paramsDesc.get(SKIP_DISCRETIONARY_HYPHENS));
        desc.addCheckboxPart(paramsDesc.get(EXTRACT_BREAKS_INLINE));

        desc.addSeparatorPart();

        desc.addTextLabelPart("Ignored Styles");
        desc.addCheckboxPart(paramsDesc.get(IGNORE_CHARACTER_KERNING));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_KERNING_MIN_IGNORANCE_THRESHOLD));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_KERNING_MAX_IGNORANCE_THRESHOLD));

        desc.addCheckboxPart(paramsDesc.get(IGNORE_CHARACTER_TRACKING));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_TRACKING_MIN_IGNORANCE_THRESHOLD));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_TRACKING_MAX_IGNORANCE_THRESHOLD));

        desc.addCheckboxPart(paramsDesc.get(IGNORE_CHARACTER_LEADING));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_LEADING_MIN_IGNORANCE_THRESHOLD));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_LEADING_MAX_IGNORANCE_THRESHOLD));

        desc.addCheckboxPart(paramsDesc.get(IGNORE_CHARACTER_BASELINE_SHIFT));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_BASELINE_SHIFT_MIN_IGNORANCE_THRESHOLD));
        desc.addOptionalTextInputPart(paramsDesc.get(CHARACTER_BASELINE_SHIFT_MAX_IGNORANCE_THRESHOLD));

        return desc;
    }

    @Override
    public void fromString(String data) {
        super.fromString(data);
        this.fontMappings.addFrom(new ParametersStringFontMappingsInput(buffer));
        loadStyleIgnorances();
    }

    @Override
    public String toString() {
        buffer.fromParametersString(
            this.fontMappings.writtenTo(new ParametersStringFontMappingsOutput()),
            false
        );
        return super.toString();
    }

    FontMappings fontMappings() {
        return this.fontMappings;
    }

    void fontMappings(final FontMappings fontMappings) {
        this.fontMappings = fontMappings;
    }

    StyleIgnorances styleIgnorances() {
        return this.styleIgnorances;
    }

    private void loadStyleIgnorances() {
        if (getIgnoreCharacterKerning()) {
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.KERNING_METHOD,
                    StyleIgnorances.Thresholds.empty()
            );
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.KERNING_VALUE,
                    new StyleIgnorances.Thresholds(
                            StyleIgnorances.Thresholds.Type.DOUBLE,
                            getCharacterKerningMinIgnoranceThreshold(),
                            getCharacterKerningMaxIgnoranceThreshold()
                    )
            );
        }
        if (getIgnoreCharacterTracking()) {
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.TRACKING,
                    new StyleIgnorances.Thresholds(
                            StyleIgnorances.Thresholds.Type.DOUBLE,
                            getCharacterTrackingMinIgnoranceThreshold(),
                            getCharacterTrackingMaxIgnoranceThreshold()
                    )
            );
        }
        if (getIgnoreCharacterLeading()) {
            this.styleIgnorances.putProperty(
                    StyleIgnorances.PropertyName.LEADING,
                    new StyleIgnorances.Thresholds(
                            StyleIgnorances.Thresholds.Type.DOUBLE,
                            getCharacterLeadingMinIgnoranceThreshold(),
                            getCharacterLeadingMaxIgnoranceThreshold()
                    )
            );
        }
        if (getIgnoreCharacterBaselineShift()) {
            this.styleIgnorances.putAttribute(
                    StyleIgnorances.AttributeName.BASELINE_SHIFT,
                    new StyleIgnorances.Thresholds(
                            StyleIgnorances.Thresholds.Type.DOUBLE,
                            getCharacterBaselineShiftMinIgnoranceThreshold(),
                            getCharacterBaselineShiftMaxIgnoranceThreshold()
                    )
            );
        }
    }
}
