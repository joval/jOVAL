// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;

import jsaf.intf.util.IProperty;
import jsaf.util.IniFile;

import org.joval.util.JOVALMsg;

/**
 * This class is used to retrieve JAXB package mappings for SCAP schemas.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public enum SchemaRegistry {
    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * definitions schema.
     */
    OVAL_DEFINITIONS("OVAL_DEFINITIONS"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * results schema.
     */
    OVAL_RESULTS("OVAL_RESULTS"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * system characteristics schema.
     */
    OVAL_SYSTEMCHARACTERISTICS("OVAL_SC"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * variables schema.
     */
    OVAL_VARIABLES("OVAL_VARIABLES"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * evaluation-id schema.
     */
    OVAL_EVALUATION_ID("OVAL_EVALUATION"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * directives schema.
     */
    OVAL_DIRECTIVES("OVAL_DIRECTIVES"),

    /**
     * Property indicating the package names for classes in the XCCDF (eXtensible Configuration Checklist Description Format)
     * schema.
     */
    XCCDF("XCCDF"),

    /**
     * Property indicating the package names for classes in the OCIL (Open Checklist Interactive Language) schema.
     */
    OCIL("OCIL"),

    /**
     * Property indicating the package names for classes in the CPE (Common Platform Enumeration) schema.
     */
    CPE("CPE"),

    /**
     * Property indicating the package names for classes in the DS (SCAP Data Stream) schema.
     */
    DS("DATASTREAM"),

    /**
     * Property indicating the package names for classes in the ARF (Asset Reporting Format) schema.
     */
    ARF("ARF"),

    /**
     * Property indicating the package names for classes in the SCE (Script Check Engine) schema.
     */
    SCE("SCE"),

    /**
     * Property indicating the package names for classes in the jOVAL diagnostic schema.
     */
    DIAGNOSTIC("DIAGNOSTIC");

    /**
     * Interface for members of the enum.
     */
    public interface ISchema {
	JAXBContext getJAXBContext() throws JAXBException;
	Marshaller createMarshaller() throws JAXBException;
    }

    /**
     * Create JAXBContext objects for the registry in a separate thread.
     */
    public static void initializeInThread() {
	if (initializing) {
	    return;
	} else {
	    initThread = new Thread(new Initializer(), "SchemaRegistry JAXB Initializer");
	    initThread.start();
	}
    }

    /**
     * Obtain a SchemaRegistry instance based on its group name.  If there is no built-in member of the enumeration
     * with the specified name, a new instance will be returned with that corresponding name.  This makes it possible
     * for the registry to support custom extended JAXB groups.
     *
     * @throws NoSuchElementException if no schema has been registered that is a member of the specified group
     */
    public static ISchema getGroup(String groupName) throws NoSuchElementException {
	for (SchemaRegistry sr : values()) {
	    if (groupName.equals(sr.impl.groupName)) {
		return sr.impl;
	    }
	}
	if (groupPackages.containsKey(groupName)) {
	    return new SchemaImpl(groupName);
	} else {
	    throw new NoSuchElementException(groupName);
	}
    }

    /**
     * Register additional schemas.
     *
     * @param in an InputStream to an INI-format file.
     * @see schema-registry.ini
     */
    public static final void register(InputStream in) {
	try {
	    Collection<String> changedGroups = new HashSet<String>();

	    IniFile ini = new IniFile(in);
	    for (String prefix : ini.listSections()) {
		IProperty props = ini.getSection(prefix);

		//
		// Set the preferred prefix for the namespace, if possible
		//
		String ns = props.getProperty("namespace");
		if (ns2Prefix.containsKey(ns)) {
		    if (!ns2Prefix.get(ns).equals(prefix)) {
			JOVALMsg.getLogger().warn(JOVALMsg.WARNING_NS_PREFIX, ns, prefix, ns2Prefix.get(ns));
			prefix = ns2Prefix.get(ns);
		    }
		} else {
		    ns2Prefix.put(ns, prefix);
		}

		String packageName = props.getProperty("package");
		StringTokenizer tok = new StringTokenizer(props.getProperty("groups"), ",");
		while (tok.hasMoreTokens()) {
		    String group = tok.nextToken().trim();
		    if (!groupPackages.containsKey(group)) {
			groupPackages.put(group, new HashSet<String>());
		    }
		    if (!groupPackages.get(group).contains(packageName)) {
			groupPackages.get(group).add(packageName);
			changedGroups.add(group);
		    }
		    if (!groupLocations.containsKey(group)) {
			groupLocations.put(group, new HashSet<String>());
		    }
		    groupLocations.get(group).add(props.getProperty("location"));
		}
	    }

	    //
	    // Re-set the JAXBContext for any groups whose member packages have been altered by the registration
	    //
	    for (String group : changedGroups) {
		for (SchemaRegistry sr : values()) {
		    if (group.equals(sr.impl.groupName)) {
			sr.impl.ctx = null;
			break;
		    }
		}
	    }
	} catch (IOException e) {
	    throw new RuntimeException(e);
	} finally {
	    try {
		in.close();
	    } catch (IOException e) {
	    }
	}
    }

    // Implement ISchema (although it's undeclared, as an enum can't implement its own inner class)

    /**
     * Obtain the JAXBContext for the schema.
     */
    public JAXBContext getJAXBContext() throws JAXBException {
	return impl.getJAXBContext();
    }

    public Marshaller createMarshaller() throws JAXBException {
	return impl.createMarshaller();
    }

    // Private

    private static final String PREFIXMAPPER_PROP = "com.sun.xml.internal.bind.namespacePrefixMapper";

    private static Map<String, Collection<String>> groupPackages;
    private static Map<String, Collection<String>> groupLocations;
    private static Map<String, String> ns2Prefix;
    static {
	groupPackages = new HashMap<String, Collection<String>>();
	groupLocations = new HashMap<String, Collection<String>>();
	ns2Prefix = new HashMap<String, String>();

	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	InputStream in = cl.getResourceAsStream("schema-registry.ini");
	if (in == null) {
	    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, "schema-registry.ini"));
	} else {
	    register(in);
	}
    }

    private SchemaImpl impl;

    private SchemaRegistry(String groupName) {
	impl = new SchemaImpl(groupName);
    }

    private static class SchemaImpl extends NamespacePrefixMapper implements ISchema {
	String groupName;
	JAXBContext ctx;

	SchemaImpl(String groupName) {
	    super();
	    this.groupName = groupName;
	}

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
	    if (ns2Prefix.containsKey(namespaceUri)) {
		return ns2Prefix.get(namespaceUri);
	    } else {
		return suggestion;
	    }
	}

	// Implement ISchema

	public synchronized JAXBContext getJAXBContext() throws JAXBException {
	    if (ctx == null) {
		StringBuffer sb = new StringBuffer();
		for (String packageName : groupPackages.get(groupName)) {
		    if (sb.length() > 0) {
			sb.append(":");
		    }
		    sb.append(packageName);
		}
		ctx = JAXBContext.newInstance(sb.toString());
	    }
	    return ctx;
	}

	public Marshaller createMarshaller() throws JAXBException {
	    Marshaller marshaller = getJAXBContext().createMarshaller();
	    StringBuffer sb = new StringBuffer();
	    for (String location : groupLocations.get(groupName)) {
		if (sb.length() > 0) {
		    sb.append(" ");
		}
		sb.append(location);
	    }
	    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, sb.toString());
	    try {
		marshaller.setProperty(PREFIXMAPPER_PROP, this);
	    } catch (PropertyException e) {
		// Not the end of the world, really.
	    }
	    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
	    return marshaller;
	}
    }

    private static boolean initializing = false;
    private static Thread initThread = null;

    private static class Initializer implements Runnable {
	public void run() {
	    SchemaRegistry.initializing = true;
	    try {
		for (SchemaRegistry sr : SchemaRegistry.values()) {
		    JOVALMsg.getLogger().debug(JOVALMsg.STATUS_XML_INIT, sr.toString());
		    sr.getJAXBContext();
		}
	    } catch (Exception e) {
		JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    } finally {
		SchemaRegistry.initializing = false;
		SchemaRegistry.initThread = null;
	    }
	}
    }
}
