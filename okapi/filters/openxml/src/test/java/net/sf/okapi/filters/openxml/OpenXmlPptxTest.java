/*===========================================================================
  Copyright (C) 2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import static net.sf.okapi.filters.openxml.CodePeekTranslator.locENUS;
import static net.sf.okapi.filters.openxml.OpenXMLTestHelpers.textUnitSourceExtractor;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OpenXmlPptxTest{

	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testMaster() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(true);
		params.setIgnorePlaceholdersInPowerpointMasters(true);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/textbox-on-master.pptx").asUri(),"UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactly(
			"My title", "My subtitle", "Textbox on layout 1", "Textbox on master"
		);
	}

	@Test
	public void extractsHiddenSlides() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.setTranslatePowerpointHidden(true);

		final List<Event> events = FilterTestDriver.getEvents(
			new OpenXMLFilter(),
			new RawDocument(
				root.in("/736.pptx").asUri(),
				"UTF-8",
				locENUS
			),
			conditionalParameters
		);

		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
			"This is a visible slide title",
			"This is a visible slide body",
			"This is a hidden slide",
			"This is hidden slide content",
			"Click to edit the title text format",
			"Click to edit the outline text format",
			"Second Outline Level",
			"Third Outline Level",
			"Fourth Outline Level",
			"Fifth Outline Level",
			"Sixth Outline Level",
			"Seventh Outline Level",
			"<date/time>",
			"<footer>"
		);
	}

	@Test
	public void doesNotExtractHiddenSlides() {
		final List<Event> events = FilterTestDriver.getEvents(
			new OpenXMLFilter(),
			new RawDocument(
				root.in("/736.pptx").asUri(),
				"UTF-8",
				locENUS
			),
			new ConditionalParameters()
		);

		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
			"This is a visible slide title",
			"This is a visible slide body",
			"Click to edit the title text format",
			"Click to edit the outline text format",
			"Second Outline Level",
			"Third Outline Level",
			"Fourth Outline Level",
			"Fifth Outline Level",
			"Sixth Outline Level",
			"Seventh Outline Level",
			"<date/time>",
			"<footer>"
		);
	}

	@Test
	public void doesNotExtractEmptyFormatting() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.setTranslatePowerpointMasters(false);


		List<Event> events = FilterTestDriver.getEvents(
			new OpenXMLFilter(),
			new RawDocument(
				root.in("/formatting/803-1.pptx").asUri(),
				"UTF-8",
				locENUS
			),
			conditionalParameters
		);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
			"Default slide title",
			"Default sub title",
			"Default title 2",
			"Default sub title 2",
			"Default title 3 with  <run1>bold  and <run2>italic</run2></run1>",
			"Default sub title 3 with <run1>bold and <run2>italic</run2></run1>",
			"Bold title and <run1>italic</run1>",
			"Bold sub title and <run1>italic</run1>",
			"Слайд 1",
			"User"
		);

		events = FilterTestDriver.getEvents(
				new OpenXMLFilter(),
				new RawDocument(
						root.in("/formatting/803-2.pptx").asUri(),
						"UTF-8",
						locENUS
				),
				conditionalParameters
		);

		textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
			"Default slide title",
			"Default sub title",
			"Default title with <run1>bold and <run2>italic</run2></run1>",
			"Default text with <run1>bold and <run2>italic</run2></run1>",
			"Bold title and <run1>italic</run1>",
			"Bold text and <run1>italic</run1>",
			"Default slide title",
			"User"
		);

		events = FilterTestDriver.getEvents(
				new OpenXMLFilter(),
				new RawDocument(
						root.in("/formatting/803-oo.pptx").asUri(),
						"UTF-8",
						locENUS
				),
				conditionalParameters
		);

		textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
			"Default title",
			"Default text",
			"Default title with <run1>bold and <run2>italic</run2></run1>",
			"Default text with <run1>bold and <run2>italic</run2></run1>",
			"Bold title and <run1>italic</run1>",
			"Bold text and <run1>italic</run1>"
		);

        events = FilterTestDriver.getEvents(
            new OpenXMLFilter(),
            new RawDocument(
                root.in("/formatting/803-defrprs-and-no-rprs.pptx").asUri(),
                "UTF-8",
                locENUS
            ),
            conditionalParameters
        );

        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
            "Hello world",
            "This is the body slide"
        );

        events = FilterTestDriver.getEvents(
                new OpenXMLFilter(),
                new RawDocument(
                        root.in("/formatting/803-defrprs-and-rprs.pptx").asUri(),
                        "UTF-8",
                        locENUS
                ),
                conditionalParameters
        );

        textUnits = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
                "Hello world",
                "This is the body slide"
        );
	}

	@Test
	public void extractsWithoutAggressivelyCleanedUpFormatting() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.setTranslatePowerpointMasters(false);

		final List<Event> events = FilterTestDriver.getEvents(
			new OpenXMLFilter(),
			new RawDocument(
				root.in("/formatting/823.pptx").asUri(),
				"UTF-8",
				locENUS
			),
			conditionalParameters
		);

		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
			"first<run1>/</run1><run2>last</run2>"
		);
	}

	@Test
	public void extractsWithAggressivelyCleanedUpFormatting() {
		final ConditionalParameters conditionalParameters = new ConditionalParameters();
		conditionalParameters.setTranslatePowerpointMasters(false);
		conditionalParameters.setCleanupAggressively(true);

		final List<Event> events = FilterTestDriver.getEvents(
			new OpenXMLFilter(),
			new RawDocument(
				root.in("/formatting/823.pptx").asUri(),
				"UTF-8",
				locENUS
			),
			conditionalParameters
		);

		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		Assertions.assertThat(textUnits).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).containsExactly(
				"first/last"
		);
	}

	@Test
	public void testIncludeSlidesYes() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);
		params.setPowerpointIncludedSlideNumbersOnly(true);
		params.tsPowerpointIncludedSlideNumbers = new TreeSet<>();
		params.tsPowerpointIncludedSlideNumbers.add(1);
		params.tsPowerpointIncludedSlideNumbers.add(3);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/include-slides.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"Slide 1", "Slide 3", "Note 3", "Note 1", "comment 1", "comment 3"
		);
	}

	@Test
	public void testIncludeSlidesCharts() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);
		params.setPowerpointIncludedSlideNumbersOnly(true);
		params.tsPowerpointIncludedSlideNumbers = new TreeSet<>();
		params.tsPowerpointIncludedSlideNumbers.add(1);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/include-slides-w-chart.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits)
				.hasSize(1)
				.extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
				"Title 1"
		);

		params.tsPowerpointIncludedSlideNumbers.add(2);

		events = getEvents(filter, doc);

		textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits)
				.hasSize(3)
				.extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
				"Title 1",
				"Title 2",
				"Chart title"
		);
	}

	@Test
	public void testIncludeSlidesSmartArt() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);
		params.setPowerpointIncludedSlideNumbersOnly(true);
		params.tsPowerpointIncludedSlideNumbers = new TreeSet<>();
		params.tsPowerpointIncludedSlideNumbers.add(1);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/include-slides-w-smartart.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits)
				.hasSize(1)
				.extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
				"Title 1"
		);

		params.tsPowerpointIncludedSlideNumbers.add(2);

		events = getEvents(filter, doc);

		textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits)
				.hasSize(7)
				.extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
				"Title 1",
				"Title 2",
				"Smart",
				"Art",
				"Foo",
				"Bar",
				"Baz"
		);
	}

	@Test
	public void testIncludeSlidesNo() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);
		params.setPowerpointIncludedSlideNumbersOnly(false);
		params.tsPowerpointIncludedSlideNumbers = new TreeSet<>();
		params.tsPowerpointIncludedSlideNumbers.add(1);
		params.tsPowerpointIncludedSlideNumbers.add(3);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/include-slides.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"Slide 1",
			"Slide 2",
			"Slide 3",
			"Slide 4",
			"Note 4",
			"Note 3",
			"Note 2",
			"Note 1",
			"comment 4",
			"comment 1",
			"comment 2",
			"comment 3"
		);
	}

	@Test
	public void testFormattingsPptx() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/The tomato is formatted.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"The <run1>tomato</run1> is <run2>formatted</run2>",
			"The <run1>cucumber</run1> is <run2>linked</run2>"
		);

		assertThat(
			textUnits.get(0).getSource().getParts().get(0).getContent().getCodes()
		).hasSize(4).extracting("type").containsExactly(
			"x-italic;",
			"x-italic;",
			"x-bold;",
			"x-bold;"
		);
		assertThat(
			textUnits.get(1).getSource().getParts().get(0).getContent().getCodes()
		).hasSize(4).extracting("type").containsExactly(
			"x-underline:sng;",
			"x-underline:sng;",
			"x-link;",
			"x-link;"
		);
	}

	@Test
	public void testFormattedHyperlinkPptx() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/FormattedHyperlink.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"The <run1>hyperlink</run1>"
		);

		assertThat(
			textUnits.get(0).getSource().getParts().get(0).getContent().getCodes()
		).hasSize(2);
	}

	/**
	 * The test document has some cluttered runs with identical properties. Example:
	 * <pre>{@code
	 * <a:r>
	 *   <a:rPr lang="de-DE" baseline="0" dirty="0" err="1" smtClean="0"/>
	 *   <a:t>first</a:t>
	 * </a:r>}</pre>
	 * <p>
	 * We make sure that all runs are merged into one.
	 */
	@Test
	public void testRunMergingWithBaselineAttribute() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);
		params.setTranslatePowerpointNotes(true);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/slide-with-note-and-baseline.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"This is my first slide.",
			"This is my first note."
		);
	}

	/**
	 * The document at hand contains a set baseline in the notes master and a baseline reset in the
	 * actual note.
	 */
	@Test
	public void testRunMergingWithBaselineAttributeFromMaster() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);
		params.setTranslatePowerpointNotes(true);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/baseline-on-master.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"This is my first slide.",
			"This is my first <run1>note.</run1>"
		);
	}

	@Test
	public void testExternalRelationships() throws Exception {
		ConditionalParameters params = new ConditionalParameters();
		params.setTranslateDocProperties(false);
		params.setTranslatePowerpointMasters(false);

		OpenXMLFilter filter = new OpenXMLFilter();
		filter.setParameters(params);

		RawDocument doc = new RawDocument(root.in("/Link-to-movie.pptx").asUri(), "UTF-8", locENUS);
		ArrayList<Event> events = getEvents(filter, doc);

		List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(events);
		assertThat(textUnits).extracting(textUnitSourceExtractor()).containsExactlyInAnyOrder(
			"Click <run1>here</run1>."
		);
	}

	@Test
	public void endParagraphPropertiesDoesNotTriggerAdditionalCodesCreation() throws Exception {
		final List<ITextUnit> textUnits = FilterTestDriver.filterTextUnits(
			getEvents(
				new OpenXMLFilter(),
				new RawDocument(root.in("/977.pptx").asUri(), StandardCharsets.UTF_8.name(), locENUS)
			)
		);
		Assertions.assertThat(textUnits.get(1)).extracting(OpenXMLTestHelpers.textUnitSourceExtractor()).isEqualTo(
			"The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. <tags1/>The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. <tags2/>The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog. "
		);
	}

	private ArrayList<Event> getEvents(OpenXMLFilter filter, RawDocument doc) {
		ArrayList<Event> list = new ArrayList<>();
		filter.open(doc, false);
		while (filter.hasNext()) {
			Event event = filter.next();
			list.add(event);
		}
		filter.close();
		return list;
	}
}
