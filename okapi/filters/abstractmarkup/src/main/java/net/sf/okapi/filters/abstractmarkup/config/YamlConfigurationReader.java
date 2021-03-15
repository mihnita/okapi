/*===========================================================================
  Copyright (C) 2008 Jim Hargrave
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

package net.sf.okapi.filters.abstractmarkup.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.okapi.common.exceptions.OkapiException;

import org.yaml.snakeyaml.Yaml;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class YamlConfigurationReader implements AutoCloseable {
	private static final String REGEX_META_CHARS_REGEX = "[\\(\\[\\{\\^\\$\\|\\]\\}\\)\\?\\*\\+]+";
	private static final Pattern REGEX_META_CHARS_PATTERN = Pattern.compile(REGEX_META_CHARS_REGEX);

	private boolean preserveWhitespace;
	private Yaml yaml;
	private Map<String, Object> config;
	private Map<String, Object> elementRules;
	private Map<String, Object> attributeRules;
	private Map<String, Object> elementRegexRules;
	private Map<String, Pattern> elementCompiledRegexRules;
	private Map<String, Object> attributeRegexRules;
	private Map<String, Pattern> attributeCompiledRegexRules;
	private InputStreamReader reader;

	public boolean isPreserveWhitespace() {
		return preserveWhitespace;
	}

	public void setPreserveWhitespace(boolean preserveWhitespace) {
		this.preserveWhitespace = preserveWhitespace;
	}

	/**
	 * Default Tagged Configuration
	 */
	public YamlConfigurationReader() {
		yaml = new Yaml();
		config = yaml.load("collapse_whitespace: false\nassumeWellformed: true");
		initialize();
	}

	public YamlConfigurationReader(URL configurationPathAsResource) {
		try {
			reader = new InputStreamReader(configurationPathAsResource.openStream(), StandardCharsets.UTF_8);
			yaml = new Yaml();
			config = yaml.load(reader);
			initialize();
		} catch (IOException e) {
			throw new OkapiException(e);
		}
	}

	public YamlConfigurationReader(File configurationFile) {
		try {
			reader = new InputStreamReader(new FileInputStream(configurationFile), StandardCharsets.UTF_8);
			yaml = new Yaml();
			config = yaml.load(reader);
			initialize();
		} catch (FileNotFoundException e) {
			throw new OkapiException(e);
		}
	}

	public YamlConfigurationReader(String configurationScript) {
		yaml = new Yaml();
		config = yaml.load(configurationScript);
		initialize();
	}
	
	protected void initialize() {
		elementRules = new LinkedHashMap<>();
		attributeRules = new LinkedHashMap<>();
		Map<String, Object> er = (Map<String, Object>) config.get("elements");
		Map<String, Object> ar = (Map<String, Object>) config.get("attributes");
		
		if (er != null) {
			elementRules = er;
		}
		if (ar != null) {
			attributeRules = ar;
		}
		
		elementRegexRules = new LinkedHashMap<>();
		attributeRegexRules = new LinkedHashMap<>();
		findRegexRules();
		compileRegexRules();
	}

	@Override
	public String toString() {
		// FIXME: If rules are added after the fact this is not up to date
		return yaml.dump(config);
	}

	/**
	 * Find element or attribute rules
	 */
	public List<Map> getRules(String ruleName) {
		List<Map> rules = new LinkedList<>();
		Map rule = getElementRule(ruleName);
		if (rule != null) {
			rules.add(rule);
		}
		rule = getAttributeRule(ruleName);
		if (rule != null) {
			rules.add(rule);
		}
		
		return rules;
	}

	/*
	 * Find element rules only (including regex)
	 */
	public Map getElementRule(String ruleName) {
		Map rule = (Map)elementRules.get(ruleName);

		// check our element regex patterns
		if (rule == null && !elementRegexRules.isEmpty()) {
			for (Map.Entry<String, Object> e : elementRegexRules.entrySet()) {
				Matcher m = elementCompiledRegexRules.get(e.getKey()).matcher(ruleName);
				if (m.matches()) {
					return (Map) e.getValue();
				}
			}
		}
		return rule;
	}

	/**
	 * Non regex element rules only
	 * @param ruleName rule name (aka tag name)
	 * @return true if there is a matched non-regex rule
	 */
	public Map getNonRegexElementRule(String ruleName) {
		return (Map)elementRules.get(ruleName);
	}
	
	/*
	 * Find regex element rules only
	 */
	public Map getRegexElementRule(String ruleName) {
		Map rule = null;
		// check our element regex patterns
		if (!elementRegexRules.isEmpty()) {
			for (Map.Entry<String, Object> e : elementRegexRules.entrySet()) {
				Matcher m = elementCompiledRegexRules.get(e.getKey()).matcher(ruleName);
				if (m.matches()) {
					return (Map) e.getValue();
				}
			}
		}
		return rule;
	}

	/*
	 * Find attribute rules only (including regex)
	 */
	public Map getAttributeRule(String ruleName) {
		Map rule = (Map)attributeRules.get(ruleName);

		// check our element regex patterns
		if (rule == null && !attributeRegexRules.isEmpty()) {
			for (Map.Entry<String, Object> e : attributeRegexRules.entrySet()) {
				Matcher m = attributeCompiledRegexRules.get(e.getKey()).matcher(ruleName);
				if (m.matches()) {
					return (Map) e.getValue();
				}
			}
		}
		return rule;
	}

	/*
	 * Find attribute rules only (including regex)
	 */
	public Map getRegexAttributeRule(String ruleName) {
		Map rule = null;

		// check our element regex patterns
		if (!attributeRegexRules.isEmpty()) {
			for (Map.Entry<String, Object> e : attributeRegexRules.entrySet()) {
				Matcher m = attributeCompiledRegexRules.get(e.getKey()).matcher(ruleName);
				if (m.matches()) {
					return (Map) e.getValue();
				}
			}
		}
		return rule;
	}

	public Object getProperty(String property) {
		return config.get(property);
	}

	public void addProperty(String property, boolean value) {
		config.put(property, value);
	}

	public void addProperty(String property, String value) {
		config.put(property, value);
	}

	public void addElementRule(String ruleName, Map rule) {
		elementRules.putAll(rule);
	}

	public void addAttributeRule(String ruleName, Map rule) {
		attributeRules.putAll(rule);
	}

	public void clearRules() {
		config.clear();
		elementRules.clear();
		attributeRules.clear();
		elementRegexRules.clear();
		elementCompiledRegexRules.clear();
		attributeRegexRules.clear();
		attributeCompiledRegexRules.clear();
	}

	protected void findRegexRules() {
		for (Map.Entry<String, Object> entry : elementRules.entrySet()) {
			try {
				Matcher m = REGEX_META_CHARS_PATTERN.matcher(entry.getKey());
				if (m.find()) {
					elementRegexRules.put(entry.getKey(), entry.getValue());
				}
			} catch (PatternSyntaxException e) {
				throw new IllegalConditionalAttributeException(e);
			}
		}

		for (Map.Entry<String, Object> entry : attributeRules.entrySet()) {
			try {
				Matcher m = REGEX_META_CHARS_PATTERN.matcher(entry.getKey());
				if (m.find()) {
					attributeRegexRules.put(entry.getKey(), entry.getValue());
				}
			} catch (PatternSyntaxException e) {
				throw new IllegalConditionalAttributeException(e);
			}
		}
	}

	protected void compileRegexRules() {
		if (!elementRegexRules.isEmpty()) {
			elementCompiledRegexRules = new LinkedHashMap<>();
			for (String r : elementRegexRules.keySet()) {
				Pattern compiledRegex = Pattern.compile(r);
				elementCompiledRegexRules.put(r, compiledRegex);
			}
		}

		if (!attributeRegexRules.isEmpty()) {
			attributeCompiledRegexRules = new LinkedHashMap<>();
			for (String r : attributeRegexRules.keySet()) {
				Pattern compiledRegex = Pattern.compile(r);
				attributeCompiledRegexRules.put(r, compiledRegex);
			}
		}
	}

	public 	Map<String, Object> getAttributeRules () {
		return attributeRules;
	}

	public 	Map<String, Object> getElementRules () {
		return elementRules;
	}

	@Override
	public void close() throws Exception {
		reader.close();
	}
}
