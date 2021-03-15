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

package net.sf.okapi.common;

import net.sf.okapi.common.resource.Code;
import net.sf.okapi.core.simplifierrules.ParseException;
import net.sf.okapi.core.simplifierrules.SimplifierRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.List;

/**
 *
 */

public abstract class AbstractGroupParameters extends StringParameters implements ISimplifierRulesParameters {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String data;

	public AbstractGroupParameters() {
		super();
	}

	/**
	 * Load from buffer. The protected buffer variable is visible in all subclasses
	 * of BaseParameters.
	 * <p>
	 *
	 * @example myParam = buffer.getBoolean("myParam", false);
	 */
	protected abstract void load(ParametersString buffer);

	/**
	 * Save to buffer. The protected buffer variable is visible in all subclasses of
	 * BaseParameters.
	 * <p>
	 *
	 * @example buffer.setBoolean(" myParam ", myParam);
	 */
	protected abstract void save(ParametersString buffer);

	@Override
	 public void fromString(String data) {
		this.data = StringUtil.normalizeLineBreaks(data);
		super.fromString(this.data);
		load(buffer);
		setSimplifierRules(null);
	}

	@Override
	final public String toString() {
		buffer.reset();
		save(buffer);
		return super.toString();
	}

	public <T extends AbstractGroupParameters> void loadGroup(ParametersString buffer, List<T> group,
															  Class<T> elementClass) {
		if (elementClass == null) {
			return;
		}
		loadGroup(buffer, elementClass.getSimpleName(), group, elementClass);
	}

	public <T extends AbstractGroupParameters> void loadGroup(ParametersString buffer, String groupName,
															  List<T> group, Class<T> elementClass) {
		if (buffer == null) {
			return;
		}
		if (group == null) {
			return;
		}
		if (Util.isEmpty(groupName)) {
			return;
		}
		if (elementClass == null) {
			return;
		}
		group.clear();
		int count = buffer.getInteger(String.format("%sCount", groupName));

		for (int i = 0; i < count; i++) {
			T item = null;
			try {
				item = elementClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				logger.debug("Group element instantiation failed: {}", e.getMessage());
				return;
			}

			if (item == null) {
				return;
			}
			item.load(new ParametersString(buffer.getGroup(String.format("%s%d", groupName, i))));
			group.add(item);
		}
	}

	public <T extends AbstractGroupParameters> void saveGroup(ParametersString buffer, String groupName,
															  List<T> group) {
		if (buffer == null) {
			return;
		}
		if (group == null) {
			return;
		}
		if (Util.isEmpty(groupName)) {
			return;
		}

		buffer.setInteger(String.format("%sCount", groupName), group.size());
		for (int i = 0; i < group.size(); i++) {
			AbstractGroupParameters item = group.get(i);
			ParametersString tmp = new ParametersString();
			item.save(tmp);
			buffer.setGroup(String.format("%s%d", groupName, i), tmp.toString());
		}
	}

	public <T extends AbstractGroupParameters> void saveGroup(ParametersString buffer, List<T> group,
															  Class<T> elementClass) {
		if (elementClass == null) {
			return;
		}
		saveGroup(buffer, elementClass.getSimpleName(), group);
	}

	public boolean loadFromResource(Class<?> classRef, String resourceLocation) {
		URL url = classRef.getResource(resourceLocation);
		if (url == null) {
			return false;
		}
		load(url, false);
		return true;
	}

	public void saveToResource(Class<?> classRef, String resourceLocation) {
		URL url = classRef.getResource(resourceLocation);
		if (url == null) {
			return;
		}
		save(url.getPath());
	}

	public String getData() {
		return data;
	}

	@Override
	public String getSimplifierRules() {
		return getString(SIMPLIFIERRULES);
	}

	@Override
	public void setSimplifierRules(String rules) {
		setString(SIMPLIFIERRULES, rules);
	}

	@Override
	public void validateSimplifierRules() throws ParseException {
		SimplifierRules r = new SimplifierRules(getSimplifierRules(), new Code());
		r.parse();
	}
}
