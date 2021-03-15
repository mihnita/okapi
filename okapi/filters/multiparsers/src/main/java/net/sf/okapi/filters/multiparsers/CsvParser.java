/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.multiparsers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import net.sf.okapi.common.ListUtil;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;

public class CsvParser implements IParser {

	// The Markdown filter generates automatically two line-breaks at the end of paragraphs
	// on merge, this causes those two line-breaks to occur in the field too. Technically it's OK
	// but it makes comparison and reading confusing
	public static final String LB_MARK = "@#$"; // Marker for end of Markdown field
	
	public static final String MIME_TYPE = MimeTypeMapper.CSV_MIME_TYPE;

	private Parameters params;
	private IFilterConfigurationMapper fcMapper;
	private Map<Integer, String> formatCols;
	private Map<String, IFilter> subFilterCols;
	
	private int curEntry = -1;
	private CSVParser parser = null;
	private Iterator<CSVRecord> recIter = null;
	private List<Token> entries = null;
	private String noExtractCols;
	private CsvEncoder encoder = new CsvEncoder();
	private long firstRow = 1;
	private long configRow = -1;
	
	public CsvParser (String path,
		Parameters params,
		IFilterConfigurationMapper fcMapper)
	{
		try {
			this.params = params;
			this.fcMapper = fcMapper;
			
			parser = new CSVParser(Files.newBufferedReader(Paths.get(path)), CSVFormat.DEFAULT);
			recIter = parser.iterator();
			entries = new ArrayList<>();
			
			// Read the relevant parameters for this parser
			noExtractCols = null; // Default: no exception
			configRow = -1;
			
			if ( params != null ) {
				if ( params.getCsvAutoDetectColumnTypes() ) {
					configRow = params.getCsvAutoDetectColumnTypesRow();
					formatCols = new HashMap<>();
					// Column types and sub-filters are initialized later
				}
				else {
					// List of the columns not to extract
					String tmp = params.getCsvNoExtractCols();
					if ( !Util.isEmpty(tmp) ) {
						tmp = ListUtil.intListAsString(ListUtil.stringAsIntList(tmp)); // Remove spaces
						noExtractCols = (","+tmp+","); // Ensure bracketing separators
					}
					firstRow = params.getCsvStartingRow();
					initializeColumnTypes();
				}
			}
		}
		catch (IOException e) {
			throw new OkapiIOException("Could not open: "+path, e);
		}
	}
	
	@Override
	public void close () {
		if ( parser != null ) {
			try {
				parser.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			parser = null;
			recIter = null;
			curEntry = -1;
		}
	}

	@Override
	public boolean hasNext () {
		if ( recIter == null ) return false;
		if ( curEntry > -1 ) {
			curEntry++;
			if ( curEntry < entries.size() ) {
				return true;
			}
			// Else: check for next record
		}
		return recIter.hasNext();
	}
	
	@Override
	public Token next () {
		try {
			// Do we need to read the next record?
			if (( curEntry == -1 ) || ( curEntry >= entries.size() )) {
				
				CSVRecord rec = recIter.next();
				entries.clear();
				boolean inConfigRow = false;
				
				for ( int i=0; i<rec.size(); i++ ) {
					boolean isText = true;
					if ( inConfigRow ) isText = false;
					else if ( noExtractCols != null ) {
						// If the index of this column is not in the exception list, then it is text, otherwise it is code
						isText = (!noExtractCols.contains("," + i + ","));
					}
					// Empty fields are not extracted (this could be an option)
					if ( Util.isEmpty(rec.get(i)) ) isText = false;
					
					// Check for auto-detection of the column types
					if ( configRow > -1 ) {
						if ( rec.getRecordNumber() == configRow ) {
							// Process the configuration row
							autoDetectConfiguration(rec);
							configRow = -1; //
							inConfigRow = true;
						}
						// Before or on the row: this goes to the skeleton
						isText = false;
					}
					else { // Not in auto-detection mode
						// Text before the first extractable row are not extracted
						if ( rec.getRecordNumber() < firstRow ) isText = false;
					}
					
					String comma = ((i>0) ? "," : "");
					String linebreak = ((i==rec.size()-1) ? "\n" : "");
					if ( isText ) {
						// Separator/delimiter before
						entries.add(new Token(comma+"\"", false, null));
						String format = formatCols.get(i); // Null if not there (plain-text)
						entries.add(new Token(rec.get(i), true, format));
						if (( format != null ) && ( format.startsWith("okf_markdown") )) {
							// Add special marker for MD fields (for post-processing) 
							entries.add(new Token("\""+LB_MARK+linebreak, false, null));
						}
						else {
							entries.add(new Token("\""+linebreak, false, null));
						}
					}
					else { // Code
						// We escape and add the separators/delimiters
						entries.add(new Token(comma+"\""+encoder.encode(rec.get(i), EncoderContext.SKELETON)+"\""+linebreak, false, null));
					}
				}
				curEntry = 0;
			}
			return entries.get(curEntry);
		}
		catch (Throwable e) {
			throw new OkapiIOException("Reading error: "+e.getMessage(), e);
		}
	}

	private void autoDetectConfiguration (CSVRecord rec) {
		noExtractCols = ",";
		formatCols.clear();
		
		for ( int col=0; col<rec.size(); col++ ) {
			String value = rec.get(col).toLowerCase();
			if ( value.equals("notrans") ) {
				noExtractCols += (col+",");
			}
			else if ( value.equals("text") ) {
				
			}
			else if ( value.startsWith("okf_html") || value.startsWith("okf_markdown") ) {
				formatCols.put(col, rec.get(col));
			}
			else {
				// Unexpected auto-configuration value
				throw new RuntimeException("Unexpected value in the auto-detection row: "+value);
			}
		}
		
		if ( noExtractCols.length()==1 ) noExtractCols = null;
		initializeSubFilters();
	}

	private void initializeColumnTypes () {
		formatCols = new HashMap<>(); 
		if ( params != null ) {
			// Create the map of format to use
			String tmp = params.getString(Parameters.CSV_FORMATCOLS);
			if ( !Util.isEmpty(tmp) ) {
				List<String> cols = ListUtil.stringAsList(tmp);
				for ( String col : cols ) {
					int p = col.indexOf(':');
					if ( p == -1 ) {
						throw new RuntimeException("Invalid syntax: "+col);
					}
					int index;
					try {
						index = Integer.parseInt(col.substring(0, p));
					}
					catch ( NumberFormatException e ) {
						throw new RuntimeException("Invalid index: "+col);
					}
					String format = col.substring(p+1);
					formatCols.put(index, format);
				}
			}
		}
		initializeSubFilters();
	}

	public IFilter getSubFilter (String configId) {
		return subFilterCols.get(configId);
	}
	
	public void initializeSubFilters () {
		// Now create the map of filter instances
		// (Based on the map of format to use)
		subFilterCols = new HashMap<>();
		// Skip the loop if needed
		if ( formatCols == null ) return;
		// Else: do it
		for ( String configId : formatCols.values() ) {
			if ( fcMapper == null ) {
				throw new RuntimeException("You must specify a filter configuration mapper for this filter.");
			}
			if ( !subFilterCols.containsKey(configId) ) {
				IFilter sf = fcMapper.createFilter(configId);
				subFilterCols.put(configId, sf);
			}
		}
	}

}
