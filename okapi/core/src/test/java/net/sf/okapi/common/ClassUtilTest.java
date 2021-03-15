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

package net.sf.okapi.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.exceptions.OkapiException;

@RunWith(JUnit4.class)
public class ClassUtilTest {
	private static final String DEFAULT_ENCODING = "UTF-8";

	@Test
	public void testGetPackageName() {

		assertEquals("", ClassUtil.getPackageName(null));
		assertEquals("net.sf.okapi.common", ClassUtil.getPackageName(this.getClass()));
		assertEquals("java.lang", ClassUtil.getPackageName(String.class));
		// TODO test on a class w/o package info
	}

	@Test
	public void testExtractPackageName() {

		assertEquals("", ClassUtil.extractPackageName(null));
		assertEquals("", ClassUtil.extractPackageName(""));
		assertEquals("", ClassUtil.extractPackageName("aaa/bbb/ccc"));
		assertEquals("aaa.bbb", ClassUtil.extractPackageName("aaa.bbb.ccc"));
		assertEquals("net.sf.okapi.common", ClassUtil.extractPackageName("net.sf.okapi.common.ClassUtil"));
	}

	@Test
	public void testQualifyName() {

		assertEquals("", ClassUtil.qualifyName("", null));
		assertEquals("", ClassUtil.qualifyName("", ""));
		assertEquals("", ClassUtil.qualifyName("package", ""));
		assertEquals("", ClassUtil.qualifyName("", "class"));
		assertEquals("package.class", ClassUtil.qualifyName("package", "class"));
		assertEquals("package.class", ClassUtil.qualifyName("package.", "class"));
		assertEquals("package.class", ClassUtil.qualifyName("package_class", "package.class"));
		assertEquals(".class", ClassUtil.qualifyName("package.", ".class"));
		assertEquals("java.lang.Integer", ClassUtil.qualifyName(String.class, "Integer"));

		assertEquals("java.lang.Integer", ClassUtil.qualifyName(ClassUtil.extractPackageName(
				String.class.getName()), "Integer"));

		assertEquals("net.sf.okapi.common.UtilTest", ClassUtil.qualifyName(ClassUtil.extractPackageName(
				this.getClass().getName()), "UtilTest"));

		assertEquals("net.sf.okapi.common.UtilTest", ClassUtil.qualifyName(this, "UtilTest"));
	}

	// Class reference
	@Test(expected = InstantiationException.class)
	public void testInstantiateClass1() throws Exception {
		ClassUtil.instantiateClass(BOMAwareInputStream.class);
	}

	// Class reference, empty constructor parameters
	@Test(expected = InstantiationException.class)
	public void testInstantiateClass2() throws Exception {
		assertNull(ClassUtil.instantiateClass(BOMAwareInputStream.class, (Object[]) null));
	}

	// Class reference, correct constructor parameters
	@Test
	public void testInstantiateClass3() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNotNull(ClassUtil.instantiateClass(BOMAwareInputStream.class, input, DEFAULT_ENCODING));
		}
	}

	// Class reference, wrong parameter types given, no matching constructor
	@Test(expected = OkapiException.class)
	public void testInstantiateClass4() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNull(ClassUtil.instantiateClass(BOMAwareInputStream.class, input, 3));
		}
	}

	// Class name, null
	@Test(expected = IllegalArgumentException.class)
	public void testInstantiateClass5() throws Exception {
		assertNull(ClassUtil.instantiateClass((String) null));
	}

	// Class name, empty
	@Test(expected = IllegalArgumentException.class)
	public void testInstantiateClass6() throws Exception {
		assertNull(ClassUtil.instantiateClass(""));
	}

	// Class name, wrong name
	@Test(expected = ClassNotFoundException.class)
	public void testInstantiateClass7() throws Exception {
		assertNull(ClassUtil.instantiateClass("foo.bar"));
	}

	// Class name, correct name, but no empty constructor
	@Test(expected = InstantiationException.class)
	public void testInstantiateClass8() throws Exception {
		assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream"));
	}

	// Class name, correct name
	@Test
	public void testInstantiateClass9() throws Exception {
		assertNotNull(ClassUtil.instantiateClass("java.lang.String"));
	}

	// Class name, wrong loader
	@Test(expected = IllegalArgumentException.class)
	public void testInstantiateClass10() throws Exception {
		assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.ClassUtilTest2", String.class.getClassLoader()));
	}

	// Class name, wrong name
	@Test(expected = ClassNotFoundException.class)
	public void testInstantiateClass11() throws Exception {
		assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.ClassUtilTest2", ClassUtil.class.getClassLoader()));
	}

	// Class name, correct loader
	@Test
	public void testInstantiateClass12() throws Exception {
		assertNotNull(ClassUtil.instantiateClass("net.sf.okapi.common.ClassUtilTest", this.getClass().getClassLoader()));
	}

	// Class name, correct constructor parameters
	@Test
	public void testInstantiateClass13() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNotNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream", input, DEFAULT_ENCODING));
		}
	}

	// Class name, wrong parameter types given, no matching constructor
	@Test(expected = OkapiException.class)
	public void testInstantiateClass14() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream", input, 3));
		}
	}

	// Class name, correct name, correct loader, correct constructor parameters
	@Test
	public void testInstantiateClass15() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNotNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream",
					this.getClass().getClassLoader(), input, DEFAULT_ENCODING));
		}
	}

	// Class name, correct name, correct loader, incorrect constructor parameters
	@Test(expected = OkapiException.class)
	public void testInstantiateClass16() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream",
					this.getClass().getClassLoader(), input, 10));
		}
	}

	// Class name, correct name, incorrect loader, correct constructor parameters
	@Test(expected = IllegalArgumentException.class)
	public void testInstantiateClass17() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream",
					String.class.getClassLoader(), input, DEFAULT_ENCODING));
		}
	}

	// Class name, incorrect name, correct loader, correct constructor parameters
	@Test(expected = ClassNotFoundException.class)
	public void testInstantiateClass18() throws Exception {
		try (InputStream input = FileLocation.fromClass(getClass()).in("/test.txt").asInputStream()) {
			assertNull(ClassUtil.instantiateClass("net.sf.okapi.common.BOMAwareInputStream2",
					this.getClass().getClassLoader(), input, DEFAULT_ENCODING));
		}
	}
}
