// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.linux;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.linux.RpminfoObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.linux.RpminfoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.Version;

/**
 * Evaluates Rpminfo OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpminfoAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, RpminfoItem> packageMap;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case AIX:
	      case LINUX:
		packageMap = new Hashtable<String, RpminfoItem>();
		classes.add(RpminfoObject.class);
		break;
	    }
	}
	return classes;
    }

    public Collection<RpminfoItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	RpminfoObject rObj = (RpminfoObject)obj;
	Collection<RpminfoItem> items = new Vector<RpminfoItem>();
	switch(rObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		items.add(getItem((String)rObj.getName().getValue()));
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
		Pattern p = Pattern.compile((String)rObj.getName().getValue());
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

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) {
	    return;
	}

	try {
	    session.getLogger().info(JOVALMsg.STATUS_RPMINFO_LIST);
	    packageMap = new Hashtable<String, RpminfoItem>();
	    for (String rpm : SafeCLI.multiLine("rpm -qa", session, IUnixSession.Timeout.M)) {
		try {
		    RpminfoItem item = getItem(rpm);
		    packageMap.put((String)item.getName().getValue(), item);
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.ERROR_RPMINFO, rpm);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private RpminfoItem getItem(String packageName) throws Exception {
	RpminfoItem item = packageMap.get(packageName);
	if (item != null) {
	    return item;
	}

	session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_RPM, packageName);
	item = Factories.sc.linux.createRpminfoItem();

	String pkgArch = null, pkgEpoch = null, pkgVersion = null, pkgRelease = null;
	StringBuffer command = new StringBuffer("rpm -q --qf \'");
	command.append("%{NAME}\\n");
	command.append("%{ARCH}\\n");
	command.append("%{VERSION}\\n");
	command.append("%{RELEASE}\\n");
	command.append("%{EPOCH}\\n");
	switch(session.getFlavor()) {
	  case LINUX:
	    command.append("%{RSAHEADER:pgpsig}\\n");
	    break;
	}
	command.append("\' ").append(packageName);
	List<String> lines = SafeCLI.multiLine(command.toString(), session, IUnixSession.Timeout.S);

	boolean isInstalled = lines.size() > 0;
	int lineNum = 1;
	for (String line : lines) {
	    if (!isInstalled) {
		break;
	    }
	    switch(lineNum++) {
	      case 1: // NAME
		if (line.indexOf("not installed") == -1 && line.length() > 0) {
		    packageName = line;
		} else {
		    isInstalled = false;
		}
		break;

	      case 2: // ARCH
		pkgArch = line;
		EntityItemStringType arch = Factories.sc.core.createEntityItemStringType();
		arch.setValue(pkgArch);
		item.setArch(arch);
		break;

	      case 3: // VERSION
		pkgVersion = line;
		RpminfoItem.Version version = Factories.sc.linux.createRpminfoItemVersion();
		version.setValue(pkgVersion);
		item.setRpmVersion(version);
		break;

	      case 4: // RELEASE
		pkgRelease = line;
		RpminfoItem.Release release = Factories.sc.linux.createRpminfoItemRelease();
		release.setValue(pkgRelease);
		item.setRelease(release);
		break;

	      case 5: // EPOCH
		if ("(none)".equalsIgnoreCase(line)) {
		    pkgEpoch = "0";
		} else {
		    pkgEpoch = line;
		}
		RpminfoItem.Epoch epoch = Factories.sc.linux.createRpminfoItemEpoch();
		epoch.setValue(pkgEpoch);
		item.setEpoch(epoch);

		EntityItemEVRStringType evr = Factories.sc.core.createEntityItemEVRStringType();
		evr.setValue(pkgEpoch + ":" + pkgVersion + "-" + pkgRelease);
		evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
		item.setEvr(evr);

		EntityItemStringType extendedName = Factories.sc.core.createEntityItemStringType();
		extendedName.setValue(packageName + "-" + pkgEpoch + ":" + pkgVersion + "-" + pkgRelease + "." + pkgArch);
		item.setExtendedName(extendedName);
		break;

	      case 6: // RSAHEADER -- Linux only
		EntityItemStringType signatureKeyid = Factories.sc.core.createEntityItemStringType();
		if (line.toUpperCase().indexOf("(NONE)") != -1) {
		    signatureKeyid.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else if (line.indexOf("Key ID") == -1) {
		    signatureKeyid.setStatus(StatusEnumeration.ERROR);
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_RPMINFO_SIGKEY, line));
		    item.getMessage().add(msg);
		} else {
		    signatureKeyid.setValue(line.substring(line.indexOf("Key ID")+7).trim());
		}
		item.setSignatureKeyid(signatureKeyid);
	    }
	}

	EntityItemStringType name = Factories.sc.core.createEntityItemStringType();
	name.setValue(packageName);
	item.setName(name);

	if (isInstalled) {
	    for (String line : SafeCLI.multiLine("rpm -ql " + packageName, session, IUnixSession.Timeout.S)) {
		if (!"(contains no files)".equals(line.trim())) {
		    EntityItemStringType filepath = Factories.sc.core.createEntityItemStringType();
		    filepath.setValue(line.trim());
		    item.getFilepath().add(filepath);
		}
	    }
	} else {
	    throw new NoSuchElementException(packageName);
	}

	packageMap.put(packageName, item);
	return item;
    }
}
