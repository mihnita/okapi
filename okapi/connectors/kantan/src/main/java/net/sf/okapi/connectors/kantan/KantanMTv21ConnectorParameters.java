/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.kantan;

import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.TextInputPart;

public class KantanMTv21ConnectorParameters extends StringParameters implements IEditorDescriptionProvider {

	private static final String API_TOKEN = "apiToken";
	private static final String ENGINE = "engine";
	private static final String ALIAS = "alias";

	public KantanMTv21ConnectorParameters () {
		super();
	}

	public void setApiToken (String apiToken) {
		setString(API_TOKEN, apiToken);
	}

	public String getApiToken () {
		return getString(API_TOKEN);
	}

	public void setEngine (String engine) {
		setString(ENGINE, engine);
	}

	public String getEngine () {
		return getString(ENGINE);
	}

	public void setAlias (String alias) {
		setString(ALIAS, alias);
	}

	public String getAlias () {
		return getString(ALIAS);
	}

	@Override
	public void reset() {
		super.reset();
		setApiToken("");
		setEngine("");
		setAlias("");
	}

	@Override
	public ParametersDescription getParametersDescription() {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(API_TOKEN, "Token",
			"KantanMT API Authorization Token");
		desc.add(ENGINE, "Engine name (or use Alias name)",
			"Name of the engine to use");
		desc.add(ALIAS, "Alias name (or use Engine name)",
			"Name of the alias for the engine to use");
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription (ParametersDescription parametersDescription) {
		EditorDescription desc = new EditorDescription("KantanMT v2.1 Connector Settings", true, false);
		TextInputPart tipSecret = desc.addTextInputPart(parametersDescription.get(API_TOKEN));
		tipSecret.setPassword(true);
		TextInputPart tip = desc.addTextInputPart(parametersDescription.get(ENGINE));
		tip.setAllowEmpty(true);
		tip = desc.addTextInputPart(parametersDescription.get(ALIAS));
		tip.setAllowEmpty(true);
		return desc;
	}

}
