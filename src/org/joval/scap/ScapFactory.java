// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.joval.intf.scap.IDatastreamCollection;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.xccdf.IEngine;
import org.joval.scap.DatastreamCollection;
import org.joval.scap.ScapException;
import org.joval.scap.xccdf.engine.Engine;

/**
 * A convenience class for creating SCAP management objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ScapFactory {
    public static IDatastreamCollection createDatastreamCollection(URL url) throws ScapException {
	try {
	    return new DatastreamCollection(DatastreamCollection.getDSCollection(url.openStream()));
	} catch (IOException e) {
	    throw new ScapException(e);
	}
    }

    public static IDatastreamCollection createDatastreamCollection(File f) throws ScapException {
	return new DatastreamCollection(DatastreamCollection.getDSCollection(f));
    }

    public static IEngine createEngine(IPlugin plugin) {
	return new OEMEngine(plugin);
    }

    // Private

    private static class OEMEngine extends Engine {
	OEMEngine(IPlugin plugin) {
	    super(plugin);
	}
    }
}
