/*===========================================================================
  Copyright (C) 2011-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.walker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.document.FileNode;
import net.sf.okapi.lib.xliff2.document.UnitNode;
import net.sf.okapi.lib.xliff2.document.XLIFFDocument;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.walker.selector.XliffWalkerPathSelector;
import net.sf.okapi.lib.xliff2.walker.strategy.IXliffWalkerStrategy;
import net.sf.okapi.lib.xliff2.walker.strategy.XliffWalkerStrategyFactory;
import net.sf.okapi.lib.xliff2.walker.test.TestConstants;
import net.sf.okapi.lib.xliff2.walker.test.TestFileVisitor;
import net.sf.okapi.lib.xliff2.walker.test.TestModifyingSegmentVisitor;
import net.sf.okapi.lib.xliff2.walker.test.TestSegmentVisitor;
import net.sf.okapi.lib.xliff2.walker.test.TestUnitVisitor;

@RunWith(JUnit4.class)
public class XliffWalkerTest {

	private XLIFFDocument doc;

	private XliffWalker testWalker;
	
	private FileLocation root = FileLocation.fromClass(this.getClass());
		
	@Before
	public void setup() {
		File xlfFile = root.in("/valid/everything-core-walker.xlf").asFile();
		
		this.doc = new XLIFFDocument();
		this.doc.load(xlfFile, XLIFFReader.VALIDATION_MAXIMAL);
		
		assertNotNull(this.doc);
		
		this.testWalker = new XliffWalker();
	}

	@Test
	public void testFlexibleStrategy() {

		IXliffWalkerStrategy walkerStrategy = XliffWalkerStrategyFactory.flexibleStrategy(new XliffWalkerPathSelector.Builder()
				.selector("f1", "tu1")
				.selector("f1", "tu3", 0)
				.build());

		this.testWalker = new XliffWalker(walkerStrategy);

		TestFileVisitor fileVisitor = new TestFileVisitor();
		this.testWalker.addFileVisitor(fileVisitor);

		TestUnitVisitor unitVisitor = new TestUnitVisitor();
		this.testWalker.addUnitVisitor(unitVisitor);

		TestSegmentVisitor segmentVisitor = new TestSegmentVisitor();
		this.testWalker.addSegmentVisitor(segmentVisitor);

		this.testWalker.doWalk(this.doc);

		assertEquals(1, fileVisitor.getCount());
		assertEquals(2, unitVisitor.getCount());
		assertEquals(3, segmentVisitor.getCount());

		TestModifyingSegmentVisitor segmentVisitor2 = new TestModifyingSegmentVisitor();
		this.testWalker.removeVisitors();
		this.testWalker.addSegmentVisitor(segmentVisitor2);
		this.testWalker.doWalk(doc);

		for (Unit unit : doc.getUnits()) {
			int segmentIndex = 0;
			for(Segment segment : unit.getSegments()) {
				boolean wasSegmentWalked = unit.getId().equals("tu1") || (unit.getId().equals("tu3") && segmentIndex == 0);

				assertEquals(wasSegmentWalked, segment.getSource().isEmpty());
				assertEquals(wasSegmentWalked, segment.getTarget().isEmpty());
				assertEquals(wasSegmentWalked, TestConstants.SEGMENT_TEST_MARKER.equals(segment.getSubState()));

				segmentIndex++;
			}
		}
	}

	@Test
	public void testDoWalk() {
		// Simple count test
		TestFileVisitor fileVisitor = new TestFileVisitor();
		this.testWalker.addFileVisitor(fileVisitor);
		
		TestUnitVisitor unitVisitor = new TestUnitVisitor();
		this.testWalker.addUnitVisitor(unitVisitor);
		
		TestSegmentVisitor segmentVisitor = new TestSegmentVisitor();
		this.testWalker.addSegmentVisitor(segmentVisitor);
		
		this.testWalker.doWalk(this.doc);
		
		assertEquals(1, fileVisitor.getCount());
		assertEquals(4, unitVisitor.getCount());
		assertEquals(5, segmentVisitor.getCount());
		
		// Segment manipulation test
		for(Unit unit : this.doc.getUnits()) {
			for(Segment segment : unit.getSegments()) {
				assertFalse(segment.getSource().isEmpty());
				assertFalse(segment.getTarget().isEmpty());
				assertNull(segment.getSubState());
			}
		}
		
		TestModifyingSegmentVisitor segmentVisitor2 = new TestModifyingSegmentVisitor();
		this.testWalker.removeVisitors();
		this.testWalker.addSegmentVisitor(segmentVisitor2);
		this.testWalker.doWalk(this.doc);
		
		for(Unit unit : this.doc.getUnits()) {
			for(Segment segment : unit.getSegments()) {
				assertTrue(segment.getSource().isEmpty());
				assertTrue(segment.getTarget().isEmpty());
				assertEquals(TestConstants.SEGMENT_TEST_MARKER, segment.getSubState());
			}
		}
	}

	@Test
	public void testSetVisitors() {
		List<IXliffVisitor<FileNode>> fileVisitors = new ArrayList<>();
		fileVisitors.add(new TestFileVisitor());
		
		List<IXliffVisitor<UnitNode>> unitVisitors = new ArrayList<>();
		unitVisitors.add(new TestUnitVisitor());
		unitVisitors.add(new TestUnitVisitor());
		
		this.testWalker.setVisitors(fileVisitors, unitVisitors, null /* segmentVisitors */);
		assertEquals(1, this.testWalker.getAllFileNodeVisitors().size());
		assertEquals(2, this.testWalker.getAllUnitNodeVisitors().size());
		
		this.testWalker.removeVisitors();
		assertEquals(0, this.testWalker.getAllFileNodeVisitors().size());
		assertEquals(0, this.testWalker.getAllUnitNodeVisitors().size());
	}

	@Test
	public void testAddFileVisitor() {
		IXliffVisitor<FileNode> fileVisitor = new TestFileVisitor();
		String ID = this.testWalker.addFileVisitor(fileVisitor);
		
		IXliffVisitor<FileNode> fileVisitor2 = new TestFileVisitor();
		this.testWalker.addFileVisitor(fileVisitor2);
		
		assertEquals(2, this.testWalker.getAllFileNodeVisitors().size());
		this.testWalker.removeFileVisitor(ID);
		assertEquals(1, this.testWalker.getAllFileNodeVisitors().size());
	}

	@Test
	public void testAddUnitVisitor() {
		IXliffVisitor<UnitNode> unitVisitor = new TestUnitVisitor();
		String ID = this.testWalker.addUnitVisitor(unitVisitor);
		
		IXliffVisitor<UnitNode> unitVisitor2 = new TestUnitVisitor();
		this.testWalker.addUnitVisitor(unitVisitor2);
		
		assertEquals(2, this.testWalker.getAllUnitNodeVisitors().size());
		this.testWalker.removeUnitVisitor(ID);
		assertEquals(1, this.testWalker.getAllUnitNodeVisitors().size());
	}

	@Test
	public void testAddSegmentVisitor() {
		IXliffVisitor<Segment> segmentVisitor = new TestSegmentVisitor();
		String ID = this.testWalker.addSegmentVisitor(segmentVisitor);
		
		IXliffVisitor<Segment> segmentVisitor2 = new TestSegmentVisitor();
		 this.testWalker.addSegmentVisitor(segmentVisitor2);
		
		assertEquals(2, this.testWalker.getAllSegmentVisitors().size());
		this.testWalker.removeSegmentVisitor(ID);
		assertEquals(1, this.testWalker.getAllSegmentVisitors().size());
	}
	
	@Test
	public void testIncorrectParams() {
		boolean exceptionThrown = false; 
		
		try { 
			this.testWalker.addFileVisitor(null);
		} catch (IllegalArgumentException ex) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
		
		exceptionThrown = false; 
		try { 
			this.testWalker.addUnitVisitor(null);
		} catch (IllegalArgumentException ex) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
		
		exceptionThrown = false; 
		try { 
			this.testWalker.addSegmentVisitor(null);
		} catch (IllegalArgumentException ex) {
			exceptionThrown = true;
		}
		
		assertTrue(exceptionThrown);
	}

	@Test
	public void testPipelineStrategy() {

		this.testWalker = new XliffWalker(XliffWalkerStrategyFactory.pipelineStrategy());

		this.testWalker.addFileVisitor((visitee, context) -> visitee.add(new UnitNode(new Unit("added-unit-1"))));

		this.testWalker.addUnitVisitor((visitee, context) -> {
			Segment segment = visitee.get().appendSegment();
			segment.setId("added-segment-1");
			segment.setSource("Source text.");
		}, new XliffWalkerPathSelector.Builder()
				.selector("f1", "added-unit-1")
				.build());

		this.testWalker.addSegmentVisitor((visitee, context) ->
				visitee.setTarget(visitee.getSource().getPlainText() + "Now translated."));

		this.testWalker.addSegmentVisitor((visitee, context) ->
				visitee.setTarget(visitee.getTarget().getPlainText() + "Now translated again."));

		this.testWalker.addUnitVisitor((visitee, context) -> {
			Segment segment = visitee.get().getSegment(0);
			segment.setSource(segment.getSource().getPlainText() + "Modified source text.");
			segment.setTarget(segment.getTarget().getPlainText() + "Third translation done.");
		});

		this.testWalker.doWalk(this.doc);

		Unit unit = doc.getUnitNode("f1","added-unit-1").get();
		Segment segment = unit.getSegment(0);

		assertEquals("Source text.Modified source text.", segment.getSource().getPlainText());
		assertEquals("Source text.Now translated.Now translated again.Third translation done.", segment.getTarget().getPlainText());
	}

	@Test
	public void testVisitPlaceAwareVisitor() {

		this.testWalker = new XliffWalker(XliffWalkerStrategyFactory.pipelineStrategy());

		this.testWalker.addFileVisitor((visitee, context) ->
						visitee.add(new UnitNode(new Unit("added-unit-1"))),
				new XliffWalkerPathSelector.Builder().selector("f1").build());

		this.testWalker.addUnitVisitor((visitee, context) -> {
			Segment segment = visitee.get().appendSegment();
			segment.setId("added-segment-1");
			segment.setSource("Source text.");
		}, new XliffWalkerPathSelector.Builder().selector("f1", "added-unit-1").build());

		this.testWalker.addSegmentVisitor((visitee, context) ->
				visitee.setTarget(visitee.getSource().getPlainText() + "Now translated."),
				new XliffWalkerPathSelector.Builder()
						.selector("f1", "added-unit-1", 0)
						.build());

		this.testWalker.addSegmentVisitor((visitee, context) ->
				visitee.setTarget(visitee.getTarget().getPlainText() + "Now translated again."),
				new XliffWalkerPathSelector.Builder()
						.selector("f1", "added-unit-1", 0)
						.build());

		this.testWalker.addUnitVisitor((visitee, context) -> {
			Segment segment = visitee.get().getSegment(0);
			segment.setSource(segment.getSource().getPlainText() + "Modified source text.");
			segment.setTarget(segment.getTarget().getPlainText() + "Third translation done.");
		}, new XliffWalkerPathSelector.Builder()
				.selector("f1", "added-unit-1")
				.build());

		this.testWalker.doWalk(this.doc);

		Unit unit = doc.getUnitNode("f1","added-unit-1").get();
		Segment segment = unit.getSegment(0);

		assertEquals("Source text.Modified source text.", segment.getSource().getPlainText());
		assertEquals("Source text.Now translated.Now translated again.Third translation done.", segment.getTarget().getPlainText());

		// Check that all other segments were not changed
		Unit tu1 = doc.getUnitNode("f1","tu1").get();
		assertEquals("Sample segment.", tu1.getSegment(0).getSource().getPlainText());
		assertEquals("Exemple de segment.", tu1.getSegment(0).getTarget().getPlainText());

		assertEquals("Segment's content.", tu1.getSegment(1).getSource().getPlainText());
		assertEquals("Contenu du segment.", tu1.getSegment(1).getTarget().getPlainText());

		Unit tu3 = doc.getUnitNode("f1","tu3").get();
		assertEquals("Bolded text", tu3.getSegment(0).getSource().getPlainText());
		assertEquals("Bolded text", tu3.getSegment(0).getTarget().getPlainText());

		Unit tu3end = doc.getUnitNode("f1","tu3end").get();
		assertEquals("Extra stuff", tu3end.getSegment(0).getSource().getPlainText());
		assertEquals("Extra stuff", tu3end.getSegment(0).getTarget().getPlainText());

		Unit tu2 = doc.getUnitNode("f1","tu2").get();
		assertEquals("special text and more\n.", tu2.getSegment(0).getSource().getPlainText());
		assertEquals("special text and more\n.", tu2.getSegment(0).getTarget().getPlainText());
	}

	@Test
	public void testDoWalkPipelineStrategy() {

		this.testWalker = new XliffWalker(XliffWalkerStrategyFactory.pipelineStrategy());

		// Simple count test
		TestFileVisitor fileVisitor = new TestFileVisitor();
		this.testWalker.addFileVisitor(fileVisitor);

		TestUnitVisitor unitVisitor = new TestUnitVisitor();
		this.testWalker.addUnitVisitor(unitVisitor);

		TestSegmentVisitor segmentVisitor = new TestSegmentVisitor();
		this.testWalker.addSegmentVisitor(segmentVisitor);

		// adding segment to each unit, i.e. 4 more segments in document
		this.testWalker.addUnitVisitor((visitee, context) -> visitee.get().appendSegment());

		// counting again
		this.testWalker.addSegmentVisitor(segmentVisitor);

		this.testWalker.doWalk(this.doc);

		assertEquals(1, fileVisitor.getCount());
		assertEquals(4, unitVisitor.getCount());
		assertEquals(14, segmentVisitor.getCount());
	}
}
