package net.sf.okapi.filters.markdown;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class MarkdownSkeletonWriterTest {

    @Test
    public void testProcessTextUnit() {
        StartDocument sd = new StartDocument("sd");
        sd.setEncoding("UTF-8", false);
        sd.setName("docName");
        sd.setLineBreak("\n");
        sd.setLocale(LocaleId.ENGLISH);
        sd.setMultilingual(false);

        TextUnit tu1 = new TextUnit("tu1", "Hello, World!\nHola, Mondo!\n");
        tu1.setAnnotation(new MarkdownLinePrefixAnnotation(("> > ")));

        ISkeletonWriter sw = new MarkdownSkeletonWriter();
        sw.processStartDocument(LocaleId.FRENCH, "UTF-8", null, new EncoderManager(), sd);
        assertEquals("> > Hello, World!\n> > Hola, Mondo!\n", sw.processTextUnit(tu1));

        // tu2 or tu3 doesn't end with a newline. tu3 shouldn't be added a prefix.
        TextUnit tu2 = new TextUnit("tu2", "Hello, World");
        tu2.setAnnotation(new MarkdownLinePrefixAnnotation(("> > ")));
        assertEquals("> > Hello, World", sw.processTextUnit(tu2));

        TextUnit tu3 = new TextUnit("tu3", "!\nHola, Mondo!");
        tu3.setAnnotation(new MarkdownLinePrefixAnnotation(("> > ")));
        assertEquals("!\n> > Hola, Mondo!", sw.processTextUnit(tu3));
    }

    @Test
    public void testAppendLinePrefix() {
        MarkdownSkeletonWriter mdsw = new MarkdownSkeletonWriter();
        mdsw.linePrefix = "> ";
        assertEquals("> 1. ", mdsw.appendLinePrefix("1. "));
        assertEquals("Item 1", mdsw.appendLinePrefix("Item 1"));
        assertEquals("\n", mdsw.appendLinePrefix("\n"));
        assertEquals("> \n", mdsw.appendLinePrefix("\n"));

        mdsw.linePrefix = "> > ";
        assertEquals("> > Move the mouse.", mdsw.appendLinePrefix("Move the mouse."));
        assertEquals("\n", mdsw.appendLinePrefix("\n"));
        assertEquals("> > Click it and ", mdsw.appendLinePrefix("Click it and "));
        assertEquals("wait for a few seconds.", mdsw.appendLinePrefix("wait for a few seconds."));

    }
}