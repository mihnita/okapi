/*===========================================================================
  Copyright (C) 2019 by the Okapi Framework contributors
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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotations;

@RunWith(JUnit4.class)
public class InlineAnnotationTest {

    @Test
    public void testToAndFromStringSimple () {
    	InlineAnnotation ann1 = new InlineAnnotation("data");
    	String storage = ann1.toString();
    	InlineAnnotation ann2 = InlineAnnotation.createFromString(storage);
    	assertEquals(ann2.getData(), ann1.getData());

    	ann1 = new InlineAnnotation("");
    	storage = ann1.toString();
    	ann2 = InlineAnnotation.createFromString(storage);
    	assertEquals("", ann2.getData());

    	ann1 = new InlineAnnotation(null);
    	storage = ann1.toString();
    	ann2 = InlineAnnotation.createFromString(storage);
    	assertEquals(ann1.getData(), ann2.getData());
    }

    @Test
    public void testToAndFromStringComplex () {
    	GenericAnnotations ganns1 = new GenericAnnotations();
    	ganns1.setData("data1");
    	ganns1.add("type1");
    	ganns1.add(new GenericAnnotation("type2", "field21", "value21", "field22", 22));
    	ganns1.add(new GenericAnnotation("type3", "field31", true));
    	
    	InlineAnnotation iann1 = ganns1;
    	String storage = iann1.toString();
    	InlineAnnotation iann2 = InlineAnnotation.createFromString(storage);
    	assertEquals(iann2.getData(), iann1.getData());
    	GenericAnnotations ganns2 = (GenericAnnotations)iann2;
    	List<GenericAnnotation> list = ganns2.getAllAnnotations();
    	assertEquals(3, list.size());
    	int check = 0;
    	for ( GenericAnnotation ga : ganns2 ) {
    		switch ( ga.getType() ) {
    		case "type1":
    			check += 1;
    			break;
    		case "type2":
    			if ( "value21".equals(ga.getString("field21")) ) check += 10;
    			break;
    		case "type3":
    			if ( ga.getBoolean("field31") ) check += 100;
    			break;
    		}

    	}
    	assertEquals(111, check);

    	// With data set to null, and re-using the same variables
    	// (fromString should reset them)
    	ganns1 = new GenericAnnotations();
    	ganns1.add("typeA");
    	iann1 = ganns1;
    	storage = iann1.toString();
    	iann2 = InlineAnnotation.createFromString(storage);
    	assertEquals(iann2.getData(), iann1.getData());
    	ganns2 = (GenericAnnotations)iann2;
    	list = ganns2.getAllAnnotations();
    	assertEquals(1, list.size());
    	assertEquals("typeA", list.get(0).getType());
    }

}
