// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix.system;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.unix.system.IUnixSession;
import org.joval.os.unix.UnixSystemInfo;
import org.joval.util.AbstractSession;

/**
 * A simple session implementation for Unix machines.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseUnixSession extends AbstractSession implements IUnixSession {
    protected Flavor flavor = Flavor.UNKNOWN;
    protected UnixSystemInfo info = null;

    protected BaseUnixSession() {
	super();
    }

    @Override
    protected String getOverrideKey(String key) {
	switch(flavor) {
	  case UNKNOWN:
	    return null;

	  default:
	    return new StringBuffer(flavor.value()).append(".").append(key).toString();
	}
    }

    // Implement IUnixSession

    public Flavor getFlavor() {
	return flavor;
    }

    // Implement IBaseSession

    public Type getType() {
	return Type.UNIX;
    }

    // Implement ISession

    public SystemInfoType getSystemInfo() {
	return info.getSystemInfo();
    }

    // Abstract

    public abstract String getHostname();

    public abstract boolean connect();

    public abstract void disconnect();
}
