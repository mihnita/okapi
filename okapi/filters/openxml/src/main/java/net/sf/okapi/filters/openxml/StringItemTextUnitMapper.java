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

import static net.sf.okapi.filters.openxml.ExcelWorksheetTransUnitProperty.CELL_REFERENCE;
import static net.sf.okapi.filters.openxml.ExcelWorksheetTransUnitProperty.SHEET_NAME;

import net.sf.okapi.common.ISkeleton;
import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.Property;
import net.sf.okapi.common.resource.TextContainer;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;
import net.sf.okapi.common.skeleton.GenericSkeleton;

import javax.xml.stream.XMLEventFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class StringItemTextUnitMapper extends TextUnitMapper {
    private StringItem stringItem;
    private String cell;
    private String worksheet;

    StringItemTextUnitMapper(
        final IdGenerator idGenerator,
        final XMLEventFactory eventFactory,
        final StringItem stringItem,
        final String cell,
        final String worksheet
    ) {
        super(idGenerator, eventFactory);
        this.stringItem = stringItem;
        this.cell = cell;
        this.worksheet = worksheet;
    }

    public List<ITextUnit> map() {
        return stringItem.isStyled()
            ? textUnits = mapStyledText()
            : mapText();
    }

    private List<ITextUnit> mapStyledText() {
        // Since blocks typically start and end with markup, blocks with <= 2 chunks should
        // be empty.
        if (stringItem.getChunks().size() <= 2) {
            // Sanity check
            for (XMLEvents chunk : stringItem.getChunks()) {
                if (chunk instanceof Run || chunk instanceof Text) {
                    throw new IllegalStateException(ExceptionMessages.UNEXPECTED_STRUCTURE);
                }
            }
            return Collections.emptyList();
        }
        ITextUnit textUnit = new TextUnit(idGenerator.createId());
        textUnit.setPreserveWhitespaces(true);
        TextFragment tf = new TextFragment();
        textUnit.setSource(new TextContainer(tf));
        if (null != cell && null != worksheet) {
            textUnit.setName(worksheet + "!" + cell);
        }
        textUnit.setProperty(new Property(CELL_REFERENCE.getKeyName(), cell, true));
        textUnit.setProperty(new Property(SHEET_NAME.getKeyName(), worksheet, true));

        // The first and last chunks should always be markup.  We skip them.
        List<Chunk> chunks = stringItem.getChunks().subList(1, stringItem.getChunks().size() - 1);
        baseRunProperties(chunks, stringItem.getRunName());

        boolean runHasText = false;
        for (Chunk chunk : chunks) {
            if (chunk instanceof Run) {
                runHasText |= processRun(textUnit, (Run) chunk);
            } else {
                addIsolatedCode(tf, chunk);
            }
        }
        popAllRunCodes(tf);
        List<ITextUnit> tus = new ArrayList<>(referentTus.size() + 1);
        // Runs containing no text can be skipped, but only if they don't
        // contain a reference to an embedded TU.  (If they do, we need
        // to anchor the skeleton here.  It would be possible to fix this,
        // but would require this class to distinguish deferred TUs from real
        // TUs in its return value, so the part handler could make a decision.)
        if (runHasText || !referentTus.isEmpty()) {
            // Deferred TUs already have their own block skeletons set
            ISkeleton skel = new BlockSkeleton(stringItem.getBlock(), baseRunProperties, hiddenCodes, visibleCodes);
            skel.setParent(textUnit);
            textUnit.setSkeleton(skel);
            tus.add(textUnit);
        }
        tus.addAll(referentTus);
        return tus;
    }

    private List<ITextUnit> mapText() {
        ITextUnit textUnit = new TextUnit(idGenerator.createId());
        textUnit.setPreserveWhitespaces(true);
        TextFragment tf = new TextFragment();
        textUnit.setSource(new TextContainer(tf));
        textUnit.setMimeType(OpenXMLFilter.MIME_TYPE);
        if (null != cell && null != worksheet) {
            textUnit.setName(worksheet + "!" + cell);
        }
        textUnit.setProperty(new Property(CELL_REFERENCE.getKeyName(), cell, true));
        textUnit.setProperty(new Property(SHEET_NAME.getKeyName(), worksheet, true));

        Text text = (Text) stringItem.getChunks().get(1);

        addText(tf, text.characters().getData());

        final GenericSkeleton skel = new GenericSkeleton();
        skel.add(XMLEventSerializer.serialize(stringItem.getChunks().get(0)));
        skel.add(XMLEventSerializer.serialize(text.startElement()));
        skel.addContentPlaceholder(textUnit);
        skel.add(XMLEventSerializer.serialize(text.endElement()));
        skel.add(XMLEventSerializer.serialize(stringItem.getChunks().get(2)));


        skel.setParent(textUnit);
        textUnit.setSkeleton(skel);

        return Collections.singletonList(textUnit);
    }

}
