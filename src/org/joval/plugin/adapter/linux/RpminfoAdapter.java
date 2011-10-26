// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
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
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates Rpminfo OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpminfoAdapter implements IAdapter {
    private ISession session;
    private Hashtable<String, RpminfoItem> packageMap;
    private String[] rpms;

    public RpminfoAdapter(ISession session) {
	this.session = session;
	packageMap = new Hashtable<String, RpminfoItem>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return RpminfoObject.class;
    }

    public boolean connect() {
	if (session != null) {
	    try {
		ArrayList<String> list = new ArrayList<String>();
		JOVALSystem.getLogger().trace(JOVALMsg.STATUS_RPMINFO_LIST);
		IProcess p = session.createProcess("rpm -q -a");
		p.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null) {
		    list.add(line);
		}
		br.close();
		p.waitFor(0);
		rpms = list.toArray(new String[list.size()]);
		return true;
	    } catch (Exception e) {
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return false;
    }

    public void disconnect() {
	packageMap = null;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
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
		JOVALSystem.getLogger().warn(s, e);
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
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
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
	    throw new OvalException(s);
	  }
	}

	return items;
    }

    // Private

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) return;

	JOVALSystem.getLogger().trace(JOVALMsg.STATUS_RPMINFO_FULL);
	packageMap = new Hashtable<String, RpminfoItem>();
	for (int i=0; i < rpms.length; i++) {
	    try {
		RpminfoItem item = getItem(rpms[i]);
		packageMap.put((String)item.getName().getValue(), item);
	    } catch (Exception e) {
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_RPMINFO, rpms[i]);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	loaded = true;
    }

    private RpminfoItem getItem(String packageName) throws Exception {
	RpminfoItem item = packageMap.get(packageName);
	if (item != null) {
	    return item;
	}

	JOVALSystem.getLogger().trace(JOVALMsg.STATUS_RPMINFO_RPM, packageName);
	item = JOVALSystem.factories.sc.linux.createRpminfoItem();
	String pkgArch=null, pkgVersion=null, pkgRelease=null;
	IProcess p = session.createProcess("rpm -q " + packageName + " -i");
	p.start();
	BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	boolean isInstalled = false;
	String line = null;
	for (int lineNum=1; (line = br.readLine()) != null; lineNum++) {
	    String param=null, value=null;
	    switch(lineNum) {
	      case 1:
		if (line.indexOf("not installed") == -1) {
		    isInstalled = true;
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

	    if ("Description".equals(param)) {
		StringBuffer sb = new StringBuffer();
		for(lineNum = 1; (line = br.readLine()) != null; lineNum++) {
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
	br.close();
	p.waitFor(0);

	if (isInstalled) {
	    item.setStatus(StatusEnumeration.EXISTS);

	    p = session.createProcess("rpm -q --qf '%{EPOCH}\\n' " + packageName);
	    p.start();
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String pkgEpoch = br.readLine();
	    br.close();
	    p.waitFor(0);
	    if (pkgEpoch.equals("(none)")) {
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

	    p = session.createProcess("rpm -ql " + packageName);
	    p.start();
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    while((line = br.readLine()) != null) {
		EntityItemStringType filepath = JOVALSystem.factories.sc.core.createEntityItemStringType();
		filepath.setValue(line.trim());
		item.getFilepath().add(filepath);
	    }
	    br.close();
	    p.waitFor(0);
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
