// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.system;

import java.io.File;

import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.io.UnixFilesystem;
import org.joval.util.AbstractSession;

/**
 * A simple session implementation for Unix machines.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSession extends BaseUnixSession {
    public UnixSession(File wsdir) {
	super();
	this.wsdir = wsdir;
    }

    // Implement IBaseSession

    public boolean connect() {
	if (env == null) {
	    env = new Environment(this);
	}
	if (fs == null) {
	    fs = new UnixFilesystem(this);
	}
	cwd = new File(".");
	flavor = Flavor.flavorOf(this);
	connected = true;
	return true;
    }

    public void disconnect() {
	connected = false;
    }
}
