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

package net.sf.okapi.steps.common.codesimplifier;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadStepInputException;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextUnitUtil;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.core.simplifierrules.ParseException;
import net.sf.okapi.core.simplifierrules.SimplifierRules;

/**
 * !!! It's important to include this step in a pipeline before any source-copying or leveraging steps, because it can modify
 * codes in the source, and target codes will easily get desynchronized with their sources.
 * The best place for this step -- right after the filter.
 */
@UsingParameters(Parameters.class)
public class CodeSimplifierStep extends BasePipelineStep {

    private Parameters params;

    public CodeSimplifierStep() {
        super();
        params = new Parameters();
    }

    @Override
    public String getDescription() {
        return "Merges adjacent inline codes in the source part of a text unit."
                + " Also where possible, moves leading and trailing codes of the source to the skeleton (Only for GenericSkeleton!)."
                + " Expects: filter events. Sends back: filter events.";
    }

    @Override
    public String getName() {
        return "Inline Codes Simplifier";
    }

    @Override
    public Parameters getParameters() {
        return params;
    }

    @Override
    public void setParameters(IParameters params) {
        this.params = (Parameters) params;

        // validate simplify rules
        if (Util.isEmpty(this.params.getRules())) {
            return;
        }
        SimplifierRules r = new SimplifierRules(this.params.getRules(), new Code());
        try {
            r.parse();
        } catch (ParseException e) {
            throw new OkapiBadStepInputException("Simplifier Rules are Invalid", e);
        }
    }

    @Override
    protected Event handleTextUnit(Event event) {
        ITextUnit tu = event.getTextUnit();
        TextUnitUtil.simplifyCodes(tu, getParameters().getRules(),
                params.getRemoveLeadingTrailingCodes() && tu.getSkeleton() instanceof GenericSkeleton, params.getMergeCodes());
        return super.handleTextUnit(event);
    }
}
