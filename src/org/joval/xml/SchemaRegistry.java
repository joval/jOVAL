// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;
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
	Validator getValidator() throws SAXException, IOException;
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
     * @param basename The package basename under which, visible to the static initializer's context ClassLoader, the
     *                 resources [basename]/registry.ini and [basename]/schemas.cat will be found.
     *
     * @see registry.ini
     * @see schemas.cat
     */
    public static final void register(String basename) throws IOException {
	String registry = new StringBuffer(basename).append("/registry.ini").toString();
	String catalog = new StringBuffer(basename).append("/schemas.cat").toString();

	InputStream in = null;
	try {
	    Collection<String> changedGroups = new HashSet<String>();

	    //
	    // Process the registry
	    //
	    in = CL.getResourceAsStream(registry);
	    if (in == null) {
		throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, registry));
	    }
	    IniFile ini = new IniFile(in);
	    for (String prefix : ini.listSections()) {
		IProperty props = ini.getSection(prefix);
		String ns = props.getProperty("namespace");

		//
		// Set the preferred prefix for the namespace, if possible
		//
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
		    if (!groupNamespaces.containsKey(group)) {
			groupNamespaces.put(group, new HashSet<String>());
		    }
		    groupNamespaces.get(group).add(ns);
		}
	    }

	    //
	    // If any groups had member packages altered by the registration, reset their Validator and JAXBContext,
	    // so they can be re-generated the next time they're retrieved.
	    //
	    for (String group : changedGroups) {
		for (SchemaRegistry sr : values()) {
		    if (group.equals(sr.impl.groupName)) {
			sr.impl.validator = null;
			sr.impl.ctx = null;
			break;
		    }
		}
	    }

	    //
	    // Process the TR9401 catalog
	    //
	    in = CL.getResourceAsStream(catalog);
	    if (in == null) {
		throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, catalog));
	    }
	    String line = null;
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    while ((line = reader.readLine()) != null) {
		if (line.startsWith("PUBLIC")) {
		    StringTokenizer tok = new StringTokenizer(line.substring(6).trim());
		    String publicId = tok.nextToken();
		    publicId = publicId.substring(1, publicId.length() - 1); // Strip "'s
		    String location = tok.nextToken();
		    location = location.substring(1, location.length() - 1); // Strip "'s
		    if (!publicId2URL.containsKey(publicId)) {
			URL url = CL.getResource(basename + "/" + location);
			if (url != null) {
			    publicId2URL.put(publicId, url);
			}
		    }
		} else if (line.startsWith("SYSTEM")) {
		    StringTokenizer tok = new StringTokenizer(line.substring(6).trim());
		    String systemId = tok.nextToken();
		    systemId = systemId.substring(1, systemId.length() - 1); // Strip "'s
		    String location = tok.nextToken();
		    location = location.substring(1, location.length() - 1); // Strip "'s
		    if (!systemId2URL.containsKey(systemId)) {
			URL url = CL.getResource(basename + "/" + location);
			if (url != null) {
			    systemId2URL.put(systemId, url);
			}
		    }
		}
	    }
	} finally {
	    try {
		in.close();
	    } catch (IOException e) {
	    }
	}
    }

    public ISchema getSchema() {
	return impl;
    }

    // Implement ISchema (although it's undeclared, as an enum can't implement its own inner class)

    public Validator getValidator() throws SAXException, IOException {
	return impl.getValidator();
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return impl.getJAXBContext();
    }

    public Marshaller createMarshaller() throws JAXBException {
	return impl.createMarshaller();
    }

    // Internal

    static LSResourceResolver getLSResolver() {
	return new SchemaResolver();
    }

    static class SchemaResolver implements LSResourceResolver {
	SchemaResolver() {
	}

	// Implement LSResourceResolver

	public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
	    if ("http://www.w3.org/2001/XMLSchema".equals(type) && publicId2URL.containsKey(namespaceURI)) {
		//
		// XSD
		//
		return new SchemaInput(publicId2URL.get(namespaceURI), publicId, systemId, baseURI);

	    } else if ("http://www.w3.org/TR/REC-xml".equals(type) && systemId2URL.containsKey(systemId)) {
		//
		// DTD
		//
		return new SchemaInput(systemId2URL.get(systemId), publicId, systemId, baseURI);
	    }
	    return null;
	}
    }

    static class SchemaInput implements LSInput {
	private URL url;
	private boolean certifiedText;
	private String encoding, systemId, publicId, baseURI;

	SchemaInput(URL url, String publicId, String systemId, String baseURI) {
	    this.url = url;
	    certifiedText = false;
	    encoding = "UTF-8";
	    this.publicId = publicId;
	    this.systemId = systemId;
	    this.baseURI = baseURI;
	}

	// Implement LSInput

	public void setCharacterStream(Reader characterStream) {
	    //no-op
	}

	public Reader getCharacterStream() {
	    Reader characterStream = null;
	    try {
		characterStream = new InputStreamReader(getByteStream(), encoding);
	    } catch (IOException e) {
	    }
	    return characterStream;
	}

	public void setByteStream(InputStream byteStream) {
	    //no-op
	}

	public InputStream getByteStream() {
	    InputStream byteStream = null;
	    try {
		byteStream = url.openStream();
	    } catch (IOException e) {
	    }
	    return byteStream;
	}

	public void setStringData(String stringData) {
	    //no-op
	}

	public String getStringData() {
	    return null;
	}

	public void setSystemId(String systemId) {
	    this.systemId = systemId;
	}

	public String getSystemId() {
	    return systemId;
	}

	public void setPublicId(String publicId) {
	    this.publicId = publicId;
	}

	public String getPublicId() {
	    return publicId;
	}

	public void setBaseURI(String baseURI) {
	    this.baseURI = baseURI;
	}

	public String getBaseURI() {
	    return baseURI;
	}

	public void setEncoding(String encoding) {
	    this.encoding = encoding;
	}

	public String getEncoding() {
	    return encoding;
	}

	public void setCertifiedText(boolean certifiedText) {
	    this.certifiedText = certifiedText;
	}

	public boolean getCertifiedText() {
	    return certifiedText;
	}
    }

    // Private

    private static final String PREFIXMAPPER_PROP = "com.sun.xml.internal.bind.namespacePrefixMapper";
    private static final ClassLoader CL = Thread.currentThread().getContextClassLoader();

    private static Map<String, Collection<String>> groupPackages;
    private static Map<String, Collection<String>> groupNamespaces;
    private static Map<String, String> ns2Prefix;
    private static Map<String, URL> publicId2URL;
    private static Map<String, URL> systemId2URL;
    static {
	groupPackages = new HashMap<String, Collection<String>>();
	groupNamespaces = new HashMap<String, Collection<String>>();
	ns2Prefix = new HashMap<String, String>();
	systemId2URL = new HashMap<String, URL>();
	publicId2URL = new HashMap<String, URL>();
	try {
	    register("scap");
	} catch (IOException e) {
	    throw new RuntimeException(e);
	}
    }

    private SchemaImpl impl;

    private SchemaRegistry(String groupName) {
	impl = new SchemaImpl(groupName);
    }

    private static class SchemaImpl extends NamespacePrefixMapper implements ISchema {
	Validator validator;
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

	public Validator getValidator() throws SAXException, IOException {
	    if (validator == null) {
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		sf.setResourceResolver(getLSResolver());
		ArrayList<StreamSource> sources = new ArrayList<StreamSource>();
		for (String ns : groupNamespaces.get(groupName)) {
		    sources.add(new StreamSource(publicId2URL.get(ns).openStream()));
		}
		validator = sf.newSchema(sources.toArray(new StreamSource[sources.size()])).newValidator();
	    }
	    return validator;
	}

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

	    for (String ns : groupNamespaces.get(groupName)) {
		if (publicId2URL.containsKey(ns)) {
		    if (sb.length() > 0) {
			sb.append(" ");
		    }
		    sb.append(publicId2URL.get(ns).toString());
		}
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
