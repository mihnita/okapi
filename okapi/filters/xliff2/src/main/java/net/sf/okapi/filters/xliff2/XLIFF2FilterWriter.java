/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
============================================================================*/

package net.sf.okapi.filters.xliff2;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiMergeException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;
import net.sf.okapi.lib.xliff2.reader.EventType;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

/**
 * The XLIFF2FilterWriter is essentially wrapper for the {@link XLIFFWriter}, similar to the {@link XLIFF2Filter}.
 * <p>
 * When developing for this, no manual building of XML Strings should be handled by this. Instead, that should all be
 * handled by the {@link XLIFFWriter}.
 */
public class XLIFF2FilterWriter implements IFilterWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    private Output outputChoice;
    private File outputFile;
    private OutputStreamWriter writer;
    private Charset encoding;
    private LocaleId sourceLocale;
    private LocaleId targetLocale;

    private XLIFFWriter xliffToolkitWriter;
    private OkpToX2Converter converter;

	private EncoderManager encoderManager;

    // Easier to keep track of where the output is going than using a bunch of null checks
    enum Output {NONE, PATH, STREAM}

    public XLIFF2FilterWriter() {
        converter = new OkpToX2Converter();
        outputChoice = Output.NONE;
        outputFile = null;
        encoding = StandardCharsets.UTF_8;
        xliffToolkitWriter = null;
    }

    @Override
    public String getName() {
        return "XLIFF2FilterWriter";
    }

    @Override
    public void setOptions(LocaleId locale, String defaultEncoding) {
        targetLocale = locale;
        encoding = Charset.forName(defaultEncoding);
    }

    @Override
    public void setOutput(String path) {
        close();
        xliffToolkitWriter = new XLIFFWriter();
        outputFile = new File(path);
        outputChoice = Output.PATH;
    }

    @Override
    public void setOutput(OutputStream output) {
        close();
        xliffToolkitWriter = new XLIFFWriter();
        writer = new OutputStreamWriter(output, encoding);
        outputChoice = Output.STREAM;
    }

    @Override
    public Event handleEvent(Event event) {
        if (outputChoice == Output.NONE) {
            throw new OkapiMergeException("Output has not been set. Use setOutput().");
        }

        final List<net.sf.okapi.lib.xliff2.reader.Event> xliff2Events = converter.handleEvent(event, this);

        for (net.sf.okapi.lib.xliff2.reader.Event xliff2Event : xliff2Events) {
            xliffToolkitWriter.writeEvent(xliff2Event);
        }

        // Since we instantiated a new Writer Output Stream, we have to flush the current stream before the other one is
        // closed, or we'll lose all the data. This is the same as the GenericFilterWriter's behavior.
        if (!xliff2Events.isEmpty()) {
	        final net.sf.okapi.lib.xliff2.reader.Event event1 = xliff2Events.get(xliff2Events.size() - 1);
	        if(event1.getType() == EventType.END_DOCUMENT){
	            close();
	        }
        }

        return event;

    }



    @Override
    public void close() {
        if (xliffToolkitWriter != null) {
            xliffToolkitWriter.close();
            xliffToolkitWriter = null;
        }
        outputFile = null;
        outputChoice = Output.NONE;
    }

    @Override
    public IParameters getParameters() {
        // Not implemented yet
        return null;
    }

    @Override
    public void setParameters(IParameters params) {
        // Not implemented yet
    }

    @Override
    public void cancel() {
        if(xliffToolkitWriter != null) {
            xliffToolkitWriter.close();
            xliffToolkitWriter = null;
        }
        outputChoice = Output.NONE;
    }

    @Override
    public EncoderManager getEncoderManager() {
    	if ( encoderManager == null ) {
    		encoderManager = new EncoderManager();
			encoderManager.setMapping(MimeTypeMapper.XLIFF2_MIME_TYPE, "net.sf.okapi.common.encoder.XMLEncoder");
		}
		return encoderManager;
    }

    @Override
    public ISkeletonWriter getSkeletonWriter() {
        // Not implemented yet
        return null;
    }

    /**
     * Initializes the writer with the specified source language and the set target locale. Because we can't
     * initialize the toolkit writer without a source locale, we have to wait until the Start Document event is
     * provided.
     *
     * @param sourceLang The source language derived from the XLIFF 2.0 file
     */
    void initializeWriter(LocaleId sourceLang) {
        sourceLocale = sourceLang;
        final String target = targetLocale != null ? targetLocale.toString() : null;
        switch (outputChoice) {
            case PATH:
                xliffToolkitWriter.create(outputFile, sourceLocale.toString(), target);
                break;
            case STREAM:
                xliffToolkitWriter.create(writer, sourceLocale.toString(), target);
                break;
            case NONE:
                logger.error("No output set for XLIFF 2 to be written to");
                throw new RuntimeException("No output set for XLIFF 2 to be written to");
        }
    }

    public LocaleId getSourceLocale() {
        return sourceLocale;
    }

    public LocaleId getTargetLocale() {
        return targetLocale;
    }

}
