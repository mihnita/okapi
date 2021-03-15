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

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.Event;
import net.sf.okapi.common.FileLocation;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.encoder.EncoderManager;
import net.sf.okapi.common.filters.FilterTestDriver;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.RawDocument;

@RunWith(JUnit4.class)
public class TEXWriterTest {

	private FileLocation root;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

    @Test @Ignore
    public void writeDocumentParts() throws Exception {
        TEXFilter filter = new TEXFilter();
        IFilterWriter writer = filter.createFilterWriter();

        System.out.println("Writer "+ writer.getSkeletonWriter().getClass().getName());
        
        Path path = tempFolder.newFile().toPath();
        OutputStream os = Files.newOutputStream(path);
        writer.setOutput(os);
        writer.setOptions(LocaleId.FRENCH, StandardCharsets.UTF_8.name());

        String contents = getFileContents("/Test01.tex");

        filter.open(new RawDocument(contents, null, null));
        
        while (filter.hasNext()) {
        	Event event = filter.next();
        	writer.handleEvent(event);
        }
        filter.close();
        writer.close();

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(contents, outputData);
    }
    
    @Test
    public void writeComments() throws Exception {
        TEXFilter filter = new TEXFilter();
        IFilterWriter writer = filter.createFilterWriter();

        Path path = tempFolder.newFile().toPath();
        OutputStream os = Files.newOutputStream(path);
        writer.setOutput(os);
        writer.setOptions(LocaleId.FRENCH, StandardCharsets.UTF_8.name());

		String snippet = "%\\title{Tilde's ijcnlp 2017 submission}\n" + "\n" + "% File ijcnlp2017.tex\n"
				+ "%3\nLAlala Kautkads saturs\n\n\n" + "% nokomentēju, jo nav publicēts\nVel saturs\n"+"% nokomentēju, jo nav publicēts";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
		int i = 0;
		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
			i = i+1;
			writer.handleEvent(event);
		}

        filter.close();
        writer.close();

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet, outputData);
    }
    
    @Test
    public void writeBadTable() throws Exception {
        TEXFilter filter = new TEXFilter();
        IFilterWriter writer = filter.createFilterWriter();

        Path path = tempFolder.newFile().toPath();
        OutputStream os = Files.newOutputStream(path);
        writer.setOutput(os);
        writer.setOptions(LocaleId.FRENCH, StandardCharsets.UTF_8.name());

		String snippet = "$$3*4^6$$\n\\subsection{MT System Evaluation} \\label{mt_eval}\n" + 
				"SMT and NMT systems were evaluated on a held-out set of 1000 randomly selected sentence pairs. The automatic evaluation results are given in Table~\\ref{mt-eval-table}. The results show that the SMT system achieves better results than the NMT system. This could be explained by the relatively small size of the parallel corpus and a very narrow domain, i.e., from the small amount of data, SMT learns better terminology and phrases which are specific for the particular narrow domain.\n" + 
				"\\begin{table}[]\n" + 
				"\\centering\n" + 
				"\\begin{tabular}{|lrrr|}\n" + 
				"\\hline\n" + 
				"\\multicolumn{1}{|c}{\\textbf{System}} & \\multicolumn{1}{c}{\\textbf{BLEU}} & \\multicolumn{1}{c}{\\textbf{NIST}} & \\multicolumn{1}{c|}{\\textbf{ChrF2}} \\\\ \\hline\n" + 
				"SMT & 46.57$\\pm$1.46 & 9.45$\\pm$0.18 & 0.7586 \\\\\n" + 
				"NMT & 38.44$\\pm$1.62 & 8.63$\\pm$0.15 & 0.7065 \\\\ \\hline\n" + 
				"\\end{tabular}\n" + 
				"\\caption{Automatic evaluation results}\n" + 
				"\\label{mt-eval-table}\n" + 
				"\\end{table}\n" + 
				"\n" + 
				"% attēls nav labs, varbūt mest ārā?\n" + 
				"%\\begin{figure*}[]\n" + 
				"%\\begin{center}\n" + 
				"%\\includegraphics[width=450px]{wOrder-fixed.jpg}\n" + 
				"%\\end{center}\n" + 
				"%\\caption{\\label{WO}Influence of word order on BLEU score for similar translations into Latvian by SMT (blue) and NMT (green) systems}\n" + 
				"%\\end{figure*}";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
		int i = 0;
		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
			i = i+1;
			writer.handleEvent(event);
		}

        filter.close();
        writer.close();

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet, outputData);
    }
    
    @Test
    public void writeHierarchy() throws Exception {
        TEXFilter filter = new TEXFilter();
        IFilterWriter writer = filter.createFilterWriter();

        Path path = tempFolder.newFile().toPath();
        OutputStream os = Files.newOutputStream(path);
        writer.setOutput(os);
        writer.setOptions(LocaleId.FRENCH, StandardCharsets.UTF_8.name());

		String snippet = "\\documentclass[11pt,letterpaper]{article}\n" + 
				"\\usepackage{ijcnlp2017}\\usepackage{times}\\usepackage{latexsym}\\usepackage{multirow}\\usepackage{graphicx}\\usepackage{color,soul}\\usepackage{todonotes}\\usepackage{placeins}\n" + 
				"\n" + 
				"\n" + 
				"\\newcommand\\BibTeX{B{\\sc ib}\\TeX}\n" + 
				"\\title{NMT or SMT: Case Study of a Narrow-domain English-Latvian Post-editing Project by Inguna Skadiņas}\n" + 
				"\\begin{document}\n" + 
				"\n" + 
				"\\maketitle\n" + 
				"\n" + 
				"\n" + 
				"For inter-annotator agreement, we calculated free-marginal kappa in three different conditions (see Table \\ref{agreement-table}): \\emph{perfect \\textbf{match} analysis} (i.e., by taking the precise positions and (sub)categories of errors into account), error count analysis (i.e., by ignoring error positions), and error presence analysis (i.e., by just looking at whether both annotators identified that a segment contains a certain (sub)category of errors)\\footnote{Free-marginal kappa is interpreted as: 0.01-0.20 = slight agreement, 0.21-0.40 = fair agreement, 0.41-0.60 = moderate agreement, 0.61-0.80 = substantial agreement, 0.81-1.00 = almost perfect agreement \\cite{landis1977measurement}}. The results show that when taking positions into account, there is just slight agreement between the annotators. This is explained by the different understanding of where errors need to be marked: one translator annotated errors on character level, while the other - on token level. For instance, in the case of wrong separators in numbers (e.g. 7.5), one annotator marked only the punctuation mark, while the other - the whole number.\n" + 
				"\n" + 
				"\n" + 
				"\\end{document}";
		ArrayList<Event> events = FilterTestDriver.getEvents(filter, snippet, null);
		int i = 0;
		for (Event event : events) {
//			System.out.println(i+"\t"+event.getEventType()+"\t"+event.getResource().toString());
			i = i+1;
			writer.handleEvent(event);
		}

        filter.close();
        writer.close();

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertEquals(snippet, outputData);
    }

    @Test @Ignore
    public void writeDocumentParts2() throws Exception {
        TEXFilter filter = new TEXFilter();
        IFilterWriter writer = filter.createFilterWriter();

        Path path = tempFolder.newFile().toPath();
        OutputStream os = Files.newOutputStream(path);
        writer.setOutput(os);
        writer.setOptions(LocaleId.FRENCH, StandardCharsets.UTF_8.name());

        String contents = getFileContents("/Test02.tex");

        filter.open(new RawDocument(contents, null, null));
        
        while (filter.hasNext()) {
        	Event event = filter.next();
        	writer.handleEvent(event);
        	writer.handleEvent(filter.next());
        }
        filter.close();
        writer.close();

        String outputData = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        // Russian chars
        assertEquals(contents, outputData);
    }

    
    @Test @Ignore
    public void testCommonMarkRoundTrip() throws Exception {
    	// this test uses GenericSkeletonWriter writer and test fails 
    	testRoundTrip("/Test01.tex");
    }
    
    private void testRoundTrip(String originalFile) throws Exception {
        TEXFilter filter = new TEXFilter();
        String contents = getFileContents(originalFile);

        List<Event> events = FilterTestDriver.getEvents(filter, contents, null, LocaleId.FRENCH);

        EncoderManager em = filter.getEncoderManager();
        em.setAllKnownMappings();
        em.updateEncoder(TEXFilter.TEX_MIME_TYPE);
        assertEquals(contents, FilterTestDriver.generateOutput(events, em, LocaleId.FRENCH));
    }

// Not used
//    private void testChangedOutput(String originalFile, String changedFile) throws Exception {
//        TEXFilter filter = new TEXFilter();
//
//        List<Event> events = FilterTestDriver.getEvents(filter,
//                getFileContents(originalFile), null, LocaleId.FRENCH);
//
//        EncoderManager em = new EncoderManager();
//        em.setAllKnownMappings();
//        assertEquals(getFileContents(changedFile),
//                FilterTestDriver.generateChangedOutput(events, em, LocaleId.FRENCH));
//    }

    private String getFileContents(String filename) throws Exception {
        try (InputStream is = root.in(filename).asInputStream();
             Scanner scanner = new Scanner(is, "UTF-8")) {
            return scanner.useDelimiter("\\A").next(); // A hack to read the entire file into a String by one call.
        }
    }

}
