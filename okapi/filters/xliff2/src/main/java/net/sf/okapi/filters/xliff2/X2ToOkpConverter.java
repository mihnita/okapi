/*===========================================================================
  Copyright (C) 2014 by the Okapi Framework contributors
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

package net.sf.okapi.filters.xliff2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.Util;
import net.sf.okapi.common.annotation.GenericAnnotation;
import net.sf.okapi.common.annotation.GenericAnnotationType;
import net.sf.okapi.common.annotation.GenericAnnotations;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.DocumentPart;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Segment;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextPart;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.filters.xliff2.util.PropertiesMapper;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.metadata.IMetadataItem;
import net.sf.okapi.lib.xliff2.metadata.Meta;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup;
import net.sf.okapi.lib.xliff2.metadata.Metadata;

public class X2ToOkpConverter {	
    private final Logger logger = LoggerFactory.getLogger(getClass());

	private final LocaleId trgLoc;

	/**
	 * Creates a new converter object.
	 * @param trgLoc the target locale.
	 */
	public X2ToOkpConverter(LocaleId trgLoc) {
		this.trgLoc = trgLoc;
	}
	
	public DocumentPart convert(MidFileData midFileData) {
		DocumentPart documentPart = new DocumentPart();

		// Transfer XLIFF 2 metadata module elements into context group annotations
		if (midFileData.hasMetadata()) {
			GenericAnnotation contextGroup = metadataToContextGroup(midFileData.getMetadata());
			documentPart.setAnnotation(new GenericAnnotations(contextGroup));
		}

		return documentPart;
	}

	public StartGroup convert(StartGroupData sgd, String parentId) {
		StartGroup sg = new StartGroup(parentId, sgd.getId());

		// Transfer XLIFF 2 metadata module elements into context group annotations
		if (sgd.hasMetadata()) {
			GenericAnnotation contextGroup = metadataToContextGroup(sgd.getMetadata());
			sg.setAnnotation(new GenericAnnotations(contextGroup));
		}

		return sg;
	}

	// Converts XLIFF 2 metadata module elements into TextUnit annotations
	private GenericAnnotation metadataToContextGroup(Metadata md) {
		GenericAnnotation contextGroup = new GenericAnnotation(GenericAnnotationType.MISC_METADATA);
		int i = 0;
		for (MetaGroup metaGroup : md) {
			addMeta(metaGroup, contextGroup, String.valueOf(i));
			i++;
		}

		return contextGroup;
	}
	
	private void addMeta(IMetadataItem fromMetadata, GenericAnnotation toContextGroup, String sequenceId) {
		if (fromMetadata.isGroup()) {
			final MetaGroup metaGroup = (MetaGroup) fromMetadata;
			final Iterator<IMetadataItem> metaIterator = metaGroup.iterator();
			int i = 0;
			while (metaIterator.hasNext()) {
				final IMetadataItem next = metaIterator.next();
				addMeta(next, toContextGroup, sequenceId + "." + String.valueOf(i));
				i++;
			}
		} else {
			final Meta meta = (Meta) fromMetadata;
			// check for duplicate types
			if (toContextGroup.getString(meta.getType()) != null) {
				// use the group and meta sequence numbers to make unique
				toContextGroup.setString(meta.getType() + "." + sequenceId, meta.getData());
			} else {
				toContextGroup.setString(meta.getType(), meta.getData());
			}
		}
	}
		
	public ITextUnit convert (Unit unit) {
		ITextUnit tu = new TextUnit(unit.getId());
		tu.setName(unit.getName());
		tu.setType(unit.getType());

		// Transfer the source
		TextContainer tc = tu.getSource();
		convert(unit, tc, false);
		
		// Do we have at least one target part?
		boolean hasTarget = false;
		for ( Part part : unit ) {
			if ( part.hasTarget() ) {
				hasTarget = true;
				break;
			}
		}
		// Transfer the target if needed
		if ( hasTarget ) {
			tc = tu.createTarget(trgLoc, false, IResource.CREATE_EMPTY);
			convert(unit, tc, true);
		}
		
		// Transfer XLIFF 2 metadata module elements into context group annotations
		if (unit.hasMetadata()) {
			GenericAnnotation contextGroup = metadataToContextGroup(unit.getMetadata());
			tu.setAnnotation(new GenericAnnotations(contextGroup));
		}

		return tu;
	}
	
	private void convert (Unit unit, TextContainer dest, boolean isTarget)
	{
		List<TextPart> textParts = new ArrayList<>();
		int segId = 1;
		int tpId = 1;
		
		for (Part part : unit) {
			// use original xliff2 id if available
			// otherwise use auto-generated id
			final String id = part.getId();
			final TextPart converted;
			if (part.isSegment()) {
				converted = convertToSegment(part, Util.isEmpty(id) ? String.valueOf(segId) : id);
				segId++;
			} else {
				// ignorable or inter-segment text
				converted = convertToTextPart(part, Util.isEmpty(id) ? String.valueOf(tpId) : id);
				tpId++;
			}
			
			// Convert Part content (Fragment) to TextFragment
			if ( isTarget ) {
				if ( part.hasTarget() ) {
					convert(part.getTarget(), converted);
				}
				else {
					// Nothing to do: we will get an empty part/segment
				}
			}
			else {
				convert(part.getSource(), converted);
			}
			textParts.add(converted);
			PropertiesMapper.setPartProperties(part, converted);
		}
		
		dest.setParts(textParts.toArray(new TextPart[0]));
	}
	
	private Segment convertToSegment(Part part, String id) {
		Segment s = new Segment(id);
		// we need this when we merge to restore the original id
		s.originalId = part.getId();

		return s;
	}

	private TextPart convertToTextPart(Part part, String id) {
		TextPart tp = new TextPart(id, null);	
		// we need this when we merge to restore the original id
		tp.originalId = part.getId();
	
		return tp;
	}

	private void convert (Fragment frag, TextPart part) {
		TextFragment tf = part.text;
		for ( Object obj : frag ) {
			if ( obj instanceof String ) {
				tf.append((String)obj);
			}
			else if ( obj instanceof CTag ) {
				CTag ctag = (CTag)obj;
				Code code = new Code(ctag.getType());
				PropertiesMapper.setCodeProperties(ctag, code);
				//TODO: subtype, etc.
				tf.append(code);
			}
			else if ( obj instanceof MTag ) {
				// FIXME: Add support for markers
				logger.warn("Xliff 2 marker tags (mrk) are not supported segment/ignorable id='{}' mtag id='{}'",
						part.originalId, ((MTag)obj).getId());
				tf.append("[MARKER]");
			}
		}
	}
}
