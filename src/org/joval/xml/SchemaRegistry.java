// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.joval.util.JOVALMsg;

/**
 * This class is used to retrieve JAXB package mappings for SCAP schemas.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SchemaRegistry {
    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * definitions schema.
     */
    public static final String OVAL_DEFINITIONS = "oval.definitions.packages";

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * results schema.
     */
    public static final String OVAL_RESULTS = "oval.results.packages";

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * system characteristics schema.
     */
    public static final String OVAL_SYSTEMCHARACTERISTICS = "oval.systemcharacteristics.packages";

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * variables schema.
     */
    public static final String OVAL_VARIABLES = "oval.variables.packages";

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * evaluation-id schema.
     */
    public static final String OVAL_EVALUATION_ID = "oval.evaluation-id.packages";

    /**
     * Property indicating the package names for classes in the OVAL (Open Vulnerability and Assessment Language)
     * directives schema.
     */
    public static final String OVAL_DIRECTIVES = "oval.directives.packages";

    /**
     * Property indicating the package names for classes in the XCCDF (eXtensible Configuration Checklist Description Format)
     * schema.
     */
    public static final String XCCDF = "xccdf.packages";

    /**
     * Property indicating the package names for classes in the CPE (Common Platform Enumeration) schema.
     */
    public static final String OCIL = "ocil.packages";

    /**
     * Property indicating the package names for classes in the CPE (Common Platform Enumeration) schema.
     */
    public static final String CPE = "cpe.packages";

    /**
     * Property indicating the package names for classes in the DS (SCAP Data Stream) schema.
     */
    public static final String DS = "ds.packages";

    /**
     * Property indicating the package names for classes in the ARF (Asset Reporting Format) schema.
     */
    public static final String ARF = "arf.packages";

    /**
     * Property indicating the package names for classes in the SVRL (Schematron Validation Report Language) schema.
     */
    public static final String SVRL = "svrl.packages";

    /**
     * Property resource files, containing JAXB package information.
     */
    private static final String[] RESOURCES = {	"arf.properties",
						"ds.properties",
						"oval.properties",
						"cpe.properties",
						"xccdf.properties",
						"ocil.properties",
						"svrl.properties" };

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
		    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), res);
		}
	    }
	}
    }

    /**
     * Retrieve package names from the registry.
     *
     * @param name specify one of the OVAL_*, CPE, XCCDF, OCIL, DS or SVRL Strings defined by this class.
     */
    public static String lookup(String name) throws NoSuchElementException {
	return schemaProps.getProperty(name);
    }
}
