// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ocil;

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
    public static ocil.schemas.core.ObjectFactory core = new ocil.schemas.core.ObjectFactory();

    /**
     * Facilitates access to the OCIL variables schema ObjectFactory.
     */
    public static ocil.schemas.variables.ObjectFactory variables = new ocil.schemas.variables.ObjectFactory();
}
