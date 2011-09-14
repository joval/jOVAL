// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.results.core.OvalResults;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;

import org.joval.util.JOVALSystem;

/**
 * A utility class for performing OVAL schematron validation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SchematronValidator {
    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    public static void validate(OvalDefinitions defs, File template) throws SchematronValidationException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DEFINITIONS));
	    validate(new JAXBSource(ctx, defs), template);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalSystemCharacteristics, and verifies that the
     * result is an empty document.
     */
    public static void validate(OvalSystemCharacteristics sc, File template) throws SchematronValidationException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS));
	    validate(new JAXBSource(ctx, sc), template);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    public static void validate(OvalResults results, File template) throws SchematronValidationException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS));
	    validate(new JAXBSource(ctx, results), template);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    // Private

    private static void validate(JAXBSource src, File template) throws SchematronValidationException {
	try {
	    TransformerFactory xf = TransformerFactory.newInstance();
	    Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(template)));
	    DOMResult result = new DOMResult();
	    transformer.transform(src, result);
	    Node root = result.getNode();
	    if (root.getNodeType() == Node.DOCUMENT_NODE) {
		NodeList children = root.getChildNodes();
		int len = children.getLength();
		if (len > 0) {
		    String msg = JOVALSystem.getMessage("ERROR_SCHEMATRON_VALIDATION");
		    SchematronValidationException error = new SchematronValidationException(msg);
		    for (int i=0; i < len; i++) {
			error.getErrors().add(children.item(i).getTextContent());
		    }
		    throw error;
		}
	    }
	} catch (FileNotFoundException e) {
	    throw new SchematronValidationException(e);
	} catch (TransformerConfigurationException e) {
	    throw new SchematronValidationException(e);
	} catch (TransformerException e) {
	    throw new SchematronValidationException(e);
	}
    }
}
