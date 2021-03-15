package net.sf.okapi.filters.html;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextPart;

public class PrintEvents {

	/**
	 * @param args one or more HTML files to parse
	 * @throws URISyntaxException
	 */
	public static void main(String[] args) throws IOException {
	    String configId = null;
	    boolean nextArgIsConfigId = false;
		for(String arg: args) {
		    if(nextArgIsConfigId) {
		        configId = arg;
		        nextArgIsConfigId = false;
		        continue;
		    }

		    if(arg.equals("-fc")) {
		        nextArgIsConfigId = true;
		        continue;
		    }

		    System.out.printf("---------- File: %s ----------%n", arg);
		    try (HtmlFilter htmlFilter = new HtmlFilter();
			     InputStream htmlStream = new FileInputStream(arg)) {
		        if (configId != null ) {
		            htmlFilter.getParameters().load(new FileInputStream(configId), false);
		        }
		        htmlFilter.open(new RawDocument(htmlStream, "UTF-8", LocaleId.fromString("en")));
		        while (htmlFilter.hasNext()) {
		            Event event = htmlFilter.next();
		            System.out.print(event.toString());
		            switch(event.getEventType()) {
		            case RAW_DOCUMENT:
		                System.out.printf(":\"%s\"%n", event.getRawDocument().getInputCharSequence());
		                break;
		            case TEXT_UNIT:
	                    //System.out.printf(":\"%s\"%n", event.getTextUnit().getSource().toString());
		                System.out.printf(":%n");
	                    TextContainer tc = event.getTextUnit().getSource();
	                    List<TextPart> textParts = tc.getParts();
	                    int i = 0;
	                    for (TextPart tp: textParts) {
	                        System.out.printf("  textParts[%d] = {isSegment:%s, annotations:{%s}, ", i, tp.isSegment(), tp.getAnnotations());
	                        if (tp.isSegment()) {
	                            TextFragment seg = tp.getContent();
	                            System.out.printf("text:\"%s\", codedText:\"%s\", codes: [%n",
	                                    seg.getText(), escapeNonAscii(seg.getCodedText()));
	                            int j=0;
	                            for(Code c: seg.getCodes()) {
	                                System.out.printf("\t%d: {id:%d, type:%s, tagType:%s, data:\"%s\"},%n", j, c.getId(), c.getType(), c.getTagType(), c.getData());
	                                ++j;
	                            }
	                            System.out.printf("]}%n");
	                        } else {
	                            System.out.printf("}%n");
	                        }
	                        ++i;
	                    }
		                break;
		            case DOCUMENT_PART:
		                System.out.printf(":\"%s\"%n", event.getDocumentPart().toString());
		                break;
		            default:
		                System.out.println();
		            }
		        }
			}
		}
	}

    public static String escapeNonAscii(String codedString) {
        StringBuilder sb = new StringBuilder();
        for(char c: codedString.toCharArray()) {
            if (c >= (char) 0x0100 ) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
