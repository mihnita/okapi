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
import java.net.URL;
import java.util.LinkedList;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public abstract class AbstractCompoundFilter extends AbstractFilter {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final LinkedList<IFilter> siblingFilters = new LinkedList<>();
	private IFilter activeSiblingFilter = null;
	private RawDocument input;

	public IFilter getActiveSiblingFilter() {
		return activeSiblingFilter;
	}

	protected void setActiveSiblingFilter(IFilter activeSiblingFilter) {
		this.activeSiblingFilter = activeSiblingFilter;
		IParameters params = getParameters();
		if (params instanceof CompoundFilterParameters && activeSiblingFilter instanceof AbstractFilter)
			((CompoundFilterParameters) params).setActiveParameters(
					((AbstractFilter) activeSiblingFilter).getParametersClassName());
	}


	protected <A extends IFilter> boolean addSiblingFilter(Class<A> subFilterClass) {
		if (siblingFilters == null) {
			return false;
		}
		boolean res = false;

		IFilter curSiblingFilter = null;
		try {
			Constructor<A> cc = subFilterClass.getConstructor(new Class[] {});
			if (cc == null) {
				return false;
			}
			curSiblingFilter = cc.newInstance(new Object[] {});
		} catch (InstantiationException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException
				| SecurityException | IllegalAccessException e) {
			logger.debug("siblingfilter instantiation failed: {}", e.getMessage());
			return false;

		}

		res = siblingFilters.add(curSiblingFilter);
		if (!res) {
			return false;
		}

		curSiblingFilter = siblingFilters.getLast();
		if (curSiblingFilter == null) {
			return false;
		}

		addConfigurations(curSiblingFilter.getConfigurations());

		if (activeSiblingFilter == null) {
			activeSiblingFilter = curSiblingFilter; // The first non-empty registered one will become active
		}

		return res;
	}

	public IParameters getActiveParameters() {
		return (activeSiblingFilter != null) ? activeSiblingFilter.getParameters() : null;
	}

	/**
	 * Get a configId string identifying the filter's default configuration (first
	 * on the list of configurations)
	 *
	 * @return configId of default configuration
	 */
	private String getDefaultConfigId() {

		if (Util.isEmpty(configList)) {
			return "";
		}

		FilterConfiguration config = configList.get(0);
		if (config == null) {
			return "";
		}

		return config.configId;
	}

	public boolean setConfiguration(String configId) {
		boolean res = true;

		if (Util.isEmpty(configId)) {
			configId = getDefaultConfigId();
		}

		IFilter subFilter = findConfigProvider(configId);
		res &= (subFilter != null);

		if (res && activeSiblingFilter != subFilter) {
			setActiveSiblingFilter(subFilter);
		}

		// Load config from its config file
		FilterConfiguration config = findConfiguration(configId);
		if (config == null) {
			return res;
		}

		IParameters params = getParameters();

		if (config.parametersLocation != null && params instanceof CompoundFilterParameters) {

			URL url = getClass().getResource(config.parametersLocation);
			params.load(url, false);
		}

		IParameters params2 = getActiveParameters();
		if (params2 != null) {
			params2.fromString(params.toString());
		}
		return res;
	}

	/**
	 * Finds the sub-filter handling the given configuration.
	 *
	 * @param configId configuration identifier
	 * @return a sub-filter reference or null if the configuration is not supported
	 *         by any sub-filter
	 */
	private IFilter findConfigProvider(String configId) {

		if (Util.isEmpty(configList)) {
			return null;
		}

		for (FilterConfiguration config : configList) {

			if (config == null) {
				continue;
			}
			if (config.configId.equalsIgnoreCase(configId)) {
				return findSiblingFilter(config.filterClass);
			}
		}

		return null;
	}

	/**
	 * Finds an instance of the given class in the internal list of sub-filters.
	 *
	 * @param filterClass name of the class sought
	 * @return a sub-filter reference or null if no sub-filter was found
	 */
	private IFilter findSiblingFilter(String filterClass) {

		if (Util.isEmpty(filterClass)) {
			return null;
		}
		if (siblingFilters == null) {
			return null;
		}

		for (IFilter subFilter : siblingFilters) {

			if (subFilter == null) {
				continue;
			}
			if (subFilter.getClass() == null) {
				continue;
			}

			if (subFilter.getClass().getName().equalsIgnoreCase(filterClass)) {
				return subFilter;
			}
		}

		return null;
	}

	private IFilter findSiblingFilterByParameters(String parametersClassName) {
		if (Util.isEmpty(parametersClassName)) {
			return null;
		}
		if (siblingFilters == null) {
			return null;
		}

		for (IFilter subFilter : siblingFilters) {
			if (!(subFilter instanceof AbstractFilter)) {
				continue;
			}

			if (((AbstractFilter) subFilter).getParametersClassName().equalsIgnoreCase(parametersClassName)) {
				return subFilter;
			}
		}

		return null;
	}

	@Override
	public void cancel() {
		if (activeSiblingFilter != null) {
			activeSiblingFilter.cancel();
		}
	}

	@Override
	public void setParameters(IParameters params) {
		super.setParameters(params);
		if (params == null && activeSiblingFilter != null) {
			activeSiblingFilter.setParameters(null);
		}
	}

	@Override
	public void close() {
		if (input != null) {
			input.close();
		}
		if (activeSiblingFilter != null) {
			activeSiblingFilter.close();
		}
	}

	@Override
	public IFilterWriter createFilterWriter() {
		return (activeSiblingFilter != null) ? activeSiblingFilter.createFilterWriter() : null;
	}

	@Override
	public ISkeletonWriter createSkeletonWriter() {

		return (activeSiblingFilter != null) ? activeSiblingFilter.createSkeletonWriter() : null;
	}

	@Override
	public boolean hasNext() {

		return (activeSiblingFilter != null) ? activeSiblingFilter.hasNext() : false;
	}

	@Override
	public Event next() {

		Event event = (activeSiblingFilter != null) ? activeSiblingFilter.next() : null;

		if (event != null && event.getEventType() == EventType.START_DOCUMENT) {

			// Fix START_DOCUMENT to return compound filter parameters, and not the
			// activeSiblingFilter's
			StartDocument startDoc = (StartDocument) event.getResource();
			startDoc.setFilterId(getName());
			startDoc.setFilterParameters(getParameters());
		}
		return event;
	}

	@Override
	public void open(RawDocument input) {
		this.input = input;
		if (activeSiblingFilter != null) {
			activeSiblingFilter.open(input);
		}
	}

	@Override
	public void open(RawDocument input, boolean generateSkeleton) {
		if (activeSiblingFilter != null) {
			activeSiblingFilter.open(input, generateSkeleton);
		}
	}

	protected void updateSiblingFilters() {
		IParameters params = getParameters();
		String className = "";

		if (params instanceof CompoundFilterParameters)
			className = ((CompoundFilterParameters) params).getParametersClassName();
		else return;

		if (Util.isEmpty(className)) return;
		// !!! not seveActiveSiblingFilter() to prevent a deadlock
		activeSiblingFilter = findSiblingFilterByParameters(className);
		IParameters params2 = getActiveParameters();
		if (params2 != null && params != null) params2.fromString(params.toString());
		// to update internal rules of regex filter for example
		if (activeSiblingFilter != null) activeSiblingFilter.setParameters(params2);
	}
}
