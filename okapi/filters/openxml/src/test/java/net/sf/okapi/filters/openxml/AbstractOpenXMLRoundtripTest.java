package net.sf.okapi.filters.openxml;

import static org.junit.Assert.fail;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.resource.RawDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractOpenXMLRoundtripTest {
    private static final String DEFAULT_ENCODING = OpenXMLFilter.ENCODING.name();

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    protected final XMLFactories factories = new XMLFactoriesForTest();

    protected boolean allGood=true;

    public void runOneTest (String filename, boolean bTranslating, boolean bPeeking, ConditionalParameters cparams) {
        runOneTest(filename, bTranslating, bPeeking, cparams, "");
    }

    public void runOneTest (String filename, boolean bTranslating, boolean bPeeking, ConditionalParameters cparams, String goldSubdirPath) {
        runOneTest(filename, bTranslating, bPeeking, cparams, goldSubdirPath, LocaleId.US_ENGLISH, LocaleId.US_ENGLISH);
    }

    public void runOneTest (String filename, boolean bTranslating, boolean bPeeking,
        ConditionalParameters cparams, String goldSubdirPath, LocaleId sourceLocale, LocaleId targetLocale) {
        runOneTest(filename, bTranslating, bPeeking, cparams, goldSubdirPath, sourceLocale, targetLocale, null);
    }

    public void runOneTest (String filename, boolean bTranslating, boolean bPeeking,
        ConditionalParameters cparams, String goldSubdirPath,
        LocaleId sourceLocale, LocaleId targetLocale,
        IFilterConfigurationMapper configurationMapper) {

        FileLocation root = FileLocation.fromClass(getClass());
        Event event;
        URI uri;
        OpenXMLFilter filter = null;
        boolean rtrued2;
        try {
            if (bPeeking)
            {
                if (bTranslating)
                    filter = new OpenXMLFilter(new CodePeekTranslator(), targetLocale);
                else
                    filter = new OpenXMLFilter(new TagPeekTranslator(), targetLocale);
            }
            else if (bTranslating) {
                filter = new OpenXMLFilter(new PigLatinTranslator(), targetLocale);
            }
            else
                filter = new OpenXMLFilter();

            filter.setParameters(cparams);
            filter.setOptions(sourceLocale, DEFAULT_ENCODING, true);
            filter.setFilterConfigurationMapper(configurationMapper);

            uri = root.in("/" + filename).asUri();

            try
            {
                filter.open(new RawDocument(uri, DEFAULT_ENCODING, sourceLocale, targetLocale));
            }
            catch(Exception e)
            {
                throw new OkapiException(e);
            }

            OpenXMLFilterWriter writer = new OpenXMLFilterWriter(cparams,
                    factories.getInputFactory(), factories.getOutputFactory(), factories.getEventFactory());

            if (bPeeking)
                writer.setOptions(targetLocale, DEFAULT_ENCODING);
            else if (bTranslating)
                writer.setOptions(targetLocale, DEFAULT_ENCODING);
            else
                writer.setOptions(targetLocale, DEFAULT_ENCODING);

            String prefix = bPeeking ? (bTranslating ? "Peek" : "Tag") : (bTranslating ? "Tran" : "Out");
            String writerFilename = "/" + prefix + filename;
            writer.setOutput(root.out(writerFilename).toString());

            while ( filter.hasNext() ) {
                event = filter.next();
                if (event!=null)
                {
                    writer.handleEvent(event);
                }
                else
                    event = null; // just for debugging
            }
            writer.close();
            Path outputPath = root.out(writerFilename).asPath();
            LOGGER.debug("Output: {}", outputPath);
            Path goldPath = Paths.get(root.in("/gold").toString(), goldSubdirPath, writerFilename);
            LOGGER.debug("Gold: {}", goldPath);

            OpenXMLPackageDiffer differ = new OpenXMLPackageDiffer(Files.newInputStream(goldPath),
                    Files.newInputStream(outputPath));
            rtrued2 = differ.isIdentical();
            if (!rtrued2) {
                LOGGER.warn("{}{}{}", prefix, filename, (rtrued2 ? " SUCCEEDED" : " FAILED"));
                for (OpenXMLPackageDiffer.Difference d : differ.getDifferences()) {
                    LOGGER.warn("+ {}", d.toString());
                }
            }
            if (!rtrued2)
                allGood = false;
            differ.cleanup();
        }
        catch ( Throwable e ) {
            LOGGER.warn("Failed to roundtrip file {}", filename, e);
            fail("An unexpected exception was thrown on file '"+filename+"', msg="+e.getMessage());
        }
        finally {
            if ( filter != null ) filter.close();
        }
    }
}
