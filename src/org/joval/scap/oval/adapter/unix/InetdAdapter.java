// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.unix.InetdObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.EntityItemEndpointType;
import oval.schemas.systemcharacteristics.unix.EntityItemWaitStatusType;
import oval.schemas.systemcharacteristics.unix.InetdItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.io.BufferedReader;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.Version;

/**
 * Resolves Inetd OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class InetdAdapter implements IAdapter {
    public static final String CONFIG = "/etc/inetd.conf";

    private IUnixSession session;
    private HashSet<Service> services;
    private boolean applicable;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new Vector<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    applicable = true;
	    classes.add(InetdObject.class);
	}
	return classes;
    }

    public Collection<InetdItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	Collection<InetdItem> items = new Vector<InetdItem>();
	List<Service> list = new Vector<Service>();
	InetdObject iObj = (InetdObject)obj;
	try {
	    String protocol = (String)iObj.getProtocol().getValue();
	    OperationEnumeration op = iObj.getProtocol().getOperation();
	    Pattern p = null;
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(protocol);
	    }
	    for (Service service : services) {
		switch(op) {
		  case EQUALS:
		    if (service.protocol.equals(protocol)) {
			list.add(service);
		    }
		    break;
		  case CASE_INSENSITIVE_EQUALS:
		    if (service.protocol.equalsIgnoreCase(protocol)) {
			list.add(service);
		    }
		    break;
		  case NOT_EQUAL:
		    if (!service.protocol.equals(protocol)) {
			list.add(service);
		    }
		    break;
		  case PATTERN_MATCH:
		    if (p.matcher(service.protocol).find()) {
			list.add(service);
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }

	    String serviceName = (String)iObj.getServiceName().getValue();
	    op = iObj.getServiceName().getOperation();
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(serviceName);
	    }
	    for (Service service : list) {
		switch(op) {
		  case EQUALS:
		    if (service.name.equals(serviceName)) {
			items.add(makeItem(service));
		    }
		    break;
		  case CASE_INSENSITIVE_EQUALS:
		    if (service.name.equalsIgnoreCase(serviceName)) {
			items.add(makeItem(service));
		    }
		    break;
		  case NOT_EQUAL:
		    if (!service.name.equals(serviceName)) {
			items.add(makeItem(service));
		    }
		    break;
		  case PATTERN_MATCH:
		    if (p.matcher(service.name).find()) {
			items.add(makeItem(service));
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }
	} catch (PatternSyntaxException e) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.ERROR);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()));
	    rc.addMessage(msg);
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return items;
    }

    // Private

    private InetdItem makeItem(Service service) {
	InetdItem item = Factories.sc.unix.createInetdItem();

	EntityItemEndpointType endpointType = Factories.sc.unix.createEntityItemEndpointType();
	endpointType.setValue(service.socketType);
	item.setEndpointType(endpointType);

	EntityItemStringType execAsUser = Factories.sc.core.createEntityItemStringType();
	execAsUser.setValue(service.user);
	item.setExecAsUser(execAsUser);

	EntityItemStringType protocol = Factories.sc.core.createEntityItemStringType();
	protocol.setValue(service.protocol);
	item.setProtocol(protocol);

	EntityItemStringType serverArguments = Factories.sc.core.createEntityItemStringType();
	serverArguments.setValue(service.arguments);
	item.setServerArguments(serverArguments);

	EntityItemStringType serverProgram = Factories.sc.core.createEntityItemStringType();
	serverProgram.setValue(service.program);
	item.setServerProgram(serverProgram);

	EntityItemStringType serviceName = Factories.sc.core.createEntityItemStringType();
	serviceName.setValue(service.name);
	item.setServiceName(serviceName);

	EntityItemWaitStatusType waitStatus = Factories.sc.unix.createEntityItemWaitStatusType();
	waitStatus.setValue(service.wait ? "wait" : "nowait");
	item.setWaitStatus(waitStatus);

	item.setStatus(StatusEnumeration.EXISTS);
	return item;
    }

    /**
     * Idempotent.
     */
    private void init() throws CollectException {
	if (!applicable) {
	    String errmsg = JOVALMsg.getMessage(JOVALMsg.STATUS_INETD_NOCONFIG, CONFIG);
	    throw new CollectException(errmsg, FlagEnumeration.NOT_APPLICABLE);
	} else if (services != null) {
	    return;
	}
	services = new HashSet<Service>();
	IReader reader = null;
	try {
	    IFile f = session.getFilesystem().getFile(CONFIG, IFile.Flags.NOCACHE);
	    if (f.exists()) {
		reader = new BufferedReader(f.getInputStream());
		String line = null;
		while ((line = reader.readLine()) != null) {
		    line = line.trim();
		    if (!line.startsWith("#")) {
			try {
			    services.add(new Service(line));
			} catch (IllegalArgumentException e) {
			    session.getLogger().warn(JOVALMsg.ERROR_INETD_LINE, CONFIG, line);
			}
		    }
		}
	    } else {
		applicable = false;
		String errmsg = JOVALMsg.getMessage(JOVALMsg.STATUS_INETD_NOCONFIG, CONFIG);
		throw new CollectException(errmsg, FlagEnumeration.NOT_APPLICABLE);
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_IO, CONFIG, e.getMessage()));
	} finally {
	    if (reader != null) {
		try {
		    reader.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    class Service {
	String protocol, name, program, arguments, socketType, user;
	boolean wait;

	Service(String line) throws IllegalArgumentException {
	    StringTokenizer tok = new StringTokenizer(line);
	    if (tok.countTokens() >= 6) {
		name = tok.nextToken();
		socketType = tok.nextToken();
		protocol = tok.nextToken();
		wait = "wait".equals(tok.nextToken());
		user = tok.nextToken();
		program = tok.nextToken();
		if (tok.hasMoreTokens()) {
		    arguments = tok.nextToken("\n").trim();
		}
		session.getLogger().debug(JOVALMsg.STATUS_INETD_SERVICE, name);
	    } else {
		throw new IllegalArgumentException(line);
	    }
	}
    }
}
