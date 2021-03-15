package net.sf.okapi.connectors.microsoft;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class GetTranslationsArrayRequest {
	private String category;
	private List<String> texts;
	private String srcLang;
	private String trgLang;
	private int maxHits;

	GetTranslationsArrayRequest(List<String> texts, String srcLang, String trgLang, int maxHits, String category) {
		this.texts = texts;
		this.category = category;
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		this.maxHits = maxHits;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null || !(o instanceof GetTranslationsArrayRequest)) return false;
		GetTranslationsArrayRequest r = (GetTranslationsArrayRequest)o;
		return Objects.equals(category, r.category) &&
			   Objects.equals(texts, r.texts) &&
			   Objects.equals(srcLang, r.srcLang) &&
			   Objects.equals(trgLang, r.trgLang) &&
			   Objects.equals(maxHits, r.maxHits);
	}

	@Override
	public int hashCode() {
		return Objects.hash(category, texts, srcLang, trgLang, maxHits);
	}
	
	public String toJSON() throws JsonProcessingException {
	    ObjectMapper objectMapper = new ObjectMapper();
        List<TranslateRequest> translateRequests = new ArrayList<>();
        for (String text : texts) {
            translateRequests.add(new TranslateRequest(text));
        }
		return objectMapper.writeValueAsString(translateRequests);
	}

	public String toString() {
		try {
			return toJSON();
		} catch (JsonProcessingException e) {
			return "(deserialization error):" + e.toString();
		}
	}
	public int getNumRequests() {
		return texts.size();
	}

	public String getText(int index) {
		return texts.get(index);
	}
}
