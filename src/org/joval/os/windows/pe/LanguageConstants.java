// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.os.windows.pe;

import java.io.IOException;
import java.util.Properties;

import org.joval.util.JOVALMsg;

/**
 * See http://msdn.microsoft.com/en-us/library/dd318693%28v=vs.85%29.aspx
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class LanguageConstants {
    private static Properties locales;

    static {
	locales = new Properties();
	ClassLoader cl = LanguageConstants.class.getClassLoader();
	try {
	    locales.load(cl.getResourceAsStream("windows.locales.properties"));
	} catch (Throwable e) {
	    JOVALMsg.getLogger().warn(JOVALMsg.ERROR_WINPE_LOCALERESOURCE);
	    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    public static String getLocaleString(String hex) {
	return locales.getProperty(hex.substring(0, 4).toLowerCase());
    }
}
