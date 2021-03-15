package net.sf.okapi.steps.xliffsplitter;

import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.pipeline.Pipeline;
import net.sf.okapi.common.resource.RawDocument;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class XliffSplitterTest {
	private Pipeline pipeline;
	private FileLocation xlfRoot;
	private String[] xlfFileList;
	private XliffSplitterStep splitter;
	
	@Before
	public void setUp() throws Exception {
		xlfRoot = FileLocation.fromClass(getClass()); 
		xlfFileList = Util.getFilteredFiles(xlfRoot.in("/").toString(), ".xlf");
		
		// create pipeline
		pipeline = new Pipeline();
		
		// add filter step
		splitter = new XliffSplitterStep();		
		pipeline.addStep(splitter);				
	}
	
	@After
	public void tearDown() throws Exception {
		pipeline.destroy();
	}
	
	@Test
	public void splitXliffWithOneFile() {
			pipeline.startBatch();		
			String file = "tasks_Test_SDL_XLIFF_18961_es_ES_xliff_singleFile.xlf";
			splitter.setOutputURI(xlfRoot.out(file).asUri());
			pipeline.process(new RawDocument(xlfRoot.in(file).asUri(), "UTF-8", LocaleId.ENGLISH));
			pipeline.endBatch();
	}
	
	@Test
	public void splitXliffWithMultipleFiles() {
			pipeline.startBatch();		
			String file = "tasks_Test_SDL_XLIFF_18961_es_ES_xliff.xlf";
			splitter.setOutputURI(xlfRoot.out(file).asUri());
			pipeline.process(new RawDocument(xlfRoot.in(file).asUri(), "UTF-8", LocaleId.ENGLISH));
			pipeline.endBatch();
	}
	
	@Test
	public void splitXliffWithMultipleInputFiles() {
			pipeline.startBatch();		
			for (String file : xlfFileList) {
				splitter.setOutputURI(xlfRoot.out(file).asUri());
				pipeline.process(new RawDocument(xlfRoot.in(file).asUri(), "UTF-8", LocaleId.ENGLISH));
			}			
			pipeline.endBatch();
	}
	
	@Test
	public void splitBigXliffWithOneFile() {
		XliffSplitterParameters params = new XliffSplitterParameters();
		params.setBigFile(true);
		splitter.setParameters(params);
		pipeline.startBatch();		
		String file = "tasks_Test_SDL_XLIFF_18961_es_ES_xliff_singleFile.xlf";
		splitter.setOutputURI(xlfRoot.out(file).asUri());
		pipeline.process(new RawDocument(xlfRoot.in(file).asUri(), "UTF-8", LocaleId.ENGLISH));
		pipeline.endBatch();
	}
	
	@Test
	public void splitBigXliffWithMultipleFiles() {
		XliffSplitterParameters params = new XliffSplitterParameters();
		params.setBigFile(true);
		splitter.setParameters(params);
		pipeline.startBatch();		
		String file = "tasks_Test_SDL_XLIFF_18961_es_ES_xliff.xlf";
		splitter.setOutputURI(xlfRoot.out(file).asUri());
		pipeline.process(new RawDocument(xlfRoot.in(file).asUri(), "UTF-8", LocaleId.ENGLISH));
		pipeline.endBatch();
	}
	
	@Test
	public void splitBigXliffWithMultipleInputFiles() {
		XliffSplitterParameters params = new XliffSplitterParameters();
		params.setBigFile(true);
		splitter.setParameters(params);
		pipeline.startBatch();		
		
		for (String file : xlfFileList) {
			splitter.setOutputURI(xlfRoot.out(file).asUri());
			pipeline.process(new RawDocument(xlfRoot.in(file).asUri(), "UTF-8", LocaleId.ENGLISH));
		}			
		pipeline.endBatch();
	}
	
}
