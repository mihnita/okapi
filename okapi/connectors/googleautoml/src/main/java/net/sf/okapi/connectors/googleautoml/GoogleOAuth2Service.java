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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

public class GoogleOAuth2Service {
    
    private final static String AUTH_URL = "https://www.googleapis.com/auth/cloud-platform";
    
    private GoogleCredential credential;

    /**
     * Indicates if the credential for the service has been set.
     * @return true if we have credential, false if we need to set it.
     */
    public boolean hasCredential () {
        return (credential != null);
    }

    /**
     * Sets the service credential.
     * @param inputStream the input stream where the credential is.
     * @see #setCredentialFilePath(String)
     * @see #setCredentialString(String)
     */
    public void setCredential (InputStream inputStream) {
        try {
            credential = GoogleCredential.fromStream(inputStream).createScoped(Collections.singletonList(AUTH_URL));
        }
        catch (IOException e) {
            credential = null;
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls {@link #setCredential(InputStream)} with a file.
     * @param credentialFilePath the path of the file with the credential.
     */
    public void setCredentialFilePath (String credentialFilePath) {
        try {
            try (FileInputStream is = new FileInputStream(new File(credentialFilePath))) {
                setCredential(is);
            }
        }
        catch (IOException e) {
            credential = null;
            throw new RuntimeException(e);
        }
    }

    /**
     * Calls {@link #setCredential(InputStream)} with the JSON credential string.
     * @param credentialString the JSON credential string.
     */
    public void setCredentialString (String credentialString) {
        try {
            try (ByteArrayInputStream is = new ByteArrayInputStream(credentialString.getBytes(StandardCharsets.UTF_8))) {
                setCredential(is);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets an access token (refresh it if needed)
     * @return the access token for the service.
     * @throws IOException when an error occurs.
     */
    public String getAccessToken() throws IOException {
        if (credential == null) {
            throw new IllegalStateException("Credential has not been initialized");
        }
        Long expirationTime = credential.getExpirationTimeMilliseconds();
        if (expirationTime == null || expirationTime < 0) {
            boolean successfulRefresh = credential.refreshToken();
            if (!successfulRefresh) {
                throw new RuntimeException("Unable to refresh token");
            }
        }
        return credential.getAccessToken();
    }
}
