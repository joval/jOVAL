// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.DpkginfoObject;
import scap.oval.systemcharacteristics.core.EntityItemEVRStringType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.linux.DpkginfoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects dpkginfo OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class DpkginfoAdapter implements IAdapter {
    private static final String DPKG_QUERY = "/usr/bin/dpkg-query";

    private IUnixSession session;
    private Map<String, DpkginfoItem> packages;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.LINUX) {
	    this.session = (IUnixSession)session;
	    boolean hasDpkg = false;
	    try {
		hasDpkg = this.session.getFilesystem().getFile(DPKG_QUERY).exists();
	    } catch (IOException e) {
		session.getLogger().warn(Message.ERROR_IO, DPKG_QUERY, e.getMessage());
	    }
	    if (hasDpkg) {
		packages = new HashMap<String, DpkginfoItem>();
		classes.add(DpkginfoObject.class);
	    } else {
		notapplicable.add(DpkginfoObject.class);
	    }
	} else {
	    notapplicable.add(DpkginfoObject.class);
	}
	return classes;
    }

    public Collection<DpkginfoItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	initialize();
	DpkginfoObject dObj = (DpkginfoObject)obj;
	String pkgName = (String)dObj.getName().getValue();
	Collection<DpkginfoItem> items = new ArrayList<DpkginfoItem>();
	switch(dObj.getName().getOperation()) {
	  case EQUALS:
	    if (packages.containsKey(pkgName)) {
		items.add(packages.get(pkgName));
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = StringTools.pattern(pkgName);
		for (Map.Entry<String, DpkginfoItem> entry : packages.entrySet()) {
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

	  case NOT_EQUAL: {
	    for (Map.Entry<String, DpkginfoItem> entry : packages.entrySet()) {
		if (!entry.getKey().equals(pkgName)) {
		    items.add(entry.getValue());
		}
	    }
	    break;
	  }

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, dObj.getName().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    private boolean initialized = false;

    private void initialize() throws CollectException {
	if (!initialized) {
	    try {
		StringBuffer cmd = new StringBuffer(DPKG_QUERY);
		cmd.append(" -W -f='${Status}\\t${Package}\\t${Architecture}\\t${Version}\\n'");
		for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
		    DpkginfoItem item = parsePackage(line);
		    if (item != null) {
			packages.put((String)item.getName().getValue(), item);
		    }
		}
		initialized = true;
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new CollectException(e, FlagEnumeration.ERROR);
	    }
	}
    }

    private DpkginfoItem parsePackage(String line) {
	StringTokenizer tok = new StringTokenizer(line, "\t");
	if (tok.countTokens() == 4) {
	    DpkginfoItem item = Factories.sc.linux.createDpkginfoItem();

	    //
	    // Only return installed packages
	    //
	    String status = tok.nextToken();
	    if (status.toLowerCase().indexOf("installed") == -1) {
		return null;
	    }

	    EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	    nameType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    nameType.setValue(tok.nextToken());
	    item.setName(nameType);

	    EntityItemStringType archType = Factories.sc.core.createEntityItemStringType();
	    archType.setDatatype(SimpleDatatypeEnumeration.STRING.value());
	    archType.setValue(tok.nextToken());
	    item.setArch(archType);

	    String version_release = tok.nextToken();

	    DpkginfoItem.Epoch epochType = Factories.sc.linux.createDpkginfoItemEpoch();
	    int ptr = version_release.indexOf(":");
	    String epoch = null;
	    if (ptr == -1) {
		epochType.setValue("(none)");
		epoch = "0";
	    } else {
		epoch = version_release.substring(0,ptr);
		epochType.setValue(epoch);
		version_release = version_release.substring(ptr+1);
	    }
	    item.setEpoch(epochType);

	    DpkginfoItem.Version versionType = Factories.sc.linux.createDpkginfoItemVersion();
	    ptr = version_release.indexOf("-");
	    String release = null;
	    if (ptr == -1) {
		versionType.setValue(version_release);
	    } else {
		versionType.setValue(version_release.substring(0,ptr));
		release = version_release.substring(ptr+1);
	    }
	    item.setDpkginfoVersion(versionType);

	    DpkginfoItem.Release releaseType = Factories.sc.linux.createDpkginfoItemRelease();
	    if (release == null) {
		releaseType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		releaseType.setValue(release);
	    }
	    item.setRelease(releaseType);

	    EntityItemEVRStringType evrType = Factories.sc.core.createEntityItemEVRStringType();
	    evrType.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
	    StringBuffer evr = new StringBuffer(epoch).append(":").append(version_release);
	    evrType.setValue(evr.toString());
	    item.setEvr(evrType);

	    return item;
	} else {
	    session.getLogger().warn(JOVALMsg.ERROR_DPKGINFO_LINE, line);
	    return null;
	}
    }
}
