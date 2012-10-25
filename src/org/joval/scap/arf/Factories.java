// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.arf;

import javax.xml.namespace.QName;

/**
 * This class provides a consolidated access point for accessing all of the ARF schema JAXB object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Factories {
    public static final String VOCABULARY_URI = "http://scap.nist.gov/vocabulary/arf/relationships/1.0#";

    public static final QName IS_ABOUT =	new QName(VOCABULARY_URI, "isAbout");
    public static final QName CREATED_FOR =	new QName(VOCABULARY_URI, "createdFor");

    /**
     * Facilitates access to the ARF core schema ObjectFactory.
     */
    public static arf.schemas.core.ObjectFactory core = new arf.schemas.core.ObjectFactory();

    /**
     * Facilitates access to the ARF reporting schema ObjectFactory.
     */
    public static arf.schemas.reporting.ObjectFactory reporting = new arf.schemas.reporting.ObjectFactory();

    /**
     * Facilitates access to the AI variables schema ObjectFactory.
     */
    public static ai.schemas.core.ObjectFactory asset = new ai.schemas.core.ObjectFactory();
}
