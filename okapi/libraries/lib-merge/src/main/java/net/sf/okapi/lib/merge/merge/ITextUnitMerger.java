/*===========================================================================
  Copyright (C) 2008-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.merge.merge;

import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnit;

public interface ITextUnitMerger {

	/**
	 * Merges the translated text unit to the one from the skeleton. Merges the text unit's target
	 * (under certain conditions).
	 *
	 * @param tuFromSkeleton text unit from the skeleton (normally the original file)
	 * @param tuFromTranslation text unit from the translation (default merger uses xliff 1.2)
	 * @return the merged text unit
	 */
	ITextUnit mergeTargets(final ITextUnit tuFromSkeleton, final ITextUnit tuFromTranslation);

	/**
	 * Update the meta fields in {@link Code}'s "{@code to}" that match those of "{@code from}" But only if
	 * the "to" code has empty data. Otherwise keep the to code data as-is.
	 * Codes match if both data and {@link TagType} are the same.
	 *
	 * @param from {@link TextFragment} codes are used to match codes in "to"
	 * @param to {@link TextFragment} that has its code id's updated to match "from"
	 */
	void copyCodeMeta(final TextFragment source, final TextFragment target) ;

	/**
	 * Update the meta fields from {@link TextPart}'s in "{@code to}" that match those of "{@code from}"
	 * <p>
	 * <b> WARNING: Not all TextParts have id's to match on. Fallback is to match on data, but
	 * this can be ambiguous in rare cases</b>
	 * @param from list of {@link TextPart} codes are used to match codes in "to"
	 * @param to list of {@link TextPart} that has its code id's updated to match "from"
	 * @param id parent {@link TextUnit} id
	 */
	void copyTextPartMeta(final List<TextPart> from, final List<TextPart> to, final String id);

	void setTargetLocale(LocaleId trgLoc);

	Parameters getParameters();

	void setParameters(Parameters params);
}
