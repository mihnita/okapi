package net.sf.okapi.lib.xliff2.processor;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.StringWriter;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XLIFFProcessorTest {

	private FileLocation root = FileLocation.fromClass(this.getClass());

	static class SetCanResegment extends DefaultEventHandler {
		@Override
		public Event handleUnit (Event event) {
			Unit unit = event.getUnit();
			// Set canResegment to false on the unit
			unit.setCanResegment(false);
			return event;
		}
	}
	
	static class AddText extends DefaultEventHandler {
		@Override
		public Event handleUnit (Event event) {
			Unit unit = event.getUnit();
			Fragment fragment = unit.getPart(unit.getPartCount()-1).getSource();
			fragment.setCodedText(fragment.getCodedText()+"_NEW");
			return event;
		}
	}
	
	@Test (expected = InvalidParameterException.class)
	public void testSameInputOutput () {
		XLIFFProcessor processor = new XLIFFProcessor();
		File outFile = root.out("example.out1.xlf").asFile();
		processor.run(outFile, outFile);
	}

	@Test
	public void testNoOutput () {
		// Create the processor and the event handler
		XLIFFProcessor processor = new XLIFFProcessor();
		processor.setHandler(new SetCanResegment());
		// Process the file
		File inputFile = root.in("/example.xlf").asFile();
		processor.run(inputFile, null);
		// No error should occur with a null output
		// Do it again, this time after setting the output differently
		processor.setInput(inputFile);
		processor.setOutput((StringWriter)null);
		processor.run();
	}
	
	@Test
	public void testSingleHandler () {
		// Create the processor and the event handler
		XLIFFProcessor processor = new XLIFFProcessor();
		processor.setHandler(new SetCanResegment());
		// make sure any existing output file is deleted
		File outFile = root.out("/example.out1.xlf").asFile();
		outFile.delete();
		// Perform the modification
		processor.run(root.in("/example.xlf").asFile(), outFile);
		// Check the expected output
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(outFile, XLIFFReader.VALIDATION_MAXIMAL);
		Unit unit = doc.getUnitNode("f1", "1").get();
		assertFalse(unit.getCanResegment());
	}
	
	@Test
	public void testTwoHandlers () {
		// Create the processor and the event handler
		XLIFFProcessor processor = new XLIFFProcessor();
		processor.setHandler(new SetCanResegment());
		processor.add(new AddText());
		// make sure any existing output file is deleted
		File outFile = root.out("/example.out2.xlf").asFile();
		outFile.delete();
		// Perform the modification
		processor.run(root.in("/example.xlf").asFile(), outFile);
		// Check the expected output
		XLIFFDocument doc = new XLIFFDocument();
		doc.load(outFile, XLIFFReader.VALIDATION_MAXIMAL);
		Unit unit = doc.getUnitNode("f1", "1").get();
		assertFalse(unit.getCanResegment());
	}
	
}
