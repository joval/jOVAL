// Copyright (C) 2011 jOVAL.org.  All rights reserved.
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
 * A tool for running processes as a specific user.  This does not typically actually involve using the sudo command, as
 * it is not normally standard on a Unix operation system.  Rather, it makes use of the su command.  The notable exception
 * is Mac OS X, where it is installed by default (and hence used by this class).
 *
 * It is used exclusively by the remote UnixSession class.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LinuxDriver extends AbstractDriver {
    public LinuxDriver(ICredential cred) throws CredentialException {
	super(cred);
    }

    // Implement IPrivilegeEscalationDriver

    public String getSuString(String command) {
	StringBuffer sb = new StringBuffer("su ");
	sb.append(cred.getUsername());
	sb.append(" -m -c \"");
	sb.append(command.replace("\"", "\\\""));
	sb.append("\"");
	return sb.toString();
    }

    /**
     * Perform a normal login by reading the prompt from the error stream, and entering LF after the password.
     */
    public IStreams handleEscalation(IProcess p, boolean shell, long timeout) throws Exception {
	p.start();
	PerishableReader in = null;
	if (shell) {
	    in = PerishableReader.newInstance(p.getInputStream(), timeout);
	    ((PerishableReader)in).setLogger(logger);
	    in.readFully(new byte[10]); // Password:_
	} else {
	    err = PerishableReader.newInstance(p.getErrorStream(), timeout);
	    ((PerishableReader)err).setLogger(logger);
	    err.readFully(new byte[10]); // Password:_
	}
	out = p.getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write(shell ? CR : LF);
	out.flush();
	return this;
    }
}
