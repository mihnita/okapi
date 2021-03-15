package net.sf.okapi.filters.xliff.iws;

/**
 * Base class to support attribute utility classes.
 */
abstract class IwsMetadataAttribute
{
    /**
     * The attribute name.
     */
    protected String name;

    /**
     * @return the attribute name.
     */
    public String getAttributeName()
    {
        return name;
    }

    /**
     * @return a prefix to avoid conflicts between other attributes that could have the same name.
     */
    public String getPrefix() {
        return IwsProperty.IWS_NAMESPACE.concat(":");
    }

    /**
     * @return a prefixed attribute name to avoid conflicts between other attributes that could have the same name.
     */
    public String getAttributeNamePrefixed() {
        return getPrefix().concat(getAttributeName());
    }
}
