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

    public static final oval.schemas.common.ObjectFactory commonFactory = new oval.schemas.common.ObjectFactory();
    public static final oval.schemas.results.core.ObjectFactory resultsFactory = new oval.schemas.results.core.ObjectFactory();

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
}
