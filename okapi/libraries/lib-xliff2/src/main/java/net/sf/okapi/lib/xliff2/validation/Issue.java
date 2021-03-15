/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.validation;

/**
 * Represents an issue detected when processing the rules for the Validation module.
 */
public class Issue {

	private String code;
	private String text;
	private String fileId;
	private String unitId;
	private String ruleInfo;
	
	/**
	 * Creates a new {@link Issue} object with a given segment number, code and text.
	 * @param fileId the id of the file for the given unit.
	 * @param code the string representation of the code for this issue.
	 * @param text the human readable text of this issue.
	 */
	public Issue (String fileId,
		String unitId,
		String code,
		String text,
		String ruleInfo)
	{
		this.fileId = fileId;
		this.unitId = unitId;
		this.code = code;
		this.text = text;
		this.ruleInfo = ruleInfo;
	}

	/**
	 * Gets the code for this issue.
	 * @return the code for this issue.
	 */
	public String getCode () {
		return code;
	}

	/**
	 * Sets the code for this issue.
	 * @param code the code for this issue.
	 */
	public void setCode (String code) {
		this.code = code;
	}
	
	/**
	 * Gets the text for this issue.
	 * @return the text for this issue.
	 */
	public String getText () {
		return text;
	}
	
	/**
	 * Sets the text for this issue.
	 * @param text the text for this issue.
	 */
	public void setText (String text) {
		this.text = text;
	}

	/**
	 * Gets the id of the unit where this issue is.
	 * @return the id of the unit where this issue is.
	 */
	public String getUnitId () {
		return unitId;
	}

	/**
	 * Sets the id of the unit where this issue is.
	 * @param unitId the id of the unit where this issue is.
	 */
	public void setUnitId (String unitId) {
		this.unitId = unitId;
	}

	/**
	 * Gets the file id for this issue.
	 * @return the file id for this issue.
	 */
	public String getFileId () {
		return fileId;
	}

	/**
	 * Sets the file id for this issue.
	 * @param fileId the file id for this issue.
	 */
	public void setFileId (String fileId) {
		this.fileId = fileId;
	}

	/**
	 * Gets the string representation of the rule that triggered the issue.
	 * @return the the string representation of the rule that triggered the issue.
	 */
	public String getRuleInfo () {
		return ruleInfo;
	}

	/**
	 * Sets the string representation of the rule that triggered the issue.
	 * @param ruleInfo the string representation of the rule that triggered the issue.
	 */
	public void setRuleInfo (String ruleInfo) {
		this.ruleInfo = ruleInfo;
	}
	
}
