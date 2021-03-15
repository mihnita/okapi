package net.sf.okapi.filters;

public class TableMemoryLeakTestIT extends BaseMemoryLeakTestIT {
	public static void main(String[] args) throws Exception {
		TableMemoryLeakTestIT self = new TableMemoryLeakTestIT();
		self.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
		self.addConfigurations("net.sf.okapi.filters.xml.XMLFilter");
		self.addConfigurations("net.sf.okapi.filters.table.TableFilter");
		self.runIt("okf_table_csv", "/table/test2cols.csv");
	}
}
