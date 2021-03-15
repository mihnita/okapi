/*===========================================================================
  Copyright (C) 2013-2017 by the Okapi Framework contributors
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;

/**
 * Implements a URI supporting the fragment identification mechanism for XLIFF 2. 
 */
public class URIParser {

	private final URIPrefixes uriPrefixes;

	private URI uri;
	private String contextFileId;
	private String contextGroupId;
	private String contextUnitId;
	private char originator;
	private String fileId;
	private String groupId;
	private String unitId;
	private String noteId;
	private String srcInlineId;
	private String trgInlineId;
	private String dataId;
	private boolean external;
	private SimpleEntry<String, List<String>> extensionId;
	private String scope;
	private boolean errorOnUnknownPrefix = true;
	
	/**
	 * Creates a new URIParser object without custom prefixes.
	 */
	public URIParser () {
		uriPrefixes = new URIPrefixes(null);
		reset();
	}
	
	/**
	 * Creates a new empty URIParser object that can use custom prefixes.
	 * @param extraPrefixes the file object where the custom prefixes are declared (or null).
	 */
	public URIParser (File extraPrefixes) {
		uriPrefixes = new URIPrefixes(extraPrefixes);
		reset();
	}
	
	/**
	 * Creates a new URIParser object for a given URI.
	 * @param urilOrFragment the URI to process (can be empty).
	 * @throws InvalidParameterException if there is an error in the fragment syntax.
	 */
	public URIParser (String urilOrFragment) {
		this(urilOrFragment, null, null, null);
	}

	/**
	 * Creates a new URIParser object for a given URI and context identifiers.
	 * @param urilOrFragment the URI to process (can be empty).
	 * @param contextFileId the id of the &lt;file&gt; element enclosing where this URI is located (or null).
	 * @param contextGroupId the id of the &lt;group&gt; element enclosing where this URI is located (or null).
	 * @param contextUnitId the id of the &lt;unit&gt; element enclosing where this URI is located (or null).
	 * @throws InvalidParameterException if there is an error in the fragment syntax.
	 */
	public URIParser (String urilOrFragment,
		String contextFileId,
		String contextGroupId,
		String contextUnitId)
	{
		uriPrefixes = new URIPrefixes();
		setURL(urilOrFragment, contextFileId, contextGroupId, contextUnitId);
	}
	
	@Override
	public String toString () {
		if ( uri == null ) return "null";
		return uri.toString();
	}

	/**
	 * Sets the option of generating or not an error when finding an unknown prefix.
	 * @param errorOnUnknownPrefix true to generate an error, false to not generate an error.
	 */
	public void setErrorOnUnknownPrefix (boolean errorOnUnknownPrefix) {
		this.errorOnUnknownPrefix = errorOnUnknownPrefix;
	}

	/**
	 * Gets the option to generate or not an error when finding an unknown prefix.
	 * @return true when the option is to generate an error, false otherwise.
	 */
	public boolean getErrorOnUnknownPrefix () {
		return errorOnUnknownPrefix;
	}
	
	/**
	 * Adds a collection of uri/prefix to the prefixes resolver associated with this object.
	 * @param map the map to add.
	 */
	public void addPrefixes (Map<String, String> map) {
		uriPrefixes.add(map);
	}
	
	/**
	 * Sets the URI for this object.
	 * @param urilOrFragment the URI to process (can be empty).
	 * @return the object itself.
	 * @throws InvalidParameterException if there is an error in the fragment syntax.
	 */
	public URIParser setURL (String urilOrFragment) {
		return setURL(urilOrFragment, null, null, null);
	}

	/**
	 * Sets the URI and context identifiers for this object.
	 * @param urilOrFragment the URI to process (can be empty).
	 * @param contextFileId the id of the &lt;file&gt; element enclosing where this URI is located (or null).
	 * @param contextGroupId the id of the &lt;group&gt; element enclosing where this URI is located (or null).
	 * @param contextUnitId the id of the &lt;unit&gt; element enclosing where this URI is located (or null).
	 * @return the object itself.
	 * @throws InvalidParameterException if there is an error in the fragment syntax.
	 */
	public URIParser setURL (String urilOrFragment,
		String contextFileId,
		String contextGroupId,
		String contextUnitId)
	{
		this.contextFileId = contextFileId;
		this.contextGroupId = contextGroupId;
		this.contextUnitId = contextUnitId;
		if (( contextUnitId != null ) || ( contextGroupId != null )) {
			if ( contextFileId == null ) {
				throw new InvalidParameterException("Context file id must be set when a context unit/group id is set.");
			}
		}
		// Guess the container where the reference was from based on what identifiers
		// are available when we set the fragment identifier
		if ( contextUnitId != null ) originator = 'u';
		else if ( contextGroupId != null ) originator = 'g';
		else if ( contextFileId != null ) originator = 'f';
		else originator = '_';
		// Validate and parse the URI
		parse(urilOrFragment);
		return this;
	}
	
	/**
	 * Gets the URI object for this object.
	 * @return the URI object for this object.
	 */
	public URI getURI () {
		return uri;
	}
	
	private void reset () {
		// Reset all IDs
		fileId = groupId = unitId = noteId = srcInlineId = trgInlineId = dataId = null;
		extensionId = null;
		external = false;
		uri = null;
		scope = "";
	}

	private void parse (String urilOrFragment) {
		reset();
		try {
			uri = new URI(urilOrFragment);
		}
		catch ( URISyntaxException e ) {
			throw new InvalidParameterException(e.getLocalizedMessage());
		}
		
		// Guess if it's a fragment-only URI as well as if it's an XLIFF fragment identifier
		String path = uri.getPath();
		String frag = uri.getFragment();
		if ( !Util.isNoE(path) ) {
			external = true;
			// If there is a path
			if ( !Util.isNoE(frag) ) {
				// If it's an external reference: it must have a file id to be XLIFF
				if ( !frag.contains("f=") ) return;
			}
		}
		if ( frag == null ) return;
		
		if ( frag.indexOf(' ') != -1 ) {
			throw new InvalidParameterException("Spaces not allowed in fragment identifier.");
		}
		// Absolute or not
		boolean absolute = false;
		if ( frag.startsWith("/") ) {
			absolute = true;
			frag = frag.substring(1);
		}
		// Split into parts
		String[] parts = frag.split("/", 0);

		// Process the parts
		for ( String part : parts ) {
			int n = part.indexOf('=');
			if ( n == -1 ) {
				srcInlineId = check(srcInlineId, part, null, 's', frag);
				scope += "s";
				continue;
			}
			// Else: prefix=id pair
			String prefix = part.substring(0, n);
			String value = part.substring(n+1);
			switch ( prefix ) {
			case "f":
				fileId = check(fileId, value, prefix, 'f', frag);
				scope += "f";
				continue;
			case "g":
				groupId = check(groupId, value, prefix, 'g', frag);
				scope += "g";
				continue;
			case "u":
				unitId = check(unitId, value, prefix, 'u', frag);
				scope += "u";
				continue;
			case "n":
				noteId = check(noteId, value, prefix, 'n', frag);
				scope += "n";
				continue;
			case "t":
				trgInlineId = check(trgInlineId, value, prefix, 't', frag);
				scope += "t";
				continue;
			case "d":
				dataId = check(dataId, value, prefix, 'd', frag);
				scope += "d";
				continue;
			default:
				if ( prefix.length() < 2 ) {
					throw new InvalidParameterException(
						String.format("The module or extension prefix '%s' must be longer than 1 character.", prefix));
				}
				if ( !Util.isValidNmtoken(prefix) ) {
					throw new InvalidParameterException(
						String.format("The module or extension prefix '%s' is not an NMTOKEN.", prefix));
				}
				List<String> uris = uriPrefixes.resolve(prefix);
				if ( uris == null ) {
					// Invalid or undefined prefix
					if ( errorOnUnknownPrefix ) {
						throw new InvalidParameterException(
							String.format("The prefix '%s' in '%s' is not recognized.\nRecognized prefixes are: "
								+ "f, g, u, n, d, t and the following modules or extension prefixes '%s'.",
								prefix, frag, uriPrefixes.get().toString()));
					}
				}
				// Set the information (URIs may be null depending on errorOnUnknownPrefix)
				check(extensionId==null ? null : extensionId.getKey(), value, prefix, 'x', frag);
				extensionId = new SimpleEntry<>(value, uris);
				scope += "x";
			}
		}
		
		// Check if absolute is allowed
		if ( absolute ) {
			if ( fileId == null ) {
				throw new InvalidParameterException("An absolute fragment identifier without a file selector is invalid.");
			}
		}
		
		//--- Extra checks
		// 't', 'd' and 's' must have a unit container
		if (( srcInlineId != null ) || ( trgInlineId != null ) || ( dataId != null )) {
			if ( unitId == null ) unitId = contextUnitId;
			if ( unitId == null ) {
				throw new InvalidParameterException(String.format(
					"The un-prefixed selectors or 't' or 'd' selectors require a specified or context unit id ('%s').",
					frag));
			}
		}
		if (( unitId != null ) || ( groupId != null )) {
			if ( fileId == null ) fileId = contextFileId;
			if ( fileId == null ) {
				throw new InvalidParameterException(String.format(
					"The 'g' or 'u' selectors require a specified or context file id ('%s').",
					frag));
			}
		}
		if (( noteId != null ) || ( extensionId != null )) {
			if ( fileId == null ) fileId = contextFileId;
			if ( groupId == null ) groupId = contextGroupId;
			if ( unitId == null ) unitId = contextUnitId;
			if (( fileId == null ) && ( groupId == null ) && ( fileId == null )) {
				throw new InvalidParameterException(String.format(
					"The 'n' selectors or the modules or extensions selectors require a specified or context id for either file, group or unit ('%s').",
					frag));
			}
		}
	}
	
	/**
	 * Changes this object to represent a complete fragment identifier if the initial one was relative.
	 * This updates the value for {@link #getRefContainer()}.
	 * @return the object itself.
	 */
	public URIParser complementReference () {
		StringBuilder sb = new StringBuilder(uri.getFragment());
		switch ( getRefType() ) {
		case 'f':
			// Nothing to add
			break;
		case 'g':
		case 'u':
			// May need file
			if ( getRefContainer() == '_' ) sb.insert(0, "f="+fileId+"/");
			break;
		case 'n':
		case 'x':
			// May need unit/group or file
			if ( getRefContainer() == '_' ) {
				if ( originator == 'u' ) sb.insert(0, "u="+unitId+"/");
				else if ( originator == 'g' ) sb.insert(0, "g="+groupId+"/");
			}
			if (( uri.getFragment() != null ) && !uri.getFragment().contains("f=") ) {
				// And add the file
				sb.insert(0, "f="+fileId+"/");
			}
			break;
		case 's':
		case 't':
		case 'd':
			// May need unit
			if ( getRefContainer() == '_' ) {
				sb.insert(0, "u="+unitId+"/");
				// And add the file
				sb.insert(0, "f="+fileId+"/");
			}
			break;
		default:
			// Not really possible as the reference has been parsed already
			break;
		}

		// Update the fragment part of the URI
		// and re-parse it
		String tmp = uri.toString();
		int n = tmp.indexOf('#');
		if ( n != -1 ) {
			tmp = tmp.substring(0, n+1)+sb.toString();
			parse(tmp);
		}
		return this;
	}
	
	private String check (String current,
		String value,
		String prefix,
		char selectorType,
		String frag)
	{
		// Already defined?
		if ( current != null ) {
			if ( prefix == null ) {
				throw new InvalidParameterException(
					String.format("Duplicated un-prefixed selector in '%s'.", frag));
			}
			throw new InvalidParameterException(
				String.format("Duplicated selector '%s' in '%s'.", prefix, frag));
		}
		
		// Check for allowed selector in the current scope
		boolean allowed = true;
		if ( scope.endsWith("f") ) {
			// All allowed
		}
		else if ( scope.endsWith("g") ) {
			allowed = ("f".indexOf(selectorType)==-1);
		}
		else if ( scope.endsWith("u") ) {
			allowed = ("fg".indexOf(selectorType)==-1);
		}
		else if ( !scope.isEmpty() ) {
			// // All other should be terminal
			allowed = false;
		}
		if ( !allowed ) {
			if ( prefix == null ) {
				throw new InvalidParameterException(
					String.format("The un-prefixed selector is at an invalid position '%s'", frag));
			}
			// Else: with prefix 
			throw new InvalidParameterException(
				String.format("The selector with the prefix '%s' is at an invalid position in '%s'", prefix, frag));
		}
		// Valid value?
		if ( !Util.isValidNmtoken(value) ) {
			throw new InvalidParameterException(
				String.format("The id '%s' is not a valid NMTOKEN.", value));
		}
		return value;
	}
	
	/**
	 * Indicates if this URI has only a fragment.
	 * @return true if this URI has only a fragment, false otherwise.
	 */
	public boolean isFragmentOnly () {
		return !external;
	}
	
	/**
	 * Indicates if this URI has an XLIFF 2 fragment identifier.
	 * @return true if this URI has an XLIFF 2 fragment identifier, false if not.
	 */
	public boolean isXLIFF () {
		return !scope.isEmpty(); 
	}
	
	/**
	 * Gets the file identifier of this URI's fragment.
	 * @return the file identifier.
	 */
	public String getFileId() {
		return fileId;
	}
	
	/**
	 * Gets the group identifier of this URI's fragment.
	 * @return the group identifier.
	 */
	public String getGroupId () {
		return groupId;
	}
	
	/**
	 * Gets the unit identifier of this URI's fragment.
	 * @return the unit identifier.
	 */
	public String getUnitId () {
		return unitId;
	}
	
	/**
	 * Gets the note identifier of this URI's fragment.
	 * @return the note identifier.
	 */
	public String getNoteId () {
		return noteId;
	}
	
	/**
	 * Gets the segment or ignorable or source inline element of this URI's fragment.
	 * @return the segment, ignorable or source inline element identifier.
	 */
	public String getSourceInlineId () {
		return srcInlineId;
	}

	/**
	 * Gets the target inline element identifier of this URI's fragment.
	 * @return the target inline element identifier.
	 */
	public String getTargetInlineId () {
		return trgInlineId;
	}
	
	/**
	 * Gets the original data identifier of this URI's fragment.
	 * @return the original data identifier.
	 */
	public String getDataId () {
		return dataId;
	}
	
	/**
	 * Gets the context file identifier of this URI's fragment.
	 * @return the context file identifier.
	 */
	public String getContextFileId() {
		return contextFileId;
	}
	
	/**
	 * Gets the context group identifier of this URI's fragment.
	 * @return the context group identifier.
	 */
	
	public String getContextGroupId () {
		return contextGroupId;
	}
	
	/**
	 * Gets the context unit identifier of this URI's fragment.
	 * @return the context unit identifier.
	 */
	public String getContextUnitId () {
		return contextUnitId;
	}
	
	/**
	 * Gets the extension information for this fragment.
	 * It is provided as a SimpleEntry object where:
	 * <ul>
	 * <li>getKey() gives the extension id,
	 * <li>getValue() gets the list of namespace URIs associated to the prefix,
	 * </ul>
	 * @return the information or null.
	 */
	public SimpleEntry<String, List<String>> getExtensionInfo () {
		return extensionId;
	}

	/**
	 * Gets the scope string for this URI's fragment.
	 * @return the scope string for this URI's fragment.
	 */
	public String getScope () {
		return scope;
	}
	
	/**
	 * Gets the type of reference for this URI's fragment.
	 * This can be: 'f' (file),
	 * 'g' (group), 'u' (unit), 'n' (note),
	 * 'd' (data), 't' (target inline element),
	 * 's' (segment, ignorable, or source inline element,
	 *  or 'x' (module or extension element). 
	 * @return the type of reference specified (or '_' if undefined).
	 */
	public char getRefType () {
		if ( scope.isEmpty() ) return '_';
		return scope.charAt(scope.length()-1);
	}

	/**
	 * Gets the type of container for this URI reference.
	 * This can return 'f' (file), 'g' (group) or 'u' (unit).
	 * @return the type of container for this reference (or '_' if undefined).
	 */
	public char getRefContainer () {
		if ( scope.length() < 2 ) return '_';
		return scope.charAt(scope.length()-2);
	}

}
