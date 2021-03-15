package net.sf.okapi.filters.markdown;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

public class MarkdownFilterWriter extends GenericFilterWriter {

    public MarkdownFilterWriter() {
    }

    public MarkdownFilterWriter (ISkeletonWriter skelWriter,
                                EncoderManager encoderManager)
    {
        super(skelWriter, encoderManager);
    }

    protected void processStartDocument(LocaleId outputLocale,
                                        String outputEncoding,
                                        StartDocument resource) throws IOException
    {
        // Let GenericFIlterWriter create the writer.
        super.processStartDocument(outputLocale, outputEncoding, resource);

        writer.flush(); // Just in case
        writer = new LineTrimingWriter(writer); // Replace it with the line-buffered version.
    }

    /* An OutputStreamWriter adaptor that buffers the characters until the newline is seen,
     * and send the buffered line to the original OutputStreamWriter.
     * Spaces at the end of the line, if there is only one, will be removed. This is so that a line
     * "> " will become ">". If a line is entirely consists of spaces, all the spaces will be removed.
     * Otherwise we keep the line intact because it is likely the spaces are intentional
     * such as to indicate a hard line break.
     *
     * LineTrimingWriter is an OutputStreamWriter, not simply a Writer, just because GenericFilterWriter
     * exposes OutputStreamWriter.
     */
    private static class LineTrimingWriter extends OutputStreamWriter {

        OutputStreamWriter baseWriter;
        StringBuilder sb = new StringBuilder();

        public LineTrimingWriter(OutputStreamWriter baseWriter) throws UnsupportedEncodingException {
            super(new OutputStream() { // Give it a dummy stream. We Just delegate.
                    @Override
                    public void write(int b) throws IOException {
                        // Do nothing
                    }
                }, baseWriter.getEncoding());
            this.baseWriter = baseWriter;
        }

        @Override
        public void write(int c) throws IOException {
            if (c=='\n') {
                trimNonEssentialTrailingSpaces();
                sb.append('\n');
                baseWriter.write(sb.toString());
                sb.setLength(0);
            } else {
                sb.append((char)c);
            }
        }

        // Remove the trailing spaces, if only one,
        // or entire line is made of spaces.
        private void trimNonEssentialTrailingSpaces() {
            if (sb.length()==0) return;

            // Is it made of all spaces?
            int i = sb.length() - 1;
            while (sb.charAt(i)==' ') {
                i--;
                if (i==-1) { // Yes. Empty it.
                    sb.setLength(0);
                    return;
                }
            }

            // If it's just one space, remove it.
            if (i == sb.length() - 2) {
                sb.setLength(sb.length() - 1);
            }
        }

        private int findLastNonSpaceCharPos(StringBuilder sb) {
            if (sb.length()==0) return -1;
            int i = sb.length() - 1;
            while (sb.charAt(i)==' ') {
                i--;
                if (i==-1) break;
            }
            return i;
        }

        @Override
        public void write(char cbuf[], int off, int len) throws IOException {
            int i = off;
            while (len-- > 0) {
                write(cbuf[i++]);
            }
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            write(str.toCharArray(), 0, str.length());
        }

        @Override
        public void flush() throws IOException {
            if (sb.length() > 0) {
                trimNonEssentialTrailingSpaces();
                baseWriter.write(sb.toString());
            }
            baseWriter.flush();
        }

        @Override
        public void close() throws IOException {
            flush();
            baseWriter.close();
            super.close();
        }
    }
}
