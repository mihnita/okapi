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
package net.sf.okapi.common.filters;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class SubFilterEventConverterTest {
    @DataProvider
    public static Object[][] refIdsRemainedSameProvider() {
        return new Object[][] {
            {""},
            {"non-ref content"},
            {
                "before ref "
                + TextFragment.REFMARKER_START
                + "1[2\\][3"
                + TextFragment.REFMARKER_END
            },
            {
                "before ref "
                + TextFragment.REFMARKER_START
                + "1[2\\][3"
                + TextFragment.REFMARKER_END
                + "after ref"
            },
            {
                "before ref "
                + TextFragment.REFMARKER_START
                + "1[2\\][3"
                + TextFragment.REFMARKER_END
                + TextFragment.REFMARKER_START
                + "4[5\\][6"
                + TextFragment.REFMARKER_END
            },
        };
    }

    @Test
    @UseDataProvider("refIdsRemainedSameProvider")
    public void refIdsRemainedSame(final String string) {
        Assertions.assertThat(
            new SubFilterEventConverter(
                new SubFilter(
                    new StubFilter(),
                    null,
                    1,
                    "parent-id",
                    "parent-name"
                ),
                null
            ).convertRefIds(string)
        ).isEqualTo(string);
    }

    @Test
    public void refIdsConverted() {
        final SubFilterEventConverter converter = new SubFilterEventConverter(
            new SubFilter(
                new StubFilter(),
                null,
                1,
                "parent-id[0][1",
                "parent-name[0][1"
            ),
            null
        );
        converter.convertEvent(
            new Event(
                EventType.DOCUMENT_PART,
                new DocumentPart(
                    "dp1",
                    true,
                    new GenericSkeleton("<span lang=\"[#$$self$@%language]\">")
                )
            )
        );
        Assertions.assertThat(
            converter.convertRefIds(
                TextFragment.REFMARKER_START.concat("dp1").concat(TextFragment.REFMARKER_END)
            )
        ).isEqualTo(TextFragment.REFMARKER_START.concat("parent-id[0\\][1_sf1_dp1").concat(TextFragment.REFMARKER_END));
    }
}
