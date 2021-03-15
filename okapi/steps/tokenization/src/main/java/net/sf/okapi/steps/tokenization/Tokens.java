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

import net.sf.okapi.common.ListUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * List of {@link Token} and configuration for each token type.
 */

public class Tokens extends ArrayList<Token> {
    private static final TreeMap<Integer, TokenConfigs.TokenConfig> idMap;
    private static final TreeMap<String, TokenConfigs.TokenConfig> nameMap;
    private static final TokenConfigs configs;

    static {
        configs = new TokenConfigs();
        idMap = new TreeMap<>();
        nameMap = new TreeMap<>();
        configs.loadFromResource(TokenConfigs.TokenConfig.class, "/token_types.tprm");
        for (TokenConfigs.TokenConfig c : configs) {
            if (c == null) {
                continue;
            }
            idMap.put(c.getId(), c);
            nameMap.put(c.getName(), c);
        }
    }

    public static int getTokenIndex(String tokenName) {
        if (nameMap == null) {
            return 0;
        }
        TokenConfigs.TokenConfig c = nameMap.get(tokenName);
        return configs.indexOf(c) + 1; // id is 1-based
    }

    public static int getTokenId(String tokenName) {
        if (nameMap == null) {
            return -1;
        }
        TokenConfigs.TokenConfig c = nameMap.get(tokenName);
        return (c != null) ? c.getId() : -1;
    }

    public static String getTokenName(int tokenId) {
        if (idMap == null) {
            return "";
        }
        TokenConfigs.TokenConfig c = idMap.get(tokenId);
        return (c != null) ? c.getName() : "";
    }

    public static String getTokenDescription(int tokenId) {
        if (idMap == null) {
            return "";
        }
        TokenConfigs.TokenConfig c = idMap.get(tokenId);
        return (c != null) ? c.getDescription() : "";
    }

    public static String getTokenDescription(String tokenName) {
        if (nameMap == null) {
            return "";
        }
        TokenConfigs.TokenConfig c = nameMap.get(tokenName);
        return (c != null) ? c.getDescription() : "";
    }

    public static String getTokenNamesStr() {
        if (nameMap == null) {
            return "";
        }
        return ListUtil.arrayAsString(nameMap.keySet().toArray(new String[]{}));
    }

    public static Collection<TokenConfigs.TokenConfig> getRules() {
        // Returns token items, sorted by id
        if (idMap == null) {
            return null;
        }
        return idMap.values();
    }

    /**
     * Gets list of names of all tokens.
     *
     * @return List of available token names.
     */
    public static List<String> getTokenNames() {
        return new ArrayList<>(nameMap.keySet());
    }

    /**
     * Gets list of IDs of all tokens.
     *
     * @return List of available token IDs.
     */
    public static List<Integer> getTokenIDs() {
        List<Integer> idList = new ArrayList<>();
        for (String tokenName : nameMap.keySet()) {
            idList.add(getTokenId(tokenName));
        }
        return idList;
    }

    /**
     * Gets list of IDs of the given tokens.
     *
     * @return List of token IDs.
     */
    public static List<Integer> getTokenIDs(List<String> tokenNames) {
        List<Integer> idList = new ArrayList<>();
        if (tokenNames != null) {
            for (String tokenName : tokenNames) {
                idList.add(getTokenId(tokenName));
            }
        }
        return idList;
    }

    /**
	 * Return a list of TokenType objects. If tokenNames are specified, only the
	 * tokens with those names will be placed in the resulting list. If tokenNames
	 * is omitted, the list of all tokens will be returned.
	 *
	 * @param tokenNames Optional array of strings with token names to include.
	 * @return List of tokens.
	 */
    public Tokens getFilteredList(String... tokenNames) {
        List<String> names;

        if (tokenNames == null || tokenNames.length == 0) {
            return this;
        } else {
            names = Arrays.asList(tokenNames);
        }

        Tokens res = new Tokens();
        for (int i = 0; i < size(); i++) {
            Token token = get(i);
            if (token == null) {
                continue;
            }
            if (names.contains(token.getName())) {
                res.add(token);
            }
        }

        return res;
    }

    /**
	 * Return a list of TokenType objects. If tokenNames are specified, they will be
	 * excluded.
	 *
	 * @param tokenNames Optional array of strings of token names to exclude.
	 * @return List of tokens.
	 */
    public Tokens getExcludedFilteredList(String... tokenNames) {
        List<String> names;

        if (tokenNames == null || tokenNames.length == 0) {
            return this;
        } else {
            names = Arrays.asList(tokenNames);
        }

        Tokens res = new Tokens();
        for (int i = 0; i < size(); i++) {
            Token token = get(i);
            if (token == null) {
                continue;
            }
            if (!names.contains(token.getName())) {
                res.add(token);
            }
        }

        return res;
    }

    public void fixRanges(List<Integer> markerPositions) {
        for (Integer pos : markerPositions) {
            for (Token token : this) {
                if (token.getRange().start > pos) {
                    token.getRange().start += 2;
                }
                if (token.getRange().end > pos) {
                    token.getRange().end += 2;
                }
            }
        }
    }

    @Override
    public String toString() {
        List<String> res = new ArrayList<>();
        for (Token token : this) {
            res.add(token.toString());
        }
        return ListUtil.listAsString(res, "\n");
    }
}
