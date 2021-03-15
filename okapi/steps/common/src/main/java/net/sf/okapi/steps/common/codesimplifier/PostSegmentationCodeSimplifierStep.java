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
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.UsingParameters;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiBadStepInputException;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnitUtil;
import net.sf.okapi.core.simplifierrules.ParseException;
import net.sf.okapi.core.simplifierrules.SimplifierRules;

/**
 * Simplify inline codes by merging adjacent codes (where safe) and trimming start and end codes.
 * <p>
 * <b>WARNING: An error will be thrown if the TextUnits have not been segmented. This step must be run post-segmentation as trimmed codes are moved to inter-segment {@link TextPart}s.</b>
 * {@link TextPart}s will be created if needed.
 * </p>
 */
@UsingParameters(Parameters.class)
public class PostSegmentationCodeSimplifierStep extends BasePipelineStep {

    private Parameters params;

    public PostSegmentationCodeSimplifierStep() {
        super();
        params = new Parameters();

    }

    @Override
    public String getDescription() {
        return "Merges adjacent inline codes in the source and target part of a text unit."
                + " Also where possible, moves leading and trailing codes of the source to inter-segment TextParts."
                + " Expects: filter events. Sends back: filter events.";
    }

    @Override
    public String getName() {
        return "Post-segmentation Inline Codes Simplifier";
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
        boolean segmentation = MimeTypeMapper.isSegmentationSupported(tu.getMimeType());
        // don't trim codes for segmented formats like XLIFF as this changes the segment boundaries from the original
        if (!segmentation) {
            TextUnitUtil.simplifyCodesPostSegmentation(tu, getParameters().getRules(),
                    params.getRemoveLeadingTrailingCodes(), params.getMergeCodes());
        }
        return super.handleTextUnit(event);
    }
}
