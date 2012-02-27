// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.EntityObjectIntType;
import oval.schemas.definitions.core.EntityObjectStringType;
import oval.schemas.definitions.unix.Process58Object;
import oval.schemas.definitions.unix.ProcessObject;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.EntityItemCapabilityType;
import oval.schemas.systemcharacteristics.unix.Process58Item;
import oval.schemas.systemcharacteristics.unix.ProcessItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.ItemSet;
import org.joval.oval.NotCollectableException;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Scans for items associated with ProcessObject and Process58Object OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ProcessAdapter implements IAdapter {
    private IUnixSession session;
    private boolean initialized = false;
    private Hashtable<String, ProcessData> processes;
    private String error = null;

    public ProcessAdapter(IUnixSession session) {
	this.session = session;
	processes = new Hashtable<String, ProcessData>();
    }

    // Implement IAdapter

    private static Class[] objectClasses = {ProcessObject.class, Process58Object.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc)
		throws OvalException, NotCollectableException {

	if (!initialized) {
	    scanProcesses();
	}

	if (error != null) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	try {
	    if (rc.getObject() instanceof ProcessObject) {
		ProcessObject pObj = (ProcessObject)rc.getObject();
		EntityObjectStringType command = pObj.getCommand();
		ArrayList<String> commands = new ArrayList<String>();
		if (command.isSetVarRef()) {
		    commands.addAll(rc.resolve(command.getVarRef()));
		} else {
		    commands.add((String)command.getValue());
		}
		for (ProcessData data : getProcesses(command.getOperation(), commands.toArray(new String[0]))) {
		    items.add(JOVALSystem.factories.sc.unix.createProcessItem(data.getProcessItem()));
		}
	    } else {
		Process58Object pObj = (Process58Object)rc.getObject();
		ItemSet<Process58Item> set1 = null, set2 = null;

		if (pObj.isSetCommandLine()) {
		    EntityObjectStringType commandLine = pObj.getCommandLine();
		    ArrayList<String> commands = new ArrayList<String>();
		    if (commandLine.isSetVarRef()) {
			commands.addAll(rc.resolve(commandLine.getVarRef()));
		    } else {
			commands.add((String)commandLine.getValue());
		    }
		    List<Process58Item> list = new Vector<Process58Item>();
		    for (ProcessData process : getProcesses(commandLine.getOperation(), commands.toArray(new String[0]))) {
			list.add(process.getProcess58Item());
		    }
		    set1 = new ItemSet<Process58Item>(list);
		}

		if (pObj.isSetPid()) {
		    EntityObjectIntType pid = pObj.getPid();
		    ArrayList<Integer> pids = new ArrayList<Integer>();
		    if (pid.isSetVarRef()) {
			for (String s : rc.resolve(pid.getVarRef())) {
			    pids.add(new Integer(s));
			}
		    } else {
			pids.add(new Integer((String)pid.getValue()));
		    }
		    List<Process58Item> list = new Vector<Process58Item>();
		    for (ProcessData process : getProcesses(pid.getOperation(), pids.toArray(new Integer[0]))) {
			list.add(process.getProcess58Item());
		    }
		    set2 = new ItemSet<Process58Item>(list);
		}

		if (set1 == null && set2 == null) {
		    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_PROCESS58_OBJECT, pObj.getId()));
		} else if (set1 == null) {
		    for (Process58Item item : set2.toList()) {
			item.setId(null);
			items.add(JOVALSystem.factories.sc.unix.createProcess58Item(item));
		    }
		} else if (set2 == null) {
		    for (Process58Item item : set1.toList()) {
			item.setId(null);
			items.add(JOVALSystem.factories.sc.unix.createProcess58Item(item));
		    }
		} else {
		    for (Process58Item item : set1.intersection(set2).toList()) {
			item.setId(null);
			items.add(JOVALSystem.factories.sc.unix.createProcess58Item(item));
		    }
		}
	    }
	} catch (ResolveException e) {
	    throw new OvalException(e);
	} catch (NumberFormatException e) {
	    throw new OvalException(e);
	} catch (PatternSyntaxException e) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.WARNING);
	    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	}
	return items;
    }

    // Private

    /**
     * Return a collection of ProcessData objects fitting the criteria of the command StringType.
     */
    private Collection<ProcessData> getProcesses(OperationEnumeration op, String[] commands)
		throws PatternSyntaxException, NotCollectableException {

	Collection<ProcessData> result = new Vector<ProcessData>();
	for (String command : commands) {
	    switch (op) {
	      case EQUALS: {
		ProcessData data = processes.get(command);
		if (data != null) {
		    result.add(data);
		}
		break;
	      }

	      case CASE_INSENSITIVE_EQUALS:
		for (String key : processes.keySet()) {
		    if (key.equalsIgnoreCase(command)) {
			result.add(processes.get(key));
		    }
		}
		break;

	      case PATTERN_MATCH:
		for (String key : processes.keySet()) {
		    if (Pattern.compile(command).matcher(key).find()) {
			result.add(processes.get(key));
		    }
		}
		break;

	      case NOT_EQUAL:
		for (String key : processes.keySet()) {
		    if (!command.equals(key)) {
			result.add(processes.get(key));
		    }
		}
		break;

	      default: {
		String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new NotCollectableException(s);
	      }
	    }
	}
	return result;
    }

    /**
     * Return a collection of ProcessData objects fitting the criteria of the pid IntegerType.
     */
    private Collection<ProcessData> getProcesses(OperationEnumeration op, Integer[] pids)
		throws PatternSyntaxException, NotCollectableException {

	Collection<ProcessData> result = new Vector<ProcessData>();
	for (Integer pid : pids) {
	    switch (op) {
	      case EQUALS:
		for (ProcessData data : processes.values()) {
		    if (((String)data.pid.getValue()).equals(pid.toString())) {
			result.add(data);
			break;
		    }
		}
		break;

	      case GREATER_THAN:
		for (ProcessData data : processes.values()) {
		    if (Integer.parseInt((String)data.pid.getValue()) > pid.intValue()) {
			result.add(data);
		    }
		}
		break;

	      case LESS_THAN:
		for (ProcessData data : processes.values()) {
		    if (Integer.parseInt((String)data.pid.getValue()) < pid.intValue()) {
			result.add(data);
		    }
		}
		break;

	      default: {
		String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new NotCollectableException(s);
	      }
	    }
	}
	return result;
    }

    /**
     * Collect information about all the running processes on the machine.
     */
    private void scanProcesses() {
	String args = null;

//TBD (DAS):
//  sid - sessionId
//  loginuid = contents of /proc/[pid]/loginuid

	switch(session.getFlavor()) {
	  case MACOSX:
	    args = "ps -A -o pid,ppid,pri,uid,ruid,tty,time,stime,command";
	    break;

	  case AIX:
	  case LINUX:
	  case SOLARIS:
	    args = "ps -e -o pid,ppid,pri,uid,ruid,tty,class,time,stime,args";
	    break;

	  default:
	    return;
	}
	try {
	    List<String> lines = SafeCLI.multiLine(args, session, IUnixSession.Timeout.S);
	    for (int i=1; i < lines.size(); i++) { // skip the header at line 0
		StringTokenizer tok = new StringTokenizer(lines.get(i));
		ProcessData process = new ProcessData();
		process.pid.setValue(tok.nextToken());
		process.pid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.ppid.setValue(tok.nextToken());
		process.ppid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.priority.setValue(tok.nextToken());
		process.priority.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.userid.setValue(tok.nextToken());
		process.userid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.ruid.setValue(tok.nextToken());
		process.ruid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.tty.setValue(tok.nextToken());
		switch(session.getFlavor()) {
		  case MACOSX:
		    process.schedulingClass.setStatus(StatusEnumeration.NOT_COLLECTED);
		    break;
		  default:
		    process.schedulingClass.setValue(tok.nextToken());
		    break;
		}
		process.execTime.setValue(tok.nextToken());
		process.startTime.setValue(tok.nextToken());
		String cmd = tok.nextToken("\n").trim();
		process.command.setValue(cmd);
		processes.put(cmd, process);
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }

    class ProcessData {
	EntityItemStringType command, tty, startTime, execTime, schedulingClass;
	EntityItemIntType ruid, userid, priority, ppid, pid, loginuid, sessionId;
	EntityItemBoolType execShield;
	List<EntityItemCapabilityType> posixCapability;
	List<EntityItemStringType> selinuxDomainLabel;

	ProcessData() {
	    command = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    tty = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    startTime = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    execTime = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    schedulingClass = JOVALSystem.factories.sc.core.createEntityItemStringType();
	    ruid = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    userid = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    priority = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    ppid = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    pid = JOVALSystem.factories.sc.core.createEntityItemIntType();

	    //
	    // Process58Item additions
	    //
	    loginuid = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    loginuid.setStatus(StatusEnumeration.NOT_COLLECTED);
	    sessionId = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    sessionId.setStatus(StatusEnumeration.NOT_COLLECTED);
	    execShield = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	    execShield.setStatus(StatusEnumeration.NOT_COLLECTED);
	    posixCapability = new Vector<EntityItemCapabilityType>();
	    selinuxDomainLabel = new Vector<EntityItemStringType>();
	}

	ProcessItem getProcessItem() {
	    ProcessItem process = JOVALSystem.factories.sc.unix.createProcessItem();
	    process.setPid(pid);
	    process.setPpid(ppid);
	    process.setPriority(priority);
	    process.setUserId(userid);
	    process.setRuid(ruid);
	    process.setTty(tty);
	    process.setExecTime(execTime);
	    process.setStartTime(startTime);
	    process.setCommand(command);
	    return process;
	}

	Process58Item getProcess58Item() {
	    Process58Item process = JOVALSystem.factories.sc.unix.createProcess58Item();
	    process.setPid(pid);
	    process.setPpid(ppid);
	    process.setPriority(priority);
	    process.setUserId(userid);
	    process.setRuid(ruid);
	    process.setTty(tty);
	    process.setExecTime(execTime);
	    process.setStartTime(startTime);
	    process.setCommandLine(command);
	    process.setSessionId(sessionId);
	    process.setLoginuid(loginuid);
	    if (posixCapability.size() == 0) {
		process.unsetPosixCapability();
	    } else {
		process.getPosixCapability().addAll(posixCapability);
	    }
	    if (selinuxDomainLabel.size() == 0) {
		process.unsetSelinuxDomainLabel();
	    } else {
		process.getSelinuxDomainLabel().addAll(selinuxDomainLabel);
	    }
	    return process;
	}
    }

    enum PosixCapability {
	CAP_CHOWN(0),
	CAP_DAC_OVERRIDE(1),
	CAP_DAC_READ_SEARCH(2),
	CAP_FOWNER(3),
	CAP_FSETID(4),
	CAP_KILL(5),
	CAP_SETGID(6),
	CAP_SETUID(7),
	CAP_SETPCAP(8),
	CAP_LINUX_IMMUTABLE(9),
	CAP_NET_BIND_SERVICE(10),
	CAP_NET_BROADCAST(11),
	CAP_NET_ADMIN(12),
	CAP_NET_RAW(13),
	CAP_IPC_LOCK(14),
	CAP_IPC_OWNER(15),
	CAP_SYS_MODULE(16),
	CAP_SYS_RAWIO(17),
	CAP_SYS_CHROOT(18),
	CAP_SYS_PTRACE(19),
//	CAP_SYS_PAACT(20),	// DAS: for some reason this isn't specified in the OVAL EntityItemCapabilityType spec
	CAP_SYS_ADMIN(21),
	CAP_SYS_BOOT(22),
	CAP_SYS_NICE(23),
	CAP_SYS_RESOURCE(24),
	CAP_SYS_TIME(25),
	CAP_SYS_TTY_CONFIG(26),
	CAP_MKNOD(27),
	CAP_LEASE(28),
	CAP_AUDIT_WRITE(29),
	CAP_AUDIT_CONTROL(30),
	CAP_SETFCAP(31),
	CAP_MAC_OVERRIDE(32),
	CAP_MAC_ADMIN(33);

	private int val;

	PosixCapability(int val) {
	    this.val = val;
	}

	static PosixCapability getCapability(int i) throws IllegalArgumentException {
	    for (PosixCapability cap : values()) {
		if (cap.val == i) {
		    return cap;
		}
	    }
	    throw new IllegalArgumentException(Integer.toString(i));
	}
    }

}
