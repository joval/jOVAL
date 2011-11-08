// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.FileHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import ch.qos.cal10n.MessageConveyorException;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import oval.schemas.evaluation.id.EvaluationDefinitionIds;

import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.oval.OvalException;
import org.joval.oval.engine.DefinitionFilter;
import org.joval.oval.engine.Definitions;
import org.joval.oval.engine.Engine;

/**
 * This class is used to retrieve JOVAL-wide resources, like SLF4J-based logging, cal10n-based messages and jOVAL and OVAL data
 * model properties and object factories.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JOVALSystem {
    public static final String OVAL_PROP_DEFINITIONS		= "definitions.packages";
    public static final String OVAL_PROP_RESULTS		= "results.packages";
    public static final String OVAL_PROP_SYSTEMCHARACTERISTICS	= "systemcharacteristics.packages";
    public static final String OVAL_PROP_VARIABLES		= "variables.packages";
    public static final String OVAL_PROP_EVALUATION_ID		= "evaluation-id.packages";
    public static final String OVAL_PROP_DIRECTIVES		= "directives.packages";

    public static final String PROP_PRODUCT	= "productName";
    public static final String PROP_VERSION	= "version";
    public static final String PROP_BUILD_DATE	= "build.date";

    public static final Factories factories = new Factories();

    private static IMessageConveyor mc;
    private static LocLogger logger;
    private static Properties props, ovalProps;

    static {
	mc = new MessageConveyor(Locale.getDefault());
	try {
	    //
	    // Get a message to test whether localized messages are available for the default Locale
	    //
	    getMessage(JOVALMsg.ERROR_EXCEPTION);
	} catch (MessageConveyorException e) {
	    //
	    // The test failed, so set the message Locale to English
	    //
	    mc = new MessageConveyor(Locale.ENGLISH);
	}
	logger = new LocLoggerFactory(mc).getLocLogger(JOVALSystem.class);
	props = new Properties();
	ovalProps = new Properties();
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    props.load(cl.getResourceAsStream("joval.system.properties"));
	    ovalProps.load(cl.getResourceAsStream("oval.properties"));
	} catch (IOException e) {
	    logger.error(getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    public static String getMessage(JOVALMsg key, Object... args) {
	return mc.getMessage(key, args);
    }

    public static String getProperty(String name) {
	return props.getProperty(name);
    }

    public static String getOvalProperty(String name) {
	return ovalProps.getProperty(name);
    }

    public static LocLogger getLogger() {
	return logger;
    }

    /**
     * Create an IDefinitionFilter based on the supplied File, which should conform to the evaluation-ids schema.
     *
     * @throws OvalException if there was an error, such as the file not conforming to the schema.
     */
    public static final IDefinitionFilter createDefinitionFilter(File f) throws OvalException {
	return new DefinitionFilter(f);
    }

    /**
     * Create an IDefinitionFilter that will accept only IDs in the supplied collection.
     */
    public static final IDefinitionFilter createAcceptFilter(Collection<String> ids) {
	return new DefinitionFilter(ids);
    }

    public static final IDefinitions createDefinitions(File f) throws OvalException {
	return new Definitions(f);
    }

    /**
     * Create an engine for evaluating OVAL definitions using a plugin.
     */
    public static final IEngine createEngine() {
	return new Engine();
    }

    //
    // Simplify access to all the OVAL Object Factories
    //

    public static class Factories {
	public oval.schemas.common.ObjectFactory common;
	public oval.schemas.directives.core.ObjectFactory directives;
	public oval.schemas.evaluation.id.ObjectFactory evaluation;
	public oval.schemas.results.core.ObjectFactory results;
	public oval.schemas.variables.core.ObjectFactory variables;
	public DefinitionFactories definitions;
	public SystemCharacteristicsFactories sc;

	Factories() {
	    common = new oval.schemas.common.ObjectFactory();
	    definitions = new DefinitionFactories();
	    directives = new oval.schemas.directives.core.ObjectFactory();
	    evaluation = new oval.schemas.evaluation.id.ObjectFactory();
	    results = new oval.schemas.results.core.ObjectFactory();
	    sc = new SystemCharacteristicsFactories();
	    variables = new oval.schemas.variables.core.ObjectFactory();
	}
    }

    public static class DefinitionFactories {
	public oval.schemas.definitions.aix.ObjectFactory aix;
	public oval.schemas.definitions.apache.ObjectFactory apache;
	public oval.schemas.definitions.catos.ObjectFactory catos;
	public oval.schemas.definitions.core.ObjectFactory core;
	public oval.schemas.definitions.esx.ObjectFactory esx;
	public oval.schemas.definitions.freebsd.ObjectFactory freebsd;
	public oval.schemas.definitions.hpux.ObjectFactory hpux;
	public oval.schemas.definitions.independent.ObjectFactory independent;
	public oval.schemas.definitions.ios.ObjectFactory ios;
	public oval.schemas.definitions.linux.ObjectFactory linux;
	public oval.schemas.definitions.macos.ObjectFactory macos;
	public oval.schemas.definitions.pixos.ObjectFactory pixos;
	public oval.schemas.definitions.sharepoint.ObjectFactory sharepoint;
	public oval.schemas.definitions.solaris.ObjectFactory solaris;
	public oval.schemas.definitions.unix.ObjectFactory unix;
	public oval.schemas.definitions.windows.ObjectFactory windows;

	private DefinitionFactories() {
	    aix = new oval.schemas.definitions.aix.ObjectFactory();
	    apache = new oval.schemas.definitions.apache.ObjectFactory();
	    catos = new oval.schemas.definitions.catos.ObjectFactory();
	    core = new oval.schemas.definitions.core.ObjectFactory();
	    esx = new oval.schemas.definitions.esx.ObjectFactory();
	    freebsd = new oval.schemas.definitions.freebsd.ObjectFactory();
	    hpux = new oval.schemas.definitions.hpux.ObjectFactory();
	    independent = new oval.schemas.definitions.independent.ObjectFactory();
	    ios = new oval.schemas.definitions.ios.ObjectFactory();
	    linux = new oval.schemas.definitions.linux.ObjectFactory();
	    macos = new oval.schemas.definitions.macos.ObjectFactory();
	    pixos = new oval.schemas.definitions.pixos.ObjectFactory();
	    sharepoint = new oval.schemas.definitions.sharepoint.ObjectFactory();
	    solaris = new oval.schemas.definitions.solaris.ObjectFactory();
	    unix = new oval.schemas.definitions.unix.ObjectFactory();
	    windows = new oval.schemas.definitions.windows.ObjectFactory();
	}
    }

    public static class SystemCharacteristicsFactories {
	public oval.schemas.systemcharacteristics.aix.ObjectFactory aix;
	public oval.schemas.systemcharacteristics.apache.ObjectFactory apache;
	public oval.schemas.systemcharacteristics.catos.ObjectFactory catos;
	public oval.schemas.systemcharacteristics.core.ObjectFactory core;
	public oval.schemas.systemcharacteristics.esx.ObjectFactory esx;
	public oval.schemas.systemcharacteristics.freebsd.ObjectFactory freebsd;
	public oval.schemas.systemcharacteristics.hpux.ObjectFactory hpux;
	public oval.schemas.systemcharacteristics.independent.ObjectFactory independent;
	public oval.schemas.systemcharacteristics.ios.ObjectFactory ios;
	public oval.schemas.systemcharacteristics.linux.ObjectFactory linux;
	public oval.schemas.systemcharacteristics.macos.ObjectFactory macos;
	public oval.schemas.systemcharacteristics.pixos.ObjectFactory pixos;
	public oval.schemas.systemcharacteristics.sharepoint.ObjectFactory sharepoint;
	public oval.schemas.systemcharacteristics.solaris.ObjectFactory solaris;
	public oval.schemas.systemcharacteristics.unix.ObjectFactory unix;
	public oval.schemas.systemcharacteristics.windows.ObjectFactory windows;

	private SystemCharacteristicsFactories() {
	    aix = new oval.schemas.systemcharacteristics.aix.ObjectFactory();
	    apache = new oval.schemas.systemcharacteristics.apache.ObjectFactory();
	    catos = new oval.schemas.systemcharacteristics.catos.ObjectFactory();
	    core = new oval.schemas.systemcharacteristics.core.ObjectFactory();
	    esx = new oval.schemas.systemcharacteristics.esx.ObjectFactory();
	    freebsd = new oval.schemas.systemcharacteristics.freebsd.ObjectFactory();
	    hpux = new oval.schemas.systemcharacteristics.hpux.ObjectFactory();
	    independent = new oval.schemas.systemcharacteristics.independent.ObjectFactory();
	    ios = new oval.schemas.systemcharacteristics.ios.ObjectFactory();
	    linux = new oval.schemas.systemcharacteristics.linux.ObjectFactory();
	    macos = new oval.schemas.systemcharacteristics.macos.ObjectFactory();
	    pixos = new oval.schemas.systemcharacteristics.pixos.ObjectFactory();
	    sharepoint = new oval.schemas.systemcharacteristics.sharepoint.ObjectFactory();
	    solaris = new oval.schemas.systemcharacteristics.solaris.ObjectFactory();
	    unix = new oval.schemas.systemcharacteristics.unix.ObjectFactory();
	    windows = new oval.schemas.systemcharacteristics.windows.ObjectFactory();
	}
    }
}
