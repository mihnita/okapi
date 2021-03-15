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

package net.sf.okapi.lib.xliff2.core;

import net.sf.okapi.lib.xliff2.its.AnnotatorsRef;

/**
 * Represents the data inherited throughout a file element: translate, canResegment, srcDir and trgDir.
 */
public interface IWithInheritedData {

	void setInheritableData(IWithInheritedData obj);

	/**
	 * Indicates if this object can be re-segmented by default.
	 * @return true if this object can be re-segmented by default, false otherwise.
	 */
	boolean getCanResegment();
	
	/**
	 * Sets the flag indicating if this object can be re-segmented by default.
	 * @param canResegment true if this object can be re-segmented by default, false otherwise.
	 */
	void setCanResegment(boolean canResegment);
	
	/**
	 * Indicates if this object has translatable content by default.
	 * @return true if this object has translatable content by default, false otherwise.
	 */
	boolean getTranslate();
	
	/**
	 * Sets the flag indicating if this object has translatable content by default.
	 * @param translate true if this object has translatable content by default, false otherwise.
	 */
	void setTranslate(boolean translate);
	
	Directionality getSourceDir();

	void setSourceDir(Directionality dir);

	Directionality getTargetDir();

	void setTargetDir(Directionality dir);

	/**
	 * Gets the annotators-references for this object.
	 * @return the annotators-references for this object (can be null)
	 */
	AnnotatorsRef getAnnotatorsRef();
	
	/**
	 * Sets the annotators-references for this object.
	 * @param annotators the new annotators-references for this object (can be null).
	 */
	void setAnnotatorsRef(AnnotatorsRef annotators);

}
