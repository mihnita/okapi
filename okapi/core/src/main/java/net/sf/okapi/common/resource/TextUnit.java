/*===========================================================================
  Copyright (C) 2008-2011 by the Okapi Framework contributors
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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.ISegmenter;
import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiException;

/**
 * Basic unit of extraction from a filter and also the resource associated with
 * the filter event TEXT_UNIT.
 * The TextUnit object holds the extracted source text in one or more versions,
 * all its properties and annotations, and any target corresponding data.
 */
public class TextUnit extends BaseReferenceable implements ITextUnit {

	private static int TARGETS_INITCAP = 2;

	private int refCount;
	private String type;
	private boolean isTranslatable = true;
	private boolean preserveWS;

	private TextContainer source;
	private ConcurrentHashMap<LocaleId, TextContainer> targets = new ConcurrentHashMap<>(TARGETS_INITCAP);

	public TextUnit() {
		create(null, null, false, null);
	}

	/**
	 * Creates a new TextUnit object with its identifier.
	 *
	 * @param id the identifier of this resource.
	 */
	public TextUnit(String id) {
		create(id, null, false, null);
	}

	/**
	 * Creates a new TextUnit object with its identifier and a text.
	 *
	 * @param id         the identifier of this resource.
	 * @param sourceText the initial text of the source.
	 */
	public TextUnit(String id, String sourceText) {
		create(id, sourceText, false, null);
	}

	/**
	 * Creates a new TextUnit object with its ID, a text, and a flag indicating if
	 * it is a referent or not.
	 *
	 * @param id         the identifier of this resource.
	 * @param sourceText the initial text of the source (can be null).
	 * @param isReferent indicates if this resource is a referent (i.e. is referred
	 *                   to by another resource) or not.
	 */
	public TextUnit(String id, String sourceText, boolean isReferent) {
		create(id, sourceText, isReferent, null);
	}

	/**
	 * Creates a new TextUnit object with its identifier, a text, a flag indicating
	 * if it is a referent or not, and a given MIME type.
	 *
	 * @param id         the identifier of this resource.
	 * @param sourceText the initial text of the source (can be null).
	 * @param isReferent indicates if this resource is a referent (i.e. is referred
	 *                   to by another resource) or not.
	 * @param mimeType   the MIME type identifier for the content of this TextUnit.
	 */
	public TextUnit(String id, String sourceText, boolean isReferent, String mimeType) {
		create(id, sourceText, isReferent, mimeType);
	}

	private void create(String id, String sourceText, boolean isReferent, String mimeType) {
		this.id = id;
		refCount = (isReferent ? 1 : 0);
		this.mimeType = mimeType;

		source = new TextContainer(sourceText);
	}

	@Override
	public boolean isEmpty() {
		return getSource().isEmpty();
	}

	@Override
	public TextContainer getSource() {
		return source;
	}

	@Override
	public TextContainer setSource(TextContainer textContainer) {
		source = textContainer;
		return source;
	}

	@Override
	public TextFragment setSourceContent(TextFragment content) {
		getSource().setContent(content);
		return getSource().getFirstContent();
	}

	@Override
	public TextContainer createTarget(LocaleId targetLocale, boolean overwriteExisting, int creationOptions) {
		TextContainer trgCont = targets.get(targetLocale);
		if ((trgCont == null) || overwriteExisting) {
			trgCont = getSource().clone((creationOptions & INameable.COPY_PROPERTIES) == INameable.COPY_PROPERTIES);
			if ((creationOptions & INameable.COPY_SEGMENTATION) != INameable.COPY_SEGMENTATION) {
				trgCont.joinAll();
			}
			if ((creationOptions & INameable.COPY_CONTENT) != INameable.COPY_CONTENT) {
				for (Segment seg : trgCont.getSegments()) {
					seg.text.clear();
				}
			}
			targets.put(targetLocale, trgCont);
		}
		return trgCont;
	}

	@Override
	public TextContainer getTarget(LocaleId locId) {
		return targets.get(locId);
	}

	@Override
	public TextContainer setTarget(LocaleId locId, TextContainer text) {
		targets.put(locId, text);
		return text;
	}

	@Override
	public void removeTarget(LocaleId locId) {
		if (hasTarget(locId)) {
			targets.remove(locId);
		}
	}

	@Override
	public boolean hasTarget(LocaleId locId) {
		// ConcurrentHashMap doesn't allow nulls so no need to check for null
		return targets.containsKey(locId);
	}

	@Override
	public TextFragment setTargetContent(LocaleId locId, TextFragment content) {
		TextContainer tc = createTarget(locId, false, INameable.CREATE_EMPTY);
		tc.setContent(content);
		// We can use this because the setContent() removed any segmentation
		return tc.getSegments().getFirstContent();
	}

	@Override
	public IAlignedSegments getAlignedSegments() {
		return new AlignedSegments(this);
	}

	@Override
	public ISegments getSourceSegments() {
		return getSource().getSegments();
	}

	@Override
	public Segment getSourceSegment(String segId, boolean createIfNeeded) {
		Segment seg = getSource().getSegments().get(segId);
		if ((seg == null) && createIfNeeded) {
			seg = new Segment(segId);
			getSource().getSegments().append(seg);
		}
		return seg;
	}

	@Override
	public ISegments getTargetSegments(LocaleId trgLoc) {
		return createTarget(trgLoc, false, INameable.COPY_SEGMENTATION).getSegments();
	}

	@Override
	public Segment getTargetSegment(LocaleId trgLoc, String segId, boolean createIfNeeded) {
		Segment seg = createTarget(trgLoc, false, INameable.COPY_SEGMENTATION).getSegments().get(segId);
		if ((seg == null) && createIfNeeded) {
			// If the segment does not exists: create a new one if requested
			seg = new Segment(segId);
			getTarget(trgLoc).getSegments().append(seg);
			// TODO consider appending a segment to variant source if present
		}
		return seg;
	}

	@Override
	public Set<LocaleId> getTargetLocales() {
		return targets.keySet();
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public void setType(String value) {
		type = value;
	}

	@Override
	public boolean isTranslatable() {
		return isTranslatable;
	}

	@Override
	public void setIsTranslatable(boolean value) {
		isTranslatable = value;
	}

	@Override
	public boolean preserveWhitespaces() {
		return preserveWS;
	}

	@Override
	public void setPreserveWhitespaces(boolean value) {
		preserveWS = value;
	}

	@Override
	public void setSkeleton(ISkeleton skeleton) {
		if (skeleton != null)
			skeleton.setParent(this);
		super.setSkeleton(skeleton);
	}

	@Override
	public boolean isReferent() {
		return (refCount > 0);
	}

	@Override
	public void setIsReferent(boolean value) {
		refCount = (value ? 1 : 0);
	}

	@Override
	public int getReferenceCount() {
		return refCount;
	}

	@Override
	public void setReferenceCount(int value) {
		refCount = value;
	}

	/**
	 * Gets the string representation of the default source container. If the
	 * container is segmented, the representation shows the merged segments. Inline
	 * codes are also included.
	 *
	 * @return the string representation of the source container.
	 */
	@Override
	public String toString() {
		return getSource().toString();
	}

	/**
	 * Clones this TextUnit.
	 *
	 * @return A new TextUnit object that is a copy of this one.
	 */
	@Override
	public TextUnit clone() {
		TextUnit tu = new TextUnit(getId());
		tu.setIsReferent(isReferent());
		tu.setIsTranslatable(isTranslatable);
		tu.setMimeType(getMimeType());
		tu.setName(getName());
		tu.setPreserveWhitespaces(preserveWS);
		tu.setReferenceCount(getReferenceCount());
		tu.setSource(getSource().clone());
		tu.setType(getType());

		// Copy all the targets
		for (Entry<LocaleId, TextContainer> entry : targets.entrySet()) {
			tu.setTarget(entry.getKey(), entry.getValue().clone());
		}

		IWithProperties.copy(this, tu);
		IWithTargetProperties.copy(this, tu);
		IWithSourceProperties.copy(this, tu);
		IWithAnnotations.copy(this, tu);

		if (getSkeleton() != null) {
			ISkeleton skel = getSkeleton().clone();
			tu.setSkeleton(skel);
		}

		return tu;
	}

	@Override
	public void removeAllSegmentations () {
		// Desegment the source if needed
		if ( getSource().hasBeenSegmented() ) {
			getSource().joinAll();
		}

		// Desegment all targets as needed
		for (Entry<LocaleId, TextContainer> entry : targets.entrySet()) {
			if ( entry.getValue().hasBeenSegmented() ) {
				entry.getValue().joinAll();
			}
		}
	}

	@Override
	public void createSourceSegmentation(ISegmenter segmenter) {
		segmenter.computeSegments(getSource());
		getSource().getSegments().create(segmenter.getRanges());
	}

	@Override
	public void createTargetSegmentation(ISegmenter segmenter, LocaleId targetLocale)
	{
		TextContainer tc = getTarget(targetLocale);
		if ( tc == null ) {
			throw new OkapiException(String.format("There is no target content for '%s'", targetLocale.toString()));
		}
		segmenter.computeSegments(tc);
		tc.getSegments().create(segmenter.getRanges());
	}
	/*
	 * ITextUnit delegates source and target properties to TextContainer
	 */

	@Override
	public Property getSourceProperty(String name) {
		return getSource().getProperty(name);
	}

	@Override
	public Property setSourceProperty(Property property) {
		return getSource().setProperty(property);
	}

	// for source of active locale
	@Override
	public void removeSourceProperty(String name) {
		getSource().removeProperty(name);
	}

	@Override
	public Set<String> getSourcePropertyNames() {
		return getSource().getPropertyNames();
	}

	@Override
	public boolean hasSourceProperty(String name) {
		return getSource().hasProperty(name);
	}

	@Override
	public Property getTargetProperty(LocaleId locId, String name) {
		if (!hasTarget(locId))
			return null;
		return getTarget(locId).getProperty(name);
	}

	@Override
	public Property setTargetProperty(LocaleId locId, Property property) {
		return createTarget(locId, false, IResource.COPY_SEGMENTATION).setProperty(property);
	}

	@Override
	public void removeTargetProperty(LocaleId locId, String name) {
		if (hasTarget(locId)) {
			getTarget(locId).removeProperty(name);
		}
	}

	@Override
	public Set<String> getTargetPropertyNames(LocaleId locId) {
		if (hasTarget(locId)) {
			return getTarget(locId).getPropertyNames();
		}
		return Collections.emptySet();
	}

	@Override
	public boolean hasTargetProperty(LocaleId locId, String name) {
		TextContainer tc = getTarget(locId);
		if (tc == null)
			return false;
		return (tc.getProperty(name) != null);
	}

	@Override
	public Map<String, Property> getSourceProperties() {
		return getSource().getProperties();
	}

	@Override
	public Property createTargetProperty(LocaleId locId, String name, boolean overwriteExisting, int creationOptions) {
		// Get the target or create an isEmpty one
		TextContainer tc = createTarget(locId, false, CREATE_EMPTY);
		// Get the property if it exists
		Property prop = tc.getProperty(name);
		// If it does not exists or if we overwrite: create a new one
		if ((prop == null) || overwriteExisting) {
			// Get the source property
			prop = getSource().getProperty(name);
			if (prop == null) {
				// If there is no source, create an isEmpty property
				return tc.setProperty(new Property(name, "", false));
			} else { // If there is a source property
						// Create a copy, isEmpty or not depending on the options
				if (creationOptions == CREATE_EMPTY) {
					return tc.setProperty(new Property(name, "", prop.isReadOnly()));
				} else {
					return tc.setProperty(prop.clone());
				}
			}
		}
		return prop;
	}
}
