// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.sce;

import java.net.URL;
import java.util.Properties;

/**
 * A representation of a script.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SCEScript {
    private static final String ENV_VALUE_PREFIX	= "XCCDF_VALUE_";
    private static final String ENV_TYPE_PREFIX		= "XCCDF_TYPE_";

    private URL source;
    private Properties environment;

    public SCEScript(URL source) {
	this.source = source;
    }

    /**
     * Set a variable for SCE script execution.
     */
    public void setenv(String name, String value) {
	if (name.startsWith(ENV_VALUE_PREFIX)) {
	    name = ENV_VALUE_PREFIX + name;
	}
	if (value == null) {
	    environment.remove(name);
	} else {
	    environment.setProperty(name, value);
	}
    }
}
