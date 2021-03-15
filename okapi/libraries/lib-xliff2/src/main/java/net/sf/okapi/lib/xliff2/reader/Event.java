/*===========================================================================
  Copyright (C) 2011-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.reader;

import net.sf.okapi.lib.xliff2.core.InsingnificantPartData;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Skeleton;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Unit;

/**
 * Represents an event send by the the {@link XLIFFReader}. 
 */
public class Event {

	private EventType type;
	private Object object;
	private URIContext uriCtx;
	
	public Event (EventType type,
		URIContext uriCtx)
	{
		this.type = type;
		this.uriCtx = uriCtx;
	}
	
	public Event (EventType type,
		URIContext uriCtx,
		Object object)
	{
		this.type = type;
		this.uriCtx = uriCtx;
		this.object = object;
	}
	
	public EventType getType () {
		return this.type;
	}
	
	public URIContext getURIContext () {
		return uriCtx;
	}

	public boolean isStartDocument () {
		return (type == EventType.START_DOCUMENT);
	}
	
	public boolean isEndDocument () {
		return (type == EventType.END_DOCUMENT);
	}

	public boolean isStartXliff () {
		return (type == EventType.START_XLIFF);
	}
	
	public StartXliffData getStartXliffData () {
		return (StartXliffData)object;
	}
	
	public boolean isEndXliff () {
		return (type == EventType.END_XLIFF);
	}
	
	public boolean isStartFile () {
		return (type == EventType.START_FILE);
	}
	
	public StartFileData getStartFileData () {
		return (StartFileData)object;
	}

	public boolean isMidFile () {
		return (type == EventType.MID_FILE);
	}
	
	public MidFileData getMidFileData () {
		return (MidFileData)object;
	}

	public boolean isEndFile () {
		return (type == EventType.END_FILE);
	}
	
	public boolean isSkeleton () {
		return (type == EventType.SKELETON);
	}
	
	public Skeleton getSkeletonData () {
		return (Skeleton)object;
	}
	
	public boolean isStartGroup () {
		return (type == EventType.START_GROUP);
	}
	
	public StartGroupData getStartGroupData () {
		return (StartGroupData)object;
	}
	
	public boolean isEndGroup () {
		return (type == EventType.END_GROUP);
	}
	
	public boolean isUnit () {
		return (type == EventType.TEXT_UNIT);
	}
	
	public Unit getUnit () {
		return (Unit)object;
	}

	public InsingnificantPartData getInsingnificantPartData () {
		return (InsingnificantPartData)object;
	}

	public Object getResource () {
		return object;
	}
}
