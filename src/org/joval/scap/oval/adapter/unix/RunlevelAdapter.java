// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.RunlevelObject;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.RunlevelItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Resolves Runlevel OVAL objects.  Since chkconfig is not available on all Unix platforms, on some platforms it scans the
 * /etc/rcX.d directories to build an equivalent result.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class RunlevelAdapter implements IAdapter {
    private IUnixSession session;
    private Hashtable<String, Hashtable<String, StartStop>> runlevels;
    private boolean initialized = false;

    // Implement IAdapter

    public Collection<Class> init(ISession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    runlevels = new Hashtable<String, Hashtable<String, StartStop>>();
	    classes.add(RunlevelObject.class);
	}
	return classes;
    }

    public Collection<RunlevelItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (!initialized) {
	    init();
	}
	Collection<RunlevelItem> items = new Vector<RunlevelItem>();
	RunlevelObject rObj = (RunlevelObject)obj;
	String runlevel = (String)rObj.getRunlevel().getValue();
	OperationEnumeration op = rObj.getRunlevel().getOperation();
	switch(op) {
	  case EQUALS: {
	    Hashtable table = runlevels.get(runlevel);
	    if (table != null) {
		items.addAll(getItems(rObj, rc, runlevel));
	    }
	    break;
	  }

	  case PATTERN_MATCH:
	    try {
		Pattern p = Pattern.compile(runlevel);
		for (String rl : runlevels.keySet()) {
		    if (p.matcher(rl).find()) {
			items.addAll(getItems(rObj, rc, rl));
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

	  case NOT_EQUAL: {
	    for (String rl : runlevels.keySet()) {
		if (!rl.equals(runlevel)) {
		    items.addAll(getItems(rObj, rc, rl));
		}
	    }
	    break;
	  }

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	return items;
    }

    // Private

    /**
     * Get all the items matching the serviceName/operation, given the specified runlevel.
     */
    private Collection<RunlevelItem> getItems(RunlevelObject rObj, IRequestContext rc, String rl) throws CollectException {
	Collection<RunlevelItem> items = new Vector<RunlevelItem>();
	String serviceName = (String)rObj.getServiceName().getValue();
	if (runlevels.containsKey(rl)) {
	    Hashtable<String, StartStop> runlevel = runlevels.get(rl);
            OperationEnumeration op = rObj.getServiceName().getOperation();
            switch(op) {
              case EQUALS:
                if(runlevel.containsKey(serviceName)) {
                    items.add(makeItem(rl, serviceName, runlevel.get(serviceName)));
                }
                break;

              case PATTERN_MATCH:
                try {
                    Pattern p = Pattern.compile(serviceName);
                    for (String sn : runlevel.keySet()) {
                        if(p.matcher(sn).find()) {
                            items.add(makeItem(rl, sn, runlevel.get(sn)));
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

              case NOT_EQUAL:
                for (String sn : runlevel.keySet()) {
                    if (!sn.equals(serviceName)) {
                        items.add(makeItem(rl, sn, runlevel.get(sn)));
                    }
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
     * Make a RunlevelItem with the specified characteristics.
     */
    private RunlevelItem makeItem(String runlevel, String serviceName, StartStop actions) {
	RunlevelItem item = Factories.sc.unix.createRunlevelItem();

	EntityItemStringType runlevelType = Factories.sc.core.createEntityItemStringType();
	runlevelType.setValue(runlevel);
	item.setRunlevel(runlevelType);

	EntityItemStringType serviceNameType = Factories.sc.core.createEntityItemStringType();
	serviceNameType.setValue(serviceName);
	item.setServiceName(serviceNameType);

	EntityItemBoolType startType = Factories.sc.core.createEntityItemBoolType();
	startType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	if (actions.start) {
	    startType.setValue("true");
	} else {
	    startType.setValue("false");
	}
	item.setStart(startType);

	EntityItemBoolType kill = Factories.sc.core.createEntityItemBoolType();
	kill.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	if (actions.stop) {
	    kill.setValue("true");
	} else {
	    kill.setValue("false");
	}
	item.setKill(kill);

	item.setStatus(StatusEnumeration.EXISTS);
	return item;
    }

    /**
     * Collect all runlevel information from the machine.
     */
    private void init() throws CollectException {
	try {
	    switch(session.getFlavor()) {
	      case AIX:
		initUnixRunlevels("/etc/rc\\.d");
		break;

	      case SOLARIS:
		initUnixRunlevels("/etc");
		break;

	      case LINUX:
		initLinuxRunlevels();
		break;

	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_UNIX_FLAVOR, session.getFlavor());
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	initialized = true;
    }

    /**
     * Collect runlevel information from the specified base directory containing the rc.X.d directories.
     */
    private void initUnixRunlevels(String baseDir) throws IOException {
	IFile base = session.getFilesystem().getFile(baseDir);
	for (IFile dir : base.listFiles(Pattern.compile("rc[\\p{Alnum}]\\.d"))) {
	    if (dir.isDirectory()) {
		String rl = dir.getName().substring(2, 3);
		Hashtable<String, StartStop> runlevel = runlevels.get(rl);
		if (runlevel == null) {
		    runlevel = new Hashtable<String, StartStop>();
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
		    StartStop actions = runlevel.get(serviceName);
		    if (actions == null) {
			actions = new StartStop();
		    }
		    switch(children[i].charAt(0)) {
		      case 'S':
			actions.start = true;
			break;
		      case 'K':
			actions.stop = true;
			break;
		    }
		    runlevel.put(serviceName, actions);
		}
	    }
	}
    }

    /**
     * On Linux, in addition to collecting information from the rc directories, we can also use the chkconfig command.
     */
    private void initLinuxRunlevels() {
	try {
	    initUnixRunlevels("/etc/rc.d");
	    for (String line : SafeCLI.multiLine("/sbin/chkconfig --list", session, IUnixSession.Timeout.M)) {
		StringTokenizer tok = new StringTokenizer(line);
		if (tok.countTokens() == 8) {
		    String serviceName = tok.nextToken();
		    for (int i=0; i <= 6; i++) {
			String rl = "" + i;
			if (tok.nextToken().equals(rl + ":on")) {
			    Hashtable<String, StartStop> runlevel = runlevels.get(rl);
			    if (runlevel == null) {
				runlevel = new Hashtable<String, StartStop>();
				runlevels.put(rl, runlevel);
			    }
			    StartStop actions = runlevel.get(serviceName);
			    if (actions == null) {
				actions = new StartStop();
			    }
			    actions.start = true;
			    runlevel.put(serviceName, actions);
			} else {
			    Hashtable<String, StartStop> runlevel = runlevels.get(rl);
			    if (runlevel == null) {
				runlevel = new Hashtable<String, StartStop>();
				runlevels.put(rl, runlevel);
			    }
			    StartStop actions = runlevel.get(serviceName);
			    if (actions == null) {
				actions = new StartStop();
			    }
			    actions.stop = true;
			    runlevel.put(serviceName, actions);
			}
		    }
		}
	    }
	} catch (Exception e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    class StartStop {
	boolean start;
	boolean stop;

	StartStop() {
	    start = false;
	    stop = false;
	}
    }
}
