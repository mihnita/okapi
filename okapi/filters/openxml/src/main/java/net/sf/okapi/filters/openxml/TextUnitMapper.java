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

import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextUnit;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class TextUnitMapper  {
    /**
     * A default code stack pops limit.
     */
    private static final int DEFAULT_CODE_STACK_POPS_LIMIT = 0;

    /**
     * An unused run position value.
     */
    private static final int UNUSED_RUN_POSITION_VALUE = -1;

    private static final String NESTED_ID_GENERATOR_PREFIX = "sub";

    /**
     * A number of references to referent text units.
     */
    static final String REFERENCES = "references";

    protected final IdGenerator idGenerator;
    protected final XMLEventFactory eventFactory;
    protected List<ITextUnit> textUnits;
    protected List<ITextUnit> referentTus = new ArrayList<>();
    protected List<XMLEvents> hiddenCodes = new ArrayList<>();
    protected Map<Integer, XMLEvents> visibleCodes = new HashMap<>();
    protected Deque<RunCode> runCodeStack = new ArrayDeque<>();
    protected int nextCodeId = 1;

    protected IdGenerator nestedIdsGenerator;

    RunProperties baseRunProperties;

    TextUnitMapper(final IdGenerator idGenerator, final XMLEventFactory eventFactory) {
        this.idGenerator = idGenerator;
        this.eventFactory = eventFactory;
    }

    abstract public List<ITextUnit> map();

    void baseRunProperties(final List<Chunk> chunks, final QName runName) {
        this.baseRunProperties = chunks.stream()
            .filter(c -> c instanceof Run)
            .map(chunk -> ((Run) chunk).getProperties())
            .min(Comparator.comparingInt(runProperties -> runProperties.count()))
            .orElseGet(() -> new RunProperties.Default(
                this.eventFactory,
                runName.getPrefix(),
                runName.getNamespaceURI(),
                RunProperties.RPR
            ));
    }

    boolean processRun(final ITextUnit textUnit, final Run run) {
        final List<ITextUnit> nestedTus = processNestedBlocks(run, textUnit.getId());
        TextUnitProperties.addInteger(textUnit, REFERENCES, countDirectReferences(nestedTus));
        referentTus.addAll(nestedTus);
        return addRun(
            textUnit.getSource().getFirstContent(),
            DEFAULT_CODE_STACK_POPS_LIMIT,
            UNUSED_RUN_POSITION_VALUE,
            run,
            null
        );
    }

    protected List<ITextUnit> processNestedBlocks(Run run, String parentId) {
        IdGenerator nestedIdsGenerator = getNestedIdsGenerator(parentId);
        List<ITextUnit> tus = new ArrayList<>();
        for (Textual textual : run.getNestedTextualItems()) {
            if (textual instanceof TranslatableAttributeText) {
                TextUnit tu = new TextUnit(nestedIdsGenerator.createId(), ((TranslatableAttributeText) textual).getText());
                tu.setPreserveWhitespaces(true);
                tu.setIsReferent(true);
                tus.add(tu);
            }
        }
        return tus;
    }

    int countDirectReferences(final List<ITextUnit> textUnits) {
        int directReferences = textUnits.size();
        for (final ITextUnit textUnit : textUnits) {
            directReferences -= TextUnitProperties.integer(textUnit, REFERENCES);
        }
        return directReferences;
    }

    private IdGenerator getNestedIdsGenerator(String parentId) {
        if (nestedIdsGenerator == null) {
            nestedIdsGenerator = new IdGenerator(parentId, NESTED_ID_GENERATOR_PREFIX);
        }
        return nestedIdsGenerator;
    }

    /**
     * Adds a run.
     *
     * - We produce a single code for the entire structure if a run contains no text or is hidden by styling.
     * - If the new run is a superset of the old run, then this is a nested tag (this is equivalent to asking if the old
     *   run is a subset of the new one)
     *   eg, <b> --> <b><u>
     * - If the new run is a subset of the old run... end the old tag, start a new tag.
     *   (Alternately we could go back and redo the whole thing, but that's hard.)
     * - If the new run has the same properties as the old run, no new tag as needed..
     *
     * @param tf                 A text fragment
     * @param codeStackPopsLimit A code stack pops limit
     * @param runPosition        A run position
     * @param run                A run
     * @param nextRun            A next run
     *
     * @return {@code true} - if the run content has been added
     *         {@code false} - otherwise
     */
    boolean addRun(
        final TextFragment tf,
        final int codeStackPopsLimit,
        final int runPosition,
        final Run run,
        final Run nextRun
    ) {
        if (!run.containsVisibleText()) {
            // if a run contains no text or is hidden
            if (tf.getCodedText().isEmpty()) {
                hiddenCodes.add(run);
            } else {
                addIsolatedCode(tf, run);
            }
            return false;
        }

        RunProperties rp = run.getProperties();

        while (runCodeStack.size() > codeStackPopsLimit
                && !runCodeStack.isEmpty()
                && !runCodeStack.peekFirst().getRunProperties().isSubsetOf(rp)) {
            // if the size of the code stack is more than code stack pops limit
            // or the code stack is not empty
            // or the top of the code stack is not equal to the specified properties

            addClosingCode(tf, runCodeStack.pop());
        }

        if ((0 == runPosition && null != nextRun && !nextRun.getProperties().equals(rp) && !nextRun.getProperties().isSubsetOf(rp))
                || (!baseRunProperties.equals(rp) && (runCodeStack.isEmpty() || !runCodeStack.peekFirst().getRunProperties().equals(rp)))) {
            // if this is the first run and the next run is specified (this should happen only in the run container case processing)
            // or if this is not the first run and the code stack is empty or the top of the code stack is not equal to the specified run properties
            // or if the number of run properties is more than 0 and the code stack is empty or the top of the code stack is not equal to the specified run properties

            RunCode rc = new RunCode(nextCodeId++, rp, run.getCombinedProperties());
            runCodeStack.push(rc);
            addOpeningCode(tf, rc);
        }

        addRunContent(tf, run);

        return true;
    }

    protected void addRunContent(TextFragment tf, Run run) {
        for (XMLEvents runBodyChunk : run.getBodyChunks()) {
            if (runBodyChunk instanceof Run.RunText) {
                addText(tf, ((Run.RunText) runBodyChunk).characters().getData());
            } else {
                // Markup within the run, eg <w:tab/>
                addIsolatedCode(tf, runBodyChunk);
            }
        }
    }

    protected void popAllRunCodes(TextFragment tf) {
        while (!runCodeStack.isEmpty()) {
            addClosingCode(tf, runCodeStack.pop());
        }
    }

    protected void addText(TextFragment tf, String text) {
        tf.append(text);
    }

    protected void addOpeningCode(TextFragment tf, RunCode rc) {
        Code code = new Code(TextFragment.TagType.OPENING, rc.getCodeType());
        code.setId(rc.getCodeId());
        code.setData("<run" + code.getId() + ">");
        visibleCodes.put(rc.getCodeId(), rc.getRunProperties());
        tf.append(code);
    }

    protected void addClosingCode(TextFragment tf, RunCode rc) {
        Code code = new Code(TextFragment.TagType.CLOSING, rc.getCodeType());
        code.setId(rc.getCodeId());
        code.setData("</run" + code.getId() + ">");
        tf.append(code);
    }

    protected void addIsolatedCode(TextFragment tf, XMLEvents events) {
        int codeId = nextCodeId++;
        visibleCodes.put(codeId, events);
        Code code = new Code(TextFragment.TagType.PLACEHOLDER, "x", getCodeData(events, codeId));
        code.setId(codeId);
        tf.append(code);
    }

    private String getCodeData(XMLEvents codeEvents, int codeId) {
        if (codeEvents instanceof Run) {
            return "<run" + codeId + "/>";
        }
        return "<tags" + codeId + "/>";
    }
}
