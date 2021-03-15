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

package net.sf.okapi.steps.tokenization;

import net.sf.okapi.common.AbstractGroupParameters;
import net.sf.okapi.common.ParametersString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeMap;

/**
 * TokenType Definitions
 */
public class TokenConfigs extends AbstractGroupParameters implements List<TokenConfigs.TokenConfig> {
	private final List<TokenConfig> tokenConfigs = new ArrayList<>();
	private final TreeMap<Integer, TokenConfig> idMap = new TreeMap<>();

	@Override
	public void reset() {
		if (tokenConfigs != null) {
			tokenConfigs.clear();
		}
	}

	@Override
	public void load(ParametersString buffer) {
		loadGroup(buffer, "Token", tokenConfigs, TokenConfig.class);
		idMap.clear();
		for (TokenConfig c : tokenConfigs) {
			idMap.put(c.getId(), c);
		}
	}

	@Override
	public void save(ParametersString buffer) {
		saveGroup(buffer, "Token", tokenConfigs);
	}

	/**
	 * Gets a rule by its tokenId
	 *
	 * @param tokenId ID of the rule.
	 * @return TokenConfig object of null if no rule has been assigned to the given lexem ID.
	 */
	public TokenConfig getRule(int tokenId) {
		return idMap.get(tokenId);
	}

	@Override
	public boolean add(TokenConfig o) {
		return tokenConfigs.add(o);
	}

	@Override
	public void add(int index, TokenConfig element) {
		tokenConfigs.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends TokenConfig> c) {
		return tokenConfigs.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends TokenConfig> c) {
		return tokenConfigs.addAll(index, c);
	}

	@Override
	public void clear() {
		tokenConfigs.clear();
	}

	@Override
	public boolean contains(Object o) {
		return tokenConfigs.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return tokenConfigs.containsAll(c);
	}

	@Override
	public TokenConfig get(int index) {
		return tokenConfigs.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return tokenConfigs.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return tokenConfigs.isEmpty();
	}

	@Override
	public Iterator<TokenConfig> iterator() {
		return tokenConfigs.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return tokenConfigs.lastIndexOf(o);
	}

	@Override
	public ListIterator<TokenConfig> listIterator() {
		return tokenConfigs.listIterator();
	}

	@Override
	public ListIterator<TokenConfig> listIterator(int index) {
		return tokenConfigs.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		return tokenConfigs.remove(o);
	}

	@Override
	public TokenConfig remove(int index) {
		return tokenConfigs.remove(index);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return tokenConfigs.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return tokenConfigs.retainAll(c);
	}

	@Override
	public TokenConfig set(int index, TokenConfig element) {
		return tokenConfigs.set(index, element);
	}

	@Override
	public int size() {
		return tokenConfigs.size();
	}

	@Override
	public List<TokenConfig> subList(int fromIndex, int toIndex) {
		return tokenConfigs.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return tokenConfigs.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return tokenConfigs.toArray(a);
	}

	public static class TokenConfig extends AbstractGroupParameters {
		/**
		 * Short name for the rule
		 */
		private String name;

		/**
		 * TokenConfig description.
		 */
		private String description;

		/**
		 * RBBI token id.
		 */
		private int id;

		/**
		 * Exemplary text containing fragments to be captured by the pattern.
		 */
		private String sample;

		@Override
		public void reset() {
			name = "";
			description = "";
			id = 0;
			sample = "";
		}

		@Override
		protected void load(ParametersString buffer) {
			name = buffer.getString("name", "");
			description = buffer.getString("description", "");
			id = buffer.getInteger("id", 0);
			sample = buffer.getString("sample", "");
		}

		@Override
		protected void save(ParametersString buffer) {
			buffer.setString("name", name);
			buffer.setString("description", description);
			buffer.setInteger("id", id);
			buffer.setString("sample", sample);
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getSample() {
			return sample;
		}

		public void setSample(String sample) {
			this.sample = sample;
		}
	}
}
