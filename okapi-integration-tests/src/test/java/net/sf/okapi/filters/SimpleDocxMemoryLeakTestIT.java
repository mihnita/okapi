package net.sf.okapi.filters;

public class SimpleDocxMemoryLeakTestIT extends BaseMemoryLeakTestIT {	
	public static void main(String[] args) throws Exception {
		SimpleDocxMemoryLeakTestIT self = new SimpleDocxMemoryLeakTestIT();
		self.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		self.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		self.runIt("okf_openxml", "/openxml/docx/OpenXML_text_reference_v1_2.docx");
	}
}
