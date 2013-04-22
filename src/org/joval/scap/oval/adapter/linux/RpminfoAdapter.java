// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.RpminfoObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.EntityItemEVRStringType;
import scap.oval.systemcharacteristics.linux.RpminfoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Rpminfo OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpminfoAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, RpminfoItem> packageMap;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case AIX:
	      case LINUX:
		packageMap = new HashMap<String, RpminfoItem>();
		classes.add(RpminfoObject.class);
		break;
	    }
	}
	if (classes.size() == 0) {
	    notapplicable.add(RpminfoObject.class);
	}
	return classes;
    }

    public Collection<RpminfoItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	RpminfoObject rObj = (RpminfoObject)obj;
	Collection<RpminfoItem> items = new ArrayList<RpminfoItem>();
	switch(rObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		items.add(getItem(SafeCLI.checkArgument((String)rObj.getName().getValue(), session)));
	    } catch (NoSuchElementException e) {
		// the package is not installed; don't add to the item list
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALMsg.getMessage(JOVALMsg.ERROR_RPMINFO, (String)rObj.getName().getValue(), e.getMessage());
		msg.setValue(s);
		rc.addMessage(msg);
		session.getLogger().warn(s, e);
	    }
	    break;

	  case PATTERN_MATCH:
	    loadFullPackageMap();
	    try {
		Pattern p = StringTools.pattern((String)rObj.getName().getValue());
		for (String packageName : packageMap.keySet()) {
		    if (p.matcher(packageName).find()) {
			items.add(packageMap.get(packageName));
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
	    loadFullPackageMap();
	    String name = (String)rObj.getName().getValue();
	    for (String packageName : packageMap.keySet()) {
		if (!packageName.equals(name)) {
		    items.add(packageMap.get(packageName));
		}
	    }
	    break;
	  }

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, rObj.getName().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}

	return items;
    }

    // Private

    private String getBaseCommand() {
	StringBuffer command = new StringBuffer("rpm -ql --qf '");
	command.append("\\nNAME: %{NAME}\\n");
	command.append("ARCH: %{ARCH}\\n");
	command.append("VERSION: %{VERSION}\\n");
	command.append("RELEASE: %{RELEASE}\\n");
	command.append("EPOCH: %{EPOCH}\\n");
	switch(session.getFlavor()) {
	  case LINUX:
	    command.append("SIGNATURE: %{RSAHEADER:pgpsig}\\n");
	    break;
	}
	return command.append("'").toString();
    }

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) {
	    return;
	}
	try {
	    session.getLogger().info(JOVALMsg.STATUS_RPMINFO_LIST);
	    packageMap = new HashMap<String, RpminfoItem>();
	    StringBuffer cmd = new StringBuffer("rpm -qa | xargs -I{} ");
	    cmd.append(getBaseCommand()).append(" '{}'");

	    RpminfoItem item = null;
	    Iterator<String> iter = SafeCLI.manyLines(cmd.toString(), null, session);
	    while ((item = nextRpmInfo(iter)) != null) {
		packageMap.put((String)item.getName().getValue(), item);
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Get an RpminfoItem for a single package.
     */
    private RpminfoItem getItem(String packageName) throws Exception {
	if (packageMap.containsKey(packageName)) {
	    return packageMap.get(packageName);
	} else if (loaded) {
	    throw new NoSuchElementException(packageName);
	}

	session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_RPM, packageName);

	StringBuffer command = new StringBuffer(getBaseCommand()).append(" '").append(packageName).append("'");
	RpminfoItem item = nextRpmInfo(SafeCLI.multiLine(command.toString(), session, IUnixSession.Timeout.S).iterator());
	if (item == null) {
	    throw new NoSuchElementException(packageName);
	} else {
	    packageMap.put(packageName, item);
	    return item;
	}
    }

    /**
     * Builds the next RpminfoItem from the command output. Returns null when there is no more RPM data in the iterator.
     */
    private RpminfoItem nextRpmInfo(Iterator<String> lines) {
	RpminfoItem item = null;
	while(lines.hasNext()) {
	    String line = lines.next();
	    if (line.length() == 0 || line.indexOf("not installed") != -1) {
		if (item != null) {
		    return item;
		}
	    } else if (line.startsWith("NAME: ")) {
		item = Factories.sc.linux.createRpminfoItem();
		EntityItemStringType name = Factories.sc.core.createEntityItemStringType();
		name.setValue(line.substring(6));
		item.setName(name);
	    } else if (line.startsWith("ARCH: ")) {
		EntityItemStringType arch = Factories.sc.core.createEntityItemStringType();
		arch.setValue(line.substring(6));
		item.setArch(arch);
	    } else if (line.startsWith("VERSION: ")) {
		RpminfoItem.Version version = Factories.sc.linux.createRpminfoItemVersion();
		version.setValue(line.substring(9));
		item.setRpmVersion(version);
	    } else if (line.startsWith("RELEASE: ")) {
		RpminfoItem.Release release = Factories.sc.linux.createRpminfoItemRelease();
		release.setValue(line.substring(9));
		item.setRelease(release);
	    } else if (line.startsWith("EPOCH: ")) {
		RpminfoItem.Epoch epoch = Factories.sc.linux.createRpminfoItemEpoch();
		String s = line.substring(7);
		if ("(none)".equals(s)) {
		    s = "0";
		}
		epoch.setValue(s);
		item.setEpoch(epoch);

		EntityItemEVRStringType evr = Factories.sc.core.createEntityItemEVRStringType();
		StringBuffer value = new StringBuffer(s);
		value.append(":").append((String)item.getRpmVersion().getValue());
		value.append("-").append((String)item.getRelease().getValue());
		evr.setValue(value.toString());
		evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
		item.setEvr(evr);

		EntityItemStringType extendedName = Factories.sc.core.createEntityItemStringType();
		value = new StringBuffer((String)item.getName().getValue());
		value.append("-").append((String)item.getEpoch().getValue());
		value.append(":").append((String)item.getRpmVersion().getValue());
		value.append("-").append((String)item.getRelease().getValue());
		value.append(".").append((String)item.getArch().getValue());
		extendedName.setValue(value.toString());
		item.setExtendedName(extendedName);
	    } else if (line.startsWith("SIGNATURE: ")) {
		String s = line.substring(11);
		EntityItemStringType signature = Factories.sc.core.createEntityItemStringType();
		if (s.toUpperCase().indexOf("(NONE)") != -1) {
		    signature.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else if (s.indexOf("Key ID") == -1) {
		    signature.setStatus(StatusEnumeration.ERROR);
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_RPMINFO_SIGKEY, s));
		    item.getMessage().add(msg);
		} else {
		    signature.setValue(s.substring(s.indexOf("Key ID")+7).trim());
		}
		item.setSignatureKeyid(signature);
	    } else if (line.startsWith("/")) {
		EntityItemStringType filepath = Factories.sc.core.createEntityItemStringType();
		filepath.setValue(line);
		item.getFilepath().add(filepath);
	    }
	}
	return item;
    }
}
