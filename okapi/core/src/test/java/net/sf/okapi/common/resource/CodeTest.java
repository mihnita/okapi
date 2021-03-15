/*===========================================================================
  Copyright (C) 2008-2013 by the Okapi Framework contributors
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

package net.sf.okapi.common.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.resource.TextFragment.TagType;

@RunWith(JUnit4.class)
public class CodeTest {

    @Before
    public void setUp(){
    }

    @Test
    public void testAccess () {
    	Code code = new Code(TagType.OPENING, "ctype", "data");
    	assertEquals("data", code.getData());
    	assertEquals("ctype", code.getType());
    	assertEquals(TagType.OPENING, code.getTagType());
    	assertEquals("data", code.getOuterData()); // default
    	code.setOuterData("outerData");
    	assertEquals("outerData", code.getOuterData());
    	assertEquals("data", code.getData());
    }

    @Test
    public void testSimpleAnnotations () {
    	Code code = new Code(TagType.OPENING, "ctype", "data");
    	code.setAnnotation("displayText", new InlineAnnotation("[display]"));
    	assertEquals("[display]", code.getAnnotation("displayText").getData());
    	GenericAnnotation.addAnnotation(code, new GenericAnnotation("disp", "disp_value", "[display]"));
    	assertEquals("[display]", code.getGenericAnnotationString("disp", "disp_value"));
    }

    @Test
    public void testAnnotationsAccess () {
    	Code code = new Code(TagType.OPENING, "ctype", "data");
    	code.setAnnotation("displayText", new InlineAnnotation("[display]"));
    	GenericAnnotation genAnn = new GenericAnnotation("disp", "disp_value", "[display]");
    	GenericAnnotation.addAnnotation(code, genAnn);

    	Set<String> types = code.getAnnotationsTypes();
    	assertEquals(2, types.size());
    	int test = 0;
    	for ( String type : types ) {
    		InlineAnnotation annotation1 = code.getAnnotation(type);
    		assertNotNull(annotation1);
    		String storage = annotation1.toString();
    		if ( storage.equals("[display]") ) test++;
    		else if ( storage.contains("disp_value") ) test++;
    	}
    	assertEquals(2, test);
    }

    @Test
    public void testFlags () {
    	Code code = new Code(TagType.OPENING, "ctype", "data");
    	assertFalse(code.isCloneable());
    	assertFalse(code.isDeleteable());
    	assertFalse(code.hasReference());
		assertFalse(code.isMarkerMasking());
    	code.setDeleteable(true);
    	code.setCloneable(true);
    	code.setReferenceFlag(true);
    	code.setMarkerMasking(true);
    	assertTrue(code.isCloneable());
    	assertTrue(code.isDeleteable());
    	assertTrue(code.hasReference());
    	assertTrue(code.isMarkerMasking());
    }
    
    @Test
    public void testClone () {
    	Code code = new Code(TagType.OPENING, "ctype", "data");
    	code.setOuterData("out1");
    	Code c2 = code.clone();
    	assertNotSame(code, c2);
    	assertEquals(code.getId(), c2.getId());
    	assertEquals(code.getData(), c2.getData());
    	assertNotSame(code.data, c2.data);
    	assertEquals(code.getTagType(), c2.getTagType());
    	assertEquals(code.getType(), c2.getType());
    	assertEquals(code.getOuterData(), c2.getOuterData());
    	assertNotSame(code.outerData, c2.outerData);
    }

    @Test
    public void testStrings () {
    	ArrayList<Code> codes = new ArrayList<>();
    	codes.add(new Code(TagType.OPENING, "bold", "<b>"));
    	codes.add(new Code(TagType.PLACEHOLDER, "break", "<br/>"));
    	codes.add(new Code(TagType.CLOSING, "bold", "</b>"));
    	String tmp = Code.codesToString(codes);
    	
    	assertNotNull(tmp);
    	List<Code> codesAfter = Code.stringToCodes(tmp);
    	assertEquals(3, codesAfter.size());
    	
    	Code code = codesAfter.get(0);
    	assertEquals("<b>", code.getData());
    	assertEquals(TagType.OPENING, code.getTagType());
    	assertEquals("bold", code.getType());
    	
    	code = codesAfter.get(1);
    	assertEquals("<br/>", code.getData());
    	assertEquals(TagType.PLACEHOLDER, code.getTagType());
    	assertEquals("break", code.getType());
    	
    	code = codesAfter.get(2);
    	assertEquals("</b>", code.getData());
    	assertEquals(TagType.CLOSING, code.getTagType());
    	assertEquals("bold", code.getType());
    }
    
    @Test
    public void testCodeGenericAnnotations () {
    	GenericAnnotations anns1 = new GenericAnnotations();
		GenericAnnotation ann11 = anns1.add("type1");
		ann11.setString("name1", "v1");
		GenericAnnotation ann12 = anns1.add("type1");
		ann12.setString("name2", "v2-not-over");
		Code code = new Code(TagType.PLACEHOLDER, "z");
		GenericAnnotations.addAnnotations(code, anns1);

		GenericAnnotations res = (GenericAnnotations)code.getAnnotation(GenericAnnotationType.GENERIC);
		assertNotNull(res);
		assertEquals(anns1, res);
		
		GenericAnnotations anns2 = new GenericAnnotations();
		GenericAnnotation ann21 = anns2.add("type1");
		ann21.setString("name3", "v3");
		GenericAnnotation ann22 = anns2.add("type1");
		ann22.setString("name2", "another name2");
		
		GenericAnnotations.addAnnotations(code, anns2);
		res = (GenericAnnotations)code.getAnnotation(GenericAnnotationType.GENERIC);
		assertNotNull(res);
		assertEquals(anns1, res);
		List<GenericAnnotation> list = res.getAnnotations("type1");
		assertEquals(4, list.size());
		GenericAnnotation ann = list.get(1);
		assertEquals("v2-not-over", ann.getString("name2"));		
    	
    	ArrayList<Code> codes = new ArrayList<>();
    	codes.add(code);
    	assertTrue(code.getGenericAnnotations() instanceof GenericAnnotations);
    	
    	String tmp = Code.codesToString(codes);
    	
    	assertNotNull(tmp);
    	List<Code> codesAfter = Code.stringToCodes(tmp);
    	assertEquals(1, codesAfter.size());
    	
    	code = codesAfter.get(0);
    	assertTrue(code.getGenericAnnotations() instanceof GenericAnnotations);
    }

    @Test
    public void testCodeData () {
    	Code code = new Code(TagType.PLACEHOLDER, "type", null);
    	assertEquals("", code.toString());
    	
    	code = new Code(TagType.PLACEHOLDER, "type", null);
    	code.setOuterData("<x id=\"1\">");
    	assertEquals("", code.toString());
    	code.setOuterData(null);
    	assertEquals("", code.toString());
    }

    void checkConstructedCode(Code code, String data, int flag, TagType tagType, String type) {
    	assertEquals(-1, code.id);
    	assertNull(code.originalId);
    	assertNull(code.mergedData);
    	assertNull(code.displayText);
    	assertNull(code.outerData);

    	assertEquals(data, code.data.toString());
    	assertEquals(flag, code.flag);
    	assertEquals(tagType, code.tagType);
    	assertEquals(type, code.type);
    }

    @Test
    public void testConstructors() {
    	String data = "";
    	checkConstructedCode(new Code(),
    			data, /*flag*/ 0, /*tagType*/ null, /* type*/ Code.TYPE_NULL);

    	checkConstructedCode(new Code("type"),
    			data, /*flag*/ 0, /*tagType*/ null, /* type*/ "type");

    	checkConstructedCode(new Code(TagType.PLACEHOLDER, "type"),
    			data, /*flag*/ 0, /*tagType*/ TagType.PLACEHOLDER, /* type*/ "type");

    	data = "data";
    	checkConstructedCode(new Code(TagType.PLACEHOLDER, "type", "data"),
    			data, /*flag*/ 0, /*tagType*/ TagType.PLACEHOLDER, /* type*/ "type");

    	String dataWithXRef = "With with [#$ ref marker";
    	Code code = new Code(TagType.PLACEHOLDER, "type", dataWithXRef);
    	checkConstructedCode(code,
    			dataWithXRef, /*flag*/ 1, /*tagType*/ TagType.PLACEHOLDER, /* type*/ "type");

    	code = new Code(TagType.PLACEHOLDER, "type");
    	checkConstructedCode(code,
    			/*data*/ "", /*flag*/ 0, /*tagType*/ TagType.PLACEHOLDER, /* type*/ "type");
    	code.setData(dataWithXRef);
    	checkConstructedCode(code,
    			dataWithXRef, /*flag*/ 1, /*tagType*/ TagType.PLACEHOLDER, /* type*/ "type");
    }


	@Test
	public void testCodeStorageWithOriginalId () {
		Code code1 = new Code(TagType.PLACEHOLDER, "type");
		code1.originalId = "oriId";
		code1.setId(111);
		GenericAnnotations anns1 = new GenericAnnotations(new GenericAnnotation("typeA", "field1", 123));
		GenericAnnotations.addAnnotations(code1, anns1);
		List<Code> codes1 = new ArrayList<>();
		codes1.add(code1);

		// Try with just the codes
		String storageCode = Code.codesToString(codes1);
		checkCodes(Code.stringToCodes(storageCode));

		// Try with a text container
		TextFragment tf = new TextFragment("text ");
		tf.append(code1);
		TextContainer tc1 = new TextContainer(tf);
		String storageTc = TextContainer.contentToString(tc1);
		TextContainer tc2 = TextContainer.stringToContent(storageTc);
		checkCodes(tc2.getFirstSegment().getContent().getCodes());
	}
	
	@Test
	public void testBackwardCompatibility () {
		// Same code as testCodeStorageWithOriginalId
		// But saved using the old storage way (without the class name, etc.)
		
		// Try with old version of the code storage string
		// It's in base 64 to avoid issue with control characters
		String storageCodeB64 = "UExBQ0VIT0xERVLCnDExMcKcdHlwZcKcwpwwwpxvcmlJZMKcbnVsbMKcZ2VuZXJpY8Kewpp0eXBlQcKbZmllbGQxwplpwpkxMjPCn8Kd";
		byte[] array = Base64.getDecoder().decode(storageCodeB64);
		String oldStorageCode = new String(array, StandardCharsets.UTF_8);
		checkCodes(Code.stringToCodes(oldStorageCode));
		
		// Try with old version of the text container storage string
		// It's in base 64 to avoid issue with control characters
		String storageTcB64 = "MDF0ZXh0IO6Eg+6EkO6CkVBMQUNFSE9MREVSwpwxMTHCnHR5cGXCnMKcMMKcb3JpSWTCnG51bGzCnGdlbmVyaWPCnsKadHlwZUHCm2ZpZWxkMcKZacKZMTIzwp/Cne6CkTDugpI=";
		array = Base64.getDecoder().decode(storageTcB64);
		String oldStorageTc = new String(array, StandardCharsets.UTF_8);
		TextContainer tc2 = TextContainer.stringToContent(oldStorageTc);
		checkCodes(tc2.getFirstSegment().getContent().getCodes());
	}
	
	private void checkCodes (List<Code> codes) {
		// IMPORTANT: changing data in the code means you have also to change
		// the Base 64 input strings in testBackwardCompatibility()
		// Those should be generated with older version of the library
		assertEquals(1, codes.size());
		Code code2 = codes.get(0);
		assertEquals(111, code2.getId());
		assertEquals("type", code2.getType());
		assertEquals(TagType.PLACEHOLDER, code2.getTagType());
		assertEquals("oriId", code2.getOriginalId());
		InlineAnnotation iann2 = code2.getAnnotation("generic");
		GenericAnnotations anns2 = (GenericAnnotations)iann2;
		GenericAnnotation ann2 = anns2.getAnnotations("typeA").get(0);
		assertEquals(123, (int)ann2.getInteger("field1"));
	}

}
