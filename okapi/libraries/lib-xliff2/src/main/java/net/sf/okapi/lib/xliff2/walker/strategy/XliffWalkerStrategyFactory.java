/*===========================================================================
  Copyright (C) 2011-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.walker.strategy;

import java.util.Arrays;

import net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector;

/**
 * Factory class to build instances of {@link IXliffWalkerStrategy}
 *
 * @author Vladyslav Mykhalets
 */
public class XliffWalkerStrategyFactory {

    public static IXliffWalkerStrategy defaultStrategy() {
        return new DefaultXliffWalkerStrategy();
    }

    public static IXliffWalkerStrategy flexibleStrategy(XliffWalkerPathSelector... pathSelectors) {
        if (pathSelectors != null && pathSelectors.length > 0) {
            return new FlexibleXliffWalkerStrategy(Arrays.asList(pathSelectors));
        } else {
            return defaultStrategy();
        }
    }

    public static IXliffWalkerStrategy pipelineStrategy() {
        return new PipelineXliffWalkerStrategy();
    }
}
