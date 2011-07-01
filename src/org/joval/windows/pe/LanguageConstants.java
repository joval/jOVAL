package org.joval.windows.pe;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import org.joval.util.JOVALSystem;

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
	    JOVALSystem.getLogger().log(Level.WARNING, JOVALSystem.getMessage("ERROR_WINPE_LOCALERESOURCE"), e);
	}
    }

    public static String getLocaleString(String hex) {
	return locales.getProperty(hex.substring(0, 4).toLowerCase());
    }
}
