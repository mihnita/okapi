package net.sf.okapi.steps.xliffsplitter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.sf.okapi.common.BOMAwareInputStream;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class XliffJoinerTest {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private Pipeline pipeline;
	private Path inputRoot;
	private Path outputRoot;
	private XliffJoinerStep joiner;
	
	@Before
	public void setUp() throws Exception {
		inputRoot = FileLocation.fromClass(XliffJoinerTest.class).in("").asPath();
		outputRoot = FileLocation.fromClass(XliffJoinerTest.class).out("").asPath();

		// create pipeline
		pipeline = new Pipeline();
		
		// add filter step
		joiner = new XliffJoinerStep();

		pipeline.addStep(joiner);				
	}
	
	@After
	public void tearDown() throws Exception {
		pipeline.destroy();
	}

	@Test
	public void joinXliffWithMultipleInputFiles() throws Exception {
		String[] fileList = initialize("to_join", String::compareTo);
		runPipeline(fileList);
		compareWithGold("to_join", "tasks_Test_SDL_XLIFF_18961_es_ES_xliff_CONCAT.xlf",
				"tasks_Test_SDL_XLIFF_18961_es_ES_xliff_singleFile_CONCAT.gold.xlf");
	}

	@Test
	public void joinXliffWithSingleFileElement() throws Exception {
		String[] fileList = initialize("to_join_large_file", String::compareTo);
		runPipeline(fileList);
		compareWithGold("to_join_large_file", "400.html_CONCAT.xlf", "400.html_CONCAT.gold.xlf");
	}

	@Test
	public void joinXliffThatWasTriviallySplitIntoOnePart() throws Exception {
		String[] fileList = initialize("to_join_singlefile", String::compareTo);
		runPipeline(fileList);
		compareWithGold("to_join_singlefile", "input1_CONCAT.xlf", "input1.xlf");
	}

	@Test
	public void joinXliffContainingMultipleFileElementsSplitIntoMultipleParts() throws Exception {
		String[] fileList = initialize("to_join_multiple_files", String::compareTo);
		runPipeline(fileList);
		compareWithGold("to_join_multiple_files", "multiple_files_CONCAT.xlf", "multiple_files.xlf");
	}

	private void runPipeline(String[] fileList) throws IOException {
		pipeline.startBatch();
		for (String file : fileList) {
			Path inputFile = inputRoot.resolve(file);
			Path outputFile = outputRoot.resolve(file);
			Files.createDirectories(outputFile.getParent());
			joiner.setOutputURI(outputFile.toUri());
			pipeline.process(new RawDocument(inputFile.toUri(), "UTF-8", LocaleId.ENGLISH));
		}
		pipeline.endBatch();
	}

	private void compareWithGold(String directory, String outputFileName, String goldFileName) throws Exception {
		File gold = inputRoot.resolve(goldFileName).toFile();
		File out = outputRoot.resolve(directory).resolve(outputFileName).toFile();
		BOMAwareInputStream goldS = new BOMAwareInputStream(new FileInputStream(gold), "UTF-8");
		goldS.detectEncoding();
		BOMAwareInputStream outS = new BOMAwareInputStream(new FileInputStream(out), "UTF-8");
		outS.detectEncoding();
		try (Reader goldR = new InputStreamReader(goldS, StandardCharsets.UTF_8);
			 Reader outR = new InputStreamReader(outS, StandardCharsets.UTF_8)) {
			compareXML(goldR, outR, gold.getAbsolutePath(), out.getAbsolutePath());
		}
	}

	private void compareXML(Reader goldR, Reader outR, String goldName, String outName) throws Exception {
		final Diff diff = DiffBuilder.compare(Input.fromReader(goldR))
			.withTest(Input.fromReader(outR))
			.checkForIdentical()
			.build();
		if (diff.hasDifferences()) {
			logger.warn("Differences between {} and {}:", goldName, outName);
			for (Difference d : diff.getDifferences()) {
				logger.warn("- {}", d.toString());
			}
			fail();
		}
	}

	private String[] initialize(String directory, Comparator<String> c) throws Exception {
		Path testDir = inputRoot.resolve(directory);

		String[] testFileList = getTestFiles(testDir, ".xlf").toArray(new String[0]);
		Arrays.sort(testFileList, c);
		return testFileList;
	}

	private List<String> getTestFiles(Path testDir, String suffix) {
		File dir = testDir.toFile();
		FilenameFilter filter = (dir1, name) -> name.endsWith(suffix);
		return Arrays.stream(dir.listFiles(filter))
				.map(f -> testDir.getFileName() + File.separator + f.getName())
				.collect(Collectors.toList());
	}
}
