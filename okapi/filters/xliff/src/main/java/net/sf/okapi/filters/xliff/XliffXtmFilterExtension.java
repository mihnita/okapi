/*===========================================================================
  Copyright (C) 2020 by the Okapi Framework contributors
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
=========================================================================== */

package net.sf.okapi.filters.xliff;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.AltTranslation;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;

/**
 * Extension to the XLIFFFilter to handle XTM specific metadata in the XLIFF
 * document.
 * 
 * It seems that XTM uses the following:
 * <ul>
 * <li>state-qualifier="exact-match"         --> Context match
 * <li>state-qualifier="leveraged-tm"        --> 100% match if has alt-trans with match-quality="100%"
 * <li>state-qualifier="leveraged-tm"        --> Fuzzy if has alt-trans with match-quality="100%"
 * <li>state-qualifier="fuzzy-match"         --> Fuzzy
 * <li>state-qualifier="leveraged-inherited" --> Repetition (origin in alt-trans has the reference number)
 * <li>state-qualifier="x-fuzzy-forward"     --> Fuzzy repetition (origin in alt-trans has the reference number)
 * <li>state-qualifier="x-alphanumeric"      --> No translation to do (normally)
 * <li>state-qualifier="x-numeric"           --> No translation to do (normally)
 * <li>state-qualifier="x-measurement"       --> No translation to do (normally)
 * <li>state-qualifier="x-punctuation"       --> No translation to do (normally)
 * <li>state-qualifier="x-manual-notrans"    --> No translation to do -> locked
 * </ul>
 */
public class XliffXtmFilterExtension {

	// Namespaces
	public static final String XTM_NAMESPACE_URI = "urn:xliff-xtm-extensions";

	// Attributes
	public static final String XTM_POPULATE_TRG_WITH_SRC = "populate-target-with-source";

	// Attribute values for state-qualifier (in addition to the standard ones)
	public static final String SQ_XTM_ALPHANUMERIC = "x-alphanumeric";
	public static final String SQ_XTM_NUMERIC = "x-numeric";
	public static final String SQ_XTM_PUNCTUATION = "x-punctuation";
	public static final String SQ_XTM_MEASUREMENT = "x-measurement";
	public static final String SQ_XTM_MANUAL_NOTRANS = "x-manual-notrans";
	public static final String SQ_XTM_FUZZY_FORWARD = "x-fuzzy-forward";

	// Properties
	public static final String PROP_XTM_PERCENT = "xtm_percent";
	public static final String PROP_XTM_LOCKED = "xtm_locked";

	/**
	 * Consolidates all information for XTM at the segment-level.
	 * @param tu the text unit where the segment to fix is located (1 segment per text unit in XTM)
	 * @param trgLocId the locale ID of the target language.
	 */
	public static void consolidateProperties (ITextUnit tu,
		LocaleId trgLocId)
	{
		TextContainer tc = tu.getTarget(trgLocId);
		if ( tc == null ) return;
		
		AltTranslation bestAlt = null;
		AltTranslation bestRef = null;
		Segment trgSeg = tc.getFirstSegment(); // Only 1 segment per TU in XTM XLIFF
		
		Property prop = tc.getProperty(XLIFFFilter.STATE_QUALIFIER);
		if ( prop != null ) {
			switch ( prop.getValue() ) {
			case XLIFFFilter.EXACT_MATCH:
			case XLIFFFilter.FUZZY_MATCH:
			case XLIFFFilter.LEVERAGED_TM:
			case XLIFFFilter.LEVERAGED_INHERITED:
			case XliffXtmFilterExtension.SQ_XTM_FUZZY_FORWARD:
				AltTranslationsAnnotation ata = tc.getAnnotation(AltTranslationsAnnotation.class);
				if ( ata == null ) return;
				int bestValue = -1;
				int bestValueRef = -1;
				for ( AltTranslation alt : ata ) {
					if ( XLIFFFilter.LEVERAGED_INHERITED.equals(alt.getExType()) ) {
						if ( alt.getCombinedScore() > bestValueRef ) {
							bestValueRef = alt.getCombinedScore();
							bestRef = alt;
						}
					}
					if ( alt.getCombinedScore() > bestValue ) {
						bestValue = alt.getCombinedScore();
						bestAlt = alt;
					}
				}

				if ( bestRef != null ) {
					trgSeg.setProperty(new Property(PROP_XTM_PERCENT, ""+bestRef.getCombinedScore()));
				}
				else if ( bestAlt != null ) {
					trgSeg.setProperty(new Property(PROP_XTM_PERCENT, ""+bestAlt.getCombinedScore()));
				}
				break;
				
			case SQ_XTM_ALPHANUMERIC:
			case SQ_XTM_NUMERIC:
			case SQ_XTM_MEASUREMENT:
			case SQ_XTM_PUNCTUATION:
				break;
			case SQ_XTM_MANUAL_NOTRANS:
				trgSeg.setProperty(new Property(PROP_XTM_LOCKED, "true"));
				break;
			}
		}
		
		// Detect if the target is empty because it refers to some kind of repetition
		// Also, make sure the source is not, just in case.
		if ( trgSeg.getContent().isEmpty() && !tu.getSource().getFirstSegment().getContent().isEmpty() ) {
			if ( bestRef != null ) { // We did find a reference to use
				trgSeg.setProperty(new Property(XLIFFFilter.PROP_REPETITION, bestRef.getOrigin()));
			}
		}
		
	}

}