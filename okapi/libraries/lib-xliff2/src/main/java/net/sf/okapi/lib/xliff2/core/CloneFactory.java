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

package net.sf.okapi.lib.xliff2.core;

import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.changeTracking.Item;
import net.sf.okapi.lib.xliff2.changeTracking.Revision;
import net.sf.okapi.lib.xliff2.changeTracking.Revisions;
import net.sf.okapi.lib.xliff2.glossary.GlossEntry;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.metadata.Meta;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup;
import net.sf.okapi.lib.xliff2.validation.Rule;

/**
 * Provides methods to create deep-copy clones of various abstract classes or interfaces.
 */
public class CloneFactory {

	/**
	 * Creates a deep-copy clone of a given {@link Tag} object.
	 * @param original the original tag to duplicate.
	 * @param destinationTags the list of destination tags. This is used to find and connect
	 * opening tags when creating closing tags.
	 * @return the new tag.
	 */
	static public Tag create (Tag original,
		Tags destinationTags)
	{
		if ( original instanceof CTag ) {
			CTag oct = null;
			if ( original.getTagType() == TagType.CLOSING ) {
				oct = destinationTags.getOpeningCTag(original.getId());
			}
			return new CTag((CTag)original, oct);
		}
		if ( original instanceof MTag ) {
			MTag mct = null;
			if ( original.getTagType() == TagType.CLOSING ) {
				mct = destinationTags.getOpeningMTag(original.getId());
			}
			return new MTag((MTag)original, mct);
		}
		throw new InvalidParameterException("The type of the original object is invalid.");
	}
	
	/**
	 * Creates a deep-copy clone of a given {@link Part}/{@link Segment} object.
	 * @param original the original part/segment to duplicate.
	 * @return the new part/segment.
	 */
	static public Part create (Part original) {
		if ( original instanceof Segment ) {
			return new Segment((Segment)original);
		}
		return new Part(original);
	}

	/**
	 * Creates a deep-copy clone of a given {@link IExtChild} object.
	 * @param original the original object to duplicate.
	 * @return the new object.
	 */
	static public IExtChild create (IExtChild original) {
		if ( original instanceof ExtElement ) {
			return new ExtElement((ExtElement)original);
		}
		if ( original instanceof ExtContent ) {
			return new ExtContent((ExtContent)original);
		}
		if ( original instanceof ProcessingInstruction ) {
			return new ProcessingInstruction((ProcessingInstruction)original);
		}
		throw new InvalidParameterException("The type of the original is invalid.");
	}

	/**
	 * Creates a deep-copy clone of a given object.
	 * @param original the original object to duplicate.
	 * @return the new object.
	 */
	static public Object create (Object original) {
		if ( original instanceof Rule ) {
			return new Rule((Rule)original);
		}
		if ( original instanceof Note ) {
			return new Note((Note)original);
		}
		if ( original instanceof GlossEntry ) {
			return new GlossEntry((GlossEntry)original);
		}
		if ( original instanceof Match ) {
			return new Match((Match)original);
		}
		if ( original instanceof Revisions ) {
			return new Revisions((Revisions)original);
		}
		if ( original instanceof Revision ) {
			return new Revision((Revision)original);
		}
		if ( original instanceof Item ) {
			return new Item((Item)original);
		}
		if ( original instanceof MetaGroup ) {
			return new MetaGroup((MetaGroup)original);
		}
		if ( original instanceof Meta ) {
			return new Meta((Meta)original);
		}
		throw new InvalidParameterException(String.format("Cloning from the object %s is not supported.", original.getClass().toString()));
	}

}
