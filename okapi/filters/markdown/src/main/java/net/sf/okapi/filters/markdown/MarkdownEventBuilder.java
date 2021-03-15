package net.sf.okapi.filters.markdown;

import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.filters.EventBuilder;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.InlineCodeFinder;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;

public class MarkdownEventBuilder extends EventBuilder {
    private InlineCodeFinder codeFinder;

    public MarkdownEventBuilder(String rootId, IFilter subFilter) {
        super(rootId, subFilter);
        codeFinder = null;
        setMimeType(MimeTypeMapper.MARKDOWN_MIME_TYPE);
    }

    @Override
    protected ITextUnit postProcessTextUnit(ITextUnit textUnit) {
        TextFragment text = textUnit.getSource().getFirstContent();
        if (codeFinder != null) {
            codeFinder.process(text);
        }
        return textUnit;
    }

    public void setCodeFinder(InlineCodeFinder codeFinder) {
        this.codeFinder = codeFinder;
    }
}
