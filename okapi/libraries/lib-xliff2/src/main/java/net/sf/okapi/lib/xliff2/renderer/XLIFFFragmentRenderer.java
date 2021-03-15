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

package net.sf.okapi.lib.xliff2.renderer;

import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import net.sf.okapi.lib.xliff2.NSContext;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.Tag;

/**
 * Implements {@link IFragmentRenderer} for the XLIFF 2 format.
 * <p>Note that the inline tags representation uses only <code>&lt;sc>/&lt;ec></code>
 * and <code>&lt;sm>/&lt;em><code>.
 */
public class XLIFFFragmentRenderer implements IFragmentRenderer {

	private Fragment frag;
	private Stack<NSContext> nsStack;
	private Map<Tag, Integer> tagsStatus;
	
	/**
	 * Creates a new {@link XLIFFFragmentRenderer} object for a given fragment and namespace context.
	 * @param fragment the fragment to associate with this renderer.
	 * @param nsStack the namespace stack (can be null).
	 */
	public XLIFFFragmentRenderer (Fragment fragment,
		Stack<NSContext> nsStack)
	{
		set(fragment, nsStack);
	}

	@Override
	public void set (Fragment fragment,
		Stack<NSContext> nsStack)
	{
		this.frag = fragment;
		this.nsStack = nsStack;
	}
	
	@Override
	public Iterator<IFragmentObject> iterator () {
		// Get the snapshot of the tags' status
		tagsStatus = frag.getOwnTagsStatus();
		// Create the new iterator
		return new Iterator<IFragmentObject>() {
			
			Iterator<Object> iter = frag.iterator();
			
			@Override
			public void remove () {
				iter.remove();
			}
			
			@Override
			public IFragmentObject next () {
				return new XLIFFFragmentObject(iter.next(), tagsStatus, nsStack);
			}
			
			@Override
			public boolean hasNext () {
				return iter.hasNext();
			}
		};
	}

}
