// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.oval.di;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import org.joval.intf.plugin.IPlugin;
import org.joval.os.embedded.system.TechSupport;
import org.joval.plugin.CiscoPlugin;
import org.joval.protocol.tftp.TftpURLConnection;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Jovaldi continer for the CiscoPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CiscoContainer implements IPluginContainer {
    private static PropertyResourceBundle resources;
    static {
	try {
	    ClassLoader cl = CiscoContainer.class.getClassLoader();
	    Locale locale = Locale.getDefault();
	    URL url = cl.getResource("plugin.resources_" + locale.toString() + ".properties");
	    if (url == null) {
		url = cl.getResource("plugin.resources_" + locale.getLanguage() + ".properties");
	    }
	    if (url == null) {
		url = cl.getResource("plugin.resources.properties");
	    }
	    resources = new PropertyResourceBundle(url.openStream());
	} catch (IOException e) {
	    JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private IPlugin plugin;

    public CiscoContainer() {
    }

    // Implement IPluginContainer

    public void setDataDirectory(File dir) {}

    public void configure(Properties props) throws Exception {
	TechSupport tech = null;
	String str = props.getProperty("tech.url");
	Exception ex = null;
	try {
	    URL url = new URL(str);
	    tech = new TechSupport(url.openStream());
	} catch (MalformedURLException e) {
	    ex = e;
	}
	if (tech == null) {
	    try {
		URL url = new URL(null, str, new CiscoURLStreamHandler());
		tech = new TechSupport(url.openStream());
	    } catch (MalformedURLException e) {
	    } catch (SecurityException e) {
		e.printStackTrace();
	    }
	}
	if (tech == null) {
	    File f = new File(str);
	    if (f.isFile()) {
		tech = new TechSupport(new FileInputStream(f));
	    } else {
		JOVALSystem.getLogger().warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), ex);
		throw ex;
	    }
	}
	plugin = new CiscoPlugin(tech);
    }

    public String getProperty(String key) {
	return resources.getString(key);
    }

    public IPlugin getPlugin() {
	return plugin;
    }

class CiscoURLStreamHandler extends URLStreamHandler {
    CiscoURLStreamHandler() {
    }

    public URLConnection openConnection(URL u) throws IOException {
        if ("tftp".equals(u.getProtocol())) {
            return new TftpURLConnection(u);
        } else {
            throw new MalformedURLException(JOVALSystem.getMessage(JOVALMsg.ERROR_PROTOCOL, u.getProtocol()));
        }
    }
}
}
