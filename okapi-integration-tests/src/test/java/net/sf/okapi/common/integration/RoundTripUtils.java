package net.sf.okapi.common.integration;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.ISimplifierRulesParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filters.DefaultFilters;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filterwriter.XLIFFWriter;
import net.sf.okapi.common.filterwriter.XLIFFWriterParameters;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.lib.merge.step.OriginalDocumentXliffMergerStep;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.common.RawDocumentWriterStep;
import net.sf.okapi.steps.common.codesimplifier.CodeSimplifierStep;
import net.sf.okapi.steps.common.codesimplifier.PostSegmentationCodeSimplifierStep;
import net.sf.okapi.steps.segmentation.Parameters;
import net.sf.okapi.steps.segmentation.SegmentationStep;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluators;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public final class RoundTripUtils {
	private static String sourceSrx = RoundTripUtils.class.getClassLoader().getResource("default.srx").getPath();
	private static String targetSrx = RoundTripUtils.class.getClassLoader().getResource("default.srx").getPath();

	public static void extract(final LocaleId english, final LocaleId french, final String original, final String xliff,
			final String configName, final String customConfigPath, final boolean segment) throws URISyntaxException {
		extract(english, french, original, xliff, configName, customConfigPath, segment, false);
	}

	public static void extract(final LocaleId source, final LocaleId target, final String originalPath, final String outputPath, final String filterConfig, final String customConfigPath, final boolean segment, final boolean simplify) throws URISyntaxException {
		final FilterConfigurationMapper mapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(mapper, false, true);
		if (customConfigPath != null) {
			mapper.setCustomConfigurationsDirectory(customConfigPath);
			mapper.addCustomConfiguration(filterConfig);
			// look for secondary filter config - only one per subdir
			final File secondary = IntegrationtestUtils.getSecondaryConfigFile(customConfigPath, filterConfig);
			if (secondary != null) {
				mapper.addCustomConfiguration(Util.getFilename(secondary.getName(), false));
			}         
			mapper.updateCustomConfigurations();
		}

		// Create the driver
		final PipelineDriver driver = new PipelineDriver();
		driver.setFilterConfigurationMapper(mapper);

		// Raw document to filter events step 
		final RawDocumentToFilterEventsStep rd2feStep = new RawDocumentToFilterEventsStep();
		driver.addStep(rd2feStep);

		if (segment) {
			final SegmentationStep ss = new SegmentationStep();
			ss.setSourceLocale(source);
			final List<LocaleId> tl = new LinkedList<>();
			tl.add(target);
			ss.setTargetLocales(tl);
			final Parameters params = ss.getParameters();
			params.setSegmentSource(true);
			params.setSegmentTarget(true);
			params.setSourceSrxPath(sourceSrx);
			params.setTargetSrxPath(targetSrx);
			params.setCopySource(false);
			driver.addStep(ss);
		}

		if (simplify) {
			net.sf.okapi.steps.common.codesimplifier.Parameters p = new net.sf.okapi.steps.common.codesimplifier.Parameters();
			ISimplifierRulesParameters fp = (ISimplifierRulesParameters) mapper.getConfiguration(filterConfig).parameters;
			if (fp != null && null != fp.getSimplifierRules()) {
				p.setRules(fp.getSimplifierRules());
			}

			if (segment) {
				PostSegmentationCodeSimplifierStep simplifier = new PostSegmentationCodeSimplifierStep();
				simplifier.setParameters(p);
				driver.addStep(simplifier);
			} else {
				CodeSimplifierStep simplifier = new CodeSimplifierStep();
				simplifier.setParameters(p);
				driver.addStep(simplifier);
			}
		}

		// Filter events to raw document final step (using the XLIFF writer)
		final FilterEventsWriterStep fewStep = new FilterEventsWriterStep();

		try (XLIFFWriter writer = new XLIFFWriter();
				RawDocument originalDoc = new RawDocument(Util.toURI(originalPath), StandardCharsets.UTF_8.name(), source, target)) {

			fewStep.setFilterWriter(writer);
			final XLIFFWriterParameters params = writer.getParameters();		
			params.setPlaceholderMode(false);
			params.setIncludeAltTrans(true);
			params.setEscapeGt(true);
			params.setIncludeCodeAttrs(true);
			params.setCopySource(true);
			params.setIncludeIts(true);
			params.setIncludeNoTranslate(true);
			params.setToolId("okapi");
			params.setToolName("okapi-tests");
			params.setToolCompany("okapi");
			params.setToolVersion("M29");

			fewStep.setDocumentRoots(Util.getDirectoryName(originalPath));
			driver.addStep(fewStep);

			originalDoc.setFilterConfigId(filterConfig);

			driver.addBatchItem(originalDoc, new File(outputPath).toURI(), StandardCharsets.UTF_8.name());

			// Process
			driver.processBatch();
		}
	}

	public static void merge(final LocaleId source, final LocaleId target, final Boolean legacy, final String originalPath, final String xlfPath, final String outputPath, final String filterConfig, final String customConfigPath) throws URISyntaxException {
		final FilterConfigurationMapper mapper = new FilterConfigurationMapper();
		DefaultFilters.setMappings(mapper, false, true);
		if (customConfigPath != null) {
			mapper.setCustomConfigurationsDirectory(customConfigPath);
			mapper.addCustomConfiguration(filterConfig);
			// look for secondary filter config - only one per subdir
			final File secondary = IntegrationtestUtils.getSecondaryConfigFile(customConfigPath, filterConfig);
			if (secondary != null) {            	
				mapper.addCustomConfiguration(Util.getFilename(secondary.getName(), false));
			}            
			mapper.updateCustomConfigurations();
		}

		try (RawDocument originalDoc = new RawDocument(Util.toURI(originalPath), StandardCharsets.UTF_8.name(), source, target);
				RawDocument xlfDoc = new RawDocument(Util.toURI(xlfPath), StandardCharsets.UTF_8.name(), source, target)) {
			originalDoc.setFilterConfigId(filterConfig);

			final IPipelineDriver driver = new PipelineDriver();  
			driver.setFilterConfigurationMapper(mapper);
			final BatchItemContext bic = new BatchItemContext(
					xlfDoc,
					Util.toURI(outputPath), 
					StandardCharsets.UTF_8.name(), 
					originalDoc);
			driver.addBatchItem(bic);
			// Make sure that whitespace is preserved
			OriginalDocumentXliffMergerStep originalDocumentXliffMergerStep = new OriginalDocumentXliffMergerStep();
			net.sf.okapi.lib.merge.step.Parameters parameters = new net.sf.okapi.lib.merge.step.Parameters();
			parameters.setPreserveWhiteSpaceByDefault(true);
			originalDocumentXliffMergerStep.setParameters(parameters);
			driver.addStep(originalDocumentXliffMergerStep);
			driver.addStep(new RawDocumentWriterStep());
			driver.processBatch();
			driver.destroy();
		}
	}

	/**
	 * Assumes merge has been called to set source and target locales
	 */
	public static boolean compareOriginalWithTarget(final IFilter filter, final String originalfile, final String targetFile) {
		return compareOriginalWithTarget(filter, originalfile, targetFile, true, false, false, false);
	}

	/**
	 * Assumes merge has been called to set source and target locales
	 */
	public static boolean compareOriginalWithTarget(final IFilter filter, final String originalfile, final String targetFile, 
			final boolean includeSkeleton, final boolean ignoreSkelWhitespace, final boolean ignoreSegmentation, final boolean ignoreFragmentWhitespace) {
		try (RawDocument ord = new RawDocument(Util.toURI(originalfile), "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH);
				RawDocument trd = new RawDocument(Util.toURI(targetFile), "UTF-8", LocaleId.ENGLISH, LocaleId.FRENCH)) {
			final List<Event> o = IntegrationtestUtils.getEvents(filter, ord);
			final List<Event> t = IntegrationtestUtils.getEvents(filter, trd);
			final boolean r =  FilterTestDriver.compareEvents(o, t, includeSkeleton, ignoreSkelWhitespace, ignoreSegmentation, ignoreFragmentWhitespace);
			o.clear();
			t.clear();
			return r;
		}
	}

	/**
	 * Assumes merge has been called to set source and target locales
	 */
	public static boolean compareEvents(final List<Event> actual, final List<Event> expected, 
			final boolean includeSkeleton, final boolean ignoreSkelWhitespace, final boolean ignoreSegmentation, 
			final boolean ignoreFragmentWhitespace) {
		final boolean r =  FilterTestDriver.compareEvents(actual, expected, includeSkeleton, ignoreSkelWhitespace, 
				ignoreSegmentation, ignoreFragmentWhitespace);
		return r;
	}

	/**
	 * Compare two xml files and return their differences if any
	 * @param <T> The input type. Can be a string path, Stream or any number of input types
	 * 
	 * @param actual the generated file (normally merged)
	 * @param expected the original file
	 * @return the {@link Diff} object with the differences if any
	 * @throws ParserConfigurationException 
	 */
	public static <T> Diff compareXml(final T actual, final T expected) throws ParserConfigurationException {
		final DiffBuilder diff = DiffBuilder.compare(Input.from(expected)).withTest(Input.from(actual))
				.checkForSimilar()
				.ignoreComments()
				.ignoreWhitespace()
				.normalizeWhitespace()
				.withAttributeFilter(new XmlStandardAttributesToIgnore())
				.withDifferenceEvaluator(DifferenceEvaluators.chain(
						DifferenceEvaluators.Default,
						DifferenceEvaluators.ignorePrologDifferences()));
		return diff.build();
	}
}
