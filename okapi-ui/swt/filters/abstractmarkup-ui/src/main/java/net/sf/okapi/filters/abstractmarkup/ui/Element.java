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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import net.sf.okapi.filters.abstractmarkup.config.TaggedFilterConfiguration.RULE_TYPE;

class Element {
	
	String name;
	List<RULE_TYPE> rules;
	List<Condition> conditions;
	String subFilter = "";
	Map<String, List<Condition>> transAttr;
	
	public Element () {
		rules = new ArrayList<>();
		transAttr = new Hashtable<>();
	}

}
