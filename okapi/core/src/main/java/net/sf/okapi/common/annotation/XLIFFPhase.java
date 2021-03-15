/*===========================================================================
  Copyright (C) 2013 by the Okapi Framework contributors
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

package net.sf.okapi.common.annotation;

import net.sf.okapi.common.Util;

/**
 * Annotation representing the &lt;phase&gt; element within an XLIFF &lt;file&gt;&lt;header&gt;.
 * The set of phase elements should be contained within the XLIFFPhaseAnnotation
 * on a StartSubDocument Event.
 */
public class XLIFFPhase {
	// Required
	private String phaseName, processName;
	// Optional
	private String companyName, toolId, jobId, contactName, contactEmail, contactPhone;

	// TODO: Handle date and deprecated tool attributes
//	private Date date;
//	private XLIFFTool tool;
	// TODO: Handle <note> elements as well.
	private StringBuilder skel = new StringBuilder();

	public XLIFFPhase(String phaseName, String processName) {
		this.phaseName = phaseName;
		this.processName = processName;
	}

	public String getPhaseName() {
		return this.phaseName;
	}

	public String getProcessName() {
		return this.processName;
	}

	public String getCompanyName() {
		return this.companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getToolId() {
		return toolId;
	}

	public void setToolId(String toolId) {
		this.toolId = toolId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getContactName() {
		return contactName;
	}

	public void setContactName(String contactName) {
		this.contactName = contactName;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public void addSkeletonContent(String text) {
		skel.append(text);
	}

	public String toXML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<phase");
		sb.append(" phase-name=\"")
		  .append(Util.escapeToXML(phaseName, 1, false, null)).append("\"");
		sb.append(" process-name=\"")
		  .append(Util.escapeToXML(processName, 1, false, null)).append("\"");
		if (companyName != null) {
			sb.append(" company-name=\"")
			  .append(Util.escapeToXML(companyName, 1, false, null)).append("\"");
		}
		if (toolId != null) {
			sb.append(" tool-id=\"")
			  .append(Util.escapeToXML(toolId, 1, false, null)).append("\"");
		}
		if (jobId != null) {
			sb.append(" job-id=\"")
			  .append(Util.escapeToXML(jobId, 1, false, null)).append("\"");
		}
		if (contactName != null) {
			sb.append(" contact-name=\"")
			  .append(Util.escapeToXML(contactName, 1, false, null)).append("\"");
		}
		if (contactEmail != null) {
			sb.append(" contact-email=\"")
			  .append(Util.escapeToXML(contactEmail, 1, false, null)).append("\"");
		}
		if (contactPhone != null) {
			sb.append(" contact-phone=\"")
			  .append(Util.escapeToXML(contactPhone, 1, false, null)).append("\"");
		}
		sb.append(">");
		sb.append(skel);
		sb.append("</phase>");
		return sb.toString();
	}
}
