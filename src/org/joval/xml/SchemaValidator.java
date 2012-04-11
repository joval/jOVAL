// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.File;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.xml.sax.SAXException;

/**
 * Utility class for performing validations against a set of XSD schemas.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SchemaValidator {
    private Validator validator;
    private Exception lastError;

    /**
     * Create a SchemaValidator that will validate XML structures against the specified list of XSD schema files in the
     * specified base directory.
     */
    public SchemaValidator(File[] schemaFiles) throws SAXException, IOException {
	Source[] sources = new Source[schemaFiles.length];
	for (int i=0; i < schemaFiles.length; i++) {
	    sources[i] = new StreamSource(schemaFiles[i]);
	}
	Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(sources);
	validator = schema.newValidator();
    }

    /**
     * Validate an XML file.  If validation fails, details can be obtained via getLastError().
     */
    public boolean validate(File f) throws SAXException, IOException {
	try {
	    validator.validate(new StreamSource(f));
	    return true;
	} catch (SAXException e) {
	    lastError = e;
	    return false;
	}
    }

    /**
     * Get the last error, if any, that was generated.
     */
    public Exception getLastError() {
	return lastError;
    }
}
