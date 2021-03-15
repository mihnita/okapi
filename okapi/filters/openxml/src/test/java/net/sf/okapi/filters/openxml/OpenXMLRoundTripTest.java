/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.filters.fontmappings.DefaultFontMapping;
import net.sf.okapi.common.filters.fontmappings.DefaultFontMappings;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * This tests OpenXMLFilter (including OpenXMLContentFilter) and
 * OpenXMLZipFilterWriter (including OpenXMLContentSkeleton writer)
 * by filtering, automatically translating, and then writing the
 * zip file corresponding to a Word, Excel or Powerpoint 2009 file, 
 * then comparing it to a gold file to make sure nothing has changed.
 * It does this with a specific list of files.
 * 
 * <p>This is done with no translator first, to make sure the same
 * file is created that was filtered in the first place.  Then it
 * is translated into Pig Latin by PigLatinTranslator, translated so
 * codes are expanded by CodePeekTranslator, and then translated to
 * see a view like the translator will see by TagPeekTranslator.
 */

@RunWith(JUnit4.class)
public class OpenXMLRoundTripTest extends AbstractOpenXMLRoundtripTest {
	private LocaleId locENUS = LocaleId.fromString("en-us");

	private FileLocation root;

	@Before
	public void before() throws Exception {
		this.allGood = true;
		this.root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testHiddenTablesWithFormula() {
		ConditionalParameters cparams = getParametersFromUserInterface();
		cparams.setTranslateExcelHidden(false);
		runOneTest("hidden_table_with_formula.xlsx", true, false, cparams);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void testHiddenMergeCells() {
		ConditionalParameters cparams = getParametersFromUserInterface();
		cparams.setTranslateExcelHidden(false);
		runOneTest("HiddenMergeCells.xlsx", true, false, cparams);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void testPhoneticRunPropertyForAsianLanguages() {
		ConditionalParameters cparams = getParametersFromUserInterface();
		cparams.setTranslateExcelHidden(false);
		runOneTest("japanese_phonetic_run_property.xlsx", true, false, cparams);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void testExternalHyperlinks() {
		ConditionalParameters cparams = getParametersFromUserInterface();
		cparams.setExtractExternalHyperlinks(true);
		runOneTest("external_hyperlink.docx", true, false, cparams);
		runOneTest("external_hyperlink.pptx", true, false, cparams);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void testClarifiablePart() throws Exception {
		ConditionalParameters conditionalParameters = getParametersFromUserInterface();

		runOneTest("clarifiable-part-en.pptx", false, false, conditionalParameters, "", LocaleId.ENGLISH, LocaleId.ENGLISH);
		runOneTest("clarifiable-part-ar.pptx", false, false, conditionalParameters, "", LocaleId.ARABIC, LocaleId.ARABIC);
		runOneTest("clarifiable-part-en.xlsx", false, false, conditionalParameters, "", LocaleId.ENGLISH, LocaleId.ENGLISH);
		runOneTest("clarifiable-part-ar.xlsx", false, false, conditionalParameters, "", LocaleId.ARABIC, LocaleId.ARABIC);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void doesNotAcceptRevisions() throws Exception {
		ConditionalParameters conditionalParameters = getParametersFromUserInterface();
		conditionalParameters.setAutomaticallyAcceptRevisions(false);

		runOneTest("numbering-revisions.docx", false, false, conditionalParameters);
		runOneTest("table-grid-revisions.docx", false, false, conditionalParameters);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void acceptsRevisionsInComplexFields() throws Exception {
		ConditionalParameters conditionalParameters = getParametersFromUserInterface();

		runOneTest("768.docx", false, false, conditionalParameters);
		runOneTest("768-2.docx", false, false, conditionalParameters);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void acceptsMovedContentRevisions() throws Exception {
		ConditionalParameters conditionalParameters = getParametersFromUserInterface();

		runOneTest("843-1.docx", false, false, conditionalParameters);
		runOneTest("843-2.docx", false, false, conditionalParameters);
		runOneTest("843-31.docx", false, false, conditionalParameters);
		runOneTest("843-32.docx", false, false, conditionalParameters);
		runOneTest("843-33.docx", false, false, conditionalParameters);
		runOneTest("843-34.docx", false, false, conditionalParameters);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void acceptsDeletedParagraphMarkRevision() throws Exception {
		ConditionalParameters conditionalParameters = getParametersFromUserInterface();
		runOneTest("847-1.docx", false, false, conditionalParameters);
		runOneTest("847-2.docx", false, false, conditionalParameters);
		runOneTest("847-3.docx", false, false, conditionalParameters);
		assertTrue("Some Roundtrip files failed.",allGood);
	}

	@Test
	public void insertedAndDeletedTableRowRevisionsAccepted() {
		runOneTest("848.docx", false, false, new ConditionalParameters());
		runOneTest("848-nested-tables-with-revisions.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void nestedTablesWithoutRevisionsRoundTripped() {
		runOneTest("848-nested-tables.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void runTestsWithColumnExclusion() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
        params.setTranslateExcelExcludeColumns(true);
        params.tsExcelExcludedColumns = new TreeSet<>();
        params.tsExcelExcludedColumns.add("1A");

        runOneTest("shared_string_in_two_columns.xlsx", true, false, params);
        assertTrue("Some Roundtrip files failed.", allGood);
	}

	// Slimmed-down version of some of the integration tests -- this checks for idempotency
	// by roundtripping once, then using the output of that to roundtrip again.  The first and
	// second roundtrip outputs should be the same.
	@Test
	public void runTestTwice() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		runTestTwice("Escapades.docx", params);
	}

	@Test
	public void runTestsExcludeGraphicMetaData() {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateWordExcludeGraphicMetaData(true);
		runTests("exclude_graphic_metadata/", params,
				"textarea.docx",
				"picture.docx");
		assertTrue("Some Roundtrip files failed.", allGood);
	}

	@Test
	public void runTestsWithAggressiveTagStripping() {
		ConditionalParameters params = new ConditionalParameters();
		params.setCleanupAggressively(true);
		runTests("aggressive/", params,
				 "spacing.docx",
				 "vertAlign.docx");
		assertTrue("Some Roundtrip files failed.", allGood);
	}

	@Test
	public void runTestsWithHiddenCellsExposed() {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateExcelHidden(true);
		runTests("hidden_cells/", params, "hidden_cells.xlsx");
		runTests("hidden_cells/", params, "hidden_stuff.xlsx");
		runTests("hidden_cells/", params, "hidden_table.xlsx");
		assertTrue("Some Roundtrip files failed.", allGood);
	}

	@Test
	public void runTestWithStyledTextCell() {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateExcelHidden(true);
		runOneTest("styled_cells.xlsx", true, false, params);
	}

	@Test
	public void roundTripsNestedContent() throws Exception {
		runOneTest("798.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundTripsWithRefinedComplexFieldsEndBoundaries() throws Exception {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.tsComplexFieldDefinitionsToExtract.add("COMMENTS");
		conditionalParameters.tsComplexFieldDefinitionsToExtract.add("TITLE");

		runOneTest("830-1.docx", false, false, conditionalParameters);
		runOneTest("830-2.docx", false, false, conditionalParameters);
		runOneTest("830-3.docx", false, false, conditionalParameters);
		runOneTest("830-4.docx", false, false, conditionalParameters);
		runOneTest("830-5.docx", false, false, conditionalParameters);
		runOneTest("830-7.docx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void roundTripsWithStructuralDocumentTags() throws Exception {
		runOneTest("834.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundTripsWithReorderedNotesAndComments() throws Exception {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.setReorderPowerpointNotesAndComments(true);
		runOneTest("835.pptx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void roundTripsInStrictMode() throws Exception {
		runOneTest("858.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundTripsWithOptimisedWordProcessingStyles() throws Exception {
		runOneTest("853-all-common.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundTripsWithClarifiedBidiFormattingInStyles() throws Exception {
		runOneTest("899.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void powerpointBidiFormattingConsidered() throws Exception {
		runOneTest("927-bodypr-rtlcol-1.pptx", false, false, new ConditionalParameters(), "", LocaleId.ARABIC, LocaleId.ENGLISH);
		runOneTest("927-bodypr-rtlcol-0.pptx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		runOneTest("927-presentation-rtl-1.pptx", false, false, new ConditionalParameters(), "", LocaleId.ARABIC, LocaleId.ENGLISH);
		runOneTest("927-presentation-rtl-0.pptx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		runOneTest("927-tblpr-rtl-1.pptx", false, false, new ConditionalParameters(), "", LocaleId.ARABIC, LocaleId.ENGLISH);
		runOneTest("927-tblpr-rtl-0.pptx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		runOneTest("927-p-ppr-rtl-1.pptx", false, false, new ConditionalParameters(), "", LocaleId.ARABIC, LocaleId.ENGLISH);
		runOneTest("927-p-ppr-rtl-0.pptx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		runOneTest("927-r-rpr-rtl.pptx", false, false, new ConditionalParameters(), "", LocaleId.HEBREW, LocaleId.ENGLISH);
		runOneTest("927-r-rpr-no-rtl.pptx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.HEBREW);
		assertTrue(allGood);
	}

	/**
	 * Runs tests for all given files.
	 *
	 * @param files file names
	 */
	private void runTests(String goldSubDirPath, ConditionalParameters params, String... files) {
		for(String s : files)
		{
			runOneTest(s, true, false, params, goldSubDirPath);  // PigLatin
		}
		assertTrue("Some Roundtrip files failed.", allGood);
	}

	@Test
	public void runTestsAddLineSeparatorCharacter() {
		ConditionalParameters params = new ConditionalParameters();
		params.setAddLineSeparatorCharacter(true);

		List<String> files = new ArrayList<>();
		files.add("Document-with-soft-linebreaks.docx");
		files.add("Document-with-soft-linebreaks.pptx");
		files.add("PageBreak.docx");

		runTests("lbaschar/", params, files.toArray(new String[0]));
	}

	@Test
	public void testAdditionalDocumentTypes() throws Exception {
		ConditionalParameters conditionalParameters = getParametersFromUserInterface();
		conditionalParameters.setTranslateExcelSheetNames(true);

		runOneTest("macro-2.docm", true, false, conditionalParameters);

		runOneTest("template-2.dotx", true, false, conditionalParameters);
		runOneTest("macro-template-2.dotm", true, false, conditionalParameters);

		runOneTest("macro-2.pptm", true, false, conditionalParameters);

		runOneTest("show-2.ppsx", true, false, conditionalParameters);
		runOneTest("macro-show-2.ppsm", true, false, conditionalParameters);

		runOneTest("template-2.potx", true, false, conditionalParameters);
		runOneTest("macro-template-2.potm", true, false, conditionalParameters);

		runOneTest("macro-2.xlsm", true, false, conditionalParameters);

		runOneTest("template-2.xltx", true, false, conditionalParameters);
		runOneTest("macro-template-2.xltm", true, false, conditionalParameters);

		runOneTest("2-pages.vsdx", true, false, conditionalParameters);
		runOneTest("2-pages.vsdm", true, false, conditionalParameters);

		assertTrue("Some Roundtrip files failed.", allGood);
	}

	@Test
	public void testMultilineFormula() throws Exception {
		runOneTest("multiline_formula.xlsx", true, false, new ConditionalParameters());
		assertTrue("Roundtrip file failed.", allGood);
	}

	@Test
	public void doesNotCrashWithEmptyParagraphLevelsInNotesStyles() {
		runOneTest("794.pptx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundtripsWithStyleOptimisationApplied() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("formatting/803-1.pptx", false, false, conditionalParameters, "formatting/");
		runOneTest("formatting/803-2.pptx", false, false, conditionalParameters, "formatting/");
		runOneTest("formatting/803-oo.pptx", false, false, conditionalParameters, "formatting/");
		runOneTest("formatting/803-defrprs-and-no-rprs.pptx", false, false, conditionalParameters, "formatting/");
		runOneTest("formatting/803-defrprs-and-rprs.pptx", false, false, conditionalParameters, "formatting/");
		runOneTest("853-all-common.docx", false, false, conditionalParameters);
		runOneTest("884.docx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void roundtripsWithAggressiveCleanup() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.setCleanupAggressively(true);
		runOneTest("formatting/823.pptx", false, false, conditionalParameters, "formatting/");
		runOneTest("formatting/complexScript.docx", false, false, conditionalParameters, "formatting/");
		assertTrue(allGood);
	}

	@Test
	public void doesNotCrashOnRequesting0ParagraphLevel() {
		runOneTest("882.pptx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundtripsWithRunFontsHintRespect() {
		runOneTest("851.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void roundtripsWithRunFontsDifferences() {
		runOneTest("888.docx", false, false, new ConditionalParameters());
		assertTrue(allGood);
	}

	@Test
	public void documentWithRtlLanguageIsMerged() {
		runOneTest("930.docx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		assertTrue(allGood);
	}

	@Test
	public void secondDocumentWithRtlLanguageIsMerged() {
		runOneTest("992.docx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		assertTrue(allGood);
	}

	@Test
	public void doesNotCrashOnMerging() {
		runOneTest("956.docx", false, false, new ConditionalParameters(), "", LocaleId.ENGLISH, LocaleId.ARABIC);
		assertTrue(allGood);
	}

	@Test
	public void fontMappingsAppliedInWordDocuments() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.fontMappings(
			new DefaultFontMappings(
				new DefaultFontMapping(".*", ".*", "Times.*", "Arial")
			)
		);
		runOneTest("937-1.docx", false, false, conditionalParameters);
		runOneTest("937-2.docx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void fontMappingsAppliedInPresentationDocuments() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.fontMappings(
			new DefaultFontMappings(
				new DefaultFontMapping(".*", ".*", "Times.*", "Arial")
			)
		);
		runOneTest("958-1.pptx", false, false, conditionalParameters);
		conditionalParameters.fontMappings(
			new DefaultFontMappings(
				new DefaultFontMapping(".*", ".*", "Arial", "Times New Roman"),
				new DefaultFontMapping(".*", ".*", "\\+mn.*", "Times New Roman")
			)
		);
		runOneTest("958-2.pptx", false, false, conditionalParameters);
		runOneTest("958-3.pptx", false, false, conditionalParameters);
		runOneTest("958-4.pptx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void runPropertiesMinified() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("948-1.docx", false, false, conditionalParameters);
		runOneTest("948-2.pptx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	/**
	 * @todo #948: move to {@link OpenXMLRoundTripTest#runPropertiesMinified()}
	 *  when the SpreadsheetML restriction is removed
	 */
	@Test
	public void runPropertiesNotMinified() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("948-3.xlsx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void runContainersConsideredForStylesOptimisation() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("952-1.docx", false, false, conditionalParameters);
		runOneTest("952-2.docx", false, false, conditionalParameters);
		runOneTest("952-3.docx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void powerpointStylesHierarchyConsidered() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("999.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-master-body-style.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-master-body-style-override.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-master-title-style.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-master-title-style-override.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-master-lst-style.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-master-lst-style-override.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-layout-title-lst-style.pptx", false, false, conditionalParameters);
		runOneTest("999-slide-layout-title-lst-style-override.pptx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void powerpointTableStylesConsidered() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("1009-1.pptx", false, false, conditionalParameters);
		runOneTest("1009-2.pptx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	@Test
	public void okapiMarkersPreserved() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		runOneTest("OkapiMarkers.docx", false, false, conditionalParameters);
		runOneTest("OkapiMarkers.xlsx", false, false, conditionalParameters);
		runOneTest("OkapiMarkers.pptx", false, false, conditionalParameters);
		assertTrue(allGood);
	}

	private void runTestTwice (String filename, ConditionalParameters cparams) {
		try {

			Path inputPath = root.in("/" + filename).asPath();
			Path outputPath1 = root.out("/1_" + filename).asPath();

			roundTrip(inputPath, outputPath1, cparams);

			Path outputPath2 = root.out("/2_" + filename).asPath();

			roundTrip(outputPath1, outputPath2, cparams);

			OpenXMLPackageDiffer differ = new OpenXMLPackageDiffer(Files.newInputStream(outputPath1),
																   Files.newInputStream(outputPath2));
			boolean same = differ.isIdentical();
			if (!same) {
				LOGGER.warn("{}{}", filename, (same ? " SUCCEEDED" : " FAILED"));
				for (OpenXMLPackageDiffer.Difference d : differ.getDifferences()) {
					LOGGER.warn("+ {}", d.toString());
				}
			}
			differ.cleanup();
			assertTrue(same);
		}
		catch ( Throwable e ) {
			LOGGER.warn("Failed to roundtrip file {}", filename, e);
			fail("An unexpected exception was thrown on file '" + filename+e.getMessage());
		}
	}

	private void roundTrip(Path inputFullPath, Path outputFullPath, ConditionalParameters cparams) throws Exception {
		try (OpenXMLFilter filter = new OpenXMLFilter();
				OpenXMLFilterWriter writer = new OpenXMLFilterWriter(cparams,
						factories.getInputFactory(), factories.getOutputFactory(), factories.getEventFactory())) {

			filter.setParameters(cparams);
			filter.setOptions(locENUS, "UTF-8", true);
			try {
				filter.open(new RawDocument(inputFullPath.toUri(),"UTF-8", locENUS),true); // DWH 7-16-09 squishiness
			} catch(Exception e) {
				throw new OkapiException(e);
			}

			writer.setOptions(locENUS, "UTF-8");
			writer.setOutput(outputFullPath.toString());

			while ( filter.hasNext() ) {
				Event event = filter.next();
				if (event != null) {
					writer.handleEvent(event);
				}
			}
		}
	}

	private ConditionalParameters getParametersFromUserInterface()
	{
		ConditionalParameters parms;
//    Choose the first to get the UI $$$
//		parms = (new Editor()).getParametersFromUI(new ConditionalParameters());
		parms = new ConditionalParameters();
		return parms;
	}
}
