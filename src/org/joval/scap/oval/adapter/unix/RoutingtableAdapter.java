// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import jsaf.intf.io.IFile;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.RoutingtableObject;
import scap.oval.systemcharacteristics.core.EntityItemIPAddressType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.EntityItemRoutingTableFlagsType;
import scap.oval.systemcharacteristics.unix.RoutingtableItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.sysinfo.NetworkInterface;
import org.joval.util.JOVALMsg;

/**
 * Resolves Routingtable OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RoutingtableAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, Collection<RoutingtableItem>> routes;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    classes.add(RoutingtableObject.class);
	} else {
	    notapplicable.add(RoutingtableObject.class);
	}
	return classes;
    }

    public Collection<RoutingtableItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	try {
	    init();
	    RoutingtableObject iObj = (RoutingtableObject)obj;
	    String destination = (String)iObj.getDestination().getValue();
	    OperationEnumeration op = iObj.getDestination().getOperation();
	    Collection<RoutingtableItem> items = new ArrayList<RoutingtableItem>();
	    switch(op) {
	      case EQUALS:
		if (routes.containsKey(destination)) {
		    items.addAll(routes.get(destination));
		}
		break;
	      case CASE_INSENSITIVE_EQUALS:
		for (Map.Entry<String, Collection<RoutingtableItem>> entry : routes.entrySet()) {
		    if (entry.getKey().equalsIgnoreCase(destination)) {
			items.addAll(entry.getValue());
		    }
		}
		break;
	      case NOT_EQUAL:
		for (Map.Entry<String, Collection<RoutingtableItem>> entry : routes.entrySet()) {
		    if (!entry.getKey().equals(destination)) {
			items.addAll(entry.getValue());
		    }
		}
		break;
	      case PATTERN_MATCH:
		Pattern p = Pattern.compile(destination);
		for (Map.Entry<String, Collection<RoutingtableItem>> entry : routes.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.addAll(entry.getValue());
		    }
		}
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }

	    //
	    // Make sure only the proper type of route is returned.
	    //
	    String datatype = iObj.getDestination().getDatatype();
	    Iterator<RoutingtableItem> iter = items.iterator();
	    while(iter.hasNext()) {
		RoutingtableItem item = iter.next();
		if (!datatype.equals(item.getDestination().getDatatype())) {
		    iter.remove();
		}
	    }
	    return items;
	} catch (CollectException e) {
	    throw e;
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(e, FlagEnumeration.ERROR);
	}
    }

    // Private

    /**
     * Convert a 32-byte hex string to an IPv6 address format string.
     */
    private String toIp6AddressString(String hex) {
	if (hex.length() == 32) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=0; i < 8; i++) {
		if (i > 0) {
		    sb.append(":");
		}
		int start = i * 4;
		int end = start + 4;
		sb.append(hex.substring(start, end));
	    }
	    return sb.toString();
	} else if (hex.indexOf("%") != -1) {
	    return toIp6AddressString(hex.substring(0,hex.indexOf("%")));
	} else {
	    return hex;
	}
    }

    /**
     * 0c0afe -> fe0a0c
     */
    private String reverseEndian(String hex) {
	if (hex.length() % 2 == 0) {
	    StringBuffer sb = new StringBuffer();
	    for (int i=hex.length(); i > 0; i-=2) {
		int begin = i - 2;
		sb.append(hex.substring(begin, i));
	    }
	    return sb.toString();
	} else {
	    throw new IllegalArgumentException(hex);
	}
    }

    /**
     * Try to make a valid EntityItemIPAddressType from the input address.
     *
     * @param ipv6 true for IPv6, false for IPv4
     */
    private EntityItemIPAddressType makeIPAddressType(String address, boolean ipv6) {
	EntityItemIPAddressType addr = Factories.sc.core.createEntityItemIPAddressType();
	if (ipv6) {
	    addr.setValue(toIp6AddressString(address));
	    addr.setDatatype(SimpleDatatypeEnumeration.IPV_6_ADDRESS.value());
	} else {
	    if ("default".equals(address)) {
		    address = "0.0.0.0";
	    }
	    try {
		addr.setValue(NetworkInterface.toIp4AddressString(address));
	    } catch (IllegalArgumentException e) {
		addr.setValue(address); // this will not be valid...
	    }
	    addr.setDatatype(SimpleDatatypeEnumeration.IPV_4_ADDRESS.value());
	}
	return addr;
    }

    /**
     * Idempotent.
     */
    private void init() throws Exception {
	if (routes != null) {
	    return;
	}
	routes = new HashMap<String, Collection<RoutingtableItem>>();

	switch(session.getFlavor()) {
	  case AIX:
	    initAIX();
	    break;

	  case LINUX:
	    initLinux();
	    break;

	  case SOLARIS:
	    initSolaris();
	    break;

	  case MACOSX:
	    initMacOSX();
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, session.getFlavor());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
    }

    private void addRoute(RoutingtableItem item) {
	String destination = (String)item.getDestination().getValue();
	if (!routes.containsKey(destination)) {
	    routes.put(destination, new ArrayList<RoutingtableItem>());
	}
	routes.get(destination).add(item);
    }

    enum AIXRoutingTableFlag {
	ADGD("ACTIVE_DEAD_GATEWAY_DETECTION", "A"),
	UP("UP", "U"),
	HOST("HOST", "H"),
	GATEWAY("GATEWAY", "G"),
	DYNAMIC("DYNAMIC", "D"),
	MODIFIED("MODIFIED", "M"),
	LLINFO("LINK_LAYER", "L"),
	PROTOCLONE("PROTOCOL_CLONING", "c"),
	WASCLONED("WAS_CLONED", "W"),
	PROTO1("PROTOCOL_1", "1"),
	PROTO2("PROTOCOL_2", "2"),
	PROTO3("PROTOCOL_3", "3"),
	BROADCAST("BROADCAST", "b"),
	CACHE("CACHE", "e"),
	LOCAL("LOCAL", "l"),
	MULTICAST("MULTICAST", "m"),
	PINNED("PINNED", "P"),
	REJECT("REJECT", "R"), //unreachable
	MANUAL("STATIC", "S"),
	USABLE("USABLE", "u");

	private String value;
	private String flag;

	private AIXRoutingTableFlag(String value, String flag) {
	    this.value = value;
	    this.flag = flag;
	}

	boolean test(String flags) {
	    return flags.indexOf(flag) != -1;
	}

	String value() {
	    return value;
	}
    }

    private void initAIX() throws Exception {
	Iterator<String> iter = SafeCLI.manyLines("netstat -nr", null, session);
	boolean ipv6 = false;
	while(iter.hasNext()) {
	    String line = iter.next();
	    if (line.trim().length() == 0 || line.startsWith("Routing tables") || line.startsWith("Destination")) {
		continue;
	    } else if (line.startsWith("Route Tree")) {
		ipv6 = line.indexOf("Internet v6") != -1;
	    } else {
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() >= 6) {
		    RoutingtableItem item = Factories.sc.unix.createRoutingtableItem();
		    item.setDestination(makeIPAddressType(tok.nextToken(), ipv6));
		    item.setGateway(makeIPAddressType(tok.nextToken(), ipv6));

		    String flags = tok.nextToken();
		    for (AIXRoutingTableFlag flag : AIXRoutingTableFlag.values()) {
			if (flag.test(flags)) {
			    EntityItemRoutingTableFlagsType flagType =
				Factories.sc.unix.createEntityItemRoutingTableFlagsType();
			    flagType.setValue(flag.value());
			    item.getFlags().add(flagType);
			}
		    }

		    String refs = tok.nextToken();
		    String use = tok.nextToken();

		    EntityItemStringType interfaceName = Factories.sc.core.createEntityItemStringType();
		    interfaceName.setValue(tok.nextToken());
		    item.setInterfaceName(interfaceName);

		    addRoute(item);
		}
	    }
	}
    }

    enum LinuxRoutingTableFlag {
	UP("UP", 0x1),
	GATEWAY("GATEWAY", 0x2),
	HOST("HOST", 0x4),
	REINSTATE("REINSTATE", 0x8),
	DYNAMIC("DYNAMIC", 0x10),
	MODIFIED("MODIFIED", 0x20),
	CLONING("CLONING", 0x100),
	REJECT("REJECT", 0x200),
	LLINFO("LINK_LAYER", 0x400),
	MANUAL("STATIC", 0x800),
	BLACKHOLE("BLACK_HOLE", 0x1000),
	PROTO2("PROTOCOL_2", 0x4000),
	PROTO1("PROTOCOL_1", 0x8000),
	PROCLONE("PROTOCOL_CLONING", 0x10000),
	WASCLONED("WAS_CLONED", 0x20000),
	PROTO3("PROTOCOL_3", 0x40000),
	PINNED("PINNED", 0x100000),
	LOCAL("LOCAL", 0x200000),
	BROADCAST("BROADCAST", 0x400000),
	MULTICAST("MULTICAST", 0x800000),
	INTERFACE_SCOPE("INTERFACE_SCOPE",0x1000000);

	private String value;
	private int flag;

	private LinuxRoutingTableFlag(String value, int flag) {
	    this.value = value;
	    this.flag = flag;
	}

	boolean test(int flags) {
	    return flag == (flag & flags);
	}

	String value() {
	    return value;
	}
    }

    private void initLinux() throws Exception {
	BufferedReader reader = null;
	try {
	    IFile ipv6_route = session.getFilesystem().getFile("/proc/net/ipv6_route");
	    if (ipv6_route.exists()) {
		reader = new BufferedReader(new InputStreamReader(ipv6_route.getInputStream()));
		String line = null;
		while((line = reader.readLine()) != null) {
		    if (line.trim().length() == 0) {
			continue; // skip any blank lines
		    }
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() < 10) {
			continue;
		    }

		    RoutingtableItem item = Factories.sc.unix.createRoutingtableItem();
		    item.setDestination(makeIPAddressType(reverseEndian(tok.nextToken()), true));
		    String mask = tok.nextToken();
		    String source = tok.nextToken();
		    mask = tok.nextToken();
		    item.setGateway(makeIPAddressType(reverseEndian(tok.nextToken()), true));
		    String metric = tok.nextToken();
		    String refCounter = tok.nextToken();
		    String useCounter = tok.nextToken();

		    int flags = (int)Long.parseLong(tok.nextToken(), 16);
		    for (LinuxRoutingTableFlag flag : LinuxRoutingTableFlag.values()) {
			if (flag.test(flags)) {
			    EntityItemRoutingTableFlagsType flagType =
				Factories.sc.unix.createEntityItemRoutingTableFlagsType();
			    flagType.setValue(flag.value());
			    item.getFlags().add(flagType);
			}
		    }

		    EntityItemStringType interfaceName = Factories.sc.core.createEntityItemStringType();
		    interfaceName.setValue(tok.nextToken());
		    item.setInterfaceName(interfaceName);

		    addRoute(item);
		}
		reader.close();
		reader = null;
	    }

	    IFile route = session.getFilesystem().getFile("/proc/net/route");
	    if (route.exists()) {
		reader = new BufferedReader(new InputStreamReader(route.getInputStream()));
		String line = null;
		while((line = reader.readLine()) != null) {
		    if (line.trim().length() == 0 || line.toLowerCase().indexOf("destination") != -1) {
			continue; // skip blank lines and the header line
		    }
		    StringTokenizer tok = new StringTokenizer(line);
		    if (tok.countTokens() < 4) {
			continue;
		    }

		    RoutingtableItem item = Factories.sc.unix.createRoutingtableItem();

		    EntityItemStringType interfaceName = Factories.sc.core.createEntityItemStringType();
		    interfaceName.setValue(tok.nextToken());
		    item.setInterfaceName(interfaceName);

		    item.setDestination(makeIPAddressType(reverseEndian(tok.nextToken()), false));
		    item.setGateway(makeIPAddressType(reverseEndian(tok.nextToken()), false));

		    int flags = Integer.parseInt(tok.nextToken(), 16);
		    for (LinuxRoutingTableFlag flag : LinuxRoutingTableFlag.values()) {
			if (flag.test(flags)) {
			    EntityItemRoutingTableFlagsType flagType =
				Factories.sc.unix.createEntityItemRoutingTableFlagsType();
			    flagType.setValue(flag.value());
			    item.getFlags().add(flagType);
			}
		    }

		    addRoute(item);
		}
	    }
	} finally {
	    if (reader != null) {
		try {
		    reader.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    enum SolarisRoutingTableFlag {
	UP("UP", "U"),
	HOST("HOST", "H"),
	REDUNDANT("REDUNDANT", "M"),
	SETSRC("SETSRC", "S"),
	DYNAMIC("DYNAMIC", "D"),
	BROADCAST("BROADCAST", "b"),
	LOCAL("LOCAL", "L");

	private String value;
	private String flag;

	private SolarisRoutingTableFlag(String value, String flag) {
	    this.value = value;
	    this.flag = flag;
	}

	boolean test(String flags) {
	    return flags.indexOf(flag) != -1;
	}

	String value() {
	    return value;
	}
    }

    void initSolaris() throws Exception {
	List<String> lines = SafeCLI.multiLine("netstat -nr", session, IUnixSession.Timeout.S);
	boolean ipv6 = false;
	for (String line : lines) {
	    if (line.trim().length() == 0 || line.startsWith("--------") || line.trim().startsWith("Destination")) {
		continue;
	    } else if (line.startsWith("Routing Table:")) {
		ipv6 = line.indexOf("IPv6") != -1;
	    } else {
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() >= 5) {
		    RoutingtableItem item = Factories.sc.unix.createRoutingtableItem();
		    item.setDestination(makeIPAddressType(tok.nextToken(), ipv6));
		    item.setGateway(makeIPAddressType(tok.nextToken(), ipv6));

		    String flags = tok.nextToken();
		    for (SolarisRoutingTableFlag flag : SolarisRoutingTableFlag.values()) {
			if (flag.test(flags)) {
			    EntityItemRoutingTableFlagsType flagType =
				Factories.sc.unix.createEntityItemRoutingTableFlagsType();
			    flagType.setValue(flag.value());
			    item.getFlags().add(flagType);
			}
		    }

		    String refs = tok.nextToken();
		    String use = tok.nextToken();

		    if (tok.hasMoreTokens()) {
			EntityItemStringType interfaceName = Factories.sc.core.createEntityItemStringType();
			interfaceName.setValue(tok.nextToken());
			item.setInterfaceName(interfaceName);
		    }

		    addRoute(item);
		}
	    }
	}
    }

    enum DarwinRoutingTableFlag {
	PROTO1("PROTOCOL_1", "1"),
	PROTO2("PROTOCOL_2", "2"),
	PROTO3("PROTOCOL_3", "3"),
	BLACKHOLE("BLACK_HOLE", "B"),
	CLONING("CLONING", "C"),
	PROTOCLONE("PROTOCOL_CLONING", "c"),
	DYNAMIC("DYNAMIC", "D"),
	GATEWAY("GATEWAY", "G"),
	HOST("HOST", "H"),
	INTERFACE_SCOPE("INTERFACE_SCOPE","I"),
	LLINFO("LINK_LAYER", "L"),
	MODIFIED("MODIFIED", "M"),
	MULTICAST("MULTICAST", "m"),
	REJECT("REJECT", "R"),
	MANUAL("STATIC", "S"),
	UP("UP", "U"),
	WASCLONED("WAS_CLONED", "W"),
	XRESOLVE("XRESOLVE", "X");

	private String value;
	private String flag;

	private DarwinRoutingTableFlag(String value, String flag) {
	    this.value = value;
	    this.flag = flag;
	}

	boolean test(String flags) {
	    return flags.indexOf(flag) != -1;
	}

	String value() {
	    return value;
	}
    }

    void initMacOSX() throws Exception {
	List<String> lines = SafeCLI.multiLine("netstat -alnr", session, IUnixSession.Timeout.S);
	boolean ipv6 = false;
	for (String line : lines) {
	    if (line.trim().length() == 0 || line.startsWith("Routing tables") || line.startsWith("Destination")) {
		continue;
	    } else if (line.startsWith("Internet")) {
		ipv6 = line.indexOf("Internet6") != -1;
	    } else {
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() >= 4) {
		    RoutingtableItem item = Factories.sc.unix.createRoutingtableItem();
		    item.setDestination(makeIPAddressType(tok.nextToken(), ipv6));
		    item.setGateway(makeIPAddressType(tok.nextToken(), ipv6));

		    String flags = tok.nextToken();
		    for (DarwinRoutingTableFlag flag : DarwinRoutingTableFlag.values()) {
			if (flag.test(flags)) {
			    EntityItemRoutingTableFlagsType flagType =
				Factories.sc.unix.createEntityItemRoutingTableFlagsType();
			    flagType.setValue(flag.value());
			    item.getFlags().add(flagType);
			}
		    }

		    if (!ipv6) {
			String refs = tok.nextToken();
			String use = tok.nextToken();
		    }
		    EntityItemStringType interfaceName = Factories.sc.core.createEntityItemStringType();
		    interfaceName.setValue(tok.nextToken());
		    item.setInterfaceName(interfaceName);

		    addRoute(item);
		}
	    }
	}
    }
}
