// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.arf;

/**
 * This class provides a consolidated access point for accessing all of the ARF schema JAXB object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Factories {
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
