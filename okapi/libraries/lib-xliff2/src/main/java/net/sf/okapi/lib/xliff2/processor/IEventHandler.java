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

package net.sf.okapi.lib.xliff2.processor;

import net.sf.okapi.lib.xliff2.reader.Event;

/**
 * Provides the methods to handle events generated by the {@link net.sf.okapi.lib.xliff2.reader.XLIFFReader XLIFFReader} object.
 */
public interface IEventHandler {

	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#START_DOCUMENT START_DOCUMENT} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleStartDocument(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#END_DOCUMENT END_DOCUMENT} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleEndDocument(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#START_XLIFF START_XLIFF} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleStartXliff(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#END_XLIFF END_XLIFF} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleEndXliff(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#START_FILE START_FILE} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleStartFile(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#SKELETON SKELETON} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleSkeleton(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#MID_FILE MID_FILE} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleMidFile(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#END_FILE END_FILE} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleEndFile(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#START_GROUP START_GROUP} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleStartGroup(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#END_GROUP END_GROUP} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleEndGroup(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#INSIGNIFICANT_PART INSIGNIFICANT_PART} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleInsignificantPart(Event event);
	
	/**
	 * Handles the {@link net.sf.okapi.lib.xliff2.reader.EventType#TEXT_UNIT TEXT_UNIT} event.
	 * @param event the event to process.
	 * @return the processed event.
	 */
	Event handleUnit(Event event);
	
}