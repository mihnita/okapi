package net.sf.okapi.filters.regex;

import java.util.regex.Pattern;

import net.sf.okapi.common.ParametersString;

public class MetaRule {
	private String ruleName;
	private String expr;
	private Pattern pattern;

	/**
	 * Used with fromString
	 */
	public MetaRule() {
	}

	public MetaRule(String ruleName, String expr) {
		this.ruleName = ruleName;
		this.expr = expr;
		// build pattern later as we need regex options
		this.pattern = null;
	}

	/**
	 * @return the ruleName
	 */
	public String getRuleName() {
		return ruleName;
	}

	/**
	 * @param ruleName the ruleName to set
	 */
	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}

	/**
	 * @return the expr
	 */
	public String getExpression() {
		return expr;
	}

	/**
	 * @param expr the expr to set
	 */
	public void setExpression(String expr) {
		this.expr = expr;
	}

	/**
	 * @return the pattern
	 */
	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * @param pattern the pattern to set
	 */
	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public String toString() {
		ParametersString tmp = new ParametersString();
		tmp.setString("ruleName", ruleName);
		tmp.setString("expr", expr);
		return tmp.toString();
	}

	public void fromString(String data) {
		ParametersString tmp = new ParametersString(data);
		ruleName = tmp.getString("ruleName", ruleName);
		expr = tmp.getString("expr", expr);
	}
}
