package net.sf.okapi.filters.openxml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import net.sf.okapi.common.IdGenerator;
import net.sf.okapi.common.LocaleId;

@RunWith(JUnit4.class)
public class StringItemParserTest {

    private static final QName EXPECTED_TEXT_NAME = new QName(
            "http://schemas.openxmlformats.org/spreadsheetml/2006/main", "t");

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private StringItemParser stringItemParser;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ConditionalParameters conditionalParameters;

    private StyleDefinitions styleDefinitions = new StyleDefinitions.Empty();

    private XMLEventFactory xmlEventFactory;

    private LocaleId srcLang = LocaleId.ENGLISH;

    private StyleOptimisation styleOptimisation = new StyleOptimisation.Bypass();

    @Before
    public void before() {
        this.xmlEventFactory = XMLEventFactory.newFactory();
    }

    @Test
    public void doesNotLoseTextFollowedByEmptyRun() throws XMLStreamException {

        initializeStringParserWith(
                "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"1\" uniqueCount=\"1\"><si><r><t>Text1</t></r><r></r></si></sst>");

        StringItem stringItem = stringItemParser.parse();
        List<Chunk> chunks = stringItem.getChunks();
        assertThat(((Run.RunText) ((Run) chunks.get(1)).getBodyChunks().get(0)).characters().getData()).isEqualTo("Text1");
    }

    @Test
    public void emptyRunInTheMiddleIsRemoved() throws XMLStreamException {

        initializeStringParserWith(
                "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"1\" uniqueCount=\"1\"><si><r><t>Text1</t></r><r></r><r><t>Text2</t></r></si></sst>");

        StringItem stringItem = stringItemParser.parse();

        assertThat(stringItem.getTextName()).isEqualTo(EXPECTED_TEXT_NAME);
        List<Chunk> chunks = stringItem.getChunks();
        assertThat(((Run.RunText) ((Run) chunks.get(1)).getBodyChunks().get(0)).characters().getData()).isEqualTo("Text1");
        assertThat(((Run.RunText) ((Run) chunks.get(2)).getBodyChunks().get(0)).characters().getData()).isEqualTo("Text2");
    }


    @Test
    public void stringItemHasTextNameWhenLastRunHasNoTextButFormatting() throws XMLStreamException {

        initializeStringParserWith(
                "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"1\" uniqueCount=\"1\"><si><r><t>Text1</t></r><r><rPr></rPr></r></si></sst>");

        StringItem stringItem = stringItemParser.parse();

        assertThat(stringItem.getTextName()).isEqualTo(EXPECTED_TEXT_NAME);
        List<Chunk> chunks = stringItem.getChunks();
        assertThat(((Run.RunText) ((Run) chunks.get(1)).getBodyChunks().get(0)).characters().getData()).isEqualTo("Text1");
    }

    private void initializeStringParserWith(String xml) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new StringReader(
                xml));
        @SuppressWarnings("unused")
        StartDocument startDocument = (StartDocument) xmlEventReader.nextEvent();
        StartElement startSst = (StartElement) xmlEventReader.nextEvent();
        StartElement startSi = (StartElement) xmlEventReader.nextEvent();
        StartElementContext startElementContext = new StartElementContext(startSi, startSst,
                xmlEventReader,
                xmlEventFactory, conditionalParameters, srcLang);

        this.stringItemParser = new StringItemParser(startElementContext, idGenerator,
                styleDefinitions, styleOptimisation);
    }

}
