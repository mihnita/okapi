/*===========================================================================
  Copyright (C) 2017-2018 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.filters.markdown;

import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.filters.InlineCodeFinder;

public class Parameters extends StringParameters {
    private static final String HTML_SUBFILTER_CONFIG_PATH = "htmlSubfilter";
    private static final String YAML_SUBFILTER_CONFIG_PATH = "yamlSubfilter";

    private static final String USECODEFINDER = "useCodeFinder";
    private static final String CODEFINDERRULES = "codeFinderRules";
    private static final String TRANSLATE_URLS = "translateUrls";
    private static final String TRANSLATE_CODE_BLOCKS = "translateCodeBlocks";
    private static final String TRANSLATE_INLINE_CODE_BLOCKS = "translateInlineCodeBlocks";
    private static final String TRANSLATE_HEADER_METADATA = "translateHeaderMetadata";
    private static final String TRANSLATE_IMAGE_ALTTEXT = "translateImageAltText";
    private static final String URL_TO_TRANSLATE_PATTERN = "urlToTranslatePattern";
    private static final String NON_TRANSLATE_BLOCKS ="nonTranslateBlocks";

    private InlineCodeFinder codeFinder; // Initialized in reset()

    public Parameters() {
	reset();
    }
    
    /**
     * The configuration that the HTML subfilter uses, if set.
     * @return The configuration file path, or null if Markdown filter's default HTML configuration is used.
     */
    public String getHtmlSubfilter() {
        return getString(HTML_SUBFILTER_CONFIG_PATH);
    }

    /**
     * Uses the user-supplied HTML sub-filter configuration rather than Markdown's default.
     * @param htmlSubfilter The path of the configuration yml file which typically has the .fprm suffix.
     */
    public void setHtmlSubfilter(String htmlSubfilter) {
        setString(HTML_SUBFILTER_CONFIG_PATH, htmlSubfilter);
    }
 
    public boolean getUseCodeFinder() {
        return getBoolean(USECODEFINDER);
    }
    
    public void setUseCodeFinder(boolean useCodeFinder) {
        setBoolean(USECODEFINDER, useCodeFinder);
    }

    public boolean getTranslateUrls() {
        return getBoolean(TRANSLATE_URLS);
    }

    public void setTranslateUrls(boolean translateUrls) {
        setBoolean(TRANSLATE_URLS, translateUrls);
    }

    public String getUrlToTranslatePattern() {
    	return getString(URL_TO_TRANSLATE_PATTERN);
    }

    public void setUrlToTranslatePattern(String urlToTranslatePattern) {
    	setString(URL_TO_TRANSLATE_PATTERN, urlToTranslatePattern);
    }
    
    public boolean getTranslateCodeBlocks() {
        return getBoolean(TRANSLATE_CODE_BLOCKS);
    }

    public void setTranslateCodeBlocks(boolean translateCodeBlocks) {
        setBoolean(TRANSLATE_CODE_BLOCKS, translateCodeBlocks);
    }

    public boolean getTranslateInlineCodeBlocks() {
        return getBoolean(TRANSLATE_INLINE_CODE_BLOCKS);
    }

    public void setTranslateInlineCodeBlocks(boolean translateInlineCodeBlocks) {
        setBoolean(TRANSLATE_INLINE_CODE_BLOCKS, translateInlineCodeBlocks);
    }

    public boolean getTranslateHeaderMetadata() {
        return getBoolean(TRANSLATE_HEADER_METADATA);
    }

    public void setTranslateHeaderMetadata(boolean translateHeaderMetadata) {
        setBoolean(TRANSLATE_HEADER_METADATA, translateHeaderMetadata);
    }

    public boolean getTranslateImageAltText() {
        return getBoolean(TRANSLATE_IMAGE_ALTTEXT);
    }

    public void setTranslateImageAltText(boolean translateImageAltText) {
        setBoolean(TRANSLATE_IMAGE_ALTTEXT, translateImageAltText);
    }

    public InlineCodeFinder getCodeFinder() {
        return codeFinder;
    }
    
    public String getNonTranslateBlocks() {
        return getString(NON_TRANSLATE_BLOCKS);
    }

    public void setNonTranslateBlocks(String nonTranslatableBlocks) {
        setString(NON_TRANSLATE_BLOCKS, nonTranslatableBlocks);
    }

    public String getYamlSubfilter() {
        return getString(YAML_SUBFILTER_CONFIG_PATH);
    }

    public void setYamlSubfilter(String yamlSubfilter) {
        setString(YAML_SUBFILTER_CONFIG_PATH, yamlSubfilter);
    }


    @Override
    public void reset() {
        super.reset();
        setHtmlSubfilter(null);
        setUseCodeFinder(false);
        setTranslateUrls(false);
        setUrlToTranslatePattern(".+");
        setTranslateCodeBlocks(true);
        setTranslateInlineCodeBlocks(true);
        setTranslateHeaderMetadata(false);
        setTranslateImageAltText(true);
        setYamlSubfilter(null);

        codeFinder = new InlineCodeFinder();
        codeFinder.setSample("{{#test}} handle bar test {{/test}}\n{{stand-alone handle bar}}\n");
        codeFinder.setUseAllRulesWhenTesting(true);
        codeFinder.addRule("\\{\\{[^}]+\\}\\}");
    }

    public void fromString (String data) {
        super.fromString(data);
        codeFinder.fromString(buffer.getGroup(CODEFINDERRULES, ""));
    }

    @Override
    public String toString () {
        buffer.setGroup(CODEFINDERRULES, codeFinder.toString());
        return super.toString();
    }

}
