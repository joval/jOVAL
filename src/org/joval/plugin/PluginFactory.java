// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.joval.intf.plugin.IPlugin;
import org.joval.util.JOVALMsg;
import org.joval.util.LogFormatter;

/**
 * Factory for production of IPlugin instances.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class PluginFactory {
    /**
     * Create a new factory that will search for plugins in the specified base plugin directory.
     */
    public static PluginFactory newInstance(File baseDir) throws IllegalArgumentException {
	return new PluginFactory(baseDir);
    }

    /**
     * Find the specified plugin and return an instance of its main class.
     *
     * @throws PluginConfigurationException if there was a problem with the factory configuration of the container
     * @throws NoSuchElementException if the factory could not find the container in its base directory
     */
    public IPlugin createPlugin(String name) throws PluginConfigurationException, NoSuchElementException {
	String main = null;
	try {
	    ClassLoader loader = getClassLoader(name);
	    main = pluginProperties.get(name).getProperty(IPlugin.FactoryProperty.MAIN.value());
	    if (main == null) {
		throw new PluginConfigurationException(JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_MAIN));
	    }
	    return instantiate(loader, main, IPlugin.class);
	} catch (PluginConfigurationException e) {
	    throw e;
	} catch (NoSuchElementException e) {
	    throw e;
	} catch (ClassCastException e) {
	    String intfName = "org.joval.intf.plugin.IPlugin";
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_INTERFACE, main, intfName);
	    throw new PluginConfigurationException(msg);
	} catch (Exception e) {
	    throw new PluginConfigurationException(e);
	}
    }

    // Internal

    private Map<String, Properties> pluginProperties;
    private Map<String, ClassLoader> pluginClassloaders;
    private File baseDir;

    protected PluginFactory(File baseDir) throws IllegalArgumentException {
	if (!baseDir.isDirectory()) {
	    throw new IllegalArgumentException(baseDir.getPath());
	} else {
	    this.baseDir = baseDir;
	}
	pluginClassloaders = new HashMap<String, ClassLoader>();
	pluginProperties = new HashMap<String, Properties>();
    }

    protected <T extends Object> T instantiate(ClassLoader loader, String className, Class<T> type) throws Exception {
	Class clazz = loader.loadClass(className);
	Object obj = clazz.newInstance();
	if (type.isInstance(obj)) {
	    return type.cast(obj);
	}
        String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_INSTANCE, type.getName(), clazz.getName());
        throw new ClassCastException(msg);
    }

    /**
     * Get/Create the ClassLoader for the plugin with the specified name.
     */
    protected ClassLoader getClassLoader(String name) throws PluginConfigurationException, NoSuchElementException {
	if (pluginClassloaders.containsKey(name)) {
	    return pluginClassloaders.get(name);
	}
	for (File dir : baseDir.listFiles()) {
	    File f = new File(dir, IPlugin.FactoryProperty.FILENAME.value());
	    if (f.exists() && f.isFile()) {
		Properties props = new Properties();
		try {
		    props.load(new FileInputStream(f));
		} catch (IOException e) {
		}
		if (name.equals(props.getProperty(IPlugin.FactoryProperty.NAME.value()))) {
		    //
		    // Save the properties associated with the name
		    //
		    pluginProperties.put(name, props);

		    //
		    // We found the named plugin; get its classpath and create a ClassLoader
		    //
		    String classpath = props.getProperty(IPlugin.FactoryProperty.CLASSPATH.value());
		    if (classpath == null) {
			throw new PluginConfigurationException(JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_CLASSPATH));
		    }
		    StringTokenizer tok = new StringTokenizer(classpath, ":");
		    URL[] urls = new URL[tok.countTokens()];
		    try {
			for (int i=0; tok.hasMoreTokens(); i++) {
			    urls[i] = new File(dir, tok.nextToken()).toURI().toURL();
			}
		    } catch (MalformedURLException e) {
			String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_CLASSPATH_ELT, e.getMessage());
			throw new PluginConfigurationException(msg);
		    }
		    ClassLoader loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());

		    //
		    // If the plugin contains a config/session.ini file, then load the plugin classloader's Configurator
		    // and invoke its addConfiguration method.
		    //
		    File configDir = new File(dir, "config");
		    if (configDir.isDirectory()) {
			File configFile = new File(configDir, "session.ini");
			if (configFile.exists()) {
			    try {
				Class clazz = loader.loadClass("jsaf.provider.Configurator");
				@SuppressWarnings("unchecked")
				Method method = clazz.getMethod("addConfiguration", File.class);
				method.invoke(null, configFile);
			    } catch (Exception e) {
				JOVALMsg.getLogger().warn(JOVALMsg.ERROR_CONFIG_OVERLAY, LogFormatter.toString(e));
			    }
			}
		    }

		    //
		    // Save and return the classloader
		    //
		    pluginClassloaders.put(name, loader);
		    return loader;
		}
	    }
	}
	throw new NoSuchElementException(name);
    }
}
