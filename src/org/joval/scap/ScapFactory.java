// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.datastream.IDatastream;
import org.joval.intf.scap.datastream.IDatastreamCollection;
import org.joval.intf.scap.ocil.IChecklist;
import org.joval.intf.scap.xccdf.ITailoring;
import org.joval.intf.scap.xccdf.IXccdfEngine;
import org.joval.scap.ScapException;
import org.joval.scap.datastream.DatastreamCollection;
import org.joval.scap.ocil.Checklist;
import org.joval.scap.ocil.OcilException;
import org.joval.scap.xccdf.Bundle;
import org.joval.scap.xccdf.Tailoring;
import org.joval.scap.xccdf.XccdfException;
import org.joval.scap.xccdf.engine.Engine;

/**
 * A convenience class for creating SCAP management objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ScapFactory {
    /**
     * Create a datastream collection from a URL.
     */
    public static IDatastreamCollection createDatastreamCollection(URL url) throws ScapException {
	try {
	    return new DatastreamCollection(DatastreamCollection.getDSCollection(url.openStream()));
	} catch (IOException e) {
	    throw new ScapException(e);
	}
    }

    /**
     * Create a datastream collection from a file.
     */
    public static IDatastreamCollection createDatastreamCollection(File f) throws ScapException {
	return new DatastreamCollection(DatastreamCollection.getDSCollection(f));
    }

    /**
     * Create a datastream from an SCAP "bundle" of files (directory or ZIP file).
     */
    public static IDatastream createBundle(File f) throws ScapException {
	return new Bundle(f);
    }

    /**
     * Create an XCCDF tailoring from a file.
     */
    public static ITailoring createTailoring(File f) throws XccdfException {
	return new Tailoring(Tailoring.getTailoringType(f));
    }

    /**
     * Create an OCIL checklist (which can also include the results) from a file.
     */
    public static IChecklist createChecklsit(File f) throws OcilException {
	return new Checklist(f);
    }

    /**
     * Create an XCCDF processing engine.
     */
    public static IXccdfEngine createEngine(IPlugin plugin) {
	return new OEMEngine(plugin);
    }

    // Private

    private static class OEMEngine extends Engine {
	OEMEngine(IPlugin plugin) {
	    super(plugin);
	}
    }
}
