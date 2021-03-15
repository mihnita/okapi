package net.sf.okapi.lib.xliff2.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("static-method")
@RunWith(JUnit4.class)
public class LanguageDataTest {
	private static LanguageData langData;

	@BeforeClass
	public static void onlyOnce() {
		try {
			langData = new LanguageData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetLanguages() {
		String[] data = langData.getLanguages();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "fr")); // French
	}

	@Test
	public void testGetExtlangs() {
		String[] data = langData.getExtlangs();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "yue")); // Yue Chinese
	}

	@Test
	public void testGetPrefixesByVariant() {
		String[] sr = { "sr" };
		String[] srLatn = { "sr", "latn" };
		String[] srCyrl = { "sr", "cyrl" };

		String[][][] data = langData.getPrefixesByVariant();
		assertNotNull(data);
		assertNotEquals(0, data.length);

		int found = 0;
		for (int i = 0; i < data.length; i++) {
			String[][] data1 = data[i];
			for (int j = 0; j < data1.length; j++) {
				String[] data2 = data1[j];
				if (Arrays.equals(sr, data2))
					found |= 1;
				if (Arrays.equals(srCyrl, data2))
					found |= 2;
				if (Arrays.equals(srLatn, data2))
					found |= 4;
			}
		}
		assertEquals(1 | 2 | 4, found);
	}

	@Test
	public void testGetPrefixByExtlang() {
		int[] data = langData.getPrefixByExtlang();
		assertNotNull(data);
		assertNotEquals(0, data.length);
	}

	@Test
	public void testGetRegions() {
		String[] data = langData.getRegions();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "419")); // Latin America
		assertTrue(0 <= Arrays.binarySearch(data, "de")); // Germany
	}

	@Test
	public void testGetScripts() {
		String[] data = langData.getScripts();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "hant")); // Traditional Chinese
	}

	@Test
	public void testGetSuppressedScriptByLanguage() {
		int[] data = langData.getSuppressedScriptByLanguage();
		assertNotNull(data);
		assertNotEquals(0, data.length);
	}

	@Test
	public void testGetVariants() {
		String[] data = langData.getVariants();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "rozaj"));
	}

	@Test
	public void testGetDeprecated() {
		String[] data = langData.getDeprecated();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "i-klingon"));
		assertTrue(0 <= Arrays.binarySearch(data, "no-bok"));
		assertTrue(0 <= Arrays.binarySearch(data, "zh-cmn"));
	}

	@Test
	public void testGetPreferredValueByLanguageMap() {
		Map<String, String> data = langData.getPreferredValueByLanguageMap();
		assertNotNull(data);
		assertNotEquals(0, data.size());
		assertEquals("tlh", data.get("i-klingon"));
		assertEquals("nb", data.get("no-bok"));
		assertEquals("yue", data.get("zh-yue"));
		assertEquals("he", data.get("iw"));
		assertEquals("id", data.get("in"));
	}

	@Test
	public void testGetGrandfathered() {
		String[] data = langData.getGrandfathered();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "i-klingon"));
		assertTrue(0 <= Arrays.binarySearch(data, "no-bok"));
	}

	@Test
	public void testGetRedundant() {
		String[] data = langData.getRedundant();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "sr-latn"));
		assertTrue(0 <= Arrays.binarySearch(data, "zh-hans"));
	}

	@Test
	public void testGetDeprecatedLang() {
		String[] data = langData.getDeprecatedLang();
		assertNotNull(data);
		assertNotEquals(0, data.length);
		assertTrue(0 <= Arrays.binarySearch(data, "iw"));
		assertTrue(0 <= Arrays.binarySearch(data, "in"));
		assertTrue(0 <= Arrays.binarySearch(data, "mo"));
	}
}