// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.container;

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

import org.joval.intf.plugin.IPluginContainer;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Abstract container for a generic IPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class ContainerFactory {
    /**
     * Create a new factory that will search for plugins in the specified base plugin directory.
     */
    public static ContainerFactory newInstance(File baseDir) throws IllegalArgumentException {
	return new ContainerFactory(baseDir);
    }

    /**
     * Create a container for the specified plugin name.  The container's data directory will be set to the JOVALSystem
     * data directory.
     *
     * @throws ContainerConfigurationException if there was a problem with the factory configuration of the container
     * @throws NoSuchElementException if the factory could not find the container in its base directory
     */
    public IPluginContainer createContainer(String name) throws ContainerConfigurationException, NoSuchElementException {
	for (File dir : baseDir.listFiles()) {
	    File f = new File(dir, IPluginContainer.FactoryProperty.FILENAME.value());
	    if (f.exists() && f.isFile()) {
		Properties props = new Properties();
		try {
		    props.load(new FileInputStream(f));
		} catch (IOException e) {
		}
		if (name.equals(props.getProperty(IPluginContainer.FactoryProperty.NAME.value()))) {
		    String classpath = props.getProperty(IPluginContainer.FactoryProperty.CLASSPATH.value());
		    if (classpath == null) {
			throw new ContainerConfigurationException(JOVALSystem.getMessage(JOVALMsg.ERROR_CONTAINER_CLASSPATH));
		    } else {
			StringTokenizer tok = new StringTokenizer(classpath, ":");
			URL[] urls = new URL[tok.countTokens()];
			try {
			    for(int i=0; tok.hasMoreTokens(); i++) {
				urls[i] = new File(dir, tok.nextToken()).toURI().toURL();
			    }
			} catch (MalformedURLException e) {
			    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_CONTAINER_CLASSPATH_ELT, e.getMessage());
			    throw new ContainerConfigurationException(msg);
			}
			URLClassLoader loader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
			String main = props.getProperty(IPluginContainer.FactoryProperty.MAIN.value());
			if (main == null) {
			    throw new ContainerConfigurationException(JOVALSystem.getMessage(JOVALMsg.ERROR_CONTAINER_MAIN));
			} else {
			    try {
				Object obj = loader.loadClass(main).newInstance();
				if (obj instanceof IPluginContainer) {
				    IPluginContainer container = (IPluginContainer)obj;
				    File dataDir = JOVALSystem.getDataDirectory();
				    if (!dataDir.exists()) {
					dataDir.mkdirs();
				    }
				    container.setDataDirectory(dataDir);
				    return container;
				} else {
				    String msg = JOVALSystem.getMessage(JOVALMsg.ERROR_CONTAINER_INTERFACE);
				    throw new ContainerConfigurationException(msg);
				}
			    } catch (Exception e) {
				throw new ContainerConfigurationException(e);
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

    private ContainerFactory(File baseDir) throws IllegalArgumentException {
	if (!baseDir.isDirectory()) {
	    throw new IllegalArgumentException(baseDir.getPath());
	} else {
	    this.baseDir = baseDir;
	}
    }
}
