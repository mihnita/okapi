/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.okapi.common.ExecutionContext;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.Annotations;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;

/**
 * Special resource used to carry runtime parameters.
 */
public class PipelineParameters implements IResource {
	private String id;
	private URI outputURI;
	private LocaleId targetLocale;
	private List<LocaleId> trgLocs;
	private LocaleId sourceLocale;
	private String outputEncoding;
	private URI inputURI;
	private String filterConfigId;
	private IFilterConfigurationMapper fcMapper;
	private RawDocument inputRawDocument;
	private RawDocument secondInputRawDocument;
	private RawDocument thirdInputRawDocument;
	private String rootDirectory;
	private String inputRootDirectory;
	private Object uiParent;
	private ExecutionContext context;
	private int batchInputCount = -1;
	private Map<String, Property> properties;
	private Annotations annotations;

	/**
	 * Creates a new empty ParametersEvent object.
	 */
	public PipelineParameters () {
		properties = new HashMap<>();
		annotations = new Annotations();
	}

	/**
	 * Creates a ParametersEvent object with most majority of defaults initialized
	 * @param startDoc - current {@link StartDocument}
	 * @param inputDoc - input {@link RawDocument}
	 * @param secondDoc - optional second input {@link RawDocument}
	 * @param thirdDoc - optional third input {@link RawDocument}
	 */
	public PipelineParameters(final StartDocument startDoc, final RawDocument inputDoc, final RawDocument secondDoc,
			final RawDocument thirdDoc) {
		this();
		id = startDoc.getId();
		outputURI = null;
		targetLocale = inputDoc.getTargetLocale();
		trgLocs = new LinkedList<>();
		trgLocs.addAll(startDoc.getTargetLocales());
		sourceLocale = inputDoc.getSourceLocale();
		outputEncoding = null;
		inputURI = inputDoc.getInputURI();
		filterConfigId = inputDoc.getFilterConfigId();
		fcMapper = null;
		inputRawDocument = inputDoc;
		secondInputRawDocument = secondDoc;
		thirdInputRawDocument = thirdDoc;
		rootDirectory = null;
		inputRootDirectory = null;
		uiParent = null;
		context = null;
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
	public <A extends IAnnotation> A getAnnotation(final Class<A> annotationType) {
		if ( annotations == null ) {
			return null;
		}
		return annotationType.cast(annotations.get(annotationType));
	}

	public void setOutputURI(final URI outputURI) {
		this.outputURI = outputURI;
	}

	public URI getOutputURI () {
		return outputURI;
	}

	public void setTargetLocale(final LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}

	public LocaleId getTargetLocale () {
		return targetLocale;
	}

	public void setTargetLocales(final List<LocaleId> trgLocs) {
		this.trgLocs = trgLocs;
	}

	public List<LocaleId> getTargetLocales () {
		return trgLocs;
	}

	public void setSourceLocale(final LocaleId sourceLocale) {
		this.sourceLocale = sourceLocale;
	}

	public LocaleId getSourceLocale () {
		return sourceLocale;
	}

	public void setOutputEncoding(final String outputEncoding) {
		this.outputEncoding = outputEncoding;
	}

	public String getOutputEncoding () {
		return outputEncoding;
	}

	public void setInputURI(final URI inputURI) {
		this.inputURI = inputURI;
	}

	public URI getInputURI () {
		return inputURI;
	}

	public void setFilterConfigurationId(final String filterConfigId) {
		this.filterConfigId = filterConfigId;
	}

	public String getFilterConfigurationId () {
		return filterConfigId;
	}

	public void setFilterConfigurationMapper(final IFilterConfigurationMapper fcMapper) {
		this.fcMapper = fcMapper;
	}

	public IFilterConfigurationMapper getFilterConfigurationMapper () {
		return fcMapper;
	}

	public void setInputRawDocument(final RawDocument inputRawDocument) {
		this.inputRawDocument = inputRawDocument;
	}

	public RawDocument getInputRawDocument () {
		return inputRawDocument;
	}

	public void setSecondInputRawDocument(final RawDocument secondInputRawDocument) {
		this.secondInputRawDocument = secondInputRawDocument;
	}

	public RawDocument getSecondInputRawDocument () {
		return secondInputRawDocument;
	}

	public void setThirdInputRawDocument(final RawDocument thirdInputRawDocument) {
		this.thirdInputRawDocument = thirdInputRawDocument;
	}

	public RawDocument getThirdInputRawDocument () {
		return thirdInputRawDocument;
	}

	public void setRootDirectory(final String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public String getRootDirectory () {
		return rootDirectory;
	}

	public void setInputRootDirectory(final String inputRootDirectory) {
		this.inputRootDirectory = inputRootDirectory;
	}

	public String getInputRootDirectory () {
		return inputRootDirectory;
	}

	public void setUIParent(final Object uiParent) {
		this.uiParent = uiParent;
	}

	public Object getUIParent () {
		return uiParent;
	}

	public void setExecutionContext(final ExecutionContext context) {
		this.context = context;
	}

	public ExecutionContext getExecutionContext () {
		return context;
	}

	public void setBatchInputCount(final int batchInputCount) {
		this.batchInputCount = batchInputCount;
	}

	public int getBatchInputCount () {
		return batchInputCount;
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
