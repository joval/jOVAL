// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Properties;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

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
    OVAL_DEFINITIONS("oval.definitions.packages"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * results schema.
     */
    OVAL_RESULTS("oval.results.packages"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * system characteristics schema.
     */
    OVAL_SYSTEMCHARACTERISTICS("oval.systemcharacteristics.packages"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * variables schema.
     */
    OVAL_VARIABLES("oval.variables.packages"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * evaluation-id schema.
     */
    OVAL_EVALUATION_ID("oval.evaluation-id.packages"),

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * directives schema.
     */
    OVAL_DIRECTIVES("oval.directives.packages"),

    /**
     * Property indicating the package names for classes in the XCCDF (eXtensible Configuration Checklist Description Format)
     * schema.
     */
    XCCDF("xccdf.packages"),

    /**
     * Property indicating the package names for classes in the CPE (Common Platform Enumeration) schema.
     */
    OCIL("ocil.packages"),

    /**
     * Property indicating the package names for classes in the CPE (Common Platform Enumeration) schema.
     */
    CPE("cpe.packages"),

    /**
     * Property indicating the package names for classes in the DS (SCAP Data Stream) schema.
     */
    DS("ds.packages"),

    /**
     * Property indicating the package names for classes in the ARF (Asset Reporting Format) schema.
     */
    ARF("arf.packages"),

    /**
     * Property indicating the package names for classes in the SCE (Script Check Engine) schema.
     */
    SCE("sce.packages"),

    /**
     * Property indicating the package names for classes in the SVRL (Schematron Validation Report Language) schema.
     */
    SVRL("svrl.packages");

    /**
     * Obtain the JAXBContext for the schema.
     */
    public JAXBContext getJAXBContext() throws JAXBException {
	if (ctx == null) {
	    ctx = JAXBContext.newInstance(schemaProps.getProperty(resource));
	}
	return ctx;
    }

    // Private

    private JAXBContext ctx;
    private String resource;

    private SchemaRegistry(String resource) {
	this.resource = resource;
    }

    private static final String[] RESOURCES = {	"arf.properties", "ds.properties", "oval.properties",
						"cpe.properties", "xccdf.properties", "ocil.properties",
						"sce.properties", "svrl.properties" };

    private static Properties schemaProps;
    static {
	schemaProps = new Properties();
	ClassLoader cl = Thread.currentThread().getContextClassLoader();
	for (String res : RESOURCES) {
	    InputStream rsc = cl.getResourceAsStream(res);
	    if (rsc == null) {
		JOVALMsg.getLogger().warn(JOVALMsg.ERROR_MISSING_RESOURCE, res);
	    } else {
		try {
		    schemaProps.load(rsc);
		} catch (IOException e) {
		    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
    }
}
