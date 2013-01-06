// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.EntityItemCapabilityType;
import oval.schemas.systemcharacteristics.unix.Process58Item;
import oval.schemas.systemcharacteristics.unix.ProcessItem;

import jsaf.intf.io.IFile;
import jsaf.intf.system.IBaseSession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.ItemSet;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

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

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    processes = new Vector<ProcessData>();
	    classes.add(ProcessObject.class);
	    classes.add(Process58Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (!initialized) {
	    scanProcesses();
	}

	if (error != null) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	if (obj instanceof ProcessObject) {
	    Collection<ProcessItem> items = new Vector<ProcessItem>();
	    try {
		ProcessObject pObj = (ProcessObject)obj;
		String command = (String)pObj.getCommand().getValue();
		for (ProcessData process : getProcesses(pObj.getCommand().getOperation(), command)) {
		    items.add(process.getProcessItem());
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.WARNING);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
	    }
	    return items;
	} else {
	    Collection<Process58Item> items = new Vector<Process58Item>();
	    try {
		Process58Object pObj = (Process58Object)obj;
		ItemSet<Process58Item> set1 = null, set2 = null;

		if (pObj.isSetCommandLine()) {
		    String commandLine = (String)pObj.getCommandLine().getValue();
		    List<Process58Item> list = new Vector<Process58Item>();
		    for (ProcessData process : getProcesses(pObj.getCommandLine().getOperation(), commandLine)) {
			list.add(process.getProcess58Item());
		    }
		    set1 = new ItemSet<Process58Item>(list);
		}

		if (pObj.isSetPid()) {
		    Integer pid = new Integer((String)pObj.getPid().getValue());
		    List<Process58Item> list = new Vector<Process58Item>();
		    for (ProcessData process : getProcesses(pObj.getPid().getOperation(), pid)) {
			list.add(process.getProcess58Item());
		    }
		    set2 = new ItemSet<Process58Item>(list);
		}

		if (set1 == null || set2 == null) {
		    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_PROCESS58_OBJECT, pObj.getId()), FlagEnumeration.ERROR);
		} else {
		    for (Process58Item item : set1.intersection(set2)) {
			items.add(item);
		    }
		}
	    } catch (NumberFormatException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.WARNING);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.WARNING);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    return items;
	}
    }

    // Private

    /**
     * Return a collection of ProcessData objects fitting the criteria of the command StringType.
     */
    private Collection<ProcessData> getProcesses(OperationEnumeration op, String command)
		throws PatternSyntaxException, CollectException {

	Collection<ProcessData> result = new Vector<ProcessData>();
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
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return result;
    }

    /**
     * Return a collection of ProcessData objects fitting the criteria of the pid IntegerType.
     */
    private Collection<ProcessData> getProcesses(OperationEnumeration op, Integer pid)
		throws PatternSyntaxException, CollectException {

	Collection<ProcessData> result = new Vector<ProcessData>();
	switch (op) {
	  case EQUALS:
	    for (ProcessData process : processes) {
		if (((String)process.pid.getValue()).equals(pid.toString())) {
		    result.add(process);
		    break;
		}
	    }
	    break;

	  case GREATER_THAN_OR_EQUAL:
	    for (ProcessData process : processes) {
		if (Integer.parseInt((String)process.pid.getValue()) >= pid.intValue()) {
		    result.add(process);
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

	  case LESS_THAN_OR_EQUAL:
	    for (ProcessData process : processes) {
		if (Integer.parseInt((String)process.pid.getValue()) <= pid.intValue()) {
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
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
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
	    args = "/usr/sysv/bin/ps -A -o pid,ppid,pri,uid,ruid,tty,sid,class,time,stime,args | cat -";
	    break;

	  case LINUX:
	  case SOLARIS:
	    args = "ps -e -o pid,ppid,pri,uid,ruid,tty,sid,class,time,stime,args";
	    break;

	  default:
	    return;
	}
	try {
	    List<String> lines = SafeCLI.multiLine(args, session, IUnixSession.Timeout.S);
	    for (int i=0; i < lines.size(); i++) {
		String line = lines.get(i).trim();
		if (line.length() > 0 && !line.startsWith("PID")) {
		    StringTokenizer tok = new StringTokenizer(line);
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

		    try {
			switch(session.getFlavor()) {
			  case MACOSX:
			    process.sessionId.setStatus(StatusEnumeration.NOT_COLLECTED);
			    process.schedulingClass.setStatus(StatusEnumeration.NOT_COLLECTED);
			    break;
    
			  case AIX: {
			    process.sessionId.setValue(tok.nextToken());
			    process.sessionId.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    String s = tok.nextToken();
			    try {
				Long.parseLong(s);
				//
				// If this is a number, then there's a missing line continuation at this point.
				//
				process.command.setValue("?");
				StringBuffer pseudoLine = new StringBuffer(s);
				pseudoLine.append(" ").append(tok.nextToken("\n"));
				throw new LineInsertionException(pseudoLine.toString());
			    } catch (NumberFormatException e) {
				process.schedulingClass.setValue(s);
			    }
			    break;
			  }
    
			  default:
			    process.sessionId.setValue(tok.nextToken());
			    process.sessionId.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    process.schedulingClass.setValue(tok.nextToken());
			    break;
			}
			process.execTime.setValue(tok.nextToken());

			String stime=null, cmd=null;
			switch(session.getFlavor()) {
			  case MACOSX:
			  case LINUX:
			    stime = tok.nextToken();
			    cmd = tok.nextToken("\n").trim();
			    break;

			  default: {
			    String rem = tok.nextToken("\n").trim();
			    stime = rem.substring(0, 8); // XX:XX:XX or MMM_dd format
			    cmd = null;
			    if (stime.indexOf(":") == -1) {
				stime = rem.substring(0, 7);
				cmd = rem.substring(7).trim();
			    } else {
				cmd = rem.substring(8).trim();
			    }
			    break;
			  }

			}
			process.startTime.setValue(stime);
			process.command.setValue(cmd);
		    } catch (LineInsertionException e) {
			Vector<String> v = new Vector<String>();
			for (String temp : lines) {
			    v.add(temp);
			}
			v.insertElementAt(e.getMessage(), i+1);
			lines = v;
		    }
		    processes.add(process);
		}
	    }

	    //
	    // On Linux, we can gather security information about the processes as well.
	    //
	    Hashtable<String, String> labels = new Hashtable<String, String>();
	    if (session.getFlavor() == IUnixSession.Flavor.LINUX) {
	        lines = SafeCLI.multiLine("ps axZ", session, IUnixSession.Timeout.S);
		int index = 0;
		for (; index < lines.size() && lines.get(index).length() == 0; index++) {
		    // find the first non-empty line
		}
		if (lines.size() > index) {
		    int labelIndex = -1, pidIndex = -1;
		    StringTokenizer tok = new StringTokenizer(lines.get(index++));
		    for (int i=0; tok.hasMoreTokens(); i++) {
			String header = tok.nextToken();
			if ("LABEL".equals(header)) {
			    labelIndex = i;
			} else if ("PID".equals(header)) {
			    pidIndex = i;
			}
		    }

		    if (labelIndex != -1 && pidIndex != -1) {
			for (; index < lines.size(); index++) {
			    String line = lines.get(index);
			    tok = new StringTokenizer(line);

			    String label=null, pid=null;
			    for (int i=0; tok.hasMoreTokens(); i++) {
				String token = tok.nextToken();
				if (i == labelIndex) {
				    label = token;
				} else if (i == pidIndex) {
				    pid = token;
				}
			    }

			    if (label != null && pid != null) {
				labels.put(pid, label);
			    }
			}
		    }
		}
	    }

	    //
	    // On Linux, we can collect the loginuid for each process from the /proc filesystem.  So check for that,
	    // and add the security information that was just collected at the same time.
	    //
	    for (ProcessData process : processes) {
		if (session.getFlavor() == IUnixSession.Flavor.LINUX) {
		    String pid = (String)process.pid.getValue();

		    if (labels.containsKey(pid)) {
			StringTokenizer tok = new StringTokenizer(labels.get(pid), ":");
			while (tok.hasMoreTokens()) {
			    EntityItemStringType labelType = Factories.sc.core.createEntityItemStringType();
			    labelType.setValue(tok.nextToken());
			    process.selinuxDomainLabel.add(labelType);
			}
		    }

		    String luidPath = "/proc/" + pid + "/loginuid";
		    try {
			IFile f = session.getFilesystem().getFile(luidPath);
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
			}
		    } catch (FileNotFoundException e) {
			String reason = JOVALMsg.getMessage(JOVALMsg.ERROR_IO_NOT_FILE);
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_IO, luidPath, reason));
			process.loginuid.setStatus(StatusEnumeration.NOT_COLLECTED);
		    }
		} else {
		    process.loginuid.setStatus(StatusEnumeration.NOT_COLLECTED);
		}
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }

    class LineInsertionException extends Exception {
	LineInsertionException(String newLine) {
	    super(newLine);
	}
    }

    class ProcessData {
	EntityItemStringType command, tty, startTime, execTime, schedulingClass;
	EntityItemIntType ruid, userid, priority, ppid, pid, loginuid, sessionId;
	EntityItemBoolType execShield;
	List<EntityItemCapabilityType> posixCapability;
	List<EntityItemStringType> selinuxDomainLabel;

	ProcessData() {
	    command = Factories.sc.core.createEntityItemStringType();
	    tty = Factories.sc.core.createEntityItemStringType();
	    startTime = Factories.sc.core.createEntityItemStringType();
	    execTime = Factories.sc.core.createEntityItemStringType();
	    schedulingClass = Factories.sc.core.createEntityItemStringType();
	    ruid = Factories.sc.core.createEntityItemIntType();
	    userid = Factories.sc.core.createEntityItemIntType();
	    priority = Factories.sc.core.createEntityItemIntType();
	    ppid = Factories.sc.core.createEntityItemIntType();
	    pid = Factories.sc.core.createEntityItemIntType();

	    //
	    // Process58Item additions
	    //
	    sessionId = Factories.sc.core.createEntityItemIntType();
	    loginuid = Factories.sc.core.createEntityItemIntType();
	    execShield = Factories.sc.core.createEntityItemBoolType();
	    execShield.setStatus(StatusEnumeration.NOT_COLLECTED);
	    posixCapability = new Vector<EntityItemCapabilityType>();
	    selinuxDomainLabel = new Vector<EntityItemStringType>();
	}

	ProcessItem getProcessItem() {
	    ProcessItem process = Factories.sc.unix.createProcessItem();
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
	    Process58Item process = Factories.sc.unix.createProcess58Item();
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
