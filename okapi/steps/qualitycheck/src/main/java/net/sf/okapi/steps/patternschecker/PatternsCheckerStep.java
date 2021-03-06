/*===========================================================================
 Copyright (C) 2016 by the Okapi Framework contributors
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

package net.sf.okapi.steps.patternschecker;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.lib.verification.Issue;
import net.sf.okapi.lib.verification.PatternsChecker;

@UsingParameters(Parameters.class)
public class PatternsCheckerStep extends BasePipelineStep {

	// Unused for now private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private LocaleId sourceLocale;
	private LocaleId targetLocale;
	private PatternsChecker patternsChecker;
	private Parameters params;
	private net.sf.okapi.lib.verification.Parameters fullParams;

	public PatternsCheckerStep() {
		patternsChecker = new PatternsChecker();
		params = new Parameters();
		fullParams = new net.sf.okapi.lib.verification.Parameters();
	}

	@Override
	public String getName() {
		return "Pattern (regex) Quality Check";
	}

	@Override
	public String getDescription() {
		return "Compare source and target for pattern differences. " + "Expects: filter events. Sends back: filter events.";
	}

	@Override
	public Parameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(IParameters params) {
		this.params = (Parameters) params;
	}

	@StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
	public void setTargetLocale(LocaleId targetLocale) {
		this.targetLocale = targetLocale;
	}

	@StepParameterMapping(parameterType = StepParameterType.SOURCE_LOCALE)
	public void setSourceLocale(LocaleId sourceLocale) {
		this.sourceLocale = sourceLocale;
	}
	
	@Override
	protected Event handleStartBatch(Event event) {
		// must convert to full verification parameters as we delegate to the
		// verification lib GeneralChecker class
		fullParams.fromString(params.toString());
		patternsChecker.startProcess(sourceLocale, targetLocale, fullParams, new ArrayList<>());
		return event;
	}
	
	@Override
	protected Event handleStartSubDocument(Event event) {
		patternsChecker.processStartSubDocument(event.getStartSubDocument());
		return event;
	}

	@Override
	protected Event handleStartDocument(Event event) {
		// No pre-existing disabled issues: sigList = null
		patternsChecker.processStartDocument((StartDocument) event.getResource(), null);
		return event;
	}

	@Override
	protected Event handleTextUnit(Event event) {
		patternsChecker.processTextUnit(event.getTextUnit());
		return event;
	}

	/**
	 * Currently only used for Unit tests. Issue list is discarded after each
	 * startDocument event.
	 * 
	 * @return
	 */
	protected List<Issue> getIssues() {
		return patternsChecker.getIssues();
	}
}
