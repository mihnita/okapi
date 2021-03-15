/*===========================================================================
  Copyright (C) 2017 by the Okapi Framework contributors
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
===========================================================================*/

package net.sf.okapi.steps.rainbowkit.xliff;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.html.HtmlFilter;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.rainbowkit.creation.ExtractionStep;
import net.sf.okapi.steps.segmentation.Parameters;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import net.sf.okapi.steps.terminologyleveraging.BaseTerminologyConnector;
import net.sf.okapi.steps.terminologyleveraging.TerminologyLeveragingStep;
import net.sf.okapi.steps.terminologyleveraging.TerminologyParameters;
import net.sf.okapi.steps.terminologyleveraging.TerminologyQueryResult;

/**
 * @author Vladyslav Mykhalets
 */
@RunWith(JUnit4.class)
public class TerminologyStepTest {

    private FileLocation root;

    private LocaleId locEN = LocaleId.fromString("en");
    private LocaleId locFR = LocaleId.fromString("fr");

    @Before
    public void setUp() {
        root = FileLocation.fromClass(this.getClass());
    }

    @Test
    public void test() {
        Util.deleteDirectory(root.out("/xliff-terminology/work").toString(), false);

        String outFile = "/xliff-terminology/work/test-terminology.html.xlf";

        IPipelineDriver pipelineDriver = new PipelineDriver();
        FilterConfigurationMapper fcMapper = new FilterConfigurationMapper();
        fcMapper.addConfigurations(HtmlFilter.class.getName());
        pipelineDriver.setFilterConfigurationMapper(fcMapper);
        String rootDir = root.in("/").toString();
        pipelineDriver.setRootDirectories(rootDir, rootDir);

        // raw document to filter events step
        pipelineDriver.addStep(new RawDocumentToFilterEventsStep());

        // segmentation step
        SegmentationStep segmentationStep = new SegmentationStep();
        Parameters segmentationParameters = segmentationStep.getParameters();
        segmentationParameters.setSegmentSource(true);
        segmentationParameters.setSegmentTarget(false);
        segmentationParameters.setRenumberCodes(false);
        segmentationParameters.setSourceSrxPath(root.in("/defaultSegmentation.srx").toString());
        segmentationParameters.setCopySource(true);
        segmentationParameters.setCheckSegments(false);
        segmentationParameters.setTrimSrcLeadingWS(-1);
        segmentationParameters.setTrimSrcTrailingWS(-1);
        segmentationParameters.setTrimTrgLeadingWS(-1);
        segmentationParameters.setTrimTrgTrailingWS(-1);
        segmentationParameters.setForcesegmentedOutput(true);
        segmentationParameters.setOverwriteSegmentation(false);
        segmentationParameters.setDeepenSegmentation(false);
        pipelineDriver.addStep(segmentationStep);

        // terminology leveraging step
        TerminologyLeveragingStep terminologyLeveragingStep = new TerminologyLeveragingStep();
        TerminologyParameters terminologyParams = terminologyLeveragingStep.getParameters();
        terminologyParams.setLeverage(true);
        terminologyParams.setAnnotateSource(true);
        terminologyParams.setAnnotateTarget(true);
        terminologyParams.setConnectorClassName("net.sf.okapi.steps.rainbowkit.xliff.TerminologyStepTest$DefaultTerminologyConnector");
        pipelineDriver.addStep(terminologyLeveragingStep);

        // extraction step
        ExtractionStep extractionStep = new ExtractionStep();
		String outputDir = root.out("/").toString();
		extractionStep.getParameters().setPackageDirectory(outputDir);
        pipelineDriver.addStep(extractionStep);
        net.sf.okapi.steps.rainbowkit.creation.Parameters ep = extractionStep.getParameters();
        ep.setWriterClass("net.sf.okapi.steps.rainbowkit.xliff.XLIFF2TerminologyPackageWriter");
        ep.setPackageName("xliff-terminology");

        URI inputURI = root.in("/test-terminology.html").asUri();
        URI outputURI = new File(outFile).toURI();
        pipelineDriver.addBatchItem(new BatchItemContext(inputURI, "UTF-8", "okf_html", outputURI, "UTF-8", locEN, locFR));

        pipelineDriver.processBatch();

        assertTrue(root.out(outFile).asFile().exists());
    }

    public static class DefaultTerminologyConnector extends BaseTerminologyConnector {
        private final Logger LOGGER = LoggerFactory.getLogger(getClass());

        public static Map<String, TerminologyQueryResult> translations = new HashMap<>();

        static {
            TerminologyQueryResult queryResult = new TerminologyQueryResult();
            queryResult.setTerm(new TerminologyQueryResult.Term("0", "censhare", "Alice"));
            queryResult.addTranslation("52", "Alice");
            queryResult.addTranslation("53", "Anya");
            translations.put(queryResult.getTerm().getTermText(), queryResult);

            TerminologyQueryResult queryResult1 = new TerminologyQueryResult();
            queryResult1.setTerm(new TerminologyQueryResult.Term("0","censhare", "White Rabbit"));
            queryResult1.addTranslation("48", "weißes Kaninchen");
            queryResult1.addTranslation("49", "Kaninchen (weiß)");
            translations.put(queryResult1.getTerm().getTermText(), queryResult1);

            TerminologyQueryResult queryResult2 = new TerminologyQueryResult();
            queryResult2.setTerm(new TerminologyQueryResult.Term("0","censhare", "Rabbit"));
            queryResult2.addTranslation("42", "Kaninchen");
            translations.put(queryResult2.getTerm().getTermText(), queryResult2);
        }

        public String generateGlossEntryId(int segmentIndex, int termIndexInSegment, TerminologyQueryResult.Term term) {
            return term.getId()
                    + GLOSS_ENTRY_SEPARATOR_CHAR + segmentIndex
                    + GLOSS_ENTRY_SEPARATOR_CHAR + termIndexInSegment;
        }

        public String generateGlossEntryTranslationId(int segmentIndex, int termIndexInSegment, TerminologyQueryResult.Term term, TerminologyQueryResult.Translation translation) {
            return term.getId()
                    + GLOSS_ENTRY_SEPARATOR_CHAR + segmentIndex
                    + GLOSS_ENTRY_SEPARATOR_CHAR + termIndexInSegment
                    + GLOSS_ENTRY_TRANSLATION_ID_SEPARATOR_CHAR + translation.getId();
        }

        public String getName() {
            return "Default-Terminology-Connector";
        }

        @Override
        public void open() {
            LOGGER.info("Opening terminology connector");
        }

        public void close() {
            LOGGER.info("Closing terminology connector");
        }

        @Override
        protected List<List<TerminologyQueryResult>> query(List<String> sourceSegments) {
            List<List<TerminologyQueryResult>> results = new ArrayList<>();
            for (String segmentSource : sourceSegments) {
                List<TerminologyQueryResult> segmentResults = new ArrayList<>();
                for (String term : translations.keySet()) {
                    if (segmentSource.contains(term)) {
                        segmentResults.add(translations.get(term));
                    }
                }
                results.add(segmentResults);
            }
            return results;
        }

    }
}
