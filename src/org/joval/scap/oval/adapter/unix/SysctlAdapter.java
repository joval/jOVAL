// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IReader;
import jsaf.intf.io.IReaderGobbler;
import jsaf.intf.system.ISession;
import jsaf.intf.system.IEnvironment;
import jsaf.intf.unix.io.IUnixFilesystem;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.io.StreamTool;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.SysctlObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.unix.SysctlItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects Sysctl OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SysctlAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, SysctlItem> parameters;

    // Implement IAdapter

    public Collection<Class> init(ISession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case LINUX:
	      case MACOSX:
		parameters = new HashMap<String, SysctlItem>();
		classes.add(SysctlObject.class);
		break;
	    }
	}
	return classes;
    }

    public Collection<SysctlItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	SysctlObject sObj = (SysctlObject)obj;
	Collection<SysctlItem> items = new ArrayList<SysctlItem>();
	String name = (String)sObj.getName().getValue();
	switch(sObj.getName().getOperation()) {
	  case EQUALS:
	    if (parameters.containsKey(name)) {
		items.add(parameters.get(name));
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(name);
		for (Map.Entry<String, SysctlItem> entry : parameters.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.add(entry.getValue());
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case CASE_INSENSITIVE_EQUALS:
	    for (Map.Entry<String, SysctlItem> entry : parameters.entrySet()) {
		if (entry.getKey().equalsIgnoreCase(name)) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  case NOT_EQUAL:
	    for (Map.Entry<String, SysctlItem> entry : parameters.entrySet()) {
		if (!entry.getKey().equals(name)) {
		    items.add(entry.getValue());
		}
	    }
	    break;

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, sObj.getName().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private boolean initialized = false;
    private void init() {
	if (initialized) {
	    return;
	}
	File localTemp = null;
	IFile remoteTemp = null;
	BufferedReader reader = null;
	try {
	    StringBuffer sb = new StringBuffer();
	    switch(session.getFlavor()) {
	      case MACOSX:
		sb.append("/usr/sbin/sysctl -X");
		break;
	      case LINUX:
		sb.append("/sbin/sysctl -A");
		break;
	    }
	    sb.append(" | gzip > ");
	    String unique = null;
	    synchronized(this) {
		unique = Long.toString(System.currentTimeMillis());
		Thread.sleep(1);
	    }
	    IEnvironment env = session.getEnvironment();
	    String tempPath = env.expand("%HOME%" + IUnixFilesystem.DELIM_STR + ".jOVAL.sysctl" + unique + ".gz");
	    sb.append(tempPath);
	    //
	    // Run the command on the remote host, storing the results in a temporary file, then tranfer the file
	    // locally and read it.
	    //
	    SafeCLI.exec(sb.toString(), null, null, session, session.getTimeout(IUnixSession.Timeout.M), new RG(), new RG());
	    remoteTemp =  session.getFilesystem().getFile(tempPath, IFile.Flags.READWRITE);
	    GZIPInputStream gzin = null;
	    if (session.getWorkspace() == null || ISession.LOCALHOST.equals(session.getHostname())) {
		gzin = new GZIPInputStream(remoteTemp.getInputStream());
	    } else {
		localTemp = File.createTempFile("search", null, session.getWorkspace());
		StreamTool.copy(remoteTemp.getInputStream(), new FileOutputStream(localTemp), true);
		gzin = new GZIPInputStream(new FileInputStream(localTemp));
	    }
	    reader = new BufferedReader(new InputStreamReader(gzin, StringTools.ASCII));
	    String line;
	    while((line = reader.readLine()) != null) {
		parseLine(line);
	    }
	    initialized = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (reader != null) {
		try {
		    reader.close();
		} catch (IOException e) {
		}
	    }
	    if (localTemp != null) {
		localTemp.delete();
	    }
	    if (remoteTemp != null) {
		try {
		    remoteTemp.delete();
		    if (remoteTemp.exists()) {
			SafeCLI.exec("rm -f " + remoteTemp.getPath(), session, IUnixSession.Timeout.S);
		    }
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
    }

    /**
     * Parse a line from the output of the sysctl command, whose key might have been encountered previously.
     */
    private void parseLine(String line) {
	if (line.length() == 0) {
	    // skip any blank lines
	    return;
	}
	int ptr = 0;
	int offset = 0;
	int x = line.indexOf(" = ");
	int y = line.indexOf(": ");
	if (x == -1 && y == -1) {
	    session.getLogger().warn(JOVALMsg.WARNING_SYSCTL, line);
	    return;
	} else if (x == -1) {
	    ptr = y;
	    offset = 2;
	} else if (y == -1) {
	    ptr = x;
	    offset = 3;
	} else if (x < y) {
	    ptr = x;
	    offset = 3;
	} else {
	    ptr = y;
	    offset = 2;
	}
	String name = line.substring(0, ptr);
	SysctlItem item = parameters.get(name);
	if (item == null) {
	    item = Factories.sc.unix.createSysctlItem();
	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    nameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    nameType.setValue(name);
	    item.setName(nameType);
	}

	EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	String value = line.substring(ptr + offset);
	if (session.getFlavor() == IUnixSession.Flavor.MACOSX && value.startsWith("Format:S,")) {
	    valueType.setDatatype(SimpleDatatypeEnumeration.BINARY.value());
	    ptr = value.indexOf("Dump:0x");
	    value = value.substring(ptr + 7);
	}
	valueType.setValue(value);
	item.getValue().add(valueType);
	parameters.put(name, item);
    }

    /**
     * IReaderGobbler that logs lines as warnings.
     */
    class RG implements IReaderGobbler {
	RG() {}

	// Implement IReaderGobbler

	public void gobble(IReader err) throws IOException {
	    String line = null;
	    while((line = err.readLine()) != null) {
		if (line.trim().length() > 0) {
		    session.getLogger().debug(JOVALMsg.WARNING_SYSCTL, line);
		}
	    }
	}
    }
}
