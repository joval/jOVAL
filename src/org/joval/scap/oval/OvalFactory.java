// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.GregorianCalendar;
import java.util.List;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;

import scap.oval.common.GeneratorType;
import scap.oval.results.OvalResults;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IOvalEngine;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.oval.IVariables;
import org.joval.scap.oval.engine.Engine;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * A convenience class for creating OVAL management objects.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class OvalFactory {
    public static IDefinitions createDefinitions(URL url) throws OvalException {
	try {
	    return new Definitions(Definitions.getOvalDefinitions(url.openStream()));
	} catch (IOException e) {
	    throw new OvalException(e);
	}
    }

    public static IDefinitions createDefinitions(File f) throws OvalException {
	return new Definitions(f);
    }

    public static IDefinitionFilter createDefinitionFilter(File f) throws OvalException {
	return new DefinitionFilter(f);
    }

    public static IDefinitionFilter createDefinitionFilter(List<String> ids) throws OvalException {
	return new DefinitionFilter(ids);
    }

    /**
     * Create an empty Definition filter.
     */
    public static IDefinitionFilter createDefinitionFilter() {
	return new DefinitionFilter();
    }

    public static IVariables createVariables(File f) throws OvalException {
	return new Variables(f);
    }

    /**
     * Create empty variables.
     */
    public static IVariables createVariables() {
	return new Variables();
    }

    public static ISystemCharacteristics createSystemCharacteristics(File f) throws OvalException {
	return new SystemCharacteristics(f);
    }

    /**
     * Create an IResults from a file.
     */
    public static IResults createResults(File f) throws OvalException {
	return new Results(Results.getOvalResults(f));
    }

    /**
     * Create an IResults from an existing data model OVAL result.
     */
    public static IResults createResults(OvalResults results) throws OvalException {
	return new Results(results);
    }

    public static IOvalEngine createEngine(IOvalEngine.Mode mode, IPlugin plugin) {
	return new OEMEngine(mode, plugin);
    }

    public static GeneratorType getGenerator() {
	GeneratorType generator = Factories.common.createGeneratorType();
	generator.setProductName(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_PRODUCT));
	generator.setProductVersion(JOVALSystem.getSystemProperty(JOVALSystem.SYSTEM_PROP_VERSION));
	generator.setSchemaVersion(IOvalEngine.SCHEMA_VERSION.toString());
	try {
	    generator.setTimestamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
	} catch (DatatypeConfigurationException e) {
	    JOVALMsg.getLogger().warn(JOVALMsg.ERROR_TIMESTAMP);
	    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return generator;
    }

    // Private

    private static class OEMEngine extends Engine {
	OEMEngine(IOvalEngine.Mode mode, IPlugin plugin) {
	    super(mode, plugin);
	}
    }
}
