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

package net.sf.okapi.applications.rainbow.pipeline;

import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import net.sf.okapi.steps.leveraging.LeveragingStep;
import net.sf.okapi.steps.rainbowkit.creation.ExtractionStep;
import net.sf.okapi.steps.segmentation.SegmentationStep;

public class TranslationKitCreationPipeline extends PredefinedPipeline {

	public TranslationKitCreationPipeline () {
		super("TranslationKitCreationPipeline",
			"Translation Kit Creation");
		addStep(new RawDocumentToFilterEventsStep());
		
		SegmentationStep stepSeg = new SegmentationStep();
		stepSeg.getParameters().setSegmentSource(false);
		stepSeg.getParameters().setSegmentTarget(false);
		stepSeg.getParameters().setCopySource(false);
		addStep(stepSeg);

		LeveragingStep stepLev1 = new LeveragingStep();
		stepLev1.getParameters().setLeverage(false);
		addStep(stepLev1);
		
		LeveragingStep stepLev2 = new LeveragingStep();
		stepLev2.getParameters().setLeverage(false);
		stepLev2.getParameters().setFillIfTargetIsEmpty(true);
		addStep(stepLev2);
		
		addStep(new ExtractionStep());
		setInitialStepIndex(4);
	}
	
}
