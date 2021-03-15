/*===========================================================================
  Copyright (C) 2017-2018 by the Okapi Framework contributors
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

package net.sf.okapi.connectors.deepl;

import net.sf.okapi.common.ParametersDescription;
import net.sf.okapi.common.StringParameters;
import net.sf.okapi.common.uidescription.EditorDescription;
import net.sf.okapi.common.uidescription.IEditorDescriptionProvider;
import net.sf.okapi.common.uidescription.TextInputPart;

public class DeepLv1ConnectorParameters extends StringParameters implements IEditorDescriptionProvider {

	private static final String AUTHKEY = "authKey";
	private static final String PLAINTEXT = "plainText";
	private static final String SPLITSENTENCES = "splitSentences";
	private static final String PRESERVEFORMATTING = "preserveFormatting";

	public DeepLv1ConnectorParameters () {
		super();
	}

	public String getAuthKey() {
		return getString(AUTHKEY);
	}

	public void setAuthKey(String authKey) {
		setString(AUTHKEY, authKey);
	}
	
	public boolean getPlainText() {
		return getBoolean(PLAINTEXT);
	}

	public void setPlainText(boolean plainText) {
		setBoolean(PLAINTEXT, plainText);
	}

	public boolean getSplitSentences() {
		return getBoolean(SPLITSENTENCES);
	}
	
	public void setSplitSentences(boolean splitSentences) {
		setBoolean(SPLITSENTENCES, splitSentences);
	}

	public boolean getPreserveFormatting() {
		return getBoolean(PRESERVEFORMATTING);
	}
	
	public void setPreserveFormatting(boolean preserveFormatting) {
		setBoolean(PRESERVEFORMATTING, preserveFormatting);
	}

	@Override
	public void reset () {
		super.reset();
		setAuthKey("");
		setPlainText(false);
		setSplitSentences(false);
		setPreserveFormatting(true);
	}

	@Override
	public ParametersDescription getParametersDescription () {
		ParametersDescription desc = new ParametersDescription(this);
		desc.add(AUTHKEY,
			"Authentication Key (Contact Deepl to get one)",
			"DeepL authentication key");
		desc.add(PLAINTEXT,
			"Get plain-text translation",
			"Strips out inline codes and gives plain-text translations");
		desc.add(SPLITSENTENCES,
			"Split sentences",
			"Determines if the input is first split into sentences");
		desc.add(PRESERVEFORMATTING,
			"Preserve some formatting aspects",
			"Determines if some aspects of formatting are preserved (punctuation, upper/lower case)");
		return desc;
	}

	@Override
	public EditorDescription createEditorDescription (ParametersDescription paramsDesc) {
		EditorDescription desc = new EditorDescription("DeepL Connector v1 Settings", true, false);
		TextInputPart tip = desc.addTextInputPart(paramsDesc.get(AUTHKEY));
		tip.setPassword(true);
		desc.addCheckboxPart(paramsDesc.get(PLAINTEXT));
		desc.addCheckboxPart(paramsDesc.get(SPLITSENTENCES));
		desc.addCheckboxPart(paramsDesc.get(PRESERVEFORMATTING));
		return desc;
	}

}
