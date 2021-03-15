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

import java.util.Map;
import java.util.Stack;

import net.sf.okapi.lib.xliff2.Const;
import net.sf.okapi.lib.xliff2.NSContext;
import net.sf.okapi.lib.xliff2.Util;
import net.sf.okapi.lib.xliff2.core.CTag;
import net.sf.okapi.lib.xliff2.core.Fragment;
import net.sf.okapi.lib.xliff2.core.MTag;
import net.sf.okapi.lib.xliff2.core.Tag;
import net.sf.okapi.lib.xliff2.core.TagType;

/**
 * Implements {@link IFragmentObject} for XLIFF 2.
 */
public class XLIFFFragmentObject implements IFragmentObject {
	
	private final Object obj;
	private final Integer status;
	private final Stack<NSContext> nsStack;
	
	/**
	 * Creates a new fragment object for XLIFF.
	 * @param object the object to render.
	 * @param tagsStatus the map of the tags status.
	 * @param nsStack the namespace context.
	 */
	XLIFFFragmentObject (Object object,
		Map<Tag, Integer> tagsStatus,
		Stack<NSContext> nsStack)
	{
		this.obj = object;
		if ( obj instanceof String ) status = null;
		else status = tagsStatus.get(obj);
		this.nsStack = nsStack;
	}

	@Override
	public String render () {
		if ( obj instanceof String ) {
			return render((String)obj);
		}
		else if ( obj instanceof CTag ) {
			return render((CTag)obj);
		}
		else { // It's an MTag
			return render((MTag)obj);
		}
	}

	@Override
	public String getText () {
		return (String)obj;
	}

	@Override
	public CTag getCTag () {
		return (CTag)obj;
	}

	@Override
	public MTag getMTag () {
		return (MTag)obj;
	}

	@Override
	public Object getObject () {
		return obj;
	}

	private String render (String ctext) {
		StringBuilder tmp = new StringBuilder();
		for ( int i=0; i<ctext.length(); i++ ) {
			char ch = ctext.charAt(i);
			if ( ch == Fragment.PCONT_STANDALONE ) {
				// Show something that will not validate
				tmp.append("<INVALID:HIDDEN-PROTECTED-CONTENT/>");
				i++;
			}
			else {
				// In XML 1.0 the valid characters are:
				// #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
				switch ( ch ) {
				case '\r':
					tmp.append("&#13;"); // Literal
					break;
				case '<':
					tmp.append("&lt;");
					break;
				case '&':
					tmp.append("&amp;");
					break;
				case '\n':
				case '\t':
					tmp.append(ch);
					break;
				default:
					if (( ch > 0x001F ) && ( ch < 0xD800 )) {
						// Valid char (most frequent) 
						tmp.append(ch);
					}
					else if ( Character.isHighSurrogate(ch) ) {
						tmp.append(Character.toChars(ctext.codePointAt(i)));
						i++;
					}
					else if (( ch < 0x0020 )
						|| (( ch > 0xD7FF ) && ( ch < 0xE000 ))
						|| ( ch == 0xFFFE )
						|| ( ch == 0xFFFF )) {
						// Invalid characters
						tmp.append(String.format("<cp hex=\"%04X\"/>", (int)ch));
					}
					break;
				}
			}
		}
		return tmp.toString();
	}
	
	private String render (CTag ctag) {
		StringBuilder tmp = new StringBuilder();
		switch ( ctag.getTagType() ) {
		case OPENING:
			if ( status == 2 ) {
				tmp.append("<pc id=\""+ctag.getId()+"\"");
				if ( ctag.getCanOverlap() ) {
					tmp.append(" "+Const.ATTR_CANOVERLAP+"=\"yes\"");
				}
			}
			else {
				tmp.append("<sc id=\""+ctag.getId()+"\"");
				if ( !ctag.getCanOverlap() ) {
					tmp.append(" "+Const.ATTR_CANOVERLAP+"=\"no\"");
				}
				if ( status == 0 ) tmp.append(" isolated=\"yes\"");
			}
			Fragment.printCommonAttributes(ctag, null, tmp, null, false); // closing can be null
			Fragment.printExtAttributes(ctag, tmp, nsStack);
			if ( status == 2 ) tmp.append(">");
			else tmp.append("/>");
			break;
			
		case CLOSING:
			if ( status == 2 ) {
				tmp.append("</pc>");
			}
			else {
				tmp.append("<ec "+(status==0 ? "id" : "startRef")+"=\""+ctag.getId()+"\"");
				if ( !ctag.getCanOverlap() ) {
					tmp.append(" canOverlap=\"no\"");
				}
				if ( status == 0 ) tmp.append(" isolated=\"yes\"");
				Fragment.printCommonAttributes(ctag, null, tmp, null, false);
				Fragment.printExtAttributes(ctag, tmp, nsStack);
				tmp.append("/>");
			}
			break;
			
		case STANDALONE:
			tmp.append(String.format("<ph id=\"%s\"", ctag.getId()));
			Fragment.printCommonAttributes(ctag, null, tmp, null, false);
			Fragment.printExtAttributes(ctag, tmp, nsStack);
			tmp.append("/>");
			break;
		}
		return tmp.toString();
	}
	
	private String render (MTag mtag) {
		StringBuilder tmp = new StringBuilder();
		if ( mtag.getTagType() == TagType.OPENING ) {
			if ( status == 2 ) {
				tmp.append("<mrk id=\""+mtag.getId()+"\"");
			}
			else {
				tmp.append("<sm id=\""+mtag.getId()+"\"");
			}

			// Optional attributes
			if ( !mtag.getType().equals(MTag.TYPE_DEFAULT) ) {
				tmp.append(" type=\""+mtag.getType()+"\"");
			}
			if ( mtag.getTranslate() != null ) {
				tmp.append(" translate=\""+(mtag.getTranslate() ? "yes" : "no")+"\"");
			}
			if ( !Util.isNoE(mtag.getValue()) ) {
				tmp.append(" value=\""+Util.toXML(mtag.getValue(), true)+"\"");
			}
			if ( !Util.isNoE(mtag.getRef()) ) {
				tmp.append(" ref=\""+Util.toXML(mtag.getRef(), true)+"\"");
			}
			
			// ITS attributes
//TODO			
//			if ( itsWriter == null ) itsWriter = new ITSWriter();
//			if ( annotRefs == null ) annotRefs = itsWriter.createAnnotatorsRefList(context);
//			AnnotatorsRef amAR = itsWriter.createAnnotatorsRef(mtag);
//			annotRefs.add(new SimpleEntry<String, AnnotatorsRef>(mtag.getId(), amAR));
//			tmp.append(itsWriter.outputAttributes(mtag, amAR, annotRefs.get(annotRefs.size()-2).getValue()));
			
			// Extension attributes
			Fragment.printExtAttributes(mtag, tmp, nsStack);
			// Closing
			if ( status == 2 ) tmp.append(">");
			else tmp.append("/>");
		}
		else { // Can only be CLOSING for MTag
			if ( status == 2 ) {
				tmp.append("</mrk>");
			}
			else {
				tmp.append("<em startRef=\""+mtag.getId()+"\"/>");
			}
//TODO			
			// But we need to remove the annotator-references item (if one exists)
//			if ( annotRefs != null ) {
//				String id = mtag.getId();
//				for ( SimpleEntry<String, AnnotatorsRef> entry : annotRefs ) {
//					if ( id.equals(entry.getKey()) ) {
//						annotRefs.remove(entry);
//						break;
//					}
//				}
//			}
		}
		return tmp.toString();
	}

}
