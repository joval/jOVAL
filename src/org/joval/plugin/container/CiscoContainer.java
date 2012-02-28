// Copyright (C) 2012 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.plugin.container;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import org.joval.intf.cisco.system.ITechSupport;
import org.joval.os.cisco.system.TechSupport;
import org.joval.os.juniper.system.SupportInformation;
import org.joval.intf.plugin.IPlugin;
import org.joval.plugin.CiscoPlugin;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;

/**
 * Continer for the CiscoPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class CiscoContainer extends AbstractContainer {
    public CiscoContainer() {
	super();
    }

    // Implement IPluginContainer

    @Override
    public void configure(Properties props) throws Exception {
	super.configure(props);

	String str = props.getProperty("tech.url");
	if (str == null) {
	    throw new Exception(getMessage("err.configPropMissing", "tech.url"));
	}
	URL url = CiscoPlugin.toURL(props.getProperty("tech.url"));
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	InputStream in = url.openStream();
	byte[] buff = new byte[1024];
	int len = 0;
	while((len = in.read(buff)) > 0) {
	    out.write(buff, 0, len);
	}

	ITechSupport tech = new TechSupport(new ByteArrayInputStream(out.toByteArray()));
	if (tech.getHeadings().size() == 0) {
	    tech = new SupportInformation(new ByteArrayInputStream(out.toByteArray()));
	}

	plugin = new CiscoPlugin(tech);
    }
}
