// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
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

import org.iso.svrl.SchematronOutput;
import org.iso.svrl.SuccessfulReport;
import org.iso.svrl.FailedAssert;

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
    private static final String PROP_SVRL_PACKAGES = "svrl.packages";

    private static Properties props = new Properties();
    static {
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    props.load(cl.getResourceAsStream("svrl.properties"));
	} catch (IOException e) {
	    JOVALSystem.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * For command-line testing.
     */
    public static void main(String[] argv) {
	if (argv.length != 3) {
	    System.out.println("Usage: java SchematronValidator [definitions|sc|results] [xml] [xsl]");
	    System.exit(1);
	} else {
	    try {
		File xml = new File(argv[1]);
		File xsl = new File(argv[2]);
		if (argv[0].equals("definitions")) {
		    validate(Definitions.getOvalDefinitions(xml), xsl);
		} else if (argv[1].equals("sc")) {
		    validate(SystemCharacteristics.getOvalSystemCharacteristics(xml), xsl);
		} else if (argv[2].equals("results")) {
		    validate(Results.getOvalResults(xml), xsl);
		}
		System.out.println("Validation successful");
	    } catch (OvalException e) {
		e.printStackTrace();
	    } catch (SchematronValidationException e) {
		System.out.println("Validation failed");
		for (String err : e.getErrors()) {
		    System.out.println("Error: " + err);
		}
	    }
	}
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    public static void validate(OvalDefinitions defs, File xsl) throws SchematronValidationException, OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_DEFINITIONS));
	    validate(new JAXBSource(ctx, defs), xsl);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalSystemCharacteristics, and verifies that the
     * result is an empty document.
     */
    public static void validate(OvalSystemCharacteristics sc, File xsl) throws SchematronValidationException, OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_SYSTEMCHARACTERISTICS));
	    validate(new JAXBSource(ctx, sc), xsl);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    /**
     * Applies the specified Schematron XSL template transformation to the OvalDefinitions, and verifies that the result
     * is an empty document.
     */
    public static void validate(OvalResults results, File xsl) throws SchematronValidationException, OvalException {
	try {
	    JAXBContext ctx = JAXBContext.newInstance(JOVALSystem.getOvalProperty(JOVALSystem.OVAL_PROP_RESULTS));
	    validate(new JAXBSource(ctx, results), xsl);
	} catch (JAXBException e) {
	    throw new SchematronValidationException(e);
	}
    }

    // Private

    private static void validate(JAXBSource src, File xsl) throws SchematronValidationException, OvalException {
	try {
	    TransformerFactory xf = TransformerFactory.newInstance();
	    Transformer transformer = xf.newTransformer(new StreamSource(new FileInputStream(xsl)));
	    DOMResult result = new DOMResult();
	    transformer.transform(src, result);
	    Node root = result.getNode();
	    if (root.getNodeType() == Node.DOCUMENT_NODE) {
		JAXBContext ctx = JAXBContext.newInstance(props.getProperty(PROP_SVRL_PACKAGES));
		Object rootObj = ctx.createUnmarshaller().unmarshal(root);
		if (rootObj instanceof SchematronOutput) {
		    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_SCHEMATRON_VALIDATION);
		    SchematronValidationException error = new SchematronValidationException(msg);
		    SchematronOutput output = (SchematronOutput)rootObj;
		    for (Object child : output.getActivePatternAndFiredRuleAndFailedAssert()) {
			if (child instanceof FailedAssert) {
			    error.getErrors().add(((FailedAssert)child).getText());
			} else if (child instanceof SuccessfulReport) {
			    error.getErrors().add(((SuccessfulReport)child).getText());
			}
		    }
		    if (error.getErrors().size() > 0) {
			throw error;
		    }
		} else {
		    throw new OvalException("Not a SchematronOutput: " + root.getClass().getName());
		}
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	} catch (FileNotFoundException e) {
	    throw new OvalException(e);
	} catch (TransformerConfigurationException e) {
	    throw new OvalException(e);
	} catch (TransformerException e) {
	    throw new OvalException(e);
	}
    }
}
