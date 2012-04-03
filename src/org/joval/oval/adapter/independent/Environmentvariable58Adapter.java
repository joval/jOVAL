// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.adapter.independent;

import java.math.BigDecimal;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.EnvironmentvariableObject;
import oval.schemas.definitions.independent.Environmentvariable58Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.independent.EnvironmentvariableItem;
import oval.schemas.systemcharacteristics.independent.Environmentvariable58Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IReader;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.PerishableReader;
import org.joval.oval.CollectException;
import org.joval.oval.Factories;
import org.joval.oval.OvalException;
import org.joval.util.AbstractEnvironment;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environmentvariable58Adapter extends EnvironmentvariableAdapter {
    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof ISession) {
	    this.session = (ISession)session;
	    environment = this.session.getEnvironment();
	    classes.add(Environmentvariable58Object.class);
	}
	return classes;
    }

    public Collection<? extends ItemType> getItems(ObjectType obj, IRequestContext rc) throws OvalException, CollectException {
	Environmentvariable58Object eObj = (Environmentvariable58Object)obj;
	if (eObj.isSetPid() && eObj.getPid().getValue() != null) {
	    //
	    // In the absence of a pid, just leverage the legacy adapter.
	    //
	    List<Environmentvariable58Item> items = new Vector<Environmentvariable58Item>();
            EnvironmentvariableObject evo = Factories.definitions.independent.createEnvironmentvariableObject();
            evo.setName(eObj.getName());
	    for (ItemType it : super.getItems(evo, rc)) {
		EnvironmentvariableItem item = (EnvironmentvariableItem)it;
		Environmentvariable58Item newItem = Factories.sc.independent.createEnvironmentvariable58Item();
		newItem.setName(item.getName());
		newItem.setValue(item.getValue());
		items.add(newItem);
	    }
	    return items;
	} else {
	    String pid = (String)eObj.getPid().getValue().getValue();
	    Properties processEnv = null;

	    //
	    // See: http://yong321.freeshell.org/computer/ProcEnv.txt
	    //
	    switch(session.getType()) {
	      case UNIX: {
		IUnixSession us = (IUnixSession)session;
		switch(us.getFlavor()) {
		  //
		  // In Solaris 10+, there is the pargs command:
		  // http://www.unix.com/hp-ux/112024-how-can-i-get-environment-running-process.html
		  //
		  case SOLARIS: {
		    try {
			BigDecimal VER_5_9 = new BigDecimal("5.9");
			BigDecimal osVersion = new BigDecimal(SafeCLI.exec("uname -r", session, IUnixSession.Timeout.S));
			if (osVersion.compareTo(VER_5_9) >= 0) {
			    IFile proc = session.getFilesystem().getFile("/proc/" + pid);
			    if (proc.exists() && proc.isDirectory()) {
				processEnv = new Properties();
				for (String line : SafeCLI.multiLine("pargs -e " + pid, us, IUnixSession.Timeout.S)) {
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
			    }
			} else {
			    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OS_VERSION, osVersion);
			    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
			}
		    } catch (Exception e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(e.getMessage());
			rc.addMessage(msg);
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		    break;
		  }

		  case LINUX: {
		    String path = "/proc/" + pid + "/environ";
		    IReader reader = null;
		    try {
			IFile proc = session.getFilesystem().getFile(path);
			if (proc.exists()) {
			    processEnv = new Properties();
			    long timeout = session.getTimeout(IUnixSession.Timeout.M);
			    reader = PerishableReader.newInstance(proc.getInputStream(), timeout);
			    reader.setLogger(session.getLogger());
			    String pair;
			    while ((pair = new String(reader.readUntil(127))) != null) { // 127 == delimiter char
				int ptr = pair.indexOf("=");
				if (ptr > 0) {
				    String key = pair.substring(0,ptr);
				    String val = pair.substring(ptr+1);
				    processEnv.setProperty(key, val);
				}
			    }
			}
		    } catch (IOException e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_IO, path, e.getMessage()));
			rc.addMessage(msg);
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    } finally {
			if (reader != null) {
			    try {
				reader.close();
			    } catch (IOException e) {
			    }
			}
		    }
		    break;
		  }

		  case AIX: {
		    try {
			for (String line : SafeCLI.multiLine("ps eww " + pid, us, IUnixSession.Timeout.S)) {
			    line = line.trim();
			    if (line.startsWith(pid)) {
				StringTokenizer tok = new StringTokenizer(line);
				Stack<String> stack = new Stack<String>();
				while (tok.hasMoreTokens()) {
				    stack.push(tok.nextToken());
				}
				processEnv = new Properties();
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
			    }
			}
		    } catch (Exception e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(e.getMessage());
			rc.addMessage(msg);
			session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		  }

		  default: {
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor());
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		  }
		}
		break;
	      }

	      default: {
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_SESSION_TYPE, session.getType());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	      }
	    }

	    if (processEnv == null) {
		return new Vector<Environmentvariable58Item>();
	    } else {
		return getItems(obj, rc, new PropertyEnvironment(processEnv), pid);
	    }
	}
    }

    // Internal

    @Override
    ItemType makeItem(String name, String value, String pid) {
	EnvironmentvariableItem evi = (EnvironmentvariableItem)super.makeItem(name, value, pid);
	Environmentvariable58Item item = Factories.sc.independent.createEnvironmentvariable58Item();
	item.setName(evi.getName());
	item.setValue(evi.getValue());

	EntityItemIntType pidType = Factories.sc.core.createEntityItemIntType();
	pidType.setValue(pid);
	item.setPid(pidType);
	return item;
    }

    // Private

    class PropertyEnvironment extends AbstractEnvironment {
	public PropertyEnvironment(Properties props) {
	    this.props = props;
	}
    }
}
