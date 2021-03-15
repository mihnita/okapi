package net.sf.okapi.filters;

public class PlainTextMemoryLeakTestIT extends BaseMemoryLeakTestIT {
	public static void main(String[] args) throws Exception {
		PlainTextMemoryLeakTestIT self = new PlainTextMemoryLeakTestIT(); 
		self.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		self.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		self.addConfigurations("net.sf.okapi.filters.plaintext.PlainTextFilter");
		self.runIt("okf_plaintext", "/plaintext/lgpl.txt");
	}
}
