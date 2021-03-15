/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class does a simple byte-to-byte compare.  The
 * filesExactlyTheSame method takes two files specified
 * by URIs and returns a boolean indicating whether they
 * are the same. 
 */
public class FileCompare {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final static int BUFSIZ = 4096;
	private byte[] obuf = new byte[BUFSIZ];
	private byte[] gbuf = new byte[BUFSIZ];

	public boolean filesExactlyTheSame (String outputFilePath,
		String goldFilePath)
	{
		try (FileInputStream ois = new FileInputStream(new File(outputFilePath));
				FileInputStream gis = new FileInputStream(new File(goldFilePath))) {
			return filesExactlyTheSame(ois, gis);
		} catch ( Exception e ) {
			logger.error("", e);
		}
		return false;
	}
	
	public boolean filesExactlyTheSame (URI outputFileURI,
		URI goldFileURI)
	{
		try (InputStream ois = outputFileURI.toURL().openStream();
				InputStream gis = goldFileURI.toURL().openStream()) {
			return filesExactlyTheSame(ois,gis);
		} catch ( IOException e ) {
			logger.error("", e);
		}
		return false;
	}

	public boolean compareFilesPerLines (String outputFilePath,
			String goldFilePath,
			String encoding) {
		return compareFilesPerLines(outputFilePath, goldFilePath, encoding, false, false);
	}
	
	public boolean compareFilesPerLines (String outputFilePath,
		String goldFilePath,
		String encoding, boolean ignoreInitialEmptyLines, boolean ignoreCase)
	{
		try (FileInputStream ois = new FileInputStream(new File(outputFilePath));
				FileInputStream gis = new FileInputStream(new File(goldFilePath))) {
			return compareFilesPerLines(ois, gis, encoding, ignoreInitialEmptyLines, ignoreCase);
		} catch (IOException e) {
			logger.error("", e);
		}

		return false;
	}
		
	public boolean compareFilesPerLines (InputStream ois,
		InputStream gis,
		String encoding)
	{
		return compareFilesPerLines(ois,gis, encoding, false);
	}
	
	public boolean compareFilesPerLines (InputStream ois,
			InputStream gis,
			String encoding, 
			boolean ignoreInitialEmptyLines) {
		return compareFilesPerLines(ois, gis, encoding, ignoreInitialEmptyLines, false);
	}
	
	public boolean compareFilesPerLines (InputStream ois,
			InputStream gis,
			String encoding, boolean ignoreInitialEmptyLines, boolean ignoreCase)
		{
			try (BufferedReader obr = new BufferedReader(new InputStreamReader(ois, encoding));
					BufferedReader gbr = new BufferedReader(new InputStreamReader(gis, encoding))) {
				
				String oLine;
				String gLine;
				
				boolean oFirstLine = true;
				boolean gFirstLine = true;
				
				while ( true ) {
					oLine = obr.readLine();
					gLine = gbr.readLine();
					
					if(ignoreInitialEmptyLines){
						while( oFirstLine && oLine != null && oLine.equals("") ){
							logger.info("    NOTE: Ignoring initial blank line in out file: {}", gLine);
							oLine = obr.readLine();
						}

						while( gFirstLine && gLine != null && gLine.equals("") ){
							logger.info("    NOTE: Ignoring initial blank line in gold file: {}", gLine);
							gLine = gbr.readLine();
						}

						if(oFirstLine){
							oFirstLine = false;
						}
						if(gFirstLine){
							gFirstLine = false;
						}
					}
					
					if (( oLine == null ) && ( gLine != null )) {
						logger.warn("Extra line in gold file: {}", gLine);
						return false;
					}
					if (( oLine != null ) && ( gLine == null )) {
						logger.warn("Extra line in output file: {}", oLine);
						return false;
					}
					if (( oLine == null ) && ( gLine == null )) {
						return true; // Done
					}
					if ( !ignoreCase && !oLine.equals(gLine) ||
							ignoreCase && !oLine.equalsIgnoreCase(gLine))  {
						logger.warn("Difference in line:\n"
							+ " out: '{}'\n"
							+ "gold: '{}'", oLine, gLine);
						return false;
					}
				}
			} catch ( IOException e ) {
				logger.error("", e);
			}
			return false;
		}

	
	public boolean filesExactlyTheSame (InputStream ois, InputStream gis) {
		try {
			int ored, gred;
			while (( ois.available() > 0 ) && ( gis.available() > 0 )) {
				ored = ois.read(obuf);
				gred = gis.read(gbuf);
				if ( ored != gred ) {
					logger.warn("Size difference in files.");
					return false;
				}
				if ( ored > 0 ) {
					for ( int i=0; i<ored; i++ ) {
						if ( obuf[i] != gbuf[i] ) {
							int start = ((i-20) < 0 ) ? 0 : (i-20);
							int extra = (i<BUFSIZ-11) ? 10 : 1;
							String oText = new String(obuf, start, (i-start)+extra, StandardCharsets.UTF_8);
							String gText = new String(gbuf, start, (i-start)+extra, StandardCharsets.UTF_8);
							logger.warn("Difference in content:\n"
									+ " out='{}'\n"
									+ "gold='{}'", oText, gText);
							return false;
						}
					}
				}
				else { // Done
					return true;
				}
			}
			return true;
		} catch ( IOException e ) {
			logger.error("", e);
		}
		return false;
	}

}
