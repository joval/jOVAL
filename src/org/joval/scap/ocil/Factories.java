// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.ocil;

/**
 * This class provides a consolidated access point for accessing all of the OCIL schema JAXB object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Factories {
    /**
     * Facilitates access to the OCIL core schema ObjectFactory.
     */
    public static scap.ocil.core.ObjectFactory core = new scap.ocil.core.ObjectFactory();

    /**
     * Facilitates access to the OCIL variables schema ObjectFactory.
     */
    public static scap.ocil.variables.ObjectFactory variables = new scap.ocil.variables.ObjectFactory();
}
