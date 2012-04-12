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
 * IPrivilegeEscalationDriver for AIX.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class AIXDriver extends AbstractDriver {
    public AIXDriver(ICredential cred) throws CredentialException {
	super(cred);
    }

    // Implement IPrivilegeEscalationDriver

    public String getSuString(String command) {
	StringBuffer sb = new StringBuffer("su ");
	sb.append(cred.getUsername());
	sb.append(" -c \"");
	sb.append(command.replace("\"", "\\\""));
	sb.append("\"");
	return sb.toString();
    }

    /**
     * Perform an interactive login on AIX by reading the prompt from the input stream, and entering CR after the password.
     * Then read one line.
     */
    public IStreams handleEscalation(IProcess p, boolean shell, long timeout) throws Exception {
	p.setInteractive(true);
	p.start();
	in = PerishableReader.newInstance(p.getInputStream(), timeout);
	((PerishableReader)in).setLogger(logger);
	in.readFully(new byte[17]); //root's Password:_
	out = p.getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write(CR);
	out.flush();
	in.readLine();
	return this;
    }
}
