// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

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
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.EntityStateIntType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateType;
import oval.schemas.definitions.unix.ProcessObject;
import oval.schemas.definitions.unix.ProcessState;
import oval.schemas.definitions.unix.ProcessTest;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.ProcessItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.NotCollectableException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.SafeCLI;

/**
 * Evaluates ProcessTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ProcessAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String,ProcessItem> processes;
    private String error = null;

    public ProcessAdapter(IUnixSession session) {
	this.session = session;
	processes = new Hashtable<String, ProcessItem>();
    }

    // Implement IAdapter

    private static Class[] objectClasses = {ProcessObject.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public boolean connect() {
	if (session == null) {
	    return false;
	} else {
	    return scanProcesses();
	}
    }

    public void disconnect() {
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws NotCollectableException {
	ProcessObject pObj = (ProcessObject)rc.getObject();
	Collection<JAXBElement <? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	if (error != null) {
	    MessageType msg = JOVALSystem.factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    rc.addMessage(msg);
	}

	switch (pObj.getCommand().getOperation()) {
	  case EQUALS: {
	    ProcessItem item = processes.get((String)pObj.getCommand().getValue());
	    if (item != null) {
		items.add(JOVALSystem.factories.sc.unix.createProcessItem(item));
	    }
	    break;
	  }

	  case CASE_INSENSITIVE_EQUALS: {
	    String command = (String)pObj.getCommand().getValue();
	    for (String key : processes.keySet()) {
		if (key.equalsIgnoreCase(command)) {
		    items.add(JOVALSystem.factories.sc.unix.createProcessItem(processes.get(key)));
		}
	    }
	    break;
	  }

	  case PATTERN_MATCH: {
	    try {
		String command = (String)pObj.getCommand().getValue();
		for (String key : processes.keySet()) {
		    if (Pattern.compile(command).matcher(key).find()) {
			items.add(JOVALSystem.factories.sc.unix.createProcessItem(processes.get(key)));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.WARNING);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
	    }
	    break;
	  }

	  case NOT_EQUAL: {
	    String command = (String)pObj.getCommand().getValue();
	    for (String key : processes.keySet()) {
		if (!command.equals(key)) {
		    items.add(JOVALSystem.factories.sc.unix.createProcessItem(processes.get(key)));
		}
	    }
	    break;
	  }

	  default: {
	    String s = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, pObj.getCommand().getOperation());
	    throw new NotCollectableException(s);
	  }
	}

	return items;
    }

    // Internal

    /**
     * REMIND: Stops if it encounters any exceptions at all; make this more robust?
     */
    private boolean scanProcesses() {
	String args = null;
	switch(session.getFlavor()) {
	  case MACOSX:
	    args = "ps -A -o pid,ppid,pri,uid,ruid,tty,time,stime,command";
	    break;

	  case LINUX:
	  case SOLARIS:
	    args = "ps -e -o pid,ppid,pri,uid,ruid,tty,class,time,stime,args";
	    break;

	  default:
	    return false;
	}
	try {
	    List<String> lines = SafeCLI.multiLine(args, session, IUnixSession.TIMEOUT_S);
	    for (int i=1; i < lines.size(); i++) { // skip the header at line 0
		StringTokenizer tok = new StringTokenizer(lines.get(i));
		ProcessItem process = JOVALSystem.factories.sc.unix.createProcessItem();

		EntityItemIntType pid = JOVALSystem.factories.sc.core.createEntityItemIntType();
		pid.setValue(tok.nextToken());
		pid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.setPid(pid);

		EntityItemIntType ppid = JOVALSystem.factories.sc.core.createEntityItemIntType();
		ppid.setValue(tok.nextToken());
		ppid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.setPpid(ppid);

		EntityItemIntType priority = JOVALSystem.factories.sc.core.createEntityItemIntType();
		priority.setValue(tok.nextToken());
		priority.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.setPriority(priority);

		EntityItemIntType userid = JOVALSystem.factories.sc.core.createEntityItemIntType();
		userid.setValue(tok.nextToken());
		userid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.setUserId(userid);

		EntityItemIntType ruid = JOVALSystem.factories.sc.core.createEntityItemIntType();
		ruid.setValue(tok.nextToken());
		ruid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		process.setRuid(ruid);

		EntityItemStringType tty = JOVALSystem.factories.sc.core.createEntityItemStringType();
		tty.setValue(tok.nextToken());
		process.setTty(tty);

		EntityItemStringType schedulingClass = JOVALSystem.factories.sc.core.createEntityItemStringType();
		switch(session.getFlavor()) {
		  case MACOSX:
		    schedulingClass.setStatus(StatusEnumeration.NOT_COLLECTED);
		    break;
		  default:
		    schedulingClass.setValue(tok.nextToken());
		    break;
		}
		process.setSchedulingClass(schedulingClass);

		EntityItemStringType execTime = JOVALSystem.factories.sc.core.createEntityItemStringType();
		execTime.setValue(tok.nextToken());
		process.setExecTime(execTime);

		EntityItemStringType startTime = JOVALSystem.factories.sc.core.createEntityItemStringType();
		startTime.setValue(tok.nextToken());
		process.setStartTime(startTime);

		EntityItemStringType command = JOVALSystem.factories.sc.core.createEntityItemStringType();
		String cmd = tok.nextToken("\n").trim();
		command.setValue(cmd);
		process.setCommand(command);

		processes.put(cmd, process);
	    }
	} catch (Exception e) {
	    error = e.getMessage();
	    session.getLogger().error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return true;
    }
}
