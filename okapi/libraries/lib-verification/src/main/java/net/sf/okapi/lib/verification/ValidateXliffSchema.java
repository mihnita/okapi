/*===========================================================================
  Copyright (C) 2012 by the Okapi Framework contributors
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

package net.sf.okapi.lib.verification;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.okapi.common.exceptions.OkapiIOException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

class JarLSResourceResolver implements LSResourceResolver {

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        LSInput result = null;
        try {
            String featureName = "XML 1.0";
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementation impl = registry.getDOMImplementation(featureName);
            DOMImplementationLS ls = (DOMImplementationLS) impl;

            result = ls.createLSInput();
            InputStream is = JarLSResourceResolver.class.getResourceAsStream("xml.xsd");
            result.setByteStream(is);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
        }
        if (result == null)
            throw new OkapiIOException("XLIFF Validation : internal DOM error, cannot load xml.xsd");

        return result;
    }
}

public class ValidateXliffSchema {
    public final static String XLIFF12_TRANSITIONAL_SCHEMA = "xliff-core-1.2-transitional.xsd";
    public final static String XLIFF12_STRICT_SCHEMA = "xliff-core-1.2-strict.xsd";

    private final static Validator TRANSITIONAL_VALIDATOR = createValidator(XLIFF12_TRANSITIONAL_SCHEMA);
    private final static Validator STRICT_VALIDATOR = createValidator(XLIFF12_STRICT_SCHEMA);

    private static Validator createValidator(String schemaName) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // my own resolver to load xml.xsd from a jar
            LSResourceResolver jarResolver = new JarLSResourceResolver();
            factory.setResourceResolver(jarResolver);

            InputStream is = ValidateXliffSchema.class.getResourceAsStream(schemaName);
            return factory.newSchema(new StreamSource(is)).newValidator();
        } catch (SAXException ex) {
        }
        throw new OkapiIOException("XLIFF Schame Validation : internal error, invalid " + schemaName);
    }

    public static boolean validateXliffSchema(InputStream inStream) {
    	return validateXliffSchema(inStream, XLIFF12_TRANSITIONAL_SCHEMA);
    }

    public static boolean validateXliffSchema(InputStream inStream, String schemaName) {
    	Validator validator = null;
    	switch (schemaName) {
	    	case XLIFF12_TRANSITIONAL_SCHEMA:
	    		validator = TRANSITIONAL_VALIDATOR;
	    		break;
	    	case XLIFF12_STRICT_SCHEMA:
	    		validator = STRICT_VALIDATOR;
	    		break;
	    	default:
	    		validator = null;
	    		break;
    	}
    	 // Might still also be nothing because we failed to create the validators, not only from default
        if (validator == null)
            return true;
        try {
            validator.validate(new StreamSource(inStream));
            return true;
        } catch (SAXParseException ex) {
            String message = ex.getMessage();
            if (!message.startsWith("Duplicate key value [")
                    || !message.endsWith("] declared for identity constraint of element \"file\".")) {
                String errorString = String.format("XLIFF Validation Error [%d, %d]:\n  %s\n  %s", ex.getLineNumber(),
                        ex.getColumnNumber(), ex.getSystemId(), ex.getMessage());
                throw new OkapiIOException(errorString);
            }
        } catch (Exception ex) {
            throw new OkapiIOException("XLIFF Validation Error: " + ex.getMessage());
        }
        return false;
    }

    public static boolean validateXliffSchema(URI fileURI) {
    	return validateXliffSchema(fileURI, XLIFF12_TRANSITIONAL_SCHEMA);
    }

    public static boolean validateXliffSchema(URI fileURI, String schemaName) {
        try (InputStream inStream = new FileInputStream(new File(fileURI))) {
            return validateXliffSchema(inStream, schemaName);
        } catch (IOException ex) {
            // The old API did not throw IOException, so we catch and re-throw the same exception as before
            throw new OkapiIOException("XLIFF Validation Error: " + ex.getMessage());
        }
    }
}
