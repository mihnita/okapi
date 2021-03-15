package net.sf.okapi.filters.autoxliff;

import net.sf.okapi.common.StringParameters;

public class AutoXLIFFParameters extends StringParameters {

	public static final String XLIFF12_CONFIG = "xliff_config";
	public static final String XLIFF20_CONFIG = "xliff2_config";

	public AutoXLIFFParameters() {
		super();
	}

	public String getXLIFF12Config() {
		return getString(XLIFF12_CONFIG);
	}

	public void setXLIFF12Config(String config) {
		setString(XLIFF12_CONFIG, config);
	}

	public String getXLIFF20Config() {
		return getString(XLIFF20_CONFIG);
	}

	public void setXLIFF20Config(String config) {
		setString(XLIFF20_CONFIG, config);
	}

	@Override
	public void reset() {
		super.reset();
		setXLIFF12Config("okf_xliff");
		setXLIFF20Config("okf_xliff2");
	}
}
