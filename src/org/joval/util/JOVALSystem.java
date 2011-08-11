// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.util;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class is used to configure JOVAL-wide behaviors, like the java.util.logging-based logging.
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

    private static final Logger logger = Logger.getLogger("org.joval");
    private static Properties props, ovalProps;
    private static PropertyResourceBundle resources;

    static {
	props = new Properties();
	ovalProps = new Properties();
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    props.load(cl.getResourceAsStream("joval.system.properties"));
	    ovalProps.load(cl.getResourceAsStream("oval.properties"));

	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("joval.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("joval.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("joval.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    logger.log(Level.WARNING, "Unable to load JOVALSystem Properties", e);
	}
    }

    public static String getMessage(String key, Object... args) {
	return MessageFormat.format(resources.getString(key), args);
    }

    public static String getProperty(String name) {
	return props.getProperty(name);
    }

    public static String getOvalProperty(String name) {
	return ovalProps.getProperty(name);
    }

    public static Logger getLogger() {
	return logger;
    }

    /**
     * Set whether or not to use JOVAL's built-in log handler, which writes to the file <code>joval.log</code> in the
     * <code>java.io.tmpdir</code> directory.
     */
    public static void setInBuiltLogHandler(boolean useParentHandlers) throws SecurityException, IOException {
        logger.setUseParentHandlers(useParentHandlers);
        FileHandler fileHandler = new FileHandler("%t/jOVAL%g.log", 0, 1, true);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
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
