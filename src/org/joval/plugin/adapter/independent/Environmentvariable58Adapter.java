// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.independent;

import java.math.BigDecimal;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.independent.EnvironmentvariableObject;
import oval.schemas.definitions.independent.Environmentvariable58Object;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.independent.EnvironmentvariableItem;
import oval.schemas.systemcharacteristics.independent.Environmentvariable58Item;
import oval.schemas.results.core.ResultEnumeration;

import org.joval.intf.io.IFile;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.system.ISession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.StreamTool;
import org.joval.os.unix.system.Environment;
import org.joval.oval.CollectionException;
import org.joval.oval.OvalException;
import org.joval.oval.ResolveException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Evaluates Environmentvariable OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Environmentvariable58Adapter extends EnvironmentvariableAdapter {
    private ISession session;

    public Environmentvariable58Adapter(ISession session) {
	super(session);
	this.session = session;
    }

    // Implement IAdapter

    private static Class[] objectClasses = {Environmentvariable58Object.class};

    public Class[] getObjectClasses() {
	return objectClasses;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException, CollectionException {
	Environmentvariable58Object eObj = (Environmentvariable58Object)rc.getObject();

	if (eObj.isSetPid() && eObj.getPid().getValue() != null) {
	    //
	    // In the absence of a pid, just leverage the legacy adapter.
	    //
	    List<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	    for (JAXBElement<? extends ItemType> elt : super.getItems(new EVRequestContext(rc))) {
		EnvironmentvariableItem item = (EnvironmentvariableItem)elt.getValue();
		Environmentvariable58Item newItem = JOVALSystem.factories.sc.independent.createEnvironmentvariable58Item();
		newItem.setName(item.getName());
		newItem.setValue(item.getValue());
		items.add(JOVALSystem.factories.sc.independent.createEnvironmentvariable58Item(newItem));
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
			BigDecimal osVersion = new BigDecimal(session.getSystemInfo().getOsVersion());
			if (osVersion.compareTo(VER_5_9) >= 0) {
			    IFile proc = session.getFilesystem().getFile("/proc/" + pid);
			    if (proc.exists() && proc.isDirectory()) {
				processEnv = new Properties();
				IProcess p = us.createProcess("pargs -e " + pid);
				p.start();
				StreamTool.ManagedReader mr = StreamTool.getManagedReader(p.getInputStream(), IUnixSession.TIMEOUT_S);
				String line;
				while ((line = mr.readLine()) != null) {
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
				mr.close();
				p.waitFor(0);
			    }
			} else {
			    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OS_VERSION, osVersion);
			    throw new CollectionException(msg);
			}
		    } catch (Exception e) {
			MessageType msg = JOVALSystem.factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(e.getMessage());
			rc.addMessage(msg);
			JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		    break;
		  }

		  case LINUX: {
		    String path = "/proc/" + pid + "/environ";
		    InputStream in = null;
		    try {
			IFile proc = session.getFilesystem().getFile(path);
			if (proc.exists()) {
			    processEnv = new Properties();
			    in = proc.getInputStream();
			    String pair;
			    while ((pair = new String(StreamTool.readUntil(in, 127))) != null) { // 127 == delimiter char
				int ptr = pair.indexOf("=");
				if (ptr > 0) {
				    String key = pair.substring(0,ptr);
				    String val = pair.substring(ptr+1);
				    processEnv.setProperty(key, val);
				}
			    }
			}
		    } catch (IOException e) {
			MessageType msg = JOVALSystem.factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_IO, path, e.getMessage()));
			rc.addMessage(msg);
			JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    } finally {
			if (in != null) {
			    try {
				in.close();
			    } catch (IOException e) {
			    }
			}
		    }
		    break;
		  }

		  default: {
		    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, us.getFlavor());
		    throw new CollectionException(msg);
		  }
		}
		break;
	      }

	      default: {
		String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_SESSION_TYPE, session.getType());
		throw new CollectionException(msg);
	      }
	    }

	    if (processEnv == null) {
		return new Vector<JAXBElement<? extends ItemType>>();
	    } else {
		return getItems(rc, new PropertyEnvironment(processEnv), pid);
	    }
	}
    }

    // Internal

    /**
     * @override
     */
    JAXBElement<? extends ItemType> makeItem(String name, String value, String pid) {
	EnvironmentvariableItem evi = (EnvironmentvariableItem)super.makeItem(name, value, pid).getValue();
	Environmentvariable58Item item = JOVALSystem.factories.sc.independent.createEnvironmentvariable58Item();
	item.setName(evi.getName());
	item.setValue(evi.getValue());

	EntityItemIntType pidType = JOVALSystem.factories.sc.core.createEntityItemIntType();
	pidType.setValue(pid);
	item.setPid(pidType);

	return JOVALSystem.factories.sc.independent.createEnvironmentvariable58Item(item);
    }

    // Private

    class EVRequestContext implements IRequestContext {
        IRequestContext base;
        EnvironmentvariableObject object;

        EVRequestContext(IRequestContext base) {
            Environmentvariable58Object evo = (Environmentvariable58Object)base.getObject();
            object = JOVALSystem.factories.definitions.independent.createEnvironmentvariableObject();
            object.setName(evo.getName());
        }

        // Implement IRequestContext
        public ObjectType getObject() {
            return object;
        }

        public void addMessage(MessageType msg) {
            base.addMessage(msg);
        }

        public Collection<String> resolve(String variableId) throws NoSuchElementException, ResolveException, OvalException {
            return base.resolve(variableId);
        }
    }

    class PropertyEnvironment extends Environment {
	public PropertyEnvironment(Properties props) {
	    this.props = props;
	}
    }
}
