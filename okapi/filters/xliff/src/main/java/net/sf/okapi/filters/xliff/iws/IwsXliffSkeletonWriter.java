package net.sf.okapi.filters.xliff.iws;

import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.filters.xliff.Parameters;
import net.sf.okapi.filters.xliff.XLIFFSkeletonWriter;

/**
 * Skeleton write used to update the translation_status of Idiom WorldServer XLIFF files.
 */
public class IwsXliffSkeletonWriter extends XLIFFSkeletonWriter
{

    public static final String IWS_TRANS_STATUS_MARKER = "[@#$IWSTRANSSTATUS$#@]";

    public static final String IWS_TRANS_TYPE_MARKER = "[@#$IWSTRANSTYPE$#@]";

    public IwsXliffSkeletonWriter(Parameters params) {
        super(params);
    }

    /**
     * Update the Idiom WorldServer Metadata if the content has been translated.
     */
    @Override
    public String processTextUnit(ITextUnit resource) {
        if (!resource.getTargetLocales().isEmpty()) {
            LocaleId tl = resource.getTargetLocales().iterator().next();
            if (resource.hasTarget(tl) && resource.isTranslatable()) {
                resource.setSkeleton(updateIwsTransStatus(resource.getTarget(tl), (GenericSkeleton)resource.getSkeleton()));
                resource.setSkeleton(updateIwsTransType(resource.getTarget(tl), (GenericSkeleton)resource.getSkeleton()));
            }
        }
        return super.processTextUnit(resource);
    }

    /**
     * Update the IWS Translation Status.
     * @param target the <code>target</code> element
     * @param skeleton the original <code>ISkeleton</code> related to the <code>trans-unit</code>
     * @return the updated <code>ISkeleton</code>
     */
    private ISkeleton updateIwsTransStatus(TextContainer target, GenericSkeleton skeleton) {
        for (GenericSkeletonPart p : skeleton.getParts()) {
            Property s = target.getProperty(new IwsStatusAttribute(IwsProperty.TRANSLATION_STATUS).getAttributeNamePrefixed());
            String d = p.getData().toString();
            if (s != null) {
                d = d.replace(IWS_TRANS_STATUS_MARKER, !Util.isEmpty(s.getValue()) ? s.getValue() : getParams().getIwsTransStatusValue());
            } else {
                d = d.replace(IWS_TRANS_STATUS_MARKER, getParams().getIwsTransStatusValue());
            }
            // Remove TM origin so any change is picked up by IWS
            if (getParams().isIwsRemoveTmOrigin()) {
                d = d.replaceFirst("(tm_origin\\=)(\\\")([^\\\"]+)(\\\")", "");
            }
            p.setData(d);
        }
        return skeleton;
    }

    /**
     * Update the IWS Translation Type.
     * @param target the <code>target</code> element
     * @param skeleton the original <code>ISkeleton</code> related to the <code>trans-unit</code>
     * @return the updated <code>ISkeleton</code>
     */
    private ISkeleton updateIwsTransType(TextContainer target, GenericSkeleton skeleton) {
        for (GenericSkeletonPart p : skeleton.getParts()) {
            Property s = target.getProperty(new IwsStatusAttribute(IwsProperty.TRANSLATION_TYPE).getAttributeNamePrefixed());
            String d = p.getData().toString();
            if (s != null) {
                d = d.replace(IWS_TRANS_TYPE_MARKER, !Util.isEmpty(s.getValue()) ? s.getValue() : getParams().getIwsTransTypeValue());
            } else {
                d = d.replace(IWS_TRANS_TYPE_MARKER, getParams().getIwsTransTypeValue());
            }
            p.setData(d);
        }
        return skeleton;
    }
}

