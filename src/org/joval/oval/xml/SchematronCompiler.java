// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.xml.transform.Result;
import javax.xml.transform.Source; import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;

/**
 * Compiles schematron files into XSLT files.
 * @see http://www.schematron.com/implementation.html
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SchematronCompiler {
    public static void main(String[] argv) {
	if (argv.length == 3) {
	    File iso = new File(argv[0]);
	    File input = new File(argv[1]);
	    if (input.isFile()) {
		File output = new File(argv[2]);
		try {
		    new SchematronCompiler(iso).compile(input, output);
		    System.exit(0);
		} catch (Exception e) {
		    e.printStackTrace();
		    System.exit(1);
		}
	    } else {
		System.out.println("Not a file: " + argv[0]);
		System.exit(1);
	    }
	} else {
	    System.out.println("Usage: java SchematronCompiler [ISO xsl dir] [input file] [output file]");
	    System.exit(1);
	}
    }

    // Private

    private static final String PHASE1	= "iso_dsdl_include.xsl";
    private static final String PHASE2	= "iso_abstract_expand.xsl";
    private static final String PHASE3	= "iso_svrl_for_xslt1.xsl";

    private TransformerFactory factory;
    private File iso;

    private SchematronCompiler(File iso) throws TransformerFactoryConfigurationError {
	this.iso = iso;
	factory = TransformerFactory.newInstance();
    }

    private void compile(File input, File output) throws TransformerConfigurationException, TransformerException {
	Source src, xsl;

	//
	// REMIND (DAS): For whatever reason, phases 1 and 2 cause errors, but are also apparently not necessary.
	//
	src = new StreamSource(input);
	xsl = new StreamSource(new File(iso, PHASE3));
	StreamResult result = new StreamResult(output);
	factory.newTransformer(xsl).transform(src, result);

/*
	//
	// Phase 1
	//
	src = new StreamSource(input);
	xsl = new StreamSource(new File(iso, PHASE1));
	DOMResult dr = new DOMResult();
	factory.newTransformer(xsl).transform(src, dr);

	//
	// Phase 2
	//
	src = new DOMSource(dr.getNode());
	xsl = new StreamSource(new File(iso, PHASE2));
	factory.newTransformer(xsl).transform(src, dr);

	//
	// Phase 3
	//
	src = new DOMSource(dr.getNode());
	xsl = new StreamSource(new File(iso, PHASE3));
	StreamResult result = new StreamResult(output);
	factory.newTransformer(xsl).transform(src, result);
*/
    }
}
