/*===========================================================================
  Copyright (C) 2008-2012 by the Okapi Framework contributors
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

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.IAnnotation;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.encoder.IEncoder;
import net.sf.okapi.common.exceptions.OkapiException;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.layerprovider.ILayerProvider;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.EndSubfilter;
import net.sf.okapi.common.resource.Ending;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.StartSubfilter;
import net.sf.okapi.common.resource.TargetPropertiesAnnotation;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SubFilterSkeletonWriter implements ISkeletonWriter {
	public static final String GET_OUTPUT_TOKEN_ID = "#$GET_SFSW_OUTPUT$#";

	private ISkeletonWriter skelWriter; // Skeleton writer of the subfilter's internal filter
	private IEncoder parentEncoder;
	private String startResourceId;
	private Set<String> handledTextUnitIds;
	private StringBuilder sourceOutput;
	private TargetOutputs targetOutputs;
	private LocaleId targetLocaleId;

	public SubFilterSkeletonWriter (StartSubfilter resource) {
		this(
			resource.getFilterWriter().getSkeletonWriter(),
			resource.getParentEncoder(),
			resource.getId(),
			new HashSet<>(),
			new StringBuilder(),
			new TargetOutputs.Default(new LinkedHashMap<>())
		);
	}

	public SubFilterSkeletonWriter(
		final ISkeletonWriter skelWriter,
		final IEncoder parentEncoder,
		final String startResourceId,
		final Set<String> handledTextUnitIds,
		final StringBuilder sourceOutput,
		final TargetOutputs targetOutputs
	) {
		this.skelWriter = skelWriter;
		this.parentEncoder = parentEncoder;
		this.startResourceId = startResourceId;
		this.handledTextUnitIds = handledTextUnitIds;
		this.sourceOutput = sourceOutput;
		this.targetOutputs = targetOutputs;
	}

	@Override
	public void close () {
		skelWriter.close();		
	}

	public String getStartResourceId() {
		return startResourceId;
	}

	@Override
	public String processStartDocument (LocaleId outputLocale,
		String outputEncoding,
		ILayerProvider layer,
		EncoderManager encoderManager,
		StartDocument resource)
	{
		this.targetLocaleId = outputLocale;
		final String s = skelWriter.processStartDocument(outputLocale, outputEncoding, layer, encoderManager, resource);
		this.sourceOutput.append(s);
		this.targetOutputs.add(outputLocale, new StringBuilder(s));
		return "";
	}
	
	/**
	 * Get output created by this skeleton writer from a sequence of events.
	 * This method is useful when only an ISkeletonWriter reference is available.
	 * @param resource can be with the {@link SubFilterSkeletonWriter.GET_OUTPUT_TOKEN_ID} ID
	 * (to return the overall output of this skeleton writer), or any other Ending resource.
	 * @return output of this skeleton writer if the resource is with the
	 * {@link SubFilterSkeletonWriter.GET_OUTPUT_TOKEN_ID} ID or an empty string otherwise.
	 */
	@Override
	public String processEndDocument (Ending resource) {
		if (SubFilterSkeletonWriter.GET_OUTPUT_TOKEN_ID.equals(resource.getId())) {
			final Iterator<IAnnotation> iterator = resource.getAnnotations().iterator();
			if (!iterator.hasNext()) {
				// the source output requested
				return this.parentEncoder == null
					? this.sourceOutput.toString()
					: this.parentEncoder.encode(this.sourceOutput.toString(), EncoderContext.TEXT);
			} else {
				// the target output requested
				final IAnnotation annotation = iterator.next();
				if (!(annotation instanceof TargetPropertiesAnnotation)) {
					throw new OkapiException("Unexpected annotation found: ".concat(annotation.toString()));
				}
				if (1 != ((TargetPropertiesAnnotation) annotation).getLocales().size()) {
					throw new OkapiException("Unsupported number of target locales provided: "
						+ ((TargetPropertiesAnnotation) annotation).getLocales().size());
				}
				final LocaleId localeId = new ArrayList<>(((TargetPropertiesAnnotation) annotation).getLocales())
					.get(0);
				return this.parentEncoder == null
					? this.targetOutputs.outputFor(localeId).toString()
					: this.parentEncoder.encode(
						this.targetOutputs.outputFor(localeId).toString(),
						EncoderContext.TEXT
				  	);
			}
		}
		else {
			final String s = skelWriter.processEndDocument(resource);
			this.sourceOutput.append(s);
			this.targetOutputs.appendToAll(s);
			return "";
		}
	}

	@Override
	public String processStartSubDocument (StartSubDocument resource) {
		final String s = skelWriter.processStartSubDocument(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	@Override
	public String processEndSubDocument (Ending resource) {
		final String s = skelWriter.processEndSubDocument(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	@Override
	public String processStartGroup (StartGroup resource) {
		final String s = skelWriter.processStartGroup(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	@Override
	public String processEndGroup (Ending resource) {
		final String s = skelWriter.processEndGroup(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	@Override
	public String processTextUnit (final ITextUnit resource) {
		if (resource.getTargetLocales().isEmpty() && !this.handledTextUnitIds.contains(resource.getId())) {
			resource.setTarget(this.targetLocaleId, resource.getSource());
		}
		// caching target text containers
		final Map<LocaleId, TextContainer> targetTextContainers = resource.getTargetLocales().stream()
			.collect(Collectors.toMap(l -> l, l -> resource.getTarget(l)));
		// and removing them from the resource
		targetTextContainers.keySet().forEach(l -> resource.removeTarget(l));
		// obtaining as many target outputs as we have
		targetTextContainers.forEach((l, tc) -> {
			resource.setTarget(l, tc);
			this.targetOutputs.appendTo(l, skelWriter.processTextUnit(resource));
			resource.removeTarget(l);
		});
		if (!this.handledTextUnitIds.contains(resource.getId())) {
			// obtaining the source output
			this.sourceOutput.append(skelWriter.processTextUnit(resource));
		}
		// restoring the cached text containers
		targetTextContainers.forEach((l, tc) -> resource.setTarget(l, tc));
		// tracking the current text unit as already handled
		this.handledTextUnitIds.add(resource.getId());
		return "";
	}

	@Override
	public String processDocumentPart (DocumentPart resource) {
		final String s = skelWriter.processDocumentPart(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	@Override
	public String processStartSubfilter (StartSubfilter resource) {
		final String s = skelWriter.processStartSubfilter(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	@Override
	public String processEndSubfilter (EndSubfilter resource) {
		final String s = skelWriter.processEndSubfilter(resource);
		this.sourceOutput.append(s);
		this.targetOutputs.appendToAll(s);
		return "";
	}

	public String getEncodedOutput(final LocaleId locToUse) {
		final Ending ending = new Ending(SubFilterSkeletonWriter.GET_OUTPUT_TOKEN_ID);
		if (null != locToUse) {
			final TargetPropertiesAnnotation a = new TargetPropertiesAnnotation();
			a.set(locToUse, Collections.emptyMap());
			ending.setAnnotation(a);
		}
		return processEndDocument(ending);
	}

	public SubFilterSkeletonWriter setOptions (LocaleId outputLocale,
		String outputEncoding, 
		StartSubfilter startSubfilter,
		ILayerProvider layer)
	{
		StartDocument sfStartDoc = startSubfilter.getStartDoc();
		IFilterWriter sfFilterWriter = sfStartDoc.getFilterWriter();
		EncoderManager sfEncoderManager = sfFilterWriter.getEncoderManager();
		
		processStartDocument(outputLocale, outputEncoding, layer, 
			sfEncoderManager,
			startSubfilter.getStartDoc());
		return this;
	}
	
	public ISkeletonWriter getSkelWriter() {
		return skelWriter;
	}
	
	// For serialization only

	protected void setSkelWriter(ISkeletonWriter skelWriter) {
		this.skelWriter = skelWriter;
	}

	protected IEncoder getParentEncoder() {
		return parentEncoder;
	}

	protected void setParentEncoder(IEncoder parentEncoder) {
		this.parentEncoder = parentEncoder;
	}

	/**
	 * Target outputs.
	 */
	public interface TargetOutputs {
		void add(final LocaleId localeId, final StringBuilder stringBuilder);
		void appendTo(final LocaleId localeId, final String text);
		void appendToAll(final String text);
		StringBuilder outputFor(final LocaleId localeId);

		final class Default implements TargetOutputs {
			private static final String UNEXPECTED_TARGET_OUTPUT_REQUESTED_FOR =
				"Unexpected target output requested for ";
			private final Map<LocaleId, StringBuilder> outputs;

			public Default(final Map<LocaleId, StringBuilder> outputs) {
				this.outputs = outputs;
			}

			public void add(final LocaleId localeId, final StringBuilder stringBuilder) {
				this.outputs.put(localeId, stringBuilder);
			}

			public void appendTo(final LocaleId localeId, final String text) {
				if (!this.outputs.containsKey(localeId)) {
					throw new OkapiException(UNEXPECTED_TARGET_OUTPUT_REQUESTED_FOR.concat(localeId.toString()));
				}
				this.outputs.get(localeId).append(text);
			}

			public void appendToAll(final String text) {
				this.outputs.values().forEach(sb -> sb.append(text));
			}

			public StringBuilder outputFor(final LocaleId localeId) {
				if (!this.outputs.containsKey(localeId)) {
					throw new OkapiException(UNEXPECTED_TARGET_OUTPUT_REQUESTED_FOR.concat(localeId.toString()));
				}
				return this.outputs.get(localeId);
			}
		}
	}
}
