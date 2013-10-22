// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml.schematron;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;

import org.iso.svrl.SchematronOutput;
import org.iso.svrl.SuccessfulReport;
import org.iso.svrl.FailedAssert;

import org.joval.util.JOVALMsg;
import org.joval.xml.XSLTools;
import org.joval.xml.SchemaRegistry;

/**
 * A utility class for performing schematron validation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Validator {
    //
    // Register and instantiate the SVRL schema extension.
    //
    public static final SchemaRegistry.ISchema SVRL;
    static {
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	InputStream in = cl.getResourceAsStream("svrl-schema-registry.ini");
	if (in == null) {
	    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, "svrl-schema-registry.ini"));
	} else {
	    SchemaRegistry.register(in);
	    SVRL = SchemaRegistry.getGroup("SVRL");
	}
    }

    /**
     * For command-line testing.
     */
    public static void main(String[] argv) {
	if (argv.length != 2) {
	    System.out.println("Usage: java org.joval.xml.schematron.Validator [xml] [xsl]");
	    System.exit(1);
	} else {
	    try {
		File xml = new File(argv[0]);
		File xsl = new File(argv[1]);
		Validator v = new Validator(xsl);
		v.validate(xml);
		System.out.println("Validation successful");
	    } catch (IOException e) {
		e.printStackTrace();
	    } catch (TransformerException e) {
		e.printStackTrace();
	    } catch (ValidationException e) {
		System.out.println("Validation failed");
		for (String err : e.getErrors()) {
		    System.out.println("Error: " + err);
		}
	    }
	}
    }

    private Transformer transformer;

    /**
     * Create a new Validator given the specified Schematron-derived XSLT template.
     */
    public Validator(File xsl) throws IOException, TransformerException {
	transformer = XSLTools.XSLVersion.V1.getFactory().newTransformer(new StreamSource(new FileInputStream(xsl)));
    }

    /**
     * Validate a file.
     */
    public void validate(File xml) throws IOException, ValidationException {
	validate(new StreamSource(new FileInputStream(xml)));
    }

    /**
     * Applies the Schematron template transformation to the source, and verifies that there are no failed assertions
     * or reports.
     */
    public void validate(Source source) throws ValidationException {
	try {
	    DOMResult result = new DOMResult();
	    transformer.transform(source, result);
	    Node root = result.getNode();
	    if (root.getNodeType() == Node.DOCUMENT_NODE) {
		Object rootObj = SVRL.getJAXBContext().createUnmarshaller().unmarshal(root);
		if (rootObj instanceof SchematronOutput) {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_SCHEMATRON_VALIDATION);
		    ValidationException error = new ValidationException(msg);
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
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_SCHEMATRON_TYPE, root.getClass().getName());
		    throw new ValidationException(msg);
		}
	    }
	} catch (TransformerException e) {
	    throw new ValidationException(e);
	} catch (JAXBException e) {
	    throw new ValidationException(e);
	}
    }
}
