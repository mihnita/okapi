/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
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

package net.sf.okapi.filters.table;

import net.sf.okapi.common.filters.AbstractCompoundFilter;
import net.sf.okapi.common.filters.CompoundFilterParameters;

/**
 * Table Filter parameters
 * 
 * @version 0.1, 09.06.2009 
 */

public class Parameters extends CompoundFilterParameters {

	public Parameters(AbstractCompoundFilter parentFilter) {
		super(parentFilter);
		addParameters(net.sf.okapi.filters.table.csv.Parameters.class);
		addParameters(net.sf.okapi.filters.table.fwc.Parameters.class);
		addParameters(net.sf.okapi.filters.table.tsv.Parameters.class);
	}
}

