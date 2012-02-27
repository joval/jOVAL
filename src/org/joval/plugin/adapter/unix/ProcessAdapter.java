// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.math.BigInteger;
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

import org.joval.intf.io.IFile;
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
    private Collection<ProcessData> processes;
    private String error = null;

    public ProcessAdapter(IUnixSession session) {
	this.session = session;
	processes = new Vector<ProcessData>();
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
		String[] commands = null;
		if (command.isSetVarRef()) {
		    commands = rc.resolve(command.getVarRef()).toArray(new String[0]);
		} else {
		    commands = new String[1];
		    commands[0] = (String)command.getValue();
		}
		for (ProcessData process : getProcesses(command.getOperation(), commands)) {
		    items.add(JOVALSystem.factories.sc.unix.createProcessItem(process.getProcessItem()));
		}
	    } else {
		Process58Object pObj = (Process58Object)rc.getObject();
		ItemSet<Process58Item> set1 = null, set2 = null;

		if (pObj.isSetCommandLine()) {
		    EntityObjectStringType commandLine = pObj.getCommandLine();
		    String[] commands = null;
		    if (commandLine.isSetVarRef()) {
			commands = rc.resolve(commandLine.getVarRef()).toArray(new String[0]);
		    } else {
			commands = new String[1];
			commands[0] = (String)commandLine.getValue();
		    }
		    List<Process58Item> list = new Vector<Process58Item>();
		    for (ProcessData process : getProcesses(commandLine.getOperation(), commands)) {
			list.add(process.getProcess58Item());
		    }
		    set1 = new ItemSet<Process58Item>(list);
		}

		if (pObj.isSetPid()) {
		    EntityObjectIntType pid = pObj.getPid();
		    Integer[] pids = null;
		    if (pid.isSetVarRef()) {
			Collection<String> values = rc.resolve(pid.getVarRef());
			pids = new Integer[values.size()];
			int i = 0;
			for (String val : values) {
			    pids[i++] = new Integer(val);
			}
		    } else {
			pids = new Integer[1];
			pids[0] = new Integer((String)pid.getValue());
		    }
		    List<Process58Item> list = new Vector<Process58Item>();
		    for (ProcessData process : getProcesses(pid.getOperation(), pids)) {
			list.add(process.getProcess58Item());
		    }
		    set2 = new ItemSet<Process58Item>(list);
		}

		if (set1 == null || set2 == null) {
		    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_BAD_PROCESS58_OBJECT, pObj.getId()));
		} else {
		    for (Process58Item item : set1.intersection(set2)) {
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
	      case EQUALS:
		for (ProcessData process : processes) {
		    if (command.equals((String)process.command.getValue())) {
			result.add(process);
		    }
		}
		break;

	      case CASE_INSENSITIVE_EQUALS:
		for (ProcessData process : processes) {
		    if (command.equalsIgnoreCase((String)process.command.getValue())) {
			result.add(process);
		    }
		}
		break;

	      case PATTERN_MATCH:
		for (ProcessData process : processes) {
		    if (Pattern.compile(command).matcher((String)process.command.getValue()).find()) {
			result.add(process);
		    }
		}
		break;

	      case NOT_EQUAL:
		for (ProcessData process : processes) {
		    if (!command.equals((String)process.command.getValue())) {
			result.add(process);
		    }
		}
		break;

	      default:
		throw new NotCollectableException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
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
		for (ProcessData process : processes) {
		    if (((String)process.pid.getValue()).equals(pid.toString())) {
			result.add(process);
			break;
		    }
		}
		break;

	      case GREATER_THAN:
		for (ProcessData process : processes) {
		    if (Integer.parseInt((String)process.pid.getValue()) > pid.intValue()) {
			result.add(process);
		    }
		}
		break;

	      case LESS_THAN:
		for (ProcessData process : processes) {
		    if (Integer.parseInt((String)process.pid.getValue()) < pid.intValue()) {
			result.add(process);
		    }
		}
		break;

	      default:
		throw new NotCollectableException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
	    }
	}
	return result;
    }

    /**
     * Collect information about all the running processes on the machine.
     */
    private void scanProcesses() {
	String args = null;
	switch(session.getFlavor()) {
	  case MACOSX:
	    args = "ps -A -o pid,ppid,pri,uid,ruid,tty,time,stime,command";
	    break;

	  case AIX:
	  case LINUX:
	  case SOLARIS:
	    args = "ps -e -o pid,ppid,pri,uid,ruid,tty,sid,class,time,stime,args";
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
		    process.sessionId.setStatus(StatusEnumeration.NOT_COLLECTED);
		    process.schedulingClass.setStatus(StatusEnumeration.NOT_COLLECTED);
		    break;
		  default:
		    process.sessionId.setValue(tok.nextToken());
		    process.sessionId.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    process.schedulingClass.setValue(tok.nextToken());
		    break;
		}
		process.execTime.setValue(tok.nextToken());
		process.startTime.setValue(tok.nextToken());
		String cmd = tok.nextToken("\n").trim();
		process.command.setValue(cmd);
		processes.add(process);
	    }

	    //
	    // On Linux, we can collect the loginuid for each process from the /proc filesystem
	    //
	    for (ProcessData process : processes) {
		if (session.getFlavor() == IUnixSession.Flavor.LINUX) {
		    String pid = (String)process.pid.getValue();
		    IFile f = session.getFilesystem().getFile("/proc/" + pid + "/loginuid");
		    if (f.exists() && f.isFile()) {
			BufferedReader reader = null;
			try {
			    reader = new BufferedReader(new InputStreamReader(f.getInputStream()));
			    String loginuid = reader.readLine();
			    if (loginuid != null) {
				process.loginuid.setValue(loginuid);
				process.loginuid.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    }
			} finally {
			    if (reader != null) {
				try {
				    reader.close();
				} catch (IOException e) {
				}
			    }
			}
		    } else {
			String reason = JOVALSystem.getMessage(JOVALMsg.ERROR_IO_NOT_FILE);
			session.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_IO, f.getPath(), reason));
		    }
		} else {
		    process.loginuid.setStatus(StatusEnumeration.NOT_COLLECTED);
		}
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
	    sessionId = JOVALSystem.factories.sc.core.createEntityItemIntType();
	    loginuid = JOVALSystem.factories.sc.core.createEntityItemIntType();
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
