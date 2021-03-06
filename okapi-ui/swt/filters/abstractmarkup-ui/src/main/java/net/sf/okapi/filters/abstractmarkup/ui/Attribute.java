/*===========================================================================
  Copyright (C) 2010 by the Okapi Framework contributors
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

package net.sf.okapi.filters.abstractmarkup.ui;

import java.util.ArrayList;
import java.util.List;

import net.sf.okapi.filters.abstractmarkup.config.TaggedFilterConfiguration.RULE_TYPE;

class Attribute {
	
	public static final int SCOPE_ALL = 0;
	public static final int SCOPE_ONLY = 1;
	public static final int SCOPE_ALLEXCEPT = 2;

	String name;
	List<RULE_TYPE> rules;
	int scope;
	String scopeElements;
	List<Condition> conditions;
	List<Condition> wsPreserve;
	List<Condition> wsDefault;

	public Attribute () {
		rules = new ArrayList<>();
		scope = SCOPE_ALL;
		scopeElements = "";
	}
	
}
