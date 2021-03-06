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

package net.sf.okapi.common.pipelinebuilder;

public enum XPipelineType {

	/**
	 * Steps are connected sequentially, e.g. the output of a previous step connected with 
	 * the input of a next step.
	 */
	SEQUENTIAL,
	
	/**
	 * Steps are connected in parallel, e.g. inputs of all steps are joined together, 
	 * and the outputs are joined together.
	 */
	PARALLEL
}
