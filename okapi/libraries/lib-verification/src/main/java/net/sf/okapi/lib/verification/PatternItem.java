/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.lib.verification;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.okapi.common.exceptions.OkapiIOException;

public class PatternItem {
	
	public static final String SAME = "<same>";

	public String source;
	public String target;
	public boolean enabled;
	public String description;
	public int severity;
	public boolean fromSource;
	public boolean singlePattern; // True to trigger issue when single pattern has a match
	
	private Pattern srcPat;
	private Pattern trgPat;

	public static List<PatternItem> loadFile (String path) {
		ArrayList<PatternItem> list = new ArrayList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8));
			String line = br.readLine();
			while ( line != null ) {
				if ( line.trim().length() == 0 ) continue;
				if ( line.startsWith("#") ) continue;
				String[] parts = line.split("\t", -2);
				if ( parts.length < 6 ) {
					throw new OkapiIOException("Missing one or more tabs in line:\n"+line);
				}
				int severity = Issue.DISPSEVERITY_MEDIUM;
				try {
					severity = Integer.valueOf(parts[2]);
				}
				catch ( Throwable e ) {
					// Just use medium
				}
				PatternItem item = new PatternItem(parts[3], parts[4], parts[0].equals("1"), severity, parts[1].equals("1"), parts[5]);
				// Set the singlePattern option only if present
				if ( parts.length == 7 ) {
					item.singlePattern = parts[6].equals("1");
				}
				list.add(item);
				line = br.readLine();
			}
		}
		catch ( IOException e ) {
			throw new OkapiIOException("Error reading pattern file.", e);
		}
		finally {
			if ( br != null ) {
				try {
					br.close();
				}
				catch ( IOException e ) {
					throw new OkapiIOException("Error closing pattern file.", e);
				}
			}
		}
		return list;
	}
	
	public static List<PatternItem> saveFile (String path,
		List<PatternItem> list)
	{
		// Format:
		// Use?<t>fromSource?<t>severity<t>source<t>target<t>description<t>singlePattern
		final String lineBreak = System.getProperty("line.separator");
		try ( PrintWriter pr = new PrintWriter(path, StandardCharsets.UTF_8.name()) ) {
			for ( PatternItem item : list ) {
				pr.write((item.enabled ? "1" : "0")
					+ "\t" + (item.fromSource ? "1" : "0")
					+ "\t" + item.severity
					+ "\t" + item.source
					+ "\t" + item.target
					+ "\t" + item.description
					+ "\t" + (item.singlePattern ? "1" : "0")
					+ lineBreak);
			}
			pr.flush();
		} catch ( IOException e ) {
			throw new OkapiIOException("Error reading pattern file.", e);
		}
		return list;
	}
	
	public PatternItem (String source,
		String target,
		boolean enabled,
		int severity)
	{
		create(source, target, enabled, severity, true, null);
	}

	public PatternItem (String source,
		String target,
		boolean enabled,
		int severity,
		String message)
	{
		create(source, target, enabled, severity, true, message);
	}

	public PatternItem (String source,
		String target,
		boolean enabled,
		int severity,
		boolean fromSource,
		String message)
	{
		create(source, target, enabled, severity, fromSource, message);
	}

	private void create (String source,
		String target,
		boolean enabled,
		int severity,
		boolean fromSource,
		String message)
	{
		this.source = source;
		this.target = target;
		this.enabled = enabled;
		this.description = message;
		this.severity = severity;
		this.fromSource = fromSource;
	}

	public void compile () {
		if ( !fromSource ) {
			if ( singlePattern ) {
				srcPat = null; // Not used
			}
			else {
				int p = source.indexOf("$");
				if ( source.contains(SAME) || ( p>-1 && p<(source.length()-1) ) ) {
					srcPat = null; // Will be compile at usage time
				}
				else {
					srcPat = Pattern.compile(source);
				}
			}
		}
		else {
			srcPat = Pattern.compile(source);
		}
		
		if ( fromSource ) {
			if ( singlePattern ) {
				trgPat = null; // Not used
			}
			else {
				int p = target.indexOf("$");
				if ( target.contains(SAME) || ( p>-1 && p<(target.length()-1) ) ) {
					trgPat = null; // Will be compile at usage time
				}
				else {
					trgPat = Pattern.compile(target);
				}
			}
		}
		else {
			trgPat = Pattern.compile(target);
		}
	}

	public Pattern getSourcePattern () {
		return srcPat; 
	}

	public Pattern getTargetPattern () {
		return trgPat; 
	}
	
	/**
	 * Gets the pattern object for the target, automatically compiling a new one
	 * if it is null (which means the pattern contains the keyword "<same>" and/or $N).
	 * @param srcMatcher the source Matcher object (for accessing the captured groups)
	 * @return the pattern to use.
	 */
	public Pattern getTargetSmartPattern (Matcher srcMatcher) {
		if ( trgPat != null ) {
			return trgPat; // Existing target compiled pattern means there is no dynamic parts in the pattern
		}
		// Otherwise: we need to replace <same> and the $N placeholders
		String tmp = target.replace(SAME, "("+Pattern.quote(srcMatcher.group())+")");
		int grpCount = srcMatcher.groupCount();
		for ( int i=grpCount; i>0; i-- ) {
			String grp = srcMatcher.group(i); // Some groups may exist so we replace their placeholder by an empty string
			tmp = tmp.replace("$"+i, (( grp == null ) ? "" : Pattern.quote(grp)));
		}
		return Pattern.compile(tmp);
	}

	/**
	 * Gets the pattern object for the source, automatically compiling a new one
	 * if it is null (which means the pattern contains the keyword "<same>" and/or $N).
	 * @param trgMatcher the source Matcher object (for accessing the captured groups)
	 * @return the pattern to use.
	 */
	public Pattern getSourceSmartPattern (Matcher trgMatcher) {
		if ( srcPat != null ) {
			return srcPat; // Existing source compiled pattern means there is no dynamic parts in the pattern
		}
		// Otherwise: we need to replace <same> and the $N placeholders
		String tmp = source.replace(SAME, "("+Pattern.quote(trgMatcher.group())+")");
		int grpCount = trgMatcher.groupCount();
		for ( int i=grpCount; i>0; i-- ) {
			String grp = trgMatcher.group(i); // Some groups may exist so we replace their placeholder by an empty string
			tmp = tmp.replace("$"+i, (( grp == null ) ? "" : Pattern.quote(grp)));
		}
		return Pattern.compile(tmp);
	}
	
}
