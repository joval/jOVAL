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

import org.joval.intf.identity.ICredentialStore;
import org.joval.intf.oval.IDefinitionFilter;
import org.joval.intf.oval.IDefinitions;
import org.joval.intf.oval.IEngine;
import org.joval.intf.plugin.IPlugin;
import org.joval.intf.util.ILoggable;
import org.joval.oval.OvalException;
import org.joval.oval.engine.DefinitionFilter;
import org.joval.oval.engine.Definitions;
import org.joval.oval.engine.Engine;

/**
 * This class is used to retrieve JOVAL-wide resources, like SLF4J-based logging, cal10n-based messages and jOVAL and OVAL data
 * model properties and object factories.
 *
 * It is also the primary entry-point into the jOVAL SDK, and can be used to create and configure an Engine, and set properties
 * that affect the behavior of the product.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class JOVALSystem {
    /**
     * Property indicating the package names for classes in the OVAL definitions schema.
     */
    public static final String OVAL_PROP_DEFINITIONS = "definitions.packages";

    /**
     * Property indicating the package names for classes in the OVAL results schema.
     */
    public static final String OVAL_PROP_RESULTS = "results.packages";

    /**
     * Property indicating the package names for classes in the OVAL system characteristics schema.
     */
    public static final String OVAL_PROP_SYSTEMCHARACTERISTICS = "systemcharacteristics.packages";

    /**
     * Property indicating the package names for classes in the OVAL variables schema.
     */
    public static final String OVAL_PROP_VARIABLES = "variables.packages";

    /**
     * Property indicating the package names for classes in the OVAL evaluation-id schema.
     */
    public static final String OVAL_PROP_EVALUATION_ID = "evaluation-id.packages";

    /**
     * Property indicating the package names for classes in the OVAL directives schema.
     */
    public static final String OVAL_PROP_DIRECTIVES = "directives.packages";

    /**
     * Property indicating the product name.
     */
    public static final String PROP_PRODUCT = "productName";

    /**
     * Property indicating the product version.
     */
    public static final String PROP_VERSION = "version";

    /**
     * Property indicating the product build date.
     */
    public static final String PROP_BUILD_DATE = "build.date";

    /**
     * Property indicating whether to log messages from JSch to the JOVALSystem logger (true/false).
     */
    public static final String PROP_SSH_DEBUG = "ssh.debug";

    /**
     * Property indicating whether to log messages from JSch to the JOVALSystem logger (true/false).
     */
    public static final String PROP_SSH_ATTACH_LOG = "ssh.attach.log";

    /**
     * Property indicating the number of milliseconds to wait before failing to establish an SSH connection.
     */
    public static final String PROP_SSH_CONNECTION_TIMEOUT = "ssh.conn.timeout";

    /**
     * Property indicating the number of times to re-try establishing an SSH connection in the event of a failure.
     */
    public static final String PROP_SSH_CONNECTION_RETRIES = "ssh.conn.retries";

    /**
     * Property indicating the number of milliseconds to wait for a read before quiting.
     */
    public static final String PROP_SUDO_READ_TIMEOUT = "sudo.read.timeout";

    /**
     * Property indicating the number of times to re-try running a command on a non-Unix SSH-enabled device in the event of an
     * unexpected disconnect.
     */
    public static final String PROP_SSH_EXEC_RETRIES = "exec.retries.ssh";

    /**
     * Property indicating the number of times to re-try running a command on Unix in the event of an unexpected disconnect.
     */
    public static final String PROP_UNIX_EXEC_RETRIES = "exec.retries.unix";

    /**
     * Property indicating the number of times to re-try running a command on Windows in the event of an unexpected disconnect.
     */
    public static final String PROP_WINDOWS_EXEC_RETRIES = "exec.retries.windows";

    /**
     * Property indicating the number of milliseconds to wait for an IOS command to begin to return data.
     */
    public static final String PROP_IOS_READ_TIMEOUT = "ios.read.timeout";

    /**
     * Property governing the behavior of local filesystem map cache pre-load behavior (true/false) on Windows.
     *
     * If false, the local IFilesystem implementation will use the tree-search algorithm to resolve searches.  If true, it
     * will scan (and cache) all file paths on the entire filesystem, and then subsequently perform regular expression
     * matching directly on the paths.
     */
    public static final String PROP_LOCAL_FS_WINDOWS_PRELOAD = "local.fs.windows.preload";

    /**
     * Property specifying a list of root-level paths that should not be preloaded on Linux by an IFilesystem implementation.
     * Delimiter is the ':' character.
     */
    public static final String PROP_LINUX_FS_SKIP = "linux.fs.preload.skip";

    /**
     * A data structure providing easy access to the OVAL schema object factories.
     */
    public static final Factories factories = new Factories();

    private static IMessageConveyor mc;
    private static LocLoggerFactory loggerFactory;
    private static LocLogger sysLogger;
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
	loggerFactory = new LocLoggerFactory(mc);
	sysLogger = loggerFactory.getLocLogger(JOVALSystem.class);
	props = new Properties();
	ovalProps = new Properties();
	try {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    props.load(cl.getResourceAsStream("joval.system.properties"));
	    ovalProps.load(cl.getResourceAsStream("oval.properties"));
	} catch (IOException e) {
	    sysLogger.error(getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    /**
     * Retrieve a localized String, given the key and substitution arguments.
     */
    public static String getMessage(JOVALMsg key, Object... args) {
	return mc.getMessage(key, args);
    }

    /**
     * Retrieve the default localized system logger used by the jOVAL library.
     */
    public static LocLogger getLogger() {
	return sysLogger;
    }

    /**
     * Retrieve/create a localized jOVAL logger with a particular name.  This is useful for passing to an IPlugin, if you
     * want all of the plugin's log messages routed to a specific logger.
     */
    public static LocLogger getLogger(String name) {
	return loggerFactory.getLocLogger(name);
    }

    public static ILoggable getLoggable() {
	return new Loggable();
    }

    /**
     * Override a jOVAL property.
     *
     * @param key specify one of the PROP_* keys
     */
    public static void setProperty(String key, String value) {
	props.setProperty(key, value);
    }

    /**
     * Retrieve a jOVAL property.
     *
     * @param key specify one of the PROP_* keys
     */
    public static String getProperty(String key) {
	return props.getProperty(key);
    }

    /**
     * Retrieve a jOVAL property as a long.
     *
     * @return 0 if the key is not found
     */
    public static long getLongProperty(String key) {
	long l = 0;
	try {
	    String s = props.getProperty(key);
	    if (s != null) {
		l = Long.parseLong(s);
	    }
	} catch (NumberFormatException e) {
	}
	return l;
    }

    /**
     * Retrieve a jOVAL property as an int.
     *
     * @return 0 if the key is not found
     */
    public static int getIntProperty(String key) {
	int i = 0;
	try {
	    String s = props.getProperty(key);
	    if (s != null) {
		i = Integer.parseInt(s);
	    }
	} catch (NumberFormatException e) {
	}
	return i;
    }

    /**
     * Retrieve a jOVAL property as a boolean.  Default value is false.
     */
    public static boolean getBooleanProperty(String key) {
	return "true".equalsIgnoreCase(props.getProperty(key));
    }

    /**
     * Retrieve an OVAL property.
     *
     * @param key specify one of the OVAL_PROP_* keys
     */
    public static String getOvalProperty(String name) {
	return ovalProps.getProperty(name);
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

    /**
     * Create an IDefinitions index for an XML file containing OVAL definitions.
     */
    public static final IDefinitions createDefinitions(File f) throws OvalException {
	return new Definitions(f);
    }

    /**
     * Create an engine for evaluating OVAL definitions using a plugin.  A null plugin value can be specified if an
     * ISystemCharacteristics (or a system-characteristics.xml file) is set before running.
     */
    public static final IEngine createEngine(IPlugin plugin) {
	return new Engine(plugin);
    }

    //
    // Simplify access to all the OVAL Object Factories
    //

    /**
     * A data structure containing fields and structures for accessing all the OVAL object factories.
     */
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

    /**
     * A data structure containing fields for all the OVAL definition object factories.
     */
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

    /**
     * A data structure containing fields for all the OVAL system characteristics object factories.
     */
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

    // Private

    private static class Loggable implements ILoggable {
	Loggable() {}

	// Implement ILoggable

	public LocLogger getLogger() {
	    return sysLogger;
	}

	public void setLogger(LocLogger logger) {
	    // no-op
	}
    }
}
