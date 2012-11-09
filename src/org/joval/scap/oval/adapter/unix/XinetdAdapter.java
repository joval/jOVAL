// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import oval.schemas.common.MessageType;
import oval.schemas.common.MessageLevelEnumeration;
import oval.schemas.common.OperationEnumeration;
import oval.schemas.common.SimpleDatatypeEnumeration;
import oval.schemas.definitions.core.ObjectType;
import oval.schemas.definitions.unix.XinetdObject;
import oval.schemas.results.core.ResultEnumeration;
import oval.schemas.systemcharacteristics.core.EntityItemBoolType;
import oval.schemas.systemcharacteristics.core.EntityItemIntType;
import oval.schemas.systemcharacteristics.core.EntityItemStringType;
import oval.schemas.systemcharacteristics.core.EntityItemIPAddressStringType;
import oval.schemas.systemcharacteristics.core.FlagEnumeration;
import oval.schemas.systemcharacteristics.core.ItemType;
import oval.schemas.systemcharacteristics.core.StatusEnumeration;
import oval.schemas.systemcharacteristics.unix.EntityItemXinetdTypeStatusType;
import oval.schemas.systemcharacteristics.unix.XinetdItem;

import org.joval.intf.io.IFile;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.io.IReader;
import org.joval.intf.plugin.IAdapter;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.intf.util.ISearchable;
import org.joval.io.BufferedReader;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.util.SafeCLI;
import org.joval.util.Version;

/**
 * Resolves Xinetd OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XinetdAdapter implements IAdapter {
    public static final String CONFIG = "/etc/xinetd.conf";

    private IUnixSession session;
    private HashMap<String, Service> services;
    private HashMap<String, List<String>> defaults;
    private boolean applicable;

    // Implement IAdapter

    public Collection<Class> init(IBaseSession session) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    this.session = (IUnixSession)session;
	    applicable = true;
	    classes.add(XinetdObject.class);
	}
	return classes;
    }

    public Collection<XinetdItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	init();
	Collection<XinetdItem> items = new ArrayList<XinetdItem>();
	List<Service> list = new ArrayList<Service>();
	XinetdObject xObj = (XinetdObject)obj;
	try {
	    String protocol = (String)xObj.getProtocol().getValue();
	    OperationEnumeration op = xObj.getProtocol().getOperation();
	    Pattern p = null;
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(protocol);
	    }
	    for (Service service : services.values()) {
		String serviceProtocol = "";
		try {
		    serviceProtocol = service.getString(Property.PROTOCOL);
		} catch (NoSuchElementException e) {
		}
		switch(op) {
		  case EQUALS:
		    if (protocol.equals(serviceProtocol)) {
			list.add(service);
		    }
		    break;
		  case CASE_INSENSITIVE_EQUALS:
		    if (protocol.equalsIgnoreCase(serviceProtocol)) {
			list.add(service);
		    }
		    break;
		  case NOT_EQUAL:
		    if (!protocol.equals(serviceProtocol)) {
			list.add(service);
		    }
		    break;
		  case PATTERN_MATCH:
		    if (p.matcher(serviceProtocol).find()) {
			list.add(service);
		    }
		    break;
		  default:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
		}
	    }

	    String name = (String)xObj.getServiceName().getValue();
	    op = xObj.getServiceName().getOperation();
	    if (op == OperationEnumeration.PATTERN_MATCH) {
		p = Pattern.compile(name);
	    }
	    for (Service service : list) {
		String serviceName = service.getName();
		switch(op) {
		  case EQUALS:
		    if (name.equals(serviceName)) {
			items.add(makeItem(service));
		    }
		    break;
		  case CASE_INSENSITIVE_EQUALS:
		    if (name.equalsIgnoreCase(serviceName)) {
			items.add(makeItem(service));
		    }
		    break;
		  case NOT_EQUAL:
		    if (!name.equals(serviceName)) {
			items.add(makeItem(service));
		    }
		    break;
		  case PATTERN_MATCH:
		    if (p.matcher(serviceName).find()) {
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

    private XinetdItem makeItem(Service service) {
	XinetdItem item = Factories.sc.unix.createXinetdItem();

	EntityItemStringType serviceName = Factories.sc.core.createEntityItemStringType();
	serviceName.setValue(service.getName());
	item.setServiceName(serviceName);

	EntityItemBoolType disabled = Factories.sc.core.createEntityItemBoolType();
	disabled.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	try {
	    disabled.setValue(service.getString(Property.DISABLE).equals("yes") ? "1" : "0");
	} catch (NoSuchElementException e) {
	    disabled.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setDisabled(disabled);

	try {
	    for (String flag : service.get(Property.FLAGS)) {
		EntityItemStringType value = Factories.sc.core.createEntityItemStringType();
		value.setValue(flag);
		item.getFlags().add(value);
	    }
	} catch (NoSuchElementException e) {
	}

	try {
	    for (String deny : service.get(Property.NO_ACCESS)) {
		EntityItemStringType value = Factories.sc.core.createEntityItemStringType();
		value.setValue(deny);
		item.getNoAccess().add(value);
	    }
	} catch (NoSuchElementException e) {
	}

	try {
	    for (String allow : service.get(Property.ONLY_FROM)) {
		EntityItemIPAddressStringType value = Factories.sc.core.createEntityItemIPAddressStringType();
		value.setValue(allow);
		item.getOnlyFrom().add(value);
	    }
	} catch (NoSuchElementException e) {
	}

	EntityItemIntType port = Factories.sc.core.createEntityItemIntType();
	port.setDatatype(SimpleDatatypeEnumeration.INT.value());
	try {
	    port.setValue(service.getString(Property.PORT));
	} catch (NoSuchElementException e) {
	    port.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setPort(port);

	EntityItemStringType protocol = Factories.sc.core.createEntityItemStringType();
	try {
	    protocol.setValue(service.getString(Property.PROTOCOL));
	} catch (NoSuchElementException e) {
	    protocol.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setProtocol(protocol);

	EntityItemStringType server = Factories.sc.core.createEntityItemStringType();
	try {
	    server.setValue(service.getString(Property.SERVER));
	} catch (NoSuchElementException e) {
	    server.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setServer(server);

	EntityItemStringType serverArguments = Factories.sc.core.createEntityItemStringType();
	try {
	    serverArguments.setValue(service.getString(Property.SERVER_ARGUMENTS));
	} catch (NoSuchElementException e) {
	    serverArguments.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setServerArguments(serverArguments);

	EntityItemStringType socketType = Factories.sc.core.createEntityItemStringType();
	try {
	    socketType.setValue(service.getString(Property.SOCKET_TYPE));
	} catch (NoSuchElementException e) {
	    socketType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setSocketType(socketType);

	EntityItemXinetdTypeStatusType type = Factories.sc.unix.createEntityItemXinetdTypeStatusType();
	try {
	    type.setValue(service.getString(Property.TYPE));
	} catch (NoSuchElementException e) {
	    type.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setType(type);

	EntityItemStringType user = Factories.sc.core.createEntityItemStringType();
	try {
	    user.setValue(service.getString(Property.USER));
	} catch (NoSuchElementException e) {
	    user.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setUser(user);

	EntityItemBoolType wait = Factories.sc.core.createEntityItemBoolType();
	wait.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	try {
	    wait.setValue(service.getString(Property.WAIT).equals("wait") ? "1" : "0");
	} catch (NoSuchElementException e) {
	    wait.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	}
	item.setWait(wait);

	item.setStatus(StatusEnumeration.EXISTS);
	return item;
    }

    /**
     * Idempotent.
     */
    private void init() throws CollectException {
	if (!applicable) {
	    String errmsg = JOVALMsg.getMessage(JOVALMsg.STATUS_XINETD_NOCONFIG, CONFIG);
	    throw new CollectException(errmsg, FlagEnumeration.NOT_APPLICABLE);
	} else if (services != null) {
	    return;
	}
	services = new HashMap<String, Service>();
	try {
	    IFilesystem fs = session.getFilesystem();
	    IFile config = fs.getFile(CONFIG, IFile.NOCACHE);
	    if (config.exists()) {
		try {
		    parseConfigFile(config);
		} catch (IOException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_XINETD_FILE, config.getPath(), e.getMessage());
		} catch (IllegalArgumentException e) {
		    session.getLogger().warn(JOVALMsg.ERROR_XINETD_FILE, config.getPath(), e.getMessage());
		}
	    } else {
		applicable = false;
		String errmsg = JOVALMsg.getMessage(JOVALMsg.STATUS_XINETD_NOCONFIG, CONFIG);
		throw new CollectException(errmsg, FlagEnumeration.NOT_APPLICABLE);
	    }
	} catch (IOException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_IO, CONFIG, e.getMessage()));
	}
    }

    /**
     * Parses (recursively, via include directives) the specified Xinetd configuration file.
     */
    private void parseConfigFile(IFile config) throws IOException, IllegalArgumentException {
	session.getLogger().info(JOVALMsg.STATUS_XINETD_FILE, config.getPath());
	IReader reader = null;
	try {
	    String line = null;
	    reader = new BufferedReader(config.getInputStream());
	    while ((line = reader.readLine()) != null) {
		line = line.trim();
		if (line.length() == 0 || line.startsWith("#")) {
		    continue;
		} else {
		    int ptr = line.indexOf("#");
		    if (ptr > 0) {
			line = line.substring(0,ptr); // strip any trailing comment
		    }
		    if (line.startsWith("defaults")) {
			defaults = parseConfigBlock(reader);
		    } else if (line.startsWith("service")) {
			String name = line.substring(7).trim();
			services.put(name, new Service(name, parseConfigBlock(reader)));
		    } else if (line.startsWith("includefile")) {
			String path = line.substring(11).trim();
			IFile file = session.getFilesystem().getFile(path, IFile.NOCACHE);
			if (file.exists() && file.isFile()) {
			    try {
				parseConfigFile(file);
			    } catch (IOException e) {
				session.getLogger().warn(JOVALMsg.ERROR_XINETD_FILE, file.getPath(), e.getMessage());
			    } catch (IllegalArgumentException e) {
				session.getLogger().warn(JOVALMsg.ERROR_XINETD_FILE, file.getPath(), e.getMessage());
			    }
			}
		    } else if (line.startsWith("includedir")) {
			String path = line.substring(10).trim();
			IFile dir = session.getFilesystem().getFile(path, IFile.NOCACHE);
			if (dir.exists() && dir.isDirectory()) {
			    for (IFile file : dir.listFiles()) {
				try {
				    parseConfigFile(file);
				} catch (IOException e) {
				    session.getLogger().warn(JOVALMsg.ERROR_XINETD_FILE, file.getPath(), e.getMessage());
				} catch (IllegalArgumentException e) {
				    session.getLogger().warn(JOVALMsg.ERROR_XINETD_FILE, file.getPath(), e.getMessage());
				}
			    }
			}
		    }
		}
	    }
	} finally {
	    if (reader != null) {
		try {
		    reader.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    /**
     * Parses properties between { and }.  Leaves the reader open.  Throws an exception if no brackets were found.
     */
    private HashMap<String, List<String>> parseConfigBlock(IReader reader) throws IOException, IllegalArgumentException {
	String line = null;
	boolean open = false;
	HashMap<String, List<String>> props = null;
	while((line = reader.readLine()) != null) {
	    line = line.trim();
	    if (line.length() == 0 || line.startsWith("#")) {
		continue;
	    } else {
		int ptr = line.indexOf("#");
		if (ptr > 0) {
		    line = line.substring(0,ptr); // strip any trailing comment
		}
		if (!open && line.equals("{")) {
		    props = new HashMap<String, List<String>>();
		    if (defaults != null) {
			for (Map.Entry<String, List<String>> entry : defaults.entrySet()) {
			    List<String> val = new ArrayList<String>();
			    val.addAll(entry.getValue());
			    props.put(entry.getKey(), val);
			}
		    }
		    open = true;
		} else if (open && line.equals("}")) {
		    break;
		} else if (open) {
		    int eq = line.indexOf("=");
		    int pe = line.indexOf("+=");
		    int me = line.indexOf("-=");

		    if (pe > 0) {
			String key = line.substring(0,pe).trim();
			StringTokenizer tok = new StringTokenizer(line.substring(pe+2).trim());
			List<String> list = new ArrayList<String>(tok.countTokens());
			while(tok.hasMoreTokens()) {
			    list.add(tok.nextToken());
			}
			if (!props.containsKey(key)) {
			    props.put(key, new ArrayList<String>());
			}
			props.get(key).addAll(list);
		    } else if (me > 0) {
			String key = line.substring(0,me).trim();
			StringTokenizer tok = new StringTokenizer(line.substring(me+2).trim());
			List<String> list = new ArrayList<String>(tok.countTokens());
			while(tok.hasMoreTokens()) {
			    list.add(tok.nextToken());
			}
			if (props.containsKey(key)) {
			    for (String val : list) {
				props.get(key).remove(val);
			    }
			}
		    } else if (eq > 0) {
			String key = line.substring(0,eq).trim();
			StringTokenizer tok = new StringTokenizer(line.substring(eq+1).trim());
			List<String> list = new ArrayList<String>(tok.countTokens());
			while(tok.hasMoreTokens()) {
			    list.add(tok.nextToken());
			}
			props.put(key, list);
		    }
		}
	    }
	}
	if (props == null) {
	    throw new IllegalArgumentException(JOVALMsg.getMessage(JOVALMsg.ERROR_XINETD_FORMAT));
	}
	return props;
    }

    /**
     * Xinetd config property keys of interest.
     */
    enum Property {
	DISABLE("disable"),
	FLAGS("flags"),
	NO_ACCESS("no_access"),
	ONLY_FROM("only_from"),
	PORT("port"),
	PROTOCOL("protocol"),
	SERVER("server"),
	SERVER_ARGUMENTS("server_args"),
	SOCKET_TYPE("socket_type"),
	TYPE("type"),
	USER("user"),
	WAIT("wait");

	private String value;

	private Property(String value) {
	    this.value = value;
	}

	String value() {
	    return value;
	}
    }

    class Service {
	private Map<String, List<String>> props;
	private String name;

	Service(String name, Map<String, List<String>> props) {
	    this.name = name;
	    this.props = props;
	    session.getLogger().info(JOVALMsg.STATUS_XINETD_SERVICE, name);
	}

	String getName() {
	    return name;
	}

	List<String> get(Property prop) throws NoSuchElementException {
	    if (props.containsKey(prop.value())) {
		return props.get(prop.value());
	    }
	    throw new NoSuchElementException(prop.value());
	}

	String getString(Property prop) throws NoSuchElementException {
	    if (props.containsKey(prop.value())) {
		StringBuffer sb = new StringBuffer();
		for (String s : props.get(prop.value())) {
		    if (sb.length() > 0) {
			sb.append(" ");
		    }
		    sb.append(s);
		}
		return sb.toString();
	    }
	    throw new NoSuchElementException(prop.value());
	}
    }
}
