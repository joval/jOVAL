// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system.driver;

import java.io.OutputStream;
import javax.security.auth.login.CredentialException;

import org.joval.intf.identity.ICredential;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IPrivilegeEscalationDriver;
import org.joval.intf.util.IPerishable;
import org.joval.io.PerishableReader;

/**
 * IPrivilegeEscalationDriver for Mac OS X.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class MacOSXDriver extends AbstractDriver {
    /**
     * On Mac OS X, root is not normally enabled, and when it is, you're not allowed to su to root from a remote terminal.
     * Hence, this driver uses the sudo command, which requires (1) that the session account be sudo-enabled, and (2) that
     * this driver is initialized with the session account credential, not a root account credential.
     */
    public MacOSXDriver(ICredential cred) throws CredentialException {
	super(cred);
    }

    // Implement IPrivilegeEscalationDriver

    public String getSuString(String command) {
	return new StringBuffer("sudo -E -p ? ").append(command).toString();
    }

    /**
     * On Mac OSX, perform an interactive login by reading the prompt from the input stream, and entering CR after the
     * 9-character password prompt.
     */
    public IStreams handleEscalation(IProcess p, boolean shell, long timeout) throws Exception {
	p.setInteractive(true);
	p.start();
	in = PerishableReader.newInstance(p.getInputStream(), timeout);
	((PerishableReader)in).setLogger(logger);
	in.setCheckpoint(512);
	boolean success = false;
	for (int i=0; i < 512 && !success; i++) {
	    int ch = in.read();
	    if ('?' == ch) {
		out = p.getOutputStream();
		out.write(cred.getPassword().getBytes());
		out.write(LF);
		out.flush();
		success = true;
	    }
	}
	if (!success) {
	    in.restoreCheckpoint();
	}
	return this;
    }
}
