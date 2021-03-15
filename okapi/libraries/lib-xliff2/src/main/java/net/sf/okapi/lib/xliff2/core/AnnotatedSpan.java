/*===========================================================================
  Copyright (C) 2014-2017 by the Okapi Framework contributors
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

import java.util.regex.Matcher;

/**
 * Represents the information about a span of content that is associated with
 * a marker ({@link MTag} object or a derived object).
 * <p>The span my exist across several parts (segments or ignorables).
 */
public class AnnotatedSpan {

	final private MTag marker;
	final private Part startPart;
	final private int start;
	
	private Part endPart;
	private String codedText = "";
	private int end;
	private int partCount = 1;
	private boolean fullContent = false;

	/**
	 * Creates a new {@link AnnotatedSpan} object.
	 * @param marker the marker associated to this span.
	 * @param startPart the {@link Part} where this span starts.
	 * @param start the location where the part starts.
	 */
	public AnnotatedSpan (MTag marker,
		Part startPart,
		int start)
	{
		this.marker = marker;
		this.startPart = startPart;
		this.start = start;
		end = start;
	}
	
	/**
	 * Gets the {@link MTag} object associated with this span.
	 * @return the annotation for this span.
	 */
	public MTag getMarker () {
		return marker;
	}

	/**
	 * Gets the {@link Part} object where this span starts.
	 * @return the part/segment where this span starts.
	 */
	public Part getStartPart () {
		return startPart;
	}

	/**
	 * Gets the {@link Part} object where this span ends.
	 * @return the part/segment where this span ends.
	 */
	public Part getEndPart () {
		return endPart;
	}
	
	/**
	 * Sets the part/segment where this span ends.
	 * @param endPart the part/segment where this span ends.
	 */
	public void setEndPart (Part endPart) {
		this.endPart = endPart;
	}

	/**
	 * Gets the type of the marker for this span.
	 * @return the marker type for this span.
	 */
	public String getType () {
		return marker.getType();
	}

	/**
	 * Gets the id of the marker for this span.
	 * @return the marker id for this span.
	 */
	public String getId () {
		return marker.getId();
	}
	
	/**
	 * Gets the start position in the coded text of the start part of this span.
	 * @return the start position of this span.
	 * @see #getEnd()
	 * @see #getStartPart()
	 */
	public int getStart () {
		return start;
	}

	/**
	 * Appends a chunk of coded text to the coded text of this span.
	 * (This used during the creation of the span).
	 * @param chunk the chunk to append.
	 */
	protected void append (String chunk) {
		codedText += chunk;
	}

	/**
	 * Gets the coded text of the content for this span.
	 * <p><b>IMPORTANT:</b> This coded text can go across parts (segments or ignorables).</p> 
	 * @return the coded text of the span (possibly across parts)
	 */
	public String getCodedText () {
		return codedText;
	}

	/**
	 * Gets the plain text version of the content for this span.
	 * <p><b>IMPORTANT:</b> This text can go across parts (segments or ignorables).</p> 
	 * @return the plain text version of the content for this span.
	 */
	public String getPlainText () {
		Matcher m = Fragment.TAGREF_REGEX.matcher(codedText);
		return m.replaceAll("");
	}

	/**
	 * Sets the end position in the coded text of the end part of this span. 
	 * @param end the end position of this span.
	 */
	public void setEnd (int end) {
		this.end = end;
	}
	
	/**
	 * Gets the end position in the coded text of the end part of this span.
	 * @return the end position of this span.
	 * @see #getStart()
	 * @see #getEndPart()
	 */
	public int getEnd () {
		return end;
	}

	/**
	 * Sets the number of parts where this span exists.
	 * @param partCount the new number of parts where this span occurs.
	 */
	public void setPartCount (int partCount) {
		this.partCount = partCount;
	}

	/**
	 * Gets the number of parts where this span exists.
	 * @return the number of parts where this span exists.
	 */
	public int getPartCount () {
		return partCount;
	}
	
	/**
	 * Sets the flag indicating if this span covers the entire content of the
	 * part or parts where it exists.
	 * @param fullContent true if the span content is the full content of the part(s).
	 */
	public void setFullContent (boolean fullContent) {
		this.fullContent = fullContent;
	}

	/**
	 * Indicates if this span covers the entire content of the 
	 * part or parts where it exists.
	 * <p>Note that annotation markers are not counted as part of the "content" in this context.
	 * @return true if this span is the full content of the part(s).
	 */
	public boolean isFullContent () {
		return fullContent;
	}

}
