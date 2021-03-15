package net.sf.okapi.filters.markdown;

import net.sf.okapi.common.annotation.IAnnotation;

/**
 * ListItem and BlockQuote, which can be nested, and IndentedCode block (cannot be nested)
 * are marked by prefixes on each line, and they can be combined.
 * This annotation keeps track of the prefix that is
 * currently in effect.
 * Here are some examples:
 * <ul>
 * <li>"Normal" No block quote or indented code block; linePrefix=""</li>
 * <li>"> foo" Block quote; "> "</li>
 * <li>"> > foo" Nested block quote, "> > "</li>
 * <li>"> >foo" Nested block quote, and a space is omitted; "> > " (normalized)</li>
 * <li>"    public void foo()" Indented code block; "    " </li>
 * <li>">     public void foo();" Indented code block inside a block quote; ">     " (1+4 spaces, the first space being part of the block quote marker</li>
 * <li>"> 1. item 1", List item that is block quoted; ">    " (3 spaces added in preparation to handle sub list items)</li>
 * </ul>
 */
public class MarkdownLinePrefixAnnotation implements IAnnotation {
    private final String linePrefix;
    public MarkdownLinePrefixAnnotation(String linePrefix) {
        this.linePrefix = linePrefix;
    }
    public String getLinePrefix() {
        return linePrefix;
    }
    public String toString() { return String.format("%s.linePrefix='%s'", this.getClass().getSimpleName(), linePrefix);}
}
