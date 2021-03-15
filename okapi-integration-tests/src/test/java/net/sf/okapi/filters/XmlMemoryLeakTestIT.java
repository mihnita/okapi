package net.sf.okapi.filters;

public class XmlMemoryLeakTestIT extends BaseMemoryLeakTestIT {	
	public static void main(String[] args) throws Exception {
		XmlMemoryLeakTestIT self = new XmlMemoryLeakTestIT();
		self.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		self.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		self.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
		self.runIt("okf_xml", "/xml/input.xml");
	}
}
