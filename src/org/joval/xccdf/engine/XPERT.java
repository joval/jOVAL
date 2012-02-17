// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import cpe.schemas.dictionary.ListType;
import xccdf.schemas.core.Benchmark;

import org.joval.xccdf.XccdfException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * XCCDF Processing Engine and Reporting Tool (XPERT) main class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XPERT {
    /**
     * Run from the command-line.
     */
    public static void main(String[] argv) {
	File baseDir = null;
	if (argv.length == 0) {
	    //
	    // Use the current directory
	    //
	    baseDir = new File(".");
	} else if (argv.length == 1) {
	    //
	    // Use the specified directory
	    //
	    baseDir = new File(argv[0]);
	}

	if (baseDir == null || !baseDir.isDirectory()) {
	    System.out.println("Usage: java org.joval.xccdf.XPERT [basedir]");
	    System.exit(1);
	}

	File xccdf=null, cpeOval=null, cpeDictionary=null, oval=null;
	File[] files = baseDir.listFiles();
	for (File file : files) {
	    if (file.getName().toLowerCase().endsWith("xccdf.xml")) {
		xccdf = file;
	    } else if (file.getName().toLowerCase().endsWith("cpe-dictionary.xml")) {
		cpeDictionary = file;
	    } else if (file.getName().toLowerCase().endsWith("cpe-oval.xml")) {
		cpeOval = file;
	    } else if (file.getName().toLowerCase().endsWith("oval.xml")) { // NB: after cpe-oval.xml
		oval = file;
	    }
	}

	if (xccdf == null) {
	    System.out.println("ERROR: Failed to find the XCCDF benchmark file");
	    System.exit(1);
	}
	if (cpeDictionary == null) {
	    System.out.println("ERROR: Failed to find the CPE dictionary file");
	    System.exit(1);
	}
	if (cpeOval == null) {
	    System.out.println("ERROR: Failed to find the CPE oval file");
	    System.exit(1);
	}
	if (oval == null) {
	    System.out.println("ERROR: Failed to find the OVAL definitions file");
	    System.exit(1);
	}

	try {
	    XPERT engine = new XPERT(xccdf, cpeDictionary, cpeOval, oval);
	} catch (XccdfException e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }

    // Private

    private Benchmark benchmark;
    private ListType dictionary;

    private XPERT(File xccdf, File cpeDictionary, File cpeOval, File oval) throws XccdfException {
	benchmark = new XccdfDocument(xccdf).getBenchmark();
	dictionary = new CpeDictionary(cpeDictionary).getCpeList();
    }

    // Implement Runnable

    public void run() {
    }
}
