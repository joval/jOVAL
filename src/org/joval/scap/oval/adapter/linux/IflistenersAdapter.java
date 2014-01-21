// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.linux;

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
import java.util.regex.PatternSyntaxException;

import jsaf.Message;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.io.IFile;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.linux.IflistenersObject;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.linux.EntityItemProtocolType;
import scap.oval.systemcharacteristics.linux.IflistenersItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Collects dpkginfo OVAL items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class IflistenersAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, Collection<IflistenersItem>> listeners;
    private CollectException error;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.LINUX) {
	    this.session = (IUnixSession)session;
	    classes.add(IflistenersObject.class);
	} else {
	    notapplicable.add(IflistenersObject.class);
	}
	return classes;
    }

    public Collection<IflistenersItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	initialize();
	IflistenersObject iObj = (IflistenersObject)obj;
	String interfaceName = (String)iObj.getInterfaceName().getValue();
	Collection<IflistenersItem> items = new ArrayList<IflistenersItem>();
	switch(iObj.getInterfaceName().getOperation()) {
	  case EQUALS:
	    if (listeners.containsKey(interfaceName)) {
		items.addAll(listeners.get(interfaceName));
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern p = StringTools.pattern(interfaceName);
		for (Map.Entry<String, Collection<IflistenersItem>> entry : listeners.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.addAll(entry.getValue());
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

	  case NOT_EQUAL:
	    for (Map.Entry<String, Collection<IflistenersItem>> entry : listeners.entrySet()) {
		if (!entry.getKey().equals(interfaceName)) {
		    items.addAll(entry.getValue());
		}
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, iObj.getInterfaceName().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	return items;
    }

    // Private

    private void initialize() throws CollectException {
	if (error != null) {
	    throw error;
	} else if (listeners == null) {
	    try {
		List<Listener> listeners = new ArrayList<Listener>();
		IFile f = session.getFilesystem().getFile("/proc/net/packet");
		BufferedReader reader = new BufferedReader(new InputStreamReader(f.getInputStream(), StringTools.ASCII));
		String line = null;
		int lineNum = 0;
		while ((line = reader.readLine()) != null) {
		    if (lineNum == 0) {
			lineNum++;
			continue;
		    } 
		    listeners.add(new Listener(line));
		}

		Map<Integer, String[]> interfaces = new HashMap<Integer, String[]>();
		StringBuffer cmd = new StringBuffer("for intf in `ls /sys/class/net | xargs -n 1 echo`; ");
		cmd.append("do printf `cat /sys/class/net/$intf/ifindex`=$intf/`cat /sys/class/net/$intf/address`\\\\n; ");
		cmd.append("done;");
		for (String s : SafeCLI.multiLine(cmd.toString(), session, IUnixSession.Timeout.S)) {
		    int ptr = s.indexOf("=");
		    interfaces.put(new Integer(s.substring(0,ptr)), s.substring(ptr+1).split("/"));
		}

		Map<Long, Integer> inodePidMap = new HashMap<Long, Integer>();
		cmd = new StringBuffer("find /proc -maxdepth 2 -type d -name fd ");
		cmd.append(" | xargs -I{} ls -l {} 2>/dev/null");
		Integer pid = null;
		Iterator<String> lines = SafeCLI.manyLines(cmd.toString(), null, session);
		while(lines.hasNext()) {
		    line = lines.next().trim();
		    if (line.length() == 0) {
			pid = null;
		    } else if (line.startsWith("/proc/") && line.endsWith("/fd:")) {
			pid = new Integer(line.substring(6, line.lastIndexOf("/")));
		    } else if (pid != null) {
			int ptr = line.indexOf("->");
			if (ptr != -1) {
			    String link = line.substring(ptr).trim();
			    if (link.startsWith("socket:[")) {
				// Type 1 socket
				inodePidMap.put(new Long(link.substring(8,link.indexOf("]"))), pid);
			    } else if (line.startsWith("[0000]:")) {
				// Type 2 socket
				inodePidMap.put(new Long(link.substring(7).trim()), pid);
			    }
			}
		    }
		}

		Map<Integer, String[]> processMap = new HashMap<Integer, String[]>();
		lineNum = 0;
		for (String s : SafeCLI.multiLine("ps -e -o pid,uid,args", session, IUnixSession.Timeout.S)) {
		    if (lineNum == 0) {
			lineNum++;
			continue;
		    }
		    StringTokenizer tok = new StringTokenizer(s);
		    pid = new Integer(tok.nextToken());
		    String uid = tok.nextToken();
		    String arg = tok.nextToken("\n");
		    processMap.put(pid, new String[]{uid, arg});
		}

		this.listeners = new HashMap<String, Collection<IflistenersItem>>();
		for (Listener listener : listeners) {
		    IflistenersItem item = Factories.sc.linux.createIflistenersItem();

		    EntityItemStringType hwAddress = Factories.sc.core.createEntityItemStringType();
		    hwAddress.setValue(interfaces.get(listener.ifindex)[1]);
		    item.setHwAddress(hwAddress);

		    EntityItemProtocolType protocol = Factories.sc.linux.createEntityItemProtocolType();
		    protocol.setValue(Protocol.fromValue(listener.proto_num).toString());
		    item.setProtocol(protocol);

		    pid = inodePidMap.get(listener.inode);
		    EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
		    pidType.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    pidType.setValue(pid.toString());
		    item.setPid(pidType);

		    EntityItemIntType userId = Factories.sc.core.createEntityItemIntType();
		    userId.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    userId.setValue(processMap.get(pid)[0]);
		    item.setUserId(userId);

		    EntityItemStringType programName = Factories.sc.core.createEntityItemStringType();
		    programName.setValue(processMap.get(pid)[1]);
		    item.setProgramName(programName);

		    String ifname = interfaces.get(listener.ifindex)[0];
		    EntityItemStringType interfaceName = Factories.sc.core.createEntityItemStringType();
		    interfaceName.setValue(ifname);
		    item.setInterfaceName(interfaceName);
		    if (!this.listeners.containsKey(ifname)) {
			this.listeners.put(ifname, new ArrayList<IflistenersItem>());
		    }
		    this.listeners.get(ifname).add(item);
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		error = new CollectException(e, FlagEnumeration.ERROR);
		throw error;
	    }
	}
    }

    static class Listener {
	int refcnt, sk_type, proto_num, ifindex, running, rmem, uid;
	long inode;

	Listener(String line) throws IllegalArgumentException {
	    StringTokenizer tok = new StringTokenizer(line);
	    if (tok.countTokens() == 9) {
		String s = tok.nextToken();
		refcnt = Integer.parseInt(tok.nextToken());
		sk_type = Integer.parseInt(tok.nextToken(), 16);
		proto_num = Integer.parseInt(tok.nextToken());
		ifindex = Integer.parseInt(tok.nextToken());
		running = Integer.parseInt(tok.nextToken());
		rmem = Integer.parseInt(tok.nextToken());
		uid = Integer.parseInt(tok.nextToken());
		inode = Long.parseLong(tok.nextToken());
	    } else {
		throw new IllegalArgumentException(line);
	    }
	}
    }

    /**
     * enum based on /usr/include/linux/if_ether.h
     */
    enum Protocol {
	ETH_P_LOOP(0x0060),
	ETH_P_PUP(0x0200),
	ETH_P_PUPAT(0x0201),
	ETH_P_IP(0x0800),
	ETH_P_X25(0x0805),
	ETH_P_ARP(0x0806),
	ETH_P_BPQ(0x08FF),
	ETH_P_IEEEPUP(0x0a00),
	ETH_P_IEEEPUPAT(0x0a01),
	ETH_P_DEC(0x6000),
	ETH_P_DNA_DL(0x6001),
	ETH_P_DNA_RC(0x6002),
	ETH_P_DNA_RT(0x6003),
	ETH_P_LAT(0x6004),
	ETH_P_DIAG(0x6005),
	ETH_P_CUST(0x6006),
	ETH_P_SCA(0x6007),
	ETH_P_TEB(0x6558),
	ETH_P_RARP(0x8035),
	ETH_P_ATALK(0x809B),
	ETH_P_AARP(0x80F3),
	ETH_P_8021Q(0x8100),
	ETH_P_IPX(0x8137),
	ETH_P_IPV6(0x86DD),
	ETH_P_PAUSE(0x8808),
	ETH_P_SLOW(0x8809),
	ETH_P_WCCP(0x883E),
	ETH_P_PPP_DISC(0x8863),
	ETH_P_PPP_SES(0x8864),
	ETH_P_MPLS_UC(0x8847),
	ETH_P_MPLS_MC(0x8848),
	ETH_P_ATMMPOA(0x884c),
	ETH_P_LINK_CTL(0x886c),
	ETH_P_ATMFATE(0x8884),
	ETH_P_PAE(0x888E),
	ETH_P_AOE(0x88A2),
	ETH_P_TIPC(0x88CA),
	ETH_P_1588(0x88F7),
	ETH_P_FCOE(0x8906),
	ETH_P_FIP(0x8914),
	ETH_P_EDSA(0xDADA),
	ETH_P_802_3(0x0001),
	ETH_P_AX25(0x0002),
	ETH_P_ALL(0x0003),
	ETH_P_802_2(0x0004),
	ETH_P_SNAP(0x0005),
	ETH_P_DDCMP(0x0006),
	ETH_P_WAN_PPP(0x0007),
	ETH_P_PPP_MP(0x0008),
	ETH_P_LOCALTALK(0x0009),
	ETH_P_CAN(0x000C),
	ETH_P_PPPTALK(0x0010),
	ETH_P_TR_802_2(0x0011),
	ETH_P_MOBITEX(0x0015),
	ETH_P_CONTROL(0x0016),
	ETH_P_IRDA(0x0017),
	ETH_P_ECONET(0x0018),
	ETH_P_HDLC(0x0019),
	ETH_P_ARCNET(0x001A),
	ETH_P_DSA(0x001B),
	ETH_P_TRAILER(0x001C),
	ETH_P_PHONET(0x00F5),
	ETH_P_IEEE802154(0x00F6),
	ETH_P_CAIF(0x00F7);

	private int value;

	private Protocol(int value) {
	    this.value = value;
	}

	static Protocol fromValue(int i) throws IllegalArgumentException {
	    for (Protocol p : values()) {
		if (i == p.value) return p;
	    }
	    throw new IllegalArgumentException("0x" + Integer.toHexString(i));
	}
    }

}
