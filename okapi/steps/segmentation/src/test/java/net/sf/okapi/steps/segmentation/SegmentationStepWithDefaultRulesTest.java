package net.sf.okapi.steps.segmentation;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filterwriter.GenericContent;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.lib.segmentation.SRXDocument;
import net.sf.okapi.steps.segmentation.Parameters.SegmStrategy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SegmentationStepWithDefaultRulesTest {

	private SegmentationStep segStep;
	private GenericContent fmt;

	@SuppressWarnings("resource")
	@Before
	public void startUp() {
		fmt = new GenericContent();

		segStep = new SegmentationStep();
		segStep.setSourceLocale(LocaleId.ENGLISH);
		segStep.setTargetLocales(Arrays.asList(LocaleId.FRENCH, LocaleId.GERMAN));

		Parameters params = segStep.getParameters();
		params.setSourceSrxPath(SRXDocument.DEFAULT_SRX_RULES);
		params.setTargetSrxPath(SRXDocument.DEFAULT_SRX_RULES);
		params.setSegmentTarget(true);
		params.setSegmentationStrategy(SegmStrategy.OVERWRITE_EXISTING);

		segStep.handleStartBatchItem(new Event(EventType.START_BATCH_ITEM));
	}

	@Test
	public void testAllTargetsBecomeSegmented() {
		ITextUnit tu1 = new TextUnit("tu1");
		TextContainer source = tu1.getSource();
		source.append(new Segment("seg1", new TextFragment("Part 1. Part 2.")));
		TextContainer targetFr = tu1.createTarget(LocaleId.FRENCH, true, TextUnit.COPY_ALL);
		TextContainer targetDe = tu1.createTarget(LocaleId.GERMAN, true, TextUnit.COPY_ALL);

		segStep.handleTextUnit(new Event(EventType.TEXT_UNIT, tu1));

		assertEquals("[Part 1.][ Part 2.]", fmt.printSegmentedContent(source, true));
		assertEquals("[Part 1.][ Part 2.]", fmt.printSegmentedContent(targetFr, true));
		assertEquals("[Part 1.][ Part 2.]", fmt.printSegmentedContent(targetDe, true));
	}

}
