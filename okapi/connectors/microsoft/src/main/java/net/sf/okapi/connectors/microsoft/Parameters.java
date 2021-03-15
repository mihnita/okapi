/*===========================================================================
  Copyright (C) 2010-2019 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.microsoft;

import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.TextInputPart;

public class Parameters extends StringParameters implements IEditorDescriptionProvider {
	// Parameters
	private static final String AZUREKEY = "azureKey";
	private static final String CATEGORY = "category";
	private static final String BASE_URL = "baseURL";
	
	// Default values for API v3.
	/*package scope to share with unit tests*/ static final String DEFAULT_CATEGORY = "general";
	private static final String DEFAULT_BASE_URL = "https://api.cognitive.microsofttranslator.com";

	
	public Parameters () {
		super();
	}

	public String getAzureKey() {
		return getString(AZUREKEY);
	}

	public void setAzureKey(String azureKey) {
		setString(AZUREKEY, azureKey);
	}

	public String getCategory () {
		return getString(CATEGORY);
	}

	public void setCategory (String category) {
		setString(CATEGORY, category == null ? DEFAULT_CATEGORY : category);
	}

	public String getBaseURL () {
		return getString(BASE_URL);
	}

	public void setBaseURL (String baseURL) {
		setString(BASE_URL, baseURL);
	}
	
	@Override
	public void reset () {
		super.reset();
		setAzureKey("");
		setCategory(DEFAULT_CATEGORY);
		setBaseURL(DEFAULT_BASE_URL);
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(AZUREKEY,
			"Azure Key (See docs.microsoft.com/en-us/azure/cognitive-services/cognitive-services-apis-create-account)",
			"Microsoft Azure subscription key"); 
		desc.add(CATEGORY,
			"Category (See https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-translate?tabs=curl#request-parameters)", "A name to use a customized system.");
		desc.add(BASE_URL, 
			"Base URL of the translate service, before /translate", 
			"This is the part before /translate. See https://docs.microsoft.com/en-us/azure/cognitive-services/translator/reference/v3-0-reference#base-urls");
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription (ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("Microsoft MT Connector Settings", true, false);
		TextInputPart tip = desc.addTextInputPart(paramsDesc.get(AZUREKEY));
		tip.setPassword(true);
		tip = desc.addTextInputPart(paramsDesc.get(CATEGORY));
		tip.setAllowEmpty(true);
		desc.addTextInputPart(paramsDesc.get(BASE_URL));
		return desc;
	}

}
