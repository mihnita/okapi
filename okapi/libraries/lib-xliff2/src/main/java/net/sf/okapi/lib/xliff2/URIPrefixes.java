/*===========================================================================
  Copyright (C) 2013-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Represents the prefixes for modules and extensions in XLIFF URI fragment identifiers.
 * This object is set to load automatically a default mapping and possibly a custom one
 * the first time {@link #resolve(String)} is called.
 */
public class URIPrefixes {

	// See https://lists.oasis-open.org/archives/xliff/201708/msg00078.html for more information
	private final String REGISTRY_URL = "https://tools.oasis-open.org/version-control/browse/wsvn/xliff/trunk/xliff2-fragid/registry.txt?op=dl&isdir=0";

	private Map<String, List<String>> prefixes;
	private File extraPrefixes;

	/**
	 * Creates a simple URIPrefixes object. 
	 */
	public URIPrefixes () {
		// Nothing to do: The defaults are loaded if needed
	}

	/**
	 * Creates a URIPrefixes object with a given file where custom prefixes are stored.
	 * @param extraPrefixes the properties file where the custom prefixes are stored (can be null)
	 */
	public URIPrefixes (File extraPrefixes) {
		// Set the path of the extra prefixes
		this.extraPrefixes = extraPrefixes;
		// Nothing more to do: The defaults and the extra are loaded if needed
	}
	
	/**
	 * Loads the default prefixes from the default properties file.
	 * This method is called automatically if needed in {@link #resolve(String)}.
	 */
	public void loadDefaults () {
		prefixes = loadFromPropertiesFile(
			getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/prefixes.properties"));
	}
	
	/**
	 * Loads a properties file and create the corresponding mapping from it.
	 * @param inputStream the input stream of the properties file to load.
	 * @return the mapping for the given properties file.
	 */
	private Map<String, List<String>> loadFromPropertiesFile (InputStream inputStream) {
		Map<String, List<String>> prefixes = new LinkedHashMap<>();
		Properties prop = new Properties();
		try {
			// Load the properties file
			prop.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			// Fill the map
			for ( Entry<Object, Object> entry : prop.entrySet() ) {
				// Check prefix
				String prefix = (String)entry.getValue();
				if ( prefix.length() < 2 ) {
					throw new XLIFFException(String.format("The prefix '%s' is too short.", prefix));
				}
				if ( !Util.isValidNmtoken(prefix) ) {
					throw new XLIFFException(String.format("The module or extension prefix '%s' is not an NMTOKEN.", prefix));
				}
				// Add it
				List<String> uris = prefixes.computeIfAbsent(prefix, k -> new ArrayList<>());
				uris.add((String)entry.getKey());
			}
		}
		catch ( IOException e ) {
			throw new XLIFFException("Cannot load the prefixes.properties file from the resources.\n"+e.getLocalizedMessage());
		}
		return prefixes;
	}

	/**
	 * Adds a collection of uri/prefix to this object.
	 * Calls {@link #doAutoLoads()} if no mapping is loaded yet.
	 * @param map the map to add.
	 */
	public void add (Map<String, String> map) {
		// Load the auto-loads if no mapping is defined yet
		if ( prefixes == null ) doAutoLoads();
		// Add the map
		for ( String uri : map.keySet() ) {
			String prefix = map.get(uri);
			List<String> uris = prefixes.computeIfAbsent(prefix, k -> new ArrayList<>());
			uris.add(uri);
		}
	}
	
	/**
	 * Adds prefixes from a given properties file.
	 * If no mapping is loaded yet, the default one is loaded automatically before
	 * loading the one defined in this given properties file.
	 * @param inputFile the properties file where to get the extra prefixes from.
	 */
	public void addExtra (File inputFile) {
		// Load the defaults if no mapping is defined yet
		if ( prefixes == null ) loadDefaults();

		// Load the properties file and add the extra mappings.
		try (FileInputStream fis = new FileInputStream(inputFile)) {
			Map<String, List<String>> extraPrefixes = loadFromPropertiesFile(fis);
			// Add the custom extra prefixes if there are any
			// We do not override existing prefixes, just add to them or add new prefixes
			if ( extraPrefixes != null ) {
				for ( String key : extraPrefixes.keySet() ) {
					List<String> uris = prefixes.computeIfAbsent(key, k -> new ArrayList<>());
					uris.addAll(extraPrefixes.get(key));
				}
			}
		} catch ( Throwable e ) {
			throw new XLIFFException("Cannot add extra prefixes.\n"+e.getLocalizedMessage());
		}
	}

	/**
	 * Resolves a given prefix.
	 * Calls {@link #doAutoLoads()} if no mapping is loaded yet.
	 * @param prefix the prefix to look-up.
	 * @return the array of namespace URIs associated with the given prefix,
	 * or null if the prefix is not found.
	 */
	public List<String> resolve (String prefix) {
		// Load the auto-loads if no mapping is defined yet
		if ( prefixes == null ) doAutoLoads();
		return prefixes.get(prefix);
	}

	/**
	 * Gets the current mapping of prefixes.
	 * Calls {@link #doAutoLoads()} if no mapping is loaded yet.
	 * @return the current mapping.
	 */
	public Map<String, List<String>> get () {
		// Load the auto-loads if no mapping is defined yet
		if ( prefixes == null ) doAutoLoads();
		return prefixes;
	}

	/**
	 * Performs the auto-loading steps: load the defaults,
	 * then adds the custom prefixes if a file for them is set.
	 */
	public void doAutoLoads () {
		loadDefaults();
		// if there is a path for extra mappings exists: add them
		if ( extraPrefixes != null ) addExtra(extraPrefixes);
	}

	public Map<String, List<String>> loadFromRegistry () {
		InputStream input = null;
		BufferedReader br = null;
		Map<String, List<String>> prefixes = new LinkedHashMap<>();
		try {
			URL url = new URL(REGISTRY_URL);
			input = url.openStream();
			br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
			String line = br.readLine();
			while ( line != null ) {
				line = line.trim();
				// Skip comment or empty lines
				if ( line.isEmpty() || line .startsWith("#") ) {
					line = br.readLine(); // Read next line and loop
					continue;
				}
				// Otherwise: process
				String[] parts = line.split("\t");
				if ( parts.length != 2 ) {
					throw new XLIFFException(String.format("The line '%s' is invalid.", line));
				}
				// Check prefix
				String prefix = parts[1];
				if ( prefix.length() < 2 ) {
					throw new XLIFFException(String.format("The prefix '%s' is too short.", prefix));
				}
				if ( !Util.isValidNmtoken(prefix) ) {
					throw new XLIFFException(String.format("The module or extension prefix '%s' is not an NMTOKEN.", prefix));
				}
				// Add it
				List<String> uris = prefixes.computeIfAbsent(prefix, k -> new ArrayList<>());
				uris.add(parts[0]);
				// Read next line
				line = br.readLine();
			}
		}
		catch ( IOException e ) {
			throw new XLIFFException("Cannot load the prefixes registry.\n"+e.getLocalizedMessage());
		}
		finally {
				try {
					if ( br != null ) br.close();
					if ( input != null ) input.close();
				}
				catch (IOException e) {
					// Let it go
				}
		}
		return prefixes;
	}

}
