/*===========================================================================
  Copyright (C) 2015 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.changeTracking;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.core.BaseList;

/**
 * Represents the <code>&lt;changeTrack&gt;</code> element of the <a href=
 * 'http://docs.oasis-open.org/xliff/xliff-core/v2.0/xliff-core-v2.0.html#changeTracking_module'>Chan
 * g e Tracking module</a>.
 * 
 * @author Marta Borriello
 * 
 */
public class ChangeTrack extends BaseList<Revisions> {

	/** The tag element name constant. */
	public static final String TAG_NAME = "changeTrack";

	/**
	 * Creates a new {@link ChangeTrack} object.
	 */
	public ChangeTrack () {
		// Nothing to do
	}

	/**
	 * Copy constructor.
	 * @param original the original object to duplicate.
	 */
	public ChangeTrack (ChangeTrack original) {
		super(original);
	}

	/**
	 * Gets the complete opening <code>&lt;changeTrack&gt;</code> tag.
	 * 
	 * @param withNamespace
	 *            a boolean stating if the name space has to be included in the
	 *            tag.
	 * @return the complete <code>&lt;changeTrack&gt;</code> tag.
	 */
	public static String getCompleteOpeningTag(boolean withNamespace) {

		StringBuilder completeTag = new StringBuilder();
		completeTag.append("<");
		completeTag.append(Const.PREFIXCOL_TRACKINGSd);
		completeTag.append(TAG_NAME);
		if (withNamespace) {
			completeTag.append(" xmlns:");
			completeTag.append(Const.PREFIX_TRACKING);
			completeTag.append("=\"");
			completeTag.append(Const.NS_XLIFF_TRACKING20);
			completeTag.append("\"");
		}
		completeTag.append(">");
		return completeTag.toString();
	}

	/**
	 * Gets the <code>changeTrack</code> closing tag.
	 * 
	 * @return the <code>changeTrack</code> closing tag.
	 */
	public static String getClosingTag() {

		return "</" + Const.PREFIXCOL_TRACKINGSd + TAG_NAME + ">";
	}
}
