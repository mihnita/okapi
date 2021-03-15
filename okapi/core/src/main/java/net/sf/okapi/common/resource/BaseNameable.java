/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

import java.util.HashMap;
import java.util.Map;

import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.annotation.Annotations;

/**
 * Implements a nameable resource.
 */
public class BaseNameable implements INameable {
	protected String id;
	protected String name;
	protected String type;
	protected String mimeType;
	protected boolean isTranslatable = true; // Default for all resources
	protected boolean preserveWS = false; // Default for all resources
	private final Annotations annotations;
	private final Map<String, Property> properties;
	private ISkeleton skeleton;

	public BaseNameable() {
		properties = new HashMap<>();
		annotations = new Annotations();
	}

	@Override
	public String getId () {
		return id;
	}

	@Override
	public void setId(final String id) {
		this.id = id;
	}

	@Override
	public String getName () {
		return name;
	}

	@Override
	public void setName(final String value) {
		name = value;
	}

	@Override
	public String getType () {
		return type;
	}

	@Override
	public void setType(final String value) {
		type = value;
	}

	@Override
	public String getMimeType () {
		return mimeType;
	}

	@Override
	public void setMimeType(final String value) {
		mimeType = value;
	}

	@Override
	public boolean isTranslatable() {
		return isTranslatable;
	}

	@Override
	public void setIsTranslatable(final boolean value) {
		isTranslatable = value;
	}

	@Override
	public boolean preserveWhitespaces() {
		return preserveWS;
	}

	@Override
	public void setPreserveWhitespaces(final boolean value) {
		preserveWS = value;
	}

	@Override
	public ISkeleton getSkeleton() {
		return skeleton;
	}

	@Override
	public void setSkeleton(final ISkeleton skeleton) {
		this.skeleton = skeleton;
	}

	@Override
	public String toString() {
		return skeleton == null ? super.toString() : skeleton.toString();
	}

	@Override
	public Map<String, Property> getProperties() {
		return properties;
	}

	@Override
	public Annotations getAnnotations() {
		return annotations;
	}
}
