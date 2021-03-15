/*===========================================================================
  Copyright (C) 2016-2017 by the Okapi Framework contributors
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

package net.sf.okapi.filters.openxml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;

/**
 * Part handler for master slides (PPTX). We assume that
 * {@link ConditionalParameters#getTranslatePowerpointMasters()} is {@code true}.
 */
class SlidablePart extends StyledTextPart {
    private static final String EMPTY = "";

    private final SlideFragments slideFragments;
    private final StyleDefinitions initialStyleDefinitions;
    private final StyleOptimisation.Bypass bypassStyleOptimisation;

    private String contentType;

    /**
     * {@code true} if the current event is between {@code <p:sp>} and {@code </p:sp>}.
     */
    private boolean inShape;

    /**
     * {@code true} if the current event is between {@code <p:nvPr>} and {@code </p:nvPr>}
     */
    private boolean inNonVisualProperties;

    /**
     * {@code true} if the current shape has the element {@code <p:ph>} in a {@code <p:nvPr>}
     */
    private boolean placeholderAvailable;

    private boolean inGraphicFrame;
    private String graphicFrameId;
    private int tableCellNumber;

    SlidablePart(
        final Document.General generalDocument,
        final ZipEntry entry,
        final SlideFragments slideFragments
    ) {
        super(generalDocument, entry, new StyleDefinitions.Empty(), new StyleOptimisation.Bypass());
        this.contentType = this.generalDocument.contentTypeFor(this.entry);
        this.slideFragments = slideFragments;
        this.initialStyleDefinitions = new StyleDefinitions.Empty();
        this.bypassStyleOptimisation = new StyleOptimisation.Bypass();
        this.graphicFrameId = SlidablePart.EMPTY;
    }

    /**
     * Sets the values of the fields that are needed for translating PPTX masters and slide layouts.
     * If both {@link ConditionalParameters#getTranslatePowerpointMasters()} and
     * {@link ConditionalParameters#getIgnorePlaceholdersInPowerpointMasters()} are set the
     * following rule applies: We only want to translate text in shapes({@code <p:sp>}} that not
     * have the non-visual property element "ph" ({@code <p:nvPr><p:ph .../></p:nvPr>}). These texts
     * are default texts of the master slides.
     *
     * @param e the event
     */
    protected void preProcess(XMLEvent e) throws IOException, XMLStreamException {
        if (e.isStartElement()) {
            final String localPart = e.asStartElement().getName().getLocalPart();
            if (ShapeFragments.SP.equals(localPart)) {
                inShape = true;
            }
            if (inShape && ShapeFragments.C_NV_PR.equals(localPart)) {
                actualiseStyleDefinitionsAndStyleOptimisation(e.asStartElement());
            }
            if (inShape && ShapeFragments.NV_PR.equals(localPart)) {
                inNonVisualProperties = true;
            }
            if (inNonVisualProperties && ShapeFragments.PLACEHOLDER.equals(localPart)) {
                placeholderAvailable = true;
                actualiseStyleDefinitionsAndStyleOptimisation(e.asStartElement());
            }
            if (GraphicFrameFragments.GRAPHIC_FRAME.equals(localPart)) {
                this.inGraphicFrame = true;
            }
            if (this.inGraphicFrame && GraphicFrameFragments.C_NV_PR.equals(localPart)) {
                this.graphicFrameId =
                    new NonVisualIdentificationPropertyFragments.Default(e.asStartElement()).id();
            }
            if (!this.graphicFrameId.isEmpty() && GraphicFrameFragments.TC.equals(localPart)) {
                this.tableCellNumber++;
                actualiseStyleDefinitionsAndStyleOptimisation();
            }
        }

        if (e.isEndElement()) {
            final String localPart = e.asEndElement().getName().getLocalPart();
            if (ShapeFragments.NV_PR.equals(localPart)) {
                inNonVisualProperties = false;
            }
            if (ShapeFragments.SP.equals(localPart)) {
                inShape = false;
                placeholderAvailable = false; // valid until the end of sp
                resetStyleDefinitionsAndStyleOptimisation();
            }
            if (GraphicFrameFragments.GRAPHIC_FRAME.equals(localPart)) {
                this.inGraphicFrame = false;
                this.graphicFrameId = SlidablePart.EMPTY;
                resetStyleDefinitionsAndStyleOptimisation();
            }
        }
    }

    private void actualiseStyleDefinitionsAndStyleOptimisation() throws IOException, XMLStreamException {
        this.styleDefinitions = this.slideFragments.listStyleFor(this.graphicFrameId, this.tableCellNumber);
        this.styleOptimisation = styleOptimisationFor(this.entry, this.styleDefinitions);
    }

    private void actualiseStyleDefinitionsAndStyleOptimisation(final StartElement startElement) throws IOException, XMLStreamException {
        if (!this.placeholderAvailable) {
            final NonVisualIdentificationPropertyFragments nvipf =
                new NonVisualIdentificationPropertyFragments.Default(startElement);
            this.styleDefinitions = this.slideFragments.listStyleFor(nvipf.id());
        } else {
            this.styleDefinitions = this.slideFragments.listStyleFor(new Placeholder.Default(startElement));
        }
        this.styleOptimisation = styleOptimisationFor(this.entry, this.styleDefinitions);
    }

    private StyleOptimisation styleOptimisationFor(final ZipEntry entry, final StyleDefinitions styleDefinitions) throws IOException, XMLStreamException {
        final Namespace namespace = this.generalDocument.namespacesOf(entry).forPrefix(Namespace.PREFIX_A);
        if (null == namespace) {
            return this.bypassStyleOptimisation;
        }
        return new StyleOptimisation.Default(
            this.bypassStyleOptimisation,
            this.generalDocument.conditionalParameters(),
            this.generalDocument.eventFactory(),
            new QName(namespace.uri(), ParagraphBlockProperties.PPR, namespace.prefix()),
            new QName(namespace.uri(), RunProperties.DEF_RPR, namespace.prefix()),
            Collections.emptyList(),
            styleDefinitions
        );
    }

    private void resetStyleDefinitionsAndStyleOptimisation() {
        this.styleDefinitions = this.initialStyleDefinitions;
        this.styleOptimisation = this.bypassStyleOptimisation;
    }

    /**
     * It depends on {@link ConditionalParameters#getIgnorePlaceholdersInPowerpointMasters()}
     * if a block is translatable.
     * <ul>
     * <li>{@code false}: All block are translatable</li>
     * <li>{@code true}: Only non-placeholder blocks are translatable</li>
     * </ul>.
     */
    protected boolean isCurrentBlockTranslatable() {
        return !ContentTypes.Types.Powerpoint.SLIDE_MASTER_TYPE.equals(contentType())
            && !ContentTypes.Types.Powerpoint.SLIDE_LAYOUT_TYPE.equals(contentType())
            && !ContentTypes.Types.Powerpoint.NOTES_MASTER_TYPE.equals(contentType())
            || !this.generalDocument.conditionalParameters().getIgnorePlaceholdersInPowerpointMasters()
            || !this.placeholderAvailable;
    }

    private String contentType() {
        if (null == this.contentType) {
            this.contentType = this.generalDocument.contentTypeFor(entry);
        }
        return this.contentType;
    }
}
