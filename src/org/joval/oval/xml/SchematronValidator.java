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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import oval.schemas.definitions.core.OvalDefinitions;
import oval.schemas.results.core.OvalResults;
import oval.schemas.systemcharacteristics.core.OvalSystemCharacteristics;

import org.joval.oval.OvalException;
import org.joval.oval.engine.Definitions;
import org.joval.oval.engine.SystemCharacteristics;
import org.joval.oval.engine.Results;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A utility class for performing OVAL schematron validation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SchematronValidator {
    /**
     * For command-line testing.
     */
    public static void main(String[] argv) {
	if (argv.length != 4) {
	    System.out.println("Usage: java SchematronValidator [definitions|sc|results] [xml] [xsl] [out]");
	    System.exit(1);
	} else {
	    try {
		File xml = new File(argv[1]);
		File xsl = new File(argv[2]);
		File out = new File(argv[3]);
		if (argv[0].equals("definitions")) {
		    validate(Definitions.getOvalDefinitions(xml), xsl, out);
		} else if (argv[1].equals("sc")) {
		    validate(SystemCharacteristics.getOvalSystemCharacteristics(xml), xsl, out);
		} else if (argv[2].equals("results")) {
		    validate(Results.getOvalResults(xml), xsl, out);
		}
	    } catch (OvalException e) {
		e.printStackTrace();
	    } catch (SchematronValidationException e) {
		for (String err : e.getErrors()) {
		    System.out.println("Error: " + err);
		}
	    }
	}
    }

    public static void validate(OvalDefinitions defs, File template) throws SchematronValidationException {
	validate(defs, template, null);
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    public static void validate(OvalDefinitions defs, File template, File output) throws SchematronValidationException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DEFINITIONS));
	    validate(new JAXBSource(ctx, defs), template, output);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    public static void validate(OvalSystemCharacteristics sc, File template) throws SchematronValidationException {
	validate(sc, template, null);
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalSystemCharacteristics, and verifies that the
     * result is an empty document.
     */
    public static void validate(OvalSystemCharacteristics sc, File template, File output) throws SchematronValidationException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS));
	    validate(new JAXBSource(ctx, sc), template, output);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    public static void validate(OvalResults results, File template) throws SchematronValidationException {
	validate(results, template, null);
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    public static void validate(OvalResults results, File template, File output) throws SchematronValidationException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS));
	    validate(new JAXBSource(ctx, results), template, output);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    // Private

    private static void validate(JAXBSource src, File template, File output) throws SchematronValidationException {
	try {
	    TransformerFactory xf = TransformerFactory.newInstance();
	    Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(template)));
	    DOMResult result = new DOMResult();
	    transformer.transform(src, result);
	    Node root = result.getNode();
	    if (root.getNodeType() == Node.DOCUMENT_NODE) {
		if (output != null) {
		    transformer = xf.newTransformer();
		    transformer.setParameter(OutputKeys.INDENT, "yes");
		    transformer.transform(src, new StreamResult(output));
		}
		NodeList children = root.getChildNodes();
		int len = children.getLength();
		if (len > 0) {
		    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_SCHEMATRON_VALIDATION);
		    SchematronValidationException error = new SchematronValidationException(msg);
		    for (int i=0; i < len; i++) {
			Node node = children.item(i);
			error.getErrors().add(node.getTextContent());
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
