/*===========================================================================
  Copyright (C) 2009-2011 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.mymemory;

import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.TextInputPart;

public class Parameters extends StringParameters implements IEditorDescriptionProvider {

	public static final String KEY = "key";
	public static final String USEMT = "useMT";
	public static final String EMAIL = "email";
	
	public Parameters () {
		super();
	}
	
	public Parameters (String initialData) {
		super(initialData);
	}
	
	public String getKey () {
		return getString(KEY);
	}

	public void setKey (String key) {
		setString(KEY, key);
	}

	public boolean getUseMT() {
		return getBoolean(USEMT);
	}

	public void setUseMT (boolean useMT) {
		setBoolean(USEMT, useMT);
	}
	
	public String getEmail () {
		return getString(EMAIL);
	}
	
	public void setEmail (String email) {
		setString(EMAIL, email);
	}

	@Override
	public void reset () {
		super.reset();
		setKey(null);
		setUseMT(true);
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(KEY, "Key", "Access key");
		desc.add(USEMT, "Provide also machine translation result", null);
		desc.add(EMAIL, "Send e-mail address (recommended for large volumes)", null);
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription (ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("MyMemory TM Connector Settings");
		// Key is used for retrieving matches from private TMs
		TextInputPart tipKey = desc.addTextInputPart(paramsDesc.get(Parameters.KEY));
		tipKey.setPassword(true);
		desc.addCheckboxPart(paramsDesc.get(USEMT));
		TextInputPart tipEmail = desc.addTextInputPart(paramsDesc.get(Parameters.EMAIL));
		return desc;
	}

}