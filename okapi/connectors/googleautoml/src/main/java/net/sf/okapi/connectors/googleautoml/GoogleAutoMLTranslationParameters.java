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

package net.sf.okapi.connectors.googleautoml;

import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;

public class GoogleAutoMLTranslationParameters extends StringParameters implements IEditorDescriptionProvider {

    private static final String CREDENTIAL_FILE_PATH = "credentialFilePath";
    private static final String CREDENTIAL_STRING = "credentialString";
    private static final String MODEL_MAP = "modelMap";

    public String getCredentialFilePath() {
        return getString(CREDENTIAL_FILE_PATH);
    }

    public void setCredentialFilePath(String credentialFilePath) {
        if (credentialFilePath != null) {
            credentialFilePath = credentialFilePath.trim();
        }
        setString(CREDENTIAL_FILE_PATH, credentialFilePath);
    }

    public String getCredentialString () {
        return getString(CREDENTIAL_STRING);
    }

    public void setCredentialString (String credentialString) {
        if (credentialString != null) {
            credentialString = credentialString.trim();
        }
        setString(CREDENTIAL_STRING, credentialString);
    }

    public String getModelMap() {
        return getString(MODEL_MAP);
    }

    public void setModelMap(String modelMap) {
        if (modelMap != null) {
            modelMap = modelMap.trim();
        }
        setString(MODEL_MAP, modelMap);
    }

    @Override
    public void reset() {
        super.reset();
        setCredentialFilePath("");
        setCredentialString("");
        setModelMap("{}");
    }

    @Override
    public ParametersDescription getParametersDescription() {
        // Note: credential string is not expose through the UI
        ParametersDescription desc = new ParametersDescription(this);
        desc.add(CREDENTIAL_FILE_PATH, "Credential file path",
                "Absolute path to a JSON file containing private key information for a Google service account");
        desc.add(MODEL_MAP, "Model JSON map",
                "String representation of a JSON that maps a language pair to the resource name of the model "
                + "that will process translations for that language pair. Model resource names are typically "
                + "of the form \"projects/{projectId}/locations/{computeRegion}/models/{modelId}\". Example: "
                + "{\"en-US/ja-JP\": \"projects/my-project/locations/us-central1/models/ABC123\", "
                + "\"en-US/de-DE\": \"projects/my-project/locations/us-central1/models/DEF456\"}");
        return desc;
    }

    @Override
    public EditorDescription createEditorDescription(ParametersDescription parametersDescription) {
        EditorDescription desc = new EditorDescription("Google AutoML Translation Connector Settings", true, false);
        desc.addTextInputPart(parametersDescription.get(CREDENTIAL_FILE_PATH));
        desc.addTextInputPart(parametersDescription.get(MODEL_MAP));
        return desc;
    }
}
