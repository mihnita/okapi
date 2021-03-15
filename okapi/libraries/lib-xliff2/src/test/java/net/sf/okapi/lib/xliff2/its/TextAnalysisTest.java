package net.sf.okapi.lib.xliff2.its;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TextAnalysisTest {

	@Test (expected = XLIFFException.class)
	public void testTextAnalysisMissingIdent () {
		// Defines taSource but not taIdent
		TextAnalysis ta = new TextAnalysis();
		ta.setTaSource("source");
		ta.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testTextAnalysisMissingSource () {
		// Defines taIdent but not taSource
		TextAnalysis ta = new TextAnalysis();
		ta.setTaClassRef("classref");
		ta.setTaIdent("ident");
		ta.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testTextAnalysisIdentRefNotAllowed () {
		// taSource/taIdent defined: cannot use taIdentRef
		TextAnalysis ta = new TextAnalysis();
		ta.setTaSource("source");
		ta.setTaIdent("ident");
		ta.setTaIdentRef("identref");
		ta.validate();
	}

	@Test (expected = XLIFFException.class)
	public void testTextAnalysisMissingAnnotatorRef () {
		// An annotator reference must be set if taConfidence is defined
		TextAnalysis ta = new TextAnalysis();
		ta.setTaClassRef("classref");
		ta.setTaConfidence(0.5);
		ta.validate();
	}

	@Test (expected = InvalidParameterException.class)
	public void testTextAnalysisInvalidConfidence () {
		// Bad confidence value
		TextAnalysis ta = new TextAnalysis();
		ta.setTaClassRef("classref");
		ta.setTaConfidence(50.5);
	}

	@Test
	public void testTextAnalysisOK () {
		// OK combinations
		TextAnalysis ta = new TextAnalysis();
		ta.setTaClassRef("classref");
		ta.validate();
		ta.setTaSource("source");
		ta.setTaIdent("ident");
		ta.validate();
		ta.setTaConfidence(0.6);
		ta.setAnnotatorRef("myTool");
		ta.validate();
	}
	
	@Test
	public void testTextAnalysisCanNullifyConfidence () {
		// Nullifying confidence is OK
		TextAnalysis ta = new TextAnalysis();
		ta.setTaConfidence(0.6);
		ta.setTaConfidence(null);
	}
	
}
