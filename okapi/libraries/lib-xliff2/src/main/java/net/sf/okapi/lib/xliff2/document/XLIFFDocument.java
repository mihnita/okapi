/*===========================================================================
  Copyright (C) 2013-2017 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.document;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

import net.sf.okapi.lib.xliff2.URIParser;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.EventType;
import net.sf.okapi.lib.xliff2.reader.URIContext;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriterException;

/**
 * Represents a complete parsed XLIFF document in memory.
 */
public class XLIFFDocument {

	private StartXliffData docData;
	private LinkedHashMap<String, FileNode> files;
	private File docFile;
    private String lb = System.getProperty("line.separator");

    public XLIFFDocument () {
    	// Nothing to do
    }
    
    public XLIFFDocument (StartXliffData sxd) {
    	docData = sxd;
    }
    
    /**
     * Creates a new {@link #XLIFFDocument()} with given source and version.
     * @param sourceLang the source language.
     * @param version version of the XLIFF document (use null for the default).
     */
    public XLIFFDocument (String sourceLang,
    	String version)
    {
    	docData = new StartXliffData(version);
    	docData.setSourceLanguage(sourceLang);
    }
    
	/**
	 * Loads a document from a given file, with maximum validation.
	 * @param file the file to load.
	 */
	public void load (File file) {
		load(file, null, null, XLIFFReader.VALIDATION_MAXIMAL);
	}
	
	/**
	 * Loads a document from a given File.
	 * @param file the file to load.
	 * @param validation one of the VALIDATION_* constants or a ORed combination.
	 */
	public void load (File file,
		int validation)
	{
		load(file, null, null, validation);
	}
	
	/**
	 * Loads a document from a given URI.
	 * @param inputURI the URI to load from.
	 * @param validation one of the VALIDATION_* constants or a ORed combination.
	 */
	public void load (URI inputURI,
		int validation)
	{
		load(null, inputURI, null, validation);
	}
	
	/**
	 * Loads a document from a string.
	 * @param input the content of the document to load.
	 * @param validation one of the VALIDATION_* constants or a ORed combination.
	 */
	public void load (String input,
		int validation)
	{
		load(null, null, input, validation);
	}
	
	private void load (File file,
		URI inputURI,
		String input,
		int validation)
	{
		docFile = null;
		files = new LinkedHashMap<>();
		FileNode currentFile = null;
		GroupNode currentGroup = null;
		try ( XLIFFReader reader = new XLIFFReader(validation) ) {
			if ( file != null ) {
				docFile = file;
				reader.open(file);
			}
			else if ( inputURI != null ) {
				reader.open(inputURI);
			}
			else if ( input != null ) {
				reader.open(input);
			}
			// Read the document
			while ( reader.hasNext() ) {
				Event event = reader.next();
				switch ( event.getType() ) {
				case START_XLIFF:
					docData = event.getStartXliffData();
					break;
				case START_FILE:
					currentFile = new FileNode(event.getStartFileData());
					files.put(currentFile.getStartData().getId(), currentFile);
					break;
				case MID_FILE:
					currentFile.setMidData(event.getMidFileData());
					break;
				case END_FILE:
					currentFile = null;
					break;
				case SKELETON:
					currentFile.setSkeletonData(event.getSkeletonData());
					break;
				case START_GROUP:
					if ( currentGroup == null ) {
						currentGroup = currentFile.add(new GroupNode(null, event.getStartGroupData()));
					}
					else {
						currentGroup = currentGroup.add(new GroupNode(currentGroup, event.getStartGroupData()));
					}
					break;
				case END_GROUP:
					currentGroup = currentGroup.getParent();
					break;
				case TEXT_UNIT:
					if ( currentGroup == null ) {
						currentFile.add(new UnitNode(event.getUnit()));
					}
					else {
						currentGroup.add(new UnitNode(event.getUnit()));
					}
					break;
				case INSIGNIFICANT_PART:
					// Ideally we should store this too
					break;
				case START_DOCUMENT:
				case END_DOCUMENT:
				case END_XLIFF:
					// Nothing to do
					break;
				}
			}
		}
	}
	
	/**
	 * Saves this document to a specified file.
	 * If needed directories are created.
	 * @param outputFile the output file.
	 * @see #save()
	 */
	public void saveAs (File outputFile) {
		try {
			save(new OutputStreamWriter(new BufferedOutputStream(
				new FileOutputStream(outputFile)), StandardCharsets.UTF_8));
			docFile = outputFile;
		}
		catch ( Throwable e ) {
			throw new XLIFFWriterException("Cannot save the document.", e);
		}
	}
	
	/**
	 * Saves this document to the current file.
	 * The current file is not null when you have called {@link #load(File)}, {@link #load(File, int)}
	 * or {@link #saveAs(File)} before.
	 * @see #saveAs(File)
	 */
	public void save () {
		try {
			if ( docFile == null ) {
				throw new XLIFFWriterException("No file specified (use saveAs() instead).");
			}
			save(new OutputStreamWriter(new BufferedOutputStream(
				new FileOutputStream(docFile)), StandardCharsets.UTF_8));
		}
		catch ( Throwable e ) {
			throw new XLIFFWriterException("Cannot save the document.", e);
		}
	}
	
	/**
	 * Saves this document to a writer.
	 * The writer must use an encoding that supports all Unicode
	 * characters without escaping (e.g. UTF-8)
	 * @param outputWriter the output writer.
	 */
	public void save (Writer outputWriter) {
		try ( XLIFFWriter writer = new XLIFFWriter() ) {
			writer.setLineBreak(lb);
			writer.create(outputWriter, null, null);
			Iterator<Event> iter = createEventIterator();
			while ( iter.hasNext() ) {
				writer.writeEvent(iter.next());
			}
		}
	}
	
	/**
	 * Gets the document-level data for this document.
	 * @return the document-level data of this document.
	 */
	public StartXliffData getStartXliffData () {
		return docData;
	}
	
	/**
	 * Gets the {@link FileNode} from a given file id. 
	 * @param id the id of the file to lookup.
	 * @return the {@link FileNode} object or null if not found.
	 */
	public FileNode getFileNode (String id) {
		return files.get(id);
	}


    /**
     * Gets the IDs of all {@link FileNode} in this XLIFF document.
     * The order of IDs is the insertion order of files.
     * @return the list of IDs.
     */
    public List<String> getFileNodeIds () {
        return new ArrayList<>(files.keySet());
    }

	/**
	 * Gets the {@link GroupNode} from a given file id and a given group id.
	 * @param fileId the id of the file where the group is.
	 * @param groupId the id of the group to lookup.
	 * @return the {@link GroupNode} or null if not found.
	 */
	public GroupNode getGroupNode (String fileId,
		String groupId)
	{
		FileNode fn = files.get(fileId);
		if ( fn == null ) return null;
		return fn.getGroupNode(groupId);
	}
	
	/**
	 * Gets the {@link UnitNode} from a given file id and a given unit id.
	 * @param fileId the id of the file where the unit is.
	 * @param unitId the id of the unit to lookup.
	 * @return the {@link UnitNode} or null if not found.
	 */
	public UnitNode getUnitNode (String fileId,
		String unitId)
	{
		FileNode fn = files.get(fileId);
		if ( fn == null ) return null;
		return fn.getUnitNode(unitId);
	}
	
	/**
	 * Creates an iterator for the events of this document.
	 * @return a new iterator for the events of this document.
	 */
	public Iterator<Event> createEventIterator () {
		EventIterator ei = new EventIterator () {

			private Iterator<FileNode> fileIter = files.values().iterator();
			private Iterator<Event> eventIter = null;
			private int state = 0;
			
			@Override
			public boolean hasNext () {
				switch ( state ) {
				case 0: // start-input
					return true;
				case 1: // start-xliff
					return true;
				case 5: // End-input
					return true;
				case -1: // All done
					return false;
				}
				
				// case 2: normal entries
				if ( eventIter != null ) {
					if ( eventIter.hasNext() ) return true;
					// else: all done for that file
					eventIter = null;
					// Fall thru
				}
				if ( fileIter.hasNext() ) {
					eventIter = fileIter.next().createEventIterator(uriContext);
					eventIter.hasNext(); // Call once to prime that iterator
					return true;
				}
				// Else: no more file
				// next state is end-xliff
				state = 4;
				return true;
			}

			@Override
			public Event next () {
				switch ( state ) {
				case 0: // Start-input
					state = 1; // Next is start-xliff
					uriContext.push(new URIContext());
					return new Event(EventType.START_DOCUMENT, null);
				case 1: // start-xliff
					state = 2; // next is normal document entries
					return new Event(EventType.START_XLIFF, null, docData);
				case 4: // End-xliff
					state = 5; // next is end-input
					return new Event(EventType.END_XLIFF, null);
				case 5: // End-input
					state = -1; // Nothing after that
					return new Event(EventType.END_DOCUMENT, null);
				}
				
				// Else: state 3: normal entries through the current event iterator
				return eventIter.next();
			}

			@Override
			public void remove () {
				throw new UnsupportedOperationException("Remove is not supported.");
			}
		};
		
		ei.setURIContext(new Stack<>());
		return ei;
	}

	/**
	 * Retrieves the object corresponding to a given URI fragment.
	 * @param up the {@link URIParser} representing the fragment to resolve.
	 * @return the object found, or null if the object was not found or is external to this document.
	 */
	public Object fetchReference (URIParser up) {
		String scope = up.getScope();
		if ( scope.isEmpty() ) return null;
		char refType = up.getRefType();
		Object obj = null;
		FileNode fn = null;
		
		for ( int i=0; i<scope.length(); i++ ) {
			switch ( scope.charAt(i) ) {
			case 'f':
				fn = getFileNode(up.getFileId());
				if ( fn == null ) return null;
				if ( refType == 'f' ) return fn.getStartData();
				else if ( up.getRefContainer() == 'f' ) {
					if ( refType == 'n' ) {
						obj = URIContext.searchNotes(fn.getMidData(), up.getNoteId());
						if ( obj != null ) return obj;
					}
					else if ( refType == 'x' ) {
						obj = URIContext.searchExtensions(fn.getMidData(), up.getExtensionInfo());
						if ( obj != null ) return obj;
					}
				}
				break;
			case 'g':
				GroupNode gn = fn.getGroupNode(up.getGroupId());
				if ( gn == null ) return null;
				if ( refType == 'g' ) return gn.get();
				else if ( up.getRefContainer() == 'g' ) {
					if ( refType == 'n' ) {
						obj = URIContext.searchNotes(gn.get(), up.getNoteId());
						if ( obj != null ) return obj;
					}
					else if ( refType == 'x' ) {
						obj = URIContext.searchExtensions(gn.get(), up.getExtensionInfo());
						if ( obj != null ) return obj;
					}
				}
				break;
			case 'u':
				UnitNode un = fn.getUnitNode(up.getUnitId());
				if ( un == null ) return null;
				switch ( refType ) {
				case 'u':
					return un.get();
				case 's':
					obj = URIContext.searchInlineSource(un.get(), up.getSourceInlineId());
					if ( obj != null ) return obj;
					break;
				case 't':
					obj = URIContext.searchInlineTarget(un.get(), up.getTargetInlineId());
					if ( obj != null ) return obj;
					break;
				case 'n':
					if ( up.getRefContainer()=='u' ) {
						obj = URIContext.searchNotes(un.get(), up.getNoteId());
						if ( obj != null ) return obj;
					}
					break;
				case 'x':
					if ( up.getRefContainer()=='u' ) {
						obj = URIContext.searchUnit(un.get(), up.getExtensionInfo());
						if ( obj != null ) return obj;
					}
					break;
				case 'd':
					obj = URIContext.searchData(un.get(), up.getDataId());
					if ( obj != null ) return obj;
					break;
				}
				break;
			case 'x':
				break;
			}
		}
		return null;
	}

	/**
	 * Gets a list of all the units in this document.
	 * @return the list of all units in this document.
	 */
	public List<Unit> getUnits () {
		List<Unit> list = new ArrayList<>();
		Iterator<Event> iter = createEventIterator();
		while ( iter.hasNext() ) {
			Event event = iter.next();
			if ( event.isUnit() ) {
				list.add(event.getUnit());
			}
		}
		return list;
	}

	/**
	 * Gets the File object associated with this document.
	 * It can be null if the document we not read from a File.
	 * This object is automatically set when you call {@link #load(File)}, {@link #load(File, int)}
	 * and {@link #saveAs(File)}.
	 * @return the File object for this document (or null if none is associated. 
	 */
	public File getFile () {
		return docFile;
	}
	
	/**
	 * Adds a {@link FileNode} object to this document.
	 * @param id the ID of the file to add.
	 * @return the new {@link FileNode} object.
	 */
	public FileNode addFileNode (String id) {
		FileNode fn = new FileNode(new StartFileData(id));
		if ( files == null ) {
			files = new LinkedHashMap<>();
		}
		files.put(fn.getStartData().getId(), fn);
		return fn;
	}

    /**
     * Sets the line break to use when writing out this document.
     * By default the line-break used is the one of the OS.
     * @param lineBreak the line break to use for this document.
     */
    public void setLineBreak (String lineBreak) {
    	lb = lineBreak;
    }
    
}
