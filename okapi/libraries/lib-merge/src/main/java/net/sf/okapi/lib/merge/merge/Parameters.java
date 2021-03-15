/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.lib.merge.merge;

import net.sf.okapi.common.StringParameters;

public class Parameters extends StringParameters {
	static final String APPROVEDONLY = "approvedOnly"; //$NON-NLS-1$
	static final String THROW_EXCEPTION_CODE_DIFFERENCES = "throwExceptionForCodeDifferences"; //$NON-NLS-1$
	static final String THROW_EXCEPTION_SEGMENT_ID_DIFFERENCES = "throwExceptionForSegmentIdDifferences"; //$NON-NLS-1$
	static final String THROW_EXCEPTION_SEGMENT_SOURCE_DIFFERENCES = "throwExceptionForSegmentSourceDifferences"; //$NON-NLS-1$

	public Parameters () {
		super();
	}
	
	@Override
	public void reset () {
		super.reset();
		setApprovedOnly(false);
		setThrowCodeException(false);
		setThrowSegmentIdException(true);
		setThrowSegmentSourceException(false);
	}

	/**
	 * Only merge approved translations. 
	 * @return boolean - use approved target translations only?
	 */
	public boolean isApprovedOnly() {
		return getBoolean(APPROVEDONLY);
	}

	public void setApprovedOnly(boolean approvedOnly) {
		setBoolean(APPROVEDONLY, approvedOnly);
	}

	public boolean isThrowCodeException() {
		return getBoolean(THROW_EXCEPTION_CODE_DIFFERENCES);
	}

	public void setThrowCodeException(boolean exception) {
		setBoolean(THROW_EXCEPTION_CODE_DIFFERENCES, exception);
	}
	
	public boolean isThrowSegmentIdException() {
		return getBoolean(THROW_EXCEPTION_SEGMENT_ID_DIFFERENCES);
	}

	public void setThrowSegmentIdException(boolean exception) {
		setBoolean(THROW_EXCEPTION_SEGMENT_ID_DIFFERENCES, exception);
	}
	
	public boolean isThrowSegmentSourceException() {
		return getBoolean(THROW_EXCEPTION_SEGMENT_SOURCE_DIFFERENCES);
	}

	public void setThrowSegmentSourceException(boolean exception) {
		setBoolean(THROW_EXCEPTION_SEGMENT_SOURCE_DIFFERENCES, exception);
	}
}
