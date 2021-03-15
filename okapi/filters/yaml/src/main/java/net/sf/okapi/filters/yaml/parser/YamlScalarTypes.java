package net.sf.okapi.filters.yaml.parser;

public enum YamlScalarTypes {
	COMPLEX("?"),
	PLAIN(""),
	SINGLE("'"),
	DOUBLE("\""),
	LITERAL(""),
	FOLDED(""),
	UNKNOWN("");

	private final String quoteChar;

	private YamlScalarTypes(String quoteChar)
	{
		this.quoteChar = quoteChar;
	} 
	
	public String getQuoteChar()
	{
		return this.quoteChar;
	}	
}