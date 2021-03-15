/*===========================================================================
  Copyright (C) 2018 by the Okapi Framework contributors
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

package net.sf.okapi.filters.tex;

import static net.sf.okapi.common.filters.FilterTestUtil.assertDocumentPart;
import static net.sf.okapi.common.filters.FilterTestUtil.assertTextUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.EventType;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfiguration;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filters.InputDocument;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;

@RunWith(JUnit4.class)
public class TEXFilterTest {

	private TEXFilter filter;
	private TEXEncoder encoder;
	private LocaleId locEN = LocaleId.fromString("en");
	private FileLocation root;

	@Before
	public void setUp() {
		filter = new TEXFilter();
		encoder = new TEXEncoder();
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testDefaultInfo () {
		assertNull(filter.getParameters());
		assertNotNull(filter.getName());
		List<FilterConfiguration> list = filter.getConfigurations();
		assertNotNull(list);
		assertTrue(list.size()>0);
	}

	@Test
	public void testJava8Split () {
		// Check Java 8 split behavior
		String[] chars = "text".split(""); // No more empty lead string in Java 8
		assertEquals(4, chars.length);
		assertEquals("t", chars[0]);
		// test for empty string
		chars = "".split("");
		assertEquals(1, chars.length);
		assertEquals("", chars[0]);
	}
	
	@Test
	public void testStartDocument () {
		InputDocument id = new InputDocument(root.in("/Test01.tex").toString(), null);
		assertTrue("Problem in StartDocument", FilterTestDriver.testStartDocument(filter,
			id,
			"UTF-8", locEN, locEN));
	}
	@Test
	public void testMathMode() {
		String snippet = "\\begin{document}\n%\\title{Tilde's ijcnlp 2017 submission}\n" + "$$3*4^6$$\n"
				+ "% nokomentēju, jo nav publicēts\n"+"Parasts teksts";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		assertDocumentPart(events.get(1), "\\begin{document}\n%\\title{Tilde's ijcnlp 2017 submission}\n$$3*4^6$$\n% nokomentēju, jo nav publicēts\n");
		assertTextUnit(events.get(2), "Parasts teksts");
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	@Test
	public void testComments () {
		String snippet = "%\\title{Tilde's ijcnlp 2017 submission}\n" + "\n" + "% File ijcnlp2017.tex\n"
				+ "%3\n\n\n" + "% nokomentēju, jo nav publicēts\n"+"% nokomentēju, jo nav publicēts";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		assertEquals("%\\title{Tilde's ijcnlp 2017 submission}\n" + "\n" + "% File ijcnlp2017.tex\n"
				+ "%3\n\n\n" + "% nokomentēju, jo nav publicēts\n"+"% nokomentēju, jo nav publicēts",events.get(1).getResource().toString());
		assertDocumentPart(events.get(1), "%\\title{Tilde's ijcnlp 2017 submission}\n" + "\n" + "% File ijcnlp2017.tex\n"
				+ "%3\n\n\n" + "% nokomentēju, jo nav publicēts\n"+"% nokomentēju, jo nav publicēts");
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	@Test
	public void testRussian () {
		String snippet = "\\documentclass{article}\n" + 
				" \n" + 
				"%Russian-specific packages\n" + 
				"%--------------------------------------\n" + 
				"\\usepackage[T2A]{fontenc}\n" + 
				"\\usepackage[utf8]{inputenc}\n" + 
				"\\usepackage[russian]{babel}\n" + 
				"%--------------------------------------\n" + 
				" \n" + 
				"%Hyphenation rules\n" + 
				"%--------------------------------------\n" + 
				"\\usepackage{hyphenat}\n" + 
				"\\hyphenation{ма-те-ма-ти-ка вос-ста-нав-ли-вать}\n" + 
				"%--------------------------------------\n" + 
				" \n" + 
				"\\begin{document}\n" + 
				" \n" + 
				"\\tableofcontents\n" + 
				" \n" + 
				"\\begin{abstract}\n" + 
				"  Это вводный абзац в начале документа.\n" + 
				"\\end{abstract}";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		assertEquals("  ",events.get(2).getTextUnit().getSource().getFirstContent().getText());
		assertEquals("  Это вводный абзац в начале документа.",events.get(4).getTextUnit().getSource().getFirstContent().getText());
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	
	@Test
	public void testSplitTUonNewlines () {
		String snippet = "\\begin{document}\nFirst text\n" + "\n" + "Second text\n" + "Third text";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		assertEquals("#events", 6, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
		assertEquals("First text",events.get(2).getTextUnit().getSource().getFirstContent().getText());
		assertEquals("Second text\nThird text", events.get(4).getResource().toString());
        assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	@Test
	public void testSplitTUonNewlines2 () {
		String snippet = "\\begin{document}\nThe as \\textit{\\textbf{2008.} gada 30. j\\=ulijs}), terms (e.g., \\textit{drop (piliens)} is translated as \\textit{injekcija (injection)}) and entities (e.g., \\textit{Naglazyme (Naglazyme)} is \\textit{MabCampath}).\n" + 
				"\n" + 
				"Latvian also has a relatively free word order. In, it has a rather system (9\\% of errors), while impact.\n" + 
				"\n" + 
				"Errors for NMT (15\\%) than for SMT outputs (10\\%). NMT (4\\%) word SMT (9\\%), while less (8\\%) spelling errors than NMT (11\\%).\n" + 
				"\n" + 
				"%Finally, errors (324 cases or 8.43\\%) mostly, it word \"j\\=us\" (\"you\") which capitalized, but capitalized politeness.\n" + 
				"\n" + 
				"%We can conclude that two types of wide-spread SMT errors, that form almost 30\\% of all errors, little of the text.   \n" + 
				"\n" + 
				"\\subsection{Inter-annotator Agreement} \\label{agreement}\n" + 
				"Although , 200 segments two annotators  project \\footnote{http://www.qt21.eu/} by researchers/issues annotation levels (whether or not). Table \\ref{annotator-stats-table} presents summary ."
				+ "\n";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		assertEquals("#events", 11, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
		assertEquals("The as 2008. gada 30. jūlijs), terms (e.g., drop (piliens) is translated as injekcija (injection)) and entities (e.g., Naglazyme (Naglazyme) is MabCampath).",events.get(2).getTextUnit().getSource().getFirstContent().getText());
		assertEquals("Inter-annotator Agreement",events.get(8).getTextUnit().getSource().getFirstContent().getText());		
		assertEquals(" Although , 200 segments two annotators  project http://www.qt21.eu/ by researchers/issues annotation levels (whether or not). Table  presents summary .", events.get(9).getTextUnit().getSource().getFirstContent().getText());
        assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	
	@Test @Ignore
	public void testCommentsInText () {
		String snippet = "%\\title{Tilde's ijcnlp 2017 submission}\n" + "\n" + "This is some text% File ijcnlp2017.tex\n" + "%3";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		ITextUnit tu = FilterTestDriver.getTextUnit(getEvents(snippet),1);
		assertDocumentPart(events.get(1), "%\\title{Tilde's ijcnlp 2017 submission}\n\n");
		tu = FilterTestDriver.getTextUnit(getEvents(snippet),2);
		assertNull(tu);//this is not a text unit
		assertEquals("This is some text% File ijcnlp2017.tex\n%3",events.get(2).getResource().toString());
		assertEquals("This is some text\n",events.get(2).getTextUnit().getSource().getFirstContent().getText());
		
        String expected = "This is some text% File ijcnlp2017.tex\n%3";
		String result = FilterTestDriver.generateOutput(getEvents("This is some text% File ijcnlp2017.tex\n%3"),
				filter.getEncoderManager(), LocaleId.FRENCH);
		assertEquals(expected, result);
		
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}

	@Test
	public void testSimpleText() {
		String snippet = "\\begin{document}\nLa: lala\naij,aij,aij";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);

        assertEquals("#events", 4, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertEquals(EventType.DOCUMENT_PART, events.get(1).getEventType());
        assertEquals(EventType.TEXT_UNIT, events.get(2).getEventType());

        String expected = "\\begin{document}\nLa: lala\naij,aij,aij";
		String result = FilterTestDriver.generateOutput(getEvents(snippet),
				filter.getEncoderManager(), LocaleId.FRENCH);
		assertEquals(expected, result);
        
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	
	@Test
	public void testRunawayCurly() {
		String snippet = "\\begin{document}\n\\newcommand{\\code}[1]{\\small\\texttt{#1}}\n" + 
				"\\title{Tilde's Machine Translation Systems for WMT 2017}";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//        int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
		assertEquals("#events", 5, events.size());
		assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
		assertEquals("[1]{\\small\\texttt{#1}}\n", events.get(2).getResource().toString());
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	
	@Test
	public void testOneArgNoTextCommands() {
		String snippet = "%\n" + "\n" + "\\documentclass[11pt, letterpaper]{article}\n" + "\\usepackage{ijcnlp2017}\n" + 
				"\\usepackage{times}\n" + "\\usepackage{placeins}\n" + "\n" + "% Uncomment this line for the final submission:\n" + 
				"\\ijcnlpfinalcopy";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//        int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "%\n\n\\documentclass[11pt, letterpaper]{article}\n\\usepackage{ijcnlp2017}\n"
        		+ "\\usepackage{times}\n\\usepackage{placeins}\n\n% Uncomment this line for the final submission:\n"
        		+ "\\ijcnlpfinalcopy");        
        assertDocumentPart(events.get(1), "%\n\n\\documentclass[11pt, letterpaper]{article}\n\\usepackage{ijcnlp2017}\n"
        		+ "\\usepackage{times}\n\\usepackage{placeins}\n\n% Uncomment this line for the final submission:\n"
        		+ "\\ijcnlpfinalcopy");
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	
	@Test
	public void testOneArgInlineTextCommands() {
		String snippet = "\\begin{document}\n%\n" + "\n" + "\\textbf{article}\n" + "\\hbox{ijcnlp2017}\n" + 
				" And some ordinary text" + "\\hbox{and more, weird text} \n"+ "{\\tt and one more different style command}\n";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
		
//        int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        
        assertEquals("#events", 4, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{document}\n%\n\n");

        String expected = "\\begin{document}\n\\textbf{article}\n\\hbox{ijcnlp2017}\n And some ordinary text\\hbox{and more, weird text}\n{\\tt and one more different style command}\n";
		String result = FilterTestDriver.generateOutput(getEvents("\\begin{document}\n\\textbf{article}\n" + "\\hbox{ijcnlp2017}\n" + 
				" And some ordinary text" + "\\hbox{and more, weird text}\n"+ "{\\tt and one more different style command}\n"),
				filter.getEncoderManager(), LocaleId.FRENCH);
		assertEquals(expected, result);
        
        assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
		
	@Test
	public void testoneArgParaTextCommands() {
		String snippet = "\\begin{document}\n%\n" + "\n" + "\\title{Installing \\LaTeX}\n" + "La la la some paragraph\n" + 
				"\\section{One [just text] more section}\n" + "Some text \n" + "\n" + "Split with many newlines";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 11, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{document}\n%\n\n");
        assertEquals("Installing \\LaTeX",events.get(2).getResource().toString());
//		assertTextUnit(events.get(4), "\\title{Installing \\LaTeX}");// contains codes
        assertEquals("La la la some paragraph\n", events.get(4).getResource().toString());
        assertEquals("La la la some paragraph",events.get(4).getTextUnit().getSource().getFirstContent().getText());
        assertEquals("One [just text] more section",events.get(5).getResource().toString());
        assertEquals("Some text \n",events.get(7).getResource().toString());
        assertEquals("Split with many newlines",events.get(9).getResource().toString());
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}

	@Test
	public void testHeaderCommands() {
		String snippet = "\\usepackage{amssymb}\n\\setcounter{tocdepth}{3}"
				+ "\n\\urldef{\\mailsb}\\path|{daiga.deksne, toms.miks}@tilde.lv|\n" + 
				"\\urldef{\\mailsc}\\path|other_mails_if_needed|    \n" + 
				"%\\urldef{\\mailsa}\\path||";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        // assertDocumentPart(events.get(1));
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	@Test
	public void testHeaderText() {
		String snippet = "\\begin{document}\n\\newcommand{\\keywords}[1]{\\par\\addvspace\\baselineskip\n" + 
				"\\noindent\\keywordname\\enspace\\ignorespaces#1}";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 4, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        // assertDocumentPart(events.get(1));
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());
	}
	
	@Test
	public void testLatvianSymbols() {
		String snippet = "\\begin{document}%Šis ir koments - ar to nekas nav jādara\n"+"\\={a}\\v{c}\\={e}\\v{g}\\={\\i}\\c{k}\\c{l}\\c{n}\\v{s}\\={u}\\v{z}\n\n"
				+ "\\={A}\\v{C}\\={E}\\c{G}\\={I}\\c{K}\\c{L}\\c{N}\\v{S}\\={U}\\v{Z}\n\n"
				+ "\\=a\\v c\\=e\\v g\\=\\i\\c k\\c l\\c n\\v s\\=u\\v z\n\n"
				+ "\\=A\\v C\\=E\\c G\\=I\\c K\\c L\\c N\\v S\\=U\\v Z\n\n"
				+ "\\v S\\={\\i} L\\={\\i}nija p\\=Arbaud\\=a latvie\\v{s}u simbolu att\\=elo\\v{s}anu.";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 12, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{document}%Šis ir koments - ar to nekas nav jādara\n");
        assertEquals("āčēģīķļņšūž\n",events.get(2).getResource().toString());
        assertEquals("ĀČĒĢĪĶĻŅŠŪŽ\n",events.get(4).getResource().toString());
        assertEquals("āčēģīķļņšūž\n",events.get(6).getResource().toString());
        assertEquals("ĀČĒĢĪĶĻŅŠŪŽ\n", events.get(8).getResource().toString());
        assertEquals("Šī Līnija pĀrbaudā latviešu simbolu attēlošanu.", events.get(10).getResource().toString());
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
		
	}

	@Test
	public void testLatvianSymbolsEscaping() {
		String snippet = "\\begin{document}\n%Šis ir koments - ar to nekas nav jādara\n"
				+ "āčēģīķļņšūž\n\n"
				+ "ĀČĒĢĪĶĻŅŠŪŽ\n\n"
				+ "Šī Līnija pĀrbaudā latviešu simbolu attēlošanu.";
        ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 8, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{document}\n%Šis ir koments - ar to nekas nav jādara\n");
        assertEquals("\\={a}\\v{c}\\={e}\\c{g}\\={\\i}\\c{k}\\c{l}\\c{n}\\v{s}\\={u}\\v{z}\n",encoder.toNative("",events.get(2).getResource().toString()));
        assertEquals("\\={A}\\v{C}\\={E}\\c{G}\\={I}\\c{K}\\c{L}\\c{N}\\v{S}\\={U}\\v{Z}\n",encoder.toNative("",events.get(4).getResource().toString()));
        assertEquals("\\v{S}\\={\\i} L\\={\\i}nija p\\={A}rbaud\\={a} latvie\\v{s}u simbolu att\\={e}lo\\v{s}anu.", encoder.toNative("",events.get(6).getResource().toString()));
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
	}

	@Test
	public void testTable() {
		String snippet = "\\begin{table}[]\n" + 
				"\\centering\n" + 
				"\\begin{tabular}{|lcc|}\n" + 
				"\\hline\n" + 
				"\\multicolumn{1}{|c}{\\multirow{2}{*}{\\textbf{Corpus}}} & \\textbf{Sentences} & \\textbf{Sentences} \\\\\n" + 
				"\\multicolumn{1}{|c}{} & \\textbf{before filtering} & \\textbf{after filtering} \\\\ \\hline\n" + 
				"Parallel & \\multicolumn{1}{r}{378,869} & \\multicolumn{1}{r|}{325,332} \\\\\n" + 
				"Monolingual & \\multicolumn{1}{r}{378,869} & \\multicolumn{1}{r|}{332,652} \\\\ \\hline\n" + 
				"\\end{tabular}\n" + 
				"\\caption{Statistics of the training corpora}\n" + 
				"\\label{data-table}\n" + 
				"\\end{table}";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{table}[]\n\\centering\n\\begin{tabular}{|lcc|}\n\\hline\n\\multicolumn{1}{|c}{\\multirow{2}{*}{\\textbf{Corpus}}} & \\textbf{Sentences} & \\textbf{Sentences} \\\\\n\\multicolumn{1}{|c}{} & \\textbf{before filtering} & \\textbf{after filtering} \\\\ \\hline\nParallel & \\multicolumn{1}{r}{378,869} & \\multicolumn{1}{r|}{325,332} \\\\\nMonolingual & \\multicolumn{1}{r}{378,869} & \\multicolumn{1}{r|}{332,652} \\\\ \\hline\n\\end{tabular}\n\\caption{Statistics of the training corpora}\n\\label{data-table}\n\\end{table}");
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
	}
	@Test 
	public void testTable2() {
		String snippet = "\\begin{table}[]\n" + 
				"\\centering\n" + 
				"\\begin{tabular}{|lrrr|}\n" + 
				"\\hline\n" + 
				"\\multicolumn{1}{|c}{\\textbf{System}} & \\multicolumn{1}{c}{\\textbf{BLEU}} & \\multicolumn{1}{c}{\\textbf{NIST}} & \\multicolumn{1}{c|}{\\textbf{ChrF2}} \\\\ \\hline\n" + 
				"SMT & 46.57$\\pm$1.46 & 9.45$\\pm$0.18 & 0.7586 \\\\\n" + 
				"NMT & 38.44$\\pm$1.62 & 8.63$\\pm$0.15 & 0.7065 \\\\ \\hline\n" + 
				"\\end{tabular}\n" + 
				"\\caption{Automatic evaluation results}\n" + 
				"\\label{mt-eval-table}\n" + 
				"\\end{table}\n";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);

        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{table}[]\n\\centering\n\\begin{tabular}{|lrrr|}\n\\hline\n\\multicolumn{1}{|c}{\\textbf{System}} & \\multicolumn{1}{c}{\\textbf{BLEU}} & \\multicolumn{1}{c}{\\textbf{NIST}} & \\multicolumn{1}{c|}{\\textbf{ChrF2}} \\\\ \\hline\nSMT & 46.57$\\pm$1.46 & 9.45$\\pm$0.18 & 0.7586 \\\\\nNMT & 38.44$\\pm$1.62 & 8.63$\\pm$0.15 & 0.7065 \\\\ \\hline\n\\end{tabular}\n\\caption{Automatic evaluation results}\n\\label{mt-eval-table}\n\\end{table}\n");
//        assertDocumentPart(events.get(2), "\n");
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());	
	}
	
	@Test
	public void testEquation() {
		String snippet = "\\begin{equation}\n" + 
				"  S_\\textup{IC} = S_{123}\n" + 
				"\\end{equation}";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//        int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertDocumentPart(events.get(1), "\\begin{equation}\n  S_\\textup{IC} = S_{123}\n\\end{equation}");
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
	}

	@Test
	public void testHierarchy() {
		String snippet = "\\title{NMT or SMT: Case Study of a Narrow-domain English-Latvian Post-editing Project by Inguna Skadi\\c{n}as}\n" + 
				"For inter-annotator agreement, we calculated free-marginal kappa in three different conditions (see Table \\ref{agreement-table}): \\emph{perfect \\textbf{match} analysis}" + 
				"\\footnote{Free-marginal kappa is interpreted as: 0.01-0.20 = slight agreement, 0.21-0.40 = fair agreement, 0.41-0.60 = moderate agreement, 0.61-0.80 = substantial agreement, 0.81-1.00 = almost perfect agreement \\cite{landis1977measurement}}.";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
//        assertEquals("#events", 10, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertEquals("NMT or SMT: Case Study of a Narrow-domain English-Latvian Post-editing Project by Inguna Skadiņas",events.get(1).getTextUnit().getSource().getFirstContent().getText());

		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
	}
	
	@Test
	public void testDemoFile() throws Exception  {
        RawDocument rd =  new RawDocument(getFileContents("/Test01.tex"), LocaleId.ENGLISH);
        
        try (TEXFilter filter = new TEXFilter()) {
            ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, rd);
//    		int i = 0;
//    		for (Event event : events) {
//    			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//    			i = i+1;
//    		}
            assertEquals("#events", 5, events.size());
            assertEquals("Installing ",events.get(0).getTextUnit().getSource().getFirstContent().getText());
            assertEquals("Jason, Gross",events.get(1).getTextUnit().getSource().getFirstContent().getText());
            assertEquals("translated",events.get(3).getTextUnit().getSource().getFirstContent().getText());
         }
    }
	@Test
	public void testDemoFileWin() throws Exception  {
        RawDocument rd =  new RawDocument(getFileContents("/Test03.tex"), LocaleId.ENGLISH);
        
        try (TEXFilter filter = new TEXFilter()) {
            ArrayList<Event> events = FilterTestDriver.getTextUnitEvents(filter, rd);
//    		int i = 0;
//    		for (Event event : events) {
//    			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//    			i = i+1;
//    		}
            assertEquals("#events", 5, events.size());
            assertEquals("Installing ",events.get(0).getTextUnit().getSource().getFirstContent().getText());
            assertEquals("Jason, Gross",events.get(1).getTextUnit().getSource().getFirstContent().getText());
            assertEquals("translated",events.get(3).getTextUnit().getSource().getFirstContent().getText());
         }
    }

	@Test
	public void testDemoFile2() throws Exception  {
        RawDocument rd =  new RawDocument(getFileContents("/Test02.tex"), LocaleId.ENGLISH);
        
        try (TEXFilter filter = new TEXFilter()) {
            ArrayList<Event> events;
            events = FilterTestDriver.getTextUnitEvents(filter, rd);
    		assertEquals("#events", 5, events.size()); // there is space on line 2
            
        	events = FilterTestDriver.getEvents(filter, rd, null);
//    		int i = 0;
//    		for (Event event : events) {
//    			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//    			i = i+1;
//    		}
            assertEquals("#events", 13, events.size());
            assertEquals("Предисловие",events.get(4).getTextUnit().getSource().getFirstContent().getText());
            assertEquals("Кириллические символы также могут быть использованы в математическом режиме.",events.get(10).getTextUnit().getSource().getFirstContent().getText());
         }
    }
	
	@Test
	public void testNested() {
		String snippet = 
				"\\newcommand{\\enterProblemHeader}[1]{\n" + 
				"\\nobreak\\extramarks{#1}{#1 continued on next page\\ldots}\\nobreak\n" + 
				"}x\n"
				;
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
//        assertEquals("\tJust text\n",events.get(2).getTextUnit().getSource().getFirstContent().getText());

		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
	}

	
	@Test
	public void testScript() {
		String snippet = "	% Set up the header and footer\n" + 
				"	\\pagestyle{fancy}\n" + 
				"	\\lhead{\\hmwkAuthorName} % Top left header\n" + 
				"	\\chead{\\hmwkClass\\ (\\hmwkClassInstructor\\ \\hmwkClassTime): \\hmwkTitle} % Top center head\n" + 
				"	\\rhead{\\firstxmark} % Top right header\n" + 
				"	\\lfoot{\\lastxmark} % Bottom left footer\n" + 
				"	\\begin{document}\n" +
				"	Just text\n\nMore text" +
				"	\\end{document}" ;
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 6, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertEquals("\tJust text",events.get(2).getTextUnit().getSource().getFirstContent().getText());

		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());		
	}


	@Test
	public void testLineBreaks() {
		String snippet = "\\author{Inguna Skadi\\c{n}a \\and M\\=arcis Pinnis \\\\\n" + 
				"  Tilde, Vienibas gatve 75A, Riga, Latvia \\\\\n" + 
				"  {\\tt \\{inguna.skadina,marcis.pinnis\\}@tilde.lv}}";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
//		int i = 0;
//		for (Event event : events) {
//			System.out.println(i+event.getResource().toString());
//			i = i+1;
//		}
        assertEquals("#events", 3, events.size());
        assertEquals(EventType.START_DOCUMENT, events.get(0).getEventType());
        assertEquals("Inguna Skadiņa Mārcis Pinnis "
        		+ "  Tilde, Vienibas gatve 75A, Riga, Latvia   ",events.get(1).getTextUnit().getSource().getFirstContent().getText());
		assertEquals(EventType.END_DOCUMENT, events.get(events.size() - 1).getEventType());	
	}

	
	private ArrayList<Event> getEvents(String snippet) {
		return FilterTestDriver.getEvents(filter, snippet, locEN);
	}
    private String getFileContents(String filename) throws Exception {
        try (InputStream is = root.in(filename).asInputStream();
                Scanner scanner = new Scanner(is)) {
            return scanner.useDelimiter("\\A").next();
        }
    }
}
