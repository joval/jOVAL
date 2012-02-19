// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xccdf.engine;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import cpe.schemas.dictionary.ListType;
import oval.schemas.definitions.core.OvalDefinitions;
import xccdf.schemas.core.Benchmark;

import org.joval.cpe.CpeException;
import org.joval.intf.oval.IDefinitions;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.xccdf.XccdfBundle;
import org.joval.xccdf.XccdfException;

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
	File f = null;
	if (argv.length == 1) {
	    //
	    // Use the specified directory
	    //
	    f = new File(argv[0]);
	}

	if (f == null) {
	    System.out.println("Usage: java org.joval.xccdf.XPERT [path]");
	    System.exit(1);
	}

	try {
	    XPERT engine = new XPERT(new XccdfBundle(f));
	} catch (CpeException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (OvalException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (XccdfException e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }

    // Private

    private XccdfBundle xccdf;

    private XPERT(XccdfBundle xccdf) {
	this.xccdf = xccdf;
	System.out.println("Loaded document");
    }

    // Implement Runnable

    public void run() {

//Apparent order of things...
// Benchmark specifies what Rules/Groups are selected
// Groups contain rules, rules contain checks, checks export values...
//   values are specified at the root level; selected group/rule export values become OVAL external variables
//   checks -- pick the one whose system matches the OVAL definitions schema.  It will reference a definition.


    }
}
