/*===========================================================================
  Copyright (C) 2011 by the Okapi Framework contributors
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

package net.sf.okapi.steps.rainbowkit.po;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.filters.po.POWriter;
import net.sf.okapi.filters.po.Parameters;
import net.sf.okapi.filters.rainbowkit.Manifest;
import net.sf.okapi.filters.rainbowkit.MergingInfo;
import net.sf.okapi.steps.rainbowkit.common.BasePackageWriter;

public class POPackageWriter extends BasePackageWriter {

	private POWriter writer;
	private String rawDocPath;

	public POPackageWriter () {
		super(Manifest.EXTRACTIONTYPE_PO);
	}
	
	@Override
	protected void processStartBatch () {
		manifest.setSubDirectories("original", "work", "work", "done", null, null, true);
		setTMXInfo(true, null, true, true, false);
		super.processStartBatch();
	}
	
	@Override
	protected void processStartDocument (Event event) {
		super.processStartDocument(event);
		
		writer = new POWriter();
		Parameters wparams = writer.getParameters();
		wparams.setOutputGeneric(true);
		
		writer.setMode(true, false, true);
		writer.setOptions(manifest.getTargetLocale(), "UTF-8");
		
		MergingInfo item = manifest.getItem(docId);
		rawDocPath = manifest.getTempSourceDirectory() + item.getRelativeInputPath() + ".po";
		writer.setOutput(rawDocPath);

		writer.handleEvent(event);
	}
	
	@Override
	protected Event processEndDocument (Event event) {
		writer.handleEvent(event);
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
		
		if ( params.getSendOutput() ) {
			return super.creatRawDocumentEventSet(rawDocPath, "UTF-8", manifest.getSourceLocale(), manifest.getTargetLocale());
		}
		else {
			return event;
		}
	}

	@Override
	protected void processStartSubDocument (Event event) {
		writer.handleEvent(event);
	}
	
	@Override
	protected void processEndSubDocument (Event event) {
		writer.handleEvent(event);
	}
	
	@Override
	protected void processTextUnit (Event event) {
		// Skip non-translatable
		ITextUnit tu = event.getTextUnit();
		if ( !tu.isTranslatable() ) return;
		
		writer.handleEvent(event);
		writeTMXEntries(event.getTextUnit());
	}

	@Override
	public void close () {
		if ( writer != null ) {
			writer.close();
			writer = null;
		}
	}

	@Override
	public String getName () {
		return getClass().getName();
	}

}
