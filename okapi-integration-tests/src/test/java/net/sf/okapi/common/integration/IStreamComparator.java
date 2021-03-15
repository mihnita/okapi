package net.sf.okapi.common.integration;

import java.io.InputStream;

public interface IStreamComparator {
	boolean compare(InputStream actual, InputStream expected);
}
