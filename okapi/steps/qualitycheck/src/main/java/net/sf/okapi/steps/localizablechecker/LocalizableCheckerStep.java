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

package net.sf.okapi.steps.localizablechecker;

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
import net.sf.okapi.lib.verification.LocalizableChecker;


@UsingParameters(Parameters.class)
public class LocalizableCheckerStep extends BasePipelineStep {

	//Unused for now private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private LocaleId sourceLocale;
	private LocaleId targetLocale;
	private Parameters params;
	private LocalizableChecker localizableChecker;
	private net.sf.okapi.lib.verification.Parameters fullParams;

	public LocalizableCheckerStep() {
		params = new Parameters();
		localizableChecker = new LocalizableChecker();
	}

	@Override
	public String getName() {
		return "Localizable Quality Check";
	}

	@Override
	public String getDescription() {
		return "Compare source and target number, date and time patterns to assure they are localized. "
				+ "Expects: filter events. Sends back: filter events.";
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
		localizableChecker.startProcess(sourceLocale, targetLocale, fullParams, new ArrayList<>());
		return event;
	}
	
	@Override
	protected Event handleStartDocument(Event event) {
		// No pre-existing disabled issues: sigList = null
		localizableChecker.processStartDocument((StartDocument) event.getResource(), null);
		return event;
	}

	@Override
	protected Event handleTextUnit(Event event) {
		localizableChecker.processTextUnit(event.getTextUnit());
		return event;
	}
	
	/**
	 * Currently only used for Unit tests. Issue list is discarded after each
	 * startDocument event.
	 * 
	 * @return
	 */
	protected List<Issue> getIssues() {
		return localizableChecker.getIssues();
	}
}
