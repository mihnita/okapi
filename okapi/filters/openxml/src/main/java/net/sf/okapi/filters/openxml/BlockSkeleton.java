/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

import java.util.List;
import java.util.Map;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISkeleton;

public class BlockSkeleton implements ISkeleton {
	private final Block block;
	private final RunProperties baseRunProperties;
	private final List<XMLEvents> hiddenCodes;
	private final Map<Integer, XMLEvents> visibleCodes;

	private IResource parent;

	BlockSkeleton(
		final Block block,
		final RunProperties baseRunProperties,
		final List<XMLEvents> hiddenCodes,
		final Map<Integer, XMLEvents> visibleCodes
	) {
		this.block = block;
		this.baseRunProperties = baseRunProperties;
		this.hiddenCodes = hiddenCodes;
		this.visibleCodes = visibleCodes;
	}

	@Override
	public ISkeleton clone() {
		BlockSkeleton blockSkeleton =  new BlockSkeleton(block, baseRunProperties, hiddenCodes, visibleCodes);
		blockSkeleton.setParent(getParent());
		return  blockSkeleton;
	}

	Block block() {
		return block;
	}

	RunProperties baseRunProperties() {
		return baseRunProperties;
	}

	List<XMLEvents> hiddenCodes() {
		return this.hiddenCodes;
	}

	Map<Integer, XMLEvents> visibleCodes() {
		return this.visibleCodes;
	}

	@Override
	public void setParent(IResource parent) {
		this.parent = parent;
	}

	@Override
	public IResource getParent() {
		return parent;
	}

}
