package net.sf.okapi.filters.xliff.iws;

/**
 * Utility class for IWS segment-metadata attributes.
 */
public class IwsSegmentMetadataAttribute extends IwsMetadataAttribute
{
    public static String ELEMENT_NAME = "segment-metadata";

    public IwsSegmentMetadataAttribute(String name)
    {
        this.name = name;
    }

    public String getPrefix()
    {
        return IwsProperty.IWS_NAMESPACE.concat(":").concat(ELEMENT_NAME).concat(":");
    }
}
