// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.juniper.system;

import java.util.NoSuchElementException;

import org.slf4j.cal10n.LocLogger;

import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.intf.juniper.system.IJunosSession;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.net.INetconf;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.os.cisco.system.IosSession;
import org.joval.os.juniper.JunosSystemInfo;
import org.joval.protocol.netconf.NetconfSession;
import org.joval.ssh.system.SshSession;
import org.joval.util.JOVALMsg;

/**
 * A simple session implementation for Juniper JunOS devices, which is really very similar to an IOS session.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JunosSession extends IosSession implements IJunosSession {
    private JunosSystemInfo info;

    /**
     * Create an IOS session with a live SSH connection to a router.
     */
    public JunosSession(SshSession ssh) {
	super(ssh);
    }

    public JunosSession(ITechSupport techSupport) {
	super(techSupport);
    }

    // Overrides

    @Override
    public INetconf getNetconf() {
	return new NetconfSession(ssh, internalProps.getLongProperty(IJunosSession.PROP_READ_TIMEOUT));
    }

    @Override
    public String getHostname() {
	if (ssh != null) {
	    return ssh.getHostname();
	} else {
	    for (String line : techSupport.getData("show version detail")) {
		if (line.startsWith("Hostname: ")) {
		    return line.substring(10).trim();
		}
	    }
	    return null;
	}
    }

    @Override
    public boolean connect() {
	if (ssh == null) {
	    return false;
	} else if (ssh.connect()) {
	    if (initialized) {
		return true;
	    } else {
		try {
		    techSupport = new SupportInformation(this);
		    info = new JunosSystemInfo(techSupport);
		    initialized = true;
		    return true;
		} catch (Exception e) {
		    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		return false;
	    }
	} else {
	    return false;
	}
    }

    @Override
    public IProcess createProcess(String command, String[] env) throws Exception {
	if (ssh == null) {
	    throw new IllegalStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_JUNOS_OFFLINE));
	} else {
	    return ssh.createProcess(command, env);
	}
    }

    @Override
    public Type getType() {
	return Type.JUNIPER_JUNOS;
    }

    @Override
    public SystemInfoType getSystemInfo() {
	if (info == null) {
	    info = new JunosSystemInfo(techSupport);
	}
	return info.getSystemInfo();
    }
}
