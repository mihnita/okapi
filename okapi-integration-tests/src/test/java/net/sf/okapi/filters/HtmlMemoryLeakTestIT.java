package net.sf.okapi.filters;

public class HtmlMemoryLeakTestIT extends BaseMemoryLeakTestIT {
	public static void main(String[] args) throws Exception {
		HtmlMemoryLeakTestIT self = new HtmlMemoryLeakTestIT();
		self.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		self.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		self.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		self.runIt("okf_html", "/html/testBOM.html");		
	}
}
