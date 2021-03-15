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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.annotation.AltTranslation;
import net.sf.okapi.common.annotation.AltTranslationsAnnotation;
import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.skeleton.GenericSkeletonWriter;
import net.sf.okapi.common.skeleton.ISkeletonWriter;

public class TEXSkeletonWriter extends GenericSkeletonWriter implements ISkeletonWriter  {
	private TEXEncoder texencoder;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	public TEXSkeletonWriter() {
		super();
		texencoder = new TEXEncoder();
	}
	
//	/**
//	 * Gets the skeleton and the original content of a given text unit.
//	 * @param tu The text unit to process.
//	 * @param locToUse locale to output. Use null for the source, or a LocaleId
//	 * object for the target locales.
//	 * @param context Context flag: 0=text, 1=skeleton, 2=in-line.
//	 * @return The string representation of the text unit. 
//	 */
//	protected String getString (ITextUnit tu, LocaleId locToUse, EncoderContext context){
//		GenericSkeleton skel = (GenericSkeleton)tu.getSkeleton();
//		if ( skel == null ) { // No skeleton
//			String preProcessed = getContent(tu, locToUse, context);
//			// convert accented symbols to escaped ā=> \={a}
//			return texencoder.toNative("placeholder",preProcessed);
//		}
//		// Else: process the skeleton parts, one of them should
//		// refer to the text-unit content itself
//		StringBuilder tmp = new StringBuilder();
//		for ( GenericSkeletonPart part : skel.getParts() ) {
//			tmp.append(getString(part, context));
//		}
//		String preProcessed = tmp.toString();
//		// convert accented symbols to escaped ā=> \={a}
//		return texencoder.toNative("placeholder",preProcessed);
//	}
	
	/**
	 * Gets the original content of a given text unit.
	 * @param tu The text unit to process.
	 * @param locToUse locale to output. Use null for the source, or the locale
	 * for the target locales.
	 * @param context Context flag: 0=text, 1=skeleton, 2=inline.
	 * @return The string representation of the text unit content.
	 */
	protected String getContent (ITextUnit tu,
		LocaleId locToUse,
		EncoderContext context)
	{
		// Update the encoder from the TU's MIME type
		if ( encoderManager != null ) {
			encoderManager.updateEncoder(tu.getMimeType());
		}
		
		// Get the right text container
		TextContainer srcCont = tu.getSource();
		TextContainer trgCont = null;
		if ( locToUse != null ) { // Expects a target output
			trgCont = tu.getTarget(locToUse);
			// If we do not have target
			// or if the target is empty (regardless the source)
			if (( trgCont == null ) || trgCont.isEmpty() ) {
				// If there is no target available
				if ( allowEmptyOutputTarget && ( layer == null )) {
					// If empty targets are allowed and we don't have one: create a temporary one
					if ( trgCont == null ) {
						trgCont = tu.createTarget(locToUse, false, IResource.CREATE_EMPTY);
					}
				}
				else { // Fall back to the source
					trgCont = srcCont;
				}
			}
		}
		else { // Use the source
			// Set trgCont to it because that's the one driving the output
			trgCont = srcCont;
		}
		// Now trgCont is either the available target or the source (fall-back case)

		if ( !tu.isTranslatable() ) {
			context = EncoderContext.TEXT; // Keep skeleton context
		}
		
		if ( srcCont.hasBeenSegmented() || !srcCont.contentIsOneSegment()
			|| trgCont.hasBeenSegmented() || !trgCont.contentIsOneSegment() 
			|| ( trgCont.getAnnotation(AltTranslationsAnnotation.class) != null ))
		{
			return getSegmentedText(srcCont, trgCont, locToUse, context, tu.isReferent(), tu.getId());
		}

		// Else: We have only one segment
		// Use trgCont, even if locToUse == null because then it's the source
		TextFragment tf = trgCont.getFirstContent();

		// Apply the layer if there is one
		if ( layer == null ) {
			return getContent(tf, locToUse, context);
		}
		else {
			switch ( context ) {
			case SKELETON:
				return layer.endCode()
					+ getContent(tf, locToUse, EncoderContext.TEXT)
					+ layer.startCode();
			case INLINE:
				return layer.endInline()
					+ getContent(tf, locToUse, EncoderContext.TEXT)
					+ layer.startInline();
			default:
				return getContent(tf, locToUse, context);
			}
		}
	}

	
	private String getSegmentedText (TextContainer srcCont,
		TextContainer trgCont,
		LocaleId locToUse,
		EncoderContext context,
		boolean isReferent,
		String tuId)
	{
		StringBuilder tmp = new StringBuilder();

		// Get the alternate-translations if available
		AltTranslationsAnnotation atAnn = null;
//			atAnn = trgCont.getAnnotation(AltTranslationsAnnotation.class);
		
		// The output is driven by the target, not the source, so the interstices parts
		// are the ones of the target, no the one of the source
		for ( TextPart part : trgCont ) {
			if ( part.isSegment() ) {
				Segment trgSeg = (Segment)part;
				TextFragment trgFrag = trgSeg.text;

				// Compute the leverage score
				int lev = 0;
				AltTranslation at = null;
				atAnn = trgSeg.getAnnotation(AltTranslationsAnnotation.class);
				if ( atAnn != null ) {
					at = atAnn.getFirst();
					if ( at != null ) {
						lev = at.getCombinedScore();
					}
				}
				
				// Fall-back on the source if needed
				Segment srcSeg = srcCont.getSegments().get(trgSeg.id);
				if ( srcSeg == null ) {
					// A target segment without a corresponding source: give warning
					logger.warn("No source segment found for target segment seg-id='{}' (TU id='{}'):\n\"{}\".",
						trgSeg.id, tuId, trgFrag.toText());
				}
				else {
					if ( trgFrag.isEmpty() && !srcSeg.text.isEmpty() ) {
						trgFrag = srcSeg.text;
						lev = 0; // Nothing leverage (target was not copied apparently)
					}
				}

				// Write the segment (note: srcSeg can be null)
				if ( layer == null ) {
					// If no layer: just write the target
//						tmp.append(getContent(trgFrag, locToUse, context));
					tmp.append(texencoder.toNative("placeholder",getContent(trgFrag, locToUse, context)));
				}
				else { // If layer: write the bilingual entry
					switch ( context ) {
					case SKELETON:
						tmp.append(layer.endCode()
							+ layer.startSegment()
							+ ((srcSeg==null) ? "" : getContent(srcSeg.text, locToUse, EncoderContext.TEXT))
							+ layer.midSegment(lev)
							+ getContent(trgFrag, locToUse, EncoderContext.TEXT)
							+ layer.endSegment()
							+ layer.startCode());
						break;
					case INLINE:
						tmp.append(texencoder.toNative("placeholder",layer.endInline()
							+ layer.startSegment()
							+ ((srcSeg==null) ? "" : getContent(srcSeg.text, locToUse, EncoderContext.TEXT))
							+ layer.midSegment(lev)
							+ getContent(trgFrag, locToUse, EncoderContext.TEXT)
							+ layer.endSegment()
							+ layer.startInline()));
						break;
					default:
						tmp.append(texencoder.toNative("placeholder",layer.startSegment()
							+ ((srcSeg==null) ? "" : getContent(srcSeg.text, locToUse, EncoderContext.TEXT))
							+ layer.midSegment(lev)
							+ getContent(trgFrag, locToUse, EncoderContext.TEXT)
							+ layer.endSegment()));
						break;
					}
				}
			}
			else { // Normal text fragment
				// Target fragment is used
				tmp.append(getContent(part.text, locToUse, context));
			}
		}

		return tmp.toString();
	}

}