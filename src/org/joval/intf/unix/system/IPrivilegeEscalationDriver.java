// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.unix.system;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joval.intf.identity.ICredential;
import org.joval.intf.io.IReader;
import org.joval.intf.system.IProcess;
import org.joval.intf.util.ILoggable;

/**
 * An interface describing the platform-specific requirements for running a Unix process as a privileged user.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public interface IPrivilegeEscalationDriver extends ILoggable {
    int CR = 0x0D;
    int LF = 0x0A;

    /**
     * Get the preferred command string that will execute the specified command using the driver's privileged account context.
     */
    public String getSuString(String command);

    /**
     * Perform the platform-specific password input handshake, resulting from the execution of the escalated command string.
     * This will start the process.
     *
     * @param shell indicates whether the process is being invoked through an exec or shell channel.
     * @param timeout the read timeout for the escalation process
     */
    public IStreams handleEscalation(IProcess p, boolean shell, long timeout) throws Exception;

    /**
     * A container interface for streams that have been accessed during the escalation process. Since streams which
     * may have been accessed could be buffered, it would be a bad idea to subsequently access the IProcess's underlying
     * streams directly.
     */
    public interface IStreams {
	InputStream getInputStream();
	InputStream getErrorStream();
	OutputStream getOutputStream();
    }
}
