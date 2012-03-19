// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.joval.intf.plugin.IPlugin;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

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
     * Create the specified plugin.  The plugin's data directory will be set to the JOVALSystem data
     * directory.
     *
     * @throws PluginConfigurationException if there was a problem with the factory configuration of the container
     * @throws NoSuchElementException if the factory could not find the container in its base directory
     */
    public IPlugin createPlugin(String name) throws PluginConfigurationException, NoSuchElementException {
	for (File dir : baseDir.listFiles()) {
	    File f = new File(dir, IPlugin.FactoryProperty.FILENAME.value());
	    if (f.exists() && f.isFile()) {
		Properties props = new Properties();
		try {
		    props.load(new FileInputStream(f));
		} catch (IOException e) {
		}
		if (name.equals(props.getProperty(IPlugin.FactoryProperty.NAME.value()))) {
		    String classpath = props.getProperty(IPlugin.FactoryProperty.CLASSPATH.value());
		    if (classpath == null) {
			throw new PluginConfigurationException(JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_CLASSPATH));
		    } else {
			StringTokenizer tok = new StringTokenizer(classpath, ":");
			URL[] urls = new URL[tok.countTokens()];
			try {
			    for(int i=0; tok.hasMoreTokens(); i++) {
				urls[i] = new File(dir, tok.nextToken()).toURI().toURL();
			    }
			} catch (MalformedURLException e) {
			    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_CLASSPATH_ELT, e.getMessage());
			    throw new PluginConfigurationException(msg);
			}
			URLClassLoader loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
			String main = props.getProperty(IPlugin.FactoryProperty.MAIN.value());
			if (main == null) {
			    throw new PluginConfigurationException(JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_MAIN));
			} else {
			    try {
				Object obj = loader.loadClass(main).newInstance();
				if (obj instanceof IPlugin) {
				    IPlugin plugin = (IPlugin)obj;
				    File dataDir = JOVALSystem.getDataDirectory();
				    if (!dataDir.exists()) {
					dataDir.mkdirs();
				    }
				    plugin.setDataDirectory(dataDir);
				    return plugin;
				} else {
				    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_PLUGIN_INTERFACE);
				    throw new PluginConfigurationException(msg);
				}
			    } catch (Exception e) {
				throw new PluginConfigurationException(e);
			    }
			}
		    }
		}
	    }
	}
	throw new NoSuchElementException(name);
    }

    // Private

    private File baseDir;

    private PluginFactory(File baseDir) throws IllegalArgumentException {
	if (!baseDir.isDirectory()) {
	    throw new IllegalArgumentException(baseDir.getPath());
	} else {
	    this.baseDir = baseDir;
	}
    }
}
