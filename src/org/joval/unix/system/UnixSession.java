// Copyright (C) 2011 jOVAL.org.  All rights reserved.

package org.joval.unix.system;

import java.io.File;

import org.joval.io.LocalFilesystem;
import org.joval.util.BaseSession;

/**
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSession extends BaseSession {
    public UnixSession() {
	super();
    }

    // Implement ISession

    public boolean connect() {
	env = new Environment(this);
	fs = new LocalFilesystem(env, null);
	cwd = new File(".");
	return true;
    }

    public void disconnect() {
    }

    public int getType() {
	return UNIX;
    }
}
