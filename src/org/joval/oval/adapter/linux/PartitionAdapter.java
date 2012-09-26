// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.linux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.linux.PartitionObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.linux.PartitionItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.StringTools;

/**
 * Retrieves linux:partition items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PartitionAdapter implements IAdapter {
    private IUnixSession session;
    private HashMap<String, PartitionItem> partitions;
    private CollectException error;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(PartitionObject.class);
	}
	return classes;
    }

    public Collection<PartitionItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	PartitionObject pObj = (PartitionObject)obj;
	Collection<PartitionItem> items = new ArrayList<PartitionItem>();
	try {
	    String mountPoint = (String)pObj.getMountPoint().getValue();
	    OperationEnumeration op = pObj.getMountPoint().getOperation();
	    Pattern p = null;
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(mountPoint);
	    }
	    for (String mount : partitions.keySet()) {
		switch(op) {
		  case EQUALS:
		    if (mount.equals(mountPoint)) {
			items.add(partitions.get(mount));
		    }
		    break;
		  case CASE_INSENSITIVE_EQUALS:
		    if (mount.equalsIgnoreCase(mountPoint)) {
			items.add(partitions.get(mount));
		    }
		    break;
		  case NOT_EQUAL:
		    if (!mount.equals(mountPoint)) {
			items.add(partitions.get(mount));
		    }
		    break;
		  case PATTERN_MATCH:
		    if (p.matcher(mount).find()) {
			items.add(partitions.get(mount));
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    /**
     * Idempotent
     */
    private void init() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (partitions != null) {
	    return;
	}

	partitions = new HashMap<String, PartitionItem>();

	try {
	Map<String, Device> devices = getBlockDevices();
	    for (Mount mount : getMounts()) {
		if (devices.containsKey(mount.deviceName)) {
		    Device dev = devices.get(mount.deviceName);
		    Space space = new Space(mount.mountPoint);
		    PartitionItem item = Factories.sc.linux.createPartitionItem();

		    EntityItemStringType device = Factories.sc.core.createEntityItemStringType();
		    device.setValue(dev.name);
		    item.setDevice(device);

		    EntityItemStringType fsType = Factories.sc.core.createEntityItemStringType();
		    fsType.setValue(dev.type);
		    item.setFsType(fsType);

		    for (String mountOption : mount.options) {
			EntityItemStringType option = Factories.sc.core.createEntityItemStringType();
			option.setValue(mountOption);
			item.getMountOptions().add(option);
		    }

		    EntityItemStringType mountPoint = Factories.sc.core.createEntityItemStringType();
		    mountPoint.setValue(mount.mountPoint);
		    item.setMountPoint(mountPoint);

		    EntityItemIntType spaceLeft = Factories.sc.core.createEntityItemIntType();
		    spaceLeft.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    spaceLeft.setValue(Integer.toString(space.available));
		    item.setSpaceLeft(spaceLeft);

		    EntityItemIntType spaceUsed = Factories.sc.core.createEntityItemIntType();
		    spaceUsed.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    spaceUsed.setValue(Integer.toString(space.used));
		    item.setSpaceUsed(spaceUsed);

		    EntityItemIntType totalSpace = Factories.sc.core.createEntityItemIntType();
		    totalSpace.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    totalSpace.setValue(Integer.toString(space.capacity));
		    item.setTotalSpace(totalSpace);

		    EntityItemStringType uuid = Factories.sc.core.createEntityItemStringType();
		    uuid.setValue(dev.uuid);
		    item.setUuid(uuid);
	    
		    partitions.put(mount.mountPoint, item);
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    error = new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    throw error;
	}
    }

    /**
     * List all the active mounts on the machine.
     */
    List<Mount> getMounts() throws Exception {
	List<Mount> mounts = new ArrayList<Mount>();
	for (String line : SafeCLI.multiLine("/bin/mount", session, IUnixSession.Timeout.S)) {
	    if (line.length() > 0) {
		try {
		    mounts.add(new Mount(line));
		} catch (IllegalArgumentException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_LINUX_PARTITION, "Mount", e.getMessage());
		}
	    }
	}
	return mounts;
    }

    /**
     * Get the block devices on the machine, indexed by name.
     */
    Map<String, Device> getBlockDevices() throws Exception {
	Map<String, Device> devices = new HashMap<String, Device>();
	for (String line : SafeCLI.multiLine("/sbin/blkid", session, IUnixSession.Timeout.M)) {
	    if (line.length() > 0) {
		try {
		    Device device = new Device(line);
		    devices.put(device.name, device);
		} catch (IllegalArgumentException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_LINUX_PARTITION, "Device", e.getMessage());
		}
	    }
	}
	return devices;
    }

    class Device {
	String name;
	String uuid;
	String type;
	String label;

	Device(String line) throws IllegalArgumentException {
	    int ptr = line.indexOf(":");
	    if (ptr == -1) {
		throw new IllegalArgumentException(line);
	    } else {
		name = line.substring(0,ptr);
		StringTokenizer tok = new StringTokenizer(line.substring(ptr+1));
		while (tok.hasMoreTokens()) {
		    String token = tok.nextToken();
		    ptr = token.indexOf("=");
		    String attr = token.substring(0,ptr).trim();
		    String value = token.substring(ptr+1).trim();
		    if (value.startsWith("\"") && value.endsWith("\"")) {
			value = value.substring(1, value.length()-1);
		    }
		    if ("LABEL".equalsIgnoreCase(attr)) {
			label = value;
		    } else if ("UUID".equalsIgnoreCase(attr)) {
			uuid = value;
		    } else if ("TYPE".equalsIgnoreCase(attr)) {
			type = value;
		    }
		}
	    }
	}
    }

    class Mount {
	String deviceName;
	String mountPoint;
	String type;
	List<String> options;

	Mount(String line) throws IllegalArgumentException {
	    StringTokenizer tok = new StringTokenizer(line);
	    if (tok.countTokens() >= 6) {
		deviceName = tok.nextToken();
		tok.nextToken(); // on
		mountPoint = tok.nextToken();
		tok.nextToken(); // type
		type = tok.nextToken();
		String optionText = tok.nextToken();
		if (optionText.startsWith("(") && optionText.endsWith(")")) {
		    optionText = optionText.substring(1, optionText.length()-1);
		}
		options = StringTools.toList(optionText.split(","));
	    } else {
		throw new IllegalArgumentException(line);
	    }
	}
    }

    class Space {
	int available, used, capacity;

	Space(String mountPoint) throws Exception {
	    int lineNum = 0;
	    for (String line : SafeCLI.multiLine("/bin/df --direct -P " + mountPoint, session, IUnixSession.Timeout.S)) {
		line = line.trim();
		if (line.length() > 0 && lineNum++ > 0) {
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() >= 4) {
			tok.nextToken(); // mount point
			capacity = Integer.parseInt(tok.nextToken());
			used = Integer.parseInt(tok.nextToken());
			available = Integer.parseInt(tok.nextToken());
		    }
		    break;
		}
	    }
	}
    }
}
