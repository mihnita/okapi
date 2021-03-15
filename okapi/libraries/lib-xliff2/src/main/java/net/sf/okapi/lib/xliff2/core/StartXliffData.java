/*===========================================================================
  Copyright (C) 2011-2017 by the Okapi Framework contributors
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

import java.util.Objects;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.Util;

/**
 * Represents the information associated with an &lt;xliff&gt; element. 
 */
public class StartXliffData extends DataWithExtAttributes {
	
	private String version;
	private String sourceLang;
	private String targetLang;

	/**
	 * Creates a {@link StartXliffData} object.
	 * @param version the version of the XLIFF document, use null to get the default.
	 */
	public StartXliffData (String version) {
		if ( version == null ) version = "2.0";
		this.version = version;
	}
	
	/**
	 * Gets the version of this document.
	 * @return the version of this document.
	 */
	public String getVersion () {
		return version;
	}

	/**
	 * Gets the language code of the source for this document.
	 * @return the language code for the source.
	 */
	public String getSourceLanguage () {
		return sourceLang;
	}
	
	/**
	 * Sets the source language of the document.
	 * @param sourceLang the source language to set (must not be null).
	 * @throws InvalidParameterException if the language code is invalid.
	 */
	public void setSourceLanguage (String sourceLang) {
		String msg;
		if ( (msg = Util.validateLang(sourceLang)) != null ) {
			throw new InvalidParameterException(String.format("The source language value '%s' is invalid.\n"+msg, sourceLang));
		}
		this.sourceLang = sourceLang;
	}
	
	/**
	 * Gets the target language of the document.
	 * @return the target language of the document (can be null).
	 */
	public String getTargetLanguage () {
		return targetLang;
	}

	/**
	 * Sets the target language for this document.
	 * @param targetLang the target language to set (can be null).
	 * @throws InvalidParameterException if the language code is invalid.
	 */
	public void setTargetLanguage (String targetLang) {
		// Allow null for the target
		String msg;
		if (( targetLang != null ) && ((msg = Util.validateLang(targetLang)) != null) ) {
			throw new InvalidParameterException(String.format("The target language value '%s' is invalid.\n"+msg, targetLang));
		}
		this.targetLang = targetLang;
	}

	/**
	 * Sets a namespace declaration for this document.
	 * @param prefix the prefix to use for the namespace.
	 * @param namespaceURI the namespace URI.
	 */
	public void setNamespace (String prefix,
		String namespaceURI)
	{
		getExtAttributes().setNamespace(prefix, namespaceURI);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), sourceLang, targetLang, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		StartXliffData other = (StartXliffData) obj;
		if (sourceLang == null) {
			if (other.sourceLang != null) {
				return false;
			}
		} else if (!sourceLang.equals(other.sourceLang)) {
			return false;
		}
		if (targetLang == null) {
			if (other.targetLang != null) {
				return false;
			}
		} else if (!targetLang.equals(other.targetLang)) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}

}
