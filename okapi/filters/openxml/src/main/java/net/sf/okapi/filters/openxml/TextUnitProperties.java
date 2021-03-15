package net.sf.okapi.filters.openxml;

import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;

/**
 * Provides text unit properties extended operations.
 */
final class TextUnitProperties {

    /**
     * Adds an integer value to a text unit property by name.
     *
     * If there is no property, a new property is created with the provided value.
     *
     * @param textUnit The text unit
     * @param name     The property name
     * @param value    The value
     */
    static void addInteger(final ITextUnit textUnit, final String name, final int value) {
        final Property property = textUnit.getProperty(name);
        if (null == property) {
            newInteger(textUnit, name, value);
        } else {
            newInteger(textUnit, name, Integer.valueOf(property.getValue()) + value);
        }
    }

    /**
     * Creates a new text unit property with an integer value.
     *
     * @param textUnit The text unit
     * @param name     The property name
     * @param value    The value
     */
    static void newInteger(final ITextUnit textUnit, final String name, final int value) {
        textUnit.setProperty(new Property(name, String.valueOf(value)));
    }

    /**
     * Obtains a text unit property value as integer.
     *
     * @param textUnit The text unit
     * @param name     The property name
     * @return The integer value of the property or 0
     *         if the property does not exist
     */
    static int integer(final ITextUnit textUnit, final String name) {
        final Property property = textUnit.getProperty(name);
        return null == property
            ? 0
            : Integer.valueOf(property.getValue());
    }
}