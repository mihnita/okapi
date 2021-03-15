package net.sf.okapi.connectors.microsoft;

import java.util.List;

public interface MicrosoftMTAPI {

    /**
     * Call the getTranslations() API method.
     * @return API responses, or null if the API call fails
     */
	List<TranslationResponse> getTranslations(String query, String srcLang, String trgLang, int maxHits, int threshold);

    /**
     * Call the getTranslationsArray() API method.
     * @return API responses, or null if the API call fails
     */
	List<List<TranslationResponse>> getTranslationsArray(GetTranslationsArrayRequest request, String srcLang,
			String trgLang, int maxHits, int threshold);
	
	/**
	 * Gets the base URL in use, such as https://api.cognitive.microsofttranslator.com.
	 * @return String representation of the base URL of the service in use
	 */
	String getBaseURL();
	
	/**
	 * @return maximum total character count per call to {@link #getTranslationsArray(GetTranslationsArrayRequest, String, String, int, int)}
	 */
	int maxCharCount();
	
	/**
	 * @return maximum count of text per call to {@link #getTranslationsArray(GetTranslationsArrayRequest, String, String, int, int)}
	 */
	int maxTextCount();
}