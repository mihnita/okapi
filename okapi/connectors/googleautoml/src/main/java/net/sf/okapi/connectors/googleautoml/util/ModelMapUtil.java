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

package net.sf.okapi.connectors.googleautoml.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.sf.okapi.common.exceptions.OkapiException;

public class ModelMapUtil {
    
    private final JSONParser parser;
    
    private Map<String, String> map;

    public ModelMapUtil() {
        parser = new JSONParser();
        map = new HashMap<>();
    }

    /**
     * Sets the map for this object.
     * @param modelMap the JSON map to store.
     * The expected format is for example: {"en-US/ja-JP": "projects/my-project/locations/us-central1/models/ABC123",
     *  "en-US/de-DE": "projects/my-project/locations/us-central1/models/DEF456"}
     */
    public void setMap (String modelMap) {
        try {
            map = new HashMap<>();
            JSONObject obj = (JSONObject)parser.parse(modelMap);
            @SuppressWarnings("unchecked")
            Iterator<String> iter = obj.keySet().iterator();
            while ( iter.hasNext() ) {
                String key = iter.next();
                String model = (String)obj.get(key);
                map.put(key.toLowerCase(), model);
            }
        }
        catch ( ParseException e ) {
            throw new OkapiException("Error setting the Model map: ", e);
        }
    }

    /**
     * Gets the model resource name for a given language pair.
     * @param sourceCode the code of the source language (not case-sensitive)
     * @param targetCode the code of the target language (not case-sensitive)
     * @return the model resource name for the given language pair.
     * @throws OkapiException if the language pair is not found.
     */
    public String getModelResourceName (String sourceCode, String targetCode) {
        String languagePair = (sourceCode + "/" + targetCode).toLowerCase();
        String model = map.get(languagePair);
        if ( model == null ) {
            throw new OkapiException("Model map does not contain language pair " + languagePair);
        }
        return model;
    }
}
