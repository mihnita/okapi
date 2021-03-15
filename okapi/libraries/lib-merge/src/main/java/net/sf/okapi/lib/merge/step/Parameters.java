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
===========================================================================*/

package net.sf.okapi.lib.merge.step;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

@EditorFor(Parameters.class)
public class Parameters extends StringParameters implements IEditorDescriptionProvider {
	private static final String PRESERVESPACEBYDEFAULT = "preserveWhiteSpaceByDefault";

	public Parameters() {
		super();
	}

	@Override
	public void reset() {
		super.reset();
		setPreserveWhiteSpaceByDefault(false);
	}

	public boolean isPreserveWhiteSpaceByDefault() {
		return getBoolean(PRESERVESPACEBYDEFAULT);
	}

	public void setPreserveWhiteSpaceByDefault(boolean preserveWhiteSpaceByDefault) {
		setBoolean(PRESERVESPACEBYDEFAULT, preserveWhiteSpaceByDefault);
	}

	@Override
	public ParametersDescription getParametersDescription() {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(PRESERVESPACEBYDEFAULT, "Preserve Whitespace When Reading XLIFF",
				"Merging preserves the original whitespace in the XLIFF file");
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription(ParametersDescription parametersDescription) {
		EditorDescription desc = new EditorDescription("OriginalDocumentXliffMergerStep Parameters", true, false);
		desc.addCheckboxPart(parametersDescription.get(PRESERVESPACEBYDEFAULT));
		return desc;
	}

}
