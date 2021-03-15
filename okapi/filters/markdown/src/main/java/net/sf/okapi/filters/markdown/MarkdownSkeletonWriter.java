package net.sf.okapi.filters.markdown;

import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.EndSubfilter;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.IWithAnnotations;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownSkeletonWriter extends GenericSkeletonWriter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final Pattern linePat = Pattern.compile("[^\n]*\n");
    /*private*/String linePrefix = "";
    boolean lineCompleted = true; // True iff the last call to processTextUnit or processDocumentPart ended with a newline.

    @Override
    public String processTextUnit(ITextUnit resource) {
        //TextUnitUtil.unsegmentTU(resource);
        obtainLinePrefix((resource));
        return appendLinePrefix(super.processTextUnit(resource));
    }

    @Override
    public String processDocumentPart (DocumentPart resource) {
        obtainLinePrefix((resource));
        return appendLinePrefix(super.processDocumentPart(resource));
    }

    @Override
    public String processEndSubfilter (EndSubfilter resource) {
        return appendLinePrefix(super.processEndSubfilter(resource));
    }

    // Sets linePrefix according to the resouce
    private void obtainLinePrefix(IWithAnnotations resource) {
        MarkdownLinePrefixAnnotation lpa = resource.getAnnotation(MarkdownLinePrefixAnnotation.class);
        if (lpa != null) {
            linePrefix = lpa.getLinePrefix();
        }
    }

    /*     Insert a line prefix to each line in the content.
     * The logic is complicated because of the following factor:
     * - We want to insert the line prefix when the first character after a newline is written, not when the newline is written.
     *   This is because the entire file might end with a newline.
     * - We want to buffer the physical line content so that we can trim the spaces in case the line
     *  would become "> ", "    " (following the list item), etc.
     *
     * To achieve this goal, we need to buffer a line content and flush if when the newline is seen.
     * At the time of flushing, we first adds the line prefix, add the line content except for the newline,
     * trim it and add a newline.
     */
     /*private*/String appendLinePrefix(String content) {
        if (content.isEmpty()) return "";
        Matcher m = linePat.matcher(content);
        StringBuilder sb = new StringBuilder();
        int lastHitEnd = 0;
        boolean hadNewline = false; // Flag to tell there was at least one newline.
        if (lineCompleted) {
            sb.append(linePrefix);
        }
        while (m.find()) {
            if (hadNewline) { // Second time or later in the loop.
                sb.append(linePrefix);
            }
            sb.append(m.group());
            lastHitEnd = m.end();
            hadNewline = true;
        }
        lineCompleted = lastHitEnd == content.length();
        if (!lineCompleted) { // If there's something after the last hit (or no hit), add the rest.
            if (hadNewline) {
                sb.append(linePrefix);
            }
            sb.append(content.substring(lastHitEnd));
        }
        return sb.toString();
    }
}
