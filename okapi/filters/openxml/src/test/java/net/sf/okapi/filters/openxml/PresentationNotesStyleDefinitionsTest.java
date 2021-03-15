package net.sf.okapi.filters.openxml;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import net.sf.okapi.common.FileLocation;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import static java.util.Collections.singletonList;
import static net.sf.okapi.filters.openxml.RunPropertyFactory.createRunProperty;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ccudennec
 * @since 07.09.2017
 */
@RunWith(JUnit4.class)
public class PresentationNotesStyleDefinitionsTest {

	private FileLocation root;

	@Before
	public void setUp() {
		root = FileLocation.fromClass(getClass());
	}

    @Test
    public void testGetCombinedRunProperties() throws Exception {
        try (Reader reader = new InputStreamReader(root.in("/pptxParser/notesMaster/notesMaster1.xml").asInputStream(), StandardCharsets.UTF_8)) {
            final XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            final StyleDefinitions styleDefinitions = new PowerpointStyleDefinitions(
                eventFactory
            );
            final PowerpointStyleDefinitionsReader styleDefinitionsReader = new PowerpointStyleDefinitionsReader(
                new ConditionalParameters(),
                eventFactory,
                XMLInputFactory.newInstance().createXMLEventReader(reader),
                null,
                PowerpointStyleDefinitions.NOTES_STYLE
            );
            styleDefinitions.readWith(styleDefinitionsReader);
            assertThat(styleDefinitions).isNotNull();

            RunProperties runProperties = new RunProperties.Default(eventFactory, null, null, singletonList(createRunProperty(new QName(null, "baseline"), "0")));

            RunProperties combinedRunProperties = styleDefinitions.combinedRunProperties("1", "unknown", runProperties);

            assertThat(getRunPropertyValueByLocalPart(combinedRunProperties, "baseline")).isEqualTo("0");
            assertThat(getRunPropertyValueByLocalPart(combinedRunProperties, "kern")).isEqualTo("1200");
            assertThat(getRunPropertyValueByLocalPart(combinedRunProperties, "sz")).isEqualTo("1200");
        }
    }

    private String getRunPropertyValueByLocalPart(RunProperties runProperties, String localPart) {
        for (final Property runProperty : runProperties.properties()) {

            if (localPart.equals(runProperty.getName().getLocalPart())) {
                return runProperty.value();
            }
        }

        return null;
    }
}