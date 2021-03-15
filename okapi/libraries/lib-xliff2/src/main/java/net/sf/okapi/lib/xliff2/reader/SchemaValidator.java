/*===========================================================================
  Copyright (C) 2013-2014 by the Okapi Framework contributors
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

package net.sf.okapi.lib.xliff2.reader;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.okapi.lib.xliff2.XLIFFException;

import org.xml.sax.SAXException;

/**
 * Provides a way to validate XLIFF 2.0 documents against the schemas.
 * <p>The schemas loading is done only at the first validation and lasts the life-span of this object.
 * Note that location and validity of the modules at the level of the extension points is not performed
 * by this object. that part is done with {@link LocationValidator} during the process.
 */
class SchemaValidator {

	private Validator validator;

	/**
	 * Validates a given XLIFF document.
	 * @param inputSource the stream source for the XLIFF 2 document to validate.
	 * @throws XLIFFException if an error occurs.
	 */
	public void validate (StreamSource inputSource) {
		try {
			// Load the schema if needed
			if ( validator == null ) {
				try {
					SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
					Schema schema = schemaFactory.newSchema(new Source[] {
						// Load the schemas
						// Order matters: dependencies should come first
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/informativeCopiesOf3rdPartySchemas/w3c/xml.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/xliff_core_2.0.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/metadata.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/change_tracking.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/fs.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/glossary.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/matches.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/resource_data.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/size_restriction.xsd")),
						new StreamSource(this.getClass().getResourceAsStream("/net/sf/okapi/lib/xliff2/modules/validation.xsd")),
//						new StreamSource(this.getClass().getResourceAsStream("/informativeCopiesOf3rdPartySchemas/w3c/xlink.xsd")),
//						new StreamSource(this.getClass().getResourceAsStream("/informativeCopiesOf3rdPartySchemas/w3c/its20-types.xsd")),
//						new StreamSource(this.getClass().getResourceAsStream("/informativeCopiesOf3rdPartySchemas/w3c/its20.xsd"))
					});
					validator = schema.newValidator();
					validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
					validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
				}
				catch ( SAXException e ) {
					throw new XLIFFException("Cannot load one of the XLIFF-related schemas.\nReason: "+ e.getLocalizedMessage());
				}
			}
			// Validate the file against the schema
			validator.validate(inputSource);
		}
		catch ( SAXException e ) {
			String text = e.toString(); // This output has the line and column values
			if ( text.startsWith("org.xml.sax.SAXParseException; ") ) {
				text = "Error "+text.substring(31);
			}
			throw new XLIFFReaderException(wrap(text), e);
		}
		catch ( IOException e ) {
			throw new XLIFFException("IO error.\nReason: "+e.getLocalizedMessage(), e);
		}			
	}

	private String wrap (String text) {
		// TODO: Wrap the text better.
		return text;
	}
}
