// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import scap.oval.definitions.core.EntityObjectAnySimpleType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.linux.RpmInfoBehaviors;
import scap.oval.definitions.linux.RpminfoObject;
import scap.oval.definitions.linux.RpmVerifyBehaviors;
import scap.oval.definitions.linux.RpmVerifyFileBehaviors;
import scap.oval.definitions.linux.RpmVerifyPackageBehaviors;
import scap.oval.definitions.linux.RpmverifyObject;
import scap.oval.definitions.linux.RpmverifyfileObject;
import scap.oval.definitions.linux.RpmverifypackageObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.EntityItemEVRStringType;
import scap.oval.systemcharacteristics.linux.EntityItemRpmVerifyResultType;
import scap.oval.systemcharacteristics.linux.RpminfoItem;
import scap.oval.systemcharacteristics.linux.RpmverifyItem;
import scap.oval.systemcharacteristics.linux.RpmverifyfileItem;
import scap.oval.systemcharacteristics.linux.RpmverifypackageItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.scap.oval.IBatch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.Batch;
import org.joval.scap.oval.types.VersionType;
import org.joval.util.JOVALMsg;

/**
 * Adapter for RPM-related objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RpmAdapter implements IAdapter, IBatch {
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

    private IUnixSession session;
    private Collection<String> packageList;
    private Map<String, RpmData> packageMap;
    private boolean packageMapLoaded = false;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    switch(this.session.getFlavor()) {
	      case AIX:
	      case LINUX:
		packageMap = new HashMap<String, RpmData>();
		classes.add(RpminfoObject.class);
		classes.add(RpmverifyObject.class);
		classes.add(RpmverifyfileObject.class);
		classes.add(RpmverifypackageObject.class);
		break;
	    }
	}
	if (classes.size() == 0) {
	    notapplicable.add(RpminfoObject.class);
	    notapplicable.add(RpmverifyObject.class);
	    notapplicable.add(RpmverifyfileObject.class);
	    notapplicable.add(RpmverifypackageObject.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	try {
	    if (obj instanceof RpminfoObject) {
		return getRpminfoItems((RpminfoObject)obj, rc);
	    } else if (obj instanceof RpmverifyObject) {
		return getRpmverifyItems((RpmverifyObject)obj, rc);
	    } else if (obj instanceof RpmverifyfileObject) {
		return getRpmverifyfileItems((RpmverifyfileObject)obj, rc);
	    } else if (obj instanceof RpmverifypackageObject) {
		return getRpmverifypackageItems((RpmverifypackageObject)obj, rc);
	    } else {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OBJECT, obj.getClass().getName(), obj.getId());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    // Implement IBatch

    private Collection<IRequest> queue;

    /**
     * Only batch up RpminfoObjects with EQUALS name entities. Anything else will result in a full load of the RPM database,
     * which obviates the efficiency need to perform batch collection.
     *
     * The other object types could benefit from batching, as they run subcommands, but their implementation is TBD.
     */
    public boolean queue(IRequest request) {
	boolean batchable = false;
	ObjectType obj = request.getObject();
	if (obj instanceof RpminfoObject) {
	    RpminfoObject rObj = (RpminfoObject)request.getObject();
	    batchable = rObj.isSetName() && rObj.getName().getOperation() == OperationEnumeration.EQUALS;
	}
	if (batchable) {
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
			    String prefix = new StringBuffer(name).append("-").toString();
			    for (String packageName : packageList) {
				if (!packageMap.containsKey(packageName) && packageName.startsWith(prefix)) {
				    check = true;
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
		    // Gather information about packages in a single pass, and add it all to the package map.
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
			result.add(toRpminfoItem(packageMap.get(name), rObj.getBehaviors()));
		    } else {
			//
			// Look for a "short name" match
			//
			for (Map.Entry<String, RpmData> entry : packageMap.entrySet()) {
			    if (matches(name, entry.getValue())) {
				result.add(toRpminfoItem(entry.getValue(), rObj.getBehaviors()));
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

    private Collection<RpminfoItem> getRpminfoItems(RpminfoObject rObj, IRequestContext rc) throws Exception {
	Collection<RpminfoItem> items = new ArrayList<RpminfoItem>();
	for (RpmData datum : getRpmData(rObj.getName())) {
	    items.add(toRpminfoItem(datum, rObj.getBehaviors()));
	}
	return items;
    }

    private Collection<RpmverifyItem> getRpmverifyItems(RpmverifyObject rObj, IRequestContext rc) throws Exception {
	Collection<RpmverifyItem> items = new ArrayList<RpmverifyItem>();
	for (Map.Entry<RpmData, Collection<String>> entry :
	     filterFiles(getRpmData(rObj.getName()), rObj.getFilepath()).entrySet()) {

	    try {
		items.addAll(getRpmverifyItems(entry.getKey().name, entry.getValue(), rObj.getBehaviors(), rc));
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    private Collection<RpmverifyfileItem> getRpmverifyfileItems(RpmverifyfileObject rObj, IRequestContext rc)
		throws Exception {

	Collection<RpmData> data = getRpmData(rObj.getName());
	filterRpms(data, rObj.getArch(), rObj.getEpoch(), rObj.getRpmVersion(), rObj.getRelease());
	Collection<RpmverifyfileItem> items = new ArrayList<RpmverifyfileItem>();
	RpmVerifyBehaviors behaviors = toRpmVerifyBehaviors(rObj.getBehaviors());
	for (Map.Entry<RpmData, Collection<String>> entry : filterFiles(data, rObj.getFilepath()).entrySet()) {
	    try {
		for (RpmverifyItem vItem : getRpmverifyItems(entry.getKey().name, entry.getValue(), behaviors, rc)) {
		    //
		    // Copy entities from the very similar RpmverifyItem
		    //
		    RpmverifyfileItem item = Factories.sc.linux.createRpmverifyfileItem();
		    item.setName(vItem.getName());
		    item.setFilepath(vItem.getFilepath());
		    item.setSizeDiffers(vItem.getSizeDiffers());
		    item.setModeDiffers(vItem.getModeDiffers());
		    item.setMd5Differs(vItem.getMd5Differs());
		    item.setDeviceDiffers(vItem.getDeviceDiffers());
		    item.setLinkMismatch(vItem.getLinkMismatch());
		    item.setOwnershipDiffers(vItem.getOwnershipDiffers());
		    item.setGroupDiffers(vItem.getGroupDiffers());
		    item.setMtimeDiffers(vItem.getMtimeDiffers());
		    item.setCapabilitiesDiffer(vItem.getCapabilitiesDiffer());
		    item.setConfigurationFile(vItem.getConfigurationFile());
		    item.setDocumentationFile(vItem.getDocumentationFile());
		    item.setGhostFile(vItem.getGhostFile());
		    item.setLicenseFile(vItem.getLicenseFile());
		    item.setReadmeFile(vItem.getReadmeFile());

		    EntityItemStringType extendedName = Factories.sc.core.createEntityItemStringType();
		    extendedName.setValue(entry.getKey().extendedName);
		    item.setExtendedName(extendedName);

		    EntityItemStringType arch = Factories.sc.core.createEntityItemStringType();
		    arch.setValue(entry.getKey().arch);
		    item.setArch(arch);

		    RpmverifyfileItem.Epoch epoch = Factories.sc.linux.createRpmverifyfileItemEpoch();
		    epoch.setValue(entry.getKey().epoch);
		    item.setEpoch(epoch);

		    RpmverifyfileItem.Version version = Factories.sc.linux.createRpmverifyfileItemVersion();
		    version.setValue(entry.getKey().version);
		    item.setRpmVersion(version);

		    RpmverifyfileItem.Release release = Factories.sc.linux.createRpmverifyfileItemRelease();
		    release.setValue(entry.getKey().release);
		    item.setRelease(release);

		    items.add(item);
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    private Collection<RpmverifypackageItem> getRpmverifypackageItems(RpmverifypackageObject rObj, IRequestContext rc)
		throws Exception {

	Collection<RpmData> data = getRpmData(rObj.getName());
	filterRpms(data, rObj.getArch(), rObj.getEpoch(), rObj.getRpmVersion(), rObj.getRelease());
	Collection<RpmverifypackageItem> items = new ArrayList<RpmverifypackageItem>();
	for (RpmData datum : data) {
	    try {
		items.add(getRpmverifypackageItem(datum, rObj.getBehaviors()));
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
	    }
	}
	return items;
    }

    /**
     * Get a list of RpmData matching the specified "name" entity.
     */
    private Collection<RpmData> getRpmData(EntityObjectStringType nameType) throws Exception {
	Collection<RpmData> data = new ArrayList<RpmData>();
	switch(nameType.getOperation()) {
	  case EQUALS:
	    data.addAll(getRpmData((String)nameType.getValue()));
	    break;

	  case PATTERN_MATCH:
	    loadPackageMap();
	    Pattern p = StringTools.pattern((String)nameType.getValue());
	    for (RpmData datum : packageMap.values()) {
		if (p.matcher(datum.name).find()) {
		    data.add(datum);
		}
	    }
	    break;

	  case NOT_EQUAL:
	    loadPackageMap();
	    String name = (String)nameType.getValue();
	    for (RpmData datum : packageMap.values()) {
		if (!datum.name.equals(name)) {
		    data.add(datum);
		}
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, nameType.getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return data;
    }

    /**
     * Filter down the data according to the specified arch, epoch, version, and release.
     */
    private void filterRpms(Collection<RpmData> data, EntityObjectStringType arch, EntityObjectAnySimpleType epoch,
		EntityObjectAnySimpleType version, EntityObjectAnySimpleType release) throws CollectException {

	Iterator<RpmData> iter = data.iterator();
	while(iter.hasNext()) {
	    RpmData datum = iter.next();
	    switch(arch.getOperation()) {
	      case EQUALS:
		if (!((String)arch.getValue()).equals(datum.arch)) {
		    iter.remove();
		}
		break;
	      case NOT_EQUAL:
		if (((String)arch.getValue()).equals(datum.arch)) {
		    iter.remove();
		}
		break;
	      case PATTERN_MATCH:
		Pattern p = Pattern.compile((String)arch.getValue());
		if (!p.matcher(datum.arch).find()) {
		    iter.remove();
		}
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, arch.getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	iter = data.iterator();
	while(iter.hasNext()) {
	    RpmData datum = iter.next();
	    switch(SimpleDatatypeEnumeration.fromValue(epoch.getDatatype())) {
	      case INT: {
		int oe = Integer.parseInt((String)epoch.getValue());
		int de = Integer.parseInt(datum.epoch);
		switch(epoch.getOperation()) {
		  case EQUALS:
		    if (oe != de) {
			iter.remove();
		    }
		    break;
		  case NOT_EQUAL:
		    if (oe == de) {
			iter.remove();
		    }
		    break;
		  case GREATER_THAN:
		    if (oe >= de) {
			iter.remove();
		    }
		    break;
		  case GREATER_THAN_OR_EQUAL:
		    if (oe > de) {
			iter.remove();
		    }
		    break;
		  case LESS_THAN:
		    if (oe <= de) {
			iter.remove();
		    }
		    break;
		  case LESS_THAN_OR_EQUAL:
		    if (oe < de) {
			iter.remove();
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, epoch.getOperation());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
		break;
	      }
	      default:
		switch(epoch.getOperation()) {
		  case EQUALS:
		    if (!((String)epoch.getValue()).equals(datum.epoch)) {
			iter.remove();
		    }
		    break;
		  case NOT_EQUAL:
		    if (((String)epoch.getValue()).equals(datum.epoch)) {
			iter.remove();
		    }
		    break;
		  case PATTERN_MATCH:
		    Pattern p = Pattern.compile((String)epoch.getValue());
		    if (!p.matcher(datum.epoch).find()) {
			iter.remove();
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, epoch.getOperation());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
		break;
	    }
	}
	iter = data.iterator();
	while(iter.hasNext()) {
	    RpmData datum = iter.next();
	    switch(SimpleDatatypeEnumeration.fromValue(version.getDatatype())) {
	      case VERSION: {
		VersionType ov = new VersionType((String)version.getValue());
		VersionType dv = new VersionType(datum.version);
		switch(epoch.getOperation()) {
		  case EQUALS:
		    if (ov.compareTo(dv) != 0) {
			iter.remove();
		    }
		    break;
		  case NOT_EQUAL:
		    if (ov.compareTo(dv) == 0) {
			iter.remove();
		    }
		    break;
		  case GREATER_THAN:
		    if (ov.compareTo(dv) >= 0) {
			iter.remove();
		    }
		    break;
		  case GREATER_THAN_OR_EQUAL:
		    if (ov.compareTo(dv) > 0) {
			iter.remove();
		    }
		    break;
		  case LESS_THAN:
		    if (ov.compareTo(dv) <= 0) {
			iter.remove();
		    }
		    break;
		  case LESS_THAN_OR_EQUAL:
		    if (ov.compareTo(dv) < 0) {
			iter.remove();
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, version.getOperation());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
		break;
	      }
	      default:
		switch(version.getOperation()) {
		  case EQUALS:
		    if (!((String)version.getValue()).equals(datum.version)) {
			iter.remove();
		    }
		    break;
		  case NOT_EQUAL:
		    if (((String)version.getValue()).equals(datum.version)) {
			iter.remove();
		    }
		    break;
		  case PATTERN_MATCH:
		    Pattern p = Pattern.compile((String)version.getValue());
		    if (!p.matcher(datum.version).find()) {
			iter.remove();
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, version.getOperation());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	}
	iter = data.iterator();
	while(iter.hasNext()) {
	    RpmData datum = iter.next();
	    switch(release.getOperation()) {
	      case EQUALS:
		if (!((String)release.getValue()).equals(datum.release)) {
		    iter.remove();
		}
		break;
	      case NOT_EQUAL:
		if (((String)release.getValue()).equals(datum.release)) {
		    iter.remove();
		}
		break;
	      case PATTERN_MATCH:
		Pattern p = Pattern.compile((String)release.getValue());
		if (!p.matcher(datum.release).find()) {
		    iter.remove();
		}
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, release.getOperation());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
    }

    /**
     * Filter the specified RPM list down to only those that include files matching the specified filepath, and return
     * a map of those file matches.
     */
    private Map<RpmData, Collection<String>> filterFiles(Collection<RpmData> data, EntityObjectStringType filepathType)
		throws CollectException {

	Map<RpmData, Collection<String>> filepaths = new HashMap<RpmData, Collection<String>>();
	Iterator<RpmData> iter = data.iterator();
	switch(filepathType.getOperation()) {
	  case EQUALS:
	    while(iter.hasNext()) {
		RpmData datum = iter.next();
		if (datum.filepaths.contains((String)filepathType.getValue())) {
		    filepaths.put(datum, Arrays.asList((String)filepathType.getValue()));
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    Pattern p = StringTools.pattern((String)filepathType.getValue());
	    while(iter.hasNext()) {
		RpmData datum = iter.next();
		for (String filepath : datum.filepaths) {
		    if (p.matcher(filepath).find()) {
			if (!filepaths.containsKey(datum)) {
			    filepaths.put(datum, new ArrayList<String>());
			}
			filepaths.get(datum).add(filepath);
		    }
		}
	    }
	    break;

	  case NOT_EQUAL:
	    while(iter.hasNext()) {
		RpmData datum = iter.next();
		for (String filepath : datum.filepaths) {
		    if (!filepath.equals((String)filepathType.getValue())) {
			if (!filepaths.containsKey(datum)) {
			    filepaths.put(datum, new ArrayList<String>());
			}
			filepaths.get(datum).add(filepath);
		    }
		}
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, filepathType.getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return filepaths;
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
     * Load comprehensive information about every package on the system. Idempotent.
     */
    private void loadPackageMap() {
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
		packageList.add(data.extendedName);
		packageMap.put(data.extendedName, data);
	    }
	    packageMapLoaded = true;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Get RpmData matching a single package name (potentially a "short name", which can lead to multiple results).
     */
    private Collection<RpmData> getRpmData(String packageName) throws Exception {
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
		if (matches(packageName, entry.getValue())) {
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
		packageMap.put(data.extendedName, data);
		result.add(data);
	    }
	}
	return result;
    }

    /**
     * Get the RPM query command to which an RPM name is appended, that returns output suitable for nextRpmData().
     */
    private String getBaseCommand() {
	switch(session.getFlavor()) {
	  case AIX:
	    return CMD_AIX;
	  default:
	    return CMD_LINUX;
	}
    }

    /**
     * A class for storing information about an RPM.
     */
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

    /**
     * Read the next RpmData from command output.
     */
    private RpmData nextRpmData(Iterator<String> lines) {
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

    /**
     * Determine whether an RPM short name matches the specified RPM (given its RpmData).
     */
    private boolean matches(String shortName, RpmData datum) {
	if (shortName.equals(datum.name)) {
	    return true;
	}
	if (datum.epoch.equals("0")) {
	    if (shortName.equals(datum.name + "-" + datum.version)) {
		return true;
	    }
	    if (shortName.equals(datum.name + "-" + datum.version + "-" + datum.release)) {
		return true;
	    }
	} else {
	    if (shortName.equals(datum.name + "-" + datum.epoch)) {
		return true;
	    }
	    if (shortName.equals(datum.name + "-" + datum.epoch + "-" + datum.version)) {
		return true;
	    }
	    if (shortName.equals(datum.name + "-" + datum.epoch + "-" + datum.version + "-" + datum.release)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Convert an RpmData to an RpminfoItem.
     */
    private RpminfoItem toRpminfoItem(RpmData datum, RpmInfoBehaviors behaviors) {
	RpminfoItem item = Factories.sc.linux.createRpminfoItem();

	EntityItemStringType name = Factories.sc.core.createEntityItemStringType();
	name.setValue(datum.name);
	item.setName(name);

	EntityItemStringType arch = Factories.sc.core.createEntityItemStringType();
	arch.setValue(datum.arch);
	item.setArch(arch);

	RpminfoItem.Version version = Factories.sc.linux.createRpminfoItemVersion();
	version.setValue(datum.version);
	item.setRpmVersion(version);

	RpminfoItem.Release release = Factories.sc.linux.createRpminfoItemRelease();
	release.setValue(datum.release);
	item.setRelease(release);

	RpminfoItem.Epoch epoch = Factories.sc.linux.createRpminfoItemEpoch();
	epoch.setValue(datum.epoch);
	item.setEpoch(epoch);

	EntityItemEVRStringType evr = Factories.sc.core.createEntityItemEVRStringType();
	evr.setValue(datum.evr);
	evr.setDatatype(SimpleDatatypeEnumeration.EVR_STRING.value());
	item.setEvr(evr);

	EntityItemStringType extendedName = Factories.sc.core.createEntityItemStringType();
	extendedName.setValue(datum.extendedName);
	item.setExtendedName(extendedName);

	EntityItemStringType signature = Factories.sc.core.createEntityItemStringType();
	if (datum.sigError) {
	    signature.setStatus(StatusEnumeration.ERROR);
	} else if (datum.signature == null) {
	    signature.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	} else {
	    signature.setValue(datum.signature);
	}
	item.setSignatureKeyid(signature);

	if (behaviors != null && behaviors.getFilepaths()) {
	    for (String path : datum.filepaths) {
		EntityItemStringType filepath = Factories.sc.core.createEntityItemStringType();
		filepath.setValue(path);
		item.getFilepath().add(filepath);
	    }
	}

	if (datum.messages != null) {
	    item.getMessage().addAll(datum.messages);
	}

	return item;
    }

    /**
     * Allowed values for the EntityItemRpmVerifyResultType.
     */
    enum RpmVerifyResultEnum {
	PASS("pass"),
	FAIL("fail"),
	NP("not performed");

	private String value;

	private RpmVerifyResultEnum(String value) {
	    this.value = value;
	}

	String value() {
	    return value;
	}
    }

    private RpmVerifyBehaviors toRpmVerifyBehaviors(RpmVerifyFileBehaviors rvfBehaviors) {
	RpmVerifyBehaviors result = Factories.definitions.linux.createRpmVerifyBehaviors();
	result.setNoconfigfiles(rvfBehaviors.getNoconfigfiles());
	result.setNoghostfiles(rvfBehaviors.getNoghostfiles());
	result.setNogroup(rvfBehaviors.getNogroup());
	result.setNolinkto(rvfBehaviors.getNolinkto());
	result.setNomd5(rvfBehaviors.getNomd5());
	result.setNomode(rvfBehaviors.getNomode());
	result.setNomtime(rvfBehaviors.getNomtime());
	result.setNordev(rvfBehaviors.getNordev());
	result.setNosize(rvfBehaviors.getNosize());
	result.setNouser(rvfBehaviors.getNouser());
	return result;
    }

    /**
     * NOTE: In some versions of RPM, the "not performed" check may be broken because of the following defect in RPM:
     *   https://bugzilla.redhat.com/show_bug.cgi?id=803765
     */
    private Collection<RpmverifyItem> getRpmverifyItems(String packageName, Collection<String> filepaths,
		RpmVerifyBehaviors behaviors, IRequestContext rc) throws Exception {

	StringBuffer cmd = new StringBuffer("rpm -V ");
	cmd.append("'").append(packageName).append("'");
	if (behaviors == null) {
	    behaviors = Factories.definitions.linux.createRpmVerifyBehaviors();
	}
	if (behaviors.getNodeps()) {
	    cmd.append(" --nodeps");
	}
	if (behaviors.getNodigest()) {
	    cmd.append(" --nodigest");
	}
	if (behaviors.getNogroup()) {
	    cmd.append(" --nogroup");
	}
	if (behaviors.getNolinkto()) {
	    cmd.append(" --nolinkto");
	}
	if (behaviors.getNomd5()) {
	    cmd.append(" --nofiledigest");
	}
	if (behaviors.getNomode()) {
	    cmd.append(" --nomode");
	}
	if (behaviors.getNomtime()) {
	    cmd.append(" --nomtime");
	}
	if (behaviors.getNordev()) {
	    cmd.append(" --nordev");
	}
	if (behaviors.getNoscripts()) {
	    cmd.append(" --noscripts");
	}
	if (behaviors.getNosignature()) {
	    cmd.append(" --nosignature");
	}
	if (behaviors.getNosize()) {
	    cmd.append(" --nosize");
	}
	if (behaviors.getNouser()) {
	    cmd.append(" --nouser");
	}
	cmd.append(" -v");

	Collection<RpmverifyItem> items = new ArrayList<RpmverifyItem>();
	for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
	    int ptr = line.indexOf("/");
	    if (ptr == -1) {
		if (line.trim().length() > 0) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.WARNING);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.WARNING_RPMVERIFY_LINE, line));
		    rc.addMessage(msg);
		}
		continue;
	    }
	    String filepath = line.substring(ptr);

	    if (filepaths.contains(filepath)) {
		String type = line.substring(9, ptr).trim();
		RpmverifyItem item = Factories.sc.linux.createRpmverifyItem();

		EntityItemStringType filepathType = Factories.sc.core.createEntityItemStringType();
		filepathType.setValue(filepath);
		item.setFilepath(filepathType);

		EntityItemStringType name = Factories.sc.core.createEntityItemStringType();
		name.setValue(packageName);
		item.setName(name);

		EntityItemBoolType config = Factories.sc.core.createEntityItemBoolType();
		config.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		config.setValue("0");
		item.setConfigurationFile(config);

		EntityItemBoolType doc = Factories.sc.core.createEntityItemBoolType();
		doc.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		doc.setValue("0");
		item.setDocumentationFile(doc);

		EntityItemBoolType ghost = Factories.sc.core.createEntityItemBoolType();
		ghost.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		ghost.setValue("0");
		item.setGhostFile(ghost);

		EntityItemBoolType license = Factories.sc.core.createEntityItemBoolType();
		license.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		license.setValue("0");
		item.setLicenseFile(license);

		EntityItemBoolType readme = Factories.sc.core.createEntityItemBoolType();
		readme.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		readme.setValue("0");
		item.setReadmeFile(license);

		if (type.length() > 0) {
		    if ("c".equals(type)) {
			if (behaviors.getNoconfigfiles()) {
			    continue;
			} else {
			    config.setValue("1");
			}
		    } else if ("d".equals(type)) {
			doc.setValue("1");
		    } else if ("g".equals(type)) {
			if (behaviors.getNoghostfiles()) {
			    continue;
			} else {
			    ghost.setValue("1");
			}
		    } else if ("l".equals(type)) {
			license.setValue("1");
		    } else if ("r".equals(type)) {
			readme.setValue("1");
		    }
		}

		String tests = line.substring(0, 9);

		EntityItemRpmVerifyResultType sizeDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("S") != -1) {
		    sizeDiffers.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(0) == '?' || behaviors.getNosize()) {
		    sizeDiffers.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    sizeDiffers.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setSizeDiffers(sizeDiffers);

		EntityItemRpmVerifyResultType modeDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("M") != -1) {
		    modeDiffers.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(1) == '?' || behaviors.getNomode()) {
		    modeDiffers.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    modeDiffers.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setModeDiffers(modeDiffers);

		EntityItemRpmVerifyResultType md5Differs = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("5") != -1) {
		    md5Differs.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(2) == '?' || behaviors.getNomd5()) {
		    md5Differs.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    md5Differs.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setMd5Differs(md5Differs);

		EntityItemRpmVerifyResultType deviceDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("D") != -1) {
		    deviceDiffers.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(3) == '?' || behaviors.getNordev()) {
		    deviceDiffers.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    deviceDiffers.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setDeviceDiffers(deviceDiffers);

		EntityItemRpmVerifyResultType linkMismatch = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("L") != -1) {
		    linkMismatch.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(4) == '?' || behaviors.getNolinkto()) {
		    linkMismatch.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    linkMismatch.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setLinkMismatch(linkMismatch);

		EntityItemRpmVerifyResultType ownershipDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("U") != -1) {
		    ownershipDiffers.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(5) == '?' || behaviors.getNouser()) {
		    ownershipDiffers.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    ownershipDiffers.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setOwnershipDiffers(ownershipDiffers);

		EntityItemRpmVerifyResultType groupDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("G") != -1) {
		    groupDiffers.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(6) == '?' || behaviors.getNogroup()) {
		    groupDiffers.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    groupDiffers.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setGroupDiffers(groupDiffers);

		EntityItemRpmVerifyResultType mtimeDiffers = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("T") != -1) {
		    mtimeDiffers.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(7) == '?' || behaviors.getNomtime()) {
		    mtimeDiffers.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    mtimeDiffers.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setMtimeDiffers(mtimeDiffers);

		EntityItemRpmVerifyResultType capabilitiesDiffer = Factories.sc.linux.createEntityItemRpmVerifyResultType();
		if (tests.indexOf("P") != -1) {
		    capabilitiesDiffer.setValue(RpmVerifyResultEnum.FAIL.value());
		} else if (tests.charAt(8) == '?') {
		    capabilitiesDiffer.setValue(RpmVerifyResultEnum.NP.value());
		} else {
		    capabilitiesDiffer.setValue(RpmVerifyResultEnum.PASS.value());
		}
		item.setCapabilitiesDiffer(capabilitiesDiffer);

		items.add(item);
	    }
	}
	return items;
    }

    private RpmverifypackageItem getRpmverifypackageItem(RpmData datum, RpmVerifyPackageBehaviors behaviors) throws Exception {
	RpmverifypackageItem item = Factories.sc.linux.createRpmverifypackageItem();

	StringBuffer cmd = new StringBuffer("rpm -V ");
	cmd.append("'").append(datum.name).append("'");
	if (behaviors == null) {
	    behaviors = Factories.definitions.linux.createRpmVerifyPackageBehaviors();
	}
	if (behaviors.getNodeps()) {
	    cmd.append(" --nodeps");
	}
	if (behaviors.getNoscripts()) {
	    cmd.append(" --noscripts");
	}
	boolean deps=true, scripts=true;
	for (String line : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.M)) {
	    if (line.startsWith("Unsatisfied dependencies")) {
		deps = false;
	    } else if (line.trim().length() > 0) {
		// Any output means a failure
		scripts = false;
	    }
	}

	if (!behaviors.getNodeps()) {
	    EntityItemBoolType dependencyCheckPassed = Factories.sc.core.createEntityItemBoolType();
	    dependencyCheckPassed.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    dependencyCheckPassed.setValue(deps ? "1" : "0");
	    item.setDependencyCheckPassed(dependencyCheckPassed);
	}
	if (!behaviors.getNoscripts()) {
	    EntityItemBoolType verificationScriptSuccessful = Factories.sc.core.createEntityItemBoolType();
	    verificationScriptSuccessful.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    verificationScriptSuccessful.setValue(scripts ? "1" : "0");
	    item.setVerificationScriptSuccessful(verificationScriptSuccessful);
	}

	EntityItemStringType extendedName = Factories.sc.core.createEntityItemStringType();
	extendedName.setValue(datum.extendedName);
	item.setExtendedName(extendedName);

	EntityItemStringType arch = Factories.sc.core.createEntityItemStringType();
	arch.setValue(datum.arch);
	item.setArch(arch);

	RpmverifypackageItem.Epoch epoch = Factories.sc.linux.createRpmverifypackageItemEpoch();
	epoch.setValue(datum.epoch);
	item.setEpoch(epoch);

	RpmverifypackageItem.Version version = Factories.sc.linux.createRpmverifypackageItemVersion();
	version.setValue(datum.version);
	item.setRpmVersion(version);

	RpmverifypackageItem.Release release = Factories.sc.linux.createRpmverifypackageItemRelease();
	release.setValue(datum.release);
	item.setRelease(release);

	return item;
    }
}
