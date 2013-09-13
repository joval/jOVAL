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
public class RpminfoAdapter extends BaseRpmAdapter implements IBatch {
    @Override
    Class getObjectClass() {
	return RpminfoObject.class;
    }

    // Implement IAdapter

    public Collection<RpminfoItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	RpminfoObject rObj = (RpminfoObject)obj;
	Collection<RpminfoItem> items = new ArrayList<RpminfoItem>();
	switch(rObj.getName().getOperation()) {
	  case EQUALS:
	    try {
		for (RpmData data : getRpmData((String)rObj.getName().getValue())) {
		    items.add(toItem(data, rObj.getBehaviors()));
		}
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
	    loadPackageMap();
	    try {
		Pattern p = StringTools.pattern((String)rObj.getName().getValue());
		for (String packageName : packageMap.keySet()) {
		    if (p.matcher(packageName).find()) {
			items.add(toItem(packageMap.get(packageName), rObj.getBehaviors()));
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
	    loadPackageMap();
	    String name = (String)rObj.getName().getValue();
	    for (String packageName : packageMap.keySet()) {
		if (!packageName.equals(name)) {
		    items.add(toItem(packageMap.get(packageName), rObj.getBehaviors()));
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
		//
		// If the package map is not loaded, then retrieve the required subset of its contents in batch.
		//
		if (!packageMapLoaded) {
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
			RpmData data = null;
			Iterator<String> iter = SafeCLI.manyLines(cmd.toString(), null, session);
			while ((data = nextRpmData(iter)) != null) {
			    packageMap.put(data.name, data);
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
			result.add(toItem(packageMap.get(name), rObj.getBehaviors()));
		    } else {
			//
			// Look for a "short name" match
			//
			for (Map.Entry<String, RpmData> entry : packageMap.entrySet()) {
			    if (entry.getKey().startsWith(name + "-")) {
				result.add(toItem(entry.getValue(), rObj.getBehaviors()));
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

    /**
     * Fully populate packageList if it's not already populated. Idempotent.
     */
    private void initPackageList() throws Exception {
	if (!packageMapLoaded) {
	    session.getLogger().info(JOVALMsg.STATUS_RPMINFO_LIST);
	    packageList = new HashSet<String>();
	    for (String name : SafeCLI.multiLine("rpm -qa", session, IUnixSession.Timeout.M)) {
		packageList.add(name);
	    }
	}
    }

    /**
     * Convert an RpmData to an RpminfoItem.
     */
    private RpminfoItem toItem(RpmData data, RpmInfoBehaviors behaviors) {
	RpminfoItem item = Factories.sc.linux.createRpminfoItem();

	EntityItemStringType name = Factories.sc.core.createEntityItemStringType();
	name.setValue(data.name);
	item.setName(name);

	EntityItemStringType arch = Factories.sc.core.createEntityItemStringType();
	arch.setValue(data.arch);
	item.setArch(arch);

	RpminfoItem.Version version = Factories.sc.linux.createRpminfoItemVersion();
	version.setValue(data.version);
	item.setRpmVersion(version);

	RpminfoItem.Release release = Factories.sc.linux.createRpminfoItemRelease();
	release.setValue(data.release);
	item.setRelease(release);

	RpminfoItem.Epoch epoch = Factories.sc.linux.createRpminfoItemEpoch();
	epoch.setValue(data.epoch);
	item.setEpoch(epoch);

	EntityItemEVRStringType evr = Factories.sc.core.createEntityItemEVRStringType();
	evr.setValue(data.evr);
	evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
	item.setEvr(evr);

	EntityItemStringType extendedName = Factories.sc.core.createEntityItemStringType();
	extendedName.setValue(data.extendedName);
	item.setExtendedName(extendedName);

	EntityItemStringType signature = Factories.sc.core.createEntityItemStringType();
	if (data.sigError) {
	    signature.setStatus(StatusEnumeration.ERROR);
	} else if (data.signature == null) {
	    signature.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	} else {
	    signature.setValue(data.signature);
	}
	item.setSignatureKeyid(signature);

	if (behaviors != null && behaviors.getFilepaths()) {
	    for (String path : data.filepaths) {
		EntityItemStringType filepath = Factories.sc.core.createEntityItemStringType();
		filepath.setValue(path);
		item.getFilepath().add(filepath);
	    }
	}

	if (data.messages != null) {
	    item.getMessage().addAll(data.messages);
	}

	return item;
    }
}
