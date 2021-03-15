package net.sf.okapi.filters.idml;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ParametersTest {

    @Test
    public void initialisesDefaultParameters() {
        Assertions.assertThat(new Parameters().toString())
                .isEqualTo("#v1\n" +
                        "maxAttributeSize.i=4194304\n" +
                        "untagXmlStructures.b=true\n" +
                        "extractNotes.b=false\n" +
                        "extractMasterSpreads.b=true\n" +
                        "extractHiddenLayers.b=false\n" +
                        "extractHiddenPasteboardItems.b=false\n" +
                        "skipDiscretionaryHyphens.b=false\n" +
                        "extractBreaksInline.b=false\n" +
                        "ignoreCharacterKerning.b=false\n" +
                        "ignoreCharacterTracking.b=false\n" +
                        "ignoreCharacterLeading.b=false\n" +
                        "ignoreCharacterBaselineShift.b=false"
                );
    }

    @Test
    public void initialisesStyleIgnorances() {
        final Parameters parameters = new Parameters();
        parameters.fromString(
                "#v1\n" +
                        "maxAttributeSize.i=4194304\n" +
                        "untagXmlStructures.b=true\n" +
                        "extractNotes.b=false\n" +
                        "extractMasterSpreads.b=true\n" +
                        "extractHiddenLayers.b=false\n" +
                        "extractHiddenPasteboardItems.b=false\n" +
                        "skipDiscretionaryHyphens.b=false\n" +
                        "extractBreaksInline.b=false\n" +
                        "ignoreCharacterKerning.b=true\n" +
                        "ignoreCharacterTracking.b=true\n" +
                        "ignoreCharacterLeading.b=true\n" +
                        "ignoreCharacterBaselineShift.b=false\n" +
                        "characterKerningMinIgnoranceThreshold=-15.15\n" +
                        "characterKerningMaxIgnoranceThreshold=15\n" +
                        "characterTrackingMinIgnoranceThreshold=\n" +
                        "characterTrackingMaxIgnoranceThreshold=25.25\n" +
                        "characterLeadingMinIgnoranceThreshold=4.2\n" +
//                        "characterLeadingMaxIgnoranceThreshold=4.2\n" + // intentionally commented out
                        "characterBaselineShiftMinIgnoranceThreshold=\n" +
                        "characterBaselineShiftMaxIgnoranceThreshold=\n"
        );
        Assertions.assertThat(parameters.styleIgnorances()
                .isAttributeNamePresent(Namespaces.getDefaultNamespace().getQName("KerningMethod"))).isTrue();
        Assertions.assertThat(parameters.styleIgnorances()
                .isAttributeNamePresent(Namespaces.getDefaultNamespace().getQName("KerningValue"))).isTrue();
        Assertions.assertThat(parameters.styleIgnorances()
                .isAttributeNamePresent(Namespaces.getDefaultNamespace().getQName("Tracking"))).isTrue();
        Assertions.assertThat(parameters.styleIgnorances()
                .isPropertyNamePresent(Namespaces.getDefaultNamespace().getQName("Leading"))).isTrue();
        Assertions.assertThat(parameters.styleIgnorances()
                .isAttributeNamePresent(Namespaces.getDefaultNamespace().getQName("BaselineShift"))).isFalse();

        final StyleIgnorances.Thresholds kerningMethodThresholds =
                parameters.styleIgnorances().thresholds(Namespaces.getDefaultNamespace().getQName("KerningMethod"));
        Assertions.assertThat(kerningMethodThresholds.areEmpty()).isTrue();

        final StyleIgnorances.Thresholds kerningValueThresholds =
                parameters.styleIgnorances().thresholds(Namespaces.getDefaultNamespace().getQName("KerningValue"));
        Assertions.assertThat(kerningValueThresholds.type()).isEqualTo(StyleIgnorances.Thresholds.Type.DOUBLE);
        Assertions.assertThat(kerningValueThresholds.min()).isEqualTo("-15.15");
        Assertions.assertThat(kerningValueThresholds.max()).isEqualTo("15");

        final StyleIgnorances.Thresholds trackingThresholds =
                parameters.styleIgnorances().thresholds(Namespaces.getDefaultNamespace().getQName("Tracking"));
        Assertions.assertThat(trackingThresholds.type()).isEqualTo(StyleIgnorances.Thresholds.Type.DOUBLE);
        Assertions.assertThat(trackingThresholds.min()).isEqualTo("");
        Assertions.assertThat(trackingThresholds.max()).isEqualTo("25.25");

        final StyleIgnorances.Thresholds leadingThresholds =
                parameters.styleIgnorances().thresholds(Namespaces.getDefaultNamespace().getQName("Leading"));
        Assertions.assertThat(leadingThresholds.type()).isEqualTo(StyleIgnorances.Thresholds.Type.DOUBLE);
        Assertions.assertThat(leadingThresholds.min()).isEqualTo("4.2");
        Assertions.assertThat(leadingThresholds.max()).isEqualTo("");

        final StyleIgnorances.Thresholds baselineShiftThresholds =
                parameters.styleIgnorances().thresholds(Namespaces.getDefaultNamespace().getQName("BaselineShift"));
        Assertions.assertThat(baselineShiftThresholds.areEmpty()).isTrue();
    }

    @Test
    public void setsCharacterKerningMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterKerning(true);
        parameters.setCharacterKerningMinIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterKerningMinIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterKerningMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterKerning(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterKerningMinIgnoranceThreshold(" e "))
                .withMessage("Character kerning minimum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterKerningMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterKerning(true);
        parameters.setCharacterKerningMaxIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterKerningMaxIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterKerningMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterKerning(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterKerningMaxIgnoranceThreshold(" e "))
                .withMessage("Character kerning maximum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterTrackingMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterTracking(true);
        parameters.setCharacterTrackingMinIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterTrackingMinIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterTrackingMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterTracking(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterTrackingMinIgnoranceThreshold(" e "))
                .withMessage("Character tracking minimum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterTrackingMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterTracking(true);
        parameters.setCharacterTrackingMaxIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterTrackingMaxIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterTrackingMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterTracking(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterTrackingMaxIgnoranceThreshold(" e "))
                .withMessage("Character tracking maximum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterLeadingMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterLeading(true);
        parameters.setCharacterLeadingMinIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterLeadingMinIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterLeadingMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterLeading(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterLeadingMinIgnoranceThreshold(" e "))
                .withMessage("Character leading minimum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterLeadingMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterLeading(true);
        parameters.setCharacterLeadingMaxIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterLeadingMaxIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterLeadingMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterLeading(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterLeadingMaxIgnoranceThreshold(" e "))
                .withMessage("Character leading maximum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterBaselineShiftMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterBaselineShift(true);
        parameters.setCharacterBaselineShiftMinIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterBaselineShiftMinIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterBaselineShiftMinIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterBaselineShift(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterBaselineShiftMinIgnoranceThreshold(" e "))
                .withMessage("Character baseline shift minimum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void setsCharacterBaselineShiftMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterBaselineShift(true);
        parameters.setCharacterBaselineShiftMaxIgnoranceThreshold("1");
        Assertions.assertThat(parameters.getCharacterBaselineShiftMaxIgnoranceThreshold())
                .isEqualTo("1.0");
    }

    @Test
    public void failsToSetCharacterBaselineShiftMaxIgnoranceThreshold() {
        final Parameters parameters = new Parameters();
        parameters.setIgnoreCharacterBaselineShift(true);
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> parameters.setCharacterBaselineShiftMaxIgnoranceThreshold(" e "))
                .withMessage("Character baseline shift maximum ignorance threshold \"e\" value is not valid")
                .withCause(new NumberFormatException("For input string: \"e\""));
    }

    @Test
    public void fontMappingsAreInitialised() {
        final Parameters parameters = new Parameters();
        parameters.fromString(
            "#v1\n" +
            "maxAttributeSize.i=4194304\n" +
            "untagXmlStructures.b=true\n" +
            "extractNotes.b=false\n" +
            "extractMasterSpreads.b=true\n" +
            "extractHiddenLayers.b=false\n" +
            "extractHiddenPasteboardItems.b=false\n" +
            "skipDiscretionaryHyphens.b=false\n" +
            "extractBreaksInline.b=false\n" +
            "ignoreCharacterKerning.b=true\n" +
            "ignoreCharacterTracking.b=true\n" +
            "ignoreCharacterLeading.b=true\n" +
            "ignoreCharacterBaselineShift.b=false\n" +
            "characterKerningMinIgnoranceThreshold=-15.15\n" +
            "characterKerningMaxIgnoranceThreshold=15\n" +
            "characterTrackingMinIgnoranceThreshold=\n" +
            "characterTrackingMaxIgnoranceThreshold=25.25\n" +
            "characterLeadingMinIgnoranceThreshold=4.2\n" +
            "characterBaselineShiftMinIgnoranceThreshold=\n" +
            "characterBaselineShiftMaxIgnoranceThreshold=\n" +
            "fontMappings.0.sourceLocalePattern=en\n" +
            "fontMappings.0.targetLocalePattern=ru\n" +
            "fontMappings.0.sourceFontPattern=Times.*\n" +
            "fontMappings.0.targetFont:Arial\n" +
            "fontMappings.1.sourceLocalePattern=en\n" +
            "fontMappings.1.targetLocalePattern=ru\n" +
            "fontMappings.1.sourceFontPattern=Arial\n" +
            "fontMappings.1.targetFont:Times New Roman\n" +
            "fontMappings.number.i=2\n"
        );
        Assertions.assertThat(parameters.fontMappings()).isNotNull();
    }
}
