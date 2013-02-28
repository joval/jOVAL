// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.solaris;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.solaris.SmfObject;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.solaris.EntityItemSmfProtocolType;
import scap.oval.systemcharacteristics.solaris.EntityItemSmfServiceStateType;
import scap.oval.systemcharacteristics.solaris.SmfItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;

/**
 * Evaluates the Solaris SMF OVAL tests.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SmfAdapter implements IAdapter {
    private IUnixSession session;
    private Map<String, SmfItem> serviceMap = null;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession && ((IUnixSession)session).getFlavor() == IUnixSession.Flavor.SOLARIS) {
	    this.session = (IUnixSession)session;
	    serviceMap = new HashMap<String, SmfItem>();
	    classes.add(SmfObject.class);
	} else {
	    notapplicable.add(SmfObject.class);
	}
	return classes;
    }

    public Collection<SmfItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	SmfObject sObj = (SmfObject)obj;
	Collection<SmfItem> items = new ArrayList<SmfItem>();

	switch(sObj.getFmri().getOperation()) {
	  case EQUALS:
	    try {
		String fmri = SafeCLI.checkArgument((String)sObj.getFmri().getValue(), session);
		items.add(fmri.startsWith(LEGACY) ? getLegacyItem(fmri) : getItem(fmri));
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  case NOT_EQUAL: {
	    loadFullServiceMap();
	    String fmri = getFullFmri((String)sObj.getFmri().getValue());
	    for (Map.Entry<String, SmfItem> entry : serviceMap.entrySet()) {
		if (!entry.getKey().equals(fmri)) {
		    items.add(entry.getValue());
		}
	    }
	    break;
	  }

	  case PATTERN_MATCH:
	    loadFullServiceMap();
	    try {
		Pattern p = Pattern.compile((String)sObj.getFmri().getValue());
		for (Map.Entry<String, SmfItem> entry : serviceMap.entrySet()) {
		    if (p.matcher(entry.getKey()).find()) {
			items.add(entry.getValue());
		    }
		}
	    } catch (PatternSyntaxException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
		rc.addMessage(msg);
		session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	    break;

	  default: {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, sObj.getFmri().getOperation());
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	  }
	}
	return items;
    }

    // Internal

    private boolean loaded = false;
    private void loadFullServiceMap() {
	if (loaded) return;

	try {
	    session.getLogger().trace(JOVALMsg.STATUS_SMF);
	    for (String service : SafeCLI.multiLine("/usr/bin/svcs -H -o fmri", session, IUnixSession.Timeout.M)) {
		try {
		    SmfItem item = service.startsWith(LEGACY) ? getLegacyItem(service) : getItem(service);
		    serviceMap.put((String)item.getFmri().getValue(), item);
		} catch (IllegalArgumentException e) {
		    session.getLogger().warn(e.getMessage());
		} catch (Exception e) {
		    session.getLogger().warn(JOVALMsg.ERROR_SMF, service);
		    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	    loaded = true;
	} catch (Exception e) {
	    session.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private static final String LEGACY		= "lrc:";
    private static final String FMRI		= "fmri";
    private static final String NAME		= "name";
    private static final String ENABLED		= "enabled";
    private static final String STATE		= "state";
    private static final String NEXT_STATE	= "next_state";
    private static final String STATE_TIME	= "state_time";
    private static final String RESTARTER	= "restarter";
    private static final String DEPENDENCY	= "dependency";

    private static final String START_EXEC_PROP	= "start/exec";
    private static final String INETD_USER_PROP	= "inetd_start/user";
    private static final String INETD_EXEC_PROP	= "inetd_start/exec";

    private SmfItem getLegacyItem(String s) throws Exception {
	String fmri = getFullFmri(s);
	if (fmri == null) {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_FMRI, s));
	}
	if (serviceMap.containsKey(fmri)) {
	    return serviceMap.get(fmri);
	}
	session.getLogger().debug(JOVALMsg.STATUS_SMF_SERVICE, fmri);

	SmfItem item = Factories.sc.solaris.createSmfItem();
	EntityItemStringType fmriType = Factories.sc.core.createEntityItemStringType();
	fmriType.setValue(fmri);
	item.setFmri(fmriType);

	String line = SafeCLI.exec("/usr/bin/svcs -H '" + fmri + "'", session, IUnixSession.Timeout.S);
	if (line.trim().length() == 0) {
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	} else {
	    StringTokenizer tok = new StringTokenizer(line);
	    if (tok.countTokens() > 0) {
		EntityItemSmfServiceStateType serviceState = Factories.sc.solaris.createEntityItemSmfServiceStateType();
		try {
		    serviceState.setValue(toServiceState(tok.nextToken()));
		} catch (IllegalArgumentException e) {
		    serviceState.setStatus(StatusEnumeration.ERROR);
		}
		item.setServiceState(serviceState);
	    }
	}
	serviceMap.put(fmri, item);
	return item;
    }

    private SmfItem getItem(String s) throws Exception {
	String fmri = getFullFmri(s);
	if (fmri == null) {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_FMRI, s));
	}
	if (serviceMap.containsKey(fmri)) {
	    return serviceMap.get(fmri);
	}
	session.getLogger().debug(JOVALMsg.STATUS_SMF_SERVICE, fmri);

	SmfItem item = Factories.sc.solaris.createSmfItem();
	boolean found = false;
	for (String line : SafeCLI.multiLine("/usr/bin/svcs -l '" + fmri + "'", session, IUnixSession.Timeout.S)) {
	    line = line.trim();
	    if (line.length() == 0) {
		break;
	    } else if (line.startsWith(FMRI)) {
		found = true;
		fmri = getFullFmri(line.substring(FMRI.length()).trim());

		//
		// Name is based on the FMRI.  See:
		// http://making-security-measurable.1364806.n2.nabble.com/Solaris-10-SMF-test-request-UNCLASSIFIED-tt23753.html#a23757
		//
		EntityItemStringType nameType = Factories.sc.core.createEntityItemStringType();
		nameType.setValue(getName(fmri));
		item.setServiceName(nameType);
	    } else if (line.startsWith(STATE_TIME)) { // NB: this condition MUST appear before STATE
	    } else if (line.startsWith(STATE)) {
		EntityItemSmfServiceStateType serviceState = Factories.sc.solaris.createEntityItemSmfServiceStateType();
		try {
		    serviceState.setValue(toServiceState(line.substring(STATE.length())));
		} catch (IllegalArgumentException e) {
		    serviceState.setStatus(StatusEnumeration.ERROR);
		}
		item.setServiceState(serviceState);
	    }
	}
	EntityItemStringType fmriType = Factories.sc.core.createEntityItemStringType();
	fmriType.setValue(fmri);
	item.setFmri(fmriType);

	if (found) {
	    //
	    // If the service was found, then we can retrieve some information using svcprop
	    //
	    boolean inetd = false;
	    for (String line : SafeCLI.multiLine("/usr/bin/svcprop '" + fmri + "'", session, IUnixSession.Timeout.S)) {
		if (line.startsWith(START_EXEC_PROP)) {
		    setExecAndArgs(item, line.substring(START_EXEC_PROP.length()).trim());
		} else if (line.startsWith(INETD_USER_PROP)) {
		    inetd = true;
		    String suspects = line.substring(INETD_USER_PROP.length()).trim();
		    List<String> list = StringTools.toList(StringTools.tokenize(suspects, " "));
		    if (list.size() > 0) {
			String user = list.get(list.size() - 1);
			EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
			type.setValue(user);
			item.setExecAsUser(type);
		    }
		} else if (line.startsWith(INETD_EXEC_PROP)) {
		    inetd = true;
		    setExecAndArgs(item, line.substring(INETD_EXEC_PROP.length()).trim());
		}
	    }

	    //
	    // If this is an inetd-initiated service, we can get protocol information using inetadm
	    //
	    if (inetd) {
		for (String line : SafeCLI.multiLine("/usr/sbin/inetadm -l '" + fmri + "'", session, IUnixSession.Timeout.S)) {
		    if (line.trim().startsWith("proto=")) {
			String protocol = line.trim().substring(6);
			if (protocol.startsWith("\"") && protocol.endsWith("\"")) {
			    protocol = protocol.substring(1, protocol.length() - 1);
			    EntityItemSmfProtocolType type = Factories.sc.solaris.createEntityItemSmfProtocolType();
			    type.setValue(protocol);
			    item.setProtocol(type);
			}
		    }
		}
	    }
	} else {
	    item.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	serviceMap.put(fmri, item);
	return item;
    }

    private String toServiceState(String s) throws IllegalArgumentException {
	s = s.trim();
	if (s.equalsIgnoreCase("degraded")) {
	    return "DEGRADED";
	} else if (s.equalsIgnoreCase("disabled")) {
	    return "DISABLED";
	} else if (s.equalsIgnoreCase("legacy_run")) {
	    return "LEGACY-RUN";
	} else if (s.equalsIgnoreCase("offline")) {
	    return "OFFLINE";
	} else if (s.equalsIgnoreCase("online")) {
	    return "ONLINE";
	} else if (s.equalsIgnoreCase("uninitialized")) {
	    return "UNINITIALIZED";
	} else {
	    throw new IllegalArgumentException(s);
	}
    }

    /**
     * Given a property line starting after start/exec or inetd_start/exec, set the server_executable and server_arguments.
     */
    private void setExecAndArgs(SmfItem item, String argv) {
	String unescaped = argv.replace("\\ ", " ");
	StringTokenizer tok = new StringTokenizer(unescaped);
	if (tok.hasMoreTokens()) {
	    String astring = tok.nextToken();
	    if (astring.equals("astring") && tok.hasMoreTokens()) {
		String executable = tok.nextToken();
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(executable);
		item.setServerExecutable(type);
	    }
	    StringBuffer arguments = new StringBuffer();
	    while(tok.hasMoreTokens()) {
		if (arguments.length() > 0) {
		    arguments.append(" ");
		}
		arguments.append(tok.nextToken());
	    }
	    if (arguments.length() > 0) {
		EntityItemStringType type = Factories.sc.core.createEntityItemStringType();
		type.setValue(arguments.toString());
		item.setServerArguements(type);
	    }
	}
    }

    /**
     * Generally, prepend "localhost".
     */
    private String getFullFmri(String fmri) {
	if (fmri.startsWith(LEGACY)) {
	    return fmri;
	} else if (fmri.indexOf("//") == -1) {
	    int ptr = fmri.indexOf("/");
	    if (ptr == -1) {
		return null;
	    }
	    StringBuffer sb = new StringBuffer(fmri.substring(0, ptr));
	    sb.append("//localhost");
	    sb.append(fmri.substring(ptr));
	    return sb.toString();
	} else {
	    return fmri;
	}
    }

    private String getName(String fullFmri) throws IllegalArgumentException {
	int begin = fullFmri.lastIndexOf("/");
	if (begin == -1) {
	    throw new IllegalArgumentException(fullFmri);
	} else {
	    int end = fullFmri.indexOf(":", ++begin);
	    if (end == -1) {
		end = fullFmri.length();
	    }
	    return fullFmri.substring(begin, end);
	}
    }
}
