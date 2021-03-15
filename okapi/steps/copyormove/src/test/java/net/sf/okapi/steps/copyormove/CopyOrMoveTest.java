/*===========================================================================
  Copyright (C) 2010-2013 by the Okapi Framework contributors
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

package net.sf.okapi.steps.copyormove;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.FileCompare;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.StreamUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipelinedriver.BatchItemContext;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CopyOrMoveTest {

	private FileLocation root;
	private String ROOT_PREFIX = "/test_folder";
	private CopyOrMoveStep step;
	private IPipelineDriver pdriver;
	private Parameters params;
	
	public CopyOrMoveTest() {
	}

	@Before
	public void setUp() {
		root = FileLocation.fromClass(this.getClass());
		step = new CopyOrMoveStep();
		resetFiles();
		Util.deleteDirectory(root.in(ROOT_PREFIX + "/to_empty/").toString(), true);
		params = step.getParameters();
		pdriver = new PipelineDriver();
		String rootFolder = root.in(ROOT_PREFIX).toString();
		pdriver.setRootDirectories(rootFolder, rootFolder);
		pdriver.addStep(step);
	}

	@Test
	public void testBasicCopy() {
		params.setCopyOption("overwrite");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", true);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test01.txt", "/to_empty/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test02.txt", "/to_empty/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test03.txt", "/to_empty/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test04.txt", "/to_empty/test04.txt"));
	}
	
	@Test
	public void testBasicMove() {
		params.setCopyOption("overwrite");
		params.setMove(true);
		addFiles(pdriver, "/to_empty", false);
		pdriver.processBatch();
		
		resetFiles();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test01.txt", "/to_empty/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test02.txt", "/to_empty/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test03.txt", "/to_empty/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test04.txt", "/to_empty/test04.txt"));
	}

	@Test
	public void testStructuredCopy() {
		params.setCopyOption("overwrite");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", false);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/subdir11/test01.txt", "/to_empty/subdir01/subdir11/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/test02.txt", "/to_empty/subdir01/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test03.txt", "/to_empty/subdir02/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test04.txt", "/to_empty/subdir02/test04.txt"));
	}

	@Test
	public void testStructuredMove() {
		params.setCopyOption("overwrite");
		params.setMove(true);
		addFiles(pdriver, "/to_empty", false);
		pdriver.processBatch();
			
		resetFiles();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/subdir11/test01.txt", "/to_empty/subdir01/subdir11/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/test02.txt", "/to_empty/subdir01/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test03.txt", "/to_empty/subdir02/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test04.txt", "/to_empty/subdir02/test04.txt"));
	}

	@Test
	public void testOverwrite() {
		params.setCopyOption("overwrite");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", true);
		resetTargetFiles(true);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test01.txt", "/to_empty/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test02.txt", "/to_empty/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test03.txt", "/to_empty/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test04.txt", "/to_empty/test04.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test05.txt", "/to_empty/test05.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test06.txt", "/to_empty/test06.txt"));
	}
	
	@Test
	public void testBackup() {
		params.setCopyOption("backup");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", true);
		resetTargetFiles(true);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test01.txt", "/to_empty/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test02.txt", "/to_empty/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test03.txt", "/to_empty/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_flat/test04.txt", "/to_empty/test04.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test05.txt", "/to_empty/test05.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test06.txt", "/to_empty/test06.txt"));
	}
	
	@Test
	public void testSkip() {
		params.setCopyOption("skip");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", true);
		resetTargetFiles(true);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test00.txt", "/to_empty/test00.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test01.txt", "/to_empty/test01.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test02.txt", "/to_empty/test02.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test03.txt", "/to_empty/test03.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test04.txt", "/to_empty/test04.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test05.txt", "/to_empty/test05.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_flat/test06.txt", "/to_empty/test06.txt"));
	}

	@Test
	public void testStructuredOverwrite() {
		params.setCopyOption("overwrite");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", false);
		resetTargetFiles(false);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/subdir11/test01.txt", "/to_empty/subdir01/subdir11/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/test02.txt", "/to_empty/subdir01/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test03.txt", "/to_empty/subdir02/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test04.txt", "/to_empty/subdir02/test04.txt"));
		//FIXME missing file: assertFalse(filesAreIdenticalPerLines("/gold_complex/subdir01/subdir11/test05.txt", "/to_empty/subdir01/subdir11/test05.txt"));
		//FIXME missing file: assertFalse(filesAreIdenticalPerLines("/gold_complex/test06.txt", "/to_empty/test06.txt"));
	}
	
	@Test
	public void testStructuredBackup() {
		params.setCopyOption("backup");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", false);
		resetTargetFiles(false);
		pdriver.processBatch();

		FileCompare fc = new FileCompare();
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/test00.txt", "/to_empty/test00.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/subdir11/test01.txt", "/to_empty/subdir01/subdir11/test01.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/test02.txt", "/to_empty/subdir01/test02.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test03.txt", "/to_empty/subdir02/test03.txt"));
		assertTrue(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test04.txt", "/to_empty/subdir02/test04.txt"));
		//FIXME missing file: assertFalse(filesAreIdenticalPerLines("/gold_complex/subdir01/subdir11/test05.txt", "/to_empty/subdir01/subdir11/test05.txt"));
		//FIXME missing file: assertFalse(filesAreIdenticalPerLines("/gold_complex/test06.txt", "/to_empty/test06.txt"));
	}
	
	boolean filesAreIdenticalPerLines(FileCompare fc, String gold, String actual) {
		return fc.compareFilesPerLines(
				root.out(ROOT_PREFIX + actual).toString(),
				root.in(ROOT_PREFIX + gold).toString(),
				"UTF-8");
	}
	
	@Test
	public void testStructuredSkip() {
		params.setCopyOption("skip");
		params.setMove(false);
		addFiles(pdriver, "/to_empty", false);
		resetTargetFiles(false);
		pdriver.processBatch();
		
		FileCompare fc = new FileCompare();
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_complex/test00.txt", "/to_empty/test00.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/subdir11/test01.txt", "/to_empty/subdir01/subdir11/test01.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_complex/subdir01/test02.txt", "/to_empty/subdir01/test02.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test03.txt", "/to_empty/subdir02/test03.txt"));
		assertFalse(filesAreIdenticalPerLines(fc, "/gold_complex/subdir02/test04.txt", "/to_empty/subdir02/test04.txt"));
		//FIXME missing file: assertFalse(fc.compareFilesPerLines("/gold_complex/subdir01/subdir11/test05.txt", "/to_empty/subdir01/subdir11/test05.txt"));
		//FIXME missing file: assertFalse(fc.compareFilesPerLines("/gold_complex/test06.txt", "/to_empty/test06.txt"));
	}

	
	//
	// Helper methods
	//
	private void addFiles(IPipelineDriver pdriver, String outputPrefix, boolean isFlat) {
		if (isFlat) {
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_flat/test00.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/test00.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_flat/test01.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/test01.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_flat/test02.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/test02.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_flat/test03.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/test03.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_flat/test04.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/test04.txt").asUri(), null));
		} else {
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_complex/test00.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/test00.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_complex/subdir01/subdir11/test01.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/subdir01/subdir11/test01.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_complex/subdir01/test02.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/subdir01/test02.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_complex/subdir02/test03.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/subdir02/test03.txt").asUri(), null));
			pdriver.addBatchItem(new BatchItemContext(new RawDocument(root.out(ROOT_PREFIX + "/from_complex/subdir02/test04.txt").asUri(),
					"UTF-8", LocaleId.ENGLISH), root.out(ROOT_PREFIX + outputPrefix + "/subdir02/test04.txt").asUri(), null));
		}
	}
	
	private void resetFiles() {
		Util.deleteDirectory(root.out(ROOT_PREFIX + "/from_flat/").toString(), true);
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_flat/test00.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_flat/test00.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_flat/test01.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_flat/test01.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_flat/test02.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_flat/test02.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_flat/test03.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_flat/test03.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_flat/test04.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_flat/test04.txt").asOutputStream());
		
		Util.deleteDirectory(root.out(ROOT_PREFIX + "/from_complex/").toString(), true);
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_complex/test00.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_complex/test00.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_complex/subdir01/subdir11/test01.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_complex/subdir01/subdir11/test01.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_complex/subdir01/test02.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_complex/subdir01/test02.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_complex/subdir02/test03.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_complex/subdir02/test03.txt").asOutputStream());
		StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_complex/subdir02/test04.txt").asInputStream(),
				root.out(ROOT_PREFIX + "/from_complex/subdir02/test04.txt").asOutputStream());
	}
	
	private void resetTargetFiles(boolean isFlat) {
		if (isFlat) {
			Util.deleteDirectory(root + "to_empty/", true);
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test00.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test00.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test01.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test01.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test02.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test02.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test03.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test03.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test04.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test04.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test05.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test05.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_flat/test06.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test06.txt").asOutputStream());
		} else {
			Util.deleteDirectory(root + "to_empty/", true);
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/test00.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test00.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/subdir01/subdir11/test01.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/subdir01/subdir11/test01.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/subdir01/test02.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/subdir01/test02.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/subdir02/test03.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/subdir02/test03.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/subdir02/test04.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/subdir02/test04.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/subdir02/test04.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/subdir02/test04.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/subdir01/subdir11/test05.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/subdir01/subdir11/test05.txt").asOutputStream());
			StreamUtil.copy(root.in(ROOT_PREFIX + "/gold_for_options_complex/test06.txt").asInputStream(),
					root.out(ROOT_PREFIX + "/to_empty/test06.txt").asOutputStream());
		}
	}
}
