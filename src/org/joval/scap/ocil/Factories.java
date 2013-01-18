// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.ocil;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import scap.ocil.core.GeneratorType;

import org.joval.util.JOVALSystem;

/**
 * This class provides a consolidated access point for accessing all of the OCIL schema JAXB object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Factories {
    public static BigDecimal SCHEMA_VERSION = new BigDecimal("2.0");

    /**
     * Get an author-less OCIL generator object.
     */
    public static GeneratorType getGenerator() {
	GeneratorType generator = Factories.core.createGeneratorType();
	generator.setProductName(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT));
	generator.setProductVersion(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION));
	generator.setSchemaVersion(SCHEMA_VERSION);
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    e.printStackTrace();
	}
	return generator;
    }

    /**
     * Facilitates access to the OCIL core schema ObjectFactory.
     */
    public static scap.ocil.core.ObjectFactory core = new scap.ocil.core.ObjectFactory();

    /**
     * Facilitates access to the OCIL variables schema ObjectFactory.
     */
    public static scap.ocil.variables.ObjectFactory variables = new scap.ocil.variables.ObjectFactory();
}
