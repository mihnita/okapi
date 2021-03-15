/*
 * =============================================================================
 *   Copyright (C) 2010-2018 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */

package net.sf.okapi.common;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;

import static java.util.Arrays.asList;

@RunWith(JUnit4.class)
public class StringSanitiserTest {

    private static final String A_STRING_IS_NOT_VALID = "A string \"%s\" is not valid";

    @Test
    public void worksWithEmptyFilters() {
        final Sanitiser<String> sanitiser = new StringSanitiser(Collections.emptyList());
        Assertions.assertThat(sanitiser.sanitise(" s ")).isEqualTo(" s ");
    }

    @Test
    public void worksWithTrimmingFilter() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                Collections.singletonList(new StringSanitiser.TrimmingFilter())
        );
        Assertions.assertThat(sanitiser.sanitise(" t ")).isEqualTo("t");
    }

    @Test
    public void worksWithIntegerParsingFilter() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                Collections.singletonList(
                        new StringSanitiser.IntegerParsingFilter(A_STRING_IS_NOT_VALID)
                )
        );
        Assertions.assertThat(sanitiser.sanitise("1")).isEqualTo("1");
    }

    @Test
    public void failsWithIntegerParsingFilter() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                Collections.singletonList(
                        new StringSanitiser.IntegerParsingFilter(A_STRING_IS_NOT_VALID)
                )
        );
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> sanitiser.sanitise(" 1 "))
                .withMessage("A string \" 1 \" is not valid")
                .withCause(new NumberFormatException("For input string: \" 1 \""));
    }

    @Test
    public void worksWithDoubleParsingFilter() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                Collections.singletonList(
                        new StringSanitiser.DoubleParsingFilter(A_STRING_IS_NOT_VALID)
                )
        );
        Assertions.assertThat(sanitiser.sanitise("1.0")).isEqualTo("1.0");
    }

    @Test
    public void failsWithDoubleParsingFilter() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                Collections.singletonList(
                        new StringSanitiser.DoubleParsingFilter(A_STRING_IS_NOT_VALID)
                )
        );
        Assertions.assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> sanitiser.sanitise(" 1.0pt "))
                .withMessage("A string \" 1.0pt \" is not valid")
                .withCause(new NumberFormatException("For input string: \"1.0pt\""));
    }

    @Test
    public void worksWithTrimmingAndIntegerParsingFilters() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                asList(
                        new StringSanitiser.TrimmingFilter(),
                        new StringSanitiser.IntegerParsingFilter(A_STRING_IS_NOT_VALID)
                )
        );
        Assertions.assertThat(sanitiser.sanitise("  1  ")).isEqualTo("1");
    }

    @Test
    public void worksWithTrimmingAndDoubleParsingFilters() {
        final Sanitiser<String> sanitiser = new StringSanitiser(
                asList(
                        new StringSanitiser.TrimmingFilter(),
                        new StringSanitiser.DoubleParsingFilter(A_STRING_IS_NOT_VALID)
                )
        );
        Assertions.assertThat(sanitiser.sanitise("  1.0  ")).isEqualTo("1.0");
    }
}
