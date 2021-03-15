/*
 * =============================================================================
 *   Copyright (C) 2010-2020 by the Okapi Framework contributors
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
package net.sf.okapi.filters.mif;

import net.sf.okapi.common.exceptions.OkapiIOException;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class Extracts {
    private static final String DEFAULT_LINE_BREAK = "\n";
    private static final Set<String> MASTER_PAGES = new HashSet<>(Arrays.asList(
        "LeftMasterPage", "RightMasterPage", "OtherMasterPage"
    ));
    private static final Set<String> REFERENCE_PAGES = Collections.singleton("ReferencePage");
    private static final Set<String> BODY_PAGES = Collections.singleton("BodyPage");
    private static final Set<String> HIDDEN_PAGES = Collections.singleton("HiddenPage");

    private static final String MIF_FILE = "MIFFile";
    private static final String FONT_CATALOG = "FontCatalog";
    private static final String PAGE = "Page";
    private static final String PAGE_TYPE = "PageType";
    private static final String TEXT_RECT = "TextRect";
    private static final String ID = "ID";
    private static final String UNIQUE = "Unique";

    private static final String ANCHORED_FRAMES = "AFrames";
    private static final String FRAME = "Frame";
    private static final String TABLES = "Tbls";
    private static final String TEXT_FLOW = "TextFlow";
    private static final String TABLE = "Tbl";
    private static final String PARA = "Para";
    private static final String PARA_LINE = "ParaLine";
    private static final String TEXT_RECT_ID = "TextRectID";
    private static final String ANCHORED_FRAME = "AFrame";
    private static final String ANCHORED_TABLE = "ATbl";
    private static final String TABLE_ID = "TblID";
    private static final String TABLE_TITLE = "TblTitle";
    private static final String TABLE_TITLE_CONTENT = "TblTitleContent";
    private static final String TABLE_HEADER = "TblH";
    private static final String TABLE_ROW = "Row";
    private static final String TABLE_CELL = "Cell";
    private static final String TABLE_CELL_CONTENT = "CellContent";
    private static final String TABLE_BODY = "TblBody";
    private static final String PGF_TAG = "PgfTag";
    private static final String PGF_NUM_STRING = "PgfNumString";
    private static final String PGF = "Pgf";
    private static final String PGF_NUM_FORMAT = "PgfNumFormat";
    private static final String X_REF = "XRef";
    private static final String X_REF_NAME = "XRefName";

    private final Parameters parameters;
    private final FontTags fontTags;
    private final Set<String> extractableFrames;
    private final Set<String> extractableTextFlows;
    private final Set<String> extractableTables;
    private final Set<String> extractableParagraphFormatTags;
    private final Set<String> extractableReferenceFormatTags;
    private String lineBreak;

    Extracts(final Parameters parameters, final FontTags fontTags) {
        this(
            parameters,
            fontTags,
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>()
        );
    }

    Extracts(
        final Parameters parameters,
        final FontTags fontTags,
        final Set<String> extractableFrames,
        final Set<String> extractableTextFlows,
        final Set<String> extractableTables,
        final Set<String> extractableParagraphFormatTags,
        final Set<String> extractableReferenceFormatTags
    ) {
        this.parameters = parameters;
        this.fontTags = fontTags;
        this.extractableFrames = extractableFrames;
        this.extractableTextFlows = extractableTextFlows;
        this.extractableTables = extractableTables;
        this.extractableParagraphFormatTags = extractableParagraphFormatTags;
        this.extractableReferenceFormatTags = extractableReferenceFormatTags;
    }

    Set<String> additionalInlineCodeFinderRules() {
        return this.fontTags.toInlineCodeFinderRules();
    }

    String lineBreak() {
        return Objects.isNull(this.lineBreak)
            ? DEFAULT_LINE_BREAK
            : this.lineBreak;
    }

    boolean pageTypeExtractable(final String pageType) {
        return this.parameters.getExtractMasterPages() && MASTER_PAGES.contains(pageType)
            || this.parameters.getExtractReferencePages() && REFERENCE_PAGES.contains(pageType)
            || this.parameters.getExtractBodyPages() && BODY_PAGES.contains(pageType)
            || this.parameters.getExtractHiddenPages() && HIDDEN_PAGES.contains(pageType);
    }

    boolean frameExtractable(final String frameId) {
        return this.extractableFrames.contains(frameId);
    }

    boolean textFlowExtractable(final String textFlowNumber) {
        return this.extractableTextFlows.contains(textFlowNumber);
    }

    boolean tableExtractable(final String tableId) {
        return this.extractableTables.contains(tableId);
    }

    boolean paragraphFormatTagExtractable(final String paragraphFormatTag) {
        return this.extractableParagraphFormatTags.contains(paragraphFormatTag);
    }

    boolean referenceFormatTagExtractable(final String referenceFormatTag) {
        return this.extractableReferenceFormatTags.contains(referenceFormatTag);
    }

    void from(final Document document) {
        final List<Statement> pages = new LinkedList<>();
        final Set<String> textRects = new LinkedHashSet<>();
        final List<Statement> anchoredFrames = new LinkedList<>();
        final List<Statement> tables = new LinkedList<>();
        final Map<String, Statement> textFlows = new LinkedHashMap<>();

        boolean pagesSpecified = false;
        Document.Version documentVersion = null;
        int textFlowNumber = 0;
        while (document.hasNext()) {
            final Statement statement = document.next();
            if (Objects.isNull(this.lineBreak) && Statement.Type.COMMENT == statement.statementType()) {
                identifyLineBreak(statement);
                continue;
            }
            if (Statement.Type.MARKUP != statement.statementType()) {
                continue;
            }
            final String identity = statement.firstTokenOf(Token.Type.IDENTITY).toString();
            switch (identity) {
                case MIF_FILE:
                    documentVersion = new Document.Version(statement.firstTokenOf(Token.Type.LITERAL).toString());
                    documentVersion.validate();
                    break;
                case FONT_CATALOG:
                    this.fontTags.fromCatalog(statement);
                    break;
                case PAGE:
                    pagesSpecified = true;
                    if (pageTypeExtractable(statement.firstStatementWith(PAGE_TYPE).firstTokenOf(Token.Type.LITERAL).toString())) {
                        pages.add(statement);
                        textRects.addAll(textRectsFrom(Collections.singletonList(statement)));
                    }
                    break;
                case ANCHORED_FRAMES:
                    anchoredFrames.addAll(statement.statementsWith(FRAME));
                    break;
                case TABLES:
                    tables.addAll(statement.statementsWith(TABLE));
                    break;
                case TEXT_FLOW:
                    textFlows.put(String.valueOf(++textFlowNumber), statement);
            }
        }
        if (Objects.isNull(documentVersion)) {
            throw new OkapiIOException("The provided document type is unsupported.");
        }
        this.extractableFrames.clear();
        this.extractableTextFlows.clear();
        this.extractableTables.clear();
        this.extractableParagraphFormatTags.clear();
        this.extractableReferenceFormatTags.clear();
        final Map<String, Statement> anchoredTables = anchoredTables(tables);
        if (!pagesSpecified) {
            final Map<String, Statement> referentTables = referentTables(textFlows, anchoredTables);
            addExtractableTextFlowsAndTables(textFlows, referentTables);
            return;
        }
        addExtractableFrames(
            pages.stream()
                .map(s -> s.statementsWith(FRAME))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        );
        scanForExtractableTextFlowsTablesAndFrames(textFlows, textRects, anchoredFrames, anchoredTables);
        scanForExtractableParagraphAndReferenceFormatTags(textFlows, anchoredTables);
    }

    private void identifyLineBreak(final Statement statement) {
        final String value = statement.firstTokenOf(Token.Type.END).toString();
        this.lineBreak = value.equals(DEFAULT_LINE_BREAK)
            ? value
            : value.concat(DEFAULT_LINE_BREAK); // \r\n - native Mac support was dropped since version 7.0
    }

    private Set<String> textRectsFrom(final List<Statement> statements) {
        final Set<String> textRects = new LinkedHashSet<>();
        textRects.addAll(
            statements.stream()
            .map(s -> s.statementsWith(TEXT_RECT))
            .flatMap(Collection::stream)
            .map(s -> s.firstStatementWith(ID).firstTokenOf(Token.Type.LITERAL).toString())
            .collect(Collectors.toSet())
        );
        final boolean innerFramesAvailable = statements.stream()
            .anyMatch(f -> !f.statementsWith(FRAME).isEmpty());
        if (innerFramesAvailable) {
            textRects.addAll(
                textRectsFrom(
                    statements.stream()
                        .map(s -> s.statementsWith(FRAME))
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList())
                )
            );
        }
        return textRects;
    }

    private Map<String, Set<String>> anchoredFrameTextRects(final List<Statement> anchoredFrames) {
        return anchoredFrames.stream()
            .map(s -> {
                final Set<String> textRects = new LinkedHashSet<>();
                textRects.addAll(
                    s.statementsWith(TEXT_RECT).stream()
                        .map(tr -> tr.firstStatementWith(ID).firstTokenOf(Token.Type.LITERAL).toString())
                        .collect(Collectors.toSet())
                );
                final boolean innerFramesAvailable = !s.statementsWith(FRAME).isEmpty();
                if (innerFramesAvailable) {
                    textRects.addAll(
                        textRectsFrom(s.statementsWith(FRAME))
                    );
                }
                return new AbstractMap.SimpleEntry<>(
                    s.firstStatementWith(ID).firstTokenOf(Token.Type.LITERAL).toString(),
                    textRects
                );
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Statement> anchoredTables(final List<Statement> anchoredTables) {
        return anchoredTables.stream()
            .map(s -> new AbstractMap.SimpleEntry<>(
                s.firstStatementWith(TABLE_ID).firstTokenOf(Token.Type.LITERAL).toString(),
                s
            ))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void addExtractableTextFlowsAndTables(Map<String, Statement> textFlows, Map<String, Statement> tables) {
        this.extractableTextFlows.addAll(textFlows.keySet());
        this.extractableTables.addAll(tables.keySet());
        this.extractableTables.addAll(tableReferencesOf(tables.values(), ANCHORED_TABLE));
    }

    private void addExtractableFrames(final List<Statement> frames) {
        this.extractableFrames.addAll(
            frames.stream()
                .map(f -> f.firstStatementWith(UNIQUE).firstTokenOf(Token.Type.LITERAL).toString())
                .collect(Collectors.toSet())
        );
        final boolean innerFramesAvailable = frames.stream()
            .anyMatch(f -> !f.statementsWith(FRAME).isEmpty());
        if (innerFramesAvailable) {
            addExtractableFrames(
                frames.stream()
                    .map(s -> s.statementsWith(FRAME))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList())
            );
        }
    }

    private void scanForExtractableTextFlowsTablesAndFrames(
        final Map<String, Statement> textFlows,
        final Set<String> textRects,
        final List<Statement> anchoredFrames,
        final Map<String, Statement> anchoredTables
    ) {
        final Map<String, Statement> referentTextFlows = referentTextFlows(textFlows, textRects);
        final Map<String, Statement> referentTables = referentTables(referentTextFlows, anchoredTables);
        addExtractableTextFlowsAndTables(referentTextFlows, referentTables);

        final Set<String> anchoredFrameReferences = anchoredFrameReferences(referentTextFlows, referentTables);
        addExtractableFrames(
            anchoredFrames.stream()
                .filter(f -> anchoredFrameReferences.contains(f.firstStatementWith(ID).firstTokenOf(Token.Type.LITERAL).toString()))
                .collect(Collectors.toList())
        );

        final Set<String> referentTextRects = referentTextRects(
            anchoredFrameReferences,
            anchoredFrameTextRects(anchoredFrames)
        );
        if (!referentTextRects.isEmpty()) {
            scanForExtractableTextFlowsTablesAndFrames(textFlows, referentTextRects, anchoredFrames, anchoredTables);
        }
    }

    private Set<String> anchoredFrameReferences(
        final Map<String, Statement> referentTextFlows,
        final Map<String, Statement> referentTables
    ) {
        final Set<String> anchoredFrameReferences = new LinkedHashSet<>();
        anchoredFrameReferences.addAll(anchoredReferences(referentTextFlows.values(), ANCHORED_FRAME));
        anchoredFrameReferences.addAll(tableReferencesOf(referentTables.values(), ANCHORED_FRAME));
        return anchoredFrameReferences;
    }

    private Map<String, Statement> referentTextFlows(final Map<String, Statement> textFlows, final Set<String> textRects) {
        return textFlows.entrySet().stream()
            .filter(e -> e.getValue().statementsWith(PARA).stream()
                .anyMatch(p -> p.statementsWith(PARA_LINE).stream()
                    .anyMatch(pl -> textRects.contains(pl.firstStatementWith(TEXT_RECT_ID).firstTokenOf(Token.Type.LITERAL).toString()))
                )
            )
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, Statement> referentTables(
        final Map<String, Statement> frameTextFlows,
        final Map<String, Statement> anchoredTables
    ) {
        final Set<String> anchoredTableReferences = anchoredReferences(frameTextFlows.values(), ANCHORED_TABLE);
        return anchoredTables.entrySet().stream()
            .filter(e -> anchoredTableReferences.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> tableReferencesOf(final Collection<Statement> referentTables, final String referenceName) {
        final Set<String> references = new LinkedHashSet<>();
        // TblTitle > TblTitleContent
        references.addAll(anchoredReferences(tableTitleContentFlows(referentTables), referenceName));
        // TblH > Row > Cell > CellContent
        references.addAll(anchoredReferences(tableContentFlowsOf(referentTables, TABLE_HEADER), referenceName));
        // TblBody > Row > Cell > CellContent
        references.addAll(anchoredReferences(tableContentFlowsOf(referentTables, TABLE_BODY), referenceName));
        return references;
    }

    private List<Statement> tableTitleContentFlows(final Collection<Statement> referentTables) {
        return referentTables.stream()
            .map(s -> s.statementsWith(TABLE_TITLE))
            .flatMap(Collection::stream)
            .map(s -> s.statementsWith(TABLE_TITLE_CONTENT))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<Statement> tableContentFlowsOf(final Collection<Statement> referentTables, final String rootIdentity) {
        return referentTables.stream()
            .map(s -> s.statementsWith(rootIdentity))
            .flatMap(Collection::stream)
            .map(s -> s.statementsWith(TABLE_ROW))
            .flatMap(Collection::stream)
            .map(s -> s.statementsWith(TABLE_CELL))
            .flatMap(Collection::stream)
            .map(s -> s.statementsWith(TABLE_CELL_CONTENT))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private Set<String> referentTextRects(
        final Set<String> anchoredFrameReferences,
        final Map<String, Set<String>> anchoredFrameTextRects
    ) {
        return anchoredFrameReferences.stream()
            .filter(s -> anchoredFrameTextRects.keySet().contains(s))
            .map(s -> anchoredFrameTextRects.get(s))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    private Set<String> anchoredReferences(final Collection<Statement> contentFlows, final String referenceName) {
        return contentFlows.stream()
            .map(v -> v.statementsWith(PARA))
            .flatMap(Collection::stream)
            .map(s -> s.statementsWith(PARA_LINE))
            .flatMap(Collection::stream)
            .map(s -> s.statementsWith(referenceName))
            .flatMap(Collection::stream)
            .map(s -> s.firstTokenOf(Token.Type.LITERAL).toString())
            .collect(Collectors.toSet());
    }

    private void scanForExtractableParagraphAndReferenceFormatTags(final Map<String, Statement> textFlows, final Map<String, Statement> anchoredTables) {
        final List<Statement> paragraphs = new LinkedList<>();
        paragraphs.addAll(
            textFlows.entrySet().stream()
                .filter(e -> this.extractableTextFlows.contains(e.getKey()))
                .map(e -> e.getValue())
                .map(s -> s.statementsWith(PARA))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        );
        final List<Statement> tables = anchoredTables.entrySet().stream()
            .filter(e -> this.extractableTables.contains(e.getKey()))
            .map(e -> e.getValue())
            .collect(Collectors.toList());
        final List<Statement> tableContentFlows = new LinkedList<>();
        // TblTitle > TblTitleContent
        tableContentFlows.addAll(tableTitleContentFlows(tables));
        // TblH > Row > Cell > CellContent
        tableContentFlows.addAll(tableContentFlowsOf(tables, TABLE_HEADER));
        // TblBody > Row > Cell > CellContent
        tableContentFlows.addAll(tableContentFlowsOf(tables, TABLE_BODY));
        paragraphs.addAll(
            tableContentFlows.stream()
                .map(s -> s.statementsWith(PARA))
                .flatMap(Collection::stream)
                .collect(Collectors.toList())
        );
        this.extractableParagraphFormatTags.addAll(
            paragraphs.stream()
                .filter(s -> Statement.Type.EMPTY != s.firstStatementWith(PGF_TAG).statementType())
                .filter(s -> Statement.Type.EMPTY != s.firstStatementWith(PGF_NUM_STRING).statementType())
                .filter(s -> Statement.Type.EMPTY == s.firstStatementWith(PGF).firstStatementWith(PGF_NUM_FORMAT).statementType())
                .map(s -> s.firstStatementWith(PGF_TAG).firstTokenOf(Token.Type.LITERAL).toString())
                .collect(Collectors.toSet())
        );
        this.extractableReferenceFormatTags.addAll(
            paragraphs.stream()
                .map(s -> s.statementsWith(PARA_LINE))
                .flatMap(Collection::stream)
                .map(s -> s.statementsWith(X_REF))
                .flatMap(Collection::stream)
                .filter(s -> Statement.Type.EMPTY != s.firstStatementWith(X_REF_NAME).statementType())
                .map(s -> s.firstStatementWith(X_REF_NAME).firstTokenOf(Token.Type.LITERAL).toString())
                .collect(Collectors.toSet())
        );
    }
}
