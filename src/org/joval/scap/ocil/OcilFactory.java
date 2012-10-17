// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.ocil;

import java.math.BigDecimal;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import ocil.schemas.core.GeneratorType;

/**
 * This class provides a consolidated access point for accessing all of the OCIL schema JAXB object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OcilFactory {
    public static BigDecimal SCHEMA_VERSION = new BigDecimal("2.0");

    /**
     * Get an author-less OCIL generator object.
     */
    public static GeneratorType getGenerator() {
	GeneratorType generator = Factories.core.createGeneratorType();
	generator.setProductName("XPERT by jOVAL.org");
	generator.setProductVersion("1.0");
	generator.setSchemaVersion(SCHEMA_VERSION);
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    e.printStackTrace();
	}
	return generator;
    }
}
