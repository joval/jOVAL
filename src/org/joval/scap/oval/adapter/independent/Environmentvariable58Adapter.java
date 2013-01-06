// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.independent;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.Environmentvariable58Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemAnySimpleType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.independent.Environmentvariable58Item;
import oval.schemas.results.core.ResultEnumeration;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IReader;
import jsaf.intf.system.IBaseSession;
import jsaf.intf.system.IEnvironment;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.provider.windows.powershell.PowershellException;
import jsaf.util.Environment;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environmentvariable58Adapter implements IAdapter {
    private ISession session;
    private IEnvironmentBuilder builder;
    private String error;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof ISession) {
	    this.session = (ISession)session;
	    classes.add(Environmentvariable58Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (error != null) {
	    throw new CollectException(error, FlagEnumeration.NOT_COLLECTED);
	} else if (builder == null) {
	    try {
		builder = getEnvironmentBuilder();
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		if (e.getMessage() == null) {
		    error = e.getClass().getName();
		} else {
		    error = e.getMessage();
		}
		throw new CollectException(error, FlagEnumeration.NOT_COLLECTED);
	    }
	}

	//
	// First, create a map of process environments matching the specified PID
	//
	Environmentvariable58Object eObj = (Environmentvariable58Object)obj;
	HashMap<String, IEnvironment> environments = new HashMap<String, IEnvironment>();
	if (eObj.isSetPid() && !eObj.getPid().isNil()) {
	    OperationEnumeration op = eObj.getPid().getValue().getOperation();
	    String pid = (String)eObj.getPid().getValue().getValue();
	    try {
		switch(op) {
		  case EQUALS:
		    environments.put(pid, builder.getProcessEnvironment(Integer.parseInt(pid)));
		    break;
		  case NOT_EQUAL:
		    for (int i : builder.listProcesses()) {
			if (i != Integer.parseInt(pid)) {
			    environments.put(Integer.toString(i), builder.getProcessEnvironment(i));
			}
		    }
		    break;
		  case GREATER_THAN:
		    for (int i : builder.listProcesses()) {
			if (i > Integer.parseInt(pid)) {
			    environments.put(Integer.toString(i), builder.getProcessEnvironment(i));
			}
		    }
		    break;
		  case GREATER_THAN_OR_EQUAL:
		    for (int i : builder.listProcesses()) {
			if (i >= Integer.parseInt(pid)) {
			    environments.put(Integer.toString(i), builder.getProcessEnvironment(i));
			}
		    }
		    break;
		  case LESS_THAN:
		    for (int i : builder.listProcesses()) {
			if (i < Integer.parseInt(pid)) {
			    environments.put(Integer.toString(i), builder.getProcessEnvironment(i));
			}
		    }
		    break;
		  case LESS_THAN_OR_EQUAL:
		    for (int i : builder.listProcesses()) {
			if (i <= Integer.parseInt(pid)) {
			    environments.put(Integer.toString(i), builder.getProcessEnvironment(i));
			}
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } catch (CollectException e) {
		throw e;
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	} else if (session.getHostname().equals(IBaseSession.LOCALHOST)) {
	    //
	    // In this case, we're supposed to use the tool's PID, and by extension, the tool's environment.
	    // To get the PID, we use a hack to obtain it from the name of the RuntimeMXBean.
	    //
	    String name = ManagementFactory.getRuntimeMXBean().getName();
	    int ptr = name.indexOf("@");
	    if (ptr != -1) {
		try {
		int pid = Integer.parseInt(name.substring(0,ptr));
		    environments.put(Integer.toString(pid), builder.getProcessEnvironment(pid));
		} catch (Exception e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(e.getMessage());
		    rc.addMessage(msg);
		    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	} else {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.INFO);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_NO_PROCESS, session.getHostname()));
	    rc.addMessage(msg);
	}

	//
	// If no environments were found, then just quit now.
	//
	if (environments.size() == 0) {
	    @SuppressWarnings("unchecked")
	    Collection<Environmentvariable58Item> empty = (Collection<Environmentvariable58Item>)Collections.EMPTY_LIST;
	    return empty;
	}

	//
	// Then, filter the environment data according to the specified variable name.
	//
	Collection<Environmentvariable58Item> items = new ArrayList<Environmentvariable58Item>();
	if (eObj.isSetName() && eObj.getName().getValue() != null) {
	    OperationEnumeration op = eObj.getName().getOperation();
	    String name = (String)eObj.getName().getValue();
	    try {
		switch(op) {
		  case EQUALS:
		    for (Map.Entry<String, IEnvironment> env : environments.entrySet()) {
			String pid = env.getKey();
			for (String var : env.getValue()) {
			    if (name.equals(var)) {
				items.add(makeItem(pid, var, env.getValue().getenv(var)));
			    }
			}
		    }
		    break;
		  case CASE_INSENSITIVE_EQUALS:
		    for (Map.Entry<String, IEnvironment> env : environments.entrySet()) {
			String pid = env.getKey();
			for (String var : env.getValue()) {
			    if (name.equalsIgnoreCase(var)) {
				items.add(makeItem(pid, var, env.getValue().getenv(var)));
			    }
			}
		    }
		    break;
		  case NOT_EQUAL:
		    for (Map.Entry<String, IEnvironment> env : environments.entrySet()) {
			String pid = env.getKey();
			for (String var : env.getValue()) {
			    if (!name.equals(var)) {
				items.add(makeItem(pid, var, env.getValue().getenv(var)));
			    }
			}
		    }
		    break;
		  case PATTERN_MATCH:
		    Pattern p = Pattern.compile(name);
		    for (Map.Entry<String, IEnvironment> env : environments.entrySet()) {
			String pid = env.getKey();
			for (String var : env.getValue()) {
			    if (p.matcher(var).find()) {
				items.add(makeItem(pid, var, env.getValue().getenv(var)));
			    }
			}
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	} else {
	    for (Map.Entry<String, IEnvironment> env : environments.entrySet()) {
		for (String var : env.getValue()) {
		    items.add(makeItem(env.getKey(), var, env.getValue().getenv(var)));
		}
	    }
	}
	return items;
    }

    // Private

    private Environmentvariable58Item makeItem(String pid, String name, String value) {
	Environmentvariable58Item item = Factories.sc.independent.createEnvironmentvariable58Item();

	if (pid.length() > 0) { // handle the special case with no PID
	    EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
	    pidType.setValue(pid);
	    item.setPid(pidType);
	}

	EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
	nameType.setValue(name);
	item.setName(nameType);

	EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
	valueType.setValue(value);
	item.setValue(valueType);

	return item;
    }

    public interface IEnvironmentBuilder {
	IEnvironment getProcessEnvironment(int pid) throws Exception;
	int[] listProcesses() throws Exception;
    }

    /**
     * Create a platform-specific IEnvironmentBuilder.
     */
    private IEnvironmentBuilder getEnvironmentBuilder() throws Exception {
	switch(session.getType()) {
	  case UNIX:
	    IUnixSession us = (IUnixSession)session;
	    switch(us.getFlavor()) {
	      case AIX:
		return new AixEnvironmentBuilder(us);
	      case LINUX:
		return new LinuxEnvironmentBuilder(us);
	      case SOLARIS:
		return new SolarisEnvironmentBuilder(us);
	      default:
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor()));
	    }

	  case WINDOWS:
	    return new WindowsEnvironmentBuilder((IWindowsSession)session);

	  default:
	    throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_SESSION_TYPE, session.getType()));
	}
    }

    /**
     * See: http://yong321.freeshell.org/computer/ProcEnv.txt
     */
    abstract class UnixEnvironmentBuilder implements IEnvironmentBuilder {
	protected IUnixSession session;

	UnixEnvironmentBuilder(IUnixSession session) {
	    this.session = session;
	}

	public int[] listProcesses() throws Exception {
	    ArrayList<Integer> ids = new ArrayList<Integer>();
	    for (String line : SafeCLI.multiLine("ps -eo pid", session, IUnixSession.Timeout.S)) {
		if (!line.trim().equalsIgnoreCase("PID")) { // skip the header row
		    try {
			ids.add(new Integer(line.trim()));
		    } catch (NumberFormatException e) {
		    }
		}
	    }
	    int[] result = new int[ids.size()];
	    for (int i=0; i < result.length; i++) {
		result[i] = ids.get(i).intValue();
	    }
	    return result;
	}
    }

    class AixEnvironmentBuilder extends UnixEnvironmentBuilder {
	AixEnvironmentBuilder(IUnixSession session) {
	    super(session);
	}

	public IEnvironment getProcessEnvironment(int pid) throws Exception {
	    for (String line : SafeCLI.multiLine("ps eww " + pid, session, IUnixSession.Timeout.S)) {
		line = line.trim();
		if (line.startsWith(Integer.toString(pid))) {
		    StringTokenizer tok = new StringTokenizer(line);
		    Stack<String> stack = new Stack<String>();
		    while (tok.hasMoreTokens()) {
			stack.push(tok.nextToken());
		    }
		    Properties processEnv = new Properties();
		    while (!stack.empty()) {
			String token = stack.pop();
			int ptr = token.indexOf("=");
			if (ptr > 0) {
			    String key = token.substring(0,ptr);
			    String val = token.substring(ptr+1);
			    processEnv.setProperty(key, val);
			} else {
			    break; // no more environment variables
			}
		    }
		    return new Environment(processEnv);
		}
	    }
	    throw new NoSuchElementException(Integer.toString(pid));
	}
    }

    class LinuxEnvironmentBuilder extends UnixEnvironmentBuilder {
	LinuxEnvironmentBuilder(IUnixSession session) {
	    super(session);
	}

	public IEnvironment getProcessEnvironment(int pid) throws Exception {
	    String path = "/proc/" + pid + "/environ";
	    IReader reader = null;
	    IFile proc = session.getFilesystem().getFile(path);
	    if (proc.exists()) {
		Properties processEnv = new Properties();
		long timeout = session.getTimeout(IUnixSession.Timeout.M);
		byte[] bytes = SafeCLI.execData("cat " + path, null, session, timeout).getData();
		String data = new String(bytes, StringTools.ASCII);
		String delim = new StringBuffer().append((char)127).toString(); // 127 == delimiter char
		for (String pair : data.split(delim)) {
		    int ptr = pair.indexOf("=");
		    if (ptr > 0) {
			String key = pair.substring(0,ptr);
			String val = pair.substring(ptr+1);
			processEnv.setProperty(key, val);
		    }
		}
		return new Environment(processEnv);
	    } else {
		throw new NoSuchElementException(Integer.toString(pid));
	    }
	}
    }

    /**
     * In Solaris 10+, there is the pargs command:
     * http://www.unix.com/hp-ux/112024-how-can-i-get-environment-running-process.html
     */
    class SolarisEnvironmentBuilder extends UnixEnvironmentBuilder {
	SolarisEnvironmentBuilder(IUnixSession session) throws Exception {
	    super(session);
	    BigDecimal VER_5_9 = new BigDecimal("5.9");
	    BigDecimal osVersion = new BigDecimal(SafeCLI.exec("uname -r", session, IUnixSession.Timeout.S));
	    if (osVersion.compareTo(VER_5_9) < 0) {
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OS_VERSION, osVersion));
	    }
	}

	public IEnvironment getProcessEnvironment(int pid) throws Exception {
	    IFile proc = session.getFilesystem().getFile("/proc/" + pid);
	    if (proc.exists() && proc.isDirectory()) {
		Properties processEnv = new Properties();
		for (String line : SafeCLI.multiLine("pargs -e " + pid, session, IUnixSession.Timeout.S)) {
		    if (line.startsWith("envp")) {
			String pair = line.substring(line.indexOf(" ")).trim();
			int ptr = pair.indexOf("=");
			if (ptr > 0) {
			    String key = pair.substring(0,ptr);
			    String val = pair.substring(ptr+1);
			    processEnv.setProperty(key, val);
			}
		    }
		}
		return new Environment(processEnv);
	    } else {
		throw new NoSuchElementException(Integer.toString(pid));
	    }
	}
    }

    class WindowsEnvironmentBuilder implements IEnvironmentBuilder {
	private IRunspace runspace;

	WindowsEnvironmentBuilder(IWindowsSession session) {
	    //
	    // Get a runspace if there are any in the pool, or create a new one, and load the Get-Environmentvariable58
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
		    runspace.loadModule(getClass().getResourceAsStream("Environmentvariable58.psm1"));
		}
	    } catch (Exception e) {
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}

	public int[] listProcesses() throws Exception {
	    String data = runspace.invoke("foreach($process in Get-Process){$process.Id}");
	    ArrayList<Integer> ids = new ArrayList<Integer>();
	    if (data != null) {
		for (String id : data.split("\r\n")) {
		    try {
			ids.add(new Integer(id.trim()));
		    } catch (NumberFormatException e) {
		    }
		}
	    }
	    int[] result = new int[ids.size()];
	    for (int i=0; i < result.length; i++) {
		result[i] = ids.get(i).intValue();
	    }
	    return result;
	}

	public IEnvironment getProcessEnvironment(int pid) throws Exception {
	    String data = runspace.invoke("Get-ProcessEnvironment " + pid);
	    Properties processEnv = new Properties();
	    if (data != null) {
		String var = null;
		for (String line : data.split("\r\n")) {
		    int ptr = line.indexOf("=");
		    if (ptr == -1) {
			if (var != null) { // line continuation case
			    StringBuffer sb = new StringBuffer(processEnv.getProperty(var));
			    sb.append(line);
			    processEnv.setProperty(var, sb.toString());
			}
		    } else {
			var = line.substring(0,ptr);
			processEnv.setProperty(var, line.substring(ptr+1));
		    }
		}
	    }
	    return new Environment(processEnv);
	}
    }
}
