// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates ProcessTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ProcessAdapter implements IAdapter {
    private ISession session;
    private Hashtable<String,ProcessItem> processes;
    private String error = null;

    public ProcessAdapter(ISession session) {
	this.session = session;
	processes = new Hashtable<String, ProcessItem>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return ProcessObject.class;
    }

    public boolean connect() {
	if (session != null) {
	    scanProcesses();
	    return true;
	}
	return false;
    }

    public void disconnect() {
    }

    public List<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	ProcessObject pObj = (ProcessObject)rc.getObject();
	List<JAXBElement <? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

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
		msg.setValue(JOVALSystem.getMessage("ERROR_PATTERN", e.getMessage()));
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

	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", pObj.getCommand().getOperation()));
	}

	return items;
    }

    // Internal

    /**
     * REMIND: Stops if it encounters any exceptions at all; make this more robust?
     */
    private void scanProcesses() {
	try {
	    IProcess p = session.createProcess("ps -e -o pid,ppid,pri,uid,ruid,tty,class,time,stime,args");
	    p.start();
	    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
	    String line = br.readLine(); // skip over the header row.
	    while((line = br.readLine()) != null) {
		StringTokenizer tok = new StringTokenizer(line);
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
		schedulingClass.setValue(tok.nextToken());
		process.setSchedulingClass(schedulingClass);

		EntityItemStringType execTime = JOVALSystem.factories.sc.core.createEntityItemStringType();
		execTime.setValue(tok.nextToken());
		process.setExecTime(execTime);

		EntityItemStringType startTime = JOVALSystem.factories.sc.core.createEntityItemStringType();
		startTime.setValue(tok.nextToken());
		process.setStartTime(startTime);

		EntityItemStringType command = JOVALSystem.factories.sc.core.createEntityItemStringType();
		String args = tok.nextToken("\n").trim();
		command.setValue(args);
		process.setCommand(command);

		processes.put(args, process);
	    }
	    br.close();
	} catch (Exception e) {
	    error = e.getMessage();
	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	}
    }
}
