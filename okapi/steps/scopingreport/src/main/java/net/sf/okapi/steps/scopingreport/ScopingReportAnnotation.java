package net.sf.okapi.steps.scopingreport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.IAnnotation;

public class ScopingReportAnnotation  implements IAnnotation, Serializable {
	
	private static final long serialVersionUID = -1566108918173044555L;
	private Map<String, String> fields = new HashMap<>();
	
	public void put(String fieldName, String fieldValue) {
		if (Util.isEmpty(fieldValue)) fieldValue = "0"; 
		fields.put(fieldName, fieldValue);
	}

	public Map<String, String> getFields() {
		return fields;
	}
	
	public void setFields(Map<String, String> fields) {
		this.fields = fields;
	}
}
