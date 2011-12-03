// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.unix;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import oval.schemas.systemcharacteristics.core.InterfacesType;
import oval.schemas.systemcharacteristics.core.InterfaceType;
import oval.schemas.systemcharacteristics.core.SystemInfoType;

import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.tree.INode;
import org.joval.io.StreamTool;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Tool for creating a SystemInfoType from a Unix ISession implementation.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class UnixSystemInfo {
    private IUnixSession session;
    private SystemInfoType info;

    /**
     * Create a plugin for scanning or test evaluation.
     */
    public UnixSystemInfo(IUnixSession session) {
	this.session = session;
    }

    public SystemInfoType getSystemInfo() {
	if (info != null) {
	    return info;
	}

	info = JOVALSystem.factories.sc.core.createSystemInfoType();
	try {
	    IProcess p = session.createProcess("hostname");
	    p.start();
	    InputStream in = p.getInputStream();
	    info.setPrimaryHostName(StreamTool.readLine(in, IUnixSession.TIMEOUT_S));
	    in.close();
	    p.waitFor(0);
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_HOSTNAME);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    IProcess p = session.createProcess("uname -r");
	    p.start();
	    InputStream in = p.getInputStream();
	    info.setOsVersion(StreamTool.readLine(in, IUnixSession.TIMEOUT_S));
	    in.close();
	    p.waitFor(0);
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_OSVERSION);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    IFilesystem fs = session.getFilesystem();
	    for (INode node : fs.getFile("/etc").getChildren(Pattern.compile("^.*-release$"))) {
		IProcess p = session.createProcess("cat " + node.getPath());
		p.start();
		InputStream in = p.getInputStream();
		info.setOsName(StreamTool.readLine(in, IUnixSession.TIMEOUT_S));
		in.close();
		p.waitFor(0);
	    }
	    if (!info.isSetOsName()) {
		info.setOsName(session.getFlavor().getOsName());
	    }
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_OSNAME);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    IProcess p = session.createProcess("uname -p");
	    p.start();
	    InputStream in = p.getInputStream();
	    info.setArchitecture(StreamTool.readLine(in, IUnixSession.TIMEOUT_S));
	    in.close();
	    p.waitFor(0);
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_ARCH);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	try {
	    InterfacesType interfacesType = JOVALSystem.factories.sc.core.createInterfacesType();
	    List<NetworkInterface> interfaces = NetworkInterface.getInterfaces(session);
	    for (NetworkInterface intf : interfaces) {
		InterfaceType interfaceType = JOVALSystem.factories.sc.core.createInterfaceType();
		interfaceType.setMacAddress(intf.getMacAddress());
		interfaceType.setInterfaceName(intf.getDescription());

		if (intf.getIpV4Address() != null) {
		    interfaceType.setIpAddress(intf.getIpV4Address());
		} else if (intf.getIpV6Address() != null) {
		    interfaceType.setIpAddress(intf.getIpV6Address());
		}

		if (interfaceType.getIpAddress() != null) {
		    interfacesType.getInterface().add(interfaceType);
		}
	    }
	    info.setInterfaces(interfacesType);
	} catch (Exception e) {
	    JOVALSystem.getLogger().warn(JOVALMsg.ERROR_PLUGIN_INTERFACE);
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return info;
    }
}
