package net.sf.okapi.filters.openxml;

import org.assertj.core.api.iterable.Extractor;

import net.sf.okapi.common.resource.ITextUnit;

public class OpenXMLTestHelpers {
    public static Extractor<ITextUnit, Object> textUnitSourceExtractor() {
        return input -> input.getSource().toString();
    }
}
