package net.sf.okapi.steps.inconsistencycheck;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.StreamUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EventTest {

    private FileLocation root;
    private LocaleId locEN = new LocaleId("en", "us");
    private LocaleId locFR = new LocaleId("fr", "fr");
    private InconsistencyCheckStep step;

    @Before
    public void setUp() {
        root = FileLocation.fromClass(this.getClass());
        step = new InconsistencyCheckStep();
        Parameters params = step.getParameters();
        params.setAutoOpen(false);
    }

    @Test
    public void SameSourceTest() throws IOException {
        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SameSource.html.xlf").asUri();
        URI outputURI = new File("/SameSource.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        // Run pipeline
        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_SameSource.xml").toString(), "UTF-8"));
    }

    @Test
    public void SameTargetTest() throws IOException {
        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SameTarget.html.xlf").asUri();
        URI outputURI = new File("/SameTarget.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_SameTarget.xml").toString(), "UTF-8"));
    }

    @Test
    public void SameSourceAndTargetTest() throws IOException {
        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SameSourceAndTarget.html.xlf").asUri();
        URI outputURI = new File("/SameSourceAndTarget.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_SameSourceAndTarget.xml").toString(), "UTF-8"));
    }

    @Test
    public void SimpleSubDocTest() throws IOException {
        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SimpleSubDocTest.html.xlf").asUri();
        URI outputURI = new File("/SimpleSubDocTest.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_SimpleSubDocTest.xml").toString(), "UTF-8"));
    }

    @Test
    public void SameSourceWithCodeTest() throws IOException {
        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SameSourceWithCode.html.xlf").asUri();
        URI outputURI = new File("/SameSourceWithCode.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        // Run pipeline
        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_SameSourceWithCode.xml").toString(), "UTF-8"));
    }

    @Test
    public void OriginalOutputWithCodeTest() throws IOException {
        // setup parameters
        Parameters params = step.getParameters();
        params.setDisplayOption(Parameters.DISPLAYOPTION_ORIGINAL);

        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SameSourceWithCode.html.xlf").asUri();
        URI outputURI = new File("/SameSourceWithCode.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        // Run pipeline
        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_OriginalOutputWithCode.xml").toString(), "UTF-8"));
    }

    @Test
    public void PlainOutputWithCodeTest() throws IOException {
        // setup parameters
        Parameters params = step.getParameters();
        params.setDisplayOption(Parameters.DISPLAYOPTION_PLAIN);

        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI = root.in("/SameSourceWithCode.html.xlf").asUri();
        URI outputURI = new File("/SameSourceWithCode.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_xliff", outputURI, "UTF-8", locEN, locFR));

        // Run pipeline
        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_PlainOutputWithCode.xml").toString(), "UTF-8"));
    }

    @Test
    public void PerFileWithCodeTest() throws IOException {
        // setup parameters
        Parameters params = step.getParameters();
        params.setDisplayOption(Parameters.DISPLAYOPTION_GENERIC);
        params.setCheckPerFile(true);

        // Setup pipeline
        IPipelineDriver pdriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(XLIFFFilter.class.getName());
        pdriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pdriver.setRootDirectories(rootDir, rootDir);
        pdriver.addStep(new RawDocumentToFilterEventsStep());
        pdriver.addStep(step);

        // Setup input
        URI inputURI1 = root.in("/SameSourceWithCode.html.xlf").asUri();
        URI outputURI1 = new File("/SameSourceWithCode.html.out.xlf").toURI();
        URI inputURI2 = root.in("/SameTargetWithCode.html.xlf").asUri();
        URI outputURI2 = new File("/SameTargetWithCode.html.out.xlf").toURI();

        // Add files
        pdriver.addBatchItem(new BatchItemContext(inputURI1, "UTF-8", "okf_xliff", outputURI1, "UTF-8", locEN, locFR));
        pdriver.addBatchItem(new BatchItemContext(inputURI2, "UTF-8", "okf_xliff", outputURI2, "UTF-8", locEN, locFR));

        // Run pipeline
        pdriver.processBatch();

        // Compare with GOLD file
        maskDocIdPath("/inconsistency-report.xml");
        FileCompare fc = new FileCompare();
        assertTrue(fc.compareFilesPerLines(root.out("/inconsistency-report.xml").toString(),
                root.in("/Gold_PerFileWithCode.xml").toString(), "UTF-8"));
    }

    //
    //  Helper Methods
    //
    /**
     * Masks the non-fixed part of the path in the docId URI for a given report
     * file. (this part depends on where the project is built).
     *
     * @param path the path of the report file to change.
     * @throws IOException if an error occurs.
     */
    private void maskDocIdPath(String path)
            throws IOException {

        // Read it all in a buffer
        String str = StreamUtil.streamUtf8AsString(root.in(path).asInputStream());

        // Do the conversion
        Pattern pattern = Pattern.compile("doc=\"(.*)\"");
        Matcher matcher = pattern.matcher(str);
        StringBuffer buffer = new StringBuffer(str.length());
        while(matcher.find()) {
            String filename = Util.getFilename(matcher.group(1), true);
            matcher.appendReplacement(buffer, "doc=\"ROOT/" + filename + "\"");
        }
        matcher.appendTail(buffer);

        try (Writer out = new OutputStreamWriter(root.out(path).asOutputStream(), StandardCharsets.UTF_8)) {
            // Write out the result
            out.write(buffer.toString());
        }
    }
}
