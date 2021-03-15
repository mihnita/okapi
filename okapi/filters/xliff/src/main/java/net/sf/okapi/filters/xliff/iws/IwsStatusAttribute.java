package net.sf.okapi.filters.xliff.iws;

/**
 * Utility class for IWS status attributes.
 */
public class IwsStatusAttribute extends IwsMetadataAttribute
{
    public static String ELEMENT_NAME = "status";

    public IwsStatusAttribute(String name)
    {
        this.name = name;
    }

    public String getPrefix()
    {
        return IwsProperty.IWS_NAMESPACE.concat(":").concat(ELEMENT_NAME).concat(":");
    }
}
