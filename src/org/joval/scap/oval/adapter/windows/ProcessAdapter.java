// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.util.IProperty;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.Timestamp;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.util.Base64;
import jsaf.util.IniFile;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.windows.ProcessObject;
import scap.oval.definitions.windows.Process58Object;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.windows.ProcessItem;

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
public class ProcessAdapter implements IAdapter {
    private IWindowsSession session;
    private IniFile processes;
    private MessageType error;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    classes.add(ProcessObject.class);
	    classes.add(Process58Object.class);
	} else {
	    notapplicable.add(ProcessObject.class);
	    notapplicable.add(Process58Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	if (error != null) {
	    rc.addMessage(error);
	}
	if (obj instanceof Process58Object) {
	    return getItems((Process58Object)obj, rc);
	} else {
	    return getItems((ProcessObject)obj, rc);
	}
    }

    // Private

    private Collection<ProcessItem> getItems(ProcessObject pObj, IRequestContext rc) throws CollectException {
	Collection<ProcessItem> items = new ArrayList<ProcessItem>();
	for (Map.Entry<String, IProperty> entry : getProcesses(pObj.getCommandLine()).entrySet()) {
	    int processId = Integer.parseInt(entry.getKey());
	    IProperty props = entry.getValue();
	    items.add(makeItem(processId, props));
	}
	return items;
    }

    private Collection<ProcessItem> getItems(Process58Object pObj, IRequestContext rc) throws CollectException {
	int pid = Integer.parseInt((String)pObj.getPid().getValue());
	OperationEnumeration op = pObj.getPid().getOperation();
	Collection<ProcessItem> items = new ArrayList<ProcessItem>();
	for (Map.Entry<String, IProperty> entry : getProcesses(pObj.getCommandLine()).entrySet()) {
	    int processId = Integer.parseInt(entry.getKey());
	    IProperty props = entry.getValue();
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

    /**
     * Get a map of processes based on the CommandLine.
     */
    private Map<String, IProperty> getProcesses(EntityObjectStringType commandLineType) throws CollectException {
	HashMap<String, IProperty> map = new HashMap<String, IProperty>();
	OperationEnumeration op = commandLineType.getOperation();
	String commandLine = (String)commandLineType.getValue();
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
		Pattern pattern = StringTools.pattern(commandLine);
		for (String pid : processes.listSections()) {
		    IProperty props = processes.getSection(pid);
		    Matcher m = pattern.matcher(props.getProperty("CommandLine"));
		    if (m.find()) {
			map.put(pid, props);
		    }
		}
	    } catch (PatternSyntaxException e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage());
		throw new CollectException(msg, FlagEnumeration.ERROR);
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}
	return map;
    }

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
		depEnabled.setValue(prop.getBooleanProperty(key) ? "1" : "0");
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
	    try {
		IRunspace runspace = session.getRunspacePool().getRunspace();
		runspace.loadAssembly(getClass().getResourceAsStream("Process.dll"));
		runspace.loadModule(getClass().getResourceAsStream("Process.psm1"));
		String data = runspace.invoke("Get-ProcessInfo | Transfer-Encode");
		if (data != null) {
		    ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(data));
		    processes.load(in, StringTools.UTF8);
		}
	    } catch (Exception e) {
		error = Factories.common.createMessageType();
		error.setLevel(MessageLevelEnumeration.ERROR);
		error.setValue(e.getMessage());
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
    }
}
