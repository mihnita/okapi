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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filters.SubFilter;
import net.sf.okapi.common.filterwriter.GenericFilterWriter;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

public class MultiParsersFilter implements IFilter {

	public static final int MODE_CSV = 0;

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private final Pattern patFix1 = Pattern.compile("__(.*?)__( *)\r\n(?!(\r\n))"); 
	private final Pattern patFix2 = Pattern.compile("\\*\\*(.*?)\\*\\*( *)\r\n(?!(\r\n))"); 
	private final Pattern patFix3 = Pattern.compile("\\]\\((.*?)\\)( *)\r\n(?!(\r\n))"); 
	private final Pattern patTarget = Pattern.compile("\\{:target=(.*?)\\}", Pattern.CASE_INSENSITIVE);
	
	private int mode = MODE_CSV;
	private String currentMimeType = "application/x-multiparsers";
	private EncoderManager encoderManager = null;
	private IFilterConfigurationMapper fcMapper;
	private String docId;
	private Parameters params;
	private LocaleId srcLoc;
	private IParser parser = null;
	private int tuId;
	private int dpId;
	private boolean cancelled;
	private LinkedList<Event> events;
	private boolean done;
	private boolean hasNextWasCalled;
	private int sectionIndex;
	
	public MultiParsersFilter () {
		params = new Parameters();
	}
	
	@Override
	public String getName () {
		return "okf_multiparsers";
	}

	@Override
	public String getDisplayName () {
		return "Multi-Parsers Filter";
	}

	@Override
	public void open (RawDocument input) {
		open(input, true);
	}

	@Override
	public void open (RawDocument input,
		boolean generateSkeleton)
	{
		Path path = Paths.get(input.getInputURI());
		if ( input.getInputURI() == null ) {
			throw new OkapiIOException("No inputURI specified.");
		}

		srcLoc = input.getSourceLocale();
		parser = new CsvParser(path.toFile().getAbsolutePath(), params, fcMapper);
		
		tuId = 0;
		dpId = 0;
		cancelled = false;
		done = false;
		hasNextWasCalled = false;

		events = new LinkedList<>();
		docId = UUID.randomUUID().toString();
		
		StartDocument sd = new StartDocument(docId);
		sd.setFilterId(getName());
		sd.setFilterParameters(params);
		sd.setFilterWriter(createFilterWriter());
		sd.setEncoding("UTF-8", false); // TODO: auto-detect
		sd.setLineBreak("\r\n");
		
		if ( mode == MODE_CSV ) currentMimeType = CsvParser.MIME_TYPE;
		sd.setMimeType(currentMimeType);
		
		if ( input.getInputURI() != null ) {
			sd.setName(input.getInputURI().getPath());
		}
		sd.setLocale(srcLoc);
		events.add(new Event(EventType.START_DOCUMENT, sd));
	}

//	/**
//	 * Prepares the data associated with the format of the columns.
//	 * This creates a map of the formats to use and a map of the actual filter instances.
//	 * @return a map of the formats to use (colIndex, formatId).
//	 */
//	private Map<Integer, String> prepareSubFilters () {
//		Map<Integer, String> formatCols = new HashMap<>(); 
//
//		if ( params != null ) {
//			// Create the map of format to use
//			String tmp = params.getString(Parameters.CSV_FORMATCOLS);
//			if ( !Util.isEmpty(tmp) ) {
//				List<String> cols = ListUtil.stringAsList(tmp);
//				for ( String col : cols ) {
//					int p = col.indexOf(':');
//					if ( p == -1 ) {
//						throw new RuntimeException("Invalid syntax: "+col);
//					}
//					int index;
//					try {
//						index = Integer.parseInt(col.substring(0, p));
//					}
//					catch ( NumberFormatException e ) {
//						throw new RuntimeException("Invalid index: "+col);
//					}
//					String format = col.substring(p+1);
//					formatCols.put(index, format);
//				}
//			}
//			
//			// Now create the map of filter instances
//			// (Based on the map of format to use)
//			subFilterCols = new HashMap<>();
//			for ( String configId : formatCols.values() ) {
//				if ( fcMapper == null ) {
//					throw new RuntimeException("You must specify a filter configuration mapper for this filter.");
//				}
//				if ( !subFilterCols.containsKey(configId) ) {
//					IFilter sf = fcMapper.createFilter(configId);
//					subFilterCols.put(configId, sf);
//				}
//			}
//		}
//		return formatCols;
//	}

	@Override
	public void close () {
		if ( parser != null ) {
			parser.close();
			parser = null;
		}
	}

	@Override
	public boolean hasNext () {
		// System.out.println("Call to hasNext()");
		hasNextWasCalled = true; // To work around the issue of some steps calling next() without calling hasNext()
		if ( parser == null ) return false;
		if ( events.isEmpty() ) {
			boolean more = parser.hasNext();
			if ( more ) return true;
			else if ( !done ) buildEndDocument();
			else return false;
		}
		return true; // Queue not empty
	}

	@Override
	public Event next () {
		// System.out.println("Call to next()");
		// Cancel if requested
		if ( cancelled ) {
			close();
			return new Event(EventType.CANCELED);
		}

		// Work around the issue caused by some step (like RawDocument2FilterEvents itself)
		// to call IFilter.next() without calling IFilter.hasNext() first
		if ( !hasNextWasCalled ) {
			if ( !hasNext() ) {
				buildEndDocument();
			}
		}
		hasNextWasCalled = false;
		
		if ( events.isEmpty() ) {
			// Else: build the next event(s)
			Token token = parser.next();
			do {
				// And create the relevant event(s) for that token
				if ( token.isText() ) {
					if ( token.getFilterConfigId() == null ) {
						ITextUnit tu = new TextUnit(nextTuId(), token.getData());
						events.add(new Event(EventType.TEXT_UNIT, tu));
					}
					else {
						IFilter sf = parser.getSubFilter(token.getFilterConfigId());
						if ( sf == null ) {
							throw new OkapiIOException("Unexpected filter: "+token.getFilterConfigId());
						}
						// Else: use the sub-filter (events are adds directly during the call
						processWithSubfilter(token, sf);
					}
				}
				else { // Code
					DocumentPart dp = new DocumentPart(nextDpId(), false, new GenericSkeleton(token.getData()));
					events.add(new Event(EventType.DOCUMENT_PART, dp));
				}
				
				if ( events.isEmpty() ) {
					if ( parser.hasNext() ) {
						token = parser.next();
					}
					else { // No more token
						buildEndDocument();
					}
				}
			}
			while ( events.isEmpty() );
		}
		Event event = events.poll();
		
		//Debug
//		switch ( event.getEventType() ) {
//		case TEXT_UNIT:
//			String data = event.getTextUnit().getSource().getFirstContent().getCodedText();
//			System.out.println("id: "+event.getResource().getId()+" text: "+data);
//			break;
//		case START_GROUP:
//			System.out.println("start-G: "+event.getStartGroup().getId());
//			break;
//		case END_GROUP:
//			System.out.println("end-G: "+event.getEnding().getId());
//			break;
//		default:
//			System.out.println("ev: "+event.getEventType()+" id: "+event.getResource().getId());
//		}
		
		return event;
	}
	
	private void buildEndDocument () {
		Ending ed = new Ending(docId);
		events.add(new Event(EventType.END_DOCUMENT, ed));
		done = true;
	}
	
	protected String preProcessDataForMarkdown (String data) {
		Matcher m1 = patFix1.matcher(data);
		data = m1.replaceAll("__$1__$2\r\n\r\n[mrk1]");
		Matcher m2 = patFix2.matcher(data);
		data = m2.replaceAll("**$1**$2\r\n\r\n[mrk1]");
		Matcher m3 = patFix3.matcher(data);
		data = m3.replaceAll("]($1)$2\r\n\r\n[mrk1]");
		return data;
	}
	
	public String autoDetectColumnTypes (String csvPath, int autoDetectRowNum) {
		String matchedLine = null;
		// TODO: perhaps this should use csvParser
		try (BufferedReader br = new BufferedReader(new FileReader( csvPath ) ) ) {
			String currentLine;
			int lineCount = 1;
			while (( currentLine = br.readLine() ) != null) {
				if ( autoDetectRowNum == lineCount ) {
					matchedLine = currentLine;
					break;
				}
				lineCount++;
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		if ( matchedLine == null ) {
			throw new RuntimeException("Could not find a matching line at row number "+autoDetectRowNum);
		}
		
		return matchedLine;
	}
	
	private void processWithSubfilter (Token token,
		IFilter filterToUse)
	{
		String dataToUse = token.getData();
		// Pre-process the data to work around some issues
		if ( filterToUse.getName().startsWith("okf_markdown") ) {
			dataToUse = preProcessDataForMarkdown(dataToUse);
		}

		logger.info("sf: {}", dataToUse);
		// Create a TU for the part to sub-filter
		ITextUnit tu = new TextUnit(nextTuId(), dataToUse);
		
		// Create the sub-filter wrapper
		SubFilter subfilter = new SubFilter(filterToUse, encoderManager.getEncoder(),
			++sectionIndex, tu.getId(), null);

		subfilter.open(new RawDocument(dataToUse, srcLoc));
		while (subfilter.hasNext()) {
			// Get the event
			Event e = subfilter.next();
			// Extra parsing to fix Markdown filter misses
			if ( e.isTextUnit() ) {
				tu = e.getTextUnit();
				if ( tu.isTranslatable() ) {
					String tmp = tu.getSource().getCodedText();
					Matcher m = patTarget.matcher(tmp);
					while ( m.find() ) {
						int sta = m.start(0);
						int end = m.end(0);
						TextFragment tf = tu.getSource().getFirstContent();
						tf.changeToCode(sta, end, TagType.PLACEHOLDER, "target");
						tmp = tf.getCodedText();
						m = patTarget.matcher(tmp);
					}
				}
			}
			// then add the event
			events.add(e);
		}
		subfilter.close();
		
		events.add(subfilter.createRefEvent(tu));
	}
	
	private String nextTuId () {
		tuId++;
		return "tu"+tuId;
	}
	
	private String nextDpId () {
		dpId++;
		return "dp"+dpId;
	}
	
	@Override
	public void cancel () {
		cancelled = true;
	}

	@Override
	public Parameters getParameters () {
		return params;
	}

	@Override
	public void setParameters (IParameters params) {
		this.params = (Parameters)params;
	}

	@Override
	public void setFilterConfigurationMapper (IFilterConfigurationMapper fcMapper) {
		this.fcMapper = fcMapper;
	}

	public ISkeletonWriter createSkeletonWriter() {
		return new GenericSkeletonWriter();
	}

	public IFilterWriter createFilterWriter () {
		return new GenericFilterWriter(createSkeletonWriter(), getEncoderManager());
	}
	
	@Override
	public EncoderManager getEncoderManager () {
		if ( encoderManager == null ) {
			encoderManager = new EncoderManager();
			encoderManager.setMapping(CsvParser.MIME_TYPE, "net.sf.okapi.filters.multiparsers.CsvEncoder");
		}
		return encoderManager;
	}

	@Override
	public String getMimeType () {
		return currentMimeType;
	}

	@Override
	public List<FilterConfiguration> getConfigurations () {
		List<FilterConfiguration> list = new ArrayList<>();
		list.add(new FilterConfiguration("okf_multiparsers",
			CsvParser.MIME_TYPE,
			getClass().getName(),
			"Multi-Parsers: CSV with Plain-Text",
			"Configuration for CSV files with plain-text on all columns",
			null,
			".csv;"));
		return list;
	}

}
