/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.tex;

import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.IEncoder;

public class TEXEncoder implements IEncoder {

	private static final String LINEBREAK = "\n";
	private static final Map<String,String> map = new HashMap<>();

	public TEXEncoder () {
		reset();
	}

	public String encode (String text) {
		if ( text == null ) return "";
		return convertCodesToLetters(text);
	}
	
	@Override
	public String encode (String text, EncoderContext context){
		if ( text == null ) return "";
		return convertCodesToLetters(text);
	}

	@Override
	public String encode (int value,EncoderContext context){
		switch ( value ) {
			case '\n':
				return LINEBREAK;
			default:
				if ( Character.isSupplementaryCodePoint(value) ) {
					return new String(Character.toChars(value)).replace("\n", LINEBREAK);
				}
				return String.valueOf((char)value).replace("\n", LINEBREAK); 
		}
	}

	@Override
	public String encode (char value,EncoderContext context){
		switch ( value ) {
			case '\n':
				return LINEBREAK;
			default:
				return String.valueOf(value);
		}
	}

	@Override
	public CharsetEncoder getCharsetEncoder () {
		return null;
	}

	@Override
	public String getLineBreak () {
		return LINEBREAK;
	}

	@Override
	public void setOptions (IParameters params,
		String encoding,
		String lineBreak)
	{
		// Line-break is LINEBREAK
		// Encoding is always UTF-8
	}

	@Override
	public String toNative (String propertyName,String value)
	{
		return convertLettersToCodes(value);
	}

	@Override
	public IParameters getParameters() {
		return null;
	}

	@Override
	public String getEncoding() {
		return "";
	}
	
	
	
	private String convertLettersToCodes(String line) {
		return convert(line,true);	
	}
	
	private String convertCodesToLetters(String line) {
		return convert(line,false);
	}
	
	/**
	 * Get Map key using value. If multiple keys correspond have same value,
	 *  longest is returned (\={i} instead of \=\i)
	 * @param value
	 * @return
	 */
	private String getKeyByValue(String value) {
		String key = "";
		for (Entry<String,String> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())){
				if ( key.length() < entry.getKey().length() ) {
					key = entry.getKey();
				}
			}
		}
		return key;
	}
	
	/**
	 * Converts LateX codes+letters into UTF8 letters and back using predefined Map
	 * \c{n} => ņ; \=a => ā and so on
	 * ņ => \c{n} ; ā => \=a and so on
	 * https://www.ntg.nl/doc/biemesderfer/ltxcrib.pdf
	 * @param String line, Map<String,String> map
	 * @return String correctedLine
	 */
	private String convert(String line, boolean convertLettersToCodes) {
		StringBuilder sb = new StringBuilder();
	    List<String> keys = new ArrayList<>(map.keySet());
	    if (convertLettersToCodes) {
	    	keys = new ArrayList<>(map.values());
	    }
//	    sort keys to have longest first
	    next:
	    while (line.length() > 0) {
	        for (String k : keys) {
	            if (line.startsWith(k)) {
	                // we have a match!
	        	    if (convertLettersToCodes) {
	        	    	sb.append(getKeyByValue(k));
	        	    } else {
	        	    	sb.append(map.get(k));
	        	    }
	                line = line.substring(k.length(), line.length());
	                continue next;
	            }
	        }
	        // no match, advance one character
	        sb.append(line.charAt(0));
	        line = line.substring(1, line.length());
	    }
	    return sb.toString();
	}
	
	@Override
	public void reset() {
		// Add all chars to map
		// Some conflicting special symbols
		map.put("\\LaTeX", "\\LaTeX");
		map.put("\\%", "\\%");
//		map.put("\\{", "{");
//		map.put("\\}", "}");
	    // Some national symbols
		map.put("\\oe", "œ");
		map.put("\\OE", "Œ");
		map.put("\\ae", "æ");
		map.put("\\AE", "Æ");
		map.put("\\aa", "å");
		map.put("\\AA", "Å");
		map.put("\\o", "ø");
		map.put("\\O", "Ø");
		map.put("\\l", "ł");
		map.put("\\L", "Ł");
		map.put("\\ss", "ß");
		map.put("\\j", "ȷ");
		// Text-mode accents
//		Áá 	Àà 	Ăă 	Ắắ 	Ằằ 	Ẵẵ 	Ẳẳ 	Ââ 	Ấấ 	Ầầ 	Ẫẫ 	Ẩẩ 	Ǎǎ 	Åå 	Ǻǻ 	Ää 	Ǟǟ 	Ãã 	Ȧȧ 	Ǡǡ 	Ąą 	Ą́ą́ 	Ą̃ą̃ 	Āā 	Ā̀ā̀ 	Ảả
//		Ȁȁ 	A̋a̋ 	Ȃȃ 	Ạạ 	Ặặ 	Ậậ 	Ḁḁ 	Ⱥⱥ 	ᶏ 	ẚ
		map.put("\\`{a}", "à");
		map.put("\\`{A}", "À");
		map.put("\\\'{a}", "á");
		map.put("\\\'{A}", "Á");
		map.put("\\^{a}", "â");
		map.put("\\^{A}", "Â");
		map.put("\\\"{a}", "ä");
		map.put("\\\"{A}", "Ä");
		map.put("\\~{a}", "ã");
		map.put("\\~{A}", "Ã");
		map.put("\\={a}", "ā");
		map.put("\\=a", "ā");
		map.put("\\={A}", "Ā");
		map.put("\\=A", "Ā");
		map.put("\\.{a}", "ȧ");
		map.put("\\.{A}", "Ȧ");
		map.put("\\u{a}", "ă");
		map.put("\\u{A}", "Ă");
		map.put("\\v{a}", "ǎ");
		map.put("\\v{A}", "Ǎ");
		map.put("\\H{a}", "ȁ");
		map.put("\\H{A}", "Ȁ");
		map.put("\\c{a}", "ą");
		map.put("\\c{A}", "Ą");
		map.put("\\d{a}", "ạ");
		map.put("\\d{A}", "Ạ");
		map.put("\\k{a}", "ᶏ");
//		Ḃḃ 	Ḅḅ 	Ḇḇ 	Ƀƀ 	Ɓɓ 	Ꞗꞗ 	ᵬ 	ᶀ
		map.put("\\.{b}", "ḃ");
		map.put("\\.{B}", "Ḃ");
		map.put("\\c{b}", "ḇ");
		map.put("\\c{B}", "Ḇ");
		map.put("\\k{b}", "ᶀ");
//		Ćć 	Ĉĉ 	Čč 	Ċċ 	Çç 	Ḉḉ 	Ȼȼ 	Ꞓꞓ 	Ƈƈ 	ɕ 	
		map.put("\\\'{c}", "ć");
		map.put("\\\'{C}", "Ć");
		map.put("\\\'c", "ć");
		map.put("\\\'C", "Ć");
		map.put("\\^{c}", "ĉ");
		map.put("\\^{C}", "Ĉ");
		map.put("\\.{c}", "ċ");
		map.put("\\.{C}", "Ċ");
		map.put("\\v{c}", "č");
		map.put("\\v{C}", "Č");
		map.put("\\v c", "č");
		map.put("\\v C", "Č");
		map.put("\\c{c}", "ç");
		map.put("\\c{C}", "Ç");
//		Ďď 	Ḋḋ 	Ḑḑ 	D̦d̦ 	Ḍḍ 	Ḓḓ 	Ḏḏ 	Đđ 	Ðð 	Ɖɖ 	Ɗɗ 	ᵭ 	ᶁ 	ᶑ 	ȡ
		map.put("\\.{d}", "ḋ");
		map.put("\\.{D}", "Ḋ");
		map.put("\\v{d}", "ď");
		map.put("\\v{D}", "Ď");
		map.put("\\c{d}", "d̦");
		map.put("\\c{D}", "D̦");
		map.put("\\b{d}", "ḏ");
		map.put("\\b{D}", "Ḏ");
		map.put("\\dj", "đ");
		map.put("\\DJ", "Đ");
//		Éé 	Èè 	Ĕĕ 	Êê 	Ếế 	Ềề 	Ễễ 	Ểể 	Ê̄ê̄ 	Ê̌ê̌ 	Ěě 	Ëë 	Ẽẽ 	Ėė 	Ė́ė́ 	Ė̃ė̃ 	Ȩȩ 	Ḝḝ 	Ęę 	Ę́ę́ 	Ę̃ę̃ 	Ēē 	Ḗḗ 	Ḕḕ 	Ẻẻ 	Ȅȅ
//		E̋e̋ 	Ȇȇ 	Ẹẹ 	Ệệ 	Ḙḙ 	Ḛḛ 	Ɇɇ 	E̩e̩ 	È̩è̩ 	É̩é̩ 	ᶒ 	ⱸ 	ꬴ 	ꬳ 	
		map.put("\\`{e}", "è");
		map.put("\\`{E}", "È");
		map.put("\\\'{e}", "é");
		map.put("\\\'{E}", "É");
		map.put("\\^{e}", "ê");
		map.put("\\^{E}", "Ê");
		map.put("\\\"{e}", "ë");
		map.put("\\\"{E}", "Ë");
		map.put("\\~{e}", "ẽ");
		map.put("\\~{E}", "Ẽ");
		map.put("\\={e}", "ē");
		map.put("\\={E}", "Ē");
		map.put("\\=e", "ē");
		map.put("\\=E", "Ē");
		map.put("\\.{e}", "ė");
		map.put("\\.{E}", "Ė");
		map.put("\\u{e}", "ĕ");
		map.put("\\u{E}", "Ĕ");
		map.put("\\v{e}", "ě");
		map.put("\\v{E}", "Ě");
		map.put("\\H{e}", "ȅ");
		map.put("\\H{E}", "Ȅ");
		map.put("\\c{e}", "ȩ");
		map.put("\\c{E}", "Ȩ");
		map.put("\\d{e}", "ẹ");
		map.put("\\d{E}", "Ẹ");
		map.put("\\k{e}", "ę");
		map.put("\\k{E}", "Ę");
//		Ḟḟ 	Ƒƒ 	Ꞙꞙ 	ᵮ 	ᶂ 	
		map.put("\\.{f}", "ḟ");
		map.put("\\.{F}", "Ḟ");
//		Ǵǵ 	Ğğ 	Ĝĝ 	Ǧǧ 	Ġġ 	G̃g̃ 	Ģģ 	Ḡḡ 	Ǥǥ 	Ꞡꞡ 	Ɠɠ 	ᶃ 	ꬶ 	
		map.put("\\\'{g}", "ǵ");
		map.put("\\\'{G}", "Ǵ");
		map.put("\\^{g}", "ĝ");
		map.put("\\^{G}", "Ĝ");
		map.put("\\.{g}", "ġ");
		map.put("\\.{G}", "Ġ");
		map.put("\\u{g}", "ğ");
		map.put("\\u{G}", "Ğ");
		map.put("\\v{g}", "ģ");
		map.put("\\v g", "ģ");
		map.put("\\v{G}", "Ǧ");
		map.put("\\c{g}", "ģ");
		map.put("\\c{G}", "Ģ");
		map.put("\\c G", "Ģ");
//		Ĥĥ 	Ȟȟ 	Ḧḧ 	Ḣḣ 	Ḩḩ 	Ḥḥ 	Ḫḫ 	H̱ẖ 	Ħħ 	Ⱨⱨ 	Ɦɦ 	ꞕ 	
		map.put("\\^{h}", "ĥ");
		map.put("\\^{H}", "Ĥ");
		map.put("\\\"{h}", "ḧ");
		map.put("\\\"{H}", "Ḧ");
		map.put("\\.{h}", "ḣ");
		map.put("\\.{H}", "Ḣ");
		map.put("\\v{h}", "ȟ");
		map.put("\\v{H}", "Ȟ");
		map.put("\\c{h}", "ḩ");
		map.put("\\c{H}", "Ḩ");
		map.put("\\d{h}", "ḥ");
		map.put("\\d{H}", "Ḥ");
		map.put("\\b{h}", "ẖ");
		map.put("\\b{H}", "H̱");
		map.put("\\k{h}", "ⱨ");
		map.put("\\k{H}", "Ⱨ");
//		Í í 	i̇́ 	Ì ì 	i̇̀ 	Ĭ ĭ 	Î î 	Ǐ ǐ 	Ïï 	Ḯḯ 	Ĩĩ 	i̇̃ 	Į į 	Į́ 	į̇́ 	Į̃ 	į̇̃ 	Ī ī 	Ī̀ ī̀ 	Ỉ ỉ 	Ȉ ȉ 	I̋ i̋ 	Ȋ ȋ 	Ị ị 	Ḭ ḭ 	Ɨ ɨ 	ᶖ
//		İ i 	I ı 	
		map.put("\\`{i}", "ì");
		map.put("\\`{I}", "Ì");
		map.put("\\\'{i}", "í");
		map.put("\\\'{I}", "Í");
		map.put("\\^{i}", "î");
		map.put("\\^{I}", "Î");
		map.put("\\\"{i}", "ï");
		map.put("\\\"{I}", "Ï");
		map.put("\\~{i}", "ĩ");
		map.put("\\~{I}", "Ĩ");
		map.put("\\=\\i", "ī");
		map.put("\\={\\i}", "ī");
		map.put("\\={I}", "Ī");
		map.put("\\=I", "Ī");
//		map.put("\\.{i}", "i");
		map.put("\\.{I}", "İ");
		map.put("\\u{i}", "ĭ");
		map.put("\\u{I}", "Ĭ");
		map.put("\\v{i}", "ǐ");
		map.put("\\v{I}", "Ǐ");
		map.put("\\H{i}", "ȉ");
		map.put("\\H{I}", "Ȉ");
		map.put("\\k{i}", "į");
		map.put("\\k{I}", "Į");
		
//		Ĵĵ 	Ɉɉ 	J̌ǰ 	ȷ 	Ʝʝ 	J̃ 	j̇̃ 	ɟ 	ʄ 	
		map.put("\\^{j}", "ĵ");
		map.put("\\^{J}", "Ĵ");
		map.put("\\v{j}", "ǰ");
		map.put("\\v{J}", "J̌");

//		Ḱḱ 	Ǩǩ 	Ķķ 	Ḳḳ 	Ḵḵ 	Ƙƙ 	Ⱪⱪ 	ᶄ 	Ꝁꝁ 	Ꝃꝃ 	Ꝅꝅ 	Ꞣꞣ 	
		map.put("\\\'{k}", "ḱ");
		map.put("\\\'{K}", "Ḱ");
		map.put("\\v{k}", "ǩ");
		map.put("\\v{K}", "Ǩ");
		map.put("\\c{k}", "ķ");
		map.put("\\c{K}", "Ķ");
		map.put("\\c k", "ķ");
		map.put("\\c K", "Ķ");
		map.put("\\d{k}", "ḳ");
		map.put("\\d{K}", "Ḳ");
		map.put("\\k{k}", "ⱪ");
		map.put("\\k{K}", "Ⱪ");

//		Ĺĺ 	Ľľ 	Ļļ 	Ḷḷ 	Ḹḹ 	L̃l̃ 	Ḽḽ 	Ḻḻ 	Łł 	Ŀŀ 	Ƚƚ 	Ꝉꝉ 	Ⱡⱡ 	Ɫɫ 	Ɬɬ 	ꞎ 	ꬷ 	ꬸ 	ꬹ 	ᶅ 	ɭ 	ȴ 		
		map.put("\\\'{l}", "ĺ");
		map.put("\\\'{L}", "Ĺ");
		map.put("\\c{l}", "ļ");
		map.put("\\c{L}", "Ļ");
		map.put("\\c l", "ļ");
		map.put("\\c L", "Ļ");
		map.put("\\d{l}", "ḷ");
		map.put("\\d{L}", "Ḷ");
		
//		Ḿḿ 	M̋m̋ 	Ṁṁ 	Ṃṃ 	M̃m̃ 	ᵯ 	ᶆ 	Ɱɱ 	ꬺ 	
		map.put("\\\'{m}", "ḿ");
		map.put("\\\'{M}", "Ḿ");
		map.put("\\.{m}", "ṁ");
		map.put("\\.{M}", "Ṁ");
		map.put("\\d{m}", "ṃ");
		map.put("\\d{M}", "Ṃ");
		
//		Ńń 	Ǹǹ 	Ňň 	Ññ 	Ṅṅ 	Ņņ 	Ṇṇ 	Ṋṋ 	Ṉṉ 	N̈n̈ 	Ɲɲ 	Ŋŋ 	Ꞑꞑ 	Ꞥꞥ 	ᵰ 	ᶇ 	ɳ 	ȵ 	ꬻ 	ꬼ 
		map.put("\\`{n}", "ǹ");
		map.put("\\`{N}", "Ǹ");
		map.put("\\\'{n}", "ń");
		map.put("\\\'{N}", "Ń");
		map.put("\\v{n}", "ň");
		map.put("\\v{N}", "Ň");
		map.put("\\c{n}", "ņ");
		map.put("\\c{N}", "Ņ");
		map.put("\\c n", "ņ");
		map.put("\\c N", "Ņ");
		
//		Ṕṕ 	Ṗṗ 	Ᵽᵽ 	Ꝑꝑ 	Ƥƥ 	Ꝓꝓ 	Ꝕꝕ 	P̃p̃ 	ᵱ 	ᶈ 
		map.put("\\\'{p}", "ṕ");
		map.put("\\\'{P}", "Ṕ");
		map.put("\\.{p}", "ṗ");
		map.put("\\.{P}", "Ṗ");

//		Ꝗꝗ 	Ꝙꝙ 	ɋ 	ʠ 	
//		Ŕŕ 	Řř 	Ṙṙ 	Ŗŗ 	Ȑȑ 	Ȓȓ 	Ṛṛ 	Ṝṝ 	Ṟṟ 	R̃r̃ 	Ɍɍ 	Ꞧꞧ 	Ɽɽ 	ᵲ 	ᶉ 	ꭉ 	
		map.put("\\\'{r}", "ŕ");
		map.put("\\\'{R}", "Ŕ");
		map.put("\\.{r}", "ṙ");
		map.put("\\.{R}", "Ṙ");
		map.put("\\v{r}", "ř");
		map.put("\\v{R}", "Ř");
		map.put("\\c{r}", "ŗ");
		map.put("\\c{R}", "Ŗ");
		map.put("\\d{r}", "ṛ");
		map.put("\\d{R}", "Ṛ");

//		Śś 	Ṥṥ 	Ŝŝ 	Šš 	Ṧṧ 	Ṡṡ 	Şş 	Ṣṣ 	Ṩṩ 	Șș 	S̩s̩ 	Ꞩꞩ 	Ȿȿ 	ʂ 	ᶊ 	ᵴ 		
		map.put("\\\'{s}", "ś");
		map.put("\\\'{S}", "Ś");
		map.put("\\^{s}", "ŝ");
		map.put("\\^{S}", "Ŝ");
		map.put("\\.{s}", "ṡ");
		map.put("\\.{S}", "Ṡ");
		map.put("\\v{s}", "š");
		map.put("\\v{S}", "Š");
		map.put("\\v s", "š");
		map.put("\\v S", "Š");
		map.put("\\d{s}", "ṣ");
		map.put("\\d{S}", "Ṣ");
		map.put("\\k{s}", "ş");
		map.put("\\k{S}", "Ş");	

//		Ťť 	Ṫṫ 	Ţţ 	Ṭṭ 	Țț 	Ṱṱ 	Ṯṯ 	Ŧŧ 	Ⱦⱦ 	Ƭƭ 	Ʈʈ 	T̈ẗ 	ᵵ 	ƫ 	ȶ 	
		map.put("\\.{t}", "ṫ");
		map.put("\\.{T}", "Ṫ");
		map.put("\\v{t}", "ť");
		map.put("\\v{T}", "Ť");
		map.put("\\d{t}", "ṭ");
		map.put("\\d{T}", "Ṭ");

//		Úú 	Ùù 	Ŭŭ 	Ûû 	Ǔǔ 	Ůů 	Üü 	Ǘǘ 	Ǜǜ 	Ǚǚ 	Ǖǖ 	Űű 	Ũũ 	Ṹṹ 	Ųų 	Ų́ų́ 	Ų̃ų̃ 	Ūū 	Ṻṻ 	Ū̀ū̀ 	Ū́ū́ 	Ū̃ū̃ 	Ủủ 	Ȕȕ 	Ȗȗ 	Ưư
//		Ứứ 	Ừừ 	Ữữ 	Ửử 	Ựự 	Ụụ 	Ṳṳ 	Ṷṷ 	Ṵṵ 	Ʉʉ 	ᶙ 	ꭒ 	
		map.put("\\`{u}", "ù");
		map.put("\\`{U}", "Ù");
		map.put("\\\'{u}", "ú");
		map.put("\\\'{U}", "Ú");
		map.put("\\^{u}", "û");
		map.put("\\^{U}", "Û");
		map.put("\\\"{u}", "ü");
		map.put("\\\"{U}", "Ü");
		map.put("\\~{u}", "ũ");
		map.put("\\~{U}", "Ũ");
		map.put("\\={u}", "ū");
		map.put("\\={U}", "Ū");
		map.put("\\=u", "ū");
		map.put("\\=U", "Ū");
		map.put("\\u{u}", "ŭ");
		map.put("\\u{U}", "Ŭ");
		map.put("\\v{u}", "ǔ");
		map.put("\\v{U}", "Ǔ");
		map.put("\\H{u}", "ȕ");
		map.put("\\H{U}", "Ȕ");
		map.put("\\d{u}", "ụ");
		map.put("\\d{U}", "Ụ");
		
//		Ṽṽ 	Ṿṿ 	Ꝟꝟ 	Ʋʋ 	Ỽỽ 	ᶌ 	ⱱ 	ⱴ 	
		map.put("\\~{v}", "ṽ");
		map.put("\\~{V}", "Ṽ");
		map.put("\\d{v}", "ṿ");
		map.put("\\d{V}", "Ṿ");

//		Ẃẃ 	Ẁẁ 	Ŵŵ 	Ẅẅ 	Ẇẇ 	Ẉẉ 	W̊ẘ 	Ⱳⱳ 	
		map.put("\\`{w}", "ẁ");
		map.put("\\`{W}", "Ẁ");
		map.put("\\\'{w}", "ẃ");
		map.put("\\\'{W}", "Ẃ");
		map.put("\\^{w}", "ŵ");
		map.put("\\^{W}", "Ŵ");
		map.put("\\\"{w}", "ẅ");
		map.put("\\\"{W}", "Ẅ");
		map.put("\\.{w}", "ẇ");
		map.put("\\.{W}", "Ẇ");
		map.put("\\d{w}", "ẉ");
		map.put("\\d{W}", "Ẉ");
	
//		X́x́ 	X̂x̂ 	Ẍẍ 	X̌x̌ 	Ẋẋ 	X̧x̧ 	X̱x̱ 	X̣x̣ 	ᶍ 	x́
		map.put("\\\'{x}", "x́");
		map.put("\\\'{X}", "X́");
		map.put("\\^{x}", "x̂");
		map.put("\\^{X}", "X̂");
		map.put("\\\"{x}", "ẍ");
		map.put("\\\"{X}", "Ẍ");
		map.put("\\.{x}", "ẋ");
		map.put("\\.{X}", "Ẋ");
		map.put("\\v{x}", "x̌");
		map.put("\\v{X}", "X̌");
		map.put("\\d{x}", "x̣");
		map.put("\\d{X}", "X̣");

//		Ýý 	Ỳỳ 	Ŷŷ 	Y̊ẙ 	Ÿÿ 	Ỹỹ 	Ẏẏ 	Ȳȳ 	Ỷỷ 	Ỵỵ 	Ɏɏ 	Ƴƴ 	Ỿỿ 	
		map.put("\\`{y}", "ỳ");
		map.put("\\`{Y}", "Ỳ");
		map.put("\\\'{y}", "ý");
		map.put("\\\'{Y}", "Ý");
		map.put("\\^{y}", "ŷ");
		map.put("\\^{Y}", "Ŷ");
		map.put("\\\"{y}", "ÿ");
		map.put("\\\"{Y}", "Ÿ");
		map.put("\\~{y}", "ỹ");
		map.put("\\~{Y}", "Ỹ");
		map.put("\\={y}", "ȳ");
		map.put("\\={Y}", "Ȳ");
		map.put("\\.{y}", "ẏ");
		map.put("\\.{Y}", "Ẏ");
		
//		Źź 	Ẑẑ 	Žž 	Żż 	Ẓẓ 	Ẕẕ 	Ƶƶ 	Ȥȥ 	Ⱬⱬ 	Ɀɀ 	ᵶ 	ᶎ 	ʐ 	ʑ 	
		map.put("\\\'{z}", "ź");
		map.put("\\\'{Z}", "Ź");
		map.put("\\^{z}", "ẑ");
		map.put("\\^{Z}", "Ẑ");
		map.put("\\.{z}", "ż");
		map.put("\\.{Z}", "Ż");
		map.put("\\v{z}", "ž");
		map.put("\\v{Z}", "Ž");
		map.put("\\v z", "ž");
		map.put("\\v Z", "Ž");
		map.put("\\d{z}", "ẓ");
		map.put("\\d{Z}", "Ẓ");
	}

}
