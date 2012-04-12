// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system.driver;

import java.io.EOFException;
import java.io.OutputStream;
import javax.security.auth.login.CredentialException;
import javax.security.auth.login.LoginException;

import org.joval.intf.identity.ICredential;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IPrivilegeEscalationDriver;
import org.joval.intf.util.IPerishable;
import org.joval.io.PerishableReader;
import org.joval.util.JOVALMsg;

/**
 * IPrivilegeEscalationDriver for Solaris.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SolarisDriver extends AbstractDriver {
    public SolarisDriver(ICredential cred) throws CredentialException {
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
     * Perform an interactive login on Solaris by reading the prompt from the input stream, entering CR, then skipping past
     * the Message Of The Day (if any).
     */
    public IStreams handleEscalation(IProcess p, boolean shell, long timeout) throws Exception {
	p.setInteractive(true);
	p.start();
	in = PerishableReader.newInstance(p.getInputStream(), timeout);
	((PerishableReader)in).setLogger(logger);
	in.readFully(new byte[10]); //Password:_
	out = p.getOutputStream();
	out.write(cred.getPassword().getBytes());
	out.write(CR);
	out.flush();
	String line1 = in.readLine();
	if (line1 == null) {
	    throw new EOFException(null);
	} else if (line1.indexOf("Sorry") == -1) {
	    //
	    // Skip past the message of the day
	    // NOTE -- by omitting the "-" (login) argument from the su command, we need no longer skip the MOTD.
	    //
/*
	    int linesToSkip = us.getMotdLines();
	    for (int i=0; i < linesToSkip; i++) {
		if (in.readLine() == null) {
		    throw new EOFException(null);
		}
	    }
*/
	} else {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_AUTHENTICATION_FAILED, cred.getUsername());
	    throw new LoginException(msg);
	}
	return this;
    }
}
