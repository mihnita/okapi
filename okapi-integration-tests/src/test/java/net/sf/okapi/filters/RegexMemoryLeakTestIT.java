package net.sf.okapi.filters;

public class RegexMemoryLeakTestIT extends BaseMemoryLeakTestIT {
	public static void main(String[] args) throws Exception {
		RegexMemoryLeakTestIT self = new RegexMemoryLeakTestIT();
		self.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		self.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		self.addConfigurations("net.sf.okapi.filters.regex.RegexFilter");
		self.runIt("okf_regex", "/plaintext/lgpl.txt");
	}
}
