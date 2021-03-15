package net.sf.okapi.connectors.microsoft;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestGetTranslationsArrayRequest {
	@Test
	public void testJsonRequestBody() throws Exception {
		List<String> texts = new ArrayList<>();
		texts.add("string1");
		texts.add("string2");
		texts.add("string3");
		GetTranslationsArrayRequest data = new GetTranslationsArrayRequest(texts, "en", "fr", 1, "test-category");
		assertEquals("[{\"text\":\"string1\"},{\"text\":\"string2\"},{\"text\":\"string3\"}]", data.toJSON());
	}
}
