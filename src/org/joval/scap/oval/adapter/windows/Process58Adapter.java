// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.windows.Process58Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.windows.ProcessItem;

import jsaf.intf.system.IBaseSession;
import jsaf.intf.util.IProperty;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.Timestamp;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.util.IniFile;
import jsaf.util.StringTools;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Retrieves windows:process58_items.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Process58Adapter implements IAdapter {
    private IWindowsSession session;
    private IRunspace runspace;
    private IniFile processes;
    private MessageType error;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(Process58Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	if (error != null) {
	    rc.addMessage(error);
	}

	//
	// First, create a map of processes based on the CommandLine
	//
	Process58Object pObj = (Process58Object)obj;
	HashMap<String, IProperty> map = new HashMap<String, IProperty>();
	OperationEnumeration op = pObj.getCommandLine().getOperation();
	String commandLine = (String)pObj.getCommandLine().getValue();
	switch(op) {
	  case EQUALS:
	  case CASE_INSENSITIVE_EQUALS:
	  case NOT_EQUAL:
	    for (String pid : processes.listSections()) {
		IProperty props = processes.getSection(pid);
		if (op == OperationEnumeration.EQUALS) {
		    if (commandLine.equals(props.getProperty("CommandLine"))) {
			map.put(pid, props);
		    }
		} else if (op == OperationEnumeration.CASE_INSENSITIVE_EQUALS &&
			   commandLine.equalsIgnoreCase(props.getProperty("CommandLine"))) {
		    map.put(pid, props);
		} else if (op == OperationEnumeration.NOT_EQUAL && !commandLine.equals(props.getProperty("CommandLine"))) {
		    map.put(pid, props);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    try {
		Pattern pattern = Pattern.compile(commandLine);
		for (String pid : processes.listSections()) {
		    IProperty props = processes.getSection(pid);
		    Matcher m = pattern.matcher(props.getProperty("CommandLine"));
		    if (m.find()) {
			map.put(pid, props);
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

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	//
	// Then, filter the map based on the pid value
	//
	int pid = Integer.parseInt((String)pObj.getPid().getValue());
	op = pObj.getPid().getOperation();
	Collection<ProcessItem> items = new Vector<ProcessItem>();
	for (String key : map.keySet()) {
	    int processId = Integer.parseInt(key);
	    IProperty props = map.get(key);
	    switch(op) {
	      case EQUALS:
		if (processId == pid) {
		    items.add(makeItem(processId, props));
		}
		break;
	      case NOT_EQUAL:
		if (processId != pid) {
		    items.add(makeItem(processId, props));
		}
		break;
	      case LESS_THAN:
		if (processId < pid) {
		    items.add(makeItem(processId, props));
		}
		break;
	      case LESS_THAN_OR_EQUAL:
		if (processId <= pid) {
		    items.add(makeItem(processId, props));
		}
		break;
	      case GREATER_THAN:
		if (processId > pid) {
		    items.add(makeItem(processId, props));
		}
		break;
	      case GREATER_THAN_OR_EQUAL:
		if (processId >= pid) {
		    items.add(makeItem(processId, props));
		}
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	return items;
    }

    // Private

    private ProcessItem makeItem(int pid, IProperty prop) {
	ProcessItem item = Factories.sc.windows.createProcessItem();
	EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
	pidType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	pidType.setValue(Integer.toString(pid));
	item.setPid(pidType);

	for (String key : prop) {
	    if ("CommandLine".equals(key)) {
		EntityItemStringType commandLine = Factories.sc.core.createEntityItemStringType();
		commandLine.setValue(prop.getProperty(key));
		item.setCommandLine(commandLine);
	    } else if ("PPID".equals(key)) {
		EntityItemIntType ppid = Factories.sc.core.createEntityItemIntType();
		ppid.setDatatype(SimpleDatatypeEnumeration.INT.value());
		ppid.setValue(prop.getProperty(key));
		item.setPpid(ppid);
	    } else if ("Priority".equals(key)) {
		EntityItemStringType priority = Factories.sc.core.createEntityItemStringType();
		priority.setValue(prop.getProperty(key));
		item.setPriority(priority);
	    } else if ("Path".equals(key)) {
		int ptr = prop.getProperty(key).lastIndexOf("\\");
		if (ptr != -1) {
		    EntityItemStringType imagePath = Factories.sc.core.createEntityItemStringType();
		    imagePath.setValue(prop.getProperty(key).substring(ptr+1));
		    item.setImagePath(imagePath);

		    EntityItemStringType currentDir = Factories.sc.core.createEntityItemStringType();
		    currentDir.setValue(prop.getProperty(key).substring(0,ptr));
		    item.setCurrentDir(currentDir);
		}
	    } else if ("CreationDate".equals(key)) {
		try {
		    EntityItemIntType creationTime = Factories.sc.core.createEntityItemIntType();
		    creationTime.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    creationTime.setValue(Timestamp.toWindowsTimestamp(prop.getProperty(key)).toString());
		    item.setCreationTime(creationTime);
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    } else if ("DepEnabled".equals(key)) {
		EntityItemBoolType depEnabled = Factories.sc.core.createEntityItemBoolType();
		depEnabled.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		depEnabled.setValue(prop.getProperty(key));
		item.setDepEnabled(depEnabled);
	    } else if ("PrimaryWindowText".equals(key)) {
		EntityItemStringType primaryWindowText = Factories.sc.core.createEntityItemStringType();
		primaryWindowText.setValue(prop.getProperty(key));
		item.setPrimaryWindowText(primaryWindowText);
	    }
	}
	return item;
    }

    /**
     * Initialize the adapter by retrieving all the data about running processes.
     *
     * Idempotent.
     */
    private void init() throws CollectException {
	if (processes == null) {
	    processes = new IniFile();
	} else {
	    return;
	}

	//
	// Get a runspace if there are any in the pool, or create a new one, and load the Get-ProcessInfo
	// Powershell module code.
	//
	IWindowsSession.View view = session.getNativeView();
	for (IRunspace rs : session.getRunspacePool().enumerate()) {
	    if (rs.getView() == view) {
		runspace = rs;
		break;
	    }
	}
	try {
	    if (runspace == null) {
		runspace = session.getRunspacePool().spawn(view);
	    }
	    if (runspace != null) {
		runspace.loadModule(getClass().getResourceAsStream("Process58.psm1"));
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	if (runspace == null) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_POWERSHELL), FlagEnumeration.NOT_COLLECTED);
	}
	try {
	    String data = runspace.invoke("Get-ProcessInfo");
	    if (data != null) {
		ByteArrayInputStream in = new ByteArrayInputStream(data.getBytes());
		processes.load(in, StringTools.ASCII);
	    }
	} catch (Exception e) {
	    error = Factories.common.createMessageType();
	    error.setLevel(MessageLevelEnumeration.ERROR);
	    error.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION, e.getMessage()));
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }
}
