/*===========================================================================
  Copyright (C) 2017-2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.markdown.parser;

import static net.sf.okapi.filters.markdown.parser.MarkdownTokenType.*;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlBlockBase;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.HtmlInlineComment;
import com.vladsch.flexmark.ast.HtmlInnerBlock;
import com.vladsch.flexmark.ast.HtmlInnerBlockComment;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.ImageRef;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.InlineLinkNode;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.LinkNodeBase;
import com.vladsch.flexmark.ast.LinkRef;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.MailLink;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.RefNode;
import com.vladsch.flexmark.ast.Reference;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.TextBase;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ast.WhiteSpace;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.Subscript;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCaption;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterBlock;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.AllNodesVisitor;
import com.vladsch.flexmark.util.ast.BlankLine;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;

import net.sf.okapi.common.StringUtil;
import net.sf.okapi.filters.markdown.Parameters;

public class MarkdownParser {
    private static final MutableDataHolder OPTIONS = new MutableDataSet()
            //.set(Parser.PARSER_EMULATION_PROFILE, ParserEmulationProfile.GITHUB_DOC) // This is for the older GitHub compatibility.
	    // GitHub is now Common Mark based, which is the default of Flexmark-Java.
            .set(Parser.EXTENSIONS, Arrays.asList(StrikethroughSubscriptExtension.create(),
                                                  TablesExtension.create(),
                                                  YamlFrontMatterExtension.create()
                                                  ))
            .set(Parser.HEADING_NO_ATX_SPACE, true) // For compatibility with older Github
            .set(Parser.BLANK_LINES_IN_AST, true)
            ;

    private static final Parser PARSER = Parser.builder(OPTIONS).build();

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private String newline = System.lineSeparator();
    private Node root = null;
    private Deque<MarkdownToken> tokenQueue = new LinkedList<>();
    private boolean lastAddedTranslatableContent = false;
    private Parameters params;
    private Pattern urlPatternToTranslate;
    private boolean isBlockQuoteNonTranslatable = false;
    private String linePrefix = ""; // "> ", "> > ", "    " (indented code), "> >     " (doubly quoted indented code) etc.


    /**
     * Create a new {@link MarkdownParser} that uses the platform-specific newline.
     */
    public MarkdownParser(Parameters params) {
        this.params = params;
        urlPatternToTranslate = Pattern.compile(params.getUrlToTranslatePattern());
    }

    /**
     * Create a new {@link MarkdownParser} that uses the specified string as a newline.
     * @param newline The newline type that this parser will use
     */
    public MarkdownParser(Parameters params, String newline) {
        this(params);
        this.newline = newline;
    }

    /**
     * Parse the given Markdown content into tokens that can be then retrieved with
     * calls to {@link MarkdownParser#getNextToken()}. Any existing tokens from
     * previous calls to {@link MarkdownParser#parse(String)} will be discarded.
     *
     * @param markdownContent The Markdown content to parse into tokens
     */
    public void parse(String markdownContent) {
        root = PARSER.parse(markdownContent);
        tokenQueue.clear();
        lastAddedTranslatableContent = false;

        preVisitor.visit(root); // Pre-scan the node tree to check which reference text needs translation.
        visitor.visit(root); // The visit all the nodes while generating tokens which MarkdownFilter converts to Events.
    }

    public boolean hasNextToken() {
        return !tokenQueue.isEmpty();
    }

    /**
     * Returns the next available token.
     *
     * @return The next token
     * @throws IllegalStateException If no more tokens are remaining
     */
    public MarkdownToken getNextToken() {
        if (!hasNextToken()) {
            throw new IllegalStateException("No more tokens remaining");
        }
        return tokenQueue.removeFirst();
    }

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    /**
     * Dumps all tokens. This is for development.
     * @return String representation of all MarkdownTokens generated.
     */
    public String dumpTokens() {
        StringBuilder builder = new StringBuilder();
        for (MarkdownToken tok: tokenQueue) {
            builder.append(tok).append(newline);
        }
        return builder.toString();
    }
    
    /**
     * Returns a string representation the AST generated by the parser from the last
     * call to {@link MarkdownParser#parse(String)}.
     * <br>
     * <code>
     * System.out.println(markdownParser.toString())
     * </code><br>
     * is a convenient way to dump the parsed node tree during the development.
     * @return String representation of the AST
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        generateAstString(root, 0, builder);
        return builder.toString();
    }

    private void generateAstString(Node node, int depth, StringBuilder builder) {
        if (node==null) {
            builder.append("The root node is null!\n");
            return;
        }
        for(int i=0; i<depth; ++i) builder.append(' ');
        builder.append(node.toAstString(true)).append(newline);
        for (Node child: node.getChildren()) {
            generateAstString(child, depth + 1, builder);
        }
    }
    
    private String trimSpacesOnly(String str) { //TODO: Remove me
    	str = str.replaceAll("^ +", "");
    	str = str.replaceAll(" +$", "");
    	return str;
    }

    private void addToQueue(String content, boolean isTranslatable, MarkdownTokenType type, Node node) {//
        if (content.equals(newline) && !isTranslatable) {
            lastAddedTranslatableContent = isTranslatable;
                tokenQueue.addLast(new MarkdownToken(content, isTranslatable, type));
            return;
        }


        // If this token and the previously added token are translatable, merge them into a single token
        if (lastAddedTranslatableContent && isTranslatable) {
            MarkdownToken lastToken = tokenQueue.peekLast();
            lastToken.setContent(lastToken.getContent() + content);
            if (lastToken.getType().equals(SOFT_LINE_BREAK)) {
                // If a TEXT is following a SOFT_LINE_BREAK, we want the whole token to be recognized as TEXT
                lastToken.setType(type); // Most likely TEXT or SOFT_LINE_BREAK
            }
            return;
        }
        if(isBlockQuoteNonTranslatable)
        	isTranslatable = false;
        lastAddedTranslatableContent = isTranslatable;
        tokenQueue.addLast(new MarkdownToken(content, isTranslatable, type));
    }

    private void addListPaddingCharacters(String content, Node node) { //TODO: Remove me if not used
        // If a newline is the content being inserted, no need for additional whitespace padding
        if (content.equals(newline)) {
            return;
        }
        // Only allow padding after a newline
        MarkdownToken lastToken = tokenQueue.peekLast();
        if (lastToken == null || !lastToken.getContent().equals(newline)) {
            return;
        }

        // Apply additional padding to non-list items
        int depth = 1;
        if ((node instanceof BulletListItem) || (node instanceof OrderedListItem)) {
            depth = 0;
        }
        // Calculate how many nested lists deep this node is
        Node ancestor = node.getAncestorOfType(BulletList.class, OrderedList.class);
        while (ancestor != null) {
            ancestor = ancestor.getAncestorOfType(BulletList.class, OrderedList.class);
            depth++;
        }

        // Add padding based on the calculated list depth
        StringBuilder padding = new StringBuilder();
        
        for (int i = 1; i < depth; i++) {
            padding.append("   ");
        }
        if (padding.length() > 0) {
            tokenQueue.addLast(new MarkdownToken(padding.toString(), false, WHITE_SPACE));
        }
    }
    
    private Map<String, Boolean> refVisible = new HashMap<>(); // Has the ref-text been referenced without link text?
    private Set<String> usedRefTextSet = new HashSet<>(); // All reference text that are actually referenced go here.
    // Per http://spec.commonmark.org/0.28/#example-510, matching is case-insensitive, so we lower-case the reference text
    // according to the US locale rule (to be consistent regardless of the locale of the runtime).
    
    // This visitor runs first to scan all the LinkRef nodes and decide which 
    // reference text is visible and needs to be translated.
    private AllNodesVisitor preVisitor = new AllNodesVisitor() {
	@Override
	protected void process(Node node) {
	    if (node instanceof LinkRef) {
		LinkRef linkRefNode = (LinkRef) node;
		BasedSequence refTextBS = linkRefNode.getReference();
		if (isDefined(refTextBS)) {
		    String refText = refTextBS.toString();
		    if (refVisible.containsKey(refText)) { // Same ref text has been seen
			if (refVisible.get(refText)) { // ... and it was determined to be visible.
			    return;
			}
		    } else {
			refVisible.put(refText, false); // Make an entry.
		    }			
		    if (!isDefined(linkRefNode.getText())) { // No anchor text!
			refVisible.put(refText,  true); // Markdown has to show the refText.
		    }
		}
	    }
	    if (node instanceof RefNode) { // I.e. either LinkRef or ImageRef
		RefNode refNode = (RefNode) node;
		BasedSequence refTextBS = refNode.getReference();
		if (isDefined(refTextBS)) {
		    usedRefTextSet.add(refTextBS.toString().toLowerCase(Locale.US));
		}
	    }
	}
    };
    
    // Determines if a reference text is visible thus should be extracted for translation
    private boolean isVisibleRef(String refText) {
	return refVisible.getOrDefault(refText, false);
    }
    
    // Determines if the ref text is actually referenced.
    private boolean isRefTextUsed(String refText) {
	return usedRefTextSet.contains(refText.toLowerCase(Locale.US));
    }
    
    // This main visitor visits all children

	// visitBlock(node, false, BLANK_LINE); // This doesn't work because node.getContentChars() returns an empty string.
	private NodeVisitor visitor = new NodeVisitor(

        /* Core nodes */
        new VisitHandler<>(AutoLink.class, node -> addToQueue(node.getChars().toString(), false, AUTO_LINK, node)),

        new VisitHandler<>(BlankLine.class, node -> addToQueue(newline, false, BLANK_LINE, node)),
        new VisitHandler<>(BlockQuote.class, new Visitor<BlockQuote>() {
            @Override public void visit(BlockQuote node) {
            	boolean revertNonTranslatableFlag = false;
            	if(!params.getNonTranslateBlocks().isEmpty()) {
            		String[] nonTranslatableBlocks = params.getNonTranslateBlocks().split(",");
            		for(String block : nonTranslatableBlocks) {
	            		if(node.getChars().toString().contains(block)) {
	                    	isBlockQuoteNonTranslatable = true;
	                    	revertNonTranslatableFlag = true;
	                    	break;
	                    }
            		}
            	}

                String prevLinePrefix = linePrefix;
                linePrefix = prevLinePrefix + node.getOpeningMarker().toString() + " ";
                addToQueue(linePrefix, false, LINE_PREFIX, node);
                visitor.visitChildren(node);
                if (!hasDescendentParagraph(node) // The newline many not have been taken care of.
                    && !hasDescendentBlankLine(node)
                    && node.getChars().endsWith(newline)) {
                    // Only BlockQuote nodes know how it ends.
                    // Its child nodes do not include the newline sequence at the end of the block.
                    // Note a block quote can end without a newline if it is at the end of the file.
                    addNewline(node);
                }
                if(revertNonTranslatableFlag)
                	isBlockQuoteNonTranslatable = false;
                linePrefix = prevLinePrefix;
                addToQueue(linePrefix, false, LINE_PREFIX, node);
            }
        }),
        new VisitHandler<>(BulletList.class, node -> visitListBlock(node, BULLET_LIST)),
        new VisitHandler<>(BulletListItem.class, node -> visitListItem(node, BULLET_LIST_ITEM)),
        new VisitHandler<>(Code.class, new Visitor<Code>() {
            @Override public void visit(Code node) {
                if (params.getTranslateInlineCodeBlocks()) {
                    addToQueue(node.getOpeningMarker().toString(), false, CODE, node);
                    addToQueue(node.getText().toString(), true, TEXT, node);
                    addToQueue(node.getClosingMarker().toString(), false, CODE, node);
                }
                else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(node.getOpeningMarker().toString())
                      .append(node.getText().toString())
                      .append(node.getClosingMarker().toString());
                    addToQueue(sb.toString(), false, CODE, node);
                    if (node.getText().toString().contains("\n")) {
                	LOGGER.debug("Code.getText() includes one or more newlines:{}", node.getText());
                    }
                }
            }
        }),
        new VisitHandler<>(Block.class, new Visitor<Block>() {
            @Override public void visit(Block node) {
                visitor.visitChildren(node);
            }
        }),
        new VisitHandler<>(Node.class, new Visitor<Node>() {
            @Override public void visit(Node node) {
                visitor.visitChildren(node);
            }
        }),
        new VisitHandler<>(Document.class, new Visitor<Document>() {
            @Override public void visit(Document node) {
                visitor.visitChildren(node);
//                addNewline(node);
            }
        }),
        new VisitHandler<>(Emphasis.class, node -> visitDelimitedNode(node, EMPHASIS)),
        new VisitHandler<>(FencedCodeBlock.class, new Visitor<FencedCodeBlock>() {
            @Override public void visit(FencedCodeBlock node) {
                addToQueue(node.getOpeningFence().toString(), false, FENCED_CODE_BLOCK, node);
                if (isDefined(node.getInfo())) {
                    addToQueue(node.getInfo().toString(), false, FENCED_CODE_BLOCK_INFO, node);
                }
                addToQueue(newline, false, SOFT_LINE_BREAK, node);
                for (BasedSequence seq: node.getContentLines()) {
                    addToQueue(seq.toString(), params.getTranslateCodeBlocks(), TEXT, node); // Use TEXT to indicate this is not the indentation marker.
                }
                addToQueue(node.getClosingFence().toString(), false, FENCED_CODE_BLOCK, node);
                // The FencedCodeBlock node does not include the newline after the close marker.
                // This newline does not seens ti belong to any node. We have no way to know if the file ends
                // after the close marker suddenly, or it has a newline.
                // We always output the newline.
                addNewline(node);
            }
        }),
        new VisitHandler<>(YamlFrontMatterBlock.class, new Visitor<YamlFrontMatterBlock>() {
            @Override public void visit(YamlFrontMatterBlock node) {
                if (params.getTranslateHeaderMetadata()) {
                    addToQueue("---", false, THEMATIC_BREAK, node);
                    addNewline(node);
                    StringBuilder yaml = new StringBuilder();
                    for(BasedSequence sequence : node.getContentLines()) {
                        if (!sequence.matchChars("---")) {
                            yaml.append(sequence.normalizeEndWithEOL());
                        }
                    }
                    addToQueue(yaml.toString(), true, YAML_METADATA_HEADER, node);
                    addNewline(node);
                    addToQueue("---", false, THEMATIC_BREAK, node);
                    addNewline(node);
                }
                else {
                    addToQueue(node.getContentChars().toString()/*.trim() why? */, false, THEMATIC_BREAK, node);
                    if(!node.getContentChars().endsWith(newline))
                        addNewline(node);
                }
            }
        }),
        new VisitHandler<>(HardLineBreak.class, new Visitor<HardLineBreak>() {
            // NOTE: Lambda expression is not used on purpose.
            // Please don't convert this to a lambda expression untildirs 2020.
            @Override public void visit(HardLineBreak node) {
                // Hack: to insert the actual newline inside the trans-unit/source etc. in XLIFF,
                // the actual hard line break node's content is divided into the part before
                // the newline and the newline, and the newline is treated as though it were
                // a soft line break.
                String x = node.getChars().toString();
                if (x.endsWith(newline)) { // Note newline could be CR + LF.
                    addToQueue(x.substring(0, x.length() - newline.length()), false, HARD_LINE_BREAK, node);
                    addToQueue(newline, true, SOFT_LINE_BREAK, node); // isTranslatable==true so that this won't start new docpart
                } else {
                    LOGGER.warn("HardLineBreak nodes is not ending with a newline.");
                    addToQueue(x, false, HARD_LINE_BREAK, node);
                }
            }
        }),
        new VisitHandler<>(Heading.class, new Visitor<Heading>() {
            @Override public void visit(Heading node) {
                if (node.getOpeningMarker() != BasedSequence.NULL) {
                    addToQueue(node.getOpeningMarker().toString() + " ", false, HEADING_PREFIX, node);
                }
                visitor.visitChildren(node);
                if (node.getClosingMarker() != BasedSequence.NULL) {
                    addNewline(node);
                    addToQueue(node.getClosingMarker().toString(), false, HEADING_UNDERLINE, node);
                }
                addNewline(node); // We always add a newline.
                // Caveat: This causes an extra newline if the input document ends with this header without a newline.
                // This is inevitable because flexmark generated node structure doesn't have enough information.
            }
        }),
        new VisitHandler<>(HtmlBlock.class, node -> {
			visitHtmlBlockBase(node, HTML_BLOCK);
			if (node.getChars().endsWith(newline)) {
				addNewline(node);
			}
		}),
        new VisitHandler<>(HtmlCommentBlock.class, node -> {
			visitHtmlBlockBase(node, HTML_COMMENT_BLOCK);
			if (node.getChars().endsWith(newline)) {
				addNewline(node);
			}
		}),
        new VisitHandler<>(HtmlEntity.class, node -> {
		/* Handles "&amp;", "&#39;" etc. These are just handled as regular text at this point
		 * because they don't require special processing at the filter level. Also this is
		 * the only way to reproduce the original text; if we were to convert "&#39;" to "'",
		 * we can't convert it back to "&#39;" reliably because it could have been "&quot;" or
		 * the literal single quote.
		 */
			addToQueue(node.getChars().toString(), true, TEXT, node); // HTML_ENTITY is not used.
		}),
        new VisitHandler<>(HtmlInline.class, new Visitor<HtmlInline>() {
            @Override public void visit(HtmlInline node) {
                addToQueue(node.getChars().toString(), false, HTML_INLINE, node);
                visitor.visitChildren(node);
            }
        }),
        new VisitHandler<>(HtmlInlineComment.class, new Visitor<HtmlInlineComment>() {
            @Override public void visit(HtmlInlineComment node) {
                addToQueue(node.getChars().toString(), false, HTML_INLINE_COMMENT, node);
                visitor.visitChildren(node);
            }
        }),
        new VisitHandler<>(HtmlInnerBlock.class, node -> visitHtmlBlockBase(node, HTML_INNER_BLOCK)),
        new VisitHandler<>(HtmlInnerBlockComment.class, node -> visitHtmlBlockBase(node, HTML_INNER_BLOCK_COMMENT)),
        new VisitHandler<>(Image.class, node -> visitInlineLink(node, IMAGE)),
        new VisitHandler<>(ImageRef.class, node -> visitRefLink(node, IMAGE_REF)),
        new VisitHandler<>(IndentedCodeBlock.class, node -> {
            String prevLinePrefix = linePrefix;
            linePrefix = prevLinePrefix + "    ";
            addToQueue(linePrefix, false, LINE_PREFIX, node);
			for (BasedSequence seq: node.getContentLines()) {
				addToQueue(seq.toString(), true, TEXT, node); // Use TEXT to indicate this is not the indentation marker.
			}
			addToQueue("", false, END_TEXT_UNIT, node);
            linePrefix = prevLinePrefix;
            addToQueue(linePrefix, false, LINE_PREFIX, node);
		}),
        new VisitHandler<>(Link.class, node -> visitInlineLink(node, LINK)),
        new VisitHandler<>(LinkRef.class, node -> visitRefLink(node, LINK_REF)),
        new VisitHandler<>(MailLink.class, node -> addToQueue(node.getChars().toString(), false, MAIL_LINK, node)),
        new VisitHandler<>(Paragraph.class, new Visitor<Paragraph>() {
            @Override public void visit(Paragraph node) {
                visitor.visitChildren(node);
                if (node.getChars().endsWith(newline)) {
                    // Only Paragraph nodes know how the paragraph ends.
                    // Its child Text node does not include the newline sequence.
                    // Note a paragraph can end without a newline if it is part of the last list item
                    // that the file does not end with a new line sequence.
                    addNewline(node);
                }
            }
        }),
        new VisitHandler<>(OrderedList.class, node -> visitListBlock(node, ORDERED_LIST)),
        new VisitHandler<>(OrderedListItem.class, node -> visitListItem(node, ORDERED_LIST_ITEM)),
        new VisitHandler<>(Reference.class, node -> visitReferenceDefinition(node, REFERENCE)),
        new VisitHandler<>(SoftLineBreak.class, node -> addToQueue(newline, true, SOFT_LINE_BREAK, node)),
        new VisitHandler<>(StrongEmphasis.class, node -> visitDelimitedNode(node, STRONG_EMPHASIS)),
        new VisitHandler<>(Subscript.class, node -> visitDelimitedNode(node, SUBSCRIPT)),
        new VisitHandler<>(Strikethrough.class, node -> visitDelimitedNode(node, STRIKETHROUGH)),
        new VisitHandler<>(Text.class, node -> {
			if (node.getChars().toString().isEmpty()) {
				return; // No content to create token
			}

			// A text node is translatable if it is not a whitespace block.
			if (!node.getChars().toString().trim().isEmpty()) {
				addToQueue(node.getChars().toString(), true, TEXT, node);
			} else {
		// Even if it is a whitespace block, if it follows a
		// a translatable text, or an (non-translatable) inline element
		// we consider it translatable. This is to avoid a situation where
		// A run of text like:
		// 	  Here is **strongly** *emphasized* text.
		// gets broken up into two text units because there is
		// a Text node representing just one space between
		// ** and *. (Issue #715)
		// Note: there may be edge cases that this strategy might
		// not work.
		MarkdownToken lastToken = tokenQueue.peekLast();
		if (lastAddedTranslatableContent
			||(lastToken!=null && lastToken.getType().isInline())) {
		addToQueue(node.getChars().toString(), true, TEXT, node);
		} else {
		addToQueue(node.getChars().toString(), false, TEXT, node);
		}
	}
		}),
        new VisitHandler<>(TextBase.class, new Visitor<TextBase>() {
            @Override public void visit(TextBase node) {
                visitor.visitChildren(node);
            }
        }),
        new VisitHandler<>(ThematicBreak.class, node -> {
			addToQueue(node.getChars().toString(), false, THEMATIC_BREAK, node);
			addNewline(node);
		}),
        new VisitHandler<>(WhiteSpace.class, new Visitor<WhiteSpace>() {
            @Override public void visit(WhiteSpace node) {
                visitor.visitChildren(node);
            }
        }),


        /* Table nodes */

        new VisitHandler<>(TableBlock.class, new Visitor<TableBlock>() {
            @Override public void visit(TableBlock node) {
                visitor.visitChildren(node);
                if (!node.getChars().endsWith(newline)) {
                    // The table block was at the end of file that ends without a newline.
                    // In that case, we remove the last inserted SOFT_LINE_BREAK for the last TableRow.
                    tokenQueue.removeLast();
                }                
            }
        }),
        new VisitHandler<>(TableBody.class, new Visitor<TableBody>() {
            @Override public void visit(TableBody node) {
                visitor.visitChildren(node); // Has multiple TableRow children
            }
        }),
        new VisitHandler<>(TableCaption.class, new Visitor<TableCaption>() {
            @Override public void visit(TableCaption node) {
                visitor.visitChildren(node);
            }
        }),
        new VisitHandler<>(TableCell.class, new Visitor<TableCell>() {
            @Override public void visit(TableCell node) {
                addToQueue("| ", false, TABLE_PIPE, node); // Start each cell in row with a pipe
                Node cn = node.getFirstChild();
                if ( cn == node.getLastChild() && // This is the only child
                        cn instanceof Text &&
                        cn.getChars().toString().equals(" ")) {
                    // Empty cell requires special treatment because
                    // the child is always a Text of one space
                    // no matter how many spaces are there between the pipes
                    int ns = node.getTextLength() - 2; // 2 = "| ".length.
                    if (!node.getOpeningMarker().isEmpty()) {
                        // The cell in the first column has both opening and closing marker.
                        // Other cells have only the closing marker.
                        ns--;
                    }
                    addToQueue( StringUtil.repeatChar(' ', ns), false, WHITE_SPACE, node);
                } else {
                    visitor.visitChildren(node);
                    addToQueue(" ", false, WHITE_SPACE, node); // Padding after table cell content
                }
            }
        }),
        new VisitHandler<>(TableHead.class, new Visitor<TableHead>() {
            @Override public void visit(TableHead node) {
                visitor.visitChildren(node); // Child is TableRow
            }
        }),
        new VisitHandler<>(TableRow.class, new Visitor<TableRow>() {
            @Override public void visit(TableRow node) {
                visitor.visitChildren(node);
                addToQueue("|", false, TABLE_PIPE, node); // Ending pipe for row
                addToQueue(newline, false, SOFT_LINE_BREAK, node);
            }
        }),
        new VisitHandler<>(TableSeparator.class, node -> {
		/*
		 * TableSeparator represents a special row that separates the table header
		 * and the table body. Its only child is a TableRow which has one or more
		 * TableCells. Each TableCell has one Text which is 3 dashes ore more,
		 * optionally lead and/or followed by a colon, i.e. "----", ":---", ":-------:".
		 */
			String nodeText = node.getChars().toString();
			if(nodeText.endsWith("\r"))		//fix for issue #728
				nodeText = nodeText.substring(0, nodeText.length()-1);
			addToQueue(nodeText, false, TABLE_SEPARATOR, node);
			addToQueue(newline, false, SOFT_LINE_BREAK, node);
		})
    );

    // Add a softbreak to terminate a list item, etc.
    private void addNewline(Node node) {
	    addToQueue(newline, false, SOFT_LINE_BREAK, node);
    }
    
    private void visitDelimitedNode(DelimitedNode node, MarkdownTokenType type) {
        // Note: StrikeTrhough and Subscript don't inherit DelimitedNodeImpl but they
        // do implement DelimitedNode and are Nodes. Type coercsion was necessary
        // to avoid duplicate code.
        // Note that within the Emphasis, StrongEmphasis, etc., there can be any inline elements,
        // such as Link, and Emphasis, StrongEmphasis themselves. 
        assert node instanceof Node;
        addToQueue(node.getOpeningMarker().toString(), false, type, (Node) node);
        visitor.visitChildren((Node) node);
        addToQueue(node.getClosingMarker().toString(), false, type, (Node) node);
    }

    private void visitHtmlBlockBase(HtmlBlockBase node, MarkdownTokenType type) {
	boolean shouldTranslate = !type.equals(HTML_COMMENT_BLOCK) && ! type.equals(HTML_INNER_BLOCK_COMMENT);
        addToQueue(node.getChars().toString().trim(), shouldTranslate, type, node);

        for (Node child: node.getChildren()) {
            visitor.visit(child);
        }
    }

    /*
     * Visits an Image node or a Link node.
     * The main text of an Image node is an alt text, which may, or may not be extracted
     * depending on the config setting.
     * The main text of the Link node has a substructure in the node's children.
     */
    private void visitInlineLink(InlineLinkNode node, MarkdownTokenType type) {
        // Do our best to consolidate this markup into a small number of tags.
	StringBuilder sb = new StringBuilder();
	if (node instanceof Image) {
	    if (params.getTranslateImageAltText()) {
		addToQueue(node.getTextOpeningMarker().toString(), false, type, node);
		visitor.visitChildren(node); // Note: This could be "".
		sb.append(node.getTextClosingMarker());
	    } else {
		sb.append(node.getTextOpeningMarker().toString())
		  .append(node.getText().toString()) // Note: This could be "".
		  .append(node.getTextClosingMarker());
	    }
	} else { // Must be a Link node.
	    assert node instanceof Link;
	    addToQueue(node.getTextOpeningMarker().toString(), false, type, node);
	    visitor.visitChildren(node);
	    sb.append(node.getTextClosingMarker());
	}
	sb.append(node.getLinkOpeningMarker());
        sb.append(node.getUrlOpeningMarker());
        if (shouldTranslateUrl(node)) {
            addToQueue(sb.toString(), false, type, node);
            sb.setLength(0);
            addToQueue(node.getUrl().toString(), true, MarkdownTokenType.TEXT, node);
        } else {
            sb.append(node.getUrl());
        }
        sb.append(node.getUrlClosingMarker());
        if (isDefined(node.getTitle())) {
            sb.append(" ").append(node.getTitleOpeningMarker());
            addToQueue(sb.toString(), false, type, node);
            addToQueue(node.getTitle().toString(), true, MarkdownTokenType.TEXT, node);
            sb = new StringBuilder(node.getTitleClosingMarker());
        }
        sb.append(node.getLinkClosingMarker());
        addToQueue(sb.toString(), false, type, node);
    }

    /*
     * Visits a LinkRef or ImageRef node.
     * 
     * LinkRef represents: 
     * 		[anchor text][reference-text]		reference-text is not shown in this case
     * or:
     *          some text [reference-text] other text	reference-text is shown and thus should be extracted
     *          
     * ImageRef represents:
     * 		![alt text that can be empty][reference-text]
     * 
     * Note: Alt text of ImageRef should ideally be extracted in a separate TextUnit than the TextUnit that
     * captures the main flow of text. But under the current implementation, it is embeded in the main
     * TextUnit separated by placeholders. 
     * 
     * @param node The LinkRef or ImageRef node being visited
     * @param type Either LINK_REF or IMAGE_REF
     */
    private void visitRefLink(RefNode node, MarkdownTokenType type) {
        if (isDefined(node.getText())) {
            if (node instanceof ImageRef) {
                addToQueue(node.getTextOpeningMarker().toString(), false, type, node);
                addToQueue(node.getText().toString(), true, TEXT, node); // IMAGE_REF would be treated as a code.
                addToQueue(node.getTextClosingMarker().toString(), false, type, node);
            } else { // Must be LinkRef. The text can be marked up and is stored in its children.
                addToQueue(node.getTextOpeningMarker().toString(), false, type, node);
                visitor.visitChildren(node);
                addToQueue(node.getTextClosingMarker().toString(), false, type, node);
            }
        } else {
            if (node instanceof ImageRef) { // This happens in case like: ![][ref-text]
                addToQueue(node.getTextOpeningMarker().toString() + node.getTextClosingMarker().toString(),
                	false, type, node);
            } 
            // Note: When visiting LinkRef, it is possible that node.getText() is not defined.
            // For example:
            // .... visit [reference article 1] for more information.
            // .
            // [reference site 1]: http://foo.com/article/1
        }
        if (isDefined(node.getReferenceOpeningMarker())) {
            addToQueue(node.getReferenceOpeningMarker().toString(), false, type, node);

            if (isDefined(node.getReference())) {
                String refText = node.getReference().toString();
                if (isVisibleRef(refText)) {
                    visitor.visitChildren(node); // There should be Text node and other inline nodes as children.
                } else {
                    addToQueue(refText, false, type, node);
                }
            } else if("[ ]".equals(node.getChars().toString())) {	// fix for issue #727
            	// Note: The task list's check boxes [ ] and [x] are handled here, by accident.
            	// @TODO: Use TaskListExtension for proper support
            	addToQueue(" ", false, WHITE_SPACE, node); // empty checkbox for task list. add padding space 
            } else {
        	LOGGER.warn("{} node [{}] reports a reference opening marker but the reference is empty.", 
        		node.getClass().getName(), node.toAstString(false));
            }
            
            if (isDefined(node.getReferenceClosingMarker())) {
        	addToQueue(node.getReferenceClosingMarker().toString(), false, type, node);
            } else {
        	LOGGER.warn("{} node [{}] reports a reference opening marker but lacks a closing marker.", 
        		node.getClass().getName(), node.toAstString(false));
            }
        }
    }

    /*
     * Visits a Reference node, which represents a Markdown construct that looks like:
     * [ref-text-that-may-be-translatable]: http://some/url "Optional title text"
     * 
     * See http://spec.commonmark.org/0.28/#reference-link
     * @param node The Reference node being visited
     * @param type Always REFERENCE
     */
    private void visitReferenceDefinition(Reference node, MarkdownTokenType type) {
	assert type.equals(REFERENCE);
        addToQueue(node.getOpeningMarker().toString(), false, type, node);
        String refText = node.getReference().toString();
        addToQueue(refText, isVisibleRef(refText), type, node);
        addToQueue(node.getClosingMarker().toString() + " ", false, type, node);
        if (isDefined(node.getUrlOpeningMarker())) {
            addToQueue(node.getUrlOpeningMarker().toString(), false, type, node);
        }
        if (shouldTranslateUrl(node)) {
            addToQueue(node.getUrl().toString(), isRefTextUsed(refText), type, node);
        } else {
            addToQueue(node.getUrl().toString(), false, type, node);   
        }
        if (isDefined(node.getUrlClosingMarker())) {
            addToQueue(node.getUrlClosingMarker().toString(), false, type, node);
        }
        if (isDefined(node.getTitle())) {
            addToQueue(" " + node.getTitleOpeningMarker().toString(), false, type, node);
            addToQueue(node.getTitle().toString(), isRefTextUsed(refText), type, node);
            addToQueue(node.getTitleClosingMarker().toString(), false, type, node);
        }
        addToQueue(newline, false, type, node);
    }

    private void visitListBlock(ListBlock listBlock, MarkdownTokenType type) {
        visitor.visitChildren(listBlock);
    }

    private void visitListItem(ListItem listItem, MarkdownTokenType type) {
        addToQueue(listItem.getOpeningMarker().toString() + " ", false, type, listItem);
        if (!listItem.hasChildren()) { // An empty list item, e.g. just "1." In that case, there is no Paragraph or other node that would produce a newline.
            addNewline(listItem);
        } else {
            String prevLinePrefix = linePrefix;
            linePrefix = prevLinePrefix + "   ";
            addToQueue(linePrefix, false, LINE_PREFIX, listItem);

            if (!(listItem.getFirstChild() instanceof Paragraph)) { // An empty list item, followed by a blank line. The blank line is considered to be a child.
                addNewline(listItem);
            }
            visitor.visitChildren(listItem);
            linePrefix = prevLinePrefix;
            addToQueue(linePrefix, false, LINE_PREFIX, listItem);
        }
    }

    private boolean isDefined(BasedSequence sequence) {
        return sequence != BasedSequence.NULL && !sequence.isEmpty();
    }

    // Determines whether to extract the URL or not.
    private boolean shouldTranslateUrl(LinkNodeBase node) {
	return params.getTranslateUrls() && isDefined(node.getUrl())
	    && urlPatternToTranslate.matcher(node.getUrl().toString()).matches();
    }

    // Traverse the last child and its last child etc. to see if
    // we reach a Paragraph.
    private boolean hasDescendentParagraph(Node node) {
        return hasDecendentOf(Paragraph.class, node);
    }

    private boolean hasDescendentBlankLine(Node node) {
        return hasDecendentOf(BlankLine.class, node);
    }

    private boolean hasDecendentOf(Class<?> nodeClass, Node node) {
        while (node.getLastChild()!=null) {
            node = node.getLastChild();
            if (nodeClass.isInstance(node)) return true;
        }
        return false;
    }

}
