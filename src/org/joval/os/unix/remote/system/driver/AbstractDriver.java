// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.remote.system.driver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.security.auth.login.CredentialException;

import org.slf4j.cal10n.LocLogger;

import org.joval.intf.identity.ICredential;
import org.joval.intf.unix.system.IPrivilegeEscalationDriver;
import org.joval.io.PerishableReader;
import org.joval.util.JOVALMsg;

/**
 * Abstract base class for all IPrivilegeEscalationDriver implementations.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class AbstractDriver implements IPrivilegeEscalationDriver, IPrivilegeEscalationDriver.IStreams {
    protected LocLogger logger = JOVALMsg.getLogger();
    protected ICredential cred;
    protected OutputStream out;
    protected PerishableReader in, err;

    public AbstractDriver(ICredential cred) throws CredentialException {
	if (cred.getPassword() == null) {
	    throw new CredentialException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_PASSWORD, cred.getUsername()));
	} else {
	    this.cred = cred;
	}
    }

    // Implement ILoggable

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Implement IStreams

    public OutputStream getOutputStream() {
	return out;
    }

    public InputStream getInputStream() {
	return in;
    }

    public InputStream getErrorStream() {
	return err;
    }
}
