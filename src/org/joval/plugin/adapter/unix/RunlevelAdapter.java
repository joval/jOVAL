// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.adapter.unix;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.unix.RunlevelObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.RunlevelItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.plugin.IRequestContext;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.oval.OvalException;
import org.joval.oval.TestException;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Version;

/**
 * Resolves Runlevel OVAL objects.  Since chkconfig is not available on all Unix platforms, on some platforms it scans the
 * /etc/rcX.d directories to build an equivalent result.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RunlevelAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, Hashtable<String, Boolean>> runlevels;

    public RunlevelAdapter(IUnixSession session) {
	this.session = session;
	runlevels = new Hashtable<String, Hashtable<String, Boolean>>();
    }

    // Implement IAdapter

    public Class getObjectClass() {
	return RunlevelObject.class;
    }

    public boolean connect() {
	if (session != null) {
	    switch(session.getFlavor()) {
	      case SOLARIS: {
		try {
		    IFilesystem fs = session.getFilesystem();
		    for (String path : fs.search(Pattern.compile("/etc/rc.\\.d"), false)) {
			IFile dir = fs.getFile(path);
			if (dir.isDirectory()) {
			    String rl = path.substring(path.lastIndexOf(fs.getDelimiter())+1).substring(2, 3);
			    Hashtable<String, Boolean> runlevel = runlevels.get(rl);
			    if (runlevel == null) {
				runlevel = new Hashtable<String, Boolean>();
				runlevels.put(rl, runlevel);
			    }
			    String[] children = dir.list();
			    for (int i=0; i < children.length; i++) {
				int len = children[i].length();
				int ptr=1;
				boolean done = false;
				for (; ptr < len; ptr++) {
				    switch(children[i].charAt(ptr)) {
				      case '0':
				      case '1':
				      case '2':
				      case '3':
				      case '4':
				      case '5':
				      case '6':
				      case '7':
				      case '8':
				      case '9':
					break;
				      default:
					done = true;
					break;
				    }
				    if (done) {
					break;
				    }
				}
				String serviceName = children[i].substring(ptr);
				switch(children[i].charAt(0)) {
				  case 'S':
				    runlevel.put(serviceName, new Boolean(true));
				    break;
				  case 'K':
				    runlevel.put(serviceName, new Boolean(false));
				    break;
				}
			    }
			}
		    }
		    return true;
		} catch (IOException e) {
		    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		break;
	      }

	      case LINUX: {
		BufferedReader br = null;
		try {
		    IProcess p = session.createProcess("/sbin/chkconfig --list");
		    p.start();
		    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		    String line = null;
		    while ((line = br.readLine()) != null) {
			StringTokenizer tok = new StringTokenizer(line);
			if (tok.countTokens() == 8) {
			    String serviceName = tok.nextToken();
			    for (int i=0; i <= 6; i++) {
				String rl = "" + i;
				if (tok.nextToken().equals(rl + ":on")) {
				    Hashtable<String, Boolean> runlevel = runlevels.get(rl);
				    if (runlevel == null) {
					runlevel = new Hashtable<String, Boolean>();
					runlevels.put(rl, runlevel);
				    }
				    runlevel.put(serviceName, new Boolean(true));
				} else {
				    Hashtable<String, Boolean> runlevel = runlevels.get(rl);
				    if (runlevel == null) {
					runlevel = new Hashtable<String, Boolean>();
					runlevels.put(rl, runlevel);
				    }
				    runlevel.put(serviceName, new Boolean(false));
				}
			    }
			}
		    }
		    return true;
		} catch (Exception e) {
		    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		} finally {
		    if (br != null) {
			try {
			    br.close();
			} catch (IOException e) {
			}
		    }
		}
		break;
	      }

	      default:
		JOVALSystem.getLogger().warn(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, session.getFlavor());
		break;
	    }
	}
	return false;
    }

    public void disconnect() {
	runlevels = null;
    }

    public Collection<JAXBElement<? extends ItemType>> getItems(IRequestContext rc) throws OvalException {
	RunlevelObject rObj = (RunlevelObject)rc.getObject();
	Collection<JAXBElement<? extends ItemType>> items = new Vector<JAXBElement<? extends ItemType>>();
	OperationEnumeration op = rObj.getRunlevel().getOperation();
	switch(op) {
	  case EQUALS: {
	    Hashtable runlevel = runlevels.get((String)rObj.getRunlevel().getValue());
	    if (runlevel != null) {
		items.addAll(getItems(rc, ((String)rObj.getRunlevel().getValue())));
	    }
	    break;
	  }

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile((String)rObj.getRunlevel().getValue());
		for (String rl : runlevels.keySet()) {
		    if (p.matcher(rl).find()) {
			items.addAll(getItems(rc, rl));
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = JOVALSystem.factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    for (String rl : runlevels.keySet()) {
		if (!rl.equals((String)rObj.getRunlevel().getValue())) {
		    items.addAll(getItems(rc, rl));
		}
	    }
	    break;
	  }

	  default:
	    throw new OvalException(JOVALSystem.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
	}

	return items;
    }

    // Private

    /**
     * Get all the items matching the serviceName/operation, given the specified runlevel.
     */
    private Collection<JAXBElement<RunlevelItem>> getItems(IRequestContext rc, String rl) {
	RunlevelObject rObj = (RunlevelObject)rc.getObject();
	Collection<JAXBElement<RunlevelItem>> items = new Vector<JAXBElement<RunlevelItem>>();
	Hashtable<String, Boolean> runlevel = runlevels.get(rl);
	if (runlevel != null) {
	    OperationEnumeration op = rObj.getServiceName().getOperation();
	    switch(op) {
	      case EQUALS: {
		Boolean b = runlevel.get(rObj.getServiceName().getValue());
		if(b != null) {
		    items.add(makeItem(rl, (String)rObj.getServiceName().getValue(), b.booleanValue()));
		}
		break;
	      }

	      case PATTERN_MATCH: {
		try {
		    Pattern p = Pattern.compile((String)rObj.getServiceName().getValue());
		    for (String serviceName : runlevel.keySet()) {
			if(p.matcher(serviceName).find()) {
			    Boolean b = runlevel.get(serviceName);
			    items.add(makeItem(rl, serviceName, b.booleanValue()));
			}
		    }
		} catch (PatternSyntaxException e) {
		    MessageType msg = JOVALSystem.factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    msg.setValue(JOVALSystem.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		    rc.addMessage(msg);
		    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
		break;
	      }

	      case NOT_EQUAL: {
		for (String serviceName : runlevel.keySet()) {
		    if (!serviceName.equals((String)rObj.getServiceName().getValue())) {
			items.add(makeItem(rl, serviceName, runlevel.get(serviceName).booleanValue()));
		    }
		}
		break;
	      }
	    }
	}
	return items;
    }

    /**
     * Make a RunlevelItem with the specified characteristics.
     */
    private JAXBElement<RunlevelItem> makeItem(String runlevel, String serviceName, boolean start) {
	RunlevelItem item = JOVALSystem.factories.sc.unix.createRunlevelItem();

	EntityItemStringType runlevelType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	runlevelType.setValue(runlevel);
	item.setRunlevel(runlevelType);

	EntityItemStringType serviceNameType = JOVALSystem.factories.sc.core.createEntityItemStringType();
	serviceNameType.setValue(serviceName);
	item.setServiceName(serviceNameType);

	EntityItemBoolType startType = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	startType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	if (start) {
	    startType.setValue("true");
	} else {
	    startType.setValue("false");
	}
	item.setStart(startType);

	EntityItemBoolType kill = JOVALSystem.factories.sc.core.createEntityItemBoolType();
	kill.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	if (start) {
	    kill.setValue("false");
	} else {
	    kill.setValue("true");
	}
	item.setKill(kill);

	item.setStatus(StatusEnumeration.EXISTS);

	return JOVALSystem.factories.sc.unix.createRunlevelItem(item);
    }
}
