/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.plaintext;

import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.filters.AbstractCompoundFilter;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.filters.plaintext.base.BasePlainTextFilter;
import net.sf.okapi.filters.plaintext.paragraphs.ParaPlainTextFilter;
import net.sf.okapi.filters.plaintext.regex.RegexPlainTextFilter;
import net.sf.okapi.filters.plaintext.spliced.SplicedLinesFilter;

/**
 * Plain Text filter, processes text files encoded in ANSI, Unicode, UTF-8, UTF-16. Provides the byte-order mask detection. 
 * The filter is aware of the following line terminators:
 * <ul><li>Carriage return character followed immediately by a newline character ("\r\n")
 * <li>Newline (line feed) character ("\n")
 * <li>Stand-alone carriage return character ("\r")</ul><p> 
 * 
 */
@UsingParameters(Parameters.class)
public class PlainTextFilter extends AbstractCompoundFilter {

	public static final String FILTER_NAME	= "okf_plaintext";
	public static final String FILTER_MIME	= MimeTypeMapper.PLAIN_TEXT_MIME_TYPE;
	
	public PlainTextFilter() {
		super();
		setName(FILTER_NAME);
		setDisplayName("Plain Text Filter");
		setMimeType(FILTER_MIME);
		// Plain Text Filter parameters
		setParameters(new Parameters(this));
		
		addSiblingFilter(BasePlainTextFilter.class);
		addSiblingFilter(ParaPlainTextFilter.class);
		addSiblingFilter(SplicedLinesFilter.class);
		addSiblingFilter(RegexPlainTextFilter.class);
		
		// Remove configs of sub-filters not needed in the parent compound filter
		removeConfiguration(SplicedLinesFilter.FILTER_CONFIG);
		removeConfiguration(ParaPlainTextFilter.FILTER_CONFIG_LINES);
		removeConfiguration(RegexPlainTextFilter.FILTER_CONFIG);
	}

	@Override
	public void setFilterConfigurationMapper (IFilterConfigurationMapper fcMapper) {
		IFilter filter = getActiveSiblingFilter();
		if (filter != null) {
			filter.setFilterConfigurationMapper(fcMapper);
		}
	}
}
