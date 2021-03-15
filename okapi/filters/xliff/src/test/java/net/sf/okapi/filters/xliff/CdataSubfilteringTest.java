/*
 * =============================================================================
 * Copyright (C) 2010-2020 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================================
 */
package net.sf.okapi.filters.xliff;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.filters.html.HtmlFilter;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RunWith(JUnit4.class)
public class CdataSubfilteringTest {

    private final FilterConfigurationMapper filterConfigurationMapper;
    private final LocaleId sourceLocale;
    private final LocaleId targetLocale;

    private XLIFFFilter filter;
    private FileLocation fileLocation;

    public CdataSubfilteringTest() {
        this.filterConfigurationMapper = new FilterConfigurationMapper();
        this.filterConfigurationMapper.addConfigurations(HtmlFilter.class.getName());
        this.sourceLocale = LocaleId.ENGLISH;
        this.targetLocale = LocaleId.FRENCH;
    }

    @Before
    public void setUp() {
        this.filter = new XLIFFFilter();
        this.filter.setFilterConfigurationMapper(filterConfigurationMapper);
        this.fileLocation = FileLocation.fromClass(this.getClass());
    }

    @Test
    public void notSubfiltered() {
        final List<Event> events = FilterTestDriver.getEvents(
            filter,
            new RawDocument(
                fileLocation.in("/subfiltering/688-cdata.xlf").asUri(),
                XLIFFFilter.ENCODING.name(),
                this.sourceLocale,
                this.targetLocale
            ),
            new Parameters()
        );
        final ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
        Assertions.assertThat(tu).isNotNull();
        Assertions.assertThat(tu.getSource().getCodedText()).isEqualTo("t1.<p>CDATA in source & &amp;</p>.t3");
        try {
            Assertions.assertThat(
                FilterTestDriver.generateOutput(
                    events,
                    this.targetLocale,
                    filter.createSkeletonWriter(),
                    filter.getEncoderManager(),
                    false
                )
            )
            .isXmlEqualTo(
                new String(
                    Files.readAllBytes(
                        this.fileLocation.in("/subfiltering/688-cdata-gold.xlf").asPath()
                    ),
                    XLIFFFilter.ENCODING.name()
                )
            );
        } catch (final IOException e) {
            Assertions.fail("I/O exception: ", e);
        }
    }

    @Test
    public void subfilteredAsHtml() {
        final Parameters parameters = new Parameters();
        parameters.setCdataSubfilter("okf_html");
        final List<Event> events = FilterTestDriver.getEvents(
            filter,
            new RawDocument(
                fileLocation.in("/subfiltering/688-cdata.xlf").asUri(),
                XLIFFFilter.ENCODING.name(),
                this.sourceLocale,
                this.targetLocale
            ),
            parameters
        );
        final List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(tus).hasSize(2);
        Assertions.assertThat(tus.get(0).getSource().toString()).isEqualTo("CDATA in source & &");
        Assertions.assertThat(tus.get(1).getSource().toString()).isEqualTo("t1.[#$1_ssf1].t3");
        try {
            Assertions.assertThat(
                FilterTestDriver.generateOutput(
                    events,
                    this.targetLocale,
                    filter.createSkeletonWriter(),
                    filter.getEncoderManager(),
                    false
                )
            )
            .isXmlEqualTo(
                new String(
                    Files.readAllBytes(
                        this.fileLocation.in("/subfiltering/688-cdata-subfiltered-gold.xlf").asPath()
                    ),
                    XLIFFFilter.ENCODING.name()
                )
            );
        } catch (final IOException e) {
            Assertions.fail("I/O exception: ", e);
        }
    }

    @Test
    public void inlineNotSubfiltered() {
        final Parameters parameters = new Parameters();
        parameters.setInlineCdata(true);
        final List<Event> events = FilterTestDriver.getEvents(
            filter,
            new RawDocument(
                fileLocation.in("/subfiltering/688-cdata.xlf").asUri(),
                XLIFFFilter.ENCODING.name(),
                this.sourceLocale,
                this.targetLocale
            ),
            parameters
        );
        final ITextUnit tu = FilterTestDriver.getTextUnit(events, 1);
        Assertions.assertThat(tu).isNotNull();
        Assertions.assertThat(tu.getSource().getCodedText()).isEqualTo("t1.\uE101\uE110<p>CDATA in source & &amp;</p>\uE102\uE111.t3");
        try {
            Assertions.assertThat(
                FilterTestDriver.generateOutput(
                    events,
                    this.targetLocale,
                    filter.createSkeletonWriter(),
                    filter.getEncoderManager(),
                    false
                )
            )
            .isXmlEqualTo(
                new String(
                    Files.readAllBytes(
                        this.fileLocation.in("/subfiltering/688-cdata-inline-gold.xlf").asPath()
                    ),
                    XLIFFFilter.ENCODING.name()
                )
            );
        } catch (final IOException e) {
            Assertions.fail("I/O exception: ", e);
        }
    }

    @Test
    public void inlineSubfilteredAsHtml() {
        final Parameters parameters = new Parameters();
        parameters.setInlineCdata(true);
        parameters.setCdataSubfilter("okf_html");
        final List<Event> events = FilterTestDriver.getEvents(
            filter,
            new RawDocument(
                fileLocation.in("/subfiltering/688-cdata.xlf").asUri(),
                XLIFFFilter.ENCODING.name(),
                this.sourceLocale,
                this.targetLocale
            ),
            parameters
        );
        final List<ITextUnit> tus = FilterTestDriver.filterTextUnits(events);
        Assertions.assertThat(tus).hasSize(2);
        Assertions.assertThat(tus.get(0).getSource().toString()).isEqualTo("CDATA in source & &");
        Assertions.assertThat(tus.get(1).getSource().toString()).isEqualTo("t1.<![CDATA[[#$1_ssf1]]]>.t3");
        try {
            Assertions.assertThat(
                FilterTestDriver.generateOutput(
                    events,
                    this.targetLocale,
                    filter.createSkeletonWriter(),
                    filter.getEncoderManager(),
                    false
                )
            )
            .isXmlEqualTo(
                new String(
                    Files.readAllBytes(
                        this.fileLocation.in("/subfiltering/688-cdata-inline-subfiltered-gold.xlf").asPath()
                    ),
                    XLIFFFilter.ENCODING.name()
                )
            );
        } catch (final IOException e) {
            Assertions.fail("I/O exception: ", e);
        }
    }

    @Test
    public void subfilteredWithTargetsCopiedFromSource() {
        final Parameters parameters = new Parameters();
        parameters.setCdataSubfilter("okf_html");
        final List<Event> events = FilterTestDriver.getEvents(
            filter,
            new RawDocument(
                fileLocation.in("/subfiltering/998.xlf").asUri(),
                XLIFFFilter.ENCODING.name(),
                this.sourceLocale,
                this.targetLocale
            ),
            parameters
        );
        try {
            Assertions.assertThat(
                FilterTestDriver.generateOutput(
                    events,
                    this.targetLocale,
                    filter.createSkeletonWriter(),
                    filter.getEncoderManager(),
                    false
                )
            )
            .isXmlEqualTo(
                new String(
                    Files.readAllBytes(
                        this.fileLocation.in("/subfiltering/998-gold.xlf").asPath()
                    ),
                    XLIFFFilter.ENCODING.name()
                )
            );
        } catch (final IOException e) {
            Assertions.fail("I/O exception: ", e);
        }
    }

    @Test
    public void subfilteredWithTargetsCopiedFromSourceAndTranslated() {
        final Parameters parameters = new Parameters();
        parameters.setCdataSubfilter("okf_html");
        final List<Event> events = FilterTestDriver.getEvents(
            filter,
            new RawDocument(
                fileLocation.in("/subfiltering/998.xlf").asUri(),
                XLIFFFilter.ENCODING.name(),
                this.sourceLocale,
                this.targetLocale
            ),
            parameters
        );
        final ITextUnit subfiltered = FilterTestDriver.getTextUnit(events, 1);
        subfiltered.setTarget(this.targetLocale, new TextContainer("Translated CDATA"));
        final ITextUnit tu = FilterTestDriver.getTextUnit(events, 2);
        tu.setTarget(this.targetLocale, tu.getSource());
        try {
            Assertions.assertThat(
                FilterTestDriver.generateOutput(
                    events,
                    this.targetLocale,
                    filter.createSkeletonWriter(),
                    filter.getEncoderManager(),
                    false
                )
            )
            .isXmlEqualTo(
                new String(
                    Files.readAllBytes(
                        this.fileLocation.in("/subfiltering/1002-gold.xlf").asPath()
                    ),
                    XLIFFFilter.ENCODING.name()
                )
            );
        } catch (final IOException e) {
            Assertions.fail("I/O exception: ", e);
        }
    }
}
