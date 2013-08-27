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
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.RpmInfoBehaviors;
import scap.oval.definitions.linux.RpminfoObject;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.EntityItemEVRStringType;
import scap.oval.systemcharacteristics.linux.RpminfoItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.scap.oval.IBatch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.Batch;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Rpminfo OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpminfoAdapter implements IAdapter, IBatch {
    private IUnixSession session;
    private Collection<String> packageList;
    private Map<String, RpminfoItemData> packageMap;
    private boolean loaded = false;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case AIX:
	      case LINUX:
		packageMap = new HashMap<String, RpminfoItemData>();
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
		items.addAll(getItems(SafeCLI.checkArgument((String)rObj.getName().getValue(), session), rObj));
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
			items.add(packageMap.get(packageName).toItem(rObj.getBehaviors()));
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
		    items.add(packageMap.get(packageName).toItem(rObj.getBehaviors()));
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

    // Implement IBatch

    private Collection<IRequest> queue;

    public boolean queue(IRequest request) {
	if (batchable(request)) {
	    if (queue == null) {
		queue = new ArrayList<IRequest>();
	    }
	    queue.add(request);
	    return true;
	} else {
	    return false;
	}
    }

    public Collection<IResult> exec() {
	Collection<IResult> results = new ArrayList<IResult>();
	if (queue != null) {
	    try {
		if (!loaded) {
		    //
		    // Build a newline-delimited list of all the packages about which we'll need information.
		    //
		    initPackageList();
		    StringBuffer sb = new StringBuffer();
		    for (IRequest request : queue) {
			RpminfoObject rObj = (RpminfoObject)request.getObject();
			String name = SafeCLI.checkArgument((String)rObj.getName().getValue(), session);
			boolean check = false;
			if (packageList.contains(name)) {
			    check = true;
			} else {
			    //
			    // Check for a "short name" match
			    //
			    for (String packageName : packageList) {
				if (packageName.startsWith(name + "-")) {
				    if (!packageMap.containsKey(packageName)) {
					check = true;
				    }
				    break;
				}
			    }
			}
			if (check) {
			    if (sb.length() > 0) {
				sb.append("\\n");
			    }
			    sb.append(name);
			}
		    }

		    //
		    // Gather information about packages in a single pass, and it all to the package map.
		    //
		    if (sb.length() > 0) {
			//
			// Execute a single command to retrieve information about all the packges.
			//
			StringBuffer cmd = new StringBuffer("echo -e \"").append(sb.toString()).append("\"");
			cmd.append(" | xargs -I{} rpm -ql ").append(getBaseCommand()).append(" '{}'");
			RpminfoItemData data = null;
			Iterator<String> iter = SafeCLI.manyLines(cmd.toString(), null, session);
			while ((data = nextRpmInfo(iter)) != null) {
			    packageMap.put((String)data.name.getValue(), data);
			}
		    }
		}

		//
		// Use the package map to create result for all the requests.
		//
		for (IRequest request : queue) {
		    RpminfoObject rObj = (RpminfoObject)request.getObject();
		    String name = (String)rObj.getName().getValue();
		    Collection<RpminfoItem> result = new ArrayList<RpminfoItem>();
		    if (packageMap.containsKey(name)) {
			result.add(packageMap.get(name).toItem(rObj.getBehaviors()));
		    } else {
			//
			// Look for a "short name" match
			//
			for (Map.Entry<String, RpminfoItemData> entry : packageMap.entrySet()) {
			    if (entry.getKey().startsWith(name + "-")) {
				result.add(entry.getValue().toItem(rObj.getBehaviors()));
				break;
			    }
			}
		    }
		    results.add(new Batch.Result(result, request.getContext()));
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		for (IRequest request : queue) {
		    results.add(new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), request.getContext()));
		}
	    }
	}
	return results;
    }

    // Private

    /**
     * Only batch up exact match requests. Anything else will result in a full load, which obviates collection in
     * the batch exec phase anyway.
     */
    private boolean batchable(IRequest request) {
	RpminfoObject rObj = (RpminfoObject)request.getObject();
	return rObj.isSetName() && rObj.getName().getOperation() == OperationEnumeration.EQUALS;
    }

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

    /**
     * Populate packageList if it's not already populated. Idempotent.
     */
    private void initPackageList() throws Exception {
	if (packageList == null) {
	    session.getLogger().info(JOVALMsg.STATUS_RPMINFO_LIST);
	    packageList = new HashSet<String>();
	    for (String name : SafeCLI.multiLine("rpm -qa", session, IUnixSession.Timeout.M)) {
		packageList.add(name);
	    }
	}
    }

    /**
     * Load comprehensive information about every package on the system. Also initializes packageList. Idempotent.
     */
    private void loadFullPackageMap() {
	if (loaded) {
	    return;
	}
	try {
	    packageList = new HashSet<String>();
	    packageMap = new HashMap<String, RpminfoItemData>();
	    StringBuffer cmd = new StringBuffer("rpm -qa | xargs -I{} ");
	    cmd.append(getBaseCommand()).append(" '{}'");

	    RpminfoItemData data = null;
	    Iterator<String> iter = SafeCLI.manyLines(cmd.toString(), null, session);
	    while ((data = nextRpmInfo(iter)) != null) {
		String packageName = (String)data.name.getValue();
		packageList.add(packageName);
		packageMap.put(packageName, data);
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Get RpminfoItems matching a single package name (potentially a "short name", which can lead to multiple results).
     */
    private Collection<RpminfoItem> getItems(String packageName, RpminfoObject rObj) throws Exception {
	Collection<RpminfoItem> result = new ArrayList<RpminfoItem>();
	if (packageMap.containsKey(packageName)) {
	    //
	    // Return a previously-found exact match
	    //
	    result.add(packageMap.get(packageName).toItem(rObj.getBehaviors()));
	} else if (loaded) {
	    //
	    // Look for a "short name" match
	    //
	    for (Map.Entry<String, RpminfoItemData> entry : packageMap.entrySet()) {
		if (entry.getKey().startsWith(packageName + "-")) {
		    result.add(entry.getValue().toItem(rObj.getBehaviors()));
		    break;
		}
	    }
	} else {
	    //
	    // Query the RPM database for the package name; cache and return the results
	    //
	    session.getLogger().trace(JOVALMsg.STATUS_RPMINFO_RPM, packageName);
	    String command = new StringBuffer(getBaseCommand()).append(" '").append(packageName).append("'").toString();
	    Iterator<String> output = SafeCLI.multiLine(command, session, IUnixSession.Timeout.S).iterator();
	    RpminfoItemData data = null;
	    while ((data = nextRpmInfo(output)) != null) {
		packageMap.put((String)data.name.getValue(), data);
		result.add(data.toItem(rObj.getBehaviors()));
	    }
	}
	return result;
    }

    /**
     * Builds the next RpminfoItemData from the command output. Returns null when there is no more RPM data in the iterator.
     */
    private RpminfoItemData nextRpmInfo(Iterator<String> lines) {
	RpminfoItemData data = null;
	while(lines.hasNext()) {
	    String line = lines.next();
	    if (line.length() == 0 || line.indexOf("not installed") != -1) {
		if (data != null) {
		    return data;
		}
	    } else if (line.startsWith("NAME: ")) {
		data = new RpminfoItemData();
		data.name = Factories.sc.core.createEntityItemStringType();
		data.name.setValue(line.substring(6));
	    } else if (line.startsWith("ARCH: ")) {
		data.arch = Factories.sc.core.createEntityItemStringType();
		data.arch.setValue(line.substring(6));
	    } else if (line.startsWith("VERSION: ")) {
		data.version = Factories.sc.linux.createRpminfoItemVersion();
		data.version.setValue(line.substring(9));
	    } else if (line.startsWith("RELEASE: ")) {
		data.release = Factories.sc.linux.createRpminfoItemRelease();
		data.release.setValue(line.substring(9));
	    } else if (line.startsWith("EPOCH: ")) {
		data.epoch = Factories.sc.linux.createRpminfoItemEpoch();
		String s = line.substring(7);
		if ("(none)".equals(s)) {
		    s = "0";
		}
		data.epoch.setValue(s);

		data.evr = Factories.sc.core.createEntityItemEVRStringType();
		StringBuffer value = new StringBuffer(s);
		value.append(":").append((String)data.version.getValue());
		value.append("-").append((String)data.release.getValue());
		data.evr.setValue(value.toString());
		data.evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());

		data.extendedName = Factories.sc.core.createEntityItemStringType();
		value = new StringBuffer((String)data.name.getValue());
		value.append("-").append((String)data.epoch.getValue());
		value.append(":").append((String)data.version.getValue());
		value.append("-").append((String)data.release.getValue());
		value.append(".").append((String)data.arch.getValue());
		data.extendedName.setValue(value.toString());
	    } else if (line.startsWith("SIGNATURE: ")) {
		String s = line.substring(11);
		data.signature = Factories.sc.core.createEntityItemStringType();
		if (s.toUpperCase().indexOf("(NONE)") != -1) {
		    data.signature.setStatus(StatusEnumeration.DOES_NOT_EXIST);
		} else if (s.indexOf("Key ID") == -1) {
		    data.signature.setStatus(StatusEnumeration.ERROR);
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_RPMINFO_SIGKEY, s));
		    data.addMessage(msg);
		} else {
		    data.signature.setValue(s.substring(s.indexOf("Key ID")+7).trim());
		}
	    } else if (line.startsWith("/")) {
		data.addFilepath(line);
	    }
	}
	return data;
    }

    class RpminfoItemData {
	EntityItemStringType name, arch, extendedName;
	RpminfoItem.Version version;
	RpminfoItem.Release release;
	RpminfoItem.Epoch epoch;
	EntityItemEVRStringType evr;
	EntityItemStringType signature;
	List<EntityItemStringType> filepaths;
	List<MessageType> messages;

	RpminfoItemData() {
	    filepaths = new ArrayList<EntityItemStringType>();
	}

	RpminfoItem toItem(RpmInfoBehaviors behaviors) {
	    RpminfoItem item = Factories.sc.linux.createRpminfoItem();
	    item.setName(name);
	    item.setArch(arch);
	    item.setRpmVersion(version);
	    item.setRelease(release);
	    item.setEpoch(epoch);
	    item.setEvr(evr);
	    item.setExtendedName(extendedName);
	    item.setSignatureKeyid(signature);
	    if (behaviors != null && behaviors.getFilepaths()) {
		item.getFilepath().addAll(filepaths);
	    }
	    if (messages != null) {
		item.getMessage().addAll(messages);
	    }
	    return item;
	}

	void addFilepath(String path) {
	    EntityItemStringType filepath = Factories.sc.core.createEntityItemStringType();
	    filepath.setValue(path);
	    filepaths.add(filepath);
	}

	void addMessage(MessageType msg) {
	    if (messages == null) {
		messages = new ArrayList<MessageType>();
	    }
	    messages.add(msg);
	}
    }
}
