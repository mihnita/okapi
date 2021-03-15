package net.sf.okapi.filters.yaml.parser;

public enum YamlChompTypes {
	PLUS("+"),
	MINUS("-"),
	NONE("");

	private final String chompChar;

	private YamlChompTypes(String chompChar)
	{
		this.chompChar = chompChar;
	} 
	
	public String getchompChar()
	{
		return this.chompChar;
	}
}
