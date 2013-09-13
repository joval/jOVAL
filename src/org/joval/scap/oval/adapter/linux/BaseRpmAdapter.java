// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Base class for RPM-oriented adapters.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseRpmAdapter implements IAdapter {
    IUnixSession session;
    Collection<String> packageList;
    Map<String, RpmData> packageMap;
    boolean packageMapLoaded = false;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case AIX:
	      case LINUX:
		packageMap = new HashMap<String, RpmData>();
		classes.add(getObjectClass());
		break;
	    }
	}
	if (classes.size() == 0) {
	    notapplicable.add(getObjectClass());
	}
	return classes;
    }

    // Protected

    abstract Class getObjectClass();

    /**
     * Load comprehensive information about every package on the system. Idempotent.
     */
    void loadPackageMap() {
	if (packageMapLoaded) {
	    return;
	}
	try {
	    packageList = new HashSet<String>();
	    packageMap = new HashMap<String, RpmData>();
	    StringBuffer cmd = new StringBuffer("rpm -qa | xargs -I{} ");
	    cmd.append(getBaseCommand()).append(" '{}'");

	    RpmData data = null;
	    Iterator<String> iter = SafeCLI.manyLines(cmd.toString(), null, session);
	    while ((data = nextRpmData(iter)) != null) {
		packageList.add(data.name);
		packageMap.put(data.name, data);
	    }
	    packageMapLoaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Get RpmData matching a single package name (potentially a "short name", which can lead to multiple results).
     */
    Collection<RpmData> getRpmData(String packageName) throws Exception {
	Collection<RpmData> result = new ArrayList<RpmData>();
	if (packageMap.containsKey(packageName)) {
	    //
	    // Return a previously-found exact match
	    //
	    result.add(packageMap.get(packageName));
	} else if (packageMapLoaded) {
	    //
	    // Look for a "short name" match
	    //
	    for (Map.Entry<String, RpmData> entry : packageMap.entrySet()) {
		if (entry.getKey().startsWith(packageName + "-")) {
		    result.add(entry.getValue());
		    break;
		}
	    }
	} else {
	    //
	    // Query the RPM database for the package name; cache and return the results
	    //
	    session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_RPM, packageName);
	    StringBuffer sb = new StringBuffer(getBaseCommand());
	    sb.append(" '").append(SafeCLI.checkArgument(packageName, session)).append("'");
	    Iterator<String> output = SafeCLI.multiLine(sb.toString(), session, IUnixSession.Timeout.S).iterator();
	    RpmData data = null;
	    while ((data = nextRpmData(output)) != null) {
		packageMap.put(data.name, data);
		result.add(data);
	    }
	}
	return result;
    }

    private static final String CMD_AIX;
    private static final String CMD_LINUX;
    static {
        StringBuffer aix = new StringBuffer("rpm -ql --qf '");
        aix.append("\\nNAME: %{NAME}\\n");
        aix.append("ARCH: %{ARCH}\\n");
        aix.append("VERSION: %{VERSION}\\n");
        aix.append("RELEASE: %{RELEASE}\\n");
        aix.append("EPOCH: %{EPOCH}\\n");

        StringBuffer linux = new StringBuffer(aix.toString());
        linux.append("SIGNATURE: %{RSAHEADER:pgpsig}\\n");

        CMD_AIX = aix.append("'").toString();
        CMD_LINUX = linux.append("'").toString();
    }

    /**
     * Get the RPM query command to which an RPM name is appended, that returns output suitable for nextRpmData().
     */
    String getBaseCommand() {
        switch(session.getFlavor()) {
          case AIX:
            return CMD_AIX;
          default:
            return CMD_LINUX;
        }
    }

    /**
     * Read the next RpmData from command output.
     */
    RpmData nextRpmData(Iterator<String> lines) {
	RpmData data = null;
	while(lines.hasNext()) {
	    String line = lines.next();
	    if (line.length() == 0 || line.indexOf("not installed") != -1) {
		if (data != null) {
		    return data;
		}
	    } else if (line.startsWith("NAME: ")) {
		data = new RpmData();
		data.name = line.substring(6);
	    } else if (line.startsWith("ARCH: ")) {
		data.arch = line.substring(6);
	    } else if (line.startsWith("VERSION: ")) {
		data.version = line.substring(9);
	    } else if (line.startsWith("RELEASE: ")) {
		data.release = line.substring(9);
	    } else if (line.startsWith("EPOCH: ")) {
		String s = line.substring(7);
		if ("(none)".equals(s)) {
		    s = "0";
		}
		data.epoch = s;

		StringBuffer value = new StringBuffer(s);
		value.append(":").append(data.version);
		value.append("-").append(data.release);
		data.evr = value.toString();

		value = new StringBuffer(data.name);
		value.append("-").append(data.epoch);
		value.append(":").append(data.version);
		value.append("-").append(data.release);
		value.append(".").append(data.arch);
		data.extendedName = value.toString();
	    } else if (line.startsWith("SIGNATURE: ")) {
		String s = line.substring(11);
		if (s.toUpperCase().indexOf("(NONE)") == -1) {
		    if (s.indexOf("Key ID") == -1) {
			data.sigError = true;
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_RPMINFO_SIGKEY, s));
			data.addMessage(msg);
		    } else {
			data.signature = s.substring(s.indexOf("Key ID")+7).trim();
		    }
		}
	    } else if (line.startsWith("/")) {
		data.filepaths.add(line);
	    }
	}
	return data;
    }

    static class RpmData {
	String extendedName, name, arch, version, release, epoch, evr, signature;
	List<String> filepaths;
	List<MessageType> messages;
	boolean sigError;

	RpmData() {
	    filepaths = new ArrayList<String>();
	    sigError = false;
	}

	void addMessage(MessageType msg) {
	    if (messages == null) {
		messages = new ArrayList<MessageType>();
	    }
	    messages.add(msg);
	}
    }
}
