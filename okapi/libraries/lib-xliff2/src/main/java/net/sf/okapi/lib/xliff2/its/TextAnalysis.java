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

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.XLIFFException;

/**
 * Implements the <a href='http://www.w3.org/TR/its20/#textanalysis'>Text Analysis</a> data category.
 */
public class TextAnalysis extends DataCategory {

	private Double taConfidence;
	private String taClassRef;
	private String taSource;
	private String taIdent;
	private String taIdentRef;

	/**
	 * Creates an empty {@link TextAnalysis} object.
	 */
	public TextAnalysis () {
		// nothing to do
	}
	
	@Override
	public String getDataCategoryName () {
		return DataCategories.TEXTANALYSIS;
	}
	
	@Override
	public void validate () {
		if ( taClassRef == null ) {
			if (( taSource == null ) && ( taIdentRef == null )) {
				throw new XLIFFException("If taClassRef is not defined you must have either taSource/taIndent or taIdentRef defined.");
			}
		}
		if ( taSource != null ) {
			if ( taIdent == null ) {
				throw new XLIFFException("If taSource is defined taIndent must also be defined.");
			}
		}
		else {
			if ( taIdent != null ) {
				throw new XLIFFException("If taIdent is defined taSource must also be defined.");
			}
		}
		if ( taIdentRef != null ) {
			if (( taSource != null ) || ( taIdent != null )) {
				throw new XLIFFException("If taIdentRef is defined neither taSource nor taIdent can be defined.");
			}
		}
		if (( taConfidence != null ) && ( getAnnotatorRef() == null )) {
			throw new XLIFFException("An annotator reference must be defined when taConfidence is defined.");
		}
	}

	/**
	 * Gets the <code>taConfidence</code> attribute.
	 * @return the <code>taConfidence</code> attribute (can be null).
	 */
	public Double getTaConfidence () {
		return taConfidence;
	}
	
	/**
	 * Sets a new <code>taConfidence</code> attribute.
	 * @param taConfidence the new <code>taConfidence</code> attribute (between 0.0 and 1.0, or null).
	 */
	public void setTaConfidence (Double taConfidence) {
		if ( taConfidence != null ) {
			if (( taConfidence < 0.0 ) || ( taConfidence > 1.0 )) {
				throw new InvalidParameterException(String.format("The taConfidence value '%f' is out of the [0.0 to 1.0] range.", taConfidence));
			}
		}
		this.taConfidence = taConfidence;
	}

	public String getTaClassRef () {
		return taClassRef;
	}

	public void setTaClassRef (String taClassRef) {
		this.taClassRef = taClassRef;
	}

	/**
	 * Gets the <code>taSource</code> attribute.
	 * @return the <code>taSource</code> attribute (can be null).
	 */
	public String getTaSource () {
		return taSource;
	}

	/**
	 * Sets a new <code>taSource</code> attribute.
	 * @param taSource the new <code>taSource</code> attribute (can be null).
	 */
	public void setTaSource (String taSource) {
		this.taSource = taSource;
	}

	public String getTaIdent () {
		return taIdent;
	}

	public void setTaIdent (String taIdent) {
		this.taIdent = taIdent;
	}

	public String getTaIdentRef () {
		return taIdentRef;
	}

	public void setTaIdentRef (String taIdentRef) {
		this.taIdentRef = taIdentRef;
	}

	@Override
	public IITSItem createCopy () {
		TextAnalysis newItem = new TextAnalysis();
		newItem.setAnnotatorRef(getAnnotatorRef());
		newItem.taClassRef = taClassRef;
		newItem.taConfidence = taConfidence;
		newItem.taIdent = taIdent;
		newItem.taIdentRef = taIdentRef;
		newItem.taSource = taSource;
		return newItem;
	}

}
