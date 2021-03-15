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

package net.sf.okapi.common.filters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import net.sf.okapi.common.AbstractGroupParameters;
import net.sf.okapi.common.BaseParameters;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.ListUtil;
import net.sf.okapi.common.ParametersString;

/**
 * Compound Filter parameters.
 *
 */

public class CompoundFilterParameters extends AbstractGroupParameters {
	private final LinkedList<IParameters> parameters = new LinkedList<>();
	private String parametersClass = "";
	private IParameters activeParameters = null;
	private String defParametersClass = "";
	private AbstractCompoundFilter parentFilter;

	public CompoundFilterParameters(AbstractCompoundFilter parentFilter) {
		super();
		this.parentFilter = parentFilter;
	}

	public IParameters getActiveParameters() {
		return activeParameters;
	}

	protected void setActiveParameters(IParameters activeParameters) {
		this.activeParameters = activeParameters;
	}

	protected <T extends BaseParameters> boolean addParameters(Class<T> parametersClass) {
		if (parameters == null) {
			return false;
		}

		boolean res;
		IParameters params;
		BaseParameters bp;

		try {
			if (!BaseParameters.class.isAssignableFrom(parametersClass)) {
				return false;
			}
			Constructor<T> bpc;
			try {
				bpc = parametersClass.getConstructor();
				bp = bpc.newInstance();
			} catch (SecurityException | InvocationTargetException | IllegalArgumentException
					| NoSuchMethodException e) {
				return false;
			}

			res = parameters.add(bp);
			if (!res) {
				return false;
			}

			params = parameters.getLast();
			if (params == null) {
				return false;
			}

		} catch (InstantiationException | IllegalAccessException e2) {
			return false;
		}

		if (activeParameters == null) {
			activeParameters = params; // The first non-empty registered one will become active
			if (params.getClass() == null) {
				return false;
			}
			defParametersClass = params.getClass().getName();
		}

		return res;
	}

	public boolean setActiveParameters(String parametersClass) {
		IParameters params = findParameters(parametersClass);
		if (params == null) {
			return false;
		}

		if (activeParameters != params) {
			// Some finalization of the previous one might be needed
			activeParameters = params;
			this.parametersClass = parametersClass;
		}

		if (parentFilter != null) {
			parentFilter.updateSiblingFilters();
		}

		return true;
	}

	private IParameters findParameters(String parametersClass) {
		if (parameters == null) {
			return null;
		}

		for (IParameters params : parameters) {
			if (params == null) {
				continue;
			}
			if (params.getClass() == null) {
				continue;
			}

			if (params.getClass().getName().equalsIgnoreCase(parametersClass)) {
				return params;
			}
		}

		return null;
	}

	public String getParametersClassName() {
		return parametersClass;
	}

	protected void setParametersClassName(String parametersClass) {
		this.parametersClass = parametersClass;
		setActiveParameters(parametersClass);
	}

	public Class<?> getParametersClass() {
		try {
			return Class.forName(parametersClass);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public void setParametersClass(Class<?> parametersClass) {
		if (parametersClass == null) {
			return;
		}
		setParametersClassName(parametersClass.getName());
	}

	public LinkedList<IParameters> getParameters() {
		return parameters;
	}

	@Override
	public void fromString(String data) {
		super.fromString(data);
		parentFilter.updateSiblingFilters();
	}

	@Override
	public <T extends AbstractGroupParameters> void loadGroup(ParametersString buffer, String groupName,
                                                              List<T> group, Class<T> elementClass) {
		super.loadGroup(buffer, groupName, group, elementClass);
		CompoundFilterParameters item = (CompoundFilterParameters) ListUtil.getFirstNonNullItem(group);
		item.parentFilter.updateSiblingFilters();
	}

	@Override
	protected void load(ParametersString buffer) {
		setParametersClassName(buffer.getString("parametersClass", defParametersClass));
		setActiveParameters(getParametersClassName());

		for (IParameters params : parameters) {
			params.fromString(getData());
		}
		parentFilter.updateSiblingFilters();
	}

	@Override
	public void reset() {
		setParametersClassName(defParametersClass);
	}

	@Override
	protected void save(ParametersString buffer) {
		// !!! Do not change the sequence
		// Store active parameters
		if (activeParameters != null) {
			buffer.fromString(activeParameters.toString());
		}

		if (activeParameters == null) {
			parametersClass = defParametersClass;
		} else {
			parametersClass = activeParameters.getClass().getName();
		}

		buffer.setString("parametersClass", parametersClass);
	}
}
