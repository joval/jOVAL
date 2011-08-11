// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.linux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.linux.RpminfoObject;
import oval.schemas.definitions.linux.RpminfoState;
import oval.schemas.definitions.linux.RpminfoTest;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemEVRStringType;
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.linux.RpminfoItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Evaluates Rpminfo OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpminfoAdapter implements IAdapter {
    private IAdapterContext ctx;
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

    public Class getStateClass() {
	return RpminfoState.class;
    }

    public Class getItemClass() {
	return RpminfoItem.class;
    }

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public boolean connect() {
	if (session != null) {
	    try {
		ArrayList<String> list = new ArrayList<String>();
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_RPMINFO_LIST"));
		IProcess p = session.createProcess("rpm -q -a");
		p.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = null;
		while ((line = br.readLine()) != null) {
		    list.add(line);
		}
		br.close();
		rpms = list.toArray(new String[list.size()]);
		Arrays.sort(rpms);
		return true;
	    } catch (Exception e) {
		JOVALSystem.getLogger().log(Level.WARNING, e.getMessage(), e);
	    }
	}
	return false;
    }

    public void disconnect() {
	packageMap = null;
    }

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	RpminfoObject rObj = (RpminfoObject)obj;
	List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	switch(rObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		items.add(JOVALSystem.factories.sc.linux.createRpminfoItem(getItem((String)rObj.getName().getValue())));
	    } catch (Exception e) {
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String s = JOVALSystem.getMessage("ERROR_RPMINFO", (String)rObj.getName().getValue(), e.getMessage());
		msg.setValue(s);
		ctx.addObjectMessage(obj.getId(), msg);
		ctx.log(Level.WARNING, s, e);
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
		MessageType msg = new MessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		ctx.addObjectMessage(obj.getId(), msg);
		ctx.log(Level.WARNING, e.getMessage(), e);
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

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", rObj.getName().getOperation()));
	}

	return items;
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	RpminfoState state = (RpminfoState)st;
	RpminfoItem item = (RpminfoItem)it;

	if (state.getArch() != null) {
	    ResultEnumeration result = ctx.test(state.getArch(), item.getArch());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.getEpoch() != null) {
	    ResultEnumeration result = ctx.test(state.getEpoch(), item.getEpoch());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.getEvr() != null) {
	    ResultEnumeration result = ctx.test(state.getEvr(), item.getEvr());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.getName() != null) {
	    ResultEnumeration result = ctx.test(state.getName(), item.getName());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.getRelease() != null) {
	    ResultEnumeration result = ctx.test(state.getRelease(), item.getRelease());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.getRpmVersion() != null) {
	    ResultEnumeration result = ctx.test(state.getRpmVersion(), item.getVersion());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	if (state.getSignatureKeyid() != null) {
	    ResultEnumeration result = ctx.test(state.getSignatureKeyid(), item.getSignatureKeyid());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
	}
	return ResultEnumeration.TRUE;
    }

    // Private

    private boolean loaded = false;
    private void loadFullPackageMap() {
	if (loaded) return;

	packageMap = new Hashtable<String, RpminfoItem>();
	for (int i=0; i < rpms.length; i++) {
	    try {
		JOVALSystem.getLogger().log(Level.FINER, JOVALSystem.getMessage("STATUS_RPMINFO_RPM", rpms[i]));
		RpminfoItem item = getItem(rpms[i]);
		packageMap.put((String)item.getName().getValue(), item);
	    } catch (Exception e) {
		ctx.log(Level.WARNING, JOVALSystem.getMessage("ERROR_RPMINFO", rpms[i], e.getMessage()), e);
	    }
	}
	loaded = true;
    }

    private RpminfoItem getItem(String packageName) throws Exception {
	RpminfoItem item = packageMap.get(packageName);
	if (item != null) {
	    return item;
	}

	item = JOVALSystem.factories.sc.linux.createRpminfoItem();
	String pkgVersion=null, pkgRelease=null;
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
		EntityItemStringType arch = JOVALSystem.factories.sc.core.createEntityItemStringType();
		arch.setValue(value);
		item.setArch(arch);
	    } else if ("Version".equals(param)) {
		pkgVersion = value;
		RpminfoItem.Version version = JOVALSystem.factories.sc.linux.createRpminfoItemVersion();
		version.setValue(pkgVersion);
		item.setVersion(version);
	    } else if ("Release".equals(param)) {
		pkgRelease = value;
		RpminfoItem.Release release = JOVALSystem.factories.sc.linux.createRpminfoItemRelease();
		release.setValue(pkgRelease);
		item.setRelease(release);
	    } else if ("Signature".equals(param)) {
		EntityItemStringType signatureKeyid = JOVALSystem.factories.sc.core.createEntityItemStringType();
		int ptr = value.indexOf("Key ID");
		if (ptr == -1) {
		    signatureKeyid.setStatus(StatusEnumeration.ERROR);
		    MessageType msg = new MessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage("ERROR_RPMINFO_SIGKEY", value));
		    item.getMessage().add(msg);
		} else {
		    signatureKeyid.setValue(value.substring(ptr+7).trim());
		}
		item.setSignatureKeyid(signatureKeyid);
	    }
	}
	br.close();

	if (isInstalled) {
	    item.setStatus(StatusEnumeration.EXISTS);

	    p = session.createProcess("rpm -q --qf '%{EPOCH}\\n' " + packageName);
	    p.start();
	    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String pkgEpoch = br.readLine();
	    br.close();
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
