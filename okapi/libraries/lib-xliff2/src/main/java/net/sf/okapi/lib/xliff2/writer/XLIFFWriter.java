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

package net.sf.okapi.lib.xliff2.writer;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.NamespaceContext;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.InvalidParameterException;
import net.sf.okapi.lib.xliff2.NSContext;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.changeTracking.ChangeTrack;
import net.sf.okapi.lib.xliff2.changeTracking.Item;
import net.sf.okapi.lib.xliff2.changeTracking.Revision;
import net.sf.okapi.lib.xliff2.changeTracking.Revisions;
import net.sf.okapi.lib.xliff2.core.Directionality;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.IExtChild;
import net.sf.okapi.lib.xliff2.core.IWithChangeTrack;
import net.sf.okapi.lib.xliff2.core.IWithExtAttributes;
import net.sf.okapi.lib.xliff2.core.IWithExtElements;
import net.sf.okapi.lib.xliff2.core.IWithMetadata;
import net.sf.okapi.lib.xliff2.core.IWithNotes;
import net.sf.okapi.lib.xliff2.core.IWithValidation;
import net.sf.okapi.lib.xliff2.core.InheritedData;
import net.sf.okapi.lib.xliff2.core.MidFileData;
import net.sf.okapi.lib.xliff2.core.Note;
import net.sf.okapi.lib.xliff2.core.Notes;
import net.sf.okapi.lib.xliff2.core.Part;
import net.sf.okapi.lib.xliff2.core.Part.GetTarget;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.Skeleton;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.StartGroupData;
import net.sf.okapi.lib.xliff2.core.StartXliffData;
import net.sf.okapi.lib.xliff2.core.Store;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.glossary.GlossEntry;
import net.sf.okapi.lib.xliff2.glossary.Translation;
import net.sf.okapi.lib.xliff2.its.AnnotatorsRef;
import net.sf.okapi.lib.xliff2.its.DataCategories;
import net.sf.okapi.lib.xliff2.its.ITSWriter;
import net.sf.okapi.lib.xliff2.matches.Match;
import net.sf.okapi.lib.xliff2.metadata.IMetadataItem;
import net.sf.okapi.lib.xliff2.metadata.Meta;
import net.sf.okapi.lib.xliff2.metadata.MetaGroup;
import net.sf.okapi.lib.xliff2.metadata.Metadata;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.validation.Rule;
import net.sf.okapi.lib.xliff2.validation.Rule.Normalization;
import net.sf.okapi.lib.xliff2.validation.Rule.Type;
import net.sf.okapi.lib.xliff2.validation.Validation;

/**
 * Provides the methods to create an XLIFF document or to re-write an existing one.
 */
public class XLIFFWriter implements Closeable {

	private PrintWriter writer = null;
    private String lb = System.getProperty("line.separator");
    private boolean useIndentation = false;
    private boolean useInsignificantParts = false;
    private boolean indentNonUnit = (useIndentation && !useInsignificantParts);
    private String indent;
    private String nonUnitLb;
    private boolean inDocument;
    private boolean inFile;
    private StartFileData startFileData;
    private boolean withData = true;
    private String sourceLang;
    private String targetLang;
    private Stack<NSContext> nsStack;
    private Stack<InheritedData> context;
    private Stack<StartGroupData> groupStack;
    private int autoFileId;
    private int autoGroupId;
    private ITSWriter itsWriter;
    private ExtensionsWriter extWriter;

    /**
     * Creates a new document with a given file, with a source language and a target language.
     * @param file the file where to create this document. If needed directories will be created
     * automatically to create this file.
     * @param sourceLang the source language for this document (can be null if set later).
     * @param targetLang the target language for this document (can be null).
     * @throws XLIFFWriterException if an error occurs. 
     */
    public void create (File file,
    	String sourceLang,
    	String targetLang)
    {
		try {
			// Create the directories if needed
			File dir = file.getParentFile();
			if ( dir != null ) {
				if ( !dir.exists() ) {
					if ( !dir.mkdirs() ) {
						throw new XLIFFWriterException("Could not create one or more directories for "+file);
					}
				}
			}
			// Create the file
			create(new OutputStreamWriter(
				new BufferedOutputStream(new FileOutputStream(file)), StandardCharsets.UTF_8),
				sourceLang, targetLang);
		}
		catch ( FileNotFoundException e ) {
			throw new XLIFFWriterException(String.format("Cannot create the document (%s).", file), e);
		}
    }

    /**
     * Creates a new XLIFF document
     * @param file the output file where to write this document.
     * @param sourceLang the source language for this document (can be null if set later).
     */
    public void create (File file,
    	String sourceLang)
    {
    	create(file, sourceLang, null);
    }
    
    /**
     * Creates a new XLIFF document.
     * @param output the writer to use to output this document.
     * @param sourceLang the source language for this document (can be null if set later).
     * @param targetLang the target language for this document (can be null).
     */
    public void create (Writer output,
    	String sourceLang,
    	String targetLang)
    {
    	this.sourceLang = sourceLang;
    	this.targetLang = targetLang;
    	writer = new PrintWriter(output);
		indent = "";
		nonUnitLb = (useInsignificantParts ? "" : lb);
		inFile = false;
		inDocument = false;
		autoFileId = 0;
		autoGroupId = 0;
		itsWriter = new ITSWriter();
		
		nsStack = new Stack<>();
		nsStack.push(new NSContext("", Const.NS_XLIFF_CORE20));
//System.out.println("pushINIT:"+nsStack.peek().toString());

		groupStack = new Stack<>();
	}
    
    /**
     * Creates a new document for a given writer and a source language.
     * @param output the writer to use to output this document.
     * @param sourceLang the source language for this document (cannot be null or empty).
     */
    public void create (Writer output,
    	String sourceLang)
    {
    	create(output, sourceLang, null);
    }

    /**
     * Sets the flag indicating whether or not to output original data entries.
     * @param withOriginalData true to output the original data, false otherwise.
     */
    public void setWithOriginalData (boolean withOriginalData) {
   		this.withData = withOriginalData;
    }
    
    /**
     * Gets the flag indicating whether or not to output original data entries.
     * @return true if the output is done with the original data, false otherwise.
     */
    public boolean getWithOriginalData () {
    	return withData;
    }
    
    /**
     * Sets the line break to use for this document.
     * You must set this before calling any of the <code>create()</code> methods.
     * By default the line-break used is the one of the OS.
     * @param lineBreak the line break to use for this document.
     */
    public void setLineBreak (String lineBreak) {
    	lb = lineBreak;
    	nonUnitLb = (useInsignificantParts ? "" : lb);    	
    }
    
    /**
     * Gets the line break to use for this document.
     * @return the line break to use for this document.
     */
    public String getLineBreak () {
    	return lb;
    }
    
    /**
     * Sets whether or not indentations should be used for this document.
     * @param useIndentation true to use indentation, false to not use indentations.
     */
    public void setUseIndentation (boolean useIndentation) {
    	this.useIndentation = useIndentation;
    	indentNonUnit = (useIndentation && !useInsignificantParts);
    	nonUnitLb = (useInsignificantParts ? "" : lb);
    }
    
    public void setUseInsignificantParts (boolean useInsignificantParts) {
    	this.useInsignificantParts = useInsignificantParts;
    	indentNonUnit = (useIndentation && !useInsignificantParts);
    	nonUnitLb = (useInsignificantParts ? "" : lb);
    }

    /**
     * Closes the output document.
     * <p>If {@link #writeEndDocument()} has not been called, it is called automatically.
     */
    @Override
	public void close () {
		if ( writer != null ) {
			if ( inDocument ) {
				writeEndDocument();
			}
			writer.close();
			writer = null;
		}
	}
	
	/**
	 * Writes a given event.
	 * @param event the event to output.
	 */
	public void writeEvent (Event event) {
		switch ( event.getType() ) {
		case START_XLIFF:
			StartXliffData dd = event.getStartXliffData();
			sourceLang = dd.getSourceLanguage();
			if (( dd.getTargetLanguage() == null ) && ( targetLang != null )) {
				dd.setTargetLanguage(targetLang);
			}
			else {
				targetLang = dd.getTargetLanguage();
			}
			writeStartDocument(dd, null);
			break;
			
		case START_FILE:
			writeStartFile(event.getStartFileData());
			break;
			
		case SKELETON:
			writeSkeleton(event.getSkeletonData());
			break;
			
		case MID_FILE:
			writeMidFile(event.getMidFileData());
			break;

		case START_GROUP:
			writeStartGroup(event.getStartGroupData());
			break;
			
		case TEXT_UNIT:
			writeUnit(event.getUnit());
			break;
			
		case END_GROUP:
			writeEndGroup();
			break;
			
		case END_FILE:
			writeEndFile();
			break;
			
		case END_XLIFF:
			writeEndDocument();
			break;
			
		case INSIGNIFICANT_PART:
			if ( useInsignificantParts ) {
				writeText(event.getInsingnificantPartData().getData());
			}
			break;
			
		case END_DOCUMENT:
		case START_DOCUMENT:
			break;
		}
	}
	
	/**
	 * Writes a raw text, only \n is converted to the output line-break.
	 * @param text the text to write.
	 */
	private void writeText (String text) {
		writer.print(text.replace("\n", lb));
	}
	
	/**
	 * Writes a <code>&lt;unit></code> element.
	 * <p>If {@link #writeStartFile(StartFileData)} has not been called yet, it is called automatically.
	 * @param unit the {@link Unit} object to write.
	 */
	public void writeUnit (Unit unit) {
		// Check if there is something to write
		if ( unit.getPartCount() == 0 ) {
			return; // Do not output the unit
		}
		// A unit must have at least one part 
		if ( unit.getSegmentCount() == 0 ) {
			// create a default segment
			unit.appendSegment();
		}

		if ( !inFile ) writeStartFile(null);

		// Start
		context.push(new InheritedData(unit));
		// Namespace context
		nsStack.push(nsStack.peek().clone());
//System.out.println("push<unit>:"+nsStack.peek().toString());
		
		writer.print(indent+"<unit id=\""+Util.toXML(unit.getId(), true)+"\"");
		writeInheritedAttributes(unit);
		
		// Name
		if ( unit.getName() != null ) {
			writer.print(" "+Const.ATTR_NAME+"=\""+Util.toXML(unit.getName(), true)+"\"");
		}
		// Type
		if ( unit.getType() != null ) {
			writer.print(" "+Const.ATTR_TYPE+"=\""+Util.toXML(unit.getType(), true)+"\"");
		}
		
		// ITS attributes (if any)
		AnnotatorsRef unitAR = context.peek().getAnnotatorsRef();
		AnnotatorsRef.update(unitAR, unit);
		AnnotatorsRef parentAR = null;
		if ( context.size() > 1) parentAR = context.elementAt(context.size()-2).getAnnotatorsRef();
		writer.print(itsWriter.outputAttributes(unit, unitAR, parentAR));
		
		// Extension attributes
		writeExtAttributes(unit);
		writer.print(">"+lb);
		if ( useIndentation ) indent += " ";
		
		// Unit-level stand-off entries for ITS
		writer.print(itsWriter.outputStandOffElements(indent, lb, unit));
		
		writeMatches(unit);
		writeGlossary(unit);
		writeMetadata(unit);
		writeValidation(unit);
		writeChangeTracking(unit);

		// Unit-level extension elements
		writeExtElements(unit);
		
		// Unit-level notes if needed
		writeNotes(unit);
		
		// Unit-level original data store
		if ( withData ) {
			writeOriginalData(unit.getStore());
		}
		
		for ( Part part : unit ) {
			Segment seg = null;
			if ( part.isSegment() ) {
				seg = (Segment)part;
				writer.print(indent+"<"+Const.ELEM_SEGMENT);
				if ( seg.getId() != null ) {
					writer.print(" id=\"" + seg.getId() + "\"");
				}
				if ( seg.getCanResegment() != context.peek().getCanResegment() ) {
					writer.print(" " + Const.ATTR_CANRESEGMENT+"=\"" + 
						(seg.getCanResegment() ? Const.VALUE_YES : Const.VALUE_NO) + "\"");
				}
				if ( !seg.getState().equals(Segment.STATE_DEFAULT) || ( seg.getSubState() != null )) {
					writer.print(" " + Const.ATTR_STATE+"=\"" + seg.getState().toString() + "\""); 
				}
				if ( seg.getSubState() != null ) {
					writer.print(" " + Const.ATTR_SUBSTATE+"=\"" + Util.toXML(seg.getSubState(), true) + "\"");
				}
			}
			else {
				writer.print(indent+"<"+Const.ELEM_IGNORABLE);
				if ( part.getId() != null ) {
					writer.print(" id=\"" + part.getId() + "\"");
				}
			}
			
			writer.print(">"+lb);
			if ( useIndentation ) indent += " ";
			
			// Source
			writeFragment(Const.ELEM_SOURCE, part.getSource(), 0, part.getPreserveWS(), unit.getSourceDir());
			// Target
			if ( part.hasTarget() ) {
				writeFragment(Const.ELEM_TARGET, part.getTarget(GetTarget.CREATE_EMPTY), part.getTargetOrder(),
					part.getPreserveWS(), unit.getTargetDir());
			}
			
			if ( useIndentation ) indent = indent.substring(1);
			if ( seg != null ) writer.print(indent+"</segment>"+lb);
			else writer.print(indent+"</ignorable>"+lb);
		}

		// Use non-unit formatting for the data after
		if ( indentNonUnit ) indent = indent.substring(1);
		writer.print(indent+"</unit>"+nonUnitLb);
		context.pop();
		nsStack.pop();
//System.out.println("pop</unit>:"+nsStack.peek().toString());
	}

	private void writeChangeTracking (IWithChangeTrack parent) {
		if ( parent.hasChangeTrack() ) {
			writer.print(indent + ChangeTrack.getCompleteOpeningTag(true) + lb);
			if ( useIndentation ) {
				indent += " ";
			}
			for ( Revisions revs : parent.getChangeTrack() ) {
				writeRevisions(revs);
			}
			if ( indentNonUnit ) {
				indent = indent.substring(1);
			}
			writer.print(indent + ChangeTrack.getClosingTag() + lb);
		}
	}

	private void writeRevisions (Revisions revisions) {
		writer.print(indent + "<" + revisions.getOpeningTagName());
		writer.print(revisions.getAttributesString());
		writeExtAttributes(revisions);
		writer.print(">" + lb);
		if ( useIndentation ) {
			indent += " ";
		}
		for ( Revision rev : revisions ) {
			writeRevision(rev);
		}
		if ( indentNonUnit ) {
			indent = indent.substring(1);
		}
		writer.print(indent + revisions.getClosingTag() + lb);
	}

	private void writeRevision (Revision revision) {
		writer.print(indent + "<" + revision.getOpeningTagName());
		writer.print(revision.getAttributesString());
		writeExtAttributes(revision);
		writer.print(">" + lb);
		if ( useIndentation ) {
			indent += " ";
		}
		for ( Item item : revision ) {
			writeItem(item);
		}
		if ( indentNonUnit ) {
			indent = indent.substring(1);
		}
		writer.print(indent + revision.getClosingTag() + lb);
	}

	private void writeItem (Item item) {
		writer.print(indent + "<" + item.getOpeningTagName());
		writer.print(item.getAttributesString());
		writeExtAttributes(item);
		writer.print(">");
		writer.print(item.getText());
		writer.print(item.getClosingTag() + lb);
	}

	private void writeMatches (Unit unit) {
		if ( !unit.hasMatch() ) return;
		writer.print(indent+"<mtc:matches xmlns:mtc=\""+Const.NS_XLIFF_MATCHES20+"\">"+lb);
		for ( Match match : unit.getMatches() ) {
			writer.print(indent+"<mtc:match ref=\""+match.getRef()+"\"");
			if ( match.getId() != null ) writer.print(" id=\""+match.getId()+"\"");
			if ( !match.getType().equals(Match.DEFAULT_TYPE)
				|| ( match.getSubType() != null )) writer.print(" type=\""+match.getType()+"\"");
			if ( match.getSubType() != null ) writer.print(" subType=\""+Util.toXML(match.getSubType(), true)+"\"");
			if ( match.getSimilarity() != null ) writer.print(" similarity=\""+match.getSimilarity()+"\"");
			if ( match.getMatchQuality() != null ) writer.print(" matchQuality=\""+match.getMatchQuality()+"\"");
			if ( match.getMatchSuitability() != null ) writer.print(" matchSuitability=\""+match.getMatchSuitability()+"\"");
			if ( match.getOrigin() != null ) writer.print(" origin=\""+match.getOrigin()+"\"");
			writer.print(AnnotatorsRef.printDCIfDifferent(DataCategories.MTCONFIDENCE, match.getAnnotatorRef(),
				context.peek().getAnnotatorsRef()));
			writeExtAttributes(match);
			writer.print(">"+lb);
			
			// metadata
			writeMetadata(match);
			// originalData
			if ( withData ) {
				writeOriginalData(match.getStore());
			}
			// Source and target
			writeFragment("source", match.getSource(), 0, false, Directionality.INHERITED);
			writeFragment("target", match.getTarget(), 0, false, Directionality.INHERITED);
			// Extension elements
			writeExtElements(match);
			writer.print(indent+"</mtc:match>"+lb);
		}
		writer.print("</mtc:matches>"+lb);
	}
		
	private void writeGlossary (Unit unit) {
		if ( !unit.hasGlossEntry() ) return;
		
		nsStack.push(nsStack.peek().clone());

		writer.print(indent+"<gls:glossary xmlns:gls=\""+Const.NS_XLIFF_GLOSSARY20+"\">"+lb);
		for ( GlossEntry entry : unit.getGlossary() ) {
			nsStack.push(nsStack.peek().clone());
			writer.print(indent+"<gls:glossEntry");
			if ( entry.getId() != null ) writer.print(" id=\""+entry.getId()+"\"");
			if ( entry.getRef() != null ) writer.print(" ref=\""+entry.getRef()+"\"");
			writeExtAttributes(entry);
			writer.print(">"+lb);
			
			// Term
			writer.print(indent+"<gls:term");
			if ( entry.getTerm().getSource() != null )
				writer.print(" source=\""+entry.getTerm().getSource()+"\"");
			writeExtAttributes(entry.getTerm());
			writer.print(">"+Util.toXML(entry.getTerm().getText(), false)+"</gls:term>"+lb);

			// Translations
			for ( Translation trans : entry ) {
				writer.print(indent+"<gls:translation");
				if ( trans.getId() != null ) writer.print(" id=\""+trans.getId()+"\"");
				if ( trans.getRef() != null ) writer.print(" ref=\""+trans.getRef()+"\"");
				if ( trans.getSource() != null ) writer.print(" source=\""+trans.getSource()+"\"");
				writeExtAttributes(trans);
				writer.print(">"+Util.toXML(trans.getText(), false)+"</gls:translation>"+lb);
			}
			
			// Definition
			if (( entry.getDefinition() != null ) && ( entry.getDefinition().getText() != null )) {
				writer.print(indent+"<gls:definition");
				if ( entry.getDefinition().getSource() != null )
					writer.print(" source=\""+entry.getDefinition().getSource()+"\"");
				writeExtAttributes(entry.getDefinition());
				writer.print(">"+Util.toXML(entry.getDefinition().getText(), false)+"</gls:definition>"+lb);
			}
			// Extension elements
			writeExtElements(entry);
			writer.print(indent+"</gls:glossEntry>"+lb);
			nsStack.pop();
		}
		writer.print("</gls:glossary>"+lb);
		nsStack.pop();
	}

	private void writeMetadata (IWithMetadata parent) {
		if ( !parent.hasMetadata() ) return;
		NSContext nsCtx = nsStack.push(nsStack.peek().clone());
		String ns = "";
		String prefix = nsCtx.getPrefix(Const.NS_XLIFF_METADATA20);
		if ( prefix == null ) {
			// Not in scope so we write it
			prefix = Const.PREFIX_METADATA;
			ns = " xmlns:"+prefix+"=\""+Const.NS_XLIFF_METADATA20+"\"";
			// Make sure it is now in the context
			nsCtx.put(prefix, Const.NS_XLIFF_METADATA20);
		}
		writer.print(indent+"<"+prefix+":metadata");
		Metadata md = parent.getMetadata();
		if ( md.getId() != null ) writer.print(" id=\""+md.getId()+"\"");
		writer.print(ns+">"+lb);
		// Write the content
		for ( MetaGroup group : md ) {
			writeMetaGroup(group, prefix);
		}
		writer.print("</"+prefix+":metadata>"+lb);
		nsStack.pop();
	}
	
	private void writeMetaGroup (MetaGroup group,
		String prefix)
	{
		writer.print(indent+"<"+prefix+":metaGroup");
		if ( group.getId() != null ) writer.print(" id=\""+group.getId()+"\"");
		if ( group.getCategory() != null ) writer.print(" category=\""+Util.toXML(group.getCategory(), true)+"\"");
		if ( group.getAppliesTo() != MetaGroup.AppliesTo.UNDEFINED ) {
			writer.print(" appliesTo=\""+group.getAppliesTo().toString()+"\"");
		}
		writer.print(">"+lb);
		// Go through the list of items for this group
		for ( IMetadataItem item : group ) {
			if ( item.isGroup() ) {
				writeMetaGroup((MetaGroup)item, prefix);
			}
			else {
				Meta m = (Meta)item;
				writer.print(indent+"<"+prefix+":meta");
				if ( m.getType() != null ) writer.print(" type=\""+Util.toXML(m.getType(), true)+"\"");
				writer.print(">");
				writer.print(Util.toXML(m.getData(), false)); // No cp element in meta
				writer.print("</"+prefix+":meta>"+lb);
			}
		}
		writer.print(indent+"</"+prefix+":metaGroup>"+lb);
	}

	private void writeValidation (IWithValidation parent) {
		if ( !parent.hasValidation() ) return;
		Validation validation = parent.getValidation();
		
		// First check if there are non-inherited rules
		// If not: there is nothing to write
		if ( !validation.hasNonInheritedRule() ) return;

		// If there are non-inherited rules, then we can write
		NSContext nsCtx = nsStack.push(nsStack.peek().clone());
		String ns = "";
		String prefix = nsCtx.getPrefix(Const.NS_XLIFF_VALIDATION20);
		if ( prefix == null ) {
			// Not in scope so we write it
			prefix = Const.PREFIX_VALIDATION;
			ns = " xmlns:"+prefix+"=\""+Const.NS_XLIFF_VALIDATION20+"\"";
			// Make sure it is now in the context
			nsCtx.put(prefix, Const.NS_XLIFF_VALIDATION20);
		}
		
		writer.print(indent+"<"+prefix+":validation");
		writeExtAttributes(validation);
		writer.print(ns+">"+lb);
		// Write the content
		for ( Rule rule : validation ) {
			writeValidationRule(rule, prefix);
		}
		writer.print("</"+prefix+":validation>"+lb);
		nsStack.pop();
	}
	
	private void writeValidationRule (Rule rule,
		String prefix)
	{
		if ( rule.isInherited() ) return; // Skip inherited rules
		nsStack.push(nsStack.peek().clone());
		writer.print(indent+"<"+prefix+":rule");
		if ( rule.getType() != Type.CUSTOM ) {
			writer.print(" "+rule.getType()+"=\""+Util.toXML(rule.getData(), true)+"\"");
		}
		if ( !rule.isEnabled() ) {
			writer.print(" disabled=\""+(rule.isEnabled() ? "no" : "yes" )+"\"");
		}
		if ( !rule.isCaseSensitive() ) {
			writer.print(" caseSensitive=\""+(rule.isCaseSensitive() ? "yes" : "no" )+"\"");
		}
		if ( rule.getNormalization() != Normalization.NFC ) {
			writer.print(" normalization=\""+rule.getNormalization().toString()+"\"");
		}
		// Type-specific attributes
		switch ( rule.getType() ) {
		case CUSTOM:
		case ISNOTPRESENT:
			// Do nothing
			break;
		case ISPRESENT:
			if ( rule.getOccurs() > 0 ) {
				writer.print(" occurs=\""+rule.getOccurs()+"\"");
			}
			// Then fall thru
		case ENDSWITH:
		case STARTSWITH:
			if ( rule.getExistsInSource() ) {
				writer.print(" existsInSource=\""+(rule.getExistsInSource() ? "yes" : "no" )+"\"");
			}
			break;
		}
		// Extension attributes
		writeExtAttributes(rule);
		writer.print("/>"+lb);
		nsStack.pop();
	}
	
	private void writeExtAttributes (IWithExtAttributes parent) {
		if ( !parent.hasExtAttribute() ) return;
		if ( extWriter == null ) {
			extWriter = new ExtensionsWriter(getLineBreak());
		}
		writer.print(extWriter.buildExtAttributes(parent.getExtAttributes(), nsStack));
	}
	
	/**
	 * Writes the notes for an object that implements the {@link IWithNotes} interface.
	 * @param parent the object with the notes.
	 */
	private void writeNotes (IWithNotes parent) {
		if ( parent.getNoteCount() == 0 ) {
			return;
		}
		Notes notes = parent.getNotes();
		writer.print(indent+"<notes");
		nsStack.push(nsStack.peek().clone());
		writeExtAttributes(notes);
		writer.print(">"+lb);
		if ( useIndentation ) indent += " ";
		
		for ( Note note : notes ) {
			writer.print(indent+"<note");
			nsStack.push(nsStack.peek().clone());
			// Id
			if ( !Util.isNoE(note.getId()) ) {
				writer.print(" id=\""+Util.toXML(note.getId(), true)+"\"");
			}
			// AppliesTo
			switch ( note.getAppliesTo() ) {
			case SOURCE:
				writer.print(" appliesTo=\"source\"");
				break;
			case TARGET:
				writer.print(" appliesTo=\"target\"");
				break;
			case UNDEFINED:
				// This is the default,no need to output it
				break;
			}
			// Priority
			if ( note.getPriority() != 1 ) {
				writer.print(" priority=\""+note.getPriority()+"\"");
			}
			// Category
			if ( note.getCategory() != null ) {
				writer.print(" category=\""+Util.toXML(note.getCategory(), true)+"\"");
			}
			// Extension attributes
			writeExtAttributes(note);
			// Ending
			writer.print(">"+Util.toXML(note.getText(), false));
			writer.print("</note>"+lb);
			nsStack.pop();
		}
		
		if ( useIndentation ) indent = indent.substring(1);
		writer.print(indent+"</notes>"+lb);
		nsStack.pop();
	}
	
	private void writeExtElements (IWithExtElements parent) {
		if ( !parent.hasExtElements() ) return;
		if ( extWriter == null ) {
			extWriter = new ExtensionsWriter(getLineBreak());
		}
		writer.print(extWriter.buildExtElements(parent, nsStack));
	}
	
	/**
	 * Writes the start of the document (the <code>&lt;xliff></code> tag).
	 * @param docData the document data.
	 * @param comment an optional comment that is placed just after the <code>&lt;xliff></code> tag
	 * (use null for no comment).
	 */
	public void writeStartDocument (StartXliffData docData,
		String comment)
	{
		if ( docData == null ) {
			docData = new StartXliffData("2.0");
		}
		else {
			// If possible: update the source and target language from the original document
			if ( docData.getSourceLanguage() != null ) {
				sourceLang = docData.getSourceLanguage();
			}
			if ( docData.getTargetLanguage() != null ) {
				targetLang = docData.getTargetLanguage();
			}
		}
		writer.print("<?xml version=\"1.0\"?>"+lb);
		writer.print("<xliff xmlns=\""+Const.NS_XLIFF_CORE20+"\" version=\""+docData.getVersion()+"\"");
		
		// Namespace context
		nsStack.push(nsStack.peek().clone());
//System.out.println("push<xliff>:"+nsStack.peek().toString());
		
    	if ( Util.isNoE(sourceLang) ) {
    		throw new InvalidParameterException("Source language cannot be null or empty.");
    	}
		writer.print(" "+Const.ATTR_SRCLANG+"=\""+sourceLang+"\"");
		if ( !Util.isNoE(targetLang) ) {
			writer.print(" "+Const.ATTR_TRGLANG+"=\""+targetLang+"\"");
		}
		writeExtAttributes(docData);
		writer.print(">"+nonUnitLb);
		if ( indentNonUnit ) indent += " ";
		inDocument = true;

		// Extra comment at the top if needed
		if ( !Util.isNoE(comment) ) {
			writer.print(indent+"<!-- " + Util.toXML(comment, false) + " -->"+nonUnitLb);
		}
	}
	
	/**
	 * Writes the end of the document (the <code>&lt;/xliff></code> tag).
	 * <p>If {@link #writeEndFile()} has not been called, it is called automatically.
	 */
	public void writeEndDocument () {
		if ( inFile ) {
			writeEndFile();
		}
		if ( inDocument ) {
			if ( indentNonUnit ) indent = indent.substring(1);
			writer.print("</xliff>"+nonUnitLb);
			nsStack.pop();
//System.out.println("pop</xliff>:"+nsStack.peek().toString());
			inDocument = false;
		}
	}
	
	/**
	 * Sets the {@link StartFileData} object to use for the next {@link #writeStartFile(StartFileData)} call.
	 * You can use this method to set the file-level data to use next time the {@link #writeStartFile(StartFileData)}
	 * method is automatically call or called with a null parameter. This can be use to avoid writing the start of the
	 * &lt;file> element directly in case there is no data to extract for example.
	 * This object stays available until the next {@link #writeEndFile()} call.
	 * @param startFileData the object to use for the next {@link #writeStartFile(StartFileData)} call.
	 */
	public void setStartFileData (StartFileData startFileData) {
		this.startFileData = startFileData;
	}
	
	/**
	 * Writes the start of a &lt;file> element.
	 * <p>If needed {@link #writeStartDocument(StartXliffData, String)} is called automatically before.
	 * @param newFileData the data associated with the start of a XLIFF &lt;file> element (can be null).
	 * You can use {@link #setStartFileData(StartFileData)} to set this parameter before calling this
	 * method (then use null here).
	 */
	public void writeStartFile (StartFileData newFileData) {
		if ( !inDocument ) writeStartDocument(null, null);

		// Create a file data if none is provided
		if ( startFileData == null ) {
			if ( newFileData == null ) {
				startFileData = new StartFileData(null);
			}
			else startFileData = newFileData;
		}
		// Ensure file id
		if ( startFileData.getId() == null ) {
			autoFileId++;
			startFileData.setId("f"+autoFileId);
		}

		// Start
		context = new Stack<>();
		context.push(new InheritedData()); // Defaults
		context.push(new InheritedData(startFileData)); // Then the context for this file
		// Namespace context
		nsStack.push(nsStack.peek().clone());
//System.out.println("push<file>:"+nsStack.peek().toString());
		
		writer.print(indent+"<"+Const.ELEM_FILE);
		
		// Id (required)
		writer.print(" id=\""+Util.toXML(startFileData.getId(), true)+"\"");
		
		writeInheritedAttributes(startFileData);
		
		// Original
		if ( !Util.isNoE(startFileData.getOriginal()) ) {
			writer.print(" original=\""+Util.toXML(startFileData.getOriginal(), true)+"\"");
		}
		
		// Extension attributes
		writeExtAttributes(startFileData);

		// End
		writer.print(">"+nonUnitLb);
		if ( indentNonUnit ) indent += " ";
		inFile = true;
	}
	
	/**
	 * Writes the mid part of an &lt;file> document.
	 * <p>If needed {@link #writeStartFile(StartFileData)} is called automatically before.
	 * @param midFileData the data associated with a mid part of a &lt;file> element (can be null).
	 */
	public void writeMidFile (MidFileData midFileData) {
		if ( !inFile ) writeStartFile(null);
		if ( midFileData != null ) {
			writeExtElements(midFileData);
			writeMetadata(midFileData);
			writeValidation(midFileData);
			writeChangeTracking(midFileData);
			writeNotes(midFileData);
		}
	}
	
	private void writeInheritedAttributes (InheritedData data) {
		// Get the parent context (size()-1 is the current)
		InheritedData parentData = context.elementAt(context.size()-2);
		
		// CanResegment
		if ( data.getCanResegment() != parentData.getCanResegment() ) {
			writer.print(" canResegment=\""
				+ (data.getCanResegment() ? Const.VALUE_YES : Const.VALUE_NO)
				+ "\"");
		}
		
		// Translate
		if ( data.getTranslate() != parentData.getTranslate() ) {
			writer.print(" translate=\""
				+ (data.getTranslate() ? Const.VALUE_YES : Const.VALUE_NO)
				+ "\"");
		}
		
		// Source dDirectionality
		writeDirectionality(Const.ATTR_SRCDIR, data.getSourceDir(), parentData.getSourceDir());

		// Target dDirectionality
		writeDirectionality(Const.ATTR_TRGDIR, data.getTargetDir(), parentData.getTargetDir());
	}
	
	/**
	 * Writes the <skeleton> element.
	 * <p>If {@link #writeStartFile(StartFileData)} has not been called, it is called automatically.
	 * <p>If the <code>skeleton</code> attribute was used in <code>&lt;file></code>, an error occurs.
	 * @param skelData the {@link Skeleton} object (must not be null).
	 * @throws XLIFFWriterException if an error occurs.
	 */
	public void writeSkeleton (Skeleton skelData) {
		if ( !inFile ) writeStartFile(null);
		// Do we need the skeleton?
		List<IExtChild> list = skelData.getChildren();
		if ((( list != null ) && list.isEmpty()) && ( skelData.getHref() == null )) {
			// Do not write an empty skeleton
			return;
		}
		// Start tag
		writer.print(indent+"<"+Const.ELEM_SKELETON);
		if ( indentNonUnit ) indent += " ";
		// Output either href (if present) or the content.
		if ( skelData.getHref() != null ) {
			writer.print(" href=\""+Util.toXML(skelData.getHref(), true)+"\">");
		}
		else {
			writer.print(">");
			// Content
			if ( extWriter == null ) {
				extWriter = new ExtensionsWriter(getLineBreak());
			}
			writer.print(extWriter.buildExtChildren(skelData.getChildren(), null));
		}
		// End tag
		if ( indentNonUnit ) indent = indent.substring(1);
		writer.print("</"+Const.ELEM_SKELETON+">"+nonUnitLb);
	}
	
	/**
	 * Writes the end of the file (the <code>&lt;/file></code> tag).
	 * <p>Nothing is written if no file has been started.
	 */
	public void writeEndFile () {
		if ( inFile ) {
			while ( groupStack.size() > 0 ) {
				writeEndGroup();
			}
			if ( indentNonUnit ) indent = indent.substring(1);
			writer.print(indent+"</file>"+nonUnitLb);
			nsStack.pop();
//System.out.println("pop</file>:"+nsStack.peek().toString());
			
			inFile = false;
			startFileData = null;
		}
	}
	
	/**
	 * Writes the start of a &lt;group> element.
	 * <p>If needed {@link #writeStartFile(StartFileData)} is called before.
	 * @param startGroupData the data associated with the group (can be null).
	 */
	public void writeStartGroup (StartGroupData startGroupData) {
		if ( !inFile ) writeStartFile(null);

		// Create a default group data if needed
		if ( startGroupData == null ) {
			autoGroupId++;
			startGroupData = new StartGroupData("g"+autoGroupId);
		}
		
		// Start
		context.push(new InheritedData(startGroupData));
		// Namespace context
		nsStack.push(nsStack.peek().clone());
//System.out.println("push<group>:"+nsStack.peek().toString());
		
		writer.print(indent+"<"+Const.ELEM_GROUP);
		
		// Id
		if ( startGroupData.getId() != null ) {
			writer.print(" "+Const.ATTR_ID+"=\""+Util.toXML(startGroupData.getId(), true)+"\"");
		}
		
		writeInheritedAttributes(startGroupData);
		
		// Name
		if ( startGroupData.getName() != null ) {
			writer.print(" "+Const.ATTR_NAME+"=\""+Util.toXML(startGroupData.getName(), true)+"\"");
		}

		// Type
		if ( startGroupData.getType() != null ) {
			writer.print(" "+Const.ATTR_TYPE+"=\""+Util.toXML(startGroupData.getType(), true)+"\"");
		}

		// Extension attributes
		writeExtAttributes(startGroupData);
		
		writer.print(">"+nonUnitLb);
		if ( indentNonUnit ) indent += " ";
		groupStack.push(startGroupData);
		
		writeExtElements(startGroupData);
		writeMetadata(startGroupData);
		writeValidation(startGroupData);
		writeChangeTracking(startGroupData);
		writeNotes(startGroupData);
	}
	
	/**
	 * Writes the end of a &lt;group> element.
	 */
	public void writeEndGroup () {
		// Close this group
		groupStack.pop();
		context.pop();
		if ( indentNonUnit ) indent = indent.substring(1);
		writer.print(indent+"</"+Const.ELEM_GROUP+">"+nonUnitLb);
		nsStack.pop();
//System.out.println("pop</group>:"+nsStack.peek().toString());
	}
	
	/**
	 * Writes a fragment.
	 * @param name the name of the element (source or target)
	 * @param fragment the fragment.
	 * @param order the order information (must be 0 or less for source fragments)
	 * @param preserveSpace true if xml:space should be set to 'preserve'
	 * @param inheritedDir the inherited directionality
	 */
	private void writeFragment (String name,
		Fragment fragment,
		int order,
		boolean preserveSpace,
		Directionality inheritedDir) 
	{
		if ( order > 0 ) {
			writer.print(indent+String.format("<%s order=\"%d\"", name, order));
		}
		else {
			writer.print(indent+"<"+name);
		}
		//TODO: write/don't write xml:space based on context
		if ( preserveSpace ) writer.print(" xml:space=\"preserve\"");
		writer.print(">"+fragment.toXLIFF(nsStack, context, withData));
		writer.print("</"+name+">"+lb);
	}
	
	private void writeOriginalData (Store store) {
		if ( !store.hasCTagWithData() ) {
			return; // Nothing to write out
		}
		
		// Else: write the data
		store.calculateDataToIdsMap();
		Map<String, String> map = store.getOutsideRepresentationMap();

		writer.print(indent+"<"+Const.ELEM_ORIGINALDATA+">"+lb);
		if ( useIndentation ) indent += " ";

		for ( String originalDataKey : map.keySet() ) {
			String id = map.get(originalDataKey); // The original data is the key during output
			writer.print(indent+"<"+Const.ELEM_DATA+" "+Const.ATTR_ID+"=\""+id+"\"");
			// Directionality (default is AUTO)
			switch ( originalDataKey.charAt(originalDataKey.length()-1) ) {
			case 'r':
				writer.print(" "+Const.ATTR_DIR+"=\"rtl\"");
				break;
			case 'l':
				writer.print(" "+Const.ATTR_DIR+"=\"ltr\"");
				break;
			}
			// Data itself (minus the direction flag)
			writer.print(">"+Util.toSafeXML(originalDataKey.substring(0, originalDataKey.length()-1)));
			writer.print("</"+Const.ELEM_DATA+">"+lb);
		}

		if ( useIndentation ) indent = indent.substring(1);
		writer.print(indent+"</"+Const.ELEM_ORIGINALDATA+">"+lb);
		store.setOutsideRepresentationMap(map);
	}

	/**
	 * Gets the namespace context for this writer.
	 * @return the namespace context for this writer.
	 */
	public NamespaceContext getNamespaceContext () {
		return nsStack.peek();
	}

	private void writeDirectionality (String name,
		Directionality value,
		Directionality context)
	{
		if ( value == context ) return; // Not needed
		writer.print(String.format(" %s=\"%s\"", name, value.toString()));
	}

}
