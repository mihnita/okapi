/*
 * =============================================================================
 *   Copyright (C) 2010-2019 by the Okapi Framework contributors
 * -----------------------------------------------------------------------------
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * =============================================================================
 */

package net.sf.okapi.common.annotation;

import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.okapi.common.encoder.EncoderContext;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.exceptions.OkapiIOException;

/**
 * Represents the annotation of the XLIFF 1.2 context-group element.
 */
public class XLIFFContextGroup implements IAnnotation, Iterable<XLIFFContextGroup.Context> {
    public static final String ELEMENT_NAME = "context-group";
    public static final String NAME = "name";
    public static final String PURPOSE = "purpose";
    public static final String CRC = "crc";

    private final EncoderManager encoderManager;
    private final String name;
    private final String purpose;
    private final String crc;
    private final List<XLIFFContextGroup.Context> contexts;

    private String endElementPrependingText;

    public XLIFFContextGroup(
        final EncoderManager encoderManager,
        final String name,
        final String purpose,
        final String crc,
        final List<XLIFFContextGroup.Context> contexts
    ) {
        this.encoderManager = encoderManager;
        this.name = name;
        this.purpose = purpose;
        this.crc = crc;
        this.contexts = contexts;
    }

    public String name() {
        return name;
    }

    public String purpose() {
        return purpose;
    }

    public String crc() {
        return crc;
    }

    @Override
    public Iterator<XLIFFContextGroup.Context> iterator() {
        return this.contexts.iterator();
    }

    public void addContext(final XLIFFContextGroup.Context context) {
        this.contexts.add(context);
    }

    public void readWith(final XMLStreamReader streamReader) throws XMLStreamException {
        StringBuilder prependingText = new StringBuilder();
        while (streamReader.hasNext()) {
            final int eventType = streamReader.next();
            switch (eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    if (XLIFFContextGroup.Context.ELEMENT_NAME.equals(streamReader.getLocalName())) {
                        final XLIFFContextGroup.Context context = new XLIFFContextGroup.Context(
                            this.encoderManager,
                            streamReader.getAttributeValue(null, XLIFFContextGroup.Context.TYPE),
                            streamReader.getAttributeValue(null, XLIFFContextGroup.Context.MATCH_MANDATORY),
                            streamReader.getAttributeValue(null, XLIFFContextGroup.Context.CRC)
                        );
                        context.readWith(streamReader);
                        context.prependingText(prependingText.toString());
                        prependingText = new StringBuilder();
                        addContext(context);
                    } else {
                        throw new OkapiIOException("Unexpected element: ".concat(streamReader.getLocalName()));
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.SPACE:
                case XMLStreamConstants.CDATA:
                    prependingText.append(streamReader.getText());
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (XLIFFContextGroup.ELEMENT_NAME.equals(streamReader.getLocalName())) {
                        this.endElementPrependingText = prependingText.toString();
                        return;
                    } else {
                        throw new OkapiIOException("Unexpected element: ".concat(streamReader.getLocalName()));
                    }
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(XLIFFContextGroup.ELEMENT_NAME);
        if (null != this.name || null != this.purpose || null != this.crc) {
            appendAttributes(sb);
        }
        sb.append(">");
        for (final XLIFFContextGroup.Context context : this.contexts) {
            sb.append(context.toString());
        }
        if (null != this.endElementPrependingText) {
            sb.append(this.endElementPrependingText);
        }
        sb.append("</");
        sb.append(XLIFFContextGroup.ELEMENT_NAME);
        sb.append(">");
        return sb.toString();
    }

    private void appendAttributes(final StringBuilder sb) {
        if (null != this.name) {
            sb.append(" ");
            appendAttribute(sb, XLIFFContextGroup.NAME, this.name);
        }
        if (null != this.purpose) {
            sb.append(" ");
            appendAttribute(sb, XLIFFContextGroup.PURPOSE, this.purpose);
        }
        if (null != this.crc) {
            sb.append(" ");
            appendAttribute(sb, XLIFFContextGroup.CRC, this.crc);
        }
    }

    private void appendAttribute(final StringBuilder sb, final String name, final String value) {
        sb.append(name);
        sb.append("=");
        sb.append("\"");
        sb.append(encode(encoderManager, value, EncoderContext.INLINE));
        sb.append("\"");
    }

    private static String encode(EncoderManager encoder, String value, EncoderContext context) {
		return encoder != null ? encoder.encode(value, context) : value;
    }

    /**
     * Represents the context of the XLIFF 1.2 context-group element.
     */
    public static class Context {
        public static final String ELEMENT_NAME = "context";
        public static final String TYPE = "context-type";
        public static final String MATCH_MANDATORY = "match-mandatory";
        public static final String CRC = "crc";
        
        public enum StandardContextTypes {database, element, elementtitle, linenumber, numparams, paramnotes, record, recordtitle, sourcefile}
        
        private final EncoderManager encoderManager;
        private final String type;
        private final String matchMandatory;
        private final String crc;

        private String value;
        private String prependingText;

        public Context(
            final EncoderManager encoderManager,
            final String type,
            final String matchMandatory,
            final String crc
        ) {
            this.encoderManager = encoderManager;
            this.type = type;
            this.matchMandatory = matchMandatory;
            this.crc = crc;
        }

        public String type() {
            return this.type;
        }

        public String matchMandatory() {
            return this.matchMandatory;
        }

        public String crc() {
            return this.crc;
        }

        public String value() {
            return this.value;
        }

        public void value(final String value) {
            this.value = value;
        }

        public void prependingText(final String prependingText) {
            this.prependingText = prependingText;
        }

        public void readWith(final XMLStreamReader streamReader) throws XMLStreamException {
            final StringBuilder sb = new StringBuilder();
            while (streamReader.hasNext()) {
                final int eventType = streamReader.next();
                switch (eventType) {
                    case XMLStreamConstants.CHARACTERS:
                    case XMLStreamConstants.CDATA:
                    case XMLStreamConstants.SPACE:
                        sb.append(streamReader.getText());
                        break;
                    case XMLStreamConstants.START_ELEMENT:
                        throw new OkapiIOException("Unexpected element: ".concat(streamReader.getLocalName()));
                    case XMLStreamConstants.END_ELEMENT:
                        if (XLIFFContextGroup.Context.ELEMENT_NAME.equals(streamReader.getLocalName())) {
                            value(sb.toString());
                            return;
                        } else {
                            throw new OkapiIOException("Unexpected element: ".concat(streamReader.getLocalName()));
                        }
                }
            }
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            if (null != this.prependingText) {
                sb.append(this.prependingText);
            }
            sb.append("<");
            sb.append(XLIFFContextGroup.Context.ELEMENT_NAME);
            appendAttributes(sb);
            sb.append(">");
            if (null != this.value) {
                sb.append(encode(encoderManager, this.value, EncoderContext.TEXT));
            }
            sb.append("</");
            sb.append(XLIFFContextGroup.Context.ELEMENT_NAME);
            sb.append(">");
            return sb.toString();
        }

		private void appendAttributes(final StringBuilder sb) {
			sb.append(" ");
			String type = this.type;
			try {
				StandardContextTypes.valueOf(this.type);
			} catch (IllegalArgumentException e) {
				if (!this.type.startsWith("x-")) {
					type = "x-" + this.type;
				}
			}

			appendAttribute(sb, XLIFFContextGroup.Context.TYPE, type);
			if (null != this.matchMandatory) {
				sb.append(" ");
				appendAttribute(sb, XLIFFContextGroup.Context.MATCH_MANDATORY, this.matchMandatory);
			}
			if (null != this.crc) {
				sb.append(" ");
				appendAttribute(sb, XLIFFContextGroup.Context.CRC, this.crc);
			}
		}

        private void appendAttribute(final StringBuilder sb, final String name, final String value) {
            sb.append(name);
            sb.append("=");
            sb.append("\"");
            sb.append(encode(encoderManager, value, EncoderContext.INLINE));
            sb.append("\"");
        }
    }
}
