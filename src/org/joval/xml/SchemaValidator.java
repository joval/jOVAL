// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.File;
import java.io.InputStream;
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

    /**
     * Create a SchemaValidator that will validate XML structures against the specified list of XSD schema files in the
     * specified base directory.
     */
    public SchemaValidator(File[] schemaFiles) throws SAXException, IOException {
	Source[] sources = new Source[schemaFiles.length];
	for (int i=0; i < schemaFiles.length; i++) {
	    sources[i] = new StreamSource(schemaFiles[i]);
	}
	init(sources);
    }

    /**
     * Create a SchemaValidator that will validate XML structures against the specified list of resource names that will
     * correspond to XSD schema files retrievable by the context ClassLoader.
     */
    public SchemaValidator(String[] schemaResources) throws SAXException, IOException {
	Source[] sources = new Source[schemaResources.length];
	for (int i=0; i < schemaResources.length; i++) {
	    sources[i] = new StreamSource(getClass().getResourceAsStream(schemaResources[i]));
	}
	init(sources);
    }

    public SchemaValidator(Source[] sources) throws SAXException {
	init(sources);
    }

    /**
     * Validate an XML file.  If validation fails, details can be obtained via getLastError().
     */
    public void validate(File f) throws SAXException, IOException {
	validate(new StreamSource(f));
    }

    public void validate(InputStream in) throws SAXException, IOException {
	validate(new StreamSource(in));
    }

    public void validate(Source source) throws SAXException, IOException {
	validator.validate(source);
    }

    // Private

    private void init(Source[] sources) throws SAXException {
	Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(sources);
	validator = schema.newValidator();
    }
}
