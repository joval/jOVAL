// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.linux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
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
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.NotCollectableException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
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
    private String[] rpms;

    public RpminfoAdapter(IUnixSession session) {
	this.session = session;
	packageMap = new Hashtable<String, RpminfoItem>();
    }

    // Implement IAdapter

    private static Class[] objectClasses = {RpminfoObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	if (session != null) {
	    try {
		ArrayList<String> list = new ArrayList<String>();
		session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_LIST);
		for (String line : SafeCLI.multiLine("rpm -q -a", session, IUnixSession.TIMEOUT_M)) {
		    list.add(line);
		}
		rpms = list.toArray(new String[list.size()]);
		return true;
	    } catch (Exception e) {
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return false;
    }

    public void disconnect() {
	packageMap = null;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws NotCollectableException {
	RpminfoObject rObj = (RpminfoObject)rc.getObject();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(rObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		items.add(JOVALSystem.factories.sc.linux.createRpminfoItem(getItem((String)rObj.getName().getValue())));
	    } catch (Exception e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALSystem.getMessage(JOVALMsg.ERROR_RPMINFO, (String)rObj.getName().getValue(), e.getMessage());
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
			items.add(JOVALSystem.factories.sc.linux.createRpminfoItem(packageMap.get(packageName)));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadFullPackageMap();
	    String name = (String)rObj.getName().getValue();
	    for (String packageName : packageMap.keySet()) {
		if (!packageName.equals(name)) {
		    items.add(JOVALSystem.factories.sc.linux.createRpminfoItem(packageMap.get(packageName)));
		}
	    }
	    break;
	  }

	  default: {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, rObj.getName().getOperation());
	    throw new NotCollectableException(s);
	  }
	}

	return items;
    }

    // Private

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) return;

	session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_FULL);
	packageMap = new Hashtable<String, RpminfoItem>();
	for (int i=0; i < rpms.length; i++) {
	    try {
		RpminfoItem item = getItem(rpms[i]);
		packageMap.put((String)item.getName().getValue(), item);
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.ERROR_RPMINFO, rpms[i]);
		session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	loaded = true;
    }

    private RpminfoItem getItem(String packageName) throws Exception {
	RpminfoItem item = packageMap.get(packageName);
	if (item != null) {
	    return item;
	}

	session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_RPM, packageName);
	item = JOVALSystem.factories.sc.linux.createRpminfoItem();
	String pkgArch=null, pkgVersion=null, pkgRelease=null;
	boolean isInstalled = false;

	Iterator<String> lines = SafeCLI.multiLine("rpm -q " + packageName + " -i", session, IUnixSession.TIMEOUT_S).iterator();
	for (int lineNum=1; lines.hasNext(); lineNum++) {
	    String line = lines.next();
	    String param=null, value=null;
	    switch(lineNum) {
	      case 1:
		if (line.indexOf("not installed") == -1) {
		    isInstalled = true;
		    // NB: fall-through to default case
		} else {
		    break;
		}

	      default:
		int ptr = line.indexOf(":");
		if (ptr != -1) {
		    param = line.substring(0,ptr).trim();
		    value = line.substring(ptr+1).trim();
		}
		break;
	    }

	    if (param == null) {
		// unexpected or blank line; continue processing remaining lines
	    } else if ("Description".equals(param)) {
		StringBuffer sb = new StringBuffer();
		for (lineNum = 1; lines.hasNext(); lineNum++) {
		    line = lines.next();
		    switch(lineNum) {
		      case 1:
			sb = new StringBuffer(line);
			break;
		      default:
			sb.append(" ").append(line);
			break;
		    }
		}
		value = sb.toString();
		break; // last param; break the enclosing for-loop
	    } else if ("Name".equals(param)) {
		packageName = value;
		EntityItemStringType name = JOVALSystem.factories.sc.core.createEntityItemStringType();
		name.setValue(packageName);
		item.setName(name);
	    } else if ("Architecture".equals(param)) {
		pkgArch = value;
		EntityItemStringType arch = JOVALSystem.factories.sc.core.createEntityItemStringType();
		arch.setValue(value);
		item.setArch(arch);
	    } else if ("Version".equals(param)) {
		pkgVersion = value;
		RpminfoItem.Version version = JOVALSystem.factories.sc.linux.createRpminfoItemVersion();
		version.setValue(pkgVersion);
		item.setRpmVersion(version);
	    } else if ("Release".equals(param)) {
		pkgRelease = value;
		RpminfoItem.Release release = JOVALSystem.factories.sc.linux.createRpminfoItemRelease();
		release.setValue(pkgRelease);
		item.setRelease(release);
	    } else if ("Signature".equals(param)) {
		EntityItemStringType signatureKeyid = JOVALSystem.factories.sc.core.createEntityItemStringType();
		if (value.toUpperCase().indexOf("(NONE)") != -1) {
		    signatureKeyid.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else if (value.indexOf("Key ID") == -1) {
		    signatureKeyid.setStatus(StatusEnumeration.ERROR);
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_RPMINFO_SIGKEY, value));
		    item.getMessage().add(msg);
		} else {
		    signatureKeyid.setValue(value.substring(value.indexOf("Key ID")+7).trim());
		}
		item.setSignatureKeyid(signatureKeyid);
	    }
	}

	if (isInstalled) {
	    item.setStatus(StatusEnumeration.EXISTS);
	    String pkgEpoch = SafeCLI.exec("rpm -q --qf %{EPOCH} " + packageName, session, IUnixSession.TIMEOUT_S);
	    if ("(none)".equals(pkgEpoch)) {
		pkgEpoch = "0";
	    }
	    RpminfoItem.Epoch epoch = JOVALSystem.factories.sc.linux.createRpminfoItemEpoch();
	    epoch.setValue(pkgEpoch);
	    item.setEpoch(epoch);

	    EntityItemEVRStringType evr = JOVALSystem.factories.sc.core.createEntityItemEVRStringType();
	    evr.setValue(pkgEpoch + ":" + pkgVersion + "-" + pkgRelease);
	    evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
	    item.setEvr(evr);

	    EntityItemStringType extendedName = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    extendedName.setValue(packageName + "-" + pkgEpoch + ":" + pkgVersion + "-" + pkgRelease + "." + pkgArch);
	    item.setExtendedName(extendedName);

	    for (String line : SafeCLI.multiLine("rpm -ql " + packageName, session, IUnixSession.TIMEOUT_S)) {
		if (!"(contains no files)".equals(line.trim())) {
		    EntityItemStringType filepath = JOVALSystem.factories.sc.core.createEntityItemStringType();
		    filepath.setValue(line.trim());
		    item.getFilepath().add(filepath);
		}
	    }
	} else {
	    EntityItemStringType name = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    name.setValue(packageName);
	    item.setName(name);
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}

	packageMap.put(packageName, item);
	return item;
    }
}
