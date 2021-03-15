package net.sf.okapi.steps.ttxsplitter;

import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.FileLocation;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@RunWith(JUnit4.class)
public class TTXSplitterTest {
	
	private TTXSplitter splitter;
	private FileLocation root;

	@Before
	public void setUp() {	
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testSplitThenJoinSegmented ()
		throws MalformedURLException, IOException, SAXException
	{
		splitThenJoin("/Test01.html");
	}

	@Test
	public void testSplitThenJoinNotSegmented ()
		throws MalformedURLException, IOException, SAXException
	{
		splitThenJoin("/Test02_noseg.html");
	}

	private void splitThenJoin (String basename) 
		throws MalformedURLException, IOException, SAXException
	{
		// We copy the input file to the output folder, so that the split files can be created
		// in the same place. It is bad to "litter" the input folders with output files.
		// For TTXSplitter there is no way to set the output folder to be different than the input. 
		// Issue: https://bitbucket.org/okapiframework/okapi/issues/757
		File oriFile = root.out(basename + ".ttx").asFile();
		Files.copy(root.in(basename + ".ttx").asPath(), oriFile.toPath(),
				StandardCopyOption.REPLACE_EXISTING);

		File joinedFile = root.out(basename + "_joined.ttx").asFile();
		joinedFile.delete();
		File part001File = root.out(basename + "_part001.ttx").asFile();
		part001File.delete();
		File part002File = root.out(basename + "_part002.ttx").asFile();
		part002File.delete();

		TTXSplitterParameters params = new TTXSplitterParameters();
		splitter = new TTXSplitter(params);
		splitter.split(oriFile.toURI());

		// Check out (and prepare the join)
		List<URI> inputList = new ArrayList<>();
		assertTrue(part001File.exists());
		inputList.add(part001File.toURI());
		assertTrue(part002File.exists());
		inputList.add(part002File.toURI());
		
		TTXJoinerParameters params2 = new TTXJoinerParameters();
		TTXJoiner joiner = new TTXJoiner(params2);
		joiner.process(inputList);

		InputSource original = new InputSource(new BufferedInputStream(oriFile.toURI().toURL().openStream()));
		InputSource output = new InputSource(new BufferedInputStream(joinedFile.toURI().toURL().openStream()));
		
		XMLAssert.assertXMLEqual(original, output);
		
	}

}
