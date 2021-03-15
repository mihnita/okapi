/*===========================================================================
Copyright (C) 2013 by the Okapi Framework contributors
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

package net.sf.okapi.lib.merge.merge;

import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.MimeTypeMapper;
import net.sf.okapi.common.Range;
import net.sf.okapi.common.StringUtil;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.exceptions.OkapiMergeException;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.CodeAnomalies;
import net.sf.okapi.common.resource.CodeComparatorOnIdAndType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.IWithAnnotations;
import net.sf.okapi.common.resource.IWithProperties;
import net.sf.okapi.common.resource.InlineAnnotation;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragmentUtil;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextPartComparator;
import net.sf.okapi.common.resource.TextUnitUtil;
import org.incava.diff.LCS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TextUnitMerger implements ITextUnitMerger {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private LocaleId trgLoc;
	private Parameters params;
	private List<Range> srcRanges;
	private List<Range> trgRanges;

	public TextUnitMerger() {
		params = new Parameters();
	}

	@Override
	public ITextUnit mergeTargets(final ITextUnit tuFromSkel, final ITextUnit tuFromTran) {
		if (tuFromSkel == null || tuFromTran == null) {
			LOGGER.warn("Null TextUnit in TextUnitMerger.");
			return tuFromSkel;
		}

		if ( !tuFromSkel.getId().equals(tuFromTran.getId()) ) {
			final String s = String.format("Text Unit id mismatch during merger: Original id=\"%s\" target id=\"%s\"", tuFromSkel.getId(), tuFromTran.getId());
			LOGGER.error(s);
			if (params.isThrowSegmentIdException()) {
				throw new OkapiMergeException(s);
			}
		}

		// since segmentation will likely collapse whitespace in the xliff file
		// we need to do the same to avoid false positives
		// Use StringUtil.removeWhiteSpace as it covers all Unicode
		// whitespace defined by "\s" and trims.
		// This seems to cover all the differences between source in the
		// original and source in the xliff
		final String originalSource = StringUtil.removeWhiteSpace(tuFromSkel.getSource().createJoinedContent().getText());
		final String sourceFromTranslated = StringUtil.removeWhiteSpace(tuFromTran.getSource().createJoinedContent().getText());
		if (!originalSource.equals(sourceFromTranslated)) {
			final String s = String.format(
					"Text Unit source mismatch during merge: Original id=\"%s\" target id=\"%s\"\n"
							+ "Original Source=\"%s\"\n"
							+ "Translated Source=\"%s\"", tuFromSkel.getId(),
							tuFromTran.getId(), originalSource, sourceFromTranslated);
			LOGGER.error(s);
			if (params.isThrowSegmentSourceException()) {
				throw new OkapiMergeException(s);
			}
		}

		// Check if we have a translation
		// if not we merge with the original source
		// FIXME: some valid translations may be empty even if the source has text
		if (!tuFromTran.hasTarget(trgLoc) || tuFromTran.getTarget(trgLoc) == null) {
			// log error only if the source is not empty
			if (tuFromSkel.getSource().hasText()) {
				LOGGER.warn("No translation found for TU id='{}'. Using source as translation.", tuFromTran.getId());
			}
			return tuFromSkel;
		}

		// Process the "approved" property
		boolean isTransApproved = false;
		final Property traProp = tuFromTran.getTarget(trgLoc).getProperty(Property.APPROVED);
		if ( traProp != null ) {
			isTransApproved = traProp.getValue().equals("yes");
		}
		if ( params != null && params.isApprovedOnly() && !isTransApproved ) {
			LOGGER.warn("Item id='{}': Target is not approved. Skipping. Using source as translation.", tuFromSkel.getId());
			return tuFromSkel;
		}

		// NOTE: since we tested target for null above, no need to check for null below

		// create merged TextUnit. We shouldn't modify event resources if possible
		// so we copy and clone as needed until we have a complete merged
		// text unit. We start with the default tu from the original file or skel
		// to get all skeleton, properties and annotations from the filter
		final ITextUnit mergedTextUnit = tuFromSkel.clone();

		// need source from translated tu since segmentation, properties,
		// codes or annotations could have changed post filter
		// For example xliff2 filter may deepen its segmentation or
		// new properties/annotations were added
		mergedTextUnit.setSource(tuFromTran.getSource().clone());

		// clone the translated target
		// FIXME: assume bilingual only?
		mergedTextUnit.setTarget(trgLoc, tuFromTran.getTarget(trgLoc).clone());

		// we need to copy over container properties/annotations from tuFromSkel
		// as some of these may be lost during translation
		IWithProperties.copy(tuFromSkel.getSource(), mergedTextUnit.getSource());
		IWithAnnotations.copy(tuFromSkel.getSource(), mergedTextUnit.getSource());
		IWithProperties.copy(tuFromSkel.getTarget(trgLoc), mergedTextUnit.getTarget(trgLoc));
		IWithAnnotations.copy(tuFromSkel.getTarget(trgLoc), mergedTextUnit.getTarget(trgLoc));

		// We need to preserve the original segmentation for merging
		final boolean mergeAsSegments = MimeTypeMapper.isSegmentationSupported(tuFromSkel.getMimeType());

		// Remember the ranges to set them back after merging, may be empty list
		srcRanges = mergedTextUnit.getSource().getSegments().getRanges();
		trgRanges = mergedTextUnit.getTarget(trgLoc).getSegments().getRanges();

		// if there are any codes that were simplified (merged or trimmed)
		// recover the original code data now.
		// this happens in TextUnitUtil.simplifyCodesPostSegmentation
		for (final TextPart tp : mergedTextUnit.getSource().getSegments()) {
			if (TextUnitUtil.hasMergedCode(tp.text)) {
				tp.text = TextUnitUtil.expandCodes(tp.text);
			}
		}

		for (final TextPart tp : mergedTextUnit.getTarget(trgLoc).getSegments()) {
			if (TextUnitUtil.hasMergedCode(tp.text)) {
				tp.text = TextUnitUtil.expandCodes(tp.text);
			}
		}

		// join the segments together for the code transfer
		// This allows to move codes anywhere in the text unit,
		// not just each part. We do remember the ranges because
		// some formats will require to be merged with segments
		// WARNING: joining segments destroys Segment and TextPart
		// annotations, properties etc.. but we recover these later
		if (!mergedTextUnit.getSource().contentIsOneSegment()) {
			mergedTextUnit.getSource().getSegments().joinAll();
		}
		if (!mergedTextUnit.getTarget(trgLoc).contentIsOneSegment()) {
			mergedTextUnit.getTarget(trgLoc).getSegments().joinAll();
		}

		// remove outerData from translated codes as this is often a byproduct of the translated
		// file. example xliff, xliff2 etc.. Some translated files may not introduce outerData
		// such as custom JSON formats
		// FIXME: Should this be optional based on translated file type?
		removeOuterData(mergedTextUnit.getSource().getFirstContent().getCodes());
		removeOuterData(mergedTextUnit.getTarget(trgLoc).getFirstContent().getCodes());

		TextContainer skelContent = tuFromSkel.getSource();
		// we need a copy of skel to match parts in mergedTextUnit
		if (!tuFromSkel.getSource().contentIsOneSegment()) {
			skelContent = tuFromSkel.getSource().clone();
			skelContent.joinAll();
		}

		// check for code errors
		// do this before we copy code fields below as we match codes with
		// data and type, not ids
		final CodeAnomalies codeAnomalies = TextFragmentUtil.catalogCodeAnomalies(
				mergedTextUnit.getSource().getFirstContent(), mergedTextUnit.getTarget(trgLoc).getFirstContent());
		if (codeAnomalies != null) {
			final StringBuilder e = new StringBuilder();
			e.append(String.format("Text Unit id: %s", tuFromTran.getId()));
			if (codeAnomalies.hasAddedCodes()) {
				e.append(String.format("\nAdded Codes in target='%s'", codeAnomalies.addedCodesAsString()));
			}
			if (codeAnomalies.hasMissingCodes()) {
				e.append(String.format("\nMissing Codes in target='%s'", codeAnomalies.missingCodesAsString()));
			}
			e.append("\nTarget Text Unit:\n");
			e.append(mergedTextUnit.getTarget(trgLoc).getFirstContent().toText());
			LOGGER.error(e.toString());
			if (params.isThrowCodeException()) {
				throw new OkapiMergeException(e.toString());
			}
		}

		// copy code fields from original/skeleton tu. We do this because
		// we use the translated source (i.e, xliff) and info may be lost in
		// the translated file
		copyCodeMeta(skelContent.getFirstContent(),
				mergedTextUnit.getSource().getFirstContent());

		// now copy codes fields to the target so source and target codes are the same
		copyCodeMeta(mergedTextUnit.getSource().getFirstContent(),
				mergedTextUnit.getTarget(trgLoc).getFirstContent());

		// Re-apply segmentation ranges
		if ( mergeAsSegments ) {
			if ( !Util.isEmpty(srcRanges) ) {
				mergedTextUnit.getSource().getSegments().create(srcRanges, true);
			}
			if ( !Util.isEmpty(trgRanges) ) {
				mergedTextUnit.getTarget(trgLoc).getSegments().create(trgRanges, true);
			}
		}

		// Check if source/target segment count is the same
		// FIXME: maybe should be info level - this isn't illegal
		if (mergedTextUnit.getSource().getSegments().count() != mergedTextUnit.getTarget(trgLoc).getSegments().count()) {
			LOGGER.warn("Item id='{}': Different number of source and target segments.",
					tuFromTran.getId());
		}

		// we need to copy over any lost TextPart/Segment properties/annotations deleted with joinAll()
		if ( mergeAsSegments ) {
			copyTextPartMeta(tuFromSkel.getSource().getParts(), mergedTextUnit.getSource().getParts(), mergedTextUnit.getId());
			// if there was an original target from skeleton
			if (tuFromSkel.hasTarget(trgLoc)) {
				copyTextPartMeta(tuFromSkel.getTarget(trgLoc).getParts(), mergedTextUnit.getTarget(trgLoc).getParts(), mergedTextUnit.getId());
			}
		}

		return mergedTextUnit;
	}

	@SuppressWarnings("null")
	@Override
	public void copyTextPartMeta(final List<TextPart> from, final List<TextPart> to, final String id) {
		// short circuit optimization
		if (Util.isEmpty(from) && Util.isEmpty(to)) {
			return;
		}

		final TextPartComparator cmp = new TextPartComparator();
		final boolean [] matched = new boolean[to.size()];
		Arrays.fill(matched, false);

		// align TextParts based on TextPartComparator
		// this will give a more accurate match for TextParts without id's
		final LCS<TextPart> diff = new LCS<>(from, to, cmp);
		final List<Integer> matches = diff.getMatches();

		int n = -1;
		for (final Integer m : matches) {
			TextPart tp = null;
			n++;
			final TextPart origPart = from.get(n);
			if (m == null) {
				// LCS can return mismatches if heavily reordered
				// resort to linear search
				tp = findMatch(origPart, to, matched, cmp);
				if (tp == null) {
					// log error only if there was metadata in the original file TextPart
					// that needed to be copied, otherwise assume segmentation changes
					// in the translated file
					if (hasMeta(origPart)) {
						LOGGER.error("TextUnit id='{}' TextPart id='{}': Can't find matching TextPart in merged text unit. "
								+ "Cannot copy TextPart properties/annotations. Merged file may differ from original", id,
								origPart.id);
					} else {
						LOGGER.warn("TextUnit id='{}' TextPart id='{}': Can't find matching TextPart in merged text unit. "
								+ "Possible segmentation changes in the translated file", id, origPart.id);
					}
					continue;
				}
			}

			if (m != null) {
				tp = to.get(m);
				matched[m] = true;
			}

			tp.originalId = origPart.originalId;
			tp.whitespaceSrategy = origPart.whitespaceSrategy;
			IWithProperties.copy(origPart, tp);
			IWithAnnotations.copy(origPart, tp);
		}
	}

	private <T extends TextPart> boolean hasMeta(final T p) {
		final boolean hasProps = !p.getPropertyNames().isEmpty();
		final boolean hasAnnotations = p.getAnnotations().iterator().hasNext();
		return (hasProps || hasAnnotations || p.originalId != null);
	}

	@Override
	public void copyCodeMeta(final TextFragment from, final TextFragment to) {
		// short circuit optimization
		if (from == null || !from.hasCode()) {
			return;
		}

		if (to == null || !to.hasCode()) {
			return;
		}

		final CodeComparatorOnIdAndType cmp = new CodeComparatorOnIdAndType();
		final boolean [] matched = new boolean[to.getCodes().size()];
		Arrays.fill(matched, false);

		// align Codes based with CodeComparatorOnIdAndType
		// this will give more accurate contextual matching
		final LCS<Code> diff = new LCS<>(from.getCodes(), to.getCodes(), cmp);
		final List<Integer> matches = diff.getMatches();

		int n = -1;
		for (final Integer m : matches) {
			Code c = null;
			n++;
			final Code orig = from.getCode(n);

			if (m == null) {
				// LCS can return mismatches if heavily reordered
				// resort to linear search
				c = findMatch(orig, to.getCodes(), matched, cmp);
				if (c == null) {
					LOGGER.warn("Code id='{}': Can't find matching Code in merged text unit. Inserted or Deleted Code?",
							orig.getId());
					continue;
				}
			}

			if (m != null) {
				c = to.getCode(m);
				matched[m] = true;
			}

			copyCodeMeta(from.getCode(n), c);
		}
	}

	private <T> T findMatch(final T orig, final List<T> things, final boolean [] matched, final Comparator<T> cmp) {
		int i = -1;
		for (final T t : things) {
			i++;
			if (!matched[i] && cmp.compare(orig, t) == 0) {
				matched[i] = true;
				return t;
			}
		}
		return null;
	}

	private void copyCodeMeta(final Code sc, final Code tc) {
		// setData also calls setReferenceFlag
		// don't overwrite target code data if it exists
		// as it's contents may have changed
		if (!tc.hasData())
			tc.setData(sc.getData());
		tc.setOuterData(sc.getOuterData());
		tc.setOriginalId(sc.getOriginalId());
		tc.setAdded(sc.isAdded());
		tc.setCloneable(sc.isCloneable());
		tc.setDeleteable(sc.isDeleteable());
		tc.setDisplayText(sc.getDisplayText());
		tc.setFlag(sc.getFlag());
		tc.setMerged(sc.isMerged());
 		tc.setMarkerMasking(sc.isMarkerMasking());
		tc.setMergedData(sc.getMergedData());
		tc.setTagType(sc.getTagType());

		final Set<String> types = sc.getAnnotationsTypes();
		for (final String type : types) {
			final InlineAnnotation a = sc.getAnnotation(type);
			tc.setAnnotation(type, a.clone());
		}

		IWithProperties.copy(sc, tc);
	}

	private void removeOuterData(final List<Code> codes) {
		for (final Code c : codes) {
			c.setOuterData(null);
		}
	}

	@Override
	public void setTargetLocale(final LocaleId trgLoc) {
		this.trgLoc = trgLoc;
	}

	public LocaleId getTargetLocale() {
		return trgLoc;
	}

	@Override
	public Parameters getParameters() {
		return params;
	}

	@Override
	public void setParameters(final Parameters params) {
		this.params = params;
	}
}
