// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.intf.scap.xccdf;

/**
 * An enumeration of XCCDF check systems.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public enum SystemEnumeration {
    /**
     * Indicator for the OCIL check system.
     */
    OCIL("http://scap.nist.gov/schema/ocil/2"),

    /**
     * Indicator for the OVAL check system.
     */
    OVAL("http://oval.mitre.org/XMLSchema/oval-definitions-5"),

    /**
     * Indicator for the SCE check system.
     */
    SCE("http://open-scap.org/page/SCE"),

    /**
     * Not technically a check system in itself, but available here for completeness.
     */
    XCCDF("http://checklists.nist.gov/xccdf/1.2"),

    /**
     * Wildcard indicator.
     */
    ANY(null),

    /**
     * Indicator for an unsupported check system (i.e., unknown to jOVAL).
     */
    UNSUPPORTED(null);

    private String ns;

    private SystemEnumeration(String ns) {
	this.ns = ns;
    }

    public String namespace() {
	return ns;
    }
}
