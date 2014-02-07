// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.unix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;
import javax.xml.bind.JAXBElement;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jsaf.Message;
import jsaf.intf.io.IFile;
import jsaf.intf.io.IFilesystem;
import jsaf.intf.system.ISession;
import jsaf.intf.unix.system.IUnixSession;
import jsaf.io.StreamTool;
import jsaf.util.SafeCLI;
import jsaf.util.StringTools;

import scap.oval.common.MessageType;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.unix.GconfObject;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemBoolType;
import scap.oval.systemcharacteristics.core.EntityItemIntType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.unix.EntityItemGconfTypeType;
import scap.oval.systemcharacteristics.unix.GconfItem;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.XSITools;

/**
 * Resolves Gconf OVAL objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class GconfAdapter implements IAdapter {
    public static final String GCONF_TOOL = "/usr/bin/gconftool-2";
    public static final String DEFAULT_SOURCE = "";

    private IUnixSession session;
    private Map<String, GconfConfig> config;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IUnixSession) {
	    IUnixSession us = (IUnixSession)session;
	    try {
		if (us.getFilesystem().getFile(GCONF_TOOL).exists()) {
		    this.session = us;
		    config = new HashMap<String, GconfConfig>();
		    classes.add(GconfObject.class);
		} else {
		    notapplicable.add(GconfObject.class);
		}
	    } catch (IOException e) {
		// Gconf objects will not be checked
		session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	} else {
	    notapplicable.add(GconfObject.class);
	}
	return classes;
    }

    public Collection<GconfItem> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	GconfObject gObj = (GconfObject)obj;
	String source = null;
	if (XSITools.isNil(gObj.getSource())) {
	    source = DEFAULT_SOURCE;
	} else {
	    OperationEnumeration op = gObj.getSource().getValue().getOperation();
	    switch(op) {
	      case EQUALS:
		source = (String)gObj.getSource().getValue().getValue();
		break;
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	}
	GconfConfig config = getConfig(source);

	Collection<GconfItem> items = new ArrayList<GconfItem>();
	try {
	    String key = (String)gObj.getKey().getValue();
	    OperationEnumeration op = gObj.getKey().getOperation();
	    switch(op) {
	      case EQUALS:
		items.add(config.item(key));
		break;
	      case CASE_INSENSITIVE_EQUALS:
		for (String s : config.keys()) {
		    if (key.equalsIgnoreCase(s)) {
			items.add(config.item(s));
		    }
		}
		break;
	      case NOT_EQUAL:
		for (String s : config.keys()) {
		    if (!key.equals(s)) {
			items.add(config.item(s));
		    }
		}
		break;
	      case PATTERN_MATCH: {
		Pattern p = StringTools.pattern(key);
		for (String s : config.keys()) {
		    if (p.matcher(s).find()) {
			items.add(config.item(s));
		    }
		}
		break;
	      }
	      default:
		String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
		throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	    }
	} catch (PatternSyntaxException e) {
	    session.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_PATTERN, e.getMessage()), FlagEnumeration.ERROR);
	} catch (NoSuchElementException e) {
	}
	return items;
    }

    // Private

    private GconfConfig getConfig(String source) throws CollectException {
	if (config.containsKey(source)) {
	    return config.get(source);
	} else {
	    GconfConfig gc = new GconfConfig(source);
	    config.put(source, gc);
	    return gc;
	}
    }

    class GconfConfig {
	private JAXBElement<EntityItemStringType> source;
	private Map<String, Node> entries;

	public GconfConfig(String source) throws CollectException {
	    entries = new HashMap<String, Node>();
	    File local = null;
	    try {
		StringBuffer cmd = new StringBuffer("gconftool-2");
		if (!DEFAULT_SOURCE.equals(source)) {
		    EntityItemStringType sourceType = Factories.sc.core.createEntityItemStringType();
		    sourceType.setValue(source);
		    this.source = Factories.sc.unix.createGconfItemSource(sourceType);
		    cmd.append(" --config-source ").append(source);
		}
		cmd.append(" --dump /");
		String unique = null;
		synchronized(session) {
		    unique = Long.toString(System.currentTimeMillis());
		    Thread.sleep(1);
		}
		String tempPath = session.getTempDir();
		IFilesystem fs = session.getFilesystem();
		if (!tempPath.endsWith(fs.getDelimiter())) {
		    tempPath = tempPath + fs.getDelimiter();
		}
		tempPath = new StringBuffer(tempPath).append("cmd.").append(unique).append(".out").toString();
		tempPath = session.getEnvironment().expand(tempPath);
		cmd.append(" | gzip > ").append(tempPath);
		SafeCLI.exec(cmd.toString(), session, IUnixSession.Timeout.M);
		if (ISession.LOCALHOST.equals(session.getHostname())) {
		    local = new File(tempPath);
		} else {
		    IFile remoteTemp = fs.getFile(tempPath, IFile.Flags.READWRITE);
		    File tempDir = session.getWorkspace();
		    if (tempDir == null) {
			tempDir = new File(System.getProperty("user.home"));
		    }
		    local = File.createTempFile("cmd", null, tempDir);
		    StreamTool.copy(remoteTemp.getInputStream(), new FileOutputStream(local), true);
		    try {
			remoteTemp.delete();
		    } catch (IOException e) {
			try {
			    if (remoteTemp.exists()) {
				SafeCLI.exec("rm -f " + tempPath, session, ISession.Timeout.S);
			    }
			} catch (Exception e2) {
			    session.getLogger().warn(Message.getMessage(Message.ERROR_EXCEPTION), e2);
			}
		    }
		}
		NodeList nodes = DOMTools.parse(new GZIPInputStream(new FileInputStream(local))).getElementsByTagName("entry");
		for (int i=0; i < nodes.getLength(); i++) {
		    String key = null;
		    Node n = nodes.item(i);
		    NodeList children = n.getChildNodes();
		    for (int j=0; j < children.getLength(); j++) {
			Node child = children.item(j);
			if ("key".equals(child.getNodeName())) {
			    key = child.getTextContent();
			    break;
			}
		    }
		    if (key != null) {
			entries.put(key, n);
		    }
		}
	    } catch (Exception e) {
		throw new CollectException(e, FlagEnumeration.ERROR);
	    } finally {
		if (local != null) {
		    local.delete();
		}
	    }
	}

	public Collection<String> keys() {
	    return entries.keySet();
	}

	public GconfItem item(String key) throws NoSuchElementException {
	    if (!entries.containsKey(key)) {
		throw new NoSuchElementException(key);
	    }

	    GconfItem item = Factories.sc.unix.createGconfItem();
	    item.setSource(source);

	    EntityItemStringType keyType = Factories.sc.core.createEntityItemStringType();
	    keyType.setValue(key);
	    item.setKey(keyType);

	    EntityItemStringType modUserType = Factories.sc.core.createEntityItemStringType();
	    modUserType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    item.setModUser(modUserType);

	    EntityItemIntType modTimeType = Factories.sc.core.createEntityItemIntType();
	    modTimeType.setDatatype(SimpleDatatypeEnumeration.INT.value());
	    modTimeType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    item.setModTime(modTimeType);

	    EntityItemBoolType isWritableType = Factories.sc.core.createEntityItemBoolType();
	    isWritableType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    isWritableType.setStatus(StatusEnumeration.NOT_COLLECTED);
	    item.setIsWritable(isWritableType);

	    Node entry = entries.get(key);
	    Node value = getChild(entry, "value");
	    EntityItemGconfTypeType typeType = Factories.sc.unix.createEntityItemGconfTypeType();
	    if (value == null) {
		typeType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		typeType.setValue(GconfTypeEnumeration.getType(value).toString());
		item.getValue().addAll(getValues(value));
	    }
	    item.setType(typeType);

	    Node schema = getChild(entry, "schema_key");
	    EntityItemBoolType isDefaultType = Factories.sc.core.createEntityItemBoolType();
	    isDefaultType.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
	    if (schema == null) {
		isDefaultType.setStatus(StatusEnumeration.DOES_NOT_EXIST);
	    } else {
		List<EntityItemAnySimpleType> defaults = getValues(getChild(schema, "value"));
		if (defaults.size() == item.getValue().size()) {
		    isDefaultType.setValue("1");
		    for (int i=0; i < defaults.size(); i++) {
			if (!defaults.get(i).getValue().equals(item.getValue().get(i).getValue())) {
			    isDefaultType.setValue("0");
			    break;
			}
		    }
		} else {
		    isDefaultType.setValue("0");
		}
	    }
	    item.setIsDefault(isDefaultType);

	    return item;
	}

	// Private

	private List<EntityItemAnySimpleType> getValues(Node value) {
	    List<EntityItemAnySimpleType> result = new ArrayList<EntityItemAnySimpleType>();
	    if (value != null) {
		GconfTypeEnumeration gtype = GconfTypeEnumeration.getType(value);
		switch(gtype) {
		  case GCONF_VALUE_STRING: {
		    EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
		    val.setDatatype(SimpleDatatypeEnumeration.STRING.value());
		    val.setValue(getChild(value, "string").getTextContent());
		    result.add(val);
		    break;
		  }
		  case GCONF_VALUE_INT: {
		    EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
		    val.setDatatype(SimpleDatatypeEnumeration.INT.value());
		    val.setValue(getChild(value, "int").getTextContent());
		    result.add(val);
		    break;
		  }
		  case GCONF_VALUE_FLOAT: {
		    EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
		    val.setDatatype(SimpleDatatypeEnumeration.FLOAT.value());
		    val.setValue(getChild(value, "float").getTextContent());
		    result.add(val);
		    break;
		  }
		  case GCONF_VALUE_BOOL: {
		    EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
		    val.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
		    val.setValue(getChild(value, "bool").getTextContent());
		    result.add(val);
		    break;
		  }
		  case GCONF_VALUE_LIST:
		  case GCONF_VALUE_PAIR: {
		    NodeList children = ((Element)value).getElementsByTagName("value");
		    for (int i=0; i < children.getLength(); i++) {
			EntityItemAnySimpleType val = Factories.sc.core.createEntityItemAnySimpleType();
			Node child = children.item(i);
			switch(GconfTypeEnumeration.getType(child)) {
			  case GCONF_VALUE_STRING:
			    val.setDatatype(SimpleDatatypeEnumeration.STRING.value());
			    val.setValue(getChild(child, "string").getTextContent());
			    break;
			  case GCONF_VALUE_INT:
			    val.setDatatype(SimpleDatatypeEnumeration.INT.value());
			    val.setValue(getChild(child, "int").getTextContent());
			    break;
			  case GCONF_VALUE_FLOAT:
			    val.setDatatype(SimpleDatatypeEnumeration.FLOAT.value());
			    val.setValue(getChild(child, "float").getTextContent());
			    break;
			  case GCONF_VALUE_BOOL:
			    val.setDatatype(SimpleDatatypeEnumeration.BOOLEAN.value());
			    val.setValue(getChild(child, "bool").getTextContent());
			    break;
			}
			result.add(val);
		    }
		    break;
		  }
		  case GCONF_VALUE_SCHEMA: {
		    return getValues(
			getChild(getChild(getChild(getChild(value, "schema"), "locale"), "default_value"), "value")
		    );
		  }
		}
	    }
	    return result;
	}
    }

    enum GconfTypeEnumeration {
	GCONF_VALUE_STRING,
	GCONF_VALUE_INT,
	GCONF_VALUE_FLOAT,
	GCONF_VALUE_BOOL,
	GCONF_VALUE_SCHEMA,
	GCONF_VALUE_LIST,
	GCONF_VALUE_PAIR;

	static GconfTypeEnumeration getType(Node node) throws IllegalArgumentException {
	    if ("value".equals(node.getNodeName())) {
		NodeList list = node.getChildNodes();
		for (int i=0; i < list.getLength(); i++) {
		    Node child = list.item(i);
		    if ("string".equals(child.getNodeName())) {
			return GCONF_VALUE_STRING;
		    } else if ("bool".equals(child.getNodeName())) {
			return GCONF_VALUE_BOOL;
		    } else if ("int".equals(child.getNodeName())) {
			return GCONF_VALUE_INT;
		    } else if ("float".equals(child.getNodeName())) {
			return GCONF_VALUE_FLOAT;
		    } else if ("list".equals(child.getNodeName())) {
			return GCONF_VALUE_LIST;
		    } else if ("schema".equals(child.getNodeName())) {
			return GCONF_VALUE_SCHEMA;
		    } else if ("pair".equals(child.getNodeName())) {
			return GCONF_VALUE_PAIR;
		    }
		}
	    }
	    throw new IllegalArgumentException(node.getNodeName());
	}
    }

    private static final Node getChild(Node node, String name) {
	if (node != null) {
	    NodeList list = node.getChildNodes();
	    for (int i=0; i < list.getLength(); i++) {
		Node child = list.item(i);
		if (child.getNodeName().equals(name)) {
		    return child;
		}
	    }
	}
	return null;
    }
}
