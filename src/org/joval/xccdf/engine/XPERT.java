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

import org.joval.intf.oval.IDefinitions;
import org.joval.oval.OvalException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
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
	    XPERT engine = new XPERT(new XccdfDocument(f));
	} catch (XccdfException e) {
	    e.printStackTrace();
	    System.exit(1);
	} catch (OvalException e) {
	    e.printStackTrace();
	    System.exit(1);
	}

	System.exit(0);
    }

    // Private

    private Benchmark benchmark;
    private ListType dictionary;
    private IDefinitions cpeOval, oval;

    private XPERT(XccdfDocument doc) {
	System.out.println("Loaded document");
    }

    // Implement Runnable

    public void run() {
    }
}
