package net.sf.okapi.lib.xliff2.validation;

import java.text.Normalizer;
import java.text.Normalizer.Form;

import net.sf.okapi.lib.xliff2.validation.Rule.Normalization;
import net.sf.okapi.lib.xliff2.validation.Rule.Type;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class RuleTest {
	
	@Test
	public void testSimple () {
		Rule r = new Rule("startsWith", "a");
		r.prepare();
		assertSame(r.getType(), Type.STARTSWITH);
		assertEquals("a", r.getData());
		assertTrue(r.isCaseSensitive());
		assertTrue(r.isEnabled());
		assertFalse(r.getExistsInSource());
		assertEquals(Normalization.NFC, r.getNormalization());
		assertEquals("a", r.getEffectiveData());
		assertFalse(r.isInherited());
	}

	@Test
	public void testCopyConstructor () {
		Rule r = new Rule("startsWith", "b");
		r.setCaseSensitive(false);
		r.setEnabled(false);
		r.setExistsInSource(true);
		r.setNormalization(Normalization.NFD);
		r.setOccurs(3);
		r.setInherited(true);
		r.prepare();
		Rule r2 = new Rule(r);
		assertNotSame(r, r2);
		assertFalse(r2.isCaseSensitive());
		assertFalse(r2.isEnabled());
		assertTrue(r2.getExistsInSource());
		assertEquals(Normalization.NFD, r2.getNormalization());
		assertEquals(3, r2.getOccurs());
		assertEquals("b", r2.getEffectiveData());
		assertEquals("b", r2.getData());
		assertTrue(r2.isInherited());
	}

	@Test
	public void testNormalizationNFCCaseSensitive () {
		String original = "\u00C4\uFB03n";
		String expected = Normalizer.normalize(original, Form.NFC);
		Rule r = new Rule("isPresent", original);
		r.prepare();
		assertEquals(expected, r.getEffectiveData());
	}

	@Test
	public void testNormalizationNFDCaseSensitive () {
		String original = "\u00C4\uFB03n";
		String expected = Normalizer.normalize(original, Form.NFD);
		Rule r = new Rule("isPresent", original);
		r.setNormalization(Normalization.NFD);
		r.prepare();
		assertEquals(expected, r.getEffectiveData());
	}

	@Test
	public void testNormalizationNoneCaseSensitive () {
		String original = "\u00C4\uFB03n";
		String expected = original;
		Rule r = new Rule("isPresent", original);
		r.setNormalization(Normalization.NONE);
		r.prepare();
		assertEquals(expected, r.getEffectiveData());
	}

	@Test
	public void testNormalizationNFCNotCaseSensitive () {
		String original = "\u00C4\uFB03n";
		String expected = Normalizer.normalize(original.toLowerCase(), Form.NFC);
		Rule r = new Rule("isPresent", original);
		r.setCaseSensitive(false);
		r.prepare();
		assertEquals(expected, r.getEffectiveData());
	}

	@Test
	public void testNormalizationNFDNotCaseSensitive () {
		String original = "\u00C4\uFB03n";
		String expected = Normalizer.normalize(original.toLowerCase(), Form.NFD);
		Rule r = new Rule("isPresent", original);
		r.setCaseSensitive(false);
		r.setNormalization(Normalization.NFD);
		r.prepare();
		assertEquals(expected, r.getEffectiveData());
	}

	@Test
	public void testNormalizationNoneNotCaseSensitive () {
		String original = "\u00C4\uFB03n";
		String expected = original.toLowerCase();
		Rule r = new Rule("isPresent", original);
		r.setCaseSensitive(false);
		r.setNormalization(Normalization.NONE);
		r.prepare();
		assertEquals(expected, r.getEffectiveData());
	}

}
