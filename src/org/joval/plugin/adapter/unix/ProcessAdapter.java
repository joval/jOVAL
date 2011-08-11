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
import oval.schemas.systemcharacteristics.core.VariableValueType;
import oval.schemas.systemcharacteristics.unix.ProcessItem;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
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
    private IAdapterContext ctx;
    private ISession session;
    private Hashtable<String,ProcessItem> processes;
    private String error = null;

    public ProcessAdapter(ISession session) {
	this.session = session;
	processes = new Hashtable<String, ProcessItem>();
    }

    // Implement IAdapter

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
    }

    public Class getObjectClass() {
	return ProcessObject.class;
    }

    public Class getStateClass() {
	return ProcessState.class;
    }

    public Class getItemClass() {
	return ProcessItem.class;
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

    public List<JAXBElement<? extends ItemType>> getItems(ObjectType obj, List<VariableValueType> vars) throws OvalException {
	ProcessObject pObj = (ProcessObject)obj;
	List<JAXBElement <? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();

	if (error != null) {
	    MessageType msg = new MessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(error);
	    ctx.addObjectMessage(obj.getId(), msg);
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
	    String command = (String)pObj.getCommand().getValue();
	    for (String key : processes.keySet()) {
		if (Pattern.compile(command).matcher(key).find()) {
		    items.add(JOVALSystem.factories.sc.unix.createProcessItem(processes.get(key)));
		}
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


    public ResultEnumeration compare(StateType st, ItemType it) throws TestException, OvalException {
	ProcessState state = (ProcessState)st;
	ProcessItem item = (ProcessItem)it;

        if (state.isSetPid()) {
            ResultEnumeration result = ctx.test(state.getPid(), item.getPid());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetPpid()) {
            ResultEnumeration result = ctx.test(state.getPpid(), item.getPpid());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetCommand()) {
            ResultEnumeration result = ctx.test(state.getCommand(), item.getCommand());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetPriority()) {
            ResultEnumeration result = ctx.test(state.getPriority(), item.getPriority());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetExecTime()) {
            ResultEnumeration result = ctx.test(state.getExecTime(), item.getExecTime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetRuid()) {
            ResultEnumeration result = ctx.test(state.getRuid(), item.getRuid());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetSchedulingClass()) {
            ResultEnumeration result = ctx.test(state.getSchedulingClass(), item.getSchedulingClass());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetStartTime()) {
            ResultEnumeration result = ctx.test(state.getStartTime(), item.getStartTime());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetTty()) {
            ResultEnumeration result = ctx.test(state.getTty(), item.getTty());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        } else if (state.isSetUserId()) {
            ResultEnumeration result = ctx.test(state.getUserId(), item.getUserId());
	    if (result != ResultEnumeration.TRUE) {
		return result;
	    }
        }
	return ResultEnumeration.TRUE;
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
