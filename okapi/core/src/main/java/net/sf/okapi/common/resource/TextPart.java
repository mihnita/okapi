/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.annotation.Annotations;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements the base object for the parts that make up a content.
 */
public class TextPart implements IResource {
	/**
	 * Identifier of this segment.
	 */
	public String id;
	/**
	 * Original identifier of this segment in another format (xliff2 etc..).
	 */
	public String originalId;
	/**
	 * Text fragment of this part.
	 */
	public TextFragment text;
	public WhitespaceStrategy whitespaceSrategy;
	protected Map<String, Property> properties;
	protected Annotations annotations;

	/**
	 * Creates an empty part.
	 */
	public TextPart() {
		properties = new HashMap<>();
		annotations = new Annotations();
		text = new TextFragment();
		whitespaceSrategy = WhitespaceStrategy.INHERIT;
	}

	/**
	 * Creates a new TextPart with a given {@link TextFragment}.
	 *
	 * @param text the {@link TextFragment} for this new part.
	 */
	public TextPart(final TextFragment text) {
		this();
		this.text = text;
	}

	/**
	 * Creates a new TextPart with a given {@link TextFragment}.
	 *
	 * @param text the {@link TextFragment} for this new part.
	 */
	public TextPart(final String id, final TextFragment text) {
		this();
		this.id = id;
		this.text = (text == null) ? new TextFragment() : text;
	}

	/**
	 * Creates a new TextPart with a given text string.
	 *
	 * @param text the text for this new part.
	 */
	public TextPart(final String text) {
		this();
		this.text = new TextFragment(text);
	}

	/**
	 * Clone of {@link TextPart}
	 */
	@Override
	public TextPart clone() {
		final TextPart tp = new TextPart(id, text.clone());
		tp.originalId = originalId;
		tp.whitespaceSrategy = whitespaceSrategy;
		IWithProperties.copy(this, tp);
		IWithAnnotations.copy(this, tp);
		return tp;
	}

	/**
	 * Gets the identifier for this textpart.
	 * 
	 * @return the identifier for this textpart.
	 */
	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		if (text == null)
			return "";
		return text.toText();
	}

	/**
	 * Gets the text fragment for this part.
	 *
	 * @return the text fragment for this part.
	 */
	public TextFragment getContent() {
		return text;
	}

	/**
	 * Sets the {@link TextFragment} for this part.
	 *
	 * @param fragment the {@link TextFragment} to assign to this part. It must not
	 *                 be null.
	 */
	public void setContent(final TextFragment fragment) {
		text = fragment;
	}

	/**
	 * Indicates if this part is a {@link Segment}.
	 *
	 * @return true if the part is a {@link Segment}, false if it is not.
	 */
	public boolean isSegment() {
		return false;
	}

	@Override
	public Map<String, Property> getProperties() {
		return properties;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * <b>WARNING: The parent {@link ITextUnit} must be used in the case the
	 * whitespaceSrategy is INHERIT.<b/> We return false in the case of INHERIT
	 * because we have not specially set the whitespace handling TextPart and so it
	 * is the responsibility of the user to check the parent for the "real" value.
	 * 
	 * @return true if the whitespace handling has been specifically set on this
	 *         instance. False in all other cases.
	 */
	public boolean preserveWhitespaces() {
		if (whitespaceSrategy == WhitespaceStrategy.PRESERVE) {
			return true;
		}
		return false;
	}

	/**
	 * Only use this method if you are specially overriding the parent
	 * {@link ITextUnit} whitespace handling.
	 * 
	 * @param preserveWS
	 */
	public void setPreserveWhitespaces(boolean preserveWS) {
		if (preserveWS) {
			whitespaceSrategy = WhitespaceStrategy.PRESERVE;
			return;
		}
		whitespaceSrategy = WhitespaceStrategy.NORMALIZE;
	}
}
