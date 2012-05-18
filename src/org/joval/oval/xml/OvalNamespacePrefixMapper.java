// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Set;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import com.sun.xml.internal.bind.marshaller.NamespacePrefixMapper;

import org.joval.intf.util.IProperty;
import org.joval.util.IniFile;
import org.joval.util.JOVALMsg;

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
    private static final String PREFIXMAPPER_PROP	= "com.sun.xml.internal.bind.namespacePrefixMapper";
    private static final String NAMESPACE		= "namespace";
    private static final String LOCATION		= "location";
    private static final String RESOURCE		= "oval_xml.ini";

    private static final String DEF_BASE		= "def";
    private static final String SC_BASE			= "sc";

    private StringBuffer locations;
    private Hashtable<String, String> namespaces;

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
	return namespaces.get(namespaceUri);
    }

    // Private

    private OvalNamespacePrefixMapper(URI uri, Marshaller marshaller) throws PropertyException {
	locations = new StringBuffer();
	namespaces = new Hashtable<String, String>();
	namespaces.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");

	try {
	    switch(uri) {
	      case SC:
		loadData(getData(SC_BASE));
		namespaces.put(URI.SC.getUri(), "");
		break;

	      case RES:
		loadData(getData(DEF_BASE));
		loadData(getData(SC_BASE));
		namespaces.put(URI.RES.getUri(), "");
		break;
	    }
	} catch (IOException e) {
	    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, locations.toString());
    }

    private void loadData(IniFile data) {
	for (String prefix : data.listSections()) {
	    IProperty section = data.getSection(prefix);
	    String namespace = section.getProperty(NAMESPACE);
	    if (namespace != null) {
		namespaces.put(namespace, prefix);
		if (locations.length() > 0) {
		    locations.append(" ");
		}
		locations.append(namespace).append(" ").append(section.getProperty(LOCATION));
	    }
	}
    }

    private IniFile getData(String base) throws IOException {
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	String resName = "oval-" + base + ".ini";
	InputStream rsc = cl.getResourceAsStream("oval-" + base + ".ini");
	if (rsc == null) {
	    throw new IOException(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, resName));
	} else {
	    return new IniFile(rsc);
	}
    }
}
