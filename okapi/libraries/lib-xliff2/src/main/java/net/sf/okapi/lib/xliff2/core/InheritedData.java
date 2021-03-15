/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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
 * Implements the {@link IWithInheritedData} interface.
 */
public class InheritedData implements IWithInheritedData {

	private boolean translate = true;
	private boolean canResegment = true;
	private Directionality srcDir = Directionality.AUTO;
	private Directionality trgDir = Directionality.AUTO;
	private AnnotatorsRef annotators = null;

	/**
	 * Creates a InheritedData object with default values.
	 */
	public InheritedData () {
		// Defaults
	}
	
	/**
	 * Creates a new InheritedData object from an existing one.
	 * @param original the InheritedData object to get the data from.
	 */
	public InheritedData (InheritedData original) {
		setInheritableData(original);
	}
	
	/**
	 * Sets all the data from a given object.
	 * @param original the object from which to get the data.
	 */
	public void setInheritableData (IWithInheritedData original) {
		translate = original.getTranslate();
		canResegment = original.getCanResegment();
		srcDir = original.getSourceDir();
		trgDir = original.getTargetDir();
		if ( original.getAnnotatorsRef() != null ) {
			annotators = new AnnotatorsRef(original.getAnnotatorsRef());
		}
	}

	@Override
	public boolean getCanResegment () {
		return canResegment;
	}
	
	@Override
	public void setCanResegment (boolean canResegment) {
		this.canResegment = canResegment;
	}
	
	@Override
	public boolean getTranslate () {
		return translate;
	}
	
	@Override
	public void setTranslate (boolean translate) {
		this.translate = translate;
	}
	
	@Override
	public Directionality getSourceDir () {
		return srcDir;
	}

	@Override
	public void setSourceDir (Directionality dir) {
		this.srcDir = dir;
	}

	@Override
	public Directionality getTargetDir () {
		return trgDir;
	}

	@Override
	public void setTargetDir (Directionality dir) {
		this.trgDir = dir;
	}

	@Override
	public AnnotatorsRef getAnnotatorsRef () {
		return annotators;
	}

	@Override
	public void setAnnotatorsRef (AnnotatorsRef annotators) {
		this.annotators = annotators;
	}

}
