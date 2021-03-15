/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.resource.TextFragment.TagType;

public final class TextFragmentUtil {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	public static final int MAX_INLINE_CODES = 6127;

	/**
	 * Store the missing, added or modified codes in the target as compared to
	 * the source.
	 * <p>
	 * To assure consistent ids and code data run
	 * {@link TextFragment#alignCodeIds(TextFragment)} first.
	 * 
	 * @param source
	 *            - source {@link TextFragment}, use as the standard to compare
	 *            against.
	 * @param target
	 *            - target {@link TextFragment} to compare codes with source.
	 * @return {@link CodeAnomalies} or null if no anomalies.
	 */
	static public CodeAnomalies catalogCodeAnomalies(TextFragment source, TextFragment target) {
		return catalogCodeAnomalies(source, target, true);
	}

	/**
	 * Store the missing or added codes in the target as compared to the source.
	 * <p>
	 * To assure consistent ids and code data run
	 * {@link TextFragment#alignCodeIds(TextFragment)} first.
	 * 
	 * @param source
	 *            - source {@link TextFragment}, use as the standard to compare
	 *            against.
	 * @param target
	 *            - target {@link TextFragment} to compare codes with source.
	 * @param includeDeletable
	 *            - do we count deletable codes as missing? True by default.
	 * @return {@link CodeAnomalies} or null if no anomalies.
	 */
	static public CodeAnomalies catalogCodeAnomalies(TextFragment source, TextFragment target,
			boolean includeDeletable) {
		CodeAnomalies anomalies = new CodeAnomalies();

		// both are null, no anomalies
		if (source == null && target == null) {
			return null;
		}

		if (source == null) {
			// target isn't null
			if (!target.hasCode()) {
				return null;
			}
			for (Code c : target.getCodes()) {
				if (!c.isAdded()) {
					anomalies.addAddedCode(c);
				}
				return anomalies;
			}
		}

		if (target == null) {
			// source isn't null
			if (source == null || !source.hasCode()) {
				return null;
			}
			for (Code c : source.getCodes()) {
				anomalies.addMissingCode(c);
				return anomalies;
			}
		}

		// remaining codes in targetCodeSet are not found in the source
		Set<Code> sourceCodeSet = new TreeSet<>(new CodeComparatorOnData());
		Set<Code> targetCodeSet = new TreeSet<>(new CodeComparatorOnData());
		if (source != null) {
			sourceCodeSet.addAll(source.getCodes());
		}

		if (target != null) {
			targetCodeSet.addAll(target.getCodes());
		}

		targetCodeSet.removeAll(sourceCodeSet);
		for (Code c : targetCodeSet) {
			// if the code is marked as added then don't add to error list
			// if the code is deletable and we want to detect such codes
			// don't add to error list 
			//FIXME: Shouldn't deletable checks be for missing codes case, not added?			
			if (c.isAdded() || (c.isDeleteable() && !includeDeletable)) {
				continue;
			}
			anomalies.addAddedCode(c);
		}

		// remaining codes in sourceCodeSet are not found in the target
		if (target != null) {
			targetCodeSet.addAll(target.getCodes());
		}
		sourceCodeSet.removeAll(targetCodeSet);
		for (Code c : sourceCodeSet) {
			if (c.isDeleteable() && !includeDeletable) {
				continue;
			}
			anomalies.addMissingCode(c);
		}

		if (anomalies.hasAddedCodes() || anomalies.hasMissingCodes()) {
			return anomalies;
		}

		return null;
	}

	static public boolean moreThanMaxCodes(TextFragment tf) {
		List<Code> codes = tf.getCodes();
		return codes.size() > MAX_INLINE_CODES;
	}

	static public TextFragment removeMoreThanMaxCodes(TextFragment tf) {
		int lastCodeIndex = 0;
		int lastCharIndex = 0;
		String codedText = tf.getCodedText();
		StringBuilder newCodedText = new StringBuilder();		
		List<Code> codes = tf.getCodes();
		
		for (int i = 0; i < codedText.length(); i++) {
			int c = codedText.codePointAt(i);	
			if ((c == TextFragment.MARKER_OPENING || c == TextFragment.MARKER_CLOSING || c == TextFragment.MARKER_ISOLATED)) {
				if (lastCodeIndex > MAX_INLINE_CODES-1) {
					lastCharIndex = i-1;
					break;
				} else {	
					switch (c) {
					case TextFragment.MARKER_OPENING:
						newCodedText.append(""+((char)TextFragment.MARKER_OPENING)+codedText.charAt(i+1));
						lastCodeIndex = TextFragment.toIndex(codedText.charAt(i+1));
						i++;
						break;
					case TextFragment.MARKER_CLOSING:
						newCodedText.append(""+((char)TextFragment.MARKER_CLOSING)+codedText.charAt(i+1));
						lastCodeIndex = TextFragment.toIndex(codedText.charAt(i+1));
						i++;
						break;
					case TextFragment.MARKER_ISOLATED:
						newCodedText.append(""+((char)TextFragment.MARKER_ISOLATED)+codedText.charAt(i+1));
						lastCodeIndex = TextFragment.toIndex(codedText.charAt(i+1));
						i++;
						break;					
					}					
				}
			} else {
				newCodedText.appendCodePoint(c);
			}
		}
			
		// remove all remaining codes
		for (int i = lastCharIndex; i < codedText.length(); i++) {
			int c = codedText.codePointAt(i);	
			if ((c == TextFragment.MARKER_OPENING || c == TextFragment.MARKER_CLOSING || c == TextFragment.MARKER_ISOLATED)) {
				// skip code its past the MAX
				i++;
			} else {
				newCodedText.appendCodePoint(c);
			}
		}									
		
		// remove all max codes to the end
		return new TextFragment(newCodedText.toString(), codes.subList(0, lastCodeIndex+1));
	}
	
	/**
	 * Render the {@link TextFragment} including all {@link Code}s.
	 * differs from TextFragment.toText() by also using outerData.
	 * @param tf The TextFragment to render
	 * @return the rendered string
	 */
	static public String toText(TextFragment tf) {
		if (( tf.codes == null ) || ( tf.codes.size() == 0 )) return tf.toString();
		if ( !tf.isBalanced ) tf.balanceMarkers();
		StringBuilder tmp = new StringBuilder();
		Code code;
		for ( int i=0; i<tf.length(); i++ ) {
			switch ( tf.charAt(i) ) {
			case TextFragment.MARKER_OPENING:
			case TextFragment.MARKER_CLOSING:
			case TextFragment.MARKER_ISOLATED:
				code = tf.codes.get(TextFragment.toIndex(tf.charAt(++i)));
				tmp.append(code.getOuterData());
				break;
			default:
				tmp.append(tf.charAt(i));
				break;
			}
		}
		return tmp.toString();
	}
}
