// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.xml;

import java.util.Hashtable;
import java.util.Set;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;

/**
 * Ovaldi doesn't like interoperating with the XML files that the Marshaller spits out unassisted, so this class helps
 * things along so that the files are completely interoperable.
 *
 * @see http://jaxb.java.net/nonav/2.2.1/docs/vendorProperties.html
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OvalNamespacePrefixMapper extends NamespacePrefixMapper {
    public enum URI {
	SC("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5"),
	RES("http://oval.mitre.org/XMLSchema/oval-results-5");

	private String uri;

	private URI(String uri) {
	    this.uri = uri;
	}

	private String getUri() {
	    return uri;
	}
    }

    public static void configure(Marshaller marshaller, URI uri) throws PropertyException {
	marshaller.setProperty(PREFIXMAPPER_PROP, new OvalNamespacePrefixMapper(uri, marshaller));
	marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    }

    // Overrides

    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
	return namespaceMap.get(namespaceUri);
    }

/*
    public String[] getPreDeclaredNamespaceUris() {
	return namespaceMap.keySet().toArray(new String[namespaceMap.size()]);
    }

    public String[] getPreDeclaredNamespaceUris2() {
	String[] sa = new String[namespaceMap.size() * 2];
	int index = 0;
	Set<String> keys = namespaceMap.keySet();
	for (String key : keys) {
	    sa[index++] = namespaceMap.get(key);
	    sa[index++] = key;
	}
	return sa;
    }
*/

    // Private

    private static String PREFIXMAPPER_PROP	= "com.sun.xml.internal.bind.namespacePrefixMapper";

    private static String COMMON_LOCATION	= "http://oval.mitre.org/XMLSchema/oval-common-5 " +
						  "oval-common-schema.xsd";

    private static String DEF_SCHEMA_LOCATION	= "http://oval.mitre.org/XMLSchema/oval-definitions-5 " +
						  "oval-definitions-schema.xsd";

    private static String SC_SCHEMA_LOCATION	= "http://oval.mitre.org/XMLSchema/oval-system-characteristics-5 " +
						  "oval-system-characteristics-schema.xsd";

    private static String RES_SCHEMA_LOCATION	= "http://oval.mitre.org/XMLSchema/oval-results-5 " +
						  "oval-results-schema.xsd " +
						  SC_SCHEMA_LOCATION + " " + DEF_SCHEMA_LOCATION;

    private Hashtable<String, String> namespaceMap;

    private OvalNamespacePrefixMapper(URI uri, Marshaller marshaller) throws PropertyException {
	namespaceMap = new Hashtable<String, String>();

	namespaceMap.put(uri.getUri(), "");
	namespaceMap.put("http://www.w3.org/2001/XMLSchema-instance",		"xsi");
	namespaceMap.put("http://www.w3.org/2000/09/xmldsig#",			"xmldsig");

	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-common-5",	"oval");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5",	"oval-def");

	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#aix",		"aix-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#apache",	"apache-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#catos",		"catos-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#esx",		"esx-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#freebsd",	"freebsd-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#hpux",		"hpux-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#independent",	"ind-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#ios",		"ios-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#junos",		"junos-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#linux",		"linux-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#macos",		"macos-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#netconf",	"netconf-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#pixos",		"pixos-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#sharepoint",	"sharepoint-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#solaris",	"solaris-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#unix",		"unix-sc");
	namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5#windows",	"win-sc");

	switch(uri) {
	  case SC:
	    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SC_SCHEMA_LOCATION + " " + COMMON_LOCATION);
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5",	"");
	    break;

	  case RES:
	    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, RES_SCHEMA_LOCATION + " " + COMMON_LOCATION);
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-system-characteristics-5",	"oval-sc");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-results-5",			"");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#aix",		"aix-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#apache",	"apache-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#catos",	"catos-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#esx",		"esx-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#freebsd",	"freebsd-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#hpux",		"hpux-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#independent",	"ind-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#ios",		"ios-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#junos",	"junos-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#linux",	"linux-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#macos",	"macos-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#netconf",	"netconf-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#pixos",	"pixos-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#sharepoint",	"sharepoint-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris",	"solaris-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris",	"solaris-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#solaris",	"solaris-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#unix",		"unix-def");
	    namespaceMap.put("http://oval.mitre.org/XMLSchema/oval-definitions-5#windows",	"win-def");
	    break;
	}
    }
}
