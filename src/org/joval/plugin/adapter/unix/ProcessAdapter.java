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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.ExistenceEnumeration;
import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.EntityStateIntType;
import oval.schemas.definitions.core.EntityStateStringType;
import oval.schemas.definitions.core.ObjectComponentType;
import oval.schemas.definitions.core.ObjectRefType;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.core.StateRefType;
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
import oval.schemas.systemcharacteristics.unix.ObjectFactory;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.results.core.TestedItemType;
import oval.schemas.results.core.TestedVariableType;
import oval.schemas.results.core.TestType;

import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IAdapterContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.ISystemCharacteristics;
import org.joval.oval.OvalException;
import org.joval.util.JOVALSystem;

/**
 * Evaluates ProcessTest OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ProcessAdapter implements IAdapter {
    private IAdapterContext ctx;
    private IDefinitions definitions;
    private ISession session;
    private oval.schemas.systemcharacteristics.core.ObjectFactory coreFactory;
    private ObjectFactory unixFactory;
    private Hashtable<String,ProcessItem> processes;

    public ProcessAdapter(ISession session) {
	this.session = session;
	coreFactory = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	unixFactory = new ObjectFactory();
	processes = new Hashtable<String, ProcessItem>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return ProcessObject.class;
    }

    public Class getTestClass() {
	return ProcessTest.class;
    }

    public Class getStateClass() {
	return ProcessState.class;
    }

    public Class getItemClass() {
	return ProcessItem.class;
    }

    public void init(IAdapterContext ctx) {
	this.ctx = ctx;
	definitions = ctx.getDefinitions();
    }

    public void scan(ISystemCharacteristics sc) throws OvalException {
	ctx.status("Retrieving process list");
	scanProcesses();

	Iterator<ObjectType> iter = definitions.iterateObjects(ProcessObject.class);
	while (iter.hasNext()) {
	    ProcessObject pObj = (ProcessObject)iter.next();
	    ctx.status(pObj.getId());
	    ProcessItem item = null;
	    switch (pObj.getCommand().getOperation()) {
	      case EQUALS:
		item = processes.get((String)pObj.getCommand().getValue());
		break;

	      case CASE_INSENSITIVE_EQUALS: {
		String command = (String)pObj.getCommand().getValue();
		for (String key : processes.keySet()) {
		    if (key.equalsIgnoreCase(command)) {
			item = processes.get(key);
			break;
		    }
		}
		break;
	      }

	      case PATTERN_MATCH: {
		String command = (String)pObj.getCommand().getValue();
		for (String key : processes.keySet()) {
		    if (Pattern.compile(command).matcher(key).find()) {
			item = processes.get(key);
			break;
		    }
		}
		break;
	      }

	      default:
		throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION",
							       pObj.getCommand().getOperation()));
	    }
	    if (item == null) {
		sc.setObject(pObj.getId(), pObj.getComment(), pObj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, null);
	    } else {
		sc.setObject(pObj.getId(), pObj.getComment(), pObj.getVersion(), FlagEnumeration.COMPLETE, null);
		BigInteger itemId = sc.storeItem(unixFactory.createProcessItem(item));
		sc.relateItem(pObj.getId(), itemId);
	    }
	}
    }

    public String getItemData(ObjectComponentType object, ISystemCharacteristics sc) throws OvalException {
	return null; // What foolish variable would point to a ProcessObject?
    }

    public ResultEnumeration compare(StateType st, ItemType it) throws OvalException {
	ProcessState state = (ProcessState)st;
	ProcessItem item = (ProcessItem)it;

        if (state.isSetPid()) {
            if (compareTypes(state.getPid(), item.getPid())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetPpid()) {
            if (compareTypes(state.getPpid(), item.getPpid())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetCommand()) {
            if (compareTypes(state.getCommand(), item.getCommand())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetPriority()) {
            if (compareTypes(state.getPriority(), item.getPriority())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetExecTime()) {
            if (compareTypes(state.getExecTime(), item.getExecTime())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetRuid()) {
            if (compareTypes(state.getRuid(), item.getRuid())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetSchedulingClass()) {
            if (compareTypes(state.getSchedulingClass(), item.getSchedulingClass())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetStartTime()) {
            if (compareTypes(state.getStartTime(), item.getStartTime())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetTty()) {
            if (compareTypes(state.getTty(), item.getTty())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else if (state.isSetUserId()) {
            if (compareTypes(state.getUserId(), item.getUserId())) {
                return ResultEnumeration.TRUE;
            } else {
                return ResultEnumeration.FALSE;
            }
        } else {
            throw new OvalException(JOVALSystem.getMessage("ERROR_PROCESS_STATE_EMPTY", state.getId()));
        }
    }

    // Internal

    private boolean compareTypes(EntityStateIntType state, EntityItemIntType item) throws OvalException {
	int itemVal = Integer.parseInt((String)item.getValue());
	int stateVal = Integer.parseInt((String)state.getValue());

	switch (state.getOperation()) {
	  case EQUALS:
	    return stateVal == itemVal;
	  case LESS_THAN:
	    return stateVal < itemVal;
	  case LESS_THAN_OR_EQUAL:
	    return stateVal <= itemVal;
	  case GREATER_THAN:
	    return stateVal > itemVal;
	  case GREATER_THAN_OR_EQUAL:
	    return stateVal >= itemVal;
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }

    private boolean compareTypes(EntityStateStringType state, EntityItemStringType item) throws OvalException {
	switch (state.getOperation()) {
	  case CASE_INSENSITIVE_EQUALS:
	    return ((String)item.getValue()).equalsIgnoreCase((String)state.getValue());
	  case EQUALS:
	    return ((String)item.getValue()).equals((String)state.getValue());
	  case PATTERN_MATCH:
	    return Pattern.compile((String)state.getValue()).matcher((String)item.getValue()).find();
	  default:
	    throw new OvalException(JOVALSystem.getMessage("ERROR_UNSUPPORTED_OPERATION", state.getOperation()));
	}
    }

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
		ProcessItem process = unixFactory.createProcessItem();

		EntityItemIntType pid = coreFactory.createEntityItemIntType();
		pid.setValue(tok.nextToken());
		process.setPid(pid);

		EntityItemIntType ppid = coreFactory.createEntityItemIntType();
		ppid.setValue(tok.nextToken());
		process.setPpid(ppid);

		EntityItemIntType priority = coreFactory.createEntityItemIntType();
		priority.setValue(tok.nextToken());
		process.setPriority(priority);

		EntityItemIntType userid = coreFactory.createEntityItemIntType();
		userid.setValue(tok.nextToken());
		process.setUserId(userid);

		EntityItemIntType ruid = coreFactory.createEntityItemIntType();
		ruid.setValue(tok.nextToken());
		process.setRuid(ruid);

		EntityItemStringType tty = coreFactory.createEntityItemStringType();
		tty.setValue(tok.nextToken());
		process.setTty(tty);

		EntityItemStringType schedulingClass = coreFactory.createEntityItemStringType();
		schedulingClass.setValue(tok.nextToken());
		process.setSchedulingClass(schedulingClass);

		EntityItemStringType execTime = coreFactory.createEntityItemStringType();
		execTime.setValue(tok.nextToken());
		process.setExecTime(execTime);

		EntityItemStringType startTime = coreFactory.createEntityItemStringType();
		startTime.setValue(tok.nextToken());
		process.setStartTime(startTime);

		EntityItemStringType command = coreFactory.createEntityItemStringType();
		String args = tok.nextToken("\n").trim();
		command.setValue(args);
		process.setCommand(command);

		processes.put(args, process);
	    }
	    br.close();
	} catch (Exception e) {
	    JOVALSystem.getLogger().log(Level.SEVERE, e.getMessage(), e);
	}
    }
}
