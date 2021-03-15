package net.sf.okapi.common.integration;

import java.nio.file.Path;

public interface IFileComparator {
	boolean compare(Path actual, Path expected);
}
