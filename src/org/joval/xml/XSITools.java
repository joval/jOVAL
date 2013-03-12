// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.xml;

import javax.xml.bind.JAXBElement;

/**
 * Utility for working with XSI.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class XSITools {
    private static final boolean PRE_JAVA_17 =
	new Float("1.7").compareTo(new Float(System.getProperty("java.specification.version"))) > 0;

    /**
     * Determine whether xsi:nil=true. Pre-Java 1.7, this is accomplished by checking for a null value. For Java 1.7+,
     * this is accomplished via JAXBElement.isNil().
     */
    public static boolean isNil(JAXBElement elt) {
	return PRE_JAVA_17 ? elt.getValue() == null : elt.isNil();
    }
}
