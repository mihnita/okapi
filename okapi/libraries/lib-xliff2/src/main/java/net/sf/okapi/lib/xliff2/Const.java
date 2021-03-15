/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

/**
 * Provides various constants for useful names.
 */
public class Const {

	/**
	 * URI for the XML namespace.
	 */
	public static final String NS_XML = "http://www.w3.org/XML/1998/namespace";

	/**
	 * URI for the XLIFF 2.0 namespace.
	 */
	public static final String NS_XLIFF_CORE20 = "urn:oasis:names:tc:xliff:document:2.0";
	@Deprecated
	public static final String NS_XLIFF20_CORE = "urn:oasis:names:tc:xliff:document:2.0";

	/**
	 * Starting part of all the XLIFF 2 modules namespaces.
	 */
	public static final String NS_XLIFF_MODSTART = "urn:oasis:names:tc:xliff:";

	/**
	 * URI for the XLIFF Matches module 2.0
	 */
	public static final String NS_XLIFF_MATCHES20 = "urn:oasis:names:tc:xliff:matches:2.0";
	@Deprecated
	public static final String NS_XLIFF20_MATCHES = "urn:oasis:names:tc:xliff:matches:2.0";
	public static final String PREFIX_MATCHES = "mtc";
	public static final String PREFIXCOL_MATCHES = PREFIX_MATCHES+":";

	/**
	 * URI for the XLIFF Glossary module 2.0
	 */
	public static final String NS_XLIFF_GLOSSARY20 = "urn:oasis:names:tc:xliff:glossary:2.0";
	@Deprecated
	public static final String NS_XLIFF20_GLOSSARY = "urn:oasis:names:tc:xliff:glossary:2.0";
	public static final String PREFIX_GLOSSARY = "gls";
	public static final String PREFIXCOL_GLOSSARY = PREFIX_GLOSSARY+":";

	/**
	 * URI for the XLIFF Format Style module 2.0
	 */
	public static final String NS_XLIFF_FS20 = "urn:oasis:names:tc:xliff:fs:2.0";
	@Deprecated
	public static final String NS_XLIFF20_FS = "urn:oasis:names:tc:xliff:fs:2.0";
	public static final String PREFIX_FS = "fs";
	public static final String PREFIXCOL_FS = PREFIX_FS+":";

	/**
	 * URI for the XLIFF Metadata module 2.0
	 */
	public static final String NS_XLIFF_METADATA20 = "urn:oasis:names:tc:xliff:metadata:2.0";
	@Deprecated
	public static final String NS_XLIFF20_METADATA = "urn:oasis:names:tc:xliff:metadata:2.0";
	public static final String PREFIX_METADATA = "mda";
	public static final String PREFIXCOL_METADATA = PREFIX_METADATA+":";

	/**
	 * URI for the XLIFF Resource Data module 2.0
	 */
	public static final String NS_XLIFF_RESDATA20 = "urn:oasis:names:tc:xliff:resourcedata:2.0";
	@Deprecated
	public static final String NS_XLIFF20_RESDATA = "urn:oasis:names:tc:xliff:resourcedata:2.0";
	public static final String PREFIX_RESDATA = "res";
	public static final String PREFIXCOL_RESDATA = PREFIX_RESDATA+":";

	/**
	 * URI for the XLIFF Change Tracking module 2.0
	 */
	public static final String NS_XLIFF_TRACKING20 = "urn:oasis:names:tc:xliff:changetracking:2.0";
	@Deprecated
	public static final String NS_XLIFF20_TRACKING = "urn:oasis:names:tc:xliff:changetracking:2.0";
	public static final String PREFIX_TRACKING = "ctr";
	public static final String PREFIXCOL_TRACKINGSd = PREFIX_TRACKING+":";

	/**
	 * URI for the XLIFF Size and Length Restriction module 2.0
	 */
	public static final String NS_XLIFF_SIZE20 = "urn:oasis:names:tc:xliff:sizerestriction:2.0";
	@Deprecated
	public static final String NS_XLIFF20_SIZE = "urn:oasis:names:tc:xliff:sizerestriction:2.0";
	public static final String PREFIX_SIZE = "ctr";
	public static final String PREFIXCOL_SIZE = PREFIX_SIZE+":";

	/**
	 * URI for the XLIFF Validation module 2.0
	 */
	public static final String NS_XLIFF_VALIDATION20 = "urn:oasis:names:tc:xliff:validation:2.0";
	@Deprecated
	public static final String NS_XLIFF20_VALIDATION = "urn:oasis:names:tc:xliff:validation:2.0";
	public static final String PREFIX_VALIDATION = "val";
	public static final String PREFIXCOL_VALIDATION = PREFIX_VALIDATION+":";
	
	/**
	 * URI for the ITS 2.0 namespace.
	 */
	public static final String NS_ITS = "http://www.w3.org/2005/11/its";
	public static final String PREFIX_ITS = "its";
	public static final String PREFIXCOL_ITS = PREFIX_ITS+":";
	public static final String NS_ITSXLF = "http://www.w3.org/ns/its-xliff/";
	public static final String PREFIX_ITSXLF = "itsxlf";
	public static final String PREFIXCOL_ITSXLF = PREFIX_ITSXLF+":";

	/**
	 * URI for the Okapi XLIFF extensions namespace.
	 */
	public static final String NS_XLIFFOKAPI = "okapi-framework:xliff-extensions";
	public static final String PREFIX_XLIFFOKAPI = "okp";
	public static final String PREFIXCOL_XLIFFOKAPI = PREFIX_XLIFFOKAPI+":";

	public static final String VALUE_YES = "yes";
	public static final String VALUE_NO = "no";
	
	public static final String VALUE_FIRSTNO = "firstNo";

	public static final String VALUE_AUTO = "auto";
	public static final String VALUE_LTR = "ltr";
	public static final String VALUE_RTL = "rtl";
	
	public static final String ELEM_XLIFF = "xliff";
	public static final String ELEM_FILE = "file";
	public static final String ELEM_SKELETON = "skeleton";
	public static final String ELEM_GROUP = "group";
	public static final String ELEM_UNIT = "unit";
	public static final String ELEM_SEGMENT = "segment";
	public static final String ELEM_IGNORABLE = "ignorable";
	public static final String ELEM_SOURCE = "source";
	public static final String ELEM_TARGET = "target";
	public static final String ELEM_CANDIDATES = "matches";
	public static final String ELEM_CANDIDATE = "match";
	public static final String ELEM_GLOSSENTRY = "glossEntry";
	public static final String ELEM_CUSTPROP = "meta";
	public static final String ELEM_TERM = "term";
	public static final String ELEM_TRANSLATION = "translation";
	public static final String ELEM_DEFINITION = "definition";
	public static final String ELEM_NOTES = "notes";
	public static final String ELEM_NOTE = "note";
	public static final String ELEM_PLACEHOLDER = "ph";
	public static final String ELEM_PAIREDCODES = "pc";
	public static final String ELEM_OPENINGCODE = "sc";
	public static final String ELEM_CLOSINGCODE = "ec";
	public static final String ELEM_PAIREDANNO = "mrk";
	public static final String ELEM_OPENINGANNO = "sm";
	public static final String ELEM_CLOSINGANNO = "em";
	public static final String ELEM_CP = "cp";
	public static final String ELEM_ORIGINALDATA = "originalData";
	public static final String ELEM_DATA = "data";

	public static final String ATTR_ID = "id";
	public static final String ATTR_VERSION = "version";
	public static final String ATTR_STARTREF = "startRef";
	public static final String ATTR_DATAREF = "dataRef";
	public static final String ATTR_TYPE = "type";
	public static final String ATTR_SUBTYPE = "subType";
	public static final String ATTR_ISOLATED = "isolated";
	public static final String ATTR_CANOVERLAP = "canOverlap";
	public static final String ATTR_HEX = "hex";
	public static final String ATTR_SRCDIR = "srcDir";
	public static final String ATTR_TRGDIR = "trgDir";
	public static final String ATTR_DIR = "dir";
	public static final String ATTR_EQUIV = "equiv";
	public static final String ATTR_EQUIVSTART = "equivStart";
	public static final String ATTR_EQUIVEND = "equivEnd";
	public static final String ATTR_DISP = "disp";
	public static final String ATTR_DISPSTART = "dispStart";
	public static final String ATTR_DISPEND = "dispEnd";
	public static final String ATTR_SUBFLOWS = "subFlows";
	public static final String ATTR_SUBFLOWSSTART = "subFlowsStart";
	public static final String ATTR_SUBFLOWSEND = "subFlowsEnd";
	public static final String ATTR_DATAREFSTART = "dataRefStart";
	public static final String ATTR_DATAREFEND = "dataRefEnd";
	public static final String ATTR_SRCLANG = "srcLang";
	public static final String ATTR_TRGLANG = "trgLang";
	public static final String ATTR_APPLIESTO = "appliesTo";
	public static final String ATTR_VALUE = "value";
	public static final String ATTR_REF = "ref";
	public static final String ATTR_TRANSLATE = "translate";
	public static final String ATTR_ORDER = "order";
	public static final String ATTR_CANDELETE = "canDelete";
	public static final String ATTR_CANREORDER = "canReorder";
	public static final String ATTR_CANCOPY = "canCopy";
	public static final String ATTR_ORIGINAL = "original";
	public static final String ATTR_CANRESEGMENT = "canResegment";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_STATE = "state";
	public static final String ATTR_SUBSTATE = "subState";
	public static final String ATTR_PRIORITY = "priority";
	public static final String ATTR_CATEGORY = "category";
	public static final String ATTR_COPYOF = "copyOf";
	public static final String ATTR_HREF = "href";
	public static final String ATTR_SIMILARITY = "similarity";
	public static final String ATTR_ORIGIN = "origin";
	
}
