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
    public static final int XCCDF_RESULT_PASS		= 101;
    public static final int XCCDF_RESULT_FAIL		= 102;
    public static final int XCCDF_RESULT_ERROR		= 103;
    public static final int XCCDF_RESULT_UNKNOWN	= 104;
    public static final int XCCDF_RESULT_NOT_APPLICABLE	= 105;
    public static final int XCCDF_RESULT_NOT_CHECKED	= 106;
    public static final int XCCDF_RESULT_NOT_SELECTED	= 107;
    public static final int XCCDF_RESULT_INFORMATIONAL	= 108;
    public static final int XCCDF_RESULT_FIXED		= 109;

    private static final String ENV_VALUE_PREFIX	= "XCCDF_VALUE_";
    private static final String ENV_TYPE_PREFIX		= "XCCDF_TYPE_";

    private URL source;
    private Properties environment;

    public SCEScript(URL source) {
	this.source = source;
	environment = new Properties();
	environment.setProperty("XCCDF_RESULT_PASS", Integer.toString(XCCDF_RESULT_PASS));
	environment.setProperty("XCCDF_RESULT_FAIL", Integer.toString(XCCDF_RESULT_FAIL));
	environment.setProperty("XCCDF_RESULT_ERROR", Integer.toString(XCCDF_RESULT_ERROR));
	environment.setProperty("XCCDF_RESULT_UNKNOWN", Integer.toString(XCCDF_RESULT_UNKNOWN));
	environment.setProperty("XCCDF_RESULT_NOT_APPLICABLE", Integer.toString(XCCDF_RESULT_NOT_APPLICABLE));
	environment.setProperty("XCCDF_RESULT_NOT_CHECKED", Integer.toString(XCCDF_RESULT_NOT_CHECKED));
	environment.setProperty("XCCDF_RESULT_NOT_SELECTED", Integer.toString(XCCDF_RESULT_NOT_SELECTED));
	environment.setProperty("XCCDF_RESULT_INFORMATIONAL", Integer.toString(XCCDF_RESULT_INFORMATIONAL));
	environment.setProperty("XCCDF_RESULT_FIXED", Integer.toString(XCCDF_RESULT_FIXED));
    }

    /**
     * Set a variable for SCE script execution. Use a null value to unset a variable.
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
