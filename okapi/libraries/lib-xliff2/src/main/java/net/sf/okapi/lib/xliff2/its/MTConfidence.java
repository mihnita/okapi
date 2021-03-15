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
 * Implements the <a href='http://www.w3.org/TR/its20/#mtconfidence'>MT Confidence</a> data category.
 */
public class MTConfidence extends DataCategory {

	private Double mtConfidence;

	/**
	 * Creates a new {@link MTConfidence} object without initial data.
	 */
	public MTConfidence () {
		// Needed in some cases
	}

	/**
	 * Creates a new {@link MTConfidence} object with a given annotator reference and confidence score.
	 * @param annotatorRef the annotator reference.
	 * @param mtConfidence the confidence score.
	 */
	public MTConfidence (String annotatorRef,
		double mtConfidence)
	{
		setAnnotatorRef(annotatorRef);
		setMtConfidence(mtConfidence);
	}

	@Override
	public String getDataCategoryName () {
		return DataCategories.MTCONFIDENCE;
	}
	
	@Override
	public void validate () {
		if (( mtConfidence != null ) && ( getAnnotatorRef() == null )) {
			throw new XLIFFException("An annotator reference must be defined when mtConfidence is defined.");
		}
	}

	public Double getMtConfidence () {
		return mtConfidence;
	}
	
	public void setMtConfidence (Double mtConfidence) {
		if ( mtConfidence != null ) {
			if (( mtConfidence < 0.0 ) || ( mtConfidence > 1.0 )) {
				throw new InvalidParameterException(String.format("The mtConfidence value '%f' is out of the [0.0 to 1.0] range.", mtConfidence));
			}
		}
		this.mtConfidence = mtConfidence;
	}

	@Override
	public IITSItem createCopy () {
		return new MTConfidence(getAnnotatorRef(), mtConfidence);
	}

}
