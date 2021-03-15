package net.sf.okapi.common.integration;

import net.sf.okapi.common.exceptions.OkapiException;

public class OkapiTestException extends OkapiException {

	public OkapiTestException() {
	}

	public OkapiTestException(String message) {
		super(message);
	}

	public OkapiTestException(Throwable cause) {
		super(cause);
	}

	public OkapiTestException(String message, Throwable cause) {
		super(message, cause);
	}
}
