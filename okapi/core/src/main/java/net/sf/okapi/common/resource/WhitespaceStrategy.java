package net.sf.okapi.common.resource;

public enum WhitespaceStrategy {
    /**
     * Inherit from {@link ITextUnit}
     */
    INHERIT,
    /**
     * Preserve all whitespace
     */
    PRESERVE,
    /**
     * Follow normal format whitespace normalization rules
     */
    NORMALIZE
}
