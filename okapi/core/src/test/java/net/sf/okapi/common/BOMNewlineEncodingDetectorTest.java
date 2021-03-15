package net.sf.okapi.common;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** 
 * Junt4 Test class for {@link BOMNewlineEncodingDetector}
 */
@RunWith(JUnit4.class)
public class BOMNewlineEncodingDetectorTest {
	private static final String CR_NEWLINES = "this \ris  atest with newlines";
	private static final String LF_NEWLINES = "this \nis  atest with newlines";
	private static final String CRLF_NEWLINES = "this \r\nis  atest with newlines";
	
	private static final String DEFINITIVE = "\ufffe has a BOM";
	private static final String NON_DEFINITIVE = "does not have a BOM";
	
	private static final byte[] UTF_8_BOM = new byte[]{(byte)0xEF,(byte)0xBB,(byte)0xBF};	  
	private static final byte[] UTF_7_BOM = new byte[]{(byte)0x2B,(byte)0x2F,(byte)0x76, (byte)0x38};
	
	@Test
	public void staticGetNewlineType() {
		assertEquals(BOMNewlineEncodingDetector.NewlineType.CR, BOMNewlineEncodingDetector.getNewlineType(CR_NEWLINES));
		assertEquals(BOMNewlineEncodingDetector.NewlineType.LF, BOMNewlineEncodingDetector.getNewlineType(LF_NEWLINES));
		assertEquals(BOMNewlineEncodingDetector.NewlineType.CRLF, BOMNewlineEncodingDetector.getNewlineType(CRLF_NEWLINES));
	}
	
	@Test
	public void getNewlineType() {
		InputStream is = new ByteArrayInputStream(CR_NEWLINES.getBytes(StandardCharsets.UTF_8));
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_8);
		assertEquals(BOMNewlineEncodingDetector.NewlineType.CR, detector.getNewlineType());
		
		is = new ByteArrayInputStream(LF_NEWLINES.getBytes(StandardCharsets.UTF_8));
		detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_8);
		assertEquals(BOMNewlineEncodingDetector.NewlineType.LF, detector.getNewlineType());
		
		is = new ByteArrayInputStream(CRLF_NEWLINES.getBytes(StandardCharsets.UTF_8));
		detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_8);
		assertEquals(BOMNewlineEncodingDetector.NewlineType.CRLF, detector.getNewlineType());
	}
	
	@Test
	public void isDefinitve() {
		InputStream is = new ByteArrayInputStream(DEFINITIVE.getBytes(StandardCharsets.UTF_16LE));
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_16LE);
		assertTrue(detector.isDefinitive());
		
		is = new ByteArrayInputStream(NON_DEFINITIVE.getBytes(StandardCharsets.UTF_8));
		detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_8);
		detector.detectBom();
		assertFalse(detector.isDefinitive());
	}

	@Test
	public void hasBom() {
		InputStream is = new ByteArrayInputStream(DEFINITIVE.getBytes(StandardCharsets.UTF_16));
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_16);
		detector.detectBom();
		assertTrue(detector.hasBom());
		
		is = new ByteArrayInputStream(NON_DEFINITIVE.getBytes(StandardCharsets.UTF_8));
		detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_8);
		detector.detectBom();
		assertFalse(detector.hasBom());
	}
	
	@Test
	public void hasUtf8Bom() {
		InputStream is = new ByteArrayInputStream(UTF_8_BOM);
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(is, StandardCharsets.UTF_8);
		detector.detectBom();
		assertTrue(detector.hasBom());
		assertTrue(detector.hasUtf8Bom());
	}
	
	@Test
	public void hasUtf7Bom() {
		InputStream is = new ByteArrayInputStream(UTF_7_BOM);
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(is, "UTF-7");
		detector.detectBom();
		assertTrue(detector.hasBom());
		assertTrue(detector.hasUtf7Bom());
	}
	
	@Test
	public void removeBom() {
		InputStream is = new ByteArrayInputStream(DEFINITIVE.getBytes(StandardCharsets.UTF_16LE));
		BOMNewlineEncodingDetector detector1 = new BOMNewlineEncodingDetector(is);
		detector1.detectAndRemoveBom();
		
		BOMNewlineEncodingDetector detector2 = new BOMNewlineEncodingDetector(is);
		detector2.detectBom();
		assertFalse(detector2.hasBom());		
	}
	
	@Test
	public void resettingInvalidMarkException() {
		BOMNewlineEncodingDetector detector = new BOMNewlineEncodingDetector(
				FileLocation.fromClass(getClass()).in("/strings.xml").asInputStream());

		detector.detectBom();
		assertFalse(detector.hasBom());
		assertEquals("ISO-8859-1", detector.getEncoding());
		assertEquals(BOMNewlineEncodingDetector.NewlineType.CRLF, detector.getNewlineType());
		
		detector = new BOMNewlineEncodingDetector(
				FileLocation.fromClass(getClass()).in("/Test.resx").asInputStream());
		detector.detectBom();
		assertTrue(detector.hasBom());
		assertEquals("UTF-8", detector.getEncoding());
		assertEquals(BOMNewlineEncodingDetector.NewlineType.CRLF, detector.getNewlineType());
	}	
}
