// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.apple.system;

import java.text.SimpleDateFormat;
import java.util.NoSuchElementException;
import org.w3c.dom.Document;

import org.slf4j.cal10n.LocLogger;

import com.dd.plist.NSObject;

import org.joval.intf.apple.system.IiOSSession;
import org.joval.intf.system.IProcess;
import org.joval.util.AbstractBaseSession;
import org.joval.util.JOVALMsg;

/**
 * A simple session implementation for Juniper JunOS devices, which is really very similar to an IOS session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class iOSSession extends AbstractBaseSession implements IiOSSession {
    private NSObject plist;
    private boolean connected = false;

    public iOSSession(NSObject plist) {
	super();
	this.plist = plist;
    }

    // Implement IiOSSession

    public NSObject getConfigurationProfile() {
	return plist;
    }

    // Implement IBaseSession

    public String getHostname() {
	return "Generic Apple iOS Device";
    }

    public long getTime() throws Exception {
	throw new NoSuchElementException("Current time");
    }

    @Override
    public boolean isConnected() {
	return connected;
    }

    public boolean connect() {
	connected = true;
	return true;
    }

    public void disconnect() {
	connected = false;
    }

    public IProcess createProcess(String command, String[] env) throws Exception {
	throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_APPLE_OFFLINE));
    }

    public Type getType() {
	return Type.APPLE_IOS;
    }
}
