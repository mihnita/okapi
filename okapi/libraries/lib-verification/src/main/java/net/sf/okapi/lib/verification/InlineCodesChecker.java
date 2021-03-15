/*===========================================================================
 Copyright (C) 2016 by the Okapi Framework contributors
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

package net.sf.okapi.lib.verification;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.IssueType;
import net.sf.okapi.common.annotation.SkipCheckAnnotation;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ISegments;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartDocument;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment.TagType;

public class InlineCodesChecker extends AbstractChecker {

	/**
	 * Internal structure to store simple temporary code for verification
	 */
	static private class SimpleCode {

		SimpleCode (Code code) {
			this.type = code.getTagType();
			this.id = code.getId();
			this.isDeleteable = code.isDeleteable();
		}
		
		TagType type;
		int id;
		boolean isDeleteable;
	}

	@Override
	public void startProcess(LocaleId sourceLocale, LocaleId targetLocale, Parameters params, List<Issue> issues) {
		super.startProcess(sourceLocale, targetLocale, params, issues);
	}

	@Override
	public void processStartDocument(StartDocument sd, List<String> sigList) {
		super.processStartDocument(sd, sigList);
	}

	@Override
	public void processStartSubDocument(StartSubDocument ssd) {
		super.processStartSubDocument(ssd);
	}

	@Override
	public void processTextUnit(ITextUnit tu) {
		// Skip non-translatable entries
		if (!tu.isTranslatable()) {
			return;
		}

		// Get the containers
		TextContainer srcCont = tu.getSource();
		TextContainer trgCont = tu.getTarget(getTrgLoc());

		// Check if we have a target
		if (trgCont == null) {
			return;
		}

		ISegments srcSegs = srcCont.getSegments();
		ISegments trgSegs = trgCont.getSegments();
		for (Segment srcSeg : srcSegs) {
			if ( srcSeg.getAnnotation(SkipCheckAnnotation.class) != null ) {
				continue;
			}
			Segment trgSeg = trgSegs.get(srcSeg.getId());
			if (trgSeg == null) {
				// Cannot go further for that segment
				continue;
			}
			// Check code differences, if requested
			if (getParams().getCodeDifference()) {
				checkInlineCodes(srcSeg, trgSeg, tu, trgCont);
			}
		}

		setAnnotationIds(srcCont, trgCont);
	}

	// Create a copy of the codes and strip out any that has empty data.
	// They correspond to process-only codes like <df> in TTX or <mrk> in XLIFF
	private ArrayList<Code> stripNoiseCodes(Segment seg) {
		ArrayList<Code> list = new ArrayList<>(seg.text.getCodes());
		Iterator<Code> iter = list.iterator();
		while (iter.hasNext()) {
			Code code = iter.next();
			if (getParams().getTypesToIgnore().contains(code.getType() + ";") && !code.getType().isEmpty()) {
				iter.remove();
			}
		}
		return list;
	}

	private String buildCodeList(List<Code> list) {
		StringBuilder tmp = new StringBuilder();
		for (Code code : list) {
			if (tmp.length() > 0) {
				tmp.append(", ");
			}
			if ( getParams().getUseGenericCodes() ) {
				switch (code.getTagType()) {
				case PLACEHOLDER:
					tmp.append("<"+code.getId()+"/>");
					break;
				case OPENING:
					tmp.append("<"+code.getId()+">");
					break;
				case CLOSING:
					tmp.append("</"+code.getId()+">");
					break;
				}
			}
			else {
				if (code.getData().isEmpty()) {
					tmp.append(code.getOuterData().replaceAll("></x>", "/>"));
				} else { // Show the content
					tmp.append("\"" + code.getData() + "\"");
				}
			}
		}
		return tmp.toString();
	}

	private String buildOpenCloseSequence(ArrayList<Code> list) {
		StringBuilder sb = new StringBuilder();
		for (Code code : list) {
			switch (code.getTagType()) {
			case OPENING:
				sb.append("o");
				break;
			case CLOSING:
				sb.append("c");
				break;
			case PLACEHOLDER:
				if (true) {
					String tmp = code.getData();
					char ch = 'p';
					if (!Util.isEmpty(tmp) && getParams().getGuessOpenClose()) {
						if (tmp.startsWith("</")) {
							ch = 'c';
						} else if (tmp.startsWith("<")) {
							ch = 'o';
						}
						// Make sure the open is not an empty
						if (tmp.endsWith("/>")) {
							ch = 'p';
						}
					}
					// Now add only if it's an open or close
					sb.append(ch);
				}
			}
		}
		return sb.toString();
	}

	private List<SimpleCode> buildSimpleCodeslist (ArrayList<Code> list) {
		ArrayList<SimpleCode> simpleCodesList = new ArrayList<>(list.size());
		list.forEach( code -> {
			SimpleCode sc = new SimpleCode(code);
			switch (code.getTagType()) {
			case OPENING:
			case CLOSING:
				// Copy done already
				break;
			case PLACEHOLDER:
				// Adjust type based on the data (for isolated codes)
				String tmp = code.getData();
				TagType tt = TagType.PLACEHOLDER;
				if (!Util.isEmpty(tmp) && getParams().getGuessOpenClose()) {
					if (tmp.startsWith("</")) {
						tt = TagType.CLOSING;
					} else if (tmp.startsWith("<")) {
						tt = TagType.OPENING;
					}
					// Make sure the open is not an empty
					if (tmp.endsWith("/>")) {
						tt = TagType.PLACEHOLDER;
					}
				}
				// Now add only if it's an open or close
				sc.type = tt;
			}
			simpleCodesList.add(sc);
		});

		return simpleCodesList;
	}
	
	private void checkInlineCodes(Segment srcSeg, Segment trgSeg, ITextUnit tu, TextContainer trgCont) {
		ArrayList<Code> srcList = stripNoiseCodes(srcSeg);
		ArrayList<Code> trgList = stripNoiseCodes(trgSeg);

		// If no codes: don't check
		if ((srcList.size() == 0) && (trgList.size() == 0)) {
			return;
		}

		// Prepare the verification of the open-close sequence
		String srcOC = null;
		String trgOC = null;
		List<SimpleCode> srcSimpleCodes = null;
		List<SimpleCode> trgSimpleCodes = null;

		if ( getParams().getStrictCodeOrder() ) {
			srcSimpleCodes = buildSimpleCodeslist(srcList);
			trgSimpleCodes = buildSimpleCodeslist(trgList);
		}
		else { // Open/Close only
			srcOC = buildOpenCloseSequence(srcList);
			trgOC = buildOpenCloseSequence(trgList);
		}
		boolean checkOC = true;
		
		// Check codes missing in target
		Iterator<Code> srcIter = srcList.iterator();
		while (srcIter.hasNext()) {
			Code srcCode = srcIter.next();
			Iterator<Code> trgIter = trgList.iterator();
			while (trgIter.hasNext()) {
				Code trgCode = trgIter.next();
				// We check first the ID and type, then compare the original data if it is possible
				if ((trgCode.getId() == srcCode.getId()) && trgCode.getType().equals(srcCode.getType())) {
					// Found code with same ID and type
					// Check if both data are empty, if so, we can remove
					boolean okToRemove = (trgCode.getData().isEmpty() && srcCode.getData().isEmpty());
					// If one or both are not empty we compare the data
					if ( !okToRemove ) {
						if ( trgCode.getData().equals(srcCode.getData()) ) {
							okToRemove = true;
						}
						else { // Is it a case of start/end tags swapped?
							// At this point the two codes have same type, same ID but different data.
							// They could be swapped start/end tags then we want to report them not as missing/extra but suspect sequence detected later
							if (( trgCode.getTagType()==TagType.CLOSING && srcCode.getTagType()==TagType.OPENING )
								|| ( trgCode.getTagType()==TagType.OPENING && srcCode.getTagType()==TagType.CLOSING )) {
								okToRemove = true;
							}
						}
					}
					// Now remove or not
					if ( okToRemove ) {
						trgIter.remove();
						srcIter.remove();
					}
					break;
				}
			}
		}

		// --- Missing codes
		// Check if any of the missing code is one of the code allowed to be missing
		if (!srcList.isEmpty()) {
			Iterator<Code> iter = srcList.iterator();
			while (iter.hasNext()) {
				Code trgCode = iter.next();
				if (getParams().missingCodesAllowed.contains(trgCode.getData())) {
					iter.remove();
				}
				else if ( trgCode.isDeleteable() ) {
					iter.remove();
				}
			}
		}
		// What is left in the source list are the codes missing in the target
		if (!srcList.isEmpty()) {
			addAnnotationAndReportIssue(IssueType.MISSING_CODE, tu, trgCont, srcSeg.getId(),
				"Missing placeholders in the target: " + buildCodeList(srcList), 0, -1, 0, -1, Issue.SEVERITY_MEDIUM,
				getDisplay(srcSeg.getContent()),
				getDisplay(trgSeg.getContent()),
				srcList);
			checkOC = false;
		}

		// --- Extra codes
		// Check if any of the extra code is one of the code allowed to be extra
		if (!trgList.isEmpty()) {
			Iterator<Code> iter = trgList.iterator();
			while (iter.hasNext()) {
				Code srcCode = iter.next();
				if (getParams().extraCodesAllowed.contains(srcCode.getData())) {
					iter.remove();
				}
				else if ( srcCode.isDeleteable() ) {
					iter.remove();
				}
			}
		}
		// What is left in the target list are the extra codes in the target
		if (!trgList.isEmpty()) {
			addAnnotationAndReportIssue(IssueType.EXTRA_CODE, tu, trgCont, srcSeg.getId(),
				"Extra placeholders in the target: " + buildCodeList(trgList), 0, -1, 0, -1, Issue.SEVERITY_MEDIUM,
				getDisplay(srcSeg.getContent()),
				getDisplay(trgSeg.getContent()),
				trgList);
			checkOC = false;
		}

		// If requested, check sequence issue in open-close codes
		// This is checked only if we did not found already an error
		if ( checkOC && !getParams().getStrictCodeOrder() ) {
			int j = 0;
			boolean done = false;
			for (int i = 0; i < srcOC.length(); i++) {
				if (srcOC.charAt(i) == 'p') {
					continue;
				}
				// Else it's 'o' or 'c'
				while (true) {
					if (trgOC.length() <= j) {
						// No more code of this type
						addAnnotationAndReportIssue(IssueType.SUSPECT_CODE, tu, trgCont, srcSeg.getId(),
								"Suspect sequence of opening and closing target placeholders.", 0, -1, 0, -1,
								Issue.SEVERITY_MEDIUM,
								getDisplay(srcSeg.getContent()),
								getDisplay(trgSeg.getContent()),
								trgList);
						done = true;
						break;
					}
					// If it's a placeholder, move to the next code
					if (trgOC.charAt(j) == 'p') {
						j++;
						continue;
					}
					// Else: it's a 'o' or 'c'
					if (trgOC.charAt(j) != srcOC.charAt(i)) {
						// Error in sequence
						addAnnotationAndReportIssue(IssueType.SUSPECT_CODE, tu, trgCont, srcSeg.getId(),
								String.format("Suspect sequence of opening and closing placeholders in the target (placeholder %d).", i + 1),
								0, -1, 0, -1, Issue.SEVERITY_MEDIUM,
								getDisplay(srcSeg.getContent()),
								getDisplay(trgSeg.getContent()),
								trgList);
						done = true;
						break;
					}
					j++;
					break; // This code has been checked
				}
				if (done) {
					break;
				}
			}
		}

		// Otherwise, if needed, do the full forced-order analysis
		if ( checkOC && getParams().getStrictCodeOrder() ) {
			int j = 0;
			for (int i = 0; i < srcSimpleCodes.size(); i++) {
				SimpleCode srcSC = srcSimpleCodes.get(i);
				// Do we have a target code left?
				if (trgSimpleCodes.size() <= j) {
					// No more target code
					if ( !srcSC.isDeleteable ) {
						addAnnotationAndReportIssue(IssueType.SUSPECT_CODE, tu, trgCont, srcSeg.getId(),
							"Suspect sequence of target inline codes.", 0, -1, 0, -1,
							Issue.SEVERITY_MEDIUM,
							getDisplay(srcSeg.getContent()),
							getDisplay(trgSeg.getContent()),
							trgList);
						break;
					}
					continue; // Next source code
				}
				
				// At least one target code to try to match
				SimpleCode trgSC = trgSimpleCodes.get(j);
				if (( srcSC.type != trgSC.type ) || ( srcSC.id != trgSC.id )) {
					// Could the target code have been deleted?
					if ( srcSC.isDeleteable ) {
						// Then do not have an error
						continue; // Next source code
					}
					// Else: it is an error in the order
					addAnnotationAndReportIssue(IssueType.SUSPECT_CODE, tu, trgCont, srcSeg.getId(),
						String.format("Suspect sequence of codes in target: source code ID=%d (%s), target code ID=%d (%s)).",
							srcSC.id, srcSC.type, trgSC.id, trgSC.type),
						0, -1, 0, -1, Issue.SEVERITY_MEDIUM,
						getDisplay(srcSeg.getContent()),
						getDisplay(trgSeg.getContent()),
						trgList);
						break;
				}
				// No error, matching code, move to the next target code (and source code)
				j++;
			}
		}
	}

}
