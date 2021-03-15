/*
 * =============================================================================
 *   Copyright (C) 2010-2019 by the Okapi Framework contributors
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
package net.sf.okapi.filters.mif;

import org.slf4j.Logger;

/**
 * Provides a Char literal token.
 */
final class CharLiteralToken implements Token {
    private final Token token;
    private final Logger logger;

    CharLiteralToken(final Token token, final Logger logger) {
        this.token = token;
        this.logger = logger;
    }

    @Override
    public Type type() {
        return this.token.type();
    }

    @Override
    public String toString() {
        switch (this.token.toString()) {
            case "":
                return this.token.toString();
            case "Tab":
                return "\t";
            case "HardSpace":
                return "\u00a0"; // = Unicode non-breaking space
            case "SoftHyphen":
                return "";       // "\u2010" = Unicode Hyphen (not Soft-Hyphen), but we remove those
            case "HardHyphen":
                return "\u2011"; // = Unicode Non-Breaking Hyphen
            case "DiscHyphen":
                return "\u00ad"; // = Unicode Soft-Hyphen
            case "NoHyphen":
                return "\u200d"; // = Unicode Zero-Width Joiner
            case "Cent":
                return "\u00a2";
            case "Pound":
                return "\u00a3";
            case "Yen":
                return "\u00a5";
            case "EnDash":
                return "\u2013";
            case "EmDash":
                return "\u2014";
            case "Dagger":
                return "\u2020";
            case "DoubleDagger":
                return "\u2021";
            case "Bullet":
                return "\u2022";
            case "HardReturn":
                return "\n";
            case "NumberSpace":
                return "\u2007";
            case "ThinSpace":
                return "\u2009";
            case "EnSpace":
                return "\u2002";
            case "EmSpace":
                return "\u2003";
            default:
                this.logger.warn("Unknown Char literal will be ignored: '{}'", this.token.toString());
                return "";
        }
    }
}
