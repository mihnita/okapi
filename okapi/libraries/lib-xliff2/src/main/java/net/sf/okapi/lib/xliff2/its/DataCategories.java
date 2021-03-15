/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.its;

/**
 * Represents the identifiers for the different
 * <a href='http://www.w3.org/TR/its20/#datacategories-overview'>types of ITS data categories</a>.
 */
public class DataCategories {

	public static final String LOCQUALITYISSUE = "localization-quality-issue";
	public static final String PROVENANCE = "provenance";
	public static final String DOMAIN = "domain";
	public static final String TEXTANALYSIS = "text-analysis";
	public static final String MTCONFIDENCE = "mt-confidence";
	
	public static final String TERMINOLOGY = "terminology";
	
	public static final String TRANSLATE = "translate";
	
	public static final String LIST = ";translate;localization-note;terminology;directionality;language-information;"
		+ "elements-within-text;domain;text-analysis;locale-filter;provenance;external-resource;target-pointer;"
		+ "id-value;preserve-space;localization-quality-issue;localization-quality-rating;mt-confidence;"
		+ "allowed-characters;storage-size;";
}
